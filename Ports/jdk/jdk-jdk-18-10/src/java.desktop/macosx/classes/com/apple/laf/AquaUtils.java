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
import java.awt.image.*;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.UIResource;

import sun.awt.AppContext;

import sun.lwawt.macosx.CPlatformWindow;
import sun.reflect.misc.ReflectUtil;
import sun.security.action.GetPropertyAction;
import sun.swing.SwingUtilities2;

import com.apple.laf.AquaImageFactory.SlicedImageControl;
import sun.awt.image.MultiResolutionCachedImage;
import sun.swing.SwingAccessor;

final class AquaUtils {

    private static final String ANIMATIONS_PROPERTY = "swing.enableAnimations";

    /**
     * Suppresses default constructor, ensuring non-instantiability.
     */
    private AquaUtils() {
    }

    /**
     * Convenience function for determining ComponentOrientation.  Helps us
     * avoid having Munge directives throughout the code.
     */
    static boolean isLeftToRight(final Component c) {
        return c.getComponentOrientation().isLeftToRight();
    }

    static void enforceComponentOrientation(final Component c, final ComponentOrientation orientation) {
        c.setComponentOrientation(orientation);
        if (c instanceof Container) {
            for (final Component child : ((Container)c).getComponents()) {
                enforceComponentOrientation(child, orientation);
            }
        }
    }

    static Image generateSelectedDarkImage(final Image image) {
        final ImageFilter filter =  new IconImageFilter() {
            @Override
            int getGreyFor(final int gray) {
                return gray * 75 / 100;
            }
        };
        return map(image, filter);
    }

    static Image generateDisabledImage(final Image image) {
        final ImageFilter filter = new IconImageFilter() {
            @Override
            int getGreyFor(final int gray) {
                return 255 - ((255 - gray) * 65 / 100);
            }
        };
        return map(image, filter);
    }

    static Image generateLightenedImage(final Image image, final int percent) {
        final GrayFilter filter = new GrayFilter(true, percent);
        return map(image, filter);
    }

