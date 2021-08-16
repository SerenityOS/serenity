/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4904961
 * @summary Test that Dialog with zero sizes won't be created with negative sizes due to overflow in peer code
 * @author Andrei Dmitriev: area=awt.toplevel
 * @library ../../regtesthelpers
 * @build Util
 * @run main DialogSizeOverflowTest
 */

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

public class DialogSizeOverflowTest
{
    public static void main(String [] s) {
        Robot robot;
        Frame f = new Frame("a frame");
        final Dialog dlg = new Dialog(f, false);

        f.setVisible(true);

        try {
            robot = new Robot();
        } catch(AWTException e){
            throw new RuntimeException("Test interrupted.", e);
        }
        Util.waitForIdle(robot);

        dlg.setLocation(100, 100);
        dlg.setResizable(false);
        dlg.addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    Dimension size = dlg.getSize();
                    System.out.println("size.width : size.height "+size.width + " : "+ size.height);
                    if (size.width > 1000 || size.height > 1000 || size.width < 0 || size.height < 0) {
                        throw new RuntimeException("Test failed. Size is too large.");
                    }
                }
            });
        dlg.toBack();
        dlg.setVisible(true);

        Util.waitForIdle(robot);
        System.out.println("Test passed.");
    }
}
