/*
 *
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.border.*;
import javax.swing.colorchooser.*;
import javax.swing.filechooser.*;
import javax.accessibility.*;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import java.io.*;
import java.applet.*;
import java.net.*;

/**
 * JOptionPaneDemo
 *
 * @author Jeff Dinkins
 */
public class OptionPaneDemo extends DemoModule {

    /**
     * main method allows us to run as a standalone demo.
     */
    public static void main(String[] args) {
        OptionPaneDemo demo = new OptionPaneDemo(null);
        demo.mainImpl();
    }

    /**
     * OptionPaneDemo Constructor
     */
    public OptionPaneDemo(SwingSet2 swingset) {
        // Set the title for this demo, and an icon used to represent this
        // demo inside the SwingSet2 app.
        super(swingset, "OptionPaneDemo", "toolbar/JOptionPane.gif");

        JPanel demo = getDemoPanel();

        demo.setLayout(new BoxLayout(demo, BoxLayout.X_AXIS));

        JPanel bp = new JPanel() {
            public Dimension getMaximumSize() {
                return new Dimension(getPreferredSize().width, super.getMaximumSize().height);
            }
        };
        bp.setLayout(new BoxLayout(bp, BoxLayout.Y_AXIS));

        bp.add(Box.createRigidArea(VGAP30));
        bp.add(Box.createRigidArea(VGAP30));

        bp.add(createInputDialogButton());      bp.add(Box.createRigidArea(VGAP15));
        bp.add(createWarningDialogButton());    bp.add(Box.createRigidArea(VGAP15));
        bp.add(createMessageDialogButton());    bp.add(Box.createRigidArea(VGAP15));
        bp.add(createComponentDialogButton());  bp.add(Box.createRigidArea(VGAP15));
        bp.add(createConfirmDialogButton());    bp.add(Box.createVerticalGlue());

        demo.add(Box.createHorizontalGlue());
        demo.add(bp);
        demo.add(Box.createHorizontalGlue());
    }

    public JButton createWarningDialogButton() {
        Action a = new AbstractAction(getString("OptionPaneDemo.warningbutton")) {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(
                    getDemoPanel(),
                    getString("OptionPaneDemo.warningtext"),
                    getString("OptionPaneDemo.warningtitle"),
                    JOptionPane.WARNING_MESSAGE
                );
            }
        };
        return createButton(a);
    }

    public JButton createMessageDialogButton() {
        Action a = new AbstractAction(getString("OptionPaneDemo.messagebutton")) {
            URL img = getClass().getResource("/resources/images/optionpane/bottle.gif");
            String imagesrc = "<img src=\"" + img + "\" width=\"284\" height=\"100\">";
            String message = getString("OptionPaneDemo.messagetext");
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(
                    getDemoPanel(),
                    "<html>" + imagesrc + "<br><center>" + message + "</center><br></html>"
                );
            }
        };
        return createButton(a);
    }

    public JButton createConfirmDialogButton() {
        Action a = new AbstractAction(getString("OptionPaneDemo.confirmbutton")) {
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(getDemoPanel(), getString("OptionPaneDemo.confirmquestion"));
                if(result == JOptionPane.YES_OPTION) {
                    JOptionPane.showMessageDialog(getDemoPanel(), getString("OptionPaneDemo.confirmyes"));
                } else if(result == JOptionPane.NO_OPTION) {
                    JOptionPane.showMessageDialog(getDemoPanel(), getString("OptionPaneDemo.confirmno"));
                }
            }
        };
        return createButton(a);
    }

    public JButton createInputDialogButton() {
        Action a = new AbstractAction(getString("OptionPaneDemo.inputbutton")) {
            public void actionPerformed(ActionEvent e) {
                String result = JOptionPane.showInputDialog(getDemoPanel(), getString("OptionPaneDemo.inputquestion"));
                if ((result != null) && (result.length() > 0)) {
                    JOptionPane.showMessageDialog(getDemoPanel(),
                                    result + ": " +
                                    getString("OptionPaneDemo.inputresponse"));
                }
            }
        };
        return createButton(a);
    }

    public JButton createComponentDialogButton() {
        Action a = new AbstractAction(getString("OptionPaneDemo.componentbutton")) {
            public void actionPerformed(ActionEvent e) {
                // In a ComponentDialog, you can show as many message components and
                // as many options as you want:

                // Messages
                Object[]      message = new Object[4];
                message[0] = getString("OptionPaneDemo.componentmessage");
                message[1] = new JTextField(getString("OptionPaneDemo.componenttextfield"));

                JComboBox<String> cb = new JComboBox<>();
                cb.addItem(getString("OptionPaneDemo.component_cb1"));
                cb.addItem(getString("OptionPaneDemo.component_cb2"));
                cb.addItem(getString("OptionPaneDemo.component_cb3"));
                message[2] = cb;

                message[3] = getString("OptionPaneDemo.componentmessage2");

                // Options
                String[] options = {
                    getString("OptionPaneDemo.component_op1"),
                    getString("OptionPaneDemo.component_op2"),
                    getString("OptionPaneDemo.component_op3"),
                    getString("OptionPaneDemo.component_op4"),
                    getString("OptionPaneDemo.component_op5")
                };
                int result = JOptionPane.showOptionDialog(
                    getDemoPanel(),                             // the parent that the dialog blocks
                    message,                                    // the dialog message array
                    getString("OptionPaneDemo.componenttitle"), // the title of the dialog window
                    JOptionPane.DEFAULT_OPTION,                 // option type
                    JOptionPane.INFORMATION_MESSAGE,            // message type
                    null,                                       // optional icon, use null to use the default icon
                    options,                                    // options string array, will be made into buttons
                    options[3]                                  // option that should be made into a default button
                );
                switch(result) {
                   case 0: // yes
                     JOptionPane.showMessageDialog(getDemoPanel(), getString("OptionPaneDemo.component_r1"));
                     break;
                   case 1: // no
                     JOptionPane.showMessageDialog(getDemoPanel(), getString("OptionPaneDemo.component_r2"));
                     break;
                   case 2: // maybe
                     JOptionPane.showMessageDialog(getDemoPanel(), getString("OptionPaneDemo.component_r3"));
                     break;
                   case 3: // probably
                     JOptionPane.showMessageDialog(getDemoPanel(), getString("OptionPaneDemo.component_r4"));
                     break;
                   default:
                     break;
                }

            }
        };
        return createButton(a);
    }

    public JButton createButton(Action a) {
        JButton b = new JButton() {
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
