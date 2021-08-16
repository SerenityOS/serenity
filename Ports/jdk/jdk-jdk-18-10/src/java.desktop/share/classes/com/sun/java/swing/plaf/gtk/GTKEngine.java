/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.image.*;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.plaf.synth.*;

import com.sun.java.swing.plaf.gtk.GTKConstants.ArrowType;
import com.sun.java.swing.plaf.gtk.GTKConstants.ExpanderStyle;
import com.sun.java.swing.plaf.gtk.GTKConstants.Orientation;
import com.sun.java.swing.plaf.gtk.GTKConstants.PositionType;
import com.sun.java.swing.plaf.gtk.GTKConstants.ShadowType;
import com.sun.java.swing.plaf.gtk.GTKConstants.TextDirection;

import sun.awt.image.SunWritableRaster;
import sun.swing.ImageCache;

/**
 * GTKEngine delegates all painting job to native GTK libraries.
 *
 * Painting with GTKEngine looks like this:
 * First, startPainting() is called. It prepares an offscreen buffer of the
 *   required size.
 * Then, any number of paintXXX() methods can be called. They effectively ignore
 *   the Graphics parameter and draw to the offscreen buffer.
 * Finally, finishPainting() should be called. It fills the data buffer passed
 *   in with the image data.
 *
 * @author Josh Outwater
 */
class GTKEngine {

    static final GTKEngine INSTANCE = new GTKEngine();

    /** Size of the image cache */
    private static final int CACHE_SIZE = 50;

    /** This enum mirrors that in gtk2_interface.h */
    static enum WidgetType {
        BUTTON, CHECK_BOX, CHECK_BOX_MENU_ITEM, COLOR_CHOOSER,
        COMBO_BOX, COMBO_BOX_ARROW_BUTTON, COMBO_BOX_TEXT_FIELD,
        DESKTOP_ICON, DESKTOP_PANE, EDITOR_PANE, FORMATTED_TEXT_FIELD,
        HANDLE_BOX, HPROGRESS_BAR,
        HSCROLL_BAR, HSCROLL_BAR_BUTTON_LEFT, HSCROLL_BAR_BUTTON_RIGHT,
        HSCROLL_BAR_TRACK, HSCROLL_BAR_THUMB,
        HSEPARATOR, HSLIDER, HSLIDER_TRACK, HSLIDER_THUMB, HSPLIT_PANE_DIVIDER,
        INTERNAL_FRAME, INTERNAL_FRAME_TITLE_PANE, IMAGE, LABEL, LIST, MENU,
        MENU_BAR, MENU_ITEM, MENU_ITEM_ACCELERATOR, OPTION_PANE, PANEL,
        PASSWORD_FIELD, POPUP_MENU, POPUP_MENU_SEPARATOR,
        RADIO_BUTTON, RADIO_BUTTON_MENU_ITEM, ROOT_PANE, SCROLL_PANE,
        SPINNER, SPINNER_ARROW_BUTTON, SPINNER_TEXT_FIELD,
        SPLIT_PANE, TABBED_PANE, TABBED_PANE_TAB_AREA, TABBED_PANE_CONTENT,
        TABBED_PANE_TAB, TABLE, TABLE_HEADER, TEXT_AREA, TEXT_FIELD, TEXT_PANE,
        TITLED_BORDER,
        TOGGLE_BUTTON, TOOL_BAR, TOOL_BAR_DRAG_WINDOW, TOOL_BAR_SEPARATOR,
        TOOL_TIP, TREE, TREE_CELL, VIEWPORT, VPROGRESS_BAR,
        VSCROLL_BAR, VSCROLL_BAR_BUTTON_UP, VSCROLL_BAR_BUTTON_DOWN,
        VSCROLL_BAR_TRACK, VSCROLL_BAR_THUMB,
        VSEPARATOR, VSLIDER, VSLIDER_TRACK, VSLIDER_THUMB,
        VSPLIT_PANE_DIVIDER
    }

    /**
     * Representation of GtkSettings properties.
     * When we need more settings we can add them here and
     * to all implementations of getGTKSetting().
     */
    static enum Settings {
        GTK_FONT_NAME,
        GTK_ICON_SIZES,
        GTK_CURSOR_BLINK,
        GTK_CURSOR_BLINK_TIME
    }

