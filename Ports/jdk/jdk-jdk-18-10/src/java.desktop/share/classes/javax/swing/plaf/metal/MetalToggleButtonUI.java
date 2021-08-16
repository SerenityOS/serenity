/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.plaf.metal;

import sun.swing.SwingUtilities2;
import sun.awt.AppContext;

import java.awt.*;
import java.awt.event.*;
import java.lang.ref.*;
import java.util.*;
import javax.swing.plaf.basic.BasicToggleButtonUI;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.*;
import javax.swing.*;

import java.io.Serializable;

/**
 * MetalToggleButton implementation
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
 * @author Tom Santos
 */
@SuppressWarnings("serial") // Same-version serialization only
public class MetalToggleButtonUI extends BasicToggleButtonUI {

    private static final Object METAL_TOGGLE_BUTTON_UI_KEY = new Object();

    /**
     * The color of a focused toggle button.
     */
    protected Color focusColor;

    /**
     * The color of a selected button.
     */
    protected Color selectColor;

    /**
     * The color of a disabled text.
     */
    protected Color disabledTextColor;

    private boolean defaults_initialized = false;

    // ********************************
    //        Create PLAF
    // ********************************

    /**
     * Constructs a {@code MetalToggleButtonUI}.
     */
    public MetalToggleButtonUI() {}

    /**
     * Constructs the {@code MetalToogleButtonUI}.
     *
     * @param b a component
     * @return the {@code MetalToogleButtonUI}.
     */
    public static ComponentUI createUI(JComponent b) {
        AppContext appContext = AppContext.getAppContext();
        MetalToggleButtonUI metalToggleButtonUI =
                (MetalToggleButtonUI) appContext.get(METAL_TOGGLE_BUTTON_UI_KEY);
        if (metalToggleButtonUI == null) {
            metalToggleButtonUI = new MetalToggleButtonUI();
            appContext.put(METAL_TOGGLE_BUTTON_UI_KEY, metalToggleButtonUI);
        }
        return metalToggleButtonUI;
    }

    // ********************************
    //        Install Defaults
    // ********************************
    public void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        if(!defaults_initialized) {
            focusColor = UIManager.getColor(getPropertyPrefix() + "focus");
            selectColor = UIManager.getColor(getPropertyPrefix() + "select");
            disabledTextColor = UIManager.getColor(getPropertyPrefix() + "disabledText");
            defaults_initialized = true;
        }
    }

    protected void uninstallDefaults(AbstractButton b) {
        super.uninstallDefaults(b);
        defaults_initialized = false;
    }

    // ********************************
    //         Default Accessors
    // ********************************
    /**
     * Returns the color of a selected button.
     *
     * @return the color of a selected button
     */
    protected Color getSelectColor() {
        return selectColor;
    }

    /**
     * Returns the color of a disabled text.
     *
     * @return the color of a disabled text
     */
    protected Color getDisabledTextColor() {
        return disabledTextColor;
    }

    /**
     * Returns the color of a focused toggle button.
     *
     * @return the color of a focused toggle button
     */
    protected Color getFocusColor() {
        return focusColor;
    }


    // ********************************
    //        Paint Methods
    // ********************************
    /**
     * If necessary paints the background of the component, then invokes
     * <code>paint</code>.
     *
     * @param g Graphics to paint to
     * @param c JComponent painting on
     * @throws NullPointerException if <code>g</code> or <code>c</code> is
     *         null
     * @see javax.swing.plaf.ComponentUI#update
     * @see javax.swing.plaf.ComponentUI#paint
     * @since 1.5
     */
    public void update(Graphics g, JComponent c) {
        AbstractButton button = (AbstractButton)c;
        if ((c.getBackground() instanceof UIResource) &&
                        button.isContentAreaFilled() && c.isEnabled()) {
            ButtonModel model = button.getModel();
            if (!MetalUtils.isToolBarButton(c)) {
                if (!model.isArmed() && !model.isPressed() &&
                        MetalUtils.drawGradient(
                        c, g, "ToggleButton.gradient", 0, 0, c.getWidth(),
                        c.getHeight(), true)) {
                    paint(g, c);
                    return;
                }
            }
            else if ((model.isRollover() || model.isSelected()) &&
                        MetalUtils.drawGradient(c, g, "ToggleButton.gradient",
                        0, 0, c.getWidth(), c.getHeight(), true)) {
                paint(g, c);
                return;
            }
        }
        super.update(g, c);
    }

    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        if ( b.isContentAreaFilled() ) {
            g.setColor(getSelectColor());
            g.fillRect(0, 0, b.getWidth(), b.getHeight());
        }
    }

    protected void paintText(Graphics g, JComponent c, Rectangle textRect, String text) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel model = b.getModel();
        FontMetrics fm = SwingUtilities2.getFontMetrics(b, g);
        int mnemIndex = b.getDisplayedMnemonicIndex();

        /* Draw the Text */
        if(model.isEnabled()) {
            /*** paint the text normally */
            g.setColor(b.getForeground());
        }
        else {
            /*** paint the text disabled ***/
            if (model.isSelected()) {
                g.setColor(c.getBackground());
            } else {
                g.setColor(getDisabledTextColor());
            }
        }
        SwingUtilities2.drawStringUnderlineCharAt(c, g, text, mnemIndex,
                textRect.x, textRect.y + fm.getAscent());
    }

    protected void paintFocus(Graphics g, AbstractButton b,
                              Rectangle viewRect, Rectangle textRect, Rectangle iconRect){

        Rectangle focusRect = new Rectangle();
        String text = b.getText();
        boolean isIcon = b.getIcon() != null;

        // If there is text
        if ( text != null && !text.isEmpty()) {
            if ( !isIcon ) {
                focusRect.setBounds( textRect );
            }
            else {
                focusRect.setBounds( iconRect.union( textRect ) );
            }
        }
        // If there is an icon and no text
        else if ( isIcon ) {
            focusRect.setBounds( iconRect );
        }

        g.setColor(getFocusColor());
        g.drawRect((focusRect.x-1), (focusRect.y-1),
                  focusRect.width+1, focusRect.height+1);

    }

    /**
     * Paints the appropriate icon of the button <code>b</code> in the
     * space <code>iconRect</code>.
     *
     * @param g Graphics to paint to
     * @param b Button to render for
     * @param iconRect space to render in
     * @throws NullPointerException if any of the arguments are null.
     * @since 1.5
     */
    protected void paintIcon(Graphics g, AbstractButton b, Rectangle iconRect) {
        super.paintIcon(g, b, iconRect);
    }
}
