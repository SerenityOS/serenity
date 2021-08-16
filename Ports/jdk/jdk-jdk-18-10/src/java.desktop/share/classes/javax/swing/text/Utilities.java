/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.Shape;
import java.awt.Graphics2D;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;

import java.text.*;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.text.ParagraphView.Row;
import sun.swing.SwingUtilities2;
import static sun.swing.SwingUtilities2.drawChars;
import static sun.swing.SwingUtilities2.getFontCharWidth;
import static sun.swing.SwingUtilities2.getFontCharsWidth;

/**
 * A collection of methods to deal with various text
 * related activities.
 *
 * @author  Timothy Prinzing
 */
public class Utilities {
    /**
     * Constructs a {@code Utilities}.
     */
    public Utilities() {}

    /**
     * If <code>view</code>'s container is a <code>JComponent</code> it
     * is returned, after casting.
     */
    static JComponent getJComponent(View view) {
        if (view != null) {
            Component component = view.getContainer();
            if (component instanceof JComponent) {
                return (JComponent)component;
            }
        }
        return null;
    }

    /**
     * Draws the given text, expanding any tabs that are contained
     * using the given tab expansion technique.  This particular
     * implementation renders in a 1.1 style coordinate system
     * where ints are used and 72dpi is assumed.
     *
     * @param s  the source of the text
     * @param x  the X origin &gt;= 0
     * @param y  the Y origin &gt;= 0
     * @param g  the graphics context
     * @param e  how to expand the tabs.  If this value is null,
     *   tabs will be expanded as a space character.
     * @param startOffset starting offset of the text in the document &gt;= 0
     * @return  the X location at the end of the rendered text
     *
     * @deprecated replaced by
     * {@link #drawTabbedText(Segment, float, float, Graphics2D, TabExpander, int)}
     */
    @Deprecated(since = "9")
    public static final int drawTabbedText(Segment s, int x, int y, Graphics g,
                                           TabExpander e, int startOffset) {
        return drawTabbedText(null, s, x, y, g, e, startOffset);
    }

    /**
     * Draws the given text, expanding any tabs that are contained
     * using the given tab expansion technique.
     *
     * @param s  the source of the text
     * @param x  the X origin {@code >= 0}
     * @param y  the Y origin {@code >= 0}
     * @param g  the graphics context
     * @param e  how to expand the tabs.  If this value is null,
     *           tabs will be expanded as a space character.
     * @param startOffset starting offset of the text in the document {@code >= 0}
     * @return  the X location at the end of the rendered text
     *
     * @since 9
     */
    public static final float drawTabbedText(Segment s, float x, float y,
                                             Graphics2D g,
                                             TabExpander e,
                                             int startOffset)
    {
        return drawTabbedText(null, s, x, y, g, e, startOffset, null, true);
    }

    /**
     * Draws the given text, expanding any tabs that are contained
     * using the given tab expansion technique.  This particular
     * implementation renders in a 1.1 style coordinate system
     * where ints are used and 72dpi is assumed.
     *
     * @param view View requesting rendering, may be null.
     * @param s  the source of the text
     * @param x  the X origin &gt;= 0
     * @param y  the Y origin &gt;= 0
     * @param g  the graphics context
     * @param e  how to expand the tabs.  If this value is null,
     *   tabs will be expanded as a space character.
     * @param startOffset starting offset of the text in the document &gt;= 0
     * @return  the X location at the end of the rendered text
     */
    static final int drawTabbedText(View view,
                                Segment s, int x, int y, Graphics g,
                                TabExpander e, int startOffset) {
        return drawTabbedText(view, s, x, y, g, e, startOffset, null);
    }

    // In addition to the previous method it can extend spaces for
    // justification.
    //
    // all params are the same as in the preious method except the last
    // one:
    // @param justificationData justificationData for the row.
    // if null not justification is needed
    static final int drawTabbedText(View view,
                                Segment s, int x, int y, Graphics g,
                                TabExpander e, int startOffset,
                                int [] justificationData) {
        return (int) drawTabbedText(view, s, x, y, g, e, startOffset,
                                    justificationData, false);
    }

    static final float drawTabbedText(View view,
                                Segment s, float x, float y, Graphics g,
                                TabExpander e, int startOffset,
                                int [] justificationData,
                                boolean useFPAPI)
    {
        JComponent component = getJComponent(view);
        FontMetrics metrics = SwingUtilities2.getFontMetrics(component, g);
        float nextX = x;
        char[] txt = s.array;
        int txtOffset = s.offset;
        int flushLen = 0;
        int flushIndex = s.offset;
        int spaceAddon = 0;
        int spaceAddonLeftoverEnd = -1;
        int startJustifiableContent = 0;
        int endJustifiableContent = 0;
        if (justificationData != null) {
            int offset = - startOffset + txtOffset;
            View parent = null;
            if (view != null
                  && (parent = view.getParent()) != null) {
                offset += parent.getStartOffset();
            }
            spaceAddon =
                justificationData[Row.SPACE_ADDON];
            spaceAddonLeftoverEnd =
                justificationData[Row.SPACE_ADDON_LEFTOVER_END] + offset;
            startJustifiableContent =
                justificationData[Row.START_JUSTIFIABLE] + offset;
            endJustifiableContent =
                justificationData[Row.END_JUSTIFIABLE] + offset;
        }
        int n = s.offset + s.count;
        for (int i = txtOffset; i < n; i++) {
            if (txt[i] == '\t'
                || ((spaceAddon != 0 || i <= spaceAddonLeftoverEnd)
                    && (txt[i] == ' ')
                    && startJustifiableContent <= i
                    && i <= endJustifiableContent
                    )) {
                if (flushLen > 0) {
                    nextX = drawChars(component, g, txt, flushIndex, flushLen, x, y);
                    flushLen = 0;
                }
                flushIndex = i + 1;
                if (txt[i] == '\t') {
                    if (e != null) {
                        nextX = e.nextTabStop(nextX, startOffset + i - txtOffset);
                    } else {
                        nextX += getFontCharWidth(' ', metrics, useFPAPI);
                    }
                } else if (txt[i] == ' ') {
                    float spaceWidth = getFontCharWidth(' ', metrics, useFPAPI);
                    nextX += spaceWidth + spaceAddon;
                    if (i <= spaceAddonLeftoverEnd) {
                        nextX++;
                    }
                }
                x = nextX;
            } else if ((txt[i] == '\n') || (txt[i] == '\r')) {
                if (flushLen > 0) {
                    nextX = drawChars(component, g, txt, flushIndex, flushLen,
                                      x, y, useFPAPI);
                    flushLen = 0;
                }
                flushIndex = i + 1;
                x = nextX;
            } else {
                flushLen += 1;
            }
        }
        if (flushLen > 0) {
            nextX = drawChars(component, g,txt, flushIndex, flushLen, x, y, useFPAPI);
        }
        return nextX;
    }

