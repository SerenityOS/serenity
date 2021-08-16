/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, JetBrains s.r.o.. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 8267116
 * @summary  AlphaComposite for VolatileImage graphics
 * @author Alexey Ushakov
 * @run main AlphaCompositeTest
 */

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.IOException;

public class AlphaCompositeTest {
    public static void main(String[] args) throws IOException {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        VolatileImage vi = gc.createCompatibleVolatileImage(100, 100, Transparency.TRANSLUCENT);
        BufferedImage gold = gc.createCompatibleImage(100, 100, Transparency.TRANSLUCENT);
        render(gold.createGraphics());
        BufferedImage snapshot = null;

        Graphics2D g2 = vi.createGraphics();
        do {
            render(g2);
            snapshot = vi.getSnapshot();
        } while (vi.contentsLost());

        for (int x = 0; x < gold.getWidth(); ++x) {
            for (int y = 0; y < gold.getHeight(); ++y) {
                if (gold.getRGB(x, y) != snapshot.getRGB(x, y)) {
                    ImageIO.write(gold, "png", new File("gold.png"));
                    ImageIO.write(snapshot, "png", new File("bi.png"));
                    throw new RuntimeException("Test failed.");
                }
            }
        }
    }

    private static void render(Graphics2D g2) {
        g2.setColor(Color.BLUE);
        g2.fillRect(0, 0, 100, 100);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 0.5f));
        g2.setColor(Color.RED);
        g2.fillRect(10, 10, 80, 80);
    }
}
