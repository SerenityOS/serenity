/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @bug 6252005 8242174
  @key headful
  @summary Tests that realSync feature works
  @author denis.mikhalkin: area=awt.toolkit
  @modules java.desktop/sun.awt
  @run main/timeout=6000 Test
*/

import java.awt.Frame;
import java.awt.Point;
import java.awt.Robot;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedList;

import javax.swing.*;

/**
 * Tests various problematic areas and how they are fixed using real-sync API:
 * - requesting focus
 * - showing and robot mouse pressing
 * - showing and getting location on screen
 * - showing and typing
 */

public class Test {
    private static boolean doRealSync = true;
    private static boolean someFailed = false;
    private static Robot robot;
    public static void main(String[] args) {
        installListeners();

        try {
            robot = new Robot();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }


        int count = 100;
        String method = null;
        if (args.length != 0) {
            try {
                count = Integer.parseInt(args[0]);
            } catch (NumberFormatException nfe) {
                method = args[0];
                count = 1;
            }
        }
        while (count > 0 && !someFailed) {
            run(method);
            gc();
            count--;
        }

        System.err.println("Total results: " + (someFailed? ("some tests failed (" + count + ")"): "ALL TESTS PASSED!!!"));
    }

    private static void gc() {
        System.gc();
        sleep(50);
        System.gc();
        Thread.yield();
        System.gc();
    }