    /* Custom regions are needed for representing regions that don't exist
     * in the original Region class.
     */
    static class CustomRegion extends Region {
        /*
         * TITLED_BORDER Region is mapped to GtkFrame class which can draw
         * titled borders around components.
         */
        static Region TITLED_BORDER = new CustomRegion("TitledBorder");

        private CustomRegion(String name) {
            super(name, null, false);
        }
    }


    private static HashMap<Region, Object> regionToWidgetTypeMap;
    private ImageCache cache = new ImageCache(CACHE_SIZE);
    private int x0, y0, w0, h0;
    private Graphics graphics;
    private Object[] cacheArgs;

    private native void native_paint_arrow(
            int widgetType, int state, int shadowType, String detail,
            int x, int y, int width, int height, int arrowType);
    private native void native_paint_box(
            int widgetType, int state, int shadowType, String detail,
            int x, int y, int width, int height, int synthState, int dir);
    private native void native_paint_box_gap(
            int widgetType, int state, int shadowType, String detail,
            int x, int y, int width, int height,
            int gapSide, int gapX, int gapWidth);
    private native void native_paint_check(
            int widgetType, int synthState, String detail,
            int x, int y, int width, int height);
    private native void native_paint_expander(
            int widgetType, int state, String detail,
            int x, int y, int width, int height, int expanderStyle);
    private native void native_paint_extension(
            int widgetType, int state, int shadowType, String detail,
            int x, int y, int width, int height, int placement);
    private native void native_paint_flat_box(
            int widgetType, int state, int shadowType, String detail,
            int x, int y, int width, int height, boolean hasFocus);
    private native void native_paint_focus(
            int widgetType, int state, String detail,
            int x, int y, int width, int height);
    private native void native_paint_handle(
            int widgetType, int state, int shadowType, String detail,
            int x, int y, int width, int height, int orientation);
    private native void native_paint_hline(
            int widgetType, int state, String detail,
            int x, int y, int width, int height);
    private native void native_paint_option(
            int widgetType, int synthState, String detail,
            int x, int y, int width, int height);
    private native void native_paint_shadow(
            int widgetType, int state, int shadowType, String detail,
            int x, int y, int width, int height, int synthState, int dir);
    private native void native_paint_slider(
            int widgetType, int state, int shadowType, String detail, int x,
            int y, int width, int height, int orientation, boolean hasFocus);
    private native void native_paint_vline(
            int widgetType, int state, String detail,
            int x, int y, int width, int height);
    private native void native_paint_background(
            int widgetType, int state, int x, int y, int width, int height);
    private native Object native_get_gtk_setting(int property);
    private native void nativeSetRangeValue(int widgetType, double value,
                                            double min, double max,
                                            double visible);

    private native void nativeStartPainting(int w, int h);
    private native int nativeFinishPainting(int[] buffer, int width, int height);
    private native void native_switch_theme();

