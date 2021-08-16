/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @author Vincent Ryan
 * @bug 4814522
 * @summary Check that an LdapLoginModule can be initialized using various
 *          JAAS configurations.
 *          (LdapLoginModule replaces the JndiLoginModule for LDAP access)
 *
 * Run this test twice, once using the default security manager:
 *
 * @run main/othervm CheckConfigs
 * @run main/othervm/policy=CheckConfigs.policy CheckConfigs
 */

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

import javax.naming.CommunicationException;
import javax.security.auth.*;
import javax.security.auth.login.*;
import javax.security.auth.callback.*;
import com.sun.security.auth.module.LdapLoginModule;

public class CheckConfigs {

    public static void main(String[] args) throws Exception {
        SecurityManager securityManager = System.getSecurityManager();
        System.out.println(securityManager == null
            ? "[security manager is not running]"
            : "[security manager is running: " +
                securityManager.getClass().getName() + "]");
        init();
        checkConfigModes();
    }

    private static void init() throws Exception {
    }

    private static void checkConfigModes() throws Exception {

        LoginContext ldapLogin;

        // search-first mode
        System.out.println("Testing search-first mode...");
        try {
            ldapLogin = new LoginContext(LdapConfiguration.LOGIN_CONFIG_NAME,
                null, new TestCallbackHandler(), new SearchFirstMode());
            ldapLogin.login();
            throw new SecurityException("expected a LoginException");

        } catch (LoginException le) {
            // expected behaviour (because no LDAP server is available)
            if (!(le.getCause() instanceof CommunicationException)) {
                throw le;
            }
        }

        // authentication-first mode
        System.out.println("\nTesting authentication-first mode...");
        try {
            ldapLogin = new LoginContext(LdapConfiguration.LOGIN_CONFIG_NAME,
                null, new TestCallbackHandler(), new AuthFirstMode());
            ldapLogin.login();
            throw new SecurityException("expected a LoginException");

        } catch (LoginException le) {
            // expected behaviour (because no LDAP server is available)
            if (!(le.getCause() instanceof CommunicationException)) {
                throw le;
            }
        }

        // authentication-only mode
        System.out.println("\nTesting authentication-only mode...");
        try {
            ldapLogin = new LoginContext(LdapConfiguration.LOGIN_CONFIG_NAME,
                null, new TestCallbackHandler(), new AuthOnlyMode());
            ldapLogin.login();
            throw new SecurityException("expected a LoginException");

        } catch (LoginException le) {
            // expected behaviour (because no LDAP server is available)
            if (!(le.getCause() instanceof CommunicationException)) {
                throw le;
            }
        }
    }

    private static class TestCallbackHandler implements CallbackHandler {

        public void handle(Callback[] callbacks)
                throws IOException, UnsupportedCallbackException {

            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof NameCallback) {
                    ((NameCallback)callbacks[i]).setName("myname");

                } else if (callbacks[i] instanceof PasswordCallback) {
                    ((PasswordCallback)callbacks[i])
                        .setPassword("mypassword".toCharArray());

                } else {
                    throw new UnsupportedCallbackException
                        (callbacks[i], "Unrecognized callback");
                }
            }
        }
    }
}

class LdapConfiguration extends Configuration {

    // The JAAS configuration name for ldap-based authentication
    public static final String LOGIN_CONFIG_NAME = "TestAuth";

    // The JAAS configuration for ldap-based authentication
    protected static AppConfigurationEntry[] entries;

    // The classname of the login module for ldap-based authentication
    protected static final String LDAP_LOGIN_MODULE =
        LdapLoginModule.class.getName();

    /**
     * Gets the JAAS configuration for ldap-based authentication
     */
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        return name.equals(LOGIN_CONFIG_NAME) ? entries : null;
    }

    /**
     * Refreshes the configuration.
     */
    public void refresh() {
        // the configuration is fixed
    }
}

/**
 * This class defines the JAAS configuration for ldap-based authentication.
 * It is equivalent to the following textual configuration entry:
 * <pre>
 *     TestAuth {
 *         com.sun.security.auth.module.LdapLoginModule REQUIRED
 *             userProvider="ldap://localhost:23456/dc=example,dc=com"
 *             userFilter="(&(uid={USERNAME})(objectClass=inetOrgPerson))"
 *             authzIdentity="{EMPLOYEENUMBER}"
 *             debug=true;
 *     };
 * </pre>
 */
class SearchFirstMode extends LdapConfiguration {

    public SearchFirstMode() {
        super();

        Map<String, String> options = new HashMap<>(4);
        options.put("userProvider", "ldap://localhost:23456/dc=example,dc=com");
        options.put("userFilter",
            "(&(uid={USERNAME})(objectClass=inetOrgPerson))");
        options.put("authzIdentity", "{EMPLOYEENUMBER}");
        options.put("debug", "true");

        entries = new AppConfigurationEntry[] {
            new AppConfigurationEntry(LDAP_LOGIN_MODULE,
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    options)
        };
    }

}

/**
 * This class defines the JAAS configuration for ldap-based authentication.
 * It is equivalent to the following textual configuration entry:
 * <pre>
 *     TestAuth {
 *         com.sun.security.auth.module.LdapLoginModule REQUIRED
 *             userProvider="ldap://localhost:23456/dc=example,dc=com"
 *             authIdentity="{USERNAME}"
 *             userFilter="(&(|(samAccountName={USERNAME})(userPrincipalName={USERNAME})(cn={USERNAME}))(objectClass=user))"
 *             useSSL=false
 *             debug=true;
 *     };
 * </pre>
 */
class AuthFirstMode extends LdapConfiguration {

    public AuthFirstMode() {
        super();

        Map<String, String> options = new HashMap<>(5);
        options.put("userProvider", "ldap://localhost:23456/dc=example,dc=com");
        options.put("authIdentity", "{USERNAME}");
        options.put("userFilter",
            "(&(|(samAccountName={USERNAME})(userPrincipalName={USERNAME})" +
            "(cn={USERNAME}))(objectClass=user))");
        options.put("useSSL", "false");
        options.put("debug", "true");

        entries = new AppConfigurationEntry[] {
            new AppConfigurationEntry(LDAP_LOGIN_MODULE,
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    options)
        };
    }
}

/**
 * This class defines the JAAS configuration for ldap-based authentication.
 * It is equivalent to the following textual configuration entry:
 * <pre>
 *     TestAuth {
 *         com.sun.security.auth.module.LdapLoginModule REQUIRED
 *             userProvider="ldap://localhost:23456 ldap://localhost:23457"
 *             authIdentity="cn={USERNAME},ou=people,dc=example,dc=com"
 *             authzIdentity="staff"
 *             debug=true;
 *     };
 * </pre>
 */
class AuthOnlyMode extends LdapConfiguration {

    public AuthOnlyMode() {
        super();

        Map<String, String> options = new HashMap<>(4);
        options.put("userProvider",
            "ldap://localhost:23456 ldap://localhost:23457");
        options.put("authIdentity",
            "cn={USERNAME},ou=people,dc=example,dc=com");
        options.put("authzIdentity", "staff");
        options.put("debug", "true");

        entries = new AppConfigurationEntry[] {
            new AppConfigurationEntry(LDAP_LOGIN_MODULE,
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    options)
        };
    }

}
