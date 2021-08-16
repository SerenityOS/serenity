/*
 * Copyright (c) 2002, 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

/**
 * @test
 * @key headful
 * @bug 4515752 4337071
 * @author Mark Davidson
 * @summary Tests the invocation of the default button within the JComboBox.
 */
public class DefaultButtonTest extends JFrame implements ActionListener {

    private static boolean defaultButtonPressed = false;
    private static boolean editChanged = false;

    private static String[] strData =  {
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };

    private static String[] strData2 =  {
        "One", "Two", "Three", "Four", "Five", "Six", "Seven"
    };

    public static void main(String[] args) throws Throwable {
        SwingUtilities.invokeAndWait(new Runnable(){
            public void run() {
                new DefaultButtonTest();
            }
        });
        test();
        System.out.println("Test Passed");
    }

    public DefaultButtonTest() {
        getContentPane().add(new DefaultPanel(this));
        pack();
        setVisible(true);
        setLocationRelativeTo(null);
    }

    public static void test() {
        // Use Robot to automate the test
        Robot robot = null;
        try {
            robot = new Robot();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        robot.setAutoDelay(125);

        for (int i = 0; i < 3; i++) {
            // Test ENTER press on the non editable combo.
            robot.waitForIdle();
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.waitForIdle();
            robot.keyRelease(KeyEvent.VK_ENTER);
            robot.waitForIdle();
            testDefaultButton(true);

            // Test the ENTER press on the editable combo box
            robot.keyPress(KeyEvent.VK_TAB);
            robot.waitForIdle();
            robot.keyRelease(KeyEvent.VK_TAB);
            robot.waitForIdle();
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.waitForIdle();
            robot.keyRelease(KeyEvent.VK_ENTER);
            robot.waitForIdle();
            testDefaultButton(true);

            // Change the value, should generate a change but not a Default Button press.
            robot.waitForIdle();
            robot.keyPress(KeyEvent.VK_D);
            robot.waitForIdle();
            robot.keyRelease(KeyEvent.VK_D);
            robot.waitForIdle();
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.waitForIdle();
            robot.keyRelease(KeyEvent.VK_ENTER);
            robot.waitForIdle();
            testEditChange(true);
            robot.waitForIdle();
            testDefaultButton(true);

            // Change value, changing focus should fire an ActionEvent.
            robot.waitForIdle();
            robot.keyPress(KeyEvent.VK_BACK_SPACE);
            robot.keyRelease(KeyEvent.VK_BACK_SPACE);
            robot.waitForIdle();
            robot.keyPress(KeyEvent.VK_SHIFT);
            robot.keyPress(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_SHIFT);
            robot.waitForIdle();
            testEditChange(true);
            robot.waitForIdle();
            testDefaultButton(false);
        }
    }

    public void actionPerformed(ActionEvent evt) {
        String cmd = evt.getActionCommand();
        System.out.println("ActionEvent: " + cmd);

        if (cmd.equals("OK")) {
            defaultButtonPressed = true;
        }

        if (cmd.equals("comboBoxChanged")) {
            editChanged = true;
        }
    }

    public static void testDefaultButton(boolean flag) {
        if (defaultButtonPressed != flag) {
            new RuntimeException("defaultButtonPressed unexpectedly = " + defaultButtonPressed);
        }
        // reset
        defaultButtonPressed = false;
    }

    public static void testEditChange(boolean flag) {
        if (editChanged != flag) {
            new RuntimeException("editChanged unexpectedly = " + editChanged);
        }
        // reset
        editChanged = false;
    }

    class DefaultPanel extends JPanel {

        public JComboBox combo;
        public JComboBox combo2;

        private JButton okButton = new JButton("OK");
        private JButton cancelButton = new JButton("Cancel");

        public DefaultPanel(JFrame root) {
            setLayout(new BorderLayout());
            add(createPanel(), BorderLayout.NORTH);
            add(createInfoPanel(), BorderLayout.CENTER);
            add(createButtonPanel(root), BorderLayout.SOUTH);
        }

        private JPanel createPanel() {
            combo = new JComboBox(strData);
            combo.addActionListener(DefaultButtonTest.this);
            combo2 = new JComboBox(strData2);
            combo2.setEditable(true);
            combo2.addActionListener(DefaultButtonTest.this);

            JPanel panel = new JPanel();

            panel.add(combo);
            panel.add(combo2);

            return panel;
        }

        private JScrollPane createInfoPanel() {
            StringBuffer txt = new StringBuffer("Test for 4337071:\n");
            txt.append("ENTER pressed in NON-EDITABLE combo box should be passed to the OK button.\n");
            txt.append("For an EDITABLE combo box, the combo box should fire an action event.");
            txt.append("\n\nTest for 4515752:\n");
            txt.append("ENTER on an EDITABLE combo box in which the contents has not changed\n");
            txt.append("should be passed to the default button");

            JTextArea text = new JTextArea(txt.toString());
            text.setEditable(false);

            return new JScrollPane(text);
        }


        private JPanel createButtonPanel(JFrame frame) {
            frame.getRootPane().setDefaultButton(okButton);

            // This is just to check when the OK Button was pressed.
            okButton.addActionListener(DefaultButtonTest.this);

            JPanel panel = new JPanel();
            panel.add(okButton);
            panel.add(cancelButton);
            return panel;
        }
    }

}
