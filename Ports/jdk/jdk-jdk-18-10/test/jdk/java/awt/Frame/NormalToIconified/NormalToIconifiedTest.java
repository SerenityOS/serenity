/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8171949 8214046
 * @summary Tests that bitwise mask is set and state listener is notified during state transition.
 * @author Dmitry Markov
 * @library ../../regtesthelpers
 * @build Util
 * @run main NormalToIconifiedTest
 */

import java.awt.Frame;
import java.awt.Robot;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.util.concurrent.atomic.AtomicBoolean;

import test.java.awt.regtesthelpers.Util;

public class NormalToIconifiedTest {

    public static void main(String[] args) {
        test(false);
        test(true);
    }

    private static void test(final boolean undecorated) {
        AtomicBoolean listenerNotified = new AtomicBoolean(false);

        Robot robot = Util.createRobot();
        Frame testFrame = new Frame("Test Frame");
        testFrame.setUndecorated(undecorated);
        testFrame.setSize(200, 200);
        testFrame.addWindowStateListener(new WindowStateListener() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                listenerNotified.set(true);
                synchronized (listenerNotified) {
                    listenerNotified.notifyAll();
                }
            }
        });
        testFrame.setVisible(true);
        Frame mainFrame = new Frame("Main Frame");
        mainFrame.setSize(200, 200);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
        Util.waitForIdle(robot);
        try {
            Util.clickOnComp(mainFrame, robot);
            Util.waitForIdle(robot);

            // NORMAL -> ICONIFIED
            listenerNotified.set(false);
            testFrame.setExtendedState(Frame.ICONIFIED);
            Util.waitForIdle(robot);

            Util.waitForCondition(listenerNotified, 2000);
            if (!listenerNotified.get()) {
                throw new RuntimeException("Test FAILED! Window state listener was not notified during NORMAL to" +
                        "ICONIFIED transition");
            }
            if (testFrame.getExtendedState() != Frame.ICONIFIED) {
                throw new RuntimeException("Test FAILED! Frame is not in ICONIFIED state");
            }

            // ICONIFIED -> NORMAL
            listenerNotified.set(false);
            testFrame.setExtendedState(Frame.NORMAL);
            Util.waitForIdle(robot);

            Util.waitForCondition(listenerNotified, 2000);
            if (!listenerNotified.get()) {
                throw new RuntimeException("Test FAILED! Window state listener was not notified during ICONIFIED to" +
                        "NORMAL transition");
            }
            if (testFrame.getExtendedState() != Frame.NORMAL) {
                throw new RuntimeException("Test FAILED! Frame is not in NORMAL state");
            }
        } finally {
            testFrame.dispose();
            mainFrame.dispose();
        }
    }
}

