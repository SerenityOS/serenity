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

package com.sun.java.swing.plaf.gtk;

import java.awt.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.synth.*;

import sun.awt.AppContext;
import sun.awt.UNIXToolkit;
import sun.swing.SwingUtilities2;
import javax.swing.plaf.synth.SynthIcon;

import com.sun.java.swing.plaf.gtk.GTKEngine.WidgetType;
import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.KEY_TEXT_LCD_CONTRAST;

/**
 *
 * @author Scott Violet
 */
class GTKStyle extends SynthStyle implements GTKConstants {

    private static native int nativeGetXThickness(int widgetType);
    private static native int nativeGetYThickness(int widgetType);
    private static native int nativeGetColorForState(int widgetType,
                                                     int state, int typeID);
    private static native Object nativeGetClassValue(int widgetType,
                                                     String key);
    private static native String nativeGetPangoFontName(int widgetType);

    private static final String ICON_PROPERTY_PREFIX = "gtk.icon.";

    static final Color BLACK_COLOR = new ColorUIResource(Color.BLACK);
    static final Color WHITE_COLOR = new ColorUIResource(Color.WHITE);

    static final Font DEFAULT_FONT = new FontUIResource("sansserif",
                                                        Font.PLAIN, 10  );
    static final Insets BUTTON_DEFAULT_BORDER_INSETS = new Insets(1, 1, 1, 1);

    private static final GTKGraphicsUtils GTK_GRAPHICS = new GTKGraphicsUtils();

    /**
     * Maps from a key that is passed to Style.get to the equivalent class
     * specific key.
     */
    private static final Map<String,String> CLASS_SPECIFIC_MAP;

    /**
     * Backing style properties that are used if the style does not
     * defined the property.
     */
    private static final Map<String,GTKStockIcon> ICONS_MAP;

    /**
     * The font used for this particular style, as determined at
     * construction time.
     */
    private final Font font;

    /** Widget type used when looking up class specific values. */
    private final int widgetType;

    /** The x/y thickness values for this particular style. */
    private final int xThickness, yThickness;

    GTKStyle(Font userFont, WidgetType widgetType) {
        this.widgetType = widgetType.ordinal();

        String pangoFontName;
        synchronized (sun.awt.UNIXToolkit.GTK_LOCK) {
            xThickness = nativeGetXThickness(this.widgetType);
            yThickness = nativeGetYThickness(this.widgetType);
            pangoFontName = nativeGetPangoFontName(this.widgetType);
        }

        Font pangoFont = null;
        if (pangoFontName != null) {
            pangoFont = PangoFonts.lookupFont(pangoFontName);
        }
        if (pangoFont != null) {
            this.font = pangoFont;
        } else if (userFont != null) {
            this.font = userFont;
        } else {
            this.font = DEFAULT_FONT;
        }
    }

    @Override
    public void installDefaults(SynthContext context) {
        super.installDefaults(context);
        Map<Object, Object> aaTextInfo = GTKLookAndFeel.aaTextInfo;
        if (aaTextInfo != null && !context.getRegion().isSubregion()) {
            context.getComponent().putClientProperty(KEY_TEXT_ANTIALIASING,
                    aaTextInfo.get(KEY_TEXT_ANTIALIASING));
            context.getComponent().putClientProperty(KEY_TEXT_LCD_CONTRAST,
                    aaTextInfo.get(KEY_TEXT_LCD_CONTRAST));
        }
    }

    @Override
    public SynthGraphicsUtils getGraphicsUtils(SynthContext context) {
        return GTK_GRAPHICS;
    }

    /**
     * Returns a <code>SynthPainter</code> that will route the appropriate
     * calls to a <code>GTKEngine</code>.
     *
     * @param state SynthContext identifying requestor
     * @return SynthPainter
     */
    @Override
    public SynthPainter getPainter(SynthContext state) {
        return GTKPainter.INSTANCE;
    }

    protected Color getColorForState(SynthContext context, ColorType type) {
        if (type == ColorType.FOCUS || type == GTKColorType.BLACK) {
            return BLACK_COLOR;
        }
        else if (type == GTKColorType.WHITE) {
            return WHITE_COLOR;
        }

        Region id = context.getRegion();
        int state = context.getComponentState();
        state = GTKLookAndFeel.synthStateToGTKState(id, state);

        if (type == ColorType.TEXT_FOREGROUND &&
               (id == Region.BUTTON ||
                id == Region.CHECK_BOX ||
                id == Region.CHECK_BOX_MENU_ITEM ||
                id == Region.MENU ||
                id == Region.MENU_ITEM ||
                id == Region.RADIO_BUTTON ||
                id == Region.RADIO_BUTTON_MENU_ITEM ||
                id == Region.TABBED_PANE_TAB ||
                id == Region.TOGGLE_BUTTON ||
                id == Region.TOOL_TIP ||
                id == Region.MENU_ITEM_ACCELERATOR)) {
            type = ColorType.FOREGROUND;
        } else if (id == Region.TABLE ||
                   id == Region.LIST ||
                   id == Region.TREE ||
                   id == Region.TREE_CELL) {
            if (type == ColorType.FOREGROUND) {
                type = ColorType.TEXT_FOREGROUND;
                if (state == SynthConstants.PRESSED) {
                    state = SynthConstants.SELECTED;
                }
            } else if (type == ColorType.BACKGROUND) {
                type = ColorType.TEXT_BACKGROUND;
            }
        }

        return getStyleSpecificColor(context, state, type);
    }