    /**
     * Determines the width of the given segment of text taking tabs
     * into consideration.  This is implemented in a 1.1 style coordinate
     * system where ints are used and 72dpi is assumed.
     *
     * @param s  the source of the text
     * @param metrics the font metrics to use for the calculation
     * @param x  the X origin &gt;= 0
     * @param e  how to expand the tabs.  If this value is null,
     *   tabs will be expanded as a space character.
     * @param startOffset starting offset of the text in the document &gt;= 0
     * @return  the width of the text
     *
     * @deprecated replaced by
     *     {@link #getTabbedTextWidth(Segment, FontMetrics, float, TabExpander, int)}
     */
    @Deprecated(since = "9")
    public static final int getTabbedTextWidth(Segment s, FontMetrics metrics, int x,
                                               TabExpander e, int startOffset) {
        return getTabbedTextWidth(null, s, metrics, x, e, startOffset, null);
    }

    /**
     * Determines the width of the given segment of text taking tabs
     * into consideration.
     *
     * @param s  the source of the text
     * @param metrics the font metrics to use for the calculation
     * @param x  the X origin {@code >= 0}
     * @param e  how to expand the tabs.  If this value is null,
     *   tabs will be expanded as a space character.
     * @param startOffset starting offset of the text in the document {@code >= 0}
     * @return  the width of the text
     *
     * @since 9
     */
    public static final float getTabbedTextWidth(Segment s, FontMetrics metrics,
                                                 float x, TabExpander e,
                                                 int startOffset) {
        return getTabbedTextWidth(null, s, metrics, x, e, startOffset, null);
    }

    // In addition to the previous method it can extend spaces for
    // justification.
    //
    // all params are the same as in the preious method except the last
    // one:
    // @param justificationData justificationData for the row.
    // if null not justification is needed
    static final int getTabbedTextWidth(View view, Segment s,
                                        FontMetrics metrics, int x,
                                        TabExpander e, int startOffset,
                                        int[] justificationData)
    {
        return (int) getTabbedTextWidth(view, s, metrics, x, e, startOffset,
                                        justificationData, false);

    }

    static final float getTabbedTextWidth(View view, Segment s,
                                        FontMetrics metrics, float x,
                                        TabExpander e, int startOffset,
                                        int[] justificationData)
    {
        return  getTabbedTextWidth(view, s, metrics, x, e, startOffset,
                                   justificationData, true);

    }

    static final float getTabbedTextWidth(View view, Segment s,
                                        FontMetrics metrics, float x,
                                        TabExpander e, int startOffset,
                                        int[] justificationData,
                                        boolean useFPAPI) {
        float nextX = x;
        char[] txt = s.array;
        int txtOffset = s.offset;
        int n = s.offset + s.count;
        int charCount = 0;
        int spaceAddon = 0;
        int spaceAddonLeftoverEnd = -1;
        int startJustifiableContent = 0;
        int endJustifiableContent = 0;
        if (justificationData != null) {
            int offset = - startOffset + txtOffset;
            View parent = null;
            if (view != null
                  && (parent = view.getParent()) != null) {
                offset += parent.getStartOffset();
            }
            spaceAddon =
                justificationData[Row.SPACE_ADDON];
            spaceAddonLeftoverEnd =
                justificationData[Row.SPACE_ADDON_LEFTOVER_END] + offset;
            startJustifiableContent =
                justificationData[Row.START_JUSTIFIABLE] + offset;
            endJustifiableContent =
                justificationData[Row.END_JUSTIFIABLE] + offset;
        }

        for (int i = txtOffset; i < n; i++) {
            if (txt[i] == '\t'
                || ((spaceAddon != 0 || i <= spaceAddonLeftoverEnd)
                    && (txt[i] == ' ')
                    && startJustifiableContent <= i
                    && i <= endJustifiableContent
                    )) {
                nextX += metrics.charsWidth(txt, i-charCount, charCount);
                charCount = 0;
                if (txt[i] == '\t') {
                    if (e != null) {
                        nextX = e.nextTabStop(nextX, startOffset + i - txtOffset);
                    } else {
                        nextX += getFontCharWidth(' ', metrics, useFPAPI);
                    }
                } else if (txt[i] == ' ') {
                    float spaceWidth = getFontCharWidth(' ', metrics, useFPAPI);
                    nextX += spaceWidth + spaceAddon;
                    if (i <= spaceAddonLeftoverEnd) {
                        nextX++;
                    }
                }
            } else if(txt[i] == '\n') {
            // Ignore newlines, they take up space and we shouldn't be
            // counting them.
                nextX += getFontCharsWidth(txt, i - charCount, charCount,
                                           metrics, useFPAPI);
                charCount = 0;
            } else {
                charCount++;
        }
        }
        nextX += getFontCharsWidth(txt, n - charCount, charCount,
                                   metrics, useFPAPI);
        return nextX - x;
    }

