/*
 *
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package java2d.demos.Arcs_Curves;


import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.PathIterator;
import java.awt.geom.FlatteningPathIterator;
import java.awt.font.TextLayout;
import java.awt.font.FontRenderContext;
import java2d.Surface;
import static java.awt.Color.*;
import static java.awt.geom.PathIterator.*;


/**
 * CubicCurve2D & QuadCurve2D curves includes FlattenPathIterator example.
 */
@SuppressWarnings("serial")
public class Curves extends Surface {

    private static Color[] colors = { BLUE, GREEN, RED };

    public Curves() {
        setBackground(WHITE);
    }

    @Override
    public void render(int w, int h, Graphics2D g2) {

        g2.setColor(BLACK);
        FontRenderContext frc = g2.getFontRenderContext();
        TextLayout tl = new TextLayout("QuadCurve2D", g2.getFont(), frc);
        float xx = (float) (w * .5 - tl.getBounds().getWidth() / 2);
        tl.draw(g2, xx, tl.getAscent());

        tl = new TextLayout("CubicCurve2D", g2.getFont(), frc);
        xx = (float) (w * .5 - tl.getBounds().getWidth() / 2);
        tl.draw(g2, xx, h * .5f);
        g2.setStroke(new BasicStroke(5.0f));

        float yy = 20;

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                Shape shape = null;

                if (i == 0) {
                    shape = new QuadCurve2D.Float(w * .1f, yy, w * .5f, 50, w
                            * .9f, yy);
                } else {
                    shape = new CubicCurve2D.Float(w * .1f, yy, w * .4f, yy - 15,
                            w * .6f, yy + 15, w * .9f, yy);
                }
                g2.setColor(colors[j]);
                if (j != 2) {
                    g2.draw(shape);
                }

                if (j == 1) {
                    g2.setColor(LIGHT_GRAY);
                    PathIterator f = shape.getPathIterator(null);
                    while (!f.isDone()) {
                        float[] pts = new float[6];
                        switch (f.currentSegment(pts)) {
                            case SEG_MOVETO:
                            case SEG_LINETO:
                                g2.fill(new Rectangle2D.Float(pts[0], pts[1], 5,
                                        5));
                                break;
                            case SEG_CUBICTO:
                            case SEG_QUADTO:
                                g2.fill(new Rectangle2D.Float(pts[0], pts[1], 5,
                                        5));
                                if (pts[2] != 0) {
                                    g2.fill(new Rectangle2D.Float(pts[2], pts[3],
                                            5, 5));
                                }
                                if (pts[4] != 0) {
                                    g2.fill(new Rectangle2D.Float(pts[4], pts[5],
                                            5, 5));
                                }
                        }
                        f.next();
                    }
                } else if (j == 2) {
                    PathIterator p = shape.getPathIterator(null);
                    FlatteningPathIterator f =
                            new FlatteningPathIterator(p, 0.1);
                    while (!f.isDone()) {
                        float[] pts = new float[6];
                        switch (f.currentSegment(pts)) {
                            case SEG_MOVETO:
                            case SEG_LINETO:
                                g2.fill(new Ellipse2D.Float(pts[0], pts[1], 3, 3));
                        }
                        f.next();
                    }
                }
                yy += h / 6;
            }
            yy = h / 2 + 15;
        }
    }

    public static void main(String[] argv) {
        createDemoFrame(new Curves());
    }
}
