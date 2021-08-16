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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Shape;

/**
 * An interface for an object that allows one to mark up the background
 * with colored areas.
 *
 * @author  Timothy Prinzing
 */
public interface Highlighter {

    /**
     * Called when the UI is being installed into the
     * interface of a JTextComponent.  This can be used
     * to gain access to the model that is being navigated
     * by the implementation of this interface.
     *
     * @param c the JTextComponent editor
     */
    public void install(JTextComponent c);

    /**
     * Called when the UI is being removed from the
     * interface of a JTextComponent.  This is used to
     * unregister any listeners that were attached.
     *
     * @param c the JTextComponent editor
     */
    public void deinstall(JTextComponent c);

    /**
     * Renders the highlights.
     *
     * @param g the graphics context.
     */
    public void paint(Graphics g);

    /**
     * Adds a highlight to the view.  Returns a tag that can be used
     * to refer to the highlight.
     *
     * @param p0 the beginning of the range &gt;= 0
     * @param p1 the end of the range &gt;= p0
     * @param p the painter to use for the actual highlighting
     * @return an object that refers to the highlight
     * @exception BadLocationException for an invalid range specification
     */
    public Object addHighlight(int p0, int p1, HighlightPainter p) throws BadLocationException;

    /**
     * Removes a highlight from the view.
     *
     * @param tag  which highlight to remove
     */
    public void removeHighlight(Object tag);

    /**
     * Removes all highlights this highlighter is responsible for.
     */
    public void removeAllHighlights();

    /**
     * Changes the given highlight to span a different portion of
     * the document.  This may be more efficient than a remove/add
     * when a selection is expanding/shrinking (such as a sweep
     * with a mouse) by damaging only what changed.
     *
     * @param tag  which highlight to change
     * @param p0 the beginning of the range &gt;= 0
     * @param p1 the end of the range &gt;= p0
     * @exception BadLocationException for an invalid range specification
     */
    public void changeHighlight(Object tag, int p0, int p1) throws BadLocationException;

    /**
     * Fetches the current list of highlights.
     *
     * @return the highlight list
     */
    public Highlight[] getHighlights();

    /**
     * Highlight renderer.
     */
    public interface HighlightPainter {

        /**
         * Renders the highlight.
         *
         * @param g the graphics context
         * @param p0 the starting offset in the model &gt;= 0
         * @param p1 the ending offset in the model &gt;= p0
         * @param bounds the bounding box for the highlight
         * @param c the editor
         */
        public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c);

    }

    /**
     * A highlight.
     */
    public interface Highlight {

        /**
         * Gets the starting model offset for the highlight.
         *
         * @return the starting offset &gt;= 0
         */
        public int getStartOffset();

        /**
         * Gets the ending model offset for the highlight.
         *
         * @return the ending offset &gt;= 0
         */
        public int getEndOffset();

        /**
         * Gets the painter for the highlighter.
         *
         * @return the painter
         */
        public HighlightPainter getPainter();

    }
};
