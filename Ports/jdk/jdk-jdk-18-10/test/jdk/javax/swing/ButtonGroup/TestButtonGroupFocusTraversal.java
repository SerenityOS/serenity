/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8249548 8259237
 * @summary Test focus traversal in button group containing JToggleButton
 * and JRadioButton
 * @run main TestButtonGroupFocusTraversal
 */

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class TestButtonGroupFocusTraversal {
    private static JFrame frame;
    private static JTextField textFieldFirst, textFieldLast;
    private static JToggleButton toggleButton1, toggleButton2;
    private static JCheckBox checkBox1, checkBox2;
    private static boolean toggleButtonActionPerformed;
    private static boolean checkboxActionPerformed;
    private static JRadioButton radioButton1, radioButton2;
    private static Robot robot;

    private static void blockTillDisplayed(Component comp) {
        Point p = null;
        while (p == null) {
            try {
                p = comp.getLocationOnScreen();
            } catch (IllegalStateException e) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    private static void createUI() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                textFieldFirst = new JTextField("First");
                textFieldLast = new JTextField("Last");
                toggleButton1 = new JToggleButton("1");
                toggleButton2 = new JToggleButton("2");
                radioButton1 = new JRadioButton("1");
                radioButton2 = new JRadioButton("2");
                checkBox1 = new JCheckBox("1");
                checkBox2 = new JCheckBox("2");

                toggleButton1.setAction(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        toggleButtonActionPerformed = true;
                    }
                });

                toggleButton2.setAction(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        toggleButtonActionPerformed = true;
                    }
                });

                checkBox1.setAction(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        checkboxActionPerformed = true;
                    }
                });

                checkBox2.setAction(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        checkboxActionPerformed = true;
                    }
                });

                ButtonGroup toggleGroup = new ButtonGroup();
                toggleGroup.add(toggleButton1);
                toggleGroup.add(toggleButton2);

                ButtonGroup radioGroup = new ButtonGroup();
                radioGroup.add(radioButton1);
                radioGroup.add(radioButton2);

                ButtonGroup checkboxButtonGroup = new ButtonGroup();
                checkboxButtonGroup.add(checkBox1);
                checkboxButtonGroup.add(checkBox2);

                toggleButton2.setSelected(true);
                radioButton2.setSelected(true);
                checkBox2.setSelected(true);

                frame = new JFrame("Test");
                frame.setLayout(new FlowLayout());

                Container pane = frame.getContentPane();
                pane.add(textFieldFirst);
                pane.add(toggleButton1);
                pane.add(toggleButton2);
                pane.add(radioButton1);
                pane.add(radioButton2);
                pane.add(checkBox1);
                pane.add(checkBox2);
                pane.add(textFieldLast);

                frame.pack();
                frame.setAlwaysOnTop(true);
                frame.setLocationRelativeTo(null);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
            }
        });
    }

    private static void pressKey(int ...keys) {
        int num = keys.length;
        for (int i=0; i<num; i++)
            robot.keyPress(keys[i]);
        for (int i=num; i>0; i--)
            robot.keyRelease(keys[i-1]);

        robot.waitForIdle();
        robot.delay(200);
    }

    private static void checkFocusedComponent (Component component) {
        Component focusedComponent = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (!focusedComponent.equals(component)) {
            System.out.println(component);
            System.out.println(focusedComponent);
            throw new RuntimeException("Wrong Component Selected");
        }
    }

    private static void checkToggleButtonActionPerformed() {
        if (toggleButtonActionPerformed) {
            throw new RuntimeException("Toggle Button Action should not be" +
                    "performed");
        }
    }

    private static void checkCheckboxActionPerformed() {
        if (toggleButtonActionPerformed) {
            throw new RuntimeException("Checkbox Action should not be" +
                    "performed");
        }
    }

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        robot.setAutoDelay(100);

        UIManager.LookAndFeelInfo infos[] = UIManager.getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo info : infos) {
            UIManager.setLookAndFeel(info.getClassName());
            System.out.println(info.getClassName());
            try {
                createUI();

                robot.waitForIdle();
                robot.delay(200);

                blockTillDisplayed(frame);

                SwingUtilities.invokeAndWait(textFieldFirst::requestFocus);

                if (!textFieldFirst.equals(KeyboardFocusManager.getCurrentKeyboardFocusManager()
                        .getFocusOwner())) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    SwingUtilities.invokeAndWait(textFieldFirst::requestFocus);
                }

                robot.waitForIdle();
                robot.delay(200);

                pressKey(KeyEvent.VK_TAB);
                checkFocusedComponent(toggleButton2);

                pressKey(KeyEvent.VK_TAB);
                checkFocusedComponent(radioButton2);

                pressKey(KeyEvent.VK_TAB);
                checkFocusedComponent(checkBox2);

                pressKey(KeyEvent.VK_TAB);
                checkFocusedComponent(textFieldLast);

                pressKey(KeyEvent.VK_SHIFT, KeyEvent.VK_TAB);
                checkFocusedComponent(checkBox2);

                pressKey(KeyEvent.VK_SHIFT, KeyEvent.VK_TAB);
                checkFocusedComponent(radioButton2);

                pressKey(KeyEvent.VK_SHIFT, KeyEvent.VK_TAB);
                checkFocusedComponent(toggleButton2);

                pressKey(KeyEvent.VK_SHIFT, KeyEvent.VK_TAB);
                checkFocusedComponent(textFieldFirst);

                pressKey(KeyEvent.VK_TAB);
                checkFocusedComponent(toggleButton2);

                pressKey(KeyEvent.VK_LEFT);
                checkFocusedComponent(toggleButton1);
                checkToggleButtonActionPerformed();

                pressKey(KeyEvent.VK_RIGHT);
                checkFocusedComponent(toggleButton2);
                checkToggleButtonActionPerformed();

                pressKey(KeyEvent.VK_UP);
                checkFocusedComponent(toggleButton1);
                checkToggleButtonActionPerformed();

                pressKey(KeyEvent.VK_DOWN);
                checkFocusedComponent(toggleButton2);
                checkToggleButtonActionPerformed();

                pressKey(KeyEvent.VK_TAB);
                checkFocusedComponent(radioButton2);

                pressKey(KeyEvent.VK_LEFT);
                checkFocusedComponent(radioButton1);

                pressKey(KeyEvent.VK_RIGHT);
                checkFocusedComponent(radioButton2);

                pressKey(KeyEvent.VK_UP);
                checkFocusedComponent(radioButton1);

                pressKey(KeyEvent.VK_DOWN);
                checkFocusedComponent(radioButton2);

                pressKey(KeyEvent.VK_TAB);
                checkFocusedComponent(checkBox2);

                pressKey(KeyEvent.VK_LEFT);
                checkCheckboxActionPerformed();
                checkFocusedComponent(checkBox1);

                pressKey(KeyEvent.VK_RIGHT);
                checkCheckboxActionPerformed();
                checkFocusedComponent(checkBox2);

                pressKey(KeyEvent.VK_UP);
                checkCheckboxActionPerformed();
                checkFocusedComponent(checkBox1);

                pressKey(KeyEvent.VK_DOWN);
                checkCheckboxActionPerformed();
                checkFocusedComponent(checkBox2);
            } finally {
                if (frame != null) {
                    SwingUtilities.invokeAndWait(frame::dispose);
                }
            }
        }
    }
}
