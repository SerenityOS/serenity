/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8226783 8247753
 * @key headful
 * @summary Verify System L&F
 */

/*
 * Verify the System LAF is what we expect based on platform and,
 * in at least one case, the desktop environment.
 * Since changes to the system LAF are a once in a blue moon event,
 * this test is useful to tell us of unexpected problems.
 * Note: this test must be run in a headful environment
 * since otherwise a system LAF may not be available.
 */

public class SystemLookAndFeelTest {

    public static void main(String[] args) {

        String laf = javax.swing.UIManager.getSystemLookAndFeelClassName();
        String os = System.getProperty("os.name").toLowerCase();
        System.out.println("OS is " + os);
        System.out.println("Reported System LAF is " + laf);

        String expLAF = null;
        if (os.contains("windows")) {
            expLAF = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
        } else if (os.contains("macos")) {
            expLAF = "com.apple.laf.AquaLookAndFeel";
        } else if (os.contains("linux")) {
            /*
             * The implementation keys off the following desktop setting to
             * decide if GTK is an appropriate system L&F.
             * In its absence, there probably isn't support for the GTK L&F
             * anyway. It does not tell us if the GTK libraries are available
             * but they really should be if this is a gnome session.
             * If it proves necessary the test can perhaps be updated to see
             * if the GTK LAF is listed as installed and can be instantiated.
             */
            String gnome = System.getenv("GNOME_DESKTOP_SESSION_ID");
            String desktop = System.getenv("XDG_CURRENT_DESKTOP");
            System.out.println("Gnome desktop session ID is " + gnome);
            System.out.println("XDG_CURRENT_DESKTOP is set to " + desktop);
            if (gnome != null ||
                    (desktop != null && desktop.toLowerCase().contains("gnome"))) {
                expLAF = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
            } else {
                expLAF = "javax.swing.plaf.metal.MetalLookAndFeel";
            }
        }
        System.out.println("Expected System LAF is " + expLAF);
        if (expLAF == null) {
            System.out.println("No match for expected LAF, unknown OS ?");
            return;
        }
        if (!(laf.equals(expLAF))) {
            throw new RuntimeException("LAF not as expected");
        }
    }
}
