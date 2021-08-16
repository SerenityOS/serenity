/*
 * Copyright (c) 1997, 2015, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text;

import java.util.Vector;
import java.awt.*;
import javax.swing.plaf.*;
import javax.swing.*;

/**
 * Implements the Highlighter interfaces.  Implements a simple highlight
 * painter that renders in a solid color.
 *
 * @author  Timothy Prinzing
 * @see     Highlighter
 */
public class DefaultHighlighter extends LayeredHighlighter {

    /**
     * Creates a new DefaultHighlighther object.
     */
    public DefaultHighlighter() {
        drawsLayeredHighlights = true;
    }

    // ---- Highlighter methods ----------------------------------------------

    /**
     * Renders the highlights.
     *
     * @param g the graphics context
     */
    public void paint(Graphics g) {
        // PENDING(prinz) - should cull ranges not visible
        int len = highlights.size();
        for (int i = 0; i < len; i++) {
            HighlightInfo info = highlights.elementAt(i);
            if (!(info instanceof LayeredHighlightInfo)) {
                // Avoid allocing unless we need it.
                Rectangle a = component.getBounds();
                Insets insets = component.getInsets();
                a.x = insets.left;
                a.y = insets.top;
                a.width -= insets.left + insets.right;
                a.height -= insets.top + insets.bottom;
                for (; i < len; i++) {
                    info = highlights.elementAt(i);
                    if (!(info instanceof LayeredHighlightInfo)) {
                        Highlighter.HighlightPainter p = info.getPainter();
                        p.paint(g, info.getStartOffset(), info.getEndOffset(),
                                a, component);
                    }
                }
            }
        }
    }

    /**
     * Called when the UI is being installed into the
     * interface of a JTextComponent.  Installs the editor, and
     * removes any existing highlights.
     *
     * @param c the editor component
     * @see Highlighter#install
     */
    public void install(JTextComponent c) {
        component = c;
        removeAllHighlights();
    }

    /**
     * Called when the UI is being removed from the interface of
     * a JTextComponent.
     *
     * @param c the component
     * @see Highlighter#deinstall
     */
    public void deinstall(JTextComponent c) {
        component = null;
    }

    /**
     * Adds a highlight to the view.  Returns a tag that can be used
     * to refer to the highlight.
     *
     * @param p0   the start offset of the range to highlight &gt;= 0
     * @param p1   the end offset of the range to highlight &gt;= p0
     * @param p    the painter to use to actually render the highlight
     * @return     an object that can be used as a tag
     *   to refer to the highlight
     * @exception BadLocationException if the specified location is invalid
     */
    public Object addHighlight(int p0, int p1, Highlighter.HighlightPainter p) throws BadLocationException {
        if (p0 < 0) {
            throw new BadLocationException("Invalid start offset", p0);
        }

        if (p1 < p0) {
            throw new BadLocationException("Invalid end offset", p1);
        }

        Document doc = component.getDocument();
        HighlightInfo i = (getDrawsLayeredHighlights() &&
                           (p instanceof LayeredHighlighter.LayerPainter)) ?
                          new LayeredHighlightInfo() : new HighlightInfo();
        i.painter = p;
        i.p0 = doc.createPosition(p0);
        i.p1 = doc.createPosition(p1);
        highlights.addElement(i);
        safeDamageRange(p0, p1);
        return i;
    }

    /**
     * Removes a highlight from the view.
     *
     * @param tag the reference to the highlight
     */
    public void removeHighlight(Object tag) {
        if (tag instanceof LayeredHighlightInfo) {
            LayeredHighlightInfo lhi = (LayeredHighlightInfo)tag;
            if (lhi.width > 0 && lhi.height > 0) {
                component.repaint(lhi.x, lhi.y, lhi.width, lhi.height);
            }
        }
        else {
            HighlightInfo info = (HighlightInfo) tag;
            safeDamageRange(info.p0, info.p1);
        }
        highlights.removeElement(tag);
    }

