/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * <p>These classes are designed to be used while the
 * corresponding <code>LookAndFeel</code> class has been installed
 * (<code>UIManager.setLookAndFeel(new <i>XXX</i>LookAndFeel())</code>).
 * Using them while a different <code>LookAndFeel</code> is installed
 * may produce unexpected results, including exceptions.
 * Additionally, changing the <code>LookAndFeel</code>
 * maintained by the <code>UIManager</code> without updating the
 * corresponding <code>ComponentUI</code> of any
 * <code>JComponent</code>s may also produce unexpected results,
 * such as the wrong colors showing up, and is generally not
 * encouraged.
 *
 */

package com.sun.java.swing.plaf.windows;

import java.awt.*;
import java.awt.image.*;
import java.security.AccessController;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.*;
import javax.swing.text.JTextComponent;

import sun.awt.image.SunWritableRaster;
import sun.awt.windows.ThemeReader;
import sun.security.action.GetPropertyAction;
import sun.swing.CachedPainter;

import static com.sun.java.swing.plaf.windows.TMSchema.*;


/**
 * Implements Windows XP Styles for the Windows Look and Feel.
 *
 * @author Leif Samuelsson
 */
class XPStyle {
    // Singleton instance of this class
    private static XPStyle xp;

    // Singleton instance of SkinPainter
    private static SkinPainter skinPainter = new SkinPainter();

    private static Boolean themeActive = null;

    private HashMap<String, Border> borderMap;
    private HashMap<String, Color>  colorMap;

    private boolean flatMenus;

    static {
        invalidateStyle();
    }

    /** Static method for clearing the hashmap and loading the
     * current XP style and theme
     */
    static synchronized void invalidateStyle() {
        xp = null;
        themeActive = null;
        skinPainter.flush();
    }

    /** Get the singleton instance of this class
     *
     * @return the singleton instance of this class or null if XP styles
     * are not active or if this is not Windows XP
     */
    @SuppressWarnings("removal")
    static synchronized XPStyle getXP() {
        if (themeActive == null) {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            themeActive =
                (Boolean)toolkit.getDesktopProperty("win.xpstyle.themeActive");
            if (themeActive == null) {
                themeActive = Boolean.FALSE;
            }
            if (themeActive.booleanValue()) {
                GetPropertyAction propertyAction =
                    new GetPropertyAction("swing.noxp");
                if (AccessController.doPrivileged(propertyAction) == null &&
                    ThemeReader.isThemed() &&
                    !(UIManager.getLookAndFeel()
                      instanceof WindowsClassicLookAndFeel)) {

                    xp = new XPStyle();
                }
            }
        }
        return ThemeReader.isXPStyleEnabled() ? xp : null;
    }

    static boolean isVista() {
        XPStyle xp = XPStyle.getXP();
        return (xp != null && xp.isSkinDefined(null, Part.CP_DROPDOWNBUTTONRIGHT));
    }

    /** Get a named <code>String</code> value from the current style
     *
     * @param part a <code>Part</code>
     * @param state a <code>String</code>
     * @param prop a <code>String</code>
     * @return a <code>String</code> or null if key is not found
     *    in the current style
     *
     * This is currently only used by WindowsInternalFrameTitlePane for painting
     * title foregound and can be removed when no longer needed
     */
    String getString(Component c, Part part, State state, Prop prop) {
        return getTypeEnumName(c, part, state, prop);
    }

    TypeEnum getTypeEnum(Component c, Part part, State state, Prop prop) {
        int enumValue = ThemeReader.getEnum(part.getControlName(c), part.getValue(),
                                            State.getValue(part, state),
                                            prop.getValue());
        return TypeEnum.getTypeEnum(prop, enumValue);
    }

