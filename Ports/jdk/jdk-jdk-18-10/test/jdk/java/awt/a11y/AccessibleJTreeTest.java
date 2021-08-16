/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, JetBrains s.r.o.. All rights reserved.
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
 * @bug 8267387
 * @summary Test implementation of NSAccessibilityOutLine protocol peer
 * @author Artem.Semenov@jetbrains.com
 * @run main/manual AccessibleJTreeTest
 * @requires (os.family == "windows" | os.family == "mac")
 */

import javax.swing.JTree;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.SwingUtilities;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AccessibleJTreeTest extends AccessibleComponentTest {

    @Override
    public CountDownLatch createCountDownLatch() {
        return new CountDownLatch(1);
    }

    public void createSampleTree() {
        INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of JTree.\n\n"
                + "Turn screen reader on, and Tab to the tree.\n"
                + "Press the arrow buttons to move through the tree.\n\n"
                + "If you can hear tree components tab further and press PASS, otherwise press FAIL.\n";

        String root = "Root";
        String[] nodes = new String[] {"One node", "Two node"};
        String[][] leafs = new String[][]{{"leaf 1.1", "leaf 1.2", "leaf 1.3", "leaf 1.4"},
                {"leaf 2.1", "leaf 2.2", "leaf 2.3", "leaf 2.4"}};

        Hashtable<String, String[]> data = new Hashtable<String, String[]>();
        for (int i = 0; i < nodes.length; i++) {
            data.put(nodes[i], leafs[i]);
        }

        JTree tree = new JTree(data);
        tree.setRootVisible(true);

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        JScrollPane scrollPane = new JScrollPane(tree);
        panel.add(scrollPane);
        panel.setFocusable(false);
        exceptionString = "AccessibleJTree sample item test failed!";
        super.createUI(panel, "AccessibleJTreeTest");
    }

    public void createSampleTreeUnvisableRoot() {
        INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of JTree with invisible root.\n\n"
                + "Turn screen reader on, and Tab to the tree.\n"
                + "Press the arrow buttons to move through the tree.\n\n"
                + "If you can hear tree components tab further and press PASS, otherwise press FAIL.\n";

        String root = "Root";
        String[] nodes = new String[] {"One node", "Two node"};
        String[][] leafs = new String[][]{{"leaf 1.1", "leaf 1.2", "leaf 1.3", "leaf 1.4"},
                {"leaf 2.1", "leaf 2.2", "leaf 2.3", "leaf 2.4"}};

        Hashtable<String, String[]> data = new Hashtable<String, String[]>();
        for (int i = 0; i < nodes.length; i++) {
            data.put(nodes[i], leafs[i]);
        }

        JTree tree = new JTree(data);

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        JScrollPane scrollPane = new JScrollPane(tree);
        panel.add(scrollPane);
        panel.setFocusable(false);
        exceptionString = "AccessibleJTree sample item invisible root test failed!";
        super.createUI(panel, "AccessibleJTreeTest");
    }

    public void createSampleTreeNamed() {
        INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of named JTree.\n\n"
                + "Turn screen reader on, and Tab to the tree.\n"
                + "Press the tab button to move to second tree.\\n\n"
                + "If you can hear second tree name: \"second tree\" - tab further and press PASS, otherwise press FAIL.\n";

        String root = "Root";
        String[] nodes = new String[] {"One node", "Two node"};
        String[][] leafs = new String[][]{{"leaf 1.1", "leaf 1.2", "leaf 1.3", "leaf 1.4"},
                {"leaf 2.1", "leaf 2.2", "leaf 2.3", "leaf 2.4"}};

        Hashtable<String, String[]> data = new Hashtable<String, String[]>();
        for (int i = 0; i < nodes.length; i++) {
            data.put(nodes[i], leafs[i]);
        }

        JTree tree = new JTree(data);
        JTree secondTree = new JTree(data);
        secondTree.getAccessibleContext().setAccessibleName("Second tree");
        tree.setRootVisible(true);

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        JScrollPane scrollPane = new JScrollPane(tree);
        JScrollPane secondScrollPane = new JScrollPane(secondTree);
        panel.add(scrollPane);
        panel.add(secondScrollPane);
        panel.setFocusable(false);
        exceptionString = "AccessibleJTree named test failed!";
        super.createUI(panel, "AccessibleJTreeTest");
    }


    public void createRendererTree() {
        INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of JTree using renderer.\n\n"
                + "Turn screen reader on, and Tab to the tree.\n"
                + "Press the arrow buttons to move through the tree.\n\n"
                + "If you can hear tree components tab further and press PASS, otherwise press FAIL.\n";

        String root = "Root";
        String[] nodes = new String[] {"One node", "Two node"};
        String[][] leafs = new String[][]{{"leaf 1.1", "leaf 1.2", "leaf 1.3", "leaf 1.4"},
                {"leaf 2.1", "leaf 2.2", "leaf 2.3", "leaf 2.4"}};

        Hashtable<String, String[]> data = new Hashtable<String, String[]>();
        for (int i = 0; i < nodes.length; i++) {
            data.put(nodes[i], leafs[i]);
        }

        JTree tree = new JTree(data);
        tree.setRootVisible(true);
        tree.setCellRenderer(new AccessibleJTreeTestRenderer());

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        JScrollPane scrollPane = new JScrollPane(tree);
        panel.add(scrollPane);
        panel.setFocusable(false);
        exceptionString = "AccessibleJTree renderer item test failed!";
        super.createUI(panel, "AccessibleJTreeTest");
    }

    public static void main(String[] args) throws Exception {
        AccessibleJTreeTest test = new AccessibleJTreeTest();

        countDownLatch = test.createCountDownLatch();
        SwingUtilities.invokeAndWait(test::createSampleTree);
        AccessibleComponentTest.countDownLatch.await(15, TimeUnit.MINUTES);
        if (!testResult) {
            throw new RuntimeException(exceptionString);
        }

        countDownLatch = test.createCountDownLatch();
        SwingUtilities.invokeAndWait(test::createSampleTreeNamed);
        AccessibleComponentTest.countDownLatch.await(15, TimeUnit.MINUTES);
        if (!testResult) {
            throw new RuntimeException(exceptionString);
        }


        countDownLatch = test.createCountDownLatch();
        SwingUtilities.invokeAndWait(test::createSampleTreeUnvisableRoot);
        AccessibleComponentTest.countDownLatch.await(15, TimeUnit.MINUTES);
        if (!testResult) {
            throw new RuntimeException(exceptionString);
        }

        countDownLatch = test.createCountDownLatch();
        SwingUtilities.invokeAndWait(test::createRendererTree);
        countDownLatch.await(15, TimeUnit.MINUTES);
        if (!testResult) {
            throw new RuntimeException(AccessibleComponentTest.exceptionString);
        }
    }

    public static class AccessibleJTreeTestRenderer extends JPanel implements TreeCellRenderer {
        private JLabel labelAJT = new JLabel("AJT");
        private JLabel itemName = new JLabel();

        AccessibleJTreeTestRenderer() {
            super(new FlowLayout());
            setFocusable(false);
            layoutComponents();
        }

        private void layoutComponents() {
            add(labelAJT);
            add(itemName);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                itemName.setText((String) (((DefaultMutableTreeNode) value).getUserObject()));

            getAccessibleContext().setAccessibleName(labelAJT.getText() + ", " + itemName.getText());
            return this;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension(Math.min(size.width, 245), size.height);
        }
    }
}
