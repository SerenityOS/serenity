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
package java2d.demos.Fonts;


import static java.awt.Color.BLACK;
import static java.awt.Color.CYAN;
import static java.awt.Color.LIGHT_GRAY;
import static java.awt.Color.WHITE;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextHitInfo;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java2d.AnimatingSurface;


/**
 * Highlighting of text showing the caret, the highlight & the character
 * advances.
 */
@SuppressWarnings("serial")
public class Highlighting extends AnimatingSurface {

    private static String[] text = { "HIGHLIGHTING", "OpenJDK" };
    private static Color[] colors = { CYAN, LIGHT_GRAY };
    private static Font smallF = new Font("Monospaced", Font.PLAIN, 8);
    private int[] curPos;
    private TextLayout[] layouts;
    private Font[] fonts;

    public Highlighting() {
        setBackground(WHITE);
        fonts = new Font[2];
        layouts = new TextLayout[fonts.length];
        curPos = new int[fonts.length];
    }

    @Override
    public void reset(int w, int h) {
        fonts[0] = new Font("Monospaced", Font.PLAIN, w / text[0].length() + 8);
        fonts[1] = new Font("Serif", Font.BOLD, w / text[1].length());
        for (int i = 0; i < layouts.length; i++) {
            curPos[i] = 0;
        }
    }

    @Override
    public void step(int w, int h) {
        setSleepAmount(900);
        for (int i = 0; i < 2; i++) {
            if (layouts[i] == null) {
                continue;
            }
            if (curPos[i]++ == layouts[i].getCharacterCount()) {
                curPos[i] = 0;
            }
        }
    }

    @Override
    public void render(int w, int h, Graphics2D g2) {
        FontRenderContext frc = g2.getFontRenderContext();
        for (int i = 0; i < 2; i++) {
            layouts[i] = new TextLayout(text[i], fonts[i], frc);
            float rw = layouts[i].getAdvance();
            float rx = ((w - rw) / 2);
            float ry = ((i == 0) ? h / 3 : h * 0.75f);

            // draw highlighted shape
            Shape hilite = layouts[i].getLogicalHighlightShape(0, curPos[i]);
            AffineTransform at = AffineTransform.getTranslateInstance(rx, ry);
            hilite = at.createTransformedShape(hilite);
            float hy = (float) hilite.getBounds2D().getY();
            float hh = (float) hilite.getBounds2D().getHeight();
            g2.setColor(colors[i]);
            g2.fill(hilite);

            // get caret shape
            Shape[] shapes = layouts[i].getCaretShapes(curPos[i]);
            Shape caret = at.createTransformedShape(shapes[0]);

            g2.setColor(BLACK);
            layouts[i].draw(g2, rx, ry);
            g2.draw(caret);
            g2.draw(new Rectangle2D.Float(rx, hy, rw, hh));

            // Display character advances.
            for (int j = 0; j <= layouts[i].getCharacterCount(); j++) {
                float[] cInfo = layouts[i].getCaretInfo(TextHitInfo.leading(j));
                String str = String.valueOf((int) cInfo[0]);
                TextLayout tl = new TextLayout(str, smallF, frc);
                tl.draw(g2, rx + cInfo[0] - tl.getAdvance() / 2, hy + hh + tl.
                        getAscent() + 1.0f);
            }
        }
    }

    public static void main(String[] argv) {
        createDemoFrame(new Highlighting());
    }
}
