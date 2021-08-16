/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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


import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;

public final class SelectionVisible extends Applet {

    TextField tf;

    @Override
    public void init() {
        tf = new TextField(20);
        tf.setText("0123456789");
        tf.select(0, 6);

        final TextArea ta = new TextArea("INSTRUCTIONS:\n"
                                         + "The text 012345 should be selected in the TextField.\n"
                                         + "If this is what you observe, then the test passes.\n"
                                         + "Otherwise, the test fails.", 40, 5,
                                         TextArea.SCROLLBARS_NONE);
        ta.setEditable(false);
        ta.setPreferredSize(new Dimension(300, 70));
        final Panel panel = new Panel();
        panel.setLayout(new FlowLayout());
        panel.add(tf);
        setLayout(new BorderLayout());
        add(ta, BorderLayout.CENTER);
        add(panel, BorderLayout.PAGE_END);
    }

    @Override
    public void start() {
        setVisible(true);
        tf.requestFocus();
    }
}
