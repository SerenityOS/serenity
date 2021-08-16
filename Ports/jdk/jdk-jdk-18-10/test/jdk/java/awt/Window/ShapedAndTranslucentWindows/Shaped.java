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
 * @key headful
 * @summary Check if dynamically shaped window is dragged and resized
 *          by mouse correctly
 *
 * Test Description: Check if specified translucency types PERPIXEL_TRANSPARENT
 *      is supported on the current platform. Proceed if it is supported.
 *      Create a window apply shape in componentResized listener. The shape
 *      should match the window size. Drag and resize the window using AWT Robot
 *      and verify that shape is correctly applied both with pixels checking and
 *      clicks. Make the window appear on top of a known background. Repeat this
 *      for Window, Dialog, Frame,
 * Expected Result: If PERPIXEL_TRANSPARENT translucency type is supported,
 *      the window should appear with the expected shape. Clicks should come
 *      to visible parts of shaped window only and to background for clipped
 *      parts.
 *
 * @author mrkam
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @library /lib/client
 * @build Common ExtendedRobot
 * @run main Shaped
 */
public class Shaped extends Common{

    public static void main(String[] args) throws Exception {
        if (checkTranslucencyMode(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT))
            for (Class<Window> windowClass: WINDOWS_TO_TEST){
                new Shaped(windowClass).doTest();
            }
    }

    public Shaped(Class windowClass) throws Exception{
        super(windowClass);
    }

    @Override
    public void applyShape(){ applyDynamicShape(); }

    @Override
    public void doTest() throws Exception{
        super.doTest();

        checkDynamicShape();

        // Drag
        Point location = window.getLocationOnScreen();
        robot.dragAndDrop(location.x + dl, location.y + 5, location.x + dl + random.nextInt(dl), location.y + random.nextInt(dl));
        robot.waitForIdle(delay);
        checkDynamicShape();

        // Resize
        location = window.getLocationOnScreen();
        robot.dragAndDrop(location.x + 4, location.y + 4, location.x + random.nextInt(2*dl)-dl, location.y + random.nextInt(2*dl)-dl);
        robot.waitForIdle(delay);
        checkDynamicShape();

        dispose();
    }
}