    /**
     * Returns color specific to the current style. This method is
     * invoked when other variants don't fit.
     */
    private Color getStyleSpecificColor(SynthContext context, int state,
                                        ColorType type)
    {
        state = GTKLookAndFeel.synthStateToGTKStateType(state).ordinal();
        synchronized (sun.awt.UNIXToolkit.GTK_LOCK) {
            int rgb = nativeGetColorForState(widgetType, state,
                                             type.getID());
            return new ColorUIResource(rgb);
        }
    }

    Color getGTKColor(int state, ColorType type) {
        return getGTKColor(null, state, type);
    }

    Color getGTKColor(int widgetType, int state, int colorType) {
        synchronized (sun.awt.UNIXToolkit.GTK_LOCK) {
            int rgb = nativeGetColorForState(widgetType, state,
                    colorType);
            return new ColorUIResource(rgb);
        }
    }

    /**
     * Returns the color for the specified state.
     *
     * @param context SynthContext identifying requestor
     * @param state to get the color for
     * @param type of the color
     * @return Color to render with
     */
    Color getGTKColor(SynthContext context, int state, ColorType type) {
        if (context != null) {
            JComponent c = context.getComponent();
            Region id = context.getRegion();

            state = GTKLookAndFeel.synthStateToGTKState(id, state);
            if (!id.isSubregion() &&
                (state & SynthConstants.ENABLED) != 0) {
                if (type == ColorType.BACKGROUND ||
                    type == ColorType.TEXT_BACKGROUND) {
                    Color bg = c.getBackground();
                    if (!(bg instanceof UIResource)) {
                        return bg;
                    }
                }
                else if (type == ColorType.FOREGROUND ||
                         type == ColorType.TEXT_FOREGROUND) {
                    Color fg = c.getForeground();
                    if (!(fg instanceof UIResource)) {
                        return fg;
                    }
                }
            }
        }

        return getStyleSpecificColor(context, state, type);
    }

    @Override
    public Color getColor(SynthContext context, ColorType type) {
        JComponent c = context.getComponent();
        Region id = context.getRegion();
        int state = context.getComponentState();

        if (c.getName() == "Table.cellRenderer") {
             if (type == ColorType.BACKGROUND) {
                 return c.getBackground();
             }
             if (type == ColorType.FOREGROUND) {
                 return c.getForeground();
             }
        }

        if (id == Region.LABEL && type == ColorType.TEXT_FOREGROUND) {
            type = ColorType.FOREGROUND;
        }

        // For the enabled state, prefer the widget's colors
        if (!id.isSubregion() && (state & SynthConstants.ENABLED) != 0) {
            if (type == ColorType.BACKGROUND) {
                return c.getBackground();
            }
            else if (type == ColorType.FOREGROUND) {
                return c.getForeground();
            }
            else if (type == ColorType.TEXT_FOREGROUND) {
                // If getForeground returns a non-UIResource it means the
                // developer has explicitly set the foreground, use it over
                // that of TEXT_FOREGROUND as that is typically the expected
                // behavior.
                Color color = c.getForeground();
                if (color != null && !(color instanceof UIResource)) {
                    return color;
                }
            }
        }
        return getColorForState(context, type);
    }

    Font getDefaultFont() {
        return font;
    }

    protected Font getFontForState(SynthContext context) {
        Font propFont = UIManager
                              .getFont(context.getRegion().getName() + ".font");
        if (propFont != null) {
            // if font property got a value then return it
            return propFont;
        }
        return font;
    }

    /**
     * Returns the X thickness to use for this GTKStyle.
     *
     * @return x thickness.
     */
    int getXThickness() {
        return xThickness;
    }

    /**
     * Returns the Y thickness to use for this GTKStyle.
     *
     * @return y thickness.
     */
    int getYThickness() {
        return yThickness;
    }

