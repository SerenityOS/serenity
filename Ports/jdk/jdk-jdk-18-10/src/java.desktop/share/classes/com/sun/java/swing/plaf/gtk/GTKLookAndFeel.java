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
import java.beans.*;
import java.io.File;
import java.lang.ref.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;
import javax.swing.*;
import javax.swing.colorchooser.*;
import javax.swing.plaf.*;
import javax.swing.plaf.synth.*;
import javax.swing.text.DefaultEditorKit;

import com.sun.java.swing.plaf.gtk.GTKConstants.PositionType;
import com.sun.java.swing.plaf.gtk.GTKConstants.StateType;
import java.util.HashMap;
import java.util.Map;
import sun.awt.SunToolkit;
import sun.awt.UNIXToolkit;
import sun.awt.OSInfo;
import sun.security.action.GetPropertyAction;
import sun.swing.DefaultLayoutStyle;
import sun.swing.SwingAccessor;
import sun.swing.SwingUtilities2;

/**
 * @author Scott Violet
 */
@SuppressWarnings("serial") // Superclass not serializable
public class GTKLookAndFeel extends SynthLookAndFeel {
    private static boolean IS_22;
    private static boolean IS_3;

    /**
     * Whether or not text is drawn antialiased.  This keys off the
     * desktop property 'gnome.Xft/Antialias' and 'gnome.Xft/RGBA'
     * We should assume ON - or some variation of ON as no GTK desktop
     * ships with it OFF.
     */
    static Map<Object, Object> aaTextInfo;

    /*
     * Used to override if system (desktop) text anti-aliasing settings should
     * be used. The reasons for this are are is that currently its "off"
     * for CJK locales which is not likely to be a good universal answer, and
     * also its off for remote display. So this provides an unsupported
     * way to explicitly request that it be "on".
     */
    private static boolean gtkAAFontSettingsCond;

    /**
     * Font to use in places where there is no widget.
     */
    private Font fallbackFont;

    /**
     * If true, GTKLookAndFeel is inside the <code>initialize</code>
     * method.
     */
    private boolean inInitialize;

    /**
     * If true, PropertyChangeListeners have been installed for the
     * Toolkit.
     */
    private boolean pclInstalled;

    /**
     * StyleFactory needs to be created only the first time.
     */
    private GTKStyleFactory styleFactory;

    /**
     * Cached theme name. Used by GTKGraphicsUtils
     */
    private static String gtkThemeName = "Default";

    /**
     * Returns true if running on system containing at least 2.2.
     */
    static boolean is2_2() {
        // NOTE: We're currently hard coding to use 2.2.
        // If we want to support both GTK 2.0 and 2.2, we'll
        // need to get the major/minor/micro version from the .so.
        // Refer to bug 4912613 for details.
        return IS_22;
    }

    static boolean is3() {
        return IS_3;
    }

    /**
     * Maps a swing constant to a GTK constant.
     */
    static PositionType SwingOrientationConstantToGTK(int side) {
        switch (side) {
        case SwingConstants.LEFT:
            return PositionType.LEFT;
        case SwingConstants.RIGHT:
            return PositionType.RIGHT;
        case SwingConstants.TOP:
            return PositionType.TOP;
        case SwingConstants.BOTTOM:
            return PositionType.BOTTOM;
        }
        assert false : "Unknown orientation: " + side;
        return PositionType.TOP;
    }

    /**
     * Maps from Synth state to native GTK state using typesafe enumeration
     * StateType.  This is only used by GTKEngine.
     */
    static StateType synthStateToGTKStateType(int state) {
        StateType result;
        switch (state) {
            case SynthConstants.PRESSED:
                result = StateType.ACTIVE;
                break;
            case SynthConstants.MOUSE_OVER:
                result = StateType.PRELIGHT;
                break;
            case SynthConstants.SELECTED:
                result = StateType.SELECTED;
                break;
            case SynthConstants.DISABLED:
                result = StateType.INSENSITIVE;
                break;
            case SynthConstants.ENABLED:
            default:
                result = StateType.NORMAL;
                break;
        }
        return result;
    }

    /**
     * Maps from a Synth state to the corresponding GTK state.
     * The GTK states are named differently than Synth's states, the
     * following gives the mapping:
     * <table><tr><td>Synth<td>GTK
     * <tr><td>SynthConstants.PRESSED<td>ACTIVE
     * <tr><td>SynthConstants.SELECTED<td>SELECTED
     * <tr><td>SynthConstants.MOUSE_OVER<td>PRELIGHT
     * <tr><td>SynthConstants.DISABLED<td>INSENSITIVE
     * <tr><td>SynthConstants.ENABLED<td>NORMAL
     * </table>
     * Additionally some widgets are special cased.
     */
    static int synthStateToGTKState(Region region, int state) {
        if ((state & SynthConstants.PRESSED) != 0) {
            if (region == Region.RADIO_BUTTON
                    || region == Region.CHECK_BOX
                    || region == Region.MENU
                    || region == Region.MENU_ITEM
                    || region == Region.RADIO_BUTTON_MENU_ITEM
                    || region == Region.CHECK_BOX_MENU_ITEM
                    || region == Region.SPLIT_PANE) {
                state = SynthConstants.MOUSE_OVER;
            } else {
                state = SynthConstants.PRESSED;
            }

        } else if (region == Region.TABBED_PANE_TAB) {
            if ((state & SynthConstants.DISABLED) != 0) {
                state = SynthConstants.DISABLED;
            }
            else if ((state & SynthConstants.SELECTED) != 0) {
                state = SynthConstants.ENABLED;
            } else {
                state = SynthConstants.PRESSED;
            }

        } else if ((state & SynthConstants.SELECTED) != 0) {
            if (region == Region.MENU) {
                state = SynthConstants.MOUSE_OVER;
            } else if (region == Region.RADIO_BUTTON ||
                          region == Region.TOGGLE_BUTTON ||
                          region == Region.RADIO_BUTTON_MENU_ITEM ||
                          region == Region.CHECK_BOX_MENU_ITEM ||
                          region == Region.CHECK_BOX ||
                          region == Region.BUTTON) {
                if ((state & SynthConstants.DISABLED) != 0) {
                    state = SynthConstants.DISABLED;
                }
                // If the button is SELECTED and is PRELIGHT we need to
                // make the state MOUSE_OVER otherwise we don't paint the
                // PRELIGHT.
                else if ((state & SynthConstants.MOUSE_OVER) != 0) {
                    state = SynthConstants.MOUSE_OVER;
                } else {
                    state = SynthConstants.PRESSED;
                }
            } else {
                state = SynthConstants.SELECTED;
            }
        }

        else if ((state & SynthConstants.MOUSE_OVER) != 0) {
            state = SynthConstants.MOUSE_OVER;
        }
        else if ((state & SynthConstants.DISABLED) != 0) {
            state = SynthConstants.DISABLED;
        }
        else {
            if (region == Region.SLIDER_TRACK) {
                state = SynthConstants.PRESSED;
            } else {
                state = SynthConstants.ENABLED;
            }
        }
        return state;
    }

    static boolean isText(Region region) {
        // These Regions treat FOREGROUND as TEXT.
        return (region == Region.TEXT_FIELD ||
                region == Region.FORMATTED_TEXT_FIELD ||
                region == Region.LIST ||
                region == Region.PASSWORD_FIELD ||
                region == Region.SPINNER ||
                region == Region.TABLE ||
                region == Region.TEXT_AREA ||
                region == Region.TEXT_PANE ||
                region == Region.TREE);
    }

    public UIDefaults getDefaults() {
        // We need to call super for basic's properties file.
        UIDefaults table = super.getDefaults();

        // SynthTabbedPaneUI supports rollover on tabs, GTK does not
        table.put("TabbedPane.isTabRollover", Boolean.FALSE);

        // Prevents Synth from setting text AA by itself
        table.put("Synth.doNotSetTextAA", true);

        initResourceBundle(table);
        // For compatibility with apps expecting certain defaults we'll
        // populate the table with the values from basic.
        initSystemColorDefaults(table);
        initComponentDefaults(table);
        installPropertyChangeListeners();
        return table;
    }

    private void installPropertyChangeListeners() {
        if(!pclInstalled) {
            Toolkit kit = Toolkit.getDefaultToolkit();
            WeakPCL pcl = new WeakPCL(this, kit, "gnome.Net/ThemeName");
            kit.addPropertyChangeListener(pcl.getKey(), pcl);
            pcl = new WeakPCL(this, kit, "gnome.Gtk/FontName");
            kit.addPropertyChangeListener(pcl.getKey(), pcl);
            pcl = new WeakPCL(this, kit, "gnome.Xft/DPI");
            kit.addPropertyChangeListener(pcl.getKey(), pcl);

            flushUnreferenced();
            pclInstalled = true;
        }
    }

    private void initResourceBundle(UIDefaults table) {
        SwingAccessor.getUIDefaultsAccessor()
                     .addInternalBundle(table,
                             "com.sun.java.swing.plaf.gtk.resources.gtk");
    }

