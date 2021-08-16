/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.java.swing.plaf.motif;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;

import javax.swing.JTextField;
import javax.swing.UIDefaults;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.plaf.basic.BasicLookAndFeel;
import javax.swing.text.DefaultEditorKit;

import sun.swing.SwingAccessor;
import sun.swing.SwingUtilities2;

/**
 * Implements the Motif Look and Feel.
 * UI classes not implemented specifically for Motif will
 * default to those implemented in Basic.
 *
 * @deprecated The Motif Look and Feel is deprecated with the intent to remove
 *             it in some future release. It is recommended to use
 *             {@link javax.swing.plaf.metal.MetalLookAndFeel} instead.
 */
@SuppressWarnings("serial") // Superclass is not serializable across versions
@Deprecated(since="13", forRemoval=true)
public class MotifLookAndFeel extends BasicLookAndFeel
{
    public String getName() {
        return "CDE/Motif";
    }

    public String getID() {
        return "Motif";
    }

    public String getDescription() {
        return "The CDE/Motif Look and Feel";
    }


    public boolean isNativeLookAndFeel() {
        return false;
    }


    public boolean isSupportedLookAndFeel() {
        return true;
    }


    /**
     * Load the SystemColors into the defaults table.  The keys
     * for SystemColor defaults are the same as the names of
     * the public fields in SystemColor.  If the table is being
     * created on a native Motif platform we use the SystemColor
     * values, otherwise we create color objects whose values match
     * the default CDE/Motif colors.
     */
    protected void initSystemColorDefaults(UIDefaults table)
    {
        String[] defaultSystemColors = {
                  "desktop", "#005C5C", /* Color of the desktop background */
            "activeCaption", "#000080", /* Color for captions (title bars) when they are active. */
        "activeCaptionText", "#FFFFFF", /* Text color for text in captions (title bars). */
      "activeCaptionBorder", "#B24D7A", /* Border color for caption (title bar) window borders. */
          "inactiveCaption", "#AEB2C3", /* Color for captions (title bars) when not active. */
      "inactiveCaptionText", "#000000", /* Text color for text in inactive captions (title bars). */
    "inactiveCaptionBorder", "#AEB2C3", /* Border color for inactive caption (title bar) window borders. */
                   "window", "#AEB2C3", /* Default color for the interior of windows */
             "windowBorder", "#AEB2C3", /* ??? */
               "windowText", "#000000", /* ??? */
                     "menu", "#AEB2C3", /* ??? */
                 "menuText", "#000000", /* ??? */
                     "text", "#FFF7E9", /* Text background color */
                 "textText", "#000000", /* Text foreground color */
            "textHighlight", "#000000", /* Text background color when selected */
        "textHighlightText", "#FFF7E9", /* Text color when selected */
         "textInactiveText", "#808080", /* Text color when disabled */
                  "control", "#AEB2C3", /* Default color for controls (buttons, sliders, etc) */
              "controlText", "#000000", /* Default color for text in controls */
         "controlHighlight", "#DCDEE5", /* Highlight color for controls */
       "controlLtHighlight", "#DCDEE5", /* Light highlight color for controls */
            "controlShadow", "#63656F", /* Shadow color for controls */
       "controlLightShadow", "#9397A5", /* Shadow color for controls */
          "controlDkShadow", "#000000", /* Dark shadow color for controls */
                "scrollbar", "#AEB2C3", /* Scrollbar ??? color. PENDING(jeff) foreground? background? ?*/
                     "info", "#FFF7E9", /* ??? */
                 "infoText", "#000000"  /* ??? */
        };

        loadSystemColors(table, defaultSystemColors, false);
    }


    protected void initClassDefaults(UIDefaults table)
    {
        super.initClassDefaults(table);
        String motifPackageName = "com.sun.java.swing.plaf.motif.";

        Object[] uiDefaults = {
                   "ButtonUI", motifPackageName + "MotifButtonUI",
                 "CheckBoxUI", motifPackageName + "MotifCheckBoxUI",
            "DirectoryPaneUI", motifPackageName + "MotifDirectoryPaneUI",
              "FileChooserUI", motifPackageName + "MotifFileChooserUI",
                    "LabelUI", motifPackageName + "MotifLabelUI",
                  "MenuBarUI", motifPackageName + "MotifMenuBarUI",
                     "MenuUI", motifPackageName + "MotifMenuUI",
                 "MenuItemUI", motifPackageName + "MotifMenuItemUI",
         "CheckBoxMenuItemUI", motifPackageName + "MotifCheckBoxMenuItemUI",
      "RadioButtonMenuItemUI", motifPackageName + "MotifRadioButtonMenuItemUI",
              "RadioButtonUI", motifPackageName + "MotifRadioButtonUI",
             "ToggleButtonUI", motifPackageName + "MotifToggleButtonUI",
                "PopupMenuUI", motifPackageName + "MotifPopupMenuUI",
              "ProgressBarUI", motifPackageName + "MotifProgressBarUI",
                "ScrollBarUI", motifPackageName + "MotifScrollBarUI",
               "ScrollPaneUI", motifPackageName + "MotifScrollPaneUI",
                   "SliderUI", motifPackageName + "MotifSliderUI",
                "SplitPaneUI", motifPackageName + "MotifSplitPaneUI",
               "TabbedPaneUI", motifPackageName + "MotifTabbedPaneUI",
                 "TextAreaUI", motifPackageName + "MotifTextAreaUI",
                "TextFieldUI", motifPackageName + "MotifTextFieldUI",
            "PasswordFieldUI", motifPackageName + "MotifPasswordFieldUI",
                 "TextPaneUI", motifPackageName + "MotifTextPaneUI",
               "EditorPaneUI", motifPackageName + "MotifEditorPaneUI",
                     "TreeUI", motifPackageName + "MotifTreeUI",
            "InternalFrameUI", motifPackageName + "MotifInternalFrameUI",
              "DesktopPaneUI", motifPackageName + "MotifDesktopPaneUI",
                "SeparatorUI", motifPackageName + "MotifSeparatorUI",
       "PopupMenuSeparatorUI", motifPackageName + "MotifPopupMenuSeparatorUI",
               "OptionPaneUI", motifPackageName + "MotifOptionPaneUI",
                 "ComboBoxUI", motifPackageName + "MotifComboBoxUI",
              "DesktopIconUI", motifPackageName + "MotifDesktopIconUI"
        };

        table.putDefaults(uiDefaults);
    }


