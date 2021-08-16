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
package com.sun.swingset3.demos.splitpane;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.sun.swingset3.DemoProperties;
import com.sun.swingset3.demos.ResourceManager;

/**
 * Split Pane demo
 *
 * @version 1.12 11/17/05
 * @author Scott Violet
 * @author Jeff Dinkins
 */
@DemoProperties(
        value = "JSplitPane Demo",
        category = "Containers",
        description = "Demonstrates JSplitPane, a container which lays out two components in an adjustable split view (horizontal or vertical)",
        sourceFiles = {
            "com/sun/swingset3/demos/splitpane/SplitPaneDemo.java",
            "com/sun/swingset3/demos/ResourceManager.java",
            "com/sun/swingset3/demos/splitpane/resources/SplitPaneDemo.properties",
            "com/sun/swingset3/demos/splitpane/resources/images/day.jpg",
            "com/sun/swingset3/demos/splitpane/resources/images/night.jpg",
            "com/sun/swingset3/demos/splitpane/resources/images/SplitPaneDemo.gif"
        }
)
public class SplitPaneDemo extends JPanel {

    private static final ResourceManager resourceManager = new ResourceManager(SplitPaneDemo.class);
    public static final String VERTICAL_SPLIT = resourceManager.getString("SplitPaneDemo.vert_split");
    public static final String HORIZONTAL_SPLIT = resourceManager.getString("SplitPaneDemo.horz_split");
    public static final String ONE_TOUCH_EXPANDABLE = resourceManager.getString("SplitPaneDemo.one_touch_expandable");
    public static final String SECOND_COMPONENT_MIN_SIZE = resourceManager.getString("SplitPaneDemo.second_component_min_size");
    public static final String FIRST_COMPONENT_MIN_SIZE = resourceManager.getString("SplitPaneDemo.first_component_min_size");
    public static final String DIVIDER_SIZE = resourceManager.getString("SplitPaneDemo.divider_size");
    public static final String DEMO_TITLE = SplitPaneDemo.class.getAnnotation(DemoProperties.class).value();

    private static final Insets insets = new Insets(4, 8, 4, 8);

    private final JSplitPane splitPane;
    private final JLabel day;
    private final JLabel night;

    private JPanel controlPanel;
    private GridBagLayout gridbag;
    private GridBagConstraints c;

    /**
     * main method allows us to run as a standalone demo.
     *
     * @param args
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame(DEMO_TITLE);

        frame.getContentPane().add(new SplitPaneDemo());
        frame.setPreferredSize(new Dimension(800, 600));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * SplitPaneDemo Constructor
     */
    public SplitPaneDemo() {
        setLayout(new BorderLayout());

        //<snip>Create horizontal SplitPane with day and night
        day = new JLabel(resourceManager.createImageIcon("day.jpg",
                resourceManager.getString("SplitPaneDemo.day")));
        night = new JLabel(resourceManager.createImageIcon("night.jpg",
                resourceManager.getString("SplitPaneDemo.night")));

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, day, night);
        //</snip>

        //<snip>Turn on continuous layout
        splitPane.setContinuousLayout(true);
        //</snip>

        //<snip>Turn on one-touch expansion
        splitPane.setOneTouchExpandable(true);
        //</snip>

        //<snip>Set divider location
        splitPane.setDividerLocation(200);
        //</snip>

        //<snip>Set minimum size for each child
        day.setMinimumSize(new Dimension(20, 20));
        night.setMinimumSize(new Dimension(20, 20));
        //</snip>

        add(splitPane, BorderLayout.CENTER);
        setBackground(Color.black);

