/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.Vector;

import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButton;
import javax.swing.plaf.UIResource;

import sun.swing.CachedPainter;

import static sun.swing.SwingUtilities2.getAndSetAntialisingHintForScaledGraphics;
import static sun.swing.SwingUtilities2.setAntialiasingHintForScaledGraphics;

/**
 * Factory object that vends <code>Icon</code>s for
 * the Java look and feel (Metal).
 * These icons are used extensively in Metal via the defaults mechanism.
 * While other look and feels often use GIFs for icons, creating icons
 * in code facilitates switching to other themes.
 *
 * <p>
 * Each method in this class returns
 * either an <code>Icon</code> or <code>null</code>,
 * where <code>null</code> implies that there is no default icon.
 *
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @author Michael C. Albers
 */
@SuppressWarnings("serial") // Same-version serialization only
public class MetalIconFactory implements Serializable {

    // List of code-drawn Icons
    private static Icon fileChooserDetailViewIcon;
    private static Icon fileChooserHomeFolderIcon;
    private static Icon fileChooserListViewIcon;
    private static Icon fileChooserNewFolderIcon;
    private static Icon fileChooserUpFolderIcon;
    private static Icon internalFrameAltMaximizeIcon;
    private static Icon internalFrameCloseIcon;
    private static Icon internalFrameDefaultMenuIcon;
    private static Icon internalFrameMaximizeIcon;
    private static Icon internalFrameMinimizeIcon;
    private static Icon radioButtonIcon;
    private static Icon treeComputerIcon;
    private static Icon treeFloppyDriveIcon;
    private static Icon treeHardDriveIcon;


    private static Icon menuArrowIcon;
    private static Icon menuItemArrowIcon;
    private static Icon checkBoxMenuItemIcon;
    private static Icon radioButtonMenuItemIcon;
    private static Icon checkBoxIcon;


    // Ocean icons
    private static Icon oceanHorizontalSliderThumb;
    private static Icon oceanVerticalSliderThumb;

    // Constants
    /**
     * {@code DARK} is used for the property {@code Tree.expandedIcon}.
     */
    public static final boolean DARK = false;

    /**
     * {@code LIGHT} is used for the property {@code Tree.collapsedIcon}.
     */
    public static final boolean LIGHT = true;

    /**
     * Constructs a {@code MetalIconFactory}.
     */
    public MetalIconFactory() {}

    // Accessor functions for Icons. Does the caching work.
    /**
     * Returns the instance of {@code FileChooserDetailViewIcon}.
     *
     * @return the instance of {@code FileChooserDetailViewIcon}
     */
    public static Icon getFileChooserDetailViewIcon() {
        if (fileChooserDetailViewIcon == null) {
            fileChooserDetailViewIcon = new FileChooserDetailViewIcon();
        }
        return fileChooserDetailViewIcon;
    }

    /**
     * Returns the instance of {@code FileChooserHomeFolderIcon}.
     *
     * @return the instance of {@code FileChooserHomeFolderIcon}
     */
    public static Icon getFileChooserHomeFolderIcon() {
        if (fileChooserHomeFolderIcon == null) {
            fileChooserHomeFolderIcon = new FileChooserHomeFolderIcon();
        }
        return fileChooserHomeFolderIcon;
    }

    /**
     * Returns the instance of {@code FileChooserListViewIcon}.
     *
     * @return the instance of {@code FileChooserListViewIcon}
     */
    public static Icon getFileChooserListViewIcon() {
        if (fileChooserListViewIcon == null) {
            fileChooserListViewIcon = new FileChooserListViewIcon();
        }
        return fileChooserListViewIcon;
    }

    /**
     * Returns the instance of {@code FileChooserNewFolderIcon}.
     *
     * @return the instance of {@code FileChooserNewFolderIcon}
     */
    public static Icon getFileChooserNewFolderIcon() {
        if (fileChooserNewFolderIcon == null) {
            fileChooserNewFolderIcon = new FileChooserNewFolderIcon();
        }
        return fileChooserNewFolderIcon;
    }

    /**
     * Returns the instance of {@code FileChooserUpFolderIcon}.
     *
     * @return the instance of {@code FileChooserUpFolderIcon}
     */
    public static Icon getFileChooserUpFolderIcon() {
        if (fileChooserUpFolderIcon == null) {
            fileChooserUpFolderIcon = new FileChooserUpFolderIcon();
        }
        return fileChooserUpFolderIcon;
    }

    /**
     * Constructs a new instance of {@code InternalFrameAltMaximizeIcon}.
     *
     * @param size the size of the icon
     * @return a new instance of {@code InternalFrameAltMaximizeIcon}
     */
    public static Icon getInternalFrameAltMaximizeIcon(int size) {
        return new InternalFrameAltMaximizeIcon(size);
    }

    /**
     * Constructs a new instance of {@code InternalFrameCloseIcon}.
     *
     * @param size the size of the icon
     * @return a new instance of {@code InternalFrameCloseIcon}
     */
    public static Icon getInternalFrameCloseIcon(int size) {
        return new InternalFrameCloseIcon(size);
    }

    /**
     * Returns the instance of {@code InternalFrameDefaultMenuIcon}.
     *
     * @return the instance of {@code InternalFrameDefaultMenuIcon}
     */
    public static Icon getInternalFrameDefaultMenuIcon() {
        if (internalFrameDefaultMenuIcon == null) {
            internalFrameDefaultMenuIcon = new InternalFrameDefaultMenuIcon();
        }
        return internalFrameDefaultMenuIcon;
    }

    /**
     * Constructs a new instance of {@code InternalFrameMaximizeIcon}.
     *
     * @param size the size of the icon
     * @return a new instance of {@code InternalFrameMaximizeIcon}
     */
    public static Icon getInternalFrameMaximizeIcon(int size) {
        return new InternalFrameMaximizeIcon(size);
    }

    /**
     * Constructs a new instance of {@code InternalFrameMinimizeIcon}.
     *
     * @param size the size of the icon
     * @return a new instance of {@code InternalFrameMinimizeIcon}
     */
    public static Icon getInternalFrameMinimizeIcon(int size) {
        return new InternalFrameMinimizeIcon(size);
    }

    /**
     * Returns the instance of {@code RadioButtonIcon}.
     *
     * @return the instance of {@code RadioButtonIcon}
     */
    public static Icon getRadioButtonIcon() {
        if (radioButtonIcon == null) {
            radioButtonIcon = new RadioButtonIcon();
        }
        return radioButtonIcon;
    }

    /**
     * Returns a checkbox icon.
     *
     * @return a checkbox icon
     * @since 1.3
     */
    public static Icon getCheckBoxIcon() {
        if (checkBoxIcon == null) {
            checkBoxIcon = new CheckBoxIcon();
        }
        return checkBoxIcon;
    }

    /**
     * Returns the instance of {@code TreeComputerIcon}.
     *
     * @return the instance of {@code TreeComputerIcon}
     */
    public static Icon getTreeComputerIcon() {
        if ( treeComputerIcon == null ) {
            treeComputerIcon = new TreeComputerIcon();
        }
        return treeComputerIcon;
    }

    /**
     * Returns the instance of {@code TreeFloppyDriveIcon}.
     *
     * @return the instance of {@code TreeFloppyDriveIcon}
     */
    public static Icon getTreeFloppyDriveIcon() {
        if ( treeFloppyDriveIcon == null ) {
            treeFloppyDriveIcon = new TreeFloppyDriveIcon();
        }
        return treeFloppyDriveIcon;
    }

    /**
     * Constructs a new instance of {@code TreeFolderIcon}.
     *
     * @return a new instance of {@code TreeFolderIcon}
     */
    public static Icon getTreeFolderIcon() {
        return new TreeFolderIcon();
    }

    /**
     * Returns the instance of {@code TreeHardDriveIcon}.
     *
     * @return the instance of {@code TreeHardDriveIcon}
     */
    public static Icon getTreeHardDriveIcon() {
        if ( treeHardDriveIcon == null ) {
            treeHardDriveIcon = new TreeHardDriveIcon();
        }
        return treeHardDriveIcon;
    }

    /**
     * Constructs a new instance of {@code TreeLeafIcon}.
     *
     * @return a new instance of {@code TreeLeafIcon}
     */
    public static Icon getTreeLeafIcon() {
        return new TreeLeafIcon();
    }

    /**
     * Constructs a new instance of {@code TreeControlIcon}.
     *
     * @param isCollapsed if {@code true} the icon is collapsed
     * @return a new instance of {@code TreeControlIcon}
     */
    public static Icon getTreeControlIcon( boolean isCollapsed ) {
            return new TreeControlIcon( isCollapsed );
    }

    /**
     * Returns an icon to be used by {@code JMenu}.
     *
     * @return an icon to be used by {@code JMenu}
     */
    public static Icon getMenuArrowIcon() {
        if (menuArrowIcon == null) {
            menuArrowIcon = new MenuArrowIcon();
        }
        return menuArrowIcon;
    }

    /**
     * Returns an icon to be used by <code>JCheckBoxMenuItem</code>.
     *
     * @return the default icon for check box menu items,
     *         or <code>null</code> if no default exists
     */
    public static Icon getMenuItemCheckIcon() {
        return null;
    }

    /**
     * Returns an icon to be used by {@code JMenuItem}.
     *
     * @return an icon to be used by {@code JMenuItem}
     */
    public static Icon getMenuItemArrowIcon() {
        if (menuItemArrowIcon == null) {
            menuItemArrowIcon = new MenuItemArrowIcon();
        }
        return menuItemArrowIcon;
    }

    /**
     * Returns an icon to be used by {@code JCheckBoxMenuItem}.
     *
     * @return an icon to be used by {@code JCheckBoxMenuItem}
     */
    public static Icon getCheckBoxMenuItemIcon() {
        if (checkBoxMenuItemIcon == null) {
            checkBoxMenuItemIcon = new CheckBoxMenuItemIcon();
        }
        return checkBoxMenuItemIcon;
    }

    /**
     * Returns an icon to be used by {@code JRadioButtonMenuItem}.
     *
     * @return an icon to be used by {@code JRadioButtonMenuItem}
     */
    public static Icon getRadioButtonMenuItemIcon() {
        if (radioButtonMenuItemIcon == null) {
            radioButtonMenuItemIcon = new RadioButtonMenuItemIcon();
        }
        return radioButtonMenuItemIcon;
    }

    /**
     * Returns a thumb icon to be used by horizontal slider.
     *
     * @return a thumb icon to be used by horizontal slider
     */
    public static Icon getHorizontalSliderThumbIcon() {
        if (MetalLookAndFeel.usingOcean()) {
            if (oceanHorizontalSliderThumb == null) {
                oceanHorizontalSliderThumb =
                               new OceanHorizontalSliderThumbIcon();
            }
            return oceanHorizontalSliderThumb;
        }
      // don't cache these, bumps don't get updated otherwise
        return new HorizontalSliderThumbIcon();
    }

    /**
     * Returns a thumb icon to be used by vertical slider.
     *
     * @return a thumb icon to be used by vertical slider
     */
    public static Icon getVerticalSliderThumbIcon() {
        if (MetalLookAndFeel.usingOcean()) {
            if (oceanVerticalSliderThumb == null) {
                oceanVerticalSliderThumb = new OceanVerticalSliderThumbIcon();
            }
            return oceanVerticalSliderThumb;
        }
        // don't cache these, bumps don't get updated otherwise
        return new VerticalSliderThumbIcon();
    }

    // File Chooser Detail View code
    private static class FileChooserDetailViewIcon implements Icon, UIResource, Serializable {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.translate(x, y);

            // Draw outside edge of each of the documents
            g.setColor(MetalLookAndFeel.getPrimaryControlInfo());
            //     top
            g.drawLine(2,2, 5,2); // top
            g.drawLine(2,3, 2,7); // left
            g.drawLine(3,7, 6,7); // bottom
            g.drawLine(6,6, 6,3); // right
            //     bottom
            g.drawLine(2,10, 5,10); // top
            g.drawLine(2,11, 2,15); // left
            g.drawLine(3,15, 6,15); // bottom
            g.drawLine(6,14, 6,11); // right

