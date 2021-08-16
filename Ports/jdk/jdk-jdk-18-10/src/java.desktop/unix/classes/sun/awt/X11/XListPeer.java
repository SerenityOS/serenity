/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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


// Very much based on XListPeer from javaos

package sun.awt.X11;

import java.awt.*;
import java.awt.event.*;
import java.awt.peer.*;
import java.util.Objects;
import java.util.Vector;
import java.awt.image.*;
import sun.util.logging.PlatformLogger;

// TODO: some input actions should do nothing if Shift or Control are down

class XListPeer extends XComponentPeer implements ListPeer, XScrollbarClient {

    private static final PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11.XListPeer");

    public static final int     MARGIN = 2;
    public static final int     SPACE = 1;
    public static final int     SCROLLBAR_AREA = 17;  // Area reserved for the
                                                      // scrollbar
    public static final int     SCROLLBAR_WIDTH = 13; // Actual width of the
                                                      // scrollbar
    public static final int     NONE = -1;
    public static final int     WINDOW = 0;
    public static final int     VERSCROLLBAR = 1;
    public static final int     HORSCROLLBAR = 2;
    public static final int     DEFAULT_VISIBLE_ROWS = 4; // From java.awt.List,
    public static final int     HORIZ_SCROLL_AMT = 10;

    private static final int    PAINT_VSCROLL = 2;
    private static final int    PAINT_HSCROLL = 4;
    private static final int    PAINT_ITEMS = 8;
    private static final int    PAINT_FOCUS = 16;
    private static final int    PAINT_BACKGROUND = 32;
    private static final int    PAINT_HIDEFOCUS = 64;
    private static final int    PAINT_ALL =
        PAINT_VSCROLL | PAINT_HSCROLL | PAINT_ITEMS | PAINT_FOCUS | PAINT_BACKGROUND;
    private static final int    COPY_AREA = 128;

    XVerticalScrollbar       vsb;
    XHorizontalScrollbar     hsb;
    ListPainter painter;

    // TODO: ick - Vector?
    Vector<String>              items;
    boolean                     multipleSelections;
    int                         active = NONE;

    // Holds the array of the indexes of the elements which is selected
    // This array should be kept sorted, low to high.
    int[]                         selected;
    int                         fontHeight;
    int                         fontAscent;
    int                         fontLeading;

    // Holds the index of the item used in the previous operation (selectItem, deselectItem)
    // Adding of an item or clearing of the list sets this index to -1
    // The index is used at the moment of the post of ACTION_PERFORMED event after the mouse double click event.
    int                         currentIndex = -1;

    // Used for tracking selection/deselection between mousePress/Release
    // and for ItemEvents
    int                         eventIndex = -1;
    int                         eventType = NONE;

    // Holds the index of the item that receive focus
    // This variable is reasonable only for multiple list
    // since 'focusIndex' and 'selected[0]' are equal for single-selection list
    int                         focusIndex;

    int                         maxLength;
    boolean                     vsbVis;  // visibility of scrollbars
    boolean                     hsbVis;
    int                         listWidth;  // Width of list portion of List
    int                         listHeight; // Height of list portion of List
    // (i.e. without scrollbars)

    private int firstTimeVisibleIndex = 0;

    // Motif Lists don't seem to inherit the background color from their
    // parent when an app is first started up.  So, we track if the colors have
    // been set.  See getListBackground()/getListForeground().
    boolean bgColorSet;
    boolean fgColorSet;

    // Holds the true if mouse is dragging outside of the area of the list
    // The flag is used at the moment of the dragging and releasing mouse
    // See 6243382 for more information
    boolean mouseDraggedOutHorizontally = false;
    boolean mouseDraggedOutVertically = false;

    // Holds the true if a mouse event was originated on the scrollbar
    // See 6300527 for more information
    boolean isScrollBarOriginated = false;

    // This variable is set to true after the "mouse pressed" event and to false after the "mouse released" event
    // Fixed 6293432: Key events ('SPACE', 'UP', 'DOWN') aren't blocked if mouse is kept in 'PRESSED' state for List, XAWT
    boolean isMousePressed = false;

    /**
     * Create a list
     */
    XListPeer(List target) {
        super(target);
    }

    /**
     * Overridden from XWindow
     */
    public void preInit(XCreateWindowParams params) {
        super.preInit(params);

        // Stuff that must be initialized before layout() is called
        items = new Vector<>();
        createVerScrollbar();
        createHorScrollbar();

        painter = new ListPainter();

        // See 6246467 for more information
        bgColorSet = target.isBackgroundSet();
        fgColorSet = target.isForegroundSet();
    }

    public void postInit(XCreateWindowParams params) {
        super.postInit(params);
        initFontMetrics();
        // TODO: more efficient way?
        //       do we really want/need a copy of all the items?
        // get all items from target
        List l = (List)target;
        int stop = l.getItemCount();
        for (int i = 0 ; i < stop; i++) {
            items.addElement(l.getItem(i));
        }

        /* make the visible position visible. */
        int index = l.getVisibleIndex();
        if (index >= 0) {
            // Can't call makeVisible since it check scroll bar,
            // initialize scroll bar instead
            vsb.setValues(index, 0, 0, items.size());
        }

        // NOTE: needs to have target set
        maxLength = maxLength();

        // get the index containing all indexes to selected items
        int[] sel = l.getSelectedIndexes();
        selected = new int[sel.length];
        // TODO: shouldn't this be arraycopy()?
        for (int i = 0 ; i < sel.length ; i ++) {
            selected[i] = sel[i];
        }
        // The select()ed item should become the focused item, but we don't
        // get the select() call because the peer generally hasn't yet been
        // created during app initialization.
        // TODO: For multi-select lists, it should be the highest selected index
        if (sel.length > 0) {
            setFocusIndex(sel[sel.length - 1]);
        }
        else {
            setFocusIndex(0);
        }

        multipleSelections = l.isMultipleMode();
    }


    /**
     * add Vertical Scrollbar
     */
    void createVerScrollbar() {
        vsb = new XVerticalScrollbar(this);
        vsb.setValues(0, 0, 0, 0, 1, 1);
    }


    /**
     * add Horizontal scrollbar
     */
    void createHorScrollbar() {
        hsb = new XHorizontalScrollbar(this);
        hsb.setValues(0, 0, 0, 0, HORIZ_SCROLL_AMT, HORIZ_SCROLL_AMT);
    }

    public void add(String item, int index) {
        addItem(item, index);
    }

    public void removeAll() {
        clear();
        maxLength = 0;
    }

    public void setMultipleMode (boolean b) {
        setMultipleSelections(b);
    }

    public Dimension getMinimumSize() {
        return getMinimumSize(DEFAULT_VISIBLE_ROWS);
    }

    public Dimension getPreferredSize(int rows) {
        return getMinimumSize(rows);
    }

    public Dimension getMinimumSize(int rows) {
        FontMetrics fm = getFontMetrics(getFont());
        initFontMetrics();
        return new Dimension(20 + fm.stringWidth("0123456789abcde"),
                             getItemHeight() * rows + (2*MARGIN));
    }

    /**
     * Calculate font metrics
     */
    void initFontMetrics() {
        FontMetrics fm = getFontMetrics(getFont());
        fontHeight = fm.getHeight();
        fontAscent = fm.getAscent();
        fontLeading = fm.getLeading();
    }


    /**
     * return the length of the largest item in the list
     */
    int maxLength() {
        FontMetrics fm = getFontMetrics(getFont());
        int m = 0;
        int end = items.size();
        for(int i = 0 ; i < end ; i++) {
            int l = fm.stringWidth(items.elementAt(i));
            m = Math.max(m, l);
        }
        return m;
    }

    /**
     * Calculates the width of item's label
     */
    int getItemWidth(int i) {
        FontMetrics fm = getFontMetrics(getFont());
        return fm.stringWidth(items.elementAt(i));
    }

    /**
     * return the on-screen width of the given string "str"
     */
    int stringLength(String str) {
        FontMetrics fm = getFontMetrics(target.getFont());
        return fm.stringWidth(str);
    }

