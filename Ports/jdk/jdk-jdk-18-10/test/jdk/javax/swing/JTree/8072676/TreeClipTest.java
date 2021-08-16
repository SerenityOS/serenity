/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/*
 * @test
 * @bug 8072676
 * @summary Checks if the tree painter doesn't expand existing clip
 * @author Anton Nashatyrev
 */
public class TreeClipTest {

    static boolean passed = true;

    static boolean checkImage(BufferedImage img, int clipY) {
        for (int y = clipY; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x,y) & 0xFFFFFF) != 0xFFFFFF) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                DefaultMutableTreeNode      root = new DefaultMutableTreeNode("JTree");
                DefaultMutableTreeNode      parent;

                parent = new DefaultMutableTreeNode("colors");
                root.add(parent);
                parent.add(new DefaultMutableTreeNode("blue"));
                DefaultTreeModel model = new DefaultTreeModel(root);
                JTree tree = new JTree(model);

                BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
                for (int clipY = 1; clipY < 50; clipY++) {
                    Graphics2D ig = img.createGraphics();
                    ig.setColor(Color.WHITE);
                    ig.fillRect(0,0,1000, 1000);
                    tree.setSize(200,200);
                    ig.setClip(0,0,1000,clipY);
                    tree.paint(ig);
                    ig.dispose();

                    if (!checkImage(img, clipY)) {
                        System.err.println("Failed with clipY=" + clipY);
                        passed = false;
                        try {
                            ImageIO.write(img, "PNG", new File("failedResult.png"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                }
            }
        });

        if (!passed) {
            throw new RuntimeException("Test failed.");
        } else {
            System.out.println("Passed.");
        }
    }
}