    static {
        // Make sure the awt toolkit is loaded so we have access to native
        // methods.
        Toolkit.getDefaultToolkit();

        // Initialize regionToWidgetTypeMap
        regionToWidgetTypeMap = new HashMap<Region, Object>(50);
        regionToWidgetTypeMap.put(Region.ARROW_BUTTON, new WidgetType[] {
            WidgetType.SPINNER_ARROW_BUTTON,
            WidgetType.COMBO_BOX_ARROW_BUTTON,
            WidgetType.HSCROLL_BAR_BUTTON_LEFT,
            WidgetType.HSCROLL_BAR_BUTTON_RIGHT,
            WidgetType.VSCROLL_BAR_BUTTON_UP,
            WidgetType.VSCROLL_BAR_BUTTON_DOWN});
        regionToWidgetTypeMap.put(Region.BUTTON, WidgetType.BUTTON);
        regionToWidgetTypeMap.put(Region.CHECK_BOX, WidgetType.CHECK_BOX);
        regionToWidgetTypeMap.put(Region.CHECK_BOX_MENU_ITEM,
                                  WidgetType.CHECK_BOX_MENU_ITEM);
        regionToWidgetTypeMap.put(Region.COLOR_CHOOSER, WidgetType.COLOR_CHOOSER);
        regionToWidgetTypeMap.put(Region.FILE_CHOOSER, WidgetType.OPTION_PANE);
        regionToWidgetTypeMap.put(Region.COMBO_BOX, WidgetType.COMBO_BOX);
        regionToWidgetTypeMap.put(Region.DESKTOP_ICON, WidgetType.DESKTOP_ICON);
        regionToWidgetTypeMap.put(Region.DESKTOP_PANE, WidgetType.DESKTOP_PANE);
        regionToWidgetTypeMap.put(Region.EDITOR_PANE, WidgetType.EDITOR_PANE);
        regionToWidgetTypeMap.put(Region.FORMATTED_TEXT_FIELD, new WidgetType[] {
            WidgetType.FORMATTED_TEXT_FIELD, WidgetType.SPINNER_TEXT_FIELD});
        regionToWidgetTypeMap.put(GTKRegion.HANDLE_BOX, WidgetType.HANDLE_BOX);
        regionToWidgetTypeMap.put(Region.INTERNAL_FRAME,
                                  WidgetType.INTERNAL_FRAME);
        regionToWidgetTypeMap.put(Region.INTERNAL_FRAME_TITLE_PANE,
                                  WidgetType.INTERNAL_FRAME_TITLE_PANE);
        regionToWidgetTypeMap.put(Region.LABEL, new WidgetType[] {
            WidgetType.LABEL, WidgetType.COMBO_BOX_TEXT_FIELD});
        regionToWidgetTypeMap.put(Region.LIST, WidgetType.LIST);
        regionToWidgetTypeMap.put(Region.MENU, WidgetType.MENU);
        regionToWidgetTypeMap.put(Region.MENU_BAR, WidgetType.MENU_BAR);
        regionToWidgetTypeMap.put(Region.MENU_ITEM, WidgetType.MENU_ITEM);
        regionToWidgetTypeMap.put(Region.MENU_ITEM_ACCELERATOR,
                                  WidgetType.MENU_ITEM_ACCELERATOR);
        regionToWidgetTypeMap.put(Region.OPTION_PANE, WidgetType.OPTION_PANE);
        regionToWidgetTypeMap.put(Region.PANEL, WidgetType.PANEL);
        regionToWidgetTypeMap.put(Region.PASSWORD_FIELD,
                                  WidgetType.PASSWORD_FIELD);
        regionToWidgetTypeMap.put(Region.POPUP_MENU, WidgetType.POPUP_MENU);
        regionToWidgetTypeMap.put(Region.POPUP_MENU_SEPARATOR,
                                  WidgetType.POPUP_MENU_SEPARATOR);
        regionToWidgetTypeMap.put(Region.PROGRESS_BAR, new WidgetType[] {
            WidgetType.HPROGRESS_BAR, WidgetType.VPROGRESS_BAR});
        regionToWidgetTypeMap.put(Region.RADIO_BUTTON, WidgetType.RADIO_BUTTON);
        regionToWidgetTypeMap.put(Region.RADIO_BUTTON_MENU_ITEM,
                                  WidgetType.RADIO_BUTTON_MENU_ITEM);
        regionToWidgetTypeMap.put(Region.ROOT_PANE, WidgetType.ROOT_PANE);
        regionToWidgetTypeMap.put(Region.SCROLL_BAR, new WidgetType[] {
            WidgetType.HSCROLL_BAR, WidgetType.VSCROLL_BAR});
        regionToWidgetTypeMap.put(Region.SCROLL_BAR_THUMB, new WidgetType[] {
            WidgetType.HSCROLL_BAR_THUMB, WidgetType.VSCROLL_BAR_THUMB});
        regionToWidgetTypeMap.put(Region.SCROLL_BAR_TRACK, new WidgetType[] {
            WidgetType.HSCROLL_BAR_TRACK, WidgetType.VSCROLL_BAR_TRACK});
        regionToWidgetTypeMap.put(Region.SCROLL_PANE, WidgetType.SCROLL_PANE);
        regionToWidgetTypeMap.put(Region.SEPARATOR, new WidgetType[] {
            WidgetType.HSEPARATOR, WidgetType.VSEPARATOR});
        regionToWidgetTypeMap.put(Region.SLIDER, new WidgetType[] {
            WidgetType.HSLIDER, WidgetType.VSLIDER});
        regionToWidgetTypeMap.put(Region.SLIDER_THUMB, new WidgetType[] {
            WidgetType.HSLIDER_THUMB, WidgetType.VSLIDER_THUMB});
        regionToWidgetTypeMap.put(Region.SLIDER_TRACK, new WidgetType[] {
            WidgetType.HSLIDER_TRACK, WidgetType.VSLIDER_TRACK});
        regionToWidgetTypeMap.put(Region.SPINNER, WidgetType.SPINNER);
        regionToWidgetTypeMap.put(Region.SPLIT_PANE, WidgetType.SPLIT_PANE);
        regionToWidgetTypeMap.put(Region.SPLIT_PANE_DIVIDER, new WidgetType[] {
            WidgetType.HSPLIT_PANE_DIVIDER, WidgetType.VSPLIT_PANE_DIVIDER});
        regionToWidgetTypeMap.put(Region.TABBED_PANE, WidgetType.TABBED_PANE);
        regionToWidgetTypeMap.put(Region.TABBED_PANE_CONTENT,
                                  WidgetType.TABBED_PANE_CONTENT);
        regionToWidgetTypeMap.put(Region.TABBED_PANE_TAB,
                                  WidgetType.TABBED_PANE_TAB);
        regionToWidgetTypeMap.put(Region.TABBED_PANE_TAB_AREA,
                                  WidgetType.TABBED_PANE_TAB_AREA);
        regionToWidgetTypeMap.put(Region.TABLE, WidgetType.TABLE);
        regionToWidgetTypeMap.put(Region.TABLE_HEADER, WidgetType.TABLE_HEADER);
        regionToWidgetTypeMap.put(Region.TEXT_AREA, WidgetType.TEXT_AREA);
        regionToWidgetTypeMap.put(Region.TEXT_FIELD, new WidgetType[] {
            WidgetType.TEXT_FIELD, WidgetType.COMBO_BOX_TEXT_FIELD});
        regionToWidgetTypeMap.put(Region.TEXT_PANE, WidgetType.TEXT_PANE);
        regionToWidgetTypeMap.put(CustomRegion.TITLED_BORDER, WidgetType.TITLED_BORDER);
        regionToWidgetTypeMap.put(Region.TOGGLE_BUTTON, WidgetType.TOGGLE_BUTTON);
        regionToWidgetTypeMap.put(Region.TOOL_BAR, WidgetType.TOOL_BAR);
        regionToWidgetTypeMap.put(Region.TOOL_BAR_CONTENT, WidgetType.TOOL_BAR);
        regionToWidgetTypeMap.put(Region.TOOL_BAR_DRAG_WINDOW,
                                  WidgetType.TOOL_BAR_DRAG_WINDOW);
        regionToWidgetTypeMap.put(Region.TOOL_BAR_SEPARATOR,
                                  WidgetType.TOOL_BAR_SEPARATOR);
        regionToWidgetTypeMap.put(Region.TOOL_TIP, WidgetType.TOOL_TIP);
        regionToWidgetTypeMap.put(Region.TREE, WidgetType.TREE);
        regionToWidgetTypeMap.put(Region.TREE_CELL, WidgetType.TREE_CELL);
        regionToWidgetTypeMap.put(Region.VIEWPORT, WidgetType.VIEWPORT);
    }

