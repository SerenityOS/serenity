/*
 * Copyright (c) 2002, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8189204
 * @summary Possible NPE in Component::getLocationOnScreen()
 * @key headful
 * @run main ComponentGetLocationOnScreenNPETest
 */

import javax.swing.*;
import java.awt.*;

public class ComponentGetLocationOnScreenNPETest {

    private static Frame frame;
    private static JPanel panel;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame = new Frame();
            JPanel parentPanel = new JPanel() {
                @Override
                public Container getParent() {
                    return new Frame();
                }
            };
            frame.add(parentPanel);
            panel = new JPanel();
            parentPanel.add(panel);
            frame.setVisible(true);
        });
        Robot robot = new Robot();
        robot.waitForIdle();
        robot.delay(200);

        SwingUtilities.invokeAndWait(panel::getLocationOnScreen);
        SwingUtilities.invokeLater(frame::dispose);
    }
}
