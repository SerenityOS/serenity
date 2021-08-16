/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.InputEvent;

/**
 * @test
 * @key headful
 * @bug 8032595
 * @summary setResizable(false) makes a frame slide down
 * @author Petr Pchelko
 */

public class SlideNotResizableTest {

    private static volatile boolean passed = false;
    private static final Dimension FRAME_SIZE = new Dimension(100, 100);
    private static final Point FRAME_LOCATION = new Point(200, 200);

    public static void main(String[] args) throws Throwable {
        Frame aFrame = null;
        try {
            aFrame = new Frame();
            aFrame.setSize(FRAME_SIZE);
            aFrame.setLocation(FRAME_LOCATION);
            aFrame.setResizable(false);
            aFrame.setVisible(true);

            sync();

            if (!aFrame.getLocation().equals(FRAME_LOCATION)) {
                throw new RuntimeException("FAILED: Wrong frame position");
            }
        } finally {
            if (aFrame != null) {
                aFrame.dispose();
            }
        }
    }

    private static void sync() throws Exception {
        Robot robot = new Robot();
        robot.waitForIdle();
        Thread.sleep(1000);
    }
}
