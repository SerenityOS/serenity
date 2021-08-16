/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.image.BufferedImage;

/*
 * @test
 * @key headful
 * @summary Check if MouseEvents triggered by TrayIcon are visible
 *          by an AWTEventListener added to the Toolkit. It also
 *          checks if all listeners are triggered when AWTEventListeners
 *          and MouseListeners are added.
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @modules java.desktop/java.awt:open
 * @library /java/awt/patchlib
 * @library /lib/client ../
 * @build java.desktop/java.awt.Helper
 * @build ExtendedRobot SystemTrayIconHelper
 * @run main MouseEventMaskTest
 */

public class MouseEventMaskTest {

    TrayIcon icon;
    Robot robot;
    int[] buttonTypes = {
        InputEvent.BUTTON1_MASK,
        InputEvent.BUTTON2_MASK,
        InputEvent.BUTTON3_MASK
    };

    String[] buttonNames = {
        "BUTTON1",
        "BUTTON2",
        "BUTTON3"
    };

    boolean mouseEventTriggered = false;
    boolean mouseMotionEventTriggered = false;
    Object mouseEventLock = new Object();
    Object mouseMotionEventLock = new Object();
    boolean mouseMotionTest, mouseTest;

    public static void main(String[] args) throws Exception {
        if (! SystemTray.isSupported()) {
            System.out.println("SystemTray not supported on the platform under test. " +
                    "Marking the test passed");
        } else {
            if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
                System.err.println("Test can fail if the icon hides to a tray icons pool " +
                        "in Windows 7, which is behavior by default.\n" +
                        "Set \"Right mouse click\" -> \"Customize notification icons\" -> " +
                        "\"Always show all icons and notifications on the taskbar\" true " +
                        "to avoid this problem. Or change behavior only for Java SE tray " +
                        "icon and rerun test.");
            } else if (SystemTrayIconHelper.isOel7()) {
                return;
            }
            new MouseEventMaskTest().doTest();
        }
    }

    public MouseEventMaskTest() throws Exception{
        EventQueue.invokeAndWait(this::initializeGUI);
    }

    void initializeGUI() {

        SystemTray tray = SystemTray.getSystemTray();
        icon = new TrayIcon(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), "Sample Icon");

        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (mouseTest) {
                if (! event.getSource().getClass().getName().contains("Canvas")) {
                    if (!icon.equals(event.getSource()))
                        throw new RuntimeException("FAIL: MouseEvent not triggered for icon " + event);

                    mouseEventTriggered = true;
                    synchronized (mouseEventLock) {
                        try {
                            mouseEventLock.notifyAll();
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (mouseMotionTest) {
                if (! event.getSource().getClass().getName().contains("Canvas")) {
                    if (!icon.equals(event.getSource()))
                        throw new RuntimeException("FAIL: MouseMotionEvent not triggered for icon " + event);

                    mouseMotionEventTriggered = true;
                    synchronized (mouseMotionEventLock) {
                        try {
                            mouseMotionEventLock.notifyAll();
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }, AWTEvent.MOUSE_MOTION_EVENT_MASK);

        try {
            tray.add(icon);
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }

    void doTest() throws Exception {

        robot = new Robot();

        Point iconPosition = SystemTrayIconHelper.getTrayIconLocation(icon);
        if (iconPosition == null)
            throw new RuntimeException("Unable to find the icon location!");

        robot.mouseMove(iconPosition.x, iconPosition.y);
        robot.waitForIdle();

        for (int i = 0; i < buttonTypes.length; i++) {
            System.out.println("Verify button "+buttonTypes[i]);
            mouseTest = true;
            mouseEventTriggered = false;
            robot.mousePress(buttonTypes[i]);

            if (! mouseEventTriggered) {
                synchronized (mouseEventLock) {
                    try {
                        mouseEventLock.wait(3000);
                    } catch (Exception e) {
                    }
                }
            }
            if (! mouseEventTriggered)
                if (! SystemTrayIconHelper.skip(buttonTypes[i]) )
                    throw new RuntimeException("FAIL: AWTEventListener not notified when " +
                        buttonNames[i] + " pressed on TrayIcon");

            mouseEventTriggered = false;
            robot.mouseRelease(buttonTypes[i]);
            if (! mouseEventTriggered) {
                synchronized (mouseEventLock) {
                    try {
                        mouseEventLock.wait(3000);
                    } catch (Exception e) {
                    }
                }
            }

            if (! mouseEventTriggered)
                throw new RuntimeException("FAIL: AWTEventListener not notified when " +
                        buttonNames[i] + " released on TrayIcon");
        }

        mouseMotionTest = true;
        mouseTest = false;
        mouseMotionEventTriggered = false;

        for (int i = 0; i < 20; i++) {
            robot.mouseMove(iconPosition.x + i, iconPosition.y);
            robot.delay(25);
        }
        if (! mouseMotionEventTriggered) {
            synchronized (mouseMotionEventLock) {
                try {
                    mouseMotionEventLock.wait(3000);
                } catch (Exception e) {
                }
            }
        }
        if (! mouseMotionEventTriggered)
            if (! SystemTrayIconHelper.skip(0) )
                throw new RuntimeException("FAIL: AWTEventListener not notified when " +
                        "mouse moved");
    }
}
