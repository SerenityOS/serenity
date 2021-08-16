/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8139169 8158390
 * @summary verifies if TextArea gets input twice due to Apple's Screen Menubar
 * @requires (os.family=="mac")
 * @library ../../regtesthelpers
 * @build Util
 * @run main ScreenMenuBarInputTwice
 */

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import static java.awt.event.KeyEvent.VK_COMMA;
import static java.awt.event.KeyEvent.VK_META;
import static java.awt.event.KeyEvent.VK_SHIFT;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

public class ScreenMenuBarInputTwice {

    public static final String TEST_STRING = "Check string";

    private static Robot robot;
    private static JFrame frame;
    private static JPanel content;
    private static JTextArea textArea;
    private static JMenuBar menuBar;
    private static JMenu menu;
    private static JMenuItem menuItem;

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        robot.setAutoDelay(200);
        robot.setAutoWaitForIdle(true);
        createUIWithSeperateMenuBar();
        robot.waitForIdle();
        robot.delay(500);
        shortcutTestCase();
        robot.waitForIdle();
        robot.delay(250);
        cleanUp();
        robot.waitForIdle();
        robot.delay(250);
        createUIWithIntegratedMenuBar();
        robot.waitForIdle();
        robot.delay(500);
        menuTestCase();
        robot.waitForIdle();
        robot.delay(250);
        cleanUp();
    }

    private static void createUIWithSeperateMenuBar() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                System.setProperty(
                        "com.apple.mrj.application.apple.menu.about.name",
                        "A test frame");
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                frame = new JFrame("Text input twice check");
                content = new JPanel(new BorderLayout());
                textArea = new JTextArea();
                content.add(new JScrollPane(textArea,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                        BorderLayout.CENTER);
                menuBar = new JMenuBar();
                frame.setJMenuBar(menuBar);
                Action a = new AbstractAction("Insert some text") {
                    @Override
                    public void actionPerformed(ActionEvent arg0) {
                        try {

                            textArea.getDocument()
                                    .insertString(0, TEST_STRING, null);
                        } catch (BadLocationException e) {
                            frame.dispose();
                            throw new RuntimeException("Bad location: ", e);
                        }
                    }
                };
                KeyStroke keyStroke = KeyStroke.getKeyStroke(
                        "meta shift COMMA");
                a.putValue(Action.ACCELERATOR_KEY, keyStroke);
                textArea.getInputMap().put(keyStroke, "myAction");
                textArea.getActionMap().put("myAction", a);
                menu = new JMenu("The Menu");
                menuItem = new JMenuItem(a);
                menuItem.setAccelerator((KeyStroke) a.getValue(
                        Action.ACCELERATOR_KEY));
                menu.add(menuItem);
                menuBar.add(menu);
                frame.getContentPane().add(content);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setLocationRelativeTo(null);
                frame.setSize(500, 500);
                frame.setVisible(true);
                frame.toFront();
            }
        });
    }

    private static void createUIWithIntegratedMenuBar() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                System.setProperty(
                        "com.apple.mrj.application.apple.menu.about.name",
                        "A test frame");
                System.setProperty("apple.laf.useScreenMenuBar", "false");
                frame = new JFrame("Text input twice check");
                content = new JPanel(new BorderLayout());
                textArea = new JTextArea();
                content.add(new JScrollPane(textArea,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                        BorderLayout.CENTER);
                menuBar = new JMenuBar();
                frame.setJMenuBar(menuBar);
                Action a = new AbstractAction("Insert some text") {
                    @Override
                    public void actionPerformed(ActionEvent arg0) {
                        try {

                            textArea.getDocument()
                                    .insertString(0, TEST_STRING, null);
                        } catch (BadLocationException e) {
                            frame.dispose();
                            throw new RuntimeException("Bad location: ", e);
                        }
                    }
                };
                KeyStroke keyStroke = KeyStroke.getKeyStroke(
                        "meta shift COMMA");
                a.putValue(Action.ACCELERATOR_KEY, keyStroke);
                textArea.getInputMap().put(keyStroke, "myAction");
                textArea.getActionMap().put("myAction", a);
                menu = new JMenu("The Menu");
                menuItem = new JMenuItem(a);
                menuItem.setAccelerator((KeyStroke) a.getValue(
                        Action.ACCELERATOR_KEY));
                menu.add(menuItem);
                menuBar.add(menu);
                frame.getContentPane().add(content);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(500, 500);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                frame.toFront();
            }
        });
    }

    private static void shortcutTestCase() throws Exception {
        robot.keyPress(KeyEvent.VK_META);
        robot.keyPress(KeyEvent.VK_SHIFT);
        robot.keyPress(KeyEvent.VK_COMMA);
        robot.keyRelease(VK_COMMA);
        robot.keyRelease(VK_SHIFT);
        robot.keyRelease(VK_META);
        checkText(textArea.getText());
    }

    private static void menuTestCase() throws Exception {
        Point mousePoint;
        mousePoint = Util.getCenterPoint(menu);
        robot.mouseMove(mousePoint.x, mousePoint.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        mousePoint = Util.getCenterPoint(menuItem);
        robot.mouseMove(mousePoint.x, mousePoint.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        checkText(textArea.getText());
    }

    private static void checkText(String text) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                if (TEST_STRING.equals(text)) {
                    textArea.setText("");
                } else {
                    frame.dispose();
                    throw new RuntimeException("Failed. "
                            + " Menu item shortcut invoked twice");
                }
            }
        });
    }

    private static void cleanUp() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame.dispose();
            }
        });
    }
}
