/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.swingset3.demos.list;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Vector;
import javax.swing.*;

import com.sun.swingset3.DemoProperties;
import com.sun.swingset3.demos.ResourceManager;

/**
 * List Demo. This demo shows that it is not always necessary to have an array
 * of objects as big as the size of the list stored.
 *
 * Indeed, in this example, there is no array kept for the list data, rather it
 * is generated on the fly as only those elements are needed.
 *
 * @version 1.17 11/17/05
 * @author Jeff Dinkins
 */
@DemoProperties(
        value = "JList Demo",
        category = "Data",
        description = "Demonstrates JList, a component which supports display/editing of a data list",
        sourceFiles = {
            "com/sun/swingset3/demos/list/ListDemo.java",
            "com/sun/swingset3/demos/list/Permuter.java",
            "com/sun/swingset3/demos/ResourceManager.java",
            "com/sun/swingset3/demos/list/resources/ListDemo.properties",
            "com/sun/swingset3/demos/list/resources/images/blue.gif",
            "com/sun/swingset3/demos/list/resources/images/cyan.gif",
            "com/sun/swingset3/demos/list/resources/images/gray.gif",
            "com/sun/swingset3/demos/list/resources/images/green.gif",
            "com/sun/swingset3/demos/list/resources/images/ListDemo.gif",
            "com/sun/swingset3/demos/list/resources/images/magenta.gif",
            "com/sun/swingset3/demos/list/resources/images/red.gif",
            "com/sun/swingset3/demos/list/resources/images/yellow.gif"
        }
)
public final class ListDemo extends JPanel {

    private static final Dimension HGAP10 = new Dimension(10, 1);
    private static final Dimension VGAP10 = new Dimension(1, 10);
    private static final Dimension HGAP15 = new Dimension(15, 1);
    private static final Dimension HGAP30 = new Dimension(30, 1);

    private final ResourceManager resourceManager = new ResourceManager(this.getClass());
    public static final String DEMO_TITLE = ListDemo.class.getAnnotation(DemoProperties.class).value();

    private final JList<String> list;

    private JPanel prefixList;
    private JPanel suffixList;

    private Action prefixAction;
    private Action suffixAction;

    private final GeneratedListModel listModel;

    /**
     * main method allows us to run as a standalone demo.
     *
     * @param args
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame(DEMO_TITLE);

        frame.getContentPane().add(new ListDemo());
        frame.setPreferredSize(new Dimension(800, 600));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * ListDemo Constructor
     */
    public ListDemo() {
        setLayout(new BorderLayout());

        loadImages();

        JLabel description = new JLabel(resourceManager.getString("ListDemo.description"));
        add(description, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.X_AXIS));
        centerPanel.add(Box.createRigidArea(HGAP10));
        add(centerPanel, BorderLayout.CENTER);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.add(Box.createRigidArea(VGAP10));

        centerPanel.add(listPanel);
        centerPanel.add(Box.createRigidArea(HGAP30));

        // Create the list
        list = new JList<>();
        list.setCellRenderer(new CompanyLogoListCellRenderer());
        listModel = new GeneratedListModel();
        list.setModel(listModel);

        // Set the preferred row count. This affects the preferredSize
        // of the JList when it's in a scrollpane.
        list.setVisibleRowCount(22);

        // Add list to a scrollpane
        JScrollPane scrollPane = new JScrollPane(list);
        listPanel.add(scrollPane);
        listPanel.add(Box.createRigidArea(VGAP10));

        // Add the control panel (holds the prefix/suffix list and prefix/suffix checkboxes)
        centerPanel.add(createControlPanel());

        // create prefixes and suffixes
        addPrefix("Tera", true);
        addPrefix("Micro", false);
        addPrefix("Southern", false);
        addPrefix("Net", true);
        addPrefix("YoYo", true);
        addPrefix("Northern", false);
        addPrefix("Tele", false);
        addPrefix("Eastern", false);
        addPrefix("Neo", false);
        addPrefix("Digi", false);
        addPrefix("National", false);
        addPrefix("Compu", true);
        addPrefix("Meta", true);
        addPrefix("Info", false);
        addPrefix("Western", false);
        addPrefix("Data", false);
        addPrefix("Atlantic", false);
        addPrefix("Advanced", false);
        addPrefix("Euro", false);
        addPrefix("Pacific", false);
        addPrefix("Mobile", false);
        addPrefix("In", false);
        addPrefix("Computa", false);
        addPrefix("Digital", false);
        addPrefix("Analog", false);

