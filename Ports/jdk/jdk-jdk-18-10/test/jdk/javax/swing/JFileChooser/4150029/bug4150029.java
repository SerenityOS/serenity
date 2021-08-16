/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
   bug 4150029 8006087
   summary BackSpace keyboard button does not lead to parent directory
   author Oleg Mokhovikov
*/

import jdk.test.lib.Platform;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class bug4150029 extends JApplet {
    private boolean res;

    public void init() {
        if (Platform.isOSX()) {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        String tmpDir = System.getProperty("java.io.tmpdir");

        if (tmpDir.length() == 0) {//'java.io.tmpdir' isn't guaranteed to be defined
            tmpDir = System.getProperty("user.home");
        }

        System.out.println("Temp directory: " + tmpDir);

        File testDir = new File(tmpDir, "testDir");

        testDir.mkdir();

        File subDir = new File(testDir, "subDir");

        subDir.mkdir();

        System.out.println("Created directory: " + testDir);
        System.out.println("Created sub-directory: " + subDir);

        JFileChooser fileChooser = new JFileChooser(testDir);

        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        try {
            res = fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION ||
                    testDir.getCanonicalPath().equals(fileChooser.getSelectedFile().getCanonicalPath());
        } catch (IOException e) {
            res = false;

            e.printStackTrace();
        }

        try {
            subDir.delete();
            testDir.delete();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
        if (!res) {
            throw new RuntimeException("BackSpace keyboard button does not lead to parent directory");
        }
    }
}
