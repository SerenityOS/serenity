/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/*
 * @test
 * @bug 4515853 8075297 8194486
 * @summary Checks that Kerberos client tries replica KDC
 *          if primary KDC is not responding
 * @library /test/lib
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts BogusKDC
 */
public class BogusKDC {

    static final String TEST_SRC = System.getProperty("test.src", ".");
    static final String HOST = "localhost";
    static final String NOT_EXISTING_HOST = "not.existing.host";
    static final String REALM = "TEST.REALM";
    static final String USER = "USER";
    static final String USER_PRINCIPAL = USER + "@" + REALM;
    static final String USER_PASSWORD = "password";
    static final String KRBTGT_PRINCIPAL = "krbtgt/" + REALM;
    static final String KRB5_CONF = "krb5.conf";
    static final int WRONG_KDC_PORT = 21;

    static final String KRB5_CONF_TEMPLATE = ""
            + "[libdefaults]\n"
            + "default_realm = TEST.REALM\n"
            + "max_retries = 1\n"
            + "\n"
            + "[realms]\n"
            + "TEST.REALM = {\n"
            + "    kdc = %s\n"
            + "    kdc = localhost:%d\n"
            + "}";

    public static void main(String[] args) throws LoginException, IOException {
        Map<String, String> principals = new HashMap<>();
        principals.put(USER_PRINCIPAL, USER_PASSWORD);
        principals.put(KRBTGT_PRINCIPAL, null);

        System.setProperty("java.security.krb5.conf", KRB5_CONF);

        // start a local KDC
        KDC kdc = KDC.startKDC(HOST, KRB5_CONF, REALM, principals, null, null);

        System.setProperty("java.security.auth.login.config",
                TEST_SRC + File.separator + "refreshKrb5Config.jaas");

        CallbackHandler handler = new Helper.UserPasswordHandler(
                USER, USER_PASSWORD);

        // create a krb5 config with non-existing host for primary KDC,
        // and wrong port for replica KDC
        try (PrintWriter w = new PrintWriter(new FileWriter(KRB5_CONF))) {
            w.write(String.format(KRB5_CONF_TEMPLATE,
                    KDC.NOT_EXISTING_HOST, WRONG_KDC_PORT));
            w.flush();
        }

        // login with not-refreshable config
        try {
            new LoginContext("NotRefreshable", handler).login();
            throw new RuntimeException("Expected exception not thrown");
        } catch (LoginException le) {
            System.out.println("Expected login failure: " + le);
        }

        // create a krb5 config with non-existing host for primary KDC,
        // but correct port for replica KDC
        try (PrintWriter w = new PrintWriter(new FileWriter(KRB5_CONF))) {
            w.write(String.format(KRB5_CONF_TEMPLATE,
                    KDC.NOT_EXISTING_HOST, kdc.getPort()));
            w.flush();
        }

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