    /** Translate Region and JComponent into WidgetType ordinals */
    static WidgetType getWidgetType(JComponent c, Region id) {
        Object value = regionToWidgetTypeMap.get(id);

        if (value instanceof WidgetType) {
            return (WidgetType)value;
        }

        WidgetType[] widgets = (WidgetType[])value;
        if (c == null ) {
            return widgets[0];
        }

        if (c instanceof JScrollBar) {
            return (((JScrollBar)c).getOrientation() == JScrollBar.HORIZONTAL) ?
                widgets[0] : widgets[1];
        } else if (c instanceof JSeparator) {
            JSeparator separator = (JSeparator)c;

            /* We should return correrct WidgetType if the seperator is inserted
             * in Menu/PopupMenu/ToolBar. BugID: 6465603
             */
            if (separator.getParent() instanceof JPopupMenu) {
                return WidgetType.POPUP_MENU_SEPARATOR;
            } else if (separator.getParent() instanceof JToolBar) {
                return WidgetType.TOOL_BAR_SEPARATOR;
            }

            return (separator.getOrientation() == JSeparator.HORIZONTAL) ?
                widgets[0] : widgets[1];
        } else if (c instanceof JSlider) {
            return (((JSlider)c).getOrientation() == JSlider.HORIZONTAL) ?
                widgets[0] : widgets[1];
        } else if (c instanceof JProgressBar) {
            return (((JProgressBar)c).getOrientation() == JProgressBar.HORIZONTAL) ?
                widgets[0] : widgets[1];
        } else if (c instanceof JSplitPane) {
            return (((JSplitPane)c).getOrientation() == JSplitPane.HORIZONTAL_SPLIT) ?
                widgets[1] : widgets[0];
        } else if (id == Region.LABEL) {
            /*
             * For all ListCellRenderers we will use COMBO_BOX_TEXT_FIELD widget
             * type because we can get correct insets. List items however won't be
             * drawn as a text entry (see GTKPainter.paintLabelBackground).
             */
            if (c instanceof ListCellRenderer) {
                return widgets[1];
            } else {
                return widgets[0];
            }
        } else if (id == Region.TEXT_FIELD) {
            String name = c.getName();
            if (name != null && name.startsWith("ComboBox")) {
                return widgets[1];
            } else {
                return widgets[0];
            }
        } else if (id == Region.FORMATTED_TEXT_FIELD) {
            String name = c.getName();
            if (name != null && name.startsWith("Spinner")) {
                return widgets[1];
            } else {
                return widgets[0];
            }
        } else if (id == Region.ARROW_BUTTON) {
            if (c.getParent() instanceof JScrollBar) {
                Integer prop = (Integer)
                    c.getClientProperty("__arrow_direction__");
                int dir = (prop != null) ?
                    prop.intValue() : SwingConstants.WEST;
                switch (dir) {
                case SwingConstants.WEST:
                    return WidgetType.HSCROLL_BAR_BUTTON_LEFT;
                case SwingConstants.EAST:
                    return WidgetType.HSCROLL_BAR_BUTTON_RIGHT;
                case SwingConstants.NORTH:
                    return WidgetType.VSCROLL_BAR_BUTTON_UP;
                case SwingConstants.SOUTH:
                    return WidgetType.VSCROLL_BAR_BUTTON_DOWN;
                default:
                    return null;
                }
            } else if (c.getParent() instanceof JComboBox) {
                return WidgetType.COMBO_BOX_ARROW_BUTTON;
            } else {
                return WidgetType.SPINNER_ARROW_BUTTON;
            }
        }

        return null;
    }

