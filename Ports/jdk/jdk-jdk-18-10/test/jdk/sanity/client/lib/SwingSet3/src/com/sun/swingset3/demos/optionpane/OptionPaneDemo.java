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
package com.sun.swingset3.demos.optionpane;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URL;
import javax.swing.*;

import com.sun.swingset3.demos.ResourceManager;
import com.sun.swingset3.DemoProperties;

/**
 * JOptionPaneDemo
 *
 * @author Jeff Dinkins
 * @version 1.11 11/17/05
 */
@DemoProperties(
        value = "JOptionPane Demo",
        category = "Choosers",
        description = "Demonstrates JOptionPane, a component which displays standard message dialogs (question, warning, error, etc).",
        sourceFiles = {
            "com/sun/swingset3/demos/optionpane/OptionPaneDemo.java",
            "com/sun/swingset3/demos/ResourceManager.java",
            "com/sun/swingset3/demos/optionpane/resources/OptionPaneDemo.properties",
            "com/sun/swingset3/demos/optionpane/resources/images/bottle.gif",
            "com/sun/swingset3/demos/optionpane/resources/images/OptionPaneDemo.gif"
        }
)
public class OptionPaneDemo extends JPanel {

    private static final Dimension VGAP15 = new Dimension(1, 15);
    private static final Dimension VGAP30 = new Dimension(1, 30);

    private static final ResourceManager resourceManager = new ResourceManager(OptionPaneDemo.class);
    public static final String WARNING_TITLE = resourceManager.getString("OptionPaneDemo.warningtitle");
    public static final String WARNING_TEXT = resourceManager.getString("OptionPaneDemo.warningtext");
    public static final String WARNING_BUTTON = resourceManager.getString("OptionPaneDemo.warningbutton");
    public static final String CONFIRM_NO = resourceManager.getString("OptionPaneDemo.confirmno");
    public static final String CONFIRM_YES = resourceManager.getString("OptionPaneDemo.confirmyes");
    public static final String CONFIRM_QUESTION = resourceManager.getString("OptionPaneDemo.confirmquestion");
    public static final String CONFIRM_BUTTON = resourceManager.getString("OptionPaneDemo.confirmbutton");
    public static final String MESSAGE_TEXT = resourceManager.getString("OptionPaneDemo.messagetext");
    public static final String MESSAGE_BUTTON = resourceManager.getString("OptionPaneDemo.messagebutton");
    public static final String INPUT_QUESTION = resourceManager.getString("OptionPaneDemo.inputquestion");
    public static final String INPUT_RESPONSE = ": " + resourceManager.getString("OptionPaneDemo.inputresponse");
    public static final String INPUT_BUTTON = resourceManager.getString("OptionPaneDemo.inputbutton");
    public static final String COMPONENT_R4 = resourceManager.getString("OptionPaneDemo.component_r4");
    public static final String COMPONENT_R3 = resourceManager.getString("OptionPaneDemo.component_r3");
    public static final String COMPONENT_R2 = resourceManager.getString("OptionPaneDemo.component_r2");
    public static final String COMPONENT_R1 = resourceManager.getString("OptionPaneDemo.component_r1");
    public static final String COMPONENT_TITLE = resourceManager.getString("OptionPaneDemo.componenttitle");
    public static final String COMPONENT_OP5 = resourceManager.getString("OptionPaneDemo.component_op5");
    public static final String COMPONENT_OP4 = resourceManager.getString("OptionPaneDemo.component_op4");
    public static final String COMPONENT_OP3 = resourceManager.getString("OptionPaneDemo.component_op3");
    public static final String COMPONENT_OP2 = resourceManager.getString("OptionPaneDemo.component_op2");
    public static final String COMPONENT_OP1 = resourceManager.getString("OptionPaneDemo.component_op1");
    public static final String COMPONENT_MESSAGE_2 = resourceManager.getString("OptionPaneDemo.componentmessage2");
    public static final String COMPONENT_CB3 = resourceManager.getString("OptionPaneDemo.component_cb3");
    public static final String COMPONENT_CB2 = resourceManager.getString("OptionPaneDemo.component_cb2");
    public static final String COMPONENT_CB1 = resourceManager.getString("OptionPaneDemo.component_cb1");
    public static final String COMPONENT_BUTTON = resourceManager.getString("OptionPaneDemo.componentbutton");
    public static final String COMPONENT_TEXT_FIELD = resourceManager.getString("OptionPaneDemo.componenttextfield");
    public static final String COMPONENT_MESSAGE = resourceManager.getString("OptionPaneDemo.componentmessage");
    public static final String DEMO_TITLE = OptionPaneDemo.class.getAnnotation(DemoProperties.class).value();