    /**
     * Initialize the defaults table with the name of the ResourceBundle
     * used for getting localized defaults.
     */
    private void initResourceBundle(UIDefaults table) {
        SwingAccessor.getUIDefaultsAccessor()
                     .addInternalBundle(table,
                             "com.sun.java.swing.plaf.motif.resources.motif");
    }


    protected void initComponentDefaults(UIDefaults table)
    {
        super.initComponentDefaults(table);

        initResourceBundle(table);

        FontUIResource dialogPlain12 = new FontUIResource(Font.DIALOG,
                                                          Font.PLAIN, 12);
        FontUIResource serifPlain12 = new FontUIResource(Font.SERIF,
                                                          Font.PLAIN, 12);
        FontUIResource sansSerifPlain12 = new FontUIResource(Font.SANS_SERIF,
                                                          Font.PLAIN, 12);
        FontUIResource monospacedPlain12 = new FontUIResource(Font.MONOSPACED,
                                                          Font.PLAIN, 12);
        ColorUIResource red = new ColorUIResource(Color.red);
        ColorUIResource black = new ColorUIResource(Color.black);
        ColorUIResource white = new ColorUIResource(Color.white);
        ColorUIResource lightGray = new ColorUIResource(Color.lightGray);
        ColorUIResource controlDarker = new ColorUIResource(147, 151, 165);  // slate blue
        ColorUIResource scrollBarTrack = controlDarker;
        ColorUIResource menuItemPressedBackground = new ColorUIResource(165,165,165);
        ColorUIResource menuItemPressedForeground = new ColorUIResource(0,0,0);


        Border loweredBevelBorder = new MotifBorders.BevelBorder(false,
                                           table.getColor("controlShadow"),
                                           table.getColor("controlLtHighlight"));

        Border raisedBevelBorder = new MotifBorders.BevelBorder(true,                                                                  table.getColor("controlShadow"),
                                           table.getColor("controlLtHighlight"));

        Border marginBorder = new BasicBorders.MarginBorder();

        Border focusBorder = new MotifBorders.FocusBorder(
                                           table.getColor("control"),
                                           table.getColor("activeCaptionBorder"));


        Border focusBevelBorder = new BorderUIResource.CompoundBorderUIResource(
                                          focusBorder,
                                          loweredBevelBorder);

        Border comboBoxBorder = new BorderUIResource.CompoundBorderUIResource(
                                          focusBorder,
                                          raisedBevelBorder);


        Border buttonBorder = new BorderUIResource.CompoundBorderUIResource(
                                      new MotifBorders.ButtonBorder(
                                          table.getColor("Button.shadow"),
                                          table.getColor("Button.highlight"),
                                          table.getColor("Button.darkShadow"),
                                          table.getColor("activeCaptionBorder")),
                                      marginBorder);

        Border toggleButtonBorder = new BorderUIResource.CompoundBorderUIResource(
                                      new MotifBorders.ToggleButtonBorder(
                                          table.getColor("ToggleButton.shadow"),
                                          table.getColor("ToggleButton.highlight"),
                                          table.getColor("ToggleButton.darkShadow"),
                                          table.getColor("activeCaptionBorder")),                                                        marginBorder);

        Border textFieldBorder = new BorderUIResource.CompoundBorderUIResource(
                                      focusBevelBorder,
                                      marginBorder);

        Border popupMenuBorder = new BorderUIResource.CompoundBorderUIResource(
                                      raisedBevelBorder,
                                      new MotifBorders.MotifPopupMenuBorder(
                                        table.getFont("PopupMenu.font"),
                                        table.getColor("PopupMenu.background"),
                                        table.getColor("PopupMenu.foreground"),
                                        table.getColor("controlShadow"),
                                        table.getColor("controlLtHighlight")
                                        ));

        Object menuItemCheckIcon = new UIDefaults.LazyValue() {
            public Object createValue(UIDefaults table) {
                return MotifIconFactory.getMenuItemCheckIcon();
            }
        };

        Object menuItemArrowIcon = new UIDefaults.LazyValue() {
            public Object createValue(UIDefaults table) {
                return MotifIconFactory.getMenuItemArrowIcon();
            }
        };

        Object menuArrowIcon = new UIDefaults.LazyValue() {
            public Object createValue(UIDefaults table) {
                return MotifIconFactory.getMenuArrowIcon();
            }
        };

        Object checkBoxIcon = new UIDefaults.LazyValue() {
            public Object createValue(UIDefaults table) {
                return MotifIconFactory.getCheckBoxIcon();
            }
        };

        Object radioButtonIcon = new UIDefaults.LazyValue() {
            public Object createValue(UIDefaults table) {
                return MotifIconFactory.getRadioButtonIcon();
            }
        };

        Object unselectedTabBackground = new UIDefaults.LazyValue() {
            public Object createValue(UIDefaults table) {
                Color c = table.getColor("control");
                return new ColorUIResource(Math.max((int)(c.getRed()*.85),0),
                                           Math.max((int)(c.getGreen()*.85),0),
                                           Math.max((int)(c.getBlue()*.85),0));
            }
        };

        Object unselectedTabForeground = new UIDefaults.LazyValue() {
            public Object createValue(UIDefaults table) {
                Color c = table.getColor("controlText");
                return new ColorUIResource(Math.max((int)(c.getRed()*.85),0),
                                           Math.max((int)(c.getGreen()*.85),0),
                                           Math.max((int)(c.getBlue()*.85),0));
            }
        };

        Object unselectedTabShadow = new UIDefaults.LazyValue() {
            public Object createValue(UIDefaults table) {
                Color c = table.getColor("control");
                Color base = new Color(Math.max((int)(c.getRed()*.85),0),
                                       Math.max((int)(c.getGreen()*.85),0),
                                       Math.max((int)(c.getBlue()*.85),0));
                return new ColorUIResource(base.darker());
            }
        };

        Object unselectedTabHighlight = new UIDefaults.LazyValue() {
            public Object createValue(UIDefaults table) {
                Color c = table.getColor("control");
                Color base = new Color(Math.max((int)(c.getRed()*.85),0),
                                       Math.max((int)(c.getGreen()*.85),0),
                                       Math.max((int)(c.getBlue()*.85),0));
                return new ColorUIResource(base.brighter());
            }
        };

        // *** Text

        Object fieldInputMap = new UIDefaults.LazyInputMap(new Object[] {
                           "COPY", DefaultEditorKit.copyAction,
                          "PASTE", DefaultEditorKit.pasteAction,
                            "CUT", DefaultEditorKit.cutAction,
                 "control INSERT", DefaultEditorKit.copyAction,
                   "shift INSERT", DefaultEditorKit.pasteAction,
                   "shift DELETE", DefaultEditorKit.cutAction,
                      "control F", DefaultEditorKit.forwardAction,
                      "control B", DefaultEditorKit.backwardAction,
                      "control D", DefaultEditorKit.deleteNextCharAction,
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
                     "shift LEFT", DefaultEditorKit.selectionBackwardAction,
                    "shift RIGHT", DefaultEditorKit.selectionForwardAction,
                   "control LEFT", DefaultEditorKit.previousWordAction,
                  "control RIGHT", DefaultEditorKit.nextWordAction,
             "control shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
            "control shift RIGHT", DefaultEditorKit.selectionNextWordAction,
                  "control SLASH", DefaultEditorKit.selectAllAction,
                           "HOME", DefaultEditorKit.beginLineAction,
                            "END", DefaultEditorKit.endLineAction,
                     "shift HOME", DefaultEditorKit.selectionBeginLineAction,
                      "shift END", DefaultEditorKit.selectionEndLineAction,
             "control BACK_SLASH", "unselect"/*DefaultEditorKit.unselectAction*/,
                          "ENTER", JTextField.notifyAction,
                "control shift O", "toggle-componentOrientation"/*DefaultEditorKit.toggleComponentOrientation*/
        });

        Object passwordInputMap = new UIDefaults.LazyInputMap(new Object[] {
                           "COPY", DefaultEditorKit.copyAction,
                          "PASTE", DefaultEditorKit.pasteAction,
                            "CUT", DefaultEditorKit.cutAction,
                 "control INSERT", DefaultEditorKit.copyAction,
                   "shift INSERT", DefaultEditorKit.pasteAction,
                   "shift DELETE", DefaultEditorKit.cutAction,
                      "control F", DefaultEditorKit.forwardAction,
                      "control B", DefaultEditorKit.backwardAction,
                      "control D", DefaultEditorKit.deleteNextCharAction,
                     "BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
               "shift BACK_SPACE", DefaultEditorKit.deletePrevCharAction,
                         "ctrl H", DefaultEditorKit.deletePrevCharAction,
                         "DELETE", DefaultEditorKit.deleteNextCharAction,
                          "RIGHT", DefaultEditorKit.forwardAction,
                           "LEFT", DefaultEditorKit.backwardAction,
                       "KP_RIGHT", DefaultEditorKit.forwardAction,
                        "KP_LEFT", DefaultEditorKit.backwardAction,
                     "shift LEFT", DefaultEditorKit.selectionBackwardAction,
                    "shift RIGHT", DefaultEditorKit.selectionForwardAction,
                   "control LEFT", DefaultEditorKit.beginLineAction,
                  "control RIGHT", DefaultEditorKit.endLineAction,
             "control shift LEFT", DefaultEditorKit.selectionBeginLineAction,
            "control shift RIGHT", DefaultEditorKit.selectionEndLineAction,
                  "control SLASH", DefaultEditorKit.selectAllAction,
                           "HOME", DefaultEditorKit.beginLineAction,
                            "END", DefaultEditorKit.endLineAction,
                     "shift HOME", DefaultEditorKit.selectionBeginLineAction,
                      "shift END", DefaultEditorKit.selectionEndLineAction,
             "control BACK_SLASH", "unselect"/*DefaultEditorKit.unselectAction*/,
                          "ENTER", JTextField.notifyAction,
                "control shift O", "toggle-componentOrientation"/*DefaultEditorKit.toggleComponentOrientation*/
        });

        Object multilineInputMap = new UIDefaults.LazyInputMap(new Object[] {
                           "COPY", DefaultEditorKit.copyAction,
                          "PASTE", DefaultEditorKit.pasteAction,
                            "CUT", DefaultEditorKit.cutAction,
                 "control INSERT", DefaultEditorKit.copyAction,
                   "shift INSERT", DefaultEditorKit.pasteAction,
                   "shift DELETE", DefaultEditorKit.cutAction,
                      "control F", DefaultEditorKit.forwardAction,
                      "control B", DefaultEditorKit.backwardAction,
                      "control D", DefaultEditorKit.deleteNextCharAction,
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
                     "shift LEFT", DefaultEditorKit.selectionBackwardAction,
                    "shift RIGHT", DefaultEditorKit.selectionForwardAction,
                   "control LEFT", DefaultEditorKit.previousWordAction,
                  "control RIGHT", DefaultEditorKit.nextWordAction,
             "control shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
            "control shift RIGHT", DefaultEditorKit.selectionNextWordAction,
                  "control SLASH", DefaultEditorKit.selectAllAction,
                           "HOME", DefaultEditorKit.beginLineAction,
                            "END", DefaultEditorKit.endLineAction,
                     "shift HOME", DefaultEditorKit.selectionBeginLineAction,
                      "shift END", DefaultEditorKit.selectionEndLineAction,

                      "control N", DefaultEditorKit.downAction,
                      "control P", DefaultEditorKit.upAction,
                             "UP", DefaultEditorKit.upAction,
                           "DOWN", DefaultEditorKit.downAction,
                        "PAGE_UP", DefaultEditorKit.pageUpAction,
                      "PAGE_DOWN", DefaultEditorKit.pageDownAction,
                  "shift PAGE_UP", "selection-page-up",
                "shift PAGE_DOWN", "selection-page-down",
             "ctrl shift PAGE_UP", "selection-page-left",
           "ctrl shift PAGE_DOWN", "selection-page-right",
                       "shift UP", DefaultEditorKit.selectionUpAction,
                     "shift DOWN", DefaultEditorKit.selectionDownAction,
                          "ENTER", DefaultEditorKit.insertBreakAction,
                            "TAB", DefaultEditorKit.insertTabAction,
             "control BACK_SLASH", "unselect"/*DefaultEditorKit.unselectAction*/,
                   "control HOME", DefaultEditorKit.beginAction,
                    "control END", DefaultEditorKit.endAction,
             "control shift HOME", DefaultEditorKit.selectionBeginAction,
              "control shift END", DefaultEditorKit.selectionEndAction,
                      "control T", "next-link-action",
                "control shift T", "previous-link-action",
                  "control SPACE", "activate-link-action",
                "control shift O", "toggle-componentOrientation"/*DefaultEditorKit.toggleComponentOrientation*/
        });

        // *** Tree

        Object treeOpenIcon = SwingUtilities2.makeIcon(getClass(),
                                                       MotifLookAndFeel.class,
                                                       "icons/TreeOpen.gif");

        Object treeClosedIcon = SwingUtilities2.makeIcon(getClass(),
                                                         MotifLookAndFeel.class,
                                                         "icons/TreeClosed.gif");

        Object treeLeafIcon = new UIDefaults.LazyValue() {
            public Object createValue(UIDefaults table) {
                return MotifTreeCellRenderer.loadLeafIcon();
            }
        };

        Object treeExpandedIcon = new UIDefaults.LazyValue() {
            public Object createValue(UIDefaults table) {
                return MotifTreeUI.MotifExpandedIcon.createExpandedIcon();
            }
        };

        Object treeCollapsedIcon = new UIDefaults.LazyValue() {
            public Object createValue(UIDefaults table) {
                return MotifTreeUI.MotifCollapsedIcon.createCollapsedIcon();
            }
        };

        Border menuBarBorder = new MotifBorders.MenuBarBorder(
                                          table.getColor("MenuBar.shadow"),
                                          table.getColor("MenuBar.highlight"),
                                          table.getColor("MenuBar.darkShadow"),
                                          table.getColor("activeCaptionBorder"));


        Border menuMarginBorder = new BorderUIResource.CompoundBorderUIResource(
                                          loweredBevelBorder,
                                          marginBorder);


        Border focusCellHighlightBorder = new BorderUIResource.LineBorderUIResource(
                                                table.getColor("activeCaptionBorder"));

        Object sliderFocusInsets = new InsetsUIResource( 0, 0, 0, 0 );

        // ** for tabbedpane

        Object tabbedPaneTabInsets = new InsetsUIResource(3, 4, 3, 4);

        Object tabbedPaneTabPadInsets = new InsetsUIResource(3, 0, 1, 0);

        Object tabbedPaneTabAreaInsets = new InsetsUIResource(4, 2, 0, 8);

        Object tabbedPaneContentBorderInsets = new InsetsUIResource(2, 2, 2, 2);


        // ** for optionpane

        Object optionPaneBorder = new BorderUIResource.EmptyBorderUIResource(10,0,0,0);

        Object optionPaneButtonAreaBorder = new BorderUIResource.EmptyBorderUIResource(10,10,10,10);

        Object optionPaneMessageAreaBorder = new BorderUIResource.EmptyBorderUIResource(10,10,12,10);

        @SuppressWarnings("deprecation")
        final int metaMask = KeyEvent.META_MASK;
        Object[] defaults = {

            "Desktop.background", table.get("desktop"),
            "Desktop.ancestorInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                 "ctrl F5", "restore",
                 "ctrl F4", "close",
                 "ctrl F7", "move",
                 "ctrl F8", "resize",
                   "RIGHT", "right",
                "KP_RIGHT", "right",
             "shift RIGHT", "shrinkRight",
          "shift KP_RIGHT", "shrinkRight",
                    "LEFT", "left",
                 "KP_LEFT", "left",
              "shift LEFT", "shrinkLeft",
           "shift KP_LEFT", "shrinkLeft",
                      "UP", "up",
                   "KP_UP", "up",
                "shift UP", "shrinkUp",
             "shift KP_UP", "shrinkUp",
                    "DOWN", "down",
                 "KP_DOWN", "down",
              "shift DOWN", "shrinkDown",
           "shift KP_DOWN", "shrinkDown",
                  "ESCAPE", "escape",
                 "ctrl F9", "minimize",
                "ctrl F10", "maximize",
                 "ctrl F6", "selectNextFrame",
                "ctrl TAB", "selectNextFrame",
             "ctrl alt F6", "selectNextFrame",
       "shift ctrl alt F6", "selectPreviousFrame",
                "ctrl F12", "navigateNext",
          "shift ctrl F12", "navigatePrevious"
              }),

