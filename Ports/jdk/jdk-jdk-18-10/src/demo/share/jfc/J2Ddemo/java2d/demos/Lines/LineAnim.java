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
package java2d.demos.Lines;


import static java.awt.Color.BLACK;
import static java.awt.Color.GRAY;
import static java.awt.Color.LIGHT_GRAY;
import static java.awt.Color.PINK;
import static java.awt.Color.WHITE;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java2d.AnimatingSurface;


/**
 * Lines & Paths animation illustrating BasicStroke attributes.
 */
@SuppressWarnings("serial")
public class LineAnim extends AnimatingSurface {

    private static int[] caps = { BasicStroke.CAP_BUTT,
        BasicStroke.CAP_SQUARE, BasicStroke.CAP_ROUND };
    private static int[] joins = { BasicStroke.JOIN_MITER,
        BasicStroke.JOIN_BEVEL, BasicStroke.JOIN_ROUND };
    private static Color[] colors = { GRAY, PINK, LIGHT_GRAY };
    private static BasicStroke bs1 = new BasicStroke(1.0f);
    private static final int CLOCKWISE = 0;
    private Line2D[] lines = new Line2D[3];
    private int[] rAmt = new int[lines.length];
    private int[] direction = new int[lines.length];
    private int[] speed = new int[lines.length];
    private BasicStroke[] strokes = new BasicStroke[lines.length];
    private GeneralPath path;
    private Point2D[] pts;
    private float size;
    private Ellipse2D ellipse = new Ellipse2D.Double();

    public LineAnim() {
        setBackground(WHITE);
    }

    @Override
    public void reset(int w, int h) {
        size = (w > h) ? h / 6f : w / 6f;
        for (int i = 0; i < lines.length; i++) {
            lines[i] = new Line2D.Float(0, 0, size, 0);
            strokes[i] = new BasicStroke(size / 3, caps[i], joins[i]);
            rAmt[i] = i * 360 / lines.length;
            direction[i] = i % 2;
            speed[i] = i + 1;
        }

        path = new GeneralPath();
        path.moveTo(size, -size / 2);
        path.lineTo(size + size / 2, 0);
        path.lineTo(size, +size / 2);

        ellipse.setFrame(w / 2 - size * 2 - 4.5f, h / 2 - size * 2 - 4.5f, size
                * 4, size * 4);
        PathIterator pi = ellipse.getPathIterator(null, 0.9);
        Point2D[] points = new Point2D[100];
        int num_pts = 0;
        while (!pi.isDone()) {
            float[] pt = new float[6];
            switch (pi.currentSegment(pt)) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    points[num_pts] = new Point2D.Float(pt[0], pt[1]);
                    num_pts++;
            }
            pi.next();
        }
        pts = new Point2D[num_pts];
        System.arraycopy(points, 0, pts, 0, num_pts);
    }

    @Override
    public void step(int w, int h) {
        for (int i = 0; i < lines.length; i++) {
            if (direction[i] == CLOCKWISE) {
                rAmt[i] += speed[i];
                if (rAmt[i] == 360) {
                    rAmt[i] = 0;
                }
            } else {
                rAmt[i] -= speed[i];
                if (rAmt[i] == 0) {
                    rAmt[i] = 360;
                }
            }
        }
    }

    @Override
    public void render(int w, int h, Graphics2D g2) {

        ellipse.setFrame(w / 2 - size, h / 2 - size, size * 2, size * 2);
        g2.setColor(BLACK);
        g2.draw(ellipse);

        for (int i = 0; i < lines.length; i++) {
            AffineTransform at = AffineTransform.getTranslateInstance(w / 2, h
                    / 2);
            at.rotate(Math.toRadians(rAmt[i]));
            g2.setStroke(strokes[i]);
            g2.setColor(colors[i]);
            g2.draw(at.createTransformedShape(lines[i]));
            g2.draw(at.createTransformedShape(path));

            int j = (int) ((double) rAmt[i] / 360 * pts.length);
            j = (j == pts.length) ? pts.length - 1 : j;
            ellipse.setFrame(pts[j].getX(), pts[j].getY(), 9, 9);
            g2.fill(ellipse);
        }

        g2.setStroke(bs1);
        g2.setColor(BLACK);
        for (int i = 0; i < pts.length; i++) {
            ellipse.setFrame(pts[i].getX(), pts[i].getY(), 9, 9);
            g2.draw(ellipse);
        }
    }

    public static void main(String[] argv) {
        createDemoFrame(new LineAnim());
    }
}
