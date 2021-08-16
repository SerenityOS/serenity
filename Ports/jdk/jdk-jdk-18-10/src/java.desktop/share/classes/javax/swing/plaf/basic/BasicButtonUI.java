/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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
import sun.awt.AppContext;

import java.awt.*;
import java.awt.event.*;
import java.io.Serializable;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.View;

/**
 * BasicButton implementation
 *
 * @author Jeff Dinkins
 */
public class BasicButtonUI extends ButtonUI{
    // Visual constants
    // NOTE: This is not used or set any where. Were we allowed to remove
    // fields, this would be removed.
    /**
     * The default gap between a text and an icon.
     */
    protected int defaultTextIconGap;

    // Amount to offset text, the value of this comes from
    // defaultTextShiftOffset once setTextShiftOffset has been invoked.
    private int shiftOffset = 0;
    // Value that is set in shiftOffset once setTextShiftOffset has been
    // invoked. The value of this comes from the defaults table.
    /**
     * The default offset of a text.
     */
    protected int defaultTextShiftOffset;

    private static final String propertyPrefix = "Button" + ".";

    private static final Object BASIC_BUTTON_UI_KEY = new Object();

    private KeyListener keyListener = null;

    // ********************************
    //          Create PLAF
    // ********************************
    /**
     * Constructs a {@code BasicButtonUI}.
     */
    public BasicButtonUI() {}

    /**
     * Returns an instance of {@code BasicButtonUI}.
     *
     * @param c a component
     * @return an instance of {@code BasicButtonUI}
     */
    public static ComponentUI createUI(JComponent c) {
        AppContext appContext = AppContext.getAppContext();
        BasicButtonUI buttonUI =
                (BasicButtonUI) appContext.get(BASIC_BUTTON_UI_KEY);
        if (buttonUI == null) {
            buttonUI = new BasicButtonUI();
            appContext.put(BASIC_BUTTON_UI_KEY, buttonUI);
        }
        return buttonUI;
    }

    /**
     * Returns the property prefix.
     *
     * @return the property prefix
     */
    protected String getPropertyPrefix() {
        return propertyPrefix;
    }


    // ********************************
    //          Install PLAF
    // ********************************
    public void installUI(JComponent c) {
        installDefaults((AbstractButton) c);
        installListeners((AbstractButton) c);
        installKeyboardActions((AbstractButton) c);
        BasicHTML.updateRenderer(c, ((AbstractButton) c).getText());
    }

    /**
     * Installs default properties.
     *
     * @param b an abstract button
     */
    protected void installDefaults(AbstractButton b) {
        // load shared instance defaults
        String pp = getPropertyPrefix();

        defaultTextShiftOffset = UIManager.getInt(pp + "textShiftOffset");

        // set the following defaults on the button
        if (b.isContentAreaFilled()) {
            LookAndFeel.installProperty(b, "opaque", Boolean.TRUE);
        } else {
            LookAndFeel.installProperty(b, "opaque", Boolean.FALSE);
        }

        if(b.getMargin() == null || (b.getMargin() instanceof UIResource)) {
            b.setMargin(UIManager.getInsets(pp + "margin"));
        }

        LookAndFeel.installColorsAndFont(b, pp + "background",
                                         pp + "foreground", pp + "font");
        LookAndFeel.installBorder(b, pp + "border");

        Object rollover = UIManager.get(pp + "rollover");
        if (rollover != null) {
            LookAndFeel.installProperty(b, "rolloverEnabled", rollover);
        }

        LookAndFeel.installProperty(b, "iconTextGap", Integer.valueOf(4));
    }

