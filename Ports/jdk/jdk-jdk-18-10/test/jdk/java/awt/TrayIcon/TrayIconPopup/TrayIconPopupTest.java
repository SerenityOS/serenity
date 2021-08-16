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
 * @summary Check if a JPopupMenu can be displayed when TrayIcon is
 *          right clicked. It uses a JWindow as the parent of the JPopupMenu
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @modules java.desktop/java.awt:open
 * @library /java/awt/patchlib
 * @library /lib/client ../
 * @build java.desktop/java.awt.Helper
 * @build ExtendedRobot SystemTrayIconHelper
 * @run main TrayIconPopupTest
 */

public class TrayIconPopupTest {

    TrayIcon icon;
    ExtendedRobot robot;

    boolean actionPerformed = false;
    Object actionLock = new Object();
    static final int ATTEMPTS = 50;

    PopupMenu popup;
    Dialog window;

    public static void main(String[] args) throws Exception {
        if (!SystemTray.isSupported()) {
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
            new TrayIconPopupTest().doTest();
        }
    }

    TrayIconPopupTest() throws Exception {
        robot = new ExtendedRobot();
        EventQueue.invokeAndWait(this::initializeGUI);
        robot.waitForIdle(1000);
        EventQueue.invokeAndWait( () ->  window.setLocation(100, 100));
        robot.waitForIdle(1000);
    }

    private void initializeGUI() {
        window = new Dialog((Frame) null);
        window.setSize(5, 5);
        window.setVisible(true);

        popup = new PopupMenu("");

        MenuItem item = new MenuItem("Sample");
        item.addActionListener(event -> {
            actionPerformed = true;

            synchronized (actionLock) {
                try {
                    actionLock.notifyAll();
                } catch (Exception e) {
                }
            }
        });
        popup.add(item);
        popup.add(new MenuItem("Item2"));
        popup.add(new MenuItem("Item3"));

        window.add(popup);

        SystemTray tray = SystemTray.getSystemTray();
        icon = new TrayIcon(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), "Sample Icon");
        icon.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent event) {
                if (event.isPopupTrigger()) {
                    popup.show(window, 0, 0);
                }
            }

            public void mouseReleased(MouseEvent event) {
                if (event.isPopupTrigger()) {
                    popup.show(window, 0, 0);
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

        robot.mouseMove(iconPosition.x, iconPosition.y);
        robot.waitForIdle();
        robot.mousePress(InputEvent.BUTTON3_MASK);
        robot.delay(50);
        robot.mouseRelease(InputEvent.BUTTON3_MASK);
        robot.delay(6000);

        robot.mouseMove(window.getLocation().x + 10, window.getLocation().y + 10);
        robot.mousePress(InputEvent.BUTTON3_MASK);
        robot.delay(50);
        robot.mouseRelease(InputEvent.BUTTON3_MASK);

        int attempts = 0;
        while (!actionPerformed && attempts++ < ATTEMPTS) {
            synchronized (actionLock) {
                try {
                    actionLock.wait(3000);
                } catch (Exception e) {
                }
            }
        }
        if (!actionPerformed)
            throw new RuntimeException("FAIL: ActionEvent not triggered when " +
                    "JPopupMenu shown and menu item selected using keyboard");

    }
}