    /**
     * Determines the relative offset into the given text that
     * best represents the given span in the view coordinate
     * system.  This is implemented in a 1.1 style coordinate
     * system where ints are used and 72dpi is assumed.
     *
     * @param s  the source of the text
     * @param metrics the font metrics to use for the calculation
     * @param x0 the starting view location representing the start
     *   of the given text &gt;= 0.
     * @param x  the target view location to translate to an
     *   offset into the text &gt;= 0.
     * @param e  how to expand the tabs.  If this value is null,
     *   tabs will be expanded as a space character.
     * @param startOffset starting offset of the text in the document &gt;= 0
     * @return  the offset into the text &gt;= 0
     *
     * @deprecated replaced by
     *     {@link #getTabbedTextOffset(Segment, FontMetrics, float, float,
     *                                 TabExpander, int, boolean)}
     */
    @Deprecated(since = "9")
    public static final int getTabbedTextOffset(Segment s, FontMetrics metrics,
                                             int x0, int x, TabExpander e,
                                             int startOffset) {
        return getTabbedTextOffset(s, metrics, x0, x, e, startOffset, true);
    }

    static final int getTabbedTextOffset(View view, Segment s, FontMetrics metrics,
                                         int x0, int x, TabExpander e,
                                         int startOffset,
                                         int[] justificationData) {
        return getTabbedTextOffset(view, s, metrics, x0, x, e, startOffset, true,
                                   justificationData, false);
    }

    static final int getTabbedTextOffset(View view, Segment s, FontMetrics metrics,
                                         float x0, float x, TabExpander e,
                                         int startOffset,
                                         int[] justificationData) {
        return getTabbedTextOffset(view, s, metrics, x0, x, e, startOffset, true,
                justificationData, true);
    }

    /**
     * Determines the relative offset into the given text that
     * best represents the given span in the view coordinate
     * system.
     *
     * @param s  the source of the text
     * @param metrics the font metrics to use for the calculation
     * @param x0 the starting view location representing the start
     *   of the given text &gt;= 0.
     * @param x  the target view location to translate to an
     *   offset into the text &gt;= 0.
     * @param e  how to expand the tabs.  If this value is null,
     *   tabs will be expanded as a space character.
     * @param startOffset starting offset of the text in the document &gt;= 0
     * @param round whether or not to round
     * @return  the offset into the text &gt;= 0
     *
     * @deprecated replaced by
     *     {@link #getTabbedTextOffset(Segment, FontMetrics, float, float,
     *                                 TabExpander, int, boolean)}
     */
    @Deprecated(since = "9")
    public static final int getTabbedTextOffset(Segment s,
                                                FontMetrics metrics,
                                                int x0, int x, TabExpander e,
                                                int startOffset,
                                                boolean round) {
        return getTabbedTextOffset(null, s, metrics, x0, x, e, startOffset,
                                   round, null, false);
    }

    /**
     * Determines the relative offset into the given text that
     * best represents the given span in the view coordinate
     * system.
     *
     * @param s  the source of the text
     * @param metrics the font metrics to use for the calculation
     * @param x0 the starting view location representing the start
     *   of the given text {@code >= 0}.
     * @param x  the target view location to translate to an
     *   offset into the text {@code >= 0}.
     * @param e  how to expand the tabs.  If this value is null,
     *   tabs will be expanded as a space character.
     * @param startOffset starting offset of the text in the document {@code >= 0}
     * @param round whether or not to round
     * @return  the offset into the text {@code >= 0}
     *
     * @since 9
     */
    public static final int getTabbedTextOffset(Segment s,
                                                FontMetrics metrics,
                                                float x0, float x,
                                                TabExpander e,
                                                int startOffset,
                                                boolean round)
    {
        return getTabbedTextOffset(null, s, metrics, x0, x, e,
                                   startOffset, round, null, true);
    }

