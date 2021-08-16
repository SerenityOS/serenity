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

import javax.swing.*;
import java.awt.*;

/*
 * @test
 * @bug 8164811
 * @key headful
 * @summary Check if a per-pixel translucent window shows only the area having
 *          opaque pixels
 * Test Description: Check if PERPIXEL_TRANSLUCENT Translucency type is supported
 *      on the current platform. Proceed if it is supported. Create a swing window
 *      with some swing components in it and a transparent background (alpha 0.0).
 *      Bring this window on top of a known background. Do this test for JFrame,
 *      JWindow and JDialog
 * Expected Result: Only the components present in the window must be shown. Other
 *      areas of the window must be transparent so that the background shows
 * @author mrkam
 * @library /lib/client
 * @build Common ExtendedRobot
 * @run main PerPixelTranslucentSwing
 * @run main/othervm -Dsun.java2d.uiScale=1.5 PerPixelTranslucentSwing
 */

public class PerPixelTranslucentSwing extends Common {

    JButton north;

    public static void main(String[] ignored) throws Exception {
        FG_COLOR = new Color(200, 0, 0, 0);
        if (checkTranslucencyMode(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT))
            for (Class<Window> windowClass: WINDOWS_TO_TEST)
                new PerPixelTranslucentSwing(windowClass).doTest();
    }

    public PerPixelTranslucentSwing(Class windowClass) throws Exception {
        super(windowClass);
    }

    @Override
    public void createSwingComponents() {
        Container contentPane = RootPaneContainer.class.cast(window).getContentPane();
        BorderLayout bl = new BorderLayout(10, 5);
        contentPane.setLayout(bl);

        north = new JButton("North");
        contentPane.add(north, BorderLayout.NORTH);

        JList center = new JList(new String[] {"Center"});
        contentPane.add(center, BorderLayout.CENTER);

        JTextField south = new JTextField("South");
        contentPane.add(south, BorderLayout.SOUTH);

        window.pack();
        window.setVisible(true);

        north.requestFocus();
    }

    @Override
    public void doTest() throws Exception {
        robot.waitForIdle(delay);

        // Check for background translucency
        Rectangle bounds = north.getBounds();
        Point loc = north.getLocationOnScreen();

        Color color = robot.getPixelColor(loc.x + bounds.width / 2, loc.y + bounds.height + 3);
        System.out.println(color);
        if (BG_COLOR.getRGB() != color.getRGB())
            throw new RuntimeException("Background is not translucent (" + color + ")");

        EventQueue.invokeAndWait(this::dispose);
    }
}