            // Draw little dots next to documents
            //     Same color as outside edge
            g.drawLine(8,5, 15,5);     // top
            g.drawLine(8,13, 15,13);   // bottom

            // Draw inner highlight on documents
            g.setColor(MetalLookAndFeel.getPrimaryControl());
            g.drawRect(3,3, 2,3);   // top
            g.drawRect(3,11, 2,3);  // bottom

            // Draw inner inner highlight on documents
            g.setColor(MetalLookAndFeel.getPrimaryControlHighlight());
            g.drawLine(4,4, 4,5);     // top
            g.drawLine(4,12, 4,13);   // bottom

            g.translate(-x, -y);
        }

        public int getIconWidth() {
            return 18;
        }

        public int getIconHeight() {
            return 18;
        }
    }  // End class FileChooserDetailViewIcon

    // File Chooser Home Folder code
    private static class FileChooserHomeFolderIcon implements Icon, UIResource, Serializable {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.translate(x, y);

            // Draw outside edge of house
            g.setColor(MetalLookAndFeel.getPrimaryControlInfo());
            g.drawLine(8,1, 1,8);  // left edge of roof
            g.drawLine(8,1, 15,8); // right edge of roof
            g.drawLine(11,2, 11,3); // left edge of chimney
            g.drawLine(12,2, 12,4); // right edge of chimney
            g.drawLine(3,7, 3,15); // left edge of house
            g.drawLine(13,7, 13,15); // right edge of house
            g.drawLine(4,15, 12,15); // bottom edge of house
            // Draw door frame
            //     same color as edge of house
            g.drawLine( 6,9,  6,14); // left
            g.drawLine(10,9, 10,14); // right
            g.drawLine( 7,9,  9, 9); // top

            // Draw roof body
            g.setColor(MetalLookAndFeel.getControlDarkShadow());
            g.fillRect(8,2, 1,1); //top toward bottom
            g.fillRect(7,3, 3,1);
            g.fillRect(6,4, 5,1);
            g.fillRect(5,5, 7,1);
            g.fillRect(4,6, 9,2);
            // Draw doornob
            //     same color as roof body
            g.drawLine(9,12, 9,12);

            // Paint the house
            g.setColor(MetalLookAndFeel.getPrimaryControl());
            g.drawLine(4,8, 12,8); // above door
            g.fillRect(4,9, 2,6); // left of door
            g.fillRect(11,9, 2,6); // right of door

            g.translate(-x, -y);
        }

        public int getIconWidth() {
            return 18;
        }

        public int getIconHeight() {
            return 18;
        }
    }  // End class FileChooserHomeFolderIcon

    // File Chooser List View code
    private static class FileChooserListViewIcon implements Icon, UIResource, Serializable {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.translate(x, y);

            // Draw outside edge of each of the documents
            g.setColor(MetalLookAndFeel.getPrimaryControlInfo());
            //     top left
            g.drawLine(2,2, 5,2); // top
            g.drawLine(2,3, 2,7); // left
            g.drawLine(3,7, 6,7); // bottom
            g.drawLine(6,6, 6,3); // right
            //     top right
            g.drawLine(10,2, 13,2); // top
            g.drawLine(10,3, 10,7); // left
            g.drawLine(11,7, 14,7); // bottom
            g.drawLine(14,6, 14,3); // right
            //     bottom left
            g.drawLine(2,10, 5,10); // top
            g.drawLine(2,11, 2,15); // left
            g.drawLine(3,15, 6,15); // bottom
            g.drawLine(6,14, 6,11); // right
            //     bottom right
            g.drawLine(10,10, 13,10); // top
            g.drawLine(10,11, 10,15); // left
            g.drawLine(11,15, 14,15); // bottom
            g.drawLine(14,14, 14,11); // right

            // Draw little dots next to documents
            //     Same color as outside edge
            g.drawLine(8,5, 8,5);     // top left
            g.drawLine(16,5, 16,5);   // top right
            g.drawLine(8,13, 8,13);   // bottom left
            g.drawLine(16,13, 16,13); // bottom right

            // Draw inner highlight on documents
            g.setColor(MetalLookAndFeel.getPrimaryControl());
            g.drawRect(3,3, 2,3);   // top left
            g.drawRect(11,3, 2,3);  // top right
            g.drawRect(3,11, 2,3);  // bottom left
            g.drawRect(11,11, 2,3); // bottom right

            // Draw inner inner highlight on documents
            g.setColor(MetalLookAndFeel.getPrimaryControlHighlight());
            g.drawLine(4,4, 4,5);     // top left
            g.drawLine(12,4, 12,5);   // top right
            g.drawLine(4,12, 4,13);   // bottom left
            g.drawLine(12,12, 12,13); // bottom right

            g.translate(-x, -y);
        }

        public int getIconWidth() {
            return 18;
        }

        public int getIconHeight() {
            return 18;
        }
    }  // End class FileChooserListViewIcon

    // File Chooser New Folder code
    private static class FileChooserNewFolderIcon implements Icon, UIResource, Serializable {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.translate(x, y);

            // Fill background
            g.setColor(MetalLookAndFeel.getPrimaryControl());
            g.fillRect(3,5, 12,9);

            // Draw outside edge of folder
            g.setColor(MetalLookAndFeel.getPrimaryControlInfo());
            g.drawLine(1,6,    1,14); // left
            g.drawLine(2,14,  15,14); // bottom
            g.drawLine(15,13, 15,5);  // right
            g.drawLine(2,5,    9,5);  // top left
            g.drawLine(10,6,  14,6);  // top right

            // Draw inner folder highlight
            g.setColor(MetalLookAndFeel.getPrimaryControlHighlight());
            g.drawLine( 2,6,  2,13); // left
            g.drawLine( 3,6,  9,6);  // top left
            g.drawLine(10,7, 14,7);  // top right

            // Draw tab on folder
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawLine(11,3, 15,3); // top
            g.drawLine(10,4, 15,4); // bottom

            g.translate(-x, -y);
        }

        public int getIconWidth() {
            return 18;
        }

        public int getIconHeight() {
            return 18;
        }
    }  // End class FileChooserNewFolderIcon

    // File Chooser Up Folder code
    private static class FileChooserUpFolderIcon implements Icon, UIResource, Serializable {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.translate(x, y);

            // Fill background
            g.setColor(MetalLookAndFeel.getPrimaryControl());
            g.fillRect(3,5, 12,9);

            // Draw outside edge of folder
            g.setColor(MetalLookAndFeel.getPrimaryControlInfo());
            g.drawLine(1,6,    1,14); // left
            g.drawLine(2,14,  15,14); // bottom
            g.drawLine(15,13, 15,5);  // right
            g.drawLine(2,5,    9,5);  // top left
            g.drawLine(10,6,  14,6);  // top right
            // Draw the UP arrow
            //     same color as edge
            g.drawLine(8,13,  8,16); // arrow shaft
            g.drawLine(8, 9,  8, 9); // arrowhead top
            g.drawLine(7,10,  9,10);
            g.drawLine(6,11, 10,11);
            g.drawLine(5,12, 11,12);

            // Draw inner folder highlight
            g.setColor(MetalLookAndFeel.getPrimaryControlHighlight());
            g.drawLine( 2,6,  2,13); // left
            g.drawLine( 3,6,  9,6);  // top left
            g.drawLine(10,7, 14,7);  // top right

            // Draw tab on folder
            g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g.drawLine(11,3, 15,3); // top
            g.drawLine(10,4, 15,4); // bottom

            g.translate(-x, -y);
        }

        public int getIconWidth() {
            return 18;
        }

        public int getIconHeight() {
            return 18;
        }
    }  // End class FileChooserUpFolderIcon


    /**
     * Defines an icon for Palette close
     * @since 1.3
     */
    public static class PaletteCloseIcon implements Icon, UIResource, Serializable{
        int iconSize = 7;

        /**
         * Constructs a {@code PaletteCloseIcon}.
         */
        public PaletteCloseIcon() {}

        public void paintIcon(Component c, Graphics g, int x, int y) {
            JButton parentButton = (JButton)c;
            ButtonModel buttonModel = parentButton.getModel();

            Color back;
            Color highlight = MetalLookAndFeel.getPrimaryControlHighlight();
            Color shadow = MetalLookAndFeel.getPrimaryControlInfo();
            if (buttonModel.isPressed() && buttonModel.isArmed()) {
                back = shadow;
            } else {
                back = MetalLookAndFeel.getPrimaryControlDarkShadow();
            }

            g.translate(x, y);
            g.setColor(back);
            g.drawLine( 0, 1, 5, 6);
            g.drawLine( 1, 0, 6, 5);
            g.drawLine( 1, 1, 6, 6);
            g.drawLine( 6, 1, 1, 6);
            g.drawLine( 5,0, 0,5);
            g.drawLine(5,1, 1,5);

            g.setColor(highlight);
            g.drawLine(6,2, 5,3);
            g.drawLine(2,6, 3, 5);
            g.drawLine(6,6,6,6);


            g.translate(-x, -y);
        }

        public int getIconWidth() {
            return iconSize;
        }

        public int getIconHeight() {
            return iconSize;
        }
    }

    // Internal Frame Close code
    private static class InternalFrameCloseIcon implements Icon, UIResource, Serializable {
        int iconSize = 16;

        public InternalFrameCloseIcon(int size) {
            iconSize = size;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            JButton parentButton = (JButton)c;
            ButtonModel buttonModel = parentButton.getModel();

            Color backgroundColor = MetalLookAndFeel.getPrimaryControl();
            Color internalBackgroundColor =
                MetalLookAndFeel.getPrimaryControl();
            Color mainItemColor =
                MetalLookAndFeel.getPrimaryControlDarkShadow();
            Color darkHighlightColor = MetalLookAndFeel.getBlack();
            Color xLightHighlightColor = MetalLookAndFeel.getWhite();
            Color boxLightHighlightColor = MetalLookAndFeel.getWhite();

            // if the inactive window
            if (parentButton.getClientProperty("paintActive") != Boolean.TRUE)
            {
                backgroundColor = MetalLookAndFeel.getControl();
                internalBackgroundColor = backgroundColor;
                mainItemColor = MetalLookAndFeel.getControlDarkShadow();
                // if inactive and pressed
                if (buttonModel.isPressed() && buttonModel.isArmed()) {
                    internalBackgroundColor =
                        MetalLookAndFeel.getControlShadow();
                    xLightHighlightColor = internalBackgroundColor;
                    mainItemColor = darkHighlightColor;
                }
            }
            // if pressed
            else if (buttonModel.isPressed() && buttonModel.isArmed()) {
                internalBackgroundColor =
                    MetalLookAndFeel.getPrimaryControlShadow();
                xLightHighlightColor = internalBackgroundColor;
                mainItemColor = darkHighlightColor;
                // darkHighlightColor is still "getBlack()"
            }

            // Some calculations that are needed more than once later on.
            int oneHalf = iconSize / 2; // 16 -> 8

            g.translate(x, y);

            // fill background
            g.setColor(backgroundColor);
            g.fillRect(0,0, iconSize,iconSize);

            // fill inside of box area
            g.setColor(internalBackgroundColor);
            g.fillRect(3,3, iconSize-6,iconSize-6);

            // THE BOX
            // the top/left dark higlight - some of this will get overwritten
            g.setColor(darkHighlightColor);
            g.drawRect(1,1, iconSize-3,iconSize-3);
            // draw the inside bottom/right highlight
            g.drawRect(2,2, iconSize-5,iconSize-5);
            // draw the light/outside, bottom/right highlight
            g.setColor(boxLightHighlightColor);
            g.drawRect(2,2, iconSize-3,iconSize-3);
            // draw the "normal" box
            g.setColor(mainItemColor);
            g.drawRect(2,2, iconSize-4,iconSize-4);
            g.drawLine(3,iconSize-3, 3,iconSize-3); // lower left
            g.drawLine(iconSize-3,3, iconSize-3,3); // up right

            // THE "X"
            // Dark highlight
            g.setColor(darkHighlightColor);
            g.drawLine(4,5, 5,4); // far up left
            g.drawLine(4,iconSize-6, iconSize-6,4); // against body of "X"
            // Light highlight
            g.setColor(xLightHighlightColor);
            g.drawLine(6,iconSize-5, iconSize-5,6); // against body of "X"
              // one pixel over from the body
            g.drawLine(oneHalf,oneHalf+2, oneHalf+2,oneHalf);
              // bottom right
            g.drawLine(iconSize-5,iconSize-5, iconSize-4,iconSize-5);
            g.drawLine(iconSize-5,iconSize-4, iconSize-5,iconSize-4);
            // Main color
            g.setColor(mainItemColor);
              // Upper left to lower right
            g.drawLine(5,5, iconSize-6,iconSize-6); // g.drawLine(5,5, 10,10);
            g.drawLine(6,5, iconSize-5,iconSize-6); // g.drawLine(6,5, 11,10);
            g.drawLine(5,6, iconSize-6,iconSize-5); // g.drawLine(5,6, 10,11);
              // Lower left to upper right
            g.drawLine(5,iconSize-5, iconSize-5,5); // g.drawLine(5,11, 11,5);
            g.drawLine(5,iconSize-6, iconSize-6,5); // g.drawLine(5,10, 10,5);

            g.translate(-x, -y);
        }

        public int getIconWidth() {
            return iconSize;
        }

        public int getIconHeight() {
            return iconSize;
        }
    }  // End class InternalFrameCloseIcon

    // Internal Frame Alternate Maximize code (actually, the un-maximize icon)
    private static class InternalFrameAltMaximizeIcon implements Icon, UIResource, Serializable {
        int iconSize = 16;

        public InternalFrameAltMaximizeIcon(int size) {
            iconSize = size;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            JButton parentButton = (JButton)c;
            ButtonModel buttonModel = parentButton.getModel();

            Color backgroundColor = MetalLookAndFeel.getPrimaryControl();
            Color internalBackgroundColor =
                MetalLookAndFeel.getPrimaryControl();
            Color mainItemColor =
                MetalLookAndFeel.getPrimaryControlDarkShadow();
            Color darkHighlightColor = MetalLookAndFeel.getBlack();
            // ul = Upper Left and lr = Lower Right
            Color ulLightHighlightColor = MetalLookAndFeel.getWhite();
            Color lrLightHighlightColor = MetalLookAndFeel.getWhite();

            // if the internal frame is inactive
            if (parentButton.getClientProperty("paintActive") != Boolean.TRUE)
            {
                backgroundColor = MetalLookAndFeel.getControl();
                internalBackgroundColor = backgroundColor;
                mainItemColor = MetalLookAndFeel.getControlDarkShadow();
                // if inactive and pressed
                if (buttonModel.isPressed() && buttonModel.isArmed()) {
                    internalBackgroundColor =
                        MetalLookAndFeel.getControlShadow();
                    ulLightHighlightColor = internalBackgroundColor;
                    mainItemColor = darkHighlightColor;
                }
            }
            // if the button is pressed and the mouse is over it
            else if (buttonModel.isPressed() && buttonModel.isArmed()) {
                internalBackgroundColor =
                    MetalLookAndFeel.getPrimaryControlShadow();
                ulLightHighlightColor = internalBackgroundColor;
                mainItemColor = darkHighlightColor;
                // darkHighlightColor is still "getBlack()"
            }

            g.translate(x, y);

            // fill background
            g.setColor(backgroundColor);
            g.fillRect(0,0, iconSize,iconSize);

            // BOX
            // fill inside the box
            g.setColor(internalBackgroundColor);
            g.fillRect(3,6, iconSize-9,iconSize-9);

            // draw dark highlight color
            g.setColor(darkHighlightColor);
            g.drawRect(1,5, iconSize-8,iconSize-8);
            g.drawLine(1,iconSize-2, 1,iconSize-2); // extra pixel on bottom

            // draw lower right light highlight
            g.setColor(lrLightHighlightColor);
            g.drawRect(2,6, iconSize-7,iconSize-7);
            // draw upper left light highlight
            g.setColor(ulLightHighlightColor);
            g.drawRect(3,7, iconSize-9,iconSize-9);

            // draw the main box
            g.setColor(mainItemColor);
            g.drawRect(2,6, iconSize-8,iconSize-8);

            // Six extraneous pixels to deal with
            g.setColor(ulLightHighlightColor);
            g.drawLine(iconSize-6,8,iconSize-6,8);
            g.drawLine(iconSize-9,6, iconSize-7,8);
            g.setColor(mainItemColor);
            g.drawLine(3,iconSize-3,3,iconSize-3);
            g.setColor(darkHighlightColor);
            g.drawLine(iconSize-6,9,iconSize-6,9);
            g.setColor(backgroundColor);
            g.drawLine(iconSize-9,5,iconSize-9,5);

            // ARROW
            // do the shaft first
            g.setColor(mainItemColor);
            g.fillRect(iconSize-7,3, 3,5); // do a big block
            g.drawLine(iconSize-6,5, iconSize-3,2); // top shaft
            g.drawLine(iconSize-6,6, iconSize-2,2); // bottom shaft
            g.drawLine(iconSize-6,7, iconSize-3,7); // bottom arrow head

            // draw the dark highlight
            g.setColor(darkHighlightColor);
            g.drawLine(iconSize-8,2, iconSize-7,2); // top of arrowhead
            g.drawLine(iconSize-8,3, iconSize-8,7); // left of arrowhead
            g.drawLine(iconSize-6,4, iconSize-3,1); // top of shaft
            g.drawLine(iconSize-4,6, iconSize-3,6); // top,right of arrowhead

            // draw the light highlight
            g.setColor(lrLightHighlightColor);
            g.drawLine(iconSize-6,3, iconSize-6,3); // top
            g.drawLine(iconSize-4,5, iconSize-2,3); // under shaft
            g.drawLine(iconSize-4,8, iconSize-3,8); // under arrowhead
            g.drawLine(iconSize-2,8, iconSize-2,7); // right of arrowhead

            g.translate(-x, -y);
        }

        public int getIconWidth() {
            return iconSize;
        }

        public int getIconHeight() {
            return iconSize;
        }
    }  // End class InternalFrameAltMaximizeIcon

    // Code for the default icons that goes in the upper left corner
    private static class InternalFrameDefaultMenuIcon implements Icon, UIResource, Serializable {
        public void paintIcon(Component c, Graphics g, int x, int y) {

            Color windowBodyColor = MetalLookAndFeel.getWindowBackground();
            Color titleColor = MetalLookAndFeel.getPrimaryControl();
            Color edgeColor = MetalLookAndFeel.getPrimaryControlDarkShadow();

            g.translate(x, y);

            // draw background color for title area
            // catch four corners and title area
            g.setColor(titleColor);
            g.fillRect(0,0, 16,16);

            // fill body of window
            g.setColor(windowBodyColor);
            g.fillRect(2,6, 13,9);
            // draw light parts of two "bumps"
            g.drawLine(2,2, 2,2);
            g.drawLine(5,2, 5,2);
            g.drawLine(8,2, 8,2);
            g.drawLine(11,2, 11,2);

            // draw line around edge of title and icon
            g.setColor(edgeColor);
            g.drawRect(1,1, 13,13); // entire inner edge
            g.drawLine(1,0, 14,0); // top outter edge
            g.drawLine(15,1, 15,14); // right outter edge
            g.drawLine(1,15, 14,15); // bottom outter edge
            g.drawLine(0,1, 0,14); // left outter edge
            g.drawLine(2,5, 13,5); // bottom of title bar area
            // draw dark part of four "bumps" (same color)
            g.drawLine(3,3, 3,3);
            g.drawLine(6,3, 6,3);
            g.drawLine(9,3, 9,3);
            g.drawLine(12,3, 12,3);

            g.translate(-x, -y);
        }

        public int getIconWidth() {
            return 16;
        }

        public int getIconHeight() {
            return 16;
        }
    }  // End class InternalFrameDefaultMenuIcon

    // Internal Frame Maximize code
    private static class InternalFrameMaximizeIcon implements Icon, UIResource, Serializable {
        protected int iconSize = 16;

        public InternalFrameMaximizeIcon(int size) {
            iconSize = size;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            JButton parentButton = (JButton)c;
            ButtonModel buttonModel = parentButton.getModel();

            Color backgroundColor = MetalLookAndFeel.getPrimaryControl();
            Color internalBackgroundColor =
                MetalLookAndFeel.getPrimaryControl();
            Color mainItemColor =
                MetalLookAndFeel.getPrimaryControlDarkShadow();
            Color darkHighlightColor = MetalLookAndFeel.getBlack();
            // ul = Upper Left and lr = Lower Right
            Color ulLightHighlightColor = MetalLookAndFeel.getWhite();
            Color lrLightHighlightColor = MetalLookAndFeel.getWhite();

            // if the internal frame is inactive
            if (parentButton.getClientProperty("paintActive") != Boolean.TRUE)
            {
                backgroundColor = MetalLookAndFeel.getControl();
                internalBackgroundColor = backgroundColor;
                mainItemColor = MetalLookAndFeel.getControlDarkShadow();
                // if inactive and pressed
                if (buttonModel.isPressed() && buttonModel.isArmed()) {
                    internalBackgroundColor =
                        MetalLookAndFeel.getControlShadow();
                    ulLightHighlightColor = internalBackgroundColor;
                    mainItemColor = darkHighlightColor;
                }
            }
            // if the button is pressed and the mouse is over it
            else if (buttonModel.isPressed() && buttonModel.isArmed()) {
                internalBackgroundColor =
                    MetalLookAndFeel.getPrimaryControlShadow();
                ulLightHighlightColor = internalBackgroundColor;
                mainItemColor = darkHighlightColor;
                // darkHighlightColor is still "getBlack()"
            }

            g.translate(x, y);

            // fill background
            g.setColor(backgroundColor);
            g.fillRect(0,0, iconSize,iconSize);

            // BOX drawing
            // fill inside the box
            g.setColor(internalBackgroundColor);
            g.fillRect(3,7, iconSize-10,iconSize-10);

            // light highlight
            g.setColor(ulLightHighlightColor);
            g.drawRect(3,7, iconSize-10,iconSize-10); // up,left
            g.setColor(lrLightHighlightColor);
            g.drawRect(2,6, iconSize-7,iconSize-7); // low,right
            // dark highlight
            g.setColor(darkHighlightColor);
            g.drawRect(1,5, iconSize-7,iconSize-7); // outer
            g.drawRect(2,6, iconSize-9,iconSize-9); // inner
            // main box
            g.setColor(mainItemColor);
            g.drawRect(2,6, iconSize-8,iconSize-8); // g.drawRect(2,6, 8,8);

            // ARROW drawing
            // dark highlight
            g.setColor(darkHighlightColor);
              // down,left to up,right - inside box
            g.drawLine(3,iconSize-5, iconSize-9,7);
              // down,left to up,right - outside box
            g.drawLine(iconSize-6,4, iconSize-5,3);
              // outside edge of arrow head
            g.drawLine(iconSize-7,1, iconSize-7,2);
              // outside edge of arrow head
            g.drawLine(iconSize-6,1, iconSize-2,1);
            // light highlight
            g.setColor(ulLightHighlightColor);
              // down,left to up,right - inside box
            g.drawLine(5,iconSize-4, iconSize-8,9);
            g.setColor(lrLightHighlightColor);
            g.drawLine(iconSize-6,3, iconSize-4,5); // outside box
            g.drawLine(iconSize-4,5, iconSize-4,6); // one down from this
            g.drawLine(iconSize-2,7, iconSize-1,7); // outside edge arrow head
            g.drawLine(iconSize-1,2, iconSize-1,6); // outside edge arrow head
            // main part of arrow
            g.setColor(mainItemColor);
            g.drawLine(3,iconSize-4, iconSize-3,2); // top edge of staff
            g.drawLine(3,iconSize-3, iconSize-2,2); // bottom edge of staff
            g.drawLine(4,iconSize-3, 5,iconSize-3); // highlights inside of box
            g.drawLine(iconSize-7,8, iconSize-7,9); // highlights inside of box
            g.drawLine(iconSize-6,2, iconSize-4,2); // top of arrow head
            g.drawRect(iconSize-3,3, 1,3); // right of arrow head

            g.translate(-x, -y);
        }

        public int getIconWidth() {
            return iconSize;
        }

        public int getIconHeight() {
            return iconSize;
        }
    }  // End class InternalFrameMaximizeIcon

    // Internal Frame Minimize code
    private static class InternalFrameMinimizeIcon implements Icon, UIResource, Serializable {
        int iconSize = 16;

        public InternalFrameMinimizeIcon(int size) {
            iconSize = size;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            JButton parentButton = (JButton)c;
            ButtonModel buttonModel = parentButton.getModel();


            Color backgroundColor = MetalLookAndFeel.getPrimaryControl();
            Color internalBackgroundColor =
                MetalLookAndFeel.getPrimaryControl();
            Color mainItemColor =
                MetalLookAndFeel.getPrimaryControlDarkShadow();
            Color darkHighlightColor = MetalLookAndFeel.getBlack();
            // ul = Upper Left and lr = Lower Right
            Color ulLightHighlightColor = MetalLookAndFeel.getWhite();
            Color lrLightHighlightColor = MetalLookAndFeel.getWhite();

            // if the internal frame is inactive
            if (parentButton.getClientProperty("paintActive") != Boolean.TRUE)
            {
                backgroundColor = MetalLookAndFeel.getControl();
                internalBackgroundColor = backgroundColor;
                mainItemColor = MetalLookAndFeel.getControlDarkShadow();
                // if inactive and pressed
                if (buttonModel.isPressed() && buttonModel.isArmed()) {
                    internalBackgroundColor =
                        MetalLookAndFeel.getControlShadow();
                    ulLightHighlightColor = internalBackgroundColor;
                    mainItemColor = darkHighlightColor;
                }
            }
            // if the button is pressed and the mouse is over it
            else if (buttonModel.isPressed() && buttonModel.isArmed()) {
                internalBackgroundColor =
                    MetalLookAndFeel.getPrimaryControlShadow();
                ulLightHighlightColor = internalBackgroundColor;
                mainItemColor = darkHighlightColor;
                // darkHighlightColor is still "getBlack()"
            }

            g.translate(x, y);

            // fill background
            g.setColor(backgroundColor);
            g.fillRect(0,0, iconSize,iconSize);

            // BOX drawing
            // fill inside the box
            g.setColor(internalBackgroundColor);
            g.fillRect(4,11, iconSize-13,iconSize-13);
            // light highlight
            g.setColor(lrLightHighlightColor);
            g.drawRect(2,10, iconSize-10,iconSize-11); // low,right
            g.setColor(ulLightHighlightColor);
            g.drawRect(3,10, iconSize-12,iconSize-12); // up,left
            // dark highlight
            g.setColor(darkHighlightColor);
            g.drawRect(1,8, iconSize-10,iconSize-10); // outer
            g.drawRect(2,9, iconSize-12,iconSize-12); // inner
            // main box
            g.setColor(mainItemColor);
            g.drawRect(2,9, iconSize-11,iconSize-11);
            g.drawLine(iconSize-10,10, iconSize-10,10); // up right highlight
            g.drawLine(3,iconSize-3, 3,iconSize-3); // low left highlight

            // ARROW
            // do the shaft first
            g.setColor(mainItemColor);
            g.fillRect(iconSize-7,3, 3,5); // do a big block
            g.drawLine(iconSize-6,5, iconSize-3,2); // top shaft
            g.drawLine(iconSize-6,6, iconSize-2,2); // bottom shaft
            g.drawLine(iconSize-6,7, iconSize-3,7); // bottom arrow head

            // draw the dark highlight
            g.setColor(darkHighlightColor);
            g.drawLine(iconSize-8,2, iconSize-7,2); // top of arrowhead
            g.drawLine(iconSize-8,3, iconSize-8,7); // left of arrowhead
            g.drawLine(iconSize-6,4, iconSize-3,1); // top of shaft
            g.drawLine(iconSize-4,6, iconSize-3,6); // top,right of arrowhead

            // draw the light highlight
            g.setColor(lrLightHighlightColor);
            g.drawLine(iconSize-6,3, iconSize-6,3); // top
            g.drawLine(iconSize-4,5, iconSize-2,3); // under shaft
            g.drawLine(iconSize-7,8, iconSize-3,8); // under arrowhead
            g.drawLine(iconSize-2,8, iconSize-2,7); // right of arrowhead

            g.translate(-x, -y);
        }

        public int getIconWidth() {
            return iconSize;
        }

        public int getIconHeight() {
            return iconSize;
        }
    }  // End class InternalFrameMinimizeIcon

    private static class CheckBoxIcon implements Icon, UIResource, Serializable {

        protected int getControlSize() { return 13; }

        private void paintOceanIcon(Component c, Graphics g, int x, int y) {
            ButtonModel model = ((JCheckBox)c).getModel();

            g.translate(x, y);
            int w = getIconWidth();
            int h = getIconHeight();
            if ( model.isEnabled() ) {
                if (model.isPressed() && model.isArmed()) {
                    g.setColor(MetalLookAndFeel.getControlShadow());
                    g.fillRect(0, 0, w, h);
                    g.setColor(MetalLookAndFeel.getControlDarkShadow());
                    g.fillRect(0, 0, w, 2);
                    g.fillRect(0, 2, 2, h - 2);
                    g.fillRect(w - 1, 1, 1, h - 1);
                    g.fillRect(1, h - 1, w - 2, 1);
                } else if (model.isRollover()) {
                    MetalUtils.drawGradient(c, g, "CheckBox.gradient", 0, 0,
                                            w, h, true);
                    g.setColor(MetalLookAndFeel.getControlDarkShadow());
                    g.drawRect(0, 0, w - 1, h - 1);
                    g.setColor(MetalLookAndFeel.getPrimaryControl());
                    g.drawRect(1, 1, w - 3, h - 3);
                    g.drawRect(2, 2, w - 5, h - 5);
                }
                else {
                    MetalUtils.drawGradient(c, g, "CheckBox.gradient", 0, 0,
                                            w, h, true);
                    g.setColor(MetalLookAndFeel.getControlDarkShadow());
                    g.drawRect(0, 0, w - 1, h - 1);
                }
                g.setColor( MetalLookAndFeel.getControlInfo() );
            } else {
                g.setColor(MetalLookAndFeel.getControlDarkShadow());
                g.drawRect(0, 0, w - 1, h - 1);
            }
            g.translate(-x, -y);
            if (model.isSelected()) {
                drawCheck(c,g,x,y);
            }
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (MetalLookAndFeel.usingOcean()) {
                paintOceanIcon(c, g, x, y);
                return;
            }
            ButtonModel model = ((JCheckBox)c).getModel();
            int controlSize = getControlSize();

            if ( model.isEnabled() ) {
                if (model.isPressed() && model.isArmed()) {
                    g.setColor( MetalLookAndFeel.getControlShadow() );
                    g.fillRect( x, y, controlSize-1, controlSize-1);
                    MetalUtils.drawPressed3DBorder(g, x, y, controlSize, controlSize);
                } else {
                    MetalUtils.drawFlush3DBorder(g, x, y, controlSize, controlSize);
                }
                g.setColor(c.getForeground());
            } else {
                g.setColor( MetalLookAndFeel.getControlShadow() );
                g.drawRect( x, y, controlSize-2, controlSize-2);
            }

            if (model.isSelected()) {
                drawCheck(c,g,x,y);
            }

        }

        protected void drawCheck(Component c, Graphics g, int x, int y) {
            int controlSize = getControlSize();
            int csx = controlSize - 3;
            int csy1 = controlSize - 6;
            int csy2 = controlSize - 4;
            int csy3 = controlSize - 3;
            int[] xPoints = {3, 5, 5, csx, csx, 5, 5, 3};
            int[] yPoints = {5, 5, csy1, 2, 4, csy2, csy3, csy3};
            g.translate(x, y);
            g.fillPolygon(xPoints, yPoints, 8);
            g.translate(-x, -y);
        }

        public int getIconWidth() {
            return getControlSize();
        }

        public int getIconHeight() {
            return getControlSize();
        }
    } // End class CheckBoxIcon

    // Radio button code
    private static class RadioButtonIcon implements Icon, UIResource, Serializable {
        public void paintOceanIcon(Component c, Graphics g, int x, int y) {
            ButtonModel model = ((JRadioButton)c).getModel();
            boolean enabled = model.isEnabled();
            boolean pressed = (enabled && model.isPressed() &&
                               model.isArmed());
            boolean rollover = (enabled && model.isRollover());

            g.translate(x, y);
            if (enabled && !pressed) {
                // PENDING: this isn't quite right, when we're sure it won't
                // change it needs to be cleaned.
                MetalUtils.drawGradient(c, g, "RadioButton.gradient",
                                        1, 1, 10, 10, true);
                g.setColor(c.getBackground());
                g.fillRect(1, 1, 1, 1);
                g.fillRect(10, 1, 1, 1);
                g.fillRect(1, 10, 1, 1);
                g.fillRect(10, 10, 1, 1);
            }
            else if (pressed || !enabled) {
                if (pressed) {
                    g.setColor(MetalLookAndFeel.getPrimaryControl());
                }
                else {
                    g.setColor(MetalLookAndFeel.getControl());
                }
                g.fillOval(1, 1, 9, 9);
            }

            // draw Dark Circle (start at top, go clockwise)
            if (!enabled) {
                g.setColor(MetalLookAndFeel.getInactiveControlTextColor());
            }
            else {
                g.setColor(MetalLookAndFeel.getControlDarkShadow());
            }
            g.drawOval(0, 0, 11, 11);

            if (pressed) {
                g.drawArc(1, 1, 10, 10, 60, 160);
            }
            else if (rollover) {
                g.setColor(MetalLookAndFeel.getPrimaryControl());
                g.drawOval(2, 2, 7, 7);
            }

            // selected dot
            if (model.isSelected()) {
                if (enabled) {
                    g.setColor(MetalLookAndFeel.getControlInfo());
                } else {
                    g.setColor(MetalLookAndFeel.getControlDarkShadow());
                }
                g.fillOval(2, 2, 7, 7);
            }

            g.translate(-x, -y);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {

            Object aaHint = getAndSetAntialisingHintForScaledGraphics(g);

            if (MetalLookAndFeel.usingOcean()) {
                paintOceanIcon(c, g, x, y);
                setAntialiasingHintForScaledGraphics(g, aaHint);
                return;
            }
            JRadioButton rb = (JRadioButton)c;
            ButtonModel model = rb.getModel();
            boolean drawDot = model.isSelected();

            Color background = c.getBackground();
            Color dotColor = c.getForeground();
            Color shadow = MetalLookAndFeel.getControlShadow();
            Color darkCircle = MetalLookAndFeel.getControlDarkShadow();
            Color whiteInnerLeftArc = MetalLookAndFeel.getControlHighlight();
            Color whiteOuterRightArc = MetalLookAndFeel.getControlHighlight();
            Color interiorColor = background;

            // Set up colors per RadioButtonModel condition
            if ( !model.isEnabled() ) {
                whiteInnerLeftArc = whiteOuterRightArc = background;
                darkCircle = dotColor = shadow;
            }
            else if (model.isPressed() && model.isArmed() ) {
                whiteInnerLeftArc = interiorColor = shadow;
            }

            g.translate(x, y);

            // fill interior
            if (c.isOpaque()) {
                g.setColor(interiorColor);
                g.fillRect(2, 2, 9, 9);
            }

            // draw Dark Circle (start at top, go clockwise)
            g.setColor(darkCircle);
            g.drawOval(0, 0, 11, 11);

            // draw Inner Left (usually) White Arc
            //  start at lower left corner, go clockwise
            g.setColor(whiteInnerLeftArc);
            g.drawArc(1, 1, 10, 10, 60, 160);
            // draw Outer Right White Arc
            //  start at upper right corner, go clockwise
            g.setColor(whiteOuterRightArc);
            g.drawArc(-1, -1, 13, 13, 235, 180);

            // selected dot
            if ( drawDot ) {
                g.setColor(dotColor);
                g.fillOval(2, 2, 7, 7);
            }

            g.translate(-x, -y);
            setAntialiasingHintForScaledGraphics(g, aaHint);
        }

        public int getIconWidth() {
            return 13;
        }

        public int getIconHeight() {
            return 13;
        }
    }  // End class RadioButtonIcon

    // Tree Computer Icon code
    private static class TreeComputerIcon implements Icon, UIResource, Serializable {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.translate(x, y);

            // Fill glass portion of monitor
            g.setColor(MetalLookAndFeel.getPrimaryControl());
            g.fillRect(5,4, 6,4);

            // Draw outside edge of monitor
            g.setColor(MetalLookAndFeel.getPrimaryControlInfo());
            g.drawLine( 2,2,  2,8); // left
            g.drawLine(13,2, 13,8); // right
            g.drawLine( 3,1, 12,1); // top
            g.drawLine(12,9, 12,9); // bottom right base
            g.drawLine( 3,9,  3,9); // bottom left base
            // Draw the edge of the glass
            g.drawLine( 4,4,  4,7); // left
            g.drawLine( 5,3, 10,3); // top
            g.drawLine(11,4, 11,7); // right
            g.drawLine( 5,8, 10,8); // bottom
            // Draw the edge of the CPU
            g.drawLine( 1,10, 14,10); // top
            g.drawLine(14,10, 14,14); // right
            g.drawLine( 1,14, 14,14); // bottom
            g.drawLine( 1,10,  1,14); // left

            // Draw the disk drives
            g.setColor(MetalLookAndFeel.getControlDarkShadow());
            g.drawLine( 6,12,  8,12); // left
            g.drawLine(10,12, 12,12); // right

            g.translate(-x, -y);
        }

        public int getIconWidth() {
            return 16;
        }

        public int getIconHeight() {
            return 16;
        }
    }  // End class TreeComputerIcon

    // Tree HardDrive Icon code
    private static class TreeHardDriveIcon implements Icon, UIResource, Serializable {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.translate(x, y);

            // Draw edges of the disks
            g.setColor(MetalLookAndFeel.getPrimaryControlInfo());
            //     top disk
            g.drawLine(1,4, 1,5); // left
            g.drawLine(2,3, 3,3);
            g.drawLine(4,2, 11,2); // top
            g.drawLine(12,3, 13,3);
            g.drawLine(14,4, 14,5); // right
            g.drawLine(12,6, 13,6);
            g.drawLine(4,7, 11,7); // bottom
            g.drawLine(2,6, 3,6);
            //     middle disk
            g.drawLine(1,7, 1,8); // left
            g.drawLine(2,9, 3,9);
            g.drawLine(4,10, 11,10); // bottom
            g.drawLine(12,9, 13,9);
            g.drawLine(14,7, 14, 8); // right
            //     bottom disk
            g.drawLine(1,10, 1,11); // left
            g.drawLine(2,12, 3,12);
            g.drawLine(4,13, 11,13); // bottom
            g.drawLine(12,12, 13,12);
            g.drawLine(14,10, 14,11); // right

            // Draw the down right shadows
            g.setColor(MetalLookAndFeel.getControlShadow());
            //     top disk
            g.drawLine(7,6, 7,6);
            g.drawLine(9,6, 9,6);
            g.drawLine(10,5, 10,5);
            g.drawLine(11,6, 11,6);
            g.drawLine(12,5, 13,5);
            g.drawLine(13,4, 13,4);
            //     middle disk
            g.drawLine(7,9, 7,9);
            g.drawLine(9,9, 9,9);
            g.drawLine(10,8, 10,8);
            g.drawLine(11,9, 11,9);
            g.drawLine(12,8, 13,8);
            g.drawLine(13,7, 13,7);
            //     bottom disk
            g.drawLine(7,12, 7,12);
            g.drawLine(9,12, 9,12);
            g.drawLine(10,11, 10,11);
            g.drawLine(11,12, 11,12);
            g.drawLine(12,11, 13,11);
            g.drawLine(13,10, 13,10);

            // Draw the up left highlight
            g.setColor(MetalLookAndFeel.getControlHighlight());
            //     top disk
            g.drawLine(4,3, 5,3);
            g.drawLine(7,3, 9,3);
            g.drawLine(11,3, 11,3);
            g.drawLine(2,4, 6,4);
            g.drawLine(8,4, 8,4);
            g.drawLine(2,5, 3,5);
            g.drawLine(4,6, 4,6);
            //     middle disk
            g.drawLine(2,7, 3,7);
            g.drawLine(2,8, 3,8);
            g.drawLine(4,9, 4,9);
            //     bottom disk
            g.drawLine(2,10, 3,10);
            g.drawLine(2,11, 3,11);
            g.drawLine(4,12, 4,12);

            g.translate(-x, -y);
        }

        public int getIconWidth() {
            return 16;
        }

        public int getIconHeight() {
            return 16;
        }
    }  // End class TreeHardDriveIcon

    // Tree FloppyDrive Icon code
    private static class TreeFloppyDriveIcon implements Icon, UIResource, Serializable {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.translate(x, y);

            // Fill body of floppy
            g.setColor(MetalLookAndFeel.getPrimaryControl());
            g.fillRect(2,2, 12,12);

            // Draw outside edge of floppy
            g.setColor(MetalLookAndFeel.getPrimaryControlInfo());
            g.drawLine( 1, 1, 13, 1); // top
            g.drawLine(14, 2, 14,14); // right
            g.drawLine( 1,14, 14,14); // bottom
            g.drawLine( 1, 1,  1,14); // left

            // Draw grey-ish highlights
            g.setColor(MetalLookAndFeel.getControlDarkShadow());
            g.fillRect(5,2, 6,5); // metal disk protector part
            g.drawLine(4,8, 11,8); // top of label
            g.drawLine(3,9, 3,13); // left of label
            g.drawLine(12,9, 12,13); // right of label

            // Draw label and exposed disk
            g.setColor(MetalLookAndFeel.getPrimaryControlHighlight());
            g.fillRect(8,3, 2,3); // exposed disk
            g.fillRect(4,9, 8,5); // label

            // Draw text on label
            g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
            g.drawLine(5,10, 9,10);
            g.drawLine(5,12, 8,12);

            g.translate(-x, -y);
        }

        public int getIconWidth() {
            return 16;
        }

        public int getIconHeight() {
            return 16;
        }
    }  // End class TreeFloppyDriveIcon


    private static final Dimension folderIcon16Size = new Dimension( 16, 16 );

    /**
     * Utility class for caching icon images.  This is necessary because
     * we need a new image whenever we are rendering into a new
     * GraphicsConfiguration, but we do not want to keep recreating icon
     * images for GC's that we have already seen (for example,
     * dragging a window back and forth between monitors on a multimon
     * system, or drawing an icon to different Components that have different
     * GC's).
     * So now whenever we create a new icon image for a given GC, we
     * cache that image with the GC for later retrieval.
     */
    static class ImageCacher {

        // PENDING: Replace this class with CachedPainter.

        Vector<ImageGcPair> images = new Vector<ImageGcPair>(1, 1);
        ImageGcPair currentImageGcPair;

        class ImageGcPair {
            Image image;
            GraphicsConfiguration gc;
            ImageGcPair(Image image, GraphicsConfiguration gc) {
                this.image = image;
                this.gc = gc;
            }

            boolean hasSameConfiguration(GraphicsConfiguration newGC) {
                return ((newGC != null) && (newGC.equals(gc))) ||
                        ((newGC == null) && (gc == null));
            }

        }

        Image getImage(GraphicsConfiguration newGC) {
            if ((currentImageGcPair == null) ||
                !(currentImageGcPair.hasSameConfiguration(newGC)))
            {
                for (ImageGcPair imgGcPair : images) {
                    if (imgGcPair.hasSameConfiguration(newGC)) {
                        currentImageGcPair = imgGcPair;
                        return imgGcPair.image;
                    }
                }
                return null;
            }
            return currentImageGcPair.image;
        }

        void cacheImage(Image image, GraphicsConfiguration gc) {
            ImageGcPair imgGcPair = new ImageGcPair(image, gc);
            images.addElement(imgGcPair);
            currentImageGcPair = imgGcPair;
        }

    }

    /**
     * <p>
     * <strong>Warning:</strong>
     * Serialized objects of this class will not be compatible with
     * future Swing releases. The current serialization support is
     * appropriate for short term storage or RMI between applications running
     * the same version of Swing.  As of 1.4, support for long term storage
     * of all JavaBeans
     * has been added to the <code>java.beans</code> package.
     * Please see {@link java.beans.XMLEncoder}.
     */
    @SuppressWarnings("serial") // Same-version serialization only
    public static class FolderIcon16 implements Icon, Serializable {

        ImageCacher imageCacher;

        /**
         * Constructs a {@code FolderIcon16}.
         */
        public FolderIcon16() {}

        public void paintIcon(Component c, Graphics g, int x, int y) {
            GraphicsConfiguration gc = c.getGraphicsConfiguration();
            if (imageCacher == null) {
                imageCacher = new ImageCacher();
            }
            Image image = imageCacher.getImage(gc);
            if (image == null) {
                if (gc != null) {
                    image = gc.createCompatibleImage(getIconWidth(),
                                                     getIconHeight(),
                                                     Transparency.BITMASK);
                } else {
                    image = new BufferedImage(getIconWidth(),
                                              getIconHeight(),
                                              BufferedImage.TYPE_INT_ARGB);
                }
                Graphics imageG = image.getGraphics();
                paintMe(c,imageG);
                imageG.dispose();
                imageCacher.cacheImage(image, gc);
            }
            g.drawImage(image, x, y+getShift(), null);
        }


        private void paintMe(Component c, Graphics g) {

            int right = folderIcon16Size.width - 1;
            int bottom = folderIcon16Size.height - 1;

            // Draw tab top
            g.setColor( MetalLookAndFeel.getPrimaryControlDarkShadow() );
            g.drawLine( right - 5, 3, right, 3 );
            g.drawLine( right - 6, 4, right, 4 );

            // Draw folder front
            g.setColor( MetalLookAndFeel.getPrimaryControl() );
            g.fillRect( 2, 7, 13, 8 );

            // Draw tab bottom
            g.setColor( MetalLookAndFeel.getPrimaryControlShadow() );
            g.drawLine( right - 6, 5, right - 1, 5 );

            // Draw outline
            g.setColor( MetalLookAndFeel.getPrimaryControlInfo() );
            g.drawLine( 0, 6, 0, bottom );            // left side
            g.drawLine( 1, 5, right - 7, 5 );         // first part of top
            g.drawLine( right - 6, 6, right - 1, 6 ); // second part of top
            g.drawLine( right, 5, right, bottom );    // right side
            g.drawLine( 0, bottom, right, bottom );   // bottom

            // Draw highlight
            g.setColor( MetalLookAndFeel.getPrimaryControlHighlight() );
            g.drawLine( 1, 6, 1, bottom - 1 );
            g.drawLine( 1, 6, right - 7, 6 );
            g.drawLine( right - 6, 7, right - 1, 7 );

        }

        /**
         * Returns a shift of the icon.
         *
         * @return a shift of the icon
         */
        public int getShift() { return 0; }

        /**
         * Returns an additional height of the icon.
         *
         * @return an additional height of the icon
         */
        public int getAdditionalHeight() { return 0; }

        public int getIconWidth() { return folderIcon16Size.width; }
        public int getIconHeight() { return folderIcon16Size.height + getAdditionalHeight(); }
    }


    /**
     * <p>
     * <strong>Warning:</strong>
     * Serialized objects of this class will not be compatible with
     * future Swing releases. The current serialization support is
     * appropriate for short term storage or RMI between applications running
     * the same version of Swing.  As of 1.4, support for long term storage
     * of all JavaBeans
     * has been added to the <code>java.beans</code> package.
     * Please see {@link java.beans.XMLEncoder}.
     */
    @SuppressWarnings("serial") // Same-version serialization only
    public static class TreeFolderIcon extends FolderIcon16 {
        /**
         * Constructs a {@code TreeFolderIcon}.
         */
        public TreeFolderIcon() {}

        public int getShift() { return -1; }
        public int getAdditionalHeight() { return 2; }
    }


    private static final Dimension fileIcon16Size = new Dimension( 16, 16 );

    /**
     * <p>
     * <strong>Warning:</strong>
     * Serialized objects of this class will not be compatible with
     * future Swing releases. The current serialization support is
     * appropriate for short term storage or RMI between applications running
     * the same version of Swing.  As of 1.4, support for long term storage
     * of all JavaBeans
     * has been added to the <code>java.beans</code> package.
     * Please see {@link java.beans.XMLEncoder}.
     */
    @SuppressWarnings("serial") // Same-version serialization only
    public static class FileIcon16 implements Icon, Serializable {

        ImageCacher imageCacher;

        /**
         * Constructs a {@code FileIcon16}.
         */
        public FileIcon16() {}

        public void paintIcon(Component c, Graphics g, int x, int y) {
            GraphicsConfiguration gc = c.getGraphicsConfiguration();
            if (imageCacher == null) {
                imageCacher = new ImageCacher();
            }
            Image image = imageCacher.getImage(gc);
            if (image == null) {
                if (gc != null) {
                    image = gc.createCompatibleImage(getIconWidth(),
                                                     getIconHeight(),
                                                     Transparency.BITMASK);
                } else {
                    image = new BufferedImage(getIconWidth(),
                                              getIconHeight(),
                                              BufferedImage.TYPE_INT_ARGB);
                }
                Graphics imageG = image.getGraphics();
                paintMe(c,imageG);
                imageG.dispose();
                imageCacher.cacheImage(image, gc);
            }
            g.drawImage(image, x, y+getShift(), null);
        }

        private void paintMe(Component c, Graphics g) {

                int right = fileIcon16Size.width - 1;
                int bottom = fileIcon16Size.height - 1;

                // Draw fill
                g.setColor( MetalLookAndFeel.getWindowBackground() );
                g.fillRect( 4, 2, 9, 12 );

                // Draw frame
                g.setColor( MetalLookAndFeel.getPrimaryControlInfo() );
                g.drawLine( 2, 0, 2, bottom );                 // left
                g.drawLine( 2, 0, right - 4, 0 );              // top
                g.drawLine( 2, bottom, right - 1, bottom );    // bottom
                g.drawLine( right - 1, 6, right - 1, bottom ); // right
                g.drawLine( right - 6, 2, right - 2, 6 );      // slant 1
                g.drawLine( right - 5, 1, right - 4, 1 );      // part of slant 2
                g.drawLine( right - 3, 2, right - 3, 3 );      // part of slant 2
                g.drawLine( right - 2, 4, right - 2, 5 );      // part of slant 2

                // Draw highlight
                g.setColor( MetalLookAndFeel.getPrimaryControl() );
                g.drawLine( 3, 1, 3, bottom - 1 );                  // left
                g.drawLine( 3, 1, right - 6, 1 );                   // top
                g.drawLine( right - 2, 7, right - 2, bottom - 1 );  // right
                g.drawLine( right - 5, 2, right - 3, 4 );           // slant
                g.drawLine( 3, bottom - 1, right - 2, bottom - 1 ); // bottom

        }

        /**
         * Returns a shift of the icon.
         *
         * @return a shift of the icon
         */
        public int getShift() { return 0; }

        /**
         * Returns an additional height of the icon.
         *
         * @return an additional height of the icon
         */
        public int getAdditionalHeight() { return 0; }

        public int getIconWidth() { return fileIcon16Size.width; }
        public int getIconHeight() { return fileIcon16Size.height + getAdditionalHeight(); }
    }


    /**
     * The class represents a tree leaf icon.
     */
    public static class TreeLeafIcon extends FileIcon16 {
        /**
         * Constructs a {@code TreeLeafIcon}.
         */
        public TreeLeafIcon() {}
        public int getShift() { return 2; }
        public int getAdditionalHeight() { return 4; }
    }


    private static final Dimension treeControlSize = new Dimension( 18, 18 );

    /**
     * <p>
     * <strong>Warning:</strong>
     * Serialized objects of this class will not be compatible with
     * future Swing releases. The current serialization support is
     * appropriate for short term storage or RMI between applications running
     * the same version of Swing.  As of 1.4, support for long term storage
     * of all JavaBeans
     * has been added to the <code>java.beans</code> package.
     * Please see {@link java.beans.XMLEncoder}.
     */
    @SuppressWarnings("serial") // Same-version serialization only
    public static class TreeControlIcon implements Icon, Serializable {

        /**
         * if {@code true} the icon is collapsed.
         * NOTE: This data member should not have been exposed. It's called
         * {@code isLight}, but now it really means {@code isCollapsed}.
         * Since we can't change any APIs... that's life.
         */
        protected boolean isLight;

        /**
         * Constructs an instance of {@code TreeControlIcon}.
         *
         * @param isCollapsed if {@code true} the icon is collapsed
         */
        public TreeControlIcon( boolean isCollapsed ) {
            isLight = isCollapsed;
        }

        ImageCacher imageCacher;

        transient boolean cachedOrientation = true;

        public void paintIcon(Component c, Graphics g, int x, int y) {

            GraphicsConfiguration gc = c.getGraphicsConfiguration();

            if (imageCacher == null) {
                imageCacher = new ImageCacher();
            }
            Image image = imageCacher.getImage(gc);

            if (image == null || cachedOrientation != MetalUtils.isLeftToRight(c)) {
                cachedOrientation = MetalUtils.isLeftToRight(c);
                if (gc != null) {
                    image = gc.createCompatibleImage(getIconWidth(),
                                                     getIconHeight(),
                                                     Transparency.BITMASK);
                } else {
                    image = new BufferedImage(getIconWidth(),
                                              getIconHeight(),
                                              BufferedImage.TYPE_INT_ARGB);
                }
                Graphics imageG = image.getGraphics();
                paintMe(c,imageG,x,y);
                imageG.dispose();
                imageCacher.cacheImage(image, gc);

            }

            if (MetalUtils.isLeftToRight(c)) {
                if (isLight) {    // isCollapsed
                    g.drawImage(image, x+5, y+3, x+18, y+13,
                                       4,3, 17, 13, null);
                }
                else {
                    g.drawImage(image, x+5, y+3, x+18, y+17,
                                       4,3, 17, 17, null);
                }
            }
            else {
                if (isLight) {    // isCollapsed
                    g.drawImage(image, x+3, y+3, x+16, y+13,
                                       4, 3, 17, 13, null);
                }
                else {
                    g.drawImage(image, x+3, y+3, x+16, y+17,
                                       4, 3, 17, 17, null);
                }
            }
        }

        /**
         * Paints the {@code TreeControlIcon}.
         *
         * @param c a component
         * @param g an instance of {@code Graphics}
         * @param x an X coordinate
         * @param y an Y coordinate
         */
        public void paintMe(Component c, Graphics g, int x, int y) {

            g.setColor( MetalLookAndFeel.getPrimaryControlInfo() );

            int xoff = (MetalUtils.isLeftToRight(c)) ? 0 : 4;

            // Draw circle
            g.drawLine( xoff + 4, 6, xoff + 4, 9 );     // left
            g.drawLine( xoff + 5, 5, xoff + 5, 5 );     // top left dot
            g.drawLine( xoff + 6, 4, xoff + 9, 4 );     // top
            g.drawLine( xoff + 10, 5, xoff + 10, 5 );   // top right dot
            g.drawLine( xoff + 11, 6, xoff + 11, 9 );   // right
            g.drawLine( xoff + 10, 10, xoff + 10, 10 ); // bottom right dot
            g.drawLine( xoff + 6, 11, xoff + 9, 11 );   // bottom
            g.drawLine( xoff + 5, 10, xoff + 5, 10 );   // bottom left dot

            // Draw Center Dot
            g.drawLine( xoff + 7, 7, xoff + 8, 7 );
            g.drawLine( xoff + 7, 8, xoff + 8, 8 );

            // Draw Handle
            if ( isLight ) {    // isCollapsed
                if( MetalUtils.isLeftToRight(c) ) {
                    g.drawLine( 12, 7, 15, 7 );
                    g.drawLine( 12, 8, 15, 8 );
                    //  g.setColor( c.getBackground() );
                    //  g.drawLine( 16, 7, 16, 8 );
                }
                else {
                    g.drawLine(4, 7, 7, 7);
                    g.drawLine(4, 8, 7, 8);
                }
            }
            else {
                g.drawLine( xoff + 7, 12, xoff + 7, 15 );
                g.drawLine( xoff + 8, 12, xoff + 8, 15 );
                //      g.setColor( c.getBackground() );
                //      g.drawLine( xoff + 7, 16, xoff + 8, 16 );
            }

            // Draw Fill
            g.setColor( MetalLookAndFeel.getPrimaryControlDarkShadow() );
            g.drawLine( xoff + 5, 6, xoff + 5, 9 );      // left shadow
            g.drawLine( xoff + 6, 5, xoff + 9, 5 );      // top shadow

            g.setColor( MetalLookAndFeel.getPrimaryControlShadow() );
            g.drawLine( xoff + 6, 6, xoff + 6, 6 );      // top left fill
            g.drawLine( xoff + 9, 6, xoff + 9, 6 );      // top right fill
            g.drawLine( xoff + 6, 9, xoff + 6, 9 );      // bottom left fill
            g.drawLine( xoff + 10, 6, xoff + 10, 9 );    // right fill
            g.drawLine( xoff + 6, 10, xoff + 9, 10 );    // bottom fill

            g.setColor( MetalLookAndFeel.getPrimaryControl() );
            g.drawLine( xoff + 6, 7, xoff + 6, 8 );      // left highlight
            g.drawLine( xoff + 7, 6, xoff + 8, 6 );      // top highlight
            g.drawLine( xoff + 9, 7, xoff + 9, 7 );      // right highlight
            g.drawLine( xoff + 7, 9, xoff + 7, 9 );      // bottom highlight

            g.setColor( MetalLookAndFeel.getPrimaryControlHighlight() );
            g.drawLine( xoff + 8, 9, xoff + 9, 9 );
            g.drawLine( xoff + 9, 8, xoff + 9, 8 );
        }

        public int getIconWidth() { return treeControlSize.width; }
        public int getIconHeight() { return treeControlSize.height; }
    }

  //
  // Menu Icons
  //

    private static final Dimension menuArrowIconSize = new Dimension( 4, 8 );
    private static final Dimension menuCheckIconSize = new Dimension( 10, 10 );
    private static final int xOff = 4;

    private static class MenuArrowIcon implements Icon, UIResource, Serializable
    {
        public void paintIcon( Component c, Graphics g, int x, int y )
        {
            JMenuItem b = (JMenuItem) c;
            ButtonModel model = b.getModel();

            g.translate( x, y );

            if ( !model.isEnabled() )
            {
                g.setColor( MetalLookAndFeel.getMenuDisabledForeground() );
            }
            else
            {
                if ( model.isArmed() || ( c instanceof JMenu && model.isSelected() ) )
                {
                    g.setColor( MetalLookAndFeel.getMenuSelectedForeground() );
                }
                else
                {
                    g.setColor( b.getForeground() );
                }
            }
            if (MetalUtils.isLeftToRight(b)) {
                int[] xPoints = {0, 3, 3, 0};
                int[] yPoints = {0, 3, 4, 7};
                g.fillPolygon(xPoints, yPoints, 4);
                g.drawPolygon(xPoints, yPoints, 4);

            } else {
                int[] xPoints = {4, 4, 1, 1};
                int[] yPoints = {0, 7, 4, 3};
                g.fillPolygon(xPoints, yPoints, 4);
                g.drawPolygon(xPoints, yPoints, 4);
            }

            g.translate( -x, -y );
        }

        public int getIconWidth() { return menuArrowIconSize.width; }

        public int getIconHeight() { return menuArrowIconSize.height; }

    } // End class MenuArrowIcon

    private static class MenuItemArrowIcon implements Icon, UIResource, Serializable
    {
        public void paintIcon( Component c, Graphics g, int x, int y )
        {
        }

        public int getIconWidth() { return menuArrowIconSize.width; }

        public int getIconHeight() { return menuArrowIconSize.height; }

    } // End class MenuItemArrowIcon

    private static class CheckBoxMenuItemIcon implements Icon, UIResource, Serializable
    {
        public void paintOceanIcon(Component c, Graphics g, int x, int y) {
            ButtonModel model = ((JMenuItem)c).getModel();
            boolean isSelected = model.isSelected();
            boolean isEnabled = model.isEnabled();
            boolean isPressed = model.isPressed();
            boolean isArmed = model.isArmed();

            g.translate(x, y);
            if (isEnabled) {
                MetalUtils.drawGradient(c, g, "CheckBoxMenuItem.gradient",
                                        1, 1, 7, 7, true);
                if (isPressed || isArmed) {
                    g.setColor(MetalLookAndFeel.getControlInfo());
                    g.drawLine( 0, 0, 8, 0 );
                    g.drawLine( 0, 0, 0, 8 );
                    g.drawLine( 8, 2, 8, 8 );
                    g.drawLine( 2, 8, 8, 8 );

                    g.setColor(MetalLookAndFeel.getPrimaryControl());
                    g.drawLine( 9, 1, 9, 9 );
                    g.drawLine( 1, 9, 9, 9 );
                }
                else {
                    g.setColor(MetalLookAndFeel.getControlDarkShadow());
                    g.drawLine( 0, 0, 8, 0 );
                    g.drawLine( 0, 0, 0, 8 );
                    g.drawLine( 8, 2, 8, 8 );
                    g.drawLine( 2, 8, 8, 8 );

                    g.setColor(MetalLookAndFeel.getControlHighlight());
                    g.drawLine( 9, 1, 9, 9 );
                    g.drawLine( 1, 9, 9, 9 );
                }
            }
            else {
                g.setColor(MetalLookAndFeel.getMenuDisabledForeground());
                g.drawRect( 0, 0, 8, 8 );
            }
            if (isSelected) {
                if (isEnabled) {
                    if (isArmed || ( c instanceof JMenu && isSelected)) {
                        g.setColor(
                            MetalLookAndFeel.getMenuSelectedForeground() );
                    }
                    else {
                         g.setColor(MetalLookAndFeel.getControlInfo());
                    }
                }
                else {
                    g.setColor( MetalLookAndFeel.getMenuDisabledForeground());
                }

                drawCheck(g);
            }
            g.translate( -x, -y );
        }

        public void paintIcon( Component c, Graphics g, int x, int y )
        {
            if (MetalLookAndFeel.usingOcean()) {
                paintOceanIcon(c, g, x, y);
                return;
            }
            JMenuItem b = (JMenuItem) c;
            ButtonModel model = b.getModel();

            boolean isSelected = model.isSelected();
            boolean isEnabled = model.isEnabled();
            boolean isPressed = model.isPressed();
            boolean isArmed = model.isArmed();

            g.translate( x, y );

            if ( isEnabled )
            {
                if ( isPressed || isArmed )
                {
                    g.setColor( MetalLookAndFeel.getControlInfo()  );
                    g.drawLine( 0, 0, 8, 0 );
                    g.drawLine( 0, 0, 0, 8 );
                    g.drawLine( 8, 2, 8, 8 );
                    g.drawLine( 2, 8, 8, 8 );

                    g.setColor( MetalLookAndFeel.getPrimaryControl()  );
                    g.drawLine( 1, 1, 7, 1 );
                    g.drawLine( 1, 1, 1, 7 );
                    g.drawLine( 9, 1, 9, 9 );
                    g.drawLine( 1, 9, 9, 9 );
                }
                else
                {
                    g.setColor( MetalLookAndFeel.getControlDarkShadow()  );
                    g.drawLine( 0, 0, 8, 0 );
                    g.drawLine( 0, 0, 0, 8 );
                    g.drawLine( 8, 2, 8, 8 );
                    g.drawLine( 2, 8, 8, 8 );

                    g.setColor( MetalLookAndFeel.getControlHighlight()  );
                    g.drawLine( 1, 1, 7, 1 );
                    g.drawLine( 1, 1, 1, 7 );
                    g.drawLine( 9, 1, 9, 9 );
                    g.drawLine( 1, 9, 9, 9 );
                }
            }
            else
            {
                g.setColor( MetalLookAndFeel.getMenuDisabledForeground()  );
                g.drawRect( 0, 0, 8, 8 );
            }

            if ( isSelected )
            {
                if ( isEnabled )
                {
                    if ( model.isArmed() || ( c instanceof JMenu && model.isSelected() ) )
                    {
                        g.setColor( MetalLookAndFeel.getMenuSelectedForeground() );
                    }
                    else
                    {
                        g.setColor( b.getForeground() );
                    }
                }
                else
                {
                    g.setColor( MetalLookAndFeel.getMenuDisabledForeground()  );
                }

                drawCheck(g);
            }

            g.translate( -x, -y );
        }

        private void drawCheck(Graphics g) {
            int[] xPoints = {2, 3, 3, 8, 9, 3, 2};
            int[] yPoints = {2, 2, 5, 0, 0, 6, 6};
            g.drawPolygon(xPoints, yPoints, 7);
        }

        public int getIconWidth() { return menuCheckIconSize.width; }

        public int getIconHeight() { return menuCheckIconSize.height; }

    }  // End class CheckBoxMenuItemIcon

    private static class RadioButtonMenuItemIcon implements Icon, UIResource, Serializable
    {
        public void paintOceanIcon(Component c, Graphics g, int x, int y) {
            ButtonModel model = ((JMenuItem)c).getModel();
            boolean isSelected = model.isSelected();
            boolean isEnabled = model.isEnabled();
            boolean isPressed = model.isPressed();
            boolean isArmed = model.isArmed();

            g.translate( x, y );

            if (isEnabled) {
                MetalUtils.drawGradient(c, g, "RadioButtonMenuItem.gradient",
                                        1, 1, 7, 7, true);
                if (isPressed || isArmed) {
                    g.setColor(MetalLookAndFeel.getPrimaryControl());
                }
                else {
                    g.setColor(MetalLookAndFeel.getControlHighlight());
                }

                g.drawArc(-1, -1, 10, 10, 245, 140);

                if (isPressed || isArmed) {
                    g.setColor(MetalLookAndFeel.getControlInfo());
                }
                else {
                    g.setColor(MetalLookAndFeel.getControlDarkShadow());
                }
            }
            else {
                g.setColor( MetalLookAndFeel.getMenuDisabledForeground()  );
            }

            g.drawOval(0, 0, 8, 8);

            if (isSelected) {
                if (isEnabled) {
                    if (isArmed || (c instanceof JMenu && model.isSelected())){
                        g.setColor(MetalLookAndFeel.
                                   getMenuSelectedForeground() );
                    }
                    else {
                        g.setColor(MetalLookAndFeel.getControlInfo());
                    }
                }
                else {
                    g.setColor(MetalLookAndFeel.getMenuDisabledForeground());
                }

                g.fillOval(2, 2, 4, 4);
                g.drawOval(2, 2, 4, 4);
            }

            g.translate( -x, -y );
        }

        public void paintIcon( Component c, Graphics g, int x, int y )
        {

            Object aaHint = getAndSetAntialisingHintForScaledGraphics(g);

            if (MetalLookAndFeel.usingOcean()) {
                paintOceanIcon(c, g, x, y);
                setAntialiasingHintForScaledGraphics(g, aaHint);
                return;
            }
            JMenuItem b = (JMenuItem) c;
            ButtonModel model = b.getModel();

            boolean isSelected = model.isSelected();
            boolean isEnabled = model.isEnabled();
            boolean isPressed = model.isPressed();
            boolean isArmed = model.isArmed();

            g.translate( x, y );

            if ( isEnabled )
            {
                if ( isPressed || isArmed )
                {
                    g.setColor( MetalLookAndFeel.getPrimaryControl()  );
                    g.drawOval(1, 1, 8, 8);

                    g.setColor( MetalLookAndFeel.getControlInfo()  );
                    g.drawOval(0, 0, 8, 8);
                }
                else
                {
                    g.setColor( MetalLookAndFeel.getControlHighlight()  );
                    g.drawOval(1, 1, 8, 8);

                    g.setColor( MetalLookAndFeel.getControlDarkShadow()  );
                    g.drawOval(0, 0, 8, 8);
                }
            }
            else
            {
                g.setColor( MetalLookAndFeel.getMenuDisabledForeground()  );
                g.drawOval(0, 0, 8, 8);
            }

            if ( isSelected )
            {
                if ( isEnabled )
                {
                    if ( model.isArmed() || ( c instanceof JMenu && model.isSelected() ) )
                    {
                        g.setColor( MetalLookAndFeel.getMenuSelectedForeground() );
                    }
                    else
                    {
                        g.setColor( b.getForeground() );
                    }
                }
                else
                {
                    g.setColor( MetalLookAndFeel.getMenuDisabledForeground()  );
                }

                g.fillOval(2, 2, 4, 4);
                g.drawOval(2, 2, 4, 4);
            }

            g.translate( -x, -y );
            setAntialiasingHintForScaledGraphics(g, aaHint);
        }

        public int getIconWidth() { return menuCheckIconSize.width; }

        public int getIconHeight() { return menuCheckIconSize.height; }

    }  // End class RadioButtonMenuItemIcon

