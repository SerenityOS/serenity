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
  @bug 6502358
  @summary focus is not restored after programmatic iconification and restoring
  @author : area=awt.focus
  @library ../../regtesthelpers
  @build Util
  @run main FrameMinimizeTest
*/

import java.awt.Button;
import java.awt.Frame;

import test.java.awt.regtesthelpers.Util;

public class FrameMinimizeTest {
    public static void main(String args[]) throws Exception {
        Frame frame = new Frame("Frame Minimize Test");
        Button b = new Button("Focus ownder");
        frame.add("South", b);
        frame.pack();
        frame.setVisible(true);
        Util.waitForIdle(null);
        if (!b.hasFocus()) {
            throw new RuntimeException("button is not a focus owner after showing :(");
        }
        frame.setExtendedState(Frame.ICONIFIED);
        Util.waitForIdle(null);
        frame.setExtendedState(Frame.NORMAL);
        Util.waitForIdle(null);
        if (!b.hasFocus()) {
            throw new RuntimeException("button is not a focus owner after restoring :(");
        }
    }
}