    /**
     * Returns the Insets. If <code>insets</code> is non-null the resulting
     * insets will be placed in it, otherwise a new Insets object will be
     * created and returned.
     *
     * @param state SynthContext identifying requestor
     * @param insets Where to place Insets
     * @return Insets.
     */
    @Override
    public Insets getInsets(SynthContext state, Insets insets) {
        Region id = state.getRegion();
        JComponent component = state.getComponent();
        String name = (id.isSubregion()) ? null : component.getName();

        if (insets == null) {
            insets = new Insets(0, 0, 0, 0);
        } else {
            insets.top = insets.bottom = insets.left = insets.right = 0;
        }

        if (id == Region.ARROW_BUTTON || id == Region.BUTTON ||
                id == Region.TOGGLE_BUTTON) {
            if ("Spinner.previousButton" == name ||
                    "Spinner.nextButton" == name) {
                return getSimpleInsets(state, insets, 1);
            } else {
                return getButtonInsets(state, insets);
            }
        }
        else if (id == Region.CHECK_BOX || id == Region.RADIO_BUTTON) {
            return getRadioInsets(state, insets);
        }
        else if (id == Region.MENU_BAR) {
            return getMenuBarInsets(state, insets);
        }
        else if (id == Region.MENU ||
                 id == Region.MENU_ITEM ||
                 id == Region.CHECK_BOX_MENU_ITEM ||
                 id == Region.RADIO_BUTTON_MENU_ITEM) {
            return getMenuItemInsets(state, insets);
        }
        else if (id == Region.FORMATTED_TEXT_FIELD) {
            return getTextFieldInsets(state, insets);
        }
        else if (id == Region.INTERNAL_FRAME) {
            insets = Metacity.INSTANCE.getBorderInsets(state, insets);
        }
        else if (id == Region.LABEL) {
            if ("TableHeader.renderer" == name) {
                return getButtonInsets(state, insets);
            }
            else if (component instanceof ListCellRenderer) {
                return getTextFieldInsets(state, insets);
            }
            else if ("Tree.cellRenderer" == name) {
                return getSimpleInsets(state, insets, 1);
            }
        }
        else if (id == Region.OPTION_PANE) {
            return getSimpleInsets(state, insets, 6);
        }
        else if (id == Region.POPUP_MENU) {
            return getSimpleInsets(state, insets, 2);
        }
        else if (id == Region.PROGRESS_BAR || id == Region.SLIDER ||
                 id == Region.TABBED_PANE  || id == Region.TABBED_PANE_CONTENT ||
                 id == Region.TOOL_BAR     ||
                 id == Region.TOOL_BAR_DRAG_WINDOW ||
                 id == Region.TOOL_TIP) {
            return getThicknessInsets(state, insets);
        }
        else if (id == Region.SCROLL_BAR) {
            return getScrollBarInsets(state, insets);
        }
        else if (id == Region.SLIDER_TRACK) {
            return getSliderTrackInsets(state, insets);
        }
        else if (id == Region.TABBED_PANE_TAB) {
            return getTabbedPaneTabInsets(state, insets);
        }
        else if (id == Region.TEXT_FIELD || id == Region.PASSWORD_FIELD) {
            if (name == "Tree.cellEditor") {
                return getSimpleInsets(state, insets, 1);
            }
            return getTextFieldInsets(state, insets);
        } else if (id == Region.SEPARATOR ||
                   id == Region.POPUP_MENU_SEPARATOR ||
                   id == Region.TOOL_BAR_SEPARATOR) {
            return getSeparatorInsets(state, insets);
        } else if (id == GTKEngine.CustomRegion.TITLED_BORDER) {
            return getThicknessInsets(state, insets);
        }
        return insets;
    }

    private Insets getButtonInsets(SynthContext context, Insets insets) {
        // The following calculations are derived from gtkbutton.c
        // (GTK+ version 2.8.20), gtk_button_size_allocate() method.
        int CHILD_SPACING = 1;
        int focusSize = getClassSpecificIntValue(context, "focus-line-width",1);
        int focusPad = getClassSpecificIntValue(context, "focus-padding", 1);
        int xThickness = getXThickness();
        int yThickness = getYThickness();
        int w = focusSize + focusPad + xThickness + CHILD_SPACING;
        int h = focusSize + focusPad + yThickness + CHILD_SPACING;
        insets.left = insets.right = w;
        insets.top = insets.bottom = h;

        Component component = context.getComponent();
        if ((component instanceof JButton) &&
            !(component.getParent() instanceof JToolBar) &&
            ((JButton)component).isDefaultCapable())
        {
            // Include the default border insets, but only for JButtons
            // that are default capable.  Note that
            // JButton.getDefaultCapable() returns true by default, but
            // GtkToolButtons are never default capable, so we skip this
            // step if the button is contained in a toolbar.
            Insets defaultInsets = getClassSpecificInsetsValue(context,
                          "default-border", BUTTON_DEFAULT_BORDER_INSETS);
            insets.left += defaultInsets.left;
            insets.right += defaultInsets.right;
            insets.top += defaultInsets.top;
            insets.bottom += defaultInsets.bottom;
        }

        return insets;
    }

    /*
     * This is used for both RADIO_BUTTON and CHECK_BOX.
     */
    private Insets getRadioInsets(SynthContext context, Insets insets) {
        // The following calculations are derived from gtkcheckbutton.c
        // (GTK+ version 2.8.20), gtk_check_button_size_allocate() method.
        int focusSize =
            getClassSpecificIntValue(context, "focus-line-width", 1);
        int focusPad =
            getClassSpecificIntValue(context, "focus-padding", 1);
        int totalFocus = focusSize + focusPad;

        // Note: GTKIconFactory.DelegateIcon will have already included the
        // "indicator-spacing" value in the size of the indicator icon,
        // which explains why we use zero as the left inset (or right inset
        // in the RTL case); see 6489585 for more details.
        insets.top    = totalFocus;
        insets.bottom = totalFocus;
        if (context.getComponent().getComponentOrientation().isLeftToRight()) {
            insets.left  = 0;
            insets.right = totalFocus;
        } else {
            insets.left  = totalFocus;
            insets.right = 0;
        }

        return insets;
    }

    private Insets getMenuBarInsets(SynthContext context, Insets insets) {
        // The following calculations are derived from gtkmenubar.c
        // (GTK+ version 2.8.20), gtk_menu_bar_size_allocate() method.
        int internalPadding = getClassSpecificIntValue(context,
                                                       "internal-padding", 1);
        int xThickness = getXThickness();
        int yThickness = getYThickness();
        insets.left = insets.right = xThickness + internalPadding;
        insets.top = insets.bottom = yThickness + internalPadding;
        return insets;
    }