    private static int getTextDirection(SynthContext context) {
        TextDirection dir = TextDirection.NONE;
        JComponent comp = context.getComponent();
        if (comp != null) {
            ComponentOrientation co = comp.getComponentOrientation();
            if (co != null) {
                dir = co.isLeftToRight() ?
                    TextDirection.LTR : TextDirection.RTL;
            }
        }
        return dir.ordinal();
    }

    public void paintArrow(Graphics g, SynthContext context,
            Region id, int state, ShadowType shadowType, ArrowType direction,
            String detail, int x, int y, int w, int h) {

        state = GTKLookAndFeel.synthStateToGTKStateType(state).ordinal();
        int widget = getWidgetType(context.getComponent(), id).ordinal();
        native_paint_arrow(widget, state, shadowType.ordinal(),
                detail, x - x0, y - y0, w, h, direction.ordinal());
    }

    public void paintBox(Graphics g, SynthContext context,
            Region id, int state, ShadowType shadowType,
            String detail, int x, int y, int w, int h) {

        int gtkState =
            GTKLookAndFeel.synthStateToGTKStateType(state).ordinal();
        int synthState = context.getComponentState();
        int dir = getTextDirection(context);
        int widget = getWidgetType(context.getComponent(), id).ordinal();
        native_paint_box(widget, gtkState, shadowType.ordinal(),
                         detail, x - x0, y - y0, w, h, synthState, dir);
    }

    public void paintBoxGap(Graphics g, SynthContext context,
            Region id, int state, ShadowType shadowType,
            String detail, int x, int y, int w, int h,
            PositionType boxGapType, int tabBegin, int size) {

        state = GTKLookAndFeel.synthStateToGTKStateType(state).ordinal();
        int widget = getWidgetType(context.getComponent(), id).ordinal();
        native_paint_box_gap(widget, state, shadowType.ordinal(), detail,
                x - x0, y - y0, w, h, boxGapType.ordinal(), tabBegin, size);
    }

    public void paintCheck(Graphics g, SynthContext context,
            Region id, String detail, int x, int y, int w, int h) {

        int synthState = context.getComponentState();
        int widget = getWidgetType(context.getComponent(), id).ordinal();
        native_paint_check(widget, synthState, detail, x - x0, y - y0, w, h);
    }

