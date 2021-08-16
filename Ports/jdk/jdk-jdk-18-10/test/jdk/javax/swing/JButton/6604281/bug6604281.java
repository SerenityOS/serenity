/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 6604281
   @summary NimbusL&F :Regression in Focus traversal in JFileChooser in pit build
   @author Pavel Porvatov
   @run main bug6604281
*/

import javax.swing.*;
import javax.swing.plaf.IconUIResource;
import javax.swing.plaf.synth.SynthLookAndFeel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;

public class bug6604281 {
    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                SynthLookAndFeel laf = new SynthLookAndFeel();
                try {
                    UIManager.setLookAndFeel(laf);
                } catch (Exception e) {
                    fail(e.getMessage());
                }

                // Prepare image
                BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);

                Graphics2D graphics = (Graphics2D) image.getGraphics();

                graphics.setColor(Color.BLUE);
                graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
                graphics.setColor(Color.RED);
                graphics.drawLine(0, 0, image.getWidth(), image.getHeight());

                // Use IconUIResource as an icon, because with ImageIcon bug is not reproduced
                JButton button1 = new JButton(new IconUIResource(new ImageIcon(image)));

                JButton button2 = new JButton(new IconUIResource(new ImageIcon(image)));

                button2.setEnabled(false);

                if (button1.getPreferredSize().getHeight() != button2.getPreferredSize().getHeight()) {
                    fail("Two similar buttons have different size");
                }
            }
        });
    }

    private static void fail(String s) {
        throw new RuntimeException("Test failed: " + s);
    }
}
