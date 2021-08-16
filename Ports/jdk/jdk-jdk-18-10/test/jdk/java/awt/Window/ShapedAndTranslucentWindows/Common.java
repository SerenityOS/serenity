/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.event.*;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;


/*
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 */
public abstract class Common {

    ExtendedRobot robot;
    Class<? extends Frame> windowClass;
    Frame background;
    BufferedImage foreground;
    Window window;
    Container componentsContainer;

    float opacity = 1.0f;
    static final int STATIC_STEP = 30;
    static final int STATIC_WIDTH = 25;
    static final int STATIC_BLOCKS = 30;
    static final Color BG_COLOR = Color.BLUE;
    static final Color FG_COLOR = Color.RED;
    static final int delay = 55000;
    static final SecureRandom random = new SecureRandom();
    static final int dl = 100;
    static final Class[] WINDOWS_TO_TEST = { Window.class, Frame.class, Dialog.class };

    public Common(Class windowClass, float opacity) throws Exception{
        this.opacity = opacity;
        robot = new ExtendedRobot();
        this.windowClass = windowClass;
        EventQueue.invokeAndWait(this::initBackgroundFrame);
        EventQueue.invokeAndWait(this::initGUI);
    }

    public Common(Class windowClass) throws Exception{
        this(windowClass, 1.0f);
    }

    public void doTest() throws Exception {
        robot.waitForIdle(delay);
    };

    public void dispose() {
        window.dispose();
        background.dispose();
    }

    public abstract void applyShape();

