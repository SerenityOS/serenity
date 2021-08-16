/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8182043
 * @summary Access to Windows Large Icons
 * @run main SystemIconTest
 */
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;
import java.awt.image.MultiResolutionImage;
import java.io.File;

public class SystemIconTest {
    static final FileSystemView fsv = FileSystemView.getFileSystemView();

    public static void main(String[] args) {
        testSystemIcon();
        negativeTests();
    }

    static void testSystemIcon() {
        String os = System.getProperty("os.name");
        if (os.startsWith("Windows")) {
            String windir = System.getenv("windir");
            testSystemIcon(new File(windir), true);
            testSystemIcon(new File(windir + "/explorer.exe"),
                    true);
        } else {
            String homedir = System.getProperty("user.home");
            testSystemIcon(new File(homedir), false);
        }
    }

    static void negativeTests() {
        Icon icon;
        try {
            icon = fsv.getSystemIcon(new File("."), -1, 16);
            throw new RuntimeException("Negative size icon should throw invalid argument exception");
        } catch (IllegalArgumentException iae) {
            // Expected
        }

        icon = fsv.getSystemIcon(new File("thereisdefinitelynosuchfile.why"),
                16, 16);
        if (icon != null) {
            throw new RuntimeException("Icons for files with invalid names should be null");
        }
    }

    static void testSystemIcon(File file, boolean implComplete) {
        int[] sizes = new int[] {16, 32, 48, 64, 128};
        for (int size : sizes) {
            Icon i = fsv.getSystemIcon(file, size, size);

            if (i == null) {
                throw new RuntimeException(file.getAbsolutePath() + " icon is null");
            }

            if (!(i instanceof ImageIcon)) {
                // Default UI resource icon returned - it is not covered
                // by new implementation so we can not test it
                continue;
            }

            ImageIcon icon = (ImageIcon) i;
            //Enable below to see the icon
            //JLabel label = new JLabel(icon);
            //JOptionPane.showMessageDialog(null, label);

            if (implComplete && icon.getIconWidth() != size) {
                throw new RuntimeException("Wrong icon size " +
                        icon.getIconWidth() + " when requested " + size +
                         " for file " + file.getAbsolutePath());
            }

            if (icon.getImage() instanceof MultiResolutionImage) {
                MultiResolutionImage mri = (MultiResolutionImage) icon.getImage();
                if (mri.getResolutionVariant(size, size) == null) {
                    throw new RuntimeException("There is no suitable variant for the size "
                            + size + " in the multi resolution icon " + file.getAbsolutePath());
                }
            } else {
                if (implComplete) {
                    throw new RuntimeException("icon is supposed to be" +
                            " multi-resolution but it is not for " + file.getAbsolutePath());
                }
            }
        }
    }
}
