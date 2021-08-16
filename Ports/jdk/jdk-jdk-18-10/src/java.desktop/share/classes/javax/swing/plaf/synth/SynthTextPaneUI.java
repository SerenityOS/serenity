/*
 * Copyright (c) 2002, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.plaf.synth;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.plaf.*;
import java.beans.PropertyChangeEvent;
import java.awt.*;

/**
 * Provides the look and feel for a styled text editor in the
 * Synth look and feel.
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
 * @author  Shannon Hickey
 * @since 1.7
 */
@SuppressWarnings("serial") // Same-version serialization only
public class SynthTextPaneUI extends SynthEditorPaneUI {

    /**
     *
     * Constructs a {@code SynthTextPaneUI}.
     */
    public SynthTextPaneUI() {}

    /**
     * Creates a UI for the JTextPane.
     *
     * @param c the JTextPane object
     * @return the UI object
     */
    public static ComponentUI createUI(JComponent c) {
        return new SynthTextPaneUI();
    }

    /**
     * Fetches the name used as a key to lookup properties through the
     * UIManager.  This is used as a prefix to all the standard
     * text properties.
     *
     * @return the name ("TextPane")
     */
    @Override
    protected String getPropertyPrefix() {
        return "TextPane";
    }

    /**
     * Installs the UI for a component.  This does the following
     * things.
     * <ol>
     * <li>
     * Sets opaqueness of the associated component according to its style,
     * if the opaque property has not already been set by the client program.
     * <li>
     * Installs the default caret and highlighter into the
     * associated component. These properties are only set if their
     * current value is either {@code null} or an instance of
     * {@link UIResource}.
     * <li>
     * Attaches to the editor and model.  If there is no
     * model, a default one is created.
     * <li>
     * Creates the view factory and the view hierarchy used
     * to represent the model.
     * </ol>
     *
     * @param c the editor component
     * @see javax.swing.plaf.basic.BasicTextUI#installUI
     * @see ComponentUI#installUI
     */
    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        updateForeground(c.getForeground());
        updateFont(c.getFont());
    }

    /**
     * This method gets called when a bound property is changed
     * on the associated JTextComponent.  This is a hook
     * which UI implementations may change to reflect how the
     * UI displays bound properties of JTextComponent subclasses.
     * If the font, foreground or document has changed, the
     * the appropriate property is set in the default style of
     * the document.
     *
     * @param evt the property change event
     */
    @Override
    protected void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);

        String name = evt.getPropertyName();

        if (name.equals("foreground")) {
            updateForeground((Color)evt.getNewValue());
        } else if (name.equals("font")) {
            updateFont((Font)evt.getNewValue());
        } else if (name.equals("document")) {
            JComponent comp = getComponent();
            updateForeground(comp.getForeground());
            updateFont(comp.getFont());
        }
    }

    /**
     * Update the color in the default style of the document.
     *
     * @param color the new color to use or null to remove the color attribute
     *              from the document's style
     */
    private void updateForeground(Color color) {
        StyledDocument doc = (StyledDocument)getComponent().getDocument();
        Style style = doc.getStyle(StyleContext.DEFAULT_STYLE);

        if (style == null) {
            return;
        }

        if (color == null) {
            style.removeAttribute(StyleConstants.Foreground);
        } else {
            StyleConstants.setForeground(style, color);
        }
    }

    /**
     * Update the font in the default style of the document.
     *
     * @param font the new font to use or null to remove the font attribute
     *             from the document's style
     */
    private void updateFont(Font font) {
        StyledDocument doc = (StyledDocument)getComponent().getDocument();
        Style style = doc.getStyle(StyleContext.DEFAULT_STYLE);

        if (style == null) {
            return;
        }

        if (font == null) {
            style.removeAttribute(StyleConstants.FontFamily);
            style.removeAttribute(StyleConstants.FontSize);
            style.removeAttribute(StyleConstants.Bold);
            style.removeAttribute(StyleConstants.Italic);
        } else {
            StyleConstants.setFontFamily(style, font.getName());
            StyleConstants.setFontSize(style, font.getSize());
            StyleConstants.setBold(style, font.isBold());
            StyleConstants.setItalic(style, font.isItalic());
        }
    }

    @Override
    void paintBackground(SynthContext context, Graphics g, JComponent c) {
        context.getPainter().paintTextPaneBackground(context, g, 0, 0,
                                                  c.getWidth(), c.getHeight());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintBorder(SynthContext context, Graphics g, int x,
                            int y, int w, int h) {
        context.getPainter().paintTextPaneBorder(context, g, x, y, w, h);
    }
}
