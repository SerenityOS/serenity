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
import java.beans.*;
import java.security.AccessController;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.*;

import apple.laf.*;
import apple.laf.JRSUIConstants.*;

import com.apple.laf.AquaUtils.RecyclableSingleton;
import com.apple.laf.AquaUtils.RecyclableSingletonFromDefaultConstructor;
import sun.security.action.GetPropertyAction;

public class AquaUtilControlSize {
    protected static final String CLIENT_PROPERTY_KEY = "JComponent.sizeVariant";
    protected static final String SYSTEM_PROPERTY_KEY = "swing.component.sizevariant";

    interface Sizeable {
        void applySizeFor(final JComponent c, final Size size);
    }

    private static final RecyclableSingleton<PropertySizeListener> sizeListener
            = new RecyclableSingletonFromDefaultConstructor<>(PropertySizeListener.class);
    protected static PropertySizeListener getSizeListener() {
        return sizeListener.get();
    }

    protected static void addSizePropertyListener(final JComponent c) {
        c.addPropertyChangeListener(CLIENT_PROPERTY_KEY, getSizeListener());
        PropertySizeListener.applyComponentSize(c, c.getClientProperty(CLIENT_PROPERTY_KEY));
    }

    protected static void removeSizePropertyListener(final JComponent c) {
        c.removePropertyChangeListener(CLIENT_PROPERTY_KEY, getSizeListener());
    }

    private static JRSUIConstants.Size getSizeFromString(final String name) {
        if ("regular".equalsIgnoreCase(name)) return Size.REGULAR;
        if ("small".equalsIgnoreCase(name)) return Size.SMALL;
        if ("mini".equalsIgnoreCase(name)) return Size.MINI;
        if ("large".equalsIgnoreCase(name)) return Size.LARGE;
        return null;
    }

    private static Size getDefaultSize() {
        @SuppressWarnings("removal")
        final String sizeProperty = AccessController.doPrivileged(
                new GetPropertyAction(SYSTEM_PROPERTY_KEY));
        final JRSUIConstants.Size size = getSizeFromString(sizeProperty);
        if (size != null) return size;
        return JRSUIConstants.Size.REGULAR;
    }

    protected static final JRSUIConstants.Size defaultSize = getDefaultSize();
    protected static JRSUIConstants.Size getUserSizeFrom(final JComponent c) {
        final Object sizeProp = c.getClientProperty(CLIENT_PROPERTY_KEY);
        if (sizeProp == null) return defaultSize;
        final Size size = getSizeFromString(sizeProp.toString());
        if (size == null) return Size.REGULAR;
        return size;
    }

    protected static JRSUIConstants.Size applySizeForControl(final JComponent c,
                                                             final AquaPainter<? extends JRSUIState> painter) {
        final JRSUIConstants.Size sizeFromUser = getUserSizeFrom(c);
        final JRSUIConstants.Size size = sizeFromUser == null
                                         ? JRSUIConstants.Size.REGULAR
                                         : sizeFromUser;
        painter.state.set(size);
        return size;
    }

    protected static Font getFontForSize(final Component c,
                                         final JRSUIConstants.Size size) {
        final Font initialFont = c.getFont();

        if (size == null || !(initialFont instanceof UIResource)) {
            return initialFont;
        }

        if (size == JRSUIConstants.Size.MINI) {
            return initialFont.deriveFont(
                    AquaFonts.getMiniControlTextFont().getSize2D());
        }
        if (size == JRSUIConstants.Size.SMALL) {
            return initialFont.deriveFont(
                    AquaFonts.getSmallControlTextFont().getSize2D());
        }

        return initialFont.deriveFont(AquaFonts.getControlTextFont().getSize2D());
    }

    private static void applyBorderForSize(final JComponent c, final Size size) {
        final Border border = c.getBorder();
        if (!(border instanceof AquaBorder)) return;
        final AquaBorder aquaBorder = (AquaBorder)border;

        if (aquaBorder.sizeVariant.size == size) return;
        final AquaBorder derivedBorder = aquaBorder.deriveBorderForSize(size);
        if (derivedBorder == null) return;

        c.setBorder(derivedBorder);
    }

    protected static class PropertySizeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(final PropertyChangeEvent evt) {
            final String key = evt.getPropertyName();
            if (!CLIENT_PROPERTY_KEY.equalsIgnoreCase(key)) return;

            final Object source = evt.getSource();
            if (!(source instanceof JComponent)) return;

            final JComponent c = (JComponent)source;
            applyComponentSize(c, evt.getNewValue());
        }

