/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.jdb.ide.debug;

import com.google.common.base.Optional;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.FileEvent;
import org.eclipse.che.ide.api.resources.File;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.api.resources.SyntheticFile;
import org.eclipse.che.ide.api.resources.VirtualFile;
import org.eclipse.che.ide.debug.DebuggerManager;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.java.client.navigation.service.JavaNavigationService;
import org.eclipse.che.plugin.debugger.ide.debug.ActiveFileHandler;
import org.eclipse.che.ide.ext.java.shared.JarEntry;
import org.eclipse.che.ide.api.editor.document.Document;
import org.eclipse.che.ide.api.editor.text.TextPosition;
import org.eclipse.che.ide.api.editor.texteditor.TextEditorPresenter;
import org.eclipse.che.ide.ext.java.shared.dto.ClassContent;

import javax.validation.constraints.NotNull;
import java.util.List;

import static org.eclipse.che.ide.api.event.FileEvent.FileOperation.OPEN;

/**
 * Responsible to open files in editor when debugger stopped at breakpoint.
 *
 * @author Anatoliy Bazko
 */
public class JavaDebuggerFileHandler implements ActiveFileHandler {

    private final DebuggerManager       debuggerManager;
    private final EditorAgent           editorAgent;
    private final DtoFactory            dtoFactory;
    private final EventBus              eventBus;
    private final JavaNavigationService service;
    private final AppContext            appContext;

    @Inject
    public JavaDebuggerFileHandler(DebuggerManager debuggerManager,
                                   EditorAgent editorAgent,
                                   DtoFactory dtoFactory,
                                   EventBus eventBus,
                                   JavaNavigationService service,
                                   AppContext appContext) {
        this.debuggerManager = debuggerManager;
        this.editorAgent = editorAgent;
        this.dtoFactory = dtoFactory;
        this.eventBus = eventBus;
        this.service = service;
        this.appContext = appContext;
    }

    @Override
    public void openFile(final List<String> filePaths,
                         final String className,
                         final int lineNumber,
                         final AsyncCallback<VirtualFile> callback) {
        if (debuggerManager.getActiveDebugger() != debuggerManager.getDebugger(JavaDebugger.ID)) {
            callback.onFailure(null);
            return;
        }

        VirtualFile activeFile = null;
        final EditorPartPresenter activeEditor = editorAgent.getActiveEditor();
        if (activeEditor != null) {
            activeFile = activeEditor.getEditorInput().getFile();
        }

        if (activeFile == null || !filePaths.contains(activeFile.getLocation().toString())) {
            openFile(className, filePaths, 0, new AsyncCallback<VirtualFile>() {
                @Override
                public void onSuccess(VirtualFile result) {
                    scrollEditorToExecutionPoint((TextEditorPresenter)editorAgent.getActiveEditor(), lineNumber);
                    callback.onSuccess(result);
                }

                @Override
                public void onFailure(Throwable caught) {
                    callback.onFailure(caught);
                }
            });
        } else {
            scrollEditorToExecutionPoint((TextEditorPresenter)activeEditor, lineNumber);
            callback.onSuccess(activeFile);
        }
    }

    /**
     * Tries to open file from the project.
     * If fails then method will try to find resource from external dependencies.
     */
    private void openFile(@NotNull final String className,
                          final List<String> filePaths,
                          final int pathNumber,
                          final AsyncCallback<VirtualFile> callback) {
        if (pathNumber == filePaths.size()) {
            callback.onFailure(new IllegalArgumentException("Can't open resource " + className));
            return;
        }

        String filePath = filePaths.get(pathNumber);

        if (!filePath.startsWith("/")) {
            openExternalResource(className, callback);
            return;
        }

        appContext.getWorkspaceRoot().getFile(filePath).then(new Operation<Optional<File>>() {
            @Override
            public void apply(Optional<File> file) throws OperationException {
                if (file.isPresent()) {
                    handleActivateFile(file.get(), callback);
                    eventBus.fireEvent(new FileEvent(file.get(), OPEN));
                }
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError error) throws OperationException {
                // try another path
                openFile(className, filePaths, pathNumber + 1, callback);
            }
        });
    }

    private void openExternalResource(final String className, final AsyncCallback<VirtualFile> callback) {
        JarEntry jarEntry = dtoFactory.createDto(JarEntry.class);
        jarEntry.setPath(className);
        jarEntry.setName(className.substring(className.lastIndexOf(".") + 1) + ".class");
        jarEntry.setType(JarEntry.JarEntryType.CLASS_FILE);

        final Resource resource = appContext.getResource();

        if (resource == null) {
            callback.onFailure(new IllegalStateException());
            return;
        }

        final Project project = resource.getRelatedProject().get();

        service.getContent(project.getLocation(), className).then(new Operation<ClassContent>() {
            @Override
            public void apply(ClassContent content) throws OperationException {
                VirtualFile file =
                        new SyntheticFile(className.substring(className.lastIndexOf(".") + 1) + ".class", content.getContent());

                handleActivateFile(file, callback);
                eventBus.fireEvent(new FileEvent(file, OPEN));
            }
        });
    }

    public void handleActivateFile(final VirtualFile virtualFile, final AsyncCallback<VirtualFile> callback) {
        editorAgent.openEditor(virtualFile, new EditorAgent.OpenEditorCallback() {
            @Override
            public void onEditorOpened(EditorPartPresenter editor) {
                new Timer() {
                    @Override
                    public void run() {
                        callback.onSuccess(virtualFile);
                    }
                }.schedule(300);
            }

            @Override
            public void onEditorActivated(EditorPartPresenter editor) {
                new Timer() {
                    @Override
                    public void run() {
                        callback.onSuccess(virtualFile);
                    }
                }.schedule(300);
            }

            @Override
            public void onInitializationFailed() {
                callback.onFailure(null);
            }
        });
    }

    private void scrollEditorToExecutionPoint(TextEditorPresenter editor, int lineNumber) {
        Document document = editor.getDocument();

        if (document != null) {
            TextPosition newPosition = new TextPosition(lineNumber, 0);
            document.setCursorPosition(newPosition);
        }
    }
}
