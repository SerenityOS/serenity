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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.security.PrivilegedAction;

import javax.swing.*;
import javax.swing.plaf.*;

import sun.lwawt.macosx.LWCToolkit;
import apple.laf.JRSUIConstants.AlignmentHorizontal;
import apple.laf.JRSUIConstants.AlignmentVertical;
import apple.laf.JRSUIConstants.Direction;
import apple.laf.JRSUIConstants.State;
import apple.laf.JRSUIConstants.Widget;
import apple.laf.*;

import com.apple.eio.FileManager;
import com.apple.laf.AquaIcon.InvertableIcon;
import com.apple.laf.AquaIcon.JRSUIControlSpec;
import com.apple.laf.AquaIcon.SystemIcon;
import com.apple.laf.AquaUtils.RecyclableObject;
import com.apple.laf.AquaUtils.RecyclableSingleton;
import sun.awt.image.MultiResolutionCachedImage;
import sun.lwawt.macosx.CImage;

public class AquaImageFactory {
    public static IconUIResource getConfirmImageIcon() {
        // public, because UIDefaults.ProxyLazyValue uses reflection to get this value

        return new IconUIResource(new AquaIcon.CachingScalingIcon(kAlertIconSize, kAlertIconSize) {
            Image createImage() {
                return getGenericJavaIcon();
            }
        });
    }

    public static IconUIResource getCautionImageIcon() {
        // public, because UIDefaults.ProxyLazyValue uses reflection to get this value
        return getAppIconCompositedOn(AquaIcon.SystemIcon.getCautionIcon());
    }

    public static IconUIResource getStopImageIcon() {
        // public, because UIDefaults.ProxyLazyValue uses reflection to get this value
        return getAppIconCompositedOn(AquaIcon.SystemIcon.getStopIcon());
    }

    public static IconUIResource getLockImageIcon() {
        // public, because UIDefaults.ProxyLazyValue uses reflection to get this value
        if (JRSUIUtils.Images.shouldUseLegacySecurityUIPath()) {
            final Image lockIcon = CImage.createImageFromFile("/System/Library/CoreServices/SecurityAgent.app/Contents/Resources/Security.icns", kAlertIconSize, kAlertIconSize);
            return getAppIconCompositedOn(lockIcon);
        }

        final Image lockIcon = Toolkit.getDefaultToolkit().getImage("NSImage://NSSecurity");
        return getAppIconCompositedOn(lockIcon);
    }

    @SuppressWarnings("removal")
    static Image getGenericJavaIcon() {
        return java.security.AccessController.doPrivileged(new PrivilegedAction<Image>() {
            public Image run() {
                return com.apple.eawt.Application.getApplication().getDockIconImage();
            }
        });
    }

