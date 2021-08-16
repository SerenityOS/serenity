/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.apple.laf;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.PopupFactory;
import javax.swing.SwingConstants;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.basic.BasicBorders;
import javax.swing.plaf.basic.BasicLookAndFeel;

import apple.laf.JRSUIControl;
import apple.laf.JRSUIUtils;
import sun.swing.SwingAccessor;
import sun.swing.SwingUtilities2;

import static javax.swing.UIDefaults.LazyValue;

@SuppressWarnings("serial") // Superclass is not serializable across versions
public class AquaLookAndFeel extends BasicLookAndFeel {
    static final String sPropertyPrefix = "apple.laf."; // new prefix for things like 'useScreenMenuBar'

    // for lazy initalizers. Following the pattern from metal.
    private static final String PKG_PREFIX = "com.apple.laf.";

    /**
     * Return a short string that identifies this look and feel, e.g.
     * "CDE/Motif".  This string should be appropriate for a menu item.
     * Distinct look and feels should have different names, e.g.
     * a subclass of MotifLookAndFeel that changes the way a few components
     * are rendered should be called "CDE/Motif My Way"; something
     * that would be useful to a user trying to select a L&F from a list
     * of names.
     */
    public String getName() {
        return "Mac OS X";
    }

    /**
     * Return a string that identifies this look and feel.  This string
     * will be used by applications/services that want to recognize
     * well known look and feel implementations.  Presently
     * the well known names are "Motif", "Windows", "Mac", "Metal".  Note
     * that a LookAndFeel derived from a well known superclass
     * that doesn't make any fundamental changes to the look or feel
     * shouldn't override this method.
     */
    public String getID() {
        return "Aqua";
    }

    /**
     * Return a one line description of this look and feel implementation,
     * e.g. "The CDE/Motif Look and Feel".   This string is intended for
     * the user, e.g. in the title of a window or in a ToolTip message.
     */
    public String getDescription() {
        return "Aqua Look and Feel for Mac OS X";
    }

    /**
     * Returns true if the {@code LookAndFeel} returned
     * {@code RootPaneUI} instances support providing Window decorations
     * in a {@code JRootPane}.
     * <p>
     * The default implementation returns false, subclasses that support
     * Window decorations should override this and return true.
     *
     * @return True if the RootPaneUI instances created support client side
     *             decorations
     * @see JDialog#setDefaultLookAndFeelDecorated
     * @see JFrame#setDefaultLookAndFeelDecorated
     * @see JRootPane#setWindowDecorationStyle
     * @since 1.4
     */
    public boolean getSupportsWindowDecorations() {
        return false;
    }

    /**
     * If the underlying platform has a "native" look and feel, and this
     * is an implementation of it, return true.
     */
    public boolean isNativeLookAndFeel() {
        return true;
    }

    /**
     * Return true if the underlying platform supports and or permits
     * this look and feel.  This method returns false if the look
     * and feel depends on special resources or legal agreements that
     * aren't defined for the current platform.
     *
     * @see UIManager#setLookAndFeel
     */
    public boolean isSupportedLookAndFeel() {
        return true;
    }