            "Panel.background", table.get("control"),
            "Panel.foreground", table.get("textText"),
            "Panel.font", dialogPlain12,

            "ProgressBar.font", dialogPlain12,
            "ProgressBar.foreground", controlDarker,
            "ProgressBar.background", table.get("control"),
            "ProgressBar.selectionForeground", table.get("control"),
            "ProgressBar.selectionBackground", table.get("controlText"),
            "ProgressBar.border", loweredBevelBorder,
            "ProgressBar.cellLength", 6,
            "ProgressBar.cellSpacing", Integer.valueOf(0),

            // Buttons
            "Button.margin", new InsetsUIResource(2, 4, 2, 4),
            "Button.border", buttonBorder,
            "Button.background", table.get("control"),
            "Button.foreground", table.get("controlText"),
            "Button.select", table.get("controlLightShadow"),
            "Button.font", dialogPlain12,
            "Button.focusInputMap", new UIDefaults.LazyInputMap(new Object[] {
                          "SPACE", "pressed",
                 "released SPACE", "released"
              }),

            "CheckBox.textIconGap", 8,
            "CheckBox.margin", new InsetsUIResource(4, 2, 4, 2),
            "CheckBox.icon", checkBoxIcon,
            "CheckBox.focus", table.get("activeCaptionBorder"),
            "CheckBox.focusInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                            "SPACE", "pressed",
                   "released SPACE", "released"
                 }),

            "RadioButton.margin", new InsetsUIResource(4, 2, 4, 2),
            "RadioButton.textIconGap", 8,
            "RadioButton.background", table.get("control"),
            "RadioButton.foreground", table.get("controlText"),
            "RadioButton.icon", radioButtonIcon,
            "RadioButton.focus", table.get("activeCaptionBorder"),
            "RadioButton.icon", radioButtonIcon,
            "RadioButton.focusInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                          "SPACE", "pressed",
                 "released SPACE", "released"
              }),

            "ToggleButton.border", toggleButtonBorder,
            "ToggleButton.background", table.get("control"),
            "ToggleButton.foreground", table.get("controlText"),
            "ToggleButton.focus", table.get("controlText"),
            "ToggleButton.select", table.get("controlLightShadow"),
            "ToggleButton.focusInputMap",
              new UIDefaults.LazyInputMap(new Object[] {
                            "SPACE", "pressed",
                   "released SPACE", "released"
                }),

            // Menus
            "Menu.border", menuMarginBorder,
            "Menu.font", dialogPlain12,
            "Menu.acceleratorFont", dialogPlain12,
            "Menu.acceleratorSelectionForeground", menuItemPressedForeground,
            "Menu.foreground", table.get("menuText"),
            "Menu.background", table.get("menu"),
            "Menu.selectionForeground", menuItemPressedForeground,
            "Menu.selectionBackground", menuItemPressedBackground,
            "Menu.checkIcon", menuItemCheckIcon,
            "Menu.arrowIcon", menuArrowIcon,
            "Menu.menuPopupOffsetX", 0,
            "Menu.menuPopupOffsetY", 0,
            "Menu.submenuPopupOffsetX", -2,
            "Menu.submenuPopupOffsetY", 3,
            "Menu.shortcutKeys", new int[]{
                SwingUtilities2.getSystemMnemonicKeyMask(), metaMask,
                SwingUtilities2.setAltGraphMask(
                        SwingUtilities2.getSystemMnemonicKeyMask())
            },
            "Menu.cancelMode", "hideMenuTree",

            "MenuBar.border", menuBarBorder,
            "MenuBar.background", table.get("menu"),
            "MenuBar.foreground", table.get("menuText"),
            "MenuBar.font", dialogPlain12,
            "MenuBar.windowBindings", new Object[] {
                "F10", "takeFocus" },

            "MenuItem.border", menuMarginBorder,
            "MenuItem.font", dialogPlain12,
            "MenuItem.acceleratorFont", dialogPlain12,
            "MenuItem.acceleratorSelectionForeground", menuItemPressedForeground,
            "MenuItem.foreground", table.get("menuText"),
            "MenuItem.background", table.get("menu"),
            "MenuItem.selectionForeground", menuItemPressedForeground,
            "MenuItem.selectionBackground", menuItemPressedBackground,
            "MenuItem.checkIcon", menuItemCheckIcon,
            "MenuItem.arrowIcon", menuItemArrowIcon,

            "RadioButtonMenuItem.border", menuMarginBorder,
            "RadioButtonMenuItem.font", dialogPlain12,
            "RadioButtonMenuItem.acceleratorFont", dialogPlain12,
            "RadioButtonMenuItem.acceleratorSelectionForeground", menuItemPressedForeground,
            "RadioButtonMenuItem.foreground", table.get("menuText"),
            "RadioButtonMenuItem.background", table.get("menu"),
            "RadioButtonMenuItem.selectionForeground", menuItemPressedForeground,
            "RadioButtonMenuItem.selectionBackground", menuItemPressedBackground,
            "RadioButtonMenuItem.checkIcon", radioButtonIcon,
            "RadioButtonMenuItem.arrowIcon", menuItemArrowIcon,

            "CheckBoxMenuItem.border", menuMarginBorder,
            "CheckBoxMenuItem.font", dialogPlain12,
            "CheckBoxMenuItem.acceleratorFont", dialogPlain12,
            "CheckBoxMenuItem.acceleratorSelectionForeground", menuItemPressedForeground,
            "CheckBoxMenuItem.foreground", table.get("menuText"),
            "CheckBoxMenuItem.background", table.get("menu"),
            "CheckBoxMenuItem.selectionForeground", menuItemPressedForeground,
            "CheckBoxMenuItem.selectionBackground", menuItemPressedBackground,
            "CheckBoxMenuItem.checkIcon", checkBoxIcon,
            "CheckBoxMenuItem.arrowIcon", menuItemArrowIcon,

            "PopupMenu.background", table.get("menu"),
            "PopupMenu.border", popupMenuBorder,
            "PopupMenu.foreground", table.get("menuText"),
            "PopupMenu.font", dialogPlain12,
            "PopupMenu.consumeEventOnClose", Boolean.FALSE,

            "Label.font", dialogPlain12,
            "Label.background", table.get("control"),
            "Label.foreground", table.get("controlText"),

            "Separator.shadow", table.get("controlShadow"),          // DEPRECATED - DO NOT USE!
            "Separator.highlight", table.get("controlLtHighlight"),  // DEPRECATED - DO NOT USE!

            "Separator.background", table.get("controlLtHighlight"),
            "Separator.foreground", table.get("controlShadow"),

            "List.focusCellHighlightBorder", focusCellHighlightBorder,
            "List.focusInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
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

            "DesktopIcon.icon", SwingUtilities2.makeIcon(getClass(),
                                                         MotifLookAndFeel.class,
                                                         "icons/DesktopIcon.gif"),
            "DesktopIcon.border", null,
            // These are a little odd, MotifInternalFrameUI isntalls em!
            "DesktopIcon.windowBindings", new Object[]
              { "ESCAPE", "hideSystemMenu" },

            "InternalFrame.activeTitleBackground", table.get("activeCaptionBorder"),
            "InternalFrame.inactiveTitleBackground", table.get("inactiveCaptionBorder"),
            "InternalFrame.windowBindings", new Object[] {
                "shift ESCAPE", "showSystemMenu",
                  "ctrl SPACE", "showSystemMenu",
                      "ESCAPE", "hideSystemMenu"
            },

            "ScrollBar.background", scrollBarTrack,
            "ScrollBar.foreground", table.get("control"),
            "ScrollBar.track", scrollBarTrack,
            "ScrollBar.trackHighlight", table.get("controlDkShadow"),
            "ScrollBar.thumb", table.get("control"),
            "ScrollBar.thumbHighlight", table.get("controlHighlight"),
            "ScrollBar.thumbDarkShadow", table.get("controlDkShadow"),
            "ScrollBar.thumbShadow", table.get("controlShadow"),
            "ScrollBar.border", loweredBevelBorder,
            "ScrollBar.allowsAbsolutePositioning", Boolean.TRUE,
            "ScrollBar.ancestorInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                       "RIGHT", "positiveUnitIncrement",
                    "KP_RIGHT", "positiveUnitIncrement",
                        "DOWN", "positiveUnitIncrement",
                     "KP_DOWN", "positiveUnitIncrement",
                   "PAGE_DOWN", "positiveBlockIncrement",
              "ctrl PAGE_DOWN", "positiveBlockIncrement",
                        "LEFT", "negativeUnitIncrement",
                     "KP_LEFT", "negativeUnitIncrement",
                          "UP", "negativeUnitIncrement",
                       "KP_UP", "negativeUnitIncrement",
                     "PAGE_UP", "negativeBlockIncrement",
                "ctrl PAGE_UP", "negativeBlockIncrement",
                        "HOME", "minScroll",
                         "END", "maxScroll"
                 }),

            "ScrollPane.font", dialogPlain12,
            "ScrollPane.background", table.get("control"),
            "ScrollPane.foreground", table.get("controlText"),
            "ScrollPane.border", null,
            "ScrollPane.viewportBorder", loweredBevelBorder,
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

            "Slider.font", dialogPlain12,
            "Slider.border", focusBevelBorder,
            "Slider.foreground", table.get("control"),
            "Slider.background", controlDarker,
            "Slider.highlight", table.get("controlHighlight"),
            "Slider.shadow", table.get("controlShadow"),
            "Slider.focus", table.get("controlDkShadow"),
            "Slider.focusInsets", sliderFocusInsets,
            "Slider.focusInputMap", new UIDefaults.LazyInputMap(new Object[] {
                         "RIGHT", "positiveUnitIncrement",
                      "KP_RIGHT", "positiveUnitIncrement",
                          "DOWN", "negativeUnitIncrement",
                       "KP_DOWN", "negativeUnitIncrement",
                "ctrl PAGE_DOWN", "negativeBlockIncrement",
                          "LEFT", "negativeUnitIncrement",
                       "KP_LEFT", "negativeUnitIncrement",
                            "UP", "positiveUnitIncrement",
                         "KP_UP", "positiveUnitIncrement",
                  "ctrl PAGE_UP", "positiveBlockIncrement",
                          "HOME", "minScroll",
                           "END", "maxScroll"
            }),

            // Spinner
            "Spinner.ancestorInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                               "UP", "increment",
                            "KP_UP", "increment",
                             "DOWN", "decrement",
                          "KP_DOWN", "decrement",
               }),
            "Spinner.border", textFieldBorder,

            "SplitPane.background", table.get("control"),
            "SplitPane.highlight", table.get("controlHighlight"),
            "SplitPane.shadow", table.get("controlShadow"),
            "SplitPane.dividerSize", Integer.valueOf(20),
            "SplitPane.activeThumb", table.get("activeCaptionBorder"),
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

            "TabbedPane.font", dialogPlain12,
            "TabbedPane.background", table.get("control"),
            "TabbedPane.foreground", table.get("controlText"),
            "TabbedPane.light", table.get("controlHighlight"),
            "TabbedPane.highlight", table.get("controlLtHighlight"),
            "TabbedPane.shadow", table.get("controlShadow"),
            "TabbedPane.darkShadow", table.get("controlShadow"),
            "TabbedPane.unselectedTabBackground", unselectedTabBackground,
            "TabbedPane.unselectedTabForeground", unselectedTabForeground,
            "TabbedPane.unselectedTabHighlight", unselectedTabHighlight,
            "TabbedPane.unselectedTabShadow", unselectedTabShadow,
            "TabbedPane.focus", table.get("activeCaptionBorder"),
            "TabbedPane.tabInsets", tabbedPaneTabInsets,
            "TabbedPane.selectedTabPadInsets", tabbedPaneTabPadInsets,
            "TabbedPane.tabAreaInsets", tabbedPaneTabAreaInsets,
            "TabbedPane.contentBorderInsets", tabbedPaneContentBorderInsets,
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
                }),
            "TabbedPane.ancestorInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                   "ctrl PAGE_DOWN", "navigatePageDown",
                     "ctrl PAGE_UP", "navigatePageUp",
                          "ctrl UP", "requestFocus",
                       "ctrl KP_UP", "requestFocus",
                 }),


            "Tree.background", controlDarker,                              // default: dark slate blue
            "Tree.hash", table.get("controlDkShadow"),                     // default: black
            "Tree.iconShadow", table.get("controlShadow"),
            "Tree.iconHighlight", table.get("controlHighlight"),
            "Tree.iconBackground", table.get("control"),
            "Tree.iconForeground", table.get("controlShadow"),             // default: black
            "Tree.textBackground", controlDarker,             // default: dark slate blue
            "Tree.textForeground", table.get("textText"),           // default: black
            "Tree.selectionBackground", table.get("text"),            // default: white
            "Tree.selectionForeground", table.get("textText"),              // default: black
            "Tree.selectionBorderColor", table.get("activeCaptionBorder"), // default: maroon
            "Tree.openIcon", treeOpenIcon,
            "Tree.closedIcon", treeClosedIcon,
            "Tree.leafIcon", treeLeafIcon,
            "Tree.expandedIcon", treeExpandedIcon,
            "Tree.collapsedIcon", treeCollapsedIcon,
            "Tree.editorBorder", focusBorder,
            "Tree.editorBorderSelectionColor", table.get("activeCaptionBorder"),
            "Tree.rowHeight", 18,
            "Tree.drawsFocusBorderAroundIcon", Boolean.TRUE,
            "Tree.focusInputMap", new UIDefaults.LazyInputMap(new Object[] {
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
            "Tree.ancestorInputMap", new UIDefaults.LazyInputMap(new Object[] {
                "ESCAPE", "cancel" }),

            "Table.focusCellHighlightBorder", focusCellHighlightBorder,
            "Table.scrollPaneBorder", null,
            "Table.dropLineShortColor", table.get("activeCaptionBorder"),

            //      "Table.background", white,  // cell background color
            "Table.ancestorInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
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


            "FormattedTextField.focusInputMap",
              new UIDefaults.LazyInputMap(new Object[] {
                           "ctrl C", DefaultEditorKit.copyAction,
                           "ctrl V", DefaultEditorKit.pasteAction,
                           "ctrl X", DefaultEditorKit.cutAction,
                             "COPY", DefaultEditorKit.copyAction,
                            "PASTE", DefaultEditorKit.pasteAction,
                              "CUT", DefaultEditorKit.cutAction,
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

            // ToolBar.
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



            "ComboBox.control", table.get("control"),
            "ComboBox.controlForeground", black,
            "ComboBox.background", table.get("window"),
            "ComboBox.foreground", black,
            "ComboBox.border", comboBoxBorder,
            "ComboBox.selectionBackground", black,
            "ComboBox.selectionForeground", table.get("text"),
            "ComboBox.disabledBackground", table.get("control"),
            "ComboBox.disabledForeground", table.get("textInactiveText"),
            "ComboBox.font", dialogPlain12,
            "ComboBox.ancestorInputMap", new UIDefaults.LazyInputMap(new Object[] {
                   "ESCAPE", "hidePopup",
                  "PAGE_UP", "pageUpPassThrough",
                "PAGE_DOWN", "pageDownPassThrough",
                     "HOME", "homePassThrough",
                      "END", "endPassThrough",
                     "DOWN", "selectNext",
                  "KP_DOWN", "selectNext",
                       "UP", "selectPrevious",
                    "KP_UP", "selectPrevious",
                    "SPACE", "spacePopup",
                    "ENTER", "enterPressed"

              }),

            "TextField.caretForeground", black,
            "TextField.caretBlinkRate", Integer.valueOf(500),
            "TextField.inactiveForeground", table.get("textInactiveText"),
            "TextField.selectionBackground", table.get("textHighlight"),
            "TextField.selectionForeground", table.get("textHighlightText"),
            "TextField.background", table.get("window"),
            "TextField.foreground", table.get("textText"),
            "TextField.font", sansSerifPlain12,
            "TextField.border", textFieldBorder,
            "TextField.focusInputMap", fieldInputMap,

            "PasswordField.caretForeground", black,
            "PasswordField.caretBlinkRate", Integer.valueOf(500),
            "PasswordField.inactiveForeground", table.get("textInactiveText"),
            "PasswordField.selectionBackground", table.get("textHighlight"),
            "PasswordField.selectionForeground", table.get("textHighlightText"),
            "PasswordField.background", table.get("window"),
            "PasswordField.foreground", table.get("textText"),
            "PasswordField.font", monospacedPlain12,
            "PasswordField.border", textFieldBorder,
            "PasswordField.focusInputMap", passwordInputMap,

            "TextArea.caretForeground", black,
            "TextArea.caretBlinkRate", Integer.valueOf(500),
            "TextArea.inactiveForeground", table.get("textInactiveText"),
            "TextArea.selectionBackground", table.get("textHighlight"),
            "TextArea.selectionForeground", table.get("textHighlightText"),
            "TextArea.background", table.get("window"),
            "TextArea.foreground", table.get("textText"),
            "TextArea.font", monospacedPlain12,
            "TextArea.border", marginBorder,
            "TextArea.focusInputMap", multilineInputMap,

            "TextPane.caretForeground", black,
            "TextPane.caretBlinkRate", Integer.valueOf(500),
            "TextPane.inactiveForeground", table.get("textInactiveText"),
            "TextPane.selectionBackground", lightGray,
            "TextPane.selectionForeground", table.get("textHighlightText"),
            "TextPane.background", white,
            "TextPane.foreground", table.get("textText"),
            "TextPane.font", serifPlain12,
            "TextPane.border", marginBorder,
            "TextPane.focusInputMap", multilineInputMap,

            "EditorPane.caretForeground", red,
            "EditorPane.caretBlinkRate", Integer.valueOf(500),
            "EditorPane.inactiveForeground", table.get("textInactiveText"),
            "EditorPane.selectionBackground", lightGray,
            "EditorPane.selectionForeground", table.get("textHighlightText"),
            "EditorPane.background", white,
            "EditorPane.foreground", table.get("textText"),
            "EditorPane.font", serifPlain12,
            "EditorPane.border", marginBorder,
            "EditorPane.focusInputMap", multilineInputMap,


            "FileChooser.ancestorInputMap",
               new UIDefaults.LazyInputMap(new Object[] {
                     "ESCAPE", "cancelSelection"
                 }),


            "ToolTip.border", raisedBevelBorder,
            "ToolTip.background", table.get("info"),
            "ToolTip.foreground", table.get("infoText"),

            // These window InputMap bindings are used when the Menu is
            // selected.
            "PopupMenu.selectedWindowInputMapBindings", new Object[] {
                  "ESCAPE", "cancel",
                     "TAB", "cancel",
               "shift TAB", "cancel",
                    "DOWN", "selectNext",
                 "KP_DOWN", "selectNext",
                      "UP", "selectPrevious",
                   "KP_UP", "selectPrevious",
                    "LEFT", "selectParent",
                 "KP_LEFT", "selectParent",
                   "RIGHT", "selectChild",
                "KP_RIGHT", "selectChild",
                   "ENTER", "return",
                   "ctrl ENTER", "return",
                   "SPACE", "return"
            },


            "OptionPane.border", optionPaneBorder,
            "OptionPane.messageAreaBorder", optionPaneMessageAreaBorder,
            "OptionPane.buttonAreaBorder", optionPaneButtonAreaBorder,
            "OptionPane.errorIcon", SwingUtilities2.makeIcon(getClass(),
                                                             MotifLookAndFeel.class,
                                                             "icons/Error.gif"),
            "OptionPane.informationIcon", SwingUtilities2.makeIcon(getClass(),
                                                                   MotifLookAndFeel.class,
                                                                   "icons/Inform.gif"),
            "OptionPane.warningIcon", SwingUtilities2.makeIcon(getClass(),
                                                               MotifLookAndFeel.class,
                                                               "icons/Warn.gif"),
            "OptionPane.questionIcon", SwingUtilities2.makeIcon(getClass(),
                                                                MotifLookAndFeel.class,
                                                                "icons/Question.gif"),
            "OptionPane.windowBindings", new Object[] {
                "ESCAPE", "close" },

            // These bindings are only enabled when there is a default
            // button set on the rootpane.
            "RootPane.defaultButtonWindowKeyBindings", new Object[] {
                             "ENTER", "press",
                    "released ENTER", "release",
                        "ctrl ENTER", "press",
               "ctrl released ENTER", "release"
              },
        };

        table.putDefaults(defaults);
    }

}
