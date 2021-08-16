/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @library ../../regtesthelpers
 * @build Util
 * @bug 8033699 8154043 8167160 8208640 8226892
 * @summary  Incorrect radio button behavior when pressing tab key
 * @run main bug8033699
 */
import java.awt.KeyboardFocusManager;
import java.awt.Robot;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class bug8033699 {

    private static JFrame mainFrame;
    private static Robot robot;
    private static JButton btnStart;
    private static JButton btnEnd;
    private static JButton btnMiddle;
    private static JRadioButton radioBtn1;
    private static JRadioButton radioBtn2;
    private static JRadioButton radioBtn3;
    private static JRadioButton radioBtnSingle;

    public static void main(String args[]) throws Throwable {
        SwingUtilities.invokeAndWait(() -> {
                changeLAF();
                createAndShowGUI();
        });

        robot = new Robot();
        Thread.sleep(100);

        robot.setAutoDelay(100);

        // tab key test grouped radio button
        runTest1();

        // tab key test non-grouped radio button
        runTest2();

        // shift tab key test grouped and non grouped radio button
        runTest3();

        // left/up key test in grouped radio button
        runTest4();

        // down/right key test in grouped radio button
        runTest5();

        // tab from radio button in group to next component in the middle of button group layout
        runTest6();

        // tab to radio button in group from component in the middle of button group layout
        runTest7();

        // down key circle back to first button in grouped radio button
        runTest8();

        // Verify that ActionListener is called when a RadioButton is selected using arrow key.
        runTest9();

        SwingUtilities.invokeAndWait(() -> mainFrame.dispose());
    }

    private static void changeLAF() {
        String currentLAF = UIManager.getLookAndFeel().toString();
        System.out.println(currentLAF);
        currentLAF = currentLAF.toLowerCase();
        if (currentLAF.contains("nimbus")) {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void createAndShowGUI() {
        mainFrame = new JFrame("Bug 8033699 - 8 Tests for Grouped/Non Group Radio Buttons");
        btnStart = new JButton("Start");
        btnEnd = new JButton("End");
        btnMiddle = new JButton("Middle");

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createTitledBorder("Grouped Radio Buttons"));
        radioBtn1 = new JRadioButton("A");
        radioBtn2 = new JRadioButton("B");
        radioBtn3 = new JRadioButton("C");

        ButtonGroup btnGrp = new ButtonGroup();
        btnGrp.add(radioBtn1);
        btnGrp.add(radioBtn2);
        btnGrp.add(radioBtn3);
        radioBtn1.setSelected(true);

        box.add(radioBtn1);
        box.add(radioBtn2);
        box.add(btnMiddle);
        box.add(radioBtn3);

        radioBtnSingle = new JRadioButton("Not Grouped");
        radioBtnSingle.setSelected(true);

        mainFrame.getContentPane().add(btnStart);
        mainFrame.getContentPane().add(box);
        mainFrame.getContentPane().add(radioBtnSingle);
        mainFrame.getContentPane().add(btnEnd);

        mainFrame.getRootPane().setDefaultButton(btnStart);
        btnStart.requestFocus();

        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLayout(new BoxLayout(mainFrame.getContentPane(), BoxLayout.Y_AXIS));

        mainFrame.setSize(300, 300);
        mainFrame.setLocation(200, 200);
        mainFrame.setVisible(true);
        mainFrame.toFront();
    }

    // Radio button Group as a single component when traversing through tab key
    private static void runTest1() throws Exception {
        hitKey(robot, KeyEvent.VK_TAB);
        hitKey(robot, KeyEvent.VK_TAB);
        hitKey(robot, KeyEvent.VK_TAB);

        SwingUtilities.invokeAndWait(() -> {
            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() != radioBtnSingle) {
                System.out.println("Radio Button Group Go To Next Component through Tab Key failed");
                throw new RuntimeException("Focus is not on Radio Button Single as Expected");
            }
        });
    }

    // Non-Grouped Radio button as a single component when traversing through tab key
    private static void runTest2() throws Exception {
        hitKey(robot, KeyEvent.VK_TAB);
        SwingUtilities.invokeAndWait(() -> {
            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() != btnEnd) {
                System.out.println("Non Grouped Radio Button Go To Next Component through Tab Key failed");
                throw new RuntimeException("Focus is not on Button End as Expected");
            }
        });
    }

    // Non-Grouped Radio button and Group Radio button as a single component when traversing through shift-tab key
    private static void runTest3() throws Exception {
        hitKey(robot, KeyEvent.VK_SHIFT, KeyEvent.VK_TAB);
        hitKey(robot, KeyEvent.VK_SHIFT, KeyEvent.VK_TAB);
        hitKey(robot, KeyEvent.VK_SHIFT, KeyEvent.VK_TAB);
        SwingUtilities.invokeAndWait(() -> {
            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() != radioBtn1) {
                System.out.println("Radio button Group/Non Grouped Radio Button SHIFT-Tab Key Test failed");
                throw new RuntimeException("Focus is not on Radio Button A as Expected");
            }
        });
    }

    // Using arrow key to move focus in radio button group
    private static void runTest4() throws Exception {
        hitKey(robot, KeyEvent.VK_DOWN);
        hitKey(robot, KeyEvent.VK_RIGHT);
        SwingUtilities.invokeAndWait(() -> {
            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() != radioBtn3) {
                System.out.println("Radio button Group UP/LEFT Arrow Key Move Focus Failed");
                throw new RuntimeException("Focus is not on Radio Button C as Expected");
            }
        });
    }

    private static void runTest5() throws Exception {
        hitKey(robot, KeyEvent.VK_UP);
        hitKey(robot, KeyEvent.VK_LEFT);
        SwingUtilities.invokeAndWait(() -> {
            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() != radioBtn1) {
                System.out.println("Radio button Group Left/Up Arrow Key Move Focus Failed");
                throw new RuntimeException("Focus is not on Radio Button A as Expected");
            }
        });
    }

    private static void runTest6() throws Exception {
        hitKey(robot, KeyEvent.VK_UP);
        hitKey(robot, KeyEvent.VK_UP);
        SwingUtilities.invokeAndWait(() -> {
            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() != radioBtn2) {
                System.out.println("Radio button Group Circle Back To First Button Test");
                throw new RuntimeException("Focus is not on Radio Button B as Expected");
            }
        });
    }

    private static void runTest7() throws Exception {
        hitKey(robot, KeyEvent.VK_TAB);
        SwingUtilities.invokeAndWait(() -> {
            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() != btnMiddle) {
                System.out.println("Separate Component added in button group layout");
                throw new RuntimeException("Focus is not on Middle Button as Expected");
            }
        });
    }

    private static void runTest8() throws Exception {
        hitKey(robot, KeyEvent.VK_TAB);
        SwingUtilities.invokeAndWait(() -> {
            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() != radioBtnSingle) {
                System.out.println("Separate Component added in button group layout");
                throw new RuntimeException("Focus is not on Radio Button Single as Expected");
            }
        });
    }

    private static Boolean actRB1 = false;
    private static Boolean actRB2 = false;
    private static Boolean actRB3 = false;

    // JDK-8226892: Verify that ActionListener is called when a RadioButton is selected using arrow key.
    private static void runTest9() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            radioBtn1.setSelected(true);
            radioBtn1.requestFocusInWindow();
        });

        ActionListener actLrRB1 = e -> actRB1 = true;
        ActionListener actLrRB2 = e -> actRB2 = true;
        ActionListener actLrRB3 = e -> actRB3 = true;

        radioBtn1.addActionListener(actLrRB1);
        radioBtn2.addActionListener(actLrRB2);
        radioBtn3.addActionListener(actLrRB3);

        hitKey(robot, KeyEvent.VK_DOWN);
        hitKey(robot, KeyEvent.VK_DOWN);
        hitKey(robot, KeyEvent.VK_DOWN);

        String failMessage = "ActionListener not invoked when selected using arrow key.";
        if (!actRB2) {
            throw new RuntimeException("RadioButton 2: " + failMessage);
        }
        if (!actRB3) {
            throw new RuntimeException("RadioButton 3: " + failMessage);
        }
        if (!actRB1) {
            throw new RuntimeException("RadioButton 1: " + failMessage);
        }

        radioBtn1.removeActionListener(actLrRB1);
        radioBtn2.removeActionListener(actLrRB2);
        radioBtn3.removeActionListener(actLrRB3);
    }

    private static void hitKey(Robot robot, int keycode) {
        robot.keyPress(keycode);
        robot.keyRelease(keycode);
        robot.waitForIdle();
    }

    private static void hitKey(Robot robot, int mode, int keycode) {
        robot.keyPress(mode);
        robot.keyPress(keycode);
        robot.keyRelease(mode);
        robot.keyRelease(keycode);
        robot.waitForIdle();
    }
}
