/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6919529
 * @summary Checks if there is a null pointer exception when the component UI
 *   is null.
 * @run main MultiUIDefaultsNPECheck
 */
import javax.swing.LookAndFeel;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.UIDefaults;
import javax.swing.SwingUtilities;

public class MultiUIDefaultsNPECheck {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Test();
        });
    }

    public static void Test() {
        JLabel label = new JLabel();

        try {
            UIManager.setLookAndFeel(new LookAndFeel() {
                @Override
                public String getName() {
                    return null;
                }

                @Override
                public String getID() {
                    return null;
                }

                @Override
                public String getDescription() {
                    return null;
                }

                @Override
                public boolean isNativeLookAndFeel() {
                    return false;
                }

                @Override
                public boolean isSupportedLookAndFeel() {
                    return true;
                }

                @Override
                public UIDefaults getDefaults() {
                    return null;
                }
            });
        }  catch (UnsupportedLookAndFeelException e) {
            System.err.println("Warning: test not applicable because of " +
                "unsupported look and feel");
            return;
        }

        try {
            UIManager.getDefaults().getUI(label);
        } catch (NullPointerException e) {
            throw new RuntimeException("Got null pointer exception. Hence " +
                    "Test Failed");
        }
    }
}
