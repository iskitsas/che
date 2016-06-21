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

import org.eclipse.che.inject.ConfigurationProperties;
import org.eclipse.che.plugin.docker.client.dto.AuthConfig;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.eclipse.che.plugin.docker.client.InitialAuthConfig.CONFIG_PREFIX;
import static org.eclipse.che.plugin.docker.client.InitialAuthConfig.CONFIGURATION_PREFIX_PATTERN;
import static org.eclipse.che.plugin.docker.client.InitialAuthConfig.VALID_DOCKER_PROPERTY_NAME_EXAMPLE;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Alexander Andrienko
 */
@Listeners(MockitoTestNGListener.class)
public class InitialAuthConfigTest {

    private static final String URL1_KEY         = "registry1.url";
    private static final String URL1_VALUE       = "http://docker.io";
    private static final String USER_NAME1_KEY   = "registry1.username";
    private static final String USER_NAME1_VALUE = "hamlet";
    private static final String PASSWORD1_KEY    = "registry1.password";
    private static final String PASSWORD1_VALUE  = "The game is afoot";


    private static final String URL2_KEY         = "registry2.url";
    private static final String URL2_VALUE       = "http://some.private.registry";
    private static final String USER_NAME2_KEY   = "registry2.username";
    private static final String USER_NAME2_VALUE = "lir";
    private static final String PASSWORD2_KEY    = "registry2.password";
    private static final String PASSWORD2_VALUE  = "Truth will out";

    private final Map<String, String> properties = new HashMap<>();

    @Mock
    private ConfigurationProperties configurationProperties;

    private AuthConfig authConfig1;
    private AuthConfig authConfig2;

    @BeforeMethod
    public void cleanUp() throws IllegalArgumentException {
        properties.clear();

        properties.put(CONFIG_PREFIX + URL1_KEY, URL1_VALUE);
        properties.put(CONFIG_PREFIX + USER_NAME1_KEY, USER_NAME1_VALUE);
        properties.put(CONFIG_PREFIX + PASSWORD1_KEY, PASSWORD1_VALUE);

        properties.put(CONFIG_PREFIX + URL2_KEY, URL2_VALUE);
        properties.put(CONFIG_PREFIX + USER_NAME2_KEY, USER_NAME2_VALUE);
        properties.put(CONFIG_PREFIX + PASSWORD2_KEY, PASSWORD2_VALUE);

        when(configurationProperties.getProperties(CONFIGURATION_PREFIX_PATTERN)).thenReturn(properties);

        authConfig1 = newDto(AuthConfig.class).withUsername(USER_NAME1_VALUE)
                                              .withPassword(PASSWORD1_VALUE);

        authConfig2 = newDto(AuthConfig.class).withUsername(USER_NAME2_VALUE)
                                              .withPassword(PASSWORD2_VALUE);
    }

    @Test
    public void configurationShouldBeCreatedValidByConfigurationProperties() throws IllegalArgumentException {
        InitialAuthConfig initialAuthConfig = new InitialAuthConfig(configurationProperties);

        Map<String, AuthConfig> configs = initialAuthConfig.getAuthConfigs().getConfigs();

        assertEquals(configs.get(URL1_VALUE), authConfig1);
        assertEquals(configs.get(URL2_VALUE), authConfig2);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "You missed '.' in property '" + CONFIG_PREFIX + "url'. " +
                                            "Valid credential registry format is '" + VALID_DOCKER_PROPERTY_NAME_EXAMPLE + "'")
    public void shouldThrowExceptionWhenUserMissedPointInProperty() throws IllegalArgumentException {
        properties.put(CONFIG_PREFIX + "url", URL1_VALUE);

        new InitialAuthConfig(configurationProperties);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "You set redundant '.' in property '" + CONFIG_PREFIX + "my.registry.docker.url'. " +
                                            "Valid credential registry format is '" + VALID_DOCKER_PROPERTY_NAME_EXAMPLE + "'")
    public void shouldThrowExceptionWhenUserSetRedundantPointInProperty() throws IllegalArgumentException {
        properties.put(CONFIG_PREFIX + "my.registry.docker.url", URL1_VALUE);

        new InitialAuthConfig(configurationProperties);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "You missed property " + CONFIG_PREFIX + URL1_KEY)
    public void shouldThrowExceptionIfUserMissedUrlProperty() throws IllegalArgumentException {
        properties.remove(CONFIG_PREFIX + URL1_KEY);

        new InitialAuthConfig(configurationProperties);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "You missed property " + CONFIG_PREFIX + USER_NAME1_KEY)
    public void shouldThrowExceptionIfUserMissedUserProperty() throws IllegalArgumentException {
        properties.remove(CONFIG_PREFIX + USER_NAME1_KEY);

        new InitialAuthConfig(configurationProperties);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "You missed property " + CONFIG_PREFIX + PASSWORD1_KEY)
    public void shouldThrowExceptionIfUserMissedPasswordProperty() throws IllegalArgumentException {
        properties.remove(CONFIG_PREFIX + PASSWORD1_KEY);

        new InitialAuthConfig(configurationProperties);
    }

    @Test
    public void shouldBeReturnedAuthConfigsWithEmptyMapConfigs() throws IllegalArgumentException {
        properties.clear();

        InitialAuthConfig initialAuthConfig = new InitialAuthConfig(configurationProperties);

        assertTrue(initialAuthConfig.getAuthConfigs().getConfigs().isEmpty());
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "You missed property " + CONFIG_PREFIX + URL1_KEY)
    public void shouldThrowExceptionIfUserSetEmptyUrlValue() throws IllegalArgumentException {
        properties.put(CONFIG_PREFIX + URL1_KEY, "");

        new InitialAuthConfig(configurationProperties);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "You missed property " + CONFIG_PREFIX + URL1_KEY)
    public void shouldThrowExceptionIfUserSetNullUrlValue() throws IllegalArgumentException {
        properties.put(CONFIG_PREFIX + URL1_KEY, null);

        new InitialAuthConfig(configurationProperties);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "You missed property " + CONFIG_PREFIX + USER_NAME1_KEY)
    public void shouldThrowExceptionIfUserSetEmptyUserNameValue() throws IllegalArgumentException {
        properties.put(CONFIG_PREFIX + USER_NAME1_KEY, "");

        new InitialAuthConfig(configurationProperties);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "You missed property " + CONFIG_PREFIX + USER_NAME1_KEY)
    public void shouldThrowExceptionIfUserSetNullUserNameValue() throws IllegalArgumentException {
        properties.put(CONFIG_PREFIX + USER_NAME1_KEY, null);

        new InitialAuthConfig(configurationProperties);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "You missed property " + CONFIG_PREFIX + PASSWORD1_KEY)
    public void shouldThrowExceptionIfUserSetEmptyPasswordValue() throws IllegalArgumentException {
        properties.put(CONFIG_PREFIX + PASSWORD1_KEY, "");

        new InitialAuthConfig(configurationProperties);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "You missed property " + CONFIG_PREFIX + PASSWORD1_KEY)
    public void shouldThrowExceptionIfUserSetNullPasswordValue() throws IllegalArgumentException {
        properties.put(CONFIG_PREFIX + PASSWORD1_KEY, null);

        new InitialAuthConfig(configurationProperties);
    }
}

