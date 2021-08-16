/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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

/*
  @test
  @key headful
  @bug 6988428
  @summary Tests whether shape is always set
  @author anthony.petrov@oracle.com: area=awt.toplevel
  @run main ShapeNotSetSometimes
*/


import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.geom.*;


public class ShapeNotSetSometimes {

    private Frame backgroundFrame;
    private Frame window;
    private static final Color BACKGROUND_COLOR = Color.BLUE;
    private Shape shape;
    private int[][] pointsToCheck;

    private static Robot robot;

    public ShapeNotSetSometimes() throws Exception {
        EventQueue.invokeAndWait(this::initializeGUI);
        robot.waitForIdle();
    }

    private void initializeGUI() {
        backgroundFrame = new BackgroundFrame();
        backgroundFrame.setUndecorated(true);
        backgroundFrame.setSize(300, 300);
        backgroundFrame.setLocation(20, 400);
        backgroundFrame.setVisible(true);

        shape = null;
        String shape_name = null;
        Area a;
        GeneralPath gp;
        shape_name = "Rounded-corners";
        a = new Area();
        a.add(new Area(new Rectangle2D.Float(50, 0, 100, 150)));
        a.add(new Area(new Rectangle2D.Float(0, 50, 200, 50)));
        a.add(new Area(new Ellipse2D.Float(0, 0, 100, 100)));
        a.add(new Area(new Ellipse2D.Float(0, 50, 100, 100)));
        a.add(new Area(new Ellipse2D.Float(100, 0, 100, 100)));
        a.add(new Area(new Ellipse2D.Float(100, 50, 100, 100)));
        shape = a;
        pointsToCheck = new int[][] {
            // inside shape
            {106, 86}, {96, 38}, {76, 107}, {180, 25}, {24, 105},
            {196, 77}, {165, 50}, {14, 113}, {89, 132}, {167, 117},
            // outside shape
            {165, 196}, {191, 163}, {146, 185}, {61, 170}, {148, 171},
            {82, 172}, {186, 11}, {199, 141}, {13, 173}, {187, 3}
        };

        window = new TestFrame();
        window.setUndecorated(true);
        window.setSize(200, 200);
        window.setLocation(70, 450);
        window.setShape(shape);
        window.setVisible(true);

        System.out.println("Checking " + window.getClass().getSuperclass().getName() + " with " + shape_name + " shape (" + window.getShape() + ")...");
    }

    class BackgroundFrame extends Frame {

        @Override
        public void paint(Graphics g) {

            g.setColor(BACKGROUND_COLOR);
            g.fillRect(0, 0, 300, 300);

            super.paint(g);
        }
    }

    class TestFrame extends Frame {

        @Override
        public void paint(Graphics g) {

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 200, 200);

            super.paint(g);
        }
    }

    public static void main(String[] args) throws Exception {
        robot = new Robot();

        for(int i = 0; i < 50; i++) {
            System.out.println("Attempt " + i);
            new ShapeNotSetSometimes().doTest();
        }
    }

    private void doTest() throws Exception {
        Point wls = backgroundFrame.getLocationOnScreen();

        robot.mouseMove(wls.x + 5, wls.y + 5);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(10);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(500);

        EventQueue.invokeAndWait(window::requestFocus);

        robot.waitForIdle();
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            // ignore this one
        }

        // check transparency
        final int COUNT_TARGET = 10;

        // checking outside points only
        for(int i = COUNT_TARGET; i < COUNT_TARGET * 2; i++) {
            int x = pointsToCheck[i][0];
            int y = pointsToCheck[i][1];
            boolean inside = i < COUNT_TARGET;
            Color c = robot.getPixelColor(window.getX() + x, window.getY() + y);
            System.out.println("checking " + x + ", " + y + ", color = " + c);
            if (inside && BACKGROUND_COLOR.equals(c) || !inside && !BACKGROUND_COLOR.equals(c)) {
                System.out.println("window.getX() = " + window.getX() + ", window.getY() = " + window.getY());
                System.err.println("Checking for transparency failed: point: " +
                        (window.getX() + x) + ", " + (window.getY() + y) +
                        ", color = " + c + (inside ? " is of un" : " is not of ") +
                        "expected background color " + BACKGROUND_COLOR);
                throw new RuntimeException("Test failed. The shape has not been applied.");
            }
        }

        EventQueue.invokeAndWait(new Runnable() {
            public void run() {
                backgroundFrame.dispose();
                window.dispose();
            }
        });
    }
}
