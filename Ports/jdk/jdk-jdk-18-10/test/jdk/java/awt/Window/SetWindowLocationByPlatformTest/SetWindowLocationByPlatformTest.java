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
 * @bug 8025130
 * @summary setLocationByPlatform has no effect
 * @author Dmitry Markov
 * @library ../../regtesthelpers
 * @build Util
 * @run main SetWindowLocationByPlatformTest
 */

import java.awt.*;

import test.java.awt.regtesthelpers.Util;

public class SetWindowLocationByPlatformTest {
    public static void main(String[] args) {
        Robot r = Util.createRobot();

        Frame frame1 = new Frame ("First Frame");
        frame1.setSize(500, 300);
        frame1.setLocationByPlatform(true);
        frame1.setVisible(true);
        Util.waitForIdle(r);

        Frame frame2 = new Frame ("Second Frame");
        frame2.setSize(500, 300);
        frame2.setLocationByPlatform(true);
        frame2.setVisible(true);
        Util.waitForIdle(r);

        Point point1 = frame1.getLocationOnScreen();
        Point point2 = frame2.getLocationOnScreen();

        try {
            if (point1.equals(point2)) {
                throw new RuntimeException("Test FAILED: both frames have the same location " + point1);
            }
        } finally {
            frame1.dispose();
            frame2.dispose();
        }
    }
}

