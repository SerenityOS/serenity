/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6284489
 * @summary Confirm that JarEntry.getCertificates identifies signed entries.
 */

import java.net.URL;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.jar.*;

/*
 * Confirm that the signed entries in a JAR file are identified correctly
 * when JarEntry.getCertificates is used to extract the signer's certificates.
 *
 * NOTE: the original problem occurred only when entries were extracted using
 *       the JarInputStream.getNextJarEntry method; it never occurred when
 *       entries were extracted using the JarFile.entries method.
 */
public class ScanSignedJar {

    private static final String JAR_LOCATION =
        "file:" +
        System.getProperty("test.src", ".") +
        System.getProperty("file.separator") +
        "signed.jar";

    public static void main(String[] args) throws Exception {

        System.out.println("Opening " + JAR_LOCATION + "...");
        JarInputStream inStream =
            new JarInputStream(new URL(JAR_LOCATION).openStream(), true);
        JarEntry entry;
        byte[] buffer = new byte[1024];

        while ((entry = inStream.getNextJarEntry()) != null) {

            // need to read the entry's data to see the certs.
            while(inStream.read(buffer) != -1)
                ;

            String name = entry.getName();
            long size = entry.getSize();
            Certificate[] certificates = entry.getCertificates();
            CodeSigner[] signers = entry.getCodeSigners();

            if (signers == null && certificates == null) {
                System.out.println("[unsigned]\t" + name + "\t(" + size +
                    " bytes)");
                if (name.equals("Count.class")) {
                    throw new Exception("Count.class should be signed");
                }
            } else if (signers != null && certificates != null) {
                System.out.println("[" + signers.length +
                    (signers.length == 1 ? " signer" : " signers") + "]\t" +
                    name + "\t(" + size + " bytes)");
            } else {
                System.out.println("[*ERROR*]\t" + name + "\t(" + size +
                    " bytes)");
                throw new Exception("Cannot determine whether the entry is " +
                    "signed or unsigned (signers[] doesn't match certs[]).");
            }
        }
    }
}
