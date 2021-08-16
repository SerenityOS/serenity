/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import sun.swing.SwingUtilities2;
import sun.swing.DefaultLookup;
import sun.swing.UIAction;
import sun.awt.AppContext;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.text.View;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Insets;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.FontMetrics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * A Windows L&amp;F implementation of LabelUI.  This implementation
 * is completely static, i.e. there's only one UIView implementation
 * that's shared by all JLabel objects.
 *
 * @author Hans Muller
 */
public class BasicLabelUI extends LabelUI implements  PropertyChangeListener
{
   /**
    * The default <code>BasicLabelUI</code> instance. This field might
    * not be used. To change the default instance use a subclass which
    * overrides the <code>createUI</code> method, and place that class
    * name in defaults table under the key "LabelUI".
    */
    protected static BasicLabelUI labelUI = new BasicLabelUI();
    private static final Object BASIC_LABEL_UI_KEY = new Object();

    private Rectangle paintIconR = new Rectangle();
    private Rectangle paintTextR = new Rectangle();

    /**
     * Constructs a {@code BasicLabelUI}.
     */
    public BasicLabelUI() {}

    static void loadActionMap(LazyActionMap map) {
        map.put(new Actions(Actions.PRESS));
        map.put(new Actions(Actions.RELEASE));
    }

    /**
     * Forwards the call to SwingUtilities.layoutCompoundLabel().
     * This method is here so that a subclass could do Label specific
     * layout and to shorten the method name a little.
     *
     * @param label an instance of {@code JLabel}
     * @param fontMetrics a font metrics
     * @param text a text
     * @param icon an icon
     * @param viewR a bounding rectangle to lay out label
     * @param iconR a bounding rectangle to lay out icon
     * @param textR a bounding rectangle to lay out text
     * @return a possibly clipped version of the compound labels string
     * @see SwingUtilities#layoutCompoundLabel
     */
    protected String layoutCL(
        JLabel label,
        FontMetrics fontMetrics,
        String text,
        Icon icon,
        Rectangle viewR,
        Rectangle iconR,
        Rectangle textR)
    {
        return SwingUtilities.layoutCompoundLabel(
            (JComponent) label,
            fontMetrics,
            text,
            icon,
            label.getVerticalAlignment(),
            label.getHorizontalAlignment(),
            label.getVerticalTextPosition(),
            label.getHorizontalTextPosition(),
            viewR,
            iconR,
            textR,
            label.getIconTextGap());
    }

    /**
     * Paint clippedText at textX, textY with the labels foreground color.
     *
     * @param l an instance of {@code JLabel}
     * @param g an instance of {@code Graphics}
     * @param s a text
     * @param textX an X coordinate
     * @param textY an Y coordinate
     * @see #paint
     * @see #paintDisabledText
     */
    protected void paintEnabledText(JLabel l, Graphics g, String s, int textX, int textY)
    {
        int mnemIndex = l.getDisplayedMnemonicIndex();
        g.setColor(l.getForeground());
        SwingUtilities2.drawStringUnderlineCharAt(l, g, s, mnemIndex,
                                                     textX, textY);
    }


    /**
     * Paint clippedText at textX, textY with background.lighter() and then
     * shifted down and to the right by one pixel with background.darker().
     *
     * @param l an instance of {@code JLabel}
     * @param g an instance of {@code Graphics}
     * @param s a text
     * @param textX an X coordinate
     * @param textY an Y coordinate
     * @see #paint
     * @see #paintEnabledText
     */
    protected void paintDisabledText(JLabel l, Graphics g, String s, int textX, int textY)
    {
        int accChar = l.getDisplayedMnemonicIndex();
        Color background = l.getBackground();
        g.setColor(background.brighter());
        SwingUtilities2.drawStringUnderlineCharAt(l, g, s, accChar,
                                                   textX + 1, textY + 1);
        g.setColor(background.darker());
        SwingUtilities2.drawStringUnderlineCharAt(l, g, s, accChar,
                                                   textX, textY);
    }

