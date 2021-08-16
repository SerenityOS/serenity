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

package com.sun.java.swing.plaf.motif;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;
import javax.swing.plaf.TextUI;
import javax.swing.plaf.UIResource;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;

/**
 * Provides the look and feel features that are common across
 * the Motif/CDE text LAF implementations.
 *
 * @author  Timothy Prinzing
 */
public class MotifTextUI {

    /**
     * Creates the object to use for a caret for all of the Motif
     * text components.  The caret is rendered as an I-beam on Motif.
     *
     * @return the caret object
     */
    public static Caret createCaret() {
        return new MotifCaret();
    }

    /**
     * The motif caret is rendered as an I beam.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    public static class MotifCaret extends DefaultCaret implements UIResource {

        /**
         * Called when the component containing the caret gains
         * focus.  This is implemented to repaint the component
         * so the focus rectangle will be re-rendered, as well
         * as providing the superclass behavior.
         *
         * @param e the focus event
         * @see FocusListener#focusGained
         */
        public void focusGained(FocusEvent e) {
            super.focusGained(e);
            getComponent().repaint();
        }

        /**
         * Called when the component containing the caret loses
         * focus.  This is implemented to set the caret to visibility
         * to false.
         *
         * @param e the focus event
         * @see FocusListener#focusLost
         */
        public void focusLost(FocusEvent e) {
            super.focusLost(e);
            getComponent().repaint();
        }

        /**
         * Damages the area surrounding the caret to cause
         * it to be repainted.  If paint() is reimplemented,
         * this method should also be reimplemented.
         *
         * @param r  the current location of the caret, does nothing if null
         * @see #paint
         */
        protected void damage(Rectangle r) {
            if (r != null) {
                x = r.x - IBeamOverhang - 1;
                y = r.y;
                width = r.width + (2 * IBeamOverhang) + 3;
                height = r.height;
                repaint();
            }
        }

        /**
         * Renders the caret as a vertical line.  If this is reimplemented
         * the damage method should also be reimplemented as it assumes the
         * shape of the caret is a vertical line.  Does nothing if isVisible()
         * is false.  The caret color is derived from getCaretColor() if
         * the component has focus, else from getDisabledTextColor().
         *
         * @param g the graphics context
         * @see #damage
         */
        @SuppressWarnings("deprecation")
        public void paint(Graphics g) {
            if(isVisible()) {
                try {
                    JTextComponent c = getComponent();
                    Color fg = c.hasFocus() ? c.getCaretColor() :
                        c.getDisabledTextColor();
                    TextUI mapper = c.getUI();
                    int dot = getDot();
                    Rectangle r = mapper.modelToView(c, dot);
                    int x0 = r.x - IBeamOverhang;
                    int x1 = r.x + IBeamOverhang;
                    int y0 = r.y + 1;
                    int y1 = r.y + r.height - 2;
                    g.setColor(fg);
                    g.drawLine(r.x, y0, r.x, y1);
                    g.drawLine(x0, y0, x1, y0);
                    g.drawLine(x0, y1, x1, y1);
                } catch (BadLocationException e) {
                    // can't render I guess
                    //System.err.println("Can't render caret");
                }
            }
        }

        static final int IBeamOverhang = 2;
    }

    /**
     * Default bindings all keymaps implementing the Motif feel.
     */
    @SuppressWarnings("deprecation")
    static final JTextComponent.KeyBinding[] defaultBindings = {
        new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT,
                                                                    InputEvent.CTRL_MASK),
                                             DefaultEditorKit.copyAction),
        new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT,
                                                                    InputEvent.SHIFT_MASK),
                                             DefaultEditorKit.pasteAction),
        new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,
                                                                    InputEvent.SHIFT_MASK),
                                             DefaultEditorKit.cutAction),
        new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,
                                                                    InputEvent.SHIFT_MASK),
                                             DefaultEditorKit.selectionBackwardAction),
        new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,
                                                                    InputEvent.SHIFT_MASK),
                                             DefaultEditorKit.selectionForwardAction),
    };


}
