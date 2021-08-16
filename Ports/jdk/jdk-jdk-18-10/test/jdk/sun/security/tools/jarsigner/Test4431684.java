/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4431684
 * @summary jar signature certificate key usage check incorrect
 */

import java.util.jar.*;
import java.util.*;
import java.io.*;

public class Test4431684 {

    public static void main(String[] args) throws Exception {

        File f = new File(System.getProperty("test.src", "."),
                          "JavaApplication1.jar");
        JarFile jf = new JarFile(f);
        Enumeration entries = jf.entries();
        while (entries.hasMoreElements()) {
            JarEntry je = (JarEntry)entries.nextElement();
            if(je.getName().endsWith("class")) {
                byte[] buffer = new byte[8192];
                InputStream is = jf.getInputStream(je);
                int n;
                while ((n = is.read(buffer, 0, buffer.length)) != -1) {
                }
                is.close();
                if(je.getCodeSigners() == null) {
                    throw new RuntimeException("FAIL: Cannot get code signers");
                }
            }
        }
    }
}
