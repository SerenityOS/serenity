/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4924188 8202816
 * @summary sign a JAR file that has entry names with non-ASCII characters.
 * @modules jdk.jartool/sun.security.tools.jarsigner
 * @run main/othervm JarSigningNonAscii
 */

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.security.cert.Certificate;

public class JarSigningNonAscii {

    private static String jarFile;
    private static String keystore;

    public static void main(String[] args) throws Exception {

        String srcDir = System.getProperty("test.src", ".");
        String destDir = System.getProperty("test.classes", ".");
        String unsignedJar = srcDir + "/JarSigning_RU.jar";
        String signedJar = destDir + "/JarSigning_RU.signed.jar";
        String keystore = srcDir + "/JarSigning.keystore";

        // remove signed jar if it exists
        try {
            File removeMe = new File(signedJar);
            removeMe.delete();
        } catch (Exception e) {
            // ignore
            e.printStackTrace();
        }

        // sign the provided jar file
        String[] jsArgs = {
                        "-keystore", keystore,
                        "-storepass", "bbbbbb",
                        "-signedJar", signedJar,
                        unsignedJar, "b"
                        };
        sun.security.tools.jarsigner.Main.main(jsArgs);

        //  verify the signed jar file

        /**
         * can not do this because JarSigner calls System.exit
         * with an exit code that jtreg does not like
         *
        String[] vArgs = {
                "-verify",
                "-keystore", keystore,
                "-storepass", "bbbbbb",
                "-verbose",
                signedJar
                };
        JarSigner.main(vArgs);
        */

        JarEntry je;
        JarFile jf = new JarFile(signedJar, true);

        Vector entriesVec = new Vector();
        byte[] buffer = new byte[8192];

        Enumeration entries = jf.entries();
        while (entries.hasMoreElements()) {
            je = (JarEntry)entries.nextElement();
            entriesVec.addElement(je);
            InputStream is = jf.getInputStream(je);
            int n;
            while ((n = is.read(buffer, 0, buffer.length)) != -1) {
                // we just read. this will throw a SecurityException
                // if  a signature/digest check fails.
            }
            is.close();
        }
        jf.close();
        Manifest man = jf.getManifest();
        int isSignedCount = 0;
        if (man != null) {
            Enumeration e = entriesVec.elements();
            while (e.hasMoreElements()) {
                je = (JarEntry) e.nextElement();
                String name = je.getName();
                Certificate[] certs = je.getCertificates();
                if ((certs != null) && (certs.length > 0)) {
                    isSignedCount++;
                }
            }
        }

        if (isSignedCount != 4) {
            throw new SecurityException("error signing JAR file");
        }

        System.out.println("jar verified");
    }
}
