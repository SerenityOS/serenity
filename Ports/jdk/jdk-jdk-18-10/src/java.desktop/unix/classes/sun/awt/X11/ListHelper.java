/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.AdjustmentEvent;
import java.util.ArrayList;
import java.util.Iterator;
import sun.util.logging.PlatformLogger;

// FIXME: implement multi-select
/*
 * Class to paint a list of items, possibly with scrollbars
 * This class paints all items with the same font
 * For now, this class manages the list of items and painting thereof, but not
 * posting of Item or ActionEvents
 */
final class ListHelper implements XScrollbarClient {
    private static final PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11.ListHelper");

    private final int FOCUS_INSET = 1;

    private final int BORDER_WIDTH; // Width of border drawn around the list
                                    // of items
    private final int ITEM_MARGIN;  // Margin between the border of the list
                                    // of items and item's bg, and between
                                    // items
    private final int TEXT_SPACE;   // Space between the edge of an item and
                                    // the text

    private final int SCROLLBAR_WIDTH;  // Width of a scrollbar

    private java.util.List<String> items;        // List of items

    // TODO: maybe this would be better as a simple int[]
    private java.util.List<Integer> selected;     // List of selected items
    private boolean multiSelect;         // Can multiple items be selected
                                         // at once?
    private int focusedIndex;

    private int maxVisItems;             // # items visible without a vsb
    private XVerticalScrollbar vsb;      // null if unsupported
    private boolean vsbVis;
    private XHorizontalScrollbar hsb;    // null if unsupported
    private boolean hsbVis;

    private Font font;
    private FontMetrics fm;

    private XWindow peer;   // So far, only needed for painting
                            // on notifyValue()
    private Color[] colors; // Passed in for painting on notifyValue()

    // Holds the true if mouse is dragging outside of the area of the list
    // The flag is used at the moment of the dragging and releasing mouse
    // See 6243382 for more information
    private boolean mouseDraggedOutVertically = false;
    private volatile boolean vsbVisibilityChanged = false;

    /*
     * Comment
     */
    ListHelper(XWindow peer, Color[] colors, int initialSize,
               boolean multiSelect, boolean scrollVert, boolean scrollHoriz,
               Font font, int maxVisItems, int SPACE, int MARGIN, int BORDER,
               int SCROLLBAR) {
        this.peer = peer;
        this.colors = colors;
        this.multiSelect = multiSelect;
        items = new ArrayList<>(initialSize);
        selected = new ArrayList<>(1);
        selected.add(Integer.valueOf(-1));

        this.maxVisItems = maxVisItems;
        if (scrollVert) {
            vsb = new XVerticalScrollbar(this);
            vsb.setValues(0, 0, 0, 0, 1, maxVisItems - 1);
        }
        if (scrollHoriz) {
            hsb = new XHorizontalScrollbar(this);
            hsb.setValues(0, 0, 0, 0, 1, 1);
        }

        setFont(font);
        TEXT_SPACE = SPACE;
        ITEM_MARGIN = MARGIN;
        BORDER_WIDTH = BORDER;
        SCROLLBAR_WIDTH = SCROLLBAR;
    }

    @Override
    public Component getEventSource() {
        return peer.getEventSource();
    }

    /**********************************************************************/
    /* List management methods                                            */
    /**********************************************************************/

    void add(String item) {
        items.add(item);
        updateScrollbars();
    }

    void add(String item, int index) {
        items.add(index, item);
        updateScrollbars();
    }

    void remove(String item) {
        // FIXME: need to clean up select list, too?
        items.remove(item);
        updateScrollbars();
        // Is vsb visible now?
    }

    void remove(int index) {
        // FIXME: need to clean up select list, too?
        items.remove(index);
        updateScrollbars();
        // Is vsb visible now?
    }

    void removeAll() {
        items.clear();
        updateScrollbars();
    }

    void setMultiSelect(boolean ms) {
        multiSelect = ms;
    }

