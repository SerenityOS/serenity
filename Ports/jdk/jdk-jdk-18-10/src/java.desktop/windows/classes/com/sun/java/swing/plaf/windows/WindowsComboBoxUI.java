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

package com.sun.java.swing.plaf.windows;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ButtonModel;
import javax.swing.ComboBoxEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

import com.sun.java.swing.plaf.windows.WindowsBorders.DashedBorder;
import sun.swing.DefaultLookup;
import sun.swing.StringUIClientPropertyKey;

import static com.sun.java.swing.plaf.windows.TMSchema.Part;
import static com.sun.java.swing.plaf.windows.TMSchema.State;
import static com.sun.java.swing.plaf.windows.XPStyle.Skin;

/**
 * Windows combo box.
 *
 * @author Tom Santos
 * @author Igor Kushnirskiy
 */
public class WindowsComboBoxUI extends BasicComboBoxUI {

    private static final MouseListener rolloverListener =
        new MouseAdapter() {
            private void handleRollover(MouseEvent e, boolean isRollover) {
                JComboBox<?> comboBox = getComboBox(e);
                WindowsComboBoxUI comboBoxUI = getWindowsComboBoxUI(e);
                if (comboBox == null || comboBoxUI == null) {
                    return;
                }
                if (! comboBox.isEditable()) {
                    //mouse over editable ComboBox does not switch rollover
                    //for the arrow button
                    ButtonModel m = null;
                    if (comboBoxUI.arrowButton != null) {
                        m = comboBoxUI.arrowButton.getModel();
                    }
                    if (m != null ) {
                        m.setRollover(isRollover);
                    }
                }
                comboBoxUI.isRollover = isRollover;
                comboBox.repaint();
            }

            public void mouseEntered(MouseEvent e) {
                handleRollover(e, true);
            }

            public void mouseExited(MouseEvent e) {
                handleRollover(e, false);
            }

            private JComboBox<?> getComboBox(MouseEvent event) {
                Object source = event.getSource();
                JComboBox<?> rv = null;
                if (source instanceof JComboBox) {
                    rv = (JComboBox) source;
                } else if (source instanceof XPComboBoxButton) {
                    rv = ((XPComboBoxButton) source)
                        .getWindowsComboBoxUI().comboBox;
                } else if (source instanceof JTextField &&
                        ((JTextField) source).getParent() instanceof JComboBox) {
                    rv = (JComboBox) ((JTextField) source).getParent();
                }
                return rv;
            }

            private WindowsComboBoxUI getWindowsComboBoxUI(MouseEvent event) {
                JComboBox<?> comboBox = getComboBox(event);
                WindowsComboBoxUI rv = null;
                if (comboBox != null
                    && comboBox.getUI() instanceof WindowsComboBoxUI) {
                    rv = (WindowsComboBoxUI) comboBox.getUI();
                }
                return rv;
            }

        };
    private boolean isRollover = false;

