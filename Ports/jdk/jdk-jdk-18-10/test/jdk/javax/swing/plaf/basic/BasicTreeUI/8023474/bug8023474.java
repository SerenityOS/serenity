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
 * @bug 8023474
 * @summary Tests that the first mouse press starts editing in JTree
 * @author Dmitry Markov
 * @run main bug8023474
 */

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.EventObject;

public class bug8023474 {
    private static JTree tree;

    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        robot.setAutoDelay(50);

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });

        robot.waitForIdle();

        Point point = getRowPointToClick(1);
        robot.mouseMove(point.x, point.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        robot.waitForIdle();

        Boolean result = (Boolean)tree.getCellEditor().getCellEditorValue();
        if (!result) {
            throw new RuntimeException("Test Failed!");
        }
    }

    private static void createAndShowGUI() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode item = new DefaultMutableTreeNode("item");
        DefaultMutableTreeNode subItem = new DefaultMutableTreeNode("subItem");

        root.add(item);
        item.add(subItem);

        DefaultTreeModel model = new DefaultTreeModel(root);
        tree = new JTree(model);

        tree.setCellEditor(new Editor());
        tree.setEditable(true);
        tree.setRowHeight(30);
        tree.setCellRenderer(new CheckboxCellRenderer());

        JFrame frame = new JFrame("bug8023474");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new JScrollPane(tree));
        frame.setSize(400, 300);
        frame.setVisible(true);
    }

    private static Point getRowPointToClick(final int row) throws Exception {
        final Point[] result = new Point[1];

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                Rectangle rect = tree.getRowBounds(row);
                Point point = new Point(rect.x + 10, rect.y + rect.height / 2);
                SwingUtilities.convertPointToScreen(point, tree);
                result[0] = point;
            }
        });
        return result[0];
    }

    private static class Editor extends JPanel implements TreeCellEditor {
        private JCheckBox checkbox;

        public Editor() {
            setOpaque(false);
            checkbox = new JCheckBox();
            add(checkbox);
        }

        public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected,
                                                    boolean expanded, boolean leaf, int row) {
            checkbox.setText(value.toString());
            checkbox.setSelected(false);
            return this;
        }

        public Object getCellEditorValue() {
            return checkbox.isSelected();
        }

        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }

        public boolean shouldSelectCell(EventObject anEvent) {
            return true;
        }

        public boolean stopCellEditing() {
            return true;
        }

        public void cancelCellEditing() {
        }

        public void addCellEditorListener(CellEditorListener l) {
        }

        public void removeCellEditorListener(CellEditorListener l) {
        }
    }

    private static class CheckboxCellRenderer extends JPanel implements TreeCellRenderer {
        private JCheckBox checkbox;

        public CheckboxCellRenderer() {
            setOpaque(false);
            checkbox = new JCheckBox();
            add(checkbox);
        }

        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            checkbox.setText(value.toString());
            checkbox.setSelected(false);
            return this;
        }
    }
}