    private Insets getMenuItemInsets(SynthContext context, Insets insets) {
        // The following calculations are derived from gtkmenuitem.c
        // (GTK+ version 2.8.20), gtk_menu_item_size_allocate() method.
        int horizPadding = getClassSpecificIntValue(context,
                                                    "horizontal-padding", 3);
        int xThickness = getXThickness();
        int yThickness = getYThickness();
        insets.left = insets.right = xThickness + horizPadding;
        insets.top = insets.bottom = yThickness;
        return insets;
    }

    private Insets getThicknessInsets(SynthContext context, Insets insets) {
        insets.left = insets.right = getXThickness();
        insets.top = insets.bottom = getYThickness();
        return insets;
    }

    private Insets getSeparatorInsets(SynthContext context, Insets insets) {
        int horizPadding = 0;
        if (context.getRegion() == Region.POPUP_MENU_SEPARATOR) {
            horizPadding =
                getClassSpecificIntValue(context, "horizontal-padding", 3);
        }
        insets.right = insets.left = getXThickness() + horizPadding;
        insets.top = insets.bottom = getYThickness();
        return insets;
    }

    private Insets getSliderTrackInsets(SynthContext context, Insets insets) {
        int focusSize = getClassSpecificIntValue(context, "focus-line-width", 1);
        int focusPad = getClassSpecificIntValue(context, "focus-padding", 1);
        insets.top = insets.bottom =
                insets.left = insets.right = focusSize + focusPad;
        return insets;
    }

    private Insets getSimpleInsets(SynthContext context, Insets insets, int n) {
        insets.top = insets.bottom = insets.right = insets.left = n;
        return insets;
    }

    private Insets getTabbedPaneTabInsets(SynthContext context, Insets insets) {
        int xThickness = getXThickness();
        int yThickness = getYThickness();
        int focusSize = getClassSpecificIntValue(context, "focus-line-width",1);
        int pad = 2;

        insets.left = insets.right = focusSize + pad + xThickness;
        insets.top = insets.bottom = focusSize + pad + yThickness;
        return insets;
    }

    // NOTE: this is called for ComboBox, and FormattedTextField also
    private Insets getTextFieldInsets(SynthContext context, Insets insets) {
        insets = getClassSpecificInsetsValue(context, "inner-border",
                                    getSimpleInsets(context, insets, 2));

        int xThickness = getXThickness();
        int yThickness = getYThickness();
        boolean interiorFocus =
                getClassSpecificBoolValue(context, "interior-focus", true);
        int focusSize = 0;

        if (!interiorFocus) {
            focusSize = getClassSpecificIntValue(context, "focus-line-width",1);
        }

        insets.left   += focusSize + xThickness;
        insets.right  += focusSize + xThickness;
        insets.top    += focusSize + yThickness;
        insets.bottom += focusSize + yThickness;
        return insets;
    }

    private Insets getScrollBarInsets(SynthContext context, Insets insets) {
        int troughBorder =
            getClassSpecificIntValue(context, "trough-border", 1);
        insets.left = insets.right = insets.top = insets.bottom = troughBorder;

        JComponent c = context.getComponent();
        if (c.getParent() instanceof JScrollPane) {
            // This scrollbar is part of a scrollpane; use only the
            // "scrollbar-spacing" style property to determine the padding
            // between the scrollbar and its parent scrollpane.
            int spacing =
                getClassSpecificIntValue(WidgetType.SCROLL_PANE,
                                         "scrollbar-spacing", 3);
            if (((JScrollBar)c).getOrientation() == JScrollBar.HORIZONTAL) {
                insets.top += spacing;
            } else {
                if (c.getComponentOrientation().isLeftToRight()) {
                    insets.left += spacing;
                } else {
                    insets.right += spacing;
                }
            }
        } else {
            // This is a standalone scrollbar; leave enough room for the
            // focus line in addition to the trough border.
            if (c.isFocusable()) {
                int focusSize =
                    getClassSpecificIntValue(context, "focus-line-width", 1);
                int focusPad =
                    getClassSpecificIntValue(context, "focus-padding", 1);
                int totalFocus = focusSize + focusPad;
                insets.left   += totalFocus;
                insets.right  += totalFocus;
                insets.top    += totalFocus;
                insets.bottom += totalFocus;
            }
        }
        return insets;
    }

    /**
     * Returns the value for a class specific property for a particular
     * WidgetType.  This method is useful in those cases where we need to
     * fetch a value for a Region that is not associated with the component
     * currently in use (e.g. we need to figure out the insets for a
     * SCROLL_BAR, but certain values can only be extracted from a
     * SCROLL_PANE region).
     *
     * @param wt WidgetType for which to fetch the value
     * @param key Key identifying class specific value
     * @return Value, or null if one has not been defined
     */
    private static Object getClassSpecificValue(WidgetType wt, String key) {
        synchronized (UNIXToolkit.GTK_LOCK) {
            return nativeGetClassValue(wt.ordinal(), key);
        }
    }

    /**
     * Convenience method to get a class specific integer value for
     * a particular WidgetType.
     *
     * @param wt WidgetType for which to fetch the value
     * @param key Key identifying class specific value
     * @param defaultValue Returned if there is no value for the specified
     *        type
     * @return Value, or defaultValue if <code>key</code> is not defined
     */
    private static int getClassSpecificIntValue(WidgetType wt, String key,
                                                int defaultValue)
    {
        Object value = getClassSpecificValue(wt, key);
        if (value instanceof Number) {
            return ((Number)value).intValue();
        }
        return defaultValue;
    }

