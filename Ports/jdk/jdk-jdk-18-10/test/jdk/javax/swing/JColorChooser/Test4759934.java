/*
 * Copyright (c) 2003, 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4759934
 * @summary Tests windows activation problem
 * @author Andrey Pikalev
 * @run applet/manual=yesno Test4759934.html
 */

import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;

public class Test4759934 extends JApplet implements ActionListener {
    private static final String CMD_DIALOG = "Show Dialog"; // NON-NLS: first button
    private static final String CMD_CHOOSER = "Show ColorChooser"; // NON-NLS: second button

    private final JFrame frame = new JFrame("Test"); // NON-NLS: frame title

    public void init() {
        show(this.frame, CMD_DIALOG);
    }

    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (CMD_DIALOG.equals(command)) {
            JDialog dialog = new JDialog(this.frame, "Dialog"); // NON-NLS: dialog title
            dialog.setLocation(200, 0);
            show(dialog, CMD_CHOOSER);
        }
        else if (CMD_CHOOSER.equals(command)) {
            Object source = event.getSource();
            Component component = (source instanceof Component)
                    ? (Component) source
                    : null;

            JColorChooser.showDialog(component, "ColorChooser", Color.BLUE); // NON-NLS: title
        }
    }

    private void show(Window window, String command) {
        JButton button = new JButton(command);
        button.setActionCommand(command);
        button.addActionListener(this);
        button.setFont(button.getFont().deriveFont(64.0f));

        window.add(button);
        window.pack();
        window.setVisible(true);
    }
}
