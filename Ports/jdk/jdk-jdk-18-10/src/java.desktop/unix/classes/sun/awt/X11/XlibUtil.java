/*
 * Copyright (c) 2006, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.X11;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import sun.awt.X11GraphicsConfig;
import sun.awt.X11GraphicsDevice;
import sun.awt.X11GraphicsEnvironment;

import sun.java2d.pipe.Region;

/*
 * This class is a collection of utility methods that operate
 * with native windows.
 */
public class XlibUtil
{
    /**
     * The constructor is made private to eliminate any
     * instances of this class
    */
    private XlibUtil()
    {
    }

    /**
     * Xinerama-aware version of XlibWrapper.RootWindow method.
     */
    public static long getRootWindow(int screenNumber)
    {
        XToolkit.awtLock();
        try
        {
            X11GraphicsEnvironment x11ge = (X11GraphicsEnvironment)
                GraphicsEnvironment.getLocalGraphicsEnvironment();
            if (x11ge.runningXinerama())
            {
                // all the Xinerama windows share the same root window
                return XlibWrapper.RootWindow(XToolkit.getDisplay(), 0);
            }
            else
            {
                return XlibWrapper.RootWindow(XToolkit.getDisplay(), screenNumber);
            }
        }
        finally
        {
            XToolkit.awtUnlock();
        }
    }

    /**
     * Checks if the given window is a root window for the given screen
     */
    static boolean isRoot(long rootCandidate, long screenNumber)
    {
        long root;

        XToolkit.awtLock();
        try
        {
            root = XlibWrapper.RootWindow(XToolkit.getDisplay(),
                                          screenNumber);
        }
        finally
        {
            XToolkit.awtUnlock();
        }

        return root == rootCandidate;
    }

    /**
     * Returns the bounds of the given window, in absolute coordinates
     */
    static Rectangle getWindowGeometry(long window, int scale)
    {
        XToolkit.awtLock();
        try
        {
            int res = XlibWrapper.XGetGeometry(XToolkit.getDisplay(),
                                               window,
                                               XlibWrapper.larg1, // root_return
                                               XlibWrapper.larg2, // x_return
                                               XlibWrapper.larg3, // y_return
                                               XlibWrapper.larg4, // width_return
                                               XlibWrapper.larg5, // height_return
                                               XlibWrapper.larg6, // border_width_return
                                               XlibWrapper.larg7); // depth_return
            if (res == 0)
            {
                return null;
            }

            int x = Native.getInt(XlibWrapper.larg2);
            int y = Native.getInt(XlibWrapper.larg3);
            long width = Native.getUInt(XlibWrapper.larg4);
            long height = Native.getUInt(XlibWrapper.larg5);

            return new Rectangle(scaleDown(x, scale), scaleDown(y, scale),
                                 scaleDown((int) width, scale),
                                 scaleDown((int) height, scale));
        }
        finally
        {
            XToolkit.awtUnlock();
        }
    }

    /**
     * Translates the given point from one window to another. Returns
     * null if the translation is failed
     */
    static Point translateCoordinates(long src, long dst, Point p, int scale)
    {
        Point translated = null;

        XToolkit.awtLock();
        try
        {
            XTranslateCoordinates xtc =
                new XTranslateCoordinates(src, dst, p.x * scale, p.y * scale);
            try
            {
                int status = xtc.execute(XErrorHandler.IgnoreBadWindowHandler.getInstance());
                if ((status != 0) &&
                    ((XErrorHandlerUtil.saved_error == null) ||
                    (XErrorHandlerUtil.saved_error.get_error_code() == XConstants.Success)))
                {
                    translated = new Point(scaleDown(xtc.get_dest_x(), scale),
                                           scaleDown(xtc.get_dest_y(), scale));
                }
            }
            finally
            {
                xtc.dispose();
            }
        }
        finally
        {
            XToolkit.awtUnlock();
        }

        return translated;
    }

    /**
     * Translates the given rectangle from one window to another.
     * Returns null if the translation is failed
     */
    static Rectangle translateCoordinates(long src, long dst, Rectangle r,
                                          int scale)
    {
        Point translatedLoc = translateCoordinates(src, dst, r.getLocation(),
                                                   scale);

        if (translatedLoc == null)
        {
            return null;
        }
        else
        {
            return new Rectangle(translatedLoc, r.getSize());
        }
    }

    /**
     * Returns the parent for the given window
     */
    static long getParentWindow(long window)
    {
        XToolkit.awtLock();
        try
        {
            XBaseWindow bw = XToolkit.windowToXWindow(window);
            if (bw != null)
            {
                XBaseWindow pbw = bw.getParentWindow();
                if (pbw != null)
                {
                    return pbw.getWindow();
                }
            }

            XQueryTree qt = new XQueryTree(window);
            try
            {
                if (qt.execute() == 0)
                {
                    return 0;
                }
                else
                {
                    return qt.get_parent();
                }
            }
            finally
            {
                qt.dispose();
            }
        }
        finally
        {
            XToolkit.awtUnlock();
        }
    }

