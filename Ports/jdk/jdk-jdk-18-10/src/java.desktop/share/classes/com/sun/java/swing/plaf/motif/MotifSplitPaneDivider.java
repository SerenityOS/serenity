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

package com.sun.java.swing.plaf.motif;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.MouseEvent;

import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/**
 * Divider used for Motif split pane.
 *
 * @author Jeff Dinkins
 */
@SuppressWarnings("serial") // Same-version serialization only
public class MotifSplitPaneDivider extends BasicSplitPaneDivider
{
    /**
     * Default cursor, supers is package private, so we have to have one
     * too.
     */
    private static final Cursor defaultCursor =
                            Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);


    public static final int minimumThumbSize = 6;
    public static final int defaultDividerSize = 18;

    protected  static final int pad = 6;

    private int hThumbOffset = 30;
    private int vThumbOffset = 40;
    protected int hThumbWidth = 12;
    protected int hThumbHeight = 18;
    protected int vThumbWidth = 18;
    protected int vThumbHeight = 12;

    protected Color highlightColor;
    protected Color shadowColor;
    protected Color focusedColor;

    /**
     * Creates a new Motif SplitPaneDivider
     */
    public MotifSplitPaneDivider(BasicSplitPaneUI ui) {
        super(ui);
        highlightColor = UIManager.getColor("SplitPane.highlight");
        shadowColor = UIManager.getColor("SplitPane.shadow");
        focusedColor = UIManager.getColor("SplitPane.activeThumb");
        setDividerSize(hThumbWidth + pad);
    }

    /**
     * overrides to hardcode the size of the divider
     * PENDING(jeff) - rewrite JSplitPane so that this ins't needed
     */
    public void setDividerSize(int newSize) {
        Insets          insets = getInsets();
        int             borderSize = 0;
        if (getBasicSplitPaneUI().getOrientation() ==
            JSplitPane.HORIZONTAL_SPLIT) {
            if (insets != null) {
                borderSize = insets.left + insets.right;
            }
        }
        else if (insets != null) {
            borderSize = insets.top + insets.bottom;
        }
        if (newSize < pad + minimumThumbSize + borderSize) {
            setDividerSize(pad + minimumThumbSize + borderSize);
        } else {
            vThumbHeight = hThumbWidth = newSize - pad - borderSize;
            super.setDividerSize(newSize);
        }
    }

    /**
      * Paints the divider.
      */
    // PENDING(jeff) - the thumb's location and size is currently hard coded.
    // It should be dynamic.
    public void paint(Graphics g) {
        Color               bgColor = getBackground();
        Dimension           size = getSize();

        // fill
        g.setColor(getBackground());
        g.fillRect(0, 0, size.width, size.height);

        if(getBasicSplitPaneUI().getOrientation() ==
           JSplitPane.HORIZONTAL_SPLIT) {
            int center = size.width/2;
            int x = center - hThumbWidth/2;
            int y = hThumbOffset;

            // split line
            g.setColor(shadowColor);
            g.drawLine(center-1, 0, center-1, size.height);

            g.setColor(highlightColor);
            g.drawLine(center, 0, center, size.height);

            // draw thumb
            g.setColor((splitPane.hasFocus()) ? focusedColor :
                                                getBackground());
            g.fillRect(x+1, y+1, hThumbWidth-2, hThumbHeight-1);

            g.setColor(highlightColor);
            g.drawLine(x, y, x+hThumbWidth-1, y);       // top
            g.drawLine(x, y+1, x, y+hThumbHeight-1);    // left

            g.setColor(shadowColor);
            g.drawLine(x+1, y+hThumbHeight-1,
                       x+hThumbWidth-1, y+hThumbHeight-1);      // bottom
            g.drawLine(x+hThumbWidth-1, y+1,
                       x+hThumbWidth-1, y+hThumbHeight-2);      // right

        } else {
            int center = size.height/2;
            int x = size.width - vThumbOffset;
            int y = size.height/2 - vThumbHeight/2;

            // split line
            g.setColor(shadowColor);
            g.drawLine(0, center-1, size.width, center-1);

            g.setColor(highlightColor);
            g.drawLine(0, center, size.width, center);

            // draw thumb
            g.setColor((splitPane.hasFocus()) ? focusedColor :
                                                getBackground());
            g.fillRect(x+1, y+1, vThumbWidth-1, vThumbHeight-1);

            g.setColor(highlightColor);
            g.drawLine(x, y, x+vThumbWidth, y);    // top
            g.drawLine(x, y+1, x, y+vThumbHeight); // left

            g.setColor(shadowColor);
            g.drawLine(x+1, y+vThumbHeight,
                       x+vThumbWidth, y+vThumbHeight);          // bottom
            g.drawLine(x+vThumbWidth, y+1,
                       x+vThumbWidth, y+vThumbHeight-1);        // right
        }
        super.paint(g);

    }

    /**
      * The minimums size is the same as the preferredSize
      */
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    /**
     * Sets the SplitPaneUI that is using the receiver. This is completely
     * overriden from super to create a different MouseHandler.
     */
    public void setBasicSplitPaneUI(BasicSplitPaneUI newUI) {
        if (splitPane != null) {
            splitPane.removePropertyChangeListener(this);
           if (mouseHandler != null) {
               splitPane.removeMouseListener(mouseHandler);
               splitPane.removeMouseMotionListener(mouseHandler);
               removeMouseListener(mouseHandler);
               removeMouseMotionListener(mouseHandler);
               mouseHandler = null;
           }
        }
        splitPaneUI = newUI;
        if (newUI != null) {
            splitPane = newUI.getSplitPane();
            if (splitPane != null) {
                if (mouseHandler == null) mouseHandler=new MotifMouseHandler();
                splitPane.addMouseListener(mouseHandler);
                splitPane.addMouseMotionListener(mouseHandler);
                addMouseListener(mouseHandler);
                addMouseMotionListener(mouseHandler);
                splitPane.addPropertyChangeListener(this);
                if (splitPane.isOneTouchExpandable()) {
                    oneTouchExpandableChanged();
                }
            }
        }
        else {
            splitPane = null;
        }
    }

    /**
     * Returns true if the point at <code>x</code>, <code>y</code>
     * is inside the thumb.
     */
    private boolean isInThumb(int x, int y) {
        Dimension           size = getSize();
        int                 thumbX;
        int                 thumbY;
        int                 thumbWidth;
        int                 thumbHeight;

        if (getBasicSplitPaneUI().getOrientation() ==
            JSplitPane.HORIZONTAL_SPLIT) {
            int center = size.width/2;
            thumbX = center - hThumbWidth/2;
            thumbY = hThumbOffset;
            thumbWidth = hThumbWidth;
            thumbHeight = hThumbHeight;
        }
        else {
            int center = size.height/2;
            thumbX = size.width - vThumbOffset;
            thumbY = size.height/2 - vThumbHeight/2;
            thumbWidth = vThumbWidth;
            thumbHeight = vThumbHeight;
        }
        return (x >= thumbX && x < (thumbX + thumbWidth) &&
                y >= thumbY && y < (thumbY + thumbHeight));
    }

    //
    // Two methods are exposed so that MotifMouseHandler can see the
    // superclass protected ivars
    //

    private DragController getDragger() {
        return dragger;
    }

    private JSplitPane getSplitPane() {
        return splitPane;
    }


    /**
     * MouseHandler is subclassed to only pass off to super if the mouse
     * is in the thumb. Motif only allows dragging when the thumb is clicked
     * in.
     */
    private class MotifMouseHandler extends MouseHandler {
        public void mousePressed(MouseEvent e) {
            // Constrain the mouse pressed to the thumb.
            if (e.getSource() == MotifSplitPaneDivider.this &&
                getDragger() == null && getSplitPane().isEnabled() &&
                isInThumb(e.getX(), e.getY())) {
                super.mousePressed(e);
            }
        }

        public void mouseMoved(MouseEvent e) {
            if (getDragger() != null) {
                return;
            }
            if (!isInThumb(e.getX(), e.getY())) {
                if (getCursor() != defaultCursor) {
                    setCursor(defaultCursor);
                }
                return;
            }
            super.mouseMoved(e);
        }
    }
}
