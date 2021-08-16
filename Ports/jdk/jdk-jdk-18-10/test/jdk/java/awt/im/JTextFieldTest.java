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

/*
 * @test
 * @bug  4226191
 * @summary  Verify that Lightweight text components (like swing JTextField)
 *           work correctly with IM when there is an uneditable peered
 *           TextField/TextArea in the same parent Frame
 * @author xueming.shen@eng
 * @run applet/manual=yesno JTextFieldTest.html
 */

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import javax.swing.*;

public class JTextFieldTest extends Applet implements ActionListener {

    TextField  tf1;
    JTextField tf2;

    public JTextFieldTest() {
        tf1 = new TextField("ABCDEFGH", 10);
        tf1.setEditable(false);
        tf2 = new JTextField("12345678", 10);
        setLayout(new FlowLayout());
        add(tf1);
        add(tf2);
    }

    public void actionPerformed(ActionEvent ae) {

    }

    public static void main(String args[]) {
        JFrame  win = new JFrame();
        JTextFieldTest jtf = new JTextFieldTest();
        win.getContentPane().setLayout(new FlowLayout());
        win.getContentPane().add(jtf);
        win.pack();
        win.show();
    }
}
