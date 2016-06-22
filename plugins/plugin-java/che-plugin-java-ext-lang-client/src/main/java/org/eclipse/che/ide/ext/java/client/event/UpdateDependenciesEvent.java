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
package org.eclipse.che.ide.ext.java.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Event that describes the state of Update Dependencies process.
 *
 * @author Alexander Andrienko
 * @author Roman Nikitenko
 */
public class UpdateDependenciesEvent extends GwtEvent<UpdateDependenciesEvent.DependencyUpdatedEventHandler> {
    public static final Type<DependencyUpdatedEventHandler> TYPE = new Type<>();

    private final UpdateDependenciesState updateDependenciesState;

    public interface DependencyUpdatedEventHandler extends EventHandler {
        /** Called when Update Dependencies process is starting. */
        void onUpdateDependenciesStarting();

        /** Called when Update Dependencies process has been finished. */
        void onUpdateDependenciesFinished();
    }

    /** Describes state of Update Dependencies process. */
    public enum UpdateDependenciesState {
        STARTING, FINISHED
    }

    /**
     * Create new {@link UpdateDependenciesEvent}.
     *
     * @param state
     *         the state of Update dependencies process
     */
    protected UpdateDependenciesEvent(UpdateDependenciesState state) {
        this.updateDependenciesState = state;
    }

    /**
     * Creates a Update Dependencies Starting event.
     */
    public static UpdateDependenciesEvent createUpdateDependenciesStartingEvent() {
        return new UpdateDependenciesEvent(UpdateDependenciesState.STARTING);
    }

    /**
     * Creates a Update Dependencies Finished event.
     */
    public static UpdateDependenciesEvent createUpdateDependenciesFinishedEvent() {
        return new UpdateDependenciesEvent(UpdateDependenciesState.FINISHED);
    }

    @Override
    public Type<DependencyUpdatedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(DependencyUpdatedEventHandler handler) {
        switch (updateDependenciesState) {
            case STARTING:
                handler.onUpdateDependenciesStarting();
                break;
            case FINISHED:
                handler.onUpdateDependenciesFinished();
                break;
            default:
                break;
        }
    }
}
