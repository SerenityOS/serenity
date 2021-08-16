/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test 1.0 04/04/23
 * @key headful
 * @bug 5012888
 * @summary REGRESSION: Click & hold on arrow of JSpinner only transfers focus
 * @author Konstantin Eremin
 * @run main bug5012888
 */

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

public class bug5012888 extends JFrame {
    JSpinner spinner1, spinner2;
    public bug5012888() {
      spinner1 = new JSpinner(new SpinnerNumberModel(0, -1000, 1000, 1));
      spinner2 = new JSpinner(new SpinnerNumberModel(1, -1000, 1000, 1));
      Container pane = getContentPane();
      pane.setLayout(new BorderLayout());
      pane.add(spinner1, BorderLayout.NORTH);
      pane.add(spinner2, BorderLayout.SOUTH);
    }
    public void doTest() throws Exception {
        Robot robot = new Robot();
        robot.waitForIdle();
        Point p = spinner2.getLocationOnScreen();
        Rectangle rect = spinner2.getBounds();
        robot.mouseMove(p.x+rect.width-5, p.y+5);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        Thread.sleep(1000);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        if ( ((Integer) spinner2.getValue()).intValue() == 1 ) {
            throw new Error("Spinner value should be more than 1");
        }
    }
    public static void main(String[] argv) throws Exception {
        bug5012888 b = new bug5012888();
        b.setBounds(0, 0, 100, 100);
        b.setVisible(true);
        b.doTest();
    }
}