    // In addition to the previous method it can extend spaces for
    // justification.
    //
    // all params are the same as in the preious method except the last
    // one:
    // @param justificationData justificationData for the row.
    // if null not justification is needed
    static final int getTabbedTextOffset(View view,
                                         Segment s,
                                         FontMetrics metrics,
                                         float x0, float x, TabExpander e,
                                         int startOffset,
                                         boolean round,
                                         int[] justificationData,
                                         boolean useFPAPI) {
        if (x0 >= x) {
            // x before x0, return.
            return 0;
        }
        float nextX = x0;
        // s may be a shared segment, so it is copied prior to calling
        // the tab expander
        char[] txt = s.array;
        int txtOffset = s.offset;
        int txtCount = s.count;
        int spaceAddon = 0 ;
        int spaceAddonLeftoverEnd = -1;
        int startJustifiableContent = 0 ;
        int endJustifiableContent = 0;
        if (justificationData != null) {
            int offset = - startOffset + txtOffset;
            View parent = null;
            if (view != null
                  && (parent = view.getParent()) != null) {
                offset += parent.getStartOffset();
            }
            spaceAddon =
                justificationData[Row.SPACE_ADDON];
            spaceAddonLeftoverEnd =
                justificationData[Row.SPACE_ADDON_LEFTOVER_END] + offset;
            startJustifiableContent =
                justificationData[Row.START_JUSTIFIABLE] + offset;
            endJustifiableContent =
                justificationData[Row.END_JUSTIFIABLE] + offset;
        }
        int n = s.offset + s.count;
        for (int i = s.offset; i < n; i++) {
            if (txt[i] == '\t'
                || ((spaceAddon != 0 || i <= spaceAddonLeftoverEnd)
                    && (txt[i] == ' ')
                    && startJustifiableContent <= i
                    && i <= endJustifiableContent
                    )){
                if (txt[i] == '\t') {
                    if (e != null) {
                        nextX = e.nextTabStop(nextX, startOffset + i - txtOffset);
                    } else {
                        nextX += getFontCharWidth(' ', metrics, useFPAPI);
                    }
                } else if (txt[i] == ' ') {
                    nextX += getFontCharWidth(' ', metrics, useFPAPI);
                    nextX += spaceAddon;
                    if (i <= spaceAddonLeftoverEnd) {
                        nextX++;
                    }
                }
            } else {
                nextX += getFontCharWidth(txt[i], metrics, useFPAPI);
            }
            if (x < nextX) {
                // found the hit position... return the appropriate side
                int offset;

                // the length of the string measured as a whole may differ from
                // the sum of individual character lengths, for example if
                // fractional metrics are enabled; and we must guard from this.
                if (round) {
                    offset = i + 1 - txtOffset;

                    float width = getFontCharsWidth(txt, txtOffset, offset,
                                                    metrics, useFPAPI);
                    float span = x - x0;

                    if (span < width) {
                        while (offset > 0) {
                            float charsWidth = getFontCharsWidth(txt, txtOffset,
                                    offset - 1, metrics, useFPAPI);
                            float nextWidth = offset > 1 ? charsWidth : 0;

                            if (span >= nextWidth) {
                                if (span - nextWidth < width - span) {
                                    offset--;
                                }

                                break;
                            }

                            width = nextWidth;
                            offset--;
                        }
                    }
                } else {
                    offset = i - txtOffset;

                    while (offset > 0 && getFontCharsWidth(txt, txtOffset, offset,
                                                           metrics, useFPAPI)
                            > (x - x0)) {
                        offset--;
                    }
                }

                return offset;
            }
        }

        // didn't find, return end offset
        return txtCount;
    }

    /**
     * Determine where to break the given text to fit
     * within the given span. This tries to find a word boundary.
     * @param s  the source of the text
     * @param metrics the font metrics to use for the calculation
     * @param x0 the starting view location representing the start
     *   of the given text.
     * @param x  the target view location to translate to an
     *   offset into the text.
     * @param e  how to expand the tabs.  If this value is null,
     *   tabs will be expanded as a space character.
     * @param startOffset starting offset in the document of the text
     * @return  the offset into the given text
     *
     * @deprecated replaced by
     *     {@link #getBreakLocation(Segment, FontMetrics, float, float,
     *                              TabExpander, int)}
     */
    @Deprecated(since = "9")
    public static final int getBreakLocation(Segment s, FontMetrics metrics,
                                             int x0, int x, TabExpander e,
                                             int startOffset) {
        return getBreakLocation(s, metrics, x0, x, e, startOffset, false);
    }

    static final int getBreakLocation(Segment s, FontMetrics metrics,
                                      float x0, float x, TabExpander e,
                                      int startOffset, boolean useFPIAPI) {
        char[] txt = s.array;
        int txtOffset = s.offset;
        int txtCount = s.count;
        int index = getTabbedTextOffset(null, s, metrics, x0, x, e, startOffset,
                                        false, null, useFPIAPI);

        if (index >= txtCount - 1) {
            return txtCount;
        }

        for (int i = txtOffset + index; i >= txtOffset; i--) {
            char ch = txt[i];
            if (ch < 256) {
                // break on whitespace
                if (Character.isWhitespace(ch)) {
                    index = i - txtOffset + 1;
                    break;
                }
            } else {
                // a multibyte char found; use BreakIterator to find line break
                BreakIterator bit = BreakIterator.getLineInstance();
                bit.setText(s);
                int breakPos = bit.preceding(i + 1);
                if (breakPos > txtOffset) {
                    index = breakPos - txtOffset;
                }
                break;
            }
        }
        return index;
    }

    /**
     * Determine where to break the given text to fit
     * within the given span. This tries to find a word boundary.
     * @param s  the source of the text
     * @param metrics the font metrics to use for the calculation
     * @param x0 the starting view location representing the start
     *        of the given text.
     * @param x  the target view location to translate to an
     *        offset into the text.
     * @param e  how to expand the tabs.  If this value is null,
     *        tabs will be expanded as a space character.
     * @param startOffset starting offset in the document of the text
     * @return  the offset into the given text
     *
     * @since 9
     */
    public static final int getBreakLocation(Segment s, FontMetrics metrics,
                                             float x0, float x, TabExpander e,
                                             int startOffset) {
        return getBreakLocation(s, metrics, x0, x, e, startOffset, true);
    }

    /**
     * Determines the starting row model position of the row that contains
     * the specified model position.  The component given must have a
     * size to compute the result.  If the component doesn't have a size
     * a value of -1 will be returned.
     *
     * @param c the editor
     * @param offs the offset in the document &gt;= 0
     * @return the position &gt;= 0 if the request can be computed, otherwise
     *  a value of -1 will be returned.
     * @exception BadLocationException if the offset is out of range
     */
    @SuppressWarnings("deprecation")
    public static final int getRowStart(JTextComponent c, int offs) throws BadLocationException {
        Rectangle r = c.modelToView(offs);
        if (r == null) {
            return -1;
        }
        int lastOffs = offs;
        int y = r.y;
        while ((r != null) && (y == r.y)) {
            // Skip invisible elements
            if(r.height !=0) {
                offs = lastOffs;
            }
            lastOffs -= 1;
            r = (lastOffs >= 0) ? c.modelToView(lastOffs) : null;
        }
        return offs;
    }

