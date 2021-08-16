/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.X11;

import java.awt.*;
import java.awt.event.*;
import java.awt.peer.*;
import java.lang.reflect.*;

import sun.awt.AWTAccessor;

class XScrollPanePeer extends XComponentPeer implements ScrollPanePeer, XScrollbarClient {

    public static final int     MARGIN = 1;
    public static final int     SCROLLBAR;
    public static final int     SPACE = 2;
    public static final int     SCROLLBAR_INSET = 2;

    public static final int     VERTICAL = 1 << 0;
    public static final int     HORIZONTAL = 1 << 1;

    static {
        SCROLLBAR = XToolkit.getUIDefaults().getInt("ScrollBar.defaultWidth");
    }

    XVerticalScrollbar       vsb;
    XHorizontalScrollbar     hsb;
    XWindow                  clip;

    int                         active=VERTICAL;
    int                         hsbSpace;
    int                         vsbSpace;

    static class XScrollPaneContentWindow extends XWindow {
        XScrollPaneContentWindow(ScrollPane target, long parentWindow) {
            super(target, parentWindow);
        }
        public String getWMName() {
            return "ScrollPane content";
        }
    }

    XScrollPanePeer(ScrollPane target) {
        super(target);

        // Create the clip window. The field "clip" must be null when
        // we call winCreate, or the parent of clip will be set to itself!
        clip = null;


        XWindow c = new XScrollPaneContentWindow(target,window);
        clip = c;

        vsb = new XVerticalScrollbar(this);

        hsb = new XHorizontalScrollbar(this);

        if (target.getScrollbarDisplayPolicy() == ScrollPane.SCROLLBARS_ALWAYS) {
            vsbSpace = hsbSpace = SCROLLBAR;
        } else {
            vsbSpace = hsbSpace = 0;
        }

        int unitIncrement = 1;
        Adjustable vAdjustable = target.getVAdjustable();
        if (vAdjustable != null){
            unitIncrement = vAdjustable.getUnitIncrement();
        }
        int h = height-hsbSpace;
        vsb.setValues(0, h, 0, h, unitIncrement, Math.max(1, (int)(h * 0.90)));
        vsb.setSize(vsbSpace-SCROLLBAR_INSET, h);

        unitIncrement = 1;
        Adjustable hAdjustable = target.getHAdjustable();
        if (hAdjustable != null){
            unitIncrement = hAdjustable.getUnitIncrement();
        }
        int w = width - vsbSpace;
        hsb.setValues(0, w, 0, w, unitIncrement, Math.max(1, (int)(w * 0.90)));
        hsb.setSize(w, hsbSpace-SCROLLBAR_INSET);

        setViewportSize();
        clip.xSetVisible(true);


    }

    public long getContentWindow()
    {
        return (clip == null) ? window : clip.getWindow();
    }

    public void setBounds(int x, int y, int w, int h, int op) {
        super.setBounds(x, y, w, h, op);

        if (clip == null) return;
        setScrollbarSpace();
        setViewportSize();
        repaint();
    }

    public Insets getInsets() {
        return new Insets(MARGIN, MARGIN, MARGIN+hsbSpace, MARGIN+vsbSpace);
    }

    public int getHScrollbarHeight() {
        return SCROLLBAR;
    }

    public int getVScrollbarWidth() {
        return SCROLLBAR;
    }

    public void childResized(int w, int h) {
        if (setScrollbarSpace()) {
            setViewportSize();
        }
        repaint();
    }

    @SuppressWarnings("deprecation")
    Dimension getChildSize() {
        ScrollPane sp = (ScrollPane)target;
        if (sp.countComponents() > 0) {
            Component c = sp.getComponent(0);
            return c.size();
        } else {
            return new Dimension(0, 0);
        }
    }

