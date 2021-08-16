/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

import sun.java2d.SunGraphicsEnvironment;

@SuppressWarnings("removal")
public class FontManagerNativeLibrary {
    static {
        java.security.AccessController.doPrivileged(
                                    new java.security.PrivilegedAction<Object>() {
            public Object run() {
               /* REMIND do we really have to load awt here? */
               System.loadLibrary("awt");
               if (System.getProperty("os.name").startsWith("Windows")) {
                   /* Ideally fontmanager library should not depend on
                      particular implementation of the font scaler.
                      However, freetype scaler is basically small wrapper on
                      top of freetype library (that is used in binary form).

                      This wrapper is compiled into fontmanager and this make
                      fontmanger library depending on freetype library.

                      On Windows DLL's in the JRE's BIN directory cannot be
                      found by windows DLL loading as that directory is not
                      on the Windows PATH.

                      To avoid link error we have to load freetype explicitly
                      before we load fontmanager.

                      NB: consider moving freetype wrapper part to separate
                          shared library in order to avoid dependency. */
                   System.loadLibrary("freetype");
               }
               System.loadLibrary("fontmanager");

               return null;
            }
        });
    }

    /*
     * Call this method to ensure libraries are loaded.
     *
     * Method acts as trigger to ensure this class is loaded
     * (and therefore initializer code is executed).
     * Actual loading is performed by static initializer.
     * (no need to execute doPrivilledged block more than once)
     */
    public static void load() {}
}
