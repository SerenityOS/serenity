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

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

/*
 * @test
 * @key headful
 * @bug 8136998
 * @summary Checks that JComboBox does not prevent mouse-wheel scrolling JScrollPane.
 * @library ../../regtesthelpers
 * @build Util
 * @run main bug8136998
 * @author Alexey Ivanov
 */
public class bug8136998 {

    private static final String[] ITEMS = new String[] {
        "A", "B", "C", "D", "E", "F"
    };

    private final Robot robot;

    private JFrame frame;
    private JComboBox comboBox;
    private JScrollPane scrollPane;

    public static void main(String[] args) throws Exception {
        iterateLookAndFeels(new bug8136998());
    }

    protected static void iterateLookAndFeels(final bug8136998 test) throws Exception {
        LookAndFeelInfo[] lafInfo = UIManager.getInstalledLookAndFeels();
        for (LookAndFeelInfo info : lafInfo) {
            try {
                UIManager.setLookAndFeel(info.getClassName());
                System.out.println("Look and Feel: " + info.getClassName());
                test.runTest();
            } catch (UnsupportedLookAndFeelException e) {
                System.out.println("Skipping unsupported LaF: " + info);
            }
        }
    }

    public bug8136998() throws AWTException {
        robot = new Robot();
        robot.setAutoDelay(200);
    }

    private void setupUI() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        comboBox = new JComboBox<>(ITEMS);

        JPanel scrollable = new JPanel();
        scrollable.setLayout(new BoxLayout(scrollable, BoxLayout.Y_AXIS));

        scrollable.add(Box.createVerticalStrut(200));
        scrollable.add(comboBox);
        scrollable.add(Box.createVerticalStrut(200));

        scrollPane = new JScrollPane(scrollable);

        frame.add(scrollPane);

        frame.setSize(100, 200);
        frame.setVisible(true);
    }

    public void runTest() throws Exception {
        try {
            SwingUtilities.invokeAndWait(this::setupUI);

            robot.waitForIdle();

            SwingUtilities.invokeAndWait(() ->
                scrollPane.getViewport().scrollRectToVisible(comboBox.getBounds())
            );
            robot.waitForIdle();

            // Move mouse pointer to the center of the combo box
            Point p = comboBox.getLocationOnScreen();
            Dimension d = comboBox.getSize();
            robot.mouseMove(p.x + d.width / 2, p.y + d.height / 2);

            // The currently visible rectangle in scrollPane
            Rectangle viewRect0 = Util.invokeOnEDT(scrollPane.getViewport()::getViewRect);

            // Scroll the scrollPane with mouse wheel
            robot.mouseWheel(1);
            robot.waitForIdle();

            // The updated rectangle
            Rectangle viewRect1 = Util.invokeOnEDT(scrollPane.getViewport()::getViewRect);

            if (viewRect0.y == viewRect1.y) {
                throw new RuntimeException("Mouse wheel should have scrolled the JScrollPane");
            }
        } finally {
            if (frame != null) {
                frame.dispose();
            }
        }

        System.out.println("Test passed");
    }
}
