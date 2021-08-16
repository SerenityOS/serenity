/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6248040
  @summary List.deselect() de-selects the currently selected item regardless of the index, win32
  @author Dmitry Cherepanov area=awt.list
  @run main SingleModeDeselect
*/

import java.awt.*;

public class SingleModeDeselect
{
    public static final void main(String args[])
    {
        final Frame frame = new Frame();
        final List list = new List();

        list.add(" item 0 ");
        list.add(" item 1 ");

        frame.add(list);
        frame.setLayout(new FlowLayout());
        frame.setBounds(100,100,300,300);
        frame.setVisible(true);

        list.select(0);
        list.deselect(1);

        try {
            Robot robot = new Robot();
            robot.waitForIdle();
        }catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Unexpected failure");
        }

        if (list.getSelectedIndex() != 0){
            throw new RuntimeException("Test failed: List.getSelectedIndex() returns "+list.getSelectedIndex());
        }

    }//End  init()
}
