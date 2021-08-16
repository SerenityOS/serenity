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


import java.awt.Color;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

/**
 * @test
 * @key headful
 * @bug 8019591
 * @author Sergey Bylokhov
 */
public class WindowGCInFullScreen {

    public static void main(final String[] args)
            throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(() -> {
            final GraphicsDevice[] devices =
                    GraphicsEnvironment.getLocalGraphicsEnvironment()
                                       .getScreenDevices();
            final Frame frame = new Frame();
            frame.setBackground(Color.GREEN);
            frame.setUndecorated(true);
            frame.setSize(100, 100);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            sleep();
            for (final GraphicsDevice gd : devices) {
                try {
                    gd.setFullScreenWindow(frame);
                    if (gd.getFullScreenWindow() != frame) {
                        throw new RuntimeException("Wrong window");
                    }
                    if (frame.getGraphicsConfiguration().getDevice() != gd) {
                        throw new RuntimeException("Wrong new GraphicsDevice");
                    }
                } finally {
                    // cleaning up
                    gd.setFullScreenWindow(null);
                }
            }
            frame.dispose();
        });
    }

    private static void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
    }
}
