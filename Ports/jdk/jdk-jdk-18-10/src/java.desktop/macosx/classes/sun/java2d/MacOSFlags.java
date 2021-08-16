/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d;

import java.security.PrivilegedAction;
import sun.java2d.metal.MTLGraphicsConfig;
import sun.java2d.opengl.CGLGraphicsConfig;


public class MacOSFlags {

    /**
     * Description of command-line flags.  All flags with [true|false]
     * values
     *      metalEnabled: usage: "-Dsun.java2d.metal=[true|false]"
     */

    private static boolean oglEnabled;
    private static boolean oglVerbose;
    private static boolean metalEnabled;
    private static boolean metalVerbose;

    private enum PropertyState {ENABLED, DISABLED, UNSPECIFIED};

    static {
        initJavaFlags();
    }

    private static PropertyState getBooleanProp(String p, PropertyState defaultVal) {
        String propString = System.getProperty(p);
        PropertyState returnVal = defaultVal;
        if (propString != null) {
            if (propString.equals("true") ||
                propString.equals("t") ||
                propString.equals("True") ||
                propString.equals("T") ||
                propString.equals("")) // having the prop name alone
            {                          // is equivalent to true
                returnVal = PropertyState.ENABLED;
            } else if (propString.equals("false") ||
                       propString.equals("f") ||
                       propString.equals("False") ||
                       propString.equals("F"))
            {
                returnVal = PropertyState.DISABLED;
            }
        }
        return returnVal;
    }

    private static boolean isBooleanPropTrueVerbose(String p) {
        String propString = System.getProperty(p);
        if (propString != null) {
            if (propString.equals("True") ||
                propString.equals("T"))
            {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("removal")
    private static void initJavaFlags() {
        java.security.AccessController.doPrivileged(
                (PrivilegedAction<Object>) () -> {
                    PropertyState oglState = getBooleanProp("sun.java2d.opengl", PropertyState.UNSPECIFIED);
                    PropertyState metalState = getBooleanProp("sun.java2d.metal", PropertyState.UNSPECIFIED);

                    // Handle invalid combinations to use the default rendering pipeline
                    // Current default rendering pipeline is OpenGL
                    // (The default can be changed to Metal in future just by toggling two states in this if condition block)
                    if ((oglState == PropertyState.UNSPECIFIED && metalState == PropertyState.UNSPECIFIED) ||
                        (oglState == PropertyState.DISABLED && metalState == PropertyState.DISABLED) ||
                        (oglState == PropertyState.ENABLED && metalState == PropertyState.ENABLED)) {
                        oglState = PropertyState.ENABLED; // Enable default pipeline
                        metalState = PropertyState.DISABLED; // Disable non-default pipeline
                    }

                    if (metalState == PropertyState.UNSPECIFIED) {
                        if (oglState == PropertyState.DISABLED) {
                            oglEnabled = false;
                            metalEnabled = true;
                        } else {
                            oglEnabled = true;
                            metalEnabled = false;
                        }
                    } else if (metalState == PropertyState.ENABLED) {
                        oglEnabled = false;
                        metalEnabled = true;
                    } else if (metalState == PropertyState.DISABLED) {
                        oglEnabled = true;
                        metalEnabled = false;
                    }

                    oglVerbose = isBooleanPropTrueVerbose("sun.java2d.opengl");
                    metalVerbose = isBooleanPropTrueVerbose("sun.java2d.metal");

                    if (oglEnabled && !metalEnabled) {
                        // Check whether OGL is available
                        if (!CGLGraphicsConfig.isCGLAvailable()) {
                            if (oglVerbose) {
                                System.out.println("Could not enable OpenGL pipeline (CGL not available)");
                            }
                            oglEnabled = false;
                            metalEnabled = MTLGraphicsConfig.isMetalAvailable();
                        }
                    } else if (metalEnabled && !oglEnabled) {
                        // Check whether Metal framework is available
                        if (!MTLGraphicsConfig.isMetalAvailable()) {
                            if (metalVerbose) {
                                System.out.println("Could not enable Metal pipeline (Metal framework not available)");
                            }
                            metalEnabled = false;
                            oglEnabled = CGLGraphicsConfig.isCGLAvailable();
                        }
                    }

                    // At this point one of the rendering pipeline must be enabled.
                    if (!metalEnabled && !oglEnabled) {
                        throw new InternalError("Error - unable to initialize any rendering pipeline.");
                    }

                    return null;
                });
    }

    public static boolean isMetalEnabled() {
        return metalEnabled;
    }

    public static boolean isMetalVerbose() {
        return metalVerbose;
    }

    public static boolean isOGLEnabled() {
        return oglEnabled;
    }

    public static boolean isOGLVerbose() {
        return oglVerbose;
    }
}