        add(createSplitPaneControls(), BorderLayout.SOUTH);
    }

    /**
     * Creates controls to alter the JSplitPane.
     *
     * @return
     */
    protected final JPanel createSplitPaneControls() {

        gridbag = new GridBagLayout();
        c = new GridBagConstraints();
        controlPanel = new JPanel(gridbag);

        //<snip>Create radio box to edit splitpane orientation
        Box box = Box.createHorizontalBox();
        ButtonGroup group = new ButtonGroup();

        OrientationListener orientationListener = new OrientationListener();

        JRadioButton button = new JRadioButton(VERTICAL_SPLIT);
        button.setActionCommand("vertical");
        button.addActionListener(orientationListener);
        group.add(button);
        box.add(button);

        button = new JRadioButton(HORIZONTAL_SPLIT);
        button.setActionCommand("horizontal");
        button.setSelected(true);
        button.addActionListener(orientationListener);
        group.add(button);
        box.add(button);
        //</snip>

        addToGridbag(box, 0, 0, 1, 1,
                GridBagConstraints.NONE, GridBagConstraints.WEST);

        //<snip>Create checkbox to edit continuous layout
        JCheckBox checkBox = new JCheckBox(resourceManager.getString("SplitPaneDemo.cont_layout"));
        checkBox.setSelected(true);

        checkBox.addChangeListener((ChangeEvent e) -> {
            splitPane.setContinuousLayout(
                    ((JCheckBox) e.getSource()).isSelected());
        });
        //</snip>

        c.gridy++;
        addToGridbag(checkBox, 0, 1, 1, 1,
                GridBagConstraints.NONE, GridBagConstraints.WEST);

        //<snip>Create checkbox to edit one-touch-expandable
        checkBox = new JCheckBox(ONE_TOUCH_EXPANDABLE);
        checkBox.setSelected(true);

        checkBox.addChangeListener((ChangeEvent e) -> {
            splitPane.setOneTouchExpandable(
                    ((JCheckBox) e.getSource()).isSelected());
        });
        //</snip>

        addToGridbag(checkBox, 0, 2, 1, 1,
                GridBagConstraints.NONE, GridBagConstraints.WEST);

        JSeparator separator = new JSeparator(JSeparator.VERTICAL);
        addToGridbag(separator, 1, 0, 1, 3,
                GridBagConstraints.VERTICAL, GridBagConstraints.CENTER);

        //<snip>Create spinner to edit divider size
        final JSpinner spinner = new JSpinner(
                new SpinnerNumberModel(splitPane.getDividerSize(), 5, 50, 2));

        spinner.addChangeListener((ChangeEvent event) -> {
            SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
            splitPane.setDividerSize(model.getNumber().intValue());
        });
        //</snip>

        JLabel label = new JLabel(DIVIDER_SIZE);
        label.setLabelFor(spinner);
        addToGridbag(label, 2, 0, 1, 1,
                GridBagConstraints.NONE, GridBagConstraints.EAST);
        addToGridbag(spinner, 3, 0, 1, 1,
                GridBagConstraints.NONE, GridBagConstraints.WEST);

        //<snip>Create spinners to edit day & night's minimum sizes
        JSpinner minSizeSpinner = new JSpinner(
                new SpinnerNumberModel(day.getMinimumSize().width, 0, 300, 10));

        minSizeSpinner.addChangeListener(new MinimumSizeListener(day));
        //</snip>

        label = new JLabel(FIRST_COMPONENT_MIN_SIZE);
        label.setLabelFor(minSizeSpinner);
        addToGridbag(label, 2, 1, 1, 1,
                GridBagConstraints.NONE, GridBagConstraints.EAST);
        addToGridbag(minSizeSpinner, 3, 1, 1, 1,
                GridBagConstraints.NONE, GridBagConstraints.WEST);

        //<snip>Create spinners to edit day & night's minimum sizes
        minSizeSpinner = new JSpinner(
                new SpinnerNumberModel(night.getMinimumSize().width, 0, 300, 10));

        minSizeSpinner.addChangeListener(new MinimumSizeListener(night));
        //</snip>

        label = new JLabel(SECOND_COMPONENT_MIN_SIZE);
        label.setLabelFor(minSizeSpinner);
        addToGridbag(label, 2, 2, 1, 1,
                GridBagConstraints.NONE, GridBagConstraints.EAST);
        addToGridbag(minSizeSpinner, 3, 2, 1, 1,
                GridBagConstraints.NONE, GridBagConstraints.WEST);

        return controlPanel;
    }

    protected void addToGridbag(JComponent child, int gx, int gy,
            int gwidth, int gheight, int fill, int anchor) {
        c.insets = insets;
        c.gridx = gx;
        c.gridy = gy;
        c.gridwidth = gwidth;
        c.gridheight = gheight;
        c.fill = fill;
        c.anchor = anchor;
        gridbag.addLayoutComponent(child, c);
        controlPanel.add(child);

    }

    //<snip>Create radio box to edit splitpane orientation
    private class OrientationListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            splitPane.setOrientation(event.getActionCommand().equals("vertical")
                    ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT);
        }

    }
    //</snip>

    //<snip>Create spinners to edit day & night's minimum sizes
    public class MinimumSizeListener implements ChangeListener {

        private final JComponent component;

        public MinimumSizeListener(JComponent c) {
            this.component = c;
        }

        @Override
        public void stateChanged(ChangeEvent event) {
            JSpinner spinner = (JSpinner) event.getSource();
            SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
            int min = model.getNumber().intValue();
            component.setMinimumSize(new Dimension(min, min));
        }
    }
    //</snip>
}