    /**
     * Determines the ending row model position of the row that contains
     * the specified model position.  The component given must have a
     * size to compute the result.  If the component doesn't have a size
     * a value of -1 will be returned.
     *
     * @param c the editor
     * @param offs the offset in the document &gt;= 0
     * @return the position &gt;= 0 if the request can be computed, otherwise
     *  a value of -1 will be returned.
     * @exception BadLocationException if the offset is out of range
     */
    @SuppressWarnings("deprecation")
    public static final int getRowEnd(JTextComponent c, int offs) throws BadLocationException {
        Rectangle2D r = c.modelToView2D(offs);
        if (r == null) {
            return -1;
        }
        int n = c.getDocument().getLength();
        int lastOffs = offs;
        double y = r.getY();
        while ((r != null) && (y == r.getY())) {
            // Skip invisible elements
            if (r.getHeight() !=0) {
                offs = lastOffs;
            }
            lastOffs += 1;
            r = (lastOffs <= n) ? c.modelToView(lastOffs) : null;
        }
        return offs;
    }

    /**
     * Determines the position in the model that is closest to the given
     * view location in the row above.  The component given must have a
     * size to compute the result.  If the component doesn't have a size
     * a value of -1 will be returned.
     *
     * @param c the editor
     * @param offs the offset in the document &gt;= 0
     * @param x the X coordinate &gt;= 0
     * @return the position &gt;= 0 if the request can be computed, otherwise
     *  a value of -1 will be returned.
     * @exception BadLocationException if the offset is out of range
     *
     * @deprecated replaced by
     *     {@link #getPositionAbove(JTextComponent, int, float)}
     */
    @Deprecated(since = "9")
    public static final int getPositionAbove(JTextComponent c, int offs, int x)
            throws BadLocationException
    {
        return getPositionAbove(c, offs, x, false);
    }

    @SuppressWarnings("deprecation")
    static final int getPositionAbove(JTextComponent c, int offs, float x,
                                      boolean useFPAPI) throws BadLocationException
    {
        int lastOffs = getRowStart(c, offs) - 1;
        if (lastOffs < 0) {
            return -1;
        }
        double bestSpan = Integer.MAX_VALUE;
        double y = 0;
        Rectangle2D r = null;
        if (lastOffs >= 0) {
            r = useFPAPI ? c.modelToView2D(lastOffs) : c.modelToView(lastOffs);
            y = r.getY();
        }
        while ((r != null) && (y == r.getY())) {
            double span = Math.abs(r.getX() - x);
            if (span < bestSpan) {
                offs = lastOffs;
                bestSpan = span;
            }
            lastOffs -= 1;

            if ((lastOffs >= 0)) {
                r = useFPAPI ? c.modelToView2D(lastOffs) : c.modelToView(lastOffs);
            } else {
                r = null;
            }
        }
        return offs;
    }

    /**
     * Determines the position in the model that is closest to the given
     * view location in the row above.  The component given must have a
     * size to compute the result.  If the component doesn't have a size
     * a value of -1 will be returned.
     *
     * @param c the editor
     * @param offs the offset in the document {@code >= 0}
     * @param x the X coordinate {@code >= 0}
     * @return the position {@code >= 0} if the request can be computed, otherwise
     *  a value of -1 will be returned.
     * @exception BadLocationException if the offset is out of range
     *
     * @since 9
     */
    public static final int getPositionAbove(JTextComponent c, int offs, float x)
            throws BadLocationException {
        return getPositionAbove(c, offs, x, true);
    }

    /**
     * Determines the position in the model that is closest to the given
     * view location in the row below.  The component given must have a
     * size to compute the result.  If the component doesn't have a size
     * a value of -1 will be returned.
     *
     * @param c the editor
     * @param offs the offset in the document &gt;= 0
     * @param x the X coordinate &gt;= 0
     * @return the position &gt;= 0 if the request can be computed, otherwise
     *  a value of -1 will be returned.
     * @exception BadLocationException if the offset is out of range
     *
     * @deprecated replaced by
     *     {@link #getPositionBelow(JTextComponent, int, float)}
     */
    @Deprecated(since = "9")
    public static final int getPositionBelow(JTextComponent c, int offs, int x)
            throws BadLocationException
    {
        return getPositionBelow(c, offs, x, false);
    }

    @SuppressWarnings("deprecation")
    static final int getPositionBelow(JTextComponent c, int offs, float x,
                                      boolean useFPAPI) throws BadLocationException
    {
        int lastOffs = getRowEnd(c, offs) + 1;
        if (lastOffs <= 0) {
            return -1;
        }
        double bestSpan = Integer.MAX_VALUE;
        int n = c.getDocument().getLength();
        double y = 0;
        Rectangle2D r = null;
        if (lastOffs <= n) {
            r = useFPAPI ? c.modelToView2D(lastOffs) : c.modelToView(lastOffs);
            y = r.getY();
        }
        while ((r != null) && (y == r.getY())) {
            double span = Math.abs(x - r.getX());
            if (span < bestSpan) {
                offs = lastOffs;
                bestSpan = span;
            }
            lastOffs += 1;

            if (lastOffs <= n) {
                r = useFPAPI ? c.modelToView2D(lastOffs) : c.modelToView(lastOffs);
            } else {
                r = null;
            }
        }
        return offs;
    }

    /**
     * Determines the position in the model that is closest to the given
     * view location in the row below.  The component given must have a
     * size to compute the result.  If the component doesn't have a size
     * a value of -1 will be returned.
     *
     * @param c the editor
     * @param offs the offset in the document {@code >= 0}
     * @param x the X coordinate {@code >= 0}
     * @return the position {@code >= 0} if the request can be computed, otherwise
     *  a value of -1 will be returned.
     * @exception BadLocationException if the offset is out of range
     *
     * @since 9
     */
    public static final int getPositionBelow(JTextComponent c, int offs, float x)
            throws BadLocationException {
        return getPositionBelow(c, offs, x, true);
    }

