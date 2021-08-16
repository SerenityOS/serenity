/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Toolkit;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import sun.awt.SunToolkit;

/**
 * @test
 * @bug 8136366
 * @summary Add a public API to create a L&F without installation
 * @modules java.desktop/sun.awt
 */
public class CreateLookAndFeelTest {

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(CreateLookAndFeelTest::createLAFs);
    }

    private static void createLAFs() {

        for (LAFTest lafTest : LAFTest.values()) {
            createLAF(lafTest);
        }

        try {
            UIManager.createLookAndFeel(null);
        } catch (NullPointerException e) {
            return;
        } catch (UnsupportedLookAndFeelException ignore) {
        }

        throw new RuntimeException("NPE is not thrown!");
    }

    private static void createLAF(LAFTest lafTest) {
        try {
            UIManager.createLookAndFeel(lafTest.lafName);

        } catch (UnsupportedLookAndFeelException e) {
            if (lafTest.isSupported) {
                throw new RuntimeException(e);
            }
        }
    }

    private static boolean isOSAvailable(String supportedOS) {
        return System.getProperty("os.name")
                .toLowerCase()
                .contains(supportedOS);
    }

    private static boolean isGTKAvailable() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        if (!(toolkit instanceof SunToolkit)) {
            return false;
        }
        return ((SunToolkit) toolkit).isNativeGTKAvailable();
    }

    enum LAFTest {

        METAL("Metal"),
        NIMBUS("Nimbus"),
        MOTIF("CDE/Motif"),
        WINDOWS("Windows", isOSAvailable("windows")),
        GTK("GTK look and feel", isGTKAvailable()),
        MAC("Mac OS X", isOSAvailable("mac"));

        private final String lafName;
        private final boolean isSupported;

        private LAFTest(String lafName) {
            this(lafName, true);
        }

        private LAFTest(String lafName, boolean crossPlatform) {
            this.lafName = lafName;
            this.isSupported = crossPlatform;
        }
    }
}
