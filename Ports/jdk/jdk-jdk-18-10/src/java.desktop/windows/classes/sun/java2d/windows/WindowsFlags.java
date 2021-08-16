/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.windows;

import sun.awt.windows.WToolkit;
import sun.java2d.opengl.WGLGraphicsConfig;

public class WindowsFlags {

    /**
     * Description of command-line flags.  All flags with [true|false]
     * values (where both have possible meanings, such as with ddlock)
     * have an associated variable that indicates whether this flag
     * was set by the user.  For example, d3d is on by default, but
     * may be disabled at runtime by internal settings unless the user
     * has forced it on with d3d=true.  These associated variables have
     * the same base (eg, d3d) but end in "Set" (eg, d3dEnabled and
     * d3dSet).
     *      ddEnabled: usage: "-Dsun.java2d.noddraw[=false|true]"
     *               turns on/off all usage of Direct3D
     *      ddOffscreenEnabled: equivalent of sun.java2d.noddraw
     *      gdiBlitEnabled: usage: "-Dsun.java2d.gdiblit=false"
     *               turns off Blit loops that use GDI for copying to
     *               the screen from certain image types.  Copies will,
     *               instead, happen via ddraw locking or temporary GDI DIB
     *               creation/copying (depending on OS and other flags)
     *      d3dEnabled: usage: "-Dsun.java2d.d3d=[true|false]"
     *               Forces our use of Direct3D on or off.  Direct3D is on
     *               by default, but may be disabled in some situations, such
     *               as on a card with bad d3d line quality, or on a video card
     *               that we have had bad experience with (e.g., Trident).
     *               This flag can force us to use d3d
     *               anyway in these situations.  Or, this flag can force us to
     *               not use d3d in a situation where we would use it otherwise.
     *      offscreenSharingEnabled: usage: "-Dsun.java2d.offscreenSharing=true"
     *               Turns on the ability to share a hardware-accelerated
     *               offscreen surface through the JAWT interface.  See
     *               src/windows/native/sun/windows/awt_DrawingSurface.* for
     *               more information.  This capability is disabled by default
     *               pending more testing and time to work out the right
     *               solution; we do not want to expose more public JAWT api
     *               without being very sure that we will be willing to support
     *               that API in the future regardless of other native
     *               rendering pipeline changes.
     *      magPresent: usage: "-Djavax.accessibility.screen_magnifier_present"
     *               This flag is set either on the command line or in the
     *               properties file.  It tells Swing whether the user is
     *               currently using a screen magnifying application.  These
     *               applications tend to conflict with ddraw (which assumes
     *               it owns the entire display), so the presence of these
     *               applications implies that we should disable ddraw.
     *               So if magPresent is true, we set ddEnabled and associated
     *               variables to false and do not initialize the native
     *               hardware acceleration for these properties.
     *      opengl: usage: "-Dsun.java2d.opengl=[true|True]"
     *               Enables the use of the OpenGL-pipeline.  If the
     *               OpenGL flag is specified and WGL initialization is
     *               successful, we implicitly disable the use of DirectDraw
     *               and Direct3D, as those pipelines may interfere with the
     *               OGL pipeline.  (If "True" is specified, a message will
     *               appear on the console stating whether or not the OGL
     *               was successfully initialized.)
     * setHighDPIAware: Property usage: "-Dsun.java2d.dpiaware=[true|false]"
     *               This property flag "sun.java2d.dpiaware" is used to
     *               override the default behavior, which is:
     *               On Windows Vista, if the java process is launched from a
     *               known launcher (java, javaw, javaws, etc) - which is
     *               determined by whether a -Dsun.java.launcher property is set
     *               to "SUN_STANDARD" - the "high-DPI aware" property will be
     *               set on the native level prior to initializing the display.
     *
     */

    private static boolean gdiBlitEnabled;
    private static boolean d3dEnabled;
    private static boolean d3dVerbose;
    private static boolean d3dSet;
    private static boolean d3dOnScreenEnabled;
    private static boolean oglEnabled;
    private static boolean oglVerbose;
    private static boolean offscreenSharingEnabled;
    private static boolean magPresent;
    private static boolean setHighDPIAware;
    // TODO: other flags, including nopixfmt

    static {
        // Ensure awt is loaded already.  Also, this forces static init
        // of WToolkit and Toolkit, which we depend upon.
        WToolkit.loadLibraries();
        // First, init all Java level flags
        initJavaFlags();
        // Now, init things on the native side.  This may call up through
        // JNI to get/set the Java level flags based on native capabilities
        // and environment variables
        initNativeFlags();
    }

    private static native boolean initNativeFlags();

    // Noop: this method is just here as a convenient calling place when
    // we are initialized by Win32GraphicsEnv.  Calling this will force
    // us to run through the static block below, which is where the
    // real work occurs.
    public static void initFlags() {}