    /**
     * Returns the value for a class specific property. A class specific value
     * is a value that will be picked up based on class hierarchy.
     *
     * @param key Key identifying class specific value
     * @return Value, or null if one has not been defined.
     */
    Object getClassSpecificValue(String key) {
        synchronized (sun.awt.UNIXToolkit.GTK_LOCK) {
            return nativeGetClassValue(widgetType, key);
        }
    }

    /**
     * Convenience method to get a class specific integer value.
     *
     * @param context SynthContext identifying requestor
     * @param key Key identifying class specific value
     * @param defaultValue Returned if there is no value for the specified
     *        type
     * @return Value, or defaultValue if <code>key</code> is not defined
     */
    int getClassSpecificIntValue(SynthContext context, String key,
                                 int defaultValue)
    {
        Object value = getClassSpecificValue(key);

        if (value instanceof Number) {
            return ((Number)value).intValue();
        }
        return defaultValue;
    }

    /**
     * Convenience method to get a class specific Insets value.
     *
     * @param context SynthContext identifying requestor
     * @param key Key identifying class specific value
     * @param defaultValue Returned if there is no value for the specified
     *        type
     * @return Value, or defaultValue if <code>key</code> is not defined
     */
    Insets getClassSpecificInsetsValue(SynthContext context, String key,
                                       Insets defaultValue)
    {
        Object value = getClassSpecificValue(key);

        if (value instanceof Insets) {
            return (Insets)value;
        }
        return defaultValue;
    }

    /**
     * Convenience method to get a class specific Boolean value.
     *
     * @param context SynthContext identifying requestor
     * @param key Key identifying class specific value
     * @param defaultValue Returned if there is no value for the specified
     *        type
     * @return Value, or defaultValue if <code>key</code> is not defined
     */
    boolean getClassSpecificBoolValue(SynthContext context, String key,
                                      boolean defaultValue)
    {
        Object value = getClassSpecificValue(key);

        if (value instanceof Boolean) {
            return ((Boolean)value).booleanValue();
        }
        return defaultValue;
    }

    /**
     * Returns the value to initialize the opacity property of the Component
     * to. A Style should NOT assume the opacity will remain this value, the
     * developer may reset it or override it.
     *
     * @param context SynthContext identifying requestor
     * @return opaque Whether or not the JComponent is opaque.
     */
    @Override
    public boolean isOpaque(SynthContext context) {
        Region region = context.getRegion();
        if (region == Region.COMBO_BOX ||
              region == Region.DESKTOP_PANE ||
              region == Region.DESKTOP_ICON ||
              region == Region.INTERNAL_FRAME ||
              region == Region.LIST ||
              region == Region.MENU_BAR ||
              region == Region.PANEL ||
              region == Region.POPUP_MENU ||
              region == Region.PROGRESS_BAR ||
              region == Region.ROOT_PANE ||
              region == Region.SCROLL_PANE ||
              region == Region.SPLIT_PANE_DIVIDER ||
              region == Region.TABLE ||
              region == Region.TEXT_AREA ||
              region == Region.TOOL_BAR_DRAG_WINDOW ||
              region == Region.TOOL_TIP ||
              region == Region.TREE ||
              region == Region.VIEWPORT ||
              region == Region.TEXT_PANE) {
            return true;
        }
        if (!GTKLookAndFeel.is3()) {
            if (region == Region.EDITOR_PANE ||
                  region == Region.FORMATTED_TEXT_FIELD ||
                  region == Region.PASSWORD_FIELD ||
                  region == Region.SPINNER ||
                  region == Region.TEXT_FIELD) {
                return true;
            }
        }
        Component c = context.getComponent();
        String name = c.getName();
        if (name == "ComboBox.renderer" || name == "ComboBox.listRenderer") {
            return true;
        }
        return false;
    }

