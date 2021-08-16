/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.swing;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FocusTraversalPolicy;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.PrintGraphics;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextHitInfo;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.print.PrinterGraphics;
import java.beans.PropertyChangeEvent;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.TreeModelEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import sun.awt.AWTAccessor;
import sun.awt.AWTPermissions;
import sun.awt.AppContext;
import sun.awt.SunToolkit;
import sun.font.FontDesignMetrics;
import sun.font.FontUtilities;
import sun.java2d.SunGraphicsEnvironment;
import sun.print.ProxyPrintGraphics;

import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.KEY_TEXT_LCD_CONTRAST;
import static java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
import static java.awt.geom.AffineTransform.TYPE_FLIP;
import static java.awt.geom.AffineTransform.TYPE_MASK_SCALE;
import static java.awt.geom.AffineTransform.TYPE_TRANSLATION;

/**
 * A collection of utility methods for Swing.
 * <p>
 * <b>WARNING:</b> While this class is public, it should not be treated as
 * public API and its API may change in incompatable ways between dot dot
 * releases and even patch releases. You should not rely on this class even
 * existing.
 *
 */
public class SwingUtilities2 {
    /**
     * The {@code AppContext} key for our one {@code LAFState}
     * instance.
     */
    public static final Object LAF_STATE_KEY =
            new StringBuffer("LookAndFeel State");

    public static final Object MENU_SELECTION_MANAGER_LISTENER_KEY =
            new StringBuffer("MenuSelectionManager listener key");

    // Maintain a cache of CACHE_SIZE fonts and the left side bearing
     // of the characters falling into the range MIN_CHAR_INDEX to
     // MAX_CHAR_INDEX. The values in fontCache are created as needed.
     private static LSBCacheEntry[] fontCache;
     // Windows defines 6 font desktop properties, we will therefore only
     // cache the metrics for 6 fonts.
     private static final int CACHE_SIZE = 6;
     // nextIndex in fontCache to insert a font into.
     private static int nextIndex;
     // LSBCacheEntry used to search in fontCache to see if we already
     // have an entry for a particular font
     private static LSBCacheEntry searchKey;

     // getLeftSideBearing will consult all characters that fall in the
     // range MIN_CHAR_INDEX to MAX_CHAR_INDEX.
     private static final int MIN_CHAR_INDEX = (int)'W';
     private static final int MAX_CHAR_INDEX = (int)'W' + 1;

    public static final FontRenderContext DEFAULT_FRC =
        new FontRenderContext(null, false, false);

    /**
     * Attribute key for the content elements.  If it is set on an element, the
     * element is considered to be a line break.
     */
    public static final String IMPLIED_CR = "CR";

    /**
     * Used to tell a text component, being used as an editor for table
     * or tree, how many clicks it took to start editing.
     */
    private static final StringBuilder SKIP_CLICK_COUNT =
        new StringBuilder("skipClickCount");

    @SuppressWarnings("unchecked")
    public static void putAATextInfo(boolean lafCondition,
            Map<Object, Object> map) {
        SunToolkit.setAAFontSettingsCondition(lafCondition);
        Toolkit tk = Toolkit.getDefaultToolkit();
        Object desktopHints = tk.getDesktopProperty(SunToolkit.DESKTOPFONTHINTS);

        if (desktopHints instanceof Map) {
            Map<Object, Object> hints = (Map<Object, Object>) desktopHints;
            Object aaHint = hints.get(KEY_TEXT_ANTIALIASING);
            if (aaHint == null
                    || aaHint == VALUE_TEXT_ANTIALIAS_OFF
                    || aaHint == VALUE_TEXT_ANTIALIAS_DEFAULT) {
                return;
            }
            map.put(KEY_TEXT_ANTIALIASING, aaHint);
            map.put(KEY_TEXT_LCD_CONTRAST, hints.get(KEY_TEXT_LCD_CONTRAST));
        }
    }

    /** Client Property key for the text maximal offsets for BasicMenuItemUI */
    public static final StringUIClientPropertyKey BASICMENUITEMUI_MAX_TEXT_OFFSET =
        new StringUIClientPropertyKey ("maxTextOffset");

    // security stuff
    private static final String UntrustedClipboardAccess =
        "UNTRUSTED_CLIPBOARD_ACCESS_KEY";

    //all access to  charsBuffer is to be synchronized on charsBufferLock
    private static final int CHAR_BUFFER_SIZE = 100;
    private static final Object charsBufferLock = new Object();
    private static char[] charsBuffer = new char[CHAR_BUFFER_SIZE];

    static {
        fontCache = new LSBCacheEntry[CACHE_SIZE];
    }

    /**
     * Fill the character buffer cache.  Return the buffer length.
     */
    private static int syncCharsBuffer(String s) {
        int length = s.length();
        if ((charsBuffer == null) || (charsBuffer.length < length)) {
            charsBuffer = s.toCharArray();
        } else {
            s.getChars(0, length, charsBuffer, 0);
        }
        return length;
    }

    /**
     * checks whether TextLayout is required to handle characters.
     *
     * @param text characters to be tested
     * @param start start
     * @param limit limit
     * @return {@code true}  if TextLayout is required
     *         {@code false} if TextLayout is not required
     */
    public static final boolean isComplexLayout(char[] text, int start, int limit) {
        return FontUtilities.isComplexText(text, start, limit);
    }

    //
    // WARNING WARNING WARNING WARNING WARNING WARNING
    // Many of the following methods are invoked from older API.
    // As this older API was not passed a Component, a null Component may
    // now be passsed in.  For example, SwingUtilities.computeStringWidth
    // is implemented to call SwingUtilities2.stringWidth, the
    // SwingUtilities variant does not take a JComponent, as such
    // SwingUtilities2.stringWidth can be passed a null Component.
    // In other words, if you add new functionality to these methods you
    // need to gracefully handle null.
    //

    /**
     * Returns the left side bearing of the first character of string. The
     * left side bearing is calculated from the passed in
     * FontMetrics.  If the passed in String is less than one
     * character {@code 0} is returned.
     *
     * @param c JComponent that will display the string
     * @param fm FontMetrics used to measure the String width
     * @param string String to get the left side bearing for.
     * @throws NullPointerException if {@code string} is {@code null}
     *
     * @return the left side bearing of the first character of string
     * or {@code 0} if the string is empty
     */
    public static int getLeftSideBearing(JComponent c, FontMetrics fm,
                                         String string) {
        if ((string == null) || (string.length() == 0)) {
            return 0;
        }
        return getLeftSideBearing(c, fm, string.charAt(0));
    }

    /**
     * Returns the left side bearing of the first character of string. The
     * left side bearing is calculated from the passed in FontMetrics.
     *
     * @param c JComponent that will display the string
     * @param fm FontMetrics used to measure the String width
     * @param firstChar Character to get the left side bearing for.
     */
    public static int getLeftSideBearing(JComponent c, FontMetrics fm,
                                         char firstChar) {
        int charIndex = (int) firstChar;
        if (charIndex < MAX_CHAR_INDEX && charIndex >= MIN_CHAR_INDEX) {
            byte[] lsbs = null;

            FontRenderContext frc = getFontRenderContext(c, fm);
            Font font = fm.getFont();
            synchronized (SwingUtilities2.class) {
                LSBCacheEntry entry = null;
                if (searchKey == null) {
                    searchKey = new LSBCacheEntry(frc, font);
                } else {
                    searchKey.reset(frc, font);
                }
                // See if we already have an entry for this pair
                for (LSBCacheEntry cacheEntry : fontCache) {
                    if (searchKey.equals(cacheEntry)) {
                        entry = cacheEntry;
                        break;
                    }
                }
                if (entry == null) {
                    // No entry for this pair, add it.
                    entry = searchKey;
                    fontCache[nextIndex] = searchKey;
                    searchKey = null;
                    nextIndex = (nextIndex + 1) % CACHE_SIZE;
                }
                return entry.getLeftSideBearing(firstChar);
            }
        }
        return 0;
    }

    /**
     * Returns the FontMetrics for the current Font of the passed
     * in Graphics.  This method is used when a Graphics
     * is available, typically when painting.  If a Graphics is not
     * available the JComponent method of the same name should be used.
     * <p>
     * Callers should pass in a non-null JComponent, the exception
     * to this is if a JComponent is not readily available at the time of
     * painting.
     * <p>
     * This does not necessarily return the FontMetrics from the
     * Graphics.
     *
     * @param c JComponent requesting FontMetrics, may be null
     * @param g Graphics Graphics
     */
    public static FontMetrics getFontMetrics(JComponent c, Graphics g) {
        return getFontMetrics(c, g, g.getFont());
    }


    /**
     * Returns the FontMetrics for the specified Font.
     * This method is used when a Graphics is available, typically when
     * painting.  If a Graphics is not available the JComponent method of
     * the same name should be used.
     * <p>
     * Callers should pass in a non-null JComonent, the exception
     * to this is if a JComponent is not readily available at the time of
     * painting.
     * <p>
     * This does not necessarily return the FontMetrics from the
     * Graphics.
     *
     * @param c JComponent requesting FontMetrics, may be null
     * @param c Graphics Graphics
     * @param font Font to get FontMetrics for
     */
    @SuppressWarnings("deprecation")
    public static FontMetrics getFontMetrics(JComponent c, Graphics g,
                                             Font font) {
        if (c != null) {
            // Note: We assume that we're using the FontMetrics
            // from the widget to layout out text, otherwise we can get
            // mismatches when printing.
            return c.getFontMetrics(font);
        }
        return Toolkit.getDefaultToolkit().getFontMetrics(font);
    }


