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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.*;

/*
 * @test
 * @key headful
 * @summary Check if a window set with shape appears in the expected shape
 *
 * Test Description: Check if PERPIXEL_TRANSPARENT Translucency type is supported
 *      by the current platform. Proceed if it is supported. Apply different
 *      types of shapes on a Window. Make it appear with a known background. Use
 *      get pixel color to check whether it appears as expected. Repeat this for
 *      Window, Dialog and Frame.
 * Expected Result: If PERPIXEL_TRANSPARENT Translucency type is supported, the
 *      window should appear with the expected shape.
 *
 * @author mrkam
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @library /lib/client
 * @build Common ExtendedRobot
 * @run main SetShape
 */

public class SetShape extends Common {

    static int[][] pointsToCheck;
    static Shape shape;

    public static void main(String[] args) throws Exception {
        if (checkTranslucencyMode(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT)) {
            for (int i = 0; i < 6; i++) {
                System.out.println("Checking shape "+i);
                Area area;
                GeneralPath path;
                switch (i) {
                    case 0:
                        path = new GeneralPath();
                        path.moveTo(0, 40);
                        path.lineTo(40, 0);
                        path.lineTo(110, 0);
                        path.lineTo(150, 40);
                        path.lineTo(150, 110);
                        path.lineTo(110, 150);
                        path.lineTo(40, 150);
                        path.lineTo(0, 110);
                        path.closePath();
                        shape = path;
                        pointsToCheck = new int[][]{
                                // inside shape
                                {230, 240}, {286, 332}, {314, 267}, {220, 327}, {223, 246},
                                {229, 274}, {335, 257}, {231, 278}, {317, 299}, {266, 236},
                                // outside shape
                                {340, 353}, {373, 320}, {330, 220}, {384, 300}, {349, 406},
                                {213, 355}, {361, 260}, {399, 251}, {201, 374}, {199, 257}
                        };
                        break;
                    case 1:
                        area = new Area();
                        area.add(new Area(new Rectangle2D.Float(50, 0, 100, 150)));
                        area.add(new Area(new Rectangle2D.Float(0, 50, 200, 50)));
                        area.add(new Area(new Ellipse2D.Float(0, 0, 100, 100)));
                        area.add(new Area(new Ellipse2D.Float(0, 50, 100, 100)));
                        area.add(new Area(new Ellipse2D.Float(100, 0, 100, 100)));
                        area.add(new Area(new Ellipse2D.Float(100, 50, 100, 100)));
                        shape = area;
                        pointsToCheck = new int[][]{
                                // inside shape
                                {306, 314}, {296, 266}, {276, 335}, {380, 253}, {224, 333},
                                {396, 305}, {365, 278}, {214, 331}, {289, 349}, {367, 345},
                                // outside shape
                                {365, 424}, {391, 391}, {346, 413}, {261, 398}, {348, 399},
                                {282, 400}, {386, 215}, {399, 369}, {213, 401}, {387, 215}
                        };
                        break;
                    case 2:
                        path = new GeneralPath();
                        path.moveTo(100, 0);
                        double angle = -Math.PI / 2;
                        double angle_step = Math.PI * 2 / 8;
                        for (int c = 0; c < 8; c++, angle += angle_step) {
                            path.lineTo(100 + 100 * Math.cos(angle), 100 + 100 * Math.sin(angle));
                            path.lineTo(100 + 40 * Math.cos(angle + angle_step / 2), 100 + 40 * Math.sin(angle + angle_step / 2));
                        }
                        path.closePath();
                        shape = path;
                        pointsToCheck = new int[][]{
                                // inside shape
                                {246, 257}, {300, 314}, {255, 347}, {291, 364}, {287, 320},
                                {319, 276}, {269, 345}, {325, 291}, {289, 271}, {273, 339},
                                // outside shape
                                {373, 267}, {269, 229}, {390, 326}, {204, 216}, {379, 408},
                                {375, 330}, {296, 213}, {367, 340}, {376, 409}, {378, 308}
                        };
                        break;
                    case 3:
                        area = new Area();
                        area.add(new Area(new Rectangle2D.Float(-15, 85, 230, 30)));
                        area.transform(AffineTransform.getRotateInstance(-Math.PI / 4, 100, 100));
                        shape = area;
                        pointsToCheck = new int[][]{
                                // inside shape
                                {240, 366}, {254, 338}, {342, 244}, {264, 344}, {343, 240},
                                {292, 303}, {225, 374}, {263, 348}, {329, 290}, {278, 327},
                                // outside shape
                                {353, 289}, {334, 377}, {391, 354}, {266, 358}, {280, 364},
                                {232, 225}, {327, 309}, {375, 208}, {397, 292}, {204, 335}
                        };
                        break;
                    case 4:
                        area = new Area();
                        area.add(new Area(new Arc2D.Float(0, -100, 400, 400, 155, 50, Arc2D.PIE)));
                        area.subtract(new Area(new Ellipse2D.Float(70, 115, 20, 20)));
                        area.subtract(new Area(new Ellipse2D.Float(30, 90, 18, 18)));
                        area.subtract(new Area(new Ellipse2D.Float(17, 50, 30, 30)));
                        area.subtract(new Area(new Ellipse2D.Float(26, 124, 26, 26)));
                        area.subtract(new Area(new Ellipse2D.Float(100, 85, 25, 25)));
                        area.subtract(new Area(new Ellipse2D.Float(135, 100, 14, 14)));
                        shape = area;
                        pointsToCheck = new int[][]{
                                // inside shape
                                {225, 286}, {296, 276}, {318, 269}, {211, 357}, {295, 327},
                                {207, 300}, {322, 265}, {319, 262}, {259, 294}, {261, 250},
                                // outside shape
                                {322, 303}, {330, 367}, {302, 395}, {227, 251}, {263, 382},
                                {228, 383}, {280, 366}, {294, 248}, {316, 349}, {313, 294}
                        };
                        break;
                    case 5:
                        area = new Area();
                        area.add(new Area(new Rectangle2D.Float(0, 0, 90, 90)));
                        area.add(new Area(new Rectangle2D.Float(100, 0, 100, 200)));
                        area.add(new Area(new Ellipse2D.Float(0, 100, 100, 100)));
                        shape = area;
                        pointsToCheck = new int[][]{
                                // inside shape
                                {275, 345}, {358, 327}, {373, 374}, {273, 331}, {251, 234},
                                {285, 356}, {360, 287}, {319, 343}, {232, 210}, {323, 323},
                                // outside shape
                                {219, 291}, {270, 302}, {296, 383}, {298, 203}, {228, 293},
                                {276, 300}, {292, 294}, {293, 216}, {298, 331}, {228, 295}
                        };
                        break;
                    default:
                        break;
                }

                for (Class<Window> windowClass : WINDOWS_TO_TEST) {
                    new SetShape(windowClass).doTest();
                }
            }
        }
    }