private static class VerticalSliderThumbIcon implements Icon, Serializable, UIResource {
    protected static MetalBumps controlBumps;
    protected static MetalBumps primaryBumps;

    public VerticalSliderThumbIcon() {
        controlBumps = new MetalBumps( 6, 10,
                                MetalLookAndFeel.getControlHighlight(),
                                MetalLookAndFeel.getControlInfo(),
                                MetalLookAndFeel.getControl() );
        primaryBumps = new MetalBumps( 6, 10,
                                MetalLookAndFeel.getPrimaryControl(),
                                MetalLookAndFeel.getPrimaryControlDarkShadow(),
                                MetalLookAndFeel.getPrimaryControlShadow() );
    }

    public void paintIcon( Component c, Graphics g, int x, int y ) {
        boolean leftToRight = MetalUtils.isLeftToRight(c);

        g.translate( x, y );
        Rectangle clip = g.getClipBounds();
        g.clipRect(0, 0, getIconWidth(), getIconHeight());

        // Draw the frame
        if ( c.hasFocus() ) {
            g.setColor( MetalLookAndFeel.getPrimaryControlInfo() );
        }
        else {
            g.setColor( c.isEnabled() ? MetalLookAndFeel.getPrimaryControlInfo() :
                                             MetalLookAndFeel.getControlDarkShadow() );
        }

        if (leftToRight) {
            g.drawLine(  1,0  ,  8,0  ); // top
            g.drawLine(  0,1  ,  0,13 ); // left
            g.drawLine(  1,14 ,  8,14 ); // bottom
            g.drawLine(  9,1  , 15,7  ); // top slant
            g.drawLine(  9,13 , 15,7  ); // bottom slant
        }
        else {
            g.drawLine(  7,0  , 14,0  ); // top
            g.drawLine( 15,1  , 15,13 ); // right
            g.drawLine(  7,14 , 14,14 ); // bottom
            g.drawLine(  0,7  ,  6,1  ); // top slant
            g.drawLine(  0,7  ,  6,13 ); // bottom slant
        }

        // Fill in the background
        if ( c.hasFocus() ) {
            g.setColor( c.getForeground() );
        }
        else {
            g.setColor( MetalLookAndFeel.getControl() );
        }

        if (leftToRight) {
            g.fillRect(  1,1 ,  8,13 );

            g.drawLine(  9,2 ,  9,12 );
            g.drawLine( 10,3 , 10,11 );
            g.drawLine( 11,4 , 11,10 );
            g.drawLine( 12,5 , 12,9 );
            g.drawLine( 13,6 , 13,8 );
            g.drawLine( 14,7 , 14,7 );
        }
        else {
            g.fillRect(  7,1,   8,13 );

            g.drawLine(  6,3 ,  6,12 );
            g.drawLine(  5,4 ,  5,11 );
            g.drawLine(  4,5 ,  4,10 );
            g.drawLine(  3,6 ,  3,9 );
            g.drawLine(  2,7 ,  2,8 );
        }

        // Draw the bumps
        int offset = (leftToRight) ? 2 : 8;
        if ( c.isEnabled() ) {
            if ( c.hasFocus() ) {
                primaryBumps.paintIcon( c, g, offset, 2 );
            }
            else {
                controlBumps.paintIcon( c, g, offset, 2 );
            }
        }

        // Draw the highlight
        if ( c.isEnabled() ) {
            g.setColor( c.hasFocus() ? MetalLookAndFeel.getPrimaryControl()
                        : MetalLookAndFeel.getControlHighlight() );
            if (leftToRight) {
                g.drawLine( 1, 1, 8, 1 );
                g.drawLine( 1, 1, 1, 13 );
            }
            else {
                g.drawLine(  8,1  , 14,1  ); // top
                g.drawLine(  1,7  ,  7,1  ); // top slant
            }
        }

        g.setClip(clip);
        g.translate( -x, -y );
    }

