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

/*
 * @test
 * @key headful
 * @bug 6897701
 * @summary Verify JMenu and JMenuItem Disabled state for Nimbus LAF
 * @run main JMenuItemsTest
 */

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class JMenuItemsTest {

    private static JFrame mainFrame;
    private static JMenu disabledMenu;
    private static JMenuItem disabledMenuItem;

    public JMenuItemsTest() {
        createUI();
    }

    private void createUI() {

        mainFrame = new JFrame("Test");

        disabledMenu = new JMenu("Disabled Menu");
        disabledMenu.setForeground(Color.BLUE);
        disabledMenu.setEnabled(false);

        disabledMenuItem = new JMenuItem("Disabled MenuItem");
        disabledMenuItem.setForeground(Color.BLUE);
        disabledMenuItem.setEnabled(false);

        JMenuBar menuBar = new JMenuBar();
        menuBar = new JMenuBar();
        menuBar.add(disabledMenu);
        menuBar.add(disabledMenuItem);

        mainFrame.add(menuBar);
        mainFrame.pack();
        mainFrame.setVisible(true);
    }

    private void dispose() {
        mainFrame.dispose();
    }

    private void testDisabledStateOfJMenu() {

        // Test disabled JMenu state
        Rectangle rect = disabledMenu.getBounds();
        BufferedImage image = new BufferedImage(rect.width, rect.height,
                BufferedImage.TYPE_INT_ARGB);
        disabledMenu.paint(image.getGraphics());
        int y = image.getHeight() / 2;
        for (int x = 0; x < image.getWidth(); x++) {
            Color c = new Color(image.getRGB(x, y));
            if (c.equals(Color.BLUE)) {
                dispose();
                throw new RuntimeException("JMenu Disabled"
                        + " State not Valid.");
            }
        }

    }

    private void testDisabledStateOfJMenuItem() {

        // Test disabled JMenuItem state
        Rectangle rect = disabledMenuItem.getBounds();
        BufferedImage image = new BufferedImage(rect.width, rect.height,
                BufferedImage.TYPE_INT_ARGB);
        disabledMenuItem.paint(image.getGraphics());
        int y = image.getHeight() / 2;
        for (int x = 0; x < image.getWidth(); x++) {
            Color c = new Color(image.getRGB(x, y));
            if (c.equals(Color.BLUE)) {
                dispose();
                throw new RuntimeException("JMenuItem Disabled"
                        + " State not Valid.");
            }
        }

    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        SwingUtilities.invokeAndWait(() -> {

            try {
                JMenuItemsTest obj = new JMenuItemsTest();
                obj.testDisabledStateOfJMenu();
                obj.testDisabledStateOfJMenuItem();
                obj.dispose();

            } catch (Exception ex) {
                throw ex;
            }

        });
    }
}
