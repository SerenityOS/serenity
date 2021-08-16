/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text.html;

import java.awt.*;
import javax.swing.SizeRequirements;
import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

/**
 * Displays the a paragraph, and uses css attributes for its
 * configuration.
 *
 * @author  Timothy Prinzing
 */

public class ParagraphView extends javax.swing.text.ParagraphView {

    /**
     * Constructs a ParagraphView for the given element.
     *
     * @param elem the element that this view is responsible for
     */
    public ParagraphView(Element elem) {
        super(elem);
    }

    /**
     * Establishes the parent view for this view.  This is
     * guaranteed to be called before any other methods if the
     * parent view is functioning properly.
     * <p>
     * This is implemented
     * to forward to the superclass as well as call the
     * {@link #setPropertiesFromAttributes setPropertiesFromAttributes}
     * method to set the paragraph properties from the css
     * attributes.  The call is made at this time to ensure
     * the ability to resolve upward through the parents
     * view attributes.
     *
     * @param parent the new parent, or null if the view is
     *  being removed from a parent it was previously added
     *  to
     */
    public void setParent(View parent) {
        super.setParent(parent);
        if (parent != null) {
            setPropertiesFromAttributes();
        }
    }

    /**
     * Fetches the attributes to use when rendering.  This is
     * implemented to multiplex the attributes specified in the
     * model with a StyleSheet.
     */
    public AttributeSet getAttributes() {
        if (attr == null) {
            StyleSheet sheet = getStyleSheet();
            attr = sheet.getViewAttributes(this);
        }
        return attr;
    }

    /**
     * Sets up the paragraph from css attributes instead of
     * the values found in StyleConstants (i.e. which are used
     * by the superclass).  Since
     */
    protected void setPropertiesFromAttributes() {
        StyleSheet sheet = getStyleSheet();
        attr = sheet.getViewAttributes(this);
        painter = sheet.getBoxPainter(attr);
        if (attr != null) {
            super.setPropertiesFromAttributes();
            setInsets((short) painter.getInset(TOP, this),
                      (short) painter.getInset(LEFT, this),
                      (short) painter.getInset(BOTTOM, this),
                      (short) painter.getInset(RIGHT, this));
            Object o = attr.getAttribute(CSS.Attribute.TEXT_ALIGN);
            if (o != null) {
                // set horizontal alignment
                String ta = o.toString();
                if (ta.equals("left")) {
                    setJustification(StyleConstants.ALIGN_LEFT);
                } else if (ta.equals("center")) {
                    setJustification(StyleConstants.ALIGN_CENTER);
                } else if (ta.equals("right")) {
                    setJustification(StyleConstants.ALIGN_RIGHT);
                } else if (ta.equals("justify")) {
                    setJustification(StyleConstants.ALIGN_JUSTIFIED);
                }
            }
            // Get the width/height
            cssWidth = (CSS.LengthValue)attr.getAttribute(
                                        CSS.Attribute.WIDTH);
            cssHeight = (CSS.LengthValue)attr.getAttribute(
                                         CSS.Attribute.HEIGHT);
        }
    }

    /**
     * Convenient method to get the StyleSheet.
     *
     * @return the StyleSheet
     */
    protected StyleSheet getStyleSheet() {
        HTMLDocument doc = (HTMLDocument) getDocument();
        return doc.getStyleSheet();
    }


    /**
     * Calculate the needs for the paragraph along the minor axis.
     *
     * <p>If size requirements are explicitly specified for the paragraph,
     * use that requirements.  Otherwise, use the requirements of the
     * superclass {@link javax.swing.text.ParagraphView}.</p>
     *
     * <p>If the {@code axis} parameter is neither {@code View.X_AXIS} nor
     * {@code View.Y_AXIS}, {@link IllegalArgumentException} is thrown.  If the
     * {@code r} parameter is {@code null,} a new {@code SizeRequirements}
     * object is created, otherwise the supplied {@code SizeRequirements}
     * object is returned.</p>
     *
     * @param axis  the minor axis
     * @param r     the input {@code SizeRequirements} object
     * @return      the new or adjusted {@code SizeRequirements} object
     * @throws IllegalArgumentException  if the {@code axis} parameter is invalid
     */
    protected SizeRequirements calculateMinorAxisRequirements(
                                                int axis, SizeRequirements r) {
        r = super.calculateMinorAxisRequirements(axis, r);

        if (BlockView.spanSetFromAttributes(axis, r, cssWidth, cssHeight)) {
            // Offset by the margins so that pref/min/max return the
            // right value.
            int margin = (axis == X_AXIS) ? getLeftInset() + getRightInset() :
                                            getTopInset() + getBottomInset();
            r.minimum -= margin;
            r.preferred -= margin;
            r.maximum -= margin;
        }
        return r;
    }


