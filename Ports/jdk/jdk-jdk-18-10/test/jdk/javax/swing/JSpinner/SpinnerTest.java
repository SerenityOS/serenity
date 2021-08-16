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

/* @test
   @bug 5078989
   @key headful
   @summary Verifies Null Pointer exception is not thrown in SpinnerListMode
   @run main SpinnerTest
 */
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import javax.swing.SwingUtilities;
import java.awt.event.KeyEvent;
import java.awt.Point;
import java.awt.Robot;

public class SpinnerTest
{
    private static JFrame frame;
    private static JSpinner spinner;

    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        robot.setAutoDelay(100);
        try {
            SwingUtilities.invokeAndWait(() -> {
                //Create and set up the window.
                frame = new JFrame("SpinnerDemo");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                JPanel panel = new JPanel();
                String[] values = {"Month: ", "Year: ", null, "Date", "Sent"};

                SpinnerListModel listModel = new SpinnerListModel(values);

                JLabel l = new JLabel("Spinner");
                panel.add(l);

                spinner = new JSpinner(listModel);
                l.setLabelFor(spinner);
                panel.add(spinner);

                panel.setOpaque(true); //content panes must be opaque
                frame.setContentPane(panel);

                //Display the window.
                frame.pack();
                frame.setVisible(true);
                frame.setLocationRelativeTo(null);
            });
            robot.waitForIdle();
            robot.delay(1000);
            Point loc = spinner.getLocationOnScreen();

            robot.mouseMove(loc.x, loc.y);
            robot.keyPress(KeyEvent.VK_SPACE);
            robot.keyRelease(KeyEvent.VK_SPACE);

        } finally {
            if (frame != null) {
                SwingUtilities.invokeAndWait(() -> frame.dispose());
            }
        }
    }
}
