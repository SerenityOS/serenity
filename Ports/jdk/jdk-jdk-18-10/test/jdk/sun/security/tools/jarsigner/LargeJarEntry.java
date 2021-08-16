/*
 * Copyright (c) 2006, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6405538 6474350
 * @summary Make sure jar files with large entries (more than max heap size)
 *    can be signed
 * @modules jdk.jartool/sun.security.tools.jarsigner
 * @run main/othervm -Xmx16m LargeJarEntry
 * @author Sean Mullan
 */

import java.io.File;
import java.io.FileOutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;

public class LargeJarEntry {

    public static void main(String[] args) throws Exception {

        String srcDir = System.getProperty("test.src", ".");
        String keystore = srcDir + "/JarSigning.keystore";
        String jarName = "largeJarEntry.jar";

        // Set java.io.tmpdir to the current working dir (see 6474350)
        System.setProperty("java.io.tmpdir", System.getProperty("user.dir"));

        // first, create jar file with 8M uncompressed entry
        // note, we set the max heap size to 8M in @run tag above
        byte[] bytes = new byte[1000000];
        CRC32 crc = new CRC32();
        for (int i=0; i<8; i++) {
            crc.update(bytes);
        }
        JarEntry je = new JarEntry("large");
        je.setSize(8000000l);
        je.setMethod(JarEntry.STORED);
        je.setCrc(crc.getValue());
        File file = new File(jarName);
        FileOutputStream os = new FileOutputStream(file);
        JarOutputStream jos = new JarOutputStream(os);
        jos.setMethod(JarEntry.STORED);
        jos.putNextEntry(je);
        for (int i=0; i<8; i++) {
            jos.write(bytes, 0, bytes.length);
        }
        jos.close();

        String[] jsArgs = { "-keystore", keystore, "-storepass", "bbbbbb",
                jarName, "b" };
        // now, try to sign it
        try {
            sun.security.tools.jarsigner.Main.main(jsArgs);
        } catch (OutOfMemoryError err) {
            throw new Exception("Test failed with OutOfMemoryError", err);
        } finally {
            // remove jar file
            file.delete();
        }
    }
}