    @SuppressWarnings("deprecation")
    boolean setScrollbarSpace() {
        ScrollPane sp = (ScrollPane)target;
        boolean changed = false;
        int sbDisplayPolicy = sp.getScrollbarDisplayPolicy();

        if (sbDisplayPolicy == ScrollPane.SCROLLBARS_NEVER) {
            return changed;
        }
        Dimension cSize = getChildSize();

        if (sbDisplayPolicy == ScrollPane.SCROLLBARS_AS_NEEDED) {
            int oldHsbSpace = hsbSpace;
            int oldVsbSpace = vsbSpace;
            hsbSpace = (cSize.width <= (width - 2*MARGIN) ? 0 : SCROLLBAR);
            vsbSpace = (cSize.height <= (height - 2*MARGIN) ? 0 : SCROLLBAR);

            if (hsbSpace == 0 && vsbSpace != 0) {
                hsbSpace = (cSize.width <= (width - SCROLLBAR - 2*MARGIN) ? 0 : SCROLLBAR);
            }
            if (vsbSpace == 0 && hsbSpace != 0) {
                vsbSpace = (cSize.height <= (height - SCROLLBAR - 2*MARGIN) ? 0 : SCROLLBAR);
            }
            if (oldHsbSpace != hsbSpace || oldVsbSpace != vsbSpace) {
                changed = true;
            }
        }
        if (vsbSpace > 0) {
            int vis = height - (2*MARGIN) - hsbSpace;
            int max = Math.max(cSize.height, vis);
            vsb.setValues(vsb.getValue(), vis, 0, max);
            vsb.setBlockIncrement((int)(vsb.getVisibleAmount() * .90));
            vsb.setSize(vsbSpace-SCROLLBAR_INSET, height-hsbSpace);
            // Adjustable vadj = sp.getVAdjustable();
            // vadj.setVisibleAmount(vsb.vis);
            // vadj.setMaximum(vsb.max);
            // vadj.setBlockIncrement(vsb.page);
        }
        if (hsbSpace > 0) {
            int vis = width - (2*MARGIN) - vsbSpace;
            int max = Math.max(cSize.width, vis);
            hsb.setValues(hsb.getValue(), vis, 0, max);
            hsb.setBlockIncrement((int)(hsb.getVisibleAmount() * .90));
            hsb.setSize(width-vsbSpace, hsbSpace-SCROLLBAR_INSET);
            // Adjustable hadj = sp.getHAdjustable();
            // hadj.setVisibleAmount(hsb.vis);
            // hadj.setMaximum(hsb.max);
            // hadj.setBlockIncrement(hsb.page);
        }

        // Check to see if we hid either of the scrollbars but left
        // ourselves scrolled off of the top and/or right of the pane.
        // If we did, we need to scroll to the top and/or right of
        // the pane to make it visible.
        //
        // Reminder: see if there is a better place to put this code.
        boolean must_scroll = false;

        // Get the point at which the ScrollPane is currently located
        // if number of components > 0
        Point p = new Point(0, 0);

        if (((ScrollPane)target).getComponentCount() > 0){

            p = ((ScrollPane)target).getComponent(0).location();

            if ((vsbSpace == 0) && (p.y < 0)) {
                p.y = 0;
                must_scroll = true;
            }

            if ((hsbSpace == 0) && (p.x < 0)) {
                p.x = 0;
                must_scroll = true;
            }
        }

        if (must_scroll)
            scroll(x, y, VERTICAL | HORIZONTAL);

        return changed;
    }

    void setViewportSize() {
        clip.xSetBounds(MARGIN, MARGIN,
                width - (2*MARGIN)  - vsbSpace,
                height - (2*MARGIN) - hsbSpace);
    }

    public void setUnitIncrement(Adjustable adj, int u) {
        if (adj.getOrientation() == Adjustable.VERTICAL) {
            vsb.setUnitIncrement(u);
        } else {
            // HORIZONTAL
            hsb.setUnitIncrement(u);
        }
    }

    public void setValue(Adjustable adj, int v) {
        if (adj.getOrientation() == Adjustable.VERTICAL) {
            scroll(-1, v, VERTICAL);
        } else {
            // HORIZONTAL
            scroll(v, -1, HORIZONTAL);
        }
    }

    public void setScrollPosition(int x, int y) {
        scroll(x, y, VERTICAL | HORIZONTAL);
    }

    void scroll(int x, int y, int flag) {
        scroll(x, y, flag, AdjustmentEvent.TRACK);
    }