    /**
     * Returns the width of the passed in String.
     * If the passed String is {@code null}, returns zero.
     *
     * @param c JComponent that will display the string, may be null
     * @param fm FontMetrics used to measure the String width
     * @param string String to get the width of
     */
    public static int stringWidth(JComponent c, FontMetrics fm, String string) {
        return (int) stringWidth(c, fm, string, false);
    }

    /**
     * Returns the width of the passed in String.
     * If the passed String is {@code null}, returns zero.
     *
     * @param c JComponent that will display the string, may be null
     * @param fm FontMetrics used to measure the String width
     * @param string String to get the width of
     * @param useFPAPI use floating point API
     */
    public static float stringWidth(JComponent c, FontMetrics fm, String string,
            boolean useFPAPI){
        if (string == null || string.isEmpty()) {
            return 0;
        }
        boolean needsTextLayout = ((c != null) &&
                (c.getClientProperty(TextAttribute.NUMERIC_SHAPING) != null));
        if (needsTextLayout) {
            synchronized(charsBufferLock) {
                int length = syncCharsBuffer(string);
                needsTextLayout = isComplexLayout(charsBuffer, 0, length);
            }
        }
        if (needsTextLayout) {
            TextLayout layout = createTextLayout(c, string,
                                    fm.getFont(), fm.getFontRenderContext());
            return layout.getAdvance();
        } else {
            return getFontStringWidth(string, fm, useFPAPI);
        }
    }


    /**
     * Clips the passed in String to the space provided.
     *
     * @param c JComponent that will display the string, may be null
     * @param fm FontMetrics used to measure the String width
     * @param string String to display
     * @param availTextWidth Amount of space that the string can be drawn in
     * @return Clipped string that can fit in the provided space.
     */
    public static String clipStringIfNecessary(JComponent c, FontMetrics fm,
                                               String string,
                                               int availTextWidth) {
        if (string == null || string.isEmpty())  {
            return "";
        }
        int textWidth = SwingUtilities2.stringWidth(c, fm, string);
        if (textWidth > availTextWidth) {
            return SwingUtilities2.clipString(c, fm, string, availTextWidth);
        }
        return string;
    }


    /**
     * Clips the passed in String to the space provided.  NOTE: this assumes
     * the string does not fit in the available space.
     *
     * @param c JComponent that will display the string, may be null
     * @param fm FontMetrics used to measure the String width
     * @param string String to display
     * @param availTextWidth Amount of space that the string can be drawn in
     * @return Clipped string that can fit in the provided space.
     */
    public static String clipString(JComponent c, FontMetrics fm,
                                    String string, int availTextWidth) {
        // c may be null here.
        String clipString = "...";
        availTextWidth -= SwingUtilities2.stringWidth(c, fm, clipString);
        if (availTextWidth <= 0) {
            //can not fit any characters
            return clipString;
        }

        boolean needsTextLayout;
        synchronized (charsBufferLock) {
            int stringLength = syncCharsBuffer(string);
            needsTextLayout =
                isComplexLayout(charsBuffer, 0, stringLength);
            if (!needsTextLayout) {
                int width = 0;
                for (int nChars = 0; nChars < stringLength; nChars++) {
                    width += fm.charWidth(charsBuffer[nChars]);
                    if (width > availTextWidth) {
                        string = string.substring(0, nChars);
                        break;
                    }
                }
            }
        }
        if (needsTextLayout) {
            AttributedString aString = new AttributedString(string);
            if (c != null) {
                aString.addAttribute(TextAttribute.NUMERIC_SHAPING,
                        c.getClientProperty(TextAttribute.NUMERIC_SHAPING));
            }
            LineBreakMeasurer measurer = new LineBreakMeasurer(
                    aString.getIterator(), BreakIterator.getCharacterInstance(),
                    getFontRenderContext(c, fm));
            string = string.substring(0, measurer.nextOffset(availTextWidth));

        }
        return string + clipString;
    }


    /**
     * Draws the string at the specified location.
     *
     * @param c JComponent that will display the string, may be null
     * @param g Graphics to draw the text to
     * @param text String to display
     * @param x X coordinate to draw the text at
     * @param y Y coordinate to draw the text at
     */
    public static void drawString(JComponent c, Graphics g, String text,
                                  int x, int y) {
        drawString(c, g, text, x, y, false);
    }

    /**
     * Draws the string at the specified location.
     *
     * @param c JComponent that will display the string, may be null
     * @param g Graphics to draw the text to
     * @param text String to display
     * @param x X coordinate to draw the text at
     * @param y Y coordinate to draw the text at
     * @param useFPAPI use floating point API
     */
    public static void drawString(JComponent c, Graphics g, String text,
                                  float x, float y, boolean useFPAPI) {
        // c may be null

        // All non-editable widgets that draw strings call into this
        // methods.  By non-editable that means widgets like JLabel, JButton
        // but NOT JTextComponents.
        if ( text == null || text.length() <= 0 ) { //no need to paint empty strings
            return;
        }
        if (isPrinting(g)) {
            Graphics2D g2d = getGraphics2D(g);
            if (g2d != null) {
                /* The printed text must scale linearly with the UI.
                 * Calculate the width on screen, obtain a TextLayout with
                 * advances for the printer graphics FRC, and then justify
                 * it to fit in the screen width. This distributes the spacing
                 * more evenly than directly laying out to the screen advances.
                 */
                String trimmedText = trimTrailingSpaces(text);
                if (!trimmedText.isEmpty()) {
                    float screenWidth = (float) g2d.getFont().getStringBounds
                            (trimmedText, getFontRenderContext(c)).getWidth();
                    TextLayout layout = createTextLayout(c, text, g2d.getFont(),
                                                       g2d.getFontRenderContext());

                    // If text fits the screenWidth, then do not need to justify
                    if (SwingUtilities2.stringWidth(c, g2d.getFontMetrics(),
                                            trimmedText) > screenWidth) {
                        layout = layout.getJustifiedLayout(screenWidth);
                    }
                    /* Use alternate print color if specified */
                    Color col = g2d.getColor();
                    if (col instanceof PrintColorUIResource) {
                        g2d.setColor(((PrintColorUIResource)col).getPrintColor());
                    }

                    layout.draw(g2d, x, y);

                    g2d.setColor(col);
                }

                return;
            }
        }

        // If we get here we're not printing
        if (g instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D)g;

            boolean needsTextLayout = ((c != null) &&
                (c.getClientProperty(TextAttribute.NUMERIC_SHAPING) != null));

            if (needsTextLayout) {
                synchronized(charsBufferLock) {
                    int length = syncCharsBuffer(text);
                    needsTextLayout = isComplexLayout(charsBuffer, 0, length);
                }
            }

            Object aaHint = (c == null)
                                ? null
                                : c.getClientProperty(KEY_TEXT_ANTIALIASING);
            if (aaHint != null) {
                Object oldContrast = null;
                Object oldAAValue = g2.getRenderingHint(KEY_TEXT_ANTIALIASING);
                if (aaHint != oldAAValue) {
                    g2.setRenderingHint(KEY_TEXT_ANTIALIASING, aaHint);
                } else {
                    oldAAValue = null;
                }

                Object lcdContrastHint = c.getClientProperty(
                        KEY_TEXT_LCD_CONTRAST);
                if (lcdContrastHint != null) {
                    oldContrast = g2.getRenderingHint(KEY_TEXT_LCD_CONTRAST);
                    if (lcdContrastHint.equals(oldContrast)) {
                        oldContrast = null;
                    } else {
                        g2.setRenderingHint(KEY_TEXT_LCD_CONTRAST,
                                            lcdContrastHint);
                    }
                }

                if (needsTextLayout) {
                    TextLayout layout = createTextLayout(c, text, g2.getFont(),
                                                    g2.getFontRenderContext());
                    layout.draw(g2, x, y);
                } else {
                    g2.drawString(text, x, y);
                }

                if (oldAAValue != null) {
                    g2.setRenderingHint(KEY_TEXT_ANTIALIASING, oldAAValue);
                }
                if (oldContrast != null) {
                    g2.setRenderingHint(KEY_TEXT_LCD_CONTRAST, oldContrast);
                }

                return;
            }

            if (needsTextLayout){
                TextLayout layout = createTextLayout(c, text, g2.getFont(),
                                                    g2.getFontRenderContext());
                layout.draw(g2, x, y);
                return;
            }
        }

