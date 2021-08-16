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
import static java.awt.Color.GRAY;
import static java.awt.Color.LIGHT_GRAY;
import static java.awt.Color.WHITE;
import static java.awt.Color.YELLOW;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java2d.ControlsSurface;
import java2d.CustomControls;
import javax.swing.JLabel;
import javax.swing.JTextField;


/**
 * Rotate ellipses with controls for increment and emphasis.
 * Emphasis is defined as which ellipses have a darker color and thicker stroke.
 */
@SuppressWarnings("serial")
public class Rotate extends ControlsSurface {

    protected double increment = 5.0;
    protected int emphasis = 9;

    public Rotate() {
        setBackground(WHITE);
        setControls(new Component[] { new DemoControls(this) });
    }

    @Override
    public void render(int w, int h, Graphics2D g2) {
        int size = Math.min(w, h);
        float ew = size / 4;
        float eh = size - 20;
        Ellipse2D ellipse = new Ellipse2D.Float(-ew / 2, -eh / 2, ew, eh);
        for (double angdeg = 0; angdeg < 360; angdeg += increment) {
            if (angdeg % emphasis == 0) {
                g2.setColor(GRAY);
                g2.setStroke(new BasicStroke(2.0f));
            } else {
                g2.setColor(LIGHT_GRAY);
                g2.setStroke(new BasicStroke(0.5f));
            }
            AffineTransform at = AffineTransform.getTranslateInstance(w / 2, h
                    / 2);
            at.rotate(Math.toRadians(angdeg));
            g2.draw(at.createTransformedShape(ellipse));
        }
        g2.setColor(BLUE);
        ellipse.setFrame(w / 2 - 10, h / 2 - 10, 20, 20);
        g2.fill(ellipse);
        g2.setColor(GRAY);
        g2.setStroke(new BasicStroke(6));
        g2.draw(ellipse);
        g2.setColor(YELLOW);
        g2.setStroke(new BasicStroke(4));
        g2.draw(ellipse);
        g2.setColor(BLACK);
        g2.drawString("Rotate", 5, 15);
    }

    public static void main(String[] s) {
        createDemoFrame(new Rotate());
    }


    static class DemoControls extends CustomControls implements ActionListener {

        Rotate demo;
        JTextField tf1, tf2;

        @SuppressWarnings("LeakingThisInConstructor")
        public DemoControls(Rotate demo) {
            super(demo.name);
            this.demo = demo;
            JLabel l = new JLabel("Increment:");
            l.setForeground(BLACK);
            add(l);
            add(tf1 = new JTextField("5.0"));
            tf1.setPreferredSize(new Dimension(30, 24));
            tf1.addActionListener(this);
            add(l = new JLabel("  Emphasis:"));
            l.setForeground(BLACK);
            add(tf2 = new JTextField("9"));
            tf2.setPreferredSize(new Dimension(30, 24));
            tf2.addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                if (e.getSource().equals(tf1)) {
                    demo.increment = Double.parseDouble(tf1.getText().trim());
                    if (demo.increment < 1.0) {
                        demo.increment = 1.0;
                    }
                } else {
                    demo.emphasis = Integer.parseInt(tf2.getText().trim());
                }
                demo.repaint();
            } catch (Exception ex) {
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 39);
        }

        @Override
        @SuppressWarnings("SleepWhileHoldingLock")
        public void run() {
            Thread me = Thread.currentThread();
            while (thread == me) {
                for (int i = 3; i < 13; i += 3) {
                    try {
                        Thread.sleep(4444);
                    } catch (InterruptedException e) {
                        return;
                    }
                    tf1.setText(String.valueOf(i));
                    demo.increment = i;
                    demo.repaint();
                }
            }
            thread = null;
        }
    } // End DemoControls class
} // End Rotate class

