/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @test
 * @bug 7050028
 * @summary  ISE "zip file closed" from JarURLConnection.getInputStream on JDK 7 when !useCaches
 * @run main/othervm B7050028
 */

public class B7050028 {
    public static void main(String[] args) throws Exception {
        URLConnection conn = B7050028.class.getResource("B7050028.class").openConnection();
        int len = conn.getContentLength();
        byte[] data = new byte[len];
        InputStream is = conn.getInputStream();
        is.read(data);
        is.close();
        conn.setDefaultUseCaches(false);
        File jar = File.createTempFile("B7050028", ".jar");
        jar.deleteOnExit();
        OutputStream os = new FileOutputStream(jar);
        ZipOutputStream zos = new ZipOutputStream(os);
        ZipEntry ze = new ZipEntry("B7050028.class");
        ze.setMethod(ZipEntry.STORED);
        ze.setSize(len);
        CRC32 crc = new CRC32();
        crc.update(data);
        ze.setCrc(crc.getValue());
        zos.putNextEntry(ze);
        zos.write(data, 0, len);
        zos.closeEntry();
        zos.finish();
        zos.close();
        os.close();
        System.out.println(new URLClassLoader(new URL[] {new URL("jar:" + jar.toURI() + "!/")}, ClassLoader.getSystemClassLoader().getParent()).loadClass(B7050028.class.getName()));
    }
    private B7050028() {}
}
