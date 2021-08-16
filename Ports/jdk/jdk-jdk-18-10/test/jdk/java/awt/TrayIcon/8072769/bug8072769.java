/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 8072769
   @key headful
   @requires (os.family == "windows")
   @summary System tray icon title freezes java
   @author Semyon Sadetsky
   @library /test/lib
   @build jdk.test.lib.Platform
   @run main bug8072769
  */

import jdk.test.lib.Platform;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class bug8072769 {

    public static void main(String[] args) throws Exception {
        if (Platform.isWindows()) {
            if (SystemTray.isSupported()) {
                test();
            } else {
                System.out.println("SystemTray not supported. " +
                        "Test is skipped.");
            }
        } else {
            System.out.println("Test will only run on Windows platform. " +
                    "Test is skipped.");
        }
        System.out.println("ok");
    }

    private static void test() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                final SystemTray tray = SystemTray.getSystemTray();
                final TrayIcon trayIcon = new TrayIcon(icon.getImage());
                try {
                    tray.add(trayIcon);
                } catch (AWTException e) {
                    throw new RuntimeException(
                            "TrayIcon could not be added.");
                }


                try {
                    trayIcon.displayMessage(createString(63, 'A'),
                            createString(255, 'C'), TrayIcon.MessageType.ERROR);

                    trayIcon.setToolTip(createString(127, 'B'));

                    trayIcon.displayMessage(createString(64, 'A'),
                            createString(256, 'C'), TrayIcon.MessageType.ERROR);

                    trayIcon.setToolTip(createString(128, 'B'));

                    trayIcon.displayMessage(createString(65, 'A'),
                            createString(257, 'C'), TrayIcon.MessageType.ERROR);

                    trayIcon.setToolTip(createString(129, 'B'));
                }
                finally {
                    tray.remove(trayIcon);
                }
            }
        });
    }

    private static String createString(int len, char letter) {
        char[] chars = new char[len];
        Arrays.fill(chars, letter);
        chars[len - 2] = '=';
        chars[len - 1] = '>';
        return new String(chars);
    }

    private static ImageIcon icon = new ImageIcon(
            new byte[]{71, 73, 70, 56, 57, 97, 32, 0, 35, 0, -43, 0, 0, -1, -1,
                    -1, -19, -101, 9, -18, -95, 24, -14, -76, 71, -4, -19, -46,
                    -3, -13, -31, -17, -88, 40, -12, -63, 102, -10, -51, -124,
                    -16, -82, 55, -11, -57, 117, -2, -7, -15, -7, -32, -77, -9,
                    -45, -108, -5, -26, -62, -13, -70, 86, -8, -39, -94, 83,
                    -126, -95, -8, -38, -93, -6, -26, -63, -9, -45, -109, -4,
                    -14, -32, -15, -76, 70, -12, -58, 116, -17, -89, 39, 77,
                    121, -106, -3, -8, -17, 104, -111, -84, 126, -95, -72, 93,
                    -119, -90, -14, -70, 85, -13, -64, 101, -16, -83, 55, -109,
                    -80, -60, -7, -33, -78, -100, -84, -85, 94, -127, -104, -32,
                    -99, 39, 127, -120, -114, 83, 113, -124, -12, -9, -7, -16,
                    -16, -16, -115, 108, 45, 57, 89, 110, -50, -41, -35, 104,
                    -111, -83, 41, 65, 80, 72, 113, -116, 115, -103, -78, 88,
                    106, 112, -82, -78, -82, -45, -38, -40, -5, -20, -48, -65,
                    -48, -36, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 33, -7, 4, 0, 0, 0,
                    0, 0, 44, 0, 0, 0, 0, 32, 0, 35, 0, 0, 6, -1, 64, -128, 112,
                    72, 4, 16, 0, 14, -57, 2, 1, 96, 30, -117, -48, 40, -78,
                    -55, 96, 28, 28, -125, -62, 0, 96, 48, 74, -91, -116, 102,
                    3, 97, 64, 4, 20, -25, -128, 68, -80, 16, 24, 11, 95, 98,
                    -29, -64, 72, 11, 2, 120, -68, 96, -64, 39, 116, -29, 0, 12,
                    13, 5, 1, 3, 121, -121, -120, 9, 2, 7, 5, 15, 82, 11, 11,
                    92, 15, 6, -120, -107, -121, 7, 2, 18, 0, 112, 80, 3, 8,
                    104, -106, -95, 122, 88, 97, 68, 5, 11, 4, -95, 32, 8, 16,
                    19, 16, 8, 22, -106, -114, 79, 66, 5, 2, 15, 9, -120, 22,
                    19, 81, 21, 31, -120, 7, 6, 10, 67, 71, 4, 119, -121, 20,
                    -128, 16, -57, 120, 7, -101, -111, -58, 9, -108, 121, -55,
                    -128, 0, 16, 121, 123, -117, 67, 5, -71, 121, 30, -42, 67,
                    23, -121, 13, 66, 14, 6, 3, -34, 120, 21, -31, 66, 26, -39,
                    3, 6, -50, 11, -96, 120, 31, -19, 67, 30, 121, 9, 14, 0, 13,
                    124, -121, 68, -32, 19, 98, 6, 15, 58, 71, 18, 12, -27, 97,
                    55, 80, 68, 54, 5, 5, 24, 40, 80, 23, 96, -96, -112, 9, -39,
                    30, 52, -112, 72, -47, 34, 0, 10, 25, -53, 37, 60, -60, 16,
                    -33, 56, 61, 16, -1, 41, -60, 83, 13, 31, -122, 60, 7, 1,
                    -48, 59, -124, 65, 3, 62, -116, 48, -5, 57, 72, -112, -18,
                    -48, 5, -103, 124, 32, -32, 37, 112, -74, -119, 98, 0, 8,
                    -31, 64, -110, 35, 38, 64, 26, 34, -92, 113, 42, 48, -45,
                    70, -76, 24, -77, 60, 80, -91, -60, -70, -12, 76, -120, 49,
                    92, -120, 4, -40, -116, -126, 51, 79, -80, 97, -36, 80, 89,
                    -6, 25, -91, 96, -98, 89, -99, 62, 33, -62, 32, -59, -83, 0,
                    82, 80, 32, 1, -72, 53, 13, -113, -42, 102, -103, 54, -127,
                    25, 84, 40, 15, -115, 40, 37, 20, 49, 34, 26, 103, 78, 29,
                    52, 42, 88, 16, 65, 17, -94, -49, 31, 107, 97, 16, -116, 49,
                    32, 35, -61, 6, 14, 33, 56, 68, -120, -80, -96, 11, 1, 78,
                    -31, -6, 33, 96, 48, -93, -61, -122, 21, 46, 50, -116, -10,
                    -30, -47, -117, -125, 24, 29, 94, -100, -112, 61, -94, 54,
                    -108, 20, 38, 90, -112, -128, 81, -61, 90, 16, 0, 59},
            "try icon");

}