    private static String getTypeEnumName(Component c, Part part, State state, Prop prop) {
        int enumValue = ThemeReader.getEnum(part.getControlName(c), part.getValue(),
                                            State.getValue(part, state),
                                            prop.getValue());
        if (enumValue == -1) {
            return null;
        }
        return TypeEnum.getTypeEnum(prop, enumValue).getName();
    }




    /** Get a named <code>int</code> value from the current style
     *
     * @param part a <code>Part</code>
     * @return an <code>int</code> or null if key is not found
     *    in the current style
     */
    int getInt(Component c, Part part, State state, Prop prop, int fallback) {
        return ThemeReader.getInt(part.getControlName(c), part.getValue(),
                                  State.getValue(part, state),
                                  prop.getValue());
    }

    /** Get a named <code>Dimension</code> value from the current style
     *
     * @return a <code>Dimension</code> or null if key is not found
     *    in the current style
     *
     * This is currently only used by WindowsProgressBarUI and the value
     * should probably be cached there instead of here.
     */
    Dimension getDimension(Component c, Part part, State state, Prop prop) {
        Dimension d = ThemeReader.getPosition(part.getControlName(c), part.getValue(),
                                              State.getValue(part, state),
                                              prop.getValue());
        return (d != null) ? d : new Dimension();
    }

    /** Get a named <code>Point</code> (e.g. a location or an offset) value
     *  from the current style
     *
     * @return a <code>Point</code> or null if key is not found
     *    in the current style
     *
     * This is currently only used by WindowsInternalFrameTitlePane for painting
     * title foregound and can be removed when no longer needed
     */
    Point getPoint(Component c, Part part, State state, Prop prop) {
        Dimension d = ThemeReader.getPosition(part.getControlName(c), part.getValue(),
                                              State.getValue(part, state),
                                              prop.getValue());
        return (d != null) ? new Point(d.width, d.height) : new Point();
    }

    /** Get a named <code>Insets</code> value from the current style
     *
     * @return an <code>Insets</code> object or null if key is not found
     *    in the current style
     *
     * This is currently only used to create borders and by
     * WindowsInternalFrameTitlePane for painting title foregound.
     * The return value is already cached in those places.
     */
    Insets getMargin(Component c, Part part, State state, Prop prop) {
        Insets insets = ThemeReader.getThemeMargins(part.getControlName(c), part.getValue(),
                                                    State.getValue(part, state),
                                                    prop.getValue());
        return (insets != null) ? insets : new Insets(0, 0, 0, 0);
    }


    /** Get a named <code>Color</code> value from the current style
     *
     * @return a <code>Color</code> or null if key is not found
     *    in the current style
     */
    synchronized Color getColor(Skin skin, Prop prop, Color fallback) {
        String key = skin.toString() + "." + prop.name();
        Part part = skin.part;
        Color color = colorMap.get(key);
        if (color == null) {
            color = ThemeReader.getColor(part.getControlName(null), part.getValue(),
                                         State.getValue(part, skin.state),
                                         prop.getValue());
            if (color != null) {
                color = new ColorUIResource(color);
                colorMap.put(key, color);
            }
        }
        return (color != null) ? color : fallback;
    }

    Color getColor(Component c, Part part, State state, Prop prop, Color fallback) {
        return getColor(new Skin(c, part, state), prop, fallback);
    }



