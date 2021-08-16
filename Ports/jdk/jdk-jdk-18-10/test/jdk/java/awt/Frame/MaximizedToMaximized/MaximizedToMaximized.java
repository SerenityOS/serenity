/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Robot;

/**
 * @test
 * @key headful
 * @bug 8007219 8146168
 * @author Alexander Scherbatiy
 * @summary Frame size reverts meaning of maximized attribute
 * @run main MaximizedToMaximized
 */
public class MaximizedToMaximized {

    public static void main(String[] args) throws Exception {

        Frame frame = new Frame();
        Robot robot = new Robot();
        final Toolkit toolkit = Toolkit.getDefaultToolkit();
        final GraphicsEnvironment graphicsEnvironment =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice graphicsDevice =
                graphicsEnvironment.getDefaultScreenDevice();

        final Dimension screenSize = toolkit.getScreenSize();
        final Insets screenInsets = toolkit.getScreenInsets(
                graphicsDevice.getDefaultConfiguration());

        final Rectangle availableScreenBounds = new Rectangle(screenSize);

        availableScreenBounds.x += screenInsets.left;
        availableScreenBounds.y += screenInsets.top;
        availableScreenBounds.width -= (screenInsets.left + screenInsets.right);
        availableScreenBounds.height -= (screenInsets.top + screenInsets.bottom);

        frame.setBounds(availableScreenBounds.x, availableScreenBounds.y,
                availableScreenBounds.width, availableScreenBounds.height);
        frame.setVisible(true);
        robot.waitForIdle();

        Rectangle frameBounds = frame.getBounds();
        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        robot.waitForIdle();

        Rectangle maximizedFrameBounds = frame.getBounds();

        frame.dispose();
        if (maximizedFrameBounds.width < frameBounds.width
                || maximizedFrameBounds.height < frameBounds.height) {
            throw new RuntimeException("Maximized frame is smaller than non maximized");
        }
    }
}