    @SuppressWarnings("removal")
    static String getPathToThisApplication() {
        return java.security.AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return FileManager.getPathToApplicationBundle();
            }
        });
    }

    static IconUIResource getAppIconCompositedOn(final SystemIcon systemIcon) {
        systemIcon.setSize(kAlertIconSize, kAlertIconSize);
        return getAppIconCompositedOn(systemIcon.createImage());
    }

    private static final int kAlertIconSize = 64;
    static IconUIResource getAppIconCompositedOn(final Image background) {

        if (background instanceof MultiResolutionCachedImage) {
            int width = background.getWidth(null);
            Image mrIconImage = ((MultiResolutionCachedImage) background).map(
                    rv -> getAppIconImageCompositedOn(rv, rv.getWidth(null) / width));
            return new IconUIResource(new ImageIcon(mrIconImage));
        }

        BufferedImage iconImage = getAppIconImageCompositedOn(background, 1);
        return new IconUIResource(new ImageIcon(iconImage));
    }

    static BufferedImage getAppIconImageCompositedOn(final Image background, int scaleFactor) {

        final int scaledAlertIconSize = kAlertIconSize * scaleFactor;
        final int kAlertSubIconSize = (int) (scaledAlertIconSize * 0.5);
        final int kAlertSubIconInset = scaledAlertIconSize - kAlertSubIconSize;
        final Icon smallAppIconScaled = new AquaIcon.CachingScalingIcon(
                kAlertSubIconSize, kAlertSubIconSize) {
                    Image createImage() {
                        return getGenericJavaIcon();
                    }
                };

        final BufferedImage image = new BufferedImage(scaledAlertIconSize,
                scaledAlertIconSize, BufferedImage.TYPE_INT_ARGB_PRE);
        final Graphics g = image.getGraphics();
        g.drawImage(background, 0, 0,
                scaledAlertIconSize, scaledAlertIconSize, null);
        if (g instanceof Graphics2D) {
            // improves icon rendering quality in Quartz
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
        }

        smallAppIconScaled.paintIcon(null, g,
                kAlertSubIconInset, kAlertSubIconInset);
        g.dispose();

        return image;
    }

    public static IconUIResource getTreeFolderIcon() {
        // public, because UIDefaults.ProxyLazyValue uses reflection to get this value
        return AquaIcon.SystemIcon.getFolderIconUIResource();
    }

    public static IconUIResource getTreeOpenFolderIcon() {
        // public, because UIDefaults.ProxyLazyValue uses reflection to get this value
        return AquaIcon.SystemIcon.getOpenFolderIconUIResource();
    }

    public static IconUIResource getTreeDocumentIcon() {
        // public, because UIDefaults.ProxyLazyValue uses reflection to get this value
        return AquaIcon.SystemIcon.getDocumentIconUIResource();
    }

    public static UIResource getTreeExpandedIcon() {
        // public, because UIDefaults.ProxyLazyValue uses reflection to get this value
        return AquaIcon.getIconFor(new JRSUIControlSpec() {
            public void initIconPainter(final AquaPainter<? extends JRSUIState> painter) {
                painter.state.set(Widget.DISCLOSURE_TRIANGLE);
                painter.state.set(State.ACTIVE);
                painter.state.set(Direction.DOWN);
                painter.state.set(AlignmentHorizontal.CENTER);
                painter.state.set(AlignmentVertical.CENTER);
            }
        }, 20, 20);
    }

    public static UIResource getTreeCollapsedIcon() {
        // public, because UIDefaults.ProxyLazyValue uses reflection to get this value
        return AquaIcon.getIconFor(new JRSUIControlSpec() {
            public void initIconPainter(final AquaPainter<? extends JRSUIState> painter) {
                painter.state.set(Widget.DISCLOSURE_TRIANGLE);
                painter.state.set(State.ACTIVE);
                painter.state.set(Direction.RIGHT);
                painter.state.set(AlignmentHorizontal.CENTER);
                painter.state.set(AlignmentVertical.CENTER);
            }
        }, 20, 20);
    }

    public static UIResource getTreeRightToLeftCollapsedIcon() {
        // public, because UIDefaults.ProxyLazyValue uses reflection to get this value
        return AquaIcon.getIconFor(new JRSUIControlSpec() {
            public void initIconPainter(final AquaPainter<? extends JRSUIState> painter) {
                painter.state.set(Widget.DISCLOSURE_TRIANGLE);
                painter.state.set(State.ACTIVE);
                painter.state.set(Direction.LEFT);
                painter.state.set(AlignmentHorizontal.CENTER);
                painter.state.set(AlignmentVertical.CENTER);
            }
        }, 20, 20);
    }

    static class NamedImageSingleton extends RecyclableSingleton<Image> {
        final String namedImage;

        NamedImageSingleton(final String namedImage) {
            this.namedImage = namedImage;
        }

        @Override
        protected Image getInstance() {
            return getNSIcon(namedImage);
        }
    }

    static class IconUIResourceSingleton extends RecyclableSingleton<IconUIResource> {
        final NamedImageSingleton holder;

        public IconUIResourceSingleton(final NamedImageSingleton holder) {
            this.holder = holder;
        }

        @Override
        protected IconUIResource getInstance() {
            return new IconUIResource(new ImageIcon(holder.get()));
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    static class InvertableImageIcon extends ImageIcon implements InvertableIcon, UIResource {
        Icon invertedImage;
        public InvertableImageIcon(final Image image) {
            super(image);
        }

        @Override
        public Icon getInvertedIcon() {
            if (invertedImage != null) return invertedImage;
            return invertedImage = new IconUIResource(new ImageIcon(AquaUtils.generateLightenedImage(getImage(), 100)));
        }
    }

    private static final NamedImageSingleton northArrow = new NamedImageSingleton("NSMenuScrollUp");
    private static final IconUIResourceSingleton northArrowIcon = new IconUIResourceSingleton(northArrow);
    private static final NamedImageSingleton southArrow = new NamedImageSingleton("NSMenuScrollDown");
    private static final IconUIResourceSingleton southArrowIcon = new IconUIResourceSingleton(southArrow);
    private static final NamedImageSingleton westArrow = new NamedImageSingleton("NSMenuSubmenuLeft");
    private static final IconUIResourceSingleton westArrowIcon = new IconUIResourceSingleton(westArrow);
    private static final NamedImageSingleton eastArrow = new NamedImageSingleton("NSMenuSubmenu");
    private static final IconUIResourceSingleton eastArrowIcon = new IconUIResourceSingleton(eastArrow);

    static Image getArrowImageForDirection(final int direction) {
        switch(direction) {
            case SwingConstants.NORTH: return northArrow.get();
            case SwingConstants.SOUTH: return southArrow.get();
            case SwingConstants.EAST: return eastArrow.get();
            case SwingConstants.WEST: return westArrow.get();
        }
        return null;
    }

    static Icon getArrowIconForDirection(int direction) {
        switch(direction) {
            case SwingConstants.NORTH: return northArrowIcon.get();
            case SwingConstants.SOUTH: return southArrowIcon.get();
            case SwingConstants.EAST: return eastArrowIcon.get();
            case SwingConstants.WEST: return westArrowIcon.get();
        }
        return null;
    }

    public static Icon getMenuArrowIcon() {
        return new InvertableImageIcon(AquaUtils.generateLightenedImage(eastArrow.get(), 25));
    }

    public static Icon getMenuItemCheckIcon() {
        return new InvertableImageIcon(AquaUtils.generateLightenedImage(
                getNSIcon("NSMenuItemSelection"), 25));
    }

    public static Icon getMenuItemDashIcon() {
        return new InvertableImageIcon(AquaUtils.generateLightenedImage(
                getNSIcon("NSMenuMixedState"), 25));
    }

    private static Image getNSIcon(String imageName) {
        Image icon = Toolkit.getDefaultToolkit()
                .getImage("NSImage://" + imageName);
        return icon;
    }

    public static class NineSliceMetrics {
        public final int wCut, eCut, nCut, sCut;
        public final int minW, minH;
        public final boolean showMiddle, stretchH, stretchV;

        public NineSliceMetrics(final int minWidth, final int minHeight, final int westCut, final int eastCut, final int northCut, final int southCut) {
            this(minWidth, minHeight, westCut, eastCut, northCut, southCut, true);
        }

        public NineSliceMetrics(final int minWidth, final int minHeight, final int westCut, final int eastCut, final int northCut, final int southCut, final boolean showMiddle) {
            this(minWidth, minHeight, westCut, eastCut, northCut, southCut, showMiddle, true, true);
        }

        public NineSliceMetrics(final int minWidth, final int minHeight, final int westCut, final int eastCut, final int northCut, final int southCut, final boolean showMiddle, final boolean stretchHorizontally, final boolean stretchVertically) {
            this.wCut = westCut; this.eCut = eastCut; this.nCut = northCut; this.sCut = southCut;
            this.minW = minWidth; this.minH = minHeight;
            this.showMiddle = showMiddle; this.stretchH = stretchHorizontally; this.stretchV = stretchVertically;
        }
    }

    /*
     * A "paintable" which holds nine images, which represent a sliced up initial
     * image that can be streched from its middles.
     */
    public static class SlicedImageControl {
        final BufferedImage NW, N, NE;
        final BufferedImage W, C, E;
        final BufferedImage SW, S, SE;

        final NineSliceMetrics metrics;

        final int totalWidth, totalHeight;
        final int centerColWidth, centerRowHeight;

        public SlicedImageControl(final Image img, final int westCut, final int eastCut, final int northCut, final int southCut) {
            this(img, westCut, eastCut, northCut, southCut, true);
        }

        public SlicedImageControl(final Image img, final int westCut, final int eastCut, final int northCut, final int southCut, final boolean useMiddle) {
            this(img, westCut, eastCut, northCut, southCut, useMiddle, true, true);
        }

        public SlicedImageControl(final Image img, final int westCut, final int eastCut, final int northCut, final int southCut, final boolean useMiddle, final boolean stretchHorizontally, final boolean stretchVertically) {
            this(img, new NineSliceMetrics(img.getWidth(null), img.getHeight(null), westCut, eastCut, northCut, southCut, useMiddle, stretchHorizontally, stretchVertically));
        }

        public SlicedImageControl(final Image img, final NineSliceMetrics metrics) {
            this.metrics = metrics;

            if (img.getWidth(null) != metrics.minW || img.getHeight(null) != metrics.minH) {
                throw new IllegalArgumentException("SlicedImageControl: template image and NineSliceMetrics don't agree on minimum dimensions");
            }

            totalWidth = metrics.minW;
            totalHeight = metrics.minH;
            centerColWidth = totalWidth - metrics.wCut - metrics.eCut;
            centerRowHeight = totalHeight - metrics.nCut - metrics.sCut;

            NW = createSlice(img, 0, 0, metrics.wCut, metrics.nCut);
            N = createSlice(img, metrics.wCut, 0, centerColWidth, metrics.nCut);
            NE = createSlice(img, totalWidth - metrics.eCut, 0, metrics.eCut, metrics.nCut);
            W = createSlice(img, 0, metrics.nCut, metrics.wCut, centerRowHeight);
            C = metrics.showMiddle ? createSlice(img, metrics.wCut, metrics.nCut, centerColWidth, centerRowHeight) : null;
            E = createSlice(img, totalWidth - metrics.eCut, metrics.nCut, metrics.eCut, centerRowHeight);
            SW = createSlice(img, 0, totalHeight - metrics.sCut, metrics.wCut, metrics.sCut);
            S = createSlice(img, metrics.wCut, totalHeight - metrics.sCut, centerColWidth, metrics.sCut);
            SE = createSlice(img, totalWidth - metrics.eCut, totalHeight - metrics.sCut, metrics.eCut, metrics.sCut);
        }

        static BufferedImage createSlice(final Image img, final int x, final int y, final int w, final int h) {
            if (w == 0 || h == 0) return null;

            final BufferedImage slice = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
            final Graphics2D g2d = slice.createGraphics();
            g2d.drawImage(img, 0, 0, w, h, x, y, x + w, y + h, null);
            g2d.dispose();

            return slice;
        }

        public void paint(final Graphics g, final int x, final int y, final int w, final int h) {
            g.translate(x, y);

            if (w < totalWidth || h < totalHeight) {
                paintCompressed(g, w, h);
            } else {
                paintStretchedMiddles(g, w, h);
            }

            g.translate(-x, -y);
        }

        void paintStretchedMiddles(final Graphics g, final int w, final int h) {
            int baseX = metrics.stretchH ? 0 : ((w / 2) - (totalWidth / 2));
            int baseY = metrics.stretchV ? 0 : ((h / 2) - (totalHeight / 2));
            int adjustedWidth = metrics.stretchH ? w : totalWidth;
            int adjustedHeight = metrics.stretchV ? h : totalHeight;

            if (NW != null) g.drawImage(NW, baseX, baseY, null);
            if (N != null) g.drawImage(N, baseX + metrics.wCut, baseY, adjustedWidth - metrics.eCut - metrics.wCut, metrics.nCut, null);
            if (NE != null) g.drawImage(NE, baseX + adjustedWidth - metrics.eCut, baseY, null);
            if (W != null) g.drawImage(W, baseX, baseY + metrics.nCut, metrics.wCut, adjustedHeight - metrics.nCut - metrics.sCut, null);
            if (C != null) g.drawImage(C, baseX + metrics.wCut, baseY + metrics.nCut, adjustedWidth - metrics.eCut - metrics.wCut, adjustedHeight - metrics.nCut - metrics.sCut, null);
            if (E != null) g.drawImage(E, baseX + adjustedWidth - metrics.eCut, baseY + metrics.nCut, metrics.eCut, adjustedHeight - metrics.nCut - metrics.sCut, null);
            if (SW != null) g.drawImage(SW, baseX, baseY + adjustedHeight - metrics.sCut, null);
            if (S != null) g.drawImage(S, baseX + metrics.wCut, baseY + adjustedHeight - metrics.sCut, adjustedWidth - metrics.eCut - metrics.wCut, metrics.sCut, null);
            if (SE != null) g.drawImage(SE, baseX + adjustedWidth - metrics.eCut, baseY + adjustedHeight - metrics.sCut, null);

            /*
            if (NW != null) {g.setColor(Color.GREEN); g.fillRect(baseX, baseY, NW.getWidth(), NW.getHeight());}
            if (N != null) {g.setColor(Color.RED); g.fillRect(baseX + metrics.wCut, baseY, adjustedWidth - metrics.eCut - metrics.wCut, metrics.nCut);}
            if (NE != null) {g.setColor(Color.BLUE); g.fillRect(baseX + adjustedWidth - metrics.eCut, baseY, NE.getWidth(), NE.getHeight());}
            if (W != null) {g.setColor(Color.PINK); g.fillRect(baseX, baseY + metrics.nCut, metrics.wCut, adjustedHeight - metrics.nCut - metrics.sCut);}
            if (C != null) {g.setColor(Color.ORANGE); g.fillRect(baseX + metrics.wCut, baseY + metrics.nCut, adjustedWidth - metrics.eCut - metrics.wCut, adjustedHeight - metrics.nCut - metrics.sCut);}
            if (E != null) {g.setColor(Color.CYAN); g.fillRect(baseX + adjustedWidth - metrics.eCut, baseY + metrics.nCut, metrics.eCut, adjustedHeight - metrics.nCut - metrics.sCut);}
            if (SW != null) {g.setColor(Color.MAGENTA); g.fillRect(baseX, baseY + adjustedHeight - metrics.sCut, SW.getWidth(), SW.getHeight());}
            if (S != null) {g.setColor(Color.DARK_GRAY); g.fillRect(baseX + metrics.wCut, baseY + adjustedHeight - metrics.sCut, adjustedWidth - metrics.eCut - metrics.wCut, metrics.sCut);}
            if (SE != null) {g.setColor(Color.YELLOW); g.fillRect(baseX + adjustedWidth - metrics.eCut, baseY + adjustedHeight - metrics.sCut, SE.getWidth(), SE.getHeight());}
            */
        }

        void paintCompressed(final Graphics g, final int w, final int h) {
            final double heightRatio = h > totalHeight ? 1.0 : (double)h / (double)totalHeight;
            final double widthRatio = w > totalWidth ? 1.0 : (double)w / (double)totalWidth;

            final int northHeight = (int)(metrics.nCut * heightRatio);
            final int southHeight = (int)(metrics.sCut * heightRatio);
            final int centerHeight = h - northHeight - southHeight;

            final int westWidth = (int)(metrics.wCut * widthRatio);
            final int eastWidth = (int)(metrics.eCut * widthRatio);
            final int centerWidth = w - westWidth - eastWidth;

            if (NW != null) g.drawImage(NW, 0, 0, westWidth, northHeight, null);
            if (N != null) g.drawImage(N, westWidth, 0, centerWidth, northHeight, null);
            if (NE != null) g.drawImage(NE, w - eastWidth, 0, eastWidth, northHeight, null);
            if (W != null) g.drawImage(W, 0, northHeight, westWidth, centerHeight, null);
            if (C != null) g.drawImage(C, westWidth, northHeight, centerWidth, centerHeight, null);
            if (E != null) g.drawImage(E, w - eastWidth, northHeight, eastWidth, centerHeight, null);
            if (SW != null) g.drawImage(SW, 0, h - southHeight, westWidth, southHeight, null);
            if (S != null) g.drawImage(S, westWidth, h - southHeight, centerWidth, southHeight, null);
            if (SE != null) g.drawImage(SE, w - eastWidth, h - southHeight, eastWidth, southHeight, null);
        }
    }

    public abstract static class RecyclableSlicedImageControl extends RecyclableObject<SlicedImageControl> {
        final NineSliceMetrics metrics;

        public RecyclableSlicedImageControl(final NineSliceMetrics metrics) {
            this.metrics = metrics;
        }

        @Override
        protected SlicedImageControl create() {
            return new SlicedImageControl(createTemplateImage(metrics.minW, metrics.minH), metrics);
        }

        protected abstract Image createTemplateImage(final int width, final int height);
    }

    // when we use SystemColors, we need to proxy the color with something that implements UIResource,
    // so that it will be uninstalled when the look and feel is changed.
    @SuppressWarnings("serial") // JDK implementation class
    private static class SystemColorProxy extends Color implements UIResource {
        final Color color;
        public SystemColorProxy(final Color color) {
            super(color.getRGB());
            this.color = color;
        }

        public int getRGB() {
            return color.getRGB();
        }
    }

    public static Color getWindowBackgroundColorUIResource() {
        //return AquaNativeResources.getWindowBackgroundColorUIResource();
        return new SystemColorProxy(SystemColor.window);
    }

    public static Color getTextSelectionBackgroundColorUIResource() {
        return new SystemColorProxy(SystemColor.textHighlight);
    }

    public static Color getTextSelectionForegroundColorUIResource() {
        return new SystemColorProxy(SystemColor.textHighlightText);
    }

    public static Color getSelectionBackgroundColorUIResource() {
        return new SystemColorProxy(SystemColor.controlHighlight);
    }

    public static Color getSelectionForegroundColorUIResource() {
        return new SystemColorProxy(SystemColor.controlLtHighlight);
    }

    public static Color getFocusRingColorUIResource() {
        return new SystemColorProxy(LWCToolkit.getAppleColor(LWCToolkit.KEYBOARD_FOCUS_COLOR));
    }

    public static Color getSelectionInactiveBackgroundColorUIResource() {
        return new SystemColorProxy(LWCToolkit.getAppleColor(LWCToolkit.INACTIVE_SELECTION_BACKGROUND_COLOR));
    }

    public static Color getSelectionInactiveForegroundColorUIResource() {
        return new SystemColorProxy(LWCToolkit.getAppleColor(LWCToolkit.INACTIVE_SELECTION_FOREGROUND_COLOR));
    }

    public static Color getSelectedControlColorUIResource() {
        return new SystemColorProxy(LWCToolkit.getAppleColor(LWCToolkit.SELECTED_CONTROL_TEXT_COLOR));
    }
}
