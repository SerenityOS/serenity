/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

import sun.awt.ModalExclude;
import sun.awt.SunToolkit;
import sun.awt.UNIXToolkit;

import javax.swing.plaf.synth.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.*;
import com.sun.java.swing.plaf.gtk.GTKConstants.ArrowType;
import com.sun.java.swing.plaf.gtk.GTKConstants.ExpanderStyle;
import com.sun.java.swing.plaf.gtk.GTKConstants.Orientation;
import com.sun.java.swing.plaf.gtk.GTKConstants.PositionType;
import com.sun.java.swing.plaf.gtk.GTKConstants.ShadowType;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Joshua Outwater
 * @author Scott Violet
 */
// Need to support:
// default_outside_border: Insets when default.
// interior_focus: Indicates if focus should appear inside border, or
//                       outside border.
// focus-line-width: Integer giving size of focus border
// focus-padding: Integer giving padding between border and focus
//        indicator.
// focus-line-pattern:
//
class GTKPainter extends SynthPainter {
    private static final PositionType[] POSITIONS = {
        PositionType.BOTTOM, PositionType.RIGHT,
        PositionType.TOP, PositionType.LEFT
    };

    private static final ShadowType[] SHADOWS = {
        ShadowType.NONE, ShadowType.IN, ShadowType.OUT,
        ShadowType.ETCHED_IN, ShadowType.OUT
    };

    private static final GTKEngine ENGINE = GTKEngine.INSTANCE;
    static final GTKPainter INSTANCE = new GTKPainter();

    private GTKPainter() {
    }

    private String getName(SynthContext context) {
        return (context.getRegion().isSubregion()) ? null :
               context.getComponent().getName();
    }

    public void paintCheckBoxBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {
        paintRadioButtonBackground(context, g, x, y, w, h);
    }

    public void paintCheckBoxMenuItemBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {
        paintRadioButtonMenuItemBackground(context, g, x, y, w, h);
    }

    // FORMATTED_TEXT_FIELD
    public void paintFormattedTextFieldBackground(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h) {
        paintTextBackground(context, g, x, y, w, h);
    }

