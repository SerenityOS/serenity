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
package java2d.demos.Images;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java2d.AnimatingSurface;


/**
 * Warps a image on a CubicCurve2D flattened path.
 */
@SuppressWarnings("serial")
public class WarpImage extends AnimatingSurface {

    private static int iw, ih, iw2, ih2;
    private static Image img;
    private static final int FORWARD = 0;
    private static final int BACK = 1;
    private Point2D[] pts;
    private int direction = FORWARD;
    private int pNum;
    private int x, y;

    @SuppressWarnings("LeakingThisInConstructor")
    public WarpImage() {
        setBackground(Color.white);
        img = getImage("surfing.png");
        iw = img.getWidth(this);
        ih = img.getHeight(this);
        iw2 = iw / 2;
        ih2 = ih / 2;
    }

    @Override
    public void reset(int w, int h) {
        pNum = 0;
        direction = FORWARD;
        CubicCurve2D cc = new CubicCurve2D.Float(
                w * .2f, h * .5f, w * .4f, 0, w * .6f, h, w * .8f, h * .5f);
        PathIterator pi = cc.getPathIterator(null, 0.1);
        Point2D[] tmp = new Point2D[200];
        int i = 0;
        while (!pi.isDone()) {
            float[] coords = new float[6];
            switch (pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    tmp[i] = new Point2D.Float(coords[0], coords[1]);
            }
            i++;
            pi.next();
        }
        pts = new Point2D[i];
        System.arraycopy(tmp, 0, pts, 0, i);
    }

    @Override
    public void step(int w, int h) {
        if (pts == null) {
            return;
        }
        x = (int) pts[pNum].getX();
        y = (int) pts[pNum].getY();
        if (direction == FORWARD) {
            if (++pNum == pts.length) {
                direction = BACK;
            }
        }
        if (direction == BACK) {
            if (--pNum == 0) {
                direction = FORWARD;
            }
        }
    }

    @Override
    public void render(int w, int h, Graphics2D g2) {
        g2.drawImage(img,
                0, 0, x, y,
                0, 0, iw2, ih2,
                this);
        g2.drawImage(img,
                x, 0, w, y,
                iw2, 0, iw, ih2,
                this);
        g2.drawImage(img,
                0, y, x, h,
                0, ih2, iw2, ih,
                this);
        g2.drawImage(img,
                x, y, w, h,
                iw2, ih2, iw, ih,
                this);
    }

    public static void main(String[] argv) {
        createDemoFrame(new WarpImage());
    }
}
