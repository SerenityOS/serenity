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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.SEG_CLOSE;
import static java.awt.geom.PathIterator.SEG_CUBICTO;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;
import static java.awt.geom.PathIterator.SEG_QUADTO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * @test
 * @bug 8144718
 * @summary Check the Stroker.drawBezApproxForArc() bug (stoke with round
 * joins): if cosext2 > 0.5, it generates curves with NaN coordinates
 * @run main TextClipErrorTest
 */
public class TextClipErrorTest {

    static final boolean SAVE_IMAGE = false;
    static final boolean SERIALIZE = false;

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);

        // initialize j.u.l Looger:
        final Logger log = Logger.getLogger("sun.java2d.marlin");
        log.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                Throwable th = record.getThrown();
                // detect any Throwable:
                if (th != null) {
                    System.out.println("Test failed:\n" + record.getMessage());
                    th.printStackTrace(System.out);

                    throw new RuntimeException("Test failed: ", th);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        });

        log.info("TextClipErrorTest: start");

        // enable Marlin logging & internal checks:
        System.setProperty("sun.java2d.renderer.log", "true");
        System.setProperty("sun.java2d.renderer.useLogger", "true");
        System.setProperty("sun.java2d.renderer.doChecks", "true");

        BufferedImage image = new BufferedImage(256, 256,
                                                BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.red);
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);

            Font font = g2d.getFont();
            FontRenderContext frc = new FontRenderContext(
                new AffineTransform(), true, true);

            g2d.setStroke(new BasicStroke(4.0f,
                                          BasicStroke.CAP_ROUND,
                                          BasicStroke.JOIN_ROUND));

            final Shape badShape;
            if (SERIALIZE) {
                final GlyphVector gv1 = font.createGlyphVector(frc, "\u00d6");
                final Shape textShape = gv1.getOutline();

                final AffineTransform at1 = AffineTransform.getTranslateInstance(
                    -2091202.554154681, 5548.601436981691);
                badShape = at1.createTransformedShape(textShape);
                serializeShape(badShape);
            } else {
                badShape = deserializeShape();
            }

            g2d.draw(badShape);

            // Draw anything within bounds and it fails:
            g2d.draw(new Line2D.Double(10, 20, 30, 40));

            if (SAVE_IMAGE) {
                final File file = new File("TextClipErrorTest.png");
                System.out.println("Writing file: " + file.getAbsolutePath());
                ImageIO.write(image, "PNG", file);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            g2d.dispose();
            log.info("TextClipErrorTest: end");
        }
    }

    private static void serializeShape(Shape shape) {
        final double[] coords = new double[6];

        final int len = 32;
        final ArrayList<Integer> typeList = new ArrayList<Integer>(len);
        final ArrayList<double[]> coordsList = new ArrayList<double[]>(len);

        for (PathIterator pi = shape.getPathIterator(null);
                !pi.isDone(); pi.next())
        {
            switch (pi.currentSegment(coords)) {
                case SEG_MOVETO:
                    typeList.add(SEG_MOVETO);
                    coordsList.add(Arrays.copyOf(coords, 2));
                    break;
                case SEG_LINETO:
                    typeList.add(SEG_LINETO);
                    coordsList.add(Arrays.copyOf(coords, 2));
                    break;
                case SEG_QUADTO:
                    typeList.add(SEG_QUADTO);
                    coordsList.add(Arrays.copyOf(coords, 4));
                    break;
                case SEG_CUBICTO:
                    typeList.add(SEG_CUBICTO);
                    coordsList.add(Arrays.copyOf(coords, 6));
                    break;
                case SEG_CLOSE:
                    typeList.add(SEG_CLOSE);
                    coordsList.add(null);
                    break;
                default:
            }
        }

        final StringBuilder sb = new StringBuilder(1024);
        // types:
        sb.append("private static final int[] SHAPE_TYPES = new int[]{\n");
        for (Integer i : typeList) {
            sb.append(i).append(",\n");
        }
        sb.append("};\n");

        // coords:
        sb.append("private static final double[][] SHAPE_COORDS = new double[][]{\n");
        for (double[] c : coordsList) {
            if (c == null) {
                sb.append("null,\n");
            } else {
                sb.append("new double[]{");
                for (int i = 0; i < c.length; i++) {
                    sb.append(c[i]).append(",");
                }
                sb.append("},\n");
            }
        }
        sb.append("};\n");

        System.out.println("Shape size: " + typeList.size());
        System.out.println("Serialized shape:\n" + sb.toString());
    }

    private static Shape deserializeShape() {
        final Path2D.Double path = new Path2D.Double();

        for (int i = 0; i < SHAPE_TYPES.length; i++) {
            double[] coords = SHAPE_COORDS[i];

            switch (SHAPE_TYPES[i]) {
                case SEG_MOVETO:
                    path.moveTo(coords[0], coords[1]);
                    break;
                case SEG_LINETO:
                    path.lineTo(coords[0], coords[1]);
                    break;
                case SEG_QUADTO:
                    path.quadTo(coords[0], coords[1],
                                coords[2], coords[3]);
                    break;
                case SEG_CUBICTO:
                    path.curveTo(coords[0], coords[1],
                                 coords[2], coords[3],
                                 coords[4], coords[5]);
                    break;
                case SEG_CLOSE:
                    path.closePath();
                    break;
                default:
            }
        }

        return path;
    }

    // generated code:
    private static final int[] SHAPE_TYPES = new int[]{
        0,
        2,
        2,
        2,
        2,
        2,
        2,
        2,
        2,
        4,
        0,
        2,
        2,
        2,
        2,
        2,
        2,
        2,
        2,
        4,
        0,
        1,
        1,
        1,
        1,
        4,
        0,
        1,
        1,
        1,
        1,
        4,
    };

    private static final double[][] SHAPE_COORDS = new double[][]{
        new double[]{-2091197.819779681, 5540.648311981691,},
        new double[]{-2091199.116654681, 5540.648311981691, -2091199.874467181, 5541.609249481691,},
        new double[]{-2091200.632279681, 5542.570186981691, -2091200.632279681, 5544.242061981691,},
        new double[]{-2091200.632279681, 5545.882686981691, -2091199.874467181, 5546.843624481691,},
        new double[]{-2091199.116654681, 5547.804561981691, -2091197.819779681, 5547.804561981691,},
        new double[]{-2091196.538529681, 5547.804561981691, -2091195.780717181, 5546.843624481691,},
        new double[]{-2091195.022904681, 5545.882686981691, -2091195.022904681, 5544.242061981691,},
        new double[]{-2091195.022904681, 5542.570186981691, -2091195.780717181, 5541.609249481691,},
        new double[]{-2091196.538529681, 5540.648311981691, -2091197.819779681, 5540.648311981691,},
        null,
        new double[]{-2091197.819779681, 5539.695186981691,},
        new double[]{-2091195.991654681, 5539.695186981691, -2091194.890092181, 5540.929561981691,},
        new double[]{-2091193.788529681, 5542.163936981691, -2091193.788529681, 5544.242061981691,},
        new double[]{-2091193.788529681, 5546.304561981691, -2091194.890092181, 5547.538936981691,},
        new double[]{-2091195.991654681, 5548.773311981691, -2091197.819779681, 5548.773311981691,},
        new double[]{-2091199.663529681, 5548.773311981691, -2091200.772904681, 5547.538936981691,},
        new double[]{-2091201.882279681, 5546.304561981691, -2091201.882279681, 5544.242061981691,},
        new double[]{-2091201.882279681, 5542.163936981691, -2091200.772904681, 5540.929561981691,},
        new double[]{-2091199.663529681, 5539.695186981691, -2091197.819779681, 5539.695186981691,},
        null,
        new double[]{-2091197.210404681, 5537.835811981691,},
        new double[]{-2091196.022904681, 5537.835811981691,},
        new double[]{-2091196.022904681, 5539.023311981691,},
        new double[]{-2091197.210404681, 5539.023311981691,},
        new double[]{-2091197.210404681, 5537.835811981691,},
        null,
        new double[]{-2091199.632279681, 5537.835811981691,},
        new double[]{-2091198.444779681, 5537.835811981691,},
        new double[]{-2091198.444779681, 5539.023311981691,},
        new double[]{-2091199.632279681, 5539.023311981691,},
        new double[]{-2091199.632279681, 5537.835811981691,},
        null,
    };

}