    /**
     * Paints the label text with the foreground color, if the label is opaque
     * then paints the entire background with the background color. The Label
     * text is drawn by {@link #paintEnabledText} or {@link #paintDisabledText}.
     * The locations of the label parts are computed by {@link #layoutCL}.
     *
     * @see #paintEnabledText
     * @see #paintDisabledText
     * @see #layoutCL
     */
    public void paint(Graphics g, JComponent c)
    {
        JLabel label = (JLabel)c;
        String text = label.getText();
        Icon icon = (label.isEnabled()) ? label.getIcon() : label.getDisabledIcon();

        if ((icon == null) && (text == null)) {
            return;
        }

        FontMetrics fm = SwingUtilities2.getFontMetrics(label, g);
        String clippedText = layout(label, fm, c.getWidth(), c.getHeight());

        if (icon != null) {
            icon.paintIcon(c, g, paintIconR.x, paintIconR.y);
        }

        if (text != null) {
            View v = (View) c.getClientProperty(BasicHTML.propertyKey);
            if (v != null) {
                v.paint(g, paintTextR);
            } else {
                int textX = paintTextR.x;
                int textY = paintTextR.y + fm.getAscent();

                if (label.isEnabled()) {
                    paintEnabledText(label, g, clippedText, textX, textY);
                }
                else {
                    paintDisabledText(label, g, clippedText, textX, textY);
                }
            }
        }
    }

    private String layout(JLabel label, FontMetrics fm,
                          int width, int height) {
        Insets insets = label.getInsets(null);
        String text = label.getText();
        Icon icon = (label.isEnabled()) ? label.getIcon() :
                                          label.getDisabledIcon();
        Rectangle paintViewR = new Rectangle();
        paintViewR.x = insets.left;
        paintViewR.y = insets.top;
        paintViewR.width = width - (insets.left + insets.right);
        paintViewR.height = height - (insets.top + insets.bottom);
        paintIconR.x = paintIconR.y = paintIconR.width = paintIconR.height = 0;
        paintTextR.x = paintTextR.y = paintTextR.width = paintTextR.height = 0;
        return layoutCL(label, fm, text, icon, paintViewR, paintIconR,
                        paintTextR);
    }

    public Dimension getPreferredSize(JComponent c)
    {
        JLabel label = (JLabel)c;
        String text = label.getText();
        Icon icon = (label.isEnabled()) ? label.getIcon() :
                                          label.getDisabledIcon();
        Insets insets = label.getInsets(null);
        Font font = label.getFont();

        int dx = insets.left + insets.right;
        int dy = insets.top + insets.bottom;

        if ((icon == null) &&
            ((text == null) ||
             ((text != null) && (font == null)))) {
            return new Dimension(dx, dy);
        }
        else if ((text == null) || ((icon != null) && (font == null))) {
            return new Dimension(icon.getIconWidth() + dx,
                                 icon.getIconHeight() + dy);
        }
        else {
            FontMetrics fm = label.getFontMetrics(font);
            Rectangle iconR = new Rectangle();
            Rectangle textR = new Rectangle();
            Rectangle viewR = new Rectangle();

            iconR.x = iconR.y = iconR.width = iconR.height = 0;
            textR.x = textR.y = textR.width = textR.height = 0;
            viewR.x = dx;
            viewR.y = dy;
            viewR.width = viewR.height = Short.MAX_VALUE;

            layoutCL(label, fm, text, icon, viewR, iconR, textR);
            int x1 = Math.min(iconR.x, textR.x);
            int x2 = Math.max(iconR.x + iconR.width, textR.x + textR.width);
            int y1 = Math.min(iconR.y, textR.y);
            int y2 = Math.max(iconR.y + iconR.height, textR.y + textR.height);
            Dimension rv = new Dimension(x2 - x1, y2 - y1);

            rv.width += dx;
            rv.height += dy;
            return rv;
        }
    }


