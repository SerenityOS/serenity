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


package java2d.demos.Transforms;

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
import static java.awt.Font.BOLD;
import static java.awt.Font.ITALIC;
import static java.awt.Font.PLAIN;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
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
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.metal.MetalBorders.ButtonBorder;

/**
 * Animation of shapes, text and images rotating, scaling and translating
 * around a canvas.
 */
@SuppressWarnings("serial")
public final class TransformAnim extends AnimatingControlsSurface {

    private static final TexturePaint texturePaint;
    static {
        BufferedImage bi = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D gi = bi.createGraphics();
        gi.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        gi.setColor(RED);
        gi.fillOval(0,0,9,9);
        texturePaint = new TexturePaint(bi,new Rectangle(0,0,10,10));
    }

    private static BasicStroke bs = new BasicStroke(6);
    private static Font[] fonts = {
                new Font(Font.SERIF,      PLAIN,       48),
                new Font(Font.SERIF,      BOLD|ITALIC, 24),
                new Font(Font.MONOSPACED, BOLD,        36),
                new Font(Font.SANS_SERIF, BOLD|ITALIC, 64),
                new Font(Font.SANS_SERIF, PLAIN,       52)};
    private static String[] strings = {
                "Transformation", "Rotate", "Translate", "Shear", "Scale" };
    private static String[] imgs = { "duke.png" };
    private static Paint[] paints = {
                RED, BLUE, texturePaint, GREEN, MAGENTA, ORANGE, PINK, CYAN,
                new Color(0, 255, 0, 128), new Color(0, 0, 255, 128),
                YELLOW, LIGHT_GRAY, WHITE};
    private List<ObjData> objDatas = new ArrayList<ObjData>(13);
    private int numShapes, numStrings, numImages;
    protected boolean doRotate = true;
    protected boolean doTranslate = true;
    protected boolean doScale = true;
    protected boolean doShear;


    public TransformAnim() {
        setBackground(BLACK);
        setStrings(1);
        setImages(2);
        setShapes(10);
        setControls(new Component[] { new DemoControls(this) });
        setConstraints(new String[] { BorderLayout.EAST });
    }


    public void setImages(int num) {
        if (num < numImages) {
            List<ObjData> v = new ArrayList<ObjData>(objDatas.size());
            for (ObjData objData : objDatas) {
                if (objData.object instanceof Image) {
                    v.add(objData);
                }
            }
            objDatas.removeAll(v.subList(num, v.size()));
        } else {
            Dimension d = getSize();
            for (int i = numImages; i < num; i++) {
                Object obj = getImage(imgs[i % imgs.length]);
                ObjData objData = new ObjData(obj, BLACK);
                objData.reset(d.width, d.height);
                objDatas.add(objData);
            }
        }
        numImages = num;
    }


    public void setStrings(int num) {
        if (num < numStrings) {
            List<ObjData> v = new ArrayList<ObjData>(objDatas.size());
            for (ObjData objData : objDatas) {
                if (objData.object instanceof TextData) {
                    v.add(objData);
                }
            }
            objDatas.removeAll(v.subList(num, v.size()));
        } else {
            Dimension d = getSize();
            for (int i = numStrings; i < num; i++) {
                int j = i %   fonts.length;
                int k = i % strings.length;
                Object obj = new TextData(strings[k], fonts[j]);
                ObjData objData = new ObjData(obj, paints[i%paints.length]);
                objData.reset(d.width, d.height);
                objDatas.add(objData);
            }
        }
        numStrings = num;
    }


    public void setShapes(int num) {
        if (num < numShapes) {
            List<ObjData> v = new ArrayList<ObjData>(objDatas.size());
            for (ObjData objData : objDatas) {
                if (objData.object instanceof Shape) {
                    v.add(objData);
                }
            }
            objDatas.removeAll(v.subList(num, v.size()));
        } else {
            Dimension d = getSize();
            for (int i = numShapes; i < num; i++) {
                Object obj = null;
                switch (i % 7) {
                    case 0 : obj = new GeneralPath(); break;
                    case 1 : obj = new Rectangle2D.Double(); break;
                    case 2 : obj = new Ellipse2D.Double(); break;
                    case 3 : obj = new Arc2D.Double(); break;
                    case 4 : obj = new RoundRectangle2D.Double(); break;
                    case 5 : obj = new CubicCurve2D.Double(); break;
                    case 6 : obj = new QuadCurve2D.Double(); break;
                }
                ObjData objData = new ObjData(obj, paints[i%paints.length]);
                objData.reset(d.width, d.height);
                objDatas.add(objData);
            }
        }
        numShapes = num;
    }


