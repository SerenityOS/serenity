/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4845692 8206863
 * @summary JarFile.getInputStream should not throw when jar file is signed
 * @author Martin Buchholz
 */

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

public class SignedJarFileGetInputStream {
    public static void main(String args[]) throws Throwable {
        JarFile jar = new JarFile(
            new File(System.getProperty("test.src", "."), "Signed.jar"));

        for (Enumeration e = jar.entries(); e.hasMoreElements();) {
            JarEntry entry = (JarEntry) e.nextElement();
            InputStream is = jar.getInputStream(new ZipEntry(entry.getName()));
            is.close();
        }

        // read(), available() on closed stream should throw IOException
        InputStream is = jar.getInputStream(new ZipEntry("Test.class"));
        is.close();
        byte[] buffer = new byte[1];

        try {
            is.read();
            throw new AssertionError("Should have thrown IOException");
        } catch (IOException success) {}
        try {
            is.read(buffer);
            throw new AssertionError("Should have thrown IOException");
        } catch (IOException success) {}
        try {
            is.read(buffer, 0, buffer.length);
            throw new AssertionError("Should have thrown IOException");
        } catch (IOException success) {}
        try {
            is.available();
            throw new AssertionError("Should have thrown IOException");
        } catch (IOException success) {}
    }
}