    /**
     * @return getPreferredSize(c)
     */
    public Dimension getMinimumSize(JComponent c) {
        Dimension d = getPreferredSize(c);
        View v = (View) c.getClientProperty(BasicHTML.propertyKey);
        if (v != null) {
            d.width -= v.getPreferredSpan(View.X_AXIS) - v.getMinimumSpan(View.X_AXIS);
        }
        return d;
    }

    /**
     * @return getPreferredSize(c)
     */
    public Dimension getMaximumSize(JComponent c) {
        Dimension d = getPreferredSize(c);
        View v = (View) c.getClientProperty(BasicHTML.propertyKey);
        if (v != null) {
            d.width += v.getMaximumSpan(View.X_AXIS) - v.getPreferredSpan(View.X_AXIS);
        }
        return d;
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
        JLabel label = (JLabel)c;
        String text = label.getText();
        if (text == null || text.isEmpty() || label.getFont() == null) {
            return -1;
        }
        FontMetrics fm = label.getFontMetrics(label.getFont());
        layout(label, fm, width, height);
        return BasicHTML.getBaseline(label, paintTextR.y, fm.getAscent(),
                                     paintTextR.width, paintTextR.height);
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
        if (c.getClientProperty(BasicHTML.propertyKey) != null) {
            return Component.BaselineResizeBehavior.OTHER;
        }
        switch(((JLabel)c).getVerticalAlignment()) {
        case JLabel.TOP:
            return Component.BaselineResizeBehavior.CONSTANT_ASCENT;
        case JLabel.BOTTOM:
            return Component.BaselineResizeBehavior.CONSTANT_DESCENT;
        case JLabel.CENTER:
            return Component.BaselineResizeBehavior.CENTER_OFFSET;
        }
        return Component.BaselineResizeBehavior.OTHER;
    }


    public void installUI(JComponent c) {
        installDefaults((JLabel)c);
        installComponents((JLabel)c);
        installListeners((JLabel)c);
        installKeyboardActions((JLabel)c);
    }


    public void uninstallUI(JComponent c) {
        uninstallDefaults((JLabel) c);
        uninstallComponents((JLabel) c);
        uninstallListeners((JLabel) c);
        uninstallKeyboardActions((JLabel) c);
    }

    /**
     * Installs default properties.
     *
     * @param c an instance of {@code JLabel}
     */
    protected void installDefaults(JLabel c){
        LookAndFeel.installColorsAndFont(c, "Label.background", "Label.foreground", "Label.font");
        LookAndFeel.installProperty(c, "opaque", Boolean.FALSE);
    }

    /**
     * Registers listeners.
     *
     * @param c an instance of {@code JLabel}
     */
    protected void installListeners(JLabel c){
        c.addPropertyChangeListener(this);
    }

    /**
     * Registers components.
     *
     * @param c an instance of {@code JLabel}
     */
    protected void installComponents(JLabel c){
        BasicHTML.updateRenderer(c, c.getText());
        c.setInheritsPopupMenu(true);
    }

    /**
     * Registers keyboard actions.
     *
     * @param l an instance of {@code JLabel}
     */
    protected void installKeyboardActions(JLabel l) {
        int dka = l.getDisplayedMnemonic();
        Component lf = l.getLabelFor();
        if ((dka != 0) && (lf != null)) {
            LazyActionMap.installLazyActionMap(l, BasicLabelUI.class,
                                               "Label.actionMap");
            InputMap inputMap = SwingUtilities.getUIInputMap
                            (l, JComponent.WHEN_IN_FOCUSED_WINDOW);
            if (inputMap == null) {
                inputMap = new ComponentInputMapUIResource(l);
                SwingUtilities.replaceUIInputMap(l,
                                JComponent.WHEN_IN_FOCUSED_WINDOW, inputMap);
            }
            inputMap.clear();
            inputMap.put(KeyStroke.getKeyStroke(dka, BasicLookAndFeel.getFocusAcceleratorKeyMask(), false), "press");
            inputMap.put(KeyStroke.getKeyStroke(dka,
                    SwingUtilities2.setAltGraphMask (
                            BasicLookAndFeel.getFocusAcceleratorKeyMask()),
                    false), "press");
        }
        else {
            InputMap inputMap = SwingUtilities.getUIInputMap
                            (l, JComponent.WHEN_IN_FOCUSED_WINDOW);
            if (inputMap != null) {
                inputMap.clear();
            }
        }
    }

