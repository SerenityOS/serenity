/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.PaintEvent;

/**
 * The {@code RepaintArea} is a geometric construct created for the
 * purpose of holding the geometry of several coalesced paint events.
 * This geometry is accessed synchronously, although it is written such
 * that painting may still be executed asynchronously.
 *
 * @author      Eric Hawkes
 * @since       1.3
 */
public class RepaintArea {

    /**
     * Maximum ratio of bounding rectangle to benefit for which
     * both the vertical and horizontal unions are repainted.
     * For smaller ratios the whole bounding rectangle is repainted.
     * @see #paint
     */
    private static final int MAX_BENEFIT_RATIO = 4;

    private static final int HORIZONTAL = 0;
    private static final int VERTICAL = 1;
    private static final int UPDATE = 2;

    private static final int RECT_COUNT = UPDATE + 1;

    private Rectangle[] paintRects = new Rectangle[RECT_COUNT];


    /**
     * Constructs a new {@code RepaintArea}
     * @since   1.3
     */
    public RepaintArea() {
    }

    /**
     * Constructs a new {@code RepaintArea} initialized to match
     * the values of the specified RepaintArea.
     *
     * @param   ra  the {@code RepaintArea} from which to copy initial
     *              values to a newly constructed RepaintArea
     * @since   1.3
     */
    private RepaintArea(RepaintArea ra) {
        // This constructor is private because it should only be called
        // from the cloneAndReset method
        for (int i = 0; i < RECT_COUNT; i++) {
            paintRects[i] = ra.paintRects[i];
        }
    }

    /**
     * Adds a {@code Rectangle} to this {@code RepaintArea}.
     * PAINT Rectangles are divided into mostly vertical and mostly horizontal.
     * Each group is unioned together.
     * UPDATE Rectangles are unioned.
     *
     * @param   r   the specified {@code Rectangle}
     * @param   id  possible values PaintEvent.UPDATE or PaintEvent.PAINT
     * @since   1.3
     */
    public synchronized void add(Rectangle r, int id) {
        // Make sure this new rectangle has positive dimensions
        if (r.isEmpty()) {
            return;
        }
        int addTo = UPDATE;
        if (id == PaintEvent.PAINT) {
            addTo = (r.width > r.height) ? HORIZONTAL : VERTICAL;
        }
        if (paintRects[addTo] != null) {
            paintRects[addTo].add(r);
        } else {
            paintRects[addTo] = new Rectangle(r);
        }
    }


    /**
     * Creates a new {@code RepaintArea} with the same geometry as this
     * RepaintArea, then removes all of the geometry from this
     * RepaintArea and restores it to an empty RepaintArea.
     *
     * @return  ra a new {@code RepaintArea} having the same geometry as
     *          this RepaintArea.
     * @since   1.3
     */
    private synchronized RepaintArea cloneAndReset() {
        RepaintArea ra = new RepaintArea(this);
        for (int i = 0; i < RECT_COUNT; i++) {
            paintRects[i] = null;
        }
        return ra;
    }

