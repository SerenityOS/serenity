/*
 * Copyright (c) 2003, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.plaf.metal;

import java.awt.*;
import java.net.URL;
import java.util.*;
import javax.swing.*;
import javax.swing.plaf.*;
import sun.swing.SwingUtilities2;
import sun.swing.PrintColorUIResource;

/**
 * The default theme for the {@code MetalLookAndFeel}.
 * <p>
 * The designers
 * of the Metal Look and Feel strive to keep the default look up to
 * date, possibly through the use of new themes in the future.
 * Therefore, developers should only use this class directly when they
 * wish to customize the "Ocean" look, or force it to be the current
 * theme, regardless of future updates.

 * <p>
 * All colors returned by {@code OceanTheme} are completely
 * opaque.
 *
 * @since 1.5
 * @see MetalLookAndFeel#setCurrentTheme
 */
@SuppressWarnings("serial") // Superclass is not serializable across versions
public class OceanTheme extends DefaultMetalTheme {
    private static final ColorUIResource PRIMARY1 =
                              new ColorUIResource(0x6382BF);
    private static final ColorUIResource PRIMARY2 =
                              new ColorUIResource(0xA3B8CC);
    private static final ColorUIResource PRIMARY3 =
                              new ColorUIResource(0xB8CFE5);
    private static final ColorUIResource SECONDARY1 =
                              new ColorUIResource(0x7A8A99);
    private static final ColorUIResource SECONDARY2 =
                              new ColorUIResource(0xB8CFE5);
    private static final ColorUIResource SECONDARY3 =
                              new ColorUIResource(0xEEEEEE);

    private static final ColorUIResource CONTROL_TEXT_COLOR =
                              new PrintColorUIResource(0x333333, Color.BLACK);
    private static final ColorUIResource INACTIVE_CONTROL_TEXT_COLOR =
                              new ColorUIResource(0x999999);
    private static final ColorUIResource MENU_DISABLED_FOREGROUND =
                              new ColorUIResource(0x999999);
    private static final ColorUIResource OCEAN_BLACK =
                              new PrintColorUIResource(0x333333, Color.BLACK);

    private static final ColorUIResource OCEAN_DROP =
                              new ColorUIResource(0xD2E9FF);

    // ComponentOrientation Icon
    // Delegates to different icons based on component orientation
    private static class COIcon extends IconUIResource {
        private Icon rtl;

        public COIcon(Icon ltr, Icon rtl) {
            super(ltr);
            this.rtl = rtl;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (MetalUtils.isLeftToRight(c)) {
                super.paintIcon(c, g, x, y);
            } else {
                rtl.paintIcon(c, g, x, y);
            }
        }
    }

    // InternalFrame Icon
    // Delegates to different icons based on button state
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private static class IFIcon extends IconUIResource {
        private Icon pressed;

        public IFIcon(Icon normal, Icon pressed) {
            super(normal);
            this.pressed = pressed;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            ButtonModel model = ((AbstractButton)c).getModel();
            if (model.isPressed() && model.isArmed()) {
                pressed.paintIcon(c, g, x, y);
            } else {
                super.paintIcon(c, g, x, y);
            }
        }
    }

    /**
     * Creates an instance of <code>OceanTheme</code>
     */
    public OceanTheme() {
    }