    static Image generateFilteredImage(Image image, ImageFilter filter) {
        final ImageProducer prod = new FilteredImageSource(image.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(prod);
    }

    private static Image map(Image image, ImageFilter filter) {
        if (image instanceof MultiResolutionImage) {
            return MultiResolutionCachedImage
                    .map((MultiResolutionImage) image,
                         (img) -> generateFilteredImage(img, filter));
        }
        return generateFilteredImage(image, filter);
    }

    private abstract static class IconImageFilter extends RGBImageFilter {
        IconImageFilter() {
            super();
            canFilterIndexColorModel = true;
        }

        @Override
        public final int filterRGB(final int x, final int y, final int rgb) {
            final int red = (rgb >> 16) & 0xff;
            final int green = (rgb >> 8) & 0xff;
            final int blue = rgb & 0xff;
            final int gray = getGreyFor((int)((0.30 * red + 0.59 * green + 0.11 * blue) / 3));

            return (rgb & 0xff000000) | (grayTransform(red, gray) << 16) | (grayTransform(green, gray) << 8) | (grayTransform(blue, gray) << 0);
        }

        private static int grayTransform(final int color, final int gray) {
            int result = color - gray;
            if (result < 0) result = 0;
            if (result > 255) result = 255;
            return result;
        }

        abstract int getGreyFor(int gray);
    }

    abstract static class RecyclableObject<T> {
        private SoftReference<T> objectRef;

        T get() {
            T referent;
            if (objectRef != null && (referent = objectRef.get()) != null) return referent;
            referent = create();
            objectRef = new SoftReference<T>(referent);
            return referent;
        }

        protected abstract T create();
    }

    abstract static class RecyclableSingleton<T> {
        final T get() {
            return AppContext.getSoftReferenceValue(this, () -> getInstance());
        }

        void reset() {
            AppContext.getAppContext().remove(this);
        }

        abstract T getInstance();
    }

    static class RecyclableSingletonFromDefaultConstructor<T> extends RecyclableSingleton<T> {
        private final Class<T> clazz;

        RecyclableSingletonFromDefaultConstructor(final Class<T> clazz) {
            this.clazz = clazz;
        }

        @Override
        @SuppressWarnings("deprecation")
        T getInstance() {
            try {
                ReflectUtil.checkPackageAccess(clazz);
                return clazz.newInstance();
            } catch (ReflectiveOperationException ignored) {
            }
            return null;
        }
    }

    abstract static class LazyKeyedSingleton<K, V> {
        private Map<K, V> refs;

        V get(final K key) {
            if (refs == null) refs = new HashMap<>();

            final V cachedValue = refs.get(key);
            if (cachedValue != null) return cachedValue;

            final V value = getInstance(key);
            refs.put(key, value);
            return value;
        }

        protected abstract V getInstance(K key);
    }

    private static final RecyclableSingleton<Boolean> enableAnimations = new RecyclableSingleton<Boolean>() {
        @Override
        protected Boolean getInstance() {
            @SuppressWarnings("removal")
            final String sizeProperty = (String) AccessController.doPrivileged((PrivilegedAction<?>)new GetPropertyAction(
                    ANIMATIONS_PROPERTY));
            return !"false".equals(sizeProperty); // should be true by default
        }
    };
    private static boolean animationsEnabled() {
        return enableAnimations.get();
    }

    private static final int MENU_BLINK_DELAY = 50; // 50ms == 3/60 sec, according to the spec
    static void blinkMenu(final Selectable selectable) {
        if (!animationsEnabled()) return;
        try {
            selectable.paintSelected(false);
            Thread.sleep(MENU_BLINK_DELAY);
            selectable.paintSelected(true);
            Thread.sleep(MENU_BLINK_DELAY);
        } catch (final InterruptedException ignored) { }
    }

    interface Selectable {
        void paintSelected(boolean selected);
    }

    interface JComponentPainter {
        void paint(JComponent c, Graphics g, int x, int y, int w, int h);
    }

    interface Painter {
        void paint(Graphics g, int x, int y, int w, int h);
    }

    static void paintDropShadowText(final Graphics g, final JComponent c, final Font font, final FontMetrics metrics, final int x, final int y, final int offsetX, final int offsetY, final Color textColor, final Color shadowColor, final String text) {
        g.setFont(font);
        g.setColor(shadowColor);
        SwingUtilities2.drawString(c, g, text, x + offsetX, y + offsetY + metrics.getAscent());
        g.setColor(textColor);
        SwingUtilities2.drawString(c, g, text, x, y + metrics.getAscent());
    }

    static class ShadowBorder implements Border {
        private final Painter prePainter;
        private final Painter postPainter;

        private final int offsetX;
        private final int offsetY;
        private final float distance;
        private final int blur;
        private final Insets insets;
        private final ConvolveOp blurOp;

        ShadowBorder(final Painter prePainter, final Painter postPainter, final int offsetX, final int offsetY, final float distance, final float intensity, final int blur) {
            this.prePainter = prePainter; this.postPainter = postPainter;
            this.offsetX = offsetX; this.offsetY = offsetY; this.distance = distance; this.blur = blur;
            final int halfBlur = blur / 2;
            insets = new Insets(halfBlur - offsetY, halfBlur - offsetX, halfBlur + offsetY, halfBlur + offsetX);

            final float blurry = intensity / (blur * blur);
            final float[] blurKernel = new float[blur * blur];
            for (int i = 0; i < blurKernel.length; i++) blurKernel[i] = blurry;
            blurOp = new ConvolveOp(new Kernel(blur, blur, blurKernel));
        }

        @Override
        public final boolean isBorderOpaque() {
            return false;
        }

        @Override
        public final Insets getBorderInsets(final Component c) {
            return insets;
        }

        @Override
        public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width, final int height) {
            final BufferedImage img = new BufferedImage(width + blur * 2, height + blur * 2, BufferedImage.TYPE_INT_ARGB_PRE);
            paintToImage(img, x, y, width, height);
            g.drawImage(img, -blur, -blur, null);
        }

        private void paintToImage(final BufferedImage img, final int x, final int y, final int width, final int height) {
            // clear the prior image
            Graphics2D imgG = (Graphics2D)img.getGraphics();
            imgG.setComposite(AlphaComposite.Clear);
            imgG.setColor(Color.black);
            imgG.fillRect(0, 0, width + blur * 2, height + blur * 2);

            final int adjX = (int)(x + blur + offsetX + (insets.left * distance));
            final int adjY = (int)(y + blur + offsetY + (insets.top * distance));
            final int adjW = (int)(width - (insets.left + insets.right) * distance);
            final int adjH = (int)(height - (insets.top + insets.bottom) * distance);

            // let the delegate paint whatever they want to be blurred
            imgG.setComposite(AlphaComposite.DstAtop);
            if (prePainter != null) prePainter.paint(imgG, adjX, adjY, adjW, adjH);
            imgG.dispose();

            // blur the prior image back into the same pixels
            imgG = (Graphics2D)img.getGraphics();
            imgG.setComposite(AlphaComposite.DstAtop);
            imgG.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            imgG.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            imgG.drawImage(img, blurOp, 0, 0);

            if (postPainter != null) postPainter.paint(imgG, adjX, adjY, adjW, adjH);
            imgG.dispose();
        }
    }

