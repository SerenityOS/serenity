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
package javax.swing.border;

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Component;
import java.io.Serializable;
import java.beans.ConstructorProperties;

/**
 * A class which provides an empty, transparent border which
 * takes up space but does no drawing.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @author David Kloba
 */
@SuppressWarnings("serial")
public class EmptyBorder extends AbstractBorder implements Serializable
{
    /**
     * The left inset of the border.
     */
    protected int left;
    /**
     * The right inset of the border.
     */
    protected int right;
    /**
     * The top inset of the border.
     */
    protected int top;
    /**
     * The bottom inset of the border.
     */
    protected int bottom;

    /**
     * Creates an empty border with the specified insets.
     * @param top the top inset of the border
     * @param left the left inset of the border
     * @param bottom the bottom inset of the border
     * @param right the right inset of the border
     */
    public EmptyBorder(int top, int left, int bottom, int right)   {
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }

    /**
     * Creates an empty border with the specified insets.
     * @param borderInsets the insets of the border
     */
    @ConstructorProperties({"borderInsets"})
    public EmptyBorder(Insets borderInsets)   {
        this.top = borderInsets.top;
        this.right = borderInsets.right;
        this.bottom = borderInsets.bottom;
        this.left = borderInsets.left;
    }

    /**
     * Does no drawing by default.
     */
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    }

    /**
     * Reinitialize the insets parameter with this Border's current Insets.
     * @param c the component for which this border insets value applies
     * @param insets the object to be reinitialized
     */
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = left;
        insets.top = top;
        insets.right = right;
        insets.bottom = bottom;
        return insets;
    }

    /**
     * Returns the insets of the border.
     *
     * @return an {@code Insets} object containing the insets from top, left,
     *         bottom and right
     * @since 1.3
     */
    public Insets getBorderInsets() {
        return new Insets(top, left, bottom, right);
    }

    /**
     * Returns whether or not the border is opaque.
     * Returns false by default.
     */
    public boolean isBorderOpaque() { return false; }

}