    /**
     * Registers listeners.
     *
     * @param b an abstract button
     */
    protected void installListeners(AbstractButton b) {
        BasicButtonListener listener = createButtonListener(b);
        if(listener != null) {
            b.addMouseListener(listener);
            b.addMouseMotionListener(listener);
            b.addFocusListener(listener);
            b.addPropertyChangeListener(listener);
            b.addChangeListener(listener);
        }

        if (b instanceof JToggleButton) {
            keyListener = createKeyListener();
            b.addKeyListener(keyListener);

            // Need to get traversal key event
            b.setFocusTraversalKeysEnabled(false);

            // Map actions to the arrow keys
            b.getActionMap().put("Previous", new BasicButtonUI.SelectPreviousBtn());
            b.getActionMap().put("Next", new BasicButtonUI.SelectNextBtn());

            b.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
                    put(KeyStroke.getKeyStroke("UP"), "Previous");
            b.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
                    put(KeyStroke.getKeyStroke("DOWN"), "Next");
            b.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
                    put(KeyStroke.getKeyStroke("LEFT"), "Previous");
            b.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
                    put(KeyStroke.getKeyStroke("RIGHT"), "Next");
        }
    }

    /**
     * Registers keyboard actions.
     *
     * @param b an abstract button
     */
    protected void installKeyboardActions(AbstractButton b){
        BasicButtonListener listener = getButtonListener(b);

        if(listener != null) {
            listener.installKeyboardActions(b);
        }
    }


    // ********************************
    //         Uninstall PLAF
    // ********************************
    public void uninstallUI(JComponent c) {
        uninstallKeyboardActions((AbstractButton) c);
        uninstallListeners((AbstractButton) c);
        uninstallDefaults((AbstractButton) c);
        BasicHTML.updateRenderer(c, "");
    }

    /**
     * Unregisters keyboard actions.
     *
     * @param b an abstract button
     */
    protected void uninstallKeyboardActions(AbstractButton b) {
        BasicButtonListener listener = getButtonListener(b);
        if(listener != null) {
            listener.uninstallKeyboardActions(b);
        }
    }

    /**
     * Unregisters listeners.
     *
     * @param b an abstract button
     */
    protected void uninstallListeners(AbstractButton b) {
        BasicButtonListener listener = getButtonListener(b);
        if(listener != null) {
            b.removeMouseListener(listener);
            b.removeMouseMotionListener(listener);
            b.removeFocusListener(listener);
            b.removeChangeListener(listener);
            b.removePropertyChangeListener(listener);
        }
        if (b instanceof JToggleButton) {
            // Unmap actions from the arrow keys
            b.getActionMap().remove("Previous");
            b.getActionMap().remove("Next");
            b.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .remove(KeyStroke.getKeyStroke("UP"));
            b.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .remove(KeyStroke.getKeyStroke("DOWN"));
            b.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .remove(KeyStroke.getKeyStroke("LEFT"));
            b.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .remove(KeyStroke.getKeyStroke("RIGHT"));

            if (keyListener != null) {
                b.removeKeyListener(keyListener);
                keyListener = null;
            }
        }
    }

    /**
     * Uninstalls default properties.
     *
     * @param b an abstract button
     */
    protected void uninstallDefaults(AbstractButton b) {
        LookAndFeel.uninstallBorder(b);
    }

    // ********************************
    //        Create Listeners
    // ********************************
    /**
     * Returns a new instance of {@code BasicButtonListener}.
     *
     * @param b an abstract button
     * @return a new instance of {@code BasicButtonListener}
     */
    protected BasicButtonListener createButtonListener(AbstractButton b) {
        return new BasicButtonListener(b);
    }

    /**
     * Returns the default gap between a text and an icon.
     *
     * @param b an abstract button
     * @return the default gap between text and an icon
     */
    public int getDefaultTextIconGap(AbstractButton b) {
        return defaultTextIconGap;
    }

    /* These rectangles/insets are allocated once for all
     * ButtonUI.paint() calls.  Re-using rectangles rather than
     * allocating them in each paint call substantially reduced the time
     * it took paint to run.  Obviously, this method can't be re-entered.
     */
    private static Rectangle viewRect = new Rectangle();
    private static Rectangle textRect = new Rectangle();
    private static Rectangle iconRect = new Rectangle();

    // ********************************
    //          Paint Methods
    // ********************************

