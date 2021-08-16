/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.java.swing.plaf.windows;

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.BoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.TextUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Highlighter;
import javax.swing.text.Position;

/**
 * Provides the Windows look and feel for a text field.  This
 * is basically the following customizations to the default
 * look-and-feel.
 * <ul>
 * <li>The border is beveled (using the standard control color).
 * <li>The background is white by default.
 * <li>The highlight color is a dark color, blue by default.
 * <li>The foreground color is high contrast in the selected
 *  area, white by default.  The unselected foreground is black.
 * <li>The cursor blinks at about 1/2 second intervals.
 * <li>The entire value is selected when focus is gained.
 * <li>Shift-left-arrow and shift-right-arrow extend selection
 * <li>Ctrl-left-arrow and ctrl-right-arrow act like home and
 *   end respectively.
 * </ul>
 *
 * @author  Timothy Prinzing
 */
public class WindowsTextFieldUI extends BasicTextFieldUI
{
    /**
     * Creates a UI for a JTextField.
     *
     * @param c the text field
     * @return the UI
     */
    public static ComponentUI createUI(JComponent c) {
        return new WindowsTextFieldUI();
    }

    /**
     * Paints a background for the view.  This will only be
     * called if isOpaque() on the associated component is
     * true.  The default is to paint the background color
     * of the component.
     *
     * @param g the graphics context
     */
    protected void paintBackground(Graphics g) {
        super.paintBackground(g);
    }

    /**
     * Creates the caret for a field.
     *
     * @return the caret
     */
    protected Caret createCaret() {
        return new WindowsFieldCaret();
    }

    /**
     * WindowsFieldCaret has different scrolling behavior than
     * DefaultCaret.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    static class WindowsFieldCaret extends DefaultCaret implements UIResource {

        public WindowsFieldCaret() {
            super();
        }

        /**
         * Adjusts the visibility of the caret according to
         * the windows feel which seems to be to move the
         * caret out into the field by about a quarter of
         * a field length if not visible.
         */
        protected void adjustVisibility(Rectangle r) {
            SwingUtilities.invokeLater(new SafeScroller(r));
        }

        /**
         * Gets the painter for the Highlighter.
         *
         * @return the painter
         */
        protected Highlighter.HighlightPainter getSelectionPainter() {
            return WindowsTextUI.WindowsPainter;
        }


        private class SafeScroller implements Runnable {
            SafeScroller(Rectangle r) {
                this.r = r;
            }

            @SuppressWarnings("deprecation")
            public void run() {
                JTextField field = (JTextField) getComponent();
                if (field != null) {
                    TextUI ui = field.getUI();
                    int dot = getDot();
                    // PENDING: We need to expose the bias in DefaultCaret.
                    Position.Bias bias = Position.Bias.Forward;
                    Rectangle startRect = null;
                    try {
                        startRect = ui.modelToView(field, dot, bias);
                    } catch (BadLocationException ble) {}

                    Insets i = field.getInsets();
                    BoundedRangeModel vis = field.getHorizontalVisibility();
                    int x = r.x + vis.getValue() - i.left;
                    int quarterSpan = vis.getExtent() / 4;
                    if (r.x < i.left) {
                        vis.setValue(x - quarterSpan);
                    } else if (r.x + r.width > i.left + vis.getExtent()) {
                        vis.setValue(x - (3 * quarterSpan));
                    }
                    // If we scroll, our visual location will have changed,
                    // but we won't have updated our internal location as
                    // the model hasn't changed. This checks for the change,
                    // and if necessary, resets the internal location.
                    if (startRect != null) {
                        try {
                            Rectangle endRect;
                            endRect = ui.modelToView(field, dot, bias);
                            if (endRect != null && !endRect.equals(startRect)){
                                damage(endRect);
                            }
                        } catch (BadLocationException ble) {}
                    }
                }
            }

            private Rectangle r;
        }
    }

}
