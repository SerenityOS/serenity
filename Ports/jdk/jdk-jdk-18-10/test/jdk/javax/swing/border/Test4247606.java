/*
 * Copyright (c) 2001, 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4247606
 * @summary BorderedPane appears wrong with Title Position Below Bottom
 * @author Andrey Pikalev
 * @run applet/manual=yesno Test4247606.html
 */

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

public class Test4247606 extends JApplet {
    public void init() {
        JButton button = new JButton("Button"); // NON-NLS: the button text
        button.setBorder(BorderFactory.createLineBorder(Color.red, 1));

        TitledBorder border = new TitledBorder("Bordered Pane"); // NON-NLS: the panel title
        border.setTitlePosition(TitledBorder.BELOW_BOTTOM);

        JPanel panel = create(button, border);
        panel.setBackground(Color.green);

        getContentPane().add(create(panel, BorderFactory.createEmptyBorder(10, 10, 10, 10)));
    }

    private static JPanel create(JComponent component, Border border) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(border);
        panel.add(component);
        return panel;
    }
}
