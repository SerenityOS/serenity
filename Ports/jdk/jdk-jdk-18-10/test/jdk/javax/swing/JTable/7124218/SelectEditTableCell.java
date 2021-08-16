/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7124218
 * @summary verifies different behaviour of SPACE and ENTER in JTable
 * @library ../../regtesthelpers
 * @build Util
 * @run main SelectEditTableCell
 */
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class SelectEditTableCell {

    private static JFrame frame;
    private static JTable table;
    private static Robot robot;

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        robot.setAutoDelay(100);
        UIManager.LookAndFeelInfo[] lookAndFeelArray
                = UIManager.getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo lookAndFeelItem : lookAndFeelArray) {
            executeCase(lookAndFeelItem.getClassName());
        }
    }

    private static void executeCase(String lookAndFeelString) throws Exception {
        try {
            if (tryLookAndFeel(lookAndFeelString)) {
                createUI(lookAndFeelString);
                robot.delay(2000);
                runTestCase();
                robot.delay(2000);
                cleanUp();
                robot.delay(2000);
            }
        } finally {
            if (frame != null) {
                SwingUtilities.invokeAndWait(frame::dispose);
            }
        }

    }

    private static void createUI(final String lookAndFeelString)
            throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                String[][] data = {{"Foo"}};
                String[] cols = {"One"};
                table = new JTable(data, cols);
                table.setSelectionMode(
                        DefaultListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                frame = new JFrame(lookAndFeelString);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.getContentPane().add(table);
                frame.pack();
                frame.setSize(500, frame.getSize().height);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                frame.toFront();
            }
        });
    }

    private static void runTestCase() throws Exception {
        Point centerPoint;
        centerPoint = Util.getCenterPoint(table);
        LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
        robot.mouseMove(centerPoint.x, centerPoint.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.waitForIdle();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                table.clearSelection();
                if (table.isEditing() || table.isCellSelected(0, 0)) {
                    // assumption is bad, bail
                    frame.dispose();
                    throw new AssertionError("Failed assumption: assumed no"
                            + "editing and no selection.");
                }
            }
        });

        int fetchKeyCode;
        keyTap(fetchKeyCode = isMac(lookAndFeel)
                ? KeyEvent.VK_ENTER : KeyEvent.VK_SPACE);
        final int keyCode = fetchKeyCode;
        robot.waitForIdle();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                if (!table.isCellSelected(0, 0)) {
                    frame.dispose();
                    throw new RuntimeException(((keyCode == KeyEvent.VK_ENTER)
                            ? "Enter" : "Space")
                            + " should select cell");
                }
            }
        });

        keyTap(KeyEvent.VK_SPACE);
        robot.waitForIdle();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                if (!table.isEditing()) {
                    frame.dispose();
                    throw new RuntimeException("Space should start editing");
                }
                table.getCellEditor().cancelCellEditing();
                table.clearSelection();
                if (table.isEditing() || table.isCellSelected(0, 0)) {
                    // assumption is bad, bail
                    frame.dispose();
                    throw new AssertionError("Failed assumption: assumed no "
                            + "editing and no selection.");
                }
            }
        });

        // hitting a letter key will start editing
        keyTap(KeyEvent.VK_A);
        robot.waitForIdle();
        keyTap(KeyEvent.VK_SPACE);
        robot.waitForIdle();
        keyTap(KeyEvent.VK_A);
        robot.waitForIdle();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                if (table.isCellSelected(0, 0)) {
                    frame.dispose();
                    throw new RuntimeException("Space should not select when "
                            + "already editing.");
                }
            }
        });
    }

    private static void cleanUp() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame.dispose();
            }
        });
    }

    private static boolean isMac(LookAndFeel lookAndFeel) {

        return lookAndFeel.toString().toLowerCase().contains("mac");
    }

    private static void keyTap(int keyCode) {
        robot.keyPress(keyCode);
        robot.keyRelease(keyCode);
    }

    private static boolean tryLookAndFeel(String lookAndFeelString)
            throws Exception {
        try {
            UIManager.setLookAndFeel(
                    lookAndFeelString);

        } catch (UnsupportedLookAndFeelException
                | ClassNotFoundException
                | InstantiationException
                | IllegalAccessException e) {
            return false;
        }
        return true;
    }
}