    static class SlicedShadowBorder extends ShadowBorder {
        private final SlicedImageControl slices;

        SlicedShadowBorder(final Painter prePainter, final Painter postPainter, final int offsetX, final int offsetY, final float distance, final float intensity, final int blur, final int templateWidth, final int templateHeight, final int leftCut, final int topCut, final int rightCut, final int bottomCut) {
            super(prePainter, postPainter, offsetX, offsetY, distance, intensity, blur);

            final BufferedImage i = new BufferedImage(templateWidth, templateHeight, BufferedImage.TYPE_INT_ARGB_PRE);
            super.paintBorder(null, i.getGraphics(), 0, 0, templateWidth, templateHeight);
            slices = new SlicedImageControl(i, leftCut, topCut, rightCut, bottomCut, false);
        }

        @Override
        public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width, final int height) {
            slices.paint(g, x, y, width, height);
        }
    }

    @SuppressWarnings("removal")
    private static final RecyclableSingleton<Method> getJComponentGetFlagMethod = new RecyclableSingleton<Method>() {
        @Override
        protected Method getInstance() {
            return AccessController.doPrivileged(
                new PrivilegedAction<Method>() {
                    @Override
                    public Method run() {
                        try {
                            final Method method = JComponent.class.getDeclaredMethod(
                                    "getFlag", new Class<?>[] { int.class });
                            method.setAccessible(true);
                            return method;
                        } catch (final Throwable ignored) {
                            return null;
                        }
                    }
                }
            );
        }
    };

    private static final int OPAQUE_SET_FLAG = 24; // private int JComponent.OPAQUE_SET
    static boolean hasOpaqueBeenExplicitlySet(final JComponent c) {
        return SwingAccessor.getJComponentAccessor().getFlag(c, OPAQUE_SET_FLAG);
    }

    private static boolean isWindowTextured(final Component c) {
        if (!(c instanceof JComponent)) {
            return false;
        }
        final JRootPane pane = ((JComponent) c).getRootPane();
        if (pane == null) {
            return false;
        }
        Object prop = pane.getClientProperty(
                CPlatformWindow.WINDOW_BRUSH_METAL_LOOK);
        if (prop != null) {
            return Boolean.parseBoolean(prop.toString());
        }
        prop = pane.getClientProperty(CPlatformWindow.WINDOW_STYLE);
        return prop != null && "textured".equals(prop);
    }

    private static Color resetAlpha(final Color color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
    }

    static void fillRect(final Graphics g, final Component c) {
        fillRect(g, c, c.getBackground(), 0, 0, c.getWidth(), c.getHeight());
    }

    static void fillRect(final Graphics g, final Component c, final Color color,
                         final int x, final int y, final int w, final int h) {
        if (!(g instanceof Graphics2D)) {
            return;
        }
        final Graphics2D cg = (Graphics2D) g.create();
        try {
            if (color instanceof UIResource && isWindowTextured(c)
                    && color.equals(SystemColor.window)) {
                cg.setComposite(AlphaComposite.Src);
                cg.setColor(resetAlpha(color));
            } else {
                cg.setColor(color);
            }
            cg.fillRect(x, y, w, h);
        } finally {
            cg.dispose();
        }
    }
}

