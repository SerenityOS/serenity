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

/*
  @test
  @key headful
  @bug 4068386
  @summary Insets are incorrect for Frame w/MenuBar
  @library ../regtesthelpers
  @build Util
  @author Andrei Dmitriev : area=awt.insets
  @run main CombinedTestApp1
*/

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

public class CombinedTestApp1
{
    public static void main(String[] args)
    {
        Frame frame = new Frame("A test");
        frame.setSize (200,200);

        frame.setLayout(new FlowLayout());
        MenuBar mbar = new MenuBar();
        Menu file = new Menu("File");
        mbar.add(file);

        MenuItem quit = new MenuItem("Quit", new MenuShortcut(KeyEvent.VK_Q));
        quit.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { System.exit(0);}
            });
        file.add(quit);

        frame.setMenuBar(mbar);
        frame.add(new Button("One"));
        frame.add(new Button("Two"));
        frame.add(new Button("Three"));

        frame.pack();
        frame.setVisible(true);
        try {
            Robot robot = new Robot();
            Util.waitForIdle(robot);
        } catch(AWTException e){
            throw new RuntimeException("Test interrupted.", e);
        }

        Insets frameInsets = frame.getInsets();
        System.out.println(frameInsets);
        if (frameInsets.bottom < 0 || frameInsets.top < 0 ||
            frameInsets.right < 0 || frameInsets.left <0){
            throw new RuntimeException("Failed. Frames' Insets are incorrect.");
        }
        System.out.println(" Test Passed. ");
    }
}
