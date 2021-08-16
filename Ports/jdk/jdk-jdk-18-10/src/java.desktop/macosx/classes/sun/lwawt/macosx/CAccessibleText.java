/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

package sun.lwawt.macosx;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.Callable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleEditableText;
import javax.accessibility.AccessibleText;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;

class CAccessibleText {
    static AccessibleEditableText getAccessibleEditableText(final Accessible a, final Component c) {
        if (a == null) return null;

        return CAccessibility.invokeAndWait(new Callable<AccessibleEditableText>() {
            public AccessibleEditableText call() throws Exception {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return null;
                return ac.getAccessibleEditableText();
            }
        }, c);
    }

    static String getSelectedText(final Accessible a, final Component c) {
        if (a == null) return null;

        return CAccessibility.invokeAndWait(new Callable<String>() {
            public String call() throws Exception {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return null;

                final AccessibleText at = ac.getAccessibleText();
                if (at == null) return null;

                return at.getSelectedText();
            }
        }, c);
    }

    // replace the currently selected text with newText
    static void setSelectedText(final Accessible a, final Component c, final String newText) {
        if (a == null) return;

        CAccessibility.invokeLater(new Runnable() {
            public void run() {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return;

                final AccessibleEditableText aet = ac.getAccessibleEditableText();
                if (aet == null) return;

                final int selectionStart = aet.getSelectionStart();
                final int selectionEnd = aet.getSelectionEnd();
                aet.replaceText(selectionStart, selectionEnd, newText);
            }
        }, c);
    }

    static void setSelectedTextRange(final Accessible a, final Component c, final int startIndex, final int endIndex) {
        if (a == null) return;

        CAccessibility.invokeLater(new Runnable() {
            public void run() {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return;

                final AccessibleEditableText aet = ac.getAccessibleEditableText();
                if (aet == null) return;

                final boolean validRange = (startIndex >= 0) && (endIndex >= startIndex) && (endIndex <= aet.getCharCount());
                if (!validRange) return;

                aet.selectText(startIndex, endIndex);
            }
        }, c);
    }

    static String getTextRange(final AccessibleEditableText aet, final int start, final int stop, final Component c) {
        if (aet == null) return null;

        return CAccessibility.invokeAndWait(new Callable<String>() {
            public String call() throws Exception {
                return aet.getTextRange(start, stop);
            }
        }, c);
    }

    static int getCharacterIndexAtPosition(final Accessible a, final Component c, final int x, final int y) {
        if (a == null) return 0;

        return CAccessibility.invokeAndWait(new Callable<Integer>() {
            public Integer call() throws Exception {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return null;
                final AccessibleText at = ac.getAccessibleText();
                if (at == null) return null;
                // (x, y) passed in as java screen coords - (0, 0) at upper-left corner of screen.
                // Convert to java component-local coords
                final Point componentLocation = ac.getAccessibleComponent().getLocationOnScreen();
                final int localX = x - (int)componentLocation.getX();
                final int localY = y - (int)componentLocation.getY();

                return at.getIndexAtPoint(new Point(localX, localY));
            }
        }, c);
    }

    static int[] getSelectedTextRange(final Accessible a, final Component c) {
        if (a == null) return new int[2];

        return CAccessibility.invokeAndWait(new Callable<int[]>() {
            public int[] call() {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return new int[2];

                final AccessibleText at = ac.getAccessibleText();
                if (at == null) return new int[2];

                final int[] ret = new int[2];
                ret[0] = at.getSelectionStart();
                ret[1] = at.getSelectionEnd();
                return ret;
            }
        }, c);
    }


    static int[] getVisibleCharacterRange(final Accessible a, final Component c) {
        if (a == null) return null;
        return CAccessibility.invokeAndWait(new Callable<int[]>() {
            public int[] call() {
                return getVisibleCharacterRange(a);
            }
        }, c);
    }

    static int getLineNumberForIndex(final Accessible a, final Component c, final int index) {
        if (a == null) return 0;
        return CAccessibility.invokeAndWait(new Callable<Integer>() {
            public Integer call() {
                return Integer.valueOf(getLineNumberForIndex(a, index));
            }
        }, c);
    }

    static int getLineNumberForInsertionPoint(final Accessible a, final Component c) {
        if (a == null) return 0;
        return CAccessibility.invokeAndWait(new Callable<Integer>() {
            public Integer call() {
                return Integer.valueOf(getLineNumberForInsertionPoint(a));
            }
        }, c);
    }

    static int[] getRangeForLine(final Accessible a, final Component c, final int line) {
        if (a == null) return null;
        return CAccessibility.invokeAndWait(new Callable<int[]>() {
            public int[] call() {
                return getRangeForLine(a, line);
            }
        }, c);
    }

