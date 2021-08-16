/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6868611 8198004
 * @summary FileSystemView throws NullPointerException
 * @author Pavel Porvatov
 * @run main bug6868611
 */

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.nio.file.Files;

public class bug6868611 {
    private static final int COUNT = 1000;
    private static File tempFolder;
    private static File files[] = new File[COUNT];

    public static void main(String[] args) throws Exception {
        int fileCount = 0;
        try {
            tempFolder = Files.createTempDirectory("temp_folder").toFile();

            // Try creating 1000 files
            for (fileCount = 0; fileCount < COUNT; fileCount++) {
                files[fileCount] = new
                        File(tempFolder, "temp" + fileCount + ".txt");
                files[fileCount].createNewFile();
            }

            // Init default FileSystemView
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    FileSystemView.getFileSystemView().
                            getFiles(tempFolder, false);
                }
            });

            for (int i = 0; i < COUNT; i++) {
                Thread thread = new MyThread(tempFolder);

                thread.start();

                Thread.sleep((long) (Math.random() * 100));

                thread.interrupt();
            }
        } finally {
            // Remove created files
            for (int i = 0; i < fileCount; i++) {
                Files.delete(files[i].toPath());
            }
            Files.delete(tempFolder.toPath());
        }
    }

    private static class MyThread extends Thread {
        private final File dir;

        private MyThread(File dir) {
            this.dir = dir;
        }

        public void run() {
            FileSystemView fileSystemView = FileSystemView.getFileSystemView();

            fileSystemView.getFiles(dir, false);
        }
    }
}

