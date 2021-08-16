/*
 * Copyright 2021 JetBrains s.r.o.
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
 * @bug 8263583
 * @summary Checks that emoji character has a non-empty and identical
 *          representation when rendered to different types of images,
 *          including an accelerated (OpenGL or Metal) surface.
 * @requires (os.family == "mac")
 * @run main/othervm -Dsun.java2d.uiScale=1 MacEmoji
 */

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.List;

public class MacEmoji {
    private static final int IMG_WIDTH = 20;
    private static final int IMG_HEIGHT = 20;

    public static void main(String[] args) {
        GraphicsConfiguration cfg
                = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();

        VolatileImage vImg = cfg.createCompatibleVolatileImage(IMG_WIDTH,
                                                               IMG_HEIGHT);
        BufferedImage refImg;
        int attempt = 0;
        do {
            if (++attempt > 10) {
                throw new RuntimeException("Failed to render to VolatileImage");
            }
            if (vImg.validate(cfg) == VolatileImage.IMAGE_INCOMPATIBLE) {
                throw new RuntimeException("Unexpected validation failure");
            }
            drawEmoji(vImg);
            refImg = vImg.getSnapshot();
        } while (vImg.contentsLost());

        boolean rendered = false;
        for (int x = 0; x < IMG_WIDTH; x++) {
            for (int y = 0; y < IMG_HEIGHT; y++) {
                if (refImg.getRGB(x, y) != 0xFFFFFFFF) {
                    rendered = true;
                    break;
                }
            }
        }
        if (!rendered) {
            throw new RuntimeException("Emoji character wasn't rendered");
        }

        List<Integer> imageTypes = List.of(
                BufferedImage.TYPE_INT_RGB,
                BufferedImage.TYPE_INT_ARGB,
                BufferedImage.TYPE_INT_ARGB_PRE,
                BufferedImage.TYPE_INT_BGR,
                BufferedImage.TYPE_3BYTE_BGR,
                BufferedImage.TYPE_4BYTE_ABGR,
                BufferedImage.TYPE_4BYTE_ABGR_PRE
        );
        for (Integer type : imageTypes) {
            BufferedImage img = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, type);
            drawEmoji(img);
            for (int x = 0; x < IMG_WIDTH; x++) {
                for (int y = 0; y < IMG_HEIGHT; y++) {
                    if (refImg.getRGB(x, y) != img.getRGB(x, y)) {
                        throw new RuntimeException(
                                "Rendering differs for image type " + type);
                    }
                }
            }
        }
    }

    private static void drawEmoji(Image img) {
        Graphics g = img.getGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);
        g.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        g.drawString("\uD83D\uDE00" /* U+1F600 'GRINNING FACE' */, 2, 15);
        g.dispose();
    }
}
