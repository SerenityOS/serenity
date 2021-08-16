/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyright (c) 2011 IBM Corporation
 */

/*
 * @test
 * @key headful
 * @bug 7049024
 * @summary DnD fails with JTextArea and JTextField
 * @author Sean Chou
 */

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;

public class bug7049024 {
    public static Clipboard clipboard = null;

    public static JTextField textField = null;

    // This button is used to move focus away from textField.
    public static JButton button = null;

    public static JFrame frame = null;

    public static DefaultCaret caret = null;

    public static void main(String[] args) throws Exception {

        Robot robot = new Robot();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame = new JFrame("Test");
                textField = new JTextField("test selection for textfield");
                button = new JButton("To compete the focus");

                frame.setLayout(new FlowLayout());
                frame.getContentPane().add(textField);
                frame.getContentPane().add(button);

                frame.pack();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
            }
        });
        robot.waitForIdle();

        clipboard = textField.getToolkit().getSystemSelection();
        if (null == clipboard) {
            return;
        }

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                textField.requestFocusInWindow();
            }
        });
        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                caret = (DefaultCaret) textField.getCaret();
                caret.setDot(2);
                caret.moveDot(4);
            }
        });
        robot.waitForIdle();

        String oldSelection = (String) clipboard.getData(DataFlavor.stringFlavor);
        System.out.println("oldSelection is " + oldSelection);

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                button.requestFocusInWindow();
            }
        });
        robot.waitForIdle(); // So JTextField loses the focus.

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                caret.setDot(4);
                caret.moveDot(6);
            }
        });
        robot.waitForIdle();

        String newSelection = (String) clipboard.getData(DataFlavor.stringFlavor);
        System.out.println("newSelection is " + newSelection);

        boolean passed = newSelection.equals(oldSelection);

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame.dispose();
            }
        });

        if (!passed) {
            throw new RuntimeException("The test for bug 7049024 failed");
        }
    }
}
