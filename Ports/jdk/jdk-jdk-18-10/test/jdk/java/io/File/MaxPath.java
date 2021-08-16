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

/* @test
   @bug 6481955
   @summary Path length less than MAX_PATH (260) works on Windows
 */

import java.io.*;

public class MaxPath {
    public static void main(String[] args) throws Exception {
        String osName = System.getProperty("os.name");
        if (!osName.startsWith("Windows")) {
            return;
        }
        int MAX_PATH = 260;
        String dir = new File(".").getAbsolutePath() + "\\";
        String padding = "1234567890123456789012345678901234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890";
        for (int i = 240 - dir.length(); i < MAX_PATH - dir.length(); i++) {
            String longname = dir + padding.substring(0, i);
            try {
                File f = new File(longname);
                if (f.createNewFile()) {
                    if (!f.exists() || !f.canRead()) {
                        throw new RuntimeException("Failed at length: " + longname.length());
                    }
                    f.delete();
                }
            } catch (IOException e) {
                System.out.println("Failed at length: " + longname.length());
                throw e;
            }
        }
    }
}
