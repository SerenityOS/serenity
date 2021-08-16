/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8244148
 * @summary Test keytool -printcert with -keystore and -trustcacerts options
 * @library /test/lib
 * @library /test/jdk/sun/security/util/module_patch
 * @build java.base/sun.security.util.FilePaths
 * @modules java.base/sun.security.util
 *          java.base/jdk.internal.misc
 * @run main TrustedCert
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.security.KeyStoreUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TrustedCert {

    // The --patch-module must be explicitly specified on the keytool
    // command line because it's in a separate process
    private static final String PATCH_OPTION;

    static {
        String tmp = "";
        for (String a : jdk.internal.misc.VM.getRuntimeArguments()) {
            if (a.startsWith("--patch-module")) {
                tmp = "-J" + a + " ";
                break;
            }
        }
        PATCH_OPTION = tmp;
    }

    static OutputAnalyzer kt(String cmd, String ks) throws Exception {
        return SecurityTools.keytool(cmd + " -keystore " + ks
                + " -storepass changeit")
                .shouldHaveExitValue(0);
    }

    static OutputAnalyzer kt1(String cmd, String ks) throws Exception {
        return SecurityTools.keytool(cmd + " -keystore " + ks
                + " -storepass changeit")
                .shouldNotHaveExitValue(0);
    }

    static OutputAnalyzer patchcmd(String cmd, String options, String ks,
            boolean nonzero) throws Exception {
        if (nonzero) {
            return kt1(PATCH_OPTION + " -" + cmd + " " + options, ks);
        } else {
            return kt(PATCH_OPTION + " -" + cmd + " " + options, ks);
        }
    }

    static void rm(String s) throws IOException {
        System.out.println("---------------------------------------------");
        System.out.println("$ rm " + s);
        Files.deleteIfExists(Paths.get(s));
    }

    private static void cat(String dest, String... src) throws IOException {
        System.out.println("---------------------------------------------");
        System.out.printf("$ cat ");

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        for (String s : src) {
            System.out.printf(s + " ");
            bout.write(Files.readAllBytes(Paths.get(s)));
        }
        Files.write(Paths.get(dest), bout.toByteArray());
        System.out.println("> " + dest);
    }

    public static void main(String[] args) throws Exception {

        // Test -printcert with root CA in local keystore
        kt("-genkeypair -keyalg rsa -keysize 1024 -sigalg SHA1withRSA " +
                "-dname CN=ROOT -ext bc:c", "root.jks");
        kt("-genkeypair -keyalg RSA -dname CN=CA -ext bc:c", "ca.jks");
        kt("-genkeypair -keyalg RSA -dname CN=SERVER", "server.jks");

        kt("-exportcert -rfc -file root.pem", "root.jks");
        kt("-importcert -alias root -file root.pem -noprompt", "ca.jks");
        kt("-importcert -alias root -file root.pem -noprompt", "server.jks");

        kt("-certreq -file ca.req", "ca.jks");
        kt("-gencert -ext BC=0 -rfc -infile ca.req -outfile ca.pem", "root.jks");
        kt("-importcert -file ca.pem", "ca.jks");

        kt("-certreq -file server.req", "server.jks");
        kt("-gencert -ext ku:c=dig,keyEncipherment -rfc -infile server.req " +
                "-outfile server.pem", "ca.jks");
        kt("-importcert -file server.pem", "server.jks");

        cat("fullchain.pem", "server.pem", "root.pem");
        kt("-printcert -file fullchain.pem ", "server.jks")
                .shouldNotMatch("SHA1withRSA signature algorithm.*security risk")
                .shouldMatch("1024-bit RSA key.*security risk");

        rm("ca.jks");
        rm("server.jks");
        rm("mycacerts");

        // Test -printcert with root CA in cacerts keystore
        kt("-genkeypair -keyalg RSA -dname CN=CA -ext bc:c", "ca.jks");
        kt("-genkeypair -keyalg RSA -dname CN=SERVER", "server.jks");

        // import root CA to mycacerts keystore
        KeyStoreUtils.createCacerts("mycacerts", "root.pem");

        kt("-certreq -file ca.req", "ca.jks");
        kt("-gencert -ext BC=0 -rfc -infile ca.req -outfile ca.pem", "root.jks");

        patchcmd("importcert", "-file ca.pem", "ca.jks", true);
        patchcmd("importcert", "-file ca.pem -trustcacerts", "ca.jks", false);

        kt("-certreq -file server.req", "server.jks");
        kt("-gencert -ext ku:c=dig,keyEncipherment -rfc -infile server.req " +
                "-outfile server.pem", "ca.jks");
        kt("-importcert -file server.pem -noprompt", "server.jks");

        cat("fullchain.pem", "server.pem", "root.pem");

        patchcmd("-printcert", "-file fullchain.pem -trustcacerts", "server.jks", false)
                .shouldNotMatch("SHA1withRSA signature algorithm.*security risk")
                .shouldMatch("1024-bit RSA key.*security risk");

        patchcmd("-printcert", "-file fullchain.pem", "server.jks", false)
                .shouldMatch("SHA1withRSA signature algorithm.*security risk")
                .shouldMatch("1024-bit RSA key.*security risk");
    }
}