    @Override
    public Object get(SynthContext context, Object key) {
        // See if this is a class specific value.
        String classKey = CLASS_SPECIFIC_MAP.get(key);
        if (classKey != null) {
            Object value = getClassSpecificValue(classKey);
            if (value != null) {
                //This is a workaround as the "slider-length" property has been
                //deprecated for GtkScale from gtk 3.20, so default value of 31
                //is used and makes redering of slider wrong. Value 14 is being
                //used as default value for Slider.thumbHeight is 14 and making
                //width 14 as well makes slider thumb render in proper shape
                if ("Slider.thumbWidth".equals(key) && value.equals(31)) {
                    return 14;
                }
                return value;
            }
        }

        // Is it a specific value ?
        if (key == "ScrollPane.viewportBorderInsets") {
            return getThicknessInsets(context, new Insets(0, 0, 0, 0));
        }
        else if (key == "Slider.tickColor") {
            return getColorForState(context, ColorType.FOREGROUND);
        }
        else if (key == "ScrollBar.minimumThumbSize") {
            //This is a workaround as the "min-slider-length" property has been
            //deprecated for GtkScrollBar from gtk 3.20, so default value of 21
            //is used and makes ScrollBar thumb very small. Value 40 is being
            //used as this is the value mentioned in css files
            int len =
                getClassSpecificIntValue(context, "min-slider-length", 21);
            if (len == 21) {
                len = 40;
            }
            JScrollBar sb = (JScrollBar)context.getComponent();
            if (sb.getOrientation() == JScrollBar.HORIZONTAL) {
                return new DimensionUIResource(len, 0);
            } else {
                return new DimensionUIResource(0, len);
            }
        }
        else if (key == "Separator.thickness") {
            JSeparator sep = (JSeparator)context.getComponent();
            if (getClassSpecificBoolValue(context, "wide-separators", false)) {
                if (sep.getOrientation() == JSeparator.HORIZONTAL) {
                    return getClassSpecificIntValue(context,
                            "separator-height", 0);
                } else {
                    return getClassSpecificIntValue(context,
                            "separator-width", 0);
                }
            }
            if (sep.getOrientation() == JSeparator.HORIZONTAL) {
                return getYThickness();
            } else {
                return getXThickness();
            }
        }
        else if (key == "ToolBar.separatorSize") {
            if (getClassSpecificBoolValue(context, "wide-separators", false)) {
                return new DimensionUIResource(
                    getClassSpecificIntValue(context, "separator-width", 2),
                    getClassSpecificIntValue(context, "separator-height", 2)
                );
            }
            int size = getClassSpecificIntValue(WidgetType.TOOL_BAR,
                                                "space-size", 12);
            return new DimensionUIResource(size, size);
        }
        else if (key == "ScrollBar.buttonSize") {
            JScrollBar sb = (JScrollBar)context.getComponent().getParent();
            boolean horiz = (sb.getOrientation() == JScrollBar.HORIZONTAL);
            WidgetType wt = horiz ?
                WidgetType.HSCROLL_BAR : WidgetType.VSCROLL_BAR;
            int sliderWidth = getClassSpecificIntValue(wt, "slider-width", 14);
            int stepperSize = getClassSpecificIntValue(wt, "stepper-size", 14);
            return horiz ?
                new DimensionUIResource(stepperSize, sliderWidth) :
                new DimensionUIResource(sliderWidth, stepperSize);
        }
        else if (key == "ArrowButton.size") {
            String name = context.getComponent().getName();
            if (name != null && name.startsWith("Spinner")) {
                // Believe it or not, the size of a spinner arrow button is
                // dependent upon the size of the spinner's font.  These
                // calculations come from gtkspinbutton.c (version 2.8.20),
                // spin_button_get_arrow_size() method.
                String pangoFontName;
                synchronized (sun.awt.UNIXToolkit.GTK_LOCK) {
                    pangoFontName =
                        nativeGetPangoFontName(WidgetType.SPINNER.ordinal());
                }
                int arrowSize = (pangoFontName != null) ?
                    PangoFonts.getFontSize(pangoFontName) : 10;
                return (arrowSize + (getXThickness() * 2));
            }
            // For all other kinds of arrow buttons (e.g. combobox arrow
            // buttons), we will simply fall back on the value of
            // ArrowButton.size as defined in the UIDefaults for
            // GTKLookAndFeel when we call UIManager.get() below...
        }
        else if ("CheckBox.iconTextGap".equals(key) ||
                 "RadioButton.iconTextGap".equals(key))
        {
            // The iconTextGap value needs to include "indicator-spacing"
            // and it also needs to leave enough space for the focus line,
            // which falls between the indicator icon and the text.
            // See getRadioInsets() and 6489585 for more details.
            int indicatorSpacing =
                getClassSpecificIntValue(context, "indicator-spacing", 2);
            int focusSize =
                getClassSpecificIntValue(context, "focus-line-width", 1);
            int focusPad =
                getClassSpecificIntValue(context, "focus-padding", 1);
            return indicatorSpacing + focusSize + focusPad;
        } else if (GTKLookAndFeel.is3() && "ComboBox.forceOpaque".equals(key)) {
            return true;
        } else if ("Tree.expanderSize".equals(key)) {
            Object value = getClassSpecificValue("expander-size");
            if (value instanceof Integer) {
                return (Integer)value + 4;
            }
            return null;
        }

        // Is it a stock icon ?
        GTKStockIcon stockIcon = null;
        synchronized (ICONS_MAP) {
            stockIcon = ICONS_MAP.get(key);
        }

        if (stockIcon != null) {
            return stockIcon;
        }

        // Is it another kind of value ?
        if (key != "engine") {
            // For backward compatibility we'll fallback to the UIManager.
            // We don't go to the UIManager for engine as the engine is GTK
            // specific.
            Object value = UIManager.get(key);
            if (key == "Table.rowHeight") {
                int focusLineWidth = getClassSpecificIntValue(context,
                        "focus-line-width", 0);
                if (value == null && focusLineWidth > 0) {
                    value = Integer.valueOf(16 + 2 * focusLineWidth);
                }
            }
            return value;
        }

        // Don't call super, we don't want to pick up defaults from
        // SynthStyle.
        return null;
    }

    private Icon getStockIcon(SynthContext context, String key, int type) {
        TextDirection direction = TextDirection.LTR;

        if (context != null) {
            ComponentOrientation co = context.getComponent().
                                              getComponentOrientation();

            if (co != null && !co.isLeftToRight()) {
                direction = TextDirection.RTL;
            }
        }

        // First try loading a theme-specific icon using the native
        // GTK libraries (native GTK handles the resizing for us).
        Icon icon = getStyleSpecificIcon(key, direction, type);
        if (icon != null) {
            return icon;
        }

        // In a failure case where native GTK (unexpectedly) returns a
        // null icon, we can try loading a default icon as a fallback.
        String propName = ICON_PROPERTY_PREFIX + key + '.' + type + '.' +
                          (direction == TextDirection.RTL ? "rtl" : "ltr");
        Image img = (Image)
            Toolkit.getDefaultToolkit().getDesktopProperty(propName);
        if (img != null) {
            return new ImageIcon(img);
        }

        // In an extreme failure situation, just return null (callers are
        // already prepared to handle a null icon, so the worst that can
        // happen is that an icon won't be included in the button/dialog).
        return null;
    }

