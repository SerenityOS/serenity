/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8032078
 * @summary Frame.setExtendedState throws RuntimeException, if
 *          windowState=ICONIFIED|MAXIMIZED_BOTH, on OS X
 * @author Anton Litvinov
 */

import java.awt.*;

public class ExceptionOnSetExtendedStateTest {
    private static final int[] frameStates = { Frame.NORMAL, Frame.ICONIFIED, Frame.MAXIMIZED_BOTH };

    private static boolean validatePlatform() {
        String osName = System.getProperty("os.name");
        if (osName == null) {
            throw new RuntimeException("Name of the current OS could not be retrieved.");
        }
        return osName.startsWith("Mac");
    }

    private static void testStateChange(int oldState, int newState, boolean decoratedFrame) {
        System.out.println(String.format(
            "testStateChange: oldState='%d', newState='%d', decoratedFrame='%b'",
            oldState, newState, decoratedFrame));

        Frame frame = new Frame("ExceptionOnSetExtendedStateTest");
        frame.setSize(200, 200);
        frame.setUndecorated(!decoratedFrame);
        frame.setVisible(true);
        try {
            Robot robot = new Robot();
            robot.waitForIdle();
        }catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Unexpected failure");
        }

        frame.setExtendedState(oldState);
        sleep(1000);
        frame.setExtendedState(newState);

        boolean stateWasNotChanged = true;
        int currentState = 0;
        for (int i = 0; (i < 3) && stateWasNotChanged; i++) {
            sleep(1000);
            currentState = frame.getExtendedState();
            if ((currentState == newState) ||
                (((newState & Frame.ICONIFIED) != 0) && ((currentState & Frame.ICONIFIED) != 0))) {
                stateWasNotChanged = false;
            }
        }
        frame.dispose();

        if (stateWasNotChanged) {
            throw new RuntimeException(String.format(
                "Frame state was not changed. currentState='%d'", currentState));
        }
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (!validatePlatform()) {
            System.out.println("This test is only for OS X.");
            return;
        }

        // Verify that changing states of decorated/undecorated frame to/from supported states
        // and the state bit mask ICONIFIED | MAXIMIZED_BOTH does not raise RuntimeException.
        for (int i = 0; i < frameStates.length; i++) {
            testStateChange(frameStates[i], Frame.ICONIFIED | Frame.MAXIMIZED_BOTH, true);
            testStateChange(frameStates[i], Frame.ICONIFIED | Frame.MAXIMIZED_BOTH, false);
            testStateChange(Frame.ICONIFIED | Frame.MAXIMIZED_BOTH, frameStates[i], true);
            testStateChange(Frame.ICONIFIED | Frame.MAXIMIZED_BOTH, frameStates[i], false);
        }
    }
}