    /**
     * Uninstalls default properties.
     *
     * @param c an instance of {@code JLabel}
     */
    protected void uninstallDefaults(JLabel c){
    }

    /**
     * Unregisters listeners.
     *
     * @param c an instance of {@code JLabel}
     */
    protected void uninstallListeners(JLabel c){
        c.removePropertyChangeListener(this);
    }

    /**
     * Unregisters components.
     *
     * @param c an instance of {@code JLabel}
     */
    protected void uninstallComponents(JLabel c){
        BasicHTML.updateRenderer(c, "");
    }

    /**
     * Unregisters keyboard actions.
     *
     * @param c an instance of {@code JLabel}
     */
    protected void uninstallKeyboardActions(JLabel c) {
        SwingUtilities.replaceUIInputMap(c, JComponent.WHEN_FOCUSED, null);
        SwingUtilities.replaceUIInputMap(c, JComponent.WHEN_IN_FOCUSED_WINDOW,
                                       null);
        SwingUtilities.replaceUIActionMap(c, null);
    }

    /**
     * Returns an instance of {@code BasicLabelUI}.
     *
     * @param c a component
     * @return an instance of {@code BasicLabelUI}
     */
    @SuppressWarnings("removal")
    public static ComponentUI createUI(JComponent c) {
        if (System.getSecurityManager() != null) {
            AppContext appContext = AppContext.getAppContext();
            BasicLabelUI safeBasicLabelUI =
                    (BasicLabelUI) appContext.get(BASIC_LABEL_UI_KEY);
            if (safeBasicLabelUI == null) {
                safeBasicLabelUI = new BasicLabelUI();
                appContext.put(BASIC_LABEL_UI_KEY, safeBasicLabelUI);
            }
            return safeBasicLabelUI;
        }
        return labelUI;
    }

    public void propertyChange(PropertyChangeEvent e) {
        String name = e.getPropertyName();
        if (name == "text" || "font" == name || "foreground" == name
                || SwingUtilities2.isScaleChanged(e)) {
            // remove the old html view client property if one
            // existed, and install a new one if the text installed
            // into the JLabel is html source.
            JLabel lbl = ((JLabel) e.getSource());
            String text = lbl.getText();
            BasicHTML.updateRenderer(lbl, text);
        }
        else if (name == "labelFor" || name == "displayedMnemonic") {
            installKeyboardActions((JLabel) e.getSource());
        }
    }

    // When the accelerator is pressed, temporarily make the JLabel
    // focusTraversable by registering a WHEN_FOCUSED action for the
    // release of the accelerator.  Then give it focus so it can
    // prevent unwanted keyTyped events from getting to other components.
    private static class Actions extends UIAction {
        private static final String PRESS = "press";
        private static final String RELEASE = "release";

        Actions(String key) {
            super(key);
        }

        public void actionPerformed(ActionEvent e) {
            JLabel label = (JLabel)e.getSource();
            String key = getName();
            if (key == PRESS) {
                doPress(label);
            }
            else if (key == RELEASE) {
                doRelease(label, e.getActionCommand() != null);
            }
        }

