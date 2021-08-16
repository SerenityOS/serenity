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
package java2d.demos.Paint;


import static java.awt.Color.BLACK;
import static java.awt.Color.GRAY;
import static java.awt.Color.GREEN;
import static java.awt.Color.LIGHT_GRAY;
import static java.awt.Color.WHITE;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java2d.Surface;


/**
 * TexturePaint of gradient, buffered image and shapes.
 */
@SuppressWarnings("serial")
public class Texture extends Surface {

    private static TexturePaint bluedots, greendots, triangles;
    private static TexturePaint blacklines, gradient;

    static {
        BufferedImage bi = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D gi = bi.createGraphics();
        gi.setBackground(WHITE);
        gi.clearRect(0, 0, 10, 10);
        GeneralPath p1 = new GeneralPath();
        p1.moveTo(0, 0);
        p1.lineTo(5, 10);
        p1.lineTo(10, 0);
        p1.closePath();
        gi.setColor(LIGHT_GRAY);
        gi.fill(p1);
        triangles = new TexturePaint(bi, new Rectangle(0, 0, 10, 10));

        bi = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
        gi = bi.createGraphics();
        gi.setColor(BLACK);
        gi.fillRect(0, 0, 5, 5);
        gi.setColor(GRAY);
        gi.fillRect(1, 1, 4, 4);
        blacklines = new TexturePaint(bi, new Rectangle(0, 0, 5, 5));

        int w = 30;
        int h = 30;
        bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        gi = bi.createGraphics();
        Color oc = WHITE;
        Color ic = LIGHT_GRAY;
        gi.setPaint(new GradientPaint(0, 0, oc, w * .35f, h * .35f, ic));
        gi.fillRect(0, 0, w / 2, h / 2);
        gi.setPaint(new GradientPaint(w, 0, oc, w * .65f, h * .35f, ic));
        gi.fillRect(w / 2, 0, w / 2, h / 2);
        gi.setPaint(new GradientPaint(0, h, oc, w * .35f, h * .65f, ic));
        gi.fillRect(0, h / 2, w / 2, h / 2);
        gi.setPaint(new GradientPaint(w, h, oc, w * .65f, h * .65f, ic));
        gi.fillRect(w / 2, h / 2, w / 2, h / 2);
        gradient = new TexturePaint(bi, new Rectangle(0, 0, w, h));

        bi = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        bi.setRGB(0, 0, 0xffffffff);
        bi.setRGB(1, 0, 0xffffffff);
        bi.setRGB(0, 1, 0xffffffff);
        bi.setRGB(1, 1, 0xff0000ff);
        bluedots = new TexturePaint(bi, new Rectangle(0, 0, 2, 2));

        bi = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        bi.setRGB(0, 0, 0xffffffff);
        bi.setRGB(1, 0, 0xffffffff);
        bi.setRGB(0, 1, 0xffffffff);
        bi.setRGB(1, 1, 0xff00ff00);
        greendots = new TexturePaint(bi, new Rectangle(0, 0, 2, 2));
    }

    public Texture() {
        setBackground(WHITE);
    }

    @Override
    public void render(int w, int h, Graphics2D g2) {

        Rectangle r = new Rectangle(10, 10, w - 20, h / 2 - 20);
        g2.setPaint(gradient);
        g2.fill(r);
        g2.setPaint(GREEN);
        g2.setStroke(new BasicStroke(20));
        g2.draw(r);
        g2.setPaint(blacklines);
        g2.setStroke(new BasicStroke(15));
        g2.draw(r);

        Font f = new Font(Font.SERIF, Font.BOLD, w / 5);
        TextLayout tl = new TextLayout("Texture", f, g2.getFontRenderContext());
        int sw = (int) tl.getBounds().getWidth();
        int sh = (int) tl.getBounds().getHeight();
        Shape sha = tl.getOutline(AffineTransform.getTranslateInstance(w / 2 - sw
                / 2, h * .25 + sh / 2));
        g2.setColor(BLACK);
        g2.setStroke(new BasicStroke(3));
        g2.draw(sha);
        g2.setPaint(greendots);
        g2.fill(sha);

        r.setLocation(10, h / 2 + 10);
        g2.setPaint(triangles);
        g2.fill(r);
        g2.setPaint(blacklines);
        g2.setStroke(new BasicStroke(20));
        g2.draw(r);
        g2.setPaint(GREEN);
        g2.setStroke(new BasicStroke(4));
        g2.draw(r);

        f = new Font(Font.SERIF, Font.BOLD, w / 4);
        tl = new TextLayout("Paint", f, g2.getFontRenderContext());
        sw = (int) tl.getBounds().getWidth();
        sh = (int) tl.getBounds().getHeight();
        sha = tl.getOutline(AffineTransform.getTranslateInstance(w / 2 - sw / 2, h
                * .75 + sh / 2));
        g2.setColor(BLACK);
        g2.setStroke(new BasicStroke(5));
        g2.draw(sha);
        g2.setPaint(bluedots);
        g2.fill(sha);
    }

    public static void main(String[] s) {
        createDemoFrame(new Texture());
    }
}