    /**
     * Removes all highlights.
     */
    public void removeAllHighlights() {
        TextUI mapper = component.getUI();
        if (getDrawsLayeredHighlights()) {
            int len = highlights.size();
            if (len != 0) {
                int minX = 0;
                int minY = 0;
                int maxX = 0;
                int maxY = 0;
                int p0 = -1;
                int p1 = -1;
                for (int i = 0; i < len; i++) {
                    HighlightInfo hi = highlights.elementAt(i);
                    if (hi instanceof LayeredHighlightInfo) {
                        LayeredHighlightInfo info = (LayeredHighlightInfo)hi;
                        minX = Math.min(minX, info.x);
                        minY = Math.min(minY, info.y);
                        maxX = Math.max(maxX, info.x + info.width);
                        maxY = Math.max(maxY, info.y + info.height);
                    }
                    else {
                        if (p0 == -1) {
                            p0 = hi.p0.getOffset();
                            p1 = hi.p1.getOffset();
                        }
                        else {
                            p0 = Math.min(p0, hi.p0.getOffset());
                            p1 = Math.max(p1, hi.p1.getOffset());
                        }
                    }
                }
                if (minX != maxX && minY != maxY) {
                    component.repaint(minX, minY, maxX - minX, maxY - minY);
                }
                if (p0 != -1) {
                    try {
                        safeDamageRange(p0, p1);
                    } catch (BadLocationException e) {}
                }
                highlights.removeAllElements();
            }
        }
        else if (mapper != null) {
            int len = highlights.size();
            if (len != 0) {
                int p0 = Integer.MAX_VALUE;
                int p1 = 0;
                for (int i = 0; i < len; i++) {
                    HighlightInfo info = highlights.elementAt(i);
                    p0 = Math.min(p0, info.p0.getOffset());
                    p1 = Math.max(p1, info.p1.getOffset());
                }
                try {
                    safeDamageRange(p0, p1);
                } catch (BadLocationException e) {}

                highlights.removeAllElements();
            }
        }
    }

    /**
     * Changes a highlight.
     *
     * @param tag the highlight tag
     * @param p0 the beginning of the range &gt;= 0
     * @param p1 the end of the range &gt;= p0
     * @exception BadLocationException if the specified location is invalid
     */
    public void changeHighlight(Object tag, int p0, int p1) throws BadLocationException {
        if (p0 < 0) {
            throw new BadLocationException("Invalid beginning of the range", p0);
        }

        if (p1 < p0) {
            throw new BadLocationException("Invalid end of the range", p1);
        }

        Document doc = component.getDocument();
        if (tag instanceof LayeredHighlightInfo) {
            LayeredHighlightInfo lhi = (LayeredHighlightInfo)tag;
            if (lhi.width > 0 && lhi.height > 0) {
                component.repaint(lhi.x, lhi.y, lhi.width, lhi.height);
            }
            // Mark the highlights region as invalid, it will reset itself
            // next time asked to paint.
            lhi.width = lhi.height = 0;
            lhi.p0 = doc.createPosition(p0);
            lhi.p1 = doc.createPosition(p1);
            safeDamageRange(Math.min(p0, p1), Math.max(p0, p1));
        }
        else {
            HighlightInfo info = (HighlightInfo) tag;
            int oldP0 = info.p0.getOffset();
            int oldP1 = info.p1.getOffset();
            if (p0 == oldP0) {
                safeDamageRange(Math.min(oldP1, p1),
                                   Math.max(oldP1, p1));
            } else if (p1 == oldP1) {
                safeDamageRange(Math.min(p0, oldP0),
                                   Math.max(p0, oldP0));
            } else {
                safeDamageRange(oldP0, oldP1);
                safeDamageRange(p0, p1);
            }
            info.p0 = doc.createPosition(p0);
            info.p1 = doc.createPosition(p1);
        }
    }

    /**
     * Makes a copy of the highlights.  Does not actually clone each highlight,
     * but only makes references to them.
     *
     * @return the copy
     * @see Highlighter#getHighlights
     */
    public Highlighter.Highlight[] getHighlights() {
        int size = highlights.size();
        if (size == 0) {
            return noHighlights;
        }
        Highlighter.Highlight[] h = new Highlighter.Highlight[size];
        highlights.copyInto(h);
        return h;
    }

