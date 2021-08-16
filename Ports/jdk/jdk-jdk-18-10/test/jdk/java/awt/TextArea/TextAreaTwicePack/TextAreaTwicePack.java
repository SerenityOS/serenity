/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.TextArea;
import java.awt.Robot;


/**
 * @test
 * @key headful
 * @bug 7160627
 * @summary We shouldn't get different frame size when we call Frame.pack()
 * twice.
 * @author Sergey Bylokhov
 */
public final class TextAreaTwicePack {

    public static void main(final String[] args) {
        final Frame frame = new Frame();
        final TextArea ta = new TextArea();
        frame.add(ta);
        frame.pack();
        frame.setVisible(true);
        sleep();
        final Dimension before = frame.getSize();
        frame.pack();
        final Dimension after = frame.getSize();
        if (!after.equals(before)) {
            throw new RuntimeException(
                    "Expected size: " + before + ", actual size: " + after);
        }
        frame.dispose();
    }

    private static void sleep() {
        try {
            Robot robot = new Robot();
            robot.waitForIdle();
            Thread.sleep(500L);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }
}
