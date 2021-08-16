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


import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.CYAN;
import static java.awt.Color.GREEN;
import static java.awt.Color.LIGHT_GRAY;
import static java.awt.Color.MAGENTA;
import static java.awt.Color.ORANGE;
import static java.awt.Color.PINK;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static java.awt.Color.YELLOW;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.geom.Arc2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java2d.AnimatingControlsSurface;
import java2d.CustomControls;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * Animation of compositing shapes, text and images fading in and out.
 */
@SuppressWarnings("serial")
public final class FadeAnim extends AnimatingControlsSurface {

    private static final TexturePaint texturePaint;

    static {
        int w = 10;
        int h = 10;
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D gi = bi.createGraphics();
        Color oc = BLUE;
        Color ic = GREEN;
        gi.setPaint(new GradientPaint(0, 0, oc, w * .35f, h * .35f, ic));
        gi.fillRect(0, 0, w / 2, h / 2);
        gi.setPaint(new GradientPaint(w, 0, oc, w * .65f, h * .35f, ic));
        gi.fillRect(w / 2, 0, w / 2, h / 2);
        gi.setPaint(new GradientPaint(0, h, oc, w * .35f, h * .65f, ic));
        gi.fillRect(0, h / 2, w / 2, h / 2);
        gi.setPaint(new GradientPaint(w, h, oc, w * .65f, h * .65f, ic));
        gi.fillRect(w / 2, h / 2, w / 2, h / 2);
        texturePaint = new TexturePaint(bi, new Rectangle(0, 0, w, h));
    }
    private static BasicStroke bs = new BasicStroke(6);
    private static Font[] fonts = {
        new Font(Font.SERIF, Font.PLAIN, 64),
        new Font(Font.SERIF, Font.BOLD | Font.ITALIC, 24),
        new Font(Font.MONOSPACED, Font.BOLD, 36),
        new Font(Font.SANS_SERIF, Font.BOLD | Font.ITALIC, 48),
        new Font(Font.SANS_SERIF, Font.PLAIN, 52) };
    private static String[] strings = {
        "Alpha", "Composite", "Src", "SrcOver",
        "SrcIn", "SrcOut", "Clear", "DstOver", "DstIn" };
    private static String[] imgs = {
        "jumptojavastrip.png", "duke.png", "star7.png" };
    private static Paint[] paints = {
        RED, BLUE, GREEN, MAGENTA,
        ORANGE, PINK, CYAN, texturePaint,
        YELLOW, LIGHT_GRAY, WHITE };
    private List<ObjectData> objects = new ArrayList<ObjectData>(20);
    private int numShapes, numStrings, numImages;

    public FadeAnim() {
        setBackground(BLACK);
        setStrings(2);
        setImages(3);
        setShapes(8);
        setControls(new Component[] { new DemoControls(this) });
        setConstraints(new String[] { BorderLayout.EAST });
    }

    public void setImages(int num) {

        if (num < numImages) {
            List<ObjectData> images = new ArrayList<ObjectData>(objects.size());
            for (ObjectData obj : objects) {
                if (obj.object instanceof Image) {
                    images.add(obj);
                }
            }
            objects.removeAll(images.subList(num, images.size()));
        } else {
            Dimension d = getSize();
            for (int i = numImages; i < num; i++) {
                Object obj = getImage(imgs[i % imgs.length]);
                if (imgs[i % imgs.length].equals("jumptojavastrip.png")) {
                    int iw = ((Image) obj).getWidth(null);
                    int ih = ((Image) obj).getHeight(null);
                    BufferedImage bimage = new BufferedImage(iw, ih,
                            BufferedImage.TYPE_INT_RGB);
                    bimage.createGraphics().drawImage((Image) obj, 0, 0, null);
                    obj = bimage;
                }
                ObjectData od = new ObjectData(obj, BLACK);
                od.reset(d.width, d.height);
                objects.add(od);
            }
        }
        numImages = num;
    }

    public void setStrings(int num) {

        if (num < numStrings) {
            List<ObjectData> textDatas = new ArrayList<ObjectData>(
                    objects.size());
            //for (int i = 0; i < objects.size(); i++) {
            for (ObjectData obj : objects) {
                if (obj.object instanceof TextData) {
                    textDatas.add(obj);
                }
            }
            objects.removeAll(textDatas.subList(num, textDatas.size()));
        } else {
            Dimension d = getSize();
            for (int i = numStrings; i < num; i++) {
                int j = i % fonts.length;
                int k = i % strings.length;
                Object obj = new TextData(strings[k], fonts[j], this);
                ObjectData od = new ObjectData(obj, paints[i % paints.length]);
                od.reset(d.width, d.height);
                objects.add(od);
            }
        }
        numStrings = num;
    }