    public void paintExpander(Graphics g, SynthContext context,
            Region id, int state, ExpanderStyle expanderStyle, String detail,
            int x, int y, int w, int h) {

        state = GTKLookAndFeel.synthStateToGTKStateType(state).ordinal();
        int widget = getWidgetType(context.getComponent(), id).ordinal();
        native_paint_expander(widget, state, detail, x - x0, y - y0, w, h,
                              expanderStyle.ordinal());
    }

    public void paintExtension(Graphics g, SynthContext context,
            Region id, int state, ShadowType shadowType, String detail,
            int x, int y, int w, int h, PositionType placement, int tabIndex) {

        state = GTKLookAndFeel.synthStateToGTKStateType(state).ordinal();
        int widget = getWidgetType(context.getComponent(), id).ordinal();
        native_paint_extension(widget, state, shadowType.ordinal(), detail,
                               x - x0, y - y0, w, h, placement.ordinal());
    }

    public void paintFlatBox(Graphics g, SynthContext context,
            Region id, int state, ShadowType shadowType, String detail,
            int x, int y, int w, int h, ColorType colorType) {

        state = GTKLookAndFeel.synthStateToGTKStateType(state).ordinal();
        int widget = getWidgetType(context.getComponent(), id).ordinal();
        native_paint_flat_box(widget, state, shadowType.ordinal(), detail,
                              x - x0, y - y0, w, h,
                              context.getComponent().hasFocus());
    }

    public void paintFocus(Graphics g, SynthContext context,
            Region id, int state, String detail, int x, int y, int w, int h) {

        state = GTKLookAndFeel.synthStateToGTKStateType(state).ordinal();
        int widget = getWidgetType(context.getComponent(), id).ordinal();
        native_paint_focus(widget, state, detail, x - x0, y - y0, w, h);
    }

    public void paintHandle(Graphics g, SynthContext context,
            Region id, int state, ShadowType shadowType, String detail,
            int x, int y, int w, int h, Orientation orientation) {

        state = GTKLookAndFeel.synthStateToGTKStateType(state).ordinal();
        int widget = getWidgetType(context.getComponent(), id).ordinal();
        native_paint_handle(widget, state, shadowType.ordinal(), detail,
                            x - x0, y - y0, w, h, orientation.ordinal());
    }

    public void paintHline(Graphics g, SynthContext context,
            Region id, int state, String detail, int x, int y, int w, int h) {

        state = GTKLookAndFeel.synthStateToGTKStateType(state).ordinal();
        int widget = getWidgetType(context.getComponent(), id).ordinal();
        native_paint_hline(widget, state, detail, x - x0, y - y0, w, h);
    }

    public void paintOption(Graphics g, SynthContext context,
            Region id, String detail, int x, int y, int w, int h) {

        int synthState = context.getComponentState();
        int widget = getWidgetType(context.getComponent(), id).ordinal();
        native_paint_option(widget, synthState, detail, x - x0, y - y0, w, h);
    }

    public void paintShadow(Graphics g, SynthContext context,
            Region id, int state, ShadowType shadowType, String detail,
            int x, int y, int w, int h) {

        int gtkState =
            GTKLookAndFeel.synthStateToGTKStateType(state).ordinal();
        int synthState = context.getComponentState();
        Container parent = context.getComponent().getParent();
        if(GTKLookAndFeel.is3()) {
            if (parent != null && parent.getParent() instanceof JComboBox) {
                if (parent.getParent().hasFocus()) {
                    synthState |= SynthConstants.FOCUSED;
                }
            }
        }
        int dir = getTextDirection(context);
        int widget = getWidgetType(context.getComponent(), id).ordinal();
        native_paint_shadow(widget, gtkState, shadowType.ordinal(), detail,
                            x - x0, y - y0, w, h, synthState, dir);
    }

    public void paintSlider(Graphics g, SynthContext context,
            Region id, int state, ShadowType shadowType, String detail, int x,
            int y, int w, int h, Orientation orientation, boolean hasFocus) {

        state = GTKLookAndFeel.synthStateToGTKStateType(state).ordinal();
        int widget = getWidgetType(context.getComponent(), id).ordinal();
        native_paint_slider(widget, state, shadowType.ordinal(), detail,
                         x - x0, y - y0, w, h, orientation.ordinal(), hasFocus);
    }