    /**
     * Indicates whether or not this view should be
     * displayed.  If none of the children wish to be
     * displayed and the only visible child is the
     * break that ends the paragraph, the paragraph
     * will not be considered visible.  Otherwise,
     * it will be considered visible and return true.
     *
     * @return true if the paragraph should be displayed
     */
    public boolean isVisible() {

        int n = getLayoutViewCount() - 1;
        for (int i = 0; i < n; i++) {
            View v = getLayoutView(i);
            if (v.isVisible()) {
                return true;
            }
        }
        if (n > 0) {
            View v = getLayoutView(n);
            if ((v.getEndOffset() - v.getStartOffset()) == 1) {
                return false;
            }
        }
        // If it's the last paragraph and not editable, it shouldn't
        // be visible.
        if (getStartOffset() == getDocument().getLength()) {
            boolean editable = false;
            Component c = getContainer();
            if (c instanceof JTextComponent) {
                editable = ((JTextComponent)c).isEditable();
            }
            if (!editable) {
                return false;
            }
        }
        return true;
    }

    /**
     * Renders using the given rendering surface and area on that
     * surface.  This is implemented to delegate to the superclass
     * after stashing the base coordinate for tab calculations.
     *
     * @param g the rendering surface to use
     * @param a the allocated region to render into
     * @see View#paint
     */
    public void paint(Graphics g, Shape a) {
        if (a == null) {
            return;
        }

        Rectangle r;
        if (a instanceof Rectangle) {
            r = (Rectangle) a;
        } else {
            r = a.getBounds();
        }
        painter.paint(g, r.x, r.y, r.width, r.height, this);
        super.paint(g, a);
    }

    /**
     * Determines the preferred span for this view.  Returns
     * 0 if the view is not visible, otherwise it calls the
     * superclass method to get the preferred span.
     * axis.
     *
     * @param axis may be either View.X_AXIS or View.Y_AXIS
     * @return   the span the view would like to be rendered into;
     *           typically the view is told to render into the span
     *           that is returned, although there is no guarantee;
     *           the parent may choose to resize or break the view
     * @see javax.swing.text.ParagraphView#getPreferredSpan
     */
    public float getPreferredSpan(int axis) {
        if (!isVisible()) {
            return 0;
        }
        return super.getPreferredSpan(axis);
    }

    /**
     * Determines the minimum span for this view along an
     * axis.  Returns 0 if the view is not visible, otherwise
     * it calls the superclass method to get the minimum span.
     *
     * @param axis may be either <code>View.X_AXIS</code> or
     *  <code>View.Y_AXIS</code>
     * @return  the minimum span the view can be rendered into
     * @see javax.swing.text.ParagraphView#getMinimumSpan
     */
    public float getMinimumSpan(int axis) {
        if (!isVisible()) {
            return 0;
        }
        return super.getMinimumSpan(axis);
    }

    /**
     * Determines the maximum span for this view along an
     * axis.  Returns 0 if the view is not visible, otherwise
     * it calls the superclass method ot get the maximum span.
     *
     * @param axis may be either <code>View.X_AXIS</code> or
     *  <code>View.Y_AXIS</code>
     * @return  the maximum span the view can be rendered into
     * @see javax.swing.text.ParagraphView#getMaximumSpan
     */
    public float getMaximumSpan(int axis) {
        if (!isVisible()) {
            return 0;
        }
        return super.getMaximumSpan(axis);
    }

    private AttributeSet attr;
    private StyleSheet.BoxPainter painter;
    private CSS.LengthValue cssWidth;
    private CSS.LengthValue cssHeight;
}
