/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
  @bug 4173714
  @summary java.awt.button behaves differently under Win32/Solaris
  @author tdv@sparc.spb.su
  @library ../../../regtesthelpers
  @build Util
  @run main DisabledComponentsTest
*/

/**
 * DisabledComponentsTest.java
 *
 * summary: java.awt.button behaves differently under Win32/Solaris
 * Disabled component should not receive events. This is what this
 * test checks out.
 */

import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.atomic.AtomicBoolean;

import test.java.awt.regtesthelpers.Util;

import javax.swing.*;

public class DisabledComponentsTest {

    private static Frame frame;
    private static Button b = new Button("Button");
    private static final AtomicBoolean pressed = new AtomicBoolean(false);
    private static final AtomicBoolean entered = new AtomicBoolean(false);

    private static void init() {
        frame = new Frame("Test");
        frame.setBounds(100, 100, 100, 100);
        b = new Button("Test");
        b.setEnabled(false);
        b.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                System.err.println("Mouse pressed. target=" + e.getSource());
                if (!b.isEnabled()) {
                    System.err.println("TEST FAILED: BUTTON RECEIVED AN EVENT WHEN DISABLED!");
                    pressed.set(true);
                }
            }
            public void mouseEntered(MouseEvent e) {
                System.out.println("Mouse entered. target=" + e.getSource());
                if (!b.isEnabled())
                    System.err.println("TEST FAILED: BUTTON RECEIVED AN EVENT WHEN DISABLED!");
                entered.set(true);
            }
        });
        frame.add(b);
        frame.setVisible(true);
    }

    public static void main(String[] args) throws Exception {
        try {
            Robot r = Util.createRobot();
            r.setAutoDelay(200);
            r.setAutoWaitForIdle(true);
            r.mouseMove(0, 0);
            SwingUtilities.invokeAndWait(DisabledComponentsTest::init);
            Util.waitForIdle(r);
            Util.pointOnComp(b, r);
            if (entered.get()) {
                throw new RuntimeException("TEST FAILED: disabled button received MouseEntered event");
            }
            Util.clickOnComp(b, r);
            if (pressed.get()) {
                throw new RuntimeException("TEST FAILED: disabled button received MousePressed event");
            }
        } finally {
            if (frame != null) {
                frame.dispose();
            }
        }
    }
}