    /** Get a named <code>Border</code> value from the current style
     *
     * @param part a <code>Part</code>
     * @return a <code>Border</code> or null if key is not found
     *    in the current style or if the style for the particular
     *    part is not defined as "borderfill".
     */
    synchronized Border getBorder(Component c, Part part) {
        if (part == Part.MENU) {
            // Special case because XP has no skin for menus
            if (flatMenus) {
                // TODO: The classic border uses this color, but we should
                // create a new UI property called "PopupMenu.borderColor"
                // instead.
                return new XPFillBorder(UIManager.getColor("InternalFrame.borderShadow"),
                                        1);
            } else {
                return null;    // Will cause L&F to use classic border
            }
        }
        Skin skin = new Skin(c, part, null);
        Border border = borderMap.get(skin.string);
        if (border == null) {
            String bgType = getTypeEnumName(c, part, null, Prop.BGTYPE);
            if ("borderfill".equalsIgnoreCase(bgType)) {
                int thickness = getInt(c, part, null, Prop.BORDERSIZE, 1);
                Color color = getColor(skin, Prop.BORDERCOLOR, Color.black);
                border = new XPFillBorder(color, thickness);
                if (part == Part.CP_COMBOBOX) {
                    border = new XPStatefulFillBorder(color, thickness, part, Prop.BORDERCOLOR);
                }
            } else if ("imagefile".equalsIgnoreCase(bgType)) {
                Insets m = getMargin(c, part, null, Prop.SIZINGMARGINS);
                if (m != null) {
                    if (getBoolean(c, part, null, Prop.BORDERONLY)) {
                        border = new XPImageBorder(c, part);
                    } else if (part == Part.CP_COMBOBOX) {
                        border = new EmptyBorder(1, 1, 1, 1);
                    } else {
                        if(part == Part.TP_BUTTON) {
                            border = new XPEmptyBorder(new Insets(3,3,3,3));
                        } else {
                            border = new XPEmptyBorder(m);
                        }
                    }
                }
            }
            if (border != null) {
                borderMap.put(skin.string, border);
            }
        }
        return border;
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private class XPFillBorder extends LineBorder implements UIResource {
        XPFillBorder(Color color, int thickness) {
            super(color, thickness);
        }

        public Insets getBorderInsets(Component c, Insets insets)       {
            Insets margin = null;
            //
            // Ideally we'd have an interface defined for classes which
            // support margins (to avoid this hackery), but we've
            // decided against it for simplicity
            //
           if (c instanceof AbstractButton) {
               margin = ((AbstractButton)c).getMargin();
           } else if (c instanceof JToolBar) {
               margin = ((JToolBar)c).getMargin();
           } else if (c instanceof JTextComponent) {
               margin = ((JTextComponent)c).getMargin();
           }
           insets.top    = (margin != null? margin.top : 0)    + thickness;
           insets.left   = (margin != null? margin.left : 0)   + thickness;
           insets.bottom = (margin != null? margin.bottom : 0) + thickness;
           insets.right =  (margin != null? margin.right : 0)  + thickness;

           return insets;
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private class XPStatefulFillBorder extends XPFillBorder {
        private final Part part;
        private final Prop prop;
        XPStatefulFillBorder(Color color, int thickness, Part part, Prop prop) {
            super(color, thickness);
            this.part = part;
            this.prop = prop;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            State state = State.NORMAL;
            // special casing for comboboxes.
            // there may be more special cases in the future
            if(c instanceof JComboBox) {
                JComboBox<?> cb = (JComboBox)c;
                // note. in the future this should be replaced with a call
                // to BasicLookAndFeel.getUIOfType()
                if(cb.getUI() instanceof WindowsComboBoxUI) {
                    WindowsComboBoxUI wcb = (WindowsComboBoxUI)cb.getUI();
                    state = wcb.getXPComboBoxState(cb);
                }
            }
            lineColor = getColor(c, part, state, prop, Color.black);
            super.paintBorder(c, g, x, y, width, height);
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private class XPImageBorder extends AbstractBorder implements UIResource {
        Skin skin;

        XPImageBorder(Component c, Part part) {
            this.skin = getSkin(c, part);
        }

        public void paintBorder(Component c, Graphics g,
                                int x, int y, int width, int height) {
            skin.paintSkin(g, x, y, width, height, null);
        }

        public Insets getBorderInsets(Component c, Insets insets)       {
            Insets margin = null;
            Insets borderInsets = skin.getContentMargin();
            if(borderInsets == null) {
                borderInsets = new Insets(0, 0, 0, 0);
            }
            //
            // Ideally we'd have an interface defined for classes which
            // support margins (to avoid this hackery), but we've
            // decided against it for simplicity
            //
           if (c instanceof AbstractButton) {
               margin = ((AbstractButton)c).getMargin();
           } else if (c instanceof JToolBar) {
               margin = ((JToolBar)c).getMargin();
           } else if (c instanceof JTextComponent) {
               margin = ((JTextComponent)c).getMargin();
           }
           insets.top    = (margin != null? margin.top : 0)    + borderInsets.top;
           insets.left   = (margin != null? margin.left : 0)   + borderInsets.left;
           insets.bottom = (margin != null? margin.bottom : 0) + borderInsets.bottom;
           insets.right  = (margin != null? margin.right : 0)  + borderInsets.right;

           return insets;
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private class XPEmptyBorder extends EmptyBorder implements UIResource {
        XPEmptyBorder(Insets m) {
            super(m.top+2, m.left+2, m.bottom+2, m.right+2);
        }

        public Insets getBorderInsets(Component c, Insets insets)       {
            insets = super.getBorderInsets(c, insets);

            Insets margin = null;
            if (c instanceof AbstractButton) {
                Insets m = ((AbstractButton)c).getMargin();
                // if this is a toolbar button then ignore getMargin()
                // and subtract the padding added by the constructor
                if(c.getParent() instanceof JToolBar
                   && ! (c instanceof JRadioButton)
                   && ! (c instanceof JCheckBox)
                   && m instanceof InsetsUIResource) {
                    insets.top -= 2;
                    insets.left -= 2;
                    insets.bottom -= 2;
                    insets.right -= 2;
                } else {
                    margin = m;
                }
            } else if (c instanceof JToolBar) {
                margin = ((JToolBar)c).getMargin();
            } else if (c instanceof JTextComponent) {
                margin = ((JTextComponent)c).getMargin();
            }
            if (margin != null) {
                insets.top    = margin.top + 2;
                insets.left   = margin.left + 2;
                insets.bottom = margin.bottom + 2;
                insets.right  = margin.right + 2;
            }
            return insets;
        }
    }
    boolean isSkinDefined(Component c, Part part) {
        return (part.getValue() == 0)
            || ThemeReader.isThemePartDefined(
                   part.getControlName(c), part.getValue(), 0);
    }


    /** Get a <code>Skin</code> object from the current style
     * for a named part (component type)
     *
     * @param part a <code>Part</code>
     * @return a <code>Skin</code> object
     */
    synchronized Skin getSkin(Component c, Part part) {
        assert isSkinDefined(c, part) : "part " + part + " is not defined";
        return new Skin(c, part, null);
    }


    long getThemeTransitionDuration(Component c, Part part, State stateFrom,
                                    State stateTo, Prop prop) {
         return ThemeReader.getThemeTransitionDuration(part.getControlName(c),
                                          part.getValue(),
                                          State.getValue(part, stateFrom),
                                          State.getValue(part, stateTo),
                                          (prop != null) ? prop.getValue() : 0);
    }


    /** A class which encapsulates attributes for a given part
     * (component type) and which provides methods for painting backgrounds
     * and glyphs
     */
    static class Skin {
        final Component component;
        final Part part;
        final State state;

        private final String string;
        private Dimension size = null;
        private boolean switchStates = false;

        Skin(Component component, Part part) {
            this(component, part, null);
        }

        Skin(Part part, State state) {
            this(null, part, state);
        }

        Skin(Component component, Part part, State state) {
            this.component = component;
            this.part  = part;
            this.state = state;

            String str = part.getControlName(component) +"." + part.name();
            if (state != null) {
                str += "("+state.name()+")";
            }
            string = str;
        }

        Insets getContentMargin() {
            /* idk: it seems margins are the same for all 'big enough'
             * bounding rectangles.
             */
            int boundingWidth = 100;
            int boundingHeight = 100;

            Insets insets = ThemeReader.getThemeBackgroundContentMargins(
                part.getControlName(null), part.getValue(),
                0, boundingWidth, boundingHeight);
            return (insets != null) ? insets : new Insets(0, 0, 0, 0);
        }

        boolean haveToSwitchStates() {
            return switchStates;
        }

        void switchStates(boolean b) {
            switchStates = b;
        }

        private int getWidth(State state) {
            if (size == null) {
                size = getPartSize(part, state);
            }
            return (size != null) ? size.width : 0;
        }

        int getWidth() {
            return getWidth((state != null) ? state : State.NORMAL);
        }

        private int getHeight(State state) {
            if (size == null) {
                size = getPartSize(part, state);
            }
            return (size != null) ? size.height : 0;
        }

        int getHeight() {
            return getHeight((state != null) ? state : State.NORMAL);
        }

        public String toString() {
            return string;
        }

        public boolean equals(Object obj) {
            return (obj instanceof Skin && ((Skin)obj).string.equals(string));
        }

        public int hashCode() {
            return string.hashCode();
        }

        /** Paint a skin at x, y.
         *
         * @param g   the graphics context to use for painting
         * @param dx  the destination <i>x</i> coordinate
         * @param dy  the destination <i>y</i> coordinate
         * @param state which state to paint
         */
        void paintSkin(Graphics g, int dx, int dy, State state) {
            if (state == null) {
                state = this.state;
            }
            paintSkin(g, dx, dy, getWidth(state), getHeight(state), state);
        }

        /** Paint a skin in an area defined by a rectangle.
         *
         * @param g the graphics context to use for painting
         * @param r     a <code>Rectangle</code> defining the area to fill,
         *                     may cause the image to be stretched or tiled
         * @param state which state to paint
         */
        void paintSkin(Graphics g, Rectangle r, State state) {
            paintSkin(g, r.x, r.y, r.width, r.height, state);
        }

        /** Paint a skin at a defined position and size
         *  This method supports animation.
         *
         * @param g   the graphics context to use for painting
         * @param dx  the destination <i>x</i> coordinate
         * @param dy  the destination <i>y</i> coordinate
         * @param dw  the width of the area to fill, may cause
         *                  the image to be stretched or tiled
         * @param dh  the height of the area to fill, may cause
         *                  the image to be stretched or tiled
         * @param state which state to paint
         */
        void paintSkin(Graphics g, int dx, int dy, int dw, int dh, State state) {
            if (XPStyle.getXP() == null) {
                return;
            }
            if (component instanceof JComponent
                  && SwingUtilities.getAncestorOfClass(CellRendererPane.class,
                                                       component) == null) {
                AnimationController.paintSkin((JComponent) component, this,
                                              g, dx, dy, dw, dh, state);
            } else {
                paintSkinRaw(g, dx, dy, dw, dh, state);
            }
        }

        /** Paint a skin at a defined position and size. This method
         *  does not trigger animation. It is needed for the animation
         *  support.
         *
         * @param g   the graphics context to use for painting
         * @param dx  the destination <i>x</i> coordinate.
         * @param dy  the destination <i>y</i> coordinate.
         * @param dw  the width of the area to fill, may cause
         *                  the image to be stretched or tiled
         * @param dh  the height of the area to fill, may cause
         *                  the image to be stretched or tiled
         * @param state which state to paint
         */
        void paintSkinRaw(Graphics g, int dx, int dy, int dw, int dh, State state) {
            if (XPStyle.getXP() == null) {
                return;
            }
            skinPainter.paint(null, g, dx, dy, dw, dh, this, state);
        }

        /** Paint a skin at a defined position and size
         *
         * @param g   the graphics context to use for painting
         * @param dx  the destination <i>x</i> coordinate
         * @param dy  the destination <i>y</i> coordinate
         * @param dw  the width of the area to fill, may cause
         *                  the image to be stretched or tiled
         * @param dh  the height of the area to fill, may cause
         *                  the image to be stretched or tiled
         * @param state which state to paint
         * @param borderFill should test if the component uses a border fill
                            and skip painting if it is
         */
        void paintSkin(Graphics g, int dx, int dy, int dw, int dh, State state,
                boolean borderFill) {
            if (XPStyle.getXP() == null) {
                return;
            }
            if(borderFill && "borderfill".equals(getTypeEnumName(component, part,
                    state, Prop.BGTYPE))) {
                return;
            }
            skinPainter.paint(null, g, dx, dy, dw, dh, this, state);
        }
    }

    private static class SkinPainter extends CachedPainter {
        SkinPainter() {
            super(30);
            flush();
        }

        public void flush() {
            super.flush();
        }

        protected void paintToImage(Component c, Image image, Graphics g,
                                    int w, int h, Object[] args) {
            Skin skin = (Skin)args[0];
            Part part = skin.part;
            State state = (State)args[1];
            if (state == null) {
                state = skin.state;
            }
            if (c == null) {
                c = skin.component;
            }
            BufferedImage bi = (BufferedImage)image;
            w = bi.getWidth();
            h = bi.getHeight();

            WritableRaster raster = bi.getRaster();
            DataBufferInt dbi = (DataBufferInt)raster.getDataBuffer();
            // Note that stealData() requires a markDirty() afterwards
            // since we modify the data in it.
            ThemeReader.paintBackground(SunWritableRaster.stealData(dbi, 0),
                                        part.getControlName(c), part.getValue(),
                                        State.getValue(part, state),
                                        0, 0, w, h, w);
            SunWritableRaster.markDirty(dbi);
        }

        protected Image createImage(Component c, int w, int h,
                                    GraphicsConfiguration config, Object[] args) {
            return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    static class GlyphButton extends JButton {
        protected Skin skin;

        public GlyphButton(Component parent, Part part) {
            XPStyle xp = getXP();
            skin = xp != null ? xp.getSkin(parent, part) : null;
            setBorder(null);
            setContentAreaFilled(false);
            setMinimumSize(new Dimension(5, 5));
            setPreferredSize(new Dimension(16, 16));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        }

        @SuppressWarnings("deprecation")
        public boolean isFocusTraversable() {
            return false;
        }

        protected State getState() {
            State state = State.NORMAL;
            if (!isEnabled()) {
                state = State.DISABLED;
            } else if (getModel().isPressed()) {
                state = State.PRESSED;
            } else if (getModel().isRollover()) {
                state = State.HOT;
            }
            return state;
        }

        public void paintComponent(Graphics g) {
            if (XPStyle.getXP() == null || skin == null) {
                return;
            }
            Dimension d = getSize();
            skin.paintSkin(g, 0, 0, d.width, d.height, getState());
        }

        public void setPart(Component parent, Part part) {
            XPStyle xp = getXP();
            skin = xp != null ? xp.getSkin(parent, part) : null;
            revalidate();
            repaint();
        }

        protected void paintBorder(Graphics g) {
        }


    }

    // Private constructor
    private XPStyle() {
        flatMenus = getSysBoolean(Prop.FLATMENUS);

        colorMap  = new HashMap<String, Color>();
        borderMap = new HashMap<String, Border>();
        // Note: All further access to the maps must be synchronized
    }


    private boolean getBoolean(Component c, Part part, State state, Prop prop) {
        return ThemeReader.getBoolean(part.getControlName(c), part.getValue(),
                                      State.getValue(part, state),
                                      prop.getValue());
    }



    static Dimension getPartSize(Part part, State state) {
        return ThemeReader.getPartSize(part.getControlName(null), part.getValue(),
                                       State.getValue(part, state));
    }

    private static boolean getSysBoolean(Prop prop) {
        // We can use any widget name here, I guess.
        return ThemeReader.getSysBoolean("window", prop.getValue());
    }
}
