/*
 *
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package java2d;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;


/**
 * Displays the time for a Surface to paint. Displays the number
 * of frames per second on animated demos.  Up to four surfaces fit
 * in the display area.
 */
@SuppressWarnings("serial")
public class PerformanceMonitor extends JPanel {

    Surface surf;

    public PerformanceMonitor() {
        setLayout(new BorderLayout());
        setBorder(new TitledBorder(new EtchedBorder(), "Performance"));
        add(surf = new Surface());
    }


    public class Surface extends JPanel implements Runnable {

        public Thread thread;
        private BufferedImage bimg;
        private Font font = new Font(Font.SERIF, Font.PLAIN, 12);
        private JPanel panel;

        public Surface() {
            setBackground(Color.black);
            addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (thread == null) {
                        start();
                    } else {
                        stop();
                    }
                }
            });
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getPreferredSize() {
            int textH = getFontMetrics(font).getHeight();
            return new Dimension(135, 2 + textH * 4);
        }

        @Override
        public void paint(Graphics g) {
            if (bimg != null) {
                g.drawImage(bimg, 0, 0, this);
            }
        }

        public void start() {
            thread = new Thread(this);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setName("PerformanceMonitor");
            thread.start();
        }

        public synchronized void stop() {
            thread = null;
            setSurfaceState();
            notify();
        }

        public void setSurfaceState() {
            if (panel != null) {
                for (Component comp : panel.getComponents()) {
                    if (((DemoPanel) comp).surface != null) {
                        ((DemoPanel) comp).surface.setMonitor(thread != null);
                    }
                }
            }
        }

        public void setPanel(JPanel panel) {
            this.panel = panel;
        }

        @Override
        @SuppressWarnings("SleepWhileHoldingLock")
        public void run() {

            Thread me = Thread.currentThread();

            while (thread == me && !isShowing() || getSize().width == 0) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }
            }

            Dimension d = new Dimension(0, 0);
            Graphics2D big = null;
            FontMetrics fm = null;
            int ascent = 0;
            int descent = 0;

            while (thread == me && isShowing()) {

                if (getWidth() != d.width || getHeight() != d.height) {
                    d = getSize();
                    bimg = (BufferedImage) createImage(d.width, d.height);
                    big = bimg.createGraphics();
                    big.setFont(font);
                    fm = big.getFontMetrics();
                    ascent = fm.getAscent();
                    descent = fm.getDescent();
                    setSurfaceState();
                }

                big.setBackground(getBackground());
                big.clearRect(0, 0, d.width, d.height);
                if (panel == null) {
                    continue;
                }
                big.setColor(Color.green);
                int ssH = 1;
                for (Component comp : panel.getComponents()) {
                    if (((DemoPanel) comp).surface != null) {
                        String pStr = ((DemoPanel) comp).surface.perfStr;
                        if (pStr != null) {
                            ssH += ascent;
                            big.drawString(pStr, 4, ssH + 1);
                            ssH += descent;
                        }
                    }
                }
                repaint();

                try {
                    Thread.sleep(999);
                } catch (InterruptedException e) {
                    break;
                }
            }
            thread = null;
        }
    } // End Surface
} // End PeformanceMonitor

