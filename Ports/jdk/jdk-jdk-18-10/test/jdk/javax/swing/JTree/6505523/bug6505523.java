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
 * @bug 6505523
 * @summary NullPointerException in BasicTreeUI when a node is removed by expansion listener
 * @author Alexandr Scherbatiy
 * @run main bug6505523
 */
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

public class bug6505523 {

    private static JTree tree;

    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        robot.setAutoDelay(50);

        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                createAndShowGUI();
            }
        });

        robot.waitForIdle();

        Point point = getRowPointToClick(2);
        robot.mouseMove(point.x, point.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        robot.waitForIdle();

    }

    private static Point getRowPointToClick(final int row) throws Exception {

        final Point[] result = new Point[1];

        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                Rectangle rect = tree.getRowBounds(row);
                Point point = new Point(rect.x - 5, rect.y + rect.height / 2);
                SwingUtilities.convertPointToScreen(point, tree);
                result[0] = point;
            }
        });

        return result[0];
    }

    private static void createAndShowGUI() {
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode("Problem with NPE under JDK 1.6");
        final DefaultMutableTreeNode problematic = new DefaultMutableTreeNode("Expand me and behold a NPE in stderr");
        problematic.add(new DefaultMutableTreeNode("some content"));
        root.add(new DefaultMutableTreeNode("irrelevant..."));
        root.add(problematic);

        final DefaultTreeModel model = new DefaultTreeModel(root);
        tree = new JTree(model);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.expandRow(0);
        tree.collapseRow(2);

        // this is critical - without dragEnabled everything works
        tree.setDragEnabled(true);

        tree.addTreeExpansionListener(new TreeExpansionListener() {

            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                TreeNode parent = problematic.getParent();
                if (parent instanceof DefaultMutableTreeNode) {
                    model.removeNodeFromParent(problematic);
                }
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
            }
        });

        JFrame frame = new JFrame("JTree Problem");
        frame.add(new JScrollPane(tree));
        frame.setSize(500, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