    private Icon getStyleSpecificIcon(String key,
                                      TextDirection direction, int type)
    {
        UNIXToolkit tk = (UNIXToolkit)Toolkit.getDefaultToolkit();
        Image img =
            tk.getStockIcon(widgetType, key, type, direction.ordinal(), null);
        return (img != null) ? new ImageIcon(img) : null;
    }

    static class GTKStockIconInfo {
        private static Map<String,Integer> ICON_TYPE_MAP;
        private static final Object ICON_SIZE_KEY = new StringBuffer("IconSize");

        private static Dimension[] getIconSizesMap() {
            AppContext appContext = AppContext.getAppContext();
            Dimension[] iconSizes = (Dimension[])appContext.get(ICON_SIZE_KEY);

            if (iconSizes == null) {
                iconSizes = new Dimension[7];
                iconSizes[0] = null;                  // GTK_ICON_SIZE_INVALID
                iconSizes[1] = new Dimension(16, 16); // GTK_ICON_SIZE_MENU
                iconSizes[2] = new Dimension(18, 18); // GTK_ICON_SIZE_SMALL_TOOLBAR
                iconSizes[3] = new Dimension(24, 24); // GTK_ICON_SIZE_LARGE_TOOLBAR
                iconSizes[4] = new Dimension(20, 20); // GTK_ICON_SIZE_BUTTON
                iconSizes[5] = new Dimension(32, 32); // GTK_ICON_SIZE_DND
                iconSizes[6] = new Dimension(48, 48); // GTK_ICON_SIZE_DIALOG
                appContext.put(ICON_SIZE_KEY, iconSizes);
            }
            return iconSizes;
        }

        /**
         * Return the size of a particular icon type (logical size)
         *
         * @param type icon type (GtkIconSize value)
         * @return a Dimension object, or null if lsize is invalid
         */
        public static Dimension getIconSize(int type) {
            Dimension[] iconSizes = getIconSizesMap();
            return type >= 0 && type < iconSizes.length ?
                iconSizes[type] : null;
        }

        /**
         * Change icon size in a type to size mapping. This is called by code
         * that parses the gtk-icon-sizes setting
         *
         * @param type icon type (GtkIconSize value)
         * @param w the new icon width
         * @param h the new icon height
         */
        public static void setIconSize(int type, int w, int h) {
            Dimension[] iconSizes = getIconSizesMap();
            if (type >= 0 && type < iconSizes.length) {
                iconSizes[type] = new Dimension(w, h);
            }
        }

        /**
         * Return icon type (GtkIconSize value) given a symbolic name which can
         * occur in a theme file.
         *
         * @param size symbolic name, e.g. gtk-button
         * @return icon type. Valid types are 1 to 6
         */
        public static int getIconType(String size) {
            if (size == null) {
                return UNDEFINED;
            }
            if (ICON_TYPE_MAP == null) {
                initIconTypeMap();
            }
            Integer n = ICON_TYPE_MAP.get(size);
            return n != null ? n.intValue() : UNDEFINED;
        }

        private static void initIconTypeMap() {
            ICON_TYPE_MAP = new HashMap<String,Integer>();
            ICON_TYPE_MAP.put("gtk-menu", Integer.valueOf(1));
            ICON_TYPE_MAP.put("gtk-small-toolbar", Integer.valueOf(2));
            ICON_TYPE_MAP.put("gtk-large-toolbar", Integer.valueOf(3));
            ICON_TYPE_MAP.put("gtk-button", Integer.valueOf(4));
            ICON_TYPE_MAP.put("gtk-dnd", Integer.valueOf(5));
            ICON_TYPE_MAP.put("gtk-dialog", Integer.valueOf(6));
        }

    }

    /**
     * An Icon that is fetched using getStockIcon.
     */
    private static class GTKStockIcon implements SynthIcon {
        private String key;
        private int size;
        private boolean loadedLTR;
        private boolean loadedRTL;
        private Icon ltrIcon;
        private Icon rtlIcon;
        private SynthStyle style;

        GTKStockIcon(String key, int size) {
            this.key = key;
            this.size = size;
        }

        public void paintIcon(SynthContext context, Graphics g, int x,
                              int y, int w, int h) {
            Icon icon = getIcon(context);

            if (icon != null) {
                if (context == null) {
                    icon.paintIcon(null, g, x, y);
                }
                else {
                    icon.paintIcon(context.getComponent(), g, x, y);
                }
            }
        }

        public int getIconWidth(SynthContext context) {
            Icon icon = getIcon(context);

            if (icon != null) {
                return icon.getIconWidth();
            }
            return 0;
        }

        public int getIconHeight(SynthContext context) {
            Icon icon = getIcon(context);

            if (icon != null) {
                return icon.getIconHeight();
            }
            return 0;
        }

