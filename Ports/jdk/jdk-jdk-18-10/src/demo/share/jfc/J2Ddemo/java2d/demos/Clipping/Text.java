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
package java2d.demos.Clipping;


import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.CYAN;
import static java.awt.Color.GRAY;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static java.awt.Color.YELLOW;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java2d.ControlsSurface;
import java2d.CustomControls;
import javax.swing.AbstractButton;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;


/**
 * Clipping an image, lines, text, texture and gradient with text.
 */
@SuppressWarnings("serial")
public class Text extends ControlsSurface {

    /**
     *
     */
    static Image img;
    static TexturePaint texturePaint;

    static {
        BufferedImage bi = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
        Graphics2D big = bi.createGraphics();
        big.setBackground(YELLOW);
        big.clearRect(0, 0, 5, 5);
        big.setColor(RED);
        big.fillRect(0, 0, 3, 3);
        texturePaint = new TexturePaint(bi, new Rectangle(0, 0, 5, 5));
    }
    private String clipType = "Lines";
    protected boolean doClip = true;

    public Text() {
        setBackground(WHITE);
        img = getImage("clouds.jpg");
        setControls(new Component[] { new DemoControls(this) });
    }

    @Override
    public void render(int w, int h, Graphics2D g2) {

        FontRenderContext frc = g2.getFontRenderContext();
        Font f = new Font(Font.SANS_SERIF, Font.BOLD, 32);
        String s = "JDK";
        TextLayout tl = new TextLayout(s, f, frc);
        double sw = tl.getBounds().getWidth();
        double sh = tl.getBounds().getHeight();
        double sx = (w - 40) / sw;
        double sy = (h - 40) / sh;
        AffineTransform Tx = AffineTransform.getScaleInstance(sx, sy);
        Shape shape = tl.getOutline(Tx);
        sw = shape.getBounds().getWidth();
        sh = shape.getBounds().getHeight();
        Tx =
                AffineTransform.getTranslateInstance(w / 2 - sw / 2, h / 2 + sh
                / 2);
        shape = Tx.createTransformedShape(shape);
        Rectangle r = shape.getBounds();

        if (doClip) {
            g2.clip(shape);
        }

        if (clipType.equals("Lines")) {
            g2.setColor(BLACK);
            g2.fill(r);
            g2.setColor(YELLOW);
            g2.setStroke(new BasicStroke(1.5f));
            for (int j = r.y; j < r.y + r.height; j = j + 3) {
                Line2D line = new Line2D.Float(r.x, j,
                        (r.x + r.width), j);
                g2.draw(line);
            }
        } else if (clipType.equals("Image")) {
            g2.drawImage(img, r.x, r.y, r.width, r.height, null);
        } else if (clipType.equals("TP")) {
            g2.setPaint(texturePaint);
            g2.fill(r);
        } else if (clipType.equals("GP")) {
            g2.setPaint(new GradientPaint(0, 0, BLUE, w, h, YELLOW));
            g2.fill(r);
        } else if (clipType.equals("Text")) {
            g2.setColor(BLACK);
            g2.fill(shape.getBounds());
            g2.setColor(CYAN);
            f = new Font(Font.SERIF, Font.BOLD, 10);
            tl = new TextLayout("OpenJDK", f, frc);
            sw = tl.getBounds().getWidth();

            int x = r.x;
            int y = (int) (r.y + tl.getAscent());
            sh = r.y + r.height;
            while (y < sh) {
                tl.draw(g2, x, y);
                if ((x += (int) sw) > (r.x + r.width)) {
                    x = r.x;
                    y += (int) tl.getAscent();
                }
            }
        }
        g2.setClip(new Rectangle(0, 0, w, h));

        g2.setColor(GRAY);
        g2.draw(shape);
    }

    public static void main(String[] s) {
        createDemoFrame(new Text());
    }


    @SuppressWarnings("serial")
    static final class DemoControls extends CustomControls implements
            ActionListener {

        Text demo;
        JToolBar toolbar;

        public DemoControls(Text demo) {
            super(demo.name);
            this.demo = demo;
            add(toolbar = new JToolBar());
            toolbar.setFloatable(false);
            addTool("Clip", true);
            addTool("Lines", true);
            addTool("Image", false);
            addTool("TP", false);
            addTool("GP", false);
            addTool("Text", false);
        }

        public void addTool(String str, boolean state) {
            JToggleButton b =
                    (JToggleButton) toolbar.add(new JToggleButton(str));
            b.setFocusPainted(false);
            b.setSelected(state);
            b.addActionListener(this);
            int width = b.getPreferredSize().width;
            Dimension prefSize = new Dimension(width, 21);
            b.setPreferredSize(prefSize);
            b.setMaximumSize(prefSize);
            b.setMinimumSize(prefSize);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(toolbar.getComponentAtIndex(0))) {
                JToggleButton b = (JToggleButton) e.getSource();
                demo.doClip = b.isSelected();
            } else {
                for (Component comp : toolbar.getComponents()) {
                    ((JToggleButton) comp).setSelected(false);
                }
                JToggleButton b = (JToggleButton) e.getSource();
                b.setSelected(true);
                demo.clipType = b.getText();
            }
            demo.repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 40);
        }

        @Override
        @SuppressWarnings("SleepWhileHoldingLock")
        public void run() {
            try {
                Thread.sleep(1111);
            } catch (Exception e) {
                return;
            }
            Thread me = Thread.currentThread();
            while (thread == me) {
                for (int i = 1; i < toolbar.getComponentCount() - 1; i++) {
                    ((AbstractButton) toolbar.getComponentAtIndex(i)).doClick();
                    try {
                        Thread.sleep(4444);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
            thread = null;
        }
    } // End DemoControls
} // End Text

