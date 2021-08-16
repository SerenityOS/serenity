/*
 * Copyright (c) 2006, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @bug 6406264
 * @summary Tests that JComboBox's focusable popup can be shown.
 * @author Mikhail Lapshin
 * @run main bug6406264
 */

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.basic.BasicComboPopup;
import java.awt.*;

public class bug6406264 extends JComboBox {

    static JFrame frame;
    static bug6406264 comboBox;

    public static void main(String[] args) throws Exception {

        Robot robot = new Robot();
        // Setup and show frame
        SwingUtilities.invokeAndWait(
                new Runnable() {
                    public void run() {
                        frame = new JFrame("JComboBox6406264 test");
                        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                        comboBox = new bug6406264("One", "Two", "Three");
                        frame.getContentPane().add(comboBox);

                        frame.setLocationRelativeTo(null);
                        frame.pack();
                        frame.setVisible(true);
                    }
                }
        );

        robot.waitForIdle();

        // Show popup
        SwingUtilities.invokeAndWait(
                new Runnable() {
                    public void run() {
                        comboBox.showPopup();
                    }
                });
        robot.waitForIdle();

        // Check that popup is visible
        SwingUtilities.invokeAndWait(
                new Runnable() {
                    public void run() {
                        if (comboBox.getUI().isPopupVisible(comboBox) == false)
                        {
                            throw new RuntimeException("A focusable popup is not visible " +
                                    "in JComboBox!");
                        }
                    }
                }
        );

        frame.dispose();
    }

    public bug6406264(Object ... items) {
        super(items);
    }

    public void updateUI() {
        setUI(new CustomComboBoxUI());
    }

    // Use FocusablePopup for our JComboBox
    private class CustomComboBoxUI extends BasicComboBoxUI {
        protected ComboPopup createPopup() {
            return new FocusablePopup(bug6406264.this);
        }
    }

    // Popup window which can take a focus
    private class FocusablePopup extends BasicComboPopup {
        public FocusablePopup(JComboBox combo) {
            super(combo);
        }

        public boolean isFocusable() {
            return true;
        }
    }
}