    /**
     * Add this theme's custom entries to the defaults table.
     *
     * @param table the defaults table, non-null
     * @throws NullPointerException if {@code table} is {@code null}
     */
    public void addCustomEntriesToTable(UIDefaults table) {
        UIDefaults.LazyValue focusBorder = t ->
            new BorderUIResource.LineBorderUIResource(getPrimary1());
        // .30 0 DDE8F3 white secondary2
        java.util.List<?> buttonGradient = Arrays.asList(
                 new Object[] {Float.valueOf(.3f), Float.valueOf(0f),
                 new ColorUIResource(0xDDE8F3), getWhite(), getSecondary2() });

        // Other possible properties that aren't defined:
        //
        // Used when generating the disabled Icons, provides the region to
        // constrain grays to.
        // Button.disabledGrayRange -> Object[] of Integers giving min/max
        // InternalFrame.inactiveTitleGradient -> Gradient when the
        //   internal frame is inactive.
        Color cccccc = new ColorUIResource(0xCCCCCC);
        Color dadada = new ColorUIResource(0xDADADA);
        Color c8ddf2 = new ColorUIResource(0xC8DDF2);
        Object directoryIcon = getIconResource("icons/ocean/directory.gif");
        Object fileIcon = getIconResource("icons/ocean/file.gif");
        java.util.List<?> sliderGradient = Arrays.asList(new Object[] {
            Float.valueOf(.3f), Float.valueOf(.2f),
            c8ddf2, getWhite(), new ColorUIResource(SECONDARY2) });

        Object[] defaults = new Object[] {
            "Button.gradient", buttonGradient,
            "Button.rollover", Boolean.TRUE,
            "Button.toolBarBorderBackground", INACTIVE_CONTROL_TEXT_COLOR,
            "Button.disabledToolBarBorderBackground", cccccc,
            "Button.rolloverIconType", "ocean",

            "CheckBox.rollover", Boolean.TRUE,
            "CheckBox.gradient", buttonGradient,

            "CheckBoxMenuItem.gradient", buttonGradient,

            // home2
            "FileChooser.homeFolderIcon",
                 getIconResource("icons/ocean/homeFolder.gif"),
            // directory2
            "FileChooser.newFolderIcon",
                 getIconResource("icons/ocean/newFolder.gif"),
            // updir2
            "FileChooser.upFolderIcon",
                 getIconResource("icons/ocean/upFolder.gif"),

            // computer2
            "FileView.computerIcon",
                 getIconResource("icons/ocean/computer.gif"),
            "FileView.directoryIcon", directoryIcon,
            // disk2
            "FileView.hardDriveIcon",
                 getIconResource("icons/ocean/hardDrive.gif"),
            "FileView.fileIcon", fileIcon,
            // floppy2
            "FileView.floppyDriveIcon",
                 getIconResource("icons/ocean/floppy.gif"),

            "Label.disabledForeground", getInactiveControlTextColor(),

            "Menu.opaque", Boolean.FALSE,

            "MenuBar.gradient", Arrays.asList(new Object[] {
                     Float.valueOf(1f), Float.valueOf(0f),
                     getWhite(), dadada,
                     new ColorUIResource(dadada) }),
            "MenuBar.borderColor", cccccc,

            "InternalFrame.activeTitleGradient", buttonGradient,
            // close2
            "InternalFrame.closeIcon",
                     new UIDefaults.LazyValue() {
                         public Object createValue(UIDefaults table) {
                             return new IFIcon(getHastenedIcon("icons/ocean/close.gif", table),
                                               getHastenedIcon("icons/ocean/close-pressed.gif", table));
                         }
                     },
            // minimize
            "InternalFrame.iconifyIcon",
                     new UIDefaults.LazyValue() {
                         public Object createValue(UIDefaults table) {
                             return new IFIcon(getHastenedIcon("icons/ocean/iconify.gif", table),
                                               getHastenedIcon("icons/ocean/iconify-pressed.gif", table));
                         }
                     },
            // restore
            "InternalFrame.minimizeIcon",
                     new UIDefaults.LazyValue() {
                         public Object createValue(UIDefaults table) {
                             return new IFIcon(getHastenedIcon("icons/ocean/minimize.gif", table),
                                               getHastenedIcon("icons/ocean/minimize-pressed.gif", table));
                         }
                     },
            // menubutton3
            "InternalFrame.icon",
                     getIconResource("icons/ocean/menu.gif"),
            // maximize2
            "InternalFrame.maximizeIcon",
                     new UIDefaults.LazyValue() {
                         public Object createValue(UIDefaults table) {
                             return new IFIcon(getHastenedIcon("icons/ocean/maximize.gif", table),
                                               getHastenedIcon("icons/ocean/maximize-pressed.gif", table));
                         }
                     },
            // paletteclose
            "InternalFrame.paletteCloseIcon",
                     new UIDefaults.LazyValue() {
                         public Object createValue(UIDefaults table) {
                             return new IFIcon(getHastenedIcon("icons/ocean/paletteClose.gif", table),
                                               getHastenedIcon("icons/ocean/paletteClose-pressed.gif", table));
                         }
                     },

            "List.focusCellHighlightBorder", focusBorder,

            "MenuBarUI", "javax.swing.plaf.metal.MetalMenuBarUI",

            "OptionPane.errorIcon",
                   getIconResource("icons/ocean/error.png"),
            "OptionPane.informationIcon",
                   getIconResource("icons/ocean/info.png"),
            "OptionPane.questionIcon",
                   getIconResource("icons/ocean/question.png"),
            "OptionPane.warningIcon",
                   getIconResource("icons/ocean/warning.png"),

            "RadioButton.gradient", buttonGradient,
            "RadioButton.rollover", Boolean.TRUE,

            "RadioButtonMenuItem.gradient", buttonGradient,

            "ScrollBar.gradient", buttonGradient,

            "Slider.altTrackColor", new ColorUIResource(0xD2E2EF),
            "Slider.gradient", sliderGradient,
            "Slider.focusGradient", sliderGradient,

            "SplitPane.oneTouchButtonsOpaque", Boolean.FALSE,
            "SplitPane.dividerFocusColor", c8ddf2,

            "TabbedPane.borderHightlightColor", getPrimary1(),
            "TabbedPane.contentAreaColor", c8ddf2,
            "TabbedPane.contentBorderInsets", new Insets(4, 2, 3, 3),
            "TabbedPane.selected", c8ddf2,
            "TabbedPane.tabAreaBackground", dadada,
            "TabbedPane.tabAreaInsets", new Insets(2, 2, 0, 6),
            "TabbedPane.unselectedBackground", SECONDARY3,

            "Table.focusCellHighlightBorder", focusBorder,
            "Table.gridColor", SECONDARY1,
            "TableHeader.focusCellBackground", c8ddf2,

            "ToggleButton.gradient", buttonGradient,

            "ToolBar.borderColor", cccccc,
            "ToolBar.isRollover", Boolean.TRUE,

            "Tree.closedIcon", directoryIcon,

            "Tree.collapsedIcon",
                  new UIDefaults.LazyValue() {
                      public Object createValue(UIDefaults table) {
                          return new COIcon(getHastenedIcon("icons/ocean/collapsed.gif", table),
                                            getHastenedIcon("icons/ocean/collapsed-rtl.gif", table));
                      }
                  },

            "Tree.expandedIcon",
                  getIconResource("icons/ocean/expanded.gif"),
            "Tree.leafIcon", fileIcon,
            "Tree.openIcon", directoryIcon,
            "Tree.selectionBorderColor", getPrimary1(),
            "Tree.dropLineColor", getPrimary1(),
            "Table.dropLineColor", getPrimary1(),
            "Table.dropLineShortColor", OCEAN_BLACK,

            "Table.dropCellBackground", OCEAN_DROP,
            "Tree.dropCellBackground", OCEAN_DROP,
            "List.dropCellBackground", OCEAN_DROP,
            "List.dropLineColor", getPrimary1()
        };
        table.putDefaults(defaults);
    }

