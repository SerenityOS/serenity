/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.plaf;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.text.*;

/**
 * Text editor user interface
 *
 * @author  Timothy Prinzing
 */
public abstract class TextUI extends ComponentUI
{
    /**
     * Constructor for subclasses to call.
     */
    protected TextUI() {}

    /**
     * Converts the given location in the model to a place in
     * the view coordinate system.
     *
     * @param t the text component for which this UI is installed
     * @param pos  the local location in the model to translate &gt;= 0
     * @return the coordinates as a {@code Rectangle}
     * @exception BadLocationException  if the given position does not
     *   represent a valid location in the associated document
     *
     * @deprecated replaced by
     *     {@link #modelToView2D(JTextComponent, int, Position.Bias)}
     */
    @Deprecated(since = "9")
    public abstract Rectangle modelToView(JTextComponent t, int pos) throws BadLocationException;

    /**
     * Converts the given location in the model to a place in
     * the view coordinate system.
     *
     * @param t the text component for which this UI is installed
     * @param pos  the local location in the model to translate &gt;= 0
     * @param bias the bias for the position
     * @return the coordinates as a {@code Rectangle}
     * @exception BadLocationException  if the given position does not
     *   represent a valid location in the associated document
     *
     * @deprecated replaced by
     *     {@link #modelToView2D(JTextComponent, int, Position.Bias)}
     */
    @Deprecated(since = "9")
    public abstract Rectangle modelToView(JTextComponent t, int pos, Position.Bias bias) throws BadLocationException;

    /**
     * Converts the given location in the model to a place in
     * the view coordinate system.
     *
     * @implSpec This implementation calls
     * {@link #modelToView(JTextComponent, int, Position.Bias) modelToView(t, pos, bias)}.
     *
     * @param t the text component for which this UI is installed
     * @param pos  the local location in the model to translate {@code >= 0}
     * @param bias the bias for the position
     * @return the coordinates as a {@code Rectangle2D}
     * @exception BadLocationException  if the given position does not
     *            represent a valid location in the associated document
     *
     * @since 9
     */
    public Rectangle2D modelToView2D(JTextComponent t, int pos, Position.Bias bias)
            throws BadLocationException {
        return modelToView(t, pos, bias);
    }

    /**
     * Converts the given place in the view coordinate system
     * to the nearest representative location in the model.
     *
     * @param t the text component for which this UI is installed
     * @param pt  the location in the view to translate.  This
     *   should be in the same coordinate system as the mouse
     *   events.
     * @return the offset from the start of the document &gt;= 0
     *
     * @deprecated replaced by
     *     {@link #viewToModel2D(JTextComponent, Point2D, Position.Bias[])}
     */
    @Deprecated(since = "9")
    public abstract int viewToModel(JTextComponent t, Point pt);

    /**
     * Provides a mapping from the view coordinate space to the logical
     * coordinate space of the model.
     *
     * @param t the text component for which this UI is installed
     * @param pt the location in the view to translate.
     *           This should be in the same coordinate system
     *           as the mouse events.
     * @param biasReturn
     *           filled in by this method to indicate whether
     *           the point given is closer to the previous or the next
     *           character in the model
     *
     * @return the location within the model that best represents the
     *         given point in the view &gt;= 0
     *
     * @deprecated replaced by
     *     {@link #viewToModel2D(JTextComponent, Point2D, Position.Bias[])}
     */
    @Deprecated(since = "9")
    public abstract int viewToModel(JTextComponent t, Point pt,
                                    Position.Bias[] biasReturn);

    /**
     * Provides a mapping from the view coordinate space to the logical
     * coordinate space of the model.
     *
     * @implSpec This implementation calls
     * {@link #viewToModel(JTextComponent, Point, Position.Bias[])
     * viewToModel(t, new Point((int) pt.getX(), (int) pt.getY()),
     *             biasReturn)}.
     *
     * @param t the text component for which this UI is installed
     * @param pt the location in the view to translate.
     * @param biasReturn
     *           filled in by this method to indicate whether
     *           the point given is closer to the previous or the next
     *           character in the model
     *
     * @return the location within the model that best represents the
     *         given point in the view {@code >= 0}
     *
     * @since 9
     */
    public int viewToModel2D(JTextComponent t, Point2D pt,
                             Position.Bias[] biasReturn) {
        return viewToModel(t, new Point((int) pt.getX(), (int) pt.getY()),
                           biasReturn);
    }

