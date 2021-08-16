/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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

/**
 *
 * @bug 4959409
 * @author Naoto Sato
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class bug4959409 extends javax.swing.JApplet {
    public void init() {
        new TestFrame();
    }
}

class TestFrame extends JFrame implements KeyListener {
    JTextField text;
    JLabel label;

    TestFrame () {
        text = new JTextField();
        text.addKeyListener(this);
        label = new JLabel(" ");
        Container c = getContentPane();
        BorderLayout borderLayout1 = new BorderLayout();
        c.setLayout(borderLayout1);
        c.add(text, BorderLayout.CENTER);
        c.add(label, BorderLayout.SOUTH);
        setSize(300, 200);
        setVisible(true);
    }

    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        int mods = e.getModifiers();
        if (code == '1' && mods == KeyEvent.SHIFT_MASK) {
            label.setText("KEYPRESS received for Shift+1");
        } else {
            label.setText(" ");
        }
    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }
}
