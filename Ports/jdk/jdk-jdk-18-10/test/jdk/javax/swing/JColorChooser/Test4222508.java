/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4222508
 * @summary Tests the color chooser disabling
 * @author Sergey Malenkov
 * @run applet/manual=yesno Test4222508.html
 */

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JApplet;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;

public final class Test4222508 extends JApplet implements ItemListener {

    private JCheckBox checkbox;
    private JColorChooser chooser;

    @Override
    public void init() {
        this.chooser = new JColorChooser();
        this.checkbox = new JCheckBox("Enable the color chooser below", true);
        this.checkbox.addItemListener(this);
        add(BorderLayout.NORTH, this.checkbox);
        add(BorderLayout.CENTER, this.chooser);
    }

    public void itemStateChanged(ItemEvent event) {
        this.chooser.setEnabled(this.checkbox.isSelected());
    }
}
