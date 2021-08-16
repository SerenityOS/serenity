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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/*
 * @test
 * @key headful
 * @summary Check if components present in a window set with opacity less than 1.0
 *          triggers events correctly
 *
 * Test Description: Check if TRANSLUCENT Translucency type is supported on the
 *      current platform. Proceed if supported. Show a window which contains some
 *      components and set with opacity less than 1.0. Another Window having a
 *      canvas component drawn with an image can be used as the background for the
 *      test window. Click on the components present in the window and check if
 *      events trigger correctly.
 * Expected Result: The components should trigger events correctly
 *
 * @author mrkam
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @library /lib/client
 * @build Common ExtendedRobot
 * @run main TranslucentWindowClick
 */
public class TranslucentWindowClick extends Common {

    private Component south;
    private Component center;
    private Component north;

    volatile int clicked;

    public static void main(String[] args) throws Exception{
        if (checkTranslucencyMode(GraphicsDevice.WindowTranslucency.TRANSLUCENT))
            new TranslucentWindowClick(Window.class).doTest();
    }

    public TranslucentWindowClick(Class windowClass) throws Exception {
        super(windowClass);
    }

    @Override
    public void initGUI() {
        if (windowClass.equals(Frame.class)) {
            window = new Frame();
            ((Frame) window).setUndecorated(true);
        } else  if (windowClass.equals(Dialog.class)) {
            window = new Dialog(background);
            ((Dialog) window).setUndecorated(true);
        } else {
            window = new Window(background);
        }

        window.setPreferredSize(new Dimension(200, 200));
        window.setLocation(2 * dl, 2 * dl);
        window.setLayout(new BorderLayout());
        window.setBackground(FG_COLOR);

        south = new Button("South");
        south.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { clicked |= 1 << 2; }
        });
        window.add(south, BorderLayout.SOUTH);

        center = new List(5);
        center.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { clicked |= 1 << 1; }
        });
        window.add(center, BorderLayout.CENTER);

        north = new TextField("North");
        north.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { clicked |= 1 << 0; }
        });
        window.add(north, BorderLayout.NORTH);
        window.setOpacity(0.2f);
        window.pack();
        window.setVisible(true);

        System.out.println("Checking " + window.getClass().getName() + "...");
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

        EventQueue.invokeAndWait(() -> {
            background.dispose();
            window.dispose();
        });

        robot.waitForIdle();
    }

    @Override
    public void applyShape() { }

    void checkClick(int x, int y, int flag) throws Exception {

        System.out.println("Trying to click point " + x + ", " + y + ", looking for " + flag + " flag to trigger.");

        clicked = 0;
        robot.mouseMove(x, y);
        robot.click();

        for (int i = 0; i < 100; i++)
            if ((clicked & (1 << flag)) == 0)
                robot.delay(50);
            else
                break;

        if ((clicked & (1 << flag)) == 0)
            throw new RuntimeException("FAIL: Flag " + flag + " is not triggered for point " + x + ", " + y + "!");
    }
}
