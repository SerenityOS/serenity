/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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
import javax.swing.*;
import javax.swing.plaf.basic.*;
import javax.swing.border.*;
import javax.swing.plaf.*;
import java.io.Serializable;
import javax.swing.text.View;


/**
 * RadioButtonUI implementation for MetalRadioButtonUI
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
 * @author Michael C. Albers (Metal modifications)
 * @author Jeff Dinkins (original BasicRadioButtonCode)
 */
@SuppressWarnings("serial") // Same-version serialization only
public class MetalRadioButtonUI extends BasicRadioButtonUI {

    private static final Object METAL_RADIO_BUTTON_UI_KEY = new Object();

    /**
     * The color of the focused radio button.
     */
    protected Color focusColor;

    /**
     * The color of the selected radio button.
     */
    protected Color selectColor;

    /**
     * The color of a disabled text.
     */
    protected Color disabledTextColor;

    private boolean defaults_initialized = false;

    // ********************************
    //        Create PlAF
    // ********************************

    /**
     * Constructs a {@code MetalRadioButtonUI}.
     */
    public MetalRadioButtonUI() {}

    /**
     * Returns an instance of {@code MetalRadioButtonUI}.
     *
     * @param c a component
     * @return an instance of {@code MetalRadioButtonUI}
     */
    public static ComponentUI createUI(JComponent c) {
        AppContext appContext = AppContext.getAppContext();
        MetalRadioButtonUI metalRadioButtonUI =
                (MetalRadioButtonUI) appContext.get(METAL_RADIO_BUTTON_UI_KEY);
        if (metalRadioButtonUI == null) {
            metalRadioButtonUI = new MetalRadioButtonUI();
            appContext.put(METAL_RADIO_BUTTON_UI_KEY, metalRadioButtonUI);
        }
        return metalRadioButtonUI;
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
        LookAndFeel.installProperty(b, "opaque", Boolean.TRUE);
    }

    protected void uninstallDefaults(AbstractButton b) {
        super.uninstallDefaults(b);
        defaults_initialized = false;
    }

    // ********************************
    //         Default Accessors
    // ********************************

    /**
     * Returns the color of the selected {@code JRadioButton}.
     *
     * @return the color of the selected {@code JRadioButton}
     */
    protected Color getSelectColor() {
        return selectColor;
    }

    /**
     * Returns the color of the disabled text.
     *
     * @return the color of the disabled text
     */
    protected Color getDisabledTextColor() {
        return disabledTextColor;
    }

    /**
     * Returns the color of the focused {@code JRadioButton}.
     *
     * @return the color of the focused {@code JRadioButton}
     */
    protected Color getFocusColor() {
        return focusColor;
    }


    // ********************************
    //        Paint Methods
    // ********************************
    public synchronized void paint(Graphics g, JComponent c) {

        AbstractButton b = (AbstractButton) c;
        ButtonModel model = b.getModel();

        Dimension size = c.getSize();

        int w = size.width;
        int h = size.height;

        Font f = c.getFont();
        g.setFont(f);
        FontMetrics fm = SwingUtilities2.getFontMetrics(c, g, f);

        Rectangle viewRect = new Rectangle(size);
        Rectangle iconRect = new Rectangle();
        Rectangle textRect = new Rectangle();

        Insets i = c.getInsets();
        viewRect.x += i.left;
        viewRect.y += i.top;
        viewRect.width -= (i.right + viewRect.x);
        viewRect.height -= (i.bottom + viewRect.y);

        Icon altIcon = b.getIcon();
        Icon selectedIcon = null;
        Icon disabledIcon = null;

        String text = SwingUtilities.layoutCompoundLabel(
            c, fm, b.getText(), altIcon != null ? altIcon : getDefaultIcon(),
            b.getVerticalAlignment(), b.getHorizontalAlignment(),
            b.getVerticalTextPosition(), b.getHorizontalTextPosition(),
            viewRect, iconRect, textRect, b.getIconTextGap());

        // fill background
        if(c.isOpaque()) {
            g.setColor(b.getBackground());
            g.fillRect(0,0, size.width, size.height);
        }


        // Paint the radio button
        if(altIcon != null) {

            if(!model.isEnabled()) {
                if(model.isSelected()) {
                   altIcon = b.getDisabledSelectedIcon();
                } else {
                   altIcon = b.getDisabledIcon();
                }
            } else if(model.isPressed() && model.isArmed()) {
                altIcon = b.getPressedIcon();
                if(altIcon == null) {
                    // Use selected icon
                    altIcon = b.getSelectedIcon();
                }
            } else if(model.isSelected()) {
                if(b.isRolloverEnabled() && model.isRollover()) {
                        altIcon = b.getRolloverSelectedIcon();
                        if (altIcon == null) {
                                altIcon = b.getSelectedIcon();
                        }
                } else {
                        altIcon = b.getSelectedIcon();
                }
            } else if(b.isRolloverEnabled() && model.isRollover()) {
                altIcon = b.getRolloverIcon();
            }

            if(altIcon == null) {
                altIcon = b.getIcon();
            }

            altIcon.paintIcon(c, g, iconRect.x, iconRect.y);

        } else {
            getDefaultIcon().paintIcon(c, g, iconRect.x, iconRect.y);
        }


        // Draw the Text
        if(text != null) {
            View v = (View) c.getClientProperty(BasicHTML.propertyKey);
            if (v != null) {
                v.paint(g, textRect);
            } else {
               int mnemIndex = b.getDisplayedMnemonicIndex();
               if(model.isEnabled()) {
                   // *** paint the text normally
                   g.setColor(b.getForeground());
               } else {
                   // *** paint the text disabled
                   g.setColor(getDisabledTextColor());
               }
               SwingUtilities2.drawStringUnderlineCharAt(c,g,text,
                       mnemIndex, textRect.x, textRect.y + fm.getAscent());
           }
           if(b.hasFocus() && b.isFocusPainted() &&
              textRect.width > 0 && textRect.height > 0 ) {
               paintFocus(g,textRect,size);
           }
        }
    }

    protected void paintFocus(Graphics g, Rectangle t, Dimension d){
        g.setColor(getFocusColor());
        g.drawRect(t.x-1, t.y-1, t.width+1, t.height+1);
    }
}