    public boolean isEmpty() {
        for (int i = 0; i < RECT_COUNT; i++) {
            if (paintRects[i] != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Constrains the size of the repaint area to the passed in bounds.
     */
    public synchronized void constrain(int x, int y, int w, int h) {
        for (int i = 0; i < RECT_COUNT; i++) {
            Rectangle rect = paintRects[i];
            if (rect != null) {
                if (rect.x < x) {
                    rect.width -= (x - rect.x);
                    rect.x = x;
                }
                if (rect.y < y) {
                    rect.height -= (y - rect.y);
                    rect.y = y;
                }
                int xDelta = rect.x + rect.width - x - w;
                if (xDelta > 0) {
                    rect.width -= xDelta;
                }
                int yDelta = rect.y + rect.height - y - h;
                if (yDelta > 0) {
                    rect.height -= yDelta;
                }
                if (rect.width <= 0 || rect.height <= 0) {
                    paintRects[i] = null;
                }
            }
        }
    }

    /**
     * Marks the passed in region as not needing to be painted. It's possible
     * this will do nothing.
     */
    public synchronized void subtract(int x, int y, int w, int h) {
        Rectangle subtract = new Rectangle(x, y, w, h);
        for (int i = 0; i < RECT_COUNT; i++) {
            if (subtract(paintRects[i], subtract)) {
                if (paintRects[i] != null && paintRects[i].isEmpty()) {
                    paintRects[i] = null;
                }
            }
        }
    }

    /**
     * Invokes paint and update on target Component with optimal
     * rectangular clip region.
     * If PAINT bounding rectangle is less than
     * MAX_BENEFIT_RATIO times the benefit, then the vertical and horizontal unions are
     * painted separately.  Otherwise the entire bounding rectangle is painted.
     *
     * @param   target Component to {@code paint} or {@code update}
     * @since   1.4
     */
    public void paint(Object target, boolean shouldClearRectBeforePaint) {
        Component comp = (Component)target;

        if (isEmpty()) {
            return;
        }

        if (!comp.isVisible()) {
            return;
        }

        RepaintArea ra = this.cloneAndReset();

        if (!subtract(ra.paintRects[VERTICAL], ra.paintRects[HORIZONTAL])) {
            subtract(ra.paintRects[HORIZONTAL], ra.paintRects[VERTICAL]);
        }

        if (ra.paintRects[HORIZONTAL] != null && ra.paintRects[VERTICAL] != null) {
            Rectangle paintRect = ra.paintRects[HORIZONTAL].union(ra.paintRects[VERTICAL]);
            int square = paintRect.width * paintRect.height;
            int benefit = square - ra.paintRects[HORIZONTAL].width
                * ra.paintRects[HORIZONTAL].height - ra.paintRects[VERTICAL].width
                * ra.paintRects[VERTICAL].height;
            // if benefit is comparable with bounding box
            if (MAX_BENEFIT_RATIO * benefit < square) {
                ra.paintRects[HORIZONTAL] = paintRect;
                ra.paintRects[VERTICAL] = null;
            }
        }
        for (int i = 0; i < paintRects.length; i++) {
            if (ra.paintRects[i] != null
                && !ra.paintRects[i].isEmpty())
            {
                // Should use separate Graphics for each paint() call,
                // since paint() can change Graphics state for next call.
                Graphics g = comp.getGraphics();
                if (g != null) {
                    try {
                        g.setClip(ra.paintRects[i]);
                        if (i == UPDATE) {
                            updateComponent(comp, g);
                        } else {
                            if (shouldClearRectBeforePaint) {
                                g.clearRect( ra.paintRects[i].x,
                                             ra.paintRects[i].y,
                                             ra.paintRects[i].width,
                                             ra.paintRects[i].height);
                            }
                            paintComponent(comp, g);
                        }
                    } finally {
                        g.dispose();
                    }
                }
            }
        }
    }

    /**
     * Calls {@code Component.update(Graphics)} with given Graphics.
     */
    protected void updateComponent(Component comp, Graphics g) {
        if (comp != null) {
            comp.update(g);
        }
    }

    /**
     * Calls {@code Component.paint(Graphics)} with given Graphics.
     */
    protected void paintComponent(Component comp, Graphics g) {
        if (comp != null) {
            comp.paint(g);
        }
    }

    /**
     * Subtracts subtr from rect. If the result is rectangle
     * changes rect and returns true. Otherwise false.
     */
    static boolean subtract(Rectangle rect, Rectangle subtr) {
        if (rect == null || subtr == null) {
            return true;
        }
        Rectangle common = rect.intersection(subtr);
        if (common.isEmpty()) {
            return true;
        }
        if (rect.x == common.x && rect.y == common.y) {
            if (rect.width == common.width) {
                rect.y += common.height;
                rect.height -= common.height;
                return true;
            } else
            if (rect.height == common.height) {
                rect.x += common.width;
                rect.width -= common.width;
                return true;
            }
        } else
        if (rect.x + rect.width == common.x + common.width
            && rect.y + rect.height == common.y + common.height)
        {
            if (rect.width == common.width) {
                rect.height -= common.height;
                return true;
            } else
            if (rect.height == common.height) {
                rect.width -= common.width;
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return super.toString() + "[ horizontal=" + paintRects[0] +
            " vertical=" + paintRects[1] +
            " update=" + paintRects[2] + "]";
    }
}