        addSuffix("Tech", true);
        addSuffix("Soft", true);
        addSuffix("Telecom", true);
        addSuffix("Solutions", false);
        addSuffix("Works", true);
        addSuffix("Dyne", false);
        addSuffix("Services", false);
        addSuffix("Vers", false);
        addSuffix("Devices", false);
        addSuffix("Software", false);
        addSuffix("Serv", false);
        addSuffix("Systems", true);
        addSuffix("Dynamics", true);
        addSuffix("Net", false);
        addSuffix("Sys", false);
        addSuffix("Computing", false);
        addSuffix("Scape", false);
        addSuffix("Com", false);
        addSuffix("Ware", false);
        addSuffix("Widgets", false);
        addSuffix("Media", false);
        addSuffix("Computer", false);
        addSuffix("Hardware", false);
        addSuffix("Gizmos", false);
        addSuffix("Concepts", false);
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel() {
            private final Insets insets = new Insets(0, 4, 10, 10);

            @Override
            public Insets getInsets() {
                return insets;
            }
        };
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));

        JPanel prefixPanel = new JPanel();
        prefixPanel.setLayout(new BoxLayout(prefixPanel, BoxLayout.Y_AXIS));
        prefixPanel.add(new JLabel(resourceManager.getString("ListDemo.prefixes")));

        JPanel suffixPanel = new JPanel();
        suffixPanel.setLayout(new BoxLayout(suffixPanel, BoxLayout.Y_AXIS));
        suffixPanel.add(new JLabel(resourceManager.getString("ListDemo.suffixes")));

        prefixList = new JPanel() {
            private final Insets insets = new Insets(0, 4, 0, 0);

            @Override
            public Insets getInsets() {
                return insets;
            }
        };
        prefixList.setLayout(new BoxLayout(prefixList, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(prefixList);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
        prefixPanel.add(scrollPane);
        prefixPanel.add(Box.createRigidArea(HGAP10));

        suffixList = new JPanel() {
            private final Insets insets = new Insets(0, 4, 0, 0);

            @Override
            public Insets getInsets() {
                return insets;
            }
        };
        suffixList.setLayout(new BoxLayout(suffixList, BoxLayout.Y_AXIS));
        scrollPane = new JScrollPane(suffixList);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
        suffixPanel.add(scrollPane);
        suffixPanel.add(Box.createRigidArea(HGAP10));

        controlPanel.add(prefixPanel);
        controlPanel.add(Box.createRigidArea(HGAP15));
        controlPanel.add(suffixPanel);
        return controlPanel;
    }

    private final FocusListener listFocusListener = new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
            JComponent c = (JComponent) e.getComponent();
            c.scrollRectToVisible(new Rectangle(0, 0, c.getWidth(), c.getHeight()));
        }
    };

    private void addPrefix(String prefix, boolean selected) {
        if (prefixAction == null) {
            prefixAction = new UpdatePrefixListAction(listModel);
        }
        final JCheckBox cb = (JCheckBox) prefixList.add(new JCheckBox(prefix));
        cb.setSelected(selected);
        cb.addActionListener(prefixAction);
        if (selected) {
            listModel.addPrefix(prefix);
        }
        cb.addFocusListener(listFocusListener);
    }

    private void addSuffix(String suffix, boolean selected) {
        if (suffixAction == null) {
            suffixAction = new UpdateSuffixListAction(listModel);
        }
        final JCheckBox cb = (JCheckBox) suffixList.add(new JCheckBox(suffix));
        cb.setSelected(selected);
        cb.addActionListener(suffixAction);
        if (selected) {
            listModel.addSuffix(suffix);
        }
        cb.addFocusListener(listFocusListener);
    }

    private static class UpdatePrefixListAction extends AbstractAction {

        private final GeneratedListModel listModel;

        protected UpdatePrefixListAction(GeneratedListModel listModel) {
            this.listModel = listModel;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JCheckBox cb = (JCheckBox) e.getSource();
            if (cb.isSelected()) {
                listModel.addPrefix(cb.getText());
            } else {
                listModel.removePrefix(cb.getText());
            }
        }
    }

    private static class UpdateSuffixListAction extends AbstractAction {

        private final GeneratedListModel listModel;

        protected UpdateSuffixListAction(GeneratedListModel listModel) {
            this.listModel = listModel;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JCheckBox cb = (JCheckBox) e.getSource();
            if (cb.isSelected()) {
                listModel.addSuffix(cb.getText());
            } else {
                listModel.removeSuffix(cb.getText());
            }
        }
    }

    private static class GeneratedListModel extends AbstractListModel<String> {

        private Permuter permuter;

        private final Vector<String> prefix = new Vector<>();
        private final Vector<String> suffix = new Vector<>();

        private void update() {
            permuter = new Permuter(getSize());
            fireContentsChanged(this, 0, getSize());
        }

        public void addPrefix(String s) {
            if (!prefix.contains(s)) {
                prefix.addElement(s);
                update();
            }
        }

        public void removePrefix(String s) {
            prefix.removeElement(s);
            update();
        }

        public void addSuffix(String s) {
            if (!suffix.contains(s)) {
                suffix.addElement(s);
                update();
            }
        }

        public void removeSuffix(String s) {
            suffix.removeElement(s);
            update();
        }

        @Override
        public int getSize() {
            return prefix.size() * suffix.size();
        }

        @Override
        public String getElementAt(int index) {
            if (permuter == null) {
                update();
            }
            // morph the index to another int -- this has the benefit of
            // causing the list to look random.
            int j = permuter.map(index);
            int ps = prefix.size();
            int ss = suffix.size();
            return prefix.elementAt(j % ps) + suffix.elementAt((j / ps) % ss);
        }
    }

    private final ImageIcon[] images = new ImageIcon[7];

    void loadImages() {
        images[0] = resourceManager.createImageIcon("red.gif", resourceManager.getString("ListDemo.red"));
        images[1] = resourceManager.createImageIcon("blue.gif", resourceManager.getString("ListDemo.blue"));
        images[2] = resourceManager.createImageIcon("yellow.gif", resourceManager.getString("ListDemo.yellow"));
        images[3] = resourceManager.createImageIcon("green.gif", resourceManager.getString("ListDemo.green"));
        images[4] = resourceManager.createImageIcon("gray.gif", resourceManager.getString("ListDemo.gray"));
        images[5] = resourceManager.createImageIcon("cyan.gif", resourceManager.getString("ListDemo.cyan"));
        images[6] = resourceManager.createImageIcon("magenta.gif", resourceManager.getString("ListDemo.magenta"));
    }

    private class CompanyLogoListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            Component retValue = super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus
            );
            setIcon(images[index % 7]);
            return retValue;
        }
    }
}
