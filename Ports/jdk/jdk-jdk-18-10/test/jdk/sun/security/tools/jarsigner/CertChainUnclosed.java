/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8027991
 * @summary InputStream should be closed in sun.security.tools.jarsigner.Main
 * @modules java.base/sun.security.tools.keytool
 *          jdk.jartool/sun.security.tools.jarsigner
 * @run main/othervm CertChainUnclosed
 */

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;

public class CertChainUnclosed {

    public static void main(String[] args) throws Exception {
        String os = AccessController.doPrivileged(
                    (PrivilegedAction<String>)() -> System.getProperty("os.name"));
        if (!os.toUpperCase(Locale.US).contains("WINDOWS")) {
            System.out.println("Not Windows. Skip test.");
            return;
        }

        kt("-genkeypair -alias a -dname CN=A");
        kt("-exportcert -file a.crt -alias a");
        Files.copy(Paths.get(System.getProperty("test.src"), "AlgOptions.jar"),
                Paths.get("test.jar"));
        sun.security.tools.jarsigner.Main.main(
                "-storepass changeit -keystore jks -certchain a.crt test.jar a"
                        .split(" "));

        // On Windows, if the file is still opened (or not if GC was
        // performed) and the next line would fail
        Files.delete(Paths.get("a.crt"));
    }

    static void kt(String args) throws Exception {
        sun.security.tools.keytool.Main.main(
            ("-keystore jks -storepass changeit -keypass changeit -keyalg rsa "
                + args).split(" "));
    }
}
