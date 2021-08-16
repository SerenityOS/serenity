/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8144735 8164811
 * @key headful
 * @summary Check if a per-pixel translucent and translucent window is dragged
 *          and resized by mouse correctly
 * Test Description: Check if PERPIXEL_TRANSLUCENT and TRANSLUCENT translucency
 *      types are supported on the current platform. Proceed if they are supported.
 *      Create a window with some components in it, make window undecorated, apply
 *      opacity of 0.7, apply translucent background of 0 and have a gradient
 *      painted as background from fully-transparent to fully-opaque in
 *      componentResized listener. Drag and resize the window using AWT Robot and
 *      verify that translucency is correctly applied with pixels checking. Make
 *      the window appear on top of a known background. Repeat this for specified
 *      JWindow, JDialog, JFrame.
 * Expected Result: If PERPIXEL_TRANSLUCENT and TRANSLUCENT translucency types
 *      are supported, the window should appear as specified with the translucency.
 *      Both window background and all the controls should be translucent but
 *      background should have a gradient in translucency while controls - not.
 * @author mrkam
 * @library /lib/client
 * @build Common ExtendedRobot
 * @run main TranslucentPerPixelTranslucentGradient
 * @run main/othervm -Dsun.java2d.uiScale=1.5 TranslucentPerPixelTranslucentGradient
 */

public class TranslucentPerPixelTranslucentGradient extends Common {

    public static void main(String[] ignored) throws Exception {
        if (checkTranslucencyMode(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)
            && checkTranslucencyMode(GraphicsDevice.WindowTranslucency.TRANSLUCENT))
            for (Class<Window> windowClass: WINDOWS_TO_TEST)
                new TranslucentPerPixelTranslucentGradient(windowClass).doTest();
    }

    public TranslucentPerPixelTranslucentGradient(Class windowClass) throws Exception {
        super(windowClass, 0.7f, 0f, true);
    }

    public void doTest() throws Exception {
        robot.waitForIdle(delay);
        checkTranslucent();
    }

    @Override
    public void applyShape() {
        gradientWidth = window.getWidth();
        gradientHeight = window.getHeight();
    }
}