    /**
     * main method allows us to run as a standalone demo.
     *
     * @param args
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame(DEMO_TITLE);

        frame.getContentPane().add(new OptionPaneDemo());
        frame.setPreferredSize(new Dimension(800, 600));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * OptionPaneDemo Constructor
     */
    public OptionPaneDemo() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        JPanel bp = new JPanel() {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(getPreferredSize().width, super.getMaximumSize().height);
            }
        };
        bp.setLayout(new BoxLayout(bp, BoxLayout.Y_AXIS));

        bp.add(Box.createRigidArea(VGAP30));
        bp.add(Box.createRigidArea(VGAP30));

        bp.add(createInputDialogButton());
        bp.add(Box.createRigidArea(VGAP15));
        bp.add(createWarningDialogButton());
        bp.add(Box.createRigidArea(VGAP15));
        bp.add(createMessageDialogButton());
        bp.add(Box.createRigidArea(VGAP15));
        bp.add(createComponentDialogButton());
        bp.add(Box.createRigidArea(VGAP15));
        bp.add(createConfirmDialogButton());
        bp.add(Box.createVerticalGlue());

        add(Box.createHorizontalGlue());
        add(bp);
        add(Box.createHorizontalGlue());
    }

    private JButton createWarningDialogButton() {
        Action a = new AbstractAction(WARNING_BUTTON) {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(OptionPaneDemo.this,
                        WARNING_TEXT,
                        WARNING_TITLE,
                        JOptionPane.WARNING_MESSAGE
                );
            }
        };
        return createButton(a);
    }

    private JButton createMessageDialogButton() {
        Action a = new AbstractAction(MESSAGE_BUTTON) {
            final URL img = getClass().getResource("resources/images/bottle.gif");
            final String imagesrc = "<img src=\"" + img + "\" width=\"284\" height=\"100\">";
            final String message = MESSAGE_TEXT;

            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(
                        OptionPaneDemo.this,
                        "<html>" + imagesrc + "<br><center>" + message + "</center><br></html>"
                );
            }
        };
        return createButton(a);
    }

    private JButton createConfirmDialogButton() {
        Action a = new AbstractAction(CONFIRM_BUTTON) {
            @Override
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(OptionPaneDemo.this, CONFIRM_QUESTION);
                if (result == JOptionPane.YES_OPTION) {
                    JOptionPane.showMessageDialog(OptionPaneDemo.this, CONFIRM_YES);
                } else if (result == JOptionPane.NO_OPTION) {
                    JOptionPane.showMessageDialog(OptionPaneDemo.this, CONFIRM_NO);
                }
            }
        };
        return createButton(a);
    }

    private JButton createInputDialogButton() {
        Action a = new AbstractAction(INPUT_BUTTON) {
            @Override
            public void actionPerformed(ActionEvent e) {
                String result = JOptionPane.showInputDialog(OptionPaneDemo.this, INPUT_QUESTION);
                if ((result != null) && (result.length() > 0)) {
                    JOptionPane.showMessageDialog(OptionPaneDemo.this,
                            result + INPUT_RESPONSE);
                }
            }
        };
        return createButton(a);
    }

    private JButton createComponentDialogButton() {
        Action a = new AbstractAction(COMPONENT_BUTTON) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // In a ComponentDialog, you can show as many message components and
                // as many options as you want:

                // Messages
                Object[] message = new Object[4];
                message[0] = COMPONENT_MESSAGE;
                message[1] = new JTextField(COMPONENT_TEXT_FIELD);

                JComboBox<String> cb = new JComboBox<>();
                cb.addItem(COMPONENT_CB1);
                cb.addItem(COMPONENT_CB2);
                cb.addItem(COMPONENT_CB3);
                message[2] = cb;

                message[3] = COMPONENT_MESSAGE_2;

                // Options
                String[] options = {
                    COMPONENT_OP1, COMPONENT_OP2, COMPONENT_OP3, COMPONENT_OP4, COMPONENT_OP5};
                int result = JOptionPane.showOptionDialog(
                        OptionPaneDemo.this, // the parent that the dialog blocks
                        message, // the dialog message array
                        COMPONENT_TITLE, // the title of the dialog window
                        JOptionPane.DEFAULT_OPTION, // option type
                        JOptionPane.INFORMATION_MESSAGE, // message type
                        null, // optional icon, use null to use the default icon
                        options, // options string array, will be made into buttons
                        options[3] // option that should be made into a default button
                );
                switch (result) {
                    case 0: // yes
                        JOptionPane.showMessageDialog(OptionPaneDemo.this, COMPONENT_R1);
                        break;
                    case 1: // no
                        JOptionPane.showMessageDialog(OptionPaneDemo.this, COMPONENT_R2);
                        break;
                    case 2: // maybe
                        JOptionPane.showMessageDialog(OptionPaneDemo.this, COMPONENT_R3);
                        break;
                    case 3: // probably
                        JOptionPane.showMessageDialog(OptionPaneDemo.this, COMPONENT_R4);
                        break;
                    default:
                        break;
                }

            }
        };
        return createButton(a);
    }

    private JButton createButton(Action a) {
        JButton b = new JButton() {
            @Override
            public Dimension getMaximumSize() {
                int width = Short.MAX_VALUE;
                int height = super.getMaximumSize().height;
                return new Dimension(width, height);
            }
        };
        // setting the following client property informs the button to show
        // the action text as it's name. The default is to not show the
        // action text.
        b.putClientProperty("displayActionText", Boolean.TRUE);
        b.setAction(a);
        // b.setAlignmentX(JButton.CENTER_ALIGNMENT);
        return b;
    }

}