    /**
     * Scroll the contents to position x, y
     */
    @SuppressWarnings("deprecation")
    void scroll(int x, int y, int flag, int type) {
        checkSecurity();
        ScrollPane sp = (ScrollPane)target;
        Component c = getScrollChild();
        if (c == null) {
            return;
        }
        int sx, sy;
        Color[] colors = getGUIcolors();

        if (sp.getScrollbarDisplayPolicy() == ScrollPane.SCROLLBARS_NEVER) {
            sx = -x;
            sy = -y;
        } else {
            Point p = c.location();
            sx = p.x;
            sy = p.y;

            if ((flag & HORIZONTAL) != 0) {
                hsb.setValue(Math.min(x, hsb.getMaximum()-hsb.getVisibleAmount()));
                ScrollPaneAdjustable hadj = (ScrollPaneAdjustable)sp.getHAdjustable();
                setAdjustableValue(hadj, hsb.getValue(), type);
                sx = -(hsb.getValue());
                Graphics g = getGraphics();
                if (g != null) {
                    try {
                        paintHorScrollbar(g, colors, true);
                    } finally {
                        g.dispose();
                    }
                }
            }
            if ((flag & VERTICAL) != 0) {
                vsb.setValue(Math.min(y, vsb.getMaximum() - vsb.getVisibleAmount()));
                ScrollPaneAdjustable vadj = (ScrollPaneAdjustable)sp.getVAdjustable();
                setAdjustableValue(vadj, vsb.getValue(), type);
                sy = -(vsb.getValue());
                Graphics g = getGraphics();
                if (g != null) {
                    try {
                        paintVerScrollbar(g, colors, true);
                    } finally {
                        g.dispose();
                    }
                }
            }
        }
        c.move(sx, sy);
    }

    private void setAdjustableValue(final ScrollPaneAdjustable adj, final int value,
                            final int type) {
        AWTAccessor.getScrollPaneAdjustableAccessor().setTypedValue(adj, value,
                                                                    type);
    }
    @Override
    void paintPeer(final Graphics g) {
        final Color[] colors = getGUIcolors();
        g.setColor(colors[BACKGROUND_COLOR]);
        final int h = height - hsbSpace;
        final int w = width - vsbSpace;
        g.fillRect(0, 0, w, h);
        // paint rectangular region between scrollbars
        g.fillRect(w, h, vsbSpace, hsbSpace);
        if (MARGIN > 0) {
            draw3DRect(g, colors, 0, 0, w - 1, h - 1, false);
        }
        paintScrollBars(g, colors);
    }
    private void paintScrollBars(Graphics g, Color[] colors) {
        if (vsbSpace > 0) {
            paintVerScrollbar(g, colors, true);
            // paint the whole scrollbar
        }

        if (hsbSpace > 0) {
            paintHorScrollbar(g, colors, true);
            // paint the whole scrollbar
        }
    }
    void repaintScrollBars() {
        Graphics g = getGraphics();
        Color[] colors = getGUIcolors();
        if (g != null) {
            try {
                paintScrollBars(g, colors);
            } finally {
                g.dispose();
            }
        }
    }
    public void repaintScrollbarRequest(XScrollbar sb) {
        Graphics g = getGraphics();
        Color[] colors = getGUIcolors();
        if (g != null) {
            try {
                if (sb == vsb) {
                    paintVerScrollbar(g, colors, true);
                } else if (sb == hsb) {
                    paintHorScrollbar(g, colors, true);
                }
            } finally {
                g.dispose();
            }
        }
    }
    public void handleEvent(java.awt.AWTEvent e) {
        super.handleEvent(e);

        int id = e.getID();
        switch(id) {
            case PaintEvent.PAINT:
            case PaintEvent.UPDATE:
                repaintScrollBars();
                break;
        }
    }


    /**
     * Paint the horizontal scrollbar to the screen
     *
     * @param g the graphics context to draw into
     * @param colors the colors used to draw the scrollbar
     * @param paintAll paint the whole scrollbar if true, just the thumb if false
     */
    void paintHorScrollbar(Graphics g, Color[] colors, boolean paintAll) {
        if (hsbSpace <= 0) {
            return;
        }
        Graphics ng = g.create();
        g.setColor(colors[BACKGROUND_COLOR]);

        // SCROLLBAR is the height of scrollbar area
        // but the actual scrollbar is SCROLLBAR-SPACE high;
        // the rest must be filled with background color
        int w = width - vsbSpace - (2*MARGIN);
        g.fillRect(MARGIN, height-SCROLLBAR, w, SPACE);
        g.fillRect(0, height-SCROLLBAR, MARGIN, SCROLLBAR);
        g.fillRect(MARGIN + w, height-SCROLLBAR, MARGIN, SCROLLBAR);

        try {
            ng.translate(MARGIN, height - (SCROLLBAR - SPACE));
            hsb.paint(ng, colors, paintAll);
        }
        finally {
            ng.dispose();
        }


    }




