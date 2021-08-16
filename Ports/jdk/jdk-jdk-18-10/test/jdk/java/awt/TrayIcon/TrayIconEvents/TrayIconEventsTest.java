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
 * @summary Check for MouseEvents with all mouse buttons
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @modules java.desktop/java.awt:open
 * @library /java/awt/patchlib
 * @library /lib/client ../
 * @build java.desktop/java.awt.Helper
 * @build ExtendedRobot SystemTrayIconHelper
 * @run main TrayIconEventsTest
 */

public class TrayIconEventsTest {

    private static boolean isOEL7;
    TrayIcon icon;
    ExtendedRobot robot;

    boolean actionPerformed = false;
    Object actionLock = new Object();
    Object pressLock = new Object();
    Object releaseLock = new Object();
    Object clickLock = new Object();
    Object moveLock = new Object();

    String caption = "Sample Icon";
    boolean mousePressed = false;
    boolean mouseReleased = false;
    boolean mouseClicked = false;
    boolean mouseMoved = false;

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

    public static void main(String[] args) throws Exception {
        if (! SystemTray.isSupported()) {
            System.out.println("SystemTray not supported on the platform under test. " +
                               "Marking the test passed");
        } else {
            if (System.getProperty("os.name").toLowerCase().startsWith("win"))
                System.err.println("Test can fail if the icon hides to a tray icons pool " +
                        "in Windows 7, which is behavior by default.\n" +
                        "Set \"Right mouse click\" -> \"Customize notification icons\" -> " +
                        "\"Always show all icons and notifications on the taskbar\" true " +
                        "to avoid this problem. Or change behavior only for Java SE " +
                        "tray icon.");
            isOEL7 = SystemTrayIconHelper.isOel7();
            new TrayIconEventsTest().doTest();
        }
    }

    public TrayIconEventsTest() throws Exception {
        robot = new ExtendedRobot();
        EventQueue.invokeAndWait(this::initializeGUI);
    }