    public void paintVline(Graphics g, SynthContext context,
            Region id, int state, String detail, int x, int y, int w, int h) {

        state = GTKLookAndFeel.synthStateToGTKStateType(state).ordinal();
        int widget = getWidgetType(context.getComponent(), id).ordinal();
        native_paint_vline(widget, state, detail, x - x0, y - y0, w, h);
    }

    public void paintBackground(Graphics g, SynthContext context,
            Region id, int state, Color color, int x, int y, int w, int h) {

        state = GTKLookAndFeel.synthStateToGTKStateType(state).ordinal();
        int widget = getWidgetType(context.getComponent(), id).ordinal();
        native_paint_background(widget, state, x - x0, y - y0, w, h);
    }

    private static final ColorModel[] COLOR_MODELS = {
        // Transparency.OPAQUE
        new DirectColorModel(24, 0x00ff0000, 0x0000ff00, 0x000000ff, 0x00000000),
        // Transparency.BITMASK
        new DirectColorModel(25, 0x00ff0000, 0x0000ff00, 0x000000ff, 0x01000000),
        // Transparency.TRANSLUCENT
        ColorModel.getRGBdefault(),
    };

    private static final int[][] BAND_OFFSETS = {
        { 0x00ff0000, 0x0000ff00, 0x000000ff },             // OPAQUE
        { 0x00ff0000, 0x0000ff00, 0x000000ff, 0x01000000 }, // BITMASK
        { 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000 }  // TRANSLUCENT
    };


    /**
     * Paint a cached image identified by its size and a set of additional
     * arguments, if there's one.
     *
     * @return true if a cached image has been painted, false otherwise
     */
    public boolean paintCachedImage(Graphics g,
            int x, int y, int w, int h, Object... args) {
        if (w <= 0 || h <= 0) {
            return true;
        }

        // look for cached image
        Image img = cache.getImage(getClass(), null, w, h, args);
        if (img != null) {
            g.drawImage(img, x, y, null);
            return true;
        }
        return false;
    }

    /*
     * Allocate a native offscreen buffer of the specified size.
     */
    public void startPainting(Graphics g,
            int x, int y, int w, int h, Object... args) {
        nativeStartPainting(w, h);
        x0 = x;
        y0 = y;
        w0 = w;
        h0 = h;
        graphics = g;
        cacheArgs = args;
    }

    /**
     * Convenience method that delegates to finishPainting() with
     * caching enabled.
     */
    public BufferedImage finishPainting() {
        return finishPainting(true);
    }

    /**
     * Called to indicate that painting is finished. We create a new
     * BufferedImage from the offscreen buffer, (optionally) cache it,
     * and paint it.
     */
    public BufferedImage finishPainting(boolean useCache) {
        DataBufferInt dataBuffer = new DataBufferInt(w0 * h0);
        // Note that stealData() requires a markDirty() afterwards
        // since we modify the data in it.
        int transparency =
            nativeFinishPainting(SunWritableRaster.stealData(dataBuffer, 0),
                                 w0, h0);
        SunWritableRaster.markDirty(dataBuffer);

        int[] bands = BAND_OFFSETS[transparency - 1];
        WritableRaster raster = Raster.createPackedRaster(
                dataBuffer, w0, h0, w0, bands, null);

        ColorModel cm = COLOR_MODELS[transparency - 1];
        BufferedImage img = new BufferedImage(cm, raster, false, null);
        if (useCache) {
            cache.setImage(getClass(), null, w0, h0, cacheArgs, img);
        }
        graphics.drawImage(img, x0, y0, null);
        return img;
    }

    /**
     * Notify native layer of theme change, and flush cache
     */
    public void themeChanged() {
        synchronized(sun.awt.UNIXToolkit.GTK_LOCK) {
            native_switch_theme();
        }
        cache.flush();
    }

    /* GtkSettings enum mirrors that in gtk2_interface.h */
    public Object getSetting(Settings property) {
        synchronized(sun.awt.UNIXToolkit.GTK_LOCK) {
            return native_get_gtk_setting(property.ordinal());
        }
    }

    /**
     * Sets up the GtkAdjustment values for the native GtkRange widget
     * associated with the given region (e.g. SLIDER, SCROLL_BAR).
     */
    void setRangeValue(SynthContext context, Region id,
                       double value, double min, double max, double visible) {
        int widget = getWidgetType(context.getComponent(), id).ordinal();
        nativeSetRangeValue(widget, value, min, max, visible);
    }
}
