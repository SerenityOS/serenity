/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.TrayIcon;
import java.awt.SystemTray;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.AWTException;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;

/*
 * @test
 * @bug 6271624 8195991
 * @key headful
 * @summary Right clicking on TrayIcon shouldn't trigger ActionEvent when balloon is displayed.
 * @modules java.desktop/java.awt:open
 * @library /java/awt/patchlib
 * @library /lib/client ../
 * @build java.desktop/java.awt.Helper
 * @build ExtendedRobot SystemTrayIconHelper
 * @run main RightClickWhenBalloonDisplayed
 */

public class RightClickWhenBalloonDisplayed {

    TrayIcon icon;
    ExtendedRobot robot;
    int actionPerformedCount = -1;

    public static void main(String[] args) throws Exception {
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray not supported on the platform under test. " +
                    "Marking the test passed");
        } else {
            if (System.getProperty("os.name").toLowerCase().startsWith("win"))
                System.err.println("Test can fail if the icon hides to a tray icons pool " +
                        "in Windows 7/10, which is behavior by default.\n" +
                        "Set \"Right mouse click\" -> \"Customize notification icons\" -> " +
                        "\"Always show all icons and notifications on the taskbar\" true " +
                        "to avoid this problem. Or change behavior only for Java SE " +
                        "tray icon.");
            new RightClickWhenBalloonDisplayed().doTest();
        }
    }

    RightClickWhenBalloonDisplayed() throws Exception {
        robot = new ExtendedRobot();
        EventQueue.invokeAndWait(this::initializeGUI);
        robot.waitForIdle(1000);
    }

    private void initializeGUI() {
        SystemTray tray = SystemTray.getSystemTray();
        icon = new TrayIcon(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), "Sample Icon");
        icon.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent event) {
                icon.displayMessage("Sample Icon", "This is a test message for the tray icon", TrayIcon.MessageType.INFO);
            }
        });

        try {
            tray.add(icon);
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }

        icon.getActionCommand();
        icon.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionPerformedCount++;
            }
        });
    }

    void doTest() throws Exception {

        Point iconPosition = SystemTrayIconHelper.getTrayIconLocation(icon);
        if (iconPosition == null)
            throw new RuntimeException("Unable to find the icon location!");

        // Do left click to start displaying the message
        robot.delay(50);
        robot.mouseMove(iconPosition.x, iconPosition.y);
        robot.waitForIdle();
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(50);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(1000);

        // Do right click
        robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
        robot.delay(50);
        robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        robot.delay(50);

        if (actionPerformedCount > 0)
            throw new RuntimeException("FAIL: ActionEvent triggered when " +
                    "the tray icon was right clicked and at the same time the tray icon message is displayed");
    }
}