    protected void initComponentDefaults(UIDefaults table) {
        // For compatibility with apps expecting certain defaults we'll
        // populate the table with the values from basic.
        super.initComponentDefaults(table);

        UIDefaults.LazyValue zeroBorder =
            t -> new BorderUIResource.EmptyBorderUIResource(0, 0, 0, 0);

        Object focusBorder = new GTKStyle.GTKLazyValue(
            "com.sun.java.swing.plaf.gtk.GTKPainter$ListTableFocusBorder",
            "getUnselectedCellBorder");
        Object focusSelectedBorder = new GTKStyle.GTKLazyValue(
            "com.sun.java.swing.plaf.gtk.GTKPainter$ListTableFocusBorder",
            "getSelectedCellBorder");
        Object noFocusBorder = new GTKStyle.GTKLazyValue(
            "com.sun.java.swing.plaf.gtk.GTKPainter$ListTableFocusBorder",
            "getNoFocusCellBorder");

        SynthStyleFactory factory = getStyleFactory();
        GTKStyle tableStyle = (GTKStyle)factory.getStyle(null, Region.TREE);
        Color tableBg = tableStyle.getGTKColor(SynthConstants.ENABLED,
                GTKColorType.TEXT_BACKGROUND);
        Color tableFocusCellBg = tableStyle.getGTKColor(SynthConstants.ENABLED,
                GTKColorType.BACKGROUND);
        Color tableFocusCellFg = tableStyle.getGTKColor(SynthConstants.ENABLED,
                GTKColorType.FOREGROUND);

        // The following progress bar size calculations come from
        // gtkprogressbar.c (version 2.8.20), see MIN_* constants and
        // the gtk_progress_bar_size_request() method.
        GTKStyle progStyle = (GTKStyle)
            factory.getStyle(null, Region.PROGRESS_BAR);
        int progXThickness = progStyle.getXThickness();
        int progYThickness = progStyle.getYThickness();
        int hProgWidth  = 150 - (progXThickness * 2);
        int hProgHeight =  20 - (progYThickness * 2);
        int vProgWidth  =  22 - (progXThickness * 2);
        int vProgHeight =  80 - (progYThickness * 2);

        Integer caretBlinkRate;
        if (Boolean.FALSE.equals(GTKEngine.INSTANCE.getSetting(
                GTKEngine.Settings.GTK_CURSOR_BLINK))) {
            caretBlinkRate = Integer.valueOf(0);
        } else {
            caretBlinkRate = (Integer) GTKEngine.INSTANCE.getSetting(
                    GTKEngine.Settings.GTK_CURSOR_BLINK_TIME);
            if (caretBlinkRate == null) {
                caretBlinkRate = Integer.valueOf(500);
            }
        }
        Insets zeroInsets = new InsetsUIResource(0, 0, 0, 0);

        Double defaultCaretAspectRatio = Double.valueOf(0.025);
        Color caretColor = table.getColor("caretColor");
        Color controlText = table.getColor("controlText");

        Object fieldInputMap = new UIDefaults.LazyInputMap(new Object[] {
                       "ctrl C", DefaultEditorKit.copyAction,
                       "ctrl V", DefaultEditorKit.pasteAction,
                       "ctrl X", DefaultEditorKit.cutAction,
                         "COPY", DefaultEditorKit.copyAction,
                        "PASTE", DefaultEditorKit.pasteAction,
                          "CUT", DefaultEditorKit.cutAction,
               "control INSERT", DefaultEditorKit.copyAction,
                 "shift INSERT", DefaultEditorKit.pasteAction,
                 "shift DELETE", DefaultEditorKit.cutAction,
                   "shift LEFT", DefaultEditorKit.selectionBackwardAction,
                "shift KP_LEFT", DefaultEditorKit.selectionBackwardAction,
                  "shift RIGHT", DefaultEditorKit.selectionForwardAction,
               "shift KP_RIGHT", DefaultEditorKit.selectionForwardAction,
                    "ctrl LEFT", DefaultEditorKit.previousWordAction,
                 "ctrl KP_LEFT", DefaultEditorKit.previousWordAction,
                   "ctrl RIGHT", DefaultEditorKit.nextWordAction,
                "ctrl KP_RIGHT", DefaultEditorKit.nextWordAction,
              "ctrl shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
           "ctrl shift KP_LEFT", DefaultEditorKit.selectionPreviousWordAction,
             "ctrl shift RIGHT", DefaultEditorKit.selectionNextWordAction,
          "ctrl shift KP_RIGHT", DefaultEditorKit.selectionNextWordAction,
                       "ctrl A", DefaultEditorKit.selectAllAction,
                         "HOME", DefaultEditorKit.beginLineAction,
                          "END", DefaultEditorKit.endLineAction,
                   "shift HOME", DefaultEditorKit.selectionBeginLineAction,
                    "shift END", DefaultEditorKit.selectionEndLineAction,
                   "BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
             "shift BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
                       "ctrl H", DefaultEditorKit.deletePrevCharAction,
                       "DELETE", DefaultEditorKit.deleteNextCharAction,
                  "ctrl DELETE", DefaultEditorKit.deleteNextWordAction,
              "ctrl BACK_SPACE", DefaultEditorKit.deletePrevWordAction,
                        "RIGHT", DefaultEditorKit.forwardAction,
                         "LEFT", DefaultEditorKit.backwardAction,
                     "KP_RIGHT", DefaultEditorKit.forwardAction,
                      "KP_LEFT", DefaultEditorKit.backwardAction,
                        "ENTER", JTextField.notifyAction,
              "ctrl BACK_SLASH", "unselect"/*DefaultEditorKit.unselectAction*/,
               "control shift O", "toggle-componentOrientation"/*DefaultEditorKit.toggleComponentOrientation*/
            });

        Object passwordInputMap = new UIDefaults.LazyInputMap(new Object[] {
                       "ctrl C", DefaultEditorKit.copyAction,
                       "ctrl V", DefaultEditorKit.pasteAction,
                       "ctrl X", DefaultEditorKit.cutAction,
                         "COPY", DefaultEditorKit.copyAction,
                        "PASTE", DefaultEditorKit.pasteAction,
                          "CUT", DefaultEditorKit.cutAction,
               "control INSERT", DefaultEditorKit.copyAction,
                 "shift INSERT", DefaultEditorKit.pasteAction,
                 "shift DELETE", DefaultEditorKit.cutAction,
                   "shift LEFT", DefaultEditorKit.selectionBackwardAction,
                "shift KP_LEFT", DefaultEditorKit.selectionBackwardAction,
                  "shift RIGHT", DefaultEditorKit.selectionForwardAction,
               "shift KP_RIGHT", DefaultEditorKit.selectionForwardAction,
                    "ctrl LEFT", DefaultEditorKit.beginLineAction,
                 "ctrl KP_LEFT", DefaultEditorKit.beginLineAction,
                   "ctrl RIGHT", DefaultEditorKit.endLineAction,
                "ctrl KP_RIGHT", DefaultEditorKit.endLineAction,
              "ctrl shift LEFT", DefaultEditorKit.selectionBeginLineAction,
           "ctrl shift KP_LEFT", DefaultEditorKit.selectionBeginLineAction,
             "ctrl shift RIGHT", DefaultEditorKit.selectionEndLineAction,
          "ctrl shift KP_RIGHT", DefaultEditorKit.selectionEndLineAction,
                       "ctrl A", DefaultEditorKit.selectAllAction,
                         "HOME", DefaultEditorKit.beginLineAction,
                          "END", DefaultEditorKit.endLineAction,
                   "shift HOME", DefaultEditorKit.selectionBeginLineAction,
                    "shift END", DefaultEditorKit.selectionEndLineAction,
                   "BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
             "shift BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
                       "ctrl H", DefaultEditorKit.deletePrevCharAction,
                       "DELETE", DefaultEditorKit.deleteNextCharAction,
                        "RIGHT", DefaultEditorKit.forwardAction,
                         "LEFT", DefaultEditorKit.backwardAction,
                     "KP_RIGHT", DefaultEditorKit.forwardAction,
                      "KP_LEFT", DefaultEditorKit.backwardAction,
                        "ENTER", JTextField.notifyAction,
              "ctrl BACK_SLASH", "unselect"/*DefaultEditorKit.unselectAction*/,
               "control shift O", "toggle-componentOrientation"/*DefaultEditorKit.toggleComponentOrientation*/
            });

        Object editorMargin = new InsetsUIResource(3,3,3,3);

        Object multilineInputMap = new UIDefaults.LazyInputMap(new Object[] {
                           "ctrl C", DefaultEditorKit.copyAction,
                           "ctrl V", DefaultEditorKit.pasteAction,
                           "ctrl X", DefaultEditorKit.cutAction,
                             "COPY", DefaultEditorKit.copyAction,
                            "PASTE", DefaultEditorKit.pasteAction,
                              "CUT", DefaultEditorKit.cutAction,
                   "control INSERT", DefaultEditorKit.copyAction,
                     "shift INSERT", DefaultEditorKit.pasteAction,
                     "shift DELETE", DefaultEditorKit.cutAction,
                       "shift LEFT", DefaultEditorKit.selectionBackwardAction,
                    "shift KP_LEFT", DefaultEditorKit.selectionBackwardAction,
                      "shift RIGHT", DefaultEditorKit.selectionForwardAction,
                   "shift KP_RIGHT", DefaultEditorKit.selectionForwardAction,
                        "ctrl LEFT", DefaultEditorKit.previousWordAction,
                     "ctrl KP_LEFT", DefaultEditorKit.previousWordAction,
                       "ctrl RIGHT", DefaultEditorKit.nextWordAction,
                    "ctrl KP_RIGHT", DefaultEditorKit.nextWordAction,
                  "ctrl shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
               "ctrl shift KP_LEFT", DefaultEditorKit.selectionPreviousWordAction,
                 "ctrl shift RIGHT", DefaultEditorKit.selectionNextWordAction,
              "ctrl shift KP_RIGHT", DefaultEditorKit.selectionNextWordAction,
                           "ctrl A", DefaultEditorKit.selectAllAction,
                             "HOME", DefaultEditorKit.beginLineAction,
                              "END", DefaultEditorKit.endLineAction,
                       "shift HOME", DefaultEditorKit.selectionBeginLineAction,
                        "shift END", DefaultEditorKit.selectionEndLineAction,

                               "UP", DefaultEditorKit.upAction,
                            "KP_UP", DefaultEditorKit.upAction,
                             "DOWN", DefaultEditorKit.downAction,
                          "KP_DOWN", DefaultEditorKit.downAction,
                          "PAGE_UP", DefaultEditorKit.pageUpAction,
                        "PAGE_DOWN", DefaultEditorKit.pageDownAction,
                    "shift PAGE_UP", "selection-page-up",
                  "shift PAGE_DOWN", "selection-page-down",
               "ctrl shift PAGE_UP", "selection-page-left",
             "ctrl shift PAGE_DOWN", "selection-page-right",
                         "shift UP", DefaultEditorKit.selectionUpAction,
                      "shift KP_UP", DefaultEditorKit.selectionUpAction,
                       "shift DOWN", DefaultEditorKit.selectionDownAction,
                    "shift KP_DOWN", DefaultEditorKit.selectionDownAction,
                            "ENTER", DefaultEditorKit.insertBreakAction,
                       "BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
                 "shift BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
                           "ctrl H", DefaultEditorKit.deletePrevCharAction,
                           "DELETE", DefaultEditorKit.deleteNextCharAction,
                      "ctrl DELETE", DefaultEditorKit.deleteNextWordAction,
                  "ctrl BACK_SPACE", DefaultEditorKit.deletePrevWordAction,
                            "RIGHT", DefaultEditorKit.forwardAction,
                             "LEFT", DefaultEditorKit.backwardAction,
                         "KP_RIGHT", DefaultEditorKit.forwardAction,
                          "KP_LEFT", DefaultEditorKit.backwardAction,
                              "TAB", DefaultEditorKit.insertTabAction,
                  "ctrl BACK_SLASH", "unselect"/*DefaultEditorKit.unselectAction*/,
                        "ctrl HOME", DefaultEditorKit.beginAction,
                         "ctrl END", DefaultEditorKit.endAction,
                  "ctrl shift HOME", DefaultEditorKit.selectionBeginAction,
                   "ctrl shift END", DefaultEditorKit.selectionEndAction,
                           "ctrl T", "next-link-action",
                     "ctrl shift T", "previous-link-action",
                       "ctrl SPACE", "activate-link-action",
                   "control shift O", "toggle-componentOrientation"/*DefaultEditorKit.toggleComponentOrientation*/
            });

        class FontLazyValue implements UIDefaults.LazyValue {
            private Region region;
            FontLazyValue(Region region) {
                this.region = region;
            }
            public Object createValue(UIDefaults table) {
                SynthStyleFactory factory = getStyleFactory();
                GTKStyle style = (GTKStyle)factory.getStyle(null, region);
                return style.getDefaultFont();
            }
        }

        Object[] defaults = new Object[] {
            "ArrowButton.size", Integer.valueOf(13),


            "Button.defaultButtonFollowsFocus", Boolean.FALSE,
            "Button.focusInputMap", new UIDefaults.LazyInputMap(new Object[] {
                         "SPACE", "pressed",
                "released SPACE", "released",
                         "ENTER", "pressed",
                "released ENTER", "released"
              }),
            "Button.font", new FontLazyValue(Region.BUTTON),
            "Button.margin", zeroInsets,


            "CheckBox.focusInputMap", new UIDefaults.LazyInputMap(new Object[]{
                         "SPACE", "pressed",
                "released SPACE", "released"
              }),
            "CheckBox.icon", new GTKStyle.GTKLazyValue(
                              "com.sun.java.swing.plaf.gtk.GTKIconFactory",
                              "getCheckBoxIcon"),
            "CheckBox.font", new FontLazyValue(Region.CHECK_BOX),
            "CheckBox.margin", zeroInsets,


            "CheckBoxMenuItem.arrowIcon", null,
            "CheckBoxMenuItem.checkIcon", new GTKStyle.GTKLazyValue(
                              "com.sun.java.swing.plaf.gtk.GTKIconFactory",
                              "getCheckBoxMenuItemCheckIcon"),
            "CheckBoxMenuItem.font",
                new FontLazyValue(Region.CHECK_BOX_MENU_ITEM),
            "CheckBoxMenuItem.margin", zeroInsets,
            "CheckBoxMenuItem.alignAcceleratorText", Boolean.FALSE,


            "ColorChooser.showPreviewPanelText", Boolean.FALSE,
            "ColorChooser.panels", new UIDefaults.ActiveValue() {
                public Object createValue(UIDefaults table) {
                    return new AbstractColorChooserPanel[] {
                                       new GTKColorChooserPanel() };
                }
            },
            "ColorChooser.font", new FontLazyValue(Region.COLOR_CHOOSER),


            "ComboBox.ancestorInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                     "ESCAPE", "hidePopup",
                    "PAGE_UP", "pageUpPassThrough",
                  "PAGE_DOWN", "pageDownPassThrough",
                       "HOME", "homePassThrough",
                        "END", "endPassThrough",
                       "DOWN", "selectNext",
                    "KP_DOWN", "selectNext",
                   "alt DOWN", "togglePopup",
                "alt KP_DOWN", "togglePopup",
                     "alt UP", "togglePopup",
                  "alt KP_UP", "togglePopup",
                      "SPACE", "spacePopup",
                      "ENTER", "enterPressed",
                         "UP", "selectPrevious",
                      "KP_UP", "selectPrevious"

                 }),
            "ComboBox.font", new FontLazyValue(Region.COMBO_BOX),
            "ComboBox.isEnterSelectablePopup", Boolean.TRUE,


