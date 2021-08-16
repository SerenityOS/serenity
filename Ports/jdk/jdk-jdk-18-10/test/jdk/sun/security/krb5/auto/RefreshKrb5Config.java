/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/*
 * @test
 * @bug 4745056 8075297 8194486
 * @summary Checks if refreshKrb5Config is set to true for Krb5LoginModule,
 *          then configuration will be refreshed before login() method is called
 * @library /test/lib
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts RefreshKrb5Config
 */
public class RefreshKrb5Config {

    static final String TEST_SRC = System.getProperty("test.src", ".");
    static final String HOST = "localhost";
    static final String NOT_EXISTING_HOST = "not.existing.host";
    static final String REALM = "TEST.REALM";
    static final String USER = "USER";
    static final String USER_PRINCIPAL = USER + "@" + REALM;
    static final String USER_PASSWORD = "password";
    static final String KRBTGT_PRINCIPAL = "krbtgt/" + REALM;
    static final String KRB5_CONF_FILENAME = "krb5.conf";

    public static void main(String[] args) throws LoginException, IOException {
        Map<String, String> principals = new HashMap<>();
        principals.put(USER_PRINCIPAL, USER_PASSWORD);
        principals.put(KRBTGT_PRINCIPAL, null);

        System.setProperty("java.security.krb5.conf", KRB5_CONF_FILENAME);

        // start a local KDC, and save krb5 config
        KDC kdc = KDC.startKDC(HOST, null, REALM, principals, null, null);
        KDC.saveConfig(KRB5_CONF_FILENAME, kdc, "max_retries = 1");

        System.setProperty("java.security.auth.login.config",
                TEST_SRC + File.separator + "refreshKrb5Config.jaas");

        CallbackHandler handler = new Helper.UserPasswordHandler(
                USER, USER_PASSWORD);

        // set incorrect KDC
        System.out.println("java.security.krb5.kdc = " + NOT_EXISTING_HOST);
        System.setProperty("java.security.krb5.kdc", NOT_EXISTING_HOST);
        System.out.println("java.security.krb5.realm = " + REALM);
        System.setProperty("java.security.krb5.realm", REALM);
        try {
            new LoginContext("Refreshable", handler).login();
            throw new RuntimeException("Expected exception not thrown");
        } catch (LoginException le) {
            System.out.println("Expected login failure: " + le);
        }

        // reset properties
        System.out.println("Reset java.security.krb5.kdc");
        System.clearProperty("java.security.krb5.kdc");
        System.out.println("Reset java.security.krb5.realm");
        System.clearProperty("java.security.krb5.realm");

        // login with not-refreshable config
        try {
            new LoginContext("NotRefreshable", handler).login();
            throw new RuntimeException("Expected exception not thrown");
        } catch (LoginException le) {
            System.out.println("Expected login failure: " + le);
        }

        // login with refreshable config
        new LoginContext("Refreshable", handler).login();

        System.out.println("Test passed");
    }

}