    public void applyDynamicShape() {
        final Area a = new Area();
        Dimension size = window.getSize();
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                a.add(new Area(new Rectangle2D.Double(
                        x * size.getWidth() / 17*6, y * size.getHeight() / 17*6,
                        size.getWidth() / 17*5, size.getHeight() / 17*5)));
            }
        }
        window.setShape(a);
    }

    public void applyStaticShape() {
        final Area a = new Area();
        for (int x = 0; x < STATIC_BLOCKS; x++) {
            for (int y = 0; y < STATIC_BLOCKS; y++) {
                a.add(new Area(new Rectangle2D.Float(
                        x*STATIC_STEP, y*STATIC_STEP,
                        STATIC_WIDTH, STATIC_WIDTH)));
            }
        }
        window.setShape(a);
    }

    public BufferedImage getForegroundWindow() throws Exception {
        final BufferedImage f[] = new BufferedImage[1];
        EventQueue.invokeAndWait( () -> {
            f[0] = new BufferedImage(window.getWidth(),
                    window.getHeight(), BufferedImage.TYPE_INT_RGB);
            window.printAll(f[0].createGraphics());
        });
        robot.waitForIdle(delay);
        return f[0];
    }

    public static boolean checkTranslucencyMode(GraphicsDevice.WindowTranslucency mode) {

        if (!GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .isWindowTranslucencySupported(mode)){
            System.out.println(mode+" translucency mode isn't supported");
            return false;
        } else {
            return true;
        }

    }

    public void applyAppDragNResizeSupport() {
        MouseAdapter m = new MouseAdapter() {

            private Point dragOrigin = null;
            private Dimension origSize = null;
            private Point origLoc = null;
            private boolean left = false;
            private boolean top = false;
            private boolean bottom = false;
            private boolean right = false;

            public void mousePressed(MouseEvent e) {
                dragOrigin = e.getLocationOnScreen();
                origSize = window.getSize();
                origLoc = window.getLocationOnScreen();
                right = (origLoc.x + window.getWidth() - dragOrigin.x) < 5;
                left = !right && dragOrigin.x - origLoc.x < 5;
                bottom = (origLoc.y + window.getHeight() - dragOrigin.y) < 5;
                top = !bottom && dragOrigin.y - origLoc.y < 5;
            }

            public void mouseReleased(MouseEvent e) { resize(e); }
            public void mouseDragged(MouseEvent e) { resize(e); }

            void resize(MouseEvent e) {
                Point dragDelta = e.getLocationOnScreen();
                dragDelta.translate(-dragOrigin.x, -dragOrigin.y);
                Point newLoc = new Point(origLoc);
                newLoc.translate(dragDelta.x, dragDelta.y);
                Dimension newSize = new Dimension(origSize);
                if (left || right) {
                    newSize.width += right ? dragDelta.x : -dragDelta.x;
                }
                if (top || bottom) {
                    newSize.height += bottom ? dragDelta.y : -dragDelta.y;
                }
                if (right || (top || bottom) && !left) {
                    newLoc.x = origLoc.x;
                }
                if (bottom || (left || right) && !top) {
                    newLoc.y = origLoc.y;
                }
                window.setBounds(newLoc.x, newLoc.y, newSize.width, newSize.height);
            }
        };
        for (Component comp : window.getComponents()) {
            comp.addMouseListener(m);
            comp.addMouseMotionListener(m);
        }

        window.addMouseListener(m);
        window.addMouseMotionListener(m);
    }

    public void checkTranslucentShape() throws Exception {
        foreground = getForegroundWindow();
        Point[] points = new Point[4];

        Dimension size = window.getSize();
        Point location = window.getLocationOnScreen();

        points[0] = new Point(20, 20);
        points[1] = new Point(20, size.height-20);
        points[2] = new Point(size.width-20, 20);
        points[3] = new Point(size.width-20, size.height-20);

        for (Point p : points){
            p.translate(location.x, location.y);
            Color actual = robot.getPixelColor(p.x, p.y);
            if (actual.equals(BG_COLOR)|| actual.equals(FG_COLOR))
                throw new RuntimeException("Error in point "+p+": "+actual+" equals to foreground or background color");
            else
                System.out.println("OK with foreground point "+p);
        }
    }

    public void checkStaticShape() throws Exception {
        Point[] points = new Point[4];

        Dimension size = window.getSize();
        int xFactor = (int) Math.floor(size.getWidth()/STATIC_STEP)-1;
        int yFactor = (int) Math.floor(size.getHeight()/STATIC_STEP)-1;

        // background
        points[0] = new Point((STATIC_STEP+STATIC_WIDTH)/2, (STATIC_STEP+STATIC_WIDTH)/2);
        points[1] = new Point(STATIC_STEP*xFactor+(STATIC_STEP+STATIC_WIDTH)/2, STATIC_STEP*yFactor+(STATIC_STEP+STATIC_WIDTH)/2);
        points[2] = new Point((STATIC_STEP+STATIC_WIDTH)/2, STATIC_STEP*yFactor+(STATIC_STEP+STATIC_WIDTH)/2);
        points[3] = new Point(STATIC_STEP*xFactor+(STATIC_STEP+STATIC_WIDTH)/2, (STATIC_STEP+STATIC_WIDTH)/2);
        checkShape(points, true);

        // foreground
        if (opacity < 1.0f){
            checkTranslucentShape();
        } else {
            points[0] = new Point((STATIC_WIDTH) / 2, (STATIC_WIDTH) / 2);
            points[1] = new Point(STATIC_STEP * xFactor + (STATIC_WIDTH) / 2, STATIC_STEP * yFactor + (STATIC_WIDTH) / 2);
            points[2] = new Point((STATIC_WIDTH) / 2, STATIC_STEP * yFactor + (STATIC_WIDTH) / 2);
            points[3] = new Point(STATIC_STEP * xFactor + (STATIC_WIDTH) / 2, (STATIC_WIDTH) / 2);
            checkShape(points, false);
        }
    }

    public void checkDynamicShape() throws Exception {
        Point[] points = new Point[4];

        Dimension size = window.getSize();

        int blockSizeX = (int) (size.getWidth() / 17);
        int blockSizeY = (int) (size.getHeight() / 17);

        // background
        points[0] = new Point((int) (blockSizeX * 5.5), (int) (blockSizeY * 5.5));
        points[1] = new Point((int) (size.getWidth() - blockSizeX * 5.5), (int) (size.getHeight() - blockSizeY * 5.5));
        points[2] = new Point((int) (blockSizeX * 5.5), (int) (size.getHeight() - blockSizeY * 5.5));
        points[3] = new Point((int) (size.getWidth() - blockSizeX * 5.5), (int) (blockSizeY * 5.5));
        checkShape(points, true);

        // foreground
        if (opacity < 1.0f){
            checkTranslucentShape();
        } else {
            points[0] = new Point(3 * blockSizeX, 3 * blockSizeY);
            points[1] = new Point(14 * blockSizeX, 14 * blockSizeY);
            points[2] = new Point(3 * blockSizeX, 14 * blockSizeY);
            points[3] = new Point(14 * blockSizeX, 3 * blockSizeY);
            checkShape(points, false);
        }
    }

    public void checkShape(Point[] points, boolean areBackgroundPoints) throws Exception {

        Point location = window.getLocationOnScreen();

        for (Point p : points) {
            p.translate(location.x, location.y);
            if (areBackgroundPoints) {
                if (!robot.getPixelColor(p.x, p.y).equals(BG_COLOR))
                    throw new RuntimeException("Background point " + p + " color " + robot.getPixelColor(p.x, p.y) +
                            " does not equal to background color " + BG_COLOR);
                else
                    System.out.println("OK with background point " + p);
            } else {
                if (robot.getPixelColor(p.x, p.y).equals(BG_COLOR))
                    throw new RuntimeException("Foreground point " + p +
                            " equals to background color " + BG_COLOR);
                else
                    System.out.println("OK with foreground point " + p);
            }
        }
    }

    public void initBackgroundFrame() {
        background = new Frame();
        background.setUndecorated(true);
        background.setBackground(BG_COLOR);
        background.setSize(500, 500);
        background.setLocation(dl, dl);
        background.setVisible(true);
    }

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
        componentsContainer = new Panel();
        window.add(componentsContainer, BorderLayout.CENTER);
        window.setLocation(2 * dl, 2 * dl);
        window.setSize(255, 255);
        if (opacity < 1.0f)
            window.setOpacity(opacity);
        window.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                applyShape();
            }
        });
        applyShape();
        window.setVisible(true);
        applyAppDragNResizeSupport();
        window.toFront();
    }
}
