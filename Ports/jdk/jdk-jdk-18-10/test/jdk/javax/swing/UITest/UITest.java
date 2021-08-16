/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 4843282 4886871
 * @summary Makes sure windows is only listed on Windows platform, and
 *          GTK is not on Windows and Mac.
 * added as tabs
 * @author Scott Violet
 * @library /test/lib
 * @build jdk.test.lib.Platform
 * @run main UITest
 */
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;

import jdk.test.lib.Platform;

public class UITest {

    public static void main(String[] args) {
        LookAndFeelInfo[] lafInfo = UIManager.getInstalledLookAndFeels();
        if (Platform.isWindows()) {
            // Make sure we don't have GTK.
            if (hasLAF("gtk", lafInfo)) {
                throw new RuntimeException("On windows, but GTK is present");
            }

            // Make sure we don't have Aqua.
            if (hasLAF("mac", lafInfo)) {
                throw new RuntimeException("On windows, but Aqua is present");
            }

            // Make sure we have Windows.
            if (!hasLAF("windows", lafInfo)) {
                throw new RuntimeException("On windows and don't have Windows");
            }
        } else if (Platform.isOSX()) {
            // Make sure we don't have GTK.
            if (hasLAF("gtk", lafInfo)) {
                throw new RuntimeException("On mac, but GTK is present");
            }

            // Make sure we don't have Windows.
            if (hasLAF("windows", lafInfo)) {
                throw new RuntimeException("On mac, but Windows is present");
            }

            // Make sure we have Aqua.
            if (!hasLAF("mac", lafInfo)) {
                throw new RuntimeException("On mac and don't have Aqua");
            }
        } else {
            // Not windows and mac

            // Make sure we don't have Windows.
            if (hasLAF("windows", lafInfo)) {
                throw new RuntimeException("Not on windows and have Windows");
            }

            // Make sure we don't have Aqua.
            if (hasLAF("mac", lafInfo)) {
                throw new RuntimeException("Not on mac and have Aqua");
            }

            // Make sure we have GTK.
            if (!hasLAF("gtk", lafInfo)) {
                throw new RuntimeException(
                        "Not on Windows and Mac and don't have GTK!");
            }
        }
    }

    public static boolean hasLAF(String name, LookAndFeelInfo[] lafInfo) {

        for (int counter = 0; counter < lafInfo.length; counter++) {
            if (lafInfo[counter].getName().toLowerCase().indexOf(name) != -1) {
                return true;
            }
        }
        return false;
    }
}
