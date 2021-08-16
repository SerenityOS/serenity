/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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

import static java.awt.Color.RED;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.border.MatteBorder;

/*
 * @test
 * @bug 6910490
 * @summary Tests a matte border around a component inside a scroll pane.
 * @author Sergey Malenkov
 * @run applet/manual=yesno Test6910490.html
 */

public class Test6910490 extends JApplet implements Icon {

    @Override
    public void init() {
        Insets insets = new Insets(10, 10, 10, 10);
        Dimension size = new Dimension(getWidth() / 2, getHeight());
        JSplitPane pane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                create("Color", size, new MatteBorder(insets, RED)),
                create("Icon", size, new MatteBorder(insets, this)));
        pane.setDividerLocation(size.width - pane.getDividerSize() / 2);
        add(pane);
    }

    private JScrollPane create(String name, Dimension size, MatteBorder border) {
        JButton button = new JButton(name);
        button.setPreferredSize(size);
        button.setBorder(border);
        return new JScrollPane(button);
    }

    public int getIconWidth() {
        return 10;
    }

    public int getIconHeight() {
        return 10;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor(RED);
        g.fillRect(x, y, getIconWidth(), getIconHeight());
    }
}
