/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @key headful
  @bug 5004032
  @summary GridBagConstraints.ipad(x|y) defined in a new way
  @run main GridBagLayoutIpadXYTest
*/

import java.awt.*;

public class GridBagLayoutIpadXYTest {
    static Frame frame = new Frame();
    static TextField jtf = null;
    static final int customIpadx = 300;
    static final int customIpady = 40;

    public static void main(final String[] args) {
        frame.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        Insets fieldInsets = new Insets(0,5,5,0);

        gc.anchor = gc.NORTH;
        gc.fill = gc.HORIZONTAL;
        gc.gridx = 1;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.ipadx = customIpadx;
        gc.ipady = customIpady;
        gc.insets = fieldInsets;
        jtf = new TextField();
        frame.add(jtf, gc);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Robot robot;
        try {
            robot = new Robot();
            robot.waitForIdle();
        }catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Unexpected failure");
        }

        Dimension minSize = jtf.getMinimumSize();
        if ( minSize.width + customIpadx != jtf.getSize().width ||
             minSize.height + customIpady != jtf.getSize().height ){
            System.out.println("TextField originally has min size = " + jtf.getMinimumSize());
            System.out.println("TextField supplied with ipadx =  300, ipady =40");
            System.out.println("Frame size: " + frame.getSize());
            System.out.println(" Fields's size is "+jtf.getSize());

            throw new RuntimeException("Test Failed. TextField has incorrect width. ");
        }
        System.out.println("Test Passed.");
    }
}
