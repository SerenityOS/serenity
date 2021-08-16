/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/*
 * @summary This is a helper class to find the location of a system tray icon,
 *          and skip some OS specific cases in tests.
 * @library /lib/client
 * @build ExtendedRobot SystemTrayIconHelper
 */
public class SystemTrayIconHelper {

    static Frame frame;

    /**
     * Call this method if the tray icon need to be followed in an automated manner
     * This method will be called by automated testcases
     */
    static Point getTrayIconLocation(TrayIcon icon) throws Exception {
        if (icon == null) {
            return null;
        }

        //This is added just in case the tray's native menu is visible.
        //It has to be hidden if visible. For that, we are showing a Frame
        //and clicking on it - the assumption is, the menu will
        //be closed if another window is clicked
        ExtendedRobot robot = new ExtendedRobot();
        try {
           EventQueue.invokeAndWait(() -> {
               frame = new Frame();
               frame.setSize(100, 100);
               frame.setVisible(true);
           });
            robot.mouseMove(frame.getLocationOnScreen().x + frame.getWidth() / 2,
                    frame.getLocationOnScreen().y + frame.getHeight() / 2);
            robot.waitForIdle();
            robot.click();
            EventQueue.invokeAndWait(frame::dispose);
        } catch (Exception e) {
            return null;
        }

        if (System.getProperty("os.name").startsWith("Win")) {
            try {
                // sun.awt.windows.WTrayIconPeer
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                Dimension iconSize = icon.getSize();

                int width = (int) iconSize.getWidth();
                int height = (int) iconSize.getHeight();

                // Some previously created icons may not be removed
                // from tray until mouse move on it. So we glide
                // through the whole tray bar.
                robot.glide((int) screenSize.getWidth(), (int) (screenSize.getHeight()-15), 0, (int) (screenSize.getHeight() - 15), 1, 2);

                BufferedImage screen = robot.createScreenCapture(new Rectangle(screenSize));

                for (int x = (int) (screenSize.getWidth()-width); x > 0; x--) {
                    for (int y = (int) (screenSize.getHeight()-height); y > (screenSize.getHeight()-50); y--) {
                        if (imagesEquals(((BufferedImage)icon.getImage()).getSubimage(0, 0, width, height), screen.getSubimage(x, y, width, height))) {
                            Point point = new Point(x + 5, y + 5);
                            System.out.println("Icon location " + point);
                            return point;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else if (System.getProperty("os.name").startsWith("Mac")) {
            Point2D point2d;
            try {
                // sun.lwawt.macosx.CTrayIcon
                Field f_peer = getField( java.awt.TrayIcon.class, "peer");
                Method m_addExports = Class.forName("java.awt.Helper").getDeclaredMethod("addExports", String.class, java.lang.Module.class);
                m_addExports.invoke(null, "sun.lwawt.macosx", robot.getClass().getModule());


                Object peer = f_peer.get(icon);
                Class<?> superclass = peer.getClass().getSuperclass();
                System.out.println("superclass = " + superclass);
                Field m_getModel = superclass.getDeclaredField("ptr");
                m_getModel.setAccessible(true);
                long model = (Long) m_getModel.get(peer);
                Method m_getLocation = peer.getClass().getDeclaredMethod(
                        "nativeGetIconLocation", new Class[]{Long.TYPE});
                m_getLocation.setAccessible(true);
                point2d = (Point2D)m_getLocation.invoke(peer, new Object[]{model});
                Point po = new Point((int)(point2d.getX()), (int)(point2d.getY()));
                po.translate(10, -5);
                System.out.println("Icon location " + po);
                return po;
            }catch(Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            try {
                // sun.awt.X11.XTrayIconPeer
                Method m_addExports = Class.forName("java.awt.Helper").getDeclaredMethod("addExports", String.class, java.lang.Module.class);
                m_addExports.invoke(null, "sun.awt.X11", robot.getClass().getModule());

                Field f_peer = getField(java.awt.TrayIcon.class, "peer");

                SystemTrayIconHelper.openTrayIfNeeded(robot);

                Object peer = f_peer.get(icon);
                Method m_getLOS = peer.getClass().getDeclaredMethod(
                        "getLocationOnScreen", new Class[]{});
                m_getLOS.setAccessible(true);
                Point point = (Point)m_getLOS.invoke(peer, new Object[]{});
                point.translate(5, 5);
                System.out.println("Icon location " + point);
                return point;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    static Field getField(final Class clz, final String fieldName) {
        Field res = null;
        try {
            res = (Field)AccessController.doPrivileged((PrivilegedExceptionAction) () -> {
                Field f = clz.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f;
            });
        } catch (PrivilegedActionException ex) {
            ex.printStackTrace();
        }
        return res;
    }

    static boolean imagesEquals(BufferedImage img1, BufferedImage img2) {
        for (int x = 0; x < img1.getWidth(); x++) {
            for (int y = 0; y < img1.getHeight(); y++) {
                if (img1.getRGB(x, y) != img2.getRGB(x, y))
                    return false;
            }
        }
        return true;
    }

    static void doubleClick(Robot robot) {
        if (System.getProperty("os.name").startsWith("Mac")) {
            robot.mousePress(InputEvent.BUTTON3_MASK);
            robot.delay(50);
            robot.mouseRelease(InputEvent.BUTTON3_MASK);
        } else {
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.delay(50);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.delay(50);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.delay(50);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
        }
    }

    // Method for skipping some OS specific cases
    static boolean skip(int button) {
        if (System.getProperty("os.name").toLowerCase().startsWith("win")){
            if (button == InputEvent.BUTTON1_MASK){
                // See JDK-6827035
                return true;
            }
        } else if (System.getProperty("os.name").toLowerCase().contains("os x")){
            // See JDK-7153700
            return true;
        }
        return false;
    }

    public static boolean openTrayIfNeeded(Robot robot) {
        String sysv = System.getProperty("os.version");
        System.out.println("System version is " + sysv);
        //Additional step to raise the system try in Gnome 3 in OEL 7
        if(isOel7()) {
            System.out.println("OEL 7 detected");
            GraphicsConfiguration gc = GraphicsEnvironment.
                    getLocalGraphicsEnvironment().getDefaultScreenDevice().
                    getDefaultConfiguration();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            if(insets.bottom > 0) {
                Dimension screenSize = Toolkit.getDefaultToolkit()
                        .getScreenSize();
                robot.mouseMove(screenSize.width - insets.bottom / 2,
                        screenSize.height - insets.bottom / 2);
                robot.delay(50);
                robot.mousePress(InputEvent.BUTTON1_MASK);
                robot.delay(50);
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
                robot.waitForIdle();
                robot.delay(1000);
                System.out.println("Tray is opened");
                return true;
            }
        }
        return false;
    }

    public static boolean isOel7() {
        return System.getProperty("os.name").toLowerCase()
                .contains("linux") && System.getProperty("os.version")
                .toLowerCase().contains("el7");
    }
}
