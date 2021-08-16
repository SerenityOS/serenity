/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
   @bug 8017487 8167988
   @summary filechooser in Windows-Libraries folder: columns are mixed up
   @author Semyon Sadetsky
   @modules java.desktop/sun.awt.shell
   @library /test/lib
   @build jdk.test.lib.OSVersion jdk.test.lib.Platform
   @run main bug8017487
  */


import jdk.test.lib.Platform;
import jdk.test.lib.OSVersion;

import sun.awt.shell.ShellFolder;
import sun.awt.shell.ShellFolderColumnInfo;
import javax.swing.filechooser.FileSystemView;
import java.io.File;

public class bug8017487
{
    public static void main(String[] p_args) throws Exception {
        if (Platform.isWindows() &&
                OSVersion.current().compareTo(OSVersion.WINDOWS_VISTA) > 0 ) {
            test();
            System.out.println("ok");
        }
    }

    private static void test() throws Exception {
        FileSystemView fsv = FileSystemView.getFileSystemView();
        File def = new File(fsv.getDefaultDirectory().getAbsolutePath());
        ShellFolderColumnInfo[] defColumns =
                ShellFolder.getShellFolder(def).getFolderColumns();

        File[] files = fsv.getHomeDirectory().listFiles();
        for (File file : files) {
            if( "Libraries".equals(ShellFolder.getShellFolder( file ).getDisplayName())) {
                File[] libs = file.listFiles();
                for (File lib : libs) {
                    ShellFolder libFolder =
                            ShellFolder.getShellFolder(lib);
                    if( "Library".equals(libFolder.getFolderType() ) ) {
                        ShellFolderColumnInfo[] folderColumns =
                                libFolder.getFolderColumns();

                        for (int i = 0; i < defColumns.length; i++) {
                            if (!defColumns[i].getTitle()
                                    .equals(folderColumns[i].getTitle()))
                                throw new RuntimeException("Columnn " +
                                        folderColumns[i].getTitle() +
                                        " doesn't match " +
                                        defColumns[i].getTitle());
                        }
                    }
                }
            }
        }
    }

}
