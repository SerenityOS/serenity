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
import static java.awt.Color.GRAY;
import static java.awt.Color.WHITE;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java2d.AnimatingControlsSurface;
import java2d.CustomControls;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * Scrolling text of fonts returned from GraphicsEnvironment.getAllFonts().
 */
@SuppressWarnings("serial")
public class AllFonts extends AnimatingControlsSurface {

    private static final List<Font> fonts = new ArrayList<Font>();

    static {
        GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (Font font : ge.getAllFonts()) {
            if (font.canDisplayUpTo(font.getName()) != 0) {
                fonts.add(font);
            }
        }
    }
    private int nStrs;
    private int strH;
    private int fi;
    protected int fsize = 14;
    protected List<Font> v = new ArrayList<Font>();

    public AllFonts() {
        setBackground(WHITE);
        setSleepAmount(500);
        setControls(new Component[] { new DemoControls(this) });
    }

    public void handleThread(int state) {
    }

    @Override
    public void reset(int w, int h) {
        v.clear();
        Font f = fonts.get(0).deriveFont(Font.PLAIN, fsize);
        FontMetrics fm = getFontMetrics(f);
        strH = (fm.getAscent() + fm.getDescent());
        nStrs = h / strH + 1;
        fi = 0;
    }

    @Override
    public void step(int w, int h) {
        if (fi < fonts.size()) {
            v.add(fonts.get(fi).deriveFont(Font.PLAIN, fsize));
        }
        if (v.size() == nStrs && !v.isEmpty() || fi > fonts.size()) {
            v.remove(0);
        }
        fi = v.isEmpty() ? 0 : ++fi;
    }

    @Override
    public void render(int w, int h, Graphics2D g2) {

        g2.setColor(BLACK);

        int yy = (fi >= fonts.size()) ? 0 : h - v.size() * strH - strH / 2;

        for (int i = 0; i < v.size(); i++) {
            Font f = v.get(i);
            int sw = getFontMetrics(f).stringWidth(f.getName());
            g2.setFont(f);
            g2.drawString(f.getName(), (w / 2 - sw / 2), yy += strH);
        }
    }

    public static void main(String[] argv) {
        createDemoFrame(new AllFonts());
    }


    static class DemoControls extends CustomControls implements ActionListener,
            ChangeListener {

        AllFonts demo;
        JSlider slider;
        int[] fsize = { 8, 14, 18, 24 };
        JMenuItem[] menuitem = new JMenuItem[fsize.length];
        Font[] font = new Font[fsize.length];

        @SuppressWarnings("LeakingThisInConstructor")
        public DemoControls(AllFonts demo) {
            this.demo = demo;
            setBackground(GRAY);

            int sleepAmount = (int) demo.getSleepAmount();
            slider = new JSlider(SwingConstants.HORIZONTAL, 0, 999, sleepAmount);
            slider.setBorder(new EtchedBorder());
            slider.setPreferredSize(new Dimension(90, 22));
            slider.addChangeListener(this);
            add(slider);
            JMenuBar menubar = new JMenuBar();
            add(menubar);
            JMenu menu = menubar.add(new JMenu("Font Size"));
            for (int i = 0; i < fsize.length; i++) {
                font[i] = new Font(Font.SERIF, Font.PLAIN, fsize[i]);
                menuitem[i] = menu.add(new JMenuItem(String.valueOf(fsize[i])));
                menuitem[i].setFont(font[i]);
                menuitem[i].addActionListener(this);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            for (int i = 0; i < fsize.length; i++) {
                if (e.getSource().equals(menuitem[i])) {
                    demo.fsize = fsize[i];
                    Dimension d = demo.getSize();
                    demo.reset(d.width, d.height);
                    break;
                }
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            demo.setSleepAmount(slider.getValue());
        }
    }
}
