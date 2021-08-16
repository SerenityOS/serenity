/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
   @bug 6192331 6348207 8202076
   @summary Check if File.exists()/length() works correctly on Windows
            special files hiberfil.sys and pagefile.sys
 */

import java.io.File;
public class WinSpecialFiles {
    public static void main(String[] args) throws Exception {
        String osName = System.getProperty("os.name");
        if (!osName.startsWith("Windows")) {
            return;
        }
        File root = new File("C:\\");
        File[] dir = root.listFiles();
        for (int i = 0; i < dir.length; i++) {
            if (!dir[i].exists()) {
                throw new Exception("exists() returns false for <"
                                    + dir[i].getPath() + ">");
            }
            String name = dir[i].getPath().toLowerCase();
            if (name.indexOf("pagefile.sys") != -1 ||
                name.indexOf("hiberfil.sys") != -1) {
                if (dir[i].length() == 0) {
                    throw new Exception("Size of existing file \""
                                        + dir[i].getPath()
                                        + "\" is ZERO");
                }
            }
        }
    }
}