    @Override
    public void reset(int w, int h) {
        for (ObjData objData : objDatas) {
            objData.reset(w, h);
        }
    }


    @Override
    public void step(int w, int h) {
        for (ObjData objData : objDatas) {
            objData.step(w, h, this);
        }
    }


    @Override
    public void render(int w, int h, Graphics2D g2) {
        for (ObjData objData : objDatas) {
            g2.setTransform(objData.at);
            g2.setPaint(objData.paint);
            if (objData.object instanceof Image) {
                g2.drawImage((Image) objData.object, 0, 0, this);
            } else if (objData.object instanceof TextData) {
                g2.setFont(((TextData) objData.object).font);
                g2.drawString(((TextData) objData.object).string, 0, 0);
            } else if (objData.object instanceof  QuadCurve2D
                    || objData.object instanceof CubicCurve2D)
            {
                g2.setStroke(bs);
                g2.draw((Shape) objData.object);
            } else if (objData.object instanceof Shape) {
                g2.fill((Shape) objData.object);
            }
        }
    }


    public static void main(String[] argv) {
        createDemoFrame(new TransformAnim());
    }


    static class TextData extends Object {

        public String string;
        public Font font;

        public TextData(String str, Font font) {
            string = str;
            this.font = font;
        }
    }


    static class ObjData extends Object {
        Object object;
        Paint paint;
        static final int UP   = 0;
        static final int DOWN = 1;
        double x, y;
        double ix=5, iy=3;
        int rotate;
        double scale, shear;
        int scaleDirection, shearDirection;
        AffineTransform at = new AffineTransform();


        public ObjData(Object object, Paint paint) {
            this.object = object;
            this.paint = paint;
            rotate = (int)(Math.random() * 360);
            scale = Math.random() * 1.5;
            scaleDirection = Math.random() > 0.5 ? UP : DOWN;
            shear = Math.random() * 0.5;
            shearDirection = Math.random() > 0.5 ? UP : DOWN;
        }


        public void reset(int w, int h) {
            x = Math.random()*w;
            y = Math.random()*h;
            double ww = 20 + Math.random()*((w == 0 ? 400 : w)/4);
            double hh = 20 + Math.random()*((h == 0 ? 300 : h)/4);
            if (object instanceof Ellipse2D) {
                ((Ellipse2D) object).setFrame(0, 0, ww, hh);
            } else if (object instanceof Rectangle2D) {
                ((Rectangle2D) object).setRect(0, 0, ww, ww);
            } else if (object instanceof RoundRectangle2D) {
                ((RoundRectangle2D) object).setRoundRect(0, 0, hh, hh, 20, 20);
            } else if (object instanceof Arc2D) {
                ((Arc2D) object).setArc(0, 0, hh, hh, 45, 270, Arc2D.PIE);
            } else if (object instanceof QuadCurve2D) {
                ((QuadCurve2D) object).setCurve(0, 0, w*.2, h*.4, w*.4, 0);
            } else if (object instanceof CubicCurve2D) {
                    ((CubicCurve2D) object).setCurve(0,0,30,-60,60,60,90,0);
            } else if (object instanceof GeneralPath) {
                GeneralPath p = new GeneralPath();
                float size = (float) ww;
                p.moveTo(- size / 2.0f, - size / 8.0f);
                p.lineTo(+ size / 2.0f, - size / 8.0f);
                p.lineTo(- size / 4.0f, + size / 2.0f);
                p.lineTo(+         0.0f, - size / 2.0f);
                p.lineTo(+ size / 4.0f, + size / 2.0f);
                p.closePath();
                object = p;
            }
        }


        public void step(int w, int h, TransformAnim demo) {
            at.setToIdentity();
            if (demo.doRotate) {
                if ((rotate+=5) == 360) {
                    rotate = 0;
                }
                at.rotate(Math.toRadians(rotate), x, y);
            }
            at.translate(x, y);
            if (demo.doTranslate) {
                x += ix;
                y += iy;
                if (x > w) {
                    x = w - 1;
                    ix = Math.random() * -w/32 - 1;
                }
                if (x < 0) {
                    x = 2;
                    ix = Math.random() * w/32 + 1;
                }
                if (y > h ) {
                    y = h - 2;
                    iy = Math.random() * -h/32 - 1;
                }
                if (y < 0) {
                    y = 2;
                    iy = Math.random() * h/32 + 1;
                }
            }
            if (demo.doScale && scaleDirection == UP) {
                if ((scale += 0.05) > 1.5) {
                    scaleDirection = DOWN;
                }
            } else if (demo.doScale && scaleDirection == DOWN) {
                if ((scale -= .05) < 0.5) {
                    scaleDirection = UP;
                }
            }
            if (demo.doScale) {
                at.scale(scale, scale);
            }
            if (demo.doShear && shearDirection == UP) {
                if ((shear += 0.05) > 0.5) {
                    shearDirection = DOWN;
                }
            } else if (demo.doShear && shearDirection == DOWN) {
                if ((shear -= .05) < -0.5) {
                    shearDirection = UP;
                }
            }
            if (demo.doShear) {
                at.shear(shear, shear);
            }
        }
    } // End ObjData class