    public void paint(Graphics g, JComponent c)
    {
        AbstractButton b = (AbstractButton) c;
        ButtonModel model = b.getModel();

        String text = layout(b, SwingUtilities2.getFontMetrics(b, g),
               b.getWidth(), b.getHeight());

        clearTextShiftOffset();

        // perform UI specific press action, e.g. Windows L&F shifts text
        if (model.isArmed() && model.isPressed()) {
            paintButtonPressed(g,b);
        }

        // Paint the Icon
        if(b.getIcon() != null) {
            paintIcon(g,c,iconRect);
        }

        if (text != null && !text.isEmpty()){
            View v = (View) c.getClientProperty(BasicHTML.propertyKey);
            if (v != null) {
                v.paint(g, textRect);
            } else {
                paintText(g, b, textRect, text);
            }
        }

        if (b.isFocusPainted() && b.hasFocus()) {
            // paint UI specific focus
            paintFocus(g,b,viewRect,textRect,iconRect);
        }
    }

    /**
     * Paints an icon of the current button.
     *
     * @param g an instance of {@code Graphics}
     * @param c a component
     * @param iconRect a bounding rectangle to render the icon
     */
    protected void paintIcon(Graphics g, JComponent c, Rectangle iconRect){
            AbstractButton b = (AbstractButton) c;
            ButtonModel model = b.getModel();
            Icon icon = b.getIcon();
            Icon tmpIcon = null;

            if(icon == null) {
               return;
            }

            Icon selectedIcon = null;

            /* the fallback icon should be based on the selected state */
            if (model.isSelected()) {
                selectedIcon = b.getSelectedIcon();
                if (selectedIcon != null) {
                    icon = selectedIcon;
                }
            }

            if(!model.isEnabled()) {
                if(model.isSelected()) {
                   tmpIcon = b.getDisabledSelectedIcon();
                   if (tmpIcon == null) {
                       tmpIcon = selectedIcon;
                   }
                }

                if (tmpIcon == null) {
                    tmpIcon = b.getDisabledIcon();
                }
            } else if(model.isPressed() && model.isArmed()) {
                tmpIcon = b.getPressedIcon();
                if(tmpIcon != null) {
                    // revert back to 0 offset
                    clearTextShiftOffset();
                }
            } else if(b.isRolloverEnabled() && model.isRollover()) {
                if(model.isSelected()) {
                   tmpIcon = b.getRolloverSelectedIcon();
                   if (tmpIcon == null) {
                       tmpIcon = selectedIcon;
                   }
                }

                if (tmpIcon == null) {
                    tmpIcon = b.getRolloverIcon();
                }
            }

            if(tmpIcon != null) {
                icon = tmpIcon;
            }

            if(model.isPressed() && model.isArmed()) {
                icon.paintIcon(c, g, iconRect.x + getTextShiftOffset(),
                        iconRect.y + getTextShiftOffset());
            } else {
                icon.paintIcon(c, g, iconRect.x, iconRect.y);
            }

    }

    /**
     * Method which renders the text of the current button.
     *
     * As of Java 2 platform v 1.4 this method should not be used or overriden.
     * Use the paintText method which takes the AbstractButton argument.
     *
     * @param g an instance of {@code Graphics}
     * @param c a component
     * @param textRect a bounding rectangle to render the text
     * @param text a string to render
     */
    protected void paintText(Graphics g, JComponent c, Rectangle textRect, String text) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel model = b.getModel();
        FontMetrics fm = SwingUtilities2.getFontMetrics(c, g);
        int mnemonicIndex = b.getDisplayedMnemonicIndex();