    public int getIconWidth() {
        return 16;
    }

    public int getIconHeight() {
        return 15;
    }
}

private static class HorizontalSliderThumbIcon implements Icon, Serializable, UIResource {
    protected static MetalBumps controlBumps;
    protected static MetalBumps primaryBumps;

    public HorizontalSliderThumbIcon() {
        controlBumps = new MetalBumps( 10, 6,
                                MetalLookAndFeel.getControlHighlight(),
                                MetalLookAndFeel.getControlInfo(),
                                MetalLookAndFeel.getControl() );
        primaryBumps = new MetalBumps( 10, 6,
                                MetalLookAndFeel.getPrimaryControl(),
                                MetalLookAndFeel.getPrimaryControlDarkShadow(),
                                MetalLookAndFeel.getPrimaryControlShadow() );
    }

    public void paintIcon( Component c, Graphics g, int x, int y ) {
        g.translate( x, y );
        Rectangle clip = g.getClipBounds();
        g.clipRect(0, 0, getIconWidth(), getIconHeight());

        // Draw the frame
        if ( c.hasFocus() ) {
            g.setColor( MetalLookAndFeel.getPrimaryControlInfo() );
        }
        else {
            g.setColor( c.isEnabled() ? MetalLookAndFeel.getPrimaryControlInfo() :
                                             MetalLookAndFeel.getControlDarkShadow() );
        }

        g.drawLine(  1,0  , 13,0 );  // top
        g.drawLine(  0,1  ,  0,8 );  // left
        g.drawLine( 14,1  , 14,8 );  // right
        g.drawLine(  1,9  ,  7,15 ); // left slant
        g.drawLine(  7,15 , 14,8 );  // right slant

        // Fill in the background
        if ( c.hasFocus() ) {
            g.setColor( c.getForeground() );
        }
        else {
            g.setColor( MetalLookAndFeel.getControl() );
        }
        g.fillRect( 1,1, 13, 8 );

        g.drawLine( 2,9  , 12,9 );
        g.drawLine( 3,10 , 11,10 );
        g.drawLine( 4,11 , 10,11 );
        g.drawLine( 5,12 ,  9,12 );
        g.drawLine( 6,13 ,  8,13 );
        g.drawLine( 7,14 ,  7,14 );

        // Draw the bumps
        if ( c.isEnabled() ) {
            if ( c.hasFocus() ) {
                primaryBumps.paintIcon( c, g, 2, 2 );
            }
            else {
                controlBumps.paintIcon( c, g, 2, 2 );
            }
        }

        // Draw the highlight
        if ( c.isEnabled() ) {
            g.setColor( c.hasFocus() ? MetalLookAndFeel.getPrimaryControl()
                        : MetalLookAndFeel.getControlHighlight() );
            g.drawLine( 1, 1, 13, 1 );
            g.drawLine( 1, 1, 1, 8 );
        }

        g.setClip(clip);
        g.translate( -x, -y );
    }

