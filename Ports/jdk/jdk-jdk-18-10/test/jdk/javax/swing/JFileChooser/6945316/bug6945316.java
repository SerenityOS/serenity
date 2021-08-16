/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
   @bug 6945316
   @summary The Win32ShellFolderManager2.isFileSystemRoot can throw NPE
   @author Pavel Porvatov
   @modules java.desktop/sun.awt
            java.desktop/sun.awt.shell
   @run main bug6945316
*/

import sun.awt.OSInfo;
import sun.awt.shell.ShellFolder;

import java.awt.*;
import java.io.File;
import java.util.concurrent.CountDownLatch;

public class bug6945316 {
    public static void main(String[] args) throws Exception {
        if (OSInfo.getOSType() != OSInfo.OSType.WINDOWS) {
            System.out.println("The test is suitable only for Windows OS. Skipped.");

            return;
        }

        // Init toolkit because it shouldn't be interrupted while initialization
        Toolkit.getDefaultToolkit();

        // Init the sun.awt.shell.Win32ShellFolderManager2.drives field
        ShellFolder.get("fileChooserComboBoxFolders");

        // To get NPE the path must obey the following rules:
        // path.length() == 3 && path.charAt(1) == ':'
        final File tempFile = new File("c:\\");

        for (int i = 0; i < 10000; i++) {
            final CountDownLatch countDownLatch = new CountDownLatch(1);

            final Thread thread = new Thread() {
                public void run() {
                    countDownLatch.countDown();

                    ShellFolder.isFileSystemRoot(tempFile);
                }
            };

            thread.start();

            countDownLatch.await();

            thread.interrupt();
        }
    }
}
