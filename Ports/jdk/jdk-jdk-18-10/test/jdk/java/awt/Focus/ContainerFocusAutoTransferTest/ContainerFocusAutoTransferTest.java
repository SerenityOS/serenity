/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @key headful
  @bug       6607170
  @summary   Tests for focus-auto-transfer.
  @library   ../../regtesthelpers
  @build     Util
  @run       main ContainerFocusAutoTransferTest
*/

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.DefaultKeyboardFocusManager;
import java.awt.KeyboardFocusManager;
import java.awt.Robot;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusEvent;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import test.java.awt.regtesthelpers.Util;

public class ContainerFocusAutoTransferTest {
    Robot robot;
    TestFrame frame;
    KeyboardFocusManager kfm;
    enum TestCase {
        REMOVAL { public String toString() { return "removal"; } },
        HIDING { public String toString() { return "hiding"; } },
        DISABLING { public String toString() { return "disabling"; } },
        DEFOCUSING { public String toString() { return "defocusing"; } };
        public abstract String toString();
    };

    public static void main(String[] args) {
        ContainerFocusAutoTransferTest app = new ContainerFocusAutoTransferTest();
        app.init();
        app.start();
    }

    public void init() {
        robot = Util.createRobot();
        kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            public void eventDispatched(AWTEvent event) {
                System.out.println("--> " + event);
            }
        }, FocusEvent.FOCUS_EVENT_MASK | WindowEvent.WINDOW_FOCUS_EVENT_MASK);
    }

    public void start() {
        System.out.println("*** TEST #1 ***");
        test(TestCase.HIDING);

        System.out.println("*** TEST #2 ***");
        test(TestCase.REMOVAL);

        System.out.println("*** TEST #3 ***");
        test3(TestCase.DISABLING);

        System.out.println("*** TEST #4 ***");
        test3(TestCase.DEFOCUSING);

        System.out.println("*** TEST #5 ***");
        test4();

        System.out.println("Test passed.");
    }

    void test(final TestCase t) {
        showFrame();
        test1(t); // Test for correct auto-transfer
        test2(t); // Test for clearing focus
    }

    void test1(final TestCase t) {
        Runnable action = new Runnable() {
            public void run() {
                KeyboardFocusManager.setCurrentKeyboardFocusManager(new TestKFM());
                if (t == TestCase.REMOVAL) {
                    frame.remove(frame.panel0);

                } else if (t == TestCase.HIDING) {
                    frame.panel0.setVisible(false);
                }
                frame.repaint();
            }
        };
        if (!Util.trackFocusGained(frame.b3, action, 2000, false)) {
            throw new TestFailedException(t + ": focus wasn't transfered as expected!");
        }
        KeyboardFocusManager.setCurrentKeyboardFocusManager(kfm);
    }

    void test2(TestCase t) {
        frame.setFocusable(false); // exclude it from the focus cycle
        if (t == TestCase.REMOVAL) {
            frame.remove(frame.panel1);

        } else if (t == TestCase.HIDING) {
            frame.panel1.setVisible(false);
        }
        frame.repaint();
        Util.waitForIdle(robot);
        if (kfm.getFocusOwner() != null) {
            throw new TestFailedException(t + ": focus wasn't cleared!");
        }
    }

    void test3(final TestCase t) {
        showFrame();
        Runnable action = new Runnable() {
            public void run() {
                if (t == TestCase.DISABLING) {
                    frame.b0.setEnabled(false);

                } else if (t == TestCase.DEFOCUSING) {
                    frame.b0.setFocusable(false);
                }
            }};
        if (!Util.trackFocusGained(frame.b1, action, 2000, false)) {
            throw new TestFailedException(t + ": focus wasn't transfered as expected!");
        }
    }

    void test4() {
        showFrame();
        frame.setFocusableWindowState(false);
        Util.waitForIdle(robot);
        if (kfm.getFocusOwner() != null) {
            throw new TestFailedException("defocusing the frame: focus wasn't cleared!");
        }
    }

    void showFrame() {
        if (frame != null) {
            frame.dispose();
            Util.waitForIdle(robot);
        }
        frame = new TestFrame();
        frame.setVisible(true);
        Util.waitTillShown(frame);

        if (!frame.b0.hasFocus()) {
            Util.clickOnComp(frame.b0, robot);
            Util.waitForIdle(robot);
            if (!frame.b0.hasFocus()) {
                throw new TestErrorException("couldn't set focus on " + frame.b2);
            }
        }
    }

    class TestKFM extends DefaultKeyboardFocusManager {
        public boolean dispatchEvent(AWTEvent e) {
            if (e.getID() == FocusEvent.FOCUS_GAINED) {
                System.out.println(e);
                Component src = (Component)e.getSource();
                if (src == frame.b1 || src == frame.b2) {
                    throw new TestFailedException("wrong focus transfer on removal!");
                }
            }
            return super.dispatchEvent(e);
        }
    }
}

class TestFrame extends JFrame {
    public JPanel panel0 = new JPanel();
    public JPanel panel1 = new JPanel();
    public JButton b0 = new JButton("b0");
    public JButton b1 = new JButton("b1");
    public JButton b2 = new JButton("b2");
    public JButton b3 = new JButton("b3");
    public JButton b4 = new JButton("b4");

    public TestFrame() {
        super("TestFrame");

        // The change of the orientation and the reverse order of
        // adding the buttons to the panel is because in Container.removeNotify()
        // the child components are removed in the reverse order.
        // We want that the focus owner (b0) would be removed first and
        // that the next traversable component would be b1.
        panel0.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        panel0.add(b2);
        panel0.add(b1);
        panel0.add(b0);

        panel1.add(b3);
        panel1.add(b4);

        setLayout(new FlowLayout());
        add(panel0);
        add(panel1);
        pack();

        panel0.setBackground(Color.red);
        panel1.setBackground(Color.blue);
    }
}

// Thrown when the behavior being verified is found wrong.
class TestFailedException extends RuntimeException {
    TestFailedException(String msg) {
        super("Test failed: " + msg);
    }
}

// Thrown when an error not related to the behavior being verified is encountered.
class TestErrorException extends RuntimeException {
    TestErrorException(String msg) {
        super("Unexpected error: " + msg);
    }
}