    /**
     * UIManager.setLookAndFeel calls this method before the first
     * call (and typically the only call) to getDefaults().  Subclasses
     * should do any one-time setup they need here, rather than
     * in a static initializer, because look and feel class objects
     * may be loaded just to discover that isSupportedLookAndFeel()
     * returns false.
     *
     * @see #uninitialize
     * @see UIManager#setLookAndFeel
     */
    @SuppressWarnings("removal")
    public void initialize() {
        java.security.AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    System.loadLibrary("osxui");
                    return null;
                }
            });

        java.security.AccessController.doPrivileged(new PrivilegedAction<Void>(){
            @Override
            public Void run() {
                JRSUIControl.initJRSUI();
                return null;
            }
        });

        super.initialize();
        final ScreenPopupFactory spf = new ScreenPopupFactory();
        spf.setActive(true);
        PopupFactory.setSharedInstance(spf);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(AquaMnemonicHandler.getInstance());
    }

    /**
     * UIManager.setLookAndFeel calls this method just before we're
     * replaced by a new default look and feel.   Subclasses may
     * choose to free up some resources here.
     *
     * @see #initialize
     */
    public void uninitialize() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventPostProcessor(AquaMnemonicHandler.getInstance());

        final PopupFactory popupFactory = PopupFactory.getSharedInstance();
        if (popupFactory != null && popupFactory instanceof ScreenPopupFactory) {
            ((ScreenPopupFactory)popupFactory).setActive(false);
        }

        super.uninitialize();
    }

    /**
     * Returns an {@code ActionMap}.
     * <P>
     * This {@code ActionMap} contains {@code Actions} that
     * embody the ability to render an auditory cue. These auditory
     * cues map onto user and system activities that may be useful
     * for an end user to know about (such as a dialog box appearing).
     * <P>
     * At the appropriate time in a {@code JComponent} UI's lifecycle,
     * the ComponentUI is responsible for getting the appropriate
     * {@code Action} out of the {@code ActionMap} and passing
     * it on to {@code playSound}.
     * <P>
     * The {@code Actions} in this {@code ActionMap} are
     * created by the {@code createAudioAction} method.
     *
     * @return      an ActionMap containing Actions
     *              responsible for rendering auditory cues
     * @see #createAudioAction
     * @see #playSound(Action)
     * @since 1.4
     */
    protected ActionMap getAudioActionMap() {
        ActionMap audioActionMap = (ActionMap)UIManager.get("AuditoryCues.actionMap");
        if (audioActionMap != null) return audioActionMap;

        final Object[] acList = (Object[])UIManager.get("AuditoryCues.cueList");
        if (acList != null) {
            audioActionMap = new ActionMapUIResource();
            for (int counter = acList.length - 1; counter >= 0; counter--) {
                audioActionMap.put(acList[counter], createAudioAction(acList[counter]));
            }
        }
        UIManager.getLookAndFeelDefaults().put("AuditoryCues.actionMap", audioActionMap);

        return audioActionMap;
    }

    /**
     * We override getDefaults() so we can install our own debug defaults
     * if needed for testing
     */
    public UIDefaults getDefaults() {
        final UIDefaults table = new UIDefaults();
        // use debug defaults if you want to see every query into the defaults object.
        //UIDefaults table = new DebugDefaults();
        try {
            // PopupFactory.getSharedInstance().setPopupType(2);
            initClassDefaults(table);

            // Here we install all the Basic defaults in case we missed some in our System color
            // or component defaults that follow. Eventually we will take this out.
            // This is a big negative to performance so we want to get it out as soon
            // as we are comfortable with the Aqua defaults.
            super.initSystemColorDefaults(table);
            super.initComponentDefaults(table);

            // Because the last elements added win in precedence we add all of our aqua elements here.
            initSystemColorDefaults(table);
            initComponentDefaults(table);
        } catch(final Exception e) {
            e.printStackTrace();
        }
        return table;
    }

    /**
     * Initialize the defaults table with the name of the ResourceBundle
     * used for getting localized defaults.  Also initialize the default
     * locale used when no locale is passed into UIDefaults.get().  The
     * default locale should generally not be relied upon. It is here for
     * compatibility with releases prior to 1.4.
     */
    private void initResourceBundle(final UIDefaults table) {
        table.setDefaultLocale(Locale.getDefault());
        SwingAccessor.getUIDefaultsAccessor().addInternalBundle(table,
                PKG_PREFIX + "resources.aqua");
        try {
            final ResourceBundle aquaProperties = ResourceBundle.getBundle(PKG_PREFIX + "resources.aqua");
            final Enumeration<String> propertyKeys = aquaProperties.getKeys();

            while (propertyKeys.hasMoreElements()) {
                final String key = propertyKeys.nextElement();
                table.put(key, aquaProperties.getString(key));
            }
        } catch (final Exception e) {
        }
    }

    /**
     * This is the last step in the getDefaults routine usually called from our superclass
     */
    protected void initComponentDefaults(final UIDefaults table) {
        initResourceBundle(table);

        final InsetsUIResource zeroInsets = new InsetsUIResource(0, 0, 0, 0);
        final InsetsUIResource menuItemMargin = zeroInsets;

        // <rdar://problem/5189013> Entire Java application window refreshes when moving off Shortcut menu item
        final Boolean useOpaqueComponents = Boolean.TRUE;

        final Boolean buttonShouldBeOpaque = Boolean.FALSE;

        // *** List value objects
        final Object listCellRendererActiveValue = new UIDefaults.ActiveValue(){
            public Object createValue(UIDefaults defaultsTable) {
                return new DefaultListCellRenderer.UIResource();
            }
        };

        // SJA - I'm basing this on what is in the MetalLookAndFeel class, but
        // without being based on BasicLookAndFeel. We want more flexibility.
        // The key to doing this well is to use Lazy initializing classes as
        // much as possible.

        // Here I want to go to native and get all the values we'd need for colors etc.
        final Border toolTipBorder = new BorderUIResource.EmptyBorderUIResource(2, 0, 2, 0);
        final ColorUIResource toolTipBackground = new ColorUIResource(255, 255, (int)(255.0 * 0.80));
        final ColorUIResource black = new ColorUIResource(Color.black);
        final ColorUIResource white = new ColorUIResource(Color.white);
        final ColorUIResource smokyGlass = new ColorUIResource(new Color(0, 0, 0, 152));
        final ColorUIResource dockIconRim = new ColorUIResource(new Color(192, 192, 192, 192));
        final ColorUIResource mediumTranslucentBlack = new ColorUIResource(new Color(0, 0, 0, 100));
        final ColorUIResource translucentWhite = new ColorUIResource(new Color(255, 255, 255, 254));
    //    final ColorUIResource lightGray = new ColorUIResource(232, 232, 232);
        final ColorUIResource disabled = new ColorUIResource(0.5f, 0.5f, 0.5f);
        final ColorUIResource disabledShadow = new ColorUIResource(0.25f, 0.25f, 0.25f);
        final ColorUIResource selected = new ColorUIResource(1.0f, 0.4f, 0.4f);

        // Contrast tab UI colors

        final ColorUIResource selectedTabTitlePressedColor = new ColorUIResource(240, 240, 240);
        final ColorUIResource selectedTabTitleDisabledColor = new ColorUIResource(new Color(1, 1, 1, 0.55f));
        final ColorUIResource selectedTabTitleNormalColor = white;
        final Color selectedControlTextColor = AquaImageFactory.getSelectedControlColorUIResource();
        final ColorUIResource selectedTabTitleShadowDisabledColor = new ColorUIResource(new Color(0, 0, 0, 0.25f));
        final ColorUIResource selectedTabTitleShadowNormalColor = mediumTranslucentBlack;
        final ColorUIResource nonSelectedTabTitleNormalColor = black;

        final ColorUIResource toolbarDragHandleColor = new ColorUIResource(140, 140, 140);

        // sja todo Make these lazy values so we only get them when required - if we deem it necessary
        // it may be the case that we think the overhead of a proxy lazy value is not worth delaying
        // creating the object if we think that most swing apps will use this.
        // the lazy value is useful for delaying initialization until this default value is actually
        // accessed by the LAF instead of at init time, so making it lazy should speed up
        // our launch times of Swing apps.

        // *** Text value objects
        final LazyValue marginBorder = t -> new BasicBorders.MarginBorder();

        final int zero = 0;
        final Object editorMargin = zeroInsets; // this is not correct - look at TextEdit to determine the right margin
        final int textCaretBlinkRate = 500;
        final LazyValue textFieldBorder = t ->
            AquaTextFieldBorder.getTextFieldBorder();
        final Object textAreaBorder = marginBorder; // text areas have no real border - radar 311073

        final LazyValue scollListBorder = t ->
            AquaScrollRegionBorder.getScrollRegionBorder();
        final LazyValue aquaTitledBorder = t ->
            AquaGroupBorder.getBorderForTitledBorder();
        final LazyValue aquaInsetBorder = t ->
            AquaGroupBorder.getTitlelessBorder();

        final Border listHeaderBorder = AquaTableHeaderBorder.getListHeaderBorder();
        final Border zeroBorder = new BorderUIResource.EmptyBorderUIResource(0, 0, 0, 0);

        // we can't seem to proxy Colors
        final Color selectionBackground = AquaImageFactory.getSelectionBackgroundColorUIResource();
        final Color selectionForeground = AquaImageFactory.getSelectionForegroundColorUIResource();
        final Color selectionInactiveBackground = AquaImageFactory.getSelectionInactiveBackgroundColorUIResource();
        final Color selectionInactiveForeground = AquaImageFactory.getSelectionInactiveForegroundColorUIResource();

        final Color textHighlightText = AquaImageFactory.getTextSelectionForegroundColorUIResource();
        final Color textHighlight = AquaImageFactory.getTextSelectionBackgroundColorUIResource();
        final Color textHighlightInactive = new ColorUIResource(212, 212, 212);

        final Color textInactiveText = disabled;
        final Color textForeground = black;
        final Color textBackground = white;
        final Color textInactiveBackground = white;

        final Color textPasswordFieldCapsLockIconColor = mediumTranslucentBlack;

        final LazyValue internalFrameBorder = t ->
            BasicBorders.getInternalFrameBorder();
        final Color desktopBackgroundColor = new ColorUIResource(new Color(65, 105, 170));//SystemColor.desktop

        final Color focusRingColor = AquaImageFactory.getFocusRingColorUIResource();
        final Border focusCellHighlightBorder = new BorderUIResource.LineBorderUIResource(focusRingColor);

        final Color windowBackgroundColor = AquaImageFactory.getWindowBackgroundColorUIResource();
        final Color panelBackgroundColor = windowBackgroundColor;
        final Color tabBackgroundColor = windowBackgroundColor;
        final Color controlBackgroundColor = windowBackgroundColor;

        final LazyValue controlFont = t -> AquaFonts.getControlTextFont();
        final LazyValue controlSmallFont = t ->
            AquaFonts.getControlTextSmallFont();
        final LazyValue alertHeaderFont = t -> AquaFonts.getAlertHeaderFont();
        final LazyValue menuFont = t -> AquaFonts.getMenuFont();
        final LazyValue viewFont = t -> AquaFonts.getViewFont();

        final Color menuBackgroundColor = new ColorUIResource(Color.white);
        final Color menuForegroundColor = black;

        final Color menuSelectedForegroundColor = white;
        final Color menuSelectedBackgroundColor = focusRingColor;

        final Color menuDisabledBackgroundColor = menuBackgroundColor;
        final Color menuDisabledForegroundColor = disabled;

        final Color menuAccelForegroundColor = black;
        final Color menuAccelSelectionForegroundColor = black;

        final Border menuBorder = new AquaMenuBorder();

        final UIDefaults.LazyInputMap controlFocusInputMap = new UIDefaults.LazyInputMap(new Object[]{
            "SPACE", "pressed",
            "released SPACE", "released"
        });

        // sja testing
        final LazyValue confirmIcon = t ->
            AquaImageFactory.getConfirmImageIcon();
        final LazyValue cautionIcon = t ->
            AquaImageFactory.getCautionImageIcon();
        final LazyValue stopIcon = t ->
            AquaImageFactory.getStopImageIcon();
        final LazyValue securityIcon = t ->
            AquaImageFactory.getLockImageIcon();

        final AquaKeyBindings aquaKeyBindings = AquaKeyBindings.instance();

        final Object[] defaults = {
            "control", windowBackgroundColor, /* Default color for controls (buttons, sliders, etc) */

            // Buttons
            "Button.background", controlBackgroundColor,
            "Button.foreground", black,
            "Button.disabledText", disabled,
            "Button.select", selected,
            "Button.border",(LazyValue) t -> AquaButtonBorder.getDynamicButtonBorder(),
            "Button.font", controlFont,
            "Button.textIconGap", Integer.valueOf(4),
            "Button.textShiftOffset", zero, // radar 3308129 - aqua doesn't move images when pressed.
            "Button.focusInputMap", controlFocusInputMap,
            "Button.margin", new InsetsUIResource(0, 2, 0, 2),
            "Button.opaque", buttonShouldBeOpaque,

            "CheckBox.background", controlBackgroundColor,
            "CheckBox.foreground", black,
            "CheckBox.disabledText", disabled,
            "CheckBox.select", selected,
            "CheckBox.icon",(LazyValue) t -> AquaButtonCheckBoxUI.getSizingCheckBoxIcon(),
            "CheckBox.font", controlFont,
            "CheckBox.border", AquaButtonBorder.getBevelButtonBorder(),
            "CheckBox.margin", new InsetsUIResource(1, 1, 0, 1),
            // radar 3583849. This property never gets
            // used. The border for the Checkbox gets overridden
            // by AquaRadiButtonUI.setThemeBorder(). Needs refactoring. (vm)
            // why is button focus commented out?
            //"CheckBox.focus", getFocusColor(),
            "CheckBox.focusInputMap", controlFocusInputMap,

            "CheckBoxMenuItem.font", menuFont,
            "CheckBoxMenuItem.acceleratorFont", menuFont,
            "CheckBoxMenuItem.background", menuBackgroundColor,
            "CheckBoxMenuItem.foreground", menuForegroundColor,
            "CheckBoxMenuItem.selectionBackground", menuSelectedBackgroundColor,
            "CheckBoxMenuItem.selectionForeground", menuSelectedForegroundColor,
            "CheckBoxMenuItem.disabledBackground", menuDisabledBackgroundColor,
            "CheckBoxMenuItem.disabledForeground", menuDisabledForegroundColor,
            "CheckBoxMenuItem.acceleratorForeground", menuAccelForegroundColor,
            "CheckBoxMenuItem.acceleratorSelectionForeground", menuAccelSelectionForegroundColor,
            "CheckBoxMenuItem.acceleratorDelimiter", "",
            "CheckBoxMenuItem.border", menuBorder, // for inset calculation
            "CheckBoxMenuItem.margin", menuItemMargin,
            "CheckBoxMenuItem.borderPainted", Boolean.TRUE,
            "CheckBoxMenuItem.checkIcon",(LazyValue) t -> AquaImageFactory.getMenuItemCheckIcon(),
            "CheckBoxMenuItem.dashIcon",(LazyValue) t -> AquaImageFactory.getMenuItemDashIcon(),
            //"CheckBoxMenuItem.arrowIcon", null,

            "ColorChooser.background", panelBackgroundColor,

            // *** ComboBox
            "ComboBox.font", controlFont,
            "ComboBox.background", controlBackgroundColor, //menuBackgroundColor, // "menu" when it has no scrollbar, "listView" when it does
            "ComboBox.foreground", menuForegroundColor,
            "ComboBox.selectionBackground", menuSelectedBackgroundColor,
            "ComboBox.selectionForeground", menuSelectedForegroundColor,
            "ComboBox.disabledBackground", menuDisabledBackgroundColor,
            "ComboBox.disabledForeground", menuDisabledForegroundColor,
            "ComboBox.ancestorInputMap", aquaKeyBindings.getComboBoxInputMap(),

            "DesktopIcon.border", internalFrameBorder,
            "DesktopIcon.borderColor", smokyGlass,
            "DesktopIcon.borderRimColor", dockIconRim,
            "DesktopIcon.labelBackground", mediumTranslucentBlack,
            "Desktop.background", desktopBackgroundColor,

            "EditorPane.focusInputMap", aquaKeyBindings.getMultiLineTextInputMap(),
            "EditorPane.font", controlFont,
            "EditorPane.background", textBackground,
            "EditorPane.foreground", textForeground,
            "EditorPane.selectionBackground", textHighlight,
            "EditorPane.selectionForeground", textHighlightText,
            "EditorPane.caretForeground", textForeground,
            "EditorPane.caretBlinkRate", textCaretBlinkRate,
            "EditorPane.inactiveForeground", textInactiveText,
            "EditorPane.inactiveBackground", textInactiveBackground,
            "EditorPane.border", textAreaBorder,
            "EditorPane.margin", editorMargin,

            "FileChooser.newFolderIcon", AquaIcon.SystemIcon.getFolderIconUIResource(),
            "FileChooser.upFolderIcon", AquaIcon.SystemIcon.getFolderIconUIResource(),
            "FileChooser.homeFolderIcon", AquaIcon.SystemIcon.getDesktopIconUIResource(),
            "FileChooser.detailsViewIcon", AquaIcon.SystemIcon.getComputerIconUIResource(),
            "FileChooser.listViewIcon", AquaIcon.SystemIcon.getComputerIconUIResource(),

            "FileView.directoryIcon", AquaIcon.SystemIcon.getFolderIconUIResource(),
            "FileView.fileIcon", AquaIcon.SystemIcon.getDocumentIconUIResource(),
            "FileView.computerIcon", AquaIcon.SystemIcon.getDesktopIconUIResource(),
            "FileView.hardDriveIcon", AquaIcon.SystemIcon.getHardDriveIconUIResource(),
            "FileView.floppyDriveIcon", AquaIcon.SystemIcon.getFloppyIconUIResource(),

            // File View
            "FileChooser.cancelButtonMnemonic", zero,
            "FileChooser.saveButtonMnemonic", zero,
            "FileChooser.openButtonMnemonic", zero,
            "FileChooser.updateButtonMnemonic", zero,
            "FileChooser.helpButtonMnemonic", zero,
            "FileChooser.directoryOpenButtonMnemonic", zero,

            "FileChooser.lookInLabelMnemonic", zero,
            "FileChooser.fileNameLabelMnemonic", zero,
            "FileChooser.filesOfTypeLabelMnemonic", zero,

            "Focus.color", focusRingColor,

            "FormattedTextField.focusInputMap", aquaKeyBindings.getFormattedTextFieldInputMap(),
            "FormattedTextField.font", controlFont,
            "FormattedTextField.background", textBackground,
            "FormattedTextField.foreground", textForeground,
            "FormattedTextField.inactiveForeground", textInactiveText,
            "FormattedTextField.inactiveBackground", textInactiveBackground,
            "FormattedTextField.selectionBackground", textHighlight,
            "FormattedTextField.selectionForeground", textHighlightText,
            "FormattedTextField.caretForeground", textForeground,
            "FormattedTextField.caretBlinkRate", textCaretBlinkRate,
            "FormattedTextField.border", textFieldBorder,
            "FormattedTextField.margin", zeroInsets,

            "IconButton.font", controlSmallFont,

            "InternalFrame.titleFont", menuFont,
            "InternalFrame.background", windowBackgroundColor,
            "InternalFrame.borderColor", windowBackgroundColor,
            "InternalFrame.borderShadow", Color.red,
            "InternalFrame.borderDarkShadow", Color.green,
            "InternalFrame.borderHighlight", Color.blue,
            "InternalFrame.borderLight", Color.yellow,
            "InternalFrame.opaque", Boolean.FALSE,
            "InternalFrame.border", null, //internalFrameBorder,
            "InternalFrame.icon", null,

            "InternalFrame.paletteBorder", null,//internalFrameBorder,
            "InternalFrame.paletteTitleFont", menuFont,
            "InternalFrame.paletteBackground", windowBackgroundColor,

            "InternalFrame.optionDialogBorder", null,//internalFrameBorder,
            "InternalFrame.optionDialogTitleFont", menuFont,
            "InternalFrame.optionDialogBackground", windowBackgroundColor,

            /* Default frame icons are undefined for Basic. */

            "InternalFrame.closeIcon",(LazyValue) t -> AquaInternalFrameUI.exportCloseIcon(),
            "InternalFrame.maximizeIcon",(LazyValue) t -> AquaInternalFrameUI.exportZoomIcon(),
            "InternalFrame.iconifyIcon",(LazyValue) t -> AquaInternalFrameUI.exportMinimizeIcon(),
            "InternalFrame.minimizeIcon",(LazyValue) t -> AquaInternalFrameUI.exportMinimizeIcon(),
            // this could be either grow or icon
            // we decided to make it icon so that anyone who uses
            // these for ui will have different icons for max and min
            // these icons are pretty crappy to use in Mac OS X since
            // they really are interactive but we have to return a static
            // icon for now.

            // InternalFrame Auditory Cue Mappings
            "InternalFrame.closeSound", null,
            "InternalFrame.maximizeSound", null,
            "InternalFrame.minimizeSound", null,
            "InternalFrame.restoreDownSound", null,
            "InternalFrame.restoreUpSound", null,

            "InternalFrame.activeTitleBackground", windowBackgroundColor,
            "InternalFrame.activeTitleForeground", textForeground,
            "InternalFrame.inactiveTitleBackground", windowBackgroundColor,
            "InternalFrame.inactiveTitleForeground", textInactiveText,
            "InternalFrame.windowBindings", new Object[]{
                "shift ESCAPE", "showSystemMenu",
                "ctrl SPACE", "showSystemMenu",
                "ESCAPE", "hideSystemMenu"
            },

            // Radar [3543438]. We now define the TitledBorder properties for font and color.
            // Aqua HIG doesn't define TitledBorders as Swing does. Eventually, we might want to
            // re-think TitledBorder to behave more like a Box (NSBox). (vm)
            "TitledBorder.font", controlFont,
            "TitledBorder.titleColor", black,
        //    "TitledBorder.border", -- we inherit this property from BasicLookAndFeel (etched border)
            "TitledBorder.aquaVariant", aquaTitledBorder, // this is the border that matches what aqua really looks like
            "InsetBorder.aquaVariant", aquaInsetBorder, // this is the title-less variant

            // *** Label
            "Label.font", controlFont, // themeLabelFont is for small things like ToolbarButtons
            "Label.background", controlBackgroundColor,
            "Label.foreground", black,
            "Label.disabledForeground", disabled,
            "Label.disabledShadow", disabledShadow,
            "Label.opaque", useOpaqueComponents,
            "Label.border", null,

            "List.font", viewFont, // [3577901] Aqua HIG says "default font of text in lists and tables" should be 12 point (vm).
            "List.background", white,
            "List.foreground", black,
            "List.selectionBackground", selectionBackground,
            "List.selectionForeground", selectionForeground,
            "List.selectionInactiveBackground", selectionInactiveBackground,
            "List.selectionInactiveForeground", selectionInactiveForeground,
            "List.focusCellHighlightBorder", focusCellHighlightBorder,
            "List.border", null,
            "List.cellRenderer", listCellRendererActiveValue,

            "List.sourceListBackgroundPainter",(LazyValue) t -> AquaListUI.getSourceListBackgroundPainter(),
            "List.sourceListSelectionBackgroundPainter",(LazyValue) t -> AquaListUI.getSourceListSelectionBackgroundPainter(),
            "List.sourceListFocusedSelectionBackgroundPainter",(LazyValue) t -> AquaListUI.getSourceListFocusedSelectionBackgroundPainter(),
            "List.evenRowBackgroundPainter",(LazyValue) t -> AquaListUI.getListEvenBackgroundPainter(),
            "List.oddRowBackgroundPainter",(LazyValue) t -> AquaListUI.getListOddBackgroundPainter(),

            // <rdar://Problem/3743210> The modifier for the Mac is meta, not control.
            "List.focusInputMap", aquaKeyBindings.getListInputMap(),

            //"List.scrollPaneBorder", listBoxBorder, // Not used in Swing1.1
            //"ListItem.border", ThemeMenu.listItemBorder(), // for inset calculation

            // *** Menus
            "Menu.font", menuFont,
            "Menu.acceleratorFont", menuFont,
            "Menu.background", menuBackgroundColor,
            "Menu.foreground", menuForegroundColor,
            "Menu.selectionBackground", menuSelectedBackgroundColor,
            "Menu.selectionForeground", menuSelectedForegroundColor,
            "Menu.disabledBackground", menuDisabledBackgroundColor,
            "Menu.disabledForeground", menuDisabledForegroundColor,
            "Menu.acceleratorForeground", menuAccelForegroundColor,
            "Menu.acceleratorSelectionForeground", menuAccelSelectionForegroundColor,
            //"Menu.border", ThemeMenu.menuItemBorder(), // for inset calculation
            "Menu.border", menuBorder,
            "Menu.borderPainted", Boolean.FALSE,
            "Menu.margin", menuItemMargin,
            //"Menu.checkIcon", emptyCheckIcon, // A non-drawing GlyphIcon to make the spacing consistent
            "Menu.arrowIcon",(LazyValue) t -> AquaImageFactory.getMenuArrowIcon(),
            "Menu.consumesTabs", Boolean.TRUE,
            "Menu.menuPopupOffsetY", Integer.valueOf(1),
            "Menu.submenuPopupOffsetY", Integer.valueOf(-4),

            "MenuBar.font", menuFont,
            "MenuBar.background", menuBackgroundColor, // not a menu item, not selected
            "MenuBar.foreground", menuForegroundColor,
            "MenuBar.border", new AquaMenuBarBorder(), // sja make lazy!
            "MenuBar.margin", new InsetsUIResource(0, 8, 0, 8), // sja make lazy!
            "MenuBar.selectionBackground", menuSelectedBackgroundColor, // not a menu item, is selected
            "MenuBar.selectionForeground", menuSelectedForegroundColor,
            "MenuBar.disabledBackground", menuDisabledBackgroundColor, //ThemeBrush.GetThemeBrushForMenu(false, false), // not a menu item, not selected
            "MenuBar.disabledForeground", menuDisabledForegroundColor,
            "MenuBar.backgroundPainter",(LazyValue) t -> AquaMenuPainter.getMenuBarPainter(),
            "MenuBar.selectedBackgroundPainter",(LazyValue) t -> AquaMenuPainter.getSelectedMenuBarItemPainter(),

            "MenuItem.font", menuFont,
            "MenuItem.acceleratorFont", menuFont,
            "MenuItem.background", menuBackgroundColor,
            "MenuItem.foreground", menuForegroundColor,
            "MenuItem.selectionBackground", menuSelectedBackgroundColor,
            "MenuItem.selectionForeground", menuSelectedForegroundColor,
            "MenuItem.disabledBackground", menuDisabledBackgroundColor,
            "MenuItem.disabledForeground", menuDisabledForegroundColor,
            "MenuItem.acceleratorForeground", menuAccelForegroundColor,
            "MenuItem.acceleratorSelectionForeground", menuAccelSelectionForegroundColor,
            "MenuItem.acceleratorDelimiter", "",
            "MenuItem.border", menuBorder,
            "MenuItem.margin", menuItemMargin,
            "MenuItem.borderPainted", Boolean.TRUE,
            //"MenuItem.checkIcon", emptyCheckIcon, // A non-drawing GlyphIcon to make the spacing consistent
            //"MenuItem.arrowIcon", null,
            "MenuItem.selectedBackgroundPainter",(LazyValue) t -> AquaMenuPainter.getSelectedMenuItemPainter(),

            // *** OptionPane
            // You can additionaly define OptionPane.messageFont which will
            // dictate the fonts used for the message, and
            // OptionPane.buttonFont, which defines the font for the buttons.
            "OptionPane.font", alertHeaderFont,
            "OptionPane.messageFont", controlFont,
            "OptionPane.buttonFont", controlFont,
            "OptionPane.background", windowBackgroundColor,
            "OptionPane.foreground", black,
            "OptionPane.messageForeground", black,
            "OptionPane.border", new BorderUIResource.EmptyBorderUIResource(12, 21, 17, 21),
            "OptionPane.messageAreaBorder", zeroBorder,
            "OptionPane.buttonAreaBorder", new BorderUIResource.EmptyBorderUIResource(13, 0, 0, 0),
            "OptionPane.minimumSize", new DimensionUIResource(262, 90),

            "OptionPane.errorIcon", stopIcon,
            "OptionPane.informationIcon", confirmIcon,
            "OptionPane.warningIcon", cautionIcon,
            "OptionPane.questionIcon", confirmIcon,
            "_SecurityDecisionIcon", securityIcon,
            "OptionPane.windowBindings", new Object[]{"ESCAPE", "close"},
            // OptionPane Auditory Cue Mappings
            "OptionPane.errorSound", null,
            "OptionPane.informationSound", null, // Info and Plain
            "OptionPane.questionSound", null,
            "OptionPane.warningSound", null,
            "OptionPane.buttonClickThreshhold", Integer.valueOf(500),
            "OptionPane.yesButtonMnemonic", "",
            "OptionPane.noButtonMnemonic", "",
            "OptionPane.okButtonMnemonic", "",
            "OptionPane.cancelButtonMnemonic", "",

            "Panel.font", controlFont,
            "Panel.background", panelBackgroundColor, //new ColorUIResource(0.5647f, 0.9957f, 0.5059f),
            "Panel.foreground", black,
            "Panel.opaque", useOpaqueComponents,

            "PasswordField.focusInputMap", aquaKeyBindings.getPasswordFieldInputMap(),
            "PasswordField.font", controlFont,
            "PasswordField.background", textBackground,
            "PasswordField.foreground", textForeground,
            "PasswordField.inactiveForeground", textInactiveText,
            "PasswordField.inactiveBackground", textInactiveBackground,
            "PasswordField.selectionBackground", textHighlight,
            "PasswordField.selectionForeground", textHighlightText,
            "PasswordField.caretForeground", textForeground,
            "PasswordField.caretBlinkRate", textCaretBlinkRate,
            "PasswordField.border", textFieldBorder,
            "PasswordField.margin", zeroInsets,
            "PasswordField.echoChar", Character.valueOf((char)0x25CF),
            "PasswordField.capsLockIconColor", textPasswordFieldCapsLockIconColor,

            "PopupMenu.font", menuFont,
            "PopupMenu.background", menuBackgroundColor,
            // Fix for 7154516: make popups opaque
            "PopupMenu.translucentBackground", white,
            "PopupMenu.foreground", menuForegroundColor,
            "PopupMenu.selectionBackground", menuSelectedBackgroundColor,
            "PopupMenu.selectionForeground", menuSelectedForegroundColor,
            "PopupMenu.border", menuBorder,
//            "PopupMenu.margin",

            "ProgressBar.font", controlFont,
            "ProgressBar.foreground", black,
            "ProgressBar.background", controlBackgroundColor,
            "ProgressBar.selectionForeground", black,
            "ProgressBar.selectionBackground", white,
            "ProgressBar.border", new BorderUIResource(BorderFactory.createEmptyBorder()),
            "ProgressBar.repaintInterval", Integer.valueOf(20),

            "RadioButton.background", controlBackgroundColor,
            "RadioButton.foreground", black,
            "RadioButton.disabledText", disabled,
            "RadioButton.select", selected,
            "RadioButton.icon",(LazyValue) t -> AquaButtonRadioUI.getSizingRadioButtonIcon(),
            "RadioButton.font", controlFont,
            "RadioButton.border", AquaButtonBorder.getBevelButtonBorder(),
            "RadioButton.margin", new InsetsUIResource(1, 1, 0, 1),
            "RadioButton.focusInputMap", controlFocusInputMap,

            "RadioButtonMenuItem.font", menuFont,
            "RadioButtonMenuItem.acceleratorFont", menuFont,
            "RadioButtonMenuItem.background", menuBackgroundColor,
            "RadioButtonMenuItem.foreground", menuForegroundColor,
            "RadioButtonMenuItem.selectionBackground", menuSelectedBackgroundColor,
            "RadioButtonMenuItem.selectionForeground", menuSelectedForegroundColor,
            "RadioButtonMenuItem.disabledBackground", menuDisabledBackgroundColor,
            "RadioButtonMenuItem.disabledForeground", menuDisabledForegroundColor,
            "RadioButtonMenuItem.acceleratorForeground", menuAccelForegroundColor,
            "RadioButtonMenuItem.acceleratorSelectionForeground", menuAccelSelectionForegroundColor,
            "RadioButtonMenuItem.acceleratorDelimiter", "",
            "RadioButtonMenuItem.border", menuBorder, // for inset calculation
            "RadioButtonMenuItem.margin", menuItemMargin,
            "RadioButtonMenuItem.borderPainted", Boolean.TRUE,
            "RadioButtonMenuItem.checkIcon",(LazyValue) t -> AquaImageFactory.getMenuItemCheckIcon(),
            "RadioButtonMenuItem.dashIcon",(LazyValue) t -> AquaImageFactory.getMenuItemDashIcon(),
            //"RadioButtonMenuItem.arrowIcon", null,

            "Separator.background", null,
            "Separator.foreground", new ColorUIResource(0xD4, 0xD4, 0xD4),

            "ScrollBar.border", null,
            "ScrollBar.focusInputMap", aquaKeyBindings.getScrollBarInputMap(),
            "ScrollBar.focusInputMap.RightToLeft", aquaKeyBindings.getScrollBarRightToLeftInputMap(),
            "ScrollBar.width", Integer.valueOf(16),
            "ScrollBar.background", white,
            "ScrollBar.foreground", black,

            "ScrollPane.font", controlFont,
            "ScrollPane.background", white,
            "ScrollPane.foreground", black, //$
            "ScrollPane.border", scollListBorder,
            "ScrollPane.viewportBorder", null,

            "ScrollPane.ancestorInputMap", aquaKeyBindings.getScrollPaneInputMap(),
            "ScrollPane.ancestorInputMap.RightToLeft", new UIDefaults.LazyInputMap(new Object[]{}),

            "Viewport.font", controlFont,
            "Viewport.background", white, // The background for tables, lists, etc
            "Viewport.foreground", black,

            // *** Slider
            "Slider.foreground", black, "Slider.background", controlBackgroundColor,
            "Slider.font", controlSmallFont,
            //"Slider.highlight", table.get("controlLtHighlight"),
            //"Slider.shadow", table.get("controlShadow"),
            //"Slider.focus", table.get("controlDkShadow"),
            "Slider.tickColor", new ColorUIResource(Color.GRAY),
            "Slider.border", null,
            "Slider.focusInsets", new InsetsUIResource(2, 2, 2, 2),
            "Slider.focusInputMap", aquaKeyBindings.getSliderInputMap(),
            "Slider.focusInputMap.RightToLeft", aquaKeyBindings.getSliderRightToLeftInputMap(),

            // *** Spinner
            "Spinner.font", controlFont,
            "Spinner.background", controlBackgroundColor,
            "Spinner.foreground", black,
            "Spinner.border", null,
            "Spinner.arrowButtonSize", new Dimension(16, 5),
            "Spinner.ancestorInputMap", aquaKeyBindings.getSpinnerInputMap(),
            "Spinner.editorBorderPainted", Boolean.TRUE,
            "Spinner.editorAlignment", SwingConstants.TRAILING,

            // *** SplitPane
            //"SplitPane.highlight", table.get("controlLtHighlight"),
            //"SplitPane.shadow", table.get("controlShadow"),
            "SplitPane.background", panelBackgroundColor,
            "SplitPane.border", scollListBorder,
            "SplitPane.dividerSize", Integer.valueOf(9), //$
            "SplitPaneDivider.border", null, // AquaSplitPaneDividerUI draws it
            "SplitPaneDivider.horizontalGradientVariant",(LazyValue) t -> AquaSplitPaneDividerUI.getHorizontalSplitDividerGradientVariant(),

            // *** TabbedPane
            "TabbedPane.font", controlFont,
            "TabbedPane.smallFont", controlSmallFont,
            "TabbedPane.useSmallLayout", Boolean.FALSE,//sSmallTabs ? Boolean.TRUE : Boolean.FALSE,
            "TabbedPane.background", tabBackgroundColor, // for bug [3398277] use a background color so that
            // tabs on a custom pane get erased when they are removed.
            "TabbedPane.foreground", black, //ThemeTextColor.GetThemeTextColor(AppearanceConstants.kThemeTextColorTabFrontActive),
            //"TabbedPane.lightHighlight", table.get("controlLtHighlight"),
            //"TabbedPane.highlight", table.get("controlHighlight"),
            //"TabbedPane.shadow", table.get("controlShadow"),
            //"TabbedPane.darkShadow", table.get("controlDkShadow"),
            //"TabbedPane.focus", table.get("controlText"),
            "TabbedPane.opaque", useOpaqueComponents,
            "TabbedPane.textIconGap", Integer.valueOf(4),
            "TabbedPane.tabInsets", new InsetsUIResource(0, 10, 3, 10), // Label within tab (top, left, bottom, right)
            //"TabbedPane.rightTabInsets", new InsetsUIResource(0, 10, 3, 10), // Label within tab (top, left, bottom, right)
            "TabbedPane.leftTabInsets", new InsetsUIResource(0, 10, 3, 10), // Label within tab
            "TabbedPane.rightTabInsets", new InsetsUIResource(0, 10, 3, 10), // Label within tab
            //"TabbedPane.tabAreaInsets", new InsetsUIResource(3, 9, -1, 9), // Tabs relative to edge of pane (negative value for overlapping)
            "TabbedPane.tabAreaInsets", new InsetsUIResource(3, 9, -1, 9), // Tabs relative to edge of pane (negative value for overlapping)
            // (top = side opposite pane, left = edge || to pane, bottom = side adjacent to pane, right = left) - see rotateInsets
            "TabbedPane.contentBorderInsets", new InsetsUIResource(8, 0, 0, 0), // width of border
            //"TabbedPane.selectedTabPadInsets", new InsetsUIResource(0, 0, 1, 0), // Really outsets, this is where we allow for overlap
            "TabbedPane.selectedTabPadInsets", new InsetsUIResource(0, 0, 0, 0), // Really outsets, this is where we allow for overlap
            "TabbedPane.tabsOverlapBorder", Boolean.TRUE,
            "TabbedPane.selectedTabTitlePressedColor", selectedTabTitlePressedColor,
            "TabbedPane.selectedTabTitleDisabledColor", selectedTabTitleDisabledColor,
            "TabbedPane.selectedTabTitleNormalColor", JRSUIUtils.isMacOSXBigSurOrAbove() ? selectedControlTextColor : selectedTabTitleNormalColor,
            "TabbedPane.selectedTabTitleShadowDisabledColor", selectedTabTitleShadowDisabledColor,
            "TabbedPane.selectedTabTitleShadowNormalColor", selectedTabTitleShadowNormalColor,
            "TabbedPane.nonSelectedTabTitleNormalColor", nonSelectedTabTitleNormalColor,

            // *** Table
            "Table.font", viewFont, // [3577901] Aqua HIG says "default font of text in lists and tables" should be 12 point (vm).
            "Table.foreground", black, // cell text color
            "Table.background", white, // cell background color
            "Table.selectionForeground", selectionForeground,
            "Table.selectionBackground", selectionBackground,
            "Table.selectionInactiveBackground", selectionInactiveBackground,
            "Table.selectionInactiveForeground", selectionInactiveForeground,
            "Table.gridColor", white, // grid line color
            "Table.focusCellBackground", textHighlightText,
            "Table.focusCellForeground", textHighlight,
            "Table.focusCellHighlightBorder", focusCellHighlightBorder,
            "Table.scrollPaneBorder", scollListBorder,

            "Table.ancestorInputMap", aquaKeyBindings.getTableInputMap(),
            "Table.ancestorInputMap.RightToLeft", aquaKeyBindings.getTableRightToLeftInputMap(),

            "TableHeader.font", controlSmallFont,
            "TableHeader.foreground", black,
            "TableHeader.background", white, // header background
            "TableHeader.cellBorder", listHeaderBorder,

            // *** Text
            "TextArea.focusInputMap", aquaKeyBindings.getMultiLineTextInputMap(),
            "TextArea.font", controlFont,
            "TextArea.background", textBackground,
            "TextArea.foreground", textForeground,
            "TextArea.inactiveForeground", textInactiveText,
            "TextArea.inactiveBackground", textInactiveBackground,
            "TextArea.selectionBackground", textHighlight,
            "TextArea.selectionForeground", textHighlightText,
            "TextArea.caretForeground", textForeground,
            "TextArea.caretBlinkRate", textCaretBlinkRate,
            "TextArea.border", textAreaBorder,
            "TextArea.margin", zeroInsets,

            "TextComponent.selectionBackgroundInactive", textHighlightInactive,

            "TextField.focusInputMap", aquaKeyBindings.getTextFieldInputMap(),
            "TextField.font", controlFont,
            "TextField.background", textBackground,
            "TextField.foreground", textForeground,
            "TextField.inactiveForeground", textInactiveText,
            "TextField.inactiveBackground", textInactiveBackground,
            "TextField.selectionBackground", textHighlight,
            "TextField.selectionForeground", textHighlightText,
            "TextField.caretForeground", textForeground,
            "TextField.caretBlinkRate", textCaretBlinkRate,
            "TextField.border", textFieldBorder,
            "TextField.margin", zeroInsets,

            "TextPane.focusInputMap", aquaKeyBindings.getMultiLineTextInputMap(),
            "TextPane.font", controlFont,
            "TextPane.background", textBackground,
            "TextPane.foreground", textForeground,
            "TextPane.selectionBackground", textHighlight,
            "TextPane.selectionForeground", textHighlightText,
            "TextPane.caretForeground", textForeground,
            "TextPane.caretBlinkRate", textCaretBlinkRate,
            "TextPane.inactiveForeground", textInactiveText,
            "TextPane.inactiveBackground", textInactiveBackground,
            "TextPane.border", textAreaBorder,
            "TextPane.margin", editorMargin,

            // *** ToggleButton
            "ToggleButton.background", controlBackgroundColor,
            "ToggleButton.foreground", black,
            "ToggleButton.disabledText", disabled,
            // we need to go through and find out if these are used, and if not what to set
            // so that subclasses will get good aqua like colors.
            //    "ToggleButton.select", getControlShadow(),
            //    "ToggleButton.text", getControl(),
            //    "ToggleButton.disabledSelectedText", getControlDarkShadow(),
            //    "ToggleButton.disabledBackground", getControl(),
            //    "ToggleButton.disabledSelectedBackground", getControlShadow(),
            //"ToggleButton.focus", getFocusColor(),
            "ToggleButton.border",(LazyValue) t -> AquaButtonBorder.getDynamicButtonBorder(), // sja make this lazy!
            "ToggleButton.font", controlFont,
            "ToggleButton.focusInputMap", controlFocusInputMap,
            "ToggleButton.margin", new InsetsUIResource(2, 2, 2, 2),

            // *** ToolBar
            "ToolBar.font", controlFont,
            "ToolBar.background", panelBackgroundColor,
            "ToolBar.foreground", new ColorUIResource(Color.gray),
            "ToolBar.dockingBackground", panelBackgroundColor,
            "ToolBar.dockingForeground", selectionBackground,
            "ToolBar.floatingBackground", panelBackgroundColor,
            "ToolBar.floatingForeground", new ColorUIResource(Color.darkGray),
            "ToolBar.border",(LazyValue) t -> AquaToolBarUI.getToolBarBorder(),
            "ToolBar.borderHandleColor", toolbarDragHandleColor,
            //"ToolBar.separatorSize", new DimensionUIResource( 10, 10 ),
            "ToolBar.separatorSize", null,

            // *** ToolBarButton
            "ToolBarButton.margin", new InsetsUIResource(3, 3, 3, 3),
            "ToolBarButton.insets", new InsetsUIResource(1, 1, 1, 1),

            // *** ToolTips
            "ToolTip.font", controlSmallFont,
            //$ Tooltips - Same color as help balloons?
            "ToolTip.background", toolTipBackground,
            "ToolTip.foreground", black,
            "ToolTip.border", toolTipBorder,

            // *** Tree
            "Tree.font", viewFont, // [3577901] Aqua HIG says "default font of text in lists and tables" should be 12 point (vm).
            "Tree.background", white,
            "Tree.foreground", black,
            // for now no lines
            "Tree.hash", white, //disabled, // Line color
            "Tree.line", white, //disabled, // Line color
            "Tree.textForeground", black,
            "Tree.textBackground", white,
            "Tree.selectionForeground", selectionForeground,
            "Tree.selectionBackground", selectionBackground,
            "Tree.selectionInactiveBackground", selectionInactiveBackground,
            "Tree.selectionInactiveForeground", selectionInactiveForeground,
            "Tree.selectionBorderColor", selectionBackground, // match the background so it looks like we don't draw anything
            "Tree.editorBorderSelectionColor", null, // The EditTextFrame provides its own border
            // "Tree.editorBorder", textFieldBorder, // If you still have Sun bug 4376328 in DefaultTreeCellEditor, it has to have the same insets as TextField.border
            "Tree.leftChildIndent", Integer.valueOf(7),//$
            "Tree.rightChildIndent", Integer.valueOf(13),//$
            "Tree.rowHeight", Integer.valueOf(19),// iconHeight + 3, to match finder - a zero would have the renderer decide, except that leaves the icons touching
            "Tree.scrollsOnExpand", Boolean.FALSE,
            "Tree.openIcon",(LazyValue) t -> AquaImageFactory.getTreeOpenFolderIcon(), // Open folder icon
            "Tree.closedIcon",(LazyValue) t -> AquaImageFactory.getTreeFolderIcon(), // Closed folder icon
            "Tree.leafIcon",(LazyValue) t -> AquaImageFactory.getTreeDocumentIcon(), // Document icon
            "Tree.expandedIcon",(LazyValue) t -> AquaImageFactory.getTreeExpandedIcon(),
            "Tree.collapsedIcon",(LazyValue) t -> AquaImageFactory.getTreeCollapsedIcon(),
            "Tree.rightToLeftCollapsedIcon",(LazyValue) t -> AquaImageFactory.getTreeRightToLeftCollapsedIcon(),
            "Tree.changeSelectionWithFocus", Boolean.TRUE,
            "Tree.drawsFocusBorderAroundIcon", Boolean.FALSE,

            "Tree.focusInputMap", aquaKeyBindings.getTreeInputMap(),
            "Tree.focusInputMap.RightToLeft", aquaKeyBindings.getTreeRightToLeftInputMap(),
            "Tree.ancestorInputMap", new UIDefaults.LazyInputMap(new Object[]{"ESCAPE", "cancel"}),};

        table.putDefaults(defaults);
        SwingUtilities2.putAATextInfo(true, table);
    }

    protected void initSystemColorDefaults(final UIDefaults table) {
//        String[] defaultSystemColors = {
//                  "desktop", "#005C5C", /* Color of the desktop background */
//          "activeCaption", "#000080", /* Color for captions (title bars) when they are active. */
//          "activeCaptionText", "#FFFFFF", /* Text color for text in captions (title bars). */
//        "activeCaptionBorder", "#C0C0C0", /* Border color for caption (title bar) window borders. */
//            "inactiveCaption", "#808080", /* Color for captions (title bars) when not active. */
//        "inactiveCaptionText", "#C0C0C0", /* Text color for text in inactive captions (title bars). */
//      "inactiveCaptionBorder", "#C0C0C0", /* Border color for inactive caption (title bar) window borders. */
//                 "window", "#FFFFFF", /* Default color for the interior of windows */
//           "windowBorder", "#000000", /* ??? */
//             "windowText", "#000000", /* ??? */
//               "menu", "#C0C0C0", /* Background color for menus */
//               "menuText", "#000000", /* Text color for menus  */
//               "text", "#C0C0C0", /* Text background color */
//               "textText", "#000000", /* Text foreground color */
//          "textHighlight", "#000080", /* Text background color when selected */
//          "textHighlightText", "#FFFFFF", /* Text color when selected */
//           "textInactiveText", "#808080", /* Text color when disabled */
//                "control", "#C0C0C0", /* Default color for controls (buttons, sliders, etc) */
//            "controlText", "#000000", /* Default color for text in controls */
//           "controlHighlight", "#C0C0C0", /* Specular highlight (opposite of the shadow) */
//         "controlLtHighlight", "#FFFFFF", /* Highlight color for controls */
//          "controlShadow", "#808080", /* Shadow color for controls */
//            "controlDkShadow", "#000000", /* Dark shadow color for controls */
//              "scrollbar", "#E0E0E0", /* Scrollbar background (usually the "track") */
//               "info", "#FFFFE1", /* ??? */
//               "infoText", "#000000"  /* ??? */
//        };
//
//        loadSystemColors(table, defaultSystemColors, isNativeLookAndFeel());
    }

    /**
     * Initialize the uiClassID to AquaComponentUI mapping.
     * The JComponent classes define their own uiClassID constants
     * (see AbstractComponent.getUIClassID).  This table must
     * map those constants to a BasicComponentUI class of the
     * appropriate type.
     *
     * @see #getDefaults
     */
    protected void initClassDefaults(final UIDefaults table) {
        final String basicPackageName = "javax.swing.plaf.basic.";

        final Object[] uiDefaults = {
            "ButtonUI", PKG_PREFIX + "AquaButtonUI",
            "CheckBoxUI", PKG_PREFIX + "AquaButtonCheckBoxUI",
            "CheckBoxMenuItemUI", PKG_PREFIX + "AquaMenuItemUI",
            "LabelUI", PKG_PREFIX + "AquaLabelUI",
            "ListUI", PKG_PREFIX + "AquaListUI",
            "MenuUI", PKG_PREFIX + "AquaMenuUI",
            "MenuItemUI", PKG_PREFIX + "AquaMenuItemUI",
            "OptionPaneUI", PKG_PREFIX + "AquaOptionPaneUI",
            "PanelUI", PKG_PREFIX + "AquaPanelUI",
            "RadioButtonMenuItemUI", PKG_PREFIX + "AquaMenuItemUI",
            "RadioButtonUI", PKG_PREFIX + "AquaButtonRadioUI",
            "ProgressBarUI", PKG_PREFIX + "AquaProgressBarUI",
            "RootPaneUI", PKG_PREFIX + "AquaRootPaneUI",
            "SliderUI", PKG_PREFIX + "AquaSliderUI",
            "ScrollBarUI", PKG_PREFIX + "AquaScrollBarUI",
            "TabbedPaneUI", PKG_PREFIX + (JRSUIUtils.TabbedPane.shouldUseTabbedPaneContrastUI() ? "AquaTabbedPaneContrastUI" : "AquaTabbedPaneUI"),
            "TableUI", PKG_PREFIX + "AquaTableUI",
            "ToggleButtonUI", PKG_PREFIX + "AquaButtonToggleUI",
            "ToolBarUI", PKG_PREFIX + "AquaToolBarUI",
            "ToolTipUI", PKG_PREFIX + "AquaToolTipUI",
            "TreeUI", PKG_PREFIX + "AquaTreeUI",

            "InternalFrameUI", PKG_PREFIX + "AquaInternalFrameUI",
            "DesktopIconUI", PKG_PREFIX + "AquaInternalFrameDockIconUI",
            "DesktopPaneUI", PKG_PREFIX + "AquaInternalFramePaneUI",
            "EditorPaneUI", PKG_PREFIX + "AquaEditorPaneUI",
            "TextFieldUI", PKG_PREFIX + "AquaTextFieldUI",
            "TextPaneUI", PKG_PREFIX + "AquaTextPaneUI",
            "ComboBoxUI", PKG_PREFIX + "AquaComboBoxUI",
            "PopupMenuUI", PKG_PREFIX + "AquaPopupMenuUI",
            "TextAreaUI", PKG_PREFIX + "AquaTextAreaUI",
            "MenuBarUI", PKG_PREFIX + "AquaMenuBarUI",
            "FileChooserUI", PKG_PREFIX + "AquaFileChooserUI",
            "PasswordFieldUI", PKG_PREFIX + "AquaTextPasswordFieldUI",
            "TableHeaderUI", PKG_PREFIX + "AquaTableHeaderUI",

            "FormattedTextFieldUI", PKG_PREFIX + "AquaTextFieldFormattedUI",

            "SpinnerUI", PKG_PREFIX + "AquaSpinnerUI",
            "SplitPaneUI", PKG_PREFIX + "AquaSplitPaneUI",
            "ScrollPaneUI", PKG_PREFIX + "AquaScrollPaneUI",

            "PopupMenuSeparatorUI", PKG_PREFIX + "AquaPopupMenuSeparatorUI",
            "SeparatorUI", PKG_PREFIX + "AquaPopupMenuSeparatorUI",
            "ToolBarSeparatorUI", PKG_PREFIX + "AquaToolBarSeparatorUI",

            // as we implement aqua versions of the swing elements
            // we will aad the com.apple.laf.FooUI classes to this table.

            "ColorChooserUI", basicPackageName + "BasicColorChooserUI",

            // text UIs
            "ViewportUI", basicPackageName + "BasicViewportUI",
        };
        table.putDefaults(uiDefaults);
    }
}