    /**
     * Determines the start of a word for the given model location.
     * Uses BreakIterator.getWordInstance() to actually get the words.
     *
     * @param c the editor
     * @param offs the offset in the document &gt;= 0
     * @return the location in the model of the word start &gt;= 0
     * @exception BadLocationException if the offset is out of range
     */
    public static final int getWordStart(JTextComponent c, int offs) throws BadLocationException {
        Document doc = c.getDocument();
        Element line = getParagraphElement(c, offs);
        if (line == null) {
            throw new BadLocationException("No word at " + offs, offs);
        }
        int lineStart = line.getStartOffset();
        int lineEnd = Math.min(line.getEndOffset(), doc.getLength());

        Segment seg = SegmentCache.getSharedSegment();
        doc.getText(lineStart, lineEnd - lineStart, seg);
        if(seg.count > 0) {
            BreakIterator words = BreakIterator.getWordInstance(c.getLocale());
            words.setText(seg);
            int wordPosition = seg.offset + offs - lineStart;
            if(wordPosition >= words.last()) {
                wordPosition = words.last() - 1;
            }
            words.following(wordPosition);
            offs = lineStart + words.previous() - seg.offset;
        }
        SegmentCache.releaseSharedSegment(seg);
        return offs;
    }

    /**
     * Determines the end of a word for the given location.
     * Uses BreakIterator.getWordInstance() to actually get the words.
     *
     * @param c the editor
     * @param offs the offset in the document &gt;= 0
     * @return the location in the model of the word end &gt;= 0
     * @exception BadLocationException if the offset is out of range
     */
    public static final int getWordEnd(JTextComponent c, int offs) throws BadLocationException {
        Document doc = c.getDocument();
        Element line = getParagraphElement(c, offs);
        if (line == null) {
            throw new BadLocationException("No word at " + offs, offs);
        }
        int lineStart = line.getStartOffset();
        int lineEnd = Math.min(line.getEndOffset(), doc.getLength());

        Segment seg = SegmentCache.getSharedSegment();
        doc.getText(lineStart, lineEnd - lineStart, seg);
        if(seg.count > 0) {
            BreakIterator words = BreakIterator.getWordInstance(c.getLocale());
            words.setText(seg);
            int wordPosition = offs - lineStart + seg.offset;
            if(wordPosition >= words.last()) {
                wordPosition = words.last() - 1;
            }
            offs = lineStart + words.following(wordPosition) - seg.offset;
        }
        SegmentCache.releaseSharedSegment(seg);
        return offs;
    }

    /**
     * Determines the start of the next word for the given location.
     * Uses BreakIterator.getWordInstance() to actually get the words.
     *
     * @param c the editor
     * @param offs the offset in the document &gt;= 0
     * @return the location in the model of the word start &gt;= 0
     * @exception BadLocationException if the offset is out of range
     */
    public static final int getNextWord(JTextComponent c, int offs) throws BadLocationException {
        int nextWord;
        Element line = getParagraphElement(c, offs);
        for (nextWord = getNextWordInParagraph(c, line, offs, false);
             nextWord == BreakIterator.DONE;
             nextWord = getNextWordInParagraph(c, line, offs, true)) {

            // didn't find in this line, try the next line
            offs = line.getEndOffset();
            line = getParagraphElement(c, offs);
        }
        return nextWord;
    }

    /**
     * Finds the next word in the given elements text.  The first
     * parameter allows searching multiple paragraphs where even
     * the first offset is desired.
     * Returns the offset of the next word, or BreakIterator.DONE
     * if there are no more words in the element.
     */
    static int getNextWordInParagraph(JTextComponent c, Element line, int offs, boolean first) throws BadLocationException {
        if (line == null) {
            throw new BadLocationException("No more words", offs);
        }
        Document doc = line.getDocument();
        int lineStart = line.getStartOffset();
        int lineEnd = Math.min(line.getEndOffset(), doc.getLength());
        if ((offs >= lineEnd) || (offs < lineStart)) {
            throw new BadLocationException("No more words", offs);
        }
        Segment seg = SegmentCache.getSharedSegment();
        doc.getText(lineStart, lineEnd - lineStart, seg);
        BreakIterator words = BreakIterator.getWordInstance(c.getLocale());
        words.setText(seg);
        if ((first && (words.first() == (seg.offset + offs - lineStart))) &&
            (! Character.isWhitespace(seg.array[words.first()]))) {

            return offs;
        }
        int wordPosition = words.following(seg.offset + offs - lineStart);
        if ((wordPosition == BreakIterator.DONE) ||
            (wordPosition >= seg.offset + seg.count)) {
                // there are no more words on this line.
                return BreakIterator.DONE;
        }
        // if we haven't shot past the end... check to
        // see if the current boundary represents whitespace.
        // if so, we need to try again
        char ch = seg.array[wordPosition];
        if (! Character.isWhitespace(ch)) {
            return lineStart + wordPosition - seg.offset;
        }

        // it was whitespace, try again.  The assumption
        // is that it must be a word start if the last
        // one had whitespace following it.
        wordPosition = words.next();
        if (wordPosition != BreakIterator.DONE) {
            offs = lineStart + wordPosition - seg.offset;
            if (offs != lineEnd) {
                return offs;
            }
        }
        SegmentCache.releaseSharedSegment(seg);
        return BreakIterator.DONE;
    }


