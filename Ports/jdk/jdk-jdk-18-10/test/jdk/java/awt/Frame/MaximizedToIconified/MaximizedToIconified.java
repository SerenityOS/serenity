/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4977491 8160767
 * @summary State changes should always be reported as events
 * @run main MaximizedToIconified
 */

/*
 * MaximizedToIconified.java
 *
 * summary:  Invoking setExtendedState(ICONIFIED) on a maximized
 *           frame should not combine the maximized and iconified
 *           states in the newState of the state change event.
 */

import java.awt.Frame;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;

public class MaximizedToIconified
{
    static volatile int lastFrameState = Frame.NORMAL;
    static volatile boolean failed = false;
    static volatile Toolkit myKit;
    private static Robot robot;

    private static void checkState(Frame frame, int state) {
        frame.setExtendedState(state);
        robot.waitForIdle();
        robot.delay(100);

        System.out.println("state = " + state + "; getExtendedState() = " + frame.getExtendedState());

        if (failed) {
            frame.dispose();
            throw new RuntimeException("getOldState() != previous getNewState() in WINDOW_STATE_CHANGED event.");
        }
        if (lastFrameState != frame.getExtendedState()) {
            frame.dispose();
            throw new RuntimeException("getExtendedState() != last getNewState() in WINDOW_STATE_CHANGED event.");
        }
        if (frame.getExtendedState() != state) {
            frame.dispose();
            throw new RuntimeException("getExtendedState() != " + state + " as expected.");
        }
    }

    private static void examineStates(int states[]) {

        Frame frame = new Frame("test");
        frame.setSize(200, 200);
        frame.setVisible(true);

        robot.waitForIdle();

        frame.addWindowStateListener(new WindowStateListener() {
            public void windowStateChanged(WindowEvent e) {
                System.out.println("last = " + lastFrameState + "; getOldState() = " + e.getOldState() +
                        "; getNewState() = " + e.getNewState());
                if (e.getOldState() == lastFrameState) {
                    lastFrameState = e.getNewState();
                } else {
                    System.out.println("Wrong getOldState(): expected = " + lastFrameState + "; received = " +
                            e.getOldState());
                    failed = true;
                }
            }
        });

        for (int state : states) {
            if (myKit.isFrameStateSupported(state)) {
                checkState(frame, state);
            } else {
                System.out.println("Frame state = " + state + " is NOT supported by the native system. The state is skipped.");
            }
        }

        if (frame != null) {
            frame.dispose();
        }
    }

    private static void doTest() {

        myKit = Toolkit.getDefaultToolkit();

        // NOTE! Compound states (like MAXIMIZED_BOTH | ICONIFIED) CANNOT be used,
        //    because Toolkit.isFrameStateSupported() method reports these states
        //    as not supported. And such states will simply be skipped.
        examineStates(new int[] {Frame.MAXIMIZED_BOTH, Frame.ICONIFIED, Frame.NORMAL});
        examineStates(new int[] {Frame.ICONIFIED, Frame.MAXIMIZED_BOTH, Frame.NORMAL});

    }

    public static void main( String args[] ) throws Exception
    {
        robot = new Robot();
        doTest();

    }

}