    /**
     * Returns all the children for the given window
     */
    static Set<Long> getChildWindows(long window)
    {
        XToolkit.awtLock();
        try
        {
            XBaseWindow bw = XToolkit.windowToXWindow(window);
            if (bw != null)
            {
                return bw.getChildren();
            }

            XQueryTree xqt = new XQueryTree(window);
            try
            {
                int status = xqt.execute();
                if (status == 0)
                {
                    return Collections.emptySet();
                }

                long children = xqt.get_children();

                if (children == 0)
                {
                    return Collections.emptySet();
                }

                int childrenCount = xqt.get_nchildren();

                Set<Long> childrenSet = new HashSet<Long>(childrenCount);
                for (int i = 0; i < childrenCount; i++)
                {
                    childrenSet.add(Native.getWindow(children, i));
                }

                return childrenSet;
            }
            finally
            {
                xqt.dispose();
            }
        }
        finally
        {
            XToolkit.awtUnlock();
        }
    }

    /**
     * Checks if the given window is a Java window and is an
     * instance of XWindowPeer
     */
    static boolean isXAWTToplevelWindow(long window)
    {
        return XToolkit.windowToXWindow(window) instanceof XWindowPeer;
    }

    /**
     * NOTICE: Right now returns only decorated top-levels (not Window)
     */
    static boolean isToplevelWindow(long window)
    {
        if (XToolkit.windowToXWindow(window) instanceof XDecoratedPeer)
        {
            return true;
        }

        XToolkit.awtLock();
        try
        {
            WindowPropertyGetter wpg =
                new WindowPropertyGetter(window, XWM.XA_WM_STATE, 0, 1, false,
                                         XWM.XA_WM_STATE);
            try
            {
                wpg.execute(XErrorHandler.IgnoreBadWindowHandler.getInstance());
                if (wpg.getActualType() == XWM.XA_WM_STATE.getAtom())
                {
                    return true;
                }
            }
            finally
            {
                wpg.dispose();
            }

            return false;
        }
        finally
        {
            XToolkit.awtUnlock();
        }
    }

    /**
     * The same as isToplevelWindow(window), but doesn't treat
     * XEmbeddedFramePeer as toplevel.
     */
    static boolean isTrueToplevelWindow(long window)
    {
        if (XToolkit.windowToXWindow(window) instanceof XEmbeddedFramePeer)
        {
            return false;
        }

        return isToplevelWindow(window);
    }

    static int getWindowMapState(long window)
    {
        XToolkit.awtLock();
        XWindowAttributes wattr = new XWindowAttributes();
        try
        {
            XErrorHandlerUtil.WITH_XERROR_HANDLER(XErrorHandler.IgnoreBadWindowHandler.getInstance());
            int status = XlibWrapper.XGetWindowAttributes(XToolkit.getDisplay(),
                                                          window, wattr.pData);
            XErrorHandlerUtil.RESTORE_XERROR_HANDLER();
            if ((status != 0) &&
                ((XErrorHandlerUtil.saved_error == null) ||
                (XErrorHandlerUtil.saved_error.get_error_code() == XConstants.Success)))
            {
                return wattr.get_map_state();
            }
        }
        finally
        {
            wattr.dispose();
            XToolkit.awtUnlock();
        }

        return XConstants.IsUnmapped;
    }

    /**
     * XSHAPE extension support.
     */

    // The variable is declared static as the XSHAPE extension cannot
    // be disabled at run-time, and thus is available all the time
    // once the check is passed.
    static Boolean isShapingSupported = null;

    /**
     *  Returns whether the XSHAPE extension available
     *  @since 1.7
     */
    static synchronized boolean isShapingSupported() {

        if (isShapingSupported == null) {
            XToolkit.awtLock();
            try {
                isShapingSupported =
                    XlibWrapper.XShapeQueryExtension(
                            XToolkit.getDisplay(),
                            XlibWrapper.larg1,
                            XlibWrapper.larg2);
            } finally {
                XToolkit.awtUnlock();
            }
        }

        return isShapingSupported.booleanValue();
    }

    static int getButtonMask(int button) {
        // Button indices start with 1. The first bit in the button mask is the 8th.
        // The state mask does not support button indicies > 5, so we need to
        // cut there.
        if (button <= 0 || button > XConstants.MAX_BUTTONS) {
            return 0;
        } else {
            return 1 << (7 + button);
        }
    }

    static int scaleDown(int x, int scale) {
        return Region.clipRound(x / (double)scale);
    }
}