    /**
     * When leaf Views (such as LabelView) are rendering they should
     * call into this method. If a highlight is in the given region it will
     * be drawn immediately.
     *
     * @param g Graphics used to draw
     * @param p0 starting offset of view
     * @param p1 ending offset of view
     * @param viewBounds Bounds of View
     * @param editor JTextComponent
     * @param view View instance being rendered
     */
    public void paintLayeredHighlights(Graphics g, int p0, int p1,
                                       Shape viewBounds,
                                       JTextComponent editor, View view) {
        for (int counter = highlights.size() - 1; counter >= 0; counter--) {
            HighlightInfo tag = highlights.elementAt(counter);
            if (tag instanceof LayeredHighlightInfo) {
                LayeredHighlightInfo lhi = (LayeredHighlightInfo)tag;
                int start = lhi.getStartOffset();
                int end = lhi.getEndOffset();
                if ((p0 < start && p1 > start) ||
                    (p0 >= start && p0 < end)) {
                    lhi.paintLayeredHighlights(g, p0, p1, viewBounds,
                                               editor, view);
                }
            }
        }
    }

    /**
     * Queues damageRange() call into event dispatch thread
     * to be sure that views are in consistent state.
     */
    private void safeDamageRange(final Position p0, final Position p1) {
        safeDamager.damageRange(p0, p1);
    }

    /**
     * Queues damageRange() call into event dispatch thread
     * to be sure that views are in consistent state.
     */
    private void safeDamageRange(int a0, int a1) throws BadLocationException {
        Document doc = component.getDocument();
        safeDamageRange(doc.createPosition(a0), doc.createPosition(a1));
    }

    /**
     * If true, highlights are drawn as the Views draw the text. That is
     * the Views will call into <code>paintLayeredHighlight</code> which
     * will result in a rectangle being drawn before the text is drawn
     * (if the offsets are in a highlighted region that is). For this to
     * work the painter supplied must be an instance of
     * LayeredHighlightPainter.
     * @param newValue the new value
     */
    public void setDrawsLayeredHighlights(boolean newValue) {
        drawsLayeredHighlights = newValue;
    }

    /**
     * Return the draw layered highlights.
     * @return the draw layered highlights
     */
    public boolean getDrawsLayeredHighlights() {
        return drawsLayeredHighlights;
    }

    // ---- member variables --------------------------------------------

    private static final Highlighter.Highlight[] noHighlights =
            new Highlighter.Highlight[0];
    private Vector<HighlightInfo> highlights = new Vector<HighlightInfo>();
    private JTextComponent component;
    private boolean drawsLayeredHighlights;
    private SafeDamager safeDamager = new SafeDamager();


    /**
     * Default implementation of LayeredHighlighter.LayerPainter that can
     * be used for painting highlights.
     * <p>
     * As of 1.4 this field is final.
     */
    public static final LayeredHighlighter.LayerPainter DefaultPainter = new DefaultHighlightPainter(null);


    /**
     * Simple highlight painter that fills a highlighted area with
     * a solid color.
     */
    public static class DefaultHighlightPainter extends LayeredHighlighter.LayerPainter {

        /**
         * Constructs a new highlight painter. If <code>c</code> is null,
         * the JTextComponent will be queried for its selection color.
         *
         * @param c the color for the highlight
         */
        public DefaultHighlightPainter(Color c) {
            color = c;
        }

        /**
         * Returns the color of the highlight.
         *
         * @return the color
         */
        public Color getColor() {
            return color;
        }

        // --- HighlightPainter methods ---------------------------------------

        /**
         * Paints a highlight.
         *
         * @param g the graphics context
         * @param offs0 the starting model offset &gt;= 0
         * @param offs1 the ending model offset &gt;= offs1
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
                if (p0.y == p1.y) {
                    // same line, render a rectangle
                    Rectangle r = p0.union(p1);
                    g.fillRect(r.x, r.y, r.width, r.height);
                } else {
                    // different lines
                    int p0ToMarginWidth = alloc.x + alloc.width - p0.x;
                    g.fillRect(p0.x, p0.y, p0ToMarginWidth, p0.height);
                    if ((p0.y + p0.height) != p1.y) {
                        g.fillRect(alloc.x, p0.y + p0.height, alloc.width,
                                   p1.y - (p0.y + p0.height));
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
         * @param offs0 the starting model offset &gt;= 0
         * @param offs1 the ending model offset &gt;= offs1
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

            Rectangle r;

            if (offs0 == view.getStartOffset() &&
                offs1 == view.getEndOffset()) {
                // Contained in view, can just use bounds.
                if (bounds instanceof Rectangle) {
                    r = (Rectangle) bounds;
                }
                else {
                    r = bounds.getBounds();
                }
            }
            else {
                // Should only render part of View.
                try {
                    // --- determine locations ---
                    Shape shape = view.modelToView(offs0, Position.Bias.Forward,
                                                   offs1,Position.Bias.Backward,
                                                   bounds);
                    r = (shape instanceof Rectangle) ?
                                  (Rectangle)shape : shape.getBounds();
                } catch (BadLocationException e) {
                    // can't render
                    r = null;
                }
            }

            if (r != null) {
                // If we are asked to highlight, we should draw something even
                // if the model-to-view projection is of zero width (6340106).
                r.width = Math.max(r.width, 1);

                g.fillRect(r.x, r.y, r.width, r.height);
            }

            return r;
        }

        private Color color;

    }


    class HighlightInfo implements Highlighter.Highlight {

        public int getStartOffset() {
            return p0.getOffset();
        }

        public int getEndOffset() {
            return p1.getOffset();
        }

        public Highlighter.HighlightPainter getPainter() {
            return painter;
        }

        Position p0;
        Position p1;
        Highlighter.HighlightPainter painter;
    }


    /**
     * LayeredHighlightPainter is used when a drawsLayeredHighlights is
     * true. It maintains a rectangle of the region to paint.
     */
    class LayeredHighlightInfo extends HighlightInfo {