            "EditorPane.caretForeground", caretColor,
            "EditorPane.caretAspectRatio", defaultCaretAspectRatio,
            "EditorPane.caretBlinkRate", caretBlinkRate,
            "EditorPane.margin", editorMargin,
            "EditorPane.focusInputMap", multilineInputMap,
            "EditorPane.font", new FontLazyValue(Region.EDITOR_PANE),


            "FileChooser.ancestorInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                     "ESCAPE", "cancelSelection",
                 "ctrl ENTER", "approveSelection"
                 }),
            "FileChooserUI", "com.sun.java.swing.plaf.gtk.GTKLookAndFeel",


            "FormattedTextField.caretForeground", caretColor,
            "FormattedTextField.caretAspectRatio", defaultCaretAspectRatio,
            "FormattedTextField.caretBlinkRate", caretBlinkRate,
            "FormattedTextField.focusInputMap",
              new UIDefaults.LazyInputMap(new Object[] {
                           "ctrl C", DefaultEditorKit.copyAction,
                           "ctrl V", DefaultEditorKit.pasteAction,
                           "ctrl X", DefaultEditorKit.cutAction,
                             "COPY", DefaultEditorKit.copyAction,
                            "PASTE", DefaultEditorKit.pasteAction,
                              "CUT", DefaultEditorKit.cutAction,
                   "control INSERT", DefaultEditorKit.copyAction,
                     "shift INSERT", DefaultEditorKit.pasteAction,
                     "shift DELETE", DefaultEditorKit.cutAction,
                       "shift LEFT", DefaultEditorKit.selectionBackwardAction,
                    "shift KP_LEFT", DefaultEditorKit.selectionBackwardAction,
                      "shift RIGHT", DefaultEditorKit.selectionForwardAction,
                   "shift KP_RIGHT", DefaultEditorKit.selectionForwardAction,
                        "ctrl LEFT", DefaultEditorKit.previousWordAction,
                     "ctrl KP_LEFT", DefaultEditorKit.previousWordAction,
                       "ctrl RIGHT", DefaultEditorKit.nextWordAction,
                    "ctrl KP_RIGHT", DefaultEditorKit.nextWordAction,
                  "ctrl shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
               "ctrl shift KP_LEFT", DefaultEditorKit.selectionPreviousWordAction,
                 "ctrl shift RIGHT", DefaultEditorKit.selectionNextWordAction,
              "ctrl shift KP_RIGHT", DefaultEditorKit.selectionNextWordAction,
                           "ctrl A", DefaultEditorKit.selectAllAction,
                             "HOME", DefaultEditorKit.beginLineAction,
                              "END", DefaultEditorKit.endLineAction,
                       "shift HOME", DefaultEditorKit.selectionBeginLineAction,
                        "shift END", DefaultEditorKit.selectionEndLineAction,
                       "BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
                 "shift BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
                           "ctrl H", DefaultEditorKit.deletePrevCharAction,
                           "DELETE", DefaultEditorKit.deleteNextCharAction,
                      "ctrl DELETE", DefaultEditorKit.deleteNextWordAction,
                  "ctrl BACK_SPACE", DefaultEditorKit.deletePrevWordAction,
                            "RIGHT", DefaultEditorKit.forwardAction,
                             "LEFT", DefaultEditorKit.backwardAction,
                         "KP_RIGHT", DefaultEditorKit.forwardAction,
                          "KP_LEFT", DefaultEditorKit.backwardAction,
                            "ENTER", JTextField.notifyAction,
                  "ctrl BACK_SLASH", "unselect",
                  "control shift O", "toggle-componentOrientation",
                           "ESCAPE", "reset-field-edit",
                               "UP", "increment",
                            "KP_UP", "increment",
                             "DOWN", "decrement",
                          "KP_DOWN", "decrement",
              }),
            "FormattedTextField.font",
                new FontLazyValue(Region.FORMATTED_TEXT_FIELD),


            "InternalFrameTitlePane.titlePaneLayout",
                                new GTKStyle.GTKLazyValue("com.sun.java.swing.plaf.gtk.Metacity",
                                                 "getTitlePaneLayout"),
            "InternalFrame.windowBindings", new Object[] {
                  "shift ESCAPE", "showSystemMenu",
                    "ctrl SPACE", "showSystemMenu",
                        "ESCAPE", "hideSystemMenu" },
            "InternalFrame.layoutTitlePaneAtOrigin", Boolean.TRUE,
            "InternalFrame.useTaskBar", Boolean.TRUE,

            "InternalFrameTitlePane.iconifyButtonOpacity", null,
            "InternalFrameTitlePane.maximizeButtonOpacity", null,
            "InternalFrameTitlePane.closeButtonOpacity", null,

            "Label.font", new FontLazyValue(Region.LABEL),

            "List.background", tableBg,
            "List.focusCellHighlightBorder", focusBorder,
            "List.focusSelectedCellHighlightBorder", focusSelectedBorder,
            "List.noFocusBorder", noFocusBorder,
            "List.focusInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                           "ctrl C", "copy",
                           "ctrl V", "paste",
                           "ctrl X", "cut",
                             "COPY", "copy",
                            "PASTE", "paste",
                              "CUT", "cut",
                   "control INSERT", "copy",
                     "shift INSERT", "paste",
                     "shift DELETE", "cut",
                               "UP", "selectPreviousRow",
                            "KP_UP", "selectPreviousRow",
                         "shift UP", "selectPreviousRowExtendSelection",
                      "shift KP_UP", "selectPreviousRowExtendSelection",
                    "ctrl shift UP", "selectPreviousRowExtendSelection",
                 "ctrl shift KP_UP", "selectPreviousRowExtendSelection",
                          "ctrl UP", "selectPreviousRowChangeLead",
                       "ctrl KP_UP", "selectPreviousRowChangeLead",
                             "DOWN", "selectNextRow",
                          "KP_DOWN", "selectNextRow",
                       "shift DOWN", "selectNextRowExtendSelection",
                    "shift KP_DOWN", "selectNextRowExtendSelection",
                  "ctrl shift DOWN", "selectNextRowExtendSelection",
               "ctrl shift KP_DOWN", "selectNextRowExtendSelection",
                        "ctrl DOWN", "selectNextRowChangeLead",
                     "ctrl KP_DOWN", "selectNextRowChangeLead",
                             "LEFT", "selectPreviousColumn",
                          "KP_LEFT", "selectPreviousColumn",
                       "shift LEFT", "selectPreviousColumnExtendSelection",
                    "shift KP_LEFT", "selectPreviousColumnExtendSelection",
                  "ctrl shift LEFT", "selectPreviousColumnExtendSelection",
               "ctrl shift KP_LEFT", "selectPreviousColumnExtendSelection",
                        "ctrl LEFT", "selectPreviousColumnChangeLead",
                     "ctrl KP_LEFT", "selectPreviousColumnChangeLead",
                            "RIGHT", "selectNextColumn",
                         "KP_RIGHT", "selectNextColumn",
                      "shift RIGHT", "selectNextColumnExtendSelection",
                   "shift KP_RIGHT", "selectNextColumnExtendSelection",
                 "ctrl shift RIGHT", "selectNextColumnExtendSelection",
              "ctrl shift KP_RIGHT", "selectNextColumnExtendSelection",
                       "ctrl RIGHT", "selectNextColumnChangeLead",
                    "ctrl KP_RIGHT", "selectNextColumnChangeLead",
                             "HOME", "selectFirstRow",
                       "shift HOME", "selectFirstRowExtendSelection",
                  "ctrl shift HOME", "selectFirstRowExtendSelection",
                        "ctrl HOME", "selectFirstRowChangeLead",
                              "END", "selectLastRow",
                        "shift END", "selectLastRowExtendSelection",
                   "ctrl shift END", "selectLastRowExtendSelection",
                         "ctrl END", "selectLastRowChangeLead",
                          "PAGE_UP", "scrollUp",
                    "shift PAGE_UP", "scrollUpExtendSelection",
               "ctrl shift PAGE_UP", "scrollUpExtendSelection",
                     "ctrl PAGE_UP", "scrollUpChangeLead",
                        "PAGE_DOWN", "scrollDown",
                  "shift PAGE_DOWN", "scrollDownExtendSelection",
             "ctrl shift PAGE_DOWN", "scrollDownExtendSelection",
                   "ctrl PAGE_DOWN", "scrollDownChangeLead",
                           "ctrl A", "selectAll",
                       "ctrl SLASH", "selectAll",
                  "ctrl BACK_SLASH", "clearSelection",
                            "SPACE", "addToSelection",
                       "ctrl SPACE", "toggleAndAnchor",
                      "shift SPACE", "extendTo",
                 "ctrl shift SPACE", "moveSelectionTo"
                 }),
            "List.focusInputMap.RightToLeft",
               new UIDefaults.LazyInputMap(new Object[] {
                             "LEFT", "selectNextColumn",
                          "KP_LEFT", "selectNextColumn",
                       "shift LEFT", "selectNextColumnExtendSelection",
                    "shift KP_LEFT", "selectNextColumnExtendSelection",
                  "ctrl shift LEFT", "selectNextColumnExtendSelection",
               "ctrl shift KP_LEFT", "selectNextColumnExtendSelection",
                        "ctrl LEFT", "selectNextColumnChangeLead",
                     "ctrl KP_LEFT", "selectNextColumnChangeLead",
                            "RIGHT", "selectPreviousColumn",
                         "KP_RIGHT", "selectPreviousColumn",
                      "shift RIGHT", "selectPreviousColumnExtendSelection",
                   "shift KP_RIGHT", "selectPreviousColumnExtendSelection",
                 "ctrl shift RIGHT", "selectPreviousColumnExtendSelection",
              "ctrl shift KP_RIGHT", "selectPreviousColumnExtendSelection",
                       "ctrl RIGHT", "selectPreviousColumnChangeLead",
                    "ctrl KP_RIGHT", "selectPreviousColumnChangeLead",
                 }),
            "List.font", new FontLazyValue(Region.LIST),
            "List.rendererUseUIBorder", Boolean.FALSE,

            "Menu.arrowIcon", new GTKStyle.GTKLazyValue(
                              "com.sun.java.swing.plaf.gtk.GTKIconFactory",
                              "getMenuArrowIcon"),
            "Menu.checkIcon", null,
            "Menu.font", new FontLazyValue(Region.MENU),
            "Menu.margin", zeroInsets,
            "Menu.cancelMode", "hideMenuTree",
            "Menu.alignAcceleratorText", Boolean.FALSE,
            "Menu.useMenuBarForTopLevelMenus", Boolean.TRUE,


                "MenuBar.windowBindings", new Object[] {
                "F10", "takeFocus" },
            "MenuBar.font", new FontLazyValue(Region.MENU_BAR),


            "MenuItem.arrowIcon", null,
            "MenuItem.checkIcon", null,
            "MenuItem.font", new FontLazyValue(Region.MENU_ITEM),
            "MenuItem.margin", zeroInsets,
            "MenuItem.alignAcceleratorText", Boolean.FALSE,


            "OptionPane.setButtonMargin", Boolean.FALSE,
            "OptionPane.sameSizeButtons", Boolean.TRUE,
            "OptionPane.buttonOrientation", SwingConstants.RIGHT,
            "OptionPane.minimumSize", new DimensionUIResource(262, 90),
            "OptionPane.buttonPadding", 10,
            "OptionPane.windowBindings", new Object[] {
                "ESCAPE", "close" },
            "OptionPane.buttonClickThreshhold", 500,
            "OptionPane.isYesLast", Boolean.TRUE,
            "OptionPane.font", new FontLazyValue(Region.OPTION_PANE),

            "Panel.font", new FontLazyValue(Region.PANEL),

            "PasswordField.caretForeground", caretColor,
            "PasswordField.caretAspectRatio", defaultCaretAspectRatio,
            "PasswordField.caretBlinkRate", caretBlinkRate,
            "PasswordField.margin", zeroInsets,
            "PasswordField.focusInputMap", passwordInputMap,
            "PasswordField.font", new FontLazyValue(Region.PASSWORD_FIELD),


            "PopupMenu.consumeEventOnClose", Boolean.FALSE,
            "PopupMenu.selectedWindowInputMapBindings", new Object[] {
                  "ESCAPE", "cancel",
                    "DOWN", "selectNext",
                 "KP_DOWN", "selectNext",
                      "UP", "selectPrevious",
                   "KP_UP", "selectPrevious",
                    "LEFT", "selectParent",
                 "KP_LEFT", "selectParent",
                   "RIGHT", "selectChild",
                "KP_RIGHT", "selectChild",
                   "ENTER", "return",
                   "SPACE", "return"
            },
            "PopupMenu.selectedWindowInputMapBindings.RightToLeft",
                  new Object[] {
                    "LEFT", "selectChild",
                 "KP_LEFT", "selectChild",
                   "RIGHT", "selectParent",
                "KP_RIGHT", "selectParent",
            },
            "PopupMenu.font", new FontLazyValue(Region.POPUP_MENU),

            "ProgressBar.horizontalSize",
                new DimensionUIResource(hProgWidth, hProgHeight),
            "ProgressBar.verticalSize",
                new DimensionUIResource(vProgWidth, vProgHeight),
            "ProgressBar.font", new FontLazyValue(Region.PROGRESS_BAR),

            "RadioButton.focusInputMap",
                   new UIDefaults.LazyInputMap(new Object[] {
                            "SPACE", "pressed",
                   "released SPACE", "released",
                           "RETURN", "pressed"
                   }),
            "RadioButton.icon", new GTKStyle.GTKLazyValue(
                              "com.sun.java.swing.plaf.gtk.GTKIconFactory",
                              "getRadioButtonIcon"),
            "RadioButton.font", new FontLazyValue(Region.RADIO_BUTTON),
            "RadioButton.margin", zeroInsets,


            "RadioButtonMenuItem.arrowIcon", null,
            "RadioButtonMenuItem.checkIcon", new GTKStyle.GTKLazyValue(
                              "com.sun.java.swing.plaf.gtk.GTKIconFactory",
                              "getRadioButtonMenuItemCheckIcon"),
            "RadioButtonMenuItem.font", new FontLazyValue(Region.RADIO_BUTTON_MENU_ITEM),
            "RadioButtonMenuItem.margin", zeroInsets,
            "RadioButtonMenuItem.alignAcceleratorText", Boolean.FALSE,


            // These bindings are only enabled when there is a default
            // button set on the rootpane.
            "RootPane.defaultButtonWindowKeyBindings", new Object[] {
                               "ENTER", "press",
                      "released ENTER", "release",
                          "ctrl ENTER", "press",
                 "ctrl released ENTER", "release"
            },


            "ScrollBar.squareButtons", Boolean.FALSE,
            "ScrollBar.thumbHeight", Integer.valueOf(14),
            "ScrollBar.width", Integer.valueOf(16),
            "ScrollBar.minimumThumbSize", new Dimension(8, 8),
            "ScrollBar.maximumThumbSize", new Dimension(4096, 4096),
            "ScrollBar.allowsAbsolutePositioning", Boolean.TRUE,
            "ScrollBar.alwaysShowThumb", Boolean.TRUE,
            "ScrollBar.ancestorInputMap",
                   new UIDefaults.LazyInputMap(new Object[] {
                       "RIGHT", "positiveUnitIncrement",
                    "KP_RIGHT", "positiveUnitIncrement",
                        "DOWN", "positiveUnitIncrement",
                     "KP_DOWN", "positiveUnitIncrement",
                   "PAGE_DOWN", "positiveBlockIncrement",
                        "LEFT", "negativeUnitIncrement",
                     "KP_LEFT", "negativeUnitIncrement",
                          "UP", "negativeUnitIncrement",
                       "KP_UP", "negativeUnitIncrement",
                     "PAGE_UP", "negativeBlockIncrement",
                        "HOME", "minScroll",
                         "END", "maxScroll"
                   }),
            "ScrollBar.ancestorInputMap.RightToLeft",
                    new UIDefaults.LazyInputMap(new Object[] {
                       "RIGHT", "negativeUnitIncrement",
                    "KP_RIGHT", "negativeUnitIncrement",
                        "LEFT", "positiveUnitIncrement",
                     "KP_LEFT", "positiveUnitIncrement",
                    }),


            "Spinner.disableOnBoundaryValues", Boolean.TRUE,


            "ScrollPane.fillUpperCorner", Boolean.TRUE,
            "ScrollPane.fillLowerCorner", Boolean.TRUE,
            "ScrollPane.ancestorInputMap",
                    new UIDefaults.LazyInputMap(new Object[] {
                           "RIGHT", "unitScrollRight",
                        "KP_RIGHT", "unitScrollRight",
                            "DOWN", "unitScrollDown",
                         "KP_DOWN", "unitScrollDown",
                            "LEFT", "unitScrollLeft",
                         "KP_LEFT", "unitScrollLeft",
                              "UP", "unitScrollUp",
                           "KP_UP", "unitScrollUp",
                         "PAGE_UP", "scrollUp",
                       "PAGE_DOWN", "scrollDown",
                    "ctrl PAGE_UP", "scrollLeft",
                  "ctrl PAGE_DOWN", "scrollRight",
                       "ctrl HOME", "scrollHome",
                        "ctrl END", "scrollEnd"
                    }),
            "ScrollPane.ancestorInputMap.RightToLeft",
                    new UIDefaults.LazyInputMap(new Object[] {
                    "ctrl PAGE_UP", "scrollRight",
                  "ctrl PAGE_DOWN", "scrollLeft",
                    }),
            "ScrollPane.font", new FontLazyValue(Region.SCROLL_PANE),


            "Separator.insets", zeroInsets,
            "Separator.thickness", Integer.valueOf(2),


            "Slider.paintValue", Boolean.TRUE,
            "Slider.thumbWidth", Integer.valueOf(30),
            "Slider.thumbHeight", Integer.valueOf(14),
            "Slider.focusInputMap",
                    new UIDefaults.LazyInputMap(new Object[] {
                            "RIGHT", "positiveUnitIncrement",
                         "KP_RIGHT", "positiveUnitIncrement",
                             "DOWN", "negativeUnitIncrement",
                          "KP_DOWN", "negativeUnitIncrement",
                        "PAGE_DOWN", "negativeBlockIncrement",
                             "LEFT", "negativeUnitIncrement",
                          "KP_LEFT", "negativeUnitIncrement",
                               "UP", "positiveUnitIncrement",
                            "KP_UP", "positiveUnitIncrement",
                          "PAGE_UP", "positiveBlockIncrement",
                             "HOME", "minScroll",
                              "END", "maxScroll"
                        }),
            "Slider.focusInputMap.RightToLeft",
                    new UIDefaults.LazyInputMap(new Object[] {
                            "RIGHT", "negativeUnitIncrement",
                         "KP_RIGHT", "negativeUnitIncrement",
                             "LEFT", "positiveUnitIncrement",
                          "KP_LEFT", "positiveUnitIncrement",
                         }),
            "Slider.onlyLeftMouseButtonDrag", Boolean.FALSE,

            "Spinner.ancestorInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                               "UP", "increment",
                            "KP_UP", "increment",
                             "DOWN", "decrement",
                          "KP_DOWN", "decrement",
               }),
            "Spinner.font", new FontLazyValue(Region.SPINNER),
            "Spinner.editorAlignment", JTextField.LEADING,

            "SplitPane.ancestorInputMap",
                    new UIDefaults.LazyInputMap(new Object[] {
                        "UP", "negativeIncrement",
                      "DOWN", "positiveIncrement",
                      "LEFT", "negativeIncrement",
                     "RIGHT", "positiveIncrement",
                     "KP_UP", "negativeIncrement",
                   "KP_DOWN", "positiveIncrement",
                   "KP_LEFT", "negativeIncrement",
                  "KP_RIGHT", "positiveIncrement",
                      "HOME", "selectMin",
                       "END", "selectMax",
                        "F8", "startResize",
                        "F6", "toggleFocus",
                  "ctrl TAB", "focusOutForward",
            "ctrl shift TAB", "focusOutBackward"
                    }),


            "SplitPane.size", Integer.valueOf(7),
            "SplitPane.oneTouchOffset", Integer.valueOf(2),
            "SplitPane.oneTouchButtonSize", Integer.valueOf(5),
            "SplitPane.supportsOneTouchButtons", Boolean.FALSE,


            "TabbedPane.focusInputMap",
              new UIDefaults.LazyInputMap(new Object[] {
                         "RIGHT", "navigateRight",
                      "KP_RIGHT", "navigateRight",
                          "LEFT", "navigateLeft",
                       "KP_LEFT", "navigateLeft",
                            "UP", "navigateUp",
                         "KP_UP", "navigateUp",
                          "DOWN", "navigateDown",
                       "KP_DOWN", "navigateDown",
                     "ctrl DOWN", "requestFocusForVisibleComponent",
                  "ctrl KP_DOWN", "requestFocusForVisibleComponent",
                         "SPACE", "selectTabWithFocus"
                }),
            "TabbedPane.ancestorInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                         "ctrl TAB", "navigateNext",
                   "ctrl shift TAB", "navigatePrevious",
                   "ctrl PAGE_DOWN", "navigatePageDown",
                     "ctrl PAGE_UP", "navigatePageUp",
                          "ctrl UP", "requestFocus",
                       "ctrl KP_UP", "requestFocus",
                 }),

            "TabbedPane.labelShift", 3,
            "TabbedPane.selectedLabelShift", 3,
            "TabbedPane.font", new FontLazyValue(Region.TABBED_PANE),
            "TabbedPane.selectedTabPadInsets", new InsetsUIResource(2, 2, 0, 1),

            "Table.scrollPaneBorder", zeroBorder,
            "Table.background", tableBg,
            "Table.focusCellBackground", tableFocusCellBg,
            "Table.focusCellForeground", tableFocusCellFg,
            "Table.focusCellHighlightBorder", focusBorder,
            "Table.focusSelectedCellHighlightBorder", focusSelectedBorder,
            "Table.ancestorInputMap",
                    new UIDefaults.LazyInputMap(new Object[] {
                               "ctrl C", "copy",
                               "ctrl V", "paste",
                               "ctrl X", "cut",
                                 "COPY", "copy",
                                "PASTE", "paste",
                                  "CUT", "cut",
                       "control INSERT", "copy",
                         "shift INSERT", "paste",
                         "shift DELETE", "cut",
                                "RIGHT", "selectNextColumn",
                             "KP_RIGHT", "selectNextColumn",
                          "shift RIGHT", "selectNextColumnExtendSelection",
                       "shift KP_RIGHT", "selectNextColumnExtendSelection",
                     "ctrl shift RIGHT", "selectNextColumnExtendSelection",
                  "ctrl shift KP_RIGHT", "selectNextColumnExtendSelection",
                           "ctrl RIGHT", "selectNextColumnChangeLead",
                        "ctrl KP_RIGHT", "selectNextColumnChangeLead",
                                 "LEFT", "selectPreviousColumn",
                              "KP_LEFT", "selectPreviousColumn",
                           "shift LEFT", "selectPreviousColumnExtendSelection",
                        "shift KP_LEFT", "selectPreviousColumnExtendSelection",
                      "ctrl shift LEFT", "selectPreviousColumnExtendSelection",
                   "ctrl shift KP_LEFT", "selectPreviousColumnExtendSelection",
                            "ctrl LEFT", "selectPreviousColumnChangeLead",
                         "ctrl KP_LEFT", "selectPreviousColumnChangeLead",
                                 "DOWN", "selectNextRow",
                              "KP_DOWN", "selectNextRow",
                           "shift DOWN", "selectNextRowExtendSelection",
                        "shift KP_DOWN", "selectNextRowExtendSelection",
                      "ctrl shift DOWN", "selectNextRowExtendSelection",
                   "ctrl shift KP_DOWN", "selectNextRowExtendSelection",
                            "ctrl DOWN", "selectNextRowChangeLead",
                         "ctrl KP_DOWN", "selectNextRowChangeLead",
                                   "UP", "selectPreviousRow",
                                "KP_UP", "selectPreviousRow",
                             "shift UP", "selectPreviousRowExtendSelection",
                          "shift KP_UP", "selectPreviousRowExtendSelection",
                        "ctrl shift UP", "selectPreviousRowExtendSelection",
                     "ctrl shift KP_UP", "selectPreviousRowExtendSelection",
                              "ctrl UP", "selectPreviousRowChangeLead",
                           "ctrl KP_UP", "selectPreviousRowChangeLead",
                                 "HOME", "selectFirstColumn",
                           "shift HOME", "selectFirstColumnExtendSelection",
                      "ctrl shift HOME", "selectFirstRowExtendSelection",
                            "ctrl HOME", "selectFirstRow",
                                  "END", "selectLastColumn",
                            "shift END", "selectLastColumnExtendSelection",
                       "ctrl shift END", "selectLastRowExtendSelection",
                             "ctrl END", "selectLastRow",
                              "PAGE_UP", "scrollUpChangeSelection",
                        "shift PAGE_UP", "scrollUpExtendSelection",
                   "ctrl shift PAGE_UP", "scrollLeftExtendSelection",
                         "ctrl PAGE_UP", "scrollLeftChangeSelection",
                            "PAGE_DOWN", "scrollDownChangeSelection",
                      "shift PAGE_DOWN", "scrollDownExtendSelection",
                 "ctrl shift PAGE_DOWN", "scrollRightExtendSelection",
                       "ctrl PAGE_DOWN", "scrollRightChangeSelection",
                                  "TAB", "selectNextColumnCell",
                            "shift TAB", "selectPreviousColumnCell",
                                "ENTER", "selectNextRowCell",
                          "shift ENTER", "selectPreviousRowCell",
                               "ctrl A", "selectAll",
                           "ctrl SLASH", "selectAll",
                      "ctrl BACK_SLASH", "clearSelection",
                               "ESCAPE", "cancel",
                                   "F2", "startEditing",
                                "SPACE", "addToSelection",
                           "ctrl SPACE", "toggleAndAnchor",
                          "shift SPACE", "extendTo",
                     "ctrl shift SPACE", "moveSelectionTo",
                                   "F8", "focusHeader"
                    }),
            "Table.ancestorInputMap.RightToLeft",
                    new UIDefaults.LazyInputMap(new Object[] {
                                "RIGHT", "selectPreviousColumn",
                             "KP_RIGHT", "selectPreviousColumn",
                          "shift RIGHT", "selectPreviousColumnExtendSelection",
                       "shift KP_RIGHT", "selectPreviousColumnExtendSelection",
                     "ctrl shift RIGHT", "selectPreviousColumnExtendSelection",
                  "ctrl shift KP_RIGHT", "selectPreviousColumnExtendSelection",
                          "shift RIGHT", "selectPreviousColumnChangeLead",
                       "shift KP_RIGHT", "selectPreviousColumnChangeLead",
                                 "LEFT", "selectNextColumn",
                              "KP_LEFT", "selectNextColumn",
                           "shift LEFT", "selectNextColumnExtendSelection",
                        "shift KP_LEFT", "selectNextColumnExtendSelection",
                      "ctrl shift LEFT", "selectNextColumnExtendSelection",
                   "ctrl shift KP_LEFT", "selectNextColumnExtendSelection",
                            "ctrl LEFT", "selectNextColumnChangeLead",
                         "ctrl KP_LEFT", "selectNextColumnChangeLead",
                         "ctrl PAGE_UP", "scrollRightChangeSelection",
                       "ctrl PAGE_DOWN", "scrollLeftChangeSelection",
                   "ctrl shift PAGE_UP", "scrollRightExtendSelection",
                 "ctrl shift PAGE_DOWN", "scrollLeftExtendSelection",
                    }),
            "Table.font", new FontLazyValue(Region.TABLE),
            "Table.ascendingSortIcon",  new GTKStyle.GTKLazyValue(
                              "com.sun.java.swing.plaf.gtk.GTKIconFactory",
                              "getAscendingSortIcon"),
            "Table.descendingSortIcon",  new GTKStyle.GTKLazyValue(
                              "com.sun.java.swing.plaf.gtk.GTKIconFactory",
                              "getDescendingSortIcon"),

            "TableHeader.font", new FontLazyValue(Region.TABLE_HEADER),
            "TableHeader.alignSorterArrow", Boolean.TRUE,

            "TextArea.caretForeground", caretColor,
            "TextArea.caretAspectRatio", defaultCaretAspectRatio,
            "TextArea.caretBlinkRate", caretBlinkRate,
            "TextArea.margin", zeroInsets,
            "TextArea.focusInputMap", multilineInputMap,
            "TextArea.font", new FontLazyValue(Region.TEXT_AREA),


            "TextField.caretForeground", caretColor,
            "TextField.caretAspectRatio", defaultCaretAspectRatio,
            "TextField.caretBlinkRate", caretBlinkRate,
            "TextField.margin", zeroInsets,
            "TextField.focusInputMap", fieldInputMap,
            "TextField.font", new FontLazyValue(Region.TEXT_FIELD),


            "TextPane.caretForeground", caretColor,
            "TextPane.caretAspectRatio", defaultCaretAspectRatio,
            "TextPane.caretBlinkRate", caretBlinkRate,
            "TextPane.margin", editorMargin,
            "TextPane.focusInputMap", multilineInputMap,
            "TextPane.font", new FontLazyValue(Region.TEXT_PANE),


            "TitledBorder.titleColor", controlText,
            "TitledBorder.border", new UIDefaults.LazyValue() {
                public Object createValue(UIDefaults table) {
                    return new GTKPainter.TitledBorder();
                }
            },

            "ToggleButton.focusInputMap",
                   new UIDefaults.LazyInputMap(new Object[] {
                            "SPACE", "pressed",
                   "released SPACE", "released"
                   }),
            "ToggleButton.font", new FontLazyValue(Region.TOGGLE_BUTTON),
            "ToggleButton.margin", zeroInsets,


            "ToolBar.separatorSize", new DimensionUIResource(10, 10),
            "ToolBar.handleIcon", new UIDefaults.ActiveValue() {
                public Object createValue(UIDefaults table) {
                    return GTKIconFactory.getToolBarHandleIcon();
                }
            },
            "ToolBar.ancestorInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                        "UP", "navigateUp",
                     "KP_UP", "navigateUp",
                      "DOWN", "navigateDown",
                   "KP_DOWN", "navigateDown",
                      "LEFT", "navigateLeft",
                   "KP_LEFT", "navigateLeft",
                     "RIGHT", "navigateRight",
                  "KP_RIGHT", "navigateRight"
                 }),
            "ToolBar.font", new FontLazyValue(Region.TOOL_BAR),

            "ToolTip.font", new FontLazyValue(Region.TOOL_TIP),

            "Tree.padding", Integer.valueOf(4),
            "Tree.background", tableBg,
            "Tree.drawHorizontalLines", Boolean.FALSE,
            "Tree.drawVerticalLines", Boolean.FALSE,
            "Tree.rowHeight", Integer.valueOf(-1),
            "Tree.scrollsOnExpand", Boolean.FALSE,
            "Tree.expanderSize", Integer.valueOf(10),
            "Tree.repaintWholeRow", Boolean.TRUE,
            "Tree.closedIcon", null,
            "Tree.leafIcon", null,
            "Tree.openIcon", null,
            "Tree.expandedIcon", new GTKStyle.GTKLazyValue(
                              "com.sun.java.swing.plaf.gtk.GTKIconFactory",
                              "getTreeExpandedIcon"),
            "Tree.collapsedIcon", new GTKStyle.GTKLazyValue(
                              "com.sun.java.swing.plaf.gtk.GTKIconFactory",
                              "getTreeCollapsedIcon"),
            "Tree.leftChildIndent", Integer.valueOf(2),
            "Tree.rightChildIndent", Integer.valueOf(12),
            "Tree.scrollsHorizontallyAndVertically", Boolean.FALSE,
            "Tree.drawsFocusBorder", Boolean.TRUE,
            "Tree.focusInputMap",
                    new UIDefaults.LazyInputMap(new Object[] {
                                 "ctrl C", "copy",
                                 "ctrl V", "paste",
                                 "ctrl X", "cut",
                                   "COPY", "copy",
                                  "PASTE", "paste",
                                    "CUT", "cut",
                         "control INSERT", "copy",
                           "shift INSERT", "paste",
                           "shift DELETE", "cut",
                                     "UP", "selectPrevious",
                                  "KP_UP", "selectPrevious",
                               "shift UP", "selectPreviousExtendSelection",
                            "shift KP_UP", "selectPreviousExtendSelection",
                          "ctrl shift UP", "selectPreviousExtendSelection",
                       "ctrl shift KP_UP", "selectPreviousExtendSelection",
                                "ctrl UP", "selectPreviousChangeLead",
                             "ctrl KP_UP", "selectPreviousChangeLead",
                                   "DOWN", "selectNext",
                                "KP_DOWN", "selectNext",
                             "shift DOWN", "selectNextExtendSelection",
                          "shift KP_DOWN", "selectNextExtendSelection",
                        "ctrl shift DOWN", "selectNextExtendSelection",
                     "ctrl shift KP_DOWN", "selectNextExtendSelection",
                              "ctrl DOWN", "selectNextChangeLead",
                           "ctrl KP_DOWN", "selectNextChangeLead",
                                  "RIGHT", "selectChild",
                               "KP_RIGHT", "selectChild",
                                   "LEFT", "selectParent",
                                "KP_LEFT", "selectParent",
                                "typed +", "expand",
                                "typed -", "collapse",
                             "BACK_SPACE", "moveSelectionToParent",
                                "PAGE_UP", "scrollUpChangeSelection",
                          "shift PAGE_UP", "scrollUpExtendSelection",
                     "ctrl shift PAGE_UP", "scrollUpExtendSelection",
                           "ctrl PAGE_UP", "scrollUpChangeLead",
                              "PAGE_DOWN", "scrollDownChangeSelection",
                        "shift PAGE_DOWN", "scrollDownExtendSelection",
                   "ctrl shift PAGE_DOWN", "scrollDownExtendSelection",
                         "ctrl PAGE_DOWN", "scrollDownChangeLead",
                                   "HOME", "selectFirst",
                             "shift HOME", "selectFirstExtendSelection",
                        "ctrl shift HOME", "selectFirstExtendSelection",
                              "ctrl HOME", "selectFirstChangeLead",
                                    "END", "selectLast",
                              "shift END", "selectLastExtendSelection",
                         "ctrl shift END", "selectLastExtendSelection",
                               "ctrl END", "selectLastChangeLead",
                                     "F2", "startEditing",
                                 "ctrl A", "selectAll",
                             "ctrl SLASH", "selectAll",
                        "ctrl BACK_SLASH", "clearSelection",
                              "ctrl LEFT", "scrollLeft",
                           "ctrl KP_LEFT", "scrollLeft",
                             "ctrl RIGHT", "scrollRight",
                          "ctrl KP_RIGHT", "scrollRight",
                                  "SPACE", "addToSelection",
                             "ctrl SPACE", "toggleAndAnchor",
                            "shift SPACE", "extendTo",
                       "ctrl shift SPACE", "moveSelectionTo"
                    }),
            "Tree.focusInputMap.RightToLeft",
                    new UIDefaults.LazyInputMap(new Object[] {
                                  "RIGHT", "selectParent",
                               "KP_RIGHT", "selectParent",
                                   "LEFT", "selectChild",
                                "KP_LEFT", "selectChild",
                 }),
            "Tree.ancestorInputMap",
                      new UIDefaults.LazyInputMap(new Object[] {
                         "ESCAPE", "cancel"
                      }),
            "Tree.font", new FontLazyValue(Region.TREE),

            "Viewport.font", new FontLazyValue(Region.VIEWPORT)
        };
        table.putDefaults(defaults);

        if (fallbackFont != null) {
            table.put("TitledBorder.font", fallbackFont);
        }
        if (aaTextInfo != null) {
            table.putAll(aaTextInfo);
        }
    }

    protected void initSystemColorDefaults(UIDefaults table) {
        SynthStyleFactory factory = getStyleFactory();
        GTKStyle windowStyle =
                (GTKStyle)factory.getStyle(null, Region.INTERNAL_FRAME);
        table.put("window", windowStyle.getGTKColor(SynthConstants.ENABLED,
                GTKColorType.BACKGROUND));
        table.put("windowText", windowStyle.getGTKColor(SynthConstants.ENABLED,
                GTKColorType.TEXT_FOREGROUND));

        GTKStyle entryStyle = (GTKStyle)factory.getStyle(null, Region.TEXT_FIELD);
        table.put("text", entryStyle.getGTKColor(SynthConstants.ENABLED,
                                           GTKColorType.TEXT_BACKGROUND));
        table.put("textText", entryStyle.getGTKColor(SynthConstants.ENABLED,
                                           GTKColorType.TEXT_FOREGROUND));
        table.put("textHighlight",
                entryStyle.getGTKColor(SynthConstants.SELECTED,
                                         GTKColorType.TEXT_BACKGROUND));
        table.put("textHighlightText",
                  entryStyle.getGTKColor(SynthConstants.SELECTED,
                                         GTKColorType.TEXT_FOREGROUND));
        table.put("textInactiveText",
                  entryStyle.getGTKColor(SynthConstants.DISABLED,
                                         GTKColorType.TEXT_FOREGROUND));
        Object caretColor =
            entryStyle.getClassSpecificValue("cursor-color");
        if (caretColor == null) {
            caretColor = GTKStyle.BLACK_COLOR;
        }
        table.put("caretColor", caretColor);

        GTKStyle menuStyle = (GTKStyle)factory.getStyle(null, Region.MENU_ITEM);
        table.put("menu", menuStyle.getGTKColor(SynthConstants.ENABLED,
                                           GTKColorType.BACKGROUND));
        table.put("menuText", menuStyle.getGTKColor(SynthConstants.ENABLED,
                                           GTKColorType.TEXT_FOREGROUND));

        GTKStyle scrollbarStyle = (GTKStyle)factory.getStyle(null, Region.SCROLL_BAR);
        table.put("scrollbar", scrollbarStyle.getGTKColor(SynthConstants.ENABLED,
                                           GTKColorType.BACKGROUND));

        GTKStyle infoStyle = (GTKStyle)factory.getStyle(null, Region.OPTION_PANE);
        table.put("info", infoStyle.getGTKColor(SynthConstants.ENABLED,
                                           GTKColorType.BACKGROUND));
        table.put("infoText", infoStyle.getGTKColor(SynthConstants.ENABLED,
                                           GTKColorType.TEXT_FOREGROUND));

        GTKStyle desktopStyle = (GTKStyle)factory.getStyle(null, Region.DESKTOP_PANE);
        table.put("desktop", desktopStyle.getGTKColor(SynthConstants.ENABLED,
                                           GTKColorType.BACKGROUND));

        // colors specific only for GTK
        // It is impossible to create a simple GtkWidget without specifying the
        // type. So for GtkWidget we can use any appropriate concrete type of
        // wigdet. LABEL in this case.
        GTKStyle widgetStyle = (GTKStyle)factory.getStyle(null, Region.LABEL);
        Color bg = widgetStyle.getGTKColor(SynthConstants.ENABLED,
                                           GTKColorType.BACKGROUND);
        table.put("control", bg);
        table.put("controlHighlight", bg);
        table.put("controlText", widgetStyle.getGTKColor(SynthConstants.ENABLED,
                                               GTKColorType.TEXT_FOREGROUND));
        table.put("controlLtHighlight", widgetStyle.getGTKColor(
                SynthConstants.ENABLED, GTKColorType.LIGHT));
        table.put("controlShadow", widgetStyle.getGTKColor(
                SynthConstants.ENABLED, GTKColorType.DARK));
        table.put("controlDkShadow", widgetStyle.getGTKColor(
                SynthConstants.ENABLED, GTKColorType.BLACK));
        table.put("light", widgetStyle.getGTKColor(
                SynthConstants.ENABLED, GTKColorType.LIGHT));
        table.put("mid", widgetStyle.getGTKColor(
                SynthConstants.ENABLED, GTKColorType.MID));
        table.put("dark", widgetStyle.getGTKColor(
                SynthConstants.ENABLED, GTKColorType.DARK));
        table.put("black", widgetStyle.getGTKColor(
                SynthConstants.ENABLED, GTKColorType.BLACK));
        table.put("white", widgetStyle.getGTKColor(
                SynthConstants.ENABLED, GTKColorType.WHITE));
    }

    /**
     * Creates the GTK look and feel class for the passed in Component.
     */
    public static ComponentUI createUI(JComponent c) {
        String key = c.getUIClassID().intern();

        if (key == "FileChooserUI") {
            return GTKFileChooserUI.createUI(c);
        }
        return SynthLookAndFeel.createUI(c);
    }

    /**
     * Returns the cached gtkThemeName
     */
    static String getGtkThemeName() {
        return gtkThemeName;
    }

    static boolean isLeftToRight(Component c) {
        return c.getComponentOrientation().isLeftToRight();
    }

    public void initialize() {
        /*
         * We need to call loadGTK() to ensure that the native GTK
         * libraries are loaded.  It is very unlikely that this call will
         * fail (since we've already verified native GTK support in
         * isSupportedLookAndFeel()), but we can throw an error in the
         * failure situation just in case.
         */
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        if (toolkit instanceof UNIXToolkit &&
            !((UNIXToolkit)toolkit).loadGTK())
        {
            throw new InternalError("Unable to load native GTK libraries");
        }

        if (UNIXToolkit.getGtkVersion() == UNIXToolkit.GtkVersions.GTK2) {
            @SuppressWarnings("removal")
            String version = AccessController.doPrivileged(
                    new GetPropertyAction("jdk.gtk.version"));
            if (version != null) {
                IS_22 = version.equals("2.2");
            } else {
                IS_22 = true;
            }
        } else if (UNIXToolkit.getGtkVersion() ==
                                UNIXToolkit.GtkVersions.GTK3) {
            IS_3 = true;
        }

        super.initialize();
        inInitialize = true;
        loadStyles();
        inInitialize = false;

        /*
         * Check if system AA font settings should be used.
         * REMIND: See comment on isLocalDisplay() definition regarding
         * XRender.
         */
        gtkAAFontSettingsCond = SwingUtilities2.isLocalDisplay();
        aaTextInfo = new HashMap<>(2);
        SwingUtilities2.putAATextInfo(gtkAAFontSettingsCond, aaTextInfo);
    }

    static ReferenceQueue<GTKLookAndFeel> queue = new ReferenceQueue<GTKLookAndFeel>();

    private static void flushUnreferenced() {
        WeakPCL pcl;

        while ((pcl = (WeakPCL)queue.poll()) != null) {
            pcl.dispose();
        }
    }

    static class WeakPCL extends WeakReference<GTKLookAndFeel> implements
            PropertyChangeListener {
        private Toolkit kit;
        private String key;

        WeakPCL(GTKLookAndFeel target, Toolkit kit, String key) {
            super(target, queue);
            this.kit = kit;
            this.key = key;
        }

        public String getKey() { return key; }

        public void propertyChange(final PropertyChangeEvent pce) {
            final GTKLookAndFeel lnf = get();

            if (lnf == null || UIManager.getLookAndFeel() != lnf) {
                // The property was GC'ed, we're no longer interested in
                // PropertyChanges, remove the listener.
                dispose();
            }
            else {
                // We are using invokeLater here because we are getting called
                // on the AWT-Motif thread which can cause a deadlock.
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        String name = pce.getPropertyName();
                        /* We are listening for GTK desktop text AA settings:
                         * "gnome.Xft/Antialias" and "gnome.Xft/RGBA".
                         * However we don't need to read these here as
                         * the UIDefaults reads them and this event causes
                         * those to be reinitialised.
                         */
                        if ("gnome.Net/ThemeName".equals(name)) {
                            GTKEngine.INSTANCE.themeChanged();
                            GTKIconFactory.resetIcons();
                        }
                        lnf.loadStyles();
                        Window[] appWindows = Window.getWindows();
                        for (int i = 0; i < appWindows.length; i++) {
                            SynthLookAndFeel.updateStyles(appWindows[i]);
                        }
                    }
                });
            }
        }

        void dispose() {
            kit.removePropertyChangeListener(key, this);
        }
    }

    public boolean isSupportedLookAndFeel() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        return (toolkit instanceof SunToolkit &&
                ((SunToolkit)toolkit).isNativeGTKAvailable());
    }

    public boolean isNativeLookAndFeel() {
        return true;
    }

    public String getDescription() {
        return "GTK look and feel";
    }

    public String getName() {
        return "GTK look and feel";
    }

    public String getID() {
        return "GTK";
    }

    // Subclassed to pass in false to the superclass, we don't want to try
    // and load the system colors.
    protected void loadSystemColors(UIDefaults table, String[] systemColors, boolean useNative) {
        super.loadSystemColors(table, systemColors, false);
    }

    private void loadStyles() {
        gtkThemeName = (String)Toolkit.getDefaultToolkit().
                getDesktopProperty("gnome.Net/ThemeName");

        setStyleFactory(getGTKStyleFactory());

        // If we are in initialize initializations will be
        // called later, don't do it now.
        if (!inInitialize) {
            UIDefaults table = UIManager.getLookAndFeelDefaults();
            initSystemColorDefaults(table);
            initComponentDefaults(table);
        }
    }

    private GTKStyleFactory getGTKStyleFactory() {

        GTKEngine engine = GTKEngine.INSTANCE;
        Object iconSizes = engine.getSetting(GTKEngine.Settings.GTK_ICON_SIZES);
        if (iconSizes instanceof String) {
            if (!configIconSizes((String)iconSizes)) {
                System.err.println("Error parsing gtk-icon-sizes string: '" + iconSizes + "'");
            }
        }

        // Desktop property appears to have preference over rc font.
        Object fontName = Toolkit.getDefaultToolkit().getDesktopProperty(
                                  "gnome.Gtk/FontName");

       if (!(fontName instanceof String)) {
            fontName = engine.getSetting(GTKEngine.Settings.GTK_FONT_NAME);
            if (!(fontName instanceof String)) {
               fontName = "sans 10";
            }
        }

        if (styleFactory == null) {
            styleFactory = new GTKStyleFactory();
        }

        Font defaultFont = PangoFonts.lookupFont((String)fontName);
        fallbackFont = defaultFont;
        styleFactory.initStyles(defaultFont);

        return styleFactory;
    }

    private boolean configIconSizes(String sizeString) {
        String[] sizes = sizeString.split(":");
        for (int i = 0; i < sizes.length; i++) {
            String[] splits = sizes[i].split("=");

            if (splits.length != 2) {
                return false;
            }

            String size = splits[0].trim().intern();
            if (size.length() < 1) {
                return false;
            }

            splits = splits[1].split(",");

            if (splits.length != 2) {
                return false;
            }

            String width = splits[0].trim();
            String height = splits[1].trim();

            if (width.length() < 1 || height.length() < 1) {
                return false;
            }

            int w;
            int h;

            try {
                w = Integer.parseInt(width);
                h = Integer.parseInt(height);
            } catch (NumberFormatException nfe) {
                return false;
            }

            if (w > 0 && h > 0) {
                int type = GTKStyle.GTKStockIconInfo.getIconType(size);
                GTKStyle.GTKStockIconInfo.setIconSize(type, w, h);
            } else {
                System.err.println("Invalid size in gtk-icon-sizes: " + w + "," + h);
            }
        }

        return true;
    }

    /**
     * Returns whether or not the UIs should update their
     * <code>SynthStyles</code> from the <code>SynthStyleFactory</code>
     * when the ancestor of the Component changes.
     *
     * @return whether or not the UIs should update their
     * <code>SynthStyles</code> from the <code>SynthStyleFactory</code>
     * when the ancestor changed.
     */
    public boolean shouldUpdateStyleOnAncestorChanged() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public LayoutStyle getLayoutStyle() {
        return GnomeLayoutStyle.INSTANCE;
    }


    /**
     * Gnome layout style.  From:
     * http://developer.gnome.org/projects/gup/hig/2.0/design-window.html#window-layout-spacing
     * You'll notice this doesn't do the radiobutton/checkbox border
     * adjustments that windows/metal do.  This is because gtk doesn't
     * provide margins/insets for checkbox/radiobuttons.
     */
    @SuppressWarnings("fallthrough")
    private static class GnomeLayoutStyle extends DefaultLayoutStyle {
        private static GnomeLayoutStyle INSTANCE = new GnomeLayoutStyle();

        @Override
        public int getPreferredGap(JComponent component1,
                JComponent component2, ComponentPlacement type, int position,
                Container parent) {
            // Checks args
            super.getPreferredGap(component1, component2, type, position,
                                  parent);

            switch(type) {
            case INDENT:
                if (position == SwingConstants.EAST ||
                        position == SwingConstants.WEST) {
                    // Indent group members 12 pixels to denote hierarchy and
                    // association.
                    return 12;
                }
                // Fall through to related
            // As a basic rule of thumb, leave space between user
            // interface components in increments of 6 pixels, going up as
            // the relationship between related elements becomes more
            // distant. For example, between icon labels and associated
            // graphics within an icon, 6 pixels are adequate. Between
            // labels and associated components, leave 12 horizontal
            // pixels. For vertical spacing between groups of components,
            // 18 pixels is adequate.
            //
            // The first part of this is handled automatically by Icon (which
            // won't give you 6 pixels).
            case RELATED:
                if (isLabelAndNonlabel(component1, component2, position)) {
                    return 12;
                }
                return 6;
            case UNRELATED:
                return 12;
            }
            return 0;
        }

        @Override
        public int getContainerGap(JComponent component, int position,
                                   Container parent) {
            // Checks args
            super.getContainerGap(component, position, parent);
            // A general padding of 12 pixels is
            // recommended between the contents of a dialog window and the
            // window borders.
            return 12;
        }
    }
}