    private static boolean getBooleanProp(String p, boolean defaultVal) {
        String propString = System.getProperty(p);
        boolean returnVal = defaultVal;
        if (propString != null) {
            if (propString.equals("true") ||
                propString.equals("t") ||
                propString.equals("True") ||
                propString.equals("T") ||
                propString.isEmpty()) // having the prop name alone
            {                         // is equivalent to true
                returnVal = true;
            } else if (propString.equals("false") ||
                       propString.equals("f") ||
                       propString.equals("False") ||
                       propString.equals("F"))
            {
                returnVal = false;
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

    private static int getIntProp(String p, int defaultVal) {
        String propString = System.getProperty(p);
        int returnVal = defaultVal;
        if (propString != null) {
            try {
                returnVal = Integer.parseInt(propString);
            } catch (NumberFormatException e) {}
        }
        return returnVal;
    }

    private static boolean getPropertySet(String p) {
        String propString = System.getProperty(p);
        return (propString != null) ? true : false;
    }

    @SuppressWarnings("removal")
    private static void initJavaFlags() {
        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<Object>()
        {
            public Object run() {
                magPresent = getBooleanProp(
                    "javax.accessibility.screen_magnifier_present", false);
                boolean ddEnabled =
                    !getBooleanProp("sun.java2d.noddraw", magPresent);
                boolean ddOffscreenEnabled =
                    getBooleanProp("sun.java2d.ddoffscreen", ddEnabled);
                d3dEnabled = getBooleanProp("sun.java2d.d3d",
                    ddEnabled && ddOffscreenEnabled);
                d3dOnScreenEnabled =
                    getBooleanProp("sun.java2d.d3d.onscreen", d3dEnabled);
                oglEnabled = getBooleanProp("sun.java2d.opengl", false);
                if (oglEnabled) {
                    oglVerbose = isBooleanPropTrueVerbose("sun.java2d.opengl");
                    if (WGLGraphicsConfig.isWGLAvailable()) {
                        d3dEnabled = false;
                    } else {
                        if (oglVerbose) {
                            System.out.println(
                                "Could not enable OpenGL pipeline " +
                                "(WGL not available)");
                        }
                        oglEnabled = false;
                    }
                }
                gdiBlitEnabled = getBooleanProp("sun.java2d.gdiBlit", true);
                d3dSet = getPropertySet("sun.java2d.d3d");
                if (d3dSet) {
                    d3dVerbose = isBooleanPropTrueVerbose("sun.java2d.d3d");
                }
                offscreenSharingEnabled =
                    getBooleanProp("sun.java2d.offscreenSharing", false);
                String dpiOverride = System.getProperty("sun.java2d.dpiaware");
                if (dpiOverride != null) {
                    setHighDPIAware = dpiOverride.equalsIgnoreCase("true");
                } else {
                    String sunLauncherProperty =
                        System.getProperty("sun.java.launcher", "unknown");
                    setHighDPIAware =
                        sunLauncherProperty.equalsIgnoreCase("SUN_STANDARD");
                }
                /*
                // Output info based on some non-default flags:
                if (offscreenSharingEnabled) {
                    System.out.println(
                        "Warning: offscreenSharing has been enabled. " +
                        "The use of this capability will change in future " +
                        "releases and applications that depend on it " +
                        "may not work correctly");
                }
                */
                return null;
            }
        });
        /*
        System.out.println("WindowsFlags (Java):");
        System.out.println("  ddEnabled: " + ddEnabled + "\n" +
                           "  ddOffscreenEnabled: " + ddOffscreenEnabled + "\n" +
                           "  d3dEnabled: " + d3dEnabled + "\n" +
                           "  d3dSet: " + d3dSet + "\n" +
                           "  oglEnabled: " + oglEnabled + "\n" +
                           "  oglVerbose: " + oglVerbose + "\n" +
                           "  gdiBlitEnabled: " + gdiBlitEnabled + "\n" +
                           "  offscreenSharingEnabled: " + offscreenSharingEnabled);
        */
    }

    public static boolean isD3DEnabled() {
        return d3dEnabled;
    }

    public static boolean isD3DSet() {
        return d3dSet;
    }

    public static boolean isD3DOnScreenEnabled() {
        return d3dOnScreenEnabled;
    }

    public static boolean isD3DVerbose() {
        return d3dVerbose;
    }

    public static boolean isGdiBlitEnabled() {
        return gdiBlitEnabled;
    }

    public static boolean isOffscreenSharingEnabled() {
        return offscreenSharingEnabled;
    }

    public static boolean isMagPresent() {
        return magPresent;
    }

    public static boolean isOGLEnabled() {
        return oglEnabled;
    }

    public static boolean isOGLVerbose() {
        return oglVerbose;
    }
}
