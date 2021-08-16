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
 * @summary Test ComponentOrientation (Bidi) support in FlowLayout
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

public class FlowTest extends Panel {
    Panel       panel;

    public FlowTest() {
        setLayout(new BorderLayout());

        // Create a panel with a FlowLayout and a bunch of buttons in it
        panel = new Panel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.add(new Button("one"));
        panel.add(new Button("two"));
        panel.add(new Button("three"));
        panel.add(new Button("four"));
        panel.add(new Button("five"));
        panel.add(new Button("six"));
        panel.add(new Button("seven"));
        panel.add(new Button("eight"));
        panel.add(new Button("nine"));
        panel.add(new Button("ten"));
        panel.add(new Button("eleven"));

        add("Center", panel);

        Panel controls = new Panel();
        controls.setLayout(new GridLayout(0, 2));

        // Menu for setting the alignment of the main FlowLayout panel
        {
            Choice c = new Choice();
            c.addItem("LEFT");
            c.addItem("CENTER");
            c.addItem("RIGHT");
            c.addItem("LEADING");
            c.addItem("TRAILING");
            c.addItemListener( new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    String item = (String)(e.getItem());
                    FlowLayout layout = (FlowLayout) panel.getLayout();

                    if (item.equals("LEFT")) {
                        layout.setAlignment(FlowLayout.LEFT);
                    } else if (item.equals("CENTER")) {
                        layout.setAlignment(FlowLayout.CENTER);
                    } else if (item.equals("RIGHT")) {
                        layout.setAlignment(FlowLayout.RIGHT);
                    } else if (item.equals("LEADING")) {
                        layout.setAlignment(FlowLayout.LEADING);
                    } else if (item.equals("TRAILING")) {
                        layout.setAlignment(FlowLayout.TRAILING);
                    }
                    panel.layout();
                    panel.repaint();
                }
            } );
            controls.add(new Label("FlowLayout Alignment:"));
            controls.add(c);
        }

        // Create a popup menu for switching the Panel between orientations
        {
            Choice c = new Choice();
            c.addItem("LEFT_TO_RIGHT");
            c.addItem("RIGHT_TO_LEFT");
            c.addItem("UNKNOWN");
            c.addItemListener( new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    String item = (String)(e.getItem());

                    if (item.equals("LEFT_TO_RIGHT")) {
                        panel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
                    } else if (item.equals("RIGHT_TO_LEFT")) {
                        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
                    } else {
                        panel.setComponentOrientation(ComponentOrientation.UNKNOWN);
                    }
                    panel.layout();
                    panel.repaint();
                }
            } );

            controls.add(new Label("ComponentOrientation:"));
            controls.add(c);
        }

        add("South", controls);

    }

    public static void main(String args[]) {
        Frame f = new Frame("FlowTest");

        f.addWindowListener( new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                e.getWindow().hide();
                e.getWindow().dispose();
                System.exit(0);
            };
        } );

        FlowTest flowTest = new FlowTest();

        f.add("Center", flowTest);
        f.setSize(300, 300);
        f.show();
    }
}
