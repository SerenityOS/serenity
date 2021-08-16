/*
 * Copyright (c) 2002, 2014, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.plaf.synth;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.net.*;
import javax.swing.*;
import sun.awt.AppContext;
import sun.swing.plaf.synth.Paint9Painter;

/**
 * ImagePainter fills in the specified region using an Image. The Image
 * is split into 9 segments: north, north east, east, south east, south,
 * south west, west, north west and the center. The corners are defined
 * by way of an insets, and the remaining regions are either tiled or
 * scaled to fit.
 *
 * @author Scott Violet
 */
class ImagePainter extends SynthPainter {
    private static final StringBuffer CACHE_KEY =
                               new StringBuffer("SynthCacheKey");

    private Image image;
    private Insets sInsets;
    private Insets dInsets;
    private URL path;
    private boolean tiles;
    private boolean paintCenter;
    private Paint9Painter imageCache;
    private boolean center;

    private static Paint9Painter getPaint9Painter() {
        // A SynthPainter is created per <imagePainter>.  We want the
        // cache to be shared by all, and we don't use a static because we
        // don't want it to persist between look and feels.  For that reason
        // we use a AppContext specific Paint9Painter.  It's backed via
        // a WeakRef so that it can go away if the look and feel changes.
        synchronized(CACHE_KEY) {
            @SuppressWarnings("unchecked")
            WeakReference<Paint9Painter> cacheRef =
                     (WeakReference<Paint9Painter>)AppContext.getAppContext().
                     get(CACHE_KEY);
            Paint9Painter painter;
            if (cacheRef == null || (painter = cacheRef.get()) == null) {
                painter = new Paint9Painter(30);
                cacheRef = new WeakReference<Paint9Painter>(painter);
                AppContext.getAppContext().put(CACHE_KEY, cacheRef);
            }
            return painter;
        }
    }

    ImagePainter(boolean tiles, boolean paintCenter,
                 Insets sourceInsets, Insets destinationInsets, URL path,
                 boolean center) {
        if (sourceInsets != null) {
            this.sInsets = (Insets)sourceInsets.clone();
        }
        if (destinationInsets == null) {
            dInsets = sInsets;
        }
        else {
            this.dInsets = (Insets)destinationInsets.clone();
        }
        this.tiles = tiles;
        this.paintCenter = paintCenter;
        this.imageCache = getPaint9Painter();
        this.path = path;
        this.center = center;
    }

    public boolean getTiles() {
        return tiles;
    }

    public boolean getPaintsCenter() {
        return paintCenter;
    }

    public boolean getCenter() {
        return center;
    }

    public Insets getInsets(Insets insets) {
        if (insets == null) {
            return (Insets)this.dInsets.clone();
        }
        insets.left = this.dInsets.left;
        insets.right = this.dInsets.right;
        insets.top = this.dInsets.top;
        insets.bottom = this.dInsets.bottom;
        return insets;
    }

    public Image getImage() {
        if (image == null) {
            image = new ImageIcon(path, null).getImage();
        }
        return image;
    }

    private void paint(SynthContext context, Graphics g, int x, int y, int w,
                       int h) {
        Image image = getImage();
        if (Paint9Painter.validImage(image)) {
            Paint9Painter.PaintType type;
            if (getCenter()) {
                type = Paint9Painter.PaintType.CENTER;
            }
            else if (!getTiles()) {
                type = Paint9Painter.PaintType.PAINT9_STRETCH;
            }
            else {
                type = Paint9Painter.PaintType.PAINT9_TILE;
            }
            int mask = Paint9Painter.PAINT_ALL;
            if (!getCenter() && !getPaintsCenter()) {
                mask |= Paint9Painter.PAINT_CENTER;
            }
            imageCache.paint(context.getComponent(), g, x, y, w, h,
                             image, sInsets, dInsets, type,
                             mask);
        }
    }


