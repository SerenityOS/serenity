/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.EventQueue;
import java.awt.Image;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;


/*
 * @test
 * @bug 8161473
 * @key headful
 * @summary Check if MouseEvent has the proper modifiers when
 *          TrayIcon is clicked pressing the modifier keys
 * @modules java.desktop/java.awt:open
 * @library /java/awt/patchlib
 * @library /lib/client ../
 * @build java.desktop/java.awt.Helper
 * @build ExtendedRobot SystemTrayIconHelper
 * @run main TrayIconEventModifiersTest
 */

public class TrayIconEventModifiersTest {

    Image image;

    TrayIcon icon;
    ExtendedRobot robot;

    Object mouseLock = new Object();

    String caption = "Sample Icon";
    boolean mousePressed = false;
    boolean mouseReleased = false;
    boolean mouseClicked = false;
    int modifiers, releaseModifiers, clickModifiers;

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

    int[] buttonMasks = {
        InputEvent.BUTTON1_DOWN_MASK,
        InputEvent.BUTTON2_DOWN_MASK,
        InputEvent.BUTTON3_DOWN_MASK
    };

    static int[] keyTypes = {
        KeyEvent.VK_SHIFT,
        KeyEvent.VK_CONTROL
    };

    static String[] keyNames = {
        "SHIFT",
        "CONTROL"
    };

    static int[] keyMasks = {
        KeyEvent.SHIFT_DOWN_MASK,
        KeyEvent.CTRL_DOWN_MASK
    };

    public static void main(String[] args) throws Exception {
        if (! SystemTray.isSupported()) {
            System.out.println("SystemTray not supported on the platform under test. " +
                    "Marking the test passed");
        } else {
            if (System.getProperty("os.name").toLowerCase().startsWith("win"))
                System.err.println("Test can fail if the icon hides to a tray icons pool" +
                        "in Windows 7, which is behavior by default.\n" +
                        "Set \"Right mouse click\" -> \"Customize notification icons\" -> " +
                        "\"Always show all icons and notifications on the taskbar\" true " +
                        "to avoid this problem. Or change behavior only for Java SE tray " +
                        "icon and rerun test.");

            System.out.println(System.getProperty("os.arch"));

            if (SystemTrayIconHelper.isOel7()) {
                System.out.println("OEL 7 doesn't support click modifiers in " +
                        "systray. Skipped");
                return;
            }

            new TrayIconEventModifiersTest().doTest();
        }
    }

    public TrayIconEventModifiersTest() throws Exception {
        robot = new ExtendedRobot();
        EventQueue.invokeAndWait(this::initializeGUI);
    }

    private void initializeGUI() {

        SystemTray tray = SystemTray.getSystemTray();
        icon = new TrayIcon(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), caption);
        icon.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent event) {
                if (!icon.equals(event.getSource()))
                    throw new RuntimeException("FAIL: mousePressed: MouseEvent.getSource " +
                            "did not return TrayIcon object");

                mousePressed = true;
                modifiers = event.getModifiersEx();
                System.out.println("Icon mousePressed " + modifiers);
                synchronized (mouseLock) {
                    try {
                        mouseLock.notifyAll();
                    } catch (Exception e) {
                    }
                }
            }

            public void mouseReleased(MouseEvent event) {
                if (!icon.equals(event.getSource()))
                    throw new RuntimeException("FAIL: mouseReleased: MouseEvent.getSource " +
                            "did not return TrayIcon object");

                mouseReleased = true;
                releaseModifiers = event.getModifiersEx();
                System.out.println("Icon mouseReleased " + releaseModifiers);
                synchronized (mouseLock) {
                    try {
                        mouseLock.notifyAll();
                    } catch (Exception e) {
                    }
                }
            }

            public void mouseClicked(MouseEvent event) {
                if (!icon.equals(event.getSource()))
                    throw new RuntimeException("FAIL: mouseClicked: MouseEvent.getSource " +
                            "did not return TrayIcon object");

                mouseClicked = true;
                clickModifiers = event.getModifiersEx();
                System.out.println("Icon mouseClickedd " + clickModifiers);
                synchronized (mouseLock) {
                    try {
                        mouseLock.notifyAll();
                    } catch (Exception e) {
                    }
                }
            }
        });

        try {
            tray.add(icon);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void doTest() throws Exception {

        Point iconPosition = SystemTrayIconHelper.getTrayIconLocation(icon);
        if (iconPosition == null)
            throw new RuntimeException("Unable to find the icon location!");

        robot.mouseMove(iconPosition.x, iconPosition.y);
        robot.waitForIdle();

        for (int i = 0; i < buttonTypes.length; i++) {
            for (int j = 0; j < keyTypes.length; j++) {
                mousePressed = false;

                robot.keyPress(keyTypes[j]);
                robot.waitForIdle();
                robot.mousePress(buttonTypes[i]);

                if (! mousePressed) {
                    synchronized (mouseLock) {
                        try {
                            mouseLock.wait(3000);
                        } catch (Exception e) {
                        }
                    }
                }
                if (! mousePressed) {
                    if (! SystemTrayIconHelper.skip(buttonTypes[i]))
                        throw new RuntimeException("FAIL: mousePressed not triggered when " +
                                keyNames[j] + " + " + buttonNames[i] + " pressed");
                } else {
                    int onMask = buttonMasks[i] | keyMasks[j];
                    if ((modifiers & onMask) != onMask) {
                        throw new RuntimeException("FAIL: getModifiersEx did not return " +
                                "the correct value when " + keyNames[j] + " + " +
                                buttonNames[i] + " pressed");
                    }
                }

                mouseReleased = false;
                mouseClicked = false;
                robot.mouseRelease(buttonTypes[i]);

                if (! mouseReleased) {
                    synchronized (mouseLock) {
                        try {
                            mouseLock.wait(3000);
                        } catch (Exception e) {
                        }
                    }
                }

                robot.waitForIdle(1000);
                robot.keyRelease(keyTypes[j]);
                robot.waitForIdle(1000);

                if (! mouseReleased) {
                    if (! SystemTrayIconHelper.skip(buttonTypes[i]))
                        throw new RuntimeException("FAIL: mouseReleased not triggered when " +
                                keyNames[j] + " + " + buttonNames[i] + " released");
                } else {
                    int onMask = keyMasks[j];
                    if ((releaseModifiers & onMask) != onMask)
                        throw new RuntimeException("FAIL: getModifiersEx did not return " +
                                "the correct value when " + keyNames[j] + " + " +
                                buttonNames[i] + " released");
                }
                if (! mouseClicked) {
                    throw new RuntimeException("FAIL: mouseClicked not triggered when " +
                            keyNames[j] + " + " + buttonNames[i] +
                            " pressed & released");
                } else {
                    int onMask = keyMasks[j];
                    if ((clickModifiers & onMask) != onMask)
                        throw new RuntimeException("FAIL: getModifiersEx did not return " +
                                "the correct value when " + keyNames[j] + " + " +
                                buttonNames[i] + " pressed & released");
                }
                robot.type(KeyEvent.VK_ESCAPE);
            }
        }
    }
}

