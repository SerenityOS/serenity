/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jdk.test.lib.SecurityTools;
import sun.security.krb5.internal.ktab.KeyTab;
import sun.security.krb5.internal.ktab.KeyTabEntry;

/*
 * @test
 * @bug 6950546 8139348
 * @summary "ktab -d name etype" to "ktab -d name [-e etype] [kvno | all | old]"
 * @requires os.family == "windows"
 * @library /test/lib
 * @modules java.security.jgss/sun.security.krb5.internal.ktab
 *          java.security.jgss/sun.security.krb5
 */
public class KtabCheck {

    private static final String KEYTAB = "ktab.tmp";

    public static void main(String[] args) throws Exception {

        Files.deleteIfExists(Path.of(KEYTAB));

        // This test uses a krb5.conf file (onlythree.conf) in which
        // only 3 etypes in the default_tkt_enctypes setting are enabled
        // by default: aes128-cts(17), aes256-cts(18), and aes128-sha2(19).

        ktab("-a me mine");
        check(1,17,1,18,1,19);
        ktab("-a me mine -n 0");
        check(0,17,0,18,0,19);
        ktab("-a me mine -n 1 -append");
        check(0,17,0,18,0,19,1,17,1,18,1,19);
        ktab("-a me mine -append");
        check(0,17,0,18,0,19,1,17,1,18,1,19,2,17,2,18,2,19);
        ktab("-a me mine");
        check(3,17,3,18,3,19);
        ktab("-a me mine -n 4 -append");
        check(3,17,3,18,3,19,4,17,4,18,4,19);
        ktab("-a me mine -n 5 -append");
        check(3,17,3,18,3,19,4,17,4,18,4,19,5,17,5,18,5,19);
        ktab("-a me mine -n 6 -append");
        check(3,17,3,18,3,19,4,17,4,18,4,19,5,17,5,18,5,19,6,17,6,18,6,19);
        ktab("-d me 3");
        check(4,17,4,18,4,19,5,17,5,18,5,19,6,17,6,18,6,19);
        ktab("-d me -e 17 6");
        check(4,17,4,18,4,19,5,17,5,18,5,19,6,18,6,19);
        ktab("-d me -e 19 6");
        check(4,17,4,18,4,19,5,17,5,18,5,19,6,18);
        ktab("-d me -e 17 5");
        check(4,17,4,18,4,19,5,18,5,19,6,18);
        ktab("-d me old");
        check(4,17,5,19,6,18);
        try {
            ktab("-d me old");
            throw new Exception("Should fail");
        } catch (Exception e) {
            // no-op
        }
        check(4,17,5,19,6,18);
        ktab("-d me");
        check();
    }

    static void ktab(String s) throws Exception {
        File conf = new File(System.getProperty("test.src"), "onlythree.conf");
        SecurityTools.ktab("-J-Djava.security.krb5.conf=" + conf
                + " -k " + KEYTAB + " -f " + s).shouldHaveExitValue(0);
    }

    /**
     * Checks if a keytab contains exactly the keys (kvno and etype)
     * @param args kvno etype...
     */
    static void check(int... args) throws Exception {
        System.out.println("Checking " + Arrays.toString(args));
        KeyTab ktab = KeyTab.getInstance(KEYTAB);
        Set<String> expected = new HashSet<>();
        for (int i = 0; i < args.length; i += 2) {
            expected.add(args[i] + ":" + args[i + 1]);
        }
        for (KeyTabEntry e: ktab.getEntries()) {
            // KVNO and etype
            String vne = e.getKey().getKeyVersionNumber() + ":" +
                    e.getKey().getEType();
            if (!expected.contains(vne)) {
                throw new Exception("No " + vne + " in expected");
            }
            expected.remove(vne);
        }
        if (!expected.isEmpty()) {
            throw new Exception("Extra elements in expected");
        }
    }
}