        void union(Shape bounds) {
            if (bounds == null)
                return;

            Rectangle alloc;
            if (bounds instanceof Rectangle) {
                alloc = (Rectangle)bounds;
            }
            else {
                alloc = bounds.getBounds();
            }
            if (width == 0 || height == 0) {
                x = alloc.x;
                y = alloc.y;
                width = alloc.width;
                height = alloc.height;
            }
            else {
                width = Math.max(x + width, alloc.x + alloc.width);
                height = Math.max(y + height, alloc.y + alloc.height);
                x = Math.min(x, alloc.x);
                width -= x;
                y = Math.min(y, alloc.y);
                height -= y;
            }
        }

        /**
         * Restricts the region based on the receivers offsets and messages
         * the painter to paint the region.
         */
        void paintLayeredHighlights(Graphics g, int p0, int p1,
                                    Shape viewBounds, JTextComponent editor,
                                    View view) {
            int start = getStartOffset();
            int end = getEndOffset();
            // Restrict the region to what we represent
            p0 = Math.max(start, p0);
            p1 = Math.min(end, p1);
            // Paint the appropriate region using the painter and union
            // the effected region with our bounds.
            union(((LayeredHighlighter.LayerPainter)painter).paintLayer
                  (g, p0, p1, viewBounds, editor, view));
        }

        int x;
        int y;
        int width;
        int height;
    }

    /**
     * This class invokes <code>mapper.damageRange</code> in
     * EventDispatchThread. The only one instance per Highlighter
     * is cretaed. When a number of ranges should be damaged
     * it collects them into queue and damages
     * them in consecutive order in <code>run</code>
     * call.
     */
    class SafeDamager implements Runnable {
        private Vector<Position> p0 = new Vector<Position>(10);
        private Vector<Position> p1 = new Vector<Position>(10);
        private Document lastDoc = null;

        /**
         * Executes range(s) damage and cleans range queue.
         */
        public synchronized void run() {
            if (component != null) {
                TextUI mapper = component.getUI();
                if (mapper != null && lastDoc == component.getDocument()) {
                    // the Document should be the same to properly
                    // display highlights
                    int len = p0.size();
                    for (int i = 0; i < len; i++){
                        mapper.damageRange(component,
                                p0.get(i).getOffset(),
                                p1.get(i).getOffset());
                    }
                }
            }
            p0.clear();
            p1.clear();

            // release reference
            lastDoc = null;
        }

        /**
         * Adds the range to be damaged into the range queue. If the
         * range queue is empty (the first call or run() was already
         * invoked) then adds this class instance into EventDispatch
         * queue.
         *
         * The method also tracks if the current document changed or
         * component is null. In this case it removes all ranges added
         * before from range queue.
         */
        public synchronized void damageRange(Position pos0, Position pos1) {
            if (component == null) {
                p0.clear();
                lastDoc = null;
                return;
            }

            boolean addToQueue = p0.isEmpty();
            Document curDoc = component.getDocument();
            if (curDoc != lastDoc) {
                if (!p0.isEmpty()) {
                    p0.clear();
                    p1.clear();
                }
                lastDoc = curDoc;
            }
            p0.add(pos0);
            p1.add(pos1);

            if (addToQueue) {
                SwingUtilities.invokeLater(this);
            }
        }
    }
}