    private static void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ie) {
        }
    }

    private static java.util.List<Object> events = Collections.synchronizedList(new LinkedList<Object>());

    private static class TestFailureException extends RuntimeException {
    }

    public static void run(String method) {
        Class cl = Test.class;
        for (Method m : cl.getMethods()) {
            if (Modifier.isStatic(m.getModifiers()) && m.getName().startsWith("test") && method == null ||
                (method != null && method.equals(m.getName()))) {
                realSync(null);
                events.clear();
                try {
                    m.invoke(null);
                } catch (TestFailureException e) {
                    // Do nothing
                } catch (Exception e) {
                    fail(e);
                }
                reportErrors(m);
            }
        }
    }

    private static java.util.List<Object> errors = Collections.synchronizedList(new LinkedList<Object>());
    public static void reportErrors(Method m) {
        realSync(null);
        if (errors.size() == 0) {
//             System.err.println("Test passed: " + m.getName());
//             System.err.println("------------------------------------------------------\nEvents for " + m.getName());
//             for (Object e : events) {
//                 System.err.println(e);
//             }
            return;
        }

        someFailed = true;
        System.err.println("Test failed: " + m.getName());
        for (Object error : errors) {
            if (error instanceof Throwable) {
                ((Throwable)error).printStackTrace();
            } else {
                System.err.println("Cause: " + error);
            }
        }
        System.err.println("Events:");
        synchronized(events) {
            for (Object e : events) {
                System.err.println(e);
            }
        }
        errors.clear();
        throw new Error();
    }

    public static void asser(boolean value) {
        if (!value) {
            fail("Test failed");
        }
    }
    public static void asser(boolean value, String msg) {
        if (!value) {
            fail(msg);
        }
    }
    static int screenNum = 0;
    public static void fail(Object cause) {
        synchronized (events) {
            events.add("FAILURE MOMENT");
        }
        errors.add(cause);
        errors.add("- Focus owner: " + KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
        errors.add("- Focused window: " + KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow());
//         try {
//             Robot r = new Robot();
//             BufferedImage image = r.createScreenCapture(new Rectangle(0, 0, 1024, 768));
//             ImageIO.write(image, "GIF", new File("/tmp/screen" + screenNum + ".gif"));
//             screenNum++;
//             image.flush();
//         } catch (Exception e) {
//         }
    }

    public static void _test1() {
        Frame f = new Frame();
        f.setLocation(100, 100);

        f.setVisible(true);

        Point loc = new Point(100, 100);
        robot.mouseMove(loc.x+30, loc.y+40);

        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException ie) {
        }
    }

    public static void testType() {
        Frame f = new Frame("testType");
        f.setLayout(new BorderLayout());
        TextField b = new TextField();
        f.add(b, BorderLayout.CENTER);
        f.setBounds(100, 100, 200, 200);

        f.setVisible(true);
        realSync(f);

        f.toFront();
        realSync(f);
        b.requestFocus();
        realSync(f);
        asser(b.isFocusOwner(), "Couldn't focus text field");

        robot.keyPress(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_A);
        realSync(f);
        asser("a".equals(b.getText()), "Expected 'a' got " + "'" +
            b.getText() + "'.");
        f.dispose();
    }

    public static void testTypeSwing() {
        JFrame f = new JFrame("testTypeSwing");
        f.setLayout(new BorderLayout());
        JTextField b = new JTextField();
        f.add(b, BorderLayout.CENTER);
        f.setBounds(100, 100, 200, 200);

        f.setVisible(true);
        realSync(f);

        f.toFront();
        realSync(f);
        b.requestFocus();
        realSync(f);
        asser(b.isFocusOwner(), "Couldn't focus text field");

        robot.keyPress(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_A);
        realSync(f);
        asser("a".equals(b.getText()), "Expected 'a' got " + "'" +
            b.getText() + "'.");
        f.dispose();
    }

    private static boolean pressed;
    public static void testPress() {
        Frame f = new Frame("testPress");
        f.setLayout(new FlowLayout());
        Button b = new Button("b");
        b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    pressed = true;
                }
            });
        f.add(b);
        f.setBounds(100, 100, 200, 200);

        f.setVisible(true);
        realSync(f);

        Point loc = b.getLocationOnScreen();
        events.add("Pressing at " + loc);
        robot.mouseMove(loc.x+3, loc.y+3);
        pressed = false;
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        realSync(f);
        asser(pressed, "Not pressed");
        f.dispose();
    }

    public static void testPressSwing() {
        JFrame f = new JFrame("testPressSwing");
        f.setLayout(new FlowLayout());
        JButton b = new JButton("b");
        b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    pressed = true;
                }
            });
        f.add(b);
        f.setBounds(100, 100, 200, 200);

        f.setVisible(true);
        realSync(f);

        Point loc = b.getLocationOnScreen();
        events.add("Pressing at " + loc);
        robot.mouseMove(loc.x+3, loc.y+3);
        pressed = false;
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        realSync(f);
        asser(pressed, "Not pressed");
        f.dispose();
    }

    public static void testFocus0() {
        Frame f = new Frame("testFocus0");
        f.setLayout(new FlowLayout());
        Button b1 = new Button("b1");
        Button b2 = new Button("b2");
        f.add(b1);
        f.add(b2);
        f.setBounds(100, 100, 200, 200);
        f.setVisible(true);
        realSync(f);
        f.toFront();
        realSync(f);
        asser(b1.isFocusOwner(), "B1 didn't get focus");
        b2.requestFocus();
        realSync(f);
        asser(b2.isFocusOwner(), "Couldn't focus b2");
        f.dispose();
    }

    public static void testFocus1() {
        Frame f = new Frame("testFocus1");
        f.setLayout(new FlowLayout());
        Button b1 = new Button("b1");
        f.add(b1);
        f.setBounds(100, 100, 200, 200);
        f.setVisible(true);
        realSync(f);
        f.toFront();
        realSync(f);
        asser(b1.isFocusOwner(), "B1 didn't get focus");
        f.dispose();
    }

    public static void testFocus2() {
        Frame f = new Frame("testFocus2");
        f.setLayout(new FlowLayout());
        Button b1 = new Button("b1");
        Button b2 = new Button("b2");
        f.add(b1);
        f.add(b2);
        f.setBounds(100, 100, 200, 200);
        f.setVisible(true);
        realSync(f);
        f.toFront();
        realSync(f);
        b2.requestFocus();
        realSync(f);
        if (!b2.isFocusOwner()) {
            fail("1: " + KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
        } else {
            // Half passed
            b1.requestFocus();
            realSync(f);
            asser(b1.isFocusOwner(), "B1 couldn't get focus");
        }
        f.dispose();
    }

    public static void testFocus2Swing() {
        JFrame f = new JFrame("testFocus2Swing");
        f.setLayout(new FlowLayout());
        JButton b1 = new JButton("b1");
        JButton b2 = new JButton("b2");
        f.add(b1);
        f.add(b2);
        f.setBounds(100, 100, 200, 200);
        f.setVisible(true);
        realSync(f);
        f.toFront();
        realSync(f);
        b2.requestFocus();
        realSync(f);
        if (!b2.isFocusOwner()) {
            fail("1: " + KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
        } else {
            // Half passed
            b1.requestFocus();
            realSync(f);
            asser(b1.isFocusOwner(), "B1 couldn't get focus");
        }
        f.dispose();
    }

    public static void realSync(Window w) {
        if (doRealSync) {
            ((sun.awt.SunToolkit)Toolkit.getDefaultToolkit()).realSync();
        }
    }

    public static void installListeners() {
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
                public void eventDispatched(AWTEvent e) {
                    synchronized(events) {
                        events.add(e);
                    }
                }
            }, 0xffff & ~AWTEvent.HIERARCHY_EVENT_MASK);
//         ((XToolkit)Toolkit.getDefaultToolkit()).addXEventListener(new XToolkit.XEventListener() {
//                 public void eventProcessed(IXAnyEvent e) {
//                     synchronized(events) {
//                         events.add(e);
//                     }
//                 }
//             });
    }
}
