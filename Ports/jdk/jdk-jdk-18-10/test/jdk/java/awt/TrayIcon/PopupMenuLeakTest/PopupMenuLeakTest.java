/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug 8007220 8039081
  @summary Reference to the popup leaks after the TrayIcon is removed.
  @requires os.family != "windows"
  @library /lib/client/
  @build ExtendedRobot
  @run main/othervm -Xmx50m PopupMenuLeakTest
 */

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import javax.swing.SwingUtilities;

import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class PopupMenuLeakTest {

    static final AtomicReference<WeakReference<TrayIcon>> iconWeakReference = new AtomicReference<>();
    static final AtomicReference<WeakReference<PopupMenu>> popupWeakReference = new AtomicReference<>();
    static ExtendedRobot robot;
    public static void main(String[] args) throws Exception {
        robot = new ExtendedRobot();
        SwingUtilities.invokeAndWait(PopupMenuLeakTest::createSystemTrayIcon);
        sleep();
        // To make the test automatic we explicitly call addNotify on a popup to create the peer
        SwingUtilities.invokeAndWait(PopupMenuLeakTest::addNotifyPopup);
        sleep();
        SwingUtilities.invokeAndWait(PopupMenuLeakTest::removeIcon);
        sleep();
        assertCollected(iconWeakReference.get(), "Failed, reference to tray icon not collected");
        assertCollected(popupWeakReference.get(), "Failed, reference to popup not collected");
    }

    private static void addNotifyPopup() {
        PopupMenu menu = popupWeakReference.get().get();
        if (menu == null) {
            throw new RuntimeException("Failed: popup collected too early");
        }
        menu.addNotify();
    }

    private static void removeIcon() {
        TrayIcon icon = iconWeakReference.get().get();
        if (icon == null) {
            throw new RuntimeException("Failed: TrayIcon collected too early");
        }
        SystemTray.getSystemTray().remove(icon);
    }

    private static void assertCollected(WeakReference<?> reference, String message) {
        java.util.List<byte[]> bytes = new ArrayList<>();
        for (int i = 0; i < 5; i ++) {
            if (reference.get() == null) {
                // reference is collected, avoid OOMs.
                break;
            }
            try {
                while (true) {
                    bytes.add(new byte[4096]);
                }
            } catch (OutOfMemoryError err) {
                bytes.clear();
                causeGC();
            }
        }
        if (reference.get() != null) {
            throw new RuntimeException(message);
        }
    }

    private static void causeGC() {
        System.gc();
        System.runFinalization();
        robot.delay(1000);
    }


    private static void createSystemTrayIcon() {
        final TrayIcon trayIcon = new TrayIcon(createTrayIconImage());
        trayIcon.setImageAutoSize(true);

        try {
            // Add tray icon to system tray *before* adding popup menu to demonstrate buggy behaviour
            trayIcon.setPopupMenu(createTrayIconPopupMenu());
            SystemTray.getSystemTray().add(trayIcon);
            iconWeakReference.set(new WeakReference<>(trayIcon));
            popupWeakReference.set(new WeakReference<>(trayIcon.getPopupMenu()));
        } catch (final AWTException awte) {
            awte.printStackTrace();
        }
    }

    private static Image createTrayIconImage() {
        /**
         * Create a small image of a red circle to use as the icon for the tray icon
         */
        int trayIconImageSize = 32;
        final BufferedImage trayImage = new BufferedImage(trayIconImageSize, trayIconImageSize, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D trayImageGraphics = (Graphics2D) trayImage.getGraphics();

        trayImageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        trayImageGraphics.setColor(new Color(255, 255, 255, 0));
        trayImageGraphics.fillRect(0, 0, trayImage.getWidth(), trayImage.getHeight());

        trayImageGraphics.setColor(Color.red);

        int trayIconImageInset = 4;
        trayImageGraphics.fillOval(trayIconImageInset,
                trayIconImageInset,
                trayImage.getWidth() - 2 * trayIconImageInset,
                trayImage.getHeight() - 2 * trayIconImageInset);

        trayImageGraphics.setColor(Color.darkGray);

        trayImageGraphics.drawOval(trayIconImageInset,
                trayIconImageInset,
                trayImage.getWidth() - 2 * trayIconImageInset,
                trayImage.getHeight() - 2 * trayIconImageInset);

        return trayImage;
    }

    private static PopupMenu createTrayIconPopupMenu() {
        final PopupMenu trayIconPopupMenu = new PopupMenu();
        final MenuItem popupMenuItem = new MenuItem("TEST!");
        trayIconPopupMenu.add(popupMenuItem);
        return trayIconPopupMenu;
    }

    private static void sleep() {
        robot.waitForIdle(100);
    }
}
