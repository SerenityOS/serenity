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
package java2d.demos.Composite;


import static java.awt.AlphaComposite.Clear;
import static java.awt.AlphaComposite.Dst;
import static java.awt.AlphaComposite.DstAtop;
import static java.awt.AlphaComposite.DstIn;
import static java.awt.AlphaComposite.DstOut;
import static java.awt.AlphaComposite.DstOver;
import static java.awt.AlphaComposite.Src;
import static java.awt.AlphaComposite.SrcAtop;
import static java.awt.AlphaComposite.SrcIn;
import static java.awt.AlphaComposite.SrcOut;
import static java.awt.AlphaComposite.SrcOver;
import static java.awt.AlphaComposite.Xor;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.font.TextLayout;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java2d.AnimatingSurface;


/**
 * All the AlphaCompositing Rules demonstrated.
 */
@SuppressWarnings("serial")
public class ACrules extends AnimatingSurface {

    private static String[] compNames = {
        "Src",
        "SrcOver",
        "SrcIn",
        "SrcOut",
        "SrcAtop",
        "Clear",
        "Dst",
        "DstOver",
        "DstIn",
        "DstOut",
        "DstAtop",
        "Xor", };
    private static final AlphaComposite[] compObjs = {
        Src, SrcOver, SrcIn, SrcOut, SrcAtop, Clear,
        Dst, DstOver, DstIn, DstOut, DstAtop, Xor, };
    private static final int NUM_RULES = compObjs.length;
    private static final int HALF_NUM_RULES = NUM_RULES / 2;
    private int fadeIndex;
    private static float[][] fadeValues = {
        { 1.0f, -0.1f, 0.0f, 1.0f, 0.0f, 1.0f },
        { 0.0f, 0.1f, 1.0f, 1.0f, -0.1f, 0.0f },
        { 1.0f, 0.0f, 1.0f, 0.0f, 0.1f, 1.0f }, };
    private static String[] fadeNames = {
        "Src => transparent, Dest opaque",
        "Src => opaque, Dest => transparent",
        "Src opaque, Dest => opaque", };
    private static Font f = new Font("serif", Font.PLAIN, 10);
    private float srca = fadeValues[fadeIndex][0];
    private float dsta = fadeValues[fadeIndex][3];
    private String fadeLabel = fadeNames[0];
    private BufferedImage statBI, animBI;
    private int PADLEFT, PADRIGHT, HPAD;
    private int PADABOVE, PADBELOW, VPAD;
    private int RECTWIDTH, RECTHEIGHT;
    private int PADDEDHEIGHT;
    private GeneralPath srcpath = new GeneralPath();
    private GeneralPath dstpath = new GeneralPath();
    private LineMetrics lm;
    private BufferedImage dBI, sBI;
    private GradientPaint gradientDst, gradientSrc;

    public ACrules() {
        setBackground(Color.white);
    }

    @Override
    public void reset(int w, int h) {
        setSleepAmount(400);
        FontRenderContext frc = new FontRenderContext(null, false, false);
        lm = f.getLineMetrics(compNames[0], frc);

        PADLEFT = (w < 150) ? 10 : 15;
        PADRIGHT = (w < 150) ? 10 : 15;
        HPAD = (PADLEFT + PADRIGHT);
        PADBELOW = (h < 250) ? 1 : 2;
        PADABOVE = PADBELOW + (int) lm.getHeight();
        VPAD = (PADABOVE + PADBELOW);
        RECTWIDTH = w / 4 - HPAD;
        RECTWIDTH = (RECTWIDTH < 6) ? 6 : RECTWIDTH;
        RECTHEIGHT = (h - VPAD) / HALF_NUM_RULES - VPAD;
        RECTHEIGHT = (RECTHEIGHT < 6) ? 6 : RECTHEIGHT;
        PADDEDHEIGHT = (RECTHEIGHT + VPAD);

        srcpath.reset();
        srcpath.moveTo(0, 0);
        srcpath.lineTo(RECTWIDTH, 0);
        srcpath.lineTo(0, RECTHEIGHT);
        srcpath.closePath();

        dstpath.reset();
        dstpath.moveTo(0, 0);
        dstpath.lineTo(RECTWIDTH, RECTHEIGHT);
        dstpath.lineTo(RECTWIDTH, 0);
        dstpath.closePath();

        dBI = new BufferedImage(RECTWIDTH, RECTHEIGHT,
                BufferedImage.TYPE_INT_ARGB);
        sBI = new BufferedImage(RECTWIDTH, RECTHEIGHT,
                BufferedImage.TYPE_INT_ARGB);
        gradientDst = new GradientPaint(0, 0,
                new Color(1.0f, 0.0f, 0.0f, 1.0f),
                0, RECTHEIGHT,
                new Color(1.0f, 0.0f, 0.0f, 0.0f));
        gradientSrc = new GradientPaint(0, 0,
                new Color(0.0f, 0.0f, 1.0f, 1.0f),
                RECTWIDTH, 0,
                new Color(0.0f, 0.0f, 1.0f, 0.0f));
        statBI = new BufferedImage(w / 2, h, BufferedImage.TYPE_INT_RGB);
        statBI = drawCompBI(statBI, true);
        animBI = new BufferedImage(w / 2, h, BufferedImage.TYPE_INT_RGB);
    }

