/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Font;

import sun.swing.plaf.DesktopProperty;

/**
 * DesktopProperty that only uses font height in configuring font. This
 * is only used on Windows.
 */
final class MetalFontDesktopProperty extends DesktopProperty {
    /**
     * Maps from metal font theme type as defined in MetalTheme
     * to the corresponding desktop property name.
     */
    private static final String[] propertyMapping = {
        "win.ansiVar.font.height",
        "win.tooltip.font.height",
        "win.ansiVar.font.height",
        "win.menu.font.height",
        "win.frame.captionFont.height",
        "win.menu.font.height"
    };

    /**
     * Corresponds to a MetalTheme font type.
     */
    private int type;


    /**
     * Creates a MetalFontDesktopProperty. The key used to lookup the
     * desktop property is determined from the type of font.
     *
     * @param type MetalTheme font type.
     */
    MetalFontDesktopProperty(int type) {
        this(propertyMapping[type], type);
    }

    /**
     * Creates a MetalFontDesktopProperty.
     *
     * @param key Key used in looking up desktop value.
     * @param type Type of font being used, corresponds to MetalTheme font
     *        type.
     */
    MetalFontDesktopProperty(String key, int type) {
        super(key, null);
        this.type = type;
    }

    /**
     * Overriden to create a Font with the size coming from the desktop
     * and the style and name coming from DefaultMetalTheme.
     */
    protected Object configureValue(Object value) {
        if (value instanceof Integer) {
            value = new Font(DefaultMetalTheme.getDefaultFontName(type),
                             DefaultMetalTheme.getDefaultFontStyle(type),
                             ((Integer)value).intValue());
        }
        return super.configureValue(value);
    }

    /**
     * Returns the default font.
     */
    protected Object getDefaultValue() {
        return new Font(DefaultMetalTheme.getDefaultFontName(type),
                        DefaultMetalTheme.getDefaultFontStyle(type),
                        DefaultMetalTheme.getDefaultFontSize(type));
    }
}