    /**
     * Paint the vertical scrollbar to the screen
     *
     * @param g the graphics context to draw into
     * @param colors the colors used to draw the scrollbar
     * @param paintAll paint the whole scrollbar if true, just the thumb if false
     */
    void paintVerScrollbar(Graphics g, Color[] colors, boolean paintAll) {
        if (vsbSpace <= 0) {
            return;
        }
        Graphics ng = g.create();
        g.setColor(colors[BACKGROUND_COLOR]);

        // SCROLLBAR is the width of scrollbar area
        // but the actual scrollbar is SCROLLBAR-SPACE wide;
        // the rest must be filled with background color
        int h = height - hsbSpace - (2*MARGIN);
        g.fillRect(width-SCROLLBAR, MARGIN, SPACE, h);
        g.fillRect(width-SCROLLBAR, 0, SCROLLBAR, MARGIN);
        g.fillRect(width-SCROLLBAR, MARGIN+h, SCROLLBAR, MARGIN);

        try {
            ng.translate(width - (SCROLLBAR - SPACE), MARGIN);
            vsb.paint(ng, colors, paintAll);
        }
        finally {
            ng.dispose();
        }
    }

    /**
     *
     * @see java.awt.event.MouseEvent
     * MouseEvent.MOUSE_CLICKED
     * MouseEvent.MOUSE_PRESSED
     * MouseEvent.MOUSE_RELEASED
     * MouseEvent.MOUSE_MOVED
     * MouseEvent.MOUSE_ENTERED
     * MouseEvent.MOUSE_EXITED
     * MouseEvent.MOUSE_DRAGGED
     */
    @SuppressWarnings("deprecation")
    public void handleJavaMouseEvent( MouseEvent mouseEvent ) {
        super.handleJavaMouseEvent(mouseEvent);
        int modifiers = mouseEvent.getModifiers();
        int id = mouseEvent.getID();
        int x = mouseEvent.getX();
        int y = mouseEvent.getY();


        //        super.handleMouseEvent(mouseEvent);

        if ((modifiers & InputEvent.BUTTON1_MASK) == 0) {
            return;
        }

        switch (id) {
            case MouseEvent.MOUSE_PRESSED:
                if (inVerticalScrollbar(x,y )) {
                    active = VERTICAL;
                    int h = height - hsbSpace - (2*MARGIN);
                    vsb.handleMouseEvent(id,modifiers,x - (width - SCROLLBAR + SPACE),y-MARGIN);
                } else if (inHorizontalScrollbar(x, y) ) {
                    active = HORIZONTAL;
                    int w = width - 2*MARGIN - vsbSpace;
                    hsb.handleMouseEvent(id,modifiers,x-MARGIN,y-(height - SCROLLBAR + SPACE));
                }
                break;

                // On mouse up, pass the event through to the scrollbar to stop
                // scrolling. The x & y passed do not matter.
            case MouseEvent.MOUSE_RELEASED:
                //     winReleaseCursorFocus();
                if (active == VERTICAL) {
                    vsb.handleMouseEvent(id,modifiers,x,y);
                } else if (active == HORIZONTAL) {
                    hsb.handleMouseEvent(id,modifiers,x,y);
                }
                break;


            case MouseEvent.MOUSE_DRAGGED:
                if ((active == VERTICAL)) {
                    int h = height - 2*MARGIN - hsbSpace;
                    vsb.handleMouseEvent(id,modifiers,x-(width - SCROLLBAR + SPACE),y-MARGIN);
                } else if ((active == HORIZONTAL)) {
                    int w = width - 2*MARGIN - vsbSpace;
                    hsb.handleMouseEvent(id,modifiers,x-MARGIN,y-(height - SCROLLBAR + SPACE));
                }
                break;
        }
    }

    /**
     * return value from the scrollbar
     */
    public void notifyValue(XScrollbar obj, int type, int v, boolean isAdjusting) {
        if (obj == vsb) {
            scroll(-1, v, VERTICAL, type);
        } else if ((XHorizontalScrollbar)obj == hsb) {
            scroll(v, -1, HORIZONTAL, type);
        }
    }

    /**
     * return true if the x and y position is in the verticalscrollbar
     */
    boolean inVerticalScrollbar(int x, int y) {
        if (vsbSpace <= 0) {
            return false;
        }
        int h = height - MARGIN - hsbSpace;
        return (x >= width - (SCROLLBAR - SPACE)) && (x < width) && (y >= MARGIN) && (y < h);
    }

