/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7138665
 * @summary JOptionPane.getValue() unexpected change between JRE 1.6 and JRE 1.7
 * @author Pavel Porvatov
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class bug7138665 {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane pane = new JOptionPane("Enter value", JOptionPane.QUESTION_MESSAGE,
                        JOptionPane.OK_CANCEL_OPTION, null, null, null);
                pane.setWantsInput(true);

                JDialog dialog = pane.createDialog(null, "My Dialog");
                dialog.setVisible(true);

                Object result = pane.getValue();

                if (result == null || ((Integer) result).intValue() != JOptionPane.OK_OPTION) {
                    throw new RuntimeException("Invalid result: " + result);
                }

                System.out.println("Test bug7138665 passed");
            }
        });

        Robot robot = new Robot();

        robot.waitForIdle();

        robot.setAutoDelay(100);
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);

        robot.waitForIdle();
    }
}
