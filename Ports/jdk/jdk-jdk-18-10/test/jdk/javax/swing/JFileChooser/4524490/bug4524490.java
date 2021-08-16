/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4524490
 * @summary Tests if in JFileChooser, ALT+L does not bring focus to 'Files' selection list in Motif LAF
 * @author Konstantin Eremin
 * @library ../../regtesthelpers
 * @library /test/lib
 * @build Util jdk.test.lib.Platform
 * @run main bug4524490
 */
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import javax.swing.*;

import jdk.test.lib.Platform;

public class bug4524490 {

    private static JFileChooser fileChooser;

    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        robot.setAutoDelay(50);

        UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                fileChooser = new JFileChooser();
                fileChooser.showOpenDialog(null);
            }
        });

        robot.waitForIdle();

        if (Platform.isOSX()) {
            Util.hitKeys(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_L);
        } else {
            Util.hitKeys(robot, KeyEvent.VK_ALT, KeyEvent.VK_L);
        }
        checkFocus();
    }

    private static void checkFocus() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                JList list = (JList) Util.findSubComponent(fileChooser, "javax.swing.JList");
                System.out.println("list focus: " + list.isFocusOwner());
                if (!list.isFocusOwner()) {
                    throw new RuntimeException("Focus is not transfered to the Folders list.");
                }
            }
        });
    }
}
