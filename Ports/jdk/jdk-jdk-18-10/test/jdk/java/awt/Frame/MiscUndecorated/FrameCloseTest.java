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
 * @summary To make sure Undecorated Frame triggers correct windows events while closing
 * @author Jitender(jitender.singh@eng.sun.com) area=AWT*
 * @author yan
 * @library /lib/client
 * @build ExtendedRobot
 * @run main FrameCloseTest
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.JFrame;
import javax.swing.JButton;

public class FrameCloseTest {
    private static int delay = 150;

    private Frame frame, frame2;
    private Component button, dummyButton;
    private int eventType, eventType1, eventType2;
    private ExtendedRobot robot;
    private Object lock1 = new Object();
    private Object lock2 = new Object();
    private Object lock3 = new Object();
    private Object lock4 = new Object();
    private boolean passed = true;

    public static void main(String[] args) {
        FrameCloseTest test = new FrameCloseTest();
        test.doTest(false);
        test.doTest(true);
    }

    private void initializeGUI(boolean swingFrame) {
        frame = swingFrame? new Frame() : new JFrame();
        frame.setLayout(new FlowLayout());

        frame.setLocation(5, 20);
        frame.setSize(200, 200);
        frame.setUndecorated(true);
        frame.addWindowFocusListener(new WindowFocusListener() {
            public void windowGainedFocus(WindowEvent event) {
                System.out.println("Frame Focus gained");
                synchronized (lock4) {
                    try {
                        lock4.notifyAll();
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
                System.out.println("Undecorated Frame is activated");
                synchronized (lock1) {
                    try {
                        lock1.notifyAll();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            public void windowDeactivated(WindowEvent e) {
                eventType1 = WindowEvent.WINDOW_DEACTIVATED;
                System.out.println("Undecorated Frame got Deactivated");
                synchronized (lock2) {
                    try {
                        lock2.notifyAll();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            public void windowClosed(WindowEvent e) {
                eventType2 = WindowEvent.WINDOW_CLOSED;
                System.out.println("Undecorated Frame got closed");
                synchronized (lock3) {
                    try {
                        lock3.notifyAll();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        dummyButton = swingFrame? new JButton("Click me") : new Button("Click me");

        frame.setBackground(Color.green);
        frame.add((button = createButton(swingFrame, "Close me")));
        frame.add(dummyButton);
        frame.setVisible(true);
        frame.toFront();
    }
    private Component createButton(boolean swingControl, String txt) {
        if(swingControl) {
            JButton jbtn = new JButton(txt);
            jbtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    frame.dispose();
                }
            });
            return jbtn;
        }else {
            Button btn = new Button(txt);
            btn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    frame.dispose();
                }
            });
            return btn;
        }
    }

    public void doTest(boolean swingControl) {
        try {
            Toolkit.getDefaultToolkit().getSystemEventQueue().invokeAndWait(new Runnable() {
                public void run() {
                    initializeGUI(swingControl);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Interrupted or unexpected Exception occured");
        }
        try {
            robot = new ExtendedRobot();
            robot.waitForIdle(1000);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot create robot");
        }

        robot.mouseMove(dummyButton.getLocationOnScreen().x + dummyButton.getSize().width / 2,
                        dummyButton.getLocationOnScreen().y + dummyButton.getSize().height / 2);
        robot.waitForIdle(delay);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.waitForIdle(delay);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.waitForIdle(delay);

        eventType1 = -1;
        eventType = -1;
        eventType2 = -1;

        robot.mouseMove(button.getLocationOnScreen().x + button.getSize().width / 2,
                        button.getLocationOnScreen().y + button.getSize().height / 2);
        robot.waitForIdle(delay);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.waitForIdle(delay);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.waitForIdle(delay * 10);

        if (eventType2 != WindowEvent.WINDOW_CLOSED) {
            synchronized (lock3) {
                try {
                    lock3.wait(delay * 10);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (eventType2 != WindowEvent.WINDOW_CLOSED) {
            passed = false;
            System.err.println("WINDOW_CLOSED event did not occur when the " +
                    "undecorated frame is closed!");
        }
        if (eventType == WindowEvent.WINDOW_ACTIVATED) {
            passed = false;
            System.err.println("WINDOW_ACTIVATED event occured when the " +
                    "undecorated frame is closed!");
        }

        if (!passed) {
            System.err.println("Test failed!");
            throw new RuntimeException("Test failed");
        } else {
            System.out.println("Test passed");
        }
    }
}
