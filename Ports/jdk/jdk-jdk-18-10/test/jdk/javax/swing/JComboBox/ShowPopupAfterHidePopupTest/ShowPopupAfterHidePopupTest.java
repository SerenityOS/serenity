/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8006417
 * @summary JComboBox.showPopup(), hidePopup() fails in JRE 1.7 on OS X
 * @author Anton Litvinov
 * @run main ShowPopupAfterHidePopupTest
 */

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.metal.*;

public class ShowPopupAfterHidePopupTest {
    private static JFrame frame = null;
    private static JComboBox comboBox = null;
    private static boolean popupIsVisible = false;

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(new MetalLookAndFeel());
        Robot robot = new Robot();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame = new JFrame("Popup Menu of JComboBox");
                comboBox = new JComboBox(new String[]{"Item1", "Item2", "Item3"});
                frame.getContentPane().add(comboBox);
                frame.pack();
                frame.setVisible(true);
            }
        });
        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                comboBox.showPopup();
                comboBox.hidePopup();
                comboBox.showPopup();
            }
        });
        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                popupIsVisible = comboBox.isPopupVisible();
                frame.dispose();
            }
        });
        if (!popupIsVisible) {
            throw new RuntimeException("Calling hidePopup() affected the next call to showPopup().");
        }
    }
}
