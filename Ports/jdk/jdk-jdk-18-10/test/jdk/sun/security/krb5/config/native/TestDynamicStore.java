/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8257860
 * @summary SCDynamicStoreConfig works
 * @modules java.security.jgss/sun.security.krb5
 * @library /test/lib
 * @run main/manual/native TestDynamicStore
 * @requires (os.family == "mac")
 */

import jdk.test.lib.Asserts;
import sun.security.krb5.Config;

// =================== Attention ===================
// This test calls a native method implemented in libTestDynamicStore.m
// to modify system-level Kerberos 5 settings stored in the dynamic store.
// It must be launched by a user with enough privilege or with "sudo".
// If launched with sudo, remember to remove the report and working
// directories with sudo as well after executing the test.

public class TestDynamicStore {

    native static int actionInternal(char what, char whom);

    // what: 'a' for add, 'r' for remove
    // whom: 'a' for all, 'r' for realm, 'm' for mapping
    static int action(char what, char whom) throws Exception {
        int out = actionInternal(what, whom);
        System.out.println("Run " + what + whom + " " + out);
        Thread.sleep(1000);   // wait for callback called
        return out;
    }

    public static void main(String[] args) throws Exception {

        System.loadLibrary("TestDynamicStore");

        Config cfg = Config.getInstance();
        if (cfg.exists("libdefaults") || cfg.exists("realms")) {
            System.out.println("Already have krb5 config. Will not touch");
            return;
        }

        try {
            System.out.println("Fill in dynamic store");
            if (action('a', 'a') == 0) {
                throw new Exception("Cannot write native Kerberos settings. " +
                        "Please make sure the test runs with enough privilege.");
            }
            Asserts.assertTrue(Config.getInstance().get("libdefaults", "default_realm").equals("A.COM"));
            Asserts.assertTrue(Config.getInstance().exists("domain_realm"));

            System.out.println("Remove mapping");
            action('r', 'm');
            Asserts.assertTrue(!Config.getInstance().exists("domain_realm"));

            System.out.println("Re-add mapping");
            action('a', 'm');
            Asserts.assertTrue(Config.getInstance().exists("domain_realm"));

            System.out.println("Remove realm info");
            action('r', 'r');
            // Realm info is not watched, so no change detected
            Asserts.assertTrue(Config.getInstance().get("libdefaults", "default_realm").equals("A.COM"));

            System.out.println("Remove mapping");
            action('r', 'm');
            // But mapping is watched, so realm info is not re-read
            Asserts.assertTrue(Config.getInstance().get("libdefaults", "default_realm").equals("B.COM"));
        } finally {
            System.out.println("Remove everything");
            action('r', 'a');
            Asserts.assertTrue(!Config.getInstance().exists("libdefault"));
        }
    }
}
