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
import java.awt.ScrollPane;
import java.awt.Toolkit;

/**
 * @test
 * @key headful
 * @bug 7124213
 * @author Sergey Bylokhov
 * @library /lib/client/
 * @build ExtendedRobot
 * @run main ScrollPanePreferredSize
 */
public final class ScrollPanePreferredSize {

    public static void main(final String[] args) {
        final Dimension expected = new Dimension(300, 300);
        final Frame frame = new Frame();
        final ScrollPane sp = new ScrollPane();
        sp.setSize(expected);
        frame.add(sp);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        sleep();
        final Dimension size = frame.getSize();
        if (size.width < expected.width || size.height < expected.height) {
            throw new RuntimeException(
                    "Expected size: >= " + expected + ", actual size: " + size);
        }
        frame.dispose();
    }

    private static void sleep() {
        try {
            ExtendedRobot robot = new ExtendedRobot();
            robot.waitForIdle(500);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Unexpected failure");
        }
    }
}
