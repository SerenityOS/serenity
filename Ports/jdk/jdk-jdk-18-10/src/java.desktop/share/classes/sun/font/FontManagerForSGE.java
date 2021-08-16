/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

package sun.font;

import java.awt.Font;
import java.util.Locale;
import java.util.TreeMap;

/**
 * This is an extension of the {@link FontManager} interface which has to
 * be implemented on systems that want to use SunGraphicsEnvironment. It
 * adds a couple of methods that are only required by SGE. Graphics
 * implementations that use their own GraphicsEnvironment are not required
 * to implement this and can use plain FontManager instead.
 */
public interface FontManagerForSGE extends FontManager {

    /**
     * Return an array of created Fonts, or null, if no fonts were created yet.
     */
    public Font[] getCreatedFonts();

    /**
     * Similar to getCreatedFonts, but returns a TreeMap of fonts by family name.
     */
    public TreeMap<String, String> getCreatedFontFamilyNames();

    /**
     * Returns all fonts installed in this environment.
     */
    public Font[] getAllInstalledFonts();

    public String[] getInstalledFontFamilyNames(Locale requestedLocale);

    /* Modifies the behaviour of a subsequent call to preferLocaleFonts()
     * to use Mincho instead of Gothic for dialoginput in JA locales
     * on windows. Not needed on other platforms.
     */
    public void useAlternateFontforJALocales();

}