    /**
     * return true if the x and y position is in the horizontal scrollbar
     */
    boolean inHorizontalScrollbar(int x, int y) {
        if (hsbSpace <= 0) {
            return false;
        }
        int w = width - MARGIN - vsbSpace;
        return (x >= MARGIN) && (x < w) && (y >= height - (SCROLLBAR - SPACE)) && (y < height);
    }

    private Component getScrollChild() {
        ScrollPane sp = (ScrollPane)target;
        Component child = null;
        try {
            child = sp.getComponent(0);
        } catch (ArrayIndexOutOfBoundsException e) {
            // do nothing.  in this case we return null
        }
        return child;
    }

    int vval;
    int hval;
    int vmax;
    int hmax;
    /*
     * Print the native component by rendering the Motif look ourselves.
     * ToDo(aim): needs to query native motif for more accurate size and
     * color information.
     */
    @SuppressWarnings("deprecation")
    public void print(Graphics g) {
        ScrollPane sp = (ScrollPane)target;
        Dimension d = sp.size();
        Color bg = sp.getBackground();
        Color fg = sp.getForeground();
        Point p = sp.getScrollPosition();
        Component c = getScrollChild();
        Dimension cd;
        if (c != null) {
            cd = c.size();
        } else {
            cd = new Dimension(0, 0);
        }
        int sbDisplay = sp.getScrollbarDisplayPolicy();
        int vvis, hvis, vmin, hmin, vmax, hmax, vval, hval;

        switch (sbDisplay) {
            case ScrollPane.SCROLLBARS_NEVER:
                hsbSpace = vsbSpace = 0;
                break;
            case ScrollPane.SCROLLBARS_ALWAYS:
                hsbSpace = vsbSpace = SCROLLBAR;
                break;
            case ScrollPane.SCROLLBARS_AS_NEEDED:
                hsbSpace = (cd.width <= (d.width - 2*MARGIN)? 0 : SCROLLBAR);
                vsbSpace = (cd.height <= (d.height - 2*MARGIN)? 0 : SCROLLBAR);

                if (hsbSpace == 0 && vsbSpace != 0) {
                    hsbSpace = (cd.width <= (d.width - SCROLLBAR - 2*MARGIN)? 0 : SCROLLBAR);
                }
                if (vsbSpace == 0 && hsbSpace != 0) {
                    vsbSpace = (cd.height <= (d.height - SCROLLBAR - 2*MARGIN)? 0 : SCROLLBAR);
                }
        }

        vvis = hvis = vmin = hmin = vmax = hmax = vval = hval = 0;

        if (vsbSpace > 0) {
            vmin = 0;
            vvis = d.height - (2*MARGIN) - hsbSpace;
            vmax = Math.max(cd.height - vvis, 0);
            vval = p.y;
        }
        if (hsbSpace > 0) {
            hmin = 0;
            hvis = d.width - (2*MARGIN) - vsbSpace;
            hmax = Math.max(cd.width - hvis, 0);
            hval = p.x;
        }

        // need to be careful to add the margins back in here because
        // we're drawing the margin border, after all!
        int w = d.width - vsbSpace;
        int h = d.height - hsbSpace;

        g.setColor(bg);
        g.fillRect(0, 0, d.width, d.height);

        if (hsbSpace > 0) {
            int sbw = d.width - vsbSpace;
            g.fillRect(1, d.height - SCROLLBAR - 3, sbw - 1, SCROLLBAR - 3);
            Graphics ng = g.create();
            try {
                ng.translate(0, d.height - (SCROLLBAR - 2));
                drawScrollbar(ng, bg, SCROLLBAR - 2, sbw,
                        hmin, hmax, hval, hvis, true);
            } finally {
                ng.dispose();
            }
        }
        if (vsbSpace > 0) {
            int sbh = d.height - hsbSpace;
            g.fillRect(d.width - SCROLLBAR - 3, 1, SCROLLBAR - 3, sbh - 1);
            Graphics ng = g.create();
            try {
                ng.translate(d.width - (SCROLLBAR - 2), 0);
                drawScrollbar(ng, bg, SCROLLBAR - 2, sbh,
                        vmin, vmax, vval, vvis, false);
            } finally {
                ng.dispose();
            }
        }

        draw3DRect(g, bg, 0, 0, w - 1, h - 1, false);

        target.print(g);
        sp.printComponents(g);
    }

}
