/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6765491 8194486
 * @summary Krb5LoginModule a little too restrictive, and the doc is not clear.
 * @library /test/lib
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts LoginModuleOptions
 */
import com.sun.security.auth.module.Krb5LoginModule;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

public class LoginModuleOptions {

    private static final String NAME = "javax.security.auth.login.name";
    private static final String PWD = "javax.security.auth.login.password";

    public static void main(String[] args) throws Exception {
        OneKDC kdc = new OneKDC(null);
        kdc.addPrincipal("foo", "bar".toCharArray());
        kdc.writeKtab(OneKDC.KTAB); // rewrite to add foo

        // All 4 works: keytab, shared state, callback, cache
        login(null, "useKeyTab", "true", "principal", "dummy");
        login(null, "tryFirstPass", "true", NAME, OneKDC.USER,
                PWD, OneKDC.PASS);
        System.setProperty("test.kdc.save.ccache", "krbcc");
        login(new MyCallback(OneKDC.USER, OneKDC.PASS));    // save the cache
        System.clearProperty("test.kdc.save.ccache");
        login(null, "useTicketCache", "true", "ticketCache", "krbcc");

        // Fallbacks
        // 1. ccache -> keytab
        login(null, "useTicketCache", "true", "ticketCache", "krbcc_non_exists",
                "useKeyTab", "true", "principal", "dummy");

        // 2. keytab -> shared
        login(null, "useKeyTab", "true", "principal", "dummy",
                "keyTab", "ktab_non_exist",
                "tryFirstPass", "true", NAME, OneKDC.USER, PWD, OneKDC.PASS);

        // 3. shared -> callback
        // 3.1. useFirstPass, no callback
        boolean failed = false;
        try {
            login(new MyCallback(OneKDC.USER, OneKDC.PASS),
                    "useFirstPass", "true",
                    NAME, OneKDC.USER, PWD, "haha".toCharArray());
        } catch (Exception e) {
            failed = true;
        }
        if (!failed) {
            throw new Exception("useFirstPass should not fallback to callback");
        }
        // 3.2. tryFirstPass, has callback
        login(new MyCallback(OneKDC.USER, OneKDC.PASS),
                "tryFirstPass", "true",
                NAME, OneKDC.USER, PWD, "haha".toCharArray());

        // Preferences of type
        // 1. ccache preferred to keytab
        login(new MyCallback("foo", null),
                "useTicketCache", "true", "ticketCache", "krbcc",
                "useKeyTab", "true");
        // 2. keytab preferred to shared. This test case is not exactly correct,
        // because principal=dummy would shadow the PWD setting in the shared
        // state. So by only looking at the final authentication user name
        // (which is how this program does), there's no way to tell if keyTab
        // is picked first, or shared is tried first but fallback to keytab.
        login(null, "useKeyTab", "true", "principal", "dummy",
                "tryFirstPass", "true", NAME, "foo", PWD, "bar".toCharArray());
        // 3. shared preferred to callback
        login(new MyCallback("foo", "bar".toCharArray()),
                "tryFirstPass", "true", NAME, OneKDC.USER, PWD, OneKDC.PASS);

        // Preferences of username
        // 1. principal preferred to NAME (NAME can be wrong or missing)
        login(null, "principal", OneKDC.USER,
                "tryFirstPass", "true", NAME, "someone_else", PWD, OneKDC.PASS);
        login(null, "principal", OneKDC.USER,
                "tryFirstPass", "true", PWD, OneKDC.PASS);
        // 2. NAME preferred to callback
        login(new MyCallback("someone_else", OneKDC.PASS),
                "principal", OneKDC.USER);
        // 3. With tryFirstPass, NAME preferred to callback
        login(new MyCallback("someone_else", null),
                "tryFirstPass", "true", NAME, OneKDC.USER, PWD, OneKDC.PASS);
        // 3.1. you must provide a NAME (when there's no principal)
        failed = false;
        try {
            login(new MyCallback(OneKDC.USER, null),
                    "tryFirstPass", "true", PWD, OneKDC.PASS);
        } catch (Exception e) {
            failed = true;
        }
        if (!failed) {
            throw new Exception("useFirstPass must provide a NAME");
        }
        // 3.2 Hybrid, you can use NAME as "", and provide it using callback.
        // I don't think this is designed.
        login(new MyCallback(OneKDC.USER, null),
                "tryFirstPass", "true", NAME, "", PWD, OneKDC.PASS);

        // Test for the bug fix: doNotPrompt can be true if tryFirstPass=true
        login(null, "doNotPrompt", "true", "storeKey", "true",
                "tryFirstPass", "true", NAME, OneKDC.USER, PWD, OneKDC.PASS);
    }

    static void login(CallbackHandler callback, Object... options)
            throws Exception {
        Krb5LoginModule krb5 = new Krb5LoginModule();
        Subject subject = new Subject();
        Map<String, String> map = new HashMap<>();
        Map<String, Object> shared = new HashMap<>();

        int count = options.length / 2;
        for (int i = 0; i < count; i++) {
            String key = (String) options[2 * i];
            Object value = options[2 * i + 1];
            if (key.startsWith("javax")) {
                shared.put(key, value);
            } else {
                map.put(key, (String) value);
            }
        }
        krb5.initialize(subject, callback, shared, map);
        krb5.login();
        krb5.commit();
        if (!subject.getPrincipals().iterator().next()
                .getName().startsWith(OneKDC.USER)) {
            throw new Exception("The authenticated is not " + OneKDC.USER);
        }
    }

    static class MyCallback implements CallbackHandler {

        private String name;
        private char[] password;

        public MyCallback(String name, char[] password) {
            this.name = name;
            this.password = password;
        }

        public void handle(Callback[] callbacks) {
            for (Callback callback : callbacks) {
                System.err.println(callback);
                if (callback instanceof NameCallback) {
                    System.err.println("name is " + name);
                    ((NameCallback) callback).setName(name);
                }
                if (callback instanceof PasswordCallback) {
                    System.err.println("pass is " + new String(password));
                    ((PasswordCallback) callback).setPassword(password);
                }
            }
        }
    }
}