    public int getIconWidth() {
        return 15;
    }

    public int getIconHeight() {
        return 16;
    }
}

    private static class OceanVerticalSliderThumbIcon extends CachedPainter
                              implements Icon, Serializable, UIResource {
        // Used for clipping when the orientation is left to right
        private static Polygon LTR_THUMB_SHAPE;
        // Used for clipping when the orientation is right to left
        private static Polygon RTL_THUMB_SHAPE;

        static {
            LTR_THUMB_SHAPE = new Polygon(new int[] { 0,  8, 15,  8,  0},
                                          new int[] { 0,  0,  7, 14, 14 }, 5);
            RTL_THUMB_SHAPE = new Polygon(new int[] { 15, 15,  7,  0,  7},
                                          new int[] {  0, 14, 14,  7,  0}, 5);
        }

        OceanVerticalSliderThumbIcon() {
            super(3);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (!(g instanceof Graphics2D)) {
                return;
            }
            paint(c, g, x, y, getIconWidth(), getIconHeight(),
                  MetalUtils.isLeftToRight(c), c.hasFocus(), c.isEnabled(),
                  MetalLookAndFeel.getCurrentTheme());
        }

        protected void paintToImage(Component c, Image image, Graphics g2,
                                    int w, int h, Object[] args) {
            Graphics2D g = (Graphics2D)g2;
            boolean leftToRight = ((Boolean)args[0]).booleanValue();
            boolean hasFocus = ((Boolean)args[1]).booleanValue();
            boolean enabled = ((Boolean)args[2]).booleanValue();

            Rectangle clip = g.getClipBounds();
            if (leftToRight) {
                g.clip(LTR_THUMB_SHAPE);
            }
            else {
                g.clip(RTL_THUMB_SHAPE);
            }
            if (!enabled) {
                g.setColor(MetalLookAndFeel.getControl());
                g.fillRect(1, 1, 14, 14);
            }
            else if (hasFocus) {
                MetalUtils.drawGradient(c, g, "Slider.focusGradient",
                                        1, 1, 14, 14, false);
            }
            else {
                MetalUtils.drawGradient(c, g, "Slider.gradient",
                                        1, 1, 14, 14, false);
            }
            g.setClip(clip);

            // Draw the frame
            if (hasFocus) {
                g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            }
            else {
                g.setColor(enabled ? MetalLookAndFeel.getPrimaryControlInfo() :
                           MetalLookAndFeel.getControlDarkShadow());
            }

            if (leftToRight) {
                g.drawLine(  1,0  ,  8,0  ); // top
                g.drawLine(  0,1  ,  0,13 ); // left
                g.drawLine(  1,14 ,  8,14 ); // bottom
                g.drawLine(  9,1  , 15,7  ); // top slant
                g.drawLine(  9,13 , 15,7  ); // bottom slant
            }
            else {
                g.drawLine(  7,0  , 14,0  ); // top
                g.drawLine( 15,1  , 15,13 ); // right
                g.drawLine(  7,14 , 14,14 ); // bottom
                g.drawLine(  0,7  ,  6,1  ); // top slant
                g.drawLine(  0,7  ,  6,13 ); // bottom slant
            }

            if (hasFocus && enabled) {
                // Inner line.
                g.setColor(MetalLookAndFeel.getPrimaryControl());
                if (leftToRight) {
                    g.drawLine(  1,1  ,  8,1  ); // top
                    g.drawLine(  1,1  ,  1,13 ); // left
                    g.drawLine(  1,13 ,  8,13 ); // bottom
                    g.drawLine(  9,2  , 14,7  ); // top slant
                    g.drawLine(  9,12 , 14,7  ); // bottom slant
                }
                else {
                    g.drawLine(  7,1  , 14,1  ); // top
                    g.drawLine( 14,1  , 14,13 ); // right
                    g.drawLine(  7,13 , 14,13 ); // bottom
                    g.drawLine(  1,7  ,  7,1  ); // top slant
                    g.drawLine(  1,7  ,  7,13 ); // bottom slant
                }
            }
        }

        public int getIconWidth() {
            return 16;
        }

        public int getIconHeight() {
            return 15;
        }

        protected Image createImage(Component c, int w, int h,
                                    GraphicsConfiguration config,
                                    Object[] args) {
            if (config == null) {
                return new BufferedImage(w, h,BufferedImage.TYPE_INT_ARGB);
            }
            return config.createCompatibleImage(
                                w, h, Transparency.BITMASK);
        }
    }


    private static class OceanHorizontalSliderThumbIcon extends CachedPainter
                              implements Icon, Serializable, UIResource {
        // Used for clipping
        private static Polygon THUMB_SHAPE;

        static {
            THUMB_SHAPE = new Polygon(new int[] { 0, 14, 14,  7,  0 },
                                      new int[] { 0,  0,  8, 15,  8 }, 5);
        }

        OceanHorizontalSliderThumbIcon() {
            super(3);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (!(g instanceof Graphics2D)) {
                return;
            }
            paint(c, g, x, y, getIconWidth(), getIconHeight(),
                  c.hasFocus(), c.isEnabled(),
                  MetalLookAndFeel.getCurrentTheme());
        }


        protected Image createImage(Component c, int w, int h,
                                    GraphicsConfiguration config,
                                    Object[] args) {
            if (config == null) {
                return new BufferedImage(w, h,BufferedImage.TYPE_INT_ARGB);
            }
            return config.createCompatibleImage(
                                w, h, Transparency.BITMASK);
        }

        protected void paintToImage(Component c, Image image, Graphics g2,
                                    int w, int h, Object[] args) {
            Graphics2D g = (Graphics2D)g2;
            boolean hasFocus = ((Boolean)args[0]).booleanValue();
            boolean enabled = ((Boolean)args[1]).booleanValue();

            // Fill in the background
            Rectangle clip = g.getClipBounds();
            g.clip(THUMB_SHAPE);
            if (!enabled) {
                g.setColor(MetalLookAndFeel.getControl());
                g.fillRect(1, 1, 13, 14);
            }
            else if (hasFocus) {
                MetalUtils.drawGradient(c, g, "Slider.focusGradient",
                                        1, 1, 13, 14, true);
            }
            else {
                MetalUtils.drawGradient(c, g, "Slider.gradient",
                                        1, 1, 13, 14, true);
            }
            g.setClip(clip);

            // Draw the frame
            if (hasFocus) {
                g.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            }
            else {
                g.setColor(enabled ? MetalLookAndFeel.getPrimaryControlInfo() :
                           MetalLookAndFeel.getControlDarkShadow());
            }

            g.drawLine(  1,0  , 13,0 );  // top
            g.drawLine(  0,1  ,  0,8 );  // left
            g.drawLine( 14,1  , 14,8 );  // right
            g.drawLine(  1,9  ,  7,15 ); // left slant
            g.drawLine(  7,15 , 14,8 );  // right slant

            if (hasFocus && enabled) {
                // Inner line.
                g.setColor(MetalLookAndFeel.getPrimaryControl());
                g.fillRect(1, 1, 13, 1);
                g.fillRect(1, 2, 1, 7);
                g.fillRect(13, 2, 1, 7);
                g.drawLine(2, 9, 7, 14);
                g.drawLine(8, 13, 12, 9);
            }
        }

        public int getIconWidth() {
            return 15;
        }

        public int getIconHeight() {
            return 16;
        }
    }
}
