/*
 * Copyright (c) 1999, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @summary To check proper WINDOW_EVENTS are triggered when JFrame gains or losses the focus
 * @author Jitender(jitender.singh@eng.sun.com) area=AWT
 * @author yan
 * @library /lib/client
 * @build ExtendedRobot
 * @run main ActiveSwingWindowTest
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;


public class ActiveSwingWindowTest {

    private JFrame frame, frame2;
    private JButton button, button2;
    private JTextField textField, textField2;
    private int eventType, eventType1;
    private ExtendedRobot robot;
    private Object lock1 = new Object();
    private Object lock2 = new Object();
    private Object lock3 = new Object();
    private boolean passed = true;
    private int delay = 150;

    public static void main(String[] args) {
        ActiveSwingWindowTest test = new ActiveSwingWindowTest();
        test.doTest();
    }

    public ActiveSwingWindowTest() {
        try{
            EventQueue.invokeAndWait( () -> {
                    initializeGUI();
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Interrupted or unexpected Exception occured");
        }
    }

    private void initializeGUI() {
        frame = new JFrame();
        frame.setLayout(new FlowLayout());

        frame.setLocation(5, 20);
        frame.setSize(200, 200);
        frame.setUndecorated(true);
        frame.addWindowFocusListener(new WindowFocusListener() {
            public void windowGainedFocus(WindowEvent event) {
                System.out.println("Frame Focus gained");
                synchronized (lock3) {
                    try {
                        lock3.notifyAll();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            public void windowLostFocus(WindowEvent event) {
                    System.out.println("Frame Focus lost");
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                eventType = WindowEvent.WINDOW_ACTIVATED;
                System.out.println("Undecorated Frame is activated\n");
                synchronized (lock1) {
                    try {
                        lock1.notifyAll();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            public void windowDeactivated(WindowEvent e) {
                eventType = WindowEvent.WINDOW_DEACTIVATED;
                System.out.println("Undecorated Frame got Deactivated\n");
                synchronized (lock2) {
                    try {
                            lock2.notifyAll();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        textField = new JTextField("TextField");
        button = new JButton("Click me");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                textField.setText("Focus gained");
            }
        });

        frame.setBackground(Color.green);
        frame.add(button);
        frame.add(textField);
        frame.setVisible(true);

        frame2 = new JFrame();
        frame2.setLayout(new FlowLayout());
        frame2.setLocation(5, 250);
        frame2.setSize(200, 200);
        frame2.setBackground(Color.green);
        button2 = new JButton("Click me");
        textField2 = new JTextField("TextField");
        button2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                textField2.setText("Got the focus");
            }
        });

        frame2.add(button2, BorderLayout.SOUTH);
        frame2.add(textField2, BorderLayout.NORTH);
        frame2.setVisible(true);

        frame.toFront();
    }

    public void doTest() {
        try {
            robot = new ExtendedRobot();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot create robot");
        }

        robot.waitForIdle(5*delay);
        robot.mouseMove(button.getLocationOnScreen().x + button.getSize().width / 2,
                        button.getLocationOnScreen().y + button.getSize().height / 2);
        robot.waitForIdle(delay);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.waitForIdle(delay);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        if (eventType != WindowEvent.WINDOW_ACTIVATED) {
            synchronized (lock1) {
                try {
                    lock1.wait(delay * 10);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (eventType != WindowEvent.WINDOW_ACTIVATED) {
            passed = false;
            System.err.println("WINDOW_ACTIVATED event did not occur when the " +
                    "undecorated frame is activated!");
        }

        eventType1 = -1;
        eventType = -1;

        robot.mouseMove(button2.getLocationOnScreen().x + button2.getSize().width / 2,
                        button2.getLocationOnScreen().y + button2.getSize().height / 2);
        robot.waitForIdle(delay);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.waitForIdle(delay);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        if (eventType != WindowEvent.WINDOW_DEACTIVATED) {
            synchronized (lock2) {
                try {
                    lock2.wait(delay * 10);
                } catch (Exception e) {
                }
            }
        }
        if (eventType != WindowEvent.WINDOW_DEACTIVATED) {
            passed = false;
            System.err.println("FAIL: WINDOW_DEACTIVATED event did not occur for the " +
                    "undecorated frame when another frame gains focus!");
        }
        if (frame.hasFocus()) {
            passed = false;
            System.err.println("FAIL: The undecorated frame has focus even when " +
                        "another frame is clicked!");
        }

        if (!passed) {
            //captureScreenAndSave();
            System.err.println("Test failed!");
            throw new RuntimeException("Test failed.");
        } else {
            System.out.println("Test passed");
        }
    }
}
