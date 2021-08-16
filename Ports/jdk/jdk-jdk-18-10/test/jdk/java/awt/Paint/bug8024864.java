/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8024864 8031422
 * @summary [macosx] Problems with rendering of controls
 * @author Petr Pchelko
 * @library ../regtesthelpers
 * @build Util
 * @run main bug8024864
 */

import javax.swing.*;
import java.awt.*;

import test.java.awt.regtesthelpers.Util;

public class bug8024864
{
    private static final int REPEATS = 30;

    private static volatile JFrame frame;

    private static void showTestUI() {
        frame = new JFrame();
        frame.setBackground(Color.green);
        JPanel p = new JPanel();
        p.setBackground(Color.red);
        JLabel l = new JLabel("Test!");
        p.add(l);
        frame.add(p);
        frame.pack();
        frame.setLocation(100,100);
        frame.setVisible(true);
    }

    public static void main(String[] args) throws Exception
    {
        Robot r = new Robot();
        for (int i = 0; i < REPEATS; i++) {
            try {
                SwingUtilities.invokeAndWait(bug8024864::showTestUI);
                //Thread.sleep(100);
                Util.waitTillShown(frame);
                Util.waitForIdle(r);

                Dimension frameSize = frame.getSize();
                Point loc = new Point(frameSize.width - 15, frameSize.height - 15);
                SwingUtilities.convertPointToScreen(loc, frame);
                Color c = r.getPixelColor(loc.x, loc.y);

                if (c.getGreen() > 200) {
                    throw new RuntimeException("TEST FAILED. Unexpected pixel color " + c);
                }

            } finally {
                if (frame != null) {
                    frame.dispose();
                }
                Util.waitForIdle(r);
            }
        }
    }
}
