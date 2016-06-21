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

import org.eclipse.che.inject.ConfigurationProperties;
import org.eclipse.che.plugin.docker.client.dto.AuthConfig;
import org.eclipse.che.plugin.docker.client.dto.AuthConfigs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
public class InitialAuthConfig {

    private static final Logger LOG = LoggerFactory.getLogger(InitialAuthConfig.class);

    private static final String URL       = "url";
    private static final String USER_NAME = "username";
    private static final String PASSWORD  = "password";

    private AuthConfigs authConfigs;

    @VisibleForTesting
    protected static final String CONFIG_PREFIX                      = "docker.registry.auth.";
    @VisibleForTesting
    protected static final String CONFIGURATION_PREFIX_PATTERN       = "docker\\.registry\\.auth\\..+";
    @VisibleForTesting
    protected static final String VALID_DOCKER_PROPERTY_NAME_EXAMPLE = CONFIG_PREFIX + "registry_name.parameter_name";

    /** For testing purposes */
    public InitialAuthConfig() {
    }

    @Inject
    public InitialAuthConfig(ConfigurationProperties configurationProperties) {
        Map<String, String> authProperties = configurationProperties.getProperties(CONFIGURATION_PREFIX_PATTERN);

        Set<String> registryPrefixes = authProperties.entrySet()
                                                     .stream()
                                                     .map(property -> getRegistryPrefix(property.getKey()))
                                                     .collect(Collectors.toSet());

        Map<String, AuthConfig> configMap = new HashMap<>();
        for (String regPrefix: registryPrefixes) {
            String url = getPropertyValue(authProperties, regPrefix + URL);
            String userName = getPropertyValue(authProperties, regPrefix + USER_NAME);
            String password = getPropertyValue(authProperties, regPrefix + PASSWORD);

            configMap.put(url, newDto(AuthConfig.class).withUsername(userName).withPassword(password));
        }

        authConfigs = newDto(AuthConfigs.class).withConfigs(configMap);
    }

    /**
     * Returns docker model config file {@link AuthConfig}
     */
    public AuthConfigs getAuthConfigs() {
        return authConfigs;
    }

    private String getRegistryPrefix(String propertyName) {
        String[] parts = propertyName.replaceFirst(CONFIG_PREFIX, "").split("\\.");

        if (parts.length < 2) {
            throw new IllegalArgumentException(format("You missed '.' in property '%s'. Valid credential registry format is '%s'",
                                                      propertyName, VALID_DOCKER_PROPERTY_NAME_EXAMPLE));
        }
        if (parts.length > 2) {
            throw new IllegalArgumentException(format("You set redundant '.' in property '%s'. Valid credential registry format is '%s'",
                                                      propertyName, VALID_DOCKER_PROPERTY_NAME_EXAMPLE));
        }

        String propertyIdentifier = parts[1];
        if (!URL.equals(propertyIdentifier) && !USER_NAME.equals(propertyIdentifier) && !PASSWORD.equals(propertyIdentifier)) {
            LOG.warn("Set unused property: " + propertyName);
        }

        return CONFIG_PREFIX + parts[0] + ".";
    }

    private String getPropertyValue(Map<String, String> authProperties, String propertyName) {
        String propertyValue = authProperties.get(propertyName);
        if (isNullOrEmpty(propertyValue)) {
            throw new IllegalArgumentException("You missed property " + propertyName);
        }
        return propertyValue;
    }

}
