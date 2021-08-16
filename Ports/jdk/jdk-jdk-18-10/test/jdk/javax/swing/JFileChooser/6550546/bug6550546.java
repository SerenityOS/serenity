/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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
   @bug 6550546
   @summary Win LAF: JFileChooser -> Look in Drop down should not display any shortcuts created on desktop
   @author Pavel Porvatov
   @modules java.desktop/sun.awt
            java.desktop/sun.awt.shell
   @run main bug6550546
*/

import sun.awt.OSInfo;
import sun.awt.shell.ShellFolder;

import javax.swing.*;
import java.io.File;

public class bug6550546 {
    public static void main(String[] args) throws Exception {
        if (OSInfo.getOSType() != OSInfo.OSType.WINDOWS) {
            System.out.println("The test is suitable only for Windows, skipped.");

            return;
        }

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                File[] files = (File[]) ShellFolder.get("fileChooserComboBoxFolders");

                for (File file : files) {
                    if (file instanceof ShellFolder && ((ShellFolder) file).isLink()) {
                        throw new RuntimeException("Link shouldn't be in FileChooser combobox, " + file.getPath());
                    }
                }
            }
        });
    }
}
