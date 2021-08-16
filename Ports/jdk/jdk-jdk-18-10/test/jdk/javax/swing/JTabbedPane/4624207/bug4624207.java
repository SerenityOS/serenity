/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4624207
 * @summary JTabbedPane mnemonics don't work from outside the tabbed pane
 * @author Oleg Mokhovikov
 * @library /test/lib
 * @library ../../regtesthelpers
 * @build Util jdk.test.lib.Platform
 * @run main bug4624207
 */
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;

import jdk.test.lib.Platform;

public class bug4624207 implements ChangeListener, FocusListener {

    private static volatile boolean stateChanged = false;
    private static volatile boolean focusGained = false;
    private static JTextField txtField;
    private static JTabbedPane tab;
    private static Object listener;
    private static JFrame frame;

    public void stateChanged(ChangeEvent e) {
        System.out.println("stateChanged called");
        stateChanged = true;
    }

    public void focusGained(FocusEvent e) {
        System.out.println("focusGained called");
        focusGained = true;
    }

    public void focusLost(FocusEvent e) {
        System.out.println("focusLost called");
        focusGained = false;
    }

    public static void main(String[] args) throws Exception {
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(50);

            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    createAndShowGUI();
                }
            });

            robot.waitForIdle();

            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    txtField.requestFocus();
                }
            });

            robot.waitForIdle();

            if (!focusGained) {
                throw new RuntimeException("Couldn't gain focus for text field");
            }

            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    tab.addChangeListener((ChangeListener) listener);
                    txtField.removeFocusListener((FocusListener) listener);
                }
            });

            robot.waitForIdle();

            if (Platform.isOSX()) {
                Util.hitKeys(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_B);
            } else {
                Util.hitKeys(robot, KeyEvent.VK_ALT, KeyEvent.VK_B);
            }

            robot.waitForIdle();

            if (!stateChanged || tab.getSelectedIndex() != 1) {
                throw new RuntimeException("JTabbedPane mnemonics don't work from outside the tabbed pane");
            }
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() ->  frame.dispose());
        }
    }

    private static void createAndShowGUI() {
        tab = new JTabbedPane();
        tab.add("Tab1", new JButton("Button1"));
        tab.add("Tab2", new JButton("Button2"));
        tab.setMnemonicAt(0, KeyEvent.VK_T);
        tab.setMnemonicAt(1, KeyEvent.VK_B);

        frame = new JFrame();
        frame.getContentPane().add(tab, BorderLayout.CENTER);
        txtField = new JTextField();
        frame.getContentPane().add(txtField, BorderLayout.NORTH);
        listener = new bug4624207();
        txtField.addFocusListener((FocusListener) listener);
        frame.pack();
        frame.setVisible(true);
    }
}
