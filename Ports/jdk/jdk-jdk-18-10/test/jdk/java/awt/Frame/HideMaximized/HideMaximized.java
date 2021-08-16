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
  @test
  @key headful
  @bug 7177173
  @summary The maximized state shouldn't be reset upon hiding a frame
  @author anthony.petrov@oracle.com: area=awt.toplevel
  @run main HideMaximized
*/

import java.awt.*;

public class HideMaximized {
    public static void main(String[] args) {
        if (!Toolkit.getDefaultToolkit().isFrameStateSupported(Frame.MAXIMIZED_BOTH)) {
            // Nothing to test
            return;
        }

        // First test a decorated frame
        Frame frame = new Frame("test");
        test(frame);

        // Now test an undecorated frames
        frame = new Frame("undecorated test");
        frame.setUndecorated(true);
        test(frame);
    }

    private static void test(Frame frame) {
        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.setVisible(true);

        try { Thread.sleep(1000); } catch (Exception ex) {}

        if (frame.getExtendedState() != Frame.MAXIMIZED_BOTH) {
            throw new RuntimeException("The maximized state has not been applied");
        }

        // This will hide the frame, and also clean things up for safe exiting
        frame.dispose();

        try { Thread.sleep(1000); } catch (Exception ex) {}

        if (frame.getExtendedState() != Frame.MAXIMIZED_BOTH) {
            throw new RuntimeException("The maximized state has been reset");
        }
    }
}