    // SynthPainter
    public void paintArrowButtonBackground(SynthContext context,
                                           Graphics g, int x, int y,
                                           int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintArrowButtonBorder(SynthContext context,
                                       Graphics g, int x, int y,
                                       int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintArrowButtonForeground(SynthContext context,
                                           Graphics g, int x, int y,
                                           int w, int h,
                                           int direction) {
        paint(context, g, x, y, w, h);
    }

    // BUTTON
    public void paintButtonBackground(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintButtonBorder(SynthContext context,
                                  Graphics g, int x, int y,
                                  int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // CHECK_BOX_MENU_ITEM
    public void paintCheckBoxMenuItemBackground(SynthContext context,
                                                Graphics g, int x, int y,
                                                int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintCheckBoxMenuItemBorder(SynthContext context,
                                            Graphics g, int x, int y,
                                            int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // CHECK_BOX
    public void paintCheckBoxBackground(SynthContext context,
                                        Graphics g, int x, int y,
                                        int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintCheckBoxBorder(SynthContext context,
                                    Graphics g, int x, int y,
                                    int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // COLOR_CHOOSER
    public void paintColorChooserBackground(SynthContext context,
                                            Graphics g, int x, int y,
                                            int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintColorChooserBorder(SynthContext context,
                                        Graphics g, int x, int y,
                                        int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // COMBO_BOX
    public void paintComboBoxBackground(SynthContext context,
                                        Graphics g, int x, int y,
                                        int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintComboBoxBorder(SynthContext context,
                                        Graphics g, int x, int y,
                                        int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // DESKTOP_ICON
    public void paintDesktopIconBackground(SynthContext context,
                                        Graphics g, int x, int y,
                                        int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintDesktopIconBorder(SynthContext context,
                                           Graphics g, int x, int y,
                                           int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // DESKTOP_PANE
    public void paintDesktopPaneBackground(SynthContext context,
                                           Graphics g, int x, int y,
                                           int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintDesktopPaneBorder(SynthContext context,
                                       Graphics g, int x, int y,
                                       int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // EDITOR_PANE
    public void paintEditorPaneBackground(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintEditorPaneBorder(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // FILE_CHOOSER
    public void paintFileChooserBackground(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintFileChooserBorder(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // FORMATTED_TEXT_FIELD
    public void paintFormattedTextFieldBackground(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintFormattedTextFieldBorder(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // INTERNAL_FRAME_TITLE_PANE
    public void paintInternalFrameTitlePaneBackground(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintInternalFrameTitlePaneBorder(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // INTERNAL_FRAME
    public void paintInternalFrameBackground(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintInternalFrameBorder(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // LABEL
    public void paintLabelBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintLabelBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // LIST
    public void paintListBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintListBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // MENU_BAR
    public void paintMenuBarBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintMenuBarBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // MENU_ITEM
    public void paintMenuItemBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintMenuItemBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // MENU
    public void paintMenuBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintMenuBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // OPTION_PANE
    public void paintOptionPaneBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintOptionPaneBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // PANEL
    public void paintPanelBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintPanelBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // PANEL
    public void paintPasswordFieldBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintPasswordFieldBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // POPUP_MENU
    public void paintPopupMenuBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintPopupMenuBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // PROGRESS_BAR
    public void paintProgressBarBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintProgressBarBackground(SynthContext context,
                                           Graphics g, int x, int y,
                                           int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintProgressBarBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintProgressBarBorder(SynthContext context,
                                       Graphics g, int x, int y,
                                       int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintProgressBarForeground(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    // RADIO_BUTTON_MENU_ITEM
    public void paintRadioButtonMenuItemBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintRadioButtonMenuItemBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // RADIO_BUTTON
    public void paintRadioButtonBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintRadioButtonBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // ROOT_PANE
    public void paintRootPaneBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintRootPaneBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // SCROLL_BAR
    public void paintScrollBarBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintScrollBarBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintScrollBarBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintScrollBarBorder(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    // SCROLL_BAR_THUMB
    public void paintScrollBarThumbBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintScrollBarThumbBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    // SCROLL_BAR_TRACK
    public void paintScrollBarTrackBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintScrollBarTrackBackground(SynthContext context,
                                              Graphics g, int x, int y,
                                              int w, int h, int orientation) {
         paint(context, g, x, y, w, h);
     }

    public void paintScrollBarTrackBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintScrollBarTrackBorder(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    // SCROLL_PANE
    public void paintScrollPaneBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintScrollPaneBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // SEPARATOR
    public void paintSeparatorBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintSeparatorBackground(SynthContext context,
                                         Graphics g, int x, int y,
                                         int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintSeparatorBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintSeparatorBorder(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintSeparatorForeground(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    // SLIDER
    public void paintSliderBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintSliderBackground(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintSliderBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintSliderBorder(SynthContext context,
                                  Graphics g, int x, int y,
                                  int w, int h, int orientation) {
         paint(context, g, x, y, w, h);
     }

    // SLIDER_THUMB
    public void paintSliderThumbBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintSliderThumbBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    // SLIDER_TRACK
    public void paintSliderTrackBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintSliderTrackBackground(SynthContext context,
                                           Graphics g, int x, int y,
                                           int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintSliderTrackBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }


    public void paintSliderTrackBorder(SynthContext context,
                                       Graphics g, int x, int y,
                                       int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    // SPINNER
    public void paintSpinnerBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintSpinnerBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // SPLIT_PANE_DIVIDER
    public void paintSplitPaneDividerBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintSplitPaneDividerBackground(SynthContext context,
                                                Graphics g, int x, int y,
                                                int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintSplitPaneDividerForeground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintSplitPaneDragDivider(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    // SPLIT_PANE
    public void paintSplitPaneBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintSplitPaneBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // TABBED_PANE
    public void paintTabbedPaneBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTabbedPaneBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // TABBED_PANE_TAB_AREA
    public void paintTabbedPaneTabAreaBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTabbedPaneTabAreaBackground(SynthContext context,
                                                 Graphics g, int x, int y,
                                                 int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintTabbedPaneTabAreaBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTabbedPaneTabAreaBorder(SynthContext context,
                                             Graphics g, int x, int y,
                                             int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    // TABBED_PANE_TAB
    public void paintTabbedPaneTabBackground(SynthContext context, Graphics g,
                                         int x, int y, int w, int h,
                                         int tabIndex) {
        paint(context, g, x, y, w, h);
    }

    public void paintTabbedPaneTabBackground(SynthContext context, Graphics g,
                                             int x, int y, int w, int h,
                                             int tabIndex, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintTabbedPaneTabBorder(SynthContext context, Graphics g,
                                         int x, int y, int w, int h,
                                         int tabIndex) {
        paint(context, g, x, y, w, h);
    }

    public void paintTabbedPaneTabBorder(SynthContext context, Graphics g,
                                         int x, int y, int w, int h,
                                         int tabIndex, int orientation) {
        paint(context, g, x, y, w, h);
    }

    // TABBED_PANE_CONTENT
    public void paintTabbedPaneContentBackground(SynthContext context,
                                         Graphics g, int x, int y, int w,
                                         int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTabbedPaneContentBorder(SynthContext context, Graphics g,
                                         int x, int y, int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // TABLE_HEADER
    public void paintTableHeaderBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTableHeaderBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // TABLE
    public void paintTableBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTableBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // TEXT_AREA
    public void paintTextAreaBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTextAreaBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // TEXT_PANE
    public void paintTextPaneBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTextPaneBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // TEXT_FIELD
    public void paintTextFieldBackground(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTextFieldBorder(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // TOGGLE_BUTTON
    public void paintToggleButtonBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintToggleButtonBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // TOOL_BAR
    public void paintToolBarBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolBarBackground(SynthContext context,
                                       Graphics g, int x, int y,
                                       int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolBarBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolBarBorder(SynthContext context,
                                   Graphics g, int x, int y,
                                   int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    // TOOL_BAR_CONTENT
    public void paintToolBarContentBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolBarContentBackground(SynthContext context,
                                              Graphics g, int x, int y,
                                              int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolBarContentBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolBarContentBorder(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    // TOOL_DRAG_WINDOW
    public void paintToolBarDragWindowBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolBarDragWindowBackground(SynthContext context,
                                                 Graphics g, int x, int y,
                                                 int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolBarDragWindowBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolBarDragWindowBorder(SynthContext context,
                                             Graphics g, int x, int y,
                                             int w, int h, int orientation) {
        paint(context, g, x, y, w, h);
    }

    // TOOL_TIP
    public void paintToolTipBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintToolTipBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // TREE
    public void paintTreeBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTreeBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // TREE_CELL
    public void paintTreeCellBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTreeCellBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintTreeCellFocus(SynthContext context,
                                   Graphics g, int x, int y,
                                   int w, int h) {
        paint(context, g, x, y, w, h);
    }

    // VIEWPORT
    public void paintViewportBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
        paint(context, g, x, y, w, h);
    }

    public void paintViewportBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
        paint(context, g, x, y, w, h);
    }
}
