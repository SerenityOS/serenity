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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/*
 * @test
 * @key headful
 * @summary Check if swing components present in a window set with opacity less
 *          than 1.0 appears translucent
 * Test Description: Check if TRANSLUCENT Translucency type is supported for the
 *      current platform. Proceed if supported. Show a window containing some swing
 *      components and set it with opacity less than 1.0. Check if the swing components
 *      appear translucent and check if events trigger correctly for the components
 * Expected Result: If TRANSLUCENT Translucency type is supported, the components
 *      should appear translucent showing the background. They should trigger events
 *      correctly
 * @author mrkam
 * @library /lib/client
 * @build Common ExtendedRobot
 * @run main TranslucentWindowClickSwing
 */

public class TranslucentWindowClickSwing extends Common {

    private Component south;
    private Component center;
    private Component north;

    public static void main(String[] args) throws Exception{
        if (checkTranslucencyMode(GraphicsDevice.WindowTranslucency.TRANSLUCENT))
            new TranslucentWindowClickSwing(JWindow.class).doTest();
    }

    public TranslucentWindowClickSwing(Class windowClass) throws Exception {
        super(windowClass, 0.2f, 1.0f, false);
    }

    @Override
    public void createSwingComponents() {
        south = new JButton("South");
        south.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { clicked |= 1 << 2; }
        });
        window.add(south, BorderLayout.SOUTH);

        center = new JList();
        center.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { clicked |= 1 << 1; }
        });
        window.add(center, BorderLayout.CENTER);

        north = new JTextField("North");
        north.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { clicked |= 1 << 0; }
        });
        window.add(north, BorderLayout.NORTH);
    }

    @Override
    public void doTest() throws Exception {
        Point ls;
        robot.waitForIdle();

        ls = north.getLocationOnScreen();
        checkClick(ls.x + north.getWidth() / 3, ls.y + north.getHeight() / 2, 0);

        ls = center.getLocationOnScreen();
        checkClick(ls.x + center.getWidth() / 4, ls.y + center.getHeight() / 4, 1);

        ls = center.getLocationOnScreen();
        checkClick(ls.x + center.getWidth() * 3 / 4, ls.y + center.getHeight() * 3 / 4, 1);

        ls = south.getLocationOnScreen();
        checkClick(ls.x + south.getWidth() * 2 / 3, ls.y + south.getHeight() / 2, 2);

        EventQueue.invokeAndWait(this::dispose);
        robot.waitForIdle();
    }
}
