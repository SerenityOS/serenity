/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 8146330
 * @summary Size of values returned by UIDefaults.keys() and
            UIDefaults.keySet() are different
 * @run main UIDefaultKeySizeTest
 */

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.util.Enumeration;
import java.util.Iterator;

public class UIDefaultKeySizeTest {
    static Enumeration e;
    static Iterator itr;
    static boolean defaultTestFail, writeTestFail;

    public static void main(String[] args) throws Exception {
        UIManager.LookAndFeelInfo[] installedLookAndFeels;
        installedLookAndFeels = UIManager.getInstalledLookAndFeels();

        for (UIManager.LookAndFeelInfo LF : installedLookAndFeels) {
            try {
                UIManager.setLookAndFeel(LF.getClassName());

                defaultTestFail = keySizeTest();
                SwingUtilities.invokeAndWait(() -> {
                    UIManager.getDefaults().put("TestKey", "TestValue");
                });
                writeTestFail = keySizeTest();

                if (defaultTestFail && writeTestFail) {
                    throw new RuntimeException("Default key count and Write " +
                            "key count both are not same in keys() and" +
                            " keySet()");
                } else if (defaultTestFail || writeTestFail) {
                    if (defaultTestFail) {
                        throw new RuntimeException("Default key count is not" +
                                " same in keys() and keySet()");
                    } else {
                        throw new RuntimeException("Write key count is not" +
                                " same in keys() and keySet()");
                    }
                }

                SwingUtilities.invokeAndWait(() -> {
                    UIManager.getDefaults().remove("TestKey");
                });
            } catch(UnsupportedLookAndFeelException e) {
                System.out.println("    Note: LookAndFeel " + LF.getClassName()
                        + " is not supported on this configuration");
            }
        }
    }

    private static boolean keySizeTest() throws Exception {
        int keysCount = 0;
        int keySetCount = 0;
        SwingUtilities.invokeAndWait(() -> {
            e = UIManager.getDefaults().keys();
            itr = UIManager.getDefaults().keySet().iterator();
        });
        while (e.hasMoreElements()) {
            keysCount++;
            e.nextElement();
        }
        while (itr.hasNext()) {
            keySetCount++;
            itr.next();
        }
        return !(keysCount == keySetCount);
    }
}