    /*
     * docs.....definitely docs
     * merely keeps internal track of which items are selected for painting
     * dealing with target Components happens elsewhere
     */
    void select(int index) {
        if (index > getItemCount() - 1) {
            index = (isEmpty() ? -1 : 0);
        }
        if (multiSelect) {
            assert false : "Implement ListHelper.select() for multiselect";
        }
        else if (getSelectedIndex() != index) {
            selected.remove(0);
            selected.add(Integer.valueOf(index));
            makeVisible(index);
        }
    }

    /* docs */
    void deselect(int index) {
        assert(false);
    }

    /* docs */
    /* if called for multiselect, return -1 */
    int getSelectedIndex() {
        if (!multiSelect) {
            Integer val = selected.get(0);
            return val.intValue();
        }
        return -1;
    }

    int[] getSelectedIndexes() { assert(false); return null;}

    /*
     * A getter method for XChoicePeer.
     * Returns vsbVisiblityChanged value and sets it to false.
     */
    boolean checkVsbVisibilityChangedAndReset(){
        boolean returnVal = vsbVisibilityChanged;
        vsbVisibilityChanged = false;
        return returnVal;
    }

    boolean isEmpty() {
        return items.isEmpty();
    }

    int getItemCount() {
        return items.size();
    }

    String getItem(int index) {
        return items.get(index);
    }

    /**********************************************************************/
    /* GUI-related methods                                                */
    /**********************************************************************/

    void setFocusedIndex(int index) {
        focusedIndex = index;
    }

    private boolean isFocusedIndex(int index) {
        return index == focusedIndex;
    }

    @SuppressWarnings("deprecation")
    void setFont(Font newFont) {
        if (newFont != font) {
            font = newFont;
            fm = Toolkit.getDefaultToolkit().getFontMetrics(font);
            // Also cache stuff like fontHeight?
        }
    }

    /*
     * Returns width of the text of the longest item
     */
    int getMaxItemWidth() {
        int m = 0;
        int end = getItemCount();
        for(int i = 0 ; i < end ; i++) {
            int l = fm.stringWidth(getItem(i));
            m = Math.max(m, l);
        }
        return m;
    }

    /*
     * Height of an item (this doesn't include ITEM_MARGIN)
     */
    int getItemHeight() {
        return fm.getHeight() + (2*TEXT_SPACE);
    }