    public void setShapes(int num) {

        if (num < numShapes) {
            List<ObjectData> shapes = new ArrayList<ObjectData>(objects.size());
            //for (int i = 0; i < objects.size(); i++) {
            for (ObjectData obj : objects) {
                if (obj.object instanceof Shape) {
                    shapes.add(obj);
                }
            }
            objects.removeAll(shapes.subList(num, shapes.size()));
        } else {
            Dimension d = getSize();
            for (int i = numShapes; i < num; i++) {
                Object obj = null;
                switch (i % 7) {
                    case 0:
                        obj = new GeneralPath();
                        break;
                    case 1:
                        obj = new Rectangle2D.Double();
                        break;
                    case 2:
                        obj = new Ellipse2D.Double();
                        break;
                    case 3:
                        obj = new Arc2D.Double();
                        break;
                    case 4:
                        obj = new RoundRectangle2D.Double();
                        break;
                    case 5:
                        obj = new CubicCurve2D.Double();
                        break;
                    case 6:
                        obj = new QuadCurve2D.Double();
                        break;
                }
                ObjectData od = new ObjectData(obj, paints[i % paints.length]);
                od.reset(d.width, d.height);
                objects.add(od);
            }
        }
        numShapes = num;
    }

    @Override
    public void reset(int w, int h) {
        for (int i = 0; i < objects.size(); i++) {
            objects.get(i).reset(w, h);
        }
    }

    @Override
    public void step(int w, int h) {
        for (int i = 0; i < objects.size(); i++) {
            objects.get(i).step(w, h);
        }
    }

