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

import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Dimension;
/*
 * @test
 * @key headful
 * @bug 8066436
 * @summary Set the size of frame. Set extendedState Frame.MAXIMIZED_BOTH and Frame.NORMAL
 *          sequentially for undecorated Frame and .
 *          Check if resulted size is equal to original frame size.
 * @run main MaximizedNormalBoundsUndecoratedTest
 */


public class MaximizedNormalBoundsUndecoratedTest {
    private Frame frame;
    public static void main(String args[]) {
        if (!Toolkit.getDefaultToolkit().isFrameStateSupported(Frame.MAXIMIZED_BOTH)
                && !Toolkit.getDefaultToolkit().isFrameStateSupported(Frame.NORMAL)) {
            return;
        }
        MaximizedNormalBoundsUndecoratedTest test = new MaximizedNormalBoundsUndecoratedTest();
        boolean doPass = true;
        if( !test.doTest() ) {
            System.out.println("Maximizing frame not saving correct normal bounds");
            doPass = false;
        }

        if(!doPass) {
            throw new RuntimeException("Maximizing frame not saving correct normal bounds");
        }
    }

    boolean doTest() {
        Dimension beforeMaximizeCalled = new Dimension(300,300);

        frame = new Frame("Test Frame");
        frame.setUndecorated(true);
        frame.setFocusable(true);
        frame.setSize(beforeMaximizeCalled);
        frame.setVisible(true);
        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.setExtendedState(Frame.NORMAL);

        Dimension afterMaximizedCalled= frame.getBounds().getSize();

        frame.dispose();

        if (beforeMaximizeCalled.equals(afterMaximizedCalled)) {
            return true;
        }
        return false;
    }
}
