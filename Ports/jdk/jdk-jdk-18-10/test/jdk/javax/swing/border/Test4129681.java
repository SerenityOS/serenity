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

/*
 * @test
 * @bug 4129681
 * @summary Tests enabling/disabling of titled border's caption
 * @author Sergey Malenkov
 * @run applet/manual=yesno Test4129681.html
 */

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.BorderFactory;
import javax.swing.JApplet;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

public class Test4129681 extends JApplet implements ItemListener {
    private JLabel label;

    @Override
    public void init() {
        JCheckBox check = new JCheckBox("disable");
        check.addItemListener(this);

        this.label = new JLabel("message");
        this.label.setBorder(BorderFactory.createTitledBorder("label"));
        this.label.setEnabled(!check.isSelected());

        add(BorderLayout.NORTH, check);
        add(BorderLayout.CENTER, this.label);
    }

    public void itemStateChanged(ItemEvent event) {
        this.label.setEnabled(ItemEvent.DESELECTED == event.getStateChange());
    }
}
