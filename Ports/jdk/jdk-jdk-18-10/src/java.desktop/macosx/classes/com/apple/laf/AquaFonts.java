/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.Map;

import javax.swing.plaf.*;

import com.apple.laf.AquaUtils.RecyclableSingleton;

@SuppressWarnings("serial") // JDK implementation class
public class AquaFonts {
    private static final String MAC_DEFAULT_FONT_NAME = "Lucida Grande";

    private static final RecyclableSingleton<FontUIResource> lucida9Pt = new RecyclableSingleton<FontUIResource>() {
        @Override
        protected FontUIResource getInstance() {
            return new DerivedUIResourceFont(MAC_DEFAULT_FONT_NAME, Font.PLAIN, 9);
        }
    };
    //private static final FontUIResource lucida10Pt = new DerivedUIResourceFont(MAC_DEFAULT_FONT_NAME, Font.PLAIN, 10);
    private static final RecyclableSingleton<FontUIResource> lucida11Pt = new RecyclableSingleton<FontUIResource>() {
        @Override
        protected FontUIResource getInstance() {
            return new DerivedUIResourceFont(MAC_DEFAULT_FONT_NAME, Font.PLAIN, 11);
        }
    };
    private static final RecyclableSingleton<FontUIResource> lucida12Pt = new RecyclableSingleton<FontUIResource>() {
        @Override
        protected FontUIResource getInstance() {
            return new DerivedUIResourceFont(MAC_DEFAULT_FONT_NAME, Font.PLAIN, 12);
        }
    };
    private static final RecyclableSingleton<FontUIResource> lucida13Pt = new RecyclableSingleton<FontUIResource>() {
        @Override
        protected FontUIResource getInstance() {
            return new DerivedUIResourceFont(MAC_DEFAULT_FONT_NAME, Font.PLAIN, 13);
        }
    };
    private static final RecyclableSingleton<FontUIResource> lucida14Pt = new RecyclableSingleton<FontUIResource>() {
        @Override
        protected FontUIResource getInstance() {
            return new DerivedUIResourceFont(MAC_DEFAULT_FONT_NAME, Font.PLAIN, 14);
        }
    };

    private static final RecyclableSingleton<FontUIResource> lucida13PtBold = new RecyclableSingleton<FontUIResource>() {
        @Override
        protected FontUIResource getInstance() {
            return new DerivedUIResourceFont(MAC_DEFAULT_FONT_NAME, Font.BOLD, 13);
        }
    };
    private static final RecyclableSingleton<FontUIResource> lucida14PtBold = new RecyclableSingleton<FontUIResource>() {
        @Override
        protected FontUIResource getInstance() {
            return new DerivedUIResourceFont(MAC_DEFAULT_FONT_NAME, Font.BOLD, 14);
        }
    };

    protected static FontUIResource getMiniControlTextFont() {
        return lucida9Pt.get();
    }

    protected static FontUIResource getSmallControlTextFont() {
        return lucida11Pt.get();
    }

    public static FontUIResource getControlTextFont() {
        return lucida13Pt.get();
    }

    public static FontUIResource getControlTextSmallFont() {
        return lucida11Pt.get();
    }

    public static FontUIResource getMenuFont() {
        return lucida14Pt.get();
    }

    public static Font getDockIconFont() {
        return lucida14PtBold.get();
    }

    public static FontUIResource getAlertHeaderFont() {
        return lucida13PtBold.get();
    }

    public static FontUIResource getAlertMessageFont() {
        return lucida11Pt.get();
    }

    public static FontUIResource getViewFont() {
        return lucida12Pt.get();
    }

    /**
     * All fonts derived from this type will also be of this type, and not a plain java.awt.Font
     */
    static class DerivedUIResourceFont extends FontUIResource implements UIResource {
        public DerivedUIResourceFont(final Font font) {
            super(font);
        }

        public DerivedUIResourceFont(final String name, final int style, final int size) {
            super(name, style, size);
        }

        public Font deriveFont(final AffineTransform trans) {
            return new DerivedUIResourceFont(super.deriveFont(trans));
        }

        public Font deriveFont(final float derivedSize) {
            return new DerivedUIResourceFont(super.deriveFont(derivedSize));
        }

        public Font deriveFont(final int derivedStyle) {
            return new DerivedUIResourceFont(super.deriveFont(derivedStyle));
        }

        public Font deriveFont(final int derivedStyle, final AffineTransform trans) {
            return new DerivedUIResourceFont(super.deriveFont(derivedStyle, trans));
        }

        public Font deriveFont(final int derivedStyle, final float derivedSize) {
            return new DerivedUIResourceFont(super.deriveFont(derivedStyle, derivedSize));
        }

        public Font deriveFont(final Map<? extends Attribute, ?> attributes) {
            return new DerivedUIResourceFont(super.deriveFont(attributes));
        }
    }
}