    /**
     * Determine the start of the prev word for the given location.
     * Uses BreakIterator.getWordInstance() to actually get the words.
     *
     * @param c the editor
     * @param offs the offset in the document &gt;= 0
     * @return the location in the model of the word start &gt;= 0
     * @exception BadLocationException if the offset is out of range
     */
    public static final int getPreviousWord(JTextComponent c, int offs) throws BadLocationException {
        int prevWord;
        Element line = getParagraphElement(c, offs);
        for (prevWord = getPrevWordInParagraph(c, line, offs);
             prevWord == BreakIterator.DONE;
             prevWord = getPrevWordInParagraph(c, line, offs)) {

            // didn't find in this line, try the prev line
            offs = line.getStartOffset() - 1;
            line = getParagraphElement(c, offs);
        }
        return prevWord;
    }

    /**
     * Finds the previous word in the given elements text.  The first
     * parameter allows searching multiple paragraphs where even
     * the first offset is desired.
     * Returns the offset of the next word, or BreakIterator.DONE
     * if there are no more words in the element.
     */
    static int getPrevWordInParagraph(JTextComponent c, Element line, int offs) throws BadLocationException {
        if (line == null) {
            throw new BadLocationException("No more words", offs);
        }
        Document doc = line.getDocument();
        int lineStart = line.getStartOffset();
        int lineEnd = line.getEndOffset();
        if ((offs > lineEnd) || (offs < lineStart)) {
            throw new BadLocationException("No more words", offs);
        }
        Segment seg = SegmentCache.getSharedSegment();
        doc.getText(lineStart, lineEnd - lineStart, seg);
        BreakIterator words = BreakIterator.getWordInstance(c.getLocale());
        words.setText(seg);
        if (words.following(seg.offset + offs - lineStart) == BreakIterator.DONE) {
            words.last();
        }
        int wordPosition = words.previous();
        if (wordPosition == (seg.offset + offs - lineStart)) {
            wordPosition = words.previous();
        }

        if (wordPosition == BreakIterator.DONE) {
            // there are no more words on this line.
            return BreakIterator.DONE;
        }
        // if we haven't shot past the end... check to
        // see if the current boundary represents whitespace.
        // if so, we need to try again
        char ch = seg.array[wordPosition];
        if (! Character.isWhitespace(ch)) {
            return lineStart + wordPosition - seg.offset;
        }

        // it was whitespace, try again.  The assumption
        // is that it must be a word start if the last
        // one had whitespace following it.
        wordPosition = words.previous();
        if (wordPosition != BreakIterator.DONE) {
            return lineStart + wordPosition - seg.offset;
        }
        SegmentCache.releaseSharedSegment(seg);
        return BreakIterator.DONE;
    }

    /**
     * Determines the element to use for a paragraph/line.
     *
     * @param c the editor
     * @param offs the starting offset in the document &gt;= 0
     * @return the element
     */
    public static final Element getParagraphElement(JTextComponent c, int offs) {
        Document doc = c.getDocument();
        if (doc instanceof StyledDocument) {
            return ((StyledDocument)doc).getParagraphElement(offs);
        }
        Element map = doc.getDefaultRootElement();
        int index = map.getElementIndex(offs);
        Element paragraph = map.getElement(index);
        if ((offs >= paragraph.getStartOffset()) && (offs < paragraph.getEndOffset())) {
            return paragraph;
        }
        return null;
    }

    static boolean isComposedTextElement(Document doc, int offset) {
        Element elem = doc.getDefaultRootElement();
        while (!elem.isLeaf()) {
            elem = elem.getElement(elem.getElementIndex(offset));
        }
        return isComposedTextElement(elem);
    }

    static boolean isComposedTextElement(Element elem) {
        AttributeSet as = elem.getAttributes();
        return isComposedTextAttributeDefined(as);
    }

    static boolean isComposedTextAttributeDefined(AttributeSet as) {
        return ((as != null) &&
                (as.isDefined(StyleConstants.ComposedTextAttribute)));
    }

    /**
     * Draws the given composed text passed from an input method.
     *
     * @param view View hosting text
     * @param attr the attributes containing the composed text
     * @param g  the graphics context
     * @param x  the X origin
     * @param y  the Y origin
     * @param p0 starting offset in the composed text to be rendered
     * @param p1 ending offset in the composed text to be rendered
     * @return  the new insertion position
     */
    static int drawComposedText(View view, AttributeSet attr, Graphics g,
                                int x, int y, int p0, int p1)
            throws BadLocationException
    {
        return (int) drawComposedText(view, attr, g, x, y, p0, p1, false);
    }

    static float drawComposedText(View view, AttributeSet attr, Graphics g,
                                  float x, float y, int p0, int p1)
            throws BadLocationException
    {
        return drawComposedText(view, attr, g, x, y, p0, p1, true);
    }

    static float drawComposedText(View view, AttributeSet attr, Graphics g,
                                  float x, float y, int p0, int p1,
                                  boolean useFPAPI)
            throws BadLocationException
    {
        Graphics2D g2d = (Graphics2D)g;
        AttributedString as = (AttributedString)attr.getAttribute(
            StyleConstants.ComposedTextAttribute);
        as.addAttribute(TextAttribute.FONT, g.getFont());

        if (p0 >= p1)
            return x;

        AttributedCharacterIterator aci = as.getIterator(null, p0, p1);
        return x + SwingUtilities2.drawString(getJComponent(view), g2d, aci, x, y);
    }

