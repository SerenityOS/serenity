/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @key headful
 * @bug     4108453 4778440 6304785
 * @summary Test Window.applyResourceBundle orientation support
 *
 * @build TestBundle TestBundle_es TestBundle_iw
 * @build TestBundle1 TestBundle1_ar
 * @run main WindowTest
 */

import java.awt.Frame;
import java.awt.Panel;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.util.Locale;
import java.util.ResourceBundle;

public class WindowTest {

    public static void main(String args[]) throws Exception {
        Frame frame  = new Frame();
        frame.setSize(200,200);
        frame.setVisible(true);
        try {
            doTest(frame);
        } finally {
            frame.setVisible(false);
            frame.dispose();
        }
    }

    public static void doTest (Frame  myFrame) throws Exception{
        System.out.println("WindowTest {");

        ResourceBundle rb;

        // Create a window containing a hierarchy of components.
        System.out.println("  Creating component hierarchy...");
        myFrame.setLayout(new FlowLayout());
        Panel panel1 = new Panel();
        panel1.setLayout(new BorderLayout());
        panel1.add("North", new Button("North"));
        panel1.add("South", new Button("South"));
        panel1.add("East", new Button("East"));
        panel1.add("West", new Button("West"));
        panel1.add("Center", new Button("Center"));
        myFrame.add(panel1);

        Panel panel2 = new Panel();
        panel2.setLayout(new BorderLayout());
        panel2.add(BorderLayout.BEFORE_FIRST_LINE, new Button("FirstLine"));
        panel2.add(BorderLayout.AFTER_LAST_LINE, new Button("LastLine"));
        panel2.add(BorderLayout.BEFORE_LINE_BEGINS, new Button("FirstItem"));
        panel2.add(BorderLayout.AFTER_LINE_ENDS, new Button("LastItem"));
        panel2.add("Center", new Button("Center"));
        myFrame.add(panel2);

        // After construction, all of the components' orientations should be
        // set to ComponentOrientation.UNKNOWN.
        System.out.println("  Verifying orientation is UNKNOWN...");
        verifyOrientation(myFrame, ComponentOrientation.UNKNOWN);

        // This will load TestBundle1 using the default locale and apply
        // it to the component hierarchy.  Since the bundle has no Orientation
        // specified, this should fall back to the bundle-locale's orientation
        System.out.println("  Applying TestBundle1 by name and verifying...");
        myFrame.applyResourceBundle("TestBundle1");
        verifyOrientation(myFrame,
                    ComponentOrientation.getOrientation(
                        ResourceBundle.getBundle("TestBundle1", Locale.getDefault())));

        System.out.println("  Applying TestBundle_iw and verifying...");
        rb = ResourceBundle.getBundle("TestBundle", new Locale("iw", ""));
        myFrame.applyResourceBundle(rb);
        verifyOrientation(myFrame, ComponentOrientation.RIGHT_TO_LEFT);

        System.out.println("  Applying TestBundle_es and verifying...");
        rb = ResourceBundle.getBundle("TestBundle", new Locale("es", ""));
        myFrame.applyResourceBundle(rb);
        verifyOrientation(myFrame, ComponentOrientation.LEFT_TO_RIGHT);

        System.out.println("}");
    }

    static void verifyOrientation(Component c, ComponentOrientation orient) {

        ComponentOrientation o = c.getComponentOrientation();

        if (o != orient) {
            throw new RuntimeException("ERROR: expected " + oString(orient) +
                                        ", got " + oString(o) +
                                        " on component " + c);
        }

        if (c instanceof Container) {
            Container cont = (Container) c;
            int ncomponents = cont.getComponentCount();

            for (int i = 0 ; i < ncomponents ; ++i) {
                Component comp = cont.getComponent(i);
                verifyOrientation(comp, orient);
            }
        }
    }

    static String oString(ComponentOrientation o) {
        if (o == ComponentOrientation.LEFT_TO_RIGHT) {
            return "LEFT_TO_RIGHT";
        }
        else if (o == ComponentOrientation.RIGHT_TO_LEFT) {
            return "RIGHT_TO_LEFT";
        }
        else {
            return "UNKNOWN";
        }
    }
}