    static int[] getRangeForIndex(final Accessible a, final Component c, final int index) {
        if (a == null) return new int[2];

        return CAccessibility.invokeAndWait(new Callable<int[]>() {
            public int[] call() {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return new int[2];

                final AccessibleEditableText aet = ac.getAccessibleEditableText();
                if (aet == null) return new int[2];

                final int charCount = aet.getCharCount();
                if (index >= charCount) return new int[2];

                final String foundWord = aet.getAtIndex(AccessibleText.WORD, index);
                final int foundWordLength = foundWord.length();
                final String wholeString = aet.getTextRange(0, charCount - 1);

                // now we need to find the index of the foundWord in wholeString. It's somewhere pretty close to the passed-in index,
                // but we don't know if it's before or after the passed-in index. So, look behind and ahead of the passed-in index
                int foundWordIndex = -1;
                int offset = 0;
                while ((foundWordIndex == -1) && (offset < foundWordLength)) {
                    if (wholeString.regionMatches(true, index - offset, foundWord, 0, foundWordLength)) {
                        // is the index of foundWord to the left of the passed-in index?
                        foundWordIndex = index - offset;
                    }
                    if (wholeString.regionMatches(true, index + offset, foundWord, 0, foundWordLength)) {
                        // is the index of the foundWord to the right of the passed-in index?
                        foundWordIndex = index + offset;
                    }
                    offset++;
                }

                final int[] ret = new int[2];
                ret[0] = foundWordIndex;
                ret[1] = foundWordIndex + foundWordLength;
                return ret;
            }
        }, c);
    }

    // cmcnote: this method does not currently work for JLabels. JLabels, for some reason unbeknownst to me, do not
    // return a value from getAccessibleText. According to the javadocs, AccessibleJLabels implement AccessibleText,
    // so this doesn't really make sense. Perhaps a sun bug? Investigate. We currently get the text value out of labels
    // via "getAccessibleName". This just returns a String - so we don't know it's position, etc, as we do for
    // AccessibleText.
    static double[] getBoundsForRange(final Accessible a, final Component c, final int location, final int length) {
        final double[] ret = new double[4];
        if (a == null) return ret;

        return CAccessibility.invokeAndWait(new Callable<double[]>() {
            public double[] call() throws Exception {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return ret;

                final AccessibleText at = ac.getAccessibleText();
                if (at == null) {
                    ac.getAccessibleName();
                    ac.getAccessibleEditableText();
                    return ret;
                }

                final Rectangle2D boundsStart = at.getCharacterBounds(location);
                final Rectangle2D boundsEnd = at.getCharacterBounds(location + length - 1);
                if (boundsEnd == null || boundsStart == null) return ret;
                final Rectangle2D boundsUnion = boundsStart.createUnion(boundsEnd);
                if (boundsUnion.isEmpty()) return ret;

                final double localX = boundsUnion.getX();
                final double localY = boundsUnion.getY();

                final Point componentLocation = ac.getAccessibleComponent().getLocationOnScreen();
                if (componentLocation == null) return ret;

                final double screenX = componentLocation.getX() + localX;
                final double screenY = componentLocation.getY() + localY;

                ret[0] = screenX;
                ret[1] = screenY; // in java screen coords (from top-left corner of screen)
                ret[2] = boundsUnion.getWidth();
                ret[3] = boundsUnion.getHeight();
                return ret;
            }
        }, c);
    }

    static String getStringForRange(final Accessible a, final Component c, final int location, final int length) {
        if (a == null) return null;
        return CAccessibility.invokeAndWait(new Callable<String>() {
            public String call() throws Exception {
                final AccessibleContext ac = a.getAccessibleContext();
                if (ac == null) return null;

                final AccessibleEditableText aet = ac.getAccessibleEditableText();
                if (aet == null) return null;

                return aet.getTextRange(location, location + length);
            }
        }, c);
    }

    @SuppressWarnings("deprecation")
    static int[] getVisibleCharacterRange(final Accessible a) {
        final Accessible sa = CAccessible.getSwingAccessible(a);
        if (!(sa instanceof JTextComponent)) return null;

        final JTextComponent jc = (JTextComponent) sa;
        final Rectangle rect = jc.getVisibleRect();
        final Point topLeft = new Point(rect.x, rect.y);
        final Point topRight = new Point(rect.x + rect.width, rect.y);
        final Point bottomLeft = new Point(rect.x, rect.y + rect.height);
        final Point bottomRight = new Point(rect.x + rect.width, rect.y + rect.height);

        int start = Math.min(jc.viewToModel(topLeft), jc.viewToModel(topRight));
        int end = Math.max(jc.viewToModel(bottomLeft), jc.viewToModel(bottomRight));
        if (start < 0) start = 0;
        if (end < 0) end = 0;
        return new int[] { start, end };
    }

    static int getLineNumberForIndex(final Accessible a, int index) {
        final Accessible sa = CAccessible.getSwingAccessible(a);
        if (!(sa instanceof JTextComponent)) return -1;

        final JTextComponent jc = (JTextComponent) sa;
        final Element root = jc.getDocument().getDefaultRootElement();

        // treat -1 special, returns the current caret position
        if (index == -1) index = jc.getCaretPosition();

        // Determine line number (can be -1)
        return root.getElementIndex(index);
    }

    static int getLineNumberForInsertionPoint(final Accessible a) {
        return getLineNumberForIndex(a, -1); // uses special -1 for above
    }

    static int[] getRangeForLine(final Accessible a, final int lineIndex) {
        Accessible sa = CAccessible.getSwingAccessible(a);
        if (!(sa instanceof JTextComponent)) return null;

        final JTextComponent jc = (JTextComponent) sa;
        final Element root = jc.getDocument().getDefaultRootElement();
        final Element line = root.getElement(lineIndex);
        if (line == null) return null;

        return new int[] { line.getStartOffset(), line.getEndOffset() };
    }
}