    @Override
    public void render(int w, int h, Graphics2D g2) {
        for (int i = 0; i < objects.size(); i++) {
            ObjectData od = objects.get(i);
            AlphaComposite ac = AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, od.alpha);
            g2.setComposite(ac);
            g2.setPaint(od.paint);
            g2.translate(od.x, od.y);

            if (od.object instanceof Image) {
                g2.drawImage((Image) od.object, 0, 0, this);
            } else if (od.object instanceof TextData) {
                g2.setFont(((TextData) od.object).font);
                g2.drawString(((TextData) od.object).string, 0, 0);
            } else if (od.object instanceof QuadCurve2D
                    || od.object instanceof CubicCurve2D) {
                g2.setStroke(bs);
                g2.draw((Shape) od.object);
            } else if (od.object instanceof Shape) {
                g2.fill((Shape) od.object);
            }
            g2.translate(-od.x, -od.y);
        }
    }

    public static void main(String[] argv) {
        createDemoFrame(new FadeAnim());
    }


    static class TextData extends Object {

        public String string;
        public Font font;
        public int width, height;

        public TextData(String str, Font font, Component cmp) {
            string = str;
            this.font = font;
            FontMetrics fm = cmp.getFontMetrics(font);
            width = fm.stringWidth(str);
            height = fm.getHeight();
        }
    }


    static class ObjectData extends Object {

        final int UP = 0;
        final int DOWN = 1;
        Object object;
        BufferedImage bimg;
        Paint paint;
        double x, y;
        float alpha;
        int alphaDirection;
        int imgX;

        public ObjectData(Object object, Paint paint) {
            this.object = object;
            this.paint = paint;
            if (object instanceof BufferedImage) {
                bimg = (BufferedImage) object;
                this.object = bimg.getSubimage(0, 0, 80, 80);
            }
            getRandomXY(300, 250);
            alpha = (float) Math.random();
            alphaDirection = Math.random() > 0.5 ? UP : DOWN;
        }

        private void getRandomXY(int w, int h) {
            if (object instanceof TextData) {
                x = Math.random() * (w - ((TextData) object).width);
                y = Math.random() * h;
                y = y < ((TextData) object).height ? ((TextData) object).height
                        : y;
            } else if (object instanceof Image) {
                x = Math.random() * (w - ((Image) object).getWidth(null));
                y = Math.random() * (h - ((Image) object).getHeight(null));
            } else if (object instanceof Shape) {
                Rectangle bounds = ((Shape) object).getBounds();
                x = Math.random() * (w - bounds.width);
                y = Math.random() * (h - bounds.height);
            }
        }

        public void reset(int w, int h) {
            getRandomXY(w, h);
            double ww = 20 + Math.random() * ((w == 0 ? 400 : w) / 4);
            double hh = 20 + Math.random() * ((h == 0 ? 300 : h) / 4);
            if (object instanceof Ellipse2D) {
                ((Ellipse2D) object).setFrame(0, 0, ww, hh);
            } else if (object instanceof Rectangle2D) {
                ((Rectangle2D) object).setRect(0, 0, ww, ww);
            } else if (object instanceof RoundRectangle2D) {
                ((RoundRectangle2D) object).setRoundRect(0, 0, hh, hh, 20, 20);
            } else if (object instanceof Arc2D) {
                ((Arc2D) object).setArc(0, 0, hh, hh, 45, 270, Arc2D.PIE);
            } else if (object instanceof QuadCurve2D) {
                ((QuadCurve2D) object).setCurve(0, 0, w * .2, h * .4, w * .4, 0);
            } else if (object instanceof CubicCurve2D) {
                ((CubicCurve2D) object).setCurve(0, 0, 30, -60, 60, 60, 90, 0);
            } else if (object instanceof GeneralPath) {
                GeneralPath p = new GeneralPath();
                float size = (float) ww;
                p.moveTo(-size / 2.0f, -size / 8.0f);
                p.lineTo(+size / 2.0f, -size / 8.0f);
                p.lineTo(-size / 4.0f, +size / 2.0f);
                p.lineTo(+0.0f, -size / 2.0f);
                p.lineTo(+size / 4.0f, +size / 2.0f);
                p.closePath();
                object = p;
            }
        }

        public void step(int w, int h) {
            if (object instanceof BufferedImage) {
                if ((imgX += 80) == 800) {
                    imgX = 0;
                }
                object = bimg.getSubimage(imgX, 0, 80, 80);
            }
            if (alphaDirection == UP) {
                if ((alpha += 0.05) > .99) {
                    alphaDirection = DOWN;
                    alpha = 1.0f;
                }
            } else if (alphaDirection == DOWN) {
                if ((alpha -= .05) < 0.01) {
                    alphaDirection = UP;
                    alpha = 0;
                    getRandomXY(w, h);
                }
            }
        }
    }


    static class DemoControls extends CustomControls implements ChangeListener {

        FadeAnim demo;
        JSlider shapeSlider, stringSlider, imageSlider;
        Font font = new Font(Font.SERIF, Font.BOLD, 10);

        @SuppressWarnings("LeakingThisInConstructor")
        public DemoControls(FadeAnim demo) {
            super(demo.name);
            this.demo = demo;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            add(Box.createVerticalStrut(5));

            JToolBar toolbar = new JToolBar(SwingConstants.VERTICAL);
            toolbar.setFloatable(false);
            shapeSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 20,
                    demo.numShapes);
            shapeSlider.addChangeListener(this);
            TitledBorder tb = new TitledBorder(new EtchedBorder());
            tb.setTitleFont(font);
            tb.setTitle(String.valueOf(demo.numShapes) + " Shapes");
            shapeSlider.setBorder(tb);
            shapeSlider.setPreferredSize(new Dimension(80, 45));
            shapeSlider.setOpaque(true);
            toolbar.addSeparator();
            toolbar.add(shapeSlider);
            toolbar.addSeparator();

            stringSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 10,
                    demo.numStrings);
            stringSlider.addChangeListener(this);
            tb = new TitledBorder(new EtchedBorder());
            tb.setTitleFont(font);
            tb.setTitle(String.valueOf(demo.numStrings) + " Strings");
            stringSlider.setBorder(tb);
            stringSlider.setPreferredSize(new Dimension(80, 45));
            stringSlider.setOpaque(true);
            toolbar.add(stringSlider);
            toolbar.addSeparator();

            imageSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 10,
                    demo.numImages);
            imageSlider.addChangeListener(this);
            tb = new TitledBorder(new EtchedBorder());
            tb.setTitleFont(font);
            tb.setTitle(String.valueOf(demo.numImages) + " Images");
            imageSlider.setBorder(tb);
            imageSlider.setPreferredSize(new Dimension(80, 45));
            imageSlider.setOpaque(true);
            toolbar.add(imageSlider);
            toolbar.addSeparator();

            add(toolbar);
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            JSlider slider = (JSlider) e.getSource();
            int value = slider.getValue();
            TitledBorder tb = (TitledBorder) slider.getBorder();
            if (slider.equals(shapeSlider)) {
                tb.setTitle(String.valueOf(value) + " Shapes");
                demo.setShapes(value);
            } else if (slider.equals(stringSlider)) {
                tb.setTitle(String.valueOf(value) + " Strings");
                demo.setStrings(value);
            } else if (slider.equals(imageSlider)) {
                tb.setTitle(String.valueOf(value) + " Images");
                demo.setImages(value);
            }
            slider.repaint();
            if (!demo.animating.running()) {
                demo.repaint();
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(80, 0);
        }

        @Override
        public void run() {
            try {
                Thread.sleep(999);
            } catch (InterruptedException e) {
                return;
            }
            shapeSlider.setValue((int) (Math.random() * 5));
            stringSlider.setValue(10);
            thread = null;
        }
    } // End DemoControls
} // End FadeAnim

