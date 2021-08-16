/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 8041694
 * @summary JFileChooser removes trailing spaces in the selected directory name
 * @author Anton Litvinov
 * @library /test/lib
 * @build jdk.test.lib.Platform
 * @run main bug8041694
 */

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.metal.MetalLookAndFeel;

import jdk.test.lib.Platform;

public class bug8041694 {
    private static volatile File dir1;
    private static File dir2;
    private static volatile File selectedDir;

    private static void runTest() {
        try {
            // Set Metal L&F to make the test compatible with OS X.
            UIManager.setLookAndFeel(new MetalLookAndFeel());
            Robot robot = new Robot();
            robot.setAutoDelay(100);

            dir1 = Files.createTempDirectory("bug8041694").toFile();
            if (Platform.isWindows()) {
                dir2 = new File(String.format(
                    "\\\\?\\%s\\d ", dir1.getAbsolutePath().replace('/', '\\')));
            } else {
                dir2 = new File(dir1.getAbsolutePath() + File.separator + "d ");
            }
            dir2.mkdir();

            final CountDownLatch fChooserClosedSignal = new CountDownLatch(1);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        JFileChooser fChooser = new JFileChooser(dir1);
                        fChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        if (fChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            selectedDir = fChooser.getSelectedFile();
                        }
                    } finally {
                        fChooserClosedSignal.countDown();
                    }
                }
            });

            robot.delay(1000);
            robot.waitForIdle();
            robot.keyPress(KeyEvent.VK_D);
            robot.keyRelease(KeyEvent.VK_D);
            robot.waitForIdle();
            robot.keyPress(KeyEvent.VK_SPACE);
            robot.keyRelease(KeyEvent.VK_SPACE);
            robot.waitForIdle();
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.keyRelease(KeyEvent.VK_ENTER);
            robot.waitForIdle();

            fChooserClosedSignal.await();
            if (selectedDir == null) {
                throw new RuntimeException("No directory was selected in JFileChooser.");
            }
            System.out.println(String.format(
                "The selected directory is '%s'.", selectedDir.getAbsolutePath()));
            if (selectedDir.getName().equals("d")) {
                throw new RuntimeException(
                    "JFileChooser removed trailing spaces in the selected directory name. " +
                    "Expected 'd ' got '" + selectedDir.getName() + "'.");
            } else if (!selectedDir.getName().equals("d ")) {
                throw new RuntimeException("The selected directory name is not "
                    + "the expected 'd ' but '" + selectedDir.getName() + "'.");
            }
        } catch (UnsupportedLookAndFeelException | AWTException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (dir2 != null) {
                dir2.delete();
            }
            if (dir1 != null) {
                dir1.delete();
            }
        }
    }

    public static void main(String[] args) {
        runTest();
    }
}
