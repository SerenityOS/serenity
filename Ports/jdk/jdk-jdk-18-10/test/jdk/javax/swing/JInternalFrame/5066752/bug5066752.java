/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
  @bug 5066752
  @summary  Transparent JDesktopPane impossible because isOpaque() returns true
  @author mb50250: area=JDesktopPane
  @library ../../regtesthelpers
  @build Util
  @run main bug5066752
*/

import java.awt.*;
import javax.swing.*;

public class bug5066752
{
    private static JFrame frame;

    public static void main(String[] args) throws Exception {
        try {
            Robot r = new Robot();
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    createAndShowGUI();
                }
            });
            r.waitForIdle();

            r.delay(600);

            Point p = Util.getCenterPoint(frame);
            Color color =  r.getPixelColor((int) p.getX(), (int) p.getY());
            if (!color.equals(Color.RED)) {
                throw new Exception("Test failed: JDesktopPane isn't transparent. Expected color is (red color): " + Color.RED + ", actual color is: " + color);
            }
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    private static void createAndShowGUI() {
        frame = new JFrame();

        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(Color.RED);
        frame.add(panel, BorderLayout.CENTER);

        JDesktopPane dp = new JDesktopPane();
        dp.setOpaque(false);
        panel.add(dp, BorderLayout.CENTER);

        frame.setBounds(200, 200, 300, 200);
        frame.setVisible(true);
    }
}