    public void setForeground(Color c) {
        fgColorSet = true;
        super.setForeground(c);
    }

    public void setBackground(Color c) {
        bgColorSet = true;
        super.setBackground(c);
    }

    /**
     * Returns the color that should be used to paint the background of
     * the list of items.  Note that this is not the same as
     * target.getBackground() which is the color of the scrollbars, and the
     * lower-right corner of the Component when the scrollbars are displayed.
     */
    private Color getListBackground(Color[] colors) {
        if (bgColorSet) {
            return colors[BACKGROUND_COLOR];
        }
        else {
            return SystemColor.text;
        }
    }

    /**
     * Returns the color that should be used to paint the list item text.
     */
    private Color getListForeground(Color[] colors) {
        if (fgColorSet) {
            return colors[FOREGROUND_COLOR];
        }
        else {
            return SystemColor.textText;
        }
    }

    Rectangle getVScrollBarRec() {
        return new Rectangle(width - (SCROLLBAR_WIDTH), 0, SCROLLBAR_WIDTH+1, height);
    }

    Rectangle getHScrollBarRec() {
        return new Rectangle(0, height - SCROLLBAR_WIDTH, width, SCROLLBAR_WIDTH);
    }

    int getFirstVisibleItem() {
        if (vsbVis) {
            return vsb.getValue();
        } else {
            return 0;
        }
    }

    int getLastVisibleItem() {
        if (vsbVis) {
            return Math.min(items.size()-1, vsb.getValue() + itemsInWindow() -1);
        } else {
            return Math.min(items.size()-1, itemsInWindow()-1);
        }
    }
    public void repaintScrollbarRequest(XScrollbar scrollbar) {
        if (scrollbar == hsb)  {
            repaint(PAINT_HSCROLL);
        }
        else if (scrollbar == vsb) {
            repaint(PAINT_VSCROLL);
        }
    }
    /**
     * Overridden for performance
     */
    public void repaint() {
        repaint(getFirstVisibleItem(), getLastVisibleItem(), PAINT_ALL);
    }

    private void repaint(int options) {
        repaint(getFirstVisibleItem(), getLastVisibleItem(), options);
    }

    private void repaint(int firstItem, int lastItem, int options) {
        repaint(firstItem, lastItem, options, null, null);
    }

    /**
     * In most cases the entire area of the component doesn't have
     * to be repainted. The method repaints the particular areas of
     * the component. The areas to repaint is specified by the option
     * parameter. The possible values of the option parameter are:
     * PAINT_VSCROLL, PAINT_HSCROLL, PAINT_ITEMS, PAINT_FOCUS,
     * PAINT_HIDEFOCUS, PAINT_BACKGROUND, PAINT_ALL, COPY_AREA.
     *
     * Note that the COPY_AREA value initiates copy of a source area
     * of the component by a distance by means of the copyArea method
     * of the Graphics class.
     *
     * @param firstItem the position of the first item of the range to repaint
     * @param lastItem the position of the last item of the range to repaint
     * @param options specifies the particular area of the component to repaint
     * @param source the area of the component to copy
     * @param distance the distance to copy the source area
     */
    private void repaint(int firstItem, int lastItem, int options, Rectangle source, Point distance) {
        final Graphics g = getGraphics();
        if (g != null) {
            try {
                painter.paint(g, firstItem, lastItem, options, source, distance);
                postPaintEvent(target, 0, 0, getWidth(), getHeight());
            } finally {
                g.dispose();
            }
        }
    }
    @Override
    void paintPeer(final Graphics g) {
        painter.paint(g, getFirstVisibleItem(), getLastVisibleItem(), PAINT_ALL);
    }
    public boolean isFocusable() { return true; }

    // TODO: share/promote the Focus methods?
    public void focusGained(FocusEvent e) {
        super.focusGained(e);
        repaint(PAINT_FOCUS);
    }

    public void focusLost(FocusEvent e) {
        super.focusLost(e);
        repaint(PAINT_FOCUS);
    }

    /**
     * Layout the sub-components of the List - that is, the scrollbars and the
     * list of items.
     */
    public void layout() {
        int vis, maximum;
        boolean vsbWasVisible;
        int origVSBVal;
        assert(target != null);

        // Start with assumption there is not a horizontal scrollbar,
        // see if we need a vertical scrollbar

        // Bug: If the list DOES have a horiz scrollbar and the value is set to
        // the very bottom value, value is reset in setValues() because it isn't
        // a valid value for cases when the list DOESN'T have a horiz scrollbar.
        // This is currently worked-around with origVSGVal.
        origVSBVal = vsb.getValue();
        vis = itemsInWindow(false);
        maximum = items.size() < vis ? vis : items.size();
        vsb.setValues(vsb.getValue(), vis, vsb.getMinimum(), maximum);
        vsbVis = vsbWasVisible = vsbIsVisible(false);
        listHeight = height;

        // now see if we need a horizontal scrollbar
        listWidth = getListWidth();
        vis = listWidth - ((2 * SPACE) + (2 * MARGIN));
        maximum = maxLength < vis ? vis : maxLength;
        hsb.setValues(hsb.getValue(), vis, hsb.getMinimum(), maximum);
        hsbVis = hsbIsVisible(vsbVis);

        if (hsbVis) {
            // do need a horizontal scrollbar, so recalculate height of
            // vertical s crollbar
            listHeight = height - SCROLLBAR_AREA;
            vis = itemsInWindow(true);
            maximum = items.size() < vis ? vis : items.size();
            vsb.setValues(origVSBVal, vis, vsb.getMinimum(), maximum);
            vsbVis = vsbIsVisible(true);
        }

        // now check to make sure we haven't changed need for vertical
        // scrollbar - if we have, we need to
        // recalculate horizontal scrollbar width - then we're done...
        if (vsbWasVisible != vsbVis) {
            listWidth = getListWidth();
            vis = listWidth - ((2 * SPACE) + (2 * MARGIN));
            maximum = maxLength < vis ? 0 : maxLength;
            hsb.setValues(hsb.getValue(), vis, hsb.getMinimum(), maximum);
            hsbVis = hsbIsVisible(vsbVis);
        }

        vsb.setSize(SCROLLBAR_WIDTH, listHeight);
        hsb.setSize(listWidth, SCROLLBAR_WIDTH);

        vsb.setBlockIncrement(itemsInWindow());
        hsb.setBlockIncrement(width - ((2 * SPACE) + (2 * MARGIN) + (vsbVis ? SCROLLBAR_AREA : 0)));
    }

    int getItemWidth() {
        return width - ((2 * MARGIN) + (vsbVis ? SCROLLBAR_AREA : 0));
    }

    /* Returns height of an item in the list */
    int getItemHeight() {
        return (fontHeight - fontLeading) + (2*SPACE);
    }

    int getItemX() {
        return MARGIN + SPACE;
    }

    int getItemY(int item) {
        return index2y(item);
    }

    int getFocusIndex() {
        return focusIndex;
    }

    void setFocusIndex(int value) {
        focusIndex = value;
    }

    /**
     * Update and return the focus rectangle.
     * Focus is around the focused item, if it is visible, or
     * around the border of the list if the focused item is scrolled off the top
     * or bottom of the list.
     */
    Rectangle getFocusRect() {
        Rectangle focusRect = new Rectangle();
        // width is always only based on presence of vert sb
        focusRect.x = 1;
        focusRect.width = getListWidth() - 3;
        // if focused item is not currently displayed in the list,  paint
        // focus around entire list (not including scrollbars)
        if (isIndexDisplayed(getFocusIndex())) {
            // focus rect is around the item
            focusRect.y = index2y(getFocusIndex()) - 2;
            focusRect.height = getItemHeight()+1;
        } else {
            // focus rect is around the list
            focusRect.y = 1;
            focusRect.height = hsbVis ? height - SCROLLBAR_AREA : height;
            focusRect.height -= 3;
        }
        return focusRect;
    }

    public void handleConfigureNotifyEvent(XEvent xev) {
        super.handleConfigureNotifyEvent(xev);

        // Update buffer
        painter.invalidate();
    }
    public boolean handlesWheelScrolling() { return true; }