    @Override
    public void step(int w, int h) {
        if (getSleepAmount() == 5000) {
            setSleepAmount(200);
        }

        srca = srca + fadeValues[fadeIndex][1];
        dsta = dsta + fadeValues[fadeIndex][4];
        fadeLabel = fadeNames[fadeIndex];
        if (srca < 0 || srca > 1.0 || dsta < 0 || dsta > 1.0) {
            setSleepAmount(5000);
            srca = fadeValues[fadeIndex][2];
            dsta = fadeValues[fadeIndex][5];
            if (fadeIndex++ == fadeValues.length - 1) {
                fadeIndex = 0;
            }
        }
    }

    @Override
    public void render(int w, int h, Graphics2D g2) {

        if (statBI == null || animBI == null) {
            return;
        }
        g2.drawImage(statBI, 0, 0, null);
        g2.drawImage(drawCompBI(animBI, false), w / 2, 0, null);

        g2.setColor(Color.black);
        FontRenderContext frc = g2.getFontRenderContext();
        TextLayout tl = new TextLayout("AC Rules", g2.getFont(), frc);
        tl.draw(g2, 15.0f, (float) tl.getBounds().getHeight() + 3.0f);

        tl = new TextLayout(fadeLabel, f, frc);
        float x = (float) (w * 0.75 - tl.getBounds().getWidth() / 2);
        if ((x + tl.getBounds().getWidth()) > w) {
            x = (float) (w - tl.getBounds().getWidth());
        }
        tl.draw(g2, x, (float) tl.getBounds().getHeight() + 3.0f);
    }

    private BufferedImage drawCompBI(BufferedImage bi, boolean doGradient) {
        Graphics2D big = bi.createGraphics();
        big.setColor(getBackground());
        big.fillRect(0, 0, bi.getWidth(), bi.getHeight());
        big.setRenderingHint(RenderingHints.KEY_ANTIALIASING, AntiAlias);
        big.setFont(f);

        Graphics2D gD = dBI.createGraphics();
        gD.setRenderingHint(RenderingHints.KEY_ANTIALIASING, AntiAlias);
        Graphics2D gS = sBI.createGraphics();
        gS.setRenderingHint(RenderingHints.KEY_ANTIALIASING, AntiAlias);

        int x = 0, y = 0;
        int yy = (int) lm.getHeight() + VPAD;

        for (int i = 0; i < compNames.length; i++) {
            y = (i == 0 || i == HALF_NUM_RULES) ? yy : y + PADDEDHEIGHT;
            x = (i >= HALF_NUM_RULES) ? bi.getWidth() / 2 + PADLEFT : PADLEFT;
            big.translate(x, y);

            gD.setComposite(Clear);
            gD.fillRect(0, 0, RECTWIDTH, RECTHEIGHT);
            gD.setComposite(Src);
            if (doGradient) {
                gD.setPaint(gradientDst);
                gD.fillRect(0, 0, RECTWIDTH, RECTHEIGHT);
            } else {
                gD.setPaint(new Color(1.0f, 0.0f, 0.0f, dsta));
                gD.fill(dstpath);
            }

            gS.setComposite(Clear);
            gS.fillRect(0, 0, RECTWIDTH, RECTHEIGHT);
            gS.setComposite(Src);
            if (doGradient) {
                gS.setPaint(gradientSrc);
                gS.fillRect(0, 0, RECTWIDTH, RECTHEIGHT);
            } else {
                gS.setPaint(new Color(0.0f, 0.0f, 1.0f, srca));
                gS.fill(srcpath);
            }

            gD.setComposite(compObjs[i]);
            gD.drawImage(sBI, 0, 0, null);

            big.drawImage(dBI, 0, 0, null);
            big.setColor(Color.black);
            big.drawString(compNames[i], 0, -lm.getDescent());
            big.drawRect(0, 0, RECTWIDTH, RECTHEIGHT);
            big.translate(-x, -y);
        }

        gD.dispose();
        gS.dispose();
        big.dispose();

        return bi;
    }

    public static void main(String[] argv) {
        createDemoFrame(new ACrules());
    }
}
