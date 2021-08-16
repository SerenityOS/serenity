/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4703361
 * @modules jdk.security.auth
 * @summary can not specify Configuration to LoginContext constructor
 *
 * @run main/othervm/policy=ConfigConstructorNoPerm.policy -Djava.security.auth.login.config=file:${test.src}/ConfigConstructor.config ConfigConstructorNoPerm
 */

/**
 * This test shares the login config with ConfigConstructor.
 * This test has no configured permissions
 * (ConfigConstructor tests code with perms configured).
 */

import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.callback.CallbackHandler;

public class ConfigConstructorNoPerm {

    private static Subject s = new Subject();
    private static CallbackHandler ch =
                new com.sun.security.auth.callback.TextCallbackHandler();
    private static Configuration c = new MyConfig();

    public static void main(String[] args) throws Exception {

        // test old constructor with no permission
        try {
            LoginContext lc1 = new LoginContext
                        ("module1",
                        s,
                        ch);
            throw new RuntimeException("Test 1 Failed");
        } catch (SecurityException se) {
            // test passed
        }
        System.out.println("Test 1 Succeeded");

        // test new constructor (null config) with no permission
        try {
            LoginContext lc2 = new LoginContext
                        ("module1",
                        s,
                        ch,
                        null);
            throw new RuntimeException("Test 2 Failed");
        } catch (SecurityException se) {
            // test passed
        }
        System.out.println("Test 2 Succeeded");

        // test new constructor (config) - no permission needed
        // (and none configured)
        LoginContext lc3 = new LoginContext
                        ("module1",
                        s,
                        ch,
                        c);
        System.out.println("Test 3 Succeeded");

        // test old constructor with no permission for other
        try {
            LoginContext lc4 = new LoginContext
                        ("goToOther",
                        s,
                        ch);
            throw new RuntimeException("Test 4 Failed");
        } catch (SecurityException se) {
            // test passed
        }
        System.out.println("Test 4 Succeeded");

        // test new constructor with no permission for other
        try {
            LoginContext lc5 = new LoginContext
                        ("goToOther",
                        s,
                        ch,
                        null);
            throw new RuntimeException("Test 5 Failed");
        } catch (SecurityException se) {
            // test passed
        }
        System.out.println("Test 5 Succeeded");
    }

    private static class MyConfig extends Configuration {
        public MyConfig() { }
        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            java.util.HashMap map = new java.util.HashMap();
            AppConfigurationEntry[] entries = new AppConfigurationEntry[1];

            if (name.equals("module1")) {
                AppConfigurationEntry entry = new AppConfigurationEntry
                        ("ConfigConstructor$MyModule1",
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        map);
                entries[0] = entry;
            } else {
                entries = null;
            }
            return entries;
        }
        public void refresh() { }
    }
}
