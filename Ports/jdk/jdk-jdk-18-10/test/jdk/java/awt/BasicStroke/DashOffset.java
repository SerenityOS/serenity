/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;

/*
 * @test
 * @bug 4469881 8217263 8218682
 * @key headful
 * @summary Verifies that dashed rectangles drawn to the screen line
 *          up with their undashed counterparts
 * @author flar
 * @run main/othervm -Dsun.java2d.uiScale=1 DashOffset
 */
public class DashOffset {

    private static final BasicStroke dash =
            new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_MITER, 10.0f,
                            new float[] {2.0f, 2.0f}, 0.0f);

    private static final Color COLOR1 = Color.BLUE;
    private static final Color COLOR2 = Color.GREEN;

    private static final Color BACKGROUND = Color.WHITE;

    private static final int WIDTH = 20;
    private static final int HEIGHT = 20;

    private static final int OFFSET = 2;

    private static final int MAX_DASH_LENGTH = 3;

    public static void main(String[] argv) throws Exception {
        final boolean saveImage = argv.length > 0 && "-save".equals(argv[0]);

        final BufferedImage img = new BufferedImage(WIDTH, HEIGHT,
                                                    TYPE_INT_RGB);
        try {
            draw(img);
            validate(img);
        } finally {
            if (saveImage) {
                save(img, "bufferedImage.png");
            }
        }

        BufferedImage snapshot = null;
        try {
            final GraphicsConfiguration gc =
                    GraphicsEnvironment.getLocalGraphicsEnvironment()
                                       .getDefaultScreenDevice()
                                       .getDefaultConfiguration();
            if (gc.getColorModel() instanceof IndexColorModel) {
                System.err.println("Skipping VolatileImage because of IndexColorModel");
                return;
            }

            VolatileImage vi = gc.createCompatibleVolatileImage(WIDTH, HEIGHT);
            int attempt = 0;
            do {
                vi.validate(gc);
                draw(vi);
                snapshot = vi.getSnapshot();
            } while (vi.contentsLost() && ++attempt <= 10);
            if (attempt > 10) {
                throw new RuntimeException("Too many attempts: " + attempt);
            }
            validate(snapshot);
        } finally {
            if (saveImage && snapshot != null) {
                save(snapshot, "volatileImage.png");
            }
        }
    }

    private static void draw(final Image img) {
        Graphics g = img.getGraphics();
        g.setColor(BACKGROUND);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(COLOR1);
        g.drawRect(OFFSET, OFFSET, WIDTH - OFFSET * 2, HEIGHT - OFFSET * 2);
        g.setColor(COLOR2);
        g.clipRect(OFFSET, OFFSET, WIDTH - OFFSET * 2 + 1, HEIGHT - OFFSET * 2 + 1);
        ((Graphics2D) g).setStroke(dash);
        g.drawRect(OFFSET, OFFSET, WIDTH - OFFSET * 2, HEIGHT - OFFSET * 2);
        g.dispose();
    }

    private static void validate(final BufferedImage img) {
        checkHorizontalLine(img, OFFSET);
        checkHorizontalLine(img, HEIGHT - OFFSET);
        checkVerticalLine(img, OFFSET);
        checkVerticalLine(img, WIDTH - OFFSET);
        checkCorners(img);
    }

    private static void checkHorizontalLine(final BufferedImage img,
                                            final int y) {
        int prev = img.getRGB(OFFSET, y);
        int curr;
        int count = 1;
        checkColor(OFFSET, y, prev, COLOR1, COLOR2);
        for (int x = OFFSET + 1; x <= WIDTH - OFFSET; x++) {
            curr = img.getRGB(x, y);
            if (curr != prev) {
                checkColor(x, y, curr, COLOR1, COLOR2);
                checkCount(x, y, count);
                prev = curr;
                count = 1;
            } else {
                count++;
            }
            if (x < WIDTH - OFFSET) {
                checkColor(x, y - 1, img.getRGB(x, y - 1), BACKGROUND);
                checkColor(x, y + 1, img.getRGB(x, y + 1), BACKGROUND);
            }
        }
        checkCount(WIDTH - OFFSET, y, count);
    }

    private static void checkVerticalLine(final BufferedImage img,
                                          final int x) {
        int prev = img.getRGB(x, OFFSET);
        checkColor(x, OFFSET, prev, COLOR1, COLOR2);
        int count = 1;
        for (int y = OFFSET + 1; y <= HEIGHT - OFFSET; y++) {
            int curr = img.getRGB(x, y);
            if (curr != prev) {
                checkColor(x, y, curr, COLOR1, COLOR2);
                checkCount(x, y, count);
                prev = curr;
                count = 1;
            } else {
                count++;
            }
            if (y < HEIGHT - OFFSET) {
                checkColor(x - 1, y, img.getRGB(x - 1, y), BACKGROUND);
                checkColor(x + 1, y, img.getRGB(x + 1, y), BACKGROUND);
            }
        }
        checkCount(x, HEIGHT - OFFSET, count);
    }

    private static void checkCorners(final BufferedImage img) {
        int[][] corners = {
                {OFFSET - 1, OFFSET - 1},
                {OFFSET,     OFFSET - 1},
                {OFFSET - 1, OFFSET + 1},

                {OFFSET - 1, HEIGHT - OFFSET},
                {OFFSET - 1, HEIGHT - OFFSET + 1},
                {OFFSET,     HEIGHT - OFFSET + 1},

                {WIDTH - OFFSET,     OFFSET - 1},
                {WIDTH - OFFSET + 1, OFFSET - 1},
                {WIDTH - OFFSET + 1, OFFSET},

                {WIDTH - OFFSET + 1, HEIGHT - OFFSET},
                {WIDTH - OFFSET + 1, HEIGHT - OFFSET + 1},
                {WIDTH - OFFSET,     HEIGHT - OFFSET + 1},
        };

        for (int[] corner : corners) {
            int color = img.getRGB(corner[0], corner[1]);
            checkColor(corner[0], corner[1], color, BACKGROUND);
        }
    }

    private static void checkColor(final int x, final int y,
                                   final int color,
                                   final Color... validColors) {
        checkColor(x, y, color, Arrays.stream(validColors)
                                      .mapToInt(Color::getRGB)
                                      .toArray());
    }

    private static void checkColor(final int x, final int y,
                                   final int color,
                                   final int... validColors) {
        for (int valid : validColors) {
            if (color == valid) {
                return;
            }
        }
        throw new RuntimeException("Unexpected color at " + x + ", " + y
                + ": " + Integer.toHexString(color) + "; expected: "
                + Arrays.stream(validColors)
                        .mapToObj(Integer::toHexString)
                        .collect(Collectors.joining(", ")));
    }

    private static void checkCount(final int x, final int y, final int count) {
        if (count > MAX_DASH_LENGTH) {
            throw new RuntimeException("Dash is longer than " + MAX_DASH_LENGTH
                    + " at " + x + ", " + y);
        }
    }

    private static void save(final BufferedImage img,
                             final String fileName) throws IOException {
        ImageIO.write(img, "png", new File(fileName));
    }

}