        protected static void applyComponentSize(final JComponent c, final Object value) {
            Size size = getSizeFromString(value == null ? null : value.toString());
            if (size == null) {
                size = getUserSizeFrom(c);
                if (size == Size.REGULAR) return;
            }

            applyBorderForSize(c, size);

            final Object ui = c.getUI();
            if (ui instanceof Sizeable) {
                ((Sizeable) ui).applySizeFor(c, size);
            }

            final Font priorFont = c.getFont();
            if (!(priorFont instanceof FontUIResource)) return;
            c.setFont(getFontForSize(c, size));
        }
    }

    public static class SizeDescriptor {
        SizeVariant regular;
        SizeVariant small;
        SizeVariant mini;

        public SizeDescriptor(final SizeVariant variant) {
            regular = deriveRegular(variant);
            small = deriveSmall(new SizeVariant(regular));
            mini = deriveMini(new SizeVariant(small));
        }

        public SizeVariant deriveRegular(final SizeVariant v) {
            v.size = Size.REGULAR;
            return v;
        }

        public SizeVariant deriveSmall(final SizeVariant v) {
            v.size = Size.SMALL;
            return v;
        }

        public SizeVariant deriveMini(final SizeVariant v) {
            v.size = Size.MINI;
            return v;
        }

        public SizeVariant get(final JComponent c) {
            if (c == null) return regular;
            return get(getUserSizeFrom(c));
        }

        public SizeVariant get(final Size size) {
            if (size == Size.REGULAR) return regular;
            if (size == Size.SMALL) return small;
            if (size == Size.MINI) return mini;
            return regular;
        }

        @Override
        public String toString() {
            return "regular[" + regular + "] small[" + small + "] mini[" + mini + "]";
        }
    }

    public static class SizeVariant {
        Size size = Size.REGULAR;
        Insets insets = new InsetsUIResource(0, 0, 0, 0);
        Insets margins = new InsetsUIResource(0, 0, 0, 0);
        Float fontSize;
        int w = 0;
        int h = 0;
    //    Integer textBaseline;

        public SizeVariant() { }

        public SizeVariant(final int minWidth, final int minHeight) {
            this.w = minWidth;
            this.h = minHeight;
        }

        public SizeVariant(final SizeVariant desc){
            this.size = desc.size;
            this.insets = new InsetsUIResource(desc.insets.top,
                                               desc.insets.left,
                                               desc.insets.bottom,
                                               desc.insets.right);
            this.margins = new InsetsUIResource(desc.margins.top,
                                                desc.margins.left,
                                                desc.margins.bottom,
                                                desc.margins.right);
            this.fontSize = desc.fontSize;
            this.w = desc.w;
            this.h = desc.h;
    //        this.textBaseline = desc.textBaseline;
        }

        public SizeVariant replaceInsets(final String insetName) {
            this.insets = UIManager.getInsets(insetName);
            return this;
        }

        public SizeVariant replaceInsets(final Insets i) {
            this.insets = new InsetsUIResource(i.top, i.left, i.bottom, i.right);
            return this;
        }

        public SizeVariant alterInsets(final int top, final int left,
                                       final int bottom, final int right) {
            insets = generateInsets(insets, top, left, bottom, right);
            return this;
        }

        public SizeVariant replaceMargins(final String marginName) {
            this.margins = UIManager.getInsets(marginName);
            return this;
        }

        public SizeVariant alterMargins(final int top, final int left,
                                        final int bottom, final int right) {
            margins = generateInsets(margins, top, left, bottom, right);
            return this;
        }

        public SizeVariant alterFontSize(final float newSize) {
            final float oldSize = fontSize == null ? 0.0f : fontSize.floatValue();
            fontSize = newSize + oldSize;
            return this;
        }

        public SizeVariant alterMinSize(final int width, final int height) {
            this.w += width; this.h += height;
            return this;
        }

//        public SizeVariant alterTextBaseline(final int baseline) {
//            final int oldSize = textBaseline == null ? 0 : textBaseline.intValue();
//            textBaseline = new Integer(baseline + oldSize);
//            return this;
//        }

        static Insets generateInsets(final Insets i, final int top,
                                     final int left, final int bottom,
                                     final int right) {
            if (i == null) {
                return new InsetsUIResource(top, left, bottom, right);
            }
            i.top += top;
            i.left += left;
            i.bottom += bottom;
            i.right += right;
            return i;
        }

        @Override
        public String toString() {
            return "insets:" + insets + ", margins:" + margins + ", fontSize:"
                    + fontSize;// + ", textBaseline:" + textBaseline;
        }
    }
}
