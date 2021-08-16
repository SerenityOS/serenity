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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;

/*
 * @test
 * @key headful
 * @bug 8013450
 * @summary Check if the window events (Focus and Activation) are triggered correctly
 *          when clicked on visible and clipped areas.
 *
 * Test Description: Check if PERPIXEL_TRANSPARENT Translucency type is supported
 *      by the current platform. Proceed if it is supported. Apply different
 *      types of shapes on a Window. Make it appear with a known background.
 *      Check if mouse events which result in window-activated events are
 *      triggered only within the window's shape and not outside. Repeat this
 *      for Window, Dialog and Frame.
 * Expected Result: If PERPIXEL_TRANSPARENT Translucency type is supported, window should
 *      gain focus and should trigger activated events only when it is clicked on the
 *      visible areas. Events should be delivered to the background window if clicked
 *      on the clipped areas.
 *
 * @author mrkam
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @library /lib/client
 * @build Common ExtendedRobot
 * @run main FocusAWTTest
 */

public class FocusAWTTest extends Common {

    ExtendedRobot robot;
    int dx;
    int dy;
    static final int x = 20;
    static final int y = 400;

    static volatile HashMap<String, Boolean> flags = new HashMap<String, Boolean>();
    static {
        flags.put("backgroundWindowActivated", false);
        flags.put("backgroundWindowDeactivated", false);
        flags.put("backgroundWindowGotFocus", false);
        flags.put("backgroundWindowLostFocus", false);
        flags.put("foregroundWindowGotFocus", false);
        flags.put("foregroundWindowLostFocus", false);
        flags.put("foregroundWindowActivated", false);
        flags.put("foregroundWindowDeactivated", false);
    }

    public static void main(String[] ignored) throws Exception{
        if (checkTranslucencyMode(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT))
            for (Class<Window> windowClass: WINDOWS_TO_TEST) {
                new FocusAWTTest(windowClass).doTest();
            }
    }

    public FocusAWTTest(Class windowClass) throws Exception {
        super(windowClass);
        this.robot = new ExtendedRobot();
        robot.waitForIdle();
        EventQueue.invokeAndWait(() -> {
            dx = background.getX() - x;
            dy = background.getY() - y;
        });
        robot.waitForIdle();
    }

    @Override
    public void initBackgroundFrame() {
        background = new Frame();
        background.setSize(300, 300);
        background.setLocation(x, y);
        background.setFocusable(true);
        background.setFocusableWindowState(true);

        background.addWindowFocusListener(new WindowFocusListener() {
            public void windowGainedFocus(WindowEvent e) { flags.put("backgroundWindowGotFocus", true); }
            public void windowLostFocus(WindowEvent e) { flags.put("backgroundWindowLostFocus", true); }
        });

        background.addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) { flags.put("backgroundWindowActivated", true); }
            public void windowDeactivated(WindowEvent e) { flags.put("backgroundWindowDeactivated", true); }
        });
        background.add(new TextArea());
        background.setVisible(true);
    }

    @Override
    public void initGUI() {
        if (windowClass.equals(Frame.class)) {
            window = new Frame() {
                public void paint(Graphics g) {
                    g.setColor(Color.BLUE);
                    g.fillRect(0, 0, 200, 200);
                }
            };
            ((Frame) window).setUndecorated(true);
        } else if (windowClass.equals(Dialog.class)) {
            window = new Dialog(background) {
                public void paint(Graphics g) {
                    g.setColor(Color.BLUE);
                    g.fillRect(0, 0, 200, 200);
                }
            };
            ((Dialog) window).setUndecorated(true);
        } else {
            window = new Window(background) {
                public void paint(Graphics g) {
                    g.setColor(Color.BLUE);
                    g.fillRect(0, 0, 200, 200);
                }
            };
            window.setFocusable(true);
            window.setFocusableWindowState(true);
        }

        window.setPreferredSize(new Dimension(200, 200));
        window.setLocation(70 + dx, 450 + dy);
        window.setLayout(new BorderLayout());

        window.addWindowFocusListener(new WindowFocusListener() {
            public void windowGainedFocus(WindowEvent e) { flags.put("foregroundWindowGotFocus", true); }
            public void windowLostFocus(WindowEvent e) { flags.put("foregroundWindowLostFocus", true); }
        });

        window.addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) { flags.put("foregroundWindowActivated", true); }
            public void windowDeactivated(WindowEvent e) { flags.put("foregroundWindowDeactivated", true); }
        });

        applyShape();
        window.pack();
        window.setAlwaysOnTop(true);
        window.setVisible(true);
    }

    @Override
    public void doTest() throws Exception {
        super.doTest();
        final Point wls = new Point();
        final Dimension size = new Dimension();
        EventQueue.invokeAndWait(() -> {
            window.requestFocus();
            wls.setLocation(window.getLocationOnScreen());
            window.getSize(size);
        });

        robot.waitForIdle();

        check(wls.x + size.width - 5, wls.y + 5, wls.x + size.width / 3, wls.y + size.height / 3);
        check(wls.x + size.width / 2, wls.y + size.height / 2, wls.x + size.width * 2 / 3, wls.y + size.height * 2 / 3);

        EventQueue.invokeAndWait(() -> {
            background.dispose();
            window.dispose();
        });

        robot.waitForIdle();
    }

    @Override
    public void applyShape() {
        Shape shape;
        Area a = new Area(new Rectangle2D.Float(0, 0, 200, 200));
        GeneralPath gp;
        gp = new GeneralPath();
        gp.moveTo(190, 0);
        gp.lineTo(200, 0);
        gp.lineTo(200, 10);
        gp.lineTo(10, 200);
        gp.lineTo(0, 200);
        gp.lineTo(0, 190);
        gp.closePath();
        a.subtract(new Area(gp));
        shape = a;

        window.setShape(shape);
    }

    private void check(int xb, int yb, int xw, int yw) throws Exception {
        checkClick(xb, yb, "backgroundWindowGotFocus");
        checkClick(xw, yw, "foregroundWindowGotFocus");
        checkClick(xb, yb, "foregroundWindowLostFocus");
        checkClick(xw, yw, "backgroundWindowLostFocus");

        if (window instanceof Dialog || window instanceof Frame) {
            checkClick(xb, yb, "backgroundWindowActivated");
            checkClick(xw, yw, "foregroundWindowActivated");
            checkClick(xb, yb, "foregroundWindowDeactivated");
            checkClick(xw, yw, "backgroundWindowDeactivated");
        }

    }

    private void checkClick(int x, int y, String flag) throws Exception {
        System.out.println("Trying to click point " + x + ", " + y + ", looking for " + flag + " to trigger.");

        flags.put(flag, false);

        robot.mouseMove(x, y);
        robot.click();
        int i = 0;
        while (i < 5000 && !flags.get(flag)) {
            robot.waitForIdle(50);
            i += 50;
        }

        if (!flags.get(flag))
            throw new RuntimeException(flag + " is not triggered for click on point " + x + ", " + y + " for " + windowClass + "!");
    }
}