    int y2index(int y) {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("y=" + y +", firstIdx=" + firstDisplayedIndex() +", itemHeight=" + getItemHeight()
                     + ",item_margin=" + ITEM_MARGIN);
        }
        // See 6243382 for more information
        int newIdx = firstDisplayedIndex() + ((y - 2*ITEM_MARGIN) / (getItemHeight() + 2*ITEM_MARGIN));
        return newIdx;
    }

    /* write these
    int index2y(int);
    public int numItemsDisplayed() {}
    */

    int firstDisplayedIndex() {
        if (vsbVis) {
            return vsb.getValue();
        }
        return 0;
    }

    int lastDisplayedIndex() {
        // FIXME: need to account for horiz scroll bar
        if (hsbVis) {
            assert false : "Implement for horiz scroll bar";
        }

        return vsbVis ? vsb.getValue() + maxVisItems - 1: getItemCount() - 1;
    }

    /*
     * If the given index is not visible in the List, scroll so that it is.
     */
    private void makeVisible(int index) {
        if (vsbVis) {
            if (index < firstDisplayedIndex()) {
                vsb.setValue(index);
            }
            else if (index > lastDisplayedIndex()) {
                vsb.setValue(index - maxVisItems + 1);
            }
        }
    }

    // FIXME: multi-select needs separate focused index
    void up() {
        int curIdx = getSelectedIndex();
        int numItems = getItemCount();
        int newIdx;

        assert curIdx >= 0;

        if (curIdx == 0) {
            newIdx = numItems - 1;
        }
        else {
            newIdx = --curIdx;
        }
        // focus(newIdx);
        select(newIdx);
    }

    void down() {
        int newIdx = (getSelectedIndex() + 1) % getItemCount();
        select(newIdx);
    }

    void pageUp() {
        // FIXME: for multi-select, move the focused item, not the selected item
        if (vsbVis && firstDisplayedIndex() > 0) {
            if (multiSelect) {
                assert false : "Implement pageUp() for multiSelect";
            }
            else {
                int selectionOffset = getSelectedIndex() - firstDisplayedIndex();
                // the vsb does bounds checking
                int newIdx = firstDisplayedIndex() - vsb.getBlockIncrement();
                vsb.setValue(newIdx);
                select(firstDisplayedIndex() + selectionOffset);
            }
        }
    }
    void pageDown() {
        if (vsbVis && lastDisplayedIndex() < getItemCount() - 1) {
            if (multiSelect) {
                assert false : "Implement pageDown() for multiSelect";
            }
            else {
                int selectionOffset = getSelectedIndex() - firstDisplayedIndex();
                // the vsb does bounds checking
                int newIdx = lastDisplayedIndex();
                vsb.setValue(newIdx);
                select(firstDisplayedIndex() + selectionOffset);
            }
        }
    }
    void home() {}
    void end() {}


    boolean isVSBVisible() { return vsbVis; }
    boolean isHSBVisible() { return hsbVis; }

    XVerticalScrollbar getVSB() { return vsb; }
    XHorizontalScrollbar getHSB() { return hsb; }

    boolean isInVertSB(Rectangle bounds, int x, int y) {
        if (vsbVis) {
            assert vsb != null : "Vert scrollbar is visible, yet is null?";
            int sbHeight = hsbVis ? bounds.height - SCROLLBAR_WIDTH : bounds.height;
            return (x <= bounds.width) &&
                   (x >= bounds.width - SCROLLBAR_WIDTH) &&
                   (y >= 0) &&
                   (y <= sbHeight);
        }
        return false;
    }

    boolean isInHorizSB(Rectangle bounds, int x, int y) {
        if (hsbVis) {
            assert hsb != null : "Horiz scrollbar is visible, yet is null?";

            int sbWidth = vsbVis ? bounds.width - SCROLLBAR_WIDTH : bounds.width;
            return (x <= sbWidth) &&
                   (x >= 0) &&
                   (y >= bounds.height - SCROLLBAR_WIDTH) &&
                   (y <= bounds.height);
        }
        return false;
    }
    @SuppressWarnings("deprecation")
    void handleVSBEvent(MouseEvent e, Rectangle bounds, int x, int y) {
        int sbHeight = hsbVis ? bounds.height - SCROLLBAR_WIDTH : bounds.height;

        vsb.handleMouseEvent(e.getID(),
                             e.getModifiers(),
                             x - (bounds.width - SCROLLBAR_WIDTH),
                             y);
    }

    /*
     * Called when items are added/removed.
     * Update whether the scrollbar is visible or not, scrollbar values
     */
    private void updateScrollbars() {
        boolean oldVsbVis = vsbVis;
        vsbVis = vsb != null && items.size() > maxVisItems;
        if (vsbVis) {
            vsb.setValues(vsb.getValue(), getNumItemsDisplayed(),
                          vsb.getMinimum(), items.size());
        }

        // 6405689. If Vert Scrollbar gets disappeared from the dropdown menu we should repaint whole dropdown even if
        // no actual resize gets invoked. This is needed because some painting artifacts remained between dropdown items
        // but draw3DRect doesn't clear the area inside. Instead it just paints lines as borders.
        vsbVisibilityChanged = (vsbVis != oldVsbVis);
        // FIXME: check if added item makes a hsb necessary (if supported, that of course)
    }

    private int getNumItemsDisplayed() {
        return items.size() > maxVisItems ? maxVisItems : items.size();
    }

    @Override
    public void repaintScrollbarRequest(XScrollbar sb) {
        Graphics g = peer.getGraphics();
        Rectangle bounds = peer.getBounds();
        if ((sb == vsb) && vsbVis) {
            paintVSB(g, XComponentPeer.getSystemColors(), bounds);
        }
        else if ((sb == hsb) && hsbVis) {
            paintHSB(g, XComponentPeer.getSystemColors(), bounds);
        }
        g.dispose();
    }

    @Override
    public void notifyValue(XScrollbar obj, int type, int v, boolean isAdjusting) {
        if (obj == vsb) {
            int oldScrollValue = vsb.getValue();
            vsb.setValue(v);
            boolean needRepaint = (oldScrollValue != vsb.getValue());
            // See 6243382 for more information
            if (mouseDraggedOutVertically){
                int oldItemValue = getSelectedIndex();
                int newItemValue = getSelectedIndex() + v - oldScrollValue;
                select(newItemValue);
                needRepaint = needRepaint || (getSelectedIndex() != oldItemValue);
            }

            // FIXME: how are we going to paint!?
            Graphics g = peer.getGraphics();
            Rectangle bounds = peer.getBounds();
            int first = v;
            int last = Math.min(getItemCount() - 1,
                                v + maxVisItems);
            if (needRepaint) {
                paintItems(g, colors, bounds, first, last);
            }
            g.dispose();

        }
        else if ((XHorizontalScrollbar)obj == hsb) {
            hsb.setValue(v);
            // FIXME: how are we going to paint!?
        }
    }

    void updateColors(Color[] newColors) {
        colors = newColors;
    }

    /*
    public void paintItems(Graphics g,
                           Color[] colors,
                           Rectangle bounds,
                           Font font,
                           int first,
                           int last,
                           XVerticalScrollbar vsb,
                           XHorizontalScrollbar hsb) {
    */
    void paintItems(Graphics g,
                           Color[] colors,
                           Rectangle bounds) {
        // paint border
        // paint items
        // paint scrollbars
        // paint focus?

    }
    void paintAllItems(Graphics g,
                           Color[] colors,
                           Rectangle bounds) {
        paintItems(g, colors, bounds,
                   firstDisplayedIndex(), lastDisplayedIndex());
    }
    private void paintItems(Graphics g, Color[] colors, Rectangle bounds,
                            int first, int last) {
        peer.flush();
        int x = BORDER_WIDTH + ITEM_MARGIN;
        int width = bounds.width - 2*ITEM_MARGIN - 2*BORDER_WIDTH - (vsbVis ? SCROLLBAR_WIDTH : 0);
        int height = getItemHeight();
        int y = BORDER_WIDTH + ITEM_MARGIN;

        for (int i = first; i <= last ; i++) {
            paintItem(g, colors, getItem(i),
                      x, y, width, height,
                      isItemSelected(i),
                      isFocusedIndex(i));
            y += height + 2*ITEM_MARGIN;
        }

        if (vsbVis) {
            paintVSB(g, XComponentPeer.getSystemColors(), bounds);
        }
        if (hsbVis) {
            paintHSB(g, XComponentPeer.getSystemColors(), bounds);
        }
        peer.flush();
        // FIXME: if none of the items were focused, paint focus around the
        // entire list.  This is how java.awt.List should work.
    }

    /*
     * comment about what is painted (i.e. the focus rect
     */
    private void paintItem(Graphics g, Color[] colors, String string, int x,
                           int y, int width, int height, boolean selected,
                           boolean focused) {
        //System.out.println("LP.pI(): x="+x+" y="+y+" w="+width+" h="+height);
        //g.setColor(colors[BACKGROUND_COLOR]);

        // FIXME: items shouldn't draw into the scrollbar

        if (selected) {
            g.setColor(colors[XComponentPeer.FOREGROUND_COLOR]);
        }
        else {
            g.setColor(colors[XComponentPeer.BACKGROUND_COLOR]);
        }
        g.fillRect(x, y, width, height);

        if (focused) {
            //g.setColor(colors[XComponentPeer.FOREGROUND_COLOR]);
            g.setColor(Color.BLACK);
            g.drawRect(x + FOCUS_INSET,
                       y + FOCUS_INSET,
                       width - 2*FOCUS_INSET,
                       height - 2*FOCUS_INSET);
        }

        if (selected) {
            g.setColor(colors[XComponentPeer.BACKGROUND_COLOR]);
        }
        else {
            g.setColor(colors[XComponentPeer.FOREGROUND_COLOR]);
        }
        g.setFont(font);
        //Rectangle clip = g.getClipBounds();
        //g.clipRect(x, y, width, height);
        //g.drawString(string, x + TEXT_SPACE, y + TEXT_SPACE + ITEM_MARGIN);

        int fontAscent = fm.getAscent();
        int fontDescent = fm.getDescent();

        g.drawString(string, x + TEXT_SPACE, y + (height + fm.getMaxAscent() - fm.getMaxDescent())/2);
        //g.clipRect(clip.x, clip.y, clip.width, clip.height);
    }

    private boolean isItemSelected(int index) {
        Iterator<Integer> itr = selected.iterator();
        while (itr.hasNext()) {
            Integer val = itr.next();
            if (val.intValue() == index) {
                return true;
            }
        }
        return false;
    }

    private void paintVSB(Graphics g, Color[] colors, Rectangle bounds) {
        int height = bounds.height - 2*BORDER_WIDTH - (hsbVis ? (SCROLLBAR_WIDTH-2) : 0);
        Graphics ng = g.create();

        g.setColor(colors[XComponentPeer.BACKGROUND_COLOR]);
        try {
            ng.translate(bounds.width - BORDER_WIDTH - SCROLLBAR_WIDTH,
                         BORDER_WIDTH);
            // Update scrollbar's size
            vsb.setSize(SCROLLBAR_WIDTH, bounds.height);
            vsb.paint(ng, colors, true);
        } finally {
            ng.dispose();
        }
    }

    private void paintHSB(Graphics g, Color[] colors, Rectangle bounds) {

    }

    /*
     * Helper method for Components with integrated scrollbars.
     * Pass in the vertical and horizontal scroll bar (or null for none/hidden)
     * and the MouseWheelEvent, and the appropriate scrollbar will be scrolled
     * correctly.
     * Returns whether or not scrolling actually took place.  This will indicate
     * whether or not repainting is required.
     */
    static boolean doWheelScroll(XVerticalScrollbar vsb,
                                     XHorizontalScrollbar hsb,
                                     MouseWheelEvent e) {
        XScrollbar scroll = null;
        int wheelRotation;

        // Determine which, if any, sb to scroll
        if (vsb != null) {
            scroll = vsb;
        }
        else if (hsb != null) {
            scroll = hsb;
        }
        else { // Neither scrollbar is showing
            return false;
        }

        wheelRotation = e.getWheelRotation();

        // Check if scroll is necessary
        if ((wheelRotation < 0 && scroll.getValue() > scroll.getMinimum()) ||
            (wheelRotation > 0 && scroll.getValue() < scroll.getMaximum()) ||
            wheelRotation != 0) {

            int type = e.getScrollType();
            int incr;
            if (type == MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
                incr = wheelRotation * scroll.getBlockIncrement();
            }
            else { // type is WHEEL_UNIT_SCROLL
                incr = e.getUnitsToScroll() * scroll.getUnitIncrement();
            }
            scroll.setValue(scroll.getValue() + incr);
            return true;
        }
        return false;
    }

    /*
     * Helper method for XChoicePeer with integrated vertical scrollbar.
     * Start or stop vertical scrolling when mouse dragged in / out the area of the list if it's required
     * Restoring Motif behavior
     * See 6243382 for more information
     */
    void trackMouseDraggedScroll(int mouseX, int mouseY, int listWidth, int listHeight){

        if (!mouseDraggedOutVertically){
            if (vsb.beforeThumb(mouseX, mouseY)) {
                vsb.setMode(AdjustmentEvent.UNIT_DECREMENT);
            } else {
                vsb.setMode(AdjustmentEvent.UNIT_INCREMENT);
            }
        }

        if(!mouseDraggedOutVertically && (mouseY < 0 || mouseY >= listHeight)){
            mouseDraggedOutVertically = true;
            vsb.startScrollingInstance();
        }

        if (mouseDraggedOutVertically && mouseY >= 0 && mouseY < listHeight && mouseX >= 0 && mouseX < listWidth){
            mouseDraggedOutVertically = false;
            vsb.stopScrollingInstance();
        }
    }

    /*
     * Helper method for XChoicePeer with integrated vertical scrollbar.
     * Stop vertical scrolling when mouse released in / out the area of the list if it's required
     * Restoring Motif behavior
     * see 6243382 for more information
     */
    void trackMouseReleasedScroll(){

        if (mouseDraggedOutVertically){
            mouseDraggedOutVertically = false;
            vsb.stopScrollingInstance();
        }

    }
}
