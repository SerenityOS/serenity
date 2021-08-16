/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8004298
 * @requires (os.family == "windows")
 * @summary NPE in WindowsTreeUI.ensureRowsAreVisible
 * @author Alexander Scherbatiy
 * @library ../../regtesthelpers
 * @modules java.desktop/com.sun.java.swing.plaf.windows
 * @build Util
 * @run main bug8004298
 */

import java.awt.*;
import java.awt.event.InputEvent;
import javax.swing.*;
import javax.swing.tree.*;
import java.util.concurrent.Callable;
import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;
import com.sun.java.swing.plaf.windows.WindowsTreeUI;

public class bug8004298 {

    private static JTree tree;

    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        robot.setAutoDelay(50);
        try {
            UIManager.setLookAndFeel(new WindowsLookAndFeel());
        } catch (javax.swing.UnsupportedLookAndFeelException ulafe) {
            System.out.println(ulafe.getMessage());
            System.out.println("The test is considered PASSED");
            return;
        }
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                createAndShowGUI();
            }
        });

        robot.waitForIdle();

        Point point = Util.invokeOnEDT(new Callable<Point>() {

            @Override
            public Point call() throws Exception {
                Rectangle rect = tree.getRowBounds(2);
                Point p = new Point(rect.x + rect.width / 2, rect.y + rect.height / 2);
                SwingUtilities.convertPointToScreen(p, tree);
                return p;
            }
        });

        robot.mouseMove(point.x, point.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.waitForIdle();

    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        root.add(new DefaultMutableTreeNode("colors"));
        DefaultMutableTreeNode sports = new DefaultMutableTreeNode("sports");
        sports.add(new DefaultMutableTreeNode("basketball"));
        sports.add(new DefaultMutableTreeNode("football"));
        root.add(sports);

        tree = new JTree(root);
        tree.setUI(new NullReturningTreeUI());

        frame.getContentPane().add(tree);
        frame.pack();
        frame.setVisible(true);

    }

    private static final class NullReturningTreeUI extends WindowsTreeUI {

        @Override
        public Rectangle getPathBounds(JTree tree, TreePath path) {
            // the method can return null and callers have to be ready for
            // that. Simulate the case by returning null for unknown reason.
            if (path != null && path.toString().contains("football")) {
                return null;
            }

            return super.getPathBounds(tree, path);
        }
    }
}
