/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Graphics;
import java.awt.Point;
import javax.swing.Action;
import javax.swing.event.ChangeListener;

/**
 * A place within a document view that represents where
 * things can be inserted into the document model.  A caret
 * has a position in the document referred to as a dot.
 * The dot is where the caret is currently located in the
 * model.  There is
 * a second position maintained by the caret that represents
 * the other end of a selection called mark.  If there is
 * no selection the dot and mark will be equal.  If a selection
 * exists, the two values will be different.
 * <p>
 * The dot can be placed by either calling
 * <code>setDot</code> or <code>moveDot</code>.  Setting
 * the dot has the effect of removing any selection that may
 * have previously existed.  The dot and mark will be equal.
 * Moving the dot has the effect of creating a selection as
 * the mark is left at whatever position it previously had.
 *
 * @author  Timothy Prinzing
 */
public interface Caret {

    /**
     * Called when the UI is being installed into the
     * interface of a JTextComponent.  This can be used
     * to gain access to the model that is being navigated
     * by the implementation of this interface.
     *
     * @param c the JTextComponent
     */
    public void install(JTextComponent c);

    /**
     * Called when the UI is being removed from the
     * interface of a JTextComponent.  This is used to
     * unregister any listeners that were attached.
     *
     * @param c the JTextComponent
     */
    public void deinstall(JTextComponent c);

    /**
     * Renders the caret. This method is called by UI classes.
     *
     * @param g the graphics context
     */
    public void paint(Graphics g);

    /**
     * Adds a listener to track whenever the caret position
     * has been changed.
     *
     * @param l the change listener
     */
    public void addChangeListener(ChangeListener l);

    /**
     * Removes a listener that was tracking caret position changes.
     *
     * @param l the change listener
     */
    public void removeChangeListener(ChangeListener l);

    /**
     * Determines if the caret is currently visible.
     *
     * @return true if the caret is visible else false
     */
    public boolean isVisible();

    /**
     * Sets the visibility of the caret.
     *
     * @param v  true if the caret should be shown,
     *  and false if the caret should be hidden
     */
    public void setVisible(boolean v);

    /**
     * Determines if the selection is currently visible.
     *
     * @return true if the caret is visible else false
     */
    public boolean isSelectionVisible();

    /**
     * Sets the visibility of the selection
     *
     * @param v  true if the caret should be shown,
     *  and false if the caret should be hidden
     */
    public void setSelectionVisible(boolean v);

    /**
     * Set the current caret visual location.  This can be used when
     * moving between lines that have uneven end positions (such as
     * when caret up or down actions occur).  If text flows
     * left-to-right or right-to-left the x-coordinate will indicate
     * the desired navigation location for vertical movement.  If
     * the text flow is top-to-bottom, the y-coordinate will indicate
     * the desired navigation location for horizontal movement.
     *
     * @param p  the Point to use for the saved position.  This
     *   can be null to indicate there is no visual location.
     */
    public void setMagicCaretPosition(Point p);

    /**
     * Gets the current caret visual location.
     *
     * @return the visual position.
     * @see #setMagicCaretPosition
     */
    public Point getMagicCaretPosition();

    /**
     * Sets the blink rate of the caret.  This determines if
     * and how fast the caret blinks, commonly used as one
     * way to attract attention to the caret.
     *
     * @param rate  the delay in milliseconds &gt;=0.  If this is
     *  zero the caret will not blink.
     */
    public void setBlinkRate(int rate);

    /**
     * Gets the blink rate of the caret.  This determines if
     * and how fast the caret blinks, commonly used as one
     * way to attract attention to the caret.
     *
     * @return the delay in milliseconds &gt;=0.  If this is
     *  zero the caret will not blink.
     */
    public int getBlinkRate();

    /**
     * Fetches the current position of the caret.
     *
     * @return the position &gt;=0
     */
    public int getDot();

    /**
     * Fetches the current position of the mark.  If there
     * is a selection, the mark will not be the same as
     * the dot.
     *
     * @return the position &gt;=0
     */
    public int getMark();

    /**
     * Sets the caret position to some position.  This
     * causes the mark to become the same as the dot,
     * effectively setting the selection range to zero.
     * <p>
     * If the parameter is negative or beyond the length of the document,
     * the caret is placed at the beginning or at the end, respectively.
     *
     * @param dot  the new position to set the caret to
     */
    public void setDot(int dot);

    /**
     * Moves the caret position (dot) to some other position,
     * leaving behind the mark.  This is useful for
     * making selections.
     *
     * @param dot  the new position to move the caret to &gt;=0
     */
    public void moveDot(int dot);

};