        /* Draw the Text */
        if(model.isEnabled()) {
            /*** paint the text normally */
            g.setColor(b.getForeground());
            SwingUtilities2.drawStringUnderlineCharAt(c, g,text, mnemonicIndex,
                                          textRect.x + getTextShiftOffset(),
                                          textRect.y + fm.getAscent() + getTextShiftOffset());
        }
        else {
            /*** paint the text disabled ***/
            g.setColor(b.getBackground().brighter());
            SwingUtilities2.drawStringUnderlineCharAt(c, g,text, mnemonicIndex,
                                          textRect.x, textRect.y + fm.getAscent());
            g.setColor(b.getBackground().darker());
            SwingUtilities2.drawStringUnderlineCharAt(c, g,text, mnemonicIndex,
                                          textRect.x - 1, textRect.y + fm.getAscent() - 1);
        }
    }

    /**
     * Method which renders the text of the current button.
     *
     * @param g Graphics context
     * @param b Current button to render
     * @param textRect Bounding rectangle to render the text
     * @param text String to render
     * @since 1.4
     */
    protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
        paintText(g, (JComponent)b, textRect, text);
    }

    // Method signature defined here overriden in subclasses.
    // Perhaps this class should be abstract?
    /**
     * Paints a focused button.
     *
     * @param g an instance of {@code Graphics}
     * @param b an abstract button
     * @param viewRect a bounding rectangle to render the button
     * @param textRect a bounding rectangle to render the text
     * @param iconRect a bounding rectangle to render the icon
     */
    protected void paintFocus(Graphics g, AbstractButton b,
                              Rectangle viewRect, Rectangle textRect, Rectangle iconRect){
    }


    /**
     * Paints a pressed button.
     *
     * @param g an instance of {@code Graphics}
     * @param b an abstract button
     */
    protected void paintButtonPressed(Graphics g, AbstractButton b){
    }

    /**
     * Clears the offset of the text.
     */
    protected void clearTextShiftOffset(){
        this.shiftOffset = 0;
    }

    /**
     * Sets the offset of the text.
     */
    protected void setTextShiftOffset(){
        this.shiftOffset = defaultTextShiftOffset;
    }

    /**
     * Returns the offset of the text.
     *
     * @return the offset of the text
     */
    protected int getTextShiftOffset() {
        return shiftOffset;
    }

    // ********************************
    //          Layout Methods
    // ********************************
    public Dimension getMinimumSize(JComponent c) {
        Dimension d = getPreferredSize(c);
        View v = (View) c.getClientProperty(BasicHTML.propertyKey);
        if (v != null) {
            d.width -= v.getPreferredSpan(View.X_AXIS) - v.getMinimumSpan(View.X_AXIS);
        }
        return d;
    }

    public Dimension getPreferredSize(JComponent c) {
        AbstractButton b = (AbstractButton)c;
        return BasicGraphicsUtils.getPreferredButtonSize(b, b.getIconTextGap());
    }

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
        AbstractButton b = (AbstractButton)c;
        String text = b.getText();
        if (text == null || text.isEmpty()) {
            return -1;
        }
        FontMetrics fm = b.getFontMetrics(b.getFont());
        layout(b, fm, width, height);
        return BasicHTML.getBaseline(b, textRect.y, fm.getAscent(),
                                     textRect.width, textRect.height);
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
        switch(((AbstractButton)c).getVerticalAlignment()) {
        case AbstractButton.TOP:
            return Component.BaselineResizeBehavior.CONSTANT_ASCENT;
        case AbstractButton.BOTTOM:
            return Component.BaselineResizeBehavior.CONSTANT_DESCENT;
        case AbstractButton.CENTER:
            return Component.BaselineResizeBehavior.CENTER_OFFSET;
        }
        return Component.BaselineResizeBehavior.OTHER;
    }

    private String layout(AbstractButton b, FontMetrics fm,
                          int width, int height) {
        Insets i = b.getInsets();
        viewRect.x = i.left;
        viewRect.y = i.top;
        viewRect.width = width - (i.right + viewRect.x);
        viewRect.height = height - (i.bottom + viewRect.y);

        textRect.x = textRect.y = textRect.width = textRect.height = 0;
        iconRect.x = iconRect.y = iconRect.width = iconRect.height = 0;

        // layout the text and icon
        return SwingUtilities.layoutCompoundLabel(
            b, fm, b.getText(), b.getIcon(),
            b.getVerticalAlignment(), b.getHorizontalAlignment(),
            b.getVerticalTextPosition(), b.getHorizontalTextPosition(),
            viewRect, iconRect, textRect,
            b.getText() == null ? 0 : b.getIconTextGap());
    }

    /**
     * Returns the ButtonListener for the passed in Button, or null if one
     * could not be found.
     */
    private BasicButtonListener getButtonListener(AbstractButton b) {
        MouseMotionListener[] listeners = b.getMouseMotionListeners();

        if (listeners != null) {
            for (MouseMotionListener listener : listeners) {
                if (listener instanceof BasicButtonListener) {
                    return (BasicButtonListener) listener;
                }
            }
        }
        return null;
    }

    /////////////////////////// Private functions ////////////////////////
    /**
     * Creates the key listener to handle tab navigation in JToggleButton Group.
     */
    private KeyListener createKeyListener() {
        if (keyListener == null) {
            keyListener = new BasicButtonUI.KeyHandler();
        }
        return keyListener;
    }


    private boolean isValidToggleButtonObj(Object obj) {
        return ((obj instanceof JToggleButton) &&
                ((JToggleButton) obj).isVisible() &&
                ((JToggleButton) obj).isEnabled());
    }

    /**
     * Select toggle button based on "Previous" or "Next" operation
     *
     * @param event, the event object.
     * @param next, indicate if it's next one
     */
    private void selectToggleButton(ActionEvent event, boolean next) {
        // Get the source of the event.
        Object eventSrc = event.getSource();

        // Check whether the source is JToggleButton, it so, whether it is visible
        if (!isValidToggleButtonObj(eventSrc))
            return;

        BasicButtonUI.ButtonGroupInfo btnGroupInfo = new BasicButtonUI.ButtonGroupInfo((JToggleButton)eventSrc);
        btnGroupInfo.selectNewButton(next);
    }

    /////////////////////////// Inner Classes ////////////////////////
    @SuppressWarnings("serial")
    private class SelectPreviousBtn extends AbstractAction {
        public SelectPreviousBtn() {
            super("Previous");
        }

        public void actionPerformed(ActionEvent e) {
            BasicButtonUI.this.selectToggleButton(e, false);
        }
    }

    @SuppressWarnings("serial")
    private class SelectNextBtn extends AbstractAction{
        public SelectNextBtn() {
            super("Next");
        }

        public void actionPerformed(ActionEvent e) {
            BasicButtonUI.this.selectToggleButton(e, true);
        }
    }

    /**
     * ButtonGroupInfo, used to get related info in button group
     * for given toggle button
     */
    private class ButtonGroupInfo {

        JToggleButton activeBtn = null;

        JToggleButton firstBtn = null;
        JToggleButton lastBtn = null;

        JToggleButton previousBtn = null;
        JToggleButton nextBtn = null;

        HashSet<JToggleButton> btnsInGroup = null;

        boolean srcFound = false;
        public ButtonGroupInfo(JToggleButton btn) {
            activeBtn = btn;
            btnsInGroup = new HashSet<JToggleButton>();
        }

        // Check if given object is in the button group
        boolean containsInGroup(Object obj){
            return btnsInGroup.contains(obj);
        }

        // Check if the next object to gain focus belongs
        // to the button group or not
        Component getFocusTransferBaseComponent(boolean next){
            return firstBtn;
        }

        boolean getButtonGroupInfo() {
            if (activeBtn == null)
                return false;

            btnsInGroup.clear();

            // Get the button model from the source.
            ButtonModel model = activeBtn.getModel();
            if (!(model instanceof DefaultButtonModel))
                return false;

            // If the button model is DefaultButtonModel, and use it, otherwise return.
            DefaultButtonModel bm = (DefaultButtonModel) model;

            // get the ButtonGroup of the button from the button model
            ButtonGroup group = bm.getGroup();
            if (group == null)
                return false;

            // Get all the buttons in the group
            Enumeration<AbstractButton> e = group.getElements();
            if (e == null)
                return false;

            while (e.hasMoreElements()) {
                AbstractButton curElement = e.nextElement();
                if (!isValidToggleButtonObj(curElement))
                    continue;

                btnsInGroup.add((JToggleButton) curElement);

                // If firstBtn is not set yet, curElement is that first button
                if (null == firstBtn)
                    firstBtn = (JToggleButton) curElement;

                if (activeBtn == curElement)
                    srcFound = true;
                else if (!srcFound) {
                    // The source has not been yet found and the current element
                    // is the last previousBtn
                    previousBtn = (JToggleButton) curElement;
                } else if (nextBtn == null) {
                    // The source has been found and the current element
                    // is the next valid button of the list
                    nextBtn = (JToggleButton) curElement;
                }

                // Set new last "valid" JToggleButton of the list
                lastBtn = (JToggleButton) curElement;
            }

            return true;
        }

        /**
         * Find the new toggle/radio button that focus needs to be
         * moved to in the group, select the button
         * In case of radio button, setPressed and setArmed is called
         * on the button model, so that Action set on button is performed
         * on selecting the button
         *
         * @param next, indicate if it's arrow up/left or down/right
         */
        void selectNewButton(boolean next) {
            if (!getButtonGroupInfo())
                return;

            if (srcFound) {
                JToggleButton newSelectedBtn = null;
                if (next) {
                    // Select Next button. Cycle to the first button if the source
                    // button is the last of the group.
                    newSelectedBtn = (null == nextBtn) ? firstBtn : nextBtn;
                } else {
                    // Select previous button. Cycle to the last button if the source
                    // button is the first button of the group.
                    newSelectedBtn = (null == previousBtn) ? lastBtn : previousBtn;
                }
                if (newSelectedBtn != null &&
                        (newSelectedBtn != activeBtn)) {
                    ButtonModel btnModel = newSelectedBtn.getModel();
                    if (newSelectedBtn instanceof JRadioButton) {
                        btnModel.setPressed(true);
                        btnModel.setArmed(true);
                    }
                    newSelectedBtn.requestFocusInWindow();
                    newSelectedBtn.setSelected(true);
                    if (newSelectedBtn instanceof JRadioButton) {
                        btnModel.setPressed(false);
                        btnModel.setArmed(false);
                    }
                }
            }
        }

        /**
         * Find the button group the passed in JToggleButton belongs to, and
         * move focus to next component of the last button in the group
         * or previous component of first button
         *
         * @param next, indicate if jump to next component or previous
         */
        void jumpToNextComponent(boolean next) {
            if (!getButtonGroupInfo()){
                // In case the button does not belong to any group, it needs
                // to be treated as a component
                if (activeBtn != null){
                    lastBtn = activeBtn;
                    firstBtn = activeBtn;
                }
                else
                    return;
            }

            // Update the component we will use as base to transfer
            // focus from
            JComponent compTransferFocusFrom = activeBtn;

            // If next component in the parent window is not in
            // the button group, current active button will be
            // base, otherwise, the base will be first or last
            // button in the button group
            Component focusBase = getFocusTransferBaseComponent(next);
            if (focusBase != null){
                if (next) {
                    KeyboardFocusManager.
                            getCurrentKeyboardFocusManager().focusNextComponent(focusBase);
                } else {
                    KeyboardFocusManager.
                            getCurrentKeyboardFocusManager().focusPreviousComponent(focusBase);
                }
            }
        }
    }

    /**
     * Togglebutton KeyListener
     */
    private class KeyHandler implements KeyListener {

        // This listener checks if the key event is a focus traversal key event
        // on a toggle button, consume the event if so and move the focus
        // to next/previous component
        public void keyPressed(KeyEvent e) {
            AWTKeyStroke stroke = AWTKeyStroke.getAWTKeyStrokeForEvent(e);
            if (stroke != null && e.getSource() instanceof JToggleButton) {
                JToggleButton source = (JToggleButton) e.getSource();
                boolean next = isFocusTraversalKey(source,
                        KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                        stroke);
                if (next || isFocusTraversalKey(source,
                        KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                        stroke)) {
                    e.consume();
                    BasicButtonUI.ButtonGroupInfo btnGroupInfo = new BasicButtonUI.ButtonGroupInfo(source);
                    btnGroupInfo.jumpToNextComponent(next);
                }
            }
        }

        private boolean isFocusTraversalKey(JComponent c, int id,
                                            AWTKeyStroke stroke) {
            Set<AWTKeyStroke> keys = c.getFocusTraversalKeys(id);
            return keys != null && keys.contains(stroke);
        }

        public void keyReleased(KeyEvent e) {
        }

        public void keyTyped(KeyEvent e) {
        }
    }

}
