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
   @test
   @key headful
   @bug 4927934
   @summary JTree traversal is unlike Native windows tree traversal
   @author Andrey Pikalev
   @run main bug4927934
*/

import java.awt.Robot;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.JScrollPane;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.tree.DefaultMutableTreeNode;

public class bug4927934 implements TreeSelectionListener, TreeExpansionListener, FocusListener {

    final static Object listener = new bug4927934();

    static boolean focusGained = false;
    public static boolean selectionChanged = false;
    public static boolean treeExpanded = false;
    public static boolean treeCollapsed = false;

    static JFrame frame;
    static JTree tree;
    static Robot robot;

    public static void main(String args[]) throws Exception {
        UIManager.setLookAndFeel(new javax.swing.plaf.metal.MetalLookAndFeel());

        robot = new Robot();
        robot.setAutoDelay(50);

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame = new JFrame();

                    DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
                    createNodes(root);
                    tree = new JTree(root);
                    JScrollPane scrollPane = new JScrollPane(tree);
                    frame.getContentPane().add(scrollPane);

                    tree.addFocusListener((FocusListener) listener);
                    tree.addTreeSelectionListener((TreeSelectionListener) listener);
                    tree.addTreeExpansionListener((TreeExpansionListener) listener);

                    frame.setSize(300, 300);
                    frame.setVisible(true);
                }
            });

            robot.waitForIdle();
            Thread.sleep(1000);

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    tree.requestFocus();
                }
            });

            synchronized (listener) {
                if (!focusGained) {
                    System.out.println("waiting focusGained...");
                    try {
                        listener.wait(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            // GO TO RIGHT
            selectionChanged = false;
            hitKey(KeyEvent.VK_RIGHT);
            robot.waitForIdle();
            if (!checkSelectionChanged(tree, 0)) {
                throw new RuntimeException("Root should be selected");
            }

            selectionChanged = false;
            hitKey(KeyEvent.VK_RIGHT);
            robot.waitForIdle();
            if (!checkSelectionChanged(tree, 1)) {
                throw new RuntimeException("Node should be selected");
            }

            treeExpanded = false;
            hitKey(KeyEvent.VK_RIGHT);
            robot.waitForIdle();
            if (!isTreeExpanded()) {
                throw new RuntimeException("Node should be expanded");
            }

            selectionChanged = false;
            hitKey(KeyEvent.VK_RIGHT);
            robot.waitForIdle();
            if (!checkSelectionChanged(tree, 2)) {
                throw new RuntimeException("Leaf1 should be selected");
            }

            selectionChanged = false;
            hitKey(KeyEvent.VK_RIGHT);
            robot.waitForIdle();
            if (!checkSelectionChanged(tree, 2)) {
                throw new RuntimeException("Leaf1 should be selected");
            }

            // GO TO LEFT
            selectionChanged = false;
            hitKey(KeyEvent.VK_LEFT);
            robot.waitForIdle();
            if (!checkSelectionChanged(tree, 1)) {
                throw new RuntimeException("Node should be selected");
            }

            treeCollapsed = false;
            hitKey(KeyEvent.VK_LEFT);
            if (!isTreeCollapsed()) {
                throw new RuntimeException("Node should be collapsed");
            }

            selectionChanged = false;
            hitKey(KeyEvent.VK_LEFT);
            robot.waitForIdle();
            if (!checkSelectionChanged(tree, 0)) {
                throw new RuntimeException("Root should be selected");
            }

            treeCollapsed = false;
            hitKey(KeyEvent.VK_LEFT);
            robot.waitForIdle();
            if (!isTreeCollapsed()) {
                throw new RuntimeException("Root should be collapsed");
            }
        } finally {
            if (frame != null) {
                SwingUtilities.invokeAndWait(frame::dispose);
            }
        }
    }


    synchronized public void focusLost(FocusEvent e) {
    }

    synchronized public void focusGained(FocusEvent e) {
        focusGained = true;
        System.out.println("focusGained");
        listener.notifyAll();
    }

    private static void createNodes(DefaultMutableTreeNode root) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode("Node");
        node.add(new DefaultMutableTreeNode("Leaf1"));
        node.add(new DefaultMutableTreeNode("Leaf2"));
        root.add(node);
        root.add(new DefaultMutableTreeNode("Leaf3"));
    }

    synchronized public void valueChanged(TreeSelectionEvent e) {
        selectionChanged = true;
        System.out.println("selectionChanged");
        notifyAll();
    }

    synchronized public void treeCollapsed(TreeExpansionEvent e) {
        System.out.println("treeCollapsed");
        treeCollapsed = true;
        notifyAll();
    }

    synchronized public void treeExpanded(TreeExpansionEvent e) {
        System.out.println("treeExpanded");
        treeExpanded = true;
        notifyAll();
    }

    private static void hitKey(int key) {
        System.out.println("key " + key + " pressed");
        robot.keyPress(key);
        robot.keyRelease(key);
    }

    private static boolean checkSelectionChanged(JTree tree, int shouldBeSel) {
        synchronized(listener) {
            if (!selectionChanged) {
                System.out.println("waiting for selectionChanged...");
                try {
                    listener.wait(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        int selRow = tree.getLeadSelectionRow();
        System.out.println("Selected row: " + selRow);
        return selRow == shouldBeSel;
    }

    private static boolean isTreeExpanded() {
        synchronized(listener) {
            if (!treeExpanded) {
                System.out.println("waiting for treeExpanded...");
                try {
                    listener.wait(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return treeExpanded;
    }

    private static boolean isTreeCollapsed() {
        synchronized(listener) {
            if (!treeCollapsed) {
                System.out.println("waiting for treeCollapsed...");
                try {
                    listener.wait(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return treeCollapsed;
    }
}
