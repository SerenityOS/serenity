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
 * @bug 6348456
 * @summary Tests model changing
 * @author Sergey Malenkov
 * @run applet/manual=yesno Test6348456.html
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.colorchooser.DefaultColorSelectionModel;

public final class Test6348456 extends JApplet implements ActionListener {

    private static final DefaultColorSelectionModel WHITE = new DefaultColorSelectionModel(Color.WHITE);
    private static final DefaultColorSelectionModel BLACK = new DefaultColorSelectionModel(Color.BLACK);

    private JColorChooser chooser;

    @Override
    public void init() {
        JButton button = new JButton("Swap models");
        button.addActionListener(this);

        this.chooser = new JColorChooser(Color.RED);
        this.chooser.setSelectionModel(WHITE);

        add(BorderLayout.NORTH, button);
        add(BorderLayout.CENTER, this.chooser);
    }

    public void actionPerformed(ActionEvent event){
        this.chooser.setSelectionModel(this.chooser.getSelectionModel() == BLACK ? WHITE : BLACK);
    }
}