    /**
     * Provides a way to determine the next visually represented model
     * location that one might place a caret.  Some views may not be visible,
     * they might not be in the same order found in the model, or they just
     * might not allow access to some of the locations in the model.
     *
     * @param t the text component for which this UI is installed
     * @param pos the position to convert &gt;= 0
     * @param b the bias for the position
     * @param direction the direction from the current position that can
     *  be thought of as the arrow keys typically found on a keyboard.
     *  This may be SwingConstants.WEST, SwingConstants.EAST,
     *  SwingConstants.NORTH, or SwingConstants.SOUTH
     * @param biasRet an array to contain the bias for the returned position
     * @return the location within the model that best represents the next
     *  location visual position
     * @exception BadLocationException for a bad location within a document model
     * @exception IllegalArgumentException for an invalid direction
     */
    public abstract int getNextVisualPositionFrom(JTextComponent t,
                         int pos, Position.Bias b,
                         int direction, Position.Bias[] biasRet)
                         throws BadLocationException;

    /**
     * Causes the portion of the view responsible for the
     * given part of the model to be repainted.
     *
     * @param t the text component for which this UI is installed
     * @param p0 the beginning of the range &gt;= 0
     * @param p1 the end of the range &gt;= p0
     */
    public abstract void damageRange(JTextComponent t, int p0, int p1);

    /**
     * Causes the portion of the view responsible for the
     * given part of the model to be repainted.
     *
     * @param t the text component for which this UI is installed
     * @param p0 the beginning of the range &gt;= 0
     * @param p1 the end of the range &gt;= p0
     * @param firstBias the bias of the first character position, toward the
     *        previous character or the next character
     * @param secondBias the bias of the second character position, toward the
     *        previous character or the next character
     */
    public abstract void damageRange(JTextComponent t, int p0, int p1,
                                     Position.Bias firstBias,
                                     Position.Bias secondBias);

    /**
     * Fetches the binding of services that set a policy
     * for the type of document being edited.  This contains
     * things like the commands available, stream readers and
     * writers, etc.
     *
     * @param t the text component for which this UI is installed
     * @return the editor kit binding
     */
    public abstract EditorKit getEditorKit(JTextComponent t);

    /**
     * Fetches a View with the allocation of the associated
     * text component (i.e. the root of the hierarchy) that
     * can be traversed to determine how the model is being
     * represented spatially.
     *
     * @param t the text component for which this UI is installed
     * @return a {@code View} with the allocation of the associated
     *         text component
     */
    public abstract View getRootView(JTextComponent t);

    /**
     * Returns the string to be used as the tooltip at the passed in location.
     *
     * @param t  the text component for which this UI is installed
     * @param pt a {@code Point} specifying location for which to get a tooltip
     * @return a {@code String} containing the tooltip
     * @see javax.swing.text.JTextComponent#getToolTipText
     * @since 1.4
     *
     * @deprecated replaced by
     *     {@link #getToolTipText2D(JTextComponent, Point2D)}
     */
    @Deprecated(since = "9")
    public String getToolTipText(JTextComponent t, Point pt) {
        return null;
    }
    /**
     * Returns the string to be used as the tooltip at the passed in location.
     *
     * @implSpec This implementation calls
     * {@link #getToolTipText(JTextComponent, Point)
     *     getToolTipText(t, new Point((int) pt.getX(), (int) pt.getY())))}.
     *
     * @param t  the text component for which this UI is installed
     * @param pt a {@code Point} specifying location for which to get a tooltip
     * @return a {@code String} containing the tooltip
     *
     * @see javax.swing.text.JTextComponent#getToolTipText
     *
     * @since 9
     */
    public String getToolTipText2D(JTextComponent t, Point2D pt) {
        return getToolTipText(t, new Point((int) pt.getX(), (int) pt.getY()));
    }
}