        g.drawString(text, (int) x, (int) y);
    }

    /**
     * Draws the string at the specified location underlining the specified
     * character.
     *
     * @param c JComponent that will display the string, may be null
     * @param g Graphics to draw the text to
     * @param text String to display
     * @param underlinedIndex Index of a character in the string to underline
     * @param x X coordinate to draw the text at
     * @param y Y coordinate to draw the text at
     */

    public static void drawStringUnderlineCharAt(JComponent c,Graphics g,
                           String text, int underlinedIndex, int x, int y) {
        drawStringUnderlineCharAt(c, g, text, underlinedIndex, x, y, false);
    }
    /**
     * Draws the string at the specified location underlining the specified
     * character.
     *
     * @param c JComponent that will display the string, may be null
     * @param g Graphics to draw the text to
     * @param text String to display
     * @param underlinedIndex Index of a character in the string to underline
     * @param x X coordinate to draw the text at
     * @param y Y coordinate to draw the text at
     * @param useFPAPI use floating point API
     */
    public static void drawStringUnderlineCharAt(JComponent c, Graphics g,
                                                 String text, int underlinedIndex,
                                                 float x, float y,
                                                 boolean useFPAPI) {
        if (text == null || text.length() <= 0) {
            return;
        }
        SwingUtilities2.drawString(c, g, text, x, y, useFPAPI);
        int textLength = text.length();
        if (underlinedIndex >= 0 && underlinedIndex < textLength ) {
            float underlineRectY = y;
            int underlineRectHeight = 1;
            float underlineRectX = 0;
            int underlineRectWidth = 0;
            boolean isPrinting = isPrinting(g);
            boolean needsTextLayout = isPrinting;
            if (!needsTextLayout) {
                synchronized (charsBufferLock) {
                    syncCharsBuffer(text);
                    needsTextLayout =
                        isComplexLayout(charsBuffer, 0, textLength);
                }
            }
            if (!needsTextLayout) {
                FontMetrics fm = g.getFontMetrics();
                underlineRectX = x +
                    SwingUtilities2.stringWidth(c,fm,
                                        text.substring(0,underlinedIndex));
                underlineRectWidth = fm.charWidth(text.
                                                  charAt(underlinedIndex));
            } else {
                Graphics2D g2d = getGraphics2D(g);
                if (g2d != null) {
                    TextLayout layout =
                        createTextLayout(c, text, g2d.getFont(),
                                       g2d.getFontRenderContext());
                    if (isPrinting) {
                        float screenWidth = (float)g2d.getFont().
                            getStringBounds(text, getFontRenderContext(c)).getWidth();
                        // If text fits the screenWidth, then do not need to justify
                        if (SwingUtilities2.stringWidth(c, g2d.getFontMetrics(),
                                                        text) > screenWidth) {
                            layout = layout.getJustifiedLayout(screenWidth);
                        }
                    }
                    TextHitInfo leading =
                        TextHitInfo.leading(underlinedIndex);
                    TextHitInfo trailing =
                        TextHitInfo.trailing(underlinedIndex);
                    Shape shape =
                        layout.getVisualHighlightShape(leading, trailing);
                    Rectangle rect = shape.getBounds();
                    underlineRectX = x + rect.x;
                    underlineRectWidth = rect.width;
                }
            }
            g.fillRect((int) underlineRectX, (int) underlineRectY + 1,
                       underlineRectWidth, underlineRectHeight);
        }
    }


    /**
     * A variation of locationToIndex() which only returns an index if the
     * Point is within the actual bounds of a list item (not just in the cell)
     * and if the JList has the "List.isFileList" client property set.
     * Otherwise, this method returns -1.
     * This is used to make Windows {@literal L&F} JFileChooser act
     * like native dialogs.
     */
    public static int loc2IndexFileList(JList<?> list, Point point) {
        int index = list.locationToIndex(point);
        if (index != -1) {
            Object bySize = list.getClientProperty("List.isFileList");
            if (bySize instanceof Boolean && ((Boolean)bySize).booleanValue() &&
                !pointIsInActualBounds(list, index, point)) {
                index = -1;
            }
        }
        return index;
    }


    /**
     * Returns true if the given point is within the actual bounds of the
     * JList item at index (not just inside the cell).
     */
    private static <T> boolean pointIsInActualBounds(JList<T> list, int index,
                                                Point point) {
        ListCellRenderer<? super T> renderer = list.getCellRenderer();
        T value = list.getModel().getElementAt(index);
        Component item = renderer.getListCellRendererComponent(list,
                          value, index, false, false);
        Dimension itemSize = item.getPreferredSize();
        Rectangle cellBounds = list.getCellBounds(index, index);
        if (!item.getComponentOrientation().isLeftToRight()) {
            cellBounds.x += (cellBounds.width - itemSize.width);
        }
        cellBounds.width = itemSize.width;

        return cellBounds.contains(point);
    }


    /**
     * Returns true if the given point is outside the preferredSize of the
     * item at the given row of the table.  (Column must be 0).
     * Does not check the "Table.isFileList" property. That should be checked
     * before calling this method.
     * This is used to make Windows {@literal L&F} JFileChooser act
     * like native dialogs.
     */
    public static boolean pointOutsidePrefSize(JTable table, int row, int column, Point p) {
        if (table.convertColumnIndexToModel(column) != 0 || row == -1) {
            return true;
        }
        TableCellRenderer tcr = table.getCellRenderer(row, column);
        Object value = table.getValueAt(row, column);
        Component cell = tcr.getTableCellRendererComponent(table, value, false,
                false, row, column);
        Dimension itemSize = cell.getPreferredSize();
        Rectangle cellBounds = table.getCellRect(row, column, false);
        cellBounds.width = itemSize.width;
        cellBounds.height = itemSize.height;

        // See if coords are inside
        // ASSUME: mouse x,y will never be < cell's x,y
        assert (p.x >= cellBounds.x && p.y >= cellBounds.y);
        return p.x > cellBounds.x + cellBounds.width ||
                p.y > cellBounds.y + cellBounds.height;
    }

    /**
     * Set the lead and anchor without affecting selection.
     */
    public static void setLeadAnchorWithoutSelection(ListSelectionModel model,
                                                     int lead, int anchor) {
        if (anchor == -1) {
            anchor = lead;
        }
        if (lead == -1) {
            model.setAnchorSelectionIndex(-1);
            model.setLeadSelectionIndex(-1);
        } else {
            if (model.isSelectedIndex(lead)) {
                model.addSelectionInterval(lead, lead);
            } else {
                model.removeSelectionInterval(lead, lead);
            }
            model.setAnchorSelectionIndex(anchor);
        }
    }

    /**
     * Ignore mouse events if the component is null, not enabled, the event
     * is not associated with the left mouse button, or the event has been
     * consumed.
     */
    public static boolean shouldIgnore(MouseEvent me, JComponent c) {
        return c == null || !c.isEnabled()
                         || !SwingUtilities.isLeftMouseButton(me)
                         || me.isConsumed();
    }

    /**
     * Request focus on the given component if it doesn't already have it
     * and {@code isRequestFocusEnabled()} returns true.
     */
    public static void adjustFocus(JComponent c) {
        if (!c.hasFocus() && c.isRequestFocusEnabled()) {
            c.requestFocus();
        }
    }

    /**
     * The following draw functions have the same semantic as the
     * Graphics methods with the same names.
     *
     * this is used for printing
     */
    public static int drawChars(JComponent c, Graphics g,
                                 char[] data,
                                 int offset,
                                 int length,
                                 int x,
                                 int y) {
        return (int) drawChars(c, g, data, offset, length, x, y, false);
    }

    public static float drawChars(JComponent c, Graphics g,
                                 char[] data,
                                 int offset,
                                 int length,
                                 float x,
                                 float y) {
        return drawChars(c, g, data, offset, length, x, y, true);
    }

    public static float drawChars(JComponent c, Graphics g,
                                 char[] data,
                                 int offset,
                                 int length,
                                 float x,
                                 float y,
                                 boolean useFPAPI) {
        if ( length <= 0 ) { //no need to paint empty strings
            return x;
        }
        float nextX = x + getFontCharsWidth(data, offset, length,
                                            getFontMetrics(c, g),
                                            useFPAPI);
        if (isPrinting(g)) {
            Graphics2D g2d = getGraphics2D(g);
            if (g2d != null) {
                FontRenderContext deviceFontRenderContext = g2d.
                    getFontRenderContext();
                FontRenderContext frc = getFontRenderContext(c);
                if (frc != null &&
                    !isFontRenderContextPrintCompatible
                    (deviceFontRenderContext, frc)) {

                    String text = new String(data, offset, length);
                    TextLayout layout = new TextLayout(text, g2d.getFont(),
                                    deviceFontRenderContext);
                    String trimmedText = trimTrailingSpaces(text);
                    if (!trimmedText.isEmpty()) {
                        float screenWidth = (float)g2d.getFont().
                            getStringBounds(trimmedText, frc).getWidth();
                        // If text fits the screenWidth, then do not need to justify
                        if (SwingUtilities2.stringWidth(c, g2d.getFontMetrics(),
                                                trimmedText) > screenWidth) {
                            layout = layout.getJustifiedLayout(screenWidth);
                        }

                        /* Use alternate print color if specified */
                        Color col = g2d.getColor();
                        if (col instanceof PrintColorUIResource) {
                            g2d.setColor(((PrintColorUIResource)col).getPrintColor());
                        }

                        layout.draw(g2d,x,y);

                        g2d.setColor(col);
                    }

                    return nextX;
                }
            }
        }
        // Assume we're not printing if we get here, or that we are invoked
        // via Swing text printing which is laid out for the printer.
        Object aaHint = (c == null)
                            ? null
                            : c.getClientProperty(KEY_TEXT_ANTIALIASING);

        if (!(g instanceof Graphics2D)) {
            g.drawChars(data, offset, length, (int) x, (int) y);
            return nextX;
        }

        Graphics2D g2 = (Graphics2D) g;
        if (aaHint != null) {

            Object oldContrast = null;
            Object oldAAValue = g2.getRenderingHint(KEY_TEXT_ANTIALIASING);
            if (aaHint != null && aaHint != oldAAValue) {
                g2.setRenderingHint(KEY_TEXT_ANTIALIASING, aaHint);
            } else {
                oldAAValue = null;
            }

            Object lcdContrastHint = c.getClientProperty(KEY_TEXT_LCD_CONTRAST);
            if (lcdContrastHint != null) {
                oldContrast = g2.getRenderingHint(KEY_TEXT_LCD_CONTRAST);
                if (lcdContrastHint.equals(oldContrast)) {
                    oldContrast = null;
                } else {
                    g2.setRenderingHint(KEY_TEXT_LCD_CONTRAST,
                                        lcdContrastHint);
                }
            }

            g2.drawString(new String(data, offset, length), x, y);

            if (oldAAValue != null) {
                g2.setRenderingHint(KEY_TEXT_ANTIALIASING, oldAAValue);
            }
            if (oldContrast != null) {
                g2.setRenderingHint(KEY_TEXT_LCD_CONTRAST, oldContrast);
            }
        }
        else {
            g2.drawString(new String(data, offset, length), x, y);
        }
        return nextX;
    }

    public static float getFontCharWidth(char c, FontMetrics fm,
                                         boolean useFPAPI)
    {
        return getFontCharsWidth(new char[]{c}, 0, 1, fm, useFPAPI);
    }

    public static float getFontCharsWidth(char[] data, int offset, int len,
                                          FontMetrics fm,
                                          boolean useFPAPI)
    {
        if (len == 0) {
           return 0;
        }
        if (useFPAPI) {
            Rectangle2D bounds = fm.getFont().
                                     getStringBounds(data, offset, offset + len,
                                                     fm.getFontRenderContext());
            return (float) bounds.getWidth();
        } else {
            return fm.charsWidth(data, offset, len);
        }
    }

    public static float getFontStringWidth(String data, FontMetrics fm,
                                           boolean useFPAPI)
    {
        if (useFPAPI) {
            Rectangle2D bounds = fm.getFont()
                    .getStringBounds(data, fm.getFontRenderContext());
            return (float) bounds.getWidth();
        } else {
            return fm.stringWidth(data);
        }
    }

    /*
     * see documentation for drawChars
     * returns the advance
     */
    public static float drawString(JComponent c, Graphics g,
                                   AttributedCharacterIterator iterator,
                                   int x, int y)
    {
        return drawStringImpl(c, g, iterator, x, y);
    }

    public static float drawString(JComponent c, Graphics g,
                                   AttributedCharacterIterator iterator,
                                   float x, float y)
    {
        return drawStringImpl(c, g, iterator, x, y);
    }

    private static float drawStringImpl(JComponent c, Graphics g,
                                   AttributedCharacterIterator iterator,
                                   float x, float y)
    {

        float retVal;
        boolean isPrinting = isPrinting(g);
        Color col = g.getColor();

        if (isPrinting) {
            /* Use alternate print color if specified */
            if (col instanceof PrintColorUIResource) {
                g.setColor(((PrintColorUIResource)col).getPrintColor());
            }
        }

        Graphics2D g2d = getGraphics2D(g);
        if (g2d == null) {
            g.drawString(iterator, (int)x, (int)y); //for the cases where advance
                                                    //matters it should not happen
            retVal = x;

        } else {
            FontRenderContext frc;
            if (isPrinting) {
                frc = getFontRenderContext(c);
                if (frc.isAntiAliased() || frc.usesFractionalMetrics()) {
                    frc = new FontRenderContext(frc.getTransform(), false, false);
                }
            } else if ((frc = getFRCProperty(c)) != null) {
                /* frc = frc; ! */
            } else {
                frc = g2d.getFontRenderContext();
            }
            TextLayout layout;
            if (isPrinting) {
                FontRenderContext deviceFRC = g2d.getFontRenderContext();
                if (!isFontRenderContextPrintCompatible(frc, deviceFRC)) {
                    layout = new TextLayout(iterator, deviceFRC);
                    AttributedCharacterIterator trimmedIt =
                            getTrimmedTrailingSpacesIterator(iterator);
                    if (trimmedIt != null) {
                        float screenWidth = new TextLayout(trimmedIt, frc).
                                getAdvance();
                        layout = layout.getJustifiedLayout(screenWidth);
                    }
                } else {
                    layout = new TextLayout(iterator, frc);
                }
            } else {
                layout = new TextLayout(iterator, frc);
            }
            layout.draw(g2d, x, y);
            retVal = layout.getAdvance();
        }

        if (isPrinting) {
            g.setColor(col);
        }

        return retVal;
    }

    /**
     * This method should be used for drawing a borders over a filled rectangle.
     * Draws vertical line, using the current color, between the points {@code
     * (x, y1)} and {@code (x, y2)} in graphics context's coordinate system.
     * Note: it use {@code Graphics.fillRect()} internally.
     *
     * @param g  Graphics to draw the line to.
     * @param x  the <i>x</i> coordinate.
     * @param y1 the first point's <i>y</i> coordinate.
     * @param y2 the second point's <i>y</i> coordinate.
     */
    public static void drawVLine(Graphics g, int x, int y1, int y2) {
        if (y2 < y1) {
            final int temp = y2;
            y2 = y1;
            y1 = temp;
        }
        g.fillRect(x, y1, 1, y2 - y1 + 1);
    }

    /**
     * This method should be used for drawing a borders over a filled rectangle.
     * Draws horizontal line, using the current color, between the points {@code
     * (x1, y)} and {@code (x2, y)} in graphics context's coordinate system.
     * Note: it use {@code Graphics.fillRect()} internally.
     *
     * @param g  Graphics to draw the line to.
     * @param x1 the first point's <i>x</i> coordinate.
     * @param x2 the second point's <i>x</i> coordinate.
     * @param y  the <i>y</i> coordinate.
     */
    public static void drawHLine(Graphics g, int x1, int x2, int y) {
        if (x2 < x1) {
            final int temp = x2;
            x2 = x1;
            x1 = temp;
        }
        g.fillRect(x1, y, x2 - x1 + 1, 1);
    }

    /**
     * This method should be used for drawing a borders over a filled rectangle.
     * Draws the outline of the specified rectangle. The left and right edges of
     * the rectangle are at {@code x} and {@code x + w}. The top and bottom
     * edges are at {@code y} and {@code y + h}. The rectangle is drawn using
     * the graphics context's current color. Note: it use {@code
     * Graphics.fillRect()} internally.
     *
     * @param g Graphics to draw the rectangle to.
     * @param x the <i>x</i> coordinate of the rectangle to be drawn.
     * @param y the <i>y</i> coordinate of the rectangle to be drawn.
     * @param w the w of the rectangle to be drawn.
     * @param h the h of the rectangle to be drawn.
     * @see SwingUtilities2#drawVLine(java.awt.Graphics, int, int, int)
     * @see SwingUtilities2#drawHLine(java.awt.Graphics, int, int, int)
     */
    public static void drawRect(Graphics g, int x, int y, int w, int h) {
        if (w < 0 || h < 0) {
            return;
        }

        if (h == 0 || w == 0) {
            g.fillRect(x, y, w + 1, h + 1);
        } else {
            g.fillRect(x, y, w, 1);
            g.fillRect(x + w, y, 1, h);
            g.fillRect(x + 1, y + h, w, 1);
            g.fillRect(x, y + 1, 1, h);
        }
    }

    private static TextLayout createTextLayout(JComponent c, String s,
                                            Font f, FontRenderContext frc) {
        Object shaper = (c == null ?
                    null : c.getClientProperty(TextAttribute.NUMERIC_SHAPING));
        if (shaper == null) {
            return new TextLayout(s, f, frc);
        } else {
            Map<TextAttribute, Object> a = new HashMap<TextAttribute, Object>();
            a.put(TextAttribute.FONT, f);
            a.put(TextAttribute.NUMERIC_SHAPING, shaper);
            return new TextLayout(s, a, frc);
        }
    }

    /*
     * Checks if two given FontRenderContexts are compatible for printing.
     * We can't just use equals as we want to exclude from the comparison :
     * + whether AA is set as irrelevant for printing and shouldn't affect
     * printed metrics anyway
     * + any translation component in the transform of either FRC, as it
     * does not affect metrics.
     * Compatible means no special handling needed for text painting
     */
    private static boolean
        isFontRenderContextPrintCompatible(FontRenderContext frc1,
                                           FontRenderContext frc2) {

        if (frc1 == frc2) {
            return true;
        }

        if (frc1 == null || frc2 == null) { // not supposed to happen
            return false;
        }

        if (frc1.getFractionalMetricsHint() !=
            frc2.getFractionalMetricsHint()) {
            return false;
        }

        /* If both are identity, return true */
        if (!frc1.isTransformed() && !frc2.isTransformed()) {
            return true;
        }

        /* That's the end of the cheap tests, need to get and compare
         * the transform matrices. We don't care about the translation
         * components, so return true if they are otherwise identical.
         */
        double[] mat1 = new double[4];
        double[] mat2 = new double[4];
        frc1.getTransform().getMatrix(mat1);
        frc2.getTransform().getMatrix(mat2);
        return
            mat1[0] == mat2[0] &&
            mat1[1] == mat2[1] &&
            mat1[2] == mat2[2] &&
            mat1[3] == mat2[3];
    }

    /*
     * Tries it best to get Graphics2D out of the given Graphics
     * returns null if can not derive it.
     */
    public static Graphics2D getGraphics2D(Graphics g) {
        if (g instanceof Graphics2D) {
            return (Graphics2D) g;
        } else if (g instanceof ProxyPrintGraphics) {
            return (Graphics2D)(((ProxyPrintGraphics)g).getGraphics());
        } else {
            return null;
        }
    }

    /*
     * Returns FontRenderContext associated with Component.
     * FontRenderContext from Component.getFontMetrics is associated
     * with the component.
     *
     * Uses Component.getFontMetrics to get the FontRenderContext from.
     * see JComponent.getFontMetrics and TextLayoutStrategy.java
     */
    public static FontRenderContext getFontRenderContext(Component c) {
        assert c != null;
        if (c == null) {
            return DEFAULT_FRC;
        } else {
            return c.getFontMetrics(c.getFont()).getFontRenderContext();
        }
    }

    /**
     * A convenience method to get FontRenderContext.
     * Returns the FontRenderContext for the passed in FontMetrics or
     * for the passed in Component if FontMetrics is null
     */
    private static FontRenderContext getFontRenderContext(Component c, FontMetrics fm) {
        assert fm != null || c!= null;
        return (fm != null) ? fm.getFontRenderContext()
            : getFontRenderContext(c);
    }

    /*
     * This method is to be used only for JComponent.getFontMetrics.
     * In all other places to get FontMetrics we need to use
     * JComponent.getFontMetrics.
     *
     */
    public static FontMetrics getFontMetrics(JComponent c, Font font) {
        FontRenderContext  frc = getFRCProperty(c);
        if (frc == null) {
            frc = DEFAULT_FRC;
        }
        return FontDesignMetrics.getMetrics(font, frc);
    }


    /* Get any FontRenderContext associated with a JComponent
     * - may return null
     */
    private static FontRenderContext getFRCProperty(JComponent c) {
        if (c != null) {

            GraphicsConfiguration gc = c.getGraphicsConfiguration();
            AffineTransform tx = (gc == null) ? null : gc.getDefaultTransform();
            Object aaHint = c.getClientProperty(KEY_TEXT_ANTIALIASING);
            return getFRCFromCache(tx, aaHint);
        }
        return null;
    }

    private static final Object APP_CONTEXT_FRC_CACHE_KEY = new Object();

    private static FontRenderContext getFRCFromCache(AffineTransform tx,
                                                     Object aaHint) {
        if (tx == null && aaHint == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<Object, FontRenderContext> cache = (Map<Object, FontRenderContext>)
                AppContext.getAppContext().get(APP_CONTEXT_FRC_CACHE_KEY);

        if (cache == null) {
            cache = new HashMap<>();
            AppContext.getAppContext().put(APP_CONTEXT_FRC_CACHE_KEY, cache);
        }

        Object key = (tx == null)
                ? aaHint
                : (aaHint == null ? tx : new KeyPair(tx, aaHint));

        FontRenderContext frc = cache.get(key);
        if (frc == null) {
            aaHint = (aaHint == null) ? VALUE_TEXT_ANTIALIAS_OFF : aaHint;
            frc = new FontRenderContext(tx, aaHint,
                                        VALUE_FRACTIONALMETRICS_DEFAULT);
            cache.put(key, frc);
        }
        return frc;
    }

    private static class KeyPair {

        private final Object key1;
        private final Object key2;

        public KeyPair(Object key1, Object key2) {
            this.key1 = key1;
            this.key2 = key2;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof KeyPair)) {
                return false;
            }
            KeyPair that = (KeyPair) obj;
            return this.key1.equals(that.key1) && this.key2.equals(that.key2);
        }

        @Override
        public int hashCode() {
            return key1.hashCode() + 37 * key2.hashCode();
        }
    }

    /*
     * returns true if the Graphics is print Graphics
     * false otherwise
     */
    static boolean isPrinting(Graphics g) {
        return (g instanceof PrinterGraphics || g instanceof PrintGraphics);
    }

    private static String trimTrailingSpaces(String s) {
        int i = s.length() - 1;
        while(i >= 0 && Character.isWhitespace(s.charAt(i))) {
            i--;
        }
        return s.substring(0, i + 1);
    }

    private static AttributedCharacterIterator getTrimmedTrailingSpacesIterator
            (AttributedCharacterIterator iterator) {
        int curIdx = iterator.getIndex();

        char c = iterator.last();
        while(c != CharacterIterator.DONE && Character.isWhitespace(c)) {
            c = iterator.previous();
        }

        if (c != CharacterIterator.DONE) {
            int endIdx = iterator.getIndex();

            if (endIdx == iterator.getEndIndex() - 1) {
                iterator.setIndex(curIdx);
                return iterator;
            } else {
                AttributedString trimmedText = new AttributedString(iterator,
                        iterator.getBeginIndex(), endIdx + 1);
                return trimmedText.getIterator();
            }
        } else {
            return null;
        }
    }

    /**
     * Determines whether the SelectedTextColor should be used for painting text
     * foreground for the specified highlight.
     *
     * Returns true only if the highlight painter for the specified highlight
     * is the swing painter (whether inner class of javax.swing.text.DefaultHighlighter
     * or com.sun.java.swing.plaf.windows.WindowsTextUI) and its background color
     * is null or equals to the selection color of the text component.
     *
     * This is a hack for fixing both bugs 4761990 and 5003294
     */
    public static boolean useSelectedTextColor(Highlighter.Highlight h, JTextComponent c) {
        Highlighter.HighlightPainter painter = h.getPainter();
        String painterClass = painter.getClass().getName();
        if (painterClass.indexOf("javax.swing.text.DefaultHighlighter") != 0 &&
                painterClass.indexOf("com.sun.java.swing.plaf.windows.WindowsTextUI") != 0) {
            return false;
        }
        try {
            DefaultHighlighter.DefaultHighlightPainter defPainter =
                    (DefaultHighlighter.DefaultHighlightPainter) painter;
            if (defPainter.getColor() != null &&
                    !defPainter.getColor().equals(c.getSelectionColor())) {
                return false;
            }
        } catch (ClassCastException e) {
            return false;
        }
        return true;
    }

    /**
     * LSBCacheEntry is used to cache the left side bearing (lsb) for
     * a particular {@code Font} and {@code FontRenderContext}.
     * This only caches characters that fall in the range
     * {@code MIN_CHAR_INDEX} to {@code MAX_CHAR_INDEX}.
     */
    private static class LSBCacheEntry {
        // Used to indicate a particular entry in lsb has not been set.
        private static final byte UNSET = Byte.MAX_VALUE;
        // Used in creating a GlyphVector to get the lsb
        private static final char[] oneChar = new char[1];

        private byte[] lsbCache;
        private Font font;
        private FontRenderContext frc;


        public LSBCacheEntry(FontRenderContext frc, Font font) {
            lsbCache = new byte[MAX_CHAR_INDEX - MIN_CHAR_INDEX];
            reset(frc, font);

        }

        public void reset(FontRenderContext frc, Font font) {
            this.font = font;
            this.frc = frc;
            for (int counter = lsbCache.length - 1; counter >= 0; counter--) {
                lsbCache[counter] = UNSET;
            }
        }

        public int getLeftSideBearing(char aChar) {
            int index = aChar - MIN_CHAR_INDEX;
            assert (index >= 0 && index < (MAX_CHAR_INDEX - MIN_CHAR_INDEX));
            byte lsb = lsbCache[index];
            if (lsb == UNSET) {
                oneChar[0] = aChar;
                GlyphVector gv = font.createGlyphVector(frc, oneChar);
                lsb = (byte) gv.getGlyphPixelBounds(0, frc, 0f, 0f).x;
                if (lsb < 0) {
                    /* HRGB/HBGR LCD glyph images will always have a pixel
                     * on the left used in colour fringe reduction.
                     * Text rendering positions this correctly but here
                     * we are using the glyph image to adjust that position
                     * so must account for it.
                     */
                    Object aaHint = frc.getAntiAliasingHint();
                    if (aaHint == VALUE_TEXT_ANTIALIAS_LCD_HRGB ||
                            aaHint == VALUE_TEXT_ANTIALIAS_LCD_HBGR) {
                        lsb++;
                    }
                }
                lsbCache[index] = lsb;
            }
            return lsb;


        }

        public boolean equals(Object entry) {
            if (entry == this) {
                return true;
            }
            if (!(entry instanceof LSBCacheEntry)) {
                return false;
            }
            LSBCacheEntry oEntry = (LSBCacheEntry) entry;
            return (font.equals(oEntry.font) &&
                    frc.equals(oEntry.frc));
        }

        public int hashCode() {
            int result = 17;
            if (font != null) {
                result = 37 * result + font.hashCode();
            }
            if (frc != null) {
                result = 37 * result + frc.hashCode();
            }
            return result;
        }
    }

    /*
     * here goes the fix for 4856343 [Problem with applet interaction
     * with system selection clipboard]
     *
     * NOTE. In case isTrustedContext() no checking
     * are to be performed
     */

    /**
    * checks the security permissions for accessing system clipboard
    *
    * for untrusted context (see isTrustedContext) checks the
    * permissions for the current event being handled
    *
    */
   public static boolean canAccessSystemClipboard() {
       boolean canAccess = false;
       if (!GraphicsEnvironment.isHeadless()) {
           @SuppressWarnings("removal")
           SecurityManager sm = System.getSecurityManager();
           if (sm == null) {
               canAccess = true;
           } else {
               try {
                   sm.checkPermission(AWTPermissions.ACCESS_CLIPBOARD_PERMISSION);
                   canAccess = true;
               } catch (SecurityException e) {
               }
               if (canAccess && ! isTrustedContext()) {
                   canAccess = canCurrentEventAccessSystemClipboard(true);
               }
           }
       }
       return canAccess;
   }
    /**
    * Returns true if EventQueue.getCurrentEvent() has the permissions to
     * access the system clipboard
     */
    public static boolean canCurrentEventAccessSystemClipboard() {
        return  isTrustedContext()
            || canCurrentEventAccessSystemClipboard(false);
    }

    /**
     * Returns true if the given event has permissions to access the
     * system clipboard
     *
     * @param e AWTEvent to check
     */
    public static boolean canEventAccessSystemClipboard(AWTEvent e) {
        return isTrustedContext()
            || canEventAccessSystemClipboard(e, false);
    }

    /**
     * Returns true if the given event is corrent gesture for
     * accessing clipboard
     *
     * @param ie InputEvent to check
     */
    @SuppressWarnings("deprecation")
    private static boolean isAccessClipboardGesture(InputEvent ie) {
        boolean allowedGesture = false;
        if (ie instanceof KeyEvent) { //we can validate only keyboard gestures
            KeyEvent ke = (KeyEvent)ie;
            int keyCode = ke.getKeyCode();
            int keyModifiers = ke.getModifiers();
            switch(keyCode) {
            case KeyEvent.VK_C:
            case KeyEvent.VK_V:
            case KeyEvent.VK_X:
                allowedGesture = (keyModifiers == InputEvent.CTRL_MASK);
                break;
            case KeyEvent.VK_INSERT:
                allowedGesture = (keyModifiers == InputEvent.CTRL_MASK ||
                                  keyModifiers == InputEvent.SHIFT_MASK);
                break;
            case KeyEvent.VK_COPY:
            case KeyEvent.VK_PASTE:
            case KeyEvent.VK_CUT:
                allowedGesture = true;
                break;
            case KeyEvent.VK_DELETE:
                allowedGesture = ( keyModifiers == InputEvent.SHIFT_MASK);
                break;
            }
        }
        return allowedGesture;
    }

    /**
     * Returns true if e has the permissions to
     * access the system clipboard and if it is allowed gesture (if
     * checkGesture is true)
     *
     * @param e AWTEvent to check
     * @param checkGesture boolean
     */
    private static boolean canEventAccessSystemClipboard(AWTEvent e,
                                                        boolean checkGesture) {
        if (EventQueue.isDispatchThread()) {
            /*
             * Checking event permissions makes sense only for event
             * dispathing thread
             */
            if (e instanceof InputEvent
                && (! checkGesture || isAccessClipboardGesture((InputEvent)e))) {
                return AWTAccessor.getInputEventAccessor().
                        canAccessSystemClipboard((InputEvent) e);
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * Utility method that throws SecurityException if SecurityManager is set
     * and modifiers are not public
     *
     * @param modifiers a set of modifiers
     */
    @SuppressWarnings("removal")
    public static void checkAccess(int modifiers) {
        if (System.getSecurityManager() != null
                && !Modifier.isPublic(modifiers)) {
            throw new SecurityException("Resource is not accessible");
        }
    }

    /**
     * Returns true if EventQueue.getCurrentEvent() has the permissions to
     * access the system clipboard and if it is allowed gesture (if
     * checkGesture true)
     *
     * @param checkGesture boolean
     */
    private static boolean canCurrentEventAccessSystemClipboard(boolean
                                                               checkGesture) {
        AWTEvent event = EventQueue.getCurrentEvent();
        return canEventAccessSystemClipboard(event, checkGesture);
    }

    /**
     * see RFE 5012841 [Per AppContect security permissions] for the
     * details
     *
     */
    @SuppressWarnings("removal")
    private static boolean isTrustedContext() {
        return (System.getSecurityManager() == null)
            || (AppContext.getAppContext().
                get(UntrustedClipboardAccess) == null);
    }

    public static String displayPropertiesToCSS(Font font, Color fg) {
        StringBuilder rule = new StringBuilder("body {");
        if (font != null) {
            rule.append(" font-family: ");
            rule.append(font.getFamily());
            rule.append(" ; ");
            rule.append(" font-size: ");
            rule.append(font.getSize());
            rule.append("pt ;");
            if (font.isBold()) {
                rule.append(" font-weight: 700 ; ");
            }
            if (font.isItalic()) {
                rule.append(" font-style: italic ; ");
            }
        }
        if (fg != null) {
            rule.append(" color: #");
            if (fg.getRed() < 16) {
                rule.append('0');
            }
            rule.append(Integer.toHexString(fg.getRed()));
            if (fg.getGreen() < 16) {
                rule.append('0');
            }
            rule.append(Integer.toHexString(fg.getGreen()));
            if (fg.getBlue() < 16) {
                rule.append('0');
            }
            rule.append(Integer.toHexString(fg.getBlue()));
            rule.append(" ; ");
        }
        rule.append(" }");
        return rule.toString();
    }

    /**
     * Utility method that creates a {@code UIDefaults.LazyValue} that
     * creates an {@code ImageIcon} {@code UIResource} for the
     * specified image file name. The image is loaded using
     * {@code getResourceAsStream}, starting with a call to that method
     * on the base class parameter. If it cannot be found, searching will
     * continue through the base class' inheritance hierarchy, up to and
     * including {@code rootClass}.
     *
     * @param baseClass the first class to use in searching for the resource
     * @param rootClass an ancestor of {@code baseClass} to finish the
     *                  search at
     * @param imageFile the name of the file to be found
     * @return a lazy value that creates the {@code ImageIcon}
     *         {@code UIResource} for the image,
     *         or null if it cannot be found
     */
    public static Object makeIcon(final Class<?> baseClass,
                                  final Class<?> rootClass,
                                  final String imageFile) {
        return makeIcon(baseClass, rootClass, imageFile, true);
    }

    /**
     * Utility method that creates a {@code UIDefaults.LazyValue} that
     * creates an {@code ImageIcon} {@code UIResource} for the
     * specified image file name. The image is loaded using
     * {@code getResourceAsStream}, starting with a call to that method
     * on the base class parameter. If it cannot be found, searching will
     * continue through the base class' inheritance hierarchy, up to and
     * including {@code rootClass}.
     *
     * Finds an image with a given name without privileges enabled.
     *
     * @param baseClass the first class to use in searching for the resource
     * @param rootClass an ancestor of {@code baseClass} to finish the
     *                  search at
     * @param imageFile the name of the file to be found
     * @return a lazy value that creates the {@code ImageIcon}
     *         {@code UIResource} for the image,
     *         or null if it cannot be found
     */
    public static Object makeIcon_Unprivileged(final Class<?> baseClass,
                                  final Class<?> rootClass,
                                  final String imageFile) {
        return makeIcon(baseClass, rootClass, imageFile, false);
    }

    private static Object makeIcon(final Class<?> baseClass,
                                  final Class<?> rootClass,
                                  final String imageFile,
                                  final boolean enablePrivileges) {
        return (UIDefaults.LazyValue) (table) -> {
            @SuppressWarnings("removal")
            byte[] buffer = enablePrivileges ? AccessController.doPrivileged(
                    (PrivilegedAction<byte[]>) ()
                    -> getIconBytes(baseClass, rootClass, imageFile))
                    : getIconBytes(baseClass, rootClass, imageFile);

            if (buffer == null) {
                return null;
            }
            if (buffer.length == 0) {
                System.err.println("warning: " + imageFile
                        + " is zero-length");
                return null;
            }

            return new ImageIconUIResource(buffer);
        };
    }

    private static byte[] getIconBytes(final Class<?> baseClass,
                                  final Class<?> rootClass,
                                  final String imageFile) {
                /* Copy resource into a byte array.  This is
                 * necessary because several browsers consider
                 * Class.getResource a security risk because it
                 * can be used to load additional classes.
                 * Class.getResourceAsStream just returns raw
                 * bytes, which we can convert to an image.
                 */
                            Class<?> srchClass = baseClass;

                            while (srchClass != null) {

            try (InputStream resource =
                    srchClass.getResourceAsStream(imageFile)) {
                if (resource == null) {
                    if (srchClass == rootClass) {
                                    break;
                                }
                                srchClass = srchClass.getSuperclass();
                    continue;
                            }

                            try (BufferedInputStream in = new BufferedInputStream(resource)) {
                                return in.readAllBytes();
                            }
                        } catch (IOException ioe) {
                            System.err.println(ioe.toString());
                        }
        }
                        return null;
                    }

    /* Used to help decide if AA text rendering should be used, so
     * this local display test should be additionally qualified
     * against whether we have XRender support on both ends of the wire,
     * as with that support remote performance may be good enough to turn
     * on by default. An additional complication there is XRender does not
     * appear capable of performing gamma correction needed for LCD text.
     */
    public static boolean isLocalDisplay() {
        boolean isLocal;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        if (ge instanceof SunGraphicsEnvironment) {
            isLocal = ((SunGraphicsEnvironment) ge).isDisplayLocal();
        } else {
            isLocal = true;
        }
        return isLocal;
    }

    /**
     * Returns an integer from the defaults table. If {@code key} does
     * not map to a valid {@code Integer}, or can not be convered from
     * a {@code String} to an integer, the value 0 is returned.
     *
     * @param key  an {@code Object} specifying the int.
     * @return the int
     */
    public static int getUIDefaultsInt(Object key) {
        return getUIDefaultsInt(key, 0);
    }

    /**
     * Returns an integer from the defaults table that is appropriate
     * for the given locale. If {@code key} does not map to a valid
     * {@code Integer}, or can not be convered from a {@code String}
     * to an integer, the value 0 is returned.
     *
     * @param key  an {@code Object} specifying the int. Returned value
     *             is 0 if {@code key} is not available,
     * @param l the {@code Locale} for which the int is desired
     * @return the int
     */
    public static int getUIDefaultsInt(Object key, Locale l) {
        return getUIDefaultsInt(key, l, 0);
    }

    /**
     * Returns an integer from the defaults table. If {@code key} does
     * not map to a valid {@code Integer}, or can not be convered from
     * a {@code String} to an integer, {@code default} is
     * returned.
     *
     * @param key  an {@code Object} specifying the int. Returned value
     *             is 0 if {@code key} is not available,
     * @param defaultValue Returned value if {@code key} is not available,
     *                     or is not an Integer
     * @return the int
     */
    public static int getUIDefaultsInt(Object key, int defaultValue) {
        return getUIDefaultsInt(key, null, defaultValue);
    }

    /**
     * Returns an integer from the defaults table that is appropriate
     * for the given locale. If {@code key} does not map to a valid
     * {@code Integer}, or can not be convered from a {@code String}
     * to an integer, {@code default} is returned.
     *
     * @param key  an {@code Object} specifying the int. Returned value
     *             is 0 if {@code key} is not available,
     * @param l the {@code Locale} for which the int is desired
     * @param defaultValue Returned value if {@code key} is not available,
     *                     or is not an Integer
     * @return the int
     */
    public static int getUIDefaultsInt(Object key, Locale l, int defaultValue) {
        Object value = UIManager.get(key, l);

        if (value instanceof Integer) {
            return ((Integer)value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String)value);
            } catch (NumberFormatException nfe) {}
        }
        return defaultValue;
    }

    // At this point we need this method here. But we assume that there
    // will be a common method for this purpose in the future releases.
    public static Component compositeRequestFocus(Component component) {
        if (component instanceof Container) {
            Container container = (Container)component;
            if (container.isFocusCycleRoot()) {
                FocusTraversalPolicy policy = container.getFocusTraversalPolicy();
                Component comp = policy.getDefaultComponent(container);
                if (comp!=null) {
                    comp.requestFocus();
                    return comp;
                }
            }
            Container rootAncestor = container.getFocusCycleRootAncestor();
            if (rootAncestor!=null) {
                FocusTraversalPolicy policy = rootAncestor.getFocusTraversalPolicy();
                Component comp = policy.getComponentAfter(rootAncestor, container);

                if (comp!=null && SwingUtilities.isDescendingFrom(comp, container)) {
                    comp.requestFocus();
                    return comp;
                }
            }
        }
        if (component.isFocusable()) {
            component.requestFocus();
            return component;
        }
        return null;
    }

    /**
     * Change focus to the visible component in {@code JTabbedPane}.
     * This is not a general-purpose method and is here only to permit
     * sharing code.
     */
    @SuppressWarnings("deprecation")
    public static boolean tabbedPaneChangeFocusTo(Component comp) {
        if (comp != null) {
            if (comp.isFocusTraversable()) {
                SwingUtilities2.compositeRequestFocus(comp);
                return true;
            } else if (comp instanceof JComponent
                       && ((JComponent)comp).requestDefaultFocus()) {

                 return true;
            }
        }

        return false;
    }

    /**
     * Submits a value-returning task for execution on the EDT and
     * returns a Future representing the pending results of the task.
     *
     * @param task the task to submit
     * @return a Future representing pending completion of the task
     * @throws NullPointerException if the task is null
     */
    public static <V> Future<V> submit(Callable<V> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        FutureTask<V> future = new FutureTask<V>(task);
        execute(future);
        return future;
    }

    /**
     * Submits a Runnable task for execution on the EDT and returns a
     * Future representing that task.
     *
     * @param task the task to submit
     * @param result the result to return upon successful completion
     * @return a Future representing pending completion of the task,
     *         and whose {@code get()} method will return the given
     *         result value upon completion
     * @throws NullPointerException if the task is null
     */
    public static <V> Future<V> submit(Runnable task, V result) {
        if (task == null) {
            throw new NullPointerException();
        }
        FutureTask<V> future = new FutureTask<V>(task, result);
        execute(future);
        return future;
    }

    /**
     * Sends a Runnable to the EDT for the execution.
     */
    private static void execute(Runnable command) {
        SwingUtilities.invokeLater(command);
    }

    /**
     * Sets the {@code SKIP_CLICK_COUNT} client property on the component
     * if it is an instance of {@code JTextComponent} with a
     * {@code DefaultCaret}. This property, used for text components acting
     * as editors in a table or tree, tells {@code DefaultCaret} how many
     * clicks to skip before starting selection.
     */
    public static void setSkipClickCount(Component comp, int count) {
        if (comp instanceof JTextComponent
                && ((JTextComponent) comp).getCaret() instanceof DefaultCaret) {

            ((JTextComponent) comp).putClientProperty(SKIP_CLICK_COUNT, count);
        }
    }

    /**
     * Return the MouseEvent's click count, possibly reduced by the value of
     * the component's {@code SKIP_CLICK_COUNT} client property. Clears
     * the {@code SKIP_CLICK_COUNT} property if the mouse event's click count
     * is 1. In order for clearing of the property to work correctly, there
     * must be a mousePressed implementation on the caller with this
     * call as the first line.
     */
    public static int getAdjustedClickCount(JTextComponent comp, MouseEvent e) {
        int cc = e.getClickCount();

        if (cc == 1) {
            comp.putClientProperty(SKIP_CLICK_COUNT, null);
        } else {
            Integer sub = (Integer) comp.getClientProperty(SKIP_CLICK_COUNT);
            if (sub != null) {
                return cc - sub;
            }
        }

        return cc;
    }

    /**
     * Used by the {@code liesIn} method to return which section
     * the point lies in.
     *
     * @see #liesIn
     */
    public enum Section {

        /** The leading section */
        LEADING,

        /** The middle section */
        MIDDLE,

        /** The trailing section */
        TRAILING
    }

    /**
     * This method divides a rectangle into two or three sections along
     * the specified axis and determines which section the given point
     * lies in on that axis; used by drag and drop when calculating drop
     * locations.
     * <p>
     * For two sections, the rectangle is divided equally and the method
     * returns whether the point lies in {@code Section.LEADING} or
     * {@code Section.TRAILING}. For horizontal divisions, the calculation
     * respects component orientation.
     * <p>
     * For three sections, if the rectangle is greater than or equal to
     * 30 pixels in length along the axis, the calculation gives 10 pixels
     * to each of the leading and trailing sections and the remainder to the
     * middle. For smaller sizes, the rectangle is divided equally into three
     * sections.
     * <p>
     * Note: This method assumes that the point is within the bounds of
     * the given rectangle on the specified axis. However, in cases where
     * it isn't, the results still have meaning: {@code Section.MIDDLE}
     * remains the same, {@code Section.LEADING} indicates that the point
     * is in or somewhere before the leading section, and
     * {@code Section.TRAILING} indicates that the point is in or somewhere
     * after the trailing section.
     *
     * @param rect the rectangle
     * @param p the point the check
     * @param horizontal {@code true} to use the horizontal axis,
     *        or {@code false} for the vertical axis
     * @param ltr {@code true} for left to right orientation,
     *        or {@code false} for right to left orientation;
     *        only used for horizontal calculations
     * @param three {@code true} for three sections,
     *        or {@code false} for two
     *
     * @return the {@code Section} where the point lies
     *
     * @throws NullPointerException if {@code rect} or {@code p} are
     *         {@code null}
     */
    private static Section liesIn(Rectangle rect, Point p, boolean horizontal,
                                  boolean ltr, boolean three) {

        /* beginning of the rectangle on the axis */
        int p0;

        /* point on the axis we're interested in */
        int pComp;

        /* length of the rectangle on the axis */
        int length;

        /* value of ltr if horizontal, else true */
        boolean forward;

        if (horizontal) {
            p0 = rect.x;
            pComp = p.x;
            length = rect.width;
            forward = ltr;
        } else {
            p0 = rect.y;
            pComp = p.y;
            length = rect.height;
            forward = true;
        }

        if (three) {
            int boundary = (length >= 30) ? 10 : length / 3;

            if (pComp < p0 + boundary) {
               return forward ? Section.LEADING : Section.TRAILING;
           } else if (pComp >= p0 + length - boundary) {
               return forward ? Section.TRAILING : Section.LEADING;
           }

           return Section.MIDDLE;
        } else {
            int middle = p0 + length / 2;
            if (forward) {
                return pComp >= middle ? Section.TRAILING : Section.LEADING;
            } else {
                return pComp < middle ? Section.TRAILING : Section.LEADING;
            }
        }
    }

    /**
     * This method divides a rectangle into two or three sections along
     * the horizontal axis and determines which section the given point
     * lies in; used by drag and drop when calculating drop locations.
     * <p>
     * See the documentation for {@link #liesIn} for more information
     * on how the section is calculated.
     *
     * @param rect the rectangle
     * @param p the point the check
     * @param ltr {@code true} for left to right orientation,
     *        or {@code false} for right to left orientation
     * @param three {@code true} for three sections,
     *        or {@code false} for two
     *
     * @return the {@code Section} where the point lies
     *
     * @throws NullPointerException if {@code rect} or {@code p} are
     *         {@code null}
     */
    public static Section liesInHorizontal(Rectangle rect, Point p,
                                           boolean ltr, boolean three) {
        return liesIn(rect, p, true, ltr, three);
    }

    /**
     * This method divides a rectangle into two or three sections along
     * the vertical axis and determines which section the given point
     * lies in; used by drag and drop when calculating drop locations.
     * <p>
     * See the documentation for {@link #liesIn} for more information
     * on how the section is calculated.
     *
     * @param rect the rectangle
     * @param p the point the check
     * @param three {@code true} for three sections,
     *        or {@code false} for two
     *
     * @return the {@code Section} where the point lies
     *
     * @throws NullPointerException if {@code rect} or {@code p} are
     *         {@code null}
     */
    public static Section liesInVertical(Rectangle rect, Point p,
                                         boolean three) {
        return liesIn(rect, p, false, false, three);
    }

    /**
     * Maps the index of the column in the view at
     * {@code viewColumnIndex} to the index of the column
     * in the table model.  Returns the index of the corresponding
     * column in the model.  If {@code viewColumnIndex}
     * is less than zero, returns {@code viewColumnIndex}.
     *
     * @param cm the table model
     * @param   viewColumnIndex     the index of the column in the view
     * @return  the index of the corresponding column in the model
     *
     * @see JTable#convertColumnIndexToModel(int)
     * @see javax.swing.plaf.basic.BasicTableHeaderUI
     */
    public static int convertColumnIndexToModel(TableColumnModel cm,
                                                int viewColumnIndex) {
        if (viewColumnIndex < 0) {
            return viewColumnIndex;
        }
        return cm.getColumn(viewColumnIndex).getModelIndex();
    }

    /**
     * Maps the index of the column in the {@code cm} at
     * {@code modelColumnIndex} to the index of the column
     * in the view.  Returns the index of the
     * corresponding column in the view; returns {@code -1} if this column
     * is not being displayed. If {@code modelColumnIndex} is less than zero,
     * returns {@code modelColumnIndex}.
     *
     * @param cm the table model
     * @param modelColumnIndex the index of the column in the model
     * @return the index of the corresponding column in the view
     *
     * @see JTable#convertColumnIndexToView(int)
     * @see javax.swing.plaf.basic.BasicTableHeaderUI
     */
    public static int convertColumnIndexToView(TableColumnModel cm,
                                        int modelColumnIndex) {
        if (modelColumnIndex < 0) {
            return modelColumnIndex;
        }
        for (int column = 0; column < cm.getColumnCount(); column++) {
            if (cm.getColumn(column).getModelIndex() == modelColumnIndex) {
                return column;
            }
        }
        return -1;
    }

    /**
     * Sets the InputEvent.ALT_GRAPH mask on any modifier passed to the function
     * @param modifier the modifier passed
     * @return the modifier retiurned with ALT_GRAPH flag set
     */
    public static int setAltGraphMask(int modifier) {
        return (modifier | InputEvent.ALT_GRAPH_DOWN_MASK);
    }

    @SuppressWarnings("deprecation")
    public static int getSystemMnemonicKeyMask() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        if (toolkit instanceof SunToolkit) {
            return ((SunToolkit) toolkit).getFocusAcceleratorKeyMask();
        }
        return InputEvent.ALT_MASK;
    }

    /**
     * Returns the {@link TreePath} that identifies the changed nodes.
     *
     * @param event  changes in a tree model
     * @param model  corresponing tree model
     * @return  the path to the changed nodes
     */
    public static TreePath getTreePath(TreeModelEvent event, TreeModel model) {
        TreePath path = event.getTreePath();
        if ((path == null) && (model != null)) {
            Object root = model.getRoot();
            if (root != null) {
                path = new TreePath(root);
            }
        }
        return path;
    }

    public static boolean isScaledGraphics(Graphics g) {
        if (g instanceof Graphics2D) {
            AffineTransform tx = ((Graphics2D) g).getTransform();
            return (tx.getType() & ~(TYPE_TRANSLATION | TYPE_FLIP)) != 0;
        }
        return false;
    }

    /**
     * Enables the antialiasing rendering hint for the scaled graphics and
     * returns the previous hint value.
     * The returned null value indicates that the passed graphics is not
     * instance of Graphics2D.
     *
     * @param g the graphics
     * @return the previous antialiasing rendering hint value if the passed
     * graphics is instance of Graphics2D, null otherwise.
     */
    public static Object getAndSetAntialisingHintForScaledGraphics(Graphics g) {
        if (isScaledGraphics(g) && isLocalDisplay()) {
            Graphics2D g2d = (Graphics2D) g;
            Object hint = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            return hint;
        }
        return null;
    }

    /**
     * Sets the antialiasing rendering hint if its value is not null.
     * Null hint value indicates that the passed graphics is not instance of
     * Graphics2D.
     *
     * @param g the graphics
     * @param hint the antialiasing rendering hint
     */
    public static void setAntialiasingHintForScaledGraphics(Graphics g, Object hint) {
        if (hint != null) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, hint);
        }
    }

    public static boolean isFloatingPointScale(AffineTransform tx) {
        int type = tx.getType() & ~(TYPE_FLIP | TYPE_TRANSLATION);
        if (type == 0) {
            return false;
        } else if ((type & ~TYPE_MASK_SCALE) == 0) {
            double scaleX = tx.getScaleX();
            double scaleY = tx.getScaleY();
            return (scaleX != (int) scaleX) || (scaleY != (int) scaleY);
        } else {
            return false;
        }
    }

    /**
     * Returns the client property for the given key if it is set; otherwise
     * returns the {@L&F} property.
     *
     * @param component the component
     * @param key an {@code String} specifying the key for the desired boolean value
     * @return the boolean value of the client property if it is set or the {@L&F}
     *         property in other case.
     */
    public static boolean getBoolean(JComponent component, String key) {
        Object clientProperty = component.getClientProperty(key);

        if (clientProperty instanceof Boolean) {
            return Boolean.TRUE.equals(clientProperty);
        }

        return UIManager.getBoolean(key);
    }

    /**
     * Used to listen to "blit" repaints in RepaintManager.
     */
    public interface RepaintListener {
        void repaintPerformed(JComponent c, int x, int y, int w, int h);
    }

    /**
     * Returns whether or not the scale used by {@code GraphicsConfiguration}
     * was changed.
     *
     * @param  ev a {@code PropertyChangeEvent}
     * @return whether or not the scale was changed
     * @since 11
     */
    public static boolean isScaleChanged(final PropertyChangeEvent ev) {
        return isScaleChanged(ev.getPropertyName(), ev.getOldValue(),
                              ev.getNewValue());
    }

    /**
     * Returns whether or not the scale used by {@code GraphicsConfiguration}
     * was changed.
     *
     * @param  name the name of the property
     * @param  oldValue the old value of the property
     * @param  newValue the new value of the property
     * @return whether or not the scale was changed
     * @since 11
     */
    public static boolean isScaleChanged(final String name,
                                         final Object oldValue,
                                         final Object newValue) {
        if (oldValue == newValue || !"graphicsConfiguration".equals(name)) {
            return false;
        }
        var newGC = (GraphicsConfiguration) oldValue;
        var oldGC = (GraphicsConfiguration) newValue;
        var newTx = newGC != null ? newGC.getDefaultTransform() : null;
        var oldTx = oldGC != null ? oldGC.getDefaultTransform() : null;
        return !Objects.equals(newTx, oldTx);
    }
}
