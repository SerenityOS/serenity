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
package com.sun.swingset3.demos.togglebutton;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.sun.swingset3.DemoProperties;
import com.sun.swingset3.demos.ResourceManager;

/**
 * JButton, JRadioButton, JToggleButton, JCheckBox Demos
 *
 * @version 1.15 11/17/05
 * @author Jeff Dinkins
 */
@DemoProperties(
        value = "ToggleButtons Demo",
        category = "Controls",
        description = "Demonstrates JCheckBox & JRadioButton",
        sourceFiles = {
            "com/sun/swingset3/demos/togglebutton/ToggleButtonDemo.java",
            "com/sun/swingset3/demos/togglebutton/DirectionPanel.java",
            "com/sun/swingset3/demos/togglebutton/LayoutControlPanel.java",
            "com/sun/swingset3/demos/ResourceManager.java",
            "com/sun/swingset3/demos/togglebutton/resources/ToggleButtonDemo.properties",
            "com/sun/swingset3/demos/togglebutton/resources/images/b1.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/b1d.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/b1p.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/b1r.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/b2.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/b2d.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/b2p.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/b2r.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/b3.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/b3d.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/b3p.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/b3r.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/bl.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/bldn.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/bm.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/bmdn.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/br.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/brdn.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/c.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/cb.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/cbr.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/cbrs.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/cbs.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/cdn.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/ml.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/mldn.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/mr.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/mrdn.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/rb.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/rbp.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/rbr.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/rbrs.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/rbs.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/tl.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/tldn.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/tm.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/tmdn.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/ToggleButtonDemo.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/tr.gif",
            "com/sun/swingset3/demos/togglebutton/resources/images/trdn.gif"
        }
)
public class ToggleButtonDemo extends JPanel implements ChangeListener {

    private static final Dimension HGAP10 = new Dimension(10, 1);
    private static final Dimension HGAP20 = new Dimension(20, 1);
    private static final Dimension VGAP20 = new Dimension(1, 20);
    private static final Dimension VGAP30 = new Dimension(1, 30);

    static final ResourceManager resourceManager = new ResourceManager(ToggleButtonDemo.class);
    public static final String RADIO3 = resourceManager.getString("ToggleButtonDemo.radio3");
    public static final String RADIO2 = resourceManager.getString("ToggleButtonDemo.radio2");
    public static final String RADIO1 = resourceManager.getString("ToggleButtonDemo.radio1");
    public static final String IMAGE_RADIO_BUTTONS = resourceManager.getString("ToggleButtonDemo.imageradiobuttons");
    public static final String TEXT_RADIO_BUTTONS = resourceManager.getString("ToggleButtonDemo.textradiobuttons");
    public static final String CHECK3 = resourceManager.getString("ToggleButtonDemo.check3");
    public static final String CHECK2 = resourceManager.getString("ToggleButtonDemo.check2");
    public static final String CHECK1 = resourceManager.getString("ToggleButtonDemo.check1");
    public static final String CHECK_BOXES = resourceManager.getString("ToggleButtonDemo.checkboxes");
    public static final String IMAGE_CHECKBOXES = resourceManager.getString("ToggleButtonDemo.imagecheckboxes");
    public static final String TEXT_CHECKBOXES = resourceManager.getString("ToggleButtonDemo.textcheckboxes");
    public static final String CONTENT_FILLED = resourceManager.getString("ToggleButtonDemo.contentfilled");
    public static final String ENABLED = resourceManager.getString("ToggleButtonDemo.enabled");
    public static final String PAINT_FOCUS = resourceManager.getString("ToggleButtonDemo.paintfocus");
    public static final String PAINT_BORDER = resourceManager.getString("ToggleButtonDemo.paintborder");
    public static final String DISPLAY_OPTIONS = resourceManager.getString("ToggleButtonDemo.controlpanel_label");
    public static final String DEFAULT = resourceManager.getString("ToggleButtonDemo.default");
    public static final String PAD_AMOUNT = resourceManager.getString("ToggleButtonDemo.padamount_label");

    private final JTabbedPane tab;

    private final JPanel checkboxPanel = new JPanel();
    private final JPanel radioButtonPanel = new JPanel();

