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

/*
 * @test
 * @key headful
 * @bug 8171952
 * @summary Tests that getMousePosition() returns null for obscured component.
 * @author Dmitry Markov
 * @library ../../regtesthelpers
 * @build Util
 * @run main ObscuredFrameTest
 */

import java.awt.*;

import test.java.awt.regtesthelpers.Util;

public class ObscuredFrameTest {
    public static void main(String[] args) {
        Robot robot = Util.createRobot();

        Frame frame = new Frame("Obscured Frame");
        frame.setSize(200, 200);
        frame.setLocationRelativeTo(null);
        Button button = new Button("Button");
        frame.add(button);

        Dialog dialog = new Dialog(frame, "Visible Dialog", false);
        dialog.setSize(200, 200);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        frame.setVisible(true);

        Util.waitForIdle(robot);

        Util.pointOnComp(button, robot);
        Util.waitForIdle(robot);

        try {
            if (button.getMousePosition() != null) {
                throw new RuntimeException("Test Failed! Mouse position is not null for obscured component.");
            }
        } finally {
            frame.dispose();
            dialog.dispose();
        }
    }
}