    //
    // TOOL_BAR_DRAG_WINDOW
    //
    public void paintToolBarDragWindowBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paintToolBarBackground(context, g, x, y, w, h);
    }


    //
    // TOOL_BAR
    //
    public void paintToolBarBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        Region id = context.getRegion();
        int state = context.getComponentState();
        int gtkState = GTKLookAndFeel.synthStateToGTKState(id, state);
        int orientation = ((JToolBar)context.getComponent()).getOrientation();
        synchronized (UNIXToolkit.GTK_LOCK) {
            if (! ENGINE.paintCachedImage(g, x, y, w, h, id,
                                          state, orientation))
            {
                ENGINE.startPainting(g, x, y, w, h, id, state, orientation);
                ENGINE.paintBox(g, context, id, gtkState, ShadowType.OUT,
                                "handlebox_bin", x, y, w, h);
                ENGINE.finishPainting();
            }
        }
    }

    public void paintToolBarContentBackground(SynthContext context,
                                              Graphics g,
                                              int x, int y, int w, int h) {
        Region id = context.getRegion();
        int orientation = ((JToolBar)context.getComponent()).getOrientation();
        synchronized (UNIXToolkit.GTK_LOCK) {
            if (! ENGINE.paintCachedImage(g, x, y, w, h, id, orientation)) {
                ENGINE.startPainting(g, x, y, w, h, id, orientation);
                ENGINE.paintBox(g, context, id, SynthConstants.ENABLED,
                                ShadowType.OUT, "toolbar", x, y, w, h);
                ENGINE.finishPainting();
            }
        }
    }

    //
    // PASSWORD_FIELD
    //
    public void paintPasswordFieldBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paintTextBackground(context, g, x, y, w, h);
    }

    //
    // TEXT_FIELD
    //
    public void paintTextFieldBackground(SynthContext context, Graphics g,
                                         int x, int y, int w, int h) {
        if (getName(context) == "Tree.cellEditor") {
            paintTreeCellEditorBackground(context, g, x, y, w, h);
        } else {
            paintTextBackground(context, g, x, y, w, h);
        }
    }

    //
    // RADIO_BUTTON
    //
    // NOTE: this is called for JCheckBox too
    public void paintRadioButtonBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        Region id = context.getRegion();
        int gtkState = GTKLookAndFeel.synthStateToGTKState(
                id, context.getComponentState());
        if (gtkState == SynthConstants.MOUSE_OVER) {
            synchronized (UNIXToolkit.GTK_LOCK) {
                if (! ENGINE.paintCachedImage(g, x, y, w, h, id)) {
                    ENGINE.startPainting(g, x, y, w, h, id);
                    ENGINE.paintFlatBox(g, context, id,
                            SynthConstants.MOUSE_OVER, ShadowType.ETCHED_OUT,
                            "checkbutton", x, y, w, h, ColorType.BACKGROUND);
                    ENGINE.finishPainting();
                }
            }
        }
    }

    //This is workaround used to draw the highlight
    // when the MENU or MenuItem is selected on some platforms
    //This should be properly fixed by reading color from css
    private void paintComponentBackground(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h) {
        GTKStyle style = (GTKStyle) context.getStyle();
        Color highlightColor =
                style.getGTKColor(GTKEngine.WidgetType.TEXT_AREA.ordinal(),
                GTKLookAndFeel.synthStateToGTKStateType(SynthConstants.SELECTED).ordinal(),
                ColorType.BACKGROUND.getID());
        g.setColor(highlightColor);
        g.fillRect(x, y, w, h);
    }

    //
    // RADIO_BUTTON_MENU_ITEM
    //
    // NOTE: this is called for JCheckBoxMenuItem too
    public void paintRadioButtonMenuItemBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        Region id = context.getRegion();
        int gtkState = GTKLookAndFeel.synthStateToGTKState(
                id, context.getComponentState());
        if (gtkState == SynthConstants.MOUSE_OVER) {
            if (GTKLookAndFeel.is3()) {
                paintComponentBackground(context, g, x, y, w, h);
                return;
            }
            synchronized (UNIXToolkit.GTK_LOCK) {
                if (! ENGINE.paintCachedImage(g, x, y, w, h, id)) {
                    ShadowType shadow = (GTKLookAndFeel.is2_2() ?
                        ShadowType.NONE : ShadowType.OUT);
                    ENGINE.startPainting(g, x, y, w, h, id);
                    ENGINE.paintBox(g, context, id, gtkState,
                            shadow, "menuitem", x, y, w, h);
                    ENGINE.finishPainting();
                }
            }
        }
    }

    //
    // LABEL
    //
    public void paintLabelBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        String name = getName(context);
        JComponent c = context.getComponent();
        Container  container = c.getParent();

        if (name == "TableHeader.renderer" ||
            name == "GTKFileChooser.directoryListLabel" ||
            name == "GTKFileChooser.fileListLabel") {

            paintButtonBackgroundImpl(context, g, Region.BUTTON, "button",
                    x, y, w, h, true, false, false, false);
        }
        /*
         * If the label is a ListCellRenderer and it's in a container
         * (CellRendererPane) which is in a JComboBox then we paint the label
         * as a TextField like a gtk_entry for a combobox.
         */
        else if (c instanceof ListCellRenderer &&
                 container != null &&
                 container.getParent() instanceof JComboBox ) {
            paintTextBackground(context, g, x, y, w, h);
        }
    }

    //
    // INTERNAL_FRAME
    //
    public void paintInternalFrameBorder(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h) {
        Metacity.INSTANCE.paintFrameBorder(context, g, x, y, w, h);
    }

    //
    // DESKTOP_PANE
    //
    public void paintDesktopPaneBackground(SynthContext context,
                                           Graphics g, int x, int y,
                                           int w, int h) {
        // Does not call into ENGINE for better performance
        fillArea(context, g, x, y, w, h, ColorType.BACKGROUND);
    }

    //
    // DESKTOP_ICON
    //
    public void paintDesktopIconBorder(SynthContext context,
                                           Graphics g, int x, int y,
                                           int w, int h) {
        Metacity.INSTANCE.paintFrameBorder(context, g, x, y, w, h);
    }

    public void paintButtonBackground(SynthContext context, Graphics g,
                                      int x, int y, int w, int h) {
        String name = getName(context);
        if (name != null && name.startsWith("InternalFrameTitlePane.")) {
            Metacity.INSTANCE.paintButtonBackground(context, g, x, y, w, h);

        } else {
            AbstractButton button = (AbstractButton)context.getComponent();
            boolean paintBG = button.isContentAreaFilled() &&
                              button.isBorderPainted();
            boolean paintFocus = button.isFocusPainted();
            boolean defaultCapable = (button instanceof JButton) &&
                    ((JButton)button).isDefaultCapable();
            boolean toolButton = (button.getParent() instanceof JToolBar);
            paintButtonBackgroundImpl(context, g, Region.BUTTON, "button",
                    x, y, w, h, paintBG, paintFocus, defaultCapable, toolButton);
        }
    }

    private void paintButtonBackgroundImpl(SynthContext context, Graphics g,
            Region id, String detail, int x, int y, int w, int h,
            boolean paintBackground, boolean paintFocus,
            boolean defaultCapable, boolean toolButton) {
        int state = context.getComponentState();
        synchronized (UNIXToolkit.GTK_LOCK) {
            if (ENGINE.paintCachedImage(g, x, y, w, h, id, state, detail,
                    paintBackground, paintFocus, defaultCapable, toolButton)) {
                return;
            }
            ENGINE.startPainting(g, x, y, w, h, id, state, detail,
                paintBackground, paintFocus, defaultCapable, toolButton);

            // Paint the default indicator
            GTKStyle style = (GTKStyle)context.getStyle();
            if (defaultCapable && !toolButton) {
                Insets defaultInsets = style.getClassSpecificInsetsValue(
                        context, "default-border",
                        GTKStyle.BUTTON_DEFAULT_BORDER_INSETS);

                if (paintBackground && (state & SynthConstants.DEFAULT) != 0) {
                    ENGINE.paintBox(g, context, id, SynthConstants.ENABLED,
                            ShadowType.IN, "buttondefault", x, y, w, h);
                }
                x += defaultInsets.left;
                y += defaultInsets.top;
                w -= (defaultInsets.left + defaultInsets.right);
                h -= (defaultInsets.top + defaultInsets.bottom);
            }

            boolean interiorFocus = style.getClassSpecificBoolValue(
                    context, "interior-focus", true);
            int focusSize = style.getClassSpecificIntValue(
                    context, "focus-line-width",1);
            int focusPad = style.getClassSpecificIntValue(
                    context, "focus-padding", 1);

            int totalFocusSize = focusSize + focusPad;
            int xThickness = style.getXThickness();
            int yThickness = style.getYThickness();

            // Render the box.
            if (!interiorFocus &&
                    (state & SynthConstants.FOCUSED) == SynthConstants.FOCUSED) {
                x += totalFocusSize;
                y += totalFocusSize;
                w -= 2 * totalFocusSize;
                h -= 2 * totalFocusSize;
            }

            int gtkState = GTKLookAndFeel.synthStateToGTKState(id, state);
            boolean paintBg;
            if (toolButton) {
                // Toolbar buttons should only have their background painted
                // in the PRESSED, SELECTED, or MOUSE_OVER states.
                paintBg =
                    (gtkState != SynthConstants.ENABLED) &&
                    (gtkState != SynthConstants.DISABLED);
            } else {
                // Otherwise, always paint the button's background, unless
                // the user has overridden it and we're in the ENABLED state.
                paintBg =
                    paintBackground ||
                    (gtkState != SynthConstants.ENABLED);
            }
            if (paintBg) {
                ShadowType shadowType = ShadowType.OUT;
                if ((state & (SynthConstants.PRESSED |
                              SynthConstants.SELECTED)) != 0) {
                    shadowType = ShadowType.IN;
                }
                ENGINE.paintBox(g, context, id, gtkState,
                        shadowType, detail, x, y, w, h);
            }

            // focus
            if (paintFocus && (state & SynthConstants.FOCUSED) != 0) {
                if (interiorFocus) {
                    x += xThickness + focusPad;
                    y += yThickness + focusPad;
                    w -= 2 * (xThickness + focusPad);
                    h -= 2 * (yThickness + focusPad);
                } else {
                    x -= totalFocusSize;
                    y -= totalFocusSize;
                    w += 2 * totalFocusSize;
                    h += 2 * totalFocusSize;
                }
                ENGINE.paintFocus(g, context, id, gtkState, detail, x, y, w, h);
            }
            ENGINE.finishPainting();
        }
    }

    //
    // ARROW_BUTTON
    //
    public void paintArrowButtonForeground(SynthContext context, Graphics g,
                                           int x, int y, int w, int h,
                                           int direction) {
        Region id = context.getRegion();
        Component c = context.getComponent();
        String name = c.getName();

        ArrowType arrowType = null;
        switch (direction) {
            case SwingConstants.NORTH:
                arrowType = ArrowType.UP; break;
            case SwingConstants.SOUTH:
                arrowType = ArrowType.DOWN; break;
            case SwingConstants.EAST:
                arrowType = ArrowType.RIGHT; break;
            case SwingConstants.WEST:
                arrowType = ArrowType.LEFT; break;
        }

        String detail = "arrow";
        if ((name == "ScrollBar.button") || (name == "TabbedPane.button")) {
            if (arrowType == ArrowType.UP || arrowType == ArrowType.DOWN) {
                detail = "vscrollbar";
            } else {
                detail = "hscrollbar";
            }
        } else if (name == "Spinner.nextButton" ||
                   name == "Spinner.previousButton") {
            detail = "spinbutton";
        } else if (name != "ComboBox.arrowButton") {
            assert false : "unexpected name: " + name;
        }

        int gtkState = GTKLookAndFeel.synthStateToGTKState(
                id, context.getComponentState());
        ShadowType shadowType = (gtkState == SynthConstants.PRESSED ?
            ShadowType.IN : ShadowType.OUT);
        synchronized (UNIXToolkit.GTK_LOCK) {
            if (ENGINE.paintCachedImage(g, x, y, w, h,
                    gtkState, name, direction)) {
                return;
            }
            ENGINE.startPainting(g, x, y, w, h, gtkState, name, direction);
            ENGINE.paintArrow(g, context, id, gtkState,
                    shadowType, arrowType, detail, x, y, w, h);
            ENGINE.finishPainting();
        }
    }

    public void paintArrowButtonBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h) {
        Region id = context.getRegion();
        AbstractButton button = (AbstractButton)context.getComponent();

        String name = button.getName();
        String detail = "button";
        int direction = SwingConstants.CENTER;
        if ((name == "ScrollBar.button") || (name == "TabbedPane.button")) {
            Integer prop = (Integer)
                button.getClientProperty("__arrow_direction__");
            direction = (prop != null) ?
                prop.intValue() : SwingConstants.WEST;
            switch (direction) {
            default:
            case SwingConstants.EAST:
            case SwingConstants.WEST:
                detail = "hscrollbar";
                break;
            case SwingConstants.NORTH:
            case SwingConstants.SOUTH:
                detail = "vscrollbar";
                break;
            }
        } else if (name == "Spinner.previousButton") {
            detail = "spinbutton_down";
        } else if (name == "Spinner.nextButton") {
            detail = "spinbutton_up";
        } else if (name != "ComboBox.arrowButton") {
            assert false : "unexpected name: " + name;
        }

        int state = context.getComponentState();
        synchronized (UNIXToolkit.GTK_LOCK) {
            if (ENGINE.paintCachedImage(g, x, y, w, h, id,
                                        state, detail, direction))
            {
                return;
            }
            ENGINE.startPainting(g, x, y, w, h, id,
                                 state, detail, direction);

            if (detail.startsWith("spin")) {
                /*
                 * The ubuntulooks engine (and presumably others) expect us to
                 * first draw the full "spinbutton" background, and then draw
                 * the individual "spinbutton_up/down" buttons on top of that.
                 * Note that it is the state of the JSpinner (not its arrow
                 * button) that determines how we draw this background.
                 */
                int spinState = button.getParent().isEnabled() ?
                    SynthConstants.ENABLED : SynthConstants.DISABLED;
                int mody = (detail == "spinbutton_up") ? y : y-h;
                int modh = h*2;
                ENGINE.paintBox(g, context, id, spinState,
                                ShadowType.IN, "spinbutton",
                                x, mody, w, modh);
            }

            int gtkState = GTKLookAndFeel.synthStateToGTKState(id, state);
            ShadowType shadowType = ShadowType.OUT;
            if ((gtkState & (SynthConstants.PRESSED |
                             SynthConstants.SELECTED)) != 0)
            {
                shadowType = ShadowType.IN;
            }
            ENGINE.paintBox(g, context, id, gtkState,
                            shadowType, detail,
                            x, y, w, h);

            ENGINE.finishPainting();
        }
    }


    //
    // LIST
    //
    public void paintListBackground(SynthContext context, Graphics g,
                                    int x, int y, int w, int h) {
        // Does not call into ENGINE for better performance
        fillArea(context, g, x, y, w, h, GTKColorType.TEXT_BACKGROUND);
    }

    public void paintMenuBarBackground(SynthContext context, Graphics g,
                                       int x, int y, int w, int h) {
        Region id = context.getRegion();
        synchronized (UNIXToolkit.GTK_LOCK) {
            if (ENGINE.paintCachedImage(g, x, y, w, h, id)) {
                return;
            }
            GTKStyle style = (GTKStyle)context.getStyle();
            int shadow = style.getClassSpecificIntValue(
                    context, "shadow-type", 2);
            ShadowType shadowType = SHADOWS[shadow];
            int gtkState = GTKLookAndFeel.synthStateToGTKState(
                    id, context.getComponentState());
            ENGINE.startPainting(g, x, y, w, h, id);
            ENGINE.paintBox(g, context, id, gtkState,
                shadowType, "menubar", x, y, w, h);
            ENGINE.finishPainting();
        }
    }

    //
    // MENU
    //
    public void paintMenuBackground(SynthContext context,
                                     Graphics g,
                                     int x, int y, int w, int h) {
        paintMenuItemBackground(context, g, x, y, w, h);
    }

    // This is called for both MENU and MENU_ITEM
    public void paintMenuItemBackground(SynthContext context,
                                     Graphics g,
                                     int x, int y, int w, int h) {
        int gtkState = GTKLookAndFeel.synthStateToGTKState(
                context.getRegion(), context.getComponentState());
        if (gtkState == SynthConstants.MOUSE_OVER) {
            if (GTKLookAndFeel.is3()) {
                paintComponentBackground(context, g, x, y, w, h);
                return;
            }
            Region id = Region.MENU_ITEM;
            synchronized (UNIXToolkit.GTK_LOCK) {
                if (! ENGINE.paintCachedImage(g, x, y, w, h, id)) {
                    ShadowType shadow = (GTKLookAndFeel.is2_2() ?
                        ShadowType.NONE : ShadowType.OUT);
                    ENGINE.startPainting(g, x, y, w, h, id);
                    ENGINE.paintBox(g, context, id, gtkState, shadow,
                            "menuitem", x, y, w, h);
                    ENGINE.finishPainting();
                }
            }
        }
    }

    public void paintPopupMenuBackground(SynthContext context, Graphics g,
                                        int x, int y, int w, int h) {
        Region id = context.getRegion();
        int gtkState = GTKLookAndFeel.synthStateToGTKState(
                id, context.getComponentState());
        boolean isHW = SunToolkit.getHeavyweightComponent(
                context.getComponent()) instanceof ModalExclude;
        synchronized (UNIXToolkit.GTK_LOCK) {
            if (ENGINE.paintCachedImage(g, x, y, w, h, id, gtkState, isHW)) {
                return;
            }
            ENGINE.startPainting(g, x, y, w, h, id, gtkState);
            ENGINE.paintBox(g, context, id, gtkState,
                    ShadowType.OUT, "menu", x, y, w, h);

            GTKStyle style = (GTKStyle)context.getStyle();
            Insets insets = style.getInsets(context, null);
            ENGINE.paintBackground(g, context, id, gtkState,
                style.getGTKColor(context, gtkState, GTKColorType.BACKGROUND),
                x + insets.left, y + insets.top, w - insets.left - insets.right,
                h - insets.top - insets.bottom);
            BufferedImage img = ENGINE.finishPainting();
            if(!isHW) {
                int border = img.getRGB(0, h / 2);
                if (img != null && border == img.getRGB(w / 2, h / 2)) {
                    // fix no menu borders in Adwaita theme
                    Graphics g2 = img.getGraphics();
                    Color c = new Color(border);
                    g2.setColor(new Color(Math.max((int) (c.getRed() * 0.8), 0),
                            Math.max((int) (c.getGreen() * 0.8), 0),
                            Math.max((int) (c.getBlue() * 0.8), 0)));
                    g2.drawLine(0, 0, w - 1, 0);
                    g2.drawLine(w - 1, 0, w - 1, h - 1);
                    g2.drawLine(0, h - 1, 0, 1);
                    g2.setColor(c.darker());
                    g2.drawLine(w - 1, h - 1, 0, h - 1);
                    g2.dispose();
                    g.drawImage(img, x, y, null);
                }
            }
        }
    }

    public void paintProgressBarBackground(SynthContext context,
                                            Graphics g,
                                            int x, int y, int w, int h) {
        Region id = context.getRegion();
        synchronized (UNIXToolkit.GTK_LOCK) {
            if (! ENGINE.paintCachedImage(g, x, y, w, h, id)) {
                ENGINE.startPainting(g, x, y, w, h, id);
                ENGINE.paintBox(g, context, id, SynthConstants.ENABLED,
                        ShadowType.IN, "trough", x, y, w, h);
                ENGINE.finishPainting();
            }
        }
    }

    public void paintProgressBarForeground(SynthContext context, Graphics g,
                                            int x, int y, int w, int h,
                                            int orientation) {
        Region id = context.getRegion();
        synchronized (UNIXToolkit.GTK_LOCK) {
            // Note that we don't call paintCachedImage() here.  Since the
            // progress bar foreground is painted differently for each value
            // it would be wasteful to try to cache an image for each state,
            // so instead we simply avoid caching in this case.
            if (w <= 0 || h <= 0) {
                return;
            }
            ENGINE.startPainting(g, x, y, w, h, id, "fg");
            ENGINE.paintBox(g, context, id, SynthConstants.MOUSE_OVER,
                            ShadowType.OUT, "bar", x, y, w, h);
            ENGINE.finishPainting(false); // don't bother caching the image
        }
    }

    public void paintViewportBorder(SynthContext context, Graphics g,
                                           int x, int y, int w, int h) {
        Region id = context.getRegion();
        synchronized (UNIXToolkit.GTK_LOCK) {
            if (! ENGINE.paintCachedImage(g, x, y, w, h, id)) {
                ENGINE.startPainting(g, x, y, w, h, id);
                ENGINE.paintShadow(g, context, id, SynthConstants.ENABLED,
                        ShadowType.IN, "scrolled_window", x, y, w, h);
                ENGINE.finishPainting();
            }
        }
    }

    public void paintSeparatorBackground(SynthContext context,
                                          Graphics g,
                                          int x, int y, int w, int h,
                                         int orientation) {
        Region id = context.getRegion();
        int state = context.getComponentState();
        JComponent c = context.getComponent();

        GTKStyle style = (GTKStyle) context.getStyle();
        String detail;
        // wide-separators are painted using box not line
        if (style.getClassSpecificBoolValue(context,
                                          "wide-separators", false)) {
            Insets insets = c.getInsets();
            x += insets.left;
            y += insets.top;
            if (orientation == JSeparator.HORIZONTAL) {
                w -= (insets.left + insets.right);
            } else {
                h -= (insets.top + insets.bottom);
            }
            if (GTKLookAndFeel.is3()) {
                if (id == Region.POPUP_MENU_SEPARATOR) {
                    detail = "menuitem";
                    h -= (insets.top + insets.bottom);
                } else {
                    detail = "separator";
                }
            } else {
                detail = orientation == JSeparator.HORIZONTAL ?
                                                    "hseparator" : "vseparator";
            }
            synchronized (UNIXToolkit.GTK_LOCK) {
                if (! ENGINE.paintCachedImage(g, x, y, w, h, id, state,
                            detail, orientation)) {
                    ENGINE.startPainting(g, x, y, w, h, id, state,
                            detail, orientation);
                    ENGINE.paintBox(g, context, id, state,
                            ShadowType.ETCHED_OUT, detail, x, y, w, h);
                    ENGINE.finishPainting();
                }
            }
            return;
        }

        /*
         * Note: In theory, the style's x/y thickness values would determine
         * the width of the separator content.  In practice, however, some
         * engines will render a line that is wider than the corresponding
         * thickness value.  For example, ubuntulooks reports x/y thickness
         * values of 1 for separators, but always renders a 2-pixel wide line.
         * As a result of all this, we need to be careful not to restrict
         * the w/h values below too much, so that the full thickness of the
         * rendered line will be captured by our image caching code.
         */
        if (c instanceof JToolBar.Separator) {
            /*
             * GTK renders toolbar separators differently in that an
             * artificial padding is added to each end of the separator.
             * The value of 0.2f below is derived from the source code of
             * gtktoolbar.c in the current version of GTK+ (2.8.20 at the
             * time of this writing).  Specifically, the relevant values are:
             *     SPACE_LINE_DIVISION 10.0
             *     SPACE_LINE_START     2.0
             *     SPACE_LINE_END       8.0
             * These are used to determine the distance from the top (or left)
             * edge of the toolbar to the other edge.  So for example, the
             * starting/top point of a vertical separator is 2/10 of the
             * height of a horizontal toolbar away from the top edge, which
             * is how we arrive at 0.2f below.  Likewise, the ending/bottom
             * point is 8/10 of the height away from the top edge, or in other
             * words, it is 2/10 away from the bottom edge, which is again
             * how we arrive at the 0.2f value below.
             *
             * The separator is also centered horizontally or vertically,
             * depending on its orientation.  This was determined empirically
             * and by examining the code referenced above.
             */
            detail = "toolbar";
            float pct = 0.2f;
            JToolBar.Separator sep = (JToolBar.Separator)c;
            Dimension size = sep.getSeparatorSize();
            if (orientation == JSeparator.HORIZONTAL) {
                x += (int)(w * pct);
                w -= (int)(w * pct * 2);
                y += (size.height - style.getYThickness()) / 2;
            } else {
                y += (int)(h * pct);
                h -= (int)(h * pct * 2);
                x += (size.width - style.getXThickness()) / 2;
            }
        } else {
            // For regular/menu separators, we simply subtract out the insets.
            detail = "separator";
            Insets insets = c.getInsets();
            x += insets.left;
            y += insets.top;
            if (orientation == JSeparator.HORIZONTAL) {
                w -= (insets.left + insets.right);
            } else {
                h -= (insets.top + insets.bottom);
            }
        }

        synchronized (UNIXToolkit.GTK_LOCK) {
            if (! ENGINE.paintCachedImage(g, x, y, w, h, id,
                                          state, detail, orientation)) {
                ENGINE.startPainting(g, x, y, w, h, id,
                                     state, detail, orientation);
                if (orientation == JSeparator.HORIZONTAL) {
                    ENGINE.paintHline(g, context, id, state,
                                      detail, x, y, w, h);
                } else {
                    ENGINE.paintVline(g, context, id, state,
                                      detail, x, y, w, h);
                }
                ENGINE.finishPainting();
            }
        }
    }

    public void paintSliderTrackBackground(SynthContext context,
                                       Graphics g,
                                       int x, int y, int w,int h) {
        Region id = context.getRegion();
        int state = context.getComponentState();

        // For focused sliders, we paint focus rect outside the bounds passed.
        // Need to adjust for that.
        boolean focused = ((state & SynthConstants.FOCUSED) != 0);
        int focusSize = 0;
        if (focused) {
            GTKStyle style = (GTKStyle)context.getStyle();
            focusSize = style.getClassSpecificIntValue(
                                context, "focus-line-width", 1) +
                        style.getClassSpecificIntValue(
                                context, "focus-padding", 1);
            x -= focusSize;
            y -= focusSize;
            w += focusSize * 2;
            h += focusSize * 2;
        }

        // The ubuntulooks engine paints slider troughs differently depending
        // on the current slider value and its component orientation.
        JSlider slider = (JSlider)context.getComponent();
        if (GTKLookAndFeel.is3()) {
            if (slider.getOrientation() == JSlider.VERTICAL) {
                y += 1;
                h -= 2;
            } else {
                x += 1;
                w -= 2;
            }
        }
        double value = slider.getValue();
        double min = slider.getMinimum();
        double max = slider.getMaximum();
        double visible = 20; // not used for sliders; any value will work

        synchronized (UNIXToolkit.GTK_LOCK) {
            // Note that we don't call paintCachedImage() here.  Since some
            // engines (e.g. ubuntulooks) paint the slider background
            // differently for any given slider value, it would be wasteful
            // to try to cache an image for each state, so instead we simply
            // avoid caching in this case.
            if (w <= 0 || h <= 0) {
                return;
            }
            ENGINE.startPainting(g, x, y, w, h, id, state, value);
            int gtkState = GTKLookAndFeel.synthStateToGTKState(id, state);
            ENGINE.setRangeValue(context, id, value, min, max, visible);
            ENGINE.paintBox(g, context, id, gtkState, ShadowType.IN,
                            "trough", x + focusSize, y + focusSize,
                            w - 2 * focusSize, h - 2 * focusSize);
            if (focused) {
                ENGINE.paintFocus(g, context, id, SynthConstants.ENABLED,
                                  "trough", x, y, w, h);
            }
            ENGINE.finishPainting(false); // don't bother caching the image
        }
    }

    public void paintSliderThumbBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h, int dir) {
        Region id = context.getRegion();
        int gtkState = GTKLookAndFeel.synthStateToGTKState(
                id, context.getComponentState());
        boolean hasFocus = GTKLookAndFeel.is3() &&
                ((context.getComponentState() & SynthConstants.FOCUSED) != 0);
        synchronized (UNIXToolkit.GTK_LOCK) {
            if (! ENGINE.paintCachedImage(g, x, y, w, h, id, gtkState, dir,
                                                                    hasFocus)) {
                Orientation orientation = (dir == JSlider.HORIZONTAL ?
                    Orientation.HORIZONTAL : Orientation.VERTICAL);
                String detail = (dir == JSlider.HORIZONTAL ?
                    "hscale" : "vscale");
                ENGINE.startPainting(g, x, y, w, h, id, gtkState, dir);
                ENGINE.paintSlider(g, context, id, gtkState,
                        ShadowType.OUT, detail, x, y, w, h, orientation,
                                                                     hasFocus);
                ENGINE.finishPainting();
            }
        }
    }

    //
    // SPINNER
    //
    public void paintSpinnerBackground(SynthContext context,
                                        Graphics g,
                                        int x, int y, int w, int h) {
        // This is handled in paintTextFieldBackground
    }

    //
    // SPLIT_PANE_DIVIDER
    //
    public void paintSplitPaneDividerBackground(SynthContext context,
                                       Graphics g,
                                       int x, int y, int w, int h) {
        Region id = context.getRegion();
        int gtkState = GTKLookAndFeel.synthStateToGTKState(
                id, context.getComponentState());
        JSplitPane splitPane = (JSplitPane)context.getComponent();
        Orientation orientation =
                (splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT ?
                    Orientation.VERTICAL : Orientation.HORIZONTAL);
        synchronized (UNIXToolkit.GTK_LOCK) {
            if (! ENGINE.paintCachedImage(g, x, y, w, h,
                    id, gtkState, orientation)) {
                ENGINE.startPainting(g, x, y, w, h, id, gtkState, orientation);
                ENGINE.paintHandle(g, context, id, gtkState,
                        ShadowType.OUT, "paned", x, y, w, h, orientation);
                ENGINE.finishPainting();
            }
        }
    }

    public void paintSplitPaneDragDivider(SynthContext context,
                                       Graphics g,int x, int y, int w, int h,
                                       int orientation) {
        paintSplitPaneDividerForeground(context, g, x, y, w, h, orientation);
    }

    public void paintTabbedPaneContentBackground(SynthContext context,
                                      Graphics g, int x, int y, int w, int h) {
        JTabbedPane pane = (JTabbedPane)context.getComponent();
        int selectedIndex = pane.getSelectedIndex();
        PositionType placement = GTKLookAndFeel.SwingOrientationConstantToGTK(
                                                        pane.getTabPlacement());

        int gapStart = 0;
        int gapSize = 0;
        if (selectedIndex != -1) {
            Rectangle tabBounds = pane.getBoundsAt(selectedIndex);

            if (placement == PositionType.TOP ||
                placement == PositionType.BOTTOM) {

                gapStart = tabBounds.x - x;
                gapSize = tabBounds.width;
            }
            else {
                gapStart = tabBounds.y - y;
                gapSize = tabBounds.height;
            }
        }

        Region id = context.getRegion();
        int gtkState = GTKLookAndFeel.synthStateToGTKState(
                id, context.getComponentState());
        synchronized (UNIXToolkit.GTK_LOCK) {
            if (! ENGINE.paintCachedImage(g, x, y, w, h,
                    id, gtkState, placement, gapStart, gapSize)) {
                ENGINE.startPainting(g, x, y, w, h,
                        id, gtkState, placement, gapStart, gapSize);
                ENGINE.paintBoxGap(g, context, id, gtkState, ShadowType.OUT,
                        "notebook", x, y, w, h, placement, gapStart, gapSize);
                ENGINE.finishPainting();
            }
        }
    }

    public void paintTabbedPaneTabBackground(SynthContext context,
                                           Graphics g,
                                           int x, int y, int w, int h,
                                           int tabIndex) {
        Region id = context.getRegion();
        int state = context.getComponentState();
        int gtkState = ((state & SynthConstants.SELECTED) != 0 ?
            SynthConstants.ENABLED : SynthConstants.PRESSED);
        JTabbedPane pane = (JTabbedPane)context.getComponent();
        int placement = pane.getTabPlacement();

        synchronized (UNIXToolkit.GTK_LOCK) {
            if (! ENGINE.paintCachedImage(g, x, y, w, h,
                    id, gtkState, placement, tabIndex)) {
                PositionType side = POSITIONS[placement - 1];
                ENGINE.startPainting(g, x, y, w, h,
                        id, gtkState, placement, tabIndex);
                ENGINE.paintExtension(g, context, id, gtkState,
                        ShadowType.OUT, "tab", x, y, w, h, side, tabIndex);
                ENGINE.finishPainting();
            }
        }
    }

    //
    // TEXT_PANE
    //
    public void paintTextPaneBackground(SynthContext context, Graphics g,
                                        int x, int y, int w, int h) {
        paintTextAreaBackground(context, g, x, y, w, h);
    }

    //
    // EDITOR_PANE
    //
    public void paintEditorPaneBackground(SynthContext context, Graphics g,
                                          int x, int y, int w, int h) {
        paintTextAreaBackground(context, g, x, y, w, h);
    }

    //
    // TEXT_AREA
    //
    public void paintTextAreaBackground(SynthContext context, Graphics g,
                                        int x, int y, int w, int h) {
        // Does not call into ENGINE for better performance
        fillArea(context, g, x, y, w, h, GTKColorType.TEXT_BACKGROUND);
    }

    //
    // TEXT_FIELD
    //
    // NOTE: Combobox and Label, Password and FormattedTextField calls this
    // too.
    private void paintTextBackground(SynthContext context, Graphics g,
                                     int x, int y, int w, int h) {
        // Text is odd in that it uses the TEXT_BACKGROUND vs BACKGROUND.
        JComponent c = context.getComponent();
        Container container = c.getParent();
        Container containerParent = null;
        GTKStyle style = (GTKStyle)context.getStyle();
        Region id = context.getRegion();
        int state = context.getComponentState();

        if (c instanceof ListCellRenderer && container != null) {
            containerParent = container.getParent();
            if (containerParent instanceof JComboBox
                    && containerParent.hasFocus()) {
                state |= SynthConstants.FOCUSED;
            }
        }

        synchronized (UNIXToolkit.GTK_LOCK) {
            if (ENGINE.paintCachedImage(g, x, y, w, h, id, state)) {
                return;
            }

            int gtkState = GTKLookAndFeel.synthStateToGTKState(id, state);
            int focusSize = 0;
            boolean interiorFocus = style.getClassSpecificBoolValue(
                    context, "interior-focus", true);

            focusSize = style.getClassSpecificIntValue(context,
                    "focus-line-width",1);
            if (!interiorFocus && (state & SynthConstants.FOCUSED) != 0) {
                x += focusSize;
                y += focusSize;
                w -= 2 * focusSize;
                h -= 2 * focusSize;
            }

            int xThickness = style.getXThickness();
            int yThickness = style.getYThickness();

            ENGINE.startPainting(g, x, y, w, h, id, state);
            if (GTKLookAndFeel.is3()) {
                ENGINE.paintBackground(g, context, id, gtkState, null,
                                                                    x, y, w, h);
            }
            ENGINE.paintShadow(g, context, id, gtkState,
                               ShadowType.IN, "entry", x, y, w, h);
            if (!GTKLookAndFeel.is3()) {
                ENGINE.paintFlatBox(g, context, id,
                        gtkState, ShadowType.NONE, "entry_bg",
                        x + xThickness,
                        y + yThickness,
                        w - (2 * xThickness),
                        h - (2 * yThickness),
                        ColorType.TEXT_BACKGROUND);
            }

            if (focusSize > 0 && (state & SynthConstants.FOCUSED) != 0) {
                if (!interiorFocus) {
                    x -=  focusSize;
                    y -=  focusSize;
                    w +=  2 * focusSize;
                    h +=  2 * focusSize;
                } else {
                    if (containerParent instanceof JComboBox) {
                        x += (focusSize + 2);
                        y += focusSize + (GTKLookAndFeel.is3() ? 3 : 1);
                        w -= 2 * focusSize + (GTKLookAndFeel.is3() ? 4 : 1);
                        h -= 2 * focusSize + (GTKLookAndFeel.is3() ? 6 : 2);
                    } else {
                        x += focusSize + (GTKLookAndFeel.is3() ? 2 : 0);
                        y += focusSize + (GTKLookAndFeel.is3() ? 2 :0 );
                        w -= 2 * focusSize + (GTKLookAndFeel.is3() ? 4 : 0);
                        h -= 2 * focusSize + (GTKLookAndFeel.is3() ? 4 : 0);
                    }
                }
                ENGINE.paintFocus(g, context, id, gtkState,
                        "entry", x, y, w, h);
            }
            ENGINE.finishPainting();
        }
    }

    private void paintTreeCellEditorBackground(SynthContext context, Graphics g,
                                               int x, int y, int w, int h) {
        Region id = context.getRegion();
        int gtkState = GTKLookAndFeel.synthStateToGTKState(
                id, context.getComponentState());
        synchronized (UNIXToolkit.GTK_LOCK) {
            if (! ENGINE.paintCachedImage(g, x, y, w, h, id, gtkState)) {
                ENGINE.startPainting(g, x, y, w, h, id, gtkState);
                ENGINE.paintFlatBox(g, context, id, gtkState, ShadowType.NONE,
                        "entry_bg", x, y, w, h, ColorType.TEXT_BACKGROUND);
                ENGINE.finishPainting();
            }
        }
    }


    //
    // ROOT_PANE
    //
    public void paintRootPaneBackground(SynthContext context, Graphics g,
                                        int x, int y, int w, int h) {
        // Does not call into ENGINE for better performance
        fillArea(context, g, x, y, w, h, GTKColorType.BACKGROUND);
    }

    //
    // TOGGLE_BUTTON
    //
    public void paintToggleButtonBackground(SynthContext context,
                                            Graphics g,
                                            int x, int y, int w, int h) {
        Region id = context.getRegion();
        JToggleButton toggleButton = (JToggleButton)context.getComponent();
        boolean paintBG = toggleButton.isContentAreaFilled() &&
                          toggleButton.isBorderPainted();
        boolean paintFocus = toggleButton.isFocusPainted();
        boolean toolButton = (toggleButton.getParent() instanceof JToolBar);
        paintButtonBackgroundImpl(context, g, id, "button",
                                  x, y, w, h,
                                  paintBG, paintFocus, false, toolButton);
    }


    //
    // SCROLL_BAR
    //
    public void paintScrollBarBackground(SynthContext context,
                                          Graphics g,
                                          int x, int y, int w,int h) {
        Region id = context.getRegion();
        boolean focused =
                (context.getComponentState() & SynthConstants.FOCUSED) != 0;
        synchronized (UNIXToolkit.GTK_LOCK) {
            if (ENGINE.paintCachedImage(g, x, y, w, h, id, focused)) {
                return;
            }
            ENGINE.startPainting(g, x, y, w, h, id, focused);

            // Note: the scrollbar insets already include the "trough-border",
            // which is needed to position the scrollbar buttons properly.
            // But when we render, we need to take the trough border out
            // of the equation so that we paint the entire area covered by
            // the trough border and the scrollbar content itself.
            Insets insets = context.getComponent().getInsets();
            GTKStyle style = (GTKStyle)context.getStyle();
            int troughBorder =
                style.getClassSpecificIntValue(context, "trough-border", 1);
            insets.left   -= troughBorder;
            insets.right  -= troughBorder;
            insets.top    -= troughBorder;
            insets.bottom -= troughBorder;

            ENGINE.paintBox(g, context, id, SynthConstants.PRESSED,
                            ShadowType.IN, "trough",
                            x + insets.left,
                            y + insets.top,
                            w - insets.left - insets.right,
                            h - insets.top - insets.bottom);

            if (focused) {
                ENGINE.paintFocus(g, context, id,
                        SynthConstants.ENABLED, "trough", x, y, w, h);
            }
            ENGINE.finishPainting();
        }
    }


    //
    // SCROLL_BAR_THUMB
    //
    public void paintScrollBarThumbBackground(SynthContext context,
            Graphics g, int x, int y, int w, int h, int dir) {
        Region id = context.getRegion();
        int gtkState = GTKLookAndFeel.synthStateToGTKState(
                id, context.getComponentState());

        // The clearlooks engine paints scrollbar thumbs differently
        // depending on the current scroll value (specifically, it will avoid
        // rendering a certain line when the thumb is at the starting or
        // ending position).  Therefore, we normalize the current value to
        // the range [0,100] here and then pass it down to setRangeValue()
        // so that the native widget is configured appropriately.  Note that
        // there are really only four values that matter (min, middle, max,
        // or fill) so we restrict to one of those four values to avoid
        // blowing out the image cache.
        JScrollBar sb = (JScrollBar)context.getComponent();
        boolean rtl =
            sb.getOrientation() == JScrollBar.HORIZONTAL &&
            !sb.getComponentOrientation().isLeftToRight();
        double min = 0;
        double max = 100;
        double visible = 20;
        double value;
        if (sb.getMaximum() - sb.getMinimum() == sb.getVisibleAmount()) {
            // In this case, the thumb fills the entire track, so it is
            // touching both ends at the same time
            value = 0;
            visible = 100;
        } else if (sb.getValue() == sb.getMinimum()) {
            // At minimum
            value = rtl ? 100 : 0;
        } else if (sb.getValue() >= sb.getMaximum() - sb.getVisibleAmount()) {
            // At maximum
            value = rtl ? 0 : 100;
        } else {
            // Somewhere in between
            value = 50;
        }

        synchronized (UNIXToolkit.GTK_LOCK) {
            if (! ENGINE.paintCachedImage(g, x, y, w, h, id, gtkState,
                                          dir, value, visible, rtl))
            {
                ENGINE.startPainting(g, x, y, w, h, id, gtkState,
                                     dir, value, visible, rtl);
                Orientation orientation = (dir == JScrollBar.HORIZONTAL ?
                    Orientation.HORIZONTAL : Orientation.VERTICAL);
                ENGINE.setRangeValue(context, id, value, min, max, visible);
                ENGINE.paintSlider(g, context, id, gtkState, ShadowType.OUT,
                                      "slider", x, y, w, h, orientation, false);
                ENGINE.finishPainting();
            }
        }
    }

    //
    // TOOL_TIP
    //
    public void paintToolTipBackground(SynthContext context, Graphics g,
                                        int x, int y, int w,int h) {
        Region id = context.getRegion();
        synchronized (UNIXToolkit.GTK_LOCK) {
            if (! ENGINE.paintCachedImage(g, x, y, w, h, id)) {
                ENGINE.startPainting(g, x, y, w, h, id);
                ENGINE.paintFlatBox(g, context, id, SynthConstants.ENABLED,
                        ShadowType.OUT, "tooltip", x, y, w, h,
                        ColorType.BACKGROUND);
                ENGINE.finishPainting();
            }
        }
    }


    //
    // TREE_CELL
    //
    public void paintTreeCellBackground(SynthContext context, Graphics g,
                                        int x, int y, int w, int h) {
        Region id = context.getRegion();
        int state = context.getComponentState();
        int gtkState = GTKLookAndFeel.synthStateToGTKState(id, state);
        synchronized (UNIXToolkit.GTK_LOCK) {
            if (! ENGINE.paintCachedImage(g, x, y, w, h, id, state)) {
                ENGINE.startPainting(g, x, y, w, h, id, state);
                // the string arg should alternate based on row being painted,
                // but we currently don't pass that in.
                ENGINE.paintFlatBox(g, context, id, gtkState, ShadowType.NONE,
                        "cell_odd", x, y, w, h, ColorType.TEXT_BACKGROUND);
                ENGINE.finishPainting();
            }
        }
    }

    public void paintTreeCellFocus(SynthContext context, Graphics g,
                                    int x, int y, int w, int h) {
        Region id = Region.TREE_CELL;
        int state = context.getComponentState();
        paintFocus(context, g, id, state, "treeview", x, y, w, h);
    }


    //
    // TREE
    //
    public void paintTreeBackground(SynthContext context, Graphics g,
                                    int x, int y, int w, int h) {
        // As far as I can tell, these don't call into the ENGINE.
        fillArea(context, g, x, y, w, h, GTKColorType.TEXT_BACKGROUND);
    }


    //
    // VIEWPORT
    //
    public void paintViewportBackground(SynthContext context, Graphics g,
                                        int x, int y, int w, int h) {
        // As far as I can tell, these don't call into the ENGINE.
        // Also note that you don't want this to call into the ENGINE
        // as if it where to paint a background JViewport wouldn't scroll
        // correctly.
        fillArea(context, g, x, y, w, h, GTKColorType.TEXT_BACKGROUND);
    }

    void paintFocus(SynthContext context, Graphics g, Region id,
            int state, String detail, int x, int y, int w, int h) {
        int gtkState = GTKLookAndFeel.synthStateToGTKState(id, state);
        synchronized (UNIXToolkit.GTK_LOCK) {
            if (! ENGINE.paintCachedImage(g, x, y, w, h, id, gtkState, "focus")) {
                ENGINE.startPainting(g, x, y, w, h, id, gtkState, "focus");
                ENGINE.paintFocus(g, context, id, gtkState, detail, x, y, w, h);
                ENGINE.finishPainting();
            }
        }
    }

    void paintMetacityElement(SynthContext context, Graphics g,
            int gtkState, String detail, int x, int y, int w, int h,
            ShadowType shadow, ArrowType direction) {
        synchronized (UNIXToolkit.GTK_LOCK) {
            if (! ENGINE.paintCachedImage(
                    g, x, y, w, h, gtkState, detail, shadow, direction)) {
                ENGINE.startPainting(
                        g, x, y, w, h, gtkState, detail, shadow, direction);
                if (detail == "metacity-arrow") {
                    ENGINE.paintArrow(g, context, Region.INTERNAL_FRAME_TITLE_PANE,
                            gtkState, shadow, direction, "", x, y, w, h);

                } else if (detail == "metacity-box") {
                    ENGINE.paintBox(g, context, Region.INTERNAL_FRAME_TITLE_PANE,
                            gtkState, shadow, "", x, y, w, h);

                } else if (detail == "metacity-vline") {
                    ENGINE.paintVline(g, context, Region.INTERNAL_FRAME_TITLE_PANE,
                            gtkState, "", x, y, w, h);
                }
                ENGINE.finishPainting();
            }
        }
    }

    void paintIcon(SynthContext context, Graphics g,
            Method paintMethod, int x, int y, int w, int h) {
        int state = context.getComponentState();
        synchronized (UNIXToolkit.GTK_LOCK) {
            if (! ENGINE.paintCachedImage(g, x, y, w, h, state, paintMethod)) {
                ENGINE.startPainting(g, x, y, w, h, state, paintMethod);
                try {
                    paintMethod.invoke(this, context, g, state, x, y, w, h);
                } catch (IllegalAccessException iae) {
                    assert false;
                } catch (InvocationTargetException ite) {
                    assert false;
                }
                ENGINE.finishPainting();
            }
        }
    }

    void paintIcon(SynthContext context, Graphics g,
            Method paintMethod, int x, int y, int w, int h, Object direction) {
        int state = context.getComponentState();
        synchronized (UNIXToolkit.GTK_LOCK) {
            if (! ENGINE.paintCachedImage(g,
                    x, y, w, h, state, paintMethod, direction)) {
                ENGINE.startPainting(g,
                        x, y, w, h, state, paintMethod, direction);
                try {
                    paintMethod.invoke(this, context,
                            g, state, x, y, w, h, direction);
                } catch (IllegalAccessException iae) {
                    assert false;
                } catch (InvocationTargetException ite) {
                    assert false;
                }
                ENGINE.finishPainting();
            }
        }
    }

    // All icon painting methods are called from under GTK_LOCK

    public void paintTreeExpandedIcon(SynthContext context,
            Graphics g, int state, int x, int y, int w, int h) {
        ENGINE.paintExpander(g, context, Region.TREE,
                GTKLookAndFeel.synthStateToGTKState(context.getRegion(), state),
                ExpanderStyle.EXPANDED, "expander", x, y, w, h);
    }

    public void paintTreeCollapsedIcon(SynthContext context,
            Graphics g, int state, int x, int y, int w, int h) {
        ENGINE.paintExpander(g, context, Region.TREE,
                GTKLookAndFeel.synthStateToGTKState(context.getRegion(), state),
                ExpanderStyle.COLLAPSED, "expander", x, y, w, h);
    }

    public void paintCheckBoxIcon(SynthContext context,
            Graphics g, int state, int x, int y, int w, int h) {
        GTKStyle style = (GTKStyle)context.getStyle();
        int size = style.getClassSpecificIntValue(context,
                        "indicator-size", GTKIconFactory.DEFAULT_ICON_SIZE);
        int offset = style.getClassSpecificIntValue(context,
                        "indicator-spacing", GTKIconFactory.DEFAULT_ICON_SPACING);

        ENGINE.paintCheck(g, context, Region.CHECK_BOX, "checkbutton",
                x+offset, y+offset, size, size);
    }

    public void paintRadioButtonIcon(SynthContext context,
            Graphics g, int state, int x, int y, int w, int h) {
        GTKStyle style = (GTKStyle)context.getStyle();
        int size = style.getClassSpecificIntValue(context,
                        "indicator-size", GTKIconFactory.DEFAULT_ICON_SIZE);
        int offset = style.getClassSpecificIntValue(context,
                        "indicator-spacing", GTKIconFactory.DEFAULT_ICON_SPACING);

        ENGINE.paintOption(g, context, Region.RADIO_BUTTON, "radiobutton",
                x+offset, y+offset, size, size);
    }

    public void paintMenuArrowIcon(SynthContext context, Graphics g,
            int state, int x, int y, int w, int h, ArrowType dir) {
        int gtkState = GTKLookAndFeel.synthStateToGTKState(
                context.getRegion(), state);
        ShadowType shadow = ShadowType.OUT;
        if (gtkState == SynthConstants.MOUSE_OVER) {
            shadow = ShadowType.IN;
        }
        if (!GTKLookAndFeel.is3()) {
            x += 3;
            y += 3;
            w = h = 7;
        }
        ENGINE.paintArrow(g, context, Region.MENU_ITEM, gtkState, shadow,
                dir, "menuitem", x, y, w, h);
    }

    public void paintCheckBoxMenuItemCheckIcon(SynthContext context,
            Graphics g, int state, int x, int y, int w, int h) {

        GTKStyle style = (GTKStyle)context.getStyle();
        int size = style.getClassSpecificIntValue(context,"indicator-size",
                GTKIconFactory.DEFAULT_TOGGLE_MENU_ITEM_SIZE);

        ENGINE.paintCheck(g, context, Region.CHECK_BOX_MENU_ITEM, "check",
                x + GTKIconFactory.CHECK_ICON_EXTRA_INSET,
                y + GTKIconFactory.CHECK_ICON_EXTRA_INSET,
                size, size);
    }

    public void paintRadioButtonMenuItemCheckIcon(SynthContext context,
            Graphics g, int state, int x, int y, int w, int h) {

        GTKStyle style = (GTKStyle)context.getStyle();
        int size = style.getClassSpecificIntValue(context,"indicator-size",
                GTKIconFactory.DEFAULT_TOGGLE_MENU_ITEM_SIZE);

        ENGINE.paintOption(g, context, Region.RADIO_BUTTON_MENU_ITEM, "option",
                x + GTKIconFactory.CHECK_ICON_EXTRA_INSET,
                y + GTKIconFactory.CHECK_ICON_EXTRA_INSET,
                size, size);
    }

    public void paintToolBarHandleIcon(SynthContext context, Graphics g,
            int state, int x, int y, int w, int h, Orientation orientation) {
        int gtkState = GTKLookAndFeel.synthStateToGTKState(
                context.getRegion(), state);

        // The orientation parameter passed down by Synth refers to the
        // orientation of the toolbar, but the one we pass to GTK refers
        // to the orientation of the handle.  Therefore, we need to swap
        // the value here: horizontal toolbars have vertical handles, and
        // vice versa.
        orientation = (orientation == Orientation.HORIZONTAL) ?
            Orientation.VERTICAL : Orientation.HORIZONTAL;

        ENGINE.paintHandle(g, context, Region.TOOL_BAR, gtkState,
                ShadowType.OUT, "handlebox", x, y, w, h, orientation);
    }

    public void paintAscendingSortIcon(SynthContext context,
            Graphics g, int state, int x, int y, int w, int h) {
        ENGINE.paintArrow(g, context, Region.TABLE, SynthConstants.ENABLED,
                ShadowType.IN, ArrowType.UP, "arrow", x, y, w, h);
    }

    public void paintDescendingSortIcon(SynthContext context,
            Graphics g, int state, int x, int y, int w, int h) {
        ENGINE.paintArrow(g, context, Region.TABLE, SynthConstants.ENABLED,
                ShadowType.IN, ArrowType.DOWN, "arrow", x, y, w, h);
    }

    /*
     * Fill an area with color determined from this context's Style using the
     * specified GTKColorType
     */
    private void fillArea(SynthContext context, Graphics g,
                          int x, int y, int w, int h, ColorType colorType) {
        if (context.getComponent().isOpaque()) {
            Region id = context.getRegion();
            int gtkState = GTKLookAndFeel.synthStateToGTKState(id,
                    context.getComponentState());
            GTKStyle style = (GTKStyle)context.getStyle();

            g.setColor(style.getGTKColor(context, gtkState, colorType));
            g.fillRect(x, y, w, h);
        }
    }

    // Refer to GTKLookAndFeel for details on this.
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    static class ListTableFocusBorder extends AbstractBorder implements
                          UIResource {

        private boolean selectedCell;
        private boolean focusedCell;

        public static ListTableFocusBorder getSelectedCellBorder() {
            return new ListTableFocusBorder(true, true);
        }

        public static ListTableFocusBorder getUnselectedCellBorder() {
            return new ListTableFocusBorder(false, true);
        }

        public static ListTableFocusBorder getNoFocusCellBorder() {
            return new ListTableFocusBorder(false, false);
        }

        public ListTableFocusBorder(boolean selectedCell, boolean focusedCell) {
            this.selectedCell = selectedCell;
            this.focusedCell = focusedCell;
        }

        private SynthContext getContext(Component c) {
            SynthContext context = null;

            ComponentUI ui = null;
            if (c instanceof JLabel) {
                ui = ((JLabel)c).getUI();
            }

            if (ui instanceof SynthUI) {
                context = ((SynthUI)ui).getContext((JComponent)c);
            }

            return context;
        }

        public void paintBorder(Component c, Graphics g, int x, int y,
                                int w, int h) {
            if (focusedCell) {
                SynthContext context = getContext(c);
                int state = (selectedCell? SynthConstants.SELECTED:
                             SynthConstants.FOCUSED | SynthConstants.ENABLED);

                if (context != null) {
                    GTKPainter.INSTANCE.paintFocus(context, g,
                            Region.TABLE, state, "", x, y, w, h);
                }
            }
        }

        public Insets getBorderInsets(Component c, Insets i) {
            SynthContext context = getContext(c);

            if (context != null) {
                i = context.getStyle().getInsets(context, i);
            }

            return i;
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

    // TitledBorder implementation for GTK L&F
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    static class TitledBorder extends AbstractBorder implements UIResource {

        public void paintBorder(Component c, Graphics g, int x, int y,
                                int w, int h) {
            SynthContext context = getContext((JComponent)c);
            Region id = context.getRegion();
            int state = context.getComponentState();
            int gtkState = GTKLookAndFeel.synthStateToGTKState(id, state);

            synchronized (UNIXToolkit.GTK_LOCK) {
                if (! ENGINE.paintCachedImage(g, x, y, w, h, id)) {
                    ENGINE.startPainting(g, x, y, w, h, id);
                    ENGINE.paintShadow(g, context, id, gtkState, ShadowType.ETCHED_IN,
                                      "frame", x, y, w, h);
                    ENGINE.finishPainting();
                }
            }
        }

        public Insets getBorderInsets(Component c, Insets i) {
            SynthContext context = getContext((JComponent)c);
            return context.getStyle().getInsets(context, i);
        }

        public boolean isBorderOpaque() {
            return true;
        }

        private SynthStyle getStyle(JComponent c) {
            return SynthLookAndFeel.getStyle(c, GTKEngine.CustomRegion.TITLED_BORDER);
        }

        private SynthContext getContext(JComponent c) {
            int state = SynthConstants.DEFAULT;
            return new SynthContext(c, GTKEngine.CustomRegion.TITLED_BORDER,
                                    getStyle(c), state);
        }
    }
}
