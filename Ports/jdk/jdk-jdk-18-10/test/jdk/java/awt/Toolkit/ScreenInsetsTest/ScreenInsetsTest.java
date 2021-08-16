/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @bug 4737732
  @summary Tests that Toolkit.getScreenInsets() returns correct insets
  @author artem.ananiev: area=awt.toplevel
  @library ../../regtesthelpers
  @build Util
  @run main ScreenInsetsTest
*/

import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;

import test.java.awt.regtesthelpers.Util;

public class ScreenInsetsTest
{
    public static void main(String[] args)
    {
        boolean passed = true;

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gds = ge.getScreenDevices();
        for (GraphicsDevice gd : gds) {

            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            Rectangle gcBounds = gc.getBounds();
            Insets gcInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            int left = gcInsets.left;
            int right = gcInsets.right;
            int bottom = gcInsets.bottom;
            int top = gcInsets.top;
            if (left < 0 || right < 0 || bottom < 0 || top < 0) {
                throw new RuntimeException("Negative value: " + gcInsets);
            }
            int maxW = gcBounds.width / 3;
            int maxH = gcBounds.height / 3;
            if (left > maxW || right > maxW || bottom > maxH || top > maxH) {
                throw new RuntimeException("Big value: " + gcInsets);
            }

            if (!Toolkit.getDefaultToolkit().isFrameStateSupported(Frame.MAXIMIZED_BOTH))
            {
                // this state is used in the test - sorry
                continue;
            }

            Frame f = new Frame("Test", gc);
            f.setUndecorated(true);
            f.setBounds(gcBounds.x + 100, gcBounds.y + 100, 320, 240);
            f.setVisible(true);
            Util.waitForIdle(null);

            f.setExtendedState(Frame.MAXIMIZED_BOTH);
            Util.waitForIdle(null);

            Rectangle fBounds = f.getBounds();
            // workaround: on Windows maximized windows have negative coordinates
            if (fBounds.x < gcBounds.x)
            {
                fBounds.width -= (gcBounds.x - fBounds.x) * 2; // width is decreased
                fBounds.x = gcBounds.x;
            }
            if (fBounds.y < gcBounds.y)
            {
                fBounds.height -= (gcBounds.y - fBounds.y) * 2; // height is decreased
                fBounds.y = gcBounds.y;
            }
            Insets expected = new Insets(fBounds.y - gcBounds.y,
                                         fBounds.x - gcBounds.x,
                                         gcBounds.y + gcBounds.height - fBounds.y - fBounds.height,
                                         gcBounds.x + gcBounds.width - fBounds.x - fBounds.width);

            if (!expected.equals(gcInsets))
            {
                passed = false;
                System.err.println("Wrong insets for GraphicsConfig: " + gc);
                System.err.println("\tExpected: " + expected);
                System.err.println("\tActual: " + gcInsets);
            }

            f.dispose();
        }

        if (!passed)
        {
            throw new RuntimeException("TEST FAILED: Toolkit.getScreenInsets() returns wrong value for some screens");
        }
    }
}
