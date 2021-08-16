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
package javax.swing.plaf.basic;

import java.beans.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.*;
import javax.swing.plaf.*;

/**
 * Provides the look and feel for a plain text editor.  In this
 * implementation the default UI is extended to act as a simple
 * view factory.
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
public class BasicTextAreaUI extends BasicTextUI {

    /**
     * Creates a UI for a JTextArea.
     *
     * @param ta a text area
     * @return the UI
     */
    public static ComponentUI createUI(JComponent ta) {
        return new BasicTextAreaUI();
    }

    /**
     * Constructs a new BasicTextAreaUI object.
     */
    public BasicTextAreaUI() {
        super();
    }

    /**
     * Fetches the name used as a key to look up properties through the
     * UIManager.  This is used as a prefix to all the standard
     * text properties.
     *
     * @return the name ("TextArea")
     */
    protected String getPropertyPrefix() {
        return "TextArea";
    }

    protected void installDefaults() {
        super.installDefaults();
        //the fix for 4785160 is undone
    }

    /**
     * This method gets called when a bound property is changed
     * on the associated JTextComponent.  This is a hook
     * which UI implementations may change to reflect how the
     * UI displays bound properties of JTextComponent subclasses.
     * This is implemented to rebuild the View when the
     * <em>WrapLine</em> or the <em>WrapStyleWord</em> property changes.
     *
     * @param evt the property change event
     */
    protected void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);
        if (evt.getPropertyName().equals("lineWrap") ||
            evt.getPropertyName().equals("wrapStyleWord") ||
                evt.getPropertyName().equals("tabSize")) {
            // rebuild the view
            modelChanged();
        } else if ("editable".equals(evt.getPropertyName())) {
            updateFocusTraversalKeys();
        }
    }


    /**
     * The method is overridden to take into account caret width.
     *
     * @param c the editor component
     * @return the preferred size
     * @throws IllegalArgumentException if invalid value is passed
     *
     * @since 1.5
     */
    public Dimension getPreferredSize(JComponent c) {
        return super.getPreferredSize(c);
        //the fix for 4785160 is undone
    }

    /**
     * The method is overridden to take into account caret width.
     *
     * @param c the editor component
     * @return the minimum size
     * @throws IllegalArgumentException if invalid value is passed
     *
     * @since 1.5
     */
    public Dimension getMinimumSize(JComponent c) {
        return super.getMinimumSize(c);
        //the fix for 4785160 is undone
    }

    /**
     * Creates the view for an element.  Returns a WrappedPlainView or
     * PlainView.
     *
     * @param elem the element
     * @return the view
     */
    public View create(Element elem) {
        Document doc = elem.getDocument();
        Object i18nFlag = doc.getProperty("i18n"/*AbstractDocument.I18NProperty*/);
        if ((i18nFlag != null) && i18nFlag.equals(Boolean.TRUE)) {
            // build a view that support bidi
            return createI18N(elem);
        } else {
            JTextComponent c = getComponent();
            if (c instanceof JTextArea) {
                JTextArea area = (JTextArea) c;
                View v;
                if (area.getLineWrap()) {
                    v = new WrappedPlainView(elem, area.getWrapStyleWord());
                } else {
                    v = new PlainView(elem);
                }
                return v;
            }
        }
        return null;
    }

    View createI18N(Element elem) {
        String kind = elem.getName();
        if (kind != null) {
            if (kind.equals(AbstractDocument.ContentElementName)) {
                return new PlainParagraph(elem);
            } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                return new BoxView(elem, View.Y_AXIS);
            }
        }
        return null;
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
        Object i18nFlag = ((JTextComponent)c).getDocument().
                                              getProperty("i18n");
        Insets insets = c.getInsets();
        if (Boolean.TRUE.equals(i18nFlag)) {
            View rootView = getRootView((JTextComponent)c);
            if (rootView.getViewCount() > 0) {
                height = height - insets.top - insets.bottom;
                int baseline = insets.top;
                int fieldBaseline = BasicHTML.getBaseline(
                        rootView.getView(0), width - insets.left -
                        insets.right, height);
                if (fieldBaseline < 0) {
                    return -1;
                }
                return baseline + fieldBaseline;
            }
            return -1;
        }
        FontMetrics fm = c.getFontMetrics(c.getFont());
        return insets.top + fm.getAscent();
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
        return Component.BaselineResizeBehavior.CONSTANT_ASCENT;
    }


    /**
     * Paragraph for representing plain-text lines that support
     * bidirectional text.
     */
    static class PlainParagraph extends ParagraphView {

        PlainParagraph(Element elem) {
            super(elem);
            layoutPool = new LogicalView(elem);
            layoutPool.setParent(this);
        }

        public void setParent(View parent) {
            super.setParent(parent);
            if (parent != null) {
                setPropertiesFromAttributes();
            }
        }

        protected void setPropertiesFromAttributes() {
            Component c = getContainer();
            if ((c != null) && (! c.getComponentOrientation().isLeftToRight())) {
                setJustification(StyleConstants.ALIGN_RIGHT);
            } else {
                setJustification(StyleConstants.ALIGN_LEFT);
            }
        }

        /**
         * Fetch the constraining span to flow against for
         * the given child index.
         */
        public int getFlowSpan(int index) {
            Component c = getContainer();
            if (c instanceof JTextArea) {
                JTextArea area = (JTextArea) c;
                if (! area.getLineWrap()) {
                    // no limit if unwrapped
                    return Integer.MAX_VALUE;
                }
            }
            return super.getFlowSpan(index);
        }

        protected SizeRequirements calculateMinorAxisRequirements(int axis,
                                                                  SizeRequirements r) {
            SizeRequirements req = super.calculateMinorAxisRequirements(axis, r);
            Component c = getContainer();
            if (c instanceof JTextArea) {
                JTextArea area = (JTextArea) c;
                if (! area.getLineWrap()) {
                    // min is pref if unwrapped
                    req.minimum = req.preferred;
                } else {
                    req.minimum = 0;
                    req.preferred = getWidth();
                    if (req.preferred == Integer.MAX_VALUE) {
                        // We have been initially set to MAX_VALUE, but we
                        // don't want this as our preferred.
                        req.preferred = 100;
                    }
                }
            }
            return req;
        }

        /**
         * Sets the size of the view.  If the size has changed, layout
         * is redone.  The size is the full size of the view including
         * the inset areas.
         *
         * @param width the width >= 0
         * @param height the height >= 0
         */
        public void setSize(float width, float height) {
            if ((int) width != getWidth()) {
                preferenceChanged(null, true, true);
            }
            super.setSize(width, height);
        }

        /**
         * This class can be used to represent a logical view for
         * a flow.  It keeps the children updated to reflect the state
         * of the model, gives the logical child views access to the
         * view hierarchy, and calculates a preferred span.  It doesn't
         * do any rendering, layout, or model/view translation.
         */
        static class LogicalView extends CompositeView {

            LogicalView(Element elem) {
                super(elem);
            }

            protected int getViewIndexAtPosition(int pos) {
                Element elem = getElement();
                if (elem.getElementCount() > 0) {
                    return elem.getElementIndex(pos);
                }
                return 0;
            }

            protected boolean updateChildren(DocumentEvent.ElementChange ec,
                                             DocumentEvent e, ViewFactory f) {
                return false;
            }

            protected void loadChildren(ViewFactory f) {
                Element elem = getElement();
                if (elem.getElementCount() > 0) {
                    super.loadChildren(f);
                } else {
                    View v = new GlyphView(elem);
                    append(v);
                }
            }

            public float getPreferredSpan(int axis) {
                if( getViewCount() != 1 )
                    throw new Error("One child view is assumed.");

                View v = getView(0);
                return v.getPreferredSpan(axis);
            }

            /**
             * Forward the DocumentEvent to the given child view.  This
             * is implemented to reparent the child to the logical view
             * (the children may have been parented by a row in the flow
             * if they fit without breaking) and then execute the superclass
             * behavior.
             *
             * @param v the child view to forward the event to.
             * @param e the change information from the associated document
             * @param a the current allocation of the view
             * @param f the factory to use to rebuild if the view has children
             * @see #forwardUpdate
             * @since 1.3
             */
            protected void forwardUpdateToView(View v, DocumentEvent e,
                                               Shape a, ViewFactory f) {
                v.setParent(this);
                super.forwardUpdateToView(v, e, a, f);
            }

            // The following methods don't do anything useful, they
            // simply keep the class from being abstract.

            public void paint(Graphics g, Shape allocation) {
            }

            protected boolean isBefore(int x, int y, Rectangle alloc) {
                return false;
            }

            protected boolean isAfter(int x, int y, Rectangle alloc) {
                return false;
            }

            protected View getViewAtPoint(int x, int y, Rectangle alloc) {
                return null;
            }

            protected void childAllocation(int index, Rectangle a) {
            }
        }
    }

}
