/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8075301
 * @library /sun/security/krb5/auto /test/lib
 * @summary New test for sun.security.krb5.principal system property.
 * The principal can set using the system property sun.security.krb5.principal.
 * This property is checked during login. If this property is not set,
 * then the principal name from the configuration is used.
 * @run main/othervm/java.security.policy=principalSystemPropTest.policy
 * PrincipalSystemPropTest
 */

import java.io.File;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.security.auth.login.LoginException;
import javax.security.auth.login.LoginContext;
import com.sun.security.auth.callback.TextCallbackHandler;

public class PrincipalSystemPropTest {

    private static final boolean PASS = Boolean.TRUE;
    private static final boolean FAIL = Boolean.FALSE;
    private static final String VALID_PRINCIPAL_JAAS_ENTRY =
            "ValidPrincipalSystemPropTest";
    private static final String INVALID_PRINCIPAL_JAAS_ENTRY =
            "InvalidPrincipalSystemPropTest";
    private static final String NO_PRINCIPAL_JAAS_ENTRY =
            "NoPrincipalSystemPropTest";
    private static final String SAME_PRINCIPAL_JAAS_ENTRY =
            "SelfPrincipalSystemPropTest";
    private static final String HOST = "localhost";
    private static final String KTAB_FILENAME = "krb5.keytab.data";
    private static final String REALM = "TEST.REALM";
    private static final String TEST_SRC = System.getProperty("test.src", ".");
    private static final String USER = "USER";
    private static final String AVAILABLE_USER = "AVAILABLE";
    private static final String USER_PASSWORD = "password";
    private static final String FS = System.getProperty("file.separator");
    private static final String KRB5_CONF_FILENAME = "krb5.conf";
    private static final String JAAS_CONF_FILENAME = "jaas.conf";
    private static final String KRBTGT_PRINCIPAL = "krbtgt/" + REALM;
    private static final String USER_PRINCIPAL = USER + "@" + REALM;
    private static final String AVAILABLE_USER_PRINCIPAL =
            AVAILABLE_USER + "@" + REALM;

    public static void main(String[] args) throws Exception {

        setupTest();

        // Expected result, Jaas Config Entry, Login Principal Expected,
        // Principal passed through System property
        runTest(PASS, VALID_PRINCIPAL_JAAS_ENTRY,
                USER_PRINCIPAL, "USER@TEST.REALM");
        runTest(PASS, VALID_PRINCIPAL_JAAS_ENTRY,
                AVAILABLE_USER_PRINCIPAL,  null);
        runTest(PASS, INVALID_PRINCIPAL_JAAS_ENTRY,
                USER_PRINCIPAL, "USER@TEST.REALM");
        runTest(FAIL, INVALID_PRINCIPAL_JAAS_ENTRY, null,  null);
        runTest(PASS, NO_PRINCIPAL_JAAS_ENTRY,
                USER_PRINCIPAL, "USER@TEST.REALM");
        runTest(FAIL, NO_PRINCIPAL_JAAS_ENTRY, null, null);
        runTest(PASS, SAME_PRINCIPAL_JAAS_ENTRY,
                USER_PRINCIPAL, "USER@TEST.REALM");

    }

    private static void setupTest() {

        System.setProperty("java.security.krb5.conf", KRB5_CONF_FILENAME);
        System.setProperty("java.security.auth.login.config",
                TEST_SRC + FS + JAAS_CONF_FILENAME);

        Map<String, String> principals = new HashMap<>();
        principals.put(USER_PRINCIPAL, USER_PASSWORD);
        principals.put(AVAILABLE_USER_PRINCIPAL, USER_PASSWORD);
        principals.put(KRBTGT_PRINCIPAL, null);
        KDC.startKDC(HOST, KRB5_CONF_FILENAME, REALM, principals,
                KTAB_FILENAME, KDC.KtabMode.APPEND);

    }

    private static void runTest(boolean expected, String jaasConfigEntry,
            String expectedLoginUser, String loginUserBySysProp) {

        if(loginUserBySysProp != null) {
            System.setProperty("sun.security.krb5.principal",
                    loginUserBySysProp);
        } else {
            System.clearProperty("sun.security.krb5.principal");
        }

        try {
            LoginContext lc = new LoginContext(jaasConfigEntry,
                    new TextCallbackHandler());
            lc.login();
            System.out.println(String.format(
                    "Authentication completed with Subject '%s' ",
                    lc.getSubject()));

            if (!expected) {
                throw new RuntimeException(
                        "TEST FAILED - JAAS login success isn't expected");
            }
            if(expectedLoginUser != null && !lc.getSubject().getPrincipals()
                    .stream().map(p -> p.getName()).filter(
                            expectedLoginUser :: equals).findFirst()
                            .isPresent()) {
                throw new RuntimeException(String.format(
                        "TEST FAILED - Login principal is not matched "
                        + "to expected principal '%s'.", expectedLoginUser));
            }
            System.out.println(
                    "TEST PASSED - JAAS login success is expected.");
        } catch (LoginException ie) {
            System.out.println(String.format(
                    "Authentication failed with exception: %s",
                    ie.getMessage()));
            if (expected) {
                System.out.println(
                        "TEST FAILED - JAAS login failure isn't expected");
                throw new RuntimeException(ie);
            }
            System.out.println(
                    "TEST PASSED - JAAS login failure is expected.");
        }

    }

}
