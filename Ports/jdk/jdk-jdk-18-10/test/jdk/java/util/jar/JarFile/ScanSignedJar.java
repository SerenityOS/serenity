/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4953126
 * @summary Check that a signed JAR file containing an unsupported signer info
 *          attribute can be parsed successfully.
 */

import java.io.File;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.jar.*;

public class ScanSignedJar {

    public static void main(String[] args) throws Exception {
        boolean isSigned = false;
        try (JarFile file = new JarFile(new File(System.getProperty("test.src","."),
                 "bogus-signerinfo-attr.jar"))) {
            byte[] buffer = new byte[8192];

            for (Enumeration entries = file.entries(); entries.hasMoreElements();) {
                JarEntry entry = (JarEntry) entries.nextElement();
                try (InputStream jis = file.getInputStream(entry)) {
                    while (jis.read(buffer, 0, buffer.length) != -1) {
                        // read the jar entry
                    }
                }
                if (entry.getCertificates() != null) {
                    isSigned = true;
                }
                System.out.println((isSigned ? "[signed] " : "\t ") +
                    entry.getName());
            }
        }

        if (isSigned) {
            System.out.println("\nJAR file has signed entries");
        } else {
            throw new Exception("Failed to detect that the JAR file is signed");
        }
    }
}
