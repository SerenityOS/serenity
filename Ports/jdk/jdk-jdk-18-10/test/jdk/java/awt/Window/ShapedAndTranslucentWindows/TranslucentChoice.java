/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Check if a Choice present in a window set with opacity less than 1.0
 *          shows a translucent drop down
 *
 * Test Description: Check if TRANSLUCENT Translucency type is supported on the
 *      current platform. Proceed if supported. Show a window which contains an
 *      awt Choice and set with opacity less than 1.0. Another Window having a
 *      canvas component drawn with an image can be used as the background for
 *      the test window. Click on the ComboBox to show the drop down. Check if
 *      the drop down appears translucent. Repeat this for Window, Dialog and
 *      Frame.
 * Expected Result: If TRANSLUCENT Translucency type is supported, the drop down
 *      should appear translucent.
 *
 * @author mrkam
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @library /lib/client
 * @build Common ExtendedRobot
 * @run main TranslucentChoice
 */

public class TranslucentChoice extends Common {

    Choice south;
    Component center;
    Component north;
    volatile int clicked;

    public static void main(String[] ignored) throws Exception {
        if (checkTranslucencyMode(GraphicsDevice.WindowTranslucency.TRANSLUCENT))
            for (Class<Window> windowClass: WINDOWS_TO_TEST){
                new TranslucentChoice(windowClass).doTest();
            }
    }

    public TranslucentChoice(Class windowClass) throws Exception {
        super(windowClass);
    }

    @Override
    public void initBackgroundFrame() {
        super.initBackgroundFrame();
        background.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                clicked |= 1 << 0;
            }
        });
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

        window.setBackground(FG_COLOR);
        north = new Button("north");
        window.add(north, BorderLayout.NORTH);

        center = new List(5);
        window.add(center, BorderLayout.CENTER);

        Choice choice = new Choice();
        for(int i = 0; i < 20; i++) {
            choice.add("item " + i);
        }
        south = choice;

        south.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                clicked |= 1 << 1;
            }
        });

        window.add(south, BorderLayout.SOUTH);
        window.setAlwaysOnTop(true);
        window.setOpacity(0.3f);
        window.setLocation(2 * dl, 2 * dl);
        window.setSize(255, 255);
        window.pack();
        window.setVisible(true);

        System.out.println("Checking " + window.getClass().getName() + "...");
    }

    @Override
    public void doTest() throws Exception {

        Point ls = window.getLocationOnScreen();
        clicked = 0;

        robot.waitForIdle();

        checkClick(ls.x + window.getWidth() / 2, ls.y - 5, 0);

        robot.waitForIdle(2000);

        ls = south.getLocationOnScreen();

        Point p1 = new Point((int) (ls.x + south.getWidth() * 0.75), ls.y + south.getHeight() * 3);
        Point p2 = new Point((int) (ls.x + south.getWidth() * 0.75), ls.y - south.getHeight() * 2);
        Color c1 = robot.getPixelColor(p1.x, p1.y);
        Color c2 = robot.getPixelColor(p2.x, p2.y);

        checkClick(ls.x + south.getWidth() / 2, ls.y + south.getHeight() / 2, 1);

        robot.waitForIdle(2000);

        Color c1b = robot.getPixelColor(p1.x, p1.y);
        Color c2b = robot.getPixelColor(p2.x, p2.y);

        if (!aproximatelyEquals(c1, c1b) && !aproximatelyEquals(south.getBackground(), c1b))
            throw new RuntimeException("Check for opaque drop down failed. Before click: " + c1 + ", after click: " + c1b + ", expected is " + south.getBackground());

        if (!aproximatelyEquals(c2,c2b) && !aproximatelyEquals(south.getBackground(), c2b))
            throw new RuntimeException("Check for opaque drop down failed. Before click: " + c2 + ", after click: " + c2b + ", expected is " + south.getBackground());

        EventQueue.invokeAndWait(() -> {
            background.dispose();
            window.dispose();
        });

        robot.waitForIdle();
    }

    boolean aproximatelyEquals(Color c1, Color c2) {
        return ((Math.abs(c1.getRed()-c2.getRed())/256.0 +
                Math.abs(c1.getGreen()-c2.getGreen())/256.0 +
                Math.abs(c1.getBlue()-c2.getBlue())/256.0)) / 3 < 0.02;
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
