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
 * @bug 8154434
 * @summary Open the request focus methods of the java.awt.Component which accept
 *          FocusEvent.Cause
 * @run main RequestFocusByCauseTest
 */

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class RequestFocusByCauseTest {
    static boolean success;

    public static void main(String[] args) throws Exception {
        testRequestFocusCause();
        testRequestFocusTemporaryCause();
        testRequestFocusInWindowCause();
        System.out.println("ok");
    }

    private static void testRequestFocusCause() throws AWTException {
        Frame frame = new Frame();
        Component c = new Button();
        frame.add(new Button());
        frame.add(c);
        c.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                success = e.getCause() == FocusEvent.Cause.UNEXPECTED;
            }

            @Override
            public void focusLost(FocusEvent e) {}
        });
        Robot robot = new Robot();

        try {
            frame.setVisible(true);
            robot.waitForIdle();
            robot.delay(200);
            success = false;

            c.requestFocus(FocusEvent.Cause.UNEXPECTED);
            robot.waitForIdle();
            robot.delay(200);
            if(!success) {
                throw new RuntimeException("request failed");
            }
        } finally {
            frame.dispose();
        }
    }

    private static void testRequestFocusTemporaryCause() throws AWTException {
        Frame frame = new Frame();
        frame.add(new Button() {
            @Override
            protected boolean requestFocus(boolean temporary,
                                                       FocusEvent.Cause cause) {
                success = cause == FocusEvent.Cause.ROLLBACK;
                return super.requestFocus(temporary, cause);
            }
        });
        Component c = new Button() {
            @Override
            public void requestFocus() {
                super.requestFocus();
                setFocusable(false);
            }
        };
        frame.add(c);
        Robot robot = new Robot();

        try {
            frame.setVisible(true);
            robot.waitForIdle();
            robot.delay(200);

            success = false;
            c.requestFocus();
            robot.waitForIdle();
            robot.delay(200);


            if(!success) {
                throw new RuntimeException("rollback request is not triggered");
            }
        } finally {
            frame.dispose();
        }
    }

    private static void testRequestFocusInWindowCause() throws AWTException {
        Frame frame = new Frame();
        Component c = new Button();
        frame.add(new Button());
        frame.add(c);
        c.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                success = e.getCause() == FocusEvent.Cause.UNEXPECTED;
            }

            @Override
            public void focusLost(FocusEvent e) {
            }
        });
        Robot robot = new Robot();

        try {
            frame.setVisible(true);
            robot.waitForIdle();
            robot.delay(200);
            success = false;

            c.requestFocusInWindow(FocusEvent.Cause.UNEXPECTED);
            robot.waitForIdle();
            robot.delay(200);
            if (!success) {
                throw new RuntimeException("request in window failed");
            }
        } finally {
            frame.dispose();
        }
    }
}

