/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import java.nio.file.Files;
import javax.swing.AbstractButton;
import javax.swing.JTable;
import javax.swing.UIManager;

/**
 * @test
 * @key headful
 * @bug 7199708 8159587 8198005
 * @author Alexander Scherbatiy
 * @summary FileChooser crashs when opening large folder
 * @run main/timeout=240 bug7199708
 */
public class bug7199708 {

    private static int FILE_NUMBER = 30000;
    private static volatile JFileChooser fileChooser;
    private static volatile int locationX;
    private static volatile int locationY;
    private static volatile int width;
    private static File largeFolder;
    private static File files[] = new File[FILE_NUMBER];

    public static void main(String[] args) throws Exception {

        Robot robot = new Robot();
        robot.setAutoDelay(50);

        try {
            final File folder = createLargeFolder();
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    fileChooser = new JFileChooser(folder);
                    fileChooser.showSaveDialog(null);
                }
            });

            robot.waitForIdle();

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    final String detailsTooltip =
                        UIManager.getString("FileChooser."
                        + "detailsViewButtonToolTipText",
                          fileChooser.getLocale());

                    doAction(fileChooser, new ComponentAction() {
                        @Override
                        public boolean accept(Component component) {
                            return (component instanceof AbstractButton)
                                && detailsTooltip.equals(
                                ((AbstractButton) component).getToolTipText());
                        }

                        @Override
                        public void perform(Component component) {
                            ((AbstractButton) component).doClick();
                        }
                    });

                    doAction(fileChooser, new ComponentAction() {
                        @Override
                        public boolean accept(Component component) {
                            return (component instanceof JTable);
                        }

                        @Override
                        public void perform(Component component) {
                            Point tableLocation = component.getLocationOnScreen();
                            locationX = (int) tableLocation.getX();
                            locationY = (int) tableLocation.getY();
                            width = (int) fileChooser.getBounds().getWidth();
                        }
                    });
                }
            });

            robot.waitForIdle();

            int d = 25;
            for (int i = 0; i < width / d; i++) {
                robot.mouseMove(locationX + i * d, locationY + 5);
                robot.waitForIdle();
                robot.mousePress(InputEvent.BUTTON1_MASK);
                robot.waitForIdle();
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
                robot.waitForIdle();
            }

            robot.keyPress(KeyEvent.VK_ESCAPE);
            robot.waitForIdle();
            robot.keyRelease(KeyEvent.VK_ESCAPE);
            robot.waitForIdle();

        } finally {
            for (int i = 0; i < FILE_NUMBER; i++) {
                Files.delete(files[i].toPath());
            }
            Files.delete(largeFolder.toPath());
        }
    }

    static void doAction(Component component, ComponentAction action) {
        if (action.accept(component)) {
            action.perform(component);
        } else if (component instanceof Container) {
            for (Component comp : ((Container) component).getComponents()) {
                doAction(comp, action);
            }
        }
    }

    private static File createLargeFolder() {
        try {

            largeFolder = Files.createTempDirectory("large_folder").toFile();

            for (int i = 0; i < FILE_NUMBER; i++) {
                files[i] = new File(largeFolder, "File_" + i + ".txt");
                files[i].createNewFile();
            }
            return largeFolder;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    interface ComponentAction {

        boolean accept(Component component);

        void perform(Component component);
    }
}
