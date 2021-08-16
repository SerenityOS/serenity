/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6981400
 * @summary Tabbing between textfiled do not work properly when ALT+TAB
 * @author  anton.tarasov
 * @library ../../regtesthelpers
 * @build   Util
 * @run     main Test1
 */

// This test shows a frame with four focusable components: b0, b1, b2, b3.
// Then it presses Tab three times. EDT is freezed for a while on the first FOCUS_LOST event.
// Meantime, the test clicks in a component of another frame and then clicks in the title
// of the original frame. When EDT awakes and all the queued events get processed,
// the other frame should ones gain focus and then pass it to the original frame.
// The b3 component of the orinial frame should finally become a focus owner.
// The FOCUS_LOST/FOCUS_GAINED events order in the original frame is tracked and should be:
// b0 -> b1 -> b2 -> b3.

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;
import test.java.awt.regtesthelpers.Util;

public class Test1 {
    static JFrame f0 = new JFrame("base_frame") { public String getName() {return "base_frame";} };
    static JButton f0b0 = new JB("b0");
    static JButton f0b1 = new JB("b1");
    static JButton f0b2 = new JB("b2");
    static JButton f0b3 = new JB("b3");

    static JFrame f1 = new JFrame("swing_frame") { public String getName() {return "swing_frame";} };
    static JButton f1b0 = new JButton("button");

    static Frame f2 = new Frame("awt_frame") { public String getName() {return "awt_frame";} };
    static Button f2b0 = new Button("button");

    static Robot robot;

    static List<Component> gainedList = new ArrayList<Component>();
    static List<Component> lostList = new ArrayList<Component>();

    static Component[] refGainedList = new Component[] {f0b1, f0b2, f0b3, f0b3};
    static Component[] refLostList = new Component[] {f0b0, f0b1, f0b2, f0b3};

    static boolean tracking;

    public static void main(String[] args) {
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            public void eventDispatched(AWTEvent e) {
                System.out.println(e);
            }
        }, FocusEvent.FOCUS_EVENT_MASK | WindowEvent.WINDOW_EVENT_MASK);

        try {
            robot = new Robot();
        } catch (AWTException ex) {
            throw new RuntimeException("Error: can't create Robot");
        }

        f0.add(f0b0);
        f0.add(f0b1);
        f0.add(f0b2);
        f0.add(f0b3);
        f0.setLayout(new FlowLayout());
        f0.setBounds(0, 100, 400, 200);

        f1.add(f1b0);
        f1.setBounds(0, 400, 400, 200);

        f2.add(f2b0);
        f2.setBounds(0, 400, 400, 200);

        f0b0.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                try {
                    Thread.sleep(1000);
                } catch (Exception ex) {}
            }
        });

        //
        // Case 1. Test against swing JFrame.
        //

        f1.setVisible(true);
        f0.setVisible(true);

        Util.waitForIdle(robot);

        if (!f0b0.isFocusOwner()) {
            Util.clickOnComp(f0b0, robot);
            Util.waitForIdle(robot);
            if (!f0b0.isFocusOwner()) {
                throw new RuntimeException("Error: can't focus the component " + f0b0);
            }
        }

        System.out.println("\nTest case 1: swing frame\n");
        test(f1b0);

        //
        // Case 2. Test against awt Frame.
        //

        tracking = false;
        gainedList.clear();
        lostList.clear();

        f1.dispose();
        f2.setAutoRequestFocus(false);
        f2.setVisible(true);
        Util.waitForIdle(robot);

        Util.clickOnComp(f0b0, robot);
        Util.waitForIdle(robot);
        if (!f0b0.isFocusOwner()) {
            throw new RuntimeException("Error: can't focus the component " + f0b0);
        }

        System.out.println("\nTest case 2: awt frame\n");
        test(f2b0);

        System.out.println("\nTest passed.");
    }

    public static void test(Component compToClick) {
        tracking = true;

        robot.keyPress(KeyEvent.VK_TAB);
        robot.delay(50);
        robot.keyRelease(KeyEvent.VK_TAB);
        robot.delay(50);

        robot.keyPress(KeyEvent.VK_TAB);
        robot.delay(50);
        robot.keyRelease(KeyEvent.VK_TAB);
        robot.delay(50);

        robot.keyPress(KeyEvent.VK_TAB);
        robot.delay(50);
        robot.keyRelease(KeyEvent.VK_TAB);

        robot.delay(50);
        Util.clickOnComp(compToClick, robot);

        robot.delay(50);
        Util.clickOnTitle(f0, robot);

        Util.waitForIdle(robot);

        if (!f0b3.isFocusOwner()) {
            throw new RuntimeException("Test failed: f0b3 is not a focus owner");
        }

        if (!"sun.awt.X11.XToolkit".equals(Toolkit.getDefaultToolkit().getClass().getName())) {

            if (!Arrays.asList(refGainedList).equals(gainedList)) {
                System.out.println("gained list: " + gainedList);
                throw new RuntimeException("Test failed: wrong FOCUS_GAINED events order");
            }
            if (!Arrays.asList(refLostList).equals(lostList)) {
                System.out.println("lost list: " + lostList);
                throw new RuntimeException("Test failed: wrong FOCUS_LOST events order");
            }
        }
    }
}

class JB extends JButton {
    String name;

    public JB(String name) {
        super(name);
        this.name = name;

        addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                if (Test1.tracking)
                    Test1.gainedList.add(e.getComponent());
            }

            public void focusLost(FocusEvent e) {
                if (Test1.tracking)
                    Test1.lostList.add(e.getComponent());
            }
        });
    }

    public String toString() {
        return "[" + name + "]";
    }
}
