/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4330357
 * @summary Tests that real editor in JTree cleans up after editing was stopped
 * @library ../../regtesthelpers
 * @build Util
 * @author Peter Zhelezniakov
 * @run main bug4330357
 */

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellEditor;

public class bug4330357 {

    private static JFrame frame;
    private static JTree tree;
    private static JButton button;
    private static Robot robot;

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        robot.setAutoDelay(50);

        UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");

        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    createAndShowGUI();
                }
            });

            robot.waitForIdle();

            clickMouse(getTreeRowClickPoint(1));
            Util.hitKeys(robot, KeyEvent.VK_F2);
            Util.hitKeys(robot, KeyEvent.VK_A, KeyEvent.VK_B, KeyEvent.VK_C);
            robot.waitForIdle();

            if (!hasComponent(JTextField.class)) {
                throw new RuntimeException("Cell editor is missed for path: color");
            }


            clickMouse(getButtonClickPoint());
            robot.waitForIdle();

            clickMouse(getTreeRowClickPoint(2));
            Util.hitKeys(robot, KeyEvent.VK_F2);
            robot.waitForIdle();

            if (!hasComponent(JComboBox.class)) {
                throw new RuntimeException("Cell editor is missed for path: sports");
            }

            if (hasComponent(JTextField.class)) {
                throw new RuntimeException("Cell editor is wrongly shown for path: color");
            }
        } finally {
            if (frame != null) {
                SwingUtilities.invokeAndWait(frame::dispose);
            }
        }
    }

    static void clickMouse(Point point) {
        robot.mouseMove(point.x, point.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
    }

    private static Point getTreeRowClickPoint(final int row) throws Exception {
        final Point[] result = new Point[1];

        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {

                Rectangle rect = tree.getRowBounds(row);
                Point p = new Point(rect.x + rect.width / 2, rect.y + 2);
                SwingUtilities.convertPointToScreen(p, tree);
                result[0] = p;
            }
        });

        return result[0];
    }

    private static Point getButtonClickPoint() throws Exception {
        final Point[] result = new Point[1];

        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                Point p = button.getLocationOnScreen();
                Dimension size = button.getSize();
                result[0] = new Point(p.x + size.width / 2, p.y + size.height / 2);
            }
        });
        return result[0];
    }

    static boolean hasComponent(final Class cls) throws Exception {
        final boolean[] result = new boolean[1];

        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                result[0] = Util.findSubComponent(tree, cls.getName()) != null;
            }
        });

        return result[0];
    }

    private static void createAndShowGUI() {
        frame = new JFrame("Test");
        frame.setSize(200, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        tree = new JTree();
        tree.setEditable(true);

        final TestEditor testEditor = new TestEditor();
        tree.setCellEditor(new DefaultTreeCellEditor(tree,
                (DefaultTreeCellRenderer) tree.getCellRenderer(),
                testEditor));

        button = new JButton("stop");

        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                testEditor.stopCellEditing();
            }
        });

        frame.getContentPane().add(new JScrollPane(tree), BorderLayout.CENTER);
        frame.getContentPane().add(button, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    static class TestEditor extends AbstractCellEditor implements TreeCellEditor {

        private JComboBox comboBox;
        private JTextField textField;
        private boolean comboBoxActive;

        TestEditor() {
            comboBox = new JComboBox(new String[]{"one", "two"});
            textField = new JTextField();
        }

        public Component getTreeCellEditorComponent(JTree tree, Object value,
                boolean isSelected,
                boolean expanded,
                boolean leaf, int row) {
            if (row % 2 == 0) {
                comboBoxActive = true;
                return comboBox;
            }
            comboBoxActive = false;
            return textField;
        }

        public Object getCellEditorValue() {
            if (comboBoxActive) {
                return comboBox.getSelectedItem();
            }
            return textField.getText();
        }
    }
}
