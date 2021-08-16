/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * A simple class to create our erring Jar with a very long Main-Class
 * attribute in the manifest.
 */
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
public class ZipMeUp {

    static final CRC32 crc = new CRC32();

    private static String SOME_KLASS = ".Some";

    static byte[] getManifestAsBytes(int nchars) throws IOException {
        crc.reset();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CheckedOutputStream cos = new CheckedOutputStream(baos, crc);
        PrintStream ps = new PrintStream(cos);
        ps.println("Manifest-Version: 1.0");
        ps.print("Main-Class: ");
        for (int i = 0 ; i < nchars - SOME_KLASS.length() ; i++) {
            ps.print(i%10);
        }
        ps.println(SOME_KLASS);
        cos.flush();
        cos.close();
        ps.close();
        return baos.toByteArray();
    }

    /**
     * The arguments are: filename_to_create length
     * @param args
     * @throws java.lang.Exception
     */
    public static void main(String...args) throws Exception  {
        FileOutputStream fos = new FileOutputStream(args[0]);
        ZipOutputStream zos = new ZipOutputStream(fos);
        byte[] manifest = getManifestAsBytes(Integer.parseInt(args[1]));
        ZipEntry ze = new ZipEntry("META-INF/MANIFEST.MF");
        ze.setMethod(ZipEntry.STORED);
        ze.setSize(manifest.length);
        ze.setCompressedSize(manifest.length);
        ze.setCrc(crc.getValue());
        ze.setTime(System.currentTimeMillis());
        zos.putNextEntry(ze);
        zos.write(manifest);
        zos.flush();

        // add a zero length class
        ze = new ZipEntry(SOME_KLASS + ".class");
        ze.setMethod(ZipEntry.STORED);
        ze.setSize(0);
        ze.setCompressedSize(0);
        ze.setCrc(0);
        ze.setTime(System.currentTimeMillis());
        zos.putNextEntry(ze);
        zos.flush();
        zos.closeEntry();
        zos.close();
        System.exit(0);
    }
}
