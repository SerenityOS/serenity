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
   @bug 6688203
   @summary Memory leak and performance problems in the method getFileSystemView of FileSystemView
   @author Pavel Porvatov
   @modules java.desktop/javax.swing.filechooser:open
   @run main bug6688203
*/

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.lang.reflect.Field;

public class bug6688203 {
    public static void main(String[] args) {
        // Create an instance of FileSystemView
        FileSystemView.getFileSystemView();

        int startCount = UIManager.getPropertyChangeListeners().length;

        for (int i = 0; i < 100; i++) {
            FileSystemView.getFileSystemView();
        }

        if (startCount != UIManager.getPropertyChangeListeners().length) {
            throw new RuntimeException("New listeners were added into UIManager");
        }

        FileSystemView fileSystemView = FileSystemView.getFileSystemView();
        File file = new File("Some file");

        for (UIManager.LookAndFeelInfo lafInfo : UIManager.getInstalledLookAndFeels()) {
            try {
                UIManager.setLookAndFeel(lafInfo.getClassName());
            } catch (Exception e) {
                // Ignore such errors
                System.out.println("Cannot set LAF " + lafInfo.getName());

                continue;
            }

            fileSystemView.getSystemDisplayName(file);

            try {
                Field field = FileSystemView.class.getDeclaredField("useSystemExtensionHiding");

                field.setAccessible(true);

                Boolean value = field.getBoolean(fileSystemView);

                if (value != UIManager.getDefaults().getBoolean("FileChooser.useSystemExtensionHiding")) {
                    throw new RuntimeException("Invalid cached value of the FileSystemView.useSystemExtensionHiding field");
                }
            } catch (Exception e) {
                throw new RuntimeException("Cannot read the FileSystemView.useSystemExtensionHiding field", e);
            }
        }
    }
}
