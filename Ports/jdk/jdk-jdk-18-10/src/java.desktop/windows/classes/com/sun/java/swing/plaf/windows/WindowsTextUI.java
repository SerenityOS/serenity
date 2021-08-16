/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.java.swing.plaf.windows;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.plaf.TextUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicTextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.Position;
import javax.swing.text.View;

/**
 * Windows text rendering.
 */
public abstract class WindowsTextUI extends BasicTextUI {
    /**
     * Creates the object to use for a caret.  By default an
     * instance of WindowsCaret is created.  This method
     * can be redefined to provide something else that implements
     * the InputPosition interface or a subclass of DefaultCaret.
     *
     * @return the caret object
     */
    protected Caret createCaret() {
        return new WindowsCaret();
    }

    /* public */
    static LayeredHighlighter.LayerPainter WindowsPainter = new WindowsHighlightPainter(null);

    /* public */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    static class WindowsCaret extends DefaultCaret
                     implements UIResource {
        /**
         * Gets the painter for the Highlighter.
         *
         * @return the painter
         */
        protected Highlighter.HighlightPainter getSelectionPainter() {
            return WindowsTextUI.WindowsPainter;
        }
    }

    /* public */
    static class WindowsHighlightPainter extends
                     DefaultHighlighter.DefaultHighlightPainter {
        WindowsHighlightPainter(Color c) {
            super(c);
        }

        // --- HighlightPainter methods ---------------------------------------

        /**
         * Paints a highlight.
         *
         * @param g the graphics context
         * @param offs0 the starting model offset >= 0
         * @param offs1 the ending model offset >= offs1
         * @param bounds the bounding box for the highlight
         * @param c the editor
         */
        @SuppressWarnings("deprecation")
        public void paint(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c) {
            Rectangle alloc = bounds.getBounds();
            try {
                // --- determine locations ---
                TextUI mapper = c.getUI();
                Rectangle p0 = mapper.modelToView(c, offs0);
                Rectangle p1 = mapper.modelToView(c, offs1);

                // --- render ---
                Color color = getColor();

                if (color == null) {
                    g.setColor(c.getSelectionColor());
                }
                else {
                    g.setColor(color);
                }
                boolean firstIsDot = false;
                boolean secondIsDot = false;
                if (c.isEditable()) {
                    int dot = c.getCaretPosition();
                    firstIsDot = (offs0 == dot);
                    secondIsDot = (offs1 == dot);
                }
                if (p0.y == p1.y) {
                    // same line, render a rectangle
                    Rectangle r = p0.union(p1);
                    if (r.width > 0) {
                        if (firstIsDot) {
                            r.x++;
                            r.width--;
                        }
                        else if (secondIsDot) {
                            r.width--;
                        }
                    }
                    g.fillRect(r.x, r.y, r.width, r.height);
                } else {
                    // different lines
                    int p0ToMarginWidth = alloc.x + alloc.width - p0.x;
                    if (firstIsDot && p0ToMarginWidth > 0) {
                        p0.x++;
                        p0ToMarginWidth--;
                    }
                    g.fillRect(p0.x, p0.y, p0ToMarginWidth, p0.height);
                    if ((p0.y + p0.height) != p1.y) {
                        g.fillRect(alloc.x, p0.y + p0.height, alloc.width,
                                   p1.y - (p0.y + p0.height));
                    }
                    if (secondIsDot && p1.x > alloc.x) {
                        p1.x--;
                    }
                    g.fillRect(alloc.x, p1.y, (p1.x - alloc.x), p1.height);
                }
            } catch (BadLocationException e) {
                // can't render
            }
        }

        // --- LayerPainter methods ----------------------------
        /**
         * Paints a portion of a highlight.
         *
         * @param g the graphics context
         * @param offs0 the starting model offset >= 0
         * @param offs1 the ending model offset >= offs1
         * @param bounds the bounding box of the view, which is not
         *        necessarily the region to paint.
         * @param c the editor
         * @param view View painting for
         * @return region drawing occurred in
         */
        public Shape paintLayer(Graphics g, int offs0, int offs1,
                                Shape bounds, JTextComponent c, View view) {
            Color color = getColor();

            if (color == null) {
                g.setColor(c.getSelectionColor());
            }
            else {
                g.setColor(color);
            }
            boolean firstIsDot = false;
            boolean secondIsDot = false;
            if (c.isEditable()) {
                int dot = c.getCaretPosition();
                firstIsDot = (offs0 == dot);
                secondIsDot = (offs1 == dot);
            }
            if (offs0 == view.getStartOffset() &&
                offs1 == view.getEndOffset()) {
                // Contained in view, can just use bounds.
                Rectangle alloc;
                if (bounds instanceof Rectangle) {
                    alloc = (Rectangle)bounds;
                }
                else {
                    alloc = bounds.getBounds();
                }
                if (firstIsDot && alloc.width > 0) {
                    g.fillRect(alloc.x + 1, alloc.y, alloc.width - 1,
                               alloc.height);
                }
                else if (secondIsDot && alloc.width > 0) {
                    g.fillRect(alloc.x, alloc.y, alloc.width - 1,
                               alloc.height);
                }
                else {
                    g.fillRect(alloc.x, alloc.y, alloc.width, alloc.height);
                }
                return alloc;
            }
            else {
                // Should only render part of View.
                try {
                    // --- determine locations ---
                    Shape shape = view.modelToView(offs0, Position.Bias.Forward,
                                                   offs1,Position.Bias.Backward,
                                                   bounds);
                    Rectangle r = (shape instanceof Rectangle) ?
                                  (Rectangle)shape : shape.getBounds();
                    if (firstIsDot && r.width > 0) {
                        g.fillRect(r.x + 1, r.y, r.width - 1, r.height);
                    }
                    else if (secondIsDot && r.width > 0) {
                        g.fillRect(r.x, r.y, r.width - 1, r.height);
                    }
                    else {
                        g.fillRect(r.x, r.y, r.width, r.height);
                    }
                    return r;
                } catch (BadLocationException e) {
                    // can't render
                }
            }
            // Only if exception
            return null;
        }

    }

}
