/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8165619
 * @summary Frame is not repainted if created in state=MAXIMIZED_BOTH on Unity
 * @run main DecoratedFrameInsetsTest
 */

import java.awt.*;

public class DecoratedFrameInsetsTest {
    static Robot robot;
    private static Insets expectedInsets;

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        expectedInsets = getExpectedInsets();
        System.out.println("Normal state insets: " + expectedInsets);
        testState(Frame.MAXIMIZED_BOTH);
        testState(Frame.ICONIFIED);
        testState(Frame.MAXIMIZED_HORIZ);
        testState(Frame.MAXIMIZED_VERT);
    }

    private static Insets getExpectedInsets() {
        Frame frame = new Frame();
        frame.setVisible(true);
        robot.waitForIdle();
        robot.delay(200);
        Insets expectedInsets = frame.getInsets();
        frame.dispose();
        return expectedInsets;
    }

    static void testState(int state) {
        Frame frame = new Frame();
        if( Toolkit.getDefaultToolkit().isFrameStateSupported(state)) {
            frame.setBounds(150, 150, 200, 200);
            frame.setExtendedState(state);
            frame.setVisible(true);
            robot.waitForIdle();
            robot.delay(200);
            System.out.println("State " + state +
                                               " insets: " + frame.getInsets());

            frame.setExtendedState(Frame.NORMAL);
            frame.toFront();
            robot.waitForIdle();
            robot.delay(200);
            Insets insets = frame.getInsets();
            frame.dispose();
            System.out.println("State " + state +
                                           " back to normal insets: " + insets);
            if(!expectedInsets.equals(insets)) {
                throw new RuntimeException("Insets are wrong " + insets);
            }
        }
    }
}
