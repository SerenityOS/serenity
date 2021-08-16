/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @key headful
  @bug 8154431
  @summary Allow source and target based validation for the focus transfer
           between two JComponents.
  @run main VerifyTargetTest
*/

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class VerifyTargetTest extends InputVerifier implements FocusListener {
    static boolean success;
    private static JFrame frame;
    private static JTextField field2;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> setup());
        try {
            Robot robot = new Robot();
            robot.waitForIdle();
            robot.delay(200);
            KeyboardFocusManager.getCurrentKeyboardFocusManager()
                                                          .focusNextComponent();
            robot.waitForIdle();
            robot.delay(200);
            if (!success) {
                throw new RuntimeException("Failed");
            } else {
                System.out.println("ok");
            }
        } finally {
            SwingUtilities.invokeLater(() -> frame.dispose());
        }
    }

    static void setup() {
        frame = new JFrame();
        JTextField field1 = new JTextField("Input 1");
        VerifyTargetTest test = new VerifyTargetTest();
        field1.setInputVerifier(test);
        field1.addFocusListener(test);
        frame.getContentPane().add(field1, BorderLayout.NORTH);
        field2 = new JTextField("Input 2");
        frame.getContentPane().add(field2, BorderLayout.SOUTH);
        frame.pack();
        frame.setVisible(true);
    }

    @Override
    public boolean verify(JComponent input) {
        return true;
    }

    @Override
    public boolean verifyTarget(JComponent input) {
        success = input == field2;
        return false;
    }

    @Override
    public void focusGained(FocusEvent e) {}

    @Override
    public void focusLost(FocusEvent e) {
        success = false;
    }
}