    private void initializeGUI(){
        SystemTray tray = SystemTray.getSystemTray();
        icon = new TrayIcon(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), caption);
        icon.addActionListener(event -> {
            actionPerformed = true;
            synchronized (actionLock) {
                try {

                    actionLock.notifyAll();
                } catch (Exception e) {
                }
            }
        });
        icon.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent event) {
                mousePressed = true;
                Point p = event.getPoint();
                if (p.x != event.getX() || p.y != event.getY())
                    throw new RuntimeException("FAIL: MouseEvent.getPoint() did " +
                            "not return the same value as getX/getY " +
                            "for mousePressed");

                if (! icon.equals(event.getSource()))
                    throw new RuntimeException("FAIL: mousePressed: MouseEvent.getSource " +
                            "did not return TrayIcon object");

                synchronized (pressLock) {
                    try {
                        pressLock.notifyAll();
                    } catch (Exception e) {
                    }
                }
            }

            public void mouseReleased(MouseEvent event) {
                mouseReleased = true;
                Point p = event.getPoint();
                if (p.x != event.getX() || p.y != event.getY())
                    throw new RuntimeException("FAIL: MouseEvent.getPoint() did " +
                            "not return the same value as getX/getY " +
                            "for mouseReleased");

                if (! icon.equals(event.getSource()))
                    throw new RuntimeException("FAIL: mouseReleased: MouseEvent.getSource " +
                            "did not return TrayIcon object");

                synchronized (releaseLock) {
                    try {
                        releaseLock.notifyAll();
                    } catch (Exception e) {
                    }
                }
            }

            public void mouseClicked(MouseEvent event) {
                mouseClicked = true;
                Point p = event.getPoint();
                if (p.x != event.getX() || p.y != event.getY())
                    throw new RuntimeException("FAIL: MouseEvent.getPoint() did " +
                            "not return the same value as getX/getY " +
                            "for mouseClicked");

                if (! icon.equals(event.getSource()))
                    throw new RuntimeException("FAIL: mouseClicked: MouseEvent.getSource " +
                                       "did not return TrayIcon object");

                synchronized (clickLock) {
                    try {
                        clickLock.notifyAll();
                    } catch (Exception e) {
                    }
                }
            }
        });

        icon.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent event) {
                mouseMoved = true;
                Point p = event.getPoint();
                if (p.x != event.getX() || p.y != event.getY())
                    throw new RuntimeException("FAIL: MouseEvent.getPoint() did " +
                            "not return the same value as getX/getY " +
                            "for mouseMoved");

                if (! icon.equals(event.getSource()))
                    throw new RuntimeException("FAIL: mouseMoved: MouseEvent.getSource " +
                            "did not return TrayIcon object");

                synchronized (moveLock) {
                    try {
                        moveLock.notifyAll();
                    } catch (Exception e) {
                    }
                }
            }
        });

        try {
            tray.add(icon);
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }

    void doTest() throws Exception {

        Point iconPosition = SystemTrayIconHelper.getTrayIconLocation(icon);
        if (iconPosition == null)
            throw new RuntimeException("Unable to find the icon location!");
        if (isOEL7) {
            // close tray
            robot.mouseMove(100,100);
            robot.click(InputEvent.BUTTON1_MASK);
            robot.waitForIdle(2000);
        }

        robot.mouseMove(iconPosition.x, iconPosition.y);
        robot.waitForIdle();
        if(!isOEL7) {
            SystemTrayIconHelper.doubleClick(robot);

            if (!actionPerformed) {
                synchronized (actionLock) {
                    try {
                        actionLock.wait(10000);
                    } catch (Exception e) {
                    }
                }
            }
            if (!actionPerformed)
                throw new RuntimeException("FAIL: ActionEvent not triggered when TrayIcon is double clicked");
        }

        for (int i = 0; i < buttonTypes.length; i++) {
            mousePressed = false;
            if(isOEL7) {
                SystemTrayIconHelper.openTrayIfNeeded(robot);
                robot.mouseMove(iconPosition.x, iconPosition.y);
                robot.click(buttonTypes[i]);
            } else {
                robot.mousePress(buttonTypes[i]);
            }

            if (! mousePressed) {
                synchronized (pressLock) {
                    try {
                        pressLock.wait(6000);
                    } catch (Exception e) {
                    }
                }
            }
            if (! mousePressed)
                if (! SystemTrayIconHelper.skip(buttonTypes[i]) )
                    throw new RuntimeException("FAIL: mousePressed not triggered when " +
                            buttonNames[i] + " pressed");

            mouseReleased = false;
            mouseClicked = false;
            if(isOEL7) {
                SystemTrayIconHelper.openTrayIfNeeded(robot);
                robot.mouseMove(iconPosition.x, iconPosition.y);
                robot.click(buttonTypes[i]);
            } else {
                robot.mouseRelease(buttonTypes[i]);
            }

            if (! mouseReleased) {
                synchronized (releaseLock) {
                    try {
                        releaseLock.wait(6000);
                    } catch (Exception e) {
                    }
                }
            }
            if (! mouseReleased)
                throw new RuntimeException("FAIL: mouseReleased not triggered when " +
                        buttonNames[i] + " released");

            if (! mouseClicked) {
                synchronized (clickLock) {
                    try {
                        clickLock.wait(6000);
                    } catch (Exception e) {
                    }
                }
            }
            if (! mouseClicked)
                throw new RuntimeException("FAIL: mouseClicked not triggered when " +
                        buttonNames[i] + " pressed & released");
        }

        if (!isOEL7) {
            mouseMoved = false;
            robot.mouseMove(iconPosition.x + 100, iconPosition.y);
            robot.glide(iconPosition.x, iconPosition.y);

            if (!mouseMoved)
                if (!SystemTrayIconHelper.skip(0))
                    throw new RuntimeException("FAIL: mouseMoved not triggered even when mouse moved over the icon");
        }
    }
}
