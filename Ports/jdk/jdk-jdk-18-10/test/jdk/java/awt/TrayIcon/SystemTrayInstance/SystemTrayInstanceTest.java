/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @key headful
 * @summary Check the getSystemTray method of the SystemTray. Checks if
 *          a proper instance is returned in supported platforms and a proper
 *          exception is thrown in unsupported platforms
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @run main/othervm -DSystemTraySupport=TRUE SystemTrayInstanceTest
 */

public class SystemTrayInstanceTest {

    private static boolean supported = false;

    public static void main(String[] args) throws Exception {
        String sysTraySupport = System.getProperty("SystemTraySupport");
        if (sysTraySupport == null)
            throw new RuntimeException("SystemTray support status unknown!");

        if ("TRUE".equals(sysTraySupport)) {
            System.out.println("System tray is supported on the platform under test");
            supported = true;
        }

        new SystemTrayInstanceTest().doTest();
    }

    private void doTest() throws Exception {
        boolean flag = SystemTray.isSupported();
        if (supported != flag)
            throw new RuntimeException("FAIL: isSupported did not return the correct value"+
                    (supported ?
                            "SystemTray is supported on the platform under test" :
                            "SystemTray is not supported on the platform under test") +
                    "SystemTray.isSupported() method returned " + flag);

        if (supported) {
            SystemTray tray = SystemTray.getSystemTray();
        } else {
            try {
                SystemTray tray = SystemTray.getSystemTray();
            } catch (UnsupportedOperationException uoe) {
                System.out.println("UnsupportedOperationException thrown correctly");
            }
        }
    }
}
