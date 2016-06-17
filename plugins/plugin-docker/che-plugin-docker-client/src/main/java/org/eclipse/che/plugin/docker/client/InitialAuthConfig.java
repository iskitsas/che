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
package org.eclipse.che.plugin.docker.client;

import com.google.common.annotations.VisibleForTesting;

import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.inject.ConfigurationProperties;
import org.eclipse.che.plugin.docker.client.dto.AuthConfig;
import org.eclipse.che.plugin.docker.client.dto.AuthConfigs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static org.eclipse.che.dto.server.DtoFactory.newDto;

/**
 * Collects auth configurations for private docker registries. Credential might be configured in .properties files, see details {@link
 * org.eclipse.che.inject.CheBootstrap}. Credentials configured as (key=value) pairs. Key is string that starts with prefix
 * {@code docker.registry.auth.} followed by url and credentials of docker registry server.
 * <pre>{@code
 * docker.registry.auth.url=localhost:5000
 * docker.registry.auth.username=user1
 * docker.registry.auth.password=pass
 * }</pre>
 *
 * @author Alexander Garagatyi
 * @author Alexander Andrienko
 */
@Singleton
public class InitialAuthConfig {private static final Logger LOG = LoggerFactory.getLogger(InitialAuthConfig.class);
    private static final String URL       = "url";
    private static final String USER_NAME = "username";
    private static final String PASSWORD  = "password";

    @VisibleForTesting
    protected static final String CONFIG_PREFIX                      = "docker.registry.auth.";
    @VisibleForTesting
    protected static final String CONFIGURATION_PREFIX_PATTERN       = "docker\\.registry\\.auth\\..+";
    @VisibleForTesting
    protected static final String VALID_DOCKER_PROPERTY_NAME_EXAMPLE = CONFIG_PREFIX + "registry_name.parameter_name";

    private final Map<String, AuthConfig> configMap = new HashMap<>();

    /** For testing purposes */
    public InitialAuthConfig() {
    }

    @Inject
    public InitialAuthConfig(ConfigurationProperties properties) throws IllegalArgumentException {
        Map<String, String> authProperties = properties.getProperties(CONFIGURATION_PREFIX_PATTERN);

        Set<String> registryNames = new HashSet<>();
        for (Map.Entry<String, String> property: authProperties.entrySet()) {
            String registryName = getRegistryName(property.getKey());
            registryNames.add(registryName);
        }

        for (String registryName : registryNames) {
            String serverAddress = authProperties.get(CONFIG_PREFIX + registryName + "." + URL);
            String userName = authProperties.get(CONFIG_PREFIX  + registryName + "." + USER_NAME);
            String password = authProperties.get(CONFIG_PREFIX + registryName + "." + PASSWORD);

            AuthConfig authConfig = createConfig(serverAddress, userName, password, registryName);
            configMap.put(serverAddress, authConfig);
        }
    }

    private String getRegistryName(String propertyName) throws IllegalArgumentException {
        String classifier = propertyName.replaceFirst(CONFIG_PREFIX, "");
        String[] parts = classifier.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException(format("You missed '.' in property '%s'. Valid format for credential docker registry is '%s'",
                                             CONFIG_PREFIX + classifier, VALID_DOCKER_PROPERTY_NAME_EXAMPLE));
        }
        if (parts.length > 2) {
            throw new IllegalArgumentException(format("You set redundant '.' in property '%s'. Valid format for credential docker registry is '%s'",
                                             CONFIG_PREFIX + classifier, VALID_DOCKER_PROPERTY_NAME_EXAMPLE));
        }
        return parts[0];
    }

    @Nullable
    private static AuthConfig createConfig(String serverAddress, String username, String password, String registry) throws IllegalArgumentException {
        if (isNullOrEmpty(serverAddress)) {
            throw new IllegalArgumentException("You missed property " + CONFIG_PREFIX + registry + "." + URL);
        }
        if (isNullOrEmpty(username)) {
            throw new IllegalArgumentException("You missed property " + CONFIG_PREFIX + registry + "." + USER_NAME);
        }
        if (isNullOrEmpty(password)) {
            throw new IllegalArgumentException("You missed property " + CONFIG_PREFIX + registry + "." + PASSWORD);
        }
        return newDto(AuthConfig.class).withServeraddress(serverAddress).withUsername(username).withPassword(password);
    }

    public AuthConfigs getAuthConfigs() {
        AuthConfigs authConfigs = newDto(AuthConfigs.class);
        authConfigs.getConfigs().putAll(configMap);
        return authConfigs;
    }

}
