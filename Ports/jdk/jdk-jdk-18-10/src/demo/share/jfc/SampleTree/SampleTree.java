/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */



import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.*;
import javax.swing.tree.*;


/**
 * A demo for illustrating how to do different things with JTree.
 * The data that this displays is rather boring, that is each node will
 * have 7 children that have random names based on the fonts.  Each node
 * is then drawn with that font and in a different color.
 * While the data isn't interesting the example illustrates a number
 * of things:
 *
 * For an example of dynamicaly loading children refer to DynamicTreeNode.
 * For an example of adding/removing/inserting/reloading refer to the inner
 *     classes of this class, AddAction, RemovAction, InsertAction and
 *     ReloadAction.
 * For an example of creating your own cell renderer refer to
 *     SampleTreeCellRenderer.
 * For an example of subclassing JTreeModel for editing refer to
 *     SampleTreeModel.
 *
 * @author Scott Violet
 */
public final class SampleTree {

    /** Window for showing Tree. */
    protected JFrame frame;
    /** Tree used for the example. */
    protected JTree tree;
    /** Tree model. */
    protected DefaultTreeModel treeModel;

    /**
     * Constructs a new instance of SampleTree.
     */
    public SampleTree() {
        // Trying to set Nimbus look and feel
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        JMenuBar menuBar = constructMenuBar();
        JPanel panel = new JPanel(true);

        frame = new JFrame("SampleTree");
        frame.getContentPane().add("Center", panel);
        frame.setJMenuBar(menuBar);
        frame.setBackground(Color.lightGray);

        /* Create the JTreeModel. */
        DefaultMutableTreeNode root = createNewNode("Root");
        treeModel = new SampleTreeModel(root);

        /* Create the tree. */
        tree = new JTree(treeModel);

        /* Enable tool tips for the tree, without this tool tips will not
        be picked up. */
        ToolTipManager.sharedInstance().registerComponent(tree);

        /* Make the tree use an instance of SampleTreeCellRenderer for
        drawing. */
        tree.setCellRenderer(new SampleTreeCellRenderer());

        /* Make tree ask for the height of each row. */
        tree.setRowHeight(-1);

        /* Put the Tree in a scroller. */
        JScrollPane sp = new JScrollPane();
        sp.setPreferredSize(new Dimension(300, 300));
        sp.getViewport().add(tree);

        /* And show it. */
        panel.setLayout(new BorderLayout());
        panel.add("Center", sp);
        panel.add("South", constructOptionsPanel());

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    /** Constructs a JPanel containing check boxes for the different
     * options that tree supports. */
    @SuppressWarnings("serial")
    private JPanel constructOptionsPanel() {
        JCheckBox aCheckbox;
        JPanel retPanel = new JPanel(false);
        JPanel borderPane = new JPanel(false);

        borderPane.setLayout(new BorderLayout());
        retPanel.setLayout(new FlowLayout());

        aCheckbox = new JCheckBox("show top level handles");
        aCheckbox.setSelected(tree.getShowsRootHandles());
        aCheckbox.addChangeListener(new ShowHandlesChangeListener());
        retPanel.add(aCheckbox);

        aCheckbox = new JCheckBox("show root");
        aCheckbox.setSelected(tree.isRootVisible());
        aCheckbox.addChangeListener(new ShowRootChangeListener());
        retPanel.add(aCheckbox);

        aCheckbox = new JCheckBox("editable");
        aCheckbox.setSelected(tree.isEditable());
        aCheckbox.addChangeListener(new TreeEditableChangeListener());
        aCheckbox.setToolTipText("Triple click to edit");
        retPanel.add(aCheckbox);

        borderPane.add(retPanel, BorderLayout.CENTER);

        /* Create a set of radio buttons that dictate what selection should
        be allowed in the tree. */
        ButtonGroup group = new ButtonGroup();
        JPanel buttonPane = new JPanel(false);
        JRadioButton button;

        buttonPane.setLayout(new FlowLayout());
        buttonPane.setBorder(new TitledBorder("Selection Mode"));
        button = new JRadioButton("Single");
        button.addActionListener(new AbstractAction() {

            @Override
            public boolean isEnabled() {
                return true;
            }

            public void actionPerformed(ActionEvent e) {
                tree.getSelectionModel().setSelectionMode(
                        TreeSelectionModel.SINGLE_TREE_SELECTION);
            }
        });
        group.add(button);
        buttonPane.add(button);
        button = new JRadioButton("Contiguous");
        button.addActionListener(new AbstractAction() {

            @Override
            public boolean isEnabled() {
                return true;
            }

            public void actionPerformed(ActionEvent e) {
                tree.getSelectionModel().setSelectionMode(
                        TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
            }
        });
        group.add(button);
        buttonPane.add(button);
        button = new JRadioButton("Discontiguous");
        button.addActionListener(new AbstractAction() {

            @Override
            public boolean isEnabled() {
                return true;
            }

            public void actionPerformed(ActionEvent e) {
                tree.getSelectionModel().setSelectionMode(
                        TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
            }
        });
        button.setSelected(true);
        group.add(button);
        buttonPane.add(button);

        borderPane.add(buttonPane, BorderLayout.SOUTH);

        // NOTE: This will be enabled in a future release.
        // Create a label and combobox to determine how many clicks are
        // needed to expand.
/*
        JPanel               clickPanel = new JPanel();
        Object[]             values = { "Never", new Integer(1),
        new Integer(2), new Integer(3) };
        final JComboBox      clickCBox = new JComboBox(values);

        clickPanel.setLayout(new FlowLayout());
        clickPanel.add(new JLabel("Click count to expand:"));
        clickCBox.setSelectedIndex(2);
        clickCBox.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
        Object       selItem = clickCBox.getSelectedItem();

        if(selItem instanceof Integer)
        tree.setToggleClickCount(((Integer)selItem).intValue());
        else // Don't toggle
        tree.setToggleClickCount(0);
        }
        });
        clickPanel.add(clickCBox);
        borderPane.add(clickPanel, BorderLayout.NORTH);
         */
        return borderPane;
    }

    /** Construct a menu. */
    private JMenuBar constructMenuBar() {
        JMenu menu;
        JMenuBar menuBar = new JMenuBar();
        JMenuItem menuItem;

        /* Good ol exit. */
        menu = new JMenu("File");
        menuBar.add(menu);

        menuItem = menu.add(new JMenuItem("Exit"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        /* Tree related stuff. */
        menu = new JMenu("Tree");
        menuBar.add(menu);

        menuItem = menu.add(new JMenuItem("Add"));
        menuItem.addActionListener(new AddAction());

        menuItem = menu.add(new JMenuItem("Insert"));
        menuItem.addActionListener(new InsertAction());

        menuItem = menu.add(new JMenuItem("Reload"));
        menuItem.addActionListener(new ReloadAction());

        menuItem = menu.add(new JMenuItem("Remove"));
        menuItem.addActionListener(new RemoveAction());

        return menuBar;
    }

    /**
     * Returns the TreeNode instance that is selected in the tree.
     * If nothing is selected, null is returned.
     */
    protected DefaultMutableTreeNode getSelectedNode() {
        TreePath selPath = tree.getSelectionPath();

        if (selPath != null) {
            return (DefaultMutableTreeNode) selPath.getLastPathComponent();
        }
        return null;
    }

    /**
     * Returns the selected TreePaths in the tree, may return null if
     * nothing is selected.
     */
    protected TreePath[] getSelectedPaths() {
        return tree.getSelectionPaths();
    }

    protected DefaultMutableTreeNode createNewNode(String name) {
        return new DynamicTreeNode(new SampleData(null, Color.black, name));
    }


    /**
     * AddAction is used to add a new item after the selected item.
     */
    class AddAction extends Object implements ActionListener {

        /** Number of nodes that have been added. */
        public int addCount;

        /**
         * Messaged when the user clicks on the Add menu item.
         * Determines the selection from the Tree and adds an item
         * after that.  If nothing is selected, an item is added to
         * the root.
         */
        public void actionPerformed(ActionEvent e) {
            DefaultMutableTreeNode lastItem = getSelectedNode();
            DefaultMutableTreeNode parent;

            /* Determine where to create the new node. */
            if (lastItem != null) {
                parent = (DefaultMutableTreeNode) lastItem.getParent();
                if (parent == null) {
                    parent = (DefaultMutableTreeNode) treeModel.getRoot();
                    lastItem = null;
                }
            } else {
                parent = (DefaultMutableTreeNode) treeModel.getRoot();
            }
            if (parent == null) {
                // new root
                treeModel.setRoot(createNewNode("Added " + Integer.toString(
                        addCount++)));
            } else {
                int newIndex;
                if (lastItem == null) {
                    newIndex = treeModel.getChildCount(parent);
                } else {
                    newIndex = parent.getIndex(lastItem) + 1;
                }

                /* Let the treemodel know. */
                treeModel.insertNodeInto(createNewNode("Added " + Integer.
                        toString(addCount++)),
                        parent, newIndex);
            }
        }
    } // End of SampleTree.AddAction


    /**
     * InsertAction is used to insert a new item before the selected item.
     */
    class InsertAction extends Object implements ActionListener {

        /** Number of nodes that have been added. */
        public int insertCount;

        /**
         * Messaged when the user clicks on the Insert menu item.
         * Determines the selection from the Tree and inserts an item
         * after that.  If nothing is selected, an item is added to
         * the root.
         */
        public void actionPerformed(ActionEvent e) {
            DefaultMutableTreeNode lastItem = getSelectedNode();
            DefaultMutableTreeNode parent;

            /* Determine where to create the new node. */
            if (lastItem != null) {
                parent = (DefaultMutableTreeNode) lastItem.getParent();
                if (parent == null) {
                    parent = (DefaultMutableTreeNode) treeModel.getRoot();
                    lastItem = null;
                }
            } else {
                parent = (DefaultMutableTreeNode) treeModel.getRoot();
            }
            if (parent == null) {
                // new root
                treeModel.setRoot(createNewNode("Inserted " + Integer.toString(
                        insertCount++)));
            } else {
                int newIndex;

                if (lastItem == null) {
                    newIndex = treeModel.getChildCount(parent);
                } else {
                    newIndex = parent.getIndex(lastItem);
                }

                /* Let the treemodel know. */
                treeModel.insertNodeInto(createNewNode("Inserted " + Integer.
                        toString(insertCount++)),
                        parent, newIndex);
            }
        }
    } // End of SampleTree.InsertAction


    /**
     * ReloadAction is used to reload from the selected node.  If nothing
     * is selected, reload is not issued.
     */
    class ReloadAction extends Object implements ActionListener {

        /**
         * Messaged when the user clicks on the Reload menu item.
         * Determines the selection from the Tree and asks the treemodel
         * to reload from that node.
         */
        public void actionPerformed(ActionEvent e) {
            DefaultMutableTreeNode lastItem = getSelectedNode();

            if (lastItem != null) {
                treeModel.reload(lastItem);
            }
        }
    } // End of SampleTree.ReloadAction


    /**
     * RemoveAction removes the selected node from the tree.  If
     * The root or nothing is selected nothing is removed.
     */
    class RemoveAction extends Object implements ActionListener {

        /**
         * Removes the selected item as long as it isn't root.
         */
        public void actionPerformed(ActionEvent e) {
            TreePath[] selected = getSelectedPaths();

            if (selected != null && selected.length > 0) {
                TreePath shallowest;

                // The remove process consists of the following steps:
                // 1 - find the shallowest selected TreePath, the shallowest
                //     path is the path with the smallest number of path
                //     components.
                // 2 - Find the siblings of this TreePath
                // 3 - Remove from selected the TreePaths that are descendants
                //     of the paths that are going to be removed. They will
                //     be removed as a result of their ancestors being
                //     removed.
                // 4 - continue until selected contains only null paths.
                while ((shallowest = findShallowestPath(selected)) != null) {
                    removeSiblings(shallowest, selected);
                }
            }
        }

        /**
         * Removes the sibling TreePaths of <code>path</code>, that are
         * located in <code>paths</code>.
         */
        private void removeSiblings(TreePath path, TreePath[] paths) {
            // Find the siblings
            if (path.getPathCount() == 1) {
                // Special case, set the root to null
                for (int counter = paths.length - 1; counter >= 0; counter--) {
                    paths[counter] = null;
                }
                treeModel.setRoot(null);
            } else {
                // Find the siblings of path.
                TreePath parent = path.getParentPath();
                MutableTreeNode parentNode = (MutableTreeNode) parent.
                        getLastPathComponent();
                ArrayList<TreePath> toRemove = new ArrayList<TreePath>();

                // First pass, find paths with a parent TreePath of parent
                for (int counter = paths.length - 1; counter >= 0; counter--) {
                    if (paths[counter] != null && paths[counter].getParentPath().
                            equals(parent)) {
                        toRemove.add(paths[counter]);
                        paths[counter] = null;
                    }
                }

                // Second pass, remove any paths that are descendants of the
                // paths that are going to be removed. These paths are
                // implicitly removed as a result of removing the paths in
                // toRemove
                int rCount = toRemove.size();
                for (int counter = paths.length - 1; counter >= 0; counter--) {
                    if (paths[counter] != null) {
                        for (int rCounter = rCount - 1; rCounter >= 0;
                                rCounter--) {
                            if ((toRemove.get(rCounter)).isDescendant(
                                    paths[counter])) {
                                paths[counter] = null;
                            }
                        }
                    }
                }

                // Sort the siblings based on position in the model
                if (rCount > 1) {
                    Collections.sort(toRemove, new PositionComparator());
                }
                int[] indices = new int[rCount];
                Object[] removedNodes = new Object[rCount];
                for (int counter = rCount - 1; counter >= 0; counter--) {
                    removedNodes[counter] = (toRemove.get(counter)).
                            getLastPathComponent();
                    indices[counter] = treeModel.getIndexOfChild(parentNode,
                            removedNodes[counter]);
                    parentNode.remove(indices[counter]);
                }
                treeModel.nodesWereRemoved(parentNode, indices, removedNodes);
            }
        }

        /**
         * Returns the TreePath with the smallest path count in
         * <code>paths</code>. Will return null if there is no non-null
         * TreePath is <code>paths</code>.
         */
        private TreePath findShallowestPath(TreePath[] paths) {
            int shallowest = -1;
            TreePath shallowestPath = null;

            for (int counter = paths.length - 1; counter >= 0; counter--) {
                if (paths[counter] != null) {
                    if (shallowest != -1) {
                        if (paths[counter].getPathCount() < shallowest) {
                            shallowest = paths[counter].getPathCount();
                            shallowestPath = paths[counter];
                            if (shallowest == 1) {
                                return shallowestPath;
                            }
                        }
                    } else {
                        shallowestPath = paths[counter];
                        shallowest = paths[counter].getPathCount();
                    }
                }
            }
            return shallowestPath;
        }


        /**
         * An Comparator that bases the return value on the index of the
         * passed in objects in the TreeModel.
         * <p>
         * This is actually rather expensive, it would be more efficient
         * to extract the indices and then do the comparision.
         */
        private class PositionComparator implements Comparator<TreePath> {

            public int compare(TreePath p1, TreePath p2) {
                int p1Index = treeModel.getIndexOfChild(p1.getParentPath().
                        getLastPathComponent(), p1.getLastPathComponent());
                int p2Index = treeModel.getIndexOfChild(p2.getParentPath().
                        getLastPathComponent(), p2.getLastPathComponent());
                return p1Index - p2Index;
            }
        }
    } // End of SampleTree.RemoveAction


    /**
     * ShowHandlesChangeListener implements the ChangeListener interface
     * to toggle the state of showing the handles in the tree.
     */
    class ShowHandlesChangeListener extends Object implements ChangeListener {

        public void stateChanged(ChangeEvent e) {
            tree.setShowsRootHandles(((JCheckBox) e.getSource()).isSelected());
        }
    } // End of class SampleTree.ShowHandlesChangeListener


    /**
     * ShowRootChangeListener implements the ChangeListener interface
     * to toggle the state of showing the root node in the tree.
     */
    class ShowRootChangeListener extends Object implements ChangeListener {

        public void stateChanged(ChangeEvent e) {
            tree.setRootVisible(((JCheckBox) e.getSource()).isSelected());
        }
    } // End of class SampleTree.ShowRootChangeListener


    /**
     * TreeEditableChangeListener implements the ChangeListener interface
     * to toggle between allowing editing and now allowing editing in
     * the tree.
     */
    class TreeEditableChangeListener extends Object implements ChangeListener {

        public void stateChanged(ChangeEvent e) {
            tree.setEditable(((JCheckBox) e.getSource()).isSelected());
        }
    } // End of class SampleTree.TreeEditableChangeListener

    public static void main(String[] args) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @SuppressWarnings(value = "ResultOfObjectAllocationIgnored")
                public void run() {
                    new SampleTree();
                }
            });
        } catch (InterruptedException ex) {
            Logger.getLogger(SampleTree.class.getName()).log(Level.SEVERE, null,
                    ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(SampleTree.class.getName()).log(Level.SEVERE, null,
                    ex);
        }
    }
}
