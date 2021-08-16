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

/* @test
   @bug 7142955
   @summary DefaultTreeCellRenderer doesn't honor 'Tree.rendererFillBackground' LAF property
   @author Pavel Porvatov
*/

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;

public class bug7142955 {
    private static final Color TEST_COLOR = Color.RED;

    public static void main(String[] args) throws Exception {
        UIManager.put("Tree.rendererFillBackground", Boolean.FALSE);
        UIManager.put("Tree.textBackground", TEST_COLOR);

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                int w = 200;
                int h = 100;

                BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

                Graphics g = image.getGraphics();

                g.setColor(Color.WHITE);
                g.fillRect(0, 0, image.getWidth(), image.getHeight());

                DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();

                renderer.setSize(w, h);
                renderer.paint(g);

                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        if (image.getRGB(x, y) == TEST_COLOR.getRGB()) {
                            throw new RuntimeException("Test bug7142955 failed");
                        }
                    }
                }

                System.out.println("Test bug7142955 passed.");
            }
        });
    }
}
