/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTError;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.security.AccessController;
import java.security.PrivilegedAction;

import sun.security.action.GetPropertyAction;


/**
 * Factory class used to retrieve a valid FontManager instance for the current
 * platform.
 *
 * A default implementation is given for Linux, Solaris and Windows.
 * You can alter the behaviour of the {@link #getInstance()} method by setting
 * the {@code sun.font.fontmanager} property. For example:
 * {@code sun.font.fontmanager=sun.awt.X11FontManager}
 */
public final class FontManagerFactory {

    /** Our singleton instance. */
    private static FontManager instance = null;

    private static final String DEFAULT_CLASS;
    static {
        if (FontUtilities.isWindows) {
            DEFAULT_CLASS = "sun.awt.Win32FontManager";
        } else if (FontUtilities.isMacOSX) {
            DEFAULT_CLASS = "sun.font.CFontManager";
            } else {
            DEFAULT_CLASS = "sun.awt.X11FontManager";
            }
    }

    /**
     * Get a valid FontManager implementation for the current platform.
     *
     * @return a valid FontManager instance for the current platform
     */
    @SuppressWarnings("removal")
    public static synchronized FontManager getInstance() {

        if (instance != null) {
            return instance;
        }

        AccessController.doPrivileged(new PrivilegedAction<Object>() {

            public Object run() {
                try {
                    String fmClassName =
                            System.getProperty("sun.font.fontmanager",
                                               DEFAULT_CLASS);
                    ClassLoader cl = ClassLoader.getSystemClassLoader();
                    Class<?> fmClass = Class.forName(fmClassName, true, cl);
                    instance =
                       (FontManager) fmClass.getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException ex) {
                    throw new InternalError(ex);

                }
                return null;
            }
        });

        return instance;
    }
}
