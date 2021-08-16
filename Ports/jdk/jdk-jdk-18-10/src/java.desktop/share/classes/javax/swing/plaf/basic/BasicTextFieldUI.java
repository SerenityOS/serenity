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
package javax.swing.plaf.basic;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.beans.PropertyChangeEvent;
import java.io.Reader;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.plaf.*;
import sun.swing.DefaultLookup;

/**
 * Basis of a look and feel for a JTextField.
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
 * @author  Timothy Prinzing
 */
@SuppressWarnings("serial") // Same-version serialization only
public class BasicTextFieldUI extends BasicTextUI {

    /**
     * Creates a UI for a JTextField.
     *
     * @param c the text field
     * @return the UI
     */
    public static ComponentUI createUI(JComponent c) {
        return new BasicTextFieldUI();
    }

    /**
     * Creates a new BasicTextFieldUI.
     */
    public BasicTextFieldUI() {
        super();
    }

    /**
     * Fetches the name used as a key to lookup properties through the
     * UIManager.  This is used as a prefix to all the standard
     * text properties.
     *
     * @return the name ("TextField")
     */
    protected String getPropertyPrefix() {
        return "TextField";
    }

    /**
     * Creates a view (FieldView) based on an element.
     *
     * @param elem the element
     * @return the view
     */
    public View create(Element elem) {
        Document doc = elem.getDocument();
        Object i18nFlag = doc.getProperty("i18n"/*AbstractDocument.I18NProperty*/);
        if (Boolean.TRUE.equals(i18nFlag)) {
            // To support bidirectional text, we build a more heavyweight
            // representation of the field.
            String kind = elem.getName();
            if (kind != null) {
                if (kind.equals(AbstractDocument.ContentElementName)) {
                    return new GlyphView(elem){
                        @Override
                        public float getMinimumSpan(int axis) {
                            // no wrap
                            return getPreferredSpan(axis);
                        }
                    };
                } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                    return new I18nFieldView(elem);
                }
            }
            // this shouldn't happen, should probably throw in this case.
        }
        return new FieldView(elem);
    }

    /**
     * Returns the baseline.
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @see javax.swing.JComponent#getBaseline(int, int)
     * @since 1.6
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        View rootView = getRootView((JTextComponent)c);
        if (rootView.getViewCount() > 0) {
            Insets insets = c.getInsets();
            height = height - insets.top - insets.bottom;
            if (height > 0) {
                int baseline = insets.top;
                View fieldView = rootView.getView(0);
                int vspan = (int)fieldView.getPreferredSpan(View.Y_AXIS);
                if (height != vspan) {
                    int slop = height - vspan;
                    baseline += slop / 2;
                }
                if (fieldView instanceof I18nFieldView) {
                    int fieldBaseline = BasicHTML.getBaseline(
                            fieldView, width - insets.left - insets.right,
                            height);
                    if (fieldBaseline < 0) {
                        return -1;
                    }
                    baseline += fieldBaseline;
                }
                else {
                    FontMetrics fm = c.getFontMetrics(c.getFont());
                    baseline += fm.getAscent();
                }
                return baseline;
            }
        }
        return -1;
    }

    /**
     * Returns an enum indicating how the baseline of the component
     * changes as the size changes.
     *
     * @throws NullPointerException {@inheritDoc}
     * @see javax.swing.JComponent#getBaseline(int, int)
     * @since 1.6
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(
            JComponent c) {
        super.getBaselineResizeBehavior(c);
        return Component.BaselineResizeBehavior.CENTER_OFFSET;
    }


    /**
     * A field view that support bidirectional text via the
     * support provided by ParagraphView.
     */
    static class I18nFieldView extends ParagraphView {

        I18nFieldView(Element elem) {
            super(elem);
        }

        /**
         * Fetch the constraining span to flow against for
         * the given child index.  There is no limit for
         * a field since it scrolls, so this is implemented to
         * return <code>Integer.MAX_VALUE</code>.
         */
        public int getFlowSpan(int index) {
            return Integer.MAX_VALUE;
        }

        protected void setJustification(int j) {
            // Justification is done in adjustAllocation(), so disable
            // ParagraphView's justification handling by doing nothing here.
        }

        static boolean isLeftToRight( java.awt.Component c ) {
            return c.getComponentOrientation().isLeftToRight();
        }

        /**
         * Adjusts the allocation given to the view
         * to be a suitable allocation for a text field.
         * If the view has been allocated more than the
         * preferred span vertically, the allocation is
         * changed to be centered vertically.  Horizontally
         * the view is adjusted according to the horizontal
         * alignment property set on the associated JTextField
         * (if that is the type of the hosting component).
         *
         * @param a the allocation given to the view, which may need
         *  to be adjusted.
         * @return the allocation that the superclass should use.
         */
        Shape adjustAllocation(Shape a) {
            if (a != null) {
                Rectangle bounds = a.getBounds();
                int vspan = (int) getPreferredSpan(Y_AXIS);
                int hspan = (int) getPreferredSpan(X_AXIS);
                if (bounds.height != vspan) {
                    int slop = bounds.height - vspan;
                    bounds.y += slop / 2;
                    bounds.height -= slop;
                }

                // horizontal adjustments
                Component c = getContainer();
                if (c instanceof JTextField) {
                    JTextField field = (JTextField) c;
                    BoundedRangeModel vis = field.getHorizontalVisibility();
                    int max = Math.max(hspan, bounds.width);
                    int value = vis.getValue();
                    int extent = Math.min(max, bounds.width - 1);
                    if ((value + extent) > max) {
                        value = max - extent;
                    }
                    vis.setRangeProperties(value, extent, vis.getMinimum(),
                                           max, false);
                    if (hspan < bounds.width) {
                        // horizontally align the interior
                        int slop = bounds.width - 1 - hspan;

                        int align = ((JTextField)c).getHorizontalAlignment();
                        if(isLeftToRight(c)) {
                            if(align==LEADING) {
                                align = LEFT;
                            }
                            else if(align==TRAILING) {
                                align = RIGHT;
                            }
                        }
                        else {
                            if(align==LEADING) {
                                align = RIGHT;
                            }
                            else if(align==TRAILING) {
                                align = LEFT;
                            }
                        }

                        switch (align) {
                        case SwingConstants.CENTER:
                            bounds.x += slop / 2;
                            bounds.width -= slop;
                            break;
                        case SwingConstants.RIGHT:
                            bounds.x += slop;
                            bounds.width -= slop;
                            break;
                        }
                    } else {
                        // adjust the allocation to match the bounded range.
                        bounds.width = hspan;
                        bounds.x -= vis.getValue();
                    }
                }
                return bounds;
            }
            return null;
        }

        /**
         * Update the visibility model with the associated JTextField
         * (if there is one) to reflect the current visibility as a
         * result of changes to the document model.  The bounded
         * range properties are updated.  If the view hasn't yet been
         * shown the extent will be zero and we just set it to be full
         * until determined otherwise.
         */
        void updateVisibilityModel() {
            Component c = getContainer();
            if (c instanceof JTextField) {
                JTextField field = (JTextField) c;
                BoundedRangeModel vis = field.getHorizontalVisibility();
                int hspan = (int) getPreferredSpan(X_AXIS);
                int extent = vis.getExtent();
                int maximum = Math.max(hspan, extent);
                extent = (extent == 0) ? maximum : extent;
                int value = maximum - extent;
                int oldValue = vis.getValue();
                if ((oldValue + extent) > maximum) {
                    oldValue = maximum - extent;
                }
                value = Math.max(0, Math.min(value, oldValue));
                vis.setRangeProperties(value, extent, 0, maximum, false);
            }
        }

        // --- View methods -------------------------------------------

        /**
         * Renders using the given rendering surface and area on that surface.
         * The view may need to do layout and create child views to enable
         * itself to render into the given allocation.
         *
         * @param g the rendering surface to use
         * @param a the allocated region to render into
         *
         * @see View#paint
         */
        public void paint(Graphics g, Shape a) {
            Rectangle r = (Rectangle) a;
            g.clipRect(r.x, r.y, r.width, r.height);
            super.paint(g, adjustAllocation(a));
        }

        /**
         * Determines the resizability of the view along the
         * given axis.  A value of 0 or less is not resizable.
         *
         * @param axis View.X_AXIS or View.Y_AXIS
         * @return the weight -> 1 for View.X_AXIS, else 0
         */
        public int getResizeWeight(int axis) {
            if (axis == View.X_AXIS) {
                return 1;
            }
            return 0;
        }

        /**
         * Provides a mapping from the document model coordinate space
         * to the coordinate space of the view mapped to it.
         *
         * @param pos the position to convert >= 0
         * @param a the allocated region to render into
         * @return the bounding box of the given position
         * @exception BadLocationException  if the given position does not
         *   represent a valid location in the associated document
         * @see View#modelToView
         */
        public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
            return super.modelToView(pos, adjustAllocation(a), b);
        }

        /**
         * Provides a mapping from the document model coordinate space
         * to the coordinate space of the view mapped to it.
         *
         * @param p0 the position to convert >= 0
         * @param b0 the bias toward the previous character or the
         *  next character represented by p0, in case the
         *  position is a boundary of two views.
         * @param p1 the position to convert >= 0
         * @param b1 the bias toward the previous character or the
         *  next character represented by p1, in case the
         *  position is a boundary of two views.
         * @param a the allocated region to render into
         * @return the bounding box of the given position is returned
         * @exception BadLocationException  if the given position does
         *   not represent a valid location in the associated document
         * @exception IllegalArgumentException for an invalid bias argument
         * @see View#viewToModel
         */
        public Shape modelToView(int p0, Position.Bias b0,
                                 int p1, Position.Bias b1, Shape a)
            throws BadLocationException
        {
            return super.modelToView(p0, b0, p1, b1, adjustAllocation(a));
        }

        /**
         * Provides a mapping from the view coordinate space to the logical
         * coordinate space of the model.
         *
         * @param fx the X coordinate >= 0.0f
         * @param fy the Y coordinate >= 0.0f
         * @param a the allocated region to render into
         * @return the location within the model that best represents the
         *  given point in the view
         * @see View#viewToModel
         */
        public int viewToModel(float fx, float fy, Shape a, Position.Bias[] bias) {
            return super.viewToModel(fx, fy, adjustAllocation(a), bias);
        }

        /**
         * Gives notification that something was inserted into the document
         * in a location that this view is responsible for.
         *
         * @param changes the change information from the associated document
         * @param a the current allocation of the view
         * @param f the factory to use to rebuild if the view has children
         * @see View#insertUpdate
         */
        public void insertUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
            super.insertUpdate(changes, adjustAllocation(a), f);
            updateVisibilityModel();
        }

        /**
         * Gives notification that something was removed from the document
         * in a location that this view is responsible for.
         *
         * @param changes the change information from the associated document
         * @param a the current allocation of the view
         * @param f the factory to use to rebuild if the view has children
         * @see View#removeUpdate
         */
        public void removeUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
            super.removeUpdate(changes, adjustAllocation(a), f);
            updateVisibilityModel();
        }

    }

}