        private void doPress(JLabel label) {
            Component labelFor = label.getLabelFor();
            if (labelFor != null && labelFor.isEnabled()) {
                InputMap inputMap = SwingUtilities.getUIInputMap(label, JComponent.WHEN_FOCUSED);
                if (inputMap == null) {
                    inputMap = new InputMapUIResource();
                    SwingUtilities.replaceUIInputMap(label, JComponent.WHEN_FOCUSED, inputMap);
                }
                int dka = label.getDisplayedMnemonic();
                putOnRelease(inputMap, dka, BasicLookAndFeel
                        .getFocusAcceleratorKeyMask());
                putOnRelease(inputMap, dka, SwingUtilities2.setAltGraphMask (
                        BasicLookAndFeel.getFocusAcceleratorKeyMask()));
                // Need this when the sticky keys are enabled
                putOnRelease(inputMap, dka, 0);
                // Need this if ALT is released before the accelerator
                putOnRelease(inputMap, KeyEvent.VK_ALT, 0);
                label.requestFocus();
            }
        }

        private void doRelease(JLabel label, boolean isCommand) {
            Component labelFor = label.getLabelFor();
            if (labelFor != null && labelFor.isEnabled()) {
                if (label.hasFocus()) {
                    InputMap inputMap = SwingUtilities.getUIInputMap(label,
                            JComponent.WHEN_FOCUSED);
                    if (inputMap != null) {
                        // inputMap should never be null.
                        int dka = label.getDisplayedMnemonic();
                        removeOnRelease(inputMap, dka, BasicLookAndFeel
                                .getFocusAcceleratorKeyMask());
                        removeOnRelease(inputMap, dka,
                                SwingUtilities2.setAltGraphMask (
                                BasicLookAndFeel.getFocusAcceleratorKeyMask()));
                        removeOnRelease(inputMap, dka, 0);
                        removeOnRelease(inputMap, KeyEvent.VK_ALT, 0);
                    }
                    inputMap = SwingUtilities.getUIInputMap(label,
                            JComponent.WHEN_IN_FOCUSED_WINDOW);
                    if (inputMap == null) {
                        inputMap = new InputMapUIResource();
                        SwingUtilities.replaceUIInputMap(label,
                                JComponent.WHEN_IN_FOCUSED_WINDOW, inputMap);
                    }
                    int dka = label.getDisplayedMnemonic();
                    if (isCommand) {
                        putOnRelease(inputMap, KeyEvent.VK_ALT, 0);
                    } else {
                        putOnRelease(inputMap, dka, BasicLookAndFeel
                                .getFocusAcceleratorKeyMask());
                        putOnRelease(inputMap, dka,
                                SwingUtilities2.setAltGraphMask (
                                BasicLookAndFeel.getFocusAcceleratorKeyMask()));
                        // Need this when the sticky keys are enabled
                        putOnRelease(inputMap, dka, 0);
                    }
                    if (labelFor instanceof Container &&
                            ((Container) labelFor).isFocusCycleRoot()) {
                        labelFor.requestFocus();
                    } else {
                        SwingUtilities2.compositeRequestFocus(labelFor);
                    }
                } else {
                    InputMap inputMap = SwingUtilities.getUIInputMap(label,
                            JComponent.WHEN_IN_FOCUSED_WINDOW);
                    int dka = label.getDisplayedMnemonic();
                    if (inputMap != null) {
                        if (isCommand) {
                            removeOnRelease(inputMap, dka, BasicLookAndFeel
                                    .getFocusAcceleratorKeyMask());
                            removeOnRelease(inputMap, dka,
                                    SwingUtilities2.setAltGraphMask (
                                    BasicLookAndFeel.getFocusAcceleratorKeyMask()));
                            removeOnRelease(inputMap, dka, 0);
                        } else {
                            removeOnRelease(inputMap, KeyEvent.VK_ALT, 0);
                        }
                    }
                }
            }
        }

        private void putOnRelease(InputMap inputMap, int keyCode, int modifiers) {
            inputMap.put(KeyStroke.getKeyStroke(keyCode, modifiers, true),
                    RELEASE);
        }

        private void removeOnRelease(InputMap inputMap, int keyCode, int modifiers) {
            inputMap.remove(KeyStroke.getKeyStroke(keyCode, modifiers, true));
        }

    }
}