    static final class DemoControls extends CustomControls implements ActionListener, ChangeListener {

        TransformAnim demo;
        JSlider shapeSlider, stringSlider, imageSlider;
        Font font = new Font(Font.SERIF, Font.BOLD, 10);
        JToolBar toolbar;
        ButtonBorder buttonBorder = new ButtonBorder();

        @SuppressWarnings("LeakingThisInConstructor")
        public DemoControls(TransformAnim demo) {
            super(demo.name);
            this.demo = demo;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            add(Box.createVerticalStrut(5));

            JToolBar bar = new JToolBar(SwingConstants.VERTICAL);
            bar.setFloatable(false);
            shapeSlider = new JSlider(SwingConstants.HORIZONTAL,0,20,demo.numShapes);
            shapeSlider.addChangeListener(this);
            TitledBorder tb = new TitledBorder(new EtchedBorder());
            tb.setTitleFont(font);
            tb.setTitle(String.valueOf(demo.numShapes) + " Shapes");
            shapeSlider.setBorder(tb);
            shapeSlider.setOpaque(true);
            shapeSlider.setPreferredSize(new Dimension(80,44));
            bar.add(shapeSlider);
            bar.addSeparator();

            stringSlider = new JSlider(SwingConstants.HORIZONTAL,0,10,demo.numStrings);
            stringSlider.addChangeListener(this);
            tb = new TitledBorder(new EtchedBorder());
            tb.setTitleFont(font);
            tb.setTitle(String.valueOf(demo.numStrings) + " Strings");
            stringSlider.setBorder(tb);
            stringSlider.setOpaque(true);
            stringSlider.setPreferredSize(new Dimension(80,44));
            bar.add(stringSlider);
            bar.addSeparator();

            imageSlider = new JSlider(SwingConstants.HORIZONTAL,0,10,demo.numImages);
            imageSlider.addChangeListener(this);
            tb = new TitledBorder(new EtchedBorder());
            tb.setTitleFont(font);
            tb.setTitle(String.valueOf(demo.numImages) + " Images");
            imageSlider.setBorder(tb);
            imageSlider.setOpaque(true);
            imageSlider.setPreferredSize(new Dimension(80,44));
            bar.add(imageSlider);
            bar.addSeparator();
            add(bar);

            toolbar = new JToolBar();
            toolbar.setFloatable(false);
            addButton("T", "translate", demo.doTranslate);
            addButton("R", "rotate", demo.doRotate);
            addButton("SC", "scale", demo.doScale);
            addButton("SH", "shear", demo.doShear);
            add(toolbar);
        }


        public void addButton(String s, String tt, boolean state) {
            JToggleButton b = (JToggleButton) toolbar.add(new JToggleButton(s));
            b.setFont(font);
            b.setSelected(state);
            b.setToolTipText(tt);
            b.setFocusPainted(false);
            b.setBorder(buttonBorder);
            b.addActionListener(this);
        }


        @Override
        public void actionPerformed(ActionEvent e) {
            JToggleButton b = (JToggleButton) e.getSource();
            if (b.getText().equals("T")) {
                demo.doTranslate = b.isSelected();
            } else if (b.getText().equals("R")) {
                demo.doRotate = b.isSelected();
            } else if (b.getText().equals("SC")) {
                demo.doScale = b.isSelected();
            } else if (b.getText().equals("SH")) {
                demo.doShear = b.isSelected();
            }
            if (!demo.animating.running()) {
                demo.repaint();
            }
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
            if (!demo.animating.running()) {
                demo.repaint();
            }
            slider.repaint();
        }


        @Override
        public Dimension getPreferredSize() {
            return new Dimension(80,38);
        }


        @Override
        @SuppressWarnings("SleepWhileHoldingLock")
        public void run() {
            Thread me = Thread.currentThread();
            while (thread == me) {
                for (int i = 1; i < toolbar.getComponentCount(); i++) {
                    try {
                        Thread.sleep(4444);
                    } catch (InterruptedException e) { return; }
                    ((AbstractButton) toolbar.getComponentAtIndex(i)).doClick();
                }
            }
            thread = null;
        }
    } // End DemoControls
} // End TransformAnim