    /**
     * Paints the composed text in a GlyphView
     */
    static void paintComposedText(Graphics g, Rectangle alloc, GlyphView v) {
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            int p0 = v.getStartOffset();
            int p1 = v.getEndOffset();
            AttributeSet attrSet = v.getElement().getAttributes();
            AttributedString as =
                (AttributedString)attrSet.getAttribute(StyleConstants.ComposedTextAttribute);
            int start = v.getElement().getStartOffset();
            int y = alloc.y + alloc.height - (int)v.getGlyphPainter().getDescent(v);
            int x = alloc.x;

            //Add text attributes
            as.addAttribute(TextAttribute.FONT, v.getFont());
            as.addAttribute(TextAttribute.FOREGROUND, v.getForeground());
            if (StyleConstants.isBold(v.getAttributes())) {
                as.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
            }
            if (StyleConstants.isItalic(v.getAttributes())) {
                as.addAttribute(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
            }
            if (v.isUnderline()) {
                as.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            }
            if (v.isStrikeThrough()) {
                as.addAttribute(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
            }
            if (v.isSuperscript()) {
                as.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER);
            }
            if (v.isSubscript()) {
                as.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB);
            }

            // draw
            AttributedCharacterIterator aci = as.getIterator(null, p0 - start, p1 - start);
            SwingUtilities2.drawString(getJComponent(v),
                                       g2d,aci,x,y);
        }
    }

    /*
     * Convenience function for determining ComponentOrientation.  Helps us
     * avoid having Munge directives throughout the code.
     */
    static boolean isLeftToRight( java.awt.Component c ) {
        return c.getComponentOrientation().isLeftToRight();
    }


    /**
     * Provides a way to determine the next visually represented model
     * location that one might place a caret.  Some views may not be visible,
     * they might not be in the same order found in the model, or they just
     * might not allow access to some of the locations in the model.
     * <p>
     * This implementation assumes the views are layed out in a logical
     * manner. That is, that the view at index x + 1 is visually after
     * the View at index x, and that the View at index x - 1 is visually
     * before the View at x. There is support for reversing this behavior
     * only if the passed in <code>View</code> is an instance of
     * <code>CompositeView</code>. The <code>CompositeView</code>
     * must then override the <code>flipEastAndWestAtEnds</code> method.
     *
     * @param v View to query
     * @param pos the position to convert &gt;= 0
     * @param alloc the allocated region to render into
     * @param direction the direction from the current position that can
     *  be thought of as the arrow keys typically found on a keyboard;
     *  this may be one of the following:
     *  <ul>
     *  <li><code>SwingConstants.WEST</code>
     *  <li><code>SwingConstants.EAST</code>
     *  <li><code>SwingConstants.NORTH</code>
     *  <li><code>SwingConstants.SOUTH</code>
     *  </ul>
     * @param biasRet an array contain the bias that was checked
     * @return the location within the model that best represents the next
     *  location visual position
     * @exception BadLocationException
     * @exception IllegalArgumentException if <code>direction</code> is invalid
     */
    static int getNextVisualPositionFrom(View v, int pos, Position.Bias b,
                                          Shape alloc, int direction,
                                          Position.Bias[] biasRet)
                             throws BadLocationException {
        if (v.getViewCount() == 0) {
            // Nothing to do.
            return pos;
        }
        boolean top = (direction == SwingConstants.NORTH ||
                       direction == SwingConstants.WEST);
        int retValue;
        if (pos == -1) {
            // Start from the first View.
            int childIndex = (top) ? v.getViewCount() - 1 : 0;
            View child = v.getView(childIndex);
            Shape childBounds = v.getChildAllocation(childIndex, alloc);
            retValue = child.getNextVisualPositionFrom(pos, b, childBounds,
                                                       direction, biasRet);
            if (retValue == -1 && !top && v.getViewCount() > 1) {
                // Special case that should ONLY happen if first view
                // isn't valid (can happen when end position is put at
                // beginning of line.
                child = v.getView(1);
                childBounds = v.getChildAllocation(1, alloc);
                retValue = child.getNextVisualPositionFrom(-1, biasRet[0],
                                                           childBounds,
                                                           direction, biasRet);
            }
        }
        else {
            int increment = (top) ? -1 : 1;
            int childIndex;
            if (b == Position.Bias.Backward && pos > 0) {
                childIndex = v.getViewIndex(pos - 1, Position.Bias.Forward);
            }
            else {
                childIndex = v.getViewIndex(pos, Position.Bias.Forward);
            }
            View child = v.getView(childIndex);
            Shape childBounds = v.getChildAllocation(childIndex, alloc);
            retValue = child.getNextVisualPositionFrom(pos, b, childBounds,
                                                       direction, biasRet);
            if ((direction == SwingConstants.EAST ||
                 direction == SwingConstants.WEST) &&
                (v instanceof CompositeView) &&
                ((CompositeView)v).flipEastAndWestAtEnds(pos, b)) {
                increment *= -1;
            }
            childIndex += increment;
            if (retValue == -1 && childIndex >= 0 &&
                                  childIndex < v.getViewCount()) {
                child = v.getView(childIndex);
                childBounds = v.getChildAllocation(childIndex, alloc);
                retValue = child.getNextVisualPositionFrom(
                                     -1, b, childBounds, direction, biasRet);
                // If there is a bias change, it is a fake position
                // and we should skip it. This is usually the result
                // of two elements side be side flowing the same way.
                if (retValue == pos && biasRet[0] != b) {
                    return getNextVisualPositionFrom(v, pos, biasRet[0],
                                                     alloc, direction,
                                                     biasRet);
                }
            }
            else if (retValue != -1 && biasRet[0] != b &&
                     ((increment == 1 && child.getEndOffset() == retValue) ||
                      (increment == -1 &&
                       child.getStartOffset() == retValue)) &&
                     childIndex >= 0 && childIndex < v.getViewCount()) {
                // Reached the end of a view, make sure the next view
                // is a different direction.
                child = v.getView(childIndex);
                childBounds = v.getChildAllocation(childIndex, alloc);
                Position.Bias originalBias = biasRet[0];
                int nextPos = child.getNextVisualPositionFrom(
                                    -1, b, childBounds, direction, biasRet);
                if (biasRet[0] == b) {
                    retValue = nextPos;
                }
                else {
                    biasRet[0] = originalBias;
                }
            }
        }
        return retValue;
    }
}