    private static final PropertyChangeListener componentOrientationListener =
        new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                String propertyName = e.getPropertyName();
                Object source = null;
                if ("componentOrientation" == propertyName
                    && (source = e.getSource()) instanceof JComboBox
                    && ((JComboBox) source).getUI() instanceof
                      WindowsComboBoxUI) {
                    JComboBox<?> comboBox = (JComboBox) source;
                    WindowsComboBoxUI comboBoxUI = (WindowsComboBoxUI) comboBox.getUI();
                    if (comboBoxUI.arrowButton instanceof XPComboBoxButton) {
                        ((XPComboBoxButton) comboBoxUI.arrowButton).setPart(
                                    (comboBox.getComponentOrientation() ==
                                       ComponentOrientation.RIGHT_TO_LEFT)
                                    ? Part.CP_DROPDOWNBUTTONLEFT
                                    : Part.CP_DROPDOWNBUTTONRIGHT);
                            }
                        }
                    }
                };

    public static ComponentUI createUI(JComponent c) {
        return new WindowsComboBoxUI();
    }

    public void installUI( JComponent c ) {
        super.installUI( c );
        isRollover = false;
        comboBox.setRequestFocusEnabled( true );
        if (XPStyle.getXP() != null && arrowButton != null) {
            //we can not do it in installListeners because arrowButton
            //is initialized after installListeners is invoked
            comboBox.addMouseListener(rolloverListener);
            arrowButton.addMouseListener(rolloverListener);
            // set empty border as default to see vista animated border
            comboBox.setBorder(new EmptyBorder(1,1,1,1));
        }
    }

    public void uninstallUI(JComponent c ) {
        comboBox.removeMouseListener(rolloverListener);
        if(arrowButton != null) {
            arrowButton.removeMouseListener(rolloverListener);
        }
        super.uninstallUI( c );
    }

    /**
     * {@inheritDoc}
     * @since 1.6
     */
    @Override
    protected void installListeners() {
        super.installListeners();
        XPStyle xp = XPStyle.getXP();
        //button glyph for LTR and RTL combobox might differ
        if (xp != null
              && xp.isSkinDefined(comboBox, Part.CP_DROPDOWNBUTTONRIGHT)) {
            comboBox.addPropertyChangeListener("componentOrientation",
                                               componentOrientationListener);
        }
    }

    /**
     * {@inheritDoc}
     * @since 1.6
     */
    @Override
    protected void uninstallListeners() {
        super.uninstallListeners();
        comboBox.removePropertyChangeListener("componentOrientation",
                                              componentOrientationListener);
    }

    /**
     * {@inheritDoc}
     * @since 1.6
     */
    protected void configureEditor() {
        super.configureEditor();
        if (XPStyle.getXP() != null) {
            editor.addMouseListener(rolloverListener);
        }
    }

    /**
     * {@inheritDoc}
     * @since 1.6
     */
    protected void unconfigureEditor() {
        super.unconfigureEditor();
        editor.removeMouseListener(rolloverListener);
    }

    /**
     * {@inheritDoc}
     * @since 1.6
     */
    public void paint(Graphics g, JComponent c) {
        if (XPStyle.getXP() != null) {
            paintXPComboBoxBackground(g, c);
        }
        super.paint(g, c);
    }

    State getXPComboBoxState(JComponent c) {
        State state = State.NORMAL;
        if (!c.isEnabled()) {
            state = State.DISABLED;
        } else if (isPopupVisible(comboBox)) {
            state = State.PRESSED;
        } else if (comboBox.isEditable()
                && comboBox.getEditor().getEditorComponent().isFocusOwner()) {
            state = State.PRESSED;
        } else if (isRollover) {
            state = State.HOT;
        }
        return state;
    }

    private void paintXPComboBoxBackground(Graphics g, JComponent c) {
        XPStyle xp = XPStyle.getXP();
        if (xp == null) {
            return;
        }
        State state = getXPComboBoxState(c);
        Skin skin = null;
        if (! comboBox.isEditable()
              && xp.isSkinDefined(c, Part.CP_READONLY)) {
            skin = xp.getSkin(c, Part.CP_READONLY);
        }
        if (skin == null) {
            skin = xp.getSkin(c, Part.CP_BORDER);
        }
        skin.paintSkin(g, 0, 0, c.getWidth(), c.getHeight(), state);
    }

    /**
     * If necessary paints the currently selected item.
     *
     * @param g Graphics to paint to
     * @param bounds Region to paint current value to
     * @param hasFocus whether or not the JComboBox has focus
     * @throws NullPointerException if any of the arguments are null.
     * @since 1.5
     */
    public void paintCurrentValue(Graphics g, Rectangle bounds,
                                  boolean hasFocus) {
        XPStyle xp = XPStyle.getXP();
        if ( xp != null) {
            bounds.x += 2;
            bounds.y += 2;
            bounds.width -= 4;
            bounds.height -= 4;
        } else {
            bounds.x += 1;
            bounds.y += 1;
            bounds.width -= 2;
            bounds.height -= 2;
        }
        if (! comboBox.isEditable()
            && xp != null
            && xp.isSkinDefined(comboBox, Part.CP_READONLY)) {
            // On vista for READNLY ComboBox
            // color for currentValue is the same as for any other item

            // mostly copied from javax.swing.plaf.basic.BasicComboBoxUI.paintCurrentValue
            ListCellRenderer<Object> renderer = comboBox.getRenderer();
            Component c;
            if ( hasFocus && !isPopupVisible(comboBox) ) {
                c = renderer.getListCellRendererComponent(
                        listBox,
                        comboBox.getSelectedItem(),
                        -1,
                        true,
                        false );
            } else {
                c = renderer.getListCellRendererComponent(
                        listBox,
                        comboBox.getSelectedItem(),
                        -1,
                        false,
                        false );
            }
            c.setFont(comboBox.getFont());
            if ( comboBox.isEnabled() ) {
                c.setForeground(comboBox.getForeground());
                c.setBackground(comboBox.getBackground());
            } else {
                c.setForeground(DefaultLookup.getColor(
                         comboBox, this, "ComboBox.disabledForeground", null));
                c.setBackground(DefaultLookup.getColor(
                         comboBox, this, "ComboBox.disabledBackground", null));
            }
            boolean shouldValidate = false;
            if (c instanceof JPanel)  {
                shouldValidate = true;
            }
            currentValuePane.paintComponent(g, c, comboBox, bounds.x, bounds.y,
                                            bounds.width, bounds.height, shouldValidate);

        } else {
            super.paintCurrentValue(g, bounds, hasFocus);
        }
    }

    /**
     * {@inheritDoc}
     * @since 1.6
     */
    public void paintCurrentValueBackground(Graphics g, Rectangle bounds,
                                            boolean hasFocus) {
        if (XPStyle.getXP() == null) {
            super.paintCurrentValueBackground(g, bounds, hasFocus);
        }
    }

    public Dimension getMinimumSize( JComponent c ) {
        Dimension d = super.getMinimumSize(c);
        if (XPStyle.getXP() != null) {
            d.width += 7;
            boolean isEditable = false;
            if (c instanceof JComboBox) {
                isEditable = ((JComboBox) c).isEditable();
            }
            if (((JComboBox)c).getBorder() instanceof EmptyBorder) {
                d.height += isEditable ? 2 : 4;
            } else {
                d.height += isEditable ? 4 : 6;
            }
        } else {
            d.width += 4;
            d.height += 2;
        }
        return d;
    }

    /**
     * Creates a layout manager for managing the components which make up the
     * combo box.
     *
     * @return an instance of a layout manager
     */
    protected LayoutManager createLayoutManager() {
        return new BasicComboBoxUI.ComboBoxLayoutManager() {
            public void layoutContainer(Container parent) {
                super.layoutContainer(parent);

                if (XPStyle.getXP() != null && arrowButton != null) {
                    Dimension d = parent.getSize();
                    Insets insets = getInsets();

                    int borderInsetsCorrection = 0;
                    if (((JComboBox)parent).getBorder() instanceof EmptyBorder) {
                        borderInsetsCorrection = 1;
                    }
                    arrowButton.setBounds(
                        WindowsGraphicsUtils.isLeftToRight((JComboBox)parent)
                            ? (d.width - (insets.right - borderInsetsCorrection)
                                - arrowButton.getPreferredSize().width)
                            : insets.left - borderInsetsCorrection,
                            insets.top - borderInsetsCorrection,
                            arrowButton.getPreferredSize().width,
                            d.height - (insets.top - borderInsetsCorrection) -
                                    (insets.bottom - borderInsetsCorrection));
                }
            }
        };
    }

    protected void installKeyboardActions() {
        super.installKeyboardActions();
    }

    protected ComboPopup createPopup() {
        return new WinComboPopUp(comboBox);
    }

    /**
     * Creates the default editor that will be used in editable combo boxes.
     * A default editor will be used only if an editor has not been
     * explicitly set with <code>setEditor</code>.
     *
     * @return a <code>ComboBoxEditor</code> used for the combo box
     * @see javax.swing.JComboBox#setEditor
     */
    protected ComboBoxEditor createEditor() {
        return new WindowsComboBoxEditor();
    }

    /**
     * {@inheritDoc}
     * @since 1.6
     */
    @Override
    protected ListCellRenderer<Object> createRenderer() {
        XPStyle xp = XPStyle.getXP();
        if (xp != null && xp.isSkinDefined(comboBox, Part.CP_READONLY)) {
            return new WindowsComboBoxRenderer();
        } else {
            return super.createRenderer();
        }
    }

    /**
     * Creates an button which will be used as the control to show or hide
     * the popup portion of the combo box.
     *
     * @return a button which represents the popup control
     */
    protected JButton createArrowButton() {
        XPStyle xp = XPStyle.getXP();
        if (xp != null) {
            return new XPComboBoxButton(xp);
        } else {
            return super.createArrowButton();
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private class XPComboBoxButton extends XPStyle.GlyphButton {
        private State prevState = null;

        public XPComboBoxButton(XPStyle xp) {
            super(comboBox,
                  (! xp.isSkinDefined(comboBox, Part.CP_DROPDOWNBUTTONRIGHT))
                   ? Part.CP_DROPDOWNBUTTON
                   : (comboBox.getComponentOrientation() == ComponentOrientation.RIGHT_TO_LEFT)
                     ? Part.CP_DROPDOWNBUTTONLEFT
                     : Part.CP_DROPDOWNBUTTONRIGHT
                  );
            setRequestFocusEnabled(false);
        }

        @Override
        protected State getState() {
            State rv;

            getModel().setPressed(comboBox.isPopupVisible());

            rv = super.getState();
            XPStyle xp = XPStyle.getXP();
            if (rv != State.DISABLED
                    && comboBox != null && ! comboBox.isEditable()
                    && xp != null && xp.isSkinDefined(comboBox,
                            Part.CP_DROPDOWNBUTTONRIGHT)) {
                /*
                 * for non editable ComboBoxes Vista seems to have the
                 * same glyph for all non DISABLED states
                 */
                rv = State.NORMAL;
            }
            if (rv == State.NORMAL && (prevState == State.HOT || prevState == State.PRESSED)) {
                /*
                 * State NORMAL of combobox button cannot overpaint states HOT or PRESSED
                 * Therefore HOT state must be painted from alpha 1 to 0 and not as usual that
                 * NORMAL state is painted from alpha 0 to alpha 1.
                 */
                skin.switchStates(true);
            }
            if (rv != prevState) {
                prevState = rv;
            }

            return rv;
        }

        public Dimension getPreferredSize() {
            return new Dimension(17, 21);
        }

        void setPart(Part part) {
            setPart(comboBox, part);
        }

        WindowsComboBoxUI getWindowsComboBoxUI() {
            return WindowsComboBoxUI.this;
        }
    }

    @SuppressWarnings("serial") // Same-version serialization only
    protected class WinComboPopUp extends BasicComboPopup {
        private Skin listBoxBorder = null;
        private XPStyle xp;

        public WinComboPopUp(JComboBox<Object> combo) {
            super(combo);
            xp = XPStyle.getXP();
            if (xp != null && xp.isSkinDefined(combo, Part.LBCP_BORDER_NOSCROLL)) {
                this.listBoxBorder = new Skin(combo, Part.LBCP_BORDER_NOSCROLL);
                this.setBorder(new EmptyBorder(1,1,1,1));
            }
        }

        protected KeyListener createKeyListener() {
            return new InvocationKeyHandler();
        }

        protected class InvocationKeyHandler extends BasicComboPopup.InvocationKeyHandler {
            protected InvocationKeyHandler() {
                WinComboPopUp.this.super();
            }
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (this.listBoxBorder != null) {
                this.listBoxBorder.paintSkinRaw(g, this.getX(), this.getY(),
                        this.getWidth(), this.getHeight(), State.HOT);
            }
        }
    }


    /**
     * Subclassed to highlight selected item in an editable combo box.
     */
    public static class WindowsComboBoxEditor
        extends BasicComboBoxEditor.UIResource {

        /**
         * {@inheritDoc}
         * @since 1.6
         */
        protected JTextField createEditorComponent() {
            JTextField editor = super.createEditorComponent();
            Border border = (Border)UIManager.get("ComboBox.editorBorder");

            if (border != null) {
                editor.setBorder(border);
            }
            editor.setOpaque(false);
            return editor;
        }

        public void setItem(Object item) {
            super.setItem(item);
            Object focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if ((focus == editor) || (focus == editor.getParent())) {
                editor.selectAll();
            }
        }
    }

    /**
     * Subclassed to set opacity {@code false} on the renderer
     * and to show border for focused cells.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private static class WindowsComboBoxRenderer
          extends BasicComboBoxRenderer.UIResource {
        private static final Object BORDER_KEY
            = new StringUIClientPropertyKey("BORDER_KEY");
        private static final Border NULL_BORDER = new EmptyBorder(0, 0, 0, 0);

        // Create own version of DashedBorder with more space on left side
        private class WindowsComboBoxDashedBorder extends DashedBorder {

            public WindowsComboBoxDashedBorder(Color color, int thickness) {
                super(color, thickness);
            }

            public WindowsComboBoxDashedBorder(Color color) {
                super(color);
            }

            @Override
            public Insets getBorderInsets(Component c, Insets i) {
                return new Insets(0,2,0,0);
            }
        }

        public WindowsComboBoxRenderer() {
            super();

            // correct space on the left side of text items in the combo popup list
            Insets i = getBorder().getBorderInsets(this);
            setBorder(new EmptyBorder(0, 2, 0, i.right));
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(
                                                 JList<?> list,
                                                 Object value,
                                                 int index,
                                                 boolean isSelected,
                                                 boolean cellHasFocus) {
            Component rv =
                super.getListCellRendererComponent(list, value, index,
                                                   isSelected, cellHasFocus);
            if (rv instanceof JComponent) {
                JComponent component = (JComponent) rv;
                if (index == -1 && isSelected) {
                    Border border = component.getBorder();
                    Border dashedBorder =
                        new WindowsComboBoxDashedBorder(list.getForeground());
                    component.setBorder(dashedBorder);
                    //store current border in client property if needed
                    if (component.getClientProperty(BORDER_KEY) == null) {
                        component.putClientProperty(BORDER_KEY,
                                       (border == null) ? NULL_BORDER : border);
                    }
                } else {
                    if (component.getBorder() instanceof
                          WindowsBorders.DashedBorder) {
                        Object storedBorder = component.getClientProperty(BORDER_KEY);
                        if (storedBorder instanceof Border) {
                            component.setBorder(
                                (storedBorder == NULL_BORDER) ? null
                                    : (Border) storedBorder);
                        }
                        component.putClientProperty(BORDER_KEY, null);
                    }
                }
                if (index == -1) {
                    component.setOpaque(false);
                    component.setForeground(list.getForeground());
                } else {
                    component.setOpaque(true);
                }
            }
            return rv;
        }

    }
}
