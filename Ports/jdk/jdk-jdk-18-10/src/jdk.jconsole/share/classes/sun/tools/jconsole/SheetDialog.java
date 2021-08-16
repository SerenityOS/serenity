/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.tools.jconsole;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;

import static javax.swing.JOptionPane.*;

@SuppressWarnings("serial")
public final class SheetDialog {
    // Reusable objects
    private static Rectangle iconR = new Rectangle();
    private static Rectangle textR = new Rectangle();
    private static Rectangle viewR = new Rectangle();
    private static Insets viewInsets = new Insets(0, 0, 0, 0);

    /** Don't let anyone instantiate this class */
    private SheetDialog() {
    }

    static JOptionPane showOptionDialog(final VMPanel vmPanel, Object message,
                                        int optionType, int messageType,
                                        Icon icon, Object[] options, Object initialValue) {

        JRootPane rootPane = SwingUtilities.getRootPane(vmPanel);
        JPanel glassPane = (JPanel)rootPane.getGlassPane();

        if (!(glassPane instanceof SlideAndFadeGlassPane)) {
            glassPane = new SlideAndFadeGlassPane();
            glassPane.setName(rootPane.getName()+".glassPane");
            rootPane.setGlassPane(glassPane);
            rootPane.revalidate();
        }

        final SlideAndFadeGlassPane safGlassPane = (SlideAndFadeGlassPane)glassPane;

        // Workaround for the fact that JOptionPane does not handle
        // limiting the width when using multi-line html messages.
        // See Swing bug 5074006 and JConsole bug 6426317
        message = fixWrapping(message, rootPane.getWidth() - 75); // Leave room for icon

        final SheetOptionPane optionPane = new SheetOptionPane(message, messageType, optionType,
                                                           icon, options, initialValue);

        optionPane.setComponentOrientation(vmPanel.getComponentOrientation());
        optionPane.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                if (event.getPropertyName().equals(VALUE_PROPERTY) &&
                    event.getNewValue() != null &&
                    event.getNewValue() != UNINITIALIZED_VALUE) {
                    ((SlideAndFadeGlassPane)optionPane.getParent()).hide(optionPane);
                }
            }
        });

        // Delay this (even though we're already on the EDT)
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                safGlassPane.show(optionPane);
            }
        });

        return optionPane;
    }

    private static Object fixWrapping(Object message, final int maxWidth) {
        if (message instanceof Object[]) {
            Object[] arr = (Object[])message;
            for (int i = 0; i < arr.length; i++) {
                arr[i] = fixWrapping(arr[i], maxWidth);
            }
        } else if (message instanceof String &&
                   ((String)message).startsWith("<html>")) {
            message = new JLabel((String)message) {
                public Dimension getPreferredSize() {
                    String text = getText();
                    Insets insets = getInsets(viewInsets);
                    FontMetrics fm = getFontMetrics(getFont());
                    Dimension pref = super.getPreferredSize();
                    Dimension min = getMinimumSize();

                    iconR.x = iconR.y = iconR.width = iconR.height = 0;
                    textR.x = textR.y = textR.width = textR.height = 0;
                    int dx = insets.left + insets.right;
                    int dy = insets.top + insets.bottom;
                    viewR.x = dx;
                    viewR.y = dy;
                    viewR.width = viewR.height = Short.MAX_VALUE;

                    View v = (View)getClientProperty("html");
                    if (v != null) {
                        // Use pref width if less than 300, otherwise
                        // min width up to size of window.
                        int w = Math.min(maxWidth,
                                         Math.min(pref.width,
                                                  Math.max(min.width, 300)));
                        v.setSize((float)w, 0F);

                        SwingUtilities.layoutCompoundLabel(this, fm, text, null,
                                                           getVerticalAlignment(),
                                                           getHorizontalAlignment(),
                                                           getVerticalTextPosition(),
                                                           getHorizontalTextPosition(),
                                                           viewR, iconR, textR,
                                                           getIconTextGap());
                        return new Dimension(textR.width + dx,
                                             textR.height + dy);
                    } else {
                        return pref; //  Should not happen
                    }
                }
            };
        }
        return message;
    }

    private static class SlideAndFadeGlassPane extends JPanel {
        SheetOptionPane optionPane;

        int fade = 20;
        boolean slideIn = true;

        SlideAndFadeGlassPane() {
            super(null);
            setVisible(false);
            setOpaque(false);

            // Grab mouse input, making the dialog modal
            addMouseListener(new MouseAdapter() {});
        }

        public void show(SheetOptionPane optionPane) {
            this.optionPane = optionPane;
            removeAll();
            add(optionPane);
            setVisible(true);
            slideIn = true;
            revalidate();
            repaint();
            doSlide();
        }

        public void hide(SheetOptionPane optionPane) {
            if (optionPane != this.optionPane) {
                return;
            }

            slideIn = false;
            revalidate();
            repaint();
            doSlide();
        }

        private void doSlide() {
            if (optionPane.getParent() == null) {
                return;
            }

            if (optionPane.getWidth() == 0) {
                optionPane.setSize(optionPane.getPreferredSize());
            }

            int glassPaneWidth = getWidth();
            if (glassPaneWidth == 0 && getParent() != null) {
                glassPaneWidth = getParent().getWidth();
            }

            int x = (glassPaneWidth - optionPane.getWidth()) / 2;

            if (!slideIn) {
                    remove(optionPane);
                    setVisible(false);
                    return;
            } else {
                    optionPane.setLocation(x, 0);
                    setGrayLevel(fade);
                    return;
            }
        }

        public void setGrayLevel(int gray) {
            gray = gray * 255 / 100;
            setBackground(new Color(0, 0, 0, gray));
        }

        public void paint(Graphics g) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            super.paint(g);
        }
    }



    static class SheetOptionPane extends JOptionPane {
        SheetOptionPane(Object message, int messageType, int optionType,
                        Icon icon, Object[] options, Object initialValue) {
            super(message, messageType, optionType, icon, options, initialValue);

            setBorder(new CompoundBorder(new LineBorder(new Color(204, 204, 204), 1),
                                         new EmptyBorder(4, 4, 4, 4)));
        }


        private static Composite comp =
            AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8F);

        private static Color bgColor = new Color(241, 239, 239);

        public void setVisible(boolean visible) {
            SlideAndFadeGlassPane glassPane = (SlideAndFadeGlassPane)getParent();
            if (glassPane != null) {
                if (visible) {
                    glassPane.show(this);
                } else {
                    glassPane.hide(this);
                }
            }
        }

        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D)g;
            Composite oldComp = g2d.getComposite();
            g2d.setComposite(comp);
            Color oldColor = g2d.getColor();
            g2d.setColor(bgColor);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setColor(oldColor);
            g2d.setComposite(oldComp);
            super.paint(g);
        }
    }

}
