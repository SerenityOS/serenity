/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 8179027 8213116
 * @requires (os.family == "windows")
 * @summary JComboBox too small under Windows LAF
 * @run main WindowsComboBoxSizeTest
 */

import javax.swing.*;
import java.awt.FlowLayout;
import java.awt.Robot;

public class WindowsComboBoxSizeTest {
    private static JTextField textField;
    private static JComboBox<String> comboBox;
    private static JComboBox<String> comboBoxEd;
    private static JFrame frame;

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        SwingUtilities.invokeAndWait(() -> {
            frame = new JFrame();
            frame.getContentPane().setLayout( new FlowLayout() );

            textField = new JTextField("item 1");
            frame.getContentPane().add(textField);

            comboBox = new JComboBox<>(new String[]
                                                {"item 1", "item 2", "item 3"});
            frame.getContentPane().add(comboBox);

            comboBoxEd = new JComboBox<>(new String[]
                                                {"item 1", "item 2", "item 3"});
            comboBoxEd.setEditable( true );
            frame.getContentPane().add(comboBoxEd);

            frame.pack();
            frame.setVisible( true );
        });
        Robot robot = new Robot();
        robot.waitForIdle();
        try {
            test();
        } finally {
            SwingUtilities.invokeLater(frame::dispose);
        }
    }

    private static void test() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            int expected = textField.getSize().height;
            if (comboBox.getSize().height != expected ) {
                throw new RuntimeException(
                        "Wrong non-editable JComboBox height " +
                                              comboBox.getSize().height + " expected " + expected);
            }
            if (comboBoxEd.getSize().height != expected ) {
                throw new RuntimeException(
                        "Wrong editable JComboBox height " +
                                            comboBoxEd.getSize().height + " expected " + expected);
            }
        });
    }
}

