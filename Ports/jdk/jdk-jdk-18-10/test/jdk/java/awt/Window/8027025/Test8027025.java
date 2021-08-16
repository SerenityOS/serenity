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
 * @test
 * @key headful
 * @bug 8027025
 * @summary [macosx] getLocationOnScreen returns 0 if parent invisible
 * @author Petr Pchelko
 * @run main Test8027025
 */

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

public class Test8027025 {

    private static Frame frame;
    private static Window window;

    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame = new Frame("Dummy Frame");
                window = new Window(frame);
                window.setSize(200, 200);
                window.setLocationRelativeTo(frame);
                window.setVisible(true);
            });

            Robot robot = new Robot();
            robot.waitForIdle();

            AtomicReference<Point> point = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> point.set(window.getLocationOnScreen()));

            if (point.get().getX() == 0 || point.get().getY() == 0) {
                throw new RuntimeException("Test failed. The location was not set");
            }
        } finally {
            if (frame != null) {
                frame.dispose();
            }
            if (window != null) {
                window.dispose();
            }
        }
    }
}