    // FIXME: need to support MouseWheel scrolling, too
    void handleJavaMouseEvent(MouseEvent e) {
        super.handleJavaMouseEvent(e);
        int i = e.getID();
        switch (i) {
          case MouseEvent.MOUSE_PRESSED:
              mousePressed(e);
              break;
          case MouseEvent.MOUSE_RELEASED:
              mouseReleased(e);
              break;
          case MouseEvent.MOUSE_DRAGGED:
              mouseDragged(e);
              break;
        }
    }

    void handleJavaMouseWheelEvent(MouseWheelEvent e) {
        if (ListHelper.doWheelScroll(vsbVis ? vsb : null,
                                     hsbVis ? hsb : null, e)) {
            repaint();
        }
    }
    @SuppressWarnings("deprecation")
    void mousePressed(MouseEvent mouseEvent) {
        if (log.isLoggable(PlatformLogger.Level.FINER)) {
            log.finer(mouseEvent.toString() + ", hsb " + hsbVis + ", vsb " + vsbVis);
        }
        if (isEnabled() && mouseEvent.getButton() == MouseEvent.BUTTON1) {
            if (inWindow(mouseEvent.getX(), mouseEvent.getY())) {
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("Mouse press in items area");
                }
                active = WINDOW;
                int i = y2index(mouseEvent.getY());
                if (i >= 0) {
                    if (multipleSelections) {
                        if (isSelected(i)) {
                            // See 6243382 for more information
                            deselectItem(i);
                            eventIndex = i;
                            eventType = ItemEvent.DESELECTED;
                        }
                        else {
                            selectItem(i);
                            eventIndex = i;
                            eventType = ItemEvent.SELECTED;
                        }
                    }
                    // Backward-compatible bug: even if a single-select
                    // item is already selected, we send an ITEM_STATE_CHANGED/
                    // SELECTED event.  Engineer's Toolbox appears to rely on
                    // this.
                    //else if (!isSelected(i)) {
                    else {
                        selectItem(i);
                        eventIndex = i;
                        eventType = ItemEvent.SELECTED;
                    }
                    // Restoring Windows behaviour
                    // We should update focus index after "mouse pressed" event
                    setFocusIndex(i);
                    repaint(PAINT_FOCUS);
                } else {
                    // 6426186: reset variable to prevent action event
                    // if user clicks on unoccupied area of list
                    currentIndex = -1;
                }
            } else if (inVerticalScrollbar(mouseEvent.getX(), mouseEvent.getY())) {
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("Mouse press in vertical scrollbar");
                }
                active = VERSCROLLBAR;
                vsb.handleMouseEvent(mouseEvent.getID(),
                                     mouseEvent.getModifiers(),
                                     mouseEvent.getX() - (width - SCROLLBAR_WIDTH),
                                     mouseEvent.getY());
            } else if (inHorizontalScrollbar(mouseEvent.getX(), mouseEvent.getY())) {
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("Mouse press in horizontal scrollbar");
                }
                active = HORSCROLLBAR;
                hsb.handleMouseEvent(mouseEvent.getID(),
                                     mouseEvent.getModifiers(),
                                     mouseEvent.getX(),
                                     mouseEvent.getY() - (height - SCROLLBAR_WIDTH));

            }
            isMousePressed = true;
        }
    }
    @SuppressWarnings("deprecation")
    void mouseReleased(MouseEvent mouseEvent) {
        if (isEnabled() && mouseEvent.getButton() == MouseEvent.BUTTON1) {
            //winReleaseCursorFocus();
            int clickCount = mouseEvent.getClickCount();
            if (active == VERSCROLLBAR) {
                vsb.handleMouseEvent(mouseEvent.getID(),
                                     mouseEvent.getModifiers(),
                                     mouseEvent.getX()-(width-SCROLLBAR_WIDTH),
                                     mouseEvent.getY());
            } else if(active == HORSCROLLBAR) {
                hsb.handleMouseEvent(mouseEvent.getID(),
                                     mouseEvent.getModifiers(),
                                     mouseEvent.getX(),
                                     mouseEvent.getY()-(height-SCROLLBAR_WIDTH));
            } else if ( ( currentIndex >= 0 ) && ( clickCount >= 2 ) &&
                        ( clickCount % 2 == 0 ) ) {
                postEvent(new ActionEvent(target,
                                          ActionEvent.ACTION_PERFORMED,
                                          items.elementAt(currentIndex),
                                          mouseEvent.getWhen(),
                                          mouseEvent.getModifiers()));  // No ext mods
            } else if (active == WINDOW) {
                // See 6243382 for more information
                trackMouseReleasedScroll();

                if (eventType == ItemEvent.DESELECTED) {
                    assert multipleSelections : "Shouldn't get a deselect for a single-select List";
                    // Paint deselection the release
                    deselectItem(eventIndex);
                }
                if (eventType != NONE) {
                    postEvent(new ItemEvent((List)target,
                                ItemEvent.ITEM_STATE_CHANGED,
                                Integer.valueOf(eventIndex),
                                eventType));
                }
            }
            active = NONE;
            eventIndex = -1;
            eventType = NONE;
            isMousePressed = false;
        }
    }

    @SuppressWarnings("deprecation")
    void mouseDragged(MouseEvent mouseEvent) {
        // TODO: can you drag w/ any other buttons?  what about multiple buttons?
        if (isEnabled() &&
            (mouseEvent.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != 0) {
            if ((active == VERSCROLLBAR)) {
                vsb.handleMouseEvent(mouseEvent.getID(),
                                     mouseEvent.getModifiers(),
                                     mouseEvent.getX()-(width-SCROLLBAR_WIDTH),
                                     mouseEvent.getY());
            } else if ((active == HORSCROLLBAR)) {
                hsb.handleMouseEvent(mouseEvent.getID(),
                                     mouseEvent.getModifiers(),
                                     mouseEvent.getX(),
                                     mouseEvent.getY()-(height-SCROLLBAR_WIDTH));
            } else if (active == WINDOW) {
                int i = y2index(mouseEvent.getY());
                if (multipleSelections) {
                    // Multi-select only:
                    // If a selected item was pressed on and then dragged off
                    // of, cancel the pending deselect.
                    if (eventType == ItemEvent.DESELECTED) {
                        if (i != eventIndex) {
                            eventType = NONE;
                            eventIndex = -1;
                        }
                    }
                }
                else if (eventType == ItemEvent.SELECTED) {
                    // Single-select only:
                    // If an unselected item was pressed on, track the drag
                    // and select the item under the mouse

                    // See 6243382 for more information
                    trackMouseDraggedScroll(mouseEvent);

                    if (i >= 0 && !isSelected(i)) {
                        int oldSel = eventIndex;
                        selectItem(i);
                        eventIndex = i;
                        repaint(oldSel, eventIndex, PAINT_ITEMS);
                    }
                }
                // Restoring Windows behaviour
                // We should update focus index after "mouse dragged" event
                if (i >= 0) {
                    setFocusIndex(i);
                    repaint(PAINT_FOCUS);
                }
            }
        }
    }

    /*
     * Helper method for XListPeer with integrated vertical scrollbar.
     * Start or stop vertical scrolling when mouse dragged in / out the area of the list if it's required
     * Restoring Motif behavior
     * See 6243382 for more information
     */
    void trackMouseDraggedScroll(MouseEvent mouseEvent){

        if (vsb.beforeThumb(mouseEvent.getX(), mouseEvent.getY())) {
            vsb.setMode(AdjustmentEvent.UNIT_DECREMENT);
        } else {
            vsb.setMode(AdjustmentEvent.UNIT_INCREMENT);
        }

        if(mouseEvent.getY() < 0 || mouseEvent.getY() >= listHeight){
            if (!mouseDraggedOutVertically){
                mouseDraggedOutVertically = true;
                vsb.startScrollingInstance();
            }
        }else{
            if (mouseDraggedOutVertically){
                mouseDraggedOutVertically = false;
                vsb.stopScrollingInstance();
            }
        }

        if (hsb.beforeThumb(mouseEvent.getX(), mouseEvent.getY())) {
            hsb.setMode(AdjustmentEvent.UNIT_DECREMENT);
        } else {
            hsb.setMode(AdjustmentEvent.UNIT_INCREMENT);
        }

        if (mouseEvent.getX() < 0 || mouseEvent.getX() >= listWidth) {
            if (!mouseDraggedOutHorizontally){
                mouseDraggedOutHorizontally = true;
                hsb.startScrollingInstance();
            }
        }else{
            if (mouseDraggedOutHorizontally){
                mouseDraggedOutHorizontally = false;
                hsb.stopScrollingInstance();
            }
        }
    }

    /*
     * Helper method for XListPeer with integrated vertical scrollbar.
     * Stop vertical scrolling when mouse released in / out the area of the list if it's required
     * Restoring Motif behavior
     * see 6243382 for more information
     */
    void trackMouseReleasedScroll(){

        if (mouseDraggedOutVertically){
            mouseDraggedOutVertically = false;
            vsb.stopScrollingInstance();
        }

        if (mouseDraggedOutHorizontally){
            mouseDraggedOutHorizontally = false;
            hsb.stopScrollingInstance();
        }
    }

    void handleJavaKeyEvent(KeyEvent e) {
        switch(e.getID()) {
          case KeyEvent.KEY_PRESSED:
              if (!isMousePressed){
                  keyPressed(e);
              }
              break;
        }
    }
    @SuppressWarnings("deprecation")
    void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine(e.toString());
        }
        switch(keyCode) {
          case KeyEvent.VK_UP:
          case KeyEvent.VK_KP_UP: // TODO: I assume we also want this, too
              if (getFocusIndex() > 0) {
                  setFocusIndex(getFocusIndex()-1);
                  repaint(PAINT_HIDEFOCUS);
                  // If single-select, select the item
                  if (!multipleSelections) {
                      selectItem(getFocusIndex());
                      postEvent(new ItemEvent((List)target,
                                              ItemEvent.ITEM_STATE_CHANGED,
                                              Integer.valueOf(getFocusIndex()),
                                              ItemEvent.SELECTED));
                  }
                  if (isItemHidden(getFocusIndex())) {
                      makeVisible(getFocusIndex());
                  }
                  else {
                      repaint(PAINT_FOCUS);
                  }
              }
              break;
          case KeyEvent.VK_DOWN:
          case KeyEvent.VK_KP_DOWN: // TODO: I assume we also want this, too
              if (getFocusIndex() < items.size() - 1) {
                  setFocusIndex(getFocusIndex()+1);
                  repaint(PAINT_HIDEFOCUS);
                  // If single-select, select the item
                  if (!multipleSelections) {
                      selectItem(getFocusIndex());
                      postEvent(new ItemEvent((List)target,
                                              ItemEvent.ITEM_STATE_CHANGED,
                                              Integer.valueOf(getFocusIndex()),
                                              ItemEvent.SELECTED));
                  }
                  if (isItemHidden(getFocusIndex())) {
                      makeVisible(getFocusIndex());
                  }
                  else {
                      repaint(PAINT_FOCUS);
                  }
              }
              break;
          case KeyEvent.VK_PAGE_UP: {
              // Assumes that scrollbar does its own bounds-checking
              int previousValue = vsb.getValue();
              vsb.setValue(vsb.getValue() - vsb.getBlockIncrement());
              int currentValue = vsb.getValue();
              // 6190768 pressing pg-up on AWT multiple selection lists the items but no item event is triggered, on XToolkit
              // Restoring Motif behavior
              if (previousValue!=currentValue) {
                  setFocusIndex(Math.max(getFocusIndex()-itemsInWindow(), 0));
                  if (!multipleSelections){
                      selectItem(getFocusIndex());
                      postEvent(new ItemEvent((List)target,
                                              ItemEvent.ITEM_STATE_CHANGED,
                                              Integer.valueOf(getFocusIndex()),
                                              ItemEvent.SELECTED));
                  }
              }
              repaint();
              break;
          }
          case KeyEvent.VK_PAGE_DOWN: {
              // Assumes that scrollbar does its own bounds-checking
              int previousValue = vsb.getValue();
              vsb.setValue(vsb.getValue() + vsb.getBlockIncrement());
              int currentValue = vsb.getValue();
              // 6190768 pressing pg-down on AWT multiple selection list selects the items but no item event is triggered, on XToolkit
              // Restoring Motif behavior
              if (previousValue!=currentValue) {
                  setFocusIndex(Math.min(getFocusIndex() + itemsInWindow(), items.size()-1));
                  if (!multipleSelections){
                      selectItem(getFocusIndex());
                      postEvent(new ItemEvent((List)target,
                                              ItemEvent.ITEM_STATE_CHANGED,
                                              Integer.valueOf(getFocusIndex()),
                                              ItemEvent.SELECTED));
                  }
              }
              repaint();
              break;
          }
          case KeyEvent.VK_LEFT:
          case KeyEvent.VK_KP_LEFT:
              if (hsbVis & hsb.getValue() > 0) {
                  hsb.setValue(hsb.getValue() - HORIZ_SCROLL_AMT);
                  repaint();
              }
              break;
          case KeyEvent.VK_RIGHT:
          case KeyEvent.VK_KP_RIGHT:
              if (hsbVis) { // Should check if already at end
                  hsb.setValue(hsb.getValue() + HORIZ_SCROLL_AMT);
                  repaint();
              }
              break;
          // 6190778 CTRL + HOME, CTRL + END keys do not work properly for list on XToolkit
          // Restoring Motif behavior
          case KeyEvent.VK_HOME:
              if (!e.isControlDown() || ((List)target).getItemCount() <= 0)
                  break;
              if (vsbVis) {
                  vsb.setValue(vsb.getMinimum());
              }
              setFocusIndex(0);
              if (!multipleSelections) {
                  selectItem(getFocusIndex());
                  postEvent(new ItemEvent((List)target,
                                          ItemEvent.ITEM_STATE_CHANGED,
                                          Integer.valueOf(getFocusIndex()),
                                          ItemEvent.SELECTED));
              }
              repaint();
              break;
          case KeyEvent.VK_END:
              if (!e.isControlDown() || ((List)target).getItemCount() <= 0)
                  break;
              if (vsbVis) {
                  vsb.setValue(vsb.getMaximum());
              }
              setFocusIndex(items.size()-1);
              if (!multipleSelections) {
                  selectItem(getFocusIndex());
                  postEvent(new ItemEvent((List)target,
                                          ItemEvent.ITEM_STATE_CHANGED,
                                          Integer.valueOf(getFocusIndex()),
                                          ItemEvent.SELECTED));
              }
              repaint();
              break;
          case KeyEvent.VK_SPACE:
              // Fixed 6299853: XToolkit: Pressing space triggers ItemStateChanged event after List.removeAll called
              // If getFocusIndex() is less than 0, the event will not be triggered when space pressed
              if (getFocusIndex() < 0 || ((List)target).getItemCount() <= 0) {
                  break;
              }

              boolean isSelected = isSelected(getFocusIndex());

              // Spacebar only deselects for multi-select Lists
              if (multipleSelections && isSelected) {
                  deselectItem(getFocusIndex());
                  postEvent(new ItemEvent((List)target,
                                          ItemEvent.ITEM_STATE_CHANGED,
                                          Integer.valueOf(getFocusIndex()),
                                          ItemEvent.DESELECTED));
              }
              else if (!isSelected) { // Note: this changes the Solaris/Linux
                  // behavior to match that of win32.
                  // That is, pressing space bar on a
                  // single-select list when the focused
                  // item is already selected does NOT
                  // send an ItemEvent.SELECTED event.
                  selectItem(getFocusIndex());
                  postEvent(new ItemEvent((List)target,
                                          ItemEvent.ITEM_STATE_CHANGED,
                                          Integer.valueOf(getFocusIndex()),
                                          ItemEvent.SELECTED));
              }
              break;
          case KeyEvent.VK_ENTER:
              // It looks to me like there are bugs as well as inconsistencies
              // in the way the Enter key is handled by both Solaris and Windows.
              // So for now in XAWT, I'm going to simply go by what the List docs
              // say: "AWT also generates an action event when the user presses
              // the return key while an item in the list is selected."
              if (selected.length > 0) {
                  postEvent(new ActionEvent((List)target,
                                            ActionEvent.ACTION_PERFORMED,
                                            items.elementAt(getFocusIndex()),
                                            e.getWhen(),
                                            e.getModifiers()));  // ActionEvent doesn't have
                  // extended modifiers.
              }
              break;
        }
    }

    /**
     * return value from the scrollbar
     */
    public void notifyValue(XScrollbar obj, int type, int v, boolean isAdjusting) {

        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("Notify value changed on " + obj + " to " + v);
        }
        int value = obj.getValue();
        if (obj == vsb) {
            scrollVertical(v - value);

            // See 6243382 for more information
            int oldSel = eventIndex;
            int newSel = eventIndex+v-value;
            if (mouseDraggedOutVertically && !isSelected(newSel)){
                selectItem(newSel);
                eventIndex = newSel;
                repaint(oldSel, eventIndex, PAINT_ITEMS);
                // Scrolling select() should also set the focus index
                // Otherwise, the updating of the 'focusIndex' variable will be incorrect
                // if user drag mouse out of the area of the list
                setFocusIndex(newSel);
                repaint(PAINT_FOCUS);
            }

        } else if ((XHorizontalScrollbar)obj == hsb) {
            scrollHorizontal(v - value);
        }

    }

    /**
     * deselect all items in List
     */
    private void deselectAllItems() {
        selected = new int [0];
        repaint(PAINT_ITEMS);
    }

    /**
     * set multiple selections
     */
    public void setMultipleSelections(boolean v) {
        if (multipleSelections != v) {
            if ( !v) {
                int selPos = ( isSelected( focusIndex )) ? focusIndex: -1;
                deselectAllItems();
                if (selPos != -1){
                    selectItem(selPos);
                }
            }
            multipleSelections = v;
        }
    }

    /**
     * add an item
     * if the index of the item is < 0 or >= than items.size()
     * then add the item to the end of the list
     */
    public void addItem(String item, int i) {
        int oldMaxLength = maxLength;
        boolean hsbWasVis = hsbVis;
        boolean vsbWasVis = vsbVis;

        int addedIndex = 0; // Index where the new item ended up
        if (i < 0 || i >= items.size()) {
            i = -1;
        }

        // Why we set this variable to -1 in spite of the fact that selected[] is changed in other way?
        // It's not clear how to reproduce incorrect behaviour based on this assignment
        // since before using this variable (mouseReleased) we certainly update it to correct value
        // So we don't modify this behaviour now
        currentIndex = -1;

        if (i == -1) {
            items.addElement(item);
            i = 0;              // fix the math for the paintItems test
            addedIndex = items.size() - 1;
        } else {
            items.insertElementAt(item, i);
            addedIndex = i;
            for (int j = 0 ; j < selected.length ; j++) {
                if (selected[j] >= i) {
                    selected[j] += 1;
                }
            }
        }
        if (log.isLoggable(PlatformLogger.Level.FINER)) {
            log.finer("Adding item '" + item + "' to " + addedIndex);
        }

        // Update maxLength
        boolean repaintItems = !isItemHidden(addedIndex);
        maxLength = Math.max(maxLength, getItemWidth(addedIndex));
        layout();

        int options = 0;
        if (vsbVis != vsbWasVis || hsbVis != hsbWasVis) {
            // Scrollbars are being added or removed, so we must repaint all
            options = PAINT_ALL;
        }
        else {
            options = (repaintItems ? (PAINT_ITEMS):0)
                | ((maxLength != oldMaxLength || (hsbWasVis ^ hsbVis))?(PAINT_HSCROLL):0)
                | ((vsb.needsRepaint())?(PAINT_VSCROLL):0);

        }
        if (log.isLoggable(PlatformLogger.Level.FINEST)) {
            log.finest("Last visible: " + getLastVisibleItem() +
            ", hsb changed : " + (hsbWasVis ^ hsbVis) + ", items changed " + repaintItems);
        }
        repaint(addedIndex, getLastVisibleItem(), options);
    }

    /**
     * delete items starting with s (start position) to e (end position) including s and e
     * if s < 0 then s = 0
     * if e >= items.size() then e = items.size() - 1
     */
    public void delItems(int s, int e) {
        // save the current state of the scrollbars
        boolean hsbWasVisible = hsbVis;
        boolean vsbWasVisible = vsbVis;
        int oldLastDisplayed = lastItemDisplayed();

        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("Deleting from " + s + " to " + e);
        }

        if (log.isLoggable(PlatformLogger.Level.FINEST)) {
            log.finest("Last displayed item: " + oldLastDisplayed + ", items in window " + itemsInWindow() +
            ", size " + items.size());
        }

        if (items.size() == 0) {
            return;
        }

        // if user passed in flipped args, reverse them
        if (s > e) {
            int tmp = s;
            s = e;
            e = tmp;
        }

        // check for starting point less than zero
        if (s < 0) {
            s = 0;
        }

        // check for end point greater than the size of the list
        if (e >= items.size()) {
            e = items.size() - 1;
        }

        // determine whether we're going to delete any visible elements
        // repaint must also be done if scrollbars appear/disappear, which
        // can happen from removing a non-showing list item
        /*
          boolean repaintNeeded =
          ((s <= lastItemDisplayed()) && (e >= vsb.getValue()));
        */
        boolean repaintNeeded = (s >= getFirstVisibleItem() && s <= getLastVisibleItem());

        // delete the items out of the items list and out of the selected list
        for (int i = s ; i <= e ; i++) {
            items.removeElementAt(s);
            int j = posInSel(i);
            if (j != -1) {
                int[] newsel = new int[selected.length - 1];
                System.arraycopy(selected, 0, newsel, 0, j);
                System.arraycopy(selected, j + 1, newsel, j, selected.length - (j + 1));
                selected = newsel;
            }

        }

        // update the indexes in the selected array
        int diff = (e - s) + 1;
        for (int i = 0 ; i < selected.length ; i++) {
            if (selected[i] > e) {
                selected[i] -= diff;
            }
        }

        int options = PAINT_VSCROLL;
        // focusedIndex updating according to native (Window, Motif) behaviour
        if (getFocusIndex() > e) {
            setFocusIndex(getFocusIndex() - (e - s + 1));
            options |= PAINT_FOCUS;
        } else if (getFocusIndex() >= s && getFocusIndex() <= e) {
            // Fixed 6299858: PIT. Focused border not shown on List if selected item is removed, XToolkit
            // We should set focus to new first item if the current first item was removed
            // except if the list is empty
            int focusBound = (items.size() > 0) ? 0 : -1;
            setFocusIndex(Math.max(s-1, focusBound));
            options |= PAINT_FOCUS;
        }

        if (log.isLoggable(PlatformLogger.Level.FINEST)) {
            log.finest("Multiple selections: " + multipleSelections);
        }

        // update vsb.val
        if (vsb.getValue() >= s) {
            if (vsb.getValue() <= e) {
                vsb.setValue(e+1 - diff);
            } else {
                vsb.setValue(vsb.getValue() - diff);
            }
        }

        int oldMaxLength = maxLength;
        maxLength = maxLength();
        if (maxLength != oldMaxLength) {
            // Width of the items changed affecting the range of
            // horizontal scrollbar
            options |= PAINT_HSCROLL;
        }
        layout();
        repaintNeeded |= (vsbWasVisible ^ vsbVis) || (hsbWasVisible ^ hsbVis); // If scrollbars visibility changed
        if (repaintNeeded) {
            options |= PAINT_ALL;
        }
        repaint(s, oldLastDisplayed, options);
    }

    /**
     * ListPeer method
     */
    public void select(int index) {
        // Programmatic select() should also set the focus index
        setFocusIndex(index);
        repaint(PAINT_FOCUS);
        selectItem(index);
    }

    /**
     * select the index
     * redraw the list to the screen
     */
    void selectItem(int index) {
        // NOTE: instead of recalculating and the calling repaint(), painting
        // is done immediately

        // 6190746 List does not trigger ActionEvent when double clicking a programmatically selected item, XToolkit
        // If we invoke select(int) before setVisible(boolean), then variable currentIndex will equals -1. At the same time isSelected may be true.
        // Restoring Motif behavior
        currentIndex = index;

        if (isSelected(index)) {
            return;
        }
        if (!multipleSelections) {
            if (selected.length == 0) { // No current selection
                selected = new int[1];
                selected[0] = index;
            }
            else {
                int oldSel = selected[0];
                selected[0] = index;
                if (!isItemHidden(oldSel)) {
                    // Only bother painting if item is visible (4895367)
                    repaint(oldSel, oldSel, PAINT_ITEMS);
                }
            }
        } else {
            // insert "index" into the selection array
            int[] newsel = new int[selected.length + 1];
            int i = 0;
            while (i < selected.length && index > selected[i]) {
                newsel[i] = selected[i];
                i++;
            }
            newsel[i] = index;
            System.arraycopy(selected, i, newsel, i+1, selected.length - i);
            selected = newsel;
        }
        if (!isItemHidden(index)) {
            // Only bother painting if item is visible (4895367)
            repaint(index, index, PAINT_ITEMS);
        }
    }

    /**
     * ListPeer method
     * focusedIndex isn't updated according to native (Window, Motif) behaviour
     */
    public void deselect(int index) {
        deselectItem(index);
    }

    /**
     * deselect the index
     * redraw the list to the screen
     */
    void deselectItem(int index) {
        if (!isSelected(index)) {
            return;
        }
        if (!multipleSelections) {
            // TODO: keep an int[0] and int[1] around and just use them instead
            // creating new ones all the time
            selected = new int[0];
        } else {
            int i = posInSel(index);
            int[] newsel = new int[selected.length - 1];
            System.arraycopy(selected, 0, newsel, 0, i);
            System.arraycopy(selected, i+1, newsel, i, selected.length - (i+1));
            selected = newsel;
        }
        currentIndex = index;
        if (!isItemHidden(index)) {
            // Only bother repainting if item is visible
            repaint(index, index, PAINT_ITEMS);
        }
    }

    /**
     * ensure that the given index is visible, scrolling the List
     * if necessary, or doing nothing if the item is already visible.
     * The List must be repainted for changes to be visible.
     */
    public void makeVisible(int index) {
        if (index < 0 || index >= items.size()) {
            return;
        }
        if (isItemHidden(index)) {  // Do I really need to call this?
            // If index is above the top, scroll up
            if (index < vsb.getValue()) {
                scrollVertical(index - vsb.getValue());
            }
            // If index is below the bottom, scroll down
            else if (index > lastItemDisplayed()) {
                int val = index - lastItemDisplayed();
                scrollVertical(val);
            }
        }
    }

    /**
     * clear
     */
    public void clear() {
        selected = new int[0];
        items = new Vector<>();
        currentIndex = -1;
        // Fixed 6291736: ITEM_STATE_CHANGED triggered after List.removeAll(), XToolkit
        // We should update 'focusIndex' variable more carefully
        setFocusIndex(-1);
        vsb.setValue(0);
        maxLength = 0;
        layout();
        repaint();
    }

    /**
     * return the selected indexes
     */
    public int[] getSelectedIndexes() {
        return selected;
    }

    /**
     * return the y value of the given index "i".
     * the y value represents the top of the text
     * NOTE: index can be larger than items.size as long
     * as it can fit the window
     */
    int index2y(int index) {
        int h = getItemHeight();

        //if (index < vsb.getValue() || index > vsb.getValue() + itemsInWindow()) {
        return MARGIN + ((index - vsb.getValue()) * h) + SPACE;
    }

    /* return true if the y is a valid y coordinate for
     *  a VISIBLE list item, otherwise returns false
     */
    boolean validY(int y) {

        int shown = itemsDisplayed();
        int lastY = shown * getItemHeight() + MARGIN;

        if (shown == itemsInWindow()) {
            lastY += MARGIN;
        }

        if (y < 0 || y >= lastY) {
            return false;
        }

        return true;
    }

    /**
     * return the position of the index in the selected array
     * if the index isn't in the array selected return -1;
     */
    int posInSel(int index) {
        for (int i = 0 ; i < selected.length ; i++) {
            if (index == selected[i]) {
                return i;
            }
        }
        return -1;
    }

    boolean isIndexDisplayed(int idx) {
        int lastDisplayed = lastItemDisplayed();

        return idx <= lastDisplayed &&
            idx >= Math.max(0, lastDisplayed - itemsInWindow() + 1);
    }

    /**
     * returns index of last item displayed in the List
     */
    int lastItemDisplayed() {
        int n = itemsInWindow();
        return (Math.min(items.size() - 1, (vsb.getValue() + n) - 1));
    }

    /**
     * returns whether the given index is currently scrolled off the top or
     * bottom of the List.
     */
    boolean isItemHidden(int index) {
        return index < vsb.getValue() ||
            index >= vsb.getValue() + itemsInWindow();
    }

    /**
     * returns the width of the list portion of the component (accounts for
     * presence of vertical scrollbar)
     */
    int getListWidth() {
        return vsbVis ? width - SCROLLBAR_AREA : width;
    }

    /**
     * returns number of  items actually displayed in the List
     */
    int itemsDisplayed() {

        return (Math.min(items.size()-vsb.getValue(), itemsInWindow()));

    }

    /**
     * scrollVertical
     * y is the number of items to scroll
     */
    void scrollVertical(int y) {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("Scrolling vertically by " + y);
        }
        int itemsInWin = itemsInWindow();
        int h = getItemHeight();
        int pixelsToScroll = y * h;

        if (vsb.getValue() < -y) {
            y = -vsb.getValue();
        }
        vsb.setValue(vsb.getValue() + y);

        Rectangle source = null;
        Point distance = null;
        int firstItem = 0, lastItem = 0;
        int options = PAINT_HIDEFOCUS | PAINT_ITEMS | PAINT_VSCROLL | PAINT_FOCUS;
        if (y > 0) {
            if (y < itemsInWin) {
                source = new Rectangle(MARGIN, MARGIN + pixelsToScroll, width - SCROLLBAR_AREA, h * (itemsInWin - y - 1)-1);
                distance = new Point(0, -pixelsToScroll);
                options |= COPY_AREA;
            }
            firstItem = vsb.getValue() + itemsInWin - y - 1;
            lastItem = vsb.getValue() + itemsInWin - 1;

        } else if (y < 0) {
            if (y + itemsInWindow() > 0) {
                source = new Rectangle(MARGIN, MARGIN, width - SCROLLBAR_AREA, h * (itemsInWin + y));
                distance = new Point(0, -pixelsToScroll);
                options |= COPY_AREA;
            }
            firstItem = vsb.getValue();
            lastItem = Math.min(getLastVisibleItem(), vsb.getValue() + -y);
        }
        repaint(firstItem, lastItem, options, source, distance);
    }

    /**
     * scrollHorizontal
     * x is the number of pixels to scroll
     */
    void scrollHorizontal(int x) {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("Scrolling horizontally by " + y);
        }
        int w = getListWidth();
        w -= ((2 * SPACE) + (2 * MARGIN));
        int h = height - (SCROLLBAR_AREA + (2 * MARGIN));
        hsb.setValue(hsb.getValue() + x);

        int options = PAINT_ITEMS | PAINT_HSCROLL;

        Rectangle source = null;
        Point distance = null;
        if (x < 0) {
            source = new Rectangle(MARGIN + SPACE, MARGIN, w + x, h);
            distance = new Point(-x, 0);
            options |= COPY_AREA;
        } else if (x > 0) {
            source = new Rectangle(MARGIN + SPACE + x, MARGIN, w - x, h);
            distance = new Point(-x, 0);
            options |= COPY_AREA;
        }
        repaint(vsb.getValue(), lastItemDisplayed(), options, source, distance);
    }

    /**
     * return the index
     */
    int y2index(int y) {
        if (!validY(y)) {
            return -1;
        }

        int i = (y - MARGIN) / getItemHeight() + vsb.getValue();
        int last = lastItemDisplayed();

        if (i > last) {
            i = last;
        }

        return i;

    }

    /**
     * is the index "index" selected
     */
    boolean isSelected(int index) {
        if (eventType == ItemEvent.SELECTED && index == eventIndex) {
            return true;
        }
        for (int i = 0 ; i < selected.length ; i++) {
            if (selected[i] == index) {
                return true;
            }
        }
        return false;
    }

    /**
     * return the number of items that can fit
     * in the current window
     */
    int itemsInWindow(boolean scrollbarVisible) {
        int h;
        if (scrollbarVisible) {
            h = height - ((2 * MARGIN) + SCROLLBAR_AREA);
        } else {
            h = height - 2*MARGIN;
        }
        return (h / getItemHeight());
    }

    int itemsInWindow() {
        return itemsInWindow(hsbVis);
    }

    /**
     * return true if the x and y position is in the horizontal scrollbar
     */
    boolean inHorizontalScrollbar(int x, int y) {
        int w = getListWidth();
        int h = height - SCROLLBAR_WIDTH;
        return (hsbVis &&  (x >= 0) && (x <= w) && (y > h));
    }

    /**
     * return true if the x and y position is in the verticalscrollbar
     */
    boolean inVerticalScrollbar(int x, int y) {
        int w = width - SCROLLBAR_WIDTH;
        int h = hsbVis ? height - SCROLLBAR_AREA : height;
        return (vsbVis && (x > w) && (y >= 0) && (y <= h));
    }

    /**
     * return true if the x and y position is in the window
     */
    boolean inWindow(int x, int y) {
        int w = getListWidth();
        int h = hsbVis ? height - SCROLLBAR_AREA : height;
        return ((x >= 0) && (x <= w)) && ((y >= 0) && (y <= h));
    }

    /**
     * return true if vertical scrollbar is visible and false otherwise;
     * hsbVisible is the visibility of the horizontal scrollbar
     */
    boolean vsbIsVisible(boolean hsbVisible){
        return (items.size() > itemsInWindow(hsbVisible));
    }

    /**
     * return true if horizontal scrollbar is visible and false otherwise;
     * vsbVisible is the visibility of the vertical scrollbar
     */
    boolean hsbIsVisible(boolean vsbVisible){
        int w = width - ((2*SPACE) + (2*MARGIN) + (vsbVisible ? SCROLLBAR_AREA : 0));
        return (maxLength > w);
    }

    /*
     * Returns true if the event has been handled and should not be
     * posted to Java
     */
    boolean prePostEvent(final AWTEvent e) {
        if (e instanceof MouseEvent) {
            return prePostMouseEvent((MouseEvent)e);
        }
        return super.prePostEvent(e);
    }

    /*
     * Fixed 6240151: XToolkit: Dragging the List scrollbar initiates DnD
     * To be compatible with Motif, MouseEvent originated on the scrollbar
     * should be sent into Java in this way:
     * - post: MOUSE_ENTERED, MOUSE_EXITED, MOUSE_MOVED
     * - don't post: MOUSE_PRESSED, MOUSE_RELEASED, MOUSE_CLICKED, MOUSE_DRAGGED
     */
    boolean prePostMouseEvent(final MouseEvent me){
        if (getToplevelXWindow().isModalBlocked()) {
            return false;
        }

        int eventId = me.getID();

        if (eventId == MouseEvent.MOUSE_MOVED)
        {
            // only for performance improvement
        }else if((eventId == MouseEvent.MOUSE_DRAGGED ||
                  eventId == MouseEvent.MOUSE_RELEASED) &&
                 isScrollBarOriginated)
        {
            if (eventId == MouseEvent.MOUSE_RELEASED) {
                isScrollBarOriginated = false;
            }
            handleJavaMouseEventOnEDT(me);
            return true;
        }else if ((eventId == MouseEvent.MOUSE_PRESSED ||
                   eventId == MouseEvent.MOUSE_CLICKED) &&
                  (inVerticalScrollbar(me.getX(), me.getY()) ||
                   inHorizontalScrollbar(me.getX(), me.getY())))
        {
            if (eventId == MouseEvent.MOUSE_PRESSED) {
                isScrollBarOriginated = true;
            }
            handleJavaMouseEventOnEDT(me);
            return true;
        }
        return false;
    }

    /*
     * Do handleJavaMouseEvent on EDT
     */
    void handleJavaMouseEventOnEDT(final MouseEvent me){
        InvocationEvent ev = new InvocationEvent(target, new Runnable() {
            public void run() {
                handleJavaMouseEvent(me);
            }
        });
        postEvent(ev);
    }

    /*
     * Fixed 5010944: List's rows overlap one another
     * The bug is due to incorrent caching of the list item size
     * So we should recalculate font metrics on setFont
     */
    public void setFont(Font f) {
        if (!Objects.equals(getFont(), f)) {
            super.setFont(f);
            initFontMetrics();
            layout();
            repaint();
        }
    }

    /**
     * Sometimes painter is called on Toolkit thread, so the lock sequence is:
     *     awtLock -> Painter -> awtLock
     * Sometimes it is called on other threads:
     *     Painter -> awtLock
     * Since we can't guarantee the sequence, use awtLock.
     */
    class ListPainter {
        VolatileImage buffer;
        Color[] colors;

        private Color getListForeground() {
            if (fgColorSet) {
                return colors[FOREGROUND_COLOR];
            }
            else {
            return SystemColor.textText;
            }
        }
        private Color getListBackground() {
            if (bgColorSet) {
                return colors[BACKGROUND_COLOR];
            }
            else {
                return SystemColor.text;
            }
        }

        private Color getDisabledColor() {
            Color backgroundColor = getListBackground();
            Color foregroundColor = getListForeground();
            return (backgroundColor.equals(Color.BLACK)) ? foregroundColor.darker() : backgroundColor.darker();
        }

        private boolean createBuffer() {
            VolatileImage localBuffer = null;
            XToolkit.awtLock();
            try {
                localBuffer = buffer;
            } finally {
                XToolkit.awtUnlock();
            }

            if (localBuffer == null) {
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("Creating buffer " + width + "x" + height);
                }
                // use GraphicsConfig.cCVI() instead of Component.cVI(),
                // because the latter may cause a deadlock with the tree lock
                localBuffer =
                    graphicsConfig.createCompatibleVolatileImage(width+1,
                                                                 height+1);
            }
            XToolkit.awtLock();
            try {
                if (buffer == null) {
                    buffer = localBuffer;
                    return true;
                }
            } finally {
                XToolkit.awtUnlock();
            }
            return false;
        }

        public void invalidate() {
            XToolkit.awtLock();
            try {
                if (buffer != null) {
                    buffer.flush();
                }
                buffer = null;
            } finally {
                XToolkit.awtUnlock();
            }
        }

        private void paint(Graphics listG, int firstItem, int lastItem, int options) {
            paint(listG, firstItem, lastItem, options, null, null);
        }

        private void paint(Graphics listG, int firstItem, int lastItem, int options,
                           Rectangle source, Point distance) {
            if (log.isLoggable(PlatformLogger.Level.FINER)) {
                log.finer("Repaint from " + firstItem + " to " + lastItem + " options " + options);
            }
            if (firstItem > lastItem) {
                int t = lastItem;
                lastItem = firstItem;
                firstItem = t;
            }
            if (firstItem < 0) {
                firstItem = 0;
            }
            colors = getGUIcolors();
            VolatileImage localBuffer = null;
            do {
                XToolkit.awtLock();
                try {
                    if (createBuffer()) {
                        // First time created buffer should be painted over at full.
                        options = PAINT_ALL;
                    }
                    localBuffer = buffer;
                } finally {
                    XToolkit.awtUnlock();
                }
                switch (localBuffer.validate(getGraphicsConfiguration())) {
                  case VolatileImage.IMAGE_INCOMPATIBLE:
                      invalidate();
                      options = PAINT_ALL;
                      continue;
                  case VolatileImage.IMAGE_RESTORED:
                      options = PAINT_ALL;
                }
                Graphics g = localBuffer.createGraphics();

                // Note that the order of the following painting operations
                // should not be modified
                try {
                    g.setFont(getFont());

                    // hiding the focus rectangle must be done prior to copying
                    // area and so this is the first action to be performed
                    if ((options & (PAINT_HIDEFOCUS)) != 0) {
                        paintFocus(g, PAINT_HIDEFOCUS);
                    }
                    /*
                     * The shift of the component contents occurs while someone
                     * scrolls the component, the only purpose of the shift is to
                     * increase the painting performance. The shift should be done
                     * prior to painting any area (except hiding focus) and actually
                     * it should never be done jointly with erase background.
                     */
                    if ((options & COPY_AREA) != 0) {
                        g.copyArea(source.x, source.y, source.width, source.height,
                            distance.x, distance.y);
                    }
                    if ((options & PAINT_BACKGROUND) != 0) {
                        paintBackground(g);
                        // Since we made full erase update items
                        firstItem = getFirstVisibleItem();
                        lastItem = getLastVisibleItem();
                    }
                    if ((options & PAINT_ITEMS) != 0) {
                        paintItems(g, firstItem, lastItem, options);
                    }
                    if ((options & PAINT_VSCROLL) != 0 && vsbVis) {
                        g.setClip(getVScrollBarRec());
                        paintVerScrollbar(g, true);
                    }
                    if ((options & PAINT_HSCROLL) != 0 && hsbVis) {
                        g.setClip(getHScrollBarRec());
                        paintHorScrollbar(g, true);
                    }
                    if ((options & (PAINT_FOCUS)) != 0) {
                        paintFocus(g, PAINT_FOCUS);
                    }
                } finally {
                    g.dispose();
                }
            } while (localBuffer.contentsLost());
            listG.drawImage(localBuffer, 0, 0, null);
        }

        private void paintBackground(Graphics g) {
            g.setColor(SystemColor.window);
            g.fillRect(0, 0, width, height);
            g.setColor(getListBackground());
            g.fillRect(0, 0, listWidth, listHeight);
            draw3DRect(g, getSystemColors(), 0, 0, listWidth - 1, listHeight - 1, false);
        }

        private void paintItems(Graphics g, int firstItem, int lastItem, int options) {
            if (log.isLoggable(PlatformLogger.Level.FINER)) {
                log.finer("Painting items from " + firstItem + " to " + lastItem + ", focused " + focusIndex + ", first " + getFirstVisibleItem() + ", last " + getLastVisibleItem());
            }

            firstItem = Math.max(getFirstVisibleItem(), firstItem);
            if (firstItem > lastItem) {
                int t = lastItem;
                lastItem = firstItem;
                firstItem = t;
            }
            firstItem = Math.max(getFirstVisibleItem(), firstItem);
            lastItem = Math.min(lastItem, items.size()-1);

            if (log.isLoggable(PlatformLogger.Level.FINER)) {
                log.finer("Actually painting items from " + firstItem + " to " + lastItem +
                          ", items in window " + itemsInWindow());
            }
            for (int i = firstItem; i <= lastItem; i++) {
                paintItem(g, i);
            }
        }

        private void paintItem(Graphics g, int index) {
            if (log.isLoggable(PlatformLogger.Level.FINEST)) {
                log.finest("Painting item " + index);
            }
            // 4895367 - only paint items which are visible
            if (!isItemHidden(index)) {
                Shape clip = g.getClip();
                int w = getItemWidth();
                int h = getItemHeight();
                int y = getItemY(index);
                int x = getItemX();
                if (log.isLoggable(PlatformLogger.Level.FINEST)) {
                    log.finest("Setting clip " + new Rectangle(x, y, w - (SPACE*2), h-(SPACE*2)));
                }
                g.setClip(x, y, w - (SPACE*2), h-(SPACE*2));

                // Always paint the background so that focus is unpainted in
                // multiselect mode
                if (isSelected(index)) {
                    if (log.isLoggable(PlatformLogger.Level.FINEST)) {
                        log.finest("Painted item is selected");
                    }
                    g.setColor(getListForeground());
                } else {
                    g.setColor(getListBackground());
                }
                if (log.isLoggable(PlatformLogger.Level.FINEST)) {
                    log.finest("Filling " + new Rectangle(x, y, w, h));
                }
                g.fillRect(x, y, w, h);

                if (index <= getLastVisibleItem() && index < items.size()) {
                    if (!isEnabled()){
                        g.setColor(getDisabledColor());
                    } else if (isSelected(index)) {
                        g.setColor(getListBackground());
                    } else {
                        g.setColor(getListForeground());
                    }
                    String str = items.elementAt(index);
                    g.drawString(str, x - hsb.getValue(), y + fontAscent);
                } else {
                    // Clear the remaining area around the item - focus area and the rest of border
                    g.setClip(x, y, listWidth, h);
                    g.setColor(getListBackground());
                    g.fillRect(x, y, listWidth, h);
                }
                g.setClip(clip);
            }
        }

        void paintScrollBar(XScrollbar scr, Graphics g, int x, int y, int width, int height, boolean paintAll) {
            if (log.isLoggable(PlatformLogger.Level.FINEST)) {
                log.finest("Painting scrollbar " + scr + " width " +
                width + " height " + height + ", paintAll " + paintAll);
            }
            g.translate(x, y);
            scr.paint(g, getSystemColors(), paintAll);
            g.translate(-x, -y);
        }

        /**
         * Paint the horizontal scrollbar to the screen
         *
         * @param g the graphics context to draw into
         * @param paintAll paint the whole scrollbar if true, just the thumb if false
         */
        void paintHorScrollbar(Graphics g, boolean paintAll) {
            int w = getListWidth();
            paintScrollBar(hsb, g, 0, height - (SCROLLBAR_WIDTH), w, SCROLLBAR_WIDTH, paintAll);
        }

        /**
         * Paint the vertical scrollbar to the screen
         *
         * @param g the graphics context to draw into
         * @param paintAll paint the whole scrollbar if true, just the thumb if false
         */
        void paintVerScrollbar(Graphics g, boolean paintAll) {
            int h = height - (hsbVis ? (SCROLLBAR_AREA-2) : 0);
            paintScrollBar(vsb, g, width - SCROLLBAR_WIDTH, 0, SCROLLBAR_WIDTH - 2, h, paintAll);
        }


        private Rectangle prevFocusRect;
        private void paintFocus(Graphics g, int options) {
            boolean paintFocus = (options & PAINT_FOCUS) != 0;
            if (paintFocus && !hasFocus()) {
                paintFocus = false;
            }
            if (log.isLoggable(PlatformLogger.Level.FINE)) {
                log.fine("Painting focus, focus index " + getFocusIndex() + ", focus is " +
                         (isItemHidden(getFocusIndex())?("invisible"):("visible")) + ", paint focus is " + paintFocus);
            }
            Shape clip = g.getClip();
            g.setClip(0, 0, listWidth, listHeight);
            if (log.isLoggable(PlatformLogger.Level.FINEST)) {
                log.finest("Setting focus clip " + new Rectangle(0, 0, listWidth, listHeight));
            }
            Rectangle rect = getFocusRect();
            if (prevFocusRect != null) {
                // Erase focus rect
                if (log.isLoggable(PlatformLogger.Level.FINEST)) {
                    log.finest("Erasing previous focus rect " + prevFocusRect);
                }
                g.setColor(getListBackground());
                g.drawRect(prevFocusRect.x, prevFocusRect.y, prevFocusRect.width, prevFocusRect.height);
                prevFocusRect = null;
            }
            if (paintFocus) {
                // Paint new
                if (log.isLoggable(PlatformLogger.Level.FINEST)) {
                    log.finest("Painting focus rect " + rect);
                }
                g.setColor(getListForeground());  // Focus color is always black on Linux
                g.drawRect(rect.x, rect.y, rect.width, rect.height);
                prevFocusRect = rect;
            }
            g.setClip(clip);
        }
    }
}
