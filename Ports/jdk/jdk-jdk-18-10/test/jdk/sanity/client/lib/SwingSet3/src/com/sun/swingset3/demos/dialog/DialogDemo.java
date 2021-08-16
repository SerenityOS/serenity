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
package com.sun.swingset3.demos.dialog;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import com.sun.swingset3.DemoProperties;
import com.sun.swingset3.demos.DemoUtilities;
import com.sun.swingset3.demos.ResourceManager;
import com.sun.swingset3.demos.slider.SliderDemo;

/**
 *
 * @author aim
 */
@DemoProperties(
        value = "JDialog Demo",
        category = "Toplevel Containers",
        description = "Demonstrates JDialog, Swing's top-level dialog container.",
        sourceFiles = {
            "com/sun/swingset3/demos/dialog/DialogDemo.java",
            "com/sun/swingset3/demos/DemoUtilities.java",
            "com/sun/swingset3/demos/dialog/resources/images/DialogDemo.gif"
        }
)
public class DialogDemo extends JPanel {

    private JDialog dialog;
    private JComponent dialogSpaceholder;

    public static final String DIALOG_TITLE = "Demo JDialog";
    public static final String SHOW_BUTTON_TITLE = "Show JDialog...";
    public static final String LABEL_CONTENT = "I'm content.";
    public static final String DIALOG_DEMO_TITLE = DialogDemo.class
            .getAnnotation(DemoProperties.class).value();

    public DialogDemo() {
        initComponents();
    }

    protected void initComponents() {
        dialog = createDialog();

        setLayout(new BorderLayout());

        add(createControlPanel(), BorderLayout.WEST);
        dialogSpaceholder = createDialogSpaceholder(dialog);
        add(dialogSpaceholder, BorderLayout.CENTER);
    }

    private static JComponent createDialogSpaceholder(JDialog dialog) {
        // Create placeholder panel to provide space in which to
        // display the toplevel dialog so that the control panel is not
        // obscured by it.
        JPanel placeholder = new JPanel();
        Dimension prefSize = dialog.getPreferredSize();
        prefSize.width += 12;
        prefSize.height += 12;
        placeholder.setPreferredSize(prefSize);
        return placeholder;
    }

    protected JComponent createControlPanel() {
        // Create control panel on Left
        Box panel = Box.createVerticalBox();
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Create button to control visibility of frame
        JButton showButton = new JButton(SHOW_BUTTON_TITLE);
        showButton.addActionListener(new ShowActionListener());
        panel.add(showButton);

        return panel;
    }

    private static JDialog createDialog() {

        //<snip>Create dialog
        JDialog dialog = new JDialog(new JFrame(), DIALOG_TITLE, false);
        //</snip>

        //<snip>Add dialog's content
        JLabel label = new JLabel(LABEL_CONTENT);
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setPreferredSize(new Dimension(200, 140));
        dialog.add(label);
        //</snip>

        //<snip>Initialize dialog's size
        // which will shrink-to-fit its contents
        dialog.pack();
        //</snip>

        return dialog;
    }

    public void start() {
        DemoUtilities.setToplevelLocation(dialog, dialogSpaceholder, SwingConstants.CENTER);
        showDialog();
    }

    public void stop() {
        //<snip>Hide dialog
        dialog.setVisible(false);
        //</snip>
    }

    public void showDialog() {
        //<snip>Show dialog
        // if dialog already visible, then bring to the front
        if (dialog.isShowing()) {
            dialog.toFront();
        } else {
            dialog.setVisible(true);
        }
        //</snip>
    }

    private class ShowActionListener implements ActionListener {

        public void actionPerformed(ActionEvent actionEvent) {
            showDialog();
        }
    }

    public static void main(String args[]) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame(DIALOG_DEMO_TITLE);
                DialogDemo demo = new DialogDemo();
                frame.add(demo);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                demo.start();
            }
        });
    }
}
