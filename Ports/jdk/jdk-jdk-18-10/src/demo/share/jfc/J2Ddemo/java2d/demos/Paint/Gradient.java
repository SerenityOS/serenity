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


import static java.awt.Color.black;
import static java.awt.Color.blue;
import static java.awt.Color.cyan;
import static java.awt.Color.green;
import static java.awt.Color.lightGray;
import static java.awt.Color.magenta;
import static java.awt.Color.orange;
import static java.awt.Color.red;
import static java.awt.Color.white;
import static java.awt.Color.yellow;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextLayout;
import java2d.ControlsSurface;
import java2d.CustomControls;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;


@SuppressWarnings("serial")
public class Gradient extends ControlsSurface {

    protected Color innerC, outerC;

    public Gradient() {
        setBackground(white);
        innerC = green;
        outerC = blue;
        setControls(new Component[] { new DemoControls(this) });
    }

    @Override
    public void render(int w, int h, Graphics2D g2) {

        int w2 = w / 2;
        int h2 = h / 2;
        g2.setPaint(new GradientPaint(0, 0, outerC, w * .35f, h * .35f, innerC));
        g2.fillRect(0, 0, w2, h2);
        g2.setPaint(new GradientPaint(w, 0, outerC, w * .65f, h * .35f, innerC));
        g2.fillRect(w2, 0, w2, h2);
        g2.setPaint(new GradientPaint(0, h, outerC, w * .35f, h * .65f, innerC));
        g2.fillRect(0, h2, w2, h2);
        g2.setPaint(new GradientPaint(w, h, outerC, w * .65f, h * .65f, innerC));
        g2.fillRect(w2, h2, w2, h2);

        g2.setColor(black);
        TextLayout tl = new TextLayout(
                "GradientPaint", g2.getFont(), g2.getFontRenderContext());
        tl.draw(g2, (int) (w / 2 - tl.getBounds().getWidth() / 2),
                (int) (h / 2 + tl.getBounds().getHeight() / 2));
    }

    public static void main(String[] s) {
        createDemoFrame(new Gradient());
    }


    static class DemoControls extends CustomControls implements ActionListener {

        Gradient demo;
        Color[] colors = { red, orange, yellow, green, blue, lightGray, cyan,
            magenta };
        String[] colorName = { "Red", "Orange", "Yellow", "Green",
            "Blue", "lightGray", "Cyan", "Magenta" };
        JMenuItem[] innerMI = new JMenuItem[colors.length];
        JMenuItem[] outerMI = new JMenuItem[colors.length];
        ColoredSquare[] squares = new ColoredSquare[colors.length];
        JMenu imenu, omenu;

        @SuppressWarnings("LeakingThisInConstructor")
        public DemoControls(Gradient demo) {
            super(demo.name);
            this.demo = demo;
            JMenuBar inMenuBar = new JMenuBar();
            add(inMenuBar);
            JMenuBar outMenuBar = new JMenuBar();
            add(outMenuBar);
            Font font = new Font(Font.SERIF, Font.PLAIN, 10);

            imenu = inMenuBar.add(new JMenu("Inner Color"));
            imenu.setFont(font);
            imenu.setIcon(new ColoredSquare(demo.innerC));
            omenu = outMenuBar.add(new JMenu("Outer Color"));
            omenu.setFont(font);
            omenu.setIcon(new ColoredSquare(demo.outerC));
            for (int i = 0; i < colors.length; i++) {
                squares[i] = new ColoredSquare(colors[i]);
                innerMI[i] = imenu.add(new JMenuItem(colorName[i]));
                innerMI[i].setFont(font);
                innerMI[i].setIcon(squares[i]);
                innerMI[i].addActionListener(this);
                outerMI[i] = omenu.add(new JMenuItem(colorName[i]));
                outerMI[i].setFont(font);
                outerMI[i].setIcon(squares[i]);
                outerMI[i].addActionListener(this);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            for (int i = 0; i < colors.length; i++) {
                if (e.getSource().equals(innerMI[i])) {
                    demo.innerC = colors[i];
                    imenu.setIcon(squares[i]);
                    break;
                } else if (e.getSource().equals(outerMI[i])) {
                    demo.outerC = colors[i];
                    omenu.setIcon(squares[i]);
                    break;
                }
            }
            demo.repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 37);
        }

        @Override
        @SuppressWarnings("SleepWhileHoldingLock")
        public void run() {
            // goto double buffering
            if (demo.getImageType() <= 1) {
                demo.setImageType(2);
            }
            Thread me = Thread.currentThread();
            while (thread == me) {
                for (int i = 0; i < innerMI.length; i++) {
                    if (i != 4) {
                        try {
                            Thread.sleep(4444);
                        } catch (InterruptedException e) {
                            return;
                        }
                        innerMI[i].doClick();
                    }
                }
            }
            thread = null;
        }


        class ColoredSquare implements Icon {

            Color color;

            public ColoredSquare(Color c) {
                this.color = c;
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Color oldColor = g.getColor();
                g.setColor(color);
                g.fill3DRect(x, y, getIconWidth(), getIconHeight(), true);
                g.setColor(oldColor);
            }

            @Override
            public int getIconWidth() {
                return 12;
            }

            @Override
            public int getIconHeight() {
                return 12;
            }
        } // End ColoredSquare class
    } // End DemoControls
} // End Gradient class