    private final List<JButton> buttons = new ArrayList<>();
    private final List<JCheckBox> checkboxes = new ArrayList<>();
    private final List<JRadioButton> radiobuttons = new ArrayList<>();
    private final List<JToggleButton> togglebuttons = new ArrayList<>();

    private List<? extends JComponent> currentControls = buttons;

    private final EmptyBorder border5 = new EmptyBorder(5, 5, 5, 5);

    private ItemListener buttonDisplayListener = null;
    private ItemListener buttonPadListener = null;

    private final Insets insets0 = new Insets(0, 0, 0, 0);
    private final Insets insets10 = new Insets(10, 10, 10, 10);

    private final Border loweredBorder = new CompoundBorder(
            new SoftBevelBorder(SoftBevelBorder.LOWERED), new EmptyBorder(5, 5, 5, 5));

    /**
     * main method allows us to run as a standalone demo.
     *
     * @param args
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame(ToggleButtonDemo.class.getAnnotation(DemoProperties.class).value());

        frame.getContentPane().add(new ToggleButtonDemo());
        frame.setPreferredSize(new Dimension(800, 600));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * ButtonDemo Constructor
     */
    public ToggleButtonDemo() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        tab = new JTabbedPane();
        tab.getModel().addChangeListener(this);

        add(tab);