    /**
     * Overriden to enable picking up the system fonts, if applicable.
     */
    boolean isSystemTheme() {
        return true;
    }

    /**
     * Return the name of this theme, "Ocean".
     *
     * @return "Ocean"
     */
    public String getName() {
        return "Ocean";
    }

    /**
     * Returns the primary 1 color. This returns a color with an rgb hex value
     * of {@code 0x6382BF}.
     *
     * @return the primary 1 color
     * @see java.awt.Color#decode
     */
    protected ColorUIResource getPrimary1() {
        return PRIMARY1;
    }

    /**
     * Returns the primary 2 color. This returns a color with an rgb hex value
     * of {@code 0xA3B8CC}.
     *
     * @return the primary 2 color
     * @see java.awt.Color#decode
     */
    protected ColorUIResource getPrimary2() {
        return PRIMARY2;
    }

    /**
     * Returns the primary 3 color. This returns a color with an rgb hex value
     * of {@code 0xB8CFE5}.
     *
     * @return the primary 3 color
     * @see java.awt.Color#decode
     */
    protected ColorUIResource getPrimary3() {
        return PRIMARY3;
    }

    /**
     * Returns the secondary 1 color. This returns a color with an rgb hex
     * value of {@code 0x7A8A99}.
     *
     * @return the secondary 1 color
     * @see java.awt.Color#decode
     */
    protected ColorUIResource getSecondary1() {
        return SECONDARY1;
    }

    /**
     * Returns the secondary 2 color. This returns a color with an rgb hex
     * value of {@code 0xB8CFE5}.
     *
     * @return the secondary 2 color
     * @see java.awt.Color#decode
     */
    protected ColorUIResource getSecondary2() {
        return SECONDARY2;
    }

    /**
     * Returns the secondary 3 color. This returns a color with an rgb hex
     * value of {@code 0xEEEEEE}.
     *
     * @return the secondary 3 color
     * @see java.awt.Color#decode
     */
    protected ColorUIResource getSecondary3() {
        return SECONDARY3;
    }

    /**
     * Returns the black color. This returns a color with an rgb hex
     * value of {@code 0x333333}.
     *
     * @return the black color
     * @see java.awt.Color#decode
     */
    protected ColorUIResource getBlack() {
        return OCEAN_BLACK;
    }

    /**
     * Returns the desktop color. This returns a color with an rgb hex
     * value of {@code 0xFFFFFF}.
     *
     * @return the desktop color
     * @see java.awt.Color#decode
     */
    public ColorUIResource getDesktopColor() {
        return MetalTheme.white;
    }

    /**
     * Returns the inactive control text color. This returns a color with an
     * rgb hex value of {@code 0x999999}.
     *
     * @return the inactive control text color
     */
    public ColorUIResource getInactiveControlTextColor() {
        return INACTIVE_CONTROL_TEXT_COLOR;
    }

    /**
     * Returns the control text color. This returns a color with an
     * rgb hex value of {@code 0x333333}.
     *
     * @return the control text color
     */
    public ColorUIResource getControlTextColor() {
        return CONTROL_TEXT_COLOR;
    }

    /**
     * Returns the menu disabled foreground color. This returns a color with an
     * rgb hex value of {@code 0x999999}.
     *
     * @return the menu disabled foreground color
     */
    public ColorUIResource getMenuDisabledForeground() {
        return MENU_DISABLED_FOREGROUND;
    }

    private Object getIconResource(String iconID) {
        return SwingUtilities2.makeIcon(getClass(), OceanTheme.class, iconID);
    }

    // makes use of getIconResource() to fetch an icon and then hastens it
    // - calls createValue() on it and returns the actual icon
    private Icon getHastenedIcon(String iconID, UIDefaults table) {
        Object res = getIconResource(iconID);
        return (Icon)((UIDefaults.LazyValue)res).createValue(table);
    }
}
