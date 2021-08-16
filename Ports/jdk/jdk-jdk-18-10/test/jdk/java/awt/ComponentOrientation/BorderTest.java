/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug     4108453
 * @summary Test ComponentOrientation (Bidi) support in BorderLayout
 */
/*
 * (C) Copyright IBM Corp. 1998 - All Rights Reserved
 *
 * The original version of this source code and documentation is copyrighted
 * and owned by IBM, Inc. These materials are provided under terms of a
 * License Agreement between IBM and Sun. This technology is protected by
 * multiple US and International patents. This notice and attribution to IBM
 * may not be removed.
 */

import java.awt.*;
import java.awt.event.*;

public class BorderTest extends Panel {
    Panel       panel1;
    Panel       panel2;

    public BorderTest() {
        setLayout(new GridLayout(0,2));

        // Create a panel with a BorderLayout and a bunch of buttons in it
        panel1 = new Panel();
        panel1.setLayout(new BorderLayout());
        panel1.add("North", new Button("North"));
        panel1.add("South", new Button("South"));
        panel1.add("East", new Button("East"));
        panel1.add("West", new Button("West"));
        panel1.add("Center", new Button("Center"));
        add(panel1);

        // Create a panel with a BorderLayout and a bunch of buttons in it
        panel2 = new Panel();
        panel2.setLayout(new BorderLayout());
        panel2.add(BorderLayout.BEFORE_FIRST_LINE, new Button("FirstLine"));
        panel2.add(BorderLayout.AFTER_LAST_LINE, new Button("LastLine"));
        panel2.add(BorderLayout.BEFORE_LINE_BEGINS, new Button("FirstItem"));
        panel2.add(BorderLayout.AFTER_LINE_ENDS, new Button("LastItem"));
        panel2.add("Center", new Button("Center"));
        add(panel2);

        // Create a popup menu for switching between orientations
        {
            Choice c = new Choice();
            c.addItem("LEFT_TO_RIGHT");
            c.addItem("RIGHT_TO_LEFT");
            c.addItem("UNKNOWN");
            c.addItemListener( new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    String item = (String)(e.getItem());

                    ComponentOrientation o = ComponentOrientation.UNKNOWN;
                    if (item.equals("LEFT_TO_RIGHT")) {
                        o = ComponentOrientation.LEFT_TO_RIGHT;
                    } else if (item.equals("RIGHT_TO_LEFT")) {
                        o = ComponentOrientation.RIGHT_TO_LEFT;
                    }
                    panel1.setComponentOrientation(o);
                    panel2.setComponentOrientation(o);
                    panel1.layout();
                    panel2.layout();
                    panel1.repaint();
                    panel2.repaint();
                }
            } );
            add(c);
        }
    }

    public static void main(String args[]) {
        Frame f = new Frame("BorderTest");

        f.addWindowListener( new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                e.getWindow().hide();
                e.getWindow().dispose();
                System.exit(0);
            };
        } );

        BorderTest BorderTest = new BorderTest();

        f.add("Center", BorderTest);
        f.setSize(450, 300);
        f.show();
    }
}