        //addButtons();
        addRadioButtons();
        addCheckBoxes();
        //addToggleButtons();
        currentControls = checkboxes;
    }

    private void addRadioButtons() {
        ButtonGroup group = new ButtonGroup();

        tab.addTab(resourceManager.getString("ToggleButtonDemo.radiobuttons"), radioButtonPanel);
        radioButtonPanel.setLayout(new BoxLayout(radioButtonPanel, BoxLayout.X_AXIS));
        radioButtonPanel.setBorder(border5);

        JPanel p1 = createVerticalPanel(true);
        p1.setAlignmentY(TOP_ALIGNMENT);
        radioButtonPanel.add(p1);

        // Text Radio Buttons
        JPanel p2 = createHorizontalPanel(false);
        p1.add(p2);
        p2.setBorder(new CompoundBorder(
                new TitledBorder(
                        null, TEXT_RADIO_BUTTONS,
                        TitledBorder.LEFT, TitledBorder.TOP), border5)
        );

        JRadioButton radio = (JRadioButton) p2.add(new JRadioButton(RADIO1));
        group.add(radio);
        radiobuttons.add(radio);
        p2.add(Box.createRigidArea(HGAP10));

        radio = (JRadioButton) p2.add(new JRadioButton(RADIO2));
        group.add(radio);
        radiobuttons.add(radio);
        p2.add(Box.createRigidArea(HGAP10));

        radio = (JRadioButton) p2.add(new JRadioButton(RADIO3));
        group.add(radio);
        radiobuttons.add(radio);

        // Image Radio Buttons
        group = new ButtonGroup();
        p1.add(Box.createRigidArea(VGAP30));
        JPanel p3 = createHorizontalPanel(false);
        p1.add(p3);
        p3.setLayout(new BoxLayout(p3, BoxLayout.X_AXIS));
        p3.setBorder(new TitledBorder(null, IMAGE_RADIO_BUTTONS,
                TitledBorder.LEFT, TitledBorder.TOP));

        // image radio button 1
        String description = resourceManager.getString("ToggleButtonDemo.customradio");
        String text = RADIO1;
        radio = new JRadioButton(text, resourceManager.createImageIcon("rb.gif", description));
        radio.setPressedIcon(resourceManager.createImageIcon("rbp.gif", description));
        radio.setRolloverIcon(resourceManager.createImageIcon("rbr.gif", description));
        radio.setRolloverSelectedIcon(resourceManager.createImageIcon("rbrs.gif", description));
        radio.setSelectedIcon(resourceManager.createImageIcon("rbs.gif", description));
        radio.setMargin(new Insets(0, 0, 0, 0));
        group.add(radio);
        p3.add(radio);
        radiobuttons.add(radio);
        p3.add(Box.createRigidArea(HGAP20));

        // image radio button 2
        text = RADIO2;
        radio = new JRadioButton(text, resourceManager.createImageIcon("rb.gif", description));
        radio.setPressedIcon(resourceManager.createImageIcon("rbp.gif", description));
        radio.setRolloverIcon(resourceManager.createImageIcon("rbr.gif", description));
        radio.setRolloverSelectedIcon(resourceManager.createImageIcon("rbrs.gif", description));
        radio.setSelectedIcon(resourceManager.createImageIcon("rbs.gif", description));
        radio.setMargin(new Insets(0, 0, 0, 0));
        group.add(radio);
        p3.add(radio);
        radiobuttons.add(radio);
        p3.add(Box.createRigidArea(HGAP20));

        // image radio button 3
        text = RADIO3;
        radio = new JRadioButton(text, resourceManager.createImageIcon("rb.gif", description));
        radio.setPressedIcon(resourceManager.createImageIcon("rbp.gif", description));
        radio.setRolloverIcon(resourceManager.createImageIcon("rbr.gif", description));
        radio.setRolloverSelectedIcon(resourceManager.createImageIcon("rbrs.gif", description));
        radio.setSelectedIcon(resourceManager.createImageIcon("rbs.gif", description));
        radio.setMargin(new Insets(0, 0, 0, 0));
        group.add(radio);
        radiobuttons.add(radio);
        p3.add(radio);

        // verticaly glue fills out the rest of the box
        p1.add(Box.createVerticalGlue());

        radioButtonPanel.add(Box.createHorizontalGlue());
        currentControls = radiobuttons;
        radioButtonPanel.add(createControls());
    }

    private void addCheckBoxes() {
        tab.addTab(CHECK_BOXES, checkboxPanel);
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.X_AXIS));
        checkboxPanel.setBorder(border5);

        JPanel p1 = createVerticalPanel(true);
        p1.setAlignmentY(TOP_ALIGNMENT);
        checkboxPanel.add(p1);

        // Text Radio Buttons
        JPanel p2 = createHorizontalPanel(false);
        p1.add(p2);
        p2.setBorder(new CompoundBorder(
                new TitledBorder(
                        null, TEXT_CHECKBOXES,
                        TitledBorder.LEFT, TitledBorder.TOP), border5)
        );

        JCheckBox checkBox1 = new JCheckBox(CHECK1);
        checkboxes.add(checkBox1);
        p2.add(checkBox1);
        p2.add(Box.createRigidArea(HGAP10));

        JCheckBox checkBox2 = new JCheckBox(CHECK2);
        checkboxes.add(checkBox2);
        p2.add(checkBox2);
        p2.add(Box.createRigidArea(HGAP10));

        JCheckBox checkBox3 = new JCheckBox(CHECK3);
        checkboxes.add(checkBox3);
        p2.add(checkBox3);

        // Image Radio Buttons
        p1.add(Box.createRigidArea(VGAP30));
        JPanel p3 = createHorizontalPanel(false);
        p1.add(p3);
        p3.setLayout(new BoxLayout(p3, BoxLayout.X_AXIS));
        p3.setBorder(new TitledBorder(null, IMAGE_CHECKBOXES,
                TitledBorder.LEFT, TitledBorder.TOP));

        // image checkbox 1
        String description = resourceManager.getString("ToggleButtonDemo.customcheck");
        String text = CHECK1;
        JCheckBox check = new JCheckBox(text,
                resourceManager.createImageIcon("cb.gif", description));
        check.setRolloverIcon(resourceManager.createImageIcon("cbr.gif", description));
        check.setRolloverSelectedIcon(resourceManager.createImageIcon("cbrs.gif", description));
        check.setSelectedIcon(resourceManager.createImageIcon("cbs.gif", description));
        check.setMargin(new Insets(0, 0, 0, 0));
        p3.add(check);
        checkboxes.add(check);
        p3.add(Box.createRigidArea(HGAP20));

        // image checkbox 2
        text = CHECK2;
        check = new JCheckBox(text, resourceManager.createImageIcon("cb.gif", description));
        check.setRolloverIcon(resourceManager.createImageIcon("cbr.gif", description));
        check.setRolloverSelectedIcon(resourceManager.createImageIcon("cbrs.gif", description));
        check.setSelectedIcon(resourceManager.createImageIcon("cbs.gif", description));
        check.setMargin(new Insets(0, 0, 0, 0));
        p3.add(check);
        checkboxes.add(check);
        p3.add(Box.createRigidArea(HGAP20));

        // image checkbox 3
        text = CHECK3;
        check = new JCheckBox(text, resourceManager.createImageIcon("cb.gif", description));
        check.setRolloverIcon(resourceManager.createImageIcon("cbr.gif", description));
        check.setRolloverSelectedIcon(resourceManager.createImageIcon("cbrs.gif", description));
        check.setSelectedIcon(resourceManager.createImageIcon("cbs.gif", description));
        check.setMargin(new Insets(0, 0, 0, 0));
        p3.add(check);
        checkboxes.add(check);

        // verticaly glue fills out the rest of the box
        p1.add(Box.createVerticalGlue());

        checkboxPanel.add(Box.createHorizontalGlue());
        currentControls = checkboxes;
        checkboxPanel.add(createControls());
    }

    private JPanel createControls() {
        JPanel controls = new JPanel() {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(300, super.getMaximumSize().height);
            }
        };
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setAlignmentY(TOP_ALIGNMENT);
        controls.setAlignmentX(LEFT_ALIGNMENT);

        JPanel buttonControls = createHorizontalPanel(true);
        buttonControls.setAlignmentY(TOP_ALIGNMENT);
        buttonControls.setAlignmentX(LEFT_ALIGNMENT);

        JPanel leftColumn = createVerticalPanel(false);
        leftColumn.setAlignmentX(LEFT_ALIGNMENT);
        leftColumn.setAlignmentY(TOP_ALIGNMENT);

        JPanel rightColumn = new LayoutControlPanel(this);

        buttonControls.add(leftColumn);
        buttonControls.add(Box.createRigidArea(HGAP20));
        buttonControls.add(rightColumn);
        buttonControls.add(Box.createRigidArea(HGAP20));

        controls.add(buttonControls);

        createListeners();

        // Display Options
        JLabel l = new JLabel(DISPLAY_OPTIONS);
        leftColumn.add(l);

        JCheckBox bordered = new JCheckBox(PAINT_BORDER);
        bordered.setActionCommand("PaintBorder");
        bordered.setToolTipText(resourceManager.getString("ToggleButtonDemo.paintborder_tooltip"));
        bordered.setMnemonic(resourceManager.getMnemonic("ToggleButtonDemo.paintborder_mnemonic"));
        if (currentControls == buttons) {
            bordered.setSelected(true);
        }
        bordered.addItemListener(buttonDisplayListener);
        leftColumn.add(bordered);

        JCheckBox focused = new JCheckBox(PAINT_FOCUS);
        focused.setActionCommand("PaintFocus");
        focused.setToolTipText(resourceManager.getString("ToggleButtonDemo.paintfocus_tooltip"));
        focused.setMnemonic(resourceManager.getMnemonic("ToggleButtonDemo.paintfocus_mnemonic"));
        focused.setSelected(true);
        focused.addItemListener(buttonDisplayListener);
        leftColumn.add(focused);

        JCheckBox enabled = new JCheckBox(ENABLED);
        enabled.setActionCommand("Enabled");
        enabled.setToolTipText(resourceManager.getString("ToggleButtonDemo.enabled_tooltip"));
        enabled.setSelected(true);
        enabled.addItemListener(buttonDisplayListener);
        enabled.setMnemonic(resourceManager.getMnemonic("ToggleButtonDemo.enabled_mnemonic"));
        leftColumn.add(enabled);

        JCheckBox filled = new JCheckBox(CONTENT_FILLED);
        filled.setActionCommand("ContentFilled");
        filled.setToolTipText(resourceManager.getString("ToggleButtonDemo.contentfilled_tooltip"));
        filled.setSelected(true);
        filled.addItemListener(buttonDisplayListener);
        filled.setMnemonic(resourceManager.getMnemonic("ToggleButtonDemo.contentfilled_mnemonic"));
        leftColumn.add(filled);

        leftColumn.add(Box.createRigidArea(VGAP20));

        l = new JLabel(PAD_AMOUNT);
        leftColumn.add(l);
        ButtonGroup group = new ButtonGroup();
        JRadioButton defaultPad = new JRadioButton(DEFAULT);
        defaultPad.setToolTipText(resourceManager.getString("ToggleButtonDemo.default_tooltip"));
        defaultPad.setMnemonic(resourceManager.getMnemonic("ToggleButtonDemo.default_mnemonic"));
        defaultPad.addItemListener(buttonPadListener);
        group.add(defaultPad);
        defaultPad.setSelected(true);
        leftColumn.add(defaultPad);

        JRadioButton zeroPad = new JRadioButton(resourceManager.getString("ToggleButtonDemo.zero"));
        zeroPad.setActionCommand("ZeroPad");
        zeroPad.setToolTipText(resourceManager.getString("ToggleButtonDemo.zero_tooltip"));
        zeroPad.addItemListener(buttonPadListener);
        zeroPad.setMnemonic(resourceManager.getMnemonic("ToggleButtonDemo.zero_mnemonic"));
        group.add(zeroPad);
        leftColumn.add(zeroPad);

        JRadioButton tenPad = new JRadioButton(resourceManager.getString("ToggleButtonDemo.ten"));
        tenPad.setActionCommand("TenPad");
        tenPad.setMnemonic(resourceManager.getMnemonic("ToggleButtonDemo.ten_mnemonic"));
        tenPad.setToolTipText(resourceManager.getString("ToggleButtonDemo.ten_tooltip"));
        tenPad.addItemListener(buttonPadListener);
        group.add(tenPad);
        leftColumn.add(tenPad);

        leftColumn.add(Box.createRigidArea(VGAP20));
        return controls;
    }

    private void createListeners() {
        buttonDisplayListener = (ItemEvent e) -> {
            JCheckBox cb = (JCheckBox) e.getSource();
            String command = cb.getActionCommand();
            switch (command) {
                case "Enabled":
                    for (JComponent control : currentControls) {
                        control.setEnabled(cb.isSelected());
                        control.invalidate();
                    }
                    break;
                case "PaintBorder":
                    if (currentControls.get(0) instanceof AbstractButton) {
                        for (JComponent control : currentControls) {
                            AbstractButton b = (AbstractButton) control;
                            b.setBorderPainted(cb.isSelected());
                            b.invalidate();
                        }
                    }
                    break;
                case "PaintFocus":
                    if (currentControls.get(0) instanceof AbstractButton) {
                        for (JComponent control : currentControls) {
                            AbstractButton b = (AbstractButton) control;
                            b.setFocusPainted(cb.isSelected());
                            b.invalidate();
                        }
                    }
                    break;
                case "ContentFilled":
                    if (currentControls.get(0) instanceof AbstractButton) {
                        for (JComponent control : currentControls) {
                            AbstractButton b = (AbstractButton) control;
                            b.setContentAreaFilled(cb.isSelected());
                            b.invalidate();
                        }
                    }
                    break;
            }
            invalidate();
            validate();
            repaint();
        };

        buttonPadListener = (ItemEvent e) -> {
            // *** pad = 0
            int pad = -1;
            JRadioButton rb = (JRadioButton) e.getSource();
            String command = rb.getActionCommand();
            if ("ZeroPad".equals(command) && rb.isSelected()) {
                pad = 0;
            } else if ("TenPad".equals(command) && rb.isSelected()) {
                pad = 10;
            }

            for (JComponent control : currentControls) {
                AbstractButton b = (AbstractButton) control;
                if (pad == -1) {
                    b.setMargin(null);
                } else if (pad == 0) {
                    b.setMargin(insets0);
                } else {
                    b.setMargin(insets10);
                }
            }
            invalidate();
            validate();
            repaint();
        };
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        SingleSelectionModel model = (SingleSelectionModel) e.getSource();
        if (model.getSelectedIndex() == 0) {
            currentControls = buttons;
        } else if (model.getSelectedIndex() == 1) {
            currentControls = radiobuttons;
        } else if (model.getSelectedIndex() == 2) {
            currentControls = checkboxes;
        } else {
            currentControls = togglebuttons;
        }
    }

    public List<? extends JComponent> getCurrentControls() {
        return currentControls;
    }

    private JPanel createHorizontalPanel(boolean threeD) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setAlignmentY(TOP_ALIGNMENT);
        p.setAlignmentX(LEFT_ALIGNMENT);
        if (threeD) {
            p.setBorder(loweredBorder);
        }
        return p;
    }

    private JPanel createVerticalPanel(boolean threeD) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentY(TOP_ALIGNMENT);
        p.setAlignmentX(LEFT_ALIGNMENT);
        if (threeD) {
            p.setBorder(loweredBorder);
        }
        return p;
    }
}
