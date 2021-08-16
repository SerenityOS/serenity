/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
   @key headful
   @bug 8003399
   @summary JFileChooser gives wrong path to selected file when saving to Libraries folder on Windows 7
   @author Semyon Sadetsky
   @library /test/lib
   @build jdk.test.lib.OSVersion jdk.test.lib.Platform
   @run main bug8003399
  */

import jdk.test.lib.Platform;
import jdk.test.lib.OSVersion;

import javax.swing.filechooser.FileSystemView;
import java.io.File;

public class bug8003399 {

    public static void main(String[] args) throws Exception {
        if (Platform.isWindows() &&
                OSVersion.current().compareTo(OSVersion.WINDOWS_VISTA) > 0 ) {
            FileSystemView fsv = FileSystemView.getFileSystemView();
            for (File file : fsv.getFiles(fsv.getHomeDirectory(), false)) {
                if(file.isDirectory()) {
                    for (File file1 : fsv.getFiles(file, false)) {
                        if(file1.isDirectory())
                        {
                            String path = file1.getPath();
                            if(path.startsWith("::{") &&
                                    path.toLowerCase().endsWith(".library-ms")) {
                                System.err.println("file = " + file);
                                System.err.println("file1 = " + file1);
                                System.err.println("path = " + path);
                                throw new RuntimeException("Unconverted library link found");
                            }
                        }
                    }
                }
            }
        }
        System.out.println("ok");
    }
}
