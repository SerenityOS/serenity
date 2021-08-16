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
package java2d.demos.Colors;


import static java.awt.Color.black;
import static java.awt.Color.blue;
import static java.awt.Color.cyan;
import static java.awt.Color.green;
import static java.awt.Color.magenta;
import static java.awt.Color.orange;
import static java.awt.Color.pink;
import static java.awt.Color.red;
import static java.awt.Color.white;
import static java.awt.Color.yellow;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.color.ColorSpace;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java2d.Surface;


/**
 * ColorConvertOp a ColorSpace.TYPE_RGB BufferedImage to a ColorSpace.CS_GRAY
 * BufferedImage.
 */
@SuppressWarnings("serial")
public class ColorConvert extends Surface {

    private static Image img;
    private static Color[] colors = { red, pink, orange,
        yellow, green, magenta, cyan, blue };

    public ColorConvert() {
        setBackground(white);
        img = getImage("clouds.jpg");
    }

    @Override
    public void render(int w, int h, Graphics2D g2) {

        int iw = img.getWidth(this);
        int ih = img.getHeight(this);
        FontRenderContext frc = g2.getFontRenderContext();
        Font font = g2.getFont();
        g2.setColor(black);
        TextLayout tl = new TextLayout("ColorConvertOp RGB->GRAY", font, frc);
        tl.draw(g2, (float) (w / 2 - tl.getBounds().getWidth() / 2),
                tl.getAscent() + tl.getLeading());

        BufferedImage srcImg =
                new BufferedImage(iw, ih, BufferedImage.TYPE_INT_RGB);
        Graphics2D srcG = srcImg.createGraphics();
        RenderingHints rhs = g2.getRenderingHints();
        srcG.setRenderingHints(rhs);
        srcG.drawImage(img, 0, 0, null);

        String s = "OpenJDK";
        Font f = new Font(Font.SERIF, Font.BOLD, iw / 6);
        tl = new TextLayout(s, f, frc);
        Rectangle2D tlb = tl.getBounds();
        char[] chars = s.toCharArray();
        float charWidth = 0.0f;
        int rw = iw / chars.length;
        int rh = ih / chars.length;
        for (int i = 0; i < chars.length; i++) {
            tl = new TextLayout(String.valueOf(chars[i]), f, frc);
            Shape shape = tl.getOutline(null);
            srcG.setColor(colors[i % colors.length]);
            tl.draw(srcG, (float) (iw / 2 - tlb.getWidth() / 2 + charWidth),
                    (float) (ih / 2 + tlb.getHeight() / 2));
            charWidth += (float) shape.getBounds().getWidth();
            srcG.fillRect(i * rw, ih - rh, rw, rh);
            srcG.setColor(colors[colors.length - 1 - i % colors.length]);
            srcG.fillRect(i * rw, 0, rw, rh);
        }

        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorConvertOp theOp = new ColorConvertOp(cs, rhs);

        BufferedImage dstImg =
                new BufferedImage(iw, ih, BufferedImage.TYPE_INT_RGB);
        theOp.filter(srcImg, dstImg);

        g2.drawImage(srcImg, 10, 20, w / 2 - 20, h - 30, null);
        g2.drawImage(dstImg, w / 2 + 10, 20, w / 2 - 20, h - 30, null);
    }

    public static void main(String[] s) {
        createDemoFrame(new ColorConvert());
    }
}
