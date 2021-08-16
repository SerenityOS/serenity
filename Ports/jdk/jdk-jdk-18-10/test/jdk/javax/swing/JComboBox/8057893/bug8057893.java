/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.EventQueue;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

/**
 * @test
 * @key headful
 * @bug 8057893
 * @summary JComboBox actionListener never receives "comboBoxEdited"
 *   from getActionCommand
 * @run main bug8057893
 */
public class bug8057893 {

    private static volatile boolean isComboBoxEdited = false;
    private static JFrame frame;

    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        robot.setAutoDelay(100);

        EventQueue.invokeAndWait(() -> {
            frame = new JFrame();
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            JComboBox<String> comboBox = new JComboBox<>(new String[]{"one", "two"});
            comboBox.setEditable(true);
            comboBox.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if ("comboBoxEdited".equals(e.getActionCommand())) {
                        isComboBoxEdited = true;
                    }
                }
            });
            frame.add(comboBox);
            frame.pack();
            frame.setVisible(true);
            frame.setLocationRelativeTo(null);
            comboBox.requestFocusInWindow();
        });

        robot.waitForIdle();
        robot.delay(1000);

        robot.keyPress(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_A);
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
        robot.waitForIdle();

        if (frame != null) EventQueue.invokeAndWait(() -> frame.dispose());
        if(!isComboBoxEdited){
            throw new RuntimeException("ComboBoxEdited event is not fired!");
        }
    }
}
