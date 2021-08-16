/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @test %I% %E%
 * @key headful
 * @bug 2161766
 * @summary Component is missing after changing the z-order of the component & focus is not transfered in
 * @author  Andrei Dmitriev : area=awt.container
 * @run main CheckZOrderChange
 */

import java.awt.*;
import java.awt.event.*;

public class CheckZOrderChange {

    private static Button content[] = new Button[]{new Button("Button 1"), new Button("Button 2"), new Button("Button 3"), new Button("Button 4")};
    private static Frame frame;

    public static void main(String[] args) {

        frame = new Frame("Test Frame");
        frame.setLayout(new FlowLayout());

        for (Button b: content){
            frame.add(b);
        }

        frame.setSize(300, 300);
        frame.setVisible(true);

        /* INITIAL ZORDERS ARE*/
        for (Button b: content){
            System.out.println("frame.getComponentZOrder("+ b +") = " + frame.getComponentZOrder(b));
        }

        //Change the Z Order
        frame.setComponentZOrder(content[0], 2);
        System.out.println("ZOrder of button1 changed to 2");

        if (frame.getComponentZOrder(content[0]) != 2 ||
            frame.getComponentZOrder(content[1]) != 0 ||
            frame.getComponentZOrder(content[2]) != 1 ||
            frame.getComponentZOrder(content[3]) != 3)
        {
            for (Button b: content){
                System.out.println("frame.getComponentZOrder("+ b +") = " + frame.getComponentZOrder(b));
            }
            throw new RuntimeException("TEST FAILED: getComponentZOrder did not return the correct value");
        }
    }
}
