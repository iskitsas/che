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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
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

    private Map<String, String>     authProperties;
    private Map<String, AuthConfig> configMap;
    private AuthConfigs             authConfigs;

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
        authProperties = configurationProperties.getProperties(CONFIGURATION_PREFIX_PATTERN);

        Set<String> registryPrefixes = authProperties.entrySet()
                                                     .stream()
                                                     .map(property -> getRegistryPrefix(property.getKey()))
                                                     .collect(Collectors.toSet());

        configMap = registryPrefixes.stream().collect(toMap(regPrefix -> authProperties.get(regPrefix + URL), this::createConfig));
    }

    /**
     * Returns docker model config file {@link AuthConfig}
     */
    public AuthConfigs getAuthConfigs() {
        if (authConfigs == null) {
            authConfigs = newDto(AuthConfigs.class);
            authConfigs.getConfigs().putAll(configMap);
        }
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

    private AuthConfig createConfig(String registryPrefix) {
        return newDto(AuthConfig.class).withServeraddress(getProperty(registryPrefix + URL))
                                       .withUsername(getProperty(registryPrefix + USER_NAME))
                                       .withPassword(getProperty(registryPrefix + PASSWORD));
    }

    private String getProperty(String propertyName) {
        String property = authProperties.get(propertyName);
        if (isNullOrEmpty(property)) {
            throw new IllegalArgumentException("You missed property " + propertyName);
        }
        return property;
    }

}