        private Icon getIcon(SynthContext context) {
            if (context != null) {
                ComponentOrientation co = context.getComponent().
                                                  getComponentOrientation();
                SynthStyle style = context.getStyle();

                if (style != this.style) {
                    this.style = style;
                    loadedLTR = loadedRTL = false;
                }
                if (co == null || co.isLeftToRight()) {
                    if (!loadedLTR) {
                        loadedLTR = true;
                        ltrIcon = ((GTKStyle)context.getStyle()).
                                  getStockIcon(context, key, size);
                    }
                    return ltrIcon;
                }
                else if (!loadedRTL) {
                    loadedRTL = true;
                    rtlIcon = ((GTKStyle)context.getStyle()).
                              getStockIcon(context, key,size);
                }
                return rtlIcon;
            }
            return ltrIcon;
        }
    }

    /**
     * GTKLazyValue is a slimmed down version of <code>ProxyLaxyValue</code>.
     * The code is duplicate so that it can get at the package private
     * classes in gtk.
     */
    static class GTKLazyValue implements UIDefaults.LazyValue {
        /**
         * Name of the class to create.
         */
        private String className;
        private String methodName;

        GTKLazyValue(String name) {
            this(name, null);
        }

        GTKLazyValue(String name, String methodName) {
            this.className = name;
            this.methodName = methodName;
        }

        @SuppressWarnings("deprecation")
        public Object createValue(UIDefaults table) {
            try {
                Class<?> c = Class.forName(className, true,Thread.currentThread().
                                           getContextClassLoader());

                if (methodName == null) {
                    return c.newInstance();
                }
                Method m = c.getMethod(methodName, (Class<?>[])null);

                return m.invoke(c, (Object[])null);
            } catch (ReflectiveOperationException e) {
            }
            return null;
        }
    }

    static {
        CLASS_SPECIFIC_MAP = new HashMap<String,String>();
        CLASS_SPECIFIC_MAP.put("Slider.thumbHeight", "slider-width");
        CLASS_SPECIFIC_MAP.put("Slider.thumbWidth", "slider-length");
        CLASS_SPECIFIC_MAP.put("Slider.trackBorder", "trough-border");
        CLASS_SPECIFIC_MAP.put("SplitPane.size", "handle-size");
        CLASS_SPECIFIC_MAP.put("ScrollBar.thumbHeight", "slider-width");
        CLASS_SPECIFIC_MAP.put("ScrollBar.width", "slider-width");
        CLASS_SPECIFIC_MAP.put("TextArea.caretForeground", "cursor-color");
        CLASS_SPECIFIC_MAP.put("TextArea.caretAspectRatio", "cursor-aspect-ratio");
        CLASS_SPECIFIC_MAP.put("TextField.caretForeground", "cursor-color");
        CLASS_SPECIFIC_MAP.put("TextField.caretAspectRatio", "cursor-aspect-ratio");
        CLASS_SPECIFIC_MAP.put("PasswordField.caretForeground", "cursor-color");
        CLASS_SPECIFIC_MAP.put("PasswordField.caretAspectRatio", "cursor-aspect-ratio");
        CLASS_SPECIFIC_MAP.put("FormattedTextField.caretForeground", "cursor-color");
        CLASS_SPECIFIC_MAP.put("FormattedTextField.caretAspectRatio", "cursor-aspect-");
        CLASS_SPECIFIC_MAP.put("TextPane.caretForeground", "cursor-color");
        CLASS_SPECIFIC_MAP.put("TextPane.caretAspectRatio", "cursor-aspect-ratio");
        CLASS_SPECIFIC_MAP.put("EditorPane.caretForeground", "cursor-color");
        CLASS_SPECIFIC_MAP.put("EditorPane.caretAspectRatio", "cursor-aspect-ratio");

        ICONS_MAP = new HashMap<String, GTKStockIcon>();
        ICONS_MAP.put("FileChooser.cancelIcon", new GTKStockIcon("gtk-cancel", 4));
        ICONS_MAP.put("FileChooser.okIcon",     new GTKStockIcon("gtk-ok",     4));
        ICONS_MAP.put("OptionPane.yesIcon", new GTKStockIcon("gtk-yes", 4));
        ICONS_MAP.put("OptionPane.noIcon", new GTKStockIcon("gtk-no", 4));
        ICONS_MAP.put("OptionPane.cancelIcon", new GTKStockIcon("gtk-cancel", 4));
        ICONS_MAP.put("OptionPane.okIcon", new GTKStockIcon("gtk-ok", 4));

        //check whether the gtk version is >= 3.10 as the Icon names were
        //changed from this version
        UNIXToolkit tk = (UNIXToolkit)Toolkit.getDefaultToolkit();
        if (tk.checkGtkVersion(3, 10, 0)) {
            ICONS_MAP.put("OptionPane.errorIcon", new GTKStockIcon("dialog-error", 6));
            ICONS_MAP.put("OptionPane.informationIcon", new GTKStockIcon("dialog-information", 6));
            ICONS_MAP.put("OptionPane.warningIcon", new GTKStockIcon("dialog-warning", 6));
            ICONS_MAP.put("OptionPane.questionIcon", new GTKStockIcon("dialog-question", 6));
        } else {
            ICONS_MAP.put("OptionPane.errorIcon", new GTKStockIcon("gtk-dialog-error", 6));
            ICONS_MAP.put("OptionPane.informationIcon", new GTKStockIcon("gtk-dialog-info", 6));
            ICONS_MAP.put("OptionPane.warningIcon", new GTKStockIcon("gtk-dialog-warning", 6));
            ICONS_MAP.put("OptionPane.questionIcon", new GTKStockIcon("gtk-dialog-question", 6));
        }
    }
}