    public SetShape(Class windowClass) throws Exception {
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
        window.setBackground(FG_COLOR);
        window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                window.setShape(shape);
            }
        });
        window.setSize(200, 200);
        window.setLocation(2*dl, 2*dl);
        window.setVisible(true);

        System.out.println("Checking " + window.getClass().getName() + "...");
    }

    @Override
    public void doTest() throws Exception {
        robot.waitForIdle();

        final int COUNT_TARGET = 10;
        for(int i = 0; i < COUNT_TARGET * 2; i++) {
            int x = pointsToCheck[i][0];
            int y = pointsToCheck[i][1];
            boolean inside = i < COUNT_TARGET;
            Color c = robot.getPixelColor(x, y);
            System.out.println("checking " + x + ", " + y + ", color = " + c);
            boolean matchToForeground = FG_COLOR.equals(c);

            if (!inside && matchToForeground || inside && !matchToForeground) {
                System.err.println("Checking for transparency failed: point: " +
                        x + ", " + y +
                        ", color = " + c + (inside ? " is not of " : " is of un") +
                        "expected foreground color " + FG_COLOR);
                final Frame[] f = new Frame[1];
                EventQueue.invokeAndWait(() -> {
                    f[0] = new Frame();
                    f[0].setUndecorated(true);
                    f[0].setBackground(Color.YELLOW);
                    f[0].setPreferredSize(new Dimension(2, 2));
                    f[0].pack();
                    f[0].setVisible(true);
                });
                robot.delay(1000);
                EventQueue.invokeAndWait(() -> {
                    f[0].setLocation(x, y);
                });
                robot.delay(1000);
            }
        }
        EventQueue.invokeAndWait(window::dispose);
        EventQueue.invokeAndWait(background::dispose);

        robot.waitForIdle();
    }

    @Override
    public void applyShape() { window.setShape(shape); }

}
