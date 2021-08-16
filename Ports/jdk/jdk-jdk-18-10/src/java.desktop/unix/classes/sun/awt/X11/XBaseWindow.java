/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import sun.awt.*;
import java.util.*;
import sun.util.logging.PlatformLogger;

public class XBaseWindow {
    private static final PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11.XBaseWindow");
    private static final PlatformLogger insLog = PlatformLogger.getLogger("sun.awt.X11.insets.XBaseWindow");
    private static final PlatformLogger eventLog = PlatformLogger.getLogger("sun.awt.X11.event.XBaseWindow");
    private static final PlatformLogger focusLog = PlatformLogger.getLogger("sun.awt.X11.focus.XBaseWindow");
    private static final PlatformLogger grabLog = PlatformLogger.getLogger("sun.awt.X11.grab.XBaseWindow");

    public static final String
        PARENT_WINDOW = "parent window", // parent window, Long
        BOUNDS = "bounds", // bounds of the window, Rectangle
        OVERRIDE_REDIRECT = "overrideRedirect", // override_redirect setting, Boolean
        EVENT_MASK = "event mask", // event mask, Integer
        VALUE_MASK = "value mask", // value mask, Long
        BORDER_PIXEL = "border pixel", // border pixel value, Integer
        COLORMAP = "color map", // color map, Long
        DEPTH = "visual depth", // depth, Integer
        VISUAL_CLASS = "visual class", // visual class, Integer
        VISUAL = "visual", // visual, Long
        EMBEDDED = "embedded", // is embedded?, Boolean
        DELAYED = "delayed", // is creation delayed?, Boolean
        PARENT = "parent", // parent peer
        BACKGROUND_PIXMAP = "pixmap", // background pixmap
        VISIBLE = "visible", // whether it is visible by default
        SAVE_UNDER = "save under", // save content under this window
        BACKING_STORE = "backing store", // enables double buffering
        BIT_GRAVITY = "bit gravity"; // copy old content on geometry change
    private XCreateWindowParams delayedParams;

    Set<Long> children = new HashSet<Long>();
    long window;
    boolean visible;
    boolean mapped;
    boolean embedded;
    Rectangle maxBounds;
    volatile XBaseWindow parentWindow;

    private boolean disposed;

    private long screen;
    private XSizeHints hints;
    private XWMHints wmHints;

    static final int MIN_SIZE = 1;
    static final int DEF_LOCATION = 1;

    private static XAtom wm_client_leader;

    static enum InitialiseState {
        INITIALISING,
        INITIALISED,
        FAILED_INITIALISATION
    };

    private InitialiseState initialising;

    int x;
    int y;
    int width;
    int height;

    void awtLock() {
        XToolkit.awtLock();
    }

    void awtUnlock() {
        XToolkit.awtUnlock();
    }

    void awtLockNotifyAll() {
        XToolkit.awtLockNotifyAll();
    }

    void awtLockWait() throws InterruptedException {
        XToolkit.awtLockWait();
    }

    // To prevent errors from overriding obsolete methods
    protected final void init(long parentWindow, Rectangle bounds) {}
    protected final void preInit() {}
    protected final void postInit() {}

    // internal lock for synchronizing state changes and paint calls, initialized in preInit.
    // the order with other locks: AWTLock -> stateLock
    static class StateLock { }
    protected StateLock state_lock;

    /**
     * Called for delayed inits during construction
     */
    void instantPreInit(XCreateWindowParams params) {
        state_lock = new StateLock();
    }

    /**
     * Called before window creation, descendants should override to initialize the data,
     * initialize params.
     */
    void preInit(XCreateWindowParams params) {
        state_lock = new StateLock();
        embedded = Boolean.TRUE.equals(params.get(EMBEDDED));
        visible = Boolean.TRUE.equals(params.get(VISIBLE));

        Object parent = params.get(PARENT);
        if (parent instanceof XBaseWindow) {
            parentWindow = (XBaseWindow)parent;
        } else {
            Long parentWindowID = (Long)params.get(PARENT_WINDOW);
            if (parentWindowID != null) {
                parentWindow = XToolkit.windowToXWindow(parentWindowID);
            }
        }

        Long eventMask = (Long)params.get(EVENT_MASK);
        if (eventMask != null) {
            long mask = eventMask.longValue();
            mask |= XConstants.SubstructureNotifyMask;
            params.put(EVENT_MASK, mask);
        }

        screen = -1;
    }

    /**
     * Called after window creation, descendants should override to initialize Window
     * with class-specific values and perform post-initialization actions.
     */
    void postInit(XCreateWindowParams params) {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("WM name is " + getWMName());
        }
        updateWMName();

        // Set WM_CLIENT_LEADER property
        initClientLeader();
    }

    /**
     * Creates window using parameters {@code params}
     * If params contain flag DELAYED doesn't do anything.
     * Note: Descendants can call this method to create the window
     * at the time different to instance construction.
     */
    protected final void init(XCreateWindowParams params) {
        awtLock();
        initialising = InitialiseState.INITIALISING;
        awtUnlock();

        try {
            if (!Boolean.TRUE.equals(params.get(DELAYED))) {
                preInit(params);
                create(params);
                postInit(params);
            } else {
                instantPreInit(params);
                delayedParams = params;
            }
            awtLock();
            initialising = InitialiseState.INITIALISED;
            awtLockNotifyAll();
            awtUnlock();
        } catch (RuntimeException re) {
            awtLock();
            initialising = InitialiseState.FAILED_INITIALISATION;
            awtLockNotifyAll();
            awtUnlock();
            throw re;
        } catch (Throwable t) {
            log.warning("Exception during peer initialization", t);
            awtLock();
            initialising = InitialiseState.FAILED_INITIALISATION;
            awtLockNotifyAll();
            awtUnlock();
        }
    }

    public boolean checkInitialised() {
        awtLock();
        try {
            switch (initialising) {
              case INITIALISED:
                  return true;
              case INITIALISING:
                  try {
                      while (initialising != InitialiseState.INITIALISED) {
                          awtLockWait();
                      }
                  } catch (InterruptedException ie) {
                      return false;
                  }
                  return true;
              case FAILED_INITIALISATION:
                  return false;
              default:
                  return false;
            }
        } finally {
            awtUnlock();
        }
    }

    /*
     * Creates an invisible InputOnly window without an associated Component.
     */
    XBaseWindow() {
        this(new XCreateWindowParams());
    }

    /**
     * Creates normal child window
     */
    XBaseWindow(long parentWindow, Rectangle bounds) {
        this(new XCreateWindowParams(new Object[] {
            BOUNDS, bounds,
            PARENT_WINDOW, Long.valueOf(parentWindow)}));
    }

    /**
     * Creates top-level window
     */
    XBaseWindow(Rectangle bounds) {
        this(new XCreateWindowParams(new Object[] {
            BOUNDS, bounds
        }));
    }

    public XBaseWindow (XCreateWindowParams params) {
        init(params);
    }

    /* This create is used by the XEmbeddedFramePeer since it has to create the window
       as a child of the netscape window. This netscape window is passed in as wid */
    XBaseWindow(long parentWindow) {
        this(new XCreateWindowParams(new Object[] {
            PARENT_WINDOW, Long.valueOf(parentWindow),
            EMBEDDED, Boolean.TRUE
        }));
    }

    /**
     * Verifies that all required parameters are set. If not, sets them to default values.
     * Verifies values of critical parameters, adjust their values when needed.
     * @throws IllegalArgumentException if params is null
     */
    protected void checkParams(XCreateWindowParams params) {
        if (params == null) {
            throw new IllegalArgumentException("Window creation parameters are null");
        }
        params.putIfNull(PARENT_WINDOW, Long.valueOf(XToolkit.getDefaultRootWindow()));
        params.putIfNull(BOUNDS, new Rectangle(DEF_LOCATION, DEF_LOCATION, MIN_SIZE, MIN_SIZE));
        params.putIfNull(DEPTH, Integer.valueOf((int)XConstants.CopyFromParent));
        params.putIfNull(VISUAL, Long.valueOf(XConstants.CopyFromParent));
        params.putIfNull(VISUAL_CLASS, Integer.valueOf(XConstants.InputOnly));
        params.putIfNull(VALUE_MASK, Long.valueOf(XConstants.CWEventMask));
        Rectangle bounds = (Rectangle)params.get(BOUNDS);
        bounds.width = Math.max(MIN_SIZE, bounds.width);
        bounds.height = Math.max(MIN_SIZE, bounds.height);

        Long eventMaskObj = (Long)params.get(EVENT_MASK);
        long eventMask = eventMaskObj != null ? eventMaskObj.longValue() : 0;
        // We use our own synthetic grab see XAwtState.getGrabWindow()
        // (see X vol. 1, 8.3.3.2)
        eventMask |= XConstants.PropertyChangeMask | XConstants.OwnerGrabButtonMask;
        params.put(EVENT_MASK, Long.valueOf(eventMask));
    }

    /**
     * Returns scale factor of the window. It is used to convert native
     * coordinates to local and vice verse.
     */
    protected int getScale() {
        return 1;
    }

    protected int scaleUp(int x) {
        return x;
    }

    protected int scaleDown(int x) {
        return x;
    }

    /**
     * Creates window with parameters specified by {@code params}
     * @see #init
     */
    private void create(XCreateWindowParams params) {
        XToolkit.awtLock();
        try {
            XSetWindowAttributes xattr = new XSetWindowAttributes();
            try {
                checkParams(params);

                long value_mask = ((Long)params.get(VALUE_MASK)).longValue();

                Long eventMask = (Long)params.get(EVENT_MASK);
                xattr.set_event_mask(eventMask.longValue());
                value_mask |= XConstants.CWEventMask;

                Long border_pixel = (Long)params.get(BORDER_PIXEL);
                if (border_pixel != null) {
                    xattr.set_border_pixel(border_pixel.longValue());
                    value_mask |= XConstants.CWBorderPixel;
                }

                Long colormap = (Long)params.get(COLORMAP);
                if (colormap != null) {
                    xattr.set_colormap(colormap.longValue());
                    value_mask |= XConstants.CWColormap;
                }
                Long background_pixmap = (Long)params.get(BACKGROUND_PIXMAP);
                if (background_pixmap != null) {
                    xattr.set_background_pixmap(background_pixmap.longValue());
                    value_mask |= XConstants.CWBackPixmap;
                }

                Long parentWindow = (Long)params.get(PARENT_WINDOW);
                Rectangle bounds = (Rectangle)params.get(BOUNDS);
                Integer depth = (Integer)params.get(DEPTH);
                Integer visual_class = (Integer)params.get(VISUAL_CLASS);
                Long visual = (Long)params.get(VISUAL);
                Boolean overrideRedirect = (Boolean)params.get(OVERRIDE_REDIRECT);
                if (overrideRedirect != null) {
                    xattr.set_override_redirect(overrideRedirect.booleanValue());
                    value_mask |= XConstants.CWOverrideRedirect;
                }

                Boolean saveUnder = (Boolean)params.get(SAVE_UNDER);
                if (saveUnder != null) {
                    xattr.set_save_under(saveUnder.booleanValue());
                    value_mask |= XConstants.CWSaveUnder;
                }

                Integer backingStore = (Integer)params.get(BACKING_STORE);
                if (backingStore != null) {
                    xattr.set_backing_store(backingStore.intValue());
                    value_mask |= XConstants.CWBackingStore;
                }

                Integer bitGravity = (Integer)params.get(BIT_GRAVITY);
                if (bitGravity != null) {
                    xattr.set_bit_gravity(bitGravity.intValue());
                    value_mask |= XConstants.CWBitGravity;
                }

                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("Creating window for " + this + " with the following attributes: \n" + params);
                }
                window = XlibWrapper.XCreateWindow(XToolkit.getDisplay(),
                                                   parentWindow.longValue(),
                                                   scaleUp(bounds.x),
                                                   scaleUp(bounds.y),
                                                   scaleUp(bounds.width),
                                                   scaleUp(bounds.height),
                                                   0, // border
                                                   depth.intValue(), // depth
                                                   visual_class.intValue(), // class
                                                   visual.longValue(), // visual
                                                   value_mask,  // value mask
                                                   xattr.pData); // attributes

                if (window == 0) {
                    throw new IllegalStateException("Couldn't create window because of wrong parameters. Run with NOISY_AWT to see details");
                }
                XToolkit.addToWinMap(window, this);
            } finally {
                xattr.dispose();
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

    public XCreateWindowParams getDelayedParams() {
        return delayedParams;
    }

    protected String getWMName() {
        return XToolkit.getCorrectXIDString(getClass().getName());
    }

    protected void initClientLeader() {
        XToolkit.awtLock();
        try {
            if (wm_client_leader == null) {
                wm_client_leader = XAtom.get("WM_CLIENT_LEADER");
            }
            wm_client_leader.setWindowProperty(this, getXAWTRootWindow());
        } finally {
            XToolkit.awtUnlock();
        }
    }

    static XRootWindow getXAWTRootWindow() {
        return XRootWindow.getInstance();
    }

    void destroy() {
        XToolkit.awtLock();
        try {
            if (hints != null) {
                XlibWrapper.XFree(hints.pData);
                hints = null;
            }
            XToolkit.removeFromWinMap(getWindow(), this);
            XlibWrapper.XDestroyWindow(XToolkit.getDisplay(), getWindow());
            if (XPropertyCache.isCachingSupported()) {
                XPropertyCache.clearCache(window);
            }
            window = -1;
            if( !isDisposed() ) {
                setDisposed( true );
            }

            XAwtState.getGrabWindow(); // Magic - getGrabWindow clear state if grabbing window is disposed of.
        } finally {
            XToolkit.awtUnlock();
        }
    }

    void flush() {
        XToolkit.awtLock();
        try {
            XlibWrapper.XFlush(XToolkit.getDisplay());
        } finally {
            XToolkit.awtUnlock();
        }
    }

    /**
     * Helper function to set W
     */
    public final void setWMHints(XWMHints hints) {
        XToolkit.awtLock();
        try {
            XlibWrapper.XSetWMHints(XToolkit.getDisplay(), getWindow(), hints.pData);
        } finally {
            XToolkit.awtUnlock();
        }
    }

    public XWMHints getWMHints() {
        if (wmHints == null) {
            wmHints = new XWMHints(XlibWrapper.XAllocWMHints());
//              XlibWrapper.XGetWMHints(XToolkit.getDisplay(),
//                                      getWindow(),
//                                      wmHints.pData);
        }
        return wmHints;
    }


    /*
     * Call this method under AWTLock.
     * The lock should be acquired untill all operations with XSizeHints are completed.
     */
    public XSizeHints getHints() {
        if (hints == null) {
            long p_hints = XlibWrapper.XAllocSizeHints();
            hints = new XSizeHints(p_hints);
//              XlibWrapper.XGetWMNormalHints(XToolkit.getDisplay(), getWindow(), p_hints, XlibWrapper.larg1);
            // TODO: Shouldn't we listen for WM updates on this property?
        }
        return hints;
    }

    public void setSizeHints(long flags, int x, int y, int width, int height) {
        if (insLog.isLoggable(PlatformLogger.Level.FINER)) {
            insLog.finer("Setting hints, flags " + XlibWrapper.hintsToString(flags));
        }
        XToolkit.awtLock();
        try {
            XSizeHints hints = getHints();
            // Note: if PPosition is not set in flags this means that
            // we want to reset PPosition in hints.  This is necessary
            // for locationByPlatform functionality
            if ((flags & XUtilConstants.PPosition) != 0) {
                hints.set_x(scaleUp(x));
                hints.set_y(scaleUp(y));
            }
            if ((flags & XUtilConstants.PSize) != 0) {
                hints.set_width(scaleUp(width));
                hints.set_height(scaleUp(height));
            } else if ((hints.get_flags() & XUtilConstants.PSize) != 0) {
                flags |= XUtilConstants.PSize;
            }
            if ((flags & XUtilConstants.PMinSize) != 0) {
                hints.set_min_width(scaleUp(width));
                hints.set_min_height(scaleUp(height));
            } else if ((hints.get_flags() & XUtilConstants.PMinSize) != 0) {
                flags |= XUtilConstants.PMinSize;
                //Fix for 4320050: Minimum size for java.awt.Frame is not being enforced.
                //We don't need to reset minimum size if it's already set
            }
            if ((flags & XUtilConstants.PMaxSize) != 0) {
                if (maxBounds != null) {
                    if (maxBounds.width != Integer.MAX_VALUE) {
                        hints.set_max_width(scaleUp(maxBounds.width));
                    } else {
                        hints.set_max_width(XToolkit.getMaxWindowWidthInPixels());
                    }
                    if (maxBounds.height != Integer.MAX_VALUE) {
                        hints.set_max_height(scaleUp(maxBounds.height));
                    } else {
                        hints.set_max_height(XToolkit.getMaxWindowHeightInPixels());
                    }
                } else {
                    hints.set_max_width(scaleUp(width));
                    hints.set_max_height(scaleUp(height));
                }
            } else if ((hints.get_flags() & XUtilConstants.PMaxSize) != 0) {
                flags |= XUtilConstants.PMaxSize;
                if (maxBounds != null) {
                    if (maxBounds.width != Integer.MAX_VALUE) {
                        hints.set_max_width(scaleUp(maxBounds.width));
                    } else {
                        hints.set_max_width(XToolkit.getMaxWindowWidthInPixels());
                    }
                    if (maxBounds.height != Integer.MAX_VALUE) {
                        hints.set_max_height(scaleUp(maxBounds.height));
                    } else {
                        hints.set_max_height(XToolkit.getMaxWindowHeightInPixels());
                    }
                } else {
                    // Leave intact
                }
            }
            flags |= XUtilConstants.PWinGravity;
            hints.set_flags(flags);
            hints.set_win_gravity(XConstants.NorthWestGravity);
            if (insLog.isLoggable(PlatformLogger.Level.FINER)) {
                insLog.finer("Setting hints, resulted flags " + XlibWrapper.hintsToString(flags) +
                             ", values " + hints);
            }
            XlibWrapper.XSetWMNormalHints(XToolkit.getDisplay(), getWindow(), hints.pData);
        } finally {
            XToolkit.awtUnlock();
        }
    }

    public boolean isMinSizeSet() {
        XSizeHints hints = getHints();
        long flags = hints.get_flags();
        return ((flags & XUtilConstants.PMinSize) == XUtilConstants.PMinSize);
    }

    /**
     * This lock object can be used to protect instance data from concurrent access
     * by two threads. If both state lock and AWT lock are taken, AWT Lock should be taken first.
     */
    Object getStateLock() {
        return state_lock;
    }

    public long getWindow() {
        return window;
    }
    public long getContentWindow() {
        return window;
    }

    public XBaseWindow getContentXWindow() {
        return XToolkit.windowToXWindow(getContentWindow());
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
    public Dimension getSize() {
        return new Dimension(width, height);
    }


    public void toFront() {
        XToolkit.awtLock();
        try {
            XlibWrapper.XRaiseWindow(XToolkit.getDisplay(), getWindow());
        } finally {
            XToolkit.awtUnlock();
        }
    }
    public void xRequestFocus(long time) {
        XToolkit.awtLock();
        try {
            if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
                focusLog.finer("XSetInputFocus on " + Long.toHexString(getWindow()) + " with time " + time);
            }
            XlibWrapper.XSetInputFocus2(XToolkit.getDisplay(), getWindow(), time);
        } finally {
            XToolkit.awtUnlock();
        }
    }
    public void xRequestFocus() {
        XToolkit.awtLock();
        try {
            if (focusLog.isLoggable(PlatformLogger.Level.FINER)) {
                focusLog.finer("XSetInputFocus on " + Long.toHexString(getWindow()));
            }
             XlibWrapper.XSetInputFocus(XToolkit.getDisplay(), getWindow());
        } finally {
            XToolkit.awtUnlock();
        }
    }

    public static long xGetInputFocus() {
        XToolkit.awtLock();
        try {
            return XlibWrapper.XGetInputFocus(XToolkit.getDisplay());
        } finally {
            XToolkit.awtUnlock();
        }
    }

    public void xSetVisible(boolean visible) {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("Setting visible on " + this + " to " + visible);
        }
        XToolkit.awtLock();
        try {
            this.visible = visible;
            if (visible) {
                XlibWrapper.XMapWindow(XToolkit.getDisplay(), getWindow());
            }
            else {
                XlibWrapper.XUnmapWindow(XToolkit.getDisplay(), getWindow());
            }
            XlibWrapper.XFlush(XToolkit.getDisplay());
        } finally {
            XToolkit.awtUnlock();
        }
    }

    boolean isMapped() {
        return mapped;
    }

    void updateWMName() {
        String name = getWMName();
        XToolkit.awtLock();
        try {
            if (name == null) {
                name = " ";
            }
            XAtom nameAtom = XAtom.get(XAtom.XA_WM_NAME);
            nameAtom.setProperty(getWindow(), name);
            XAtom netNameAtom = XAtom.get("_NET_WM_NAME");
            netNameAtom.setPropertyUTF8(getWindow(), name);
        } finally {
            XToolkit.awtUnlock();
        }
    }
    void setWMClass(String[] cl) {
        if (cl.length != 2) {
            throw new IllegalArgumentException("WM_CLASS_NAME consists of exactly two strings");
        }
        XToolkit.awtLock();
        try {
            XAtom xa = XAtom.get(XAtom.XA_WM_CLASS);
            xa.setProperty8(getWindow(), cl[0] + '\0' + cl[1] + '\0');
        } finally {
            XToolkit.awtUnlock();
        }
    }

    boolean isVisible() {
        return visible;
    }

    static long getScreenOfWindow(long window) {
        XToolkit.awtLock();
        try {
            return XlibWrapper.getScreenOfWindow(XToolkit.getDisplay(), window);
        } finally {
            XToolkit.awtUnlock();
        }
    }
    long getScreenNumber() {
        XToolkit.awtLock();
        try {
            return XlibWrapper.XScreenNumberOfScreen(getScreen());
        } finally {
            XToolkit.awtUnlock();
        }
    }

    long getScreen() {
        if (screen == -1) { // Not initialized
            screen = getScreenOfWindow(window);
        }
        return screen;
    }

    public void xSetBounds(Rectangle bounds) {
        xSetBounds(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public void xSetBounds(int x, int y, int width, int height) {
        if (getWindow() == 0) {
            insLog.warning("Attempt to resize uncreated window");
            throw new IllegalStateException("Attempt to resize uncreated window");
        }
        if (insLog.isLoggable(PlatformLogger.Level.FINE)) {
            insLog.fine("Setting bounds on " + this + " to (" + x + ", " + y + "), " + width + "x" + height);
        }
        width = Math.max(MIN_SIZE, width);
        height = Math.max(MIN_SIZE, height);
        XToolkit.awtLock();
        try {
            XlibWrapper.XMoveResizeWindow(XToolkit.getDisplay(), getWindow(),
                                          scaleUp(x), scaleUp(y),
                                          scaleUp(width), scaleUp(height));
        } finally {
            XToolkit.awtUnlock();
        }
    }

    /**
     * Translate coordinates from one window into another.  Optimized
     * for XAWT - uses cached data when possible.  Preferable over
     * pure XTranslateCoordinates.
     * @return coordinates relative to dst, or null if error happened
     */
    static Point toOtherWindow(long src, long dst, int x, int y) {
        Point rpt = new Point(0, 0);

        // Check if both windows belong to XAWT - then no X calls are necessary

        XBaseWindow srcPeer = XToolkit.windowToXWindow(src);
        XBaseWindow dstPeer = XToolkit.windowToXWindow(dst);

        if (srcPeer != null && dstPeer != null) {
            // (x, y) is relative to src
            rpt.x = x + srcPeer.getAbsoluteX() - dstPeer.getAbsoluteX();
            rpt.y = y + srcPeer.getAbsoluteY() - dstPeer.getAbsoluteY();
        } else if (dstPeer != null && XlibUtil.isRoot(src, dstPeer.getScreenNumber())) {
            // from root into peer
            rpt.x = x - dstPeer.getAbsoluteX();
            rpt.y = y - dstPeer.getAbsoluteY();
        } else if (srcPeer != null && XlibUtil.isRoot(dst, srcPeer.getScreenNumber())) {
            // from peer into root
            rpt.x = x + srcPeer.getAbsoluteX();
            rpt.y = y + srcPeer.getAbsoluteY();
        } else {
            int scale = srcPeer == null ? 1 : srcPeer.getScale();
            rpt = XlibUtil.translateCoordinates(src, dst, new Point(x, y), scale);
        }
        return rpt;
    }

    /*
     * Convert to global coordinates.
     */
    Rectangle toGlobal(Rectangle rec) {
        Point p = toGlobal(rec.getLocation());
        Rectangle newRec = new Rectangle(rec);
        if (p != null) {
            newRec.setLocation(p);
        }
        return newRec;
    }

    Point toGlobal(Point pt) {
        Point p = toGlobal(pt.x, pt.y);
        if (p != null) {
            return p;
        } else {
            return new Point(pt);
        }
    }

    Point toGlobal(int x, int y) {
        long root;
        XToolkit.awtLock();
        try {
            root = XlibWrapper.RootWindow(XToolkit.getDisplay(),
                    getScreenNumber());
        } finally {
            XToolkit.awtUnlock();
        }
        Point p = toOtherWindow(getContentWindow(), root, x, y);
        if (p != null) {
            return p;
        } else {
            return new Point(x, y);
        }
    }

    /*
     * Convert to local coordinates.
     */
    Point toLocal(Point pt) {
        Point p = toLocal(pt.x, pt.y);
        if (p != null) {
            return p;
        } else {
            return new Point(pt);
        }
    }

    Point toLocal(int x, int y) {
        long root;
        XToolkit.awtLock();
        try {
            root = XlibWrapper.RootWindow(XToolkit.getDisplay(),
                    getScreenNumber());
        } finally {
            XToolkit.awtUnlock();
        }
        Point p = toOtherWindow(root, getContentWindow(), x, y);
        if (p != null) {
            return p;
        } else {
            return new Point(x, y);
        }
    }

    /**
     * We should always grab both keyboard and pointer to control event flow
     * on popups. This also simplifies synthetic grab implementation.
     * The active grab overrides activated automatic grab.
     */
    public boolean grabInput() {
        if (grabLog.isLoggable(PlatformLogger.Level.FINE)) {
            grabLog.fine("Grab input on {0}", this);
        }

        XToolkit.awtLock();
        try {
            if (XAwtState.getGrabWindow() == this &&
                XAwtState.isManualGrab())
            {
                grabLog.fine("    Already Grabbed");
                return true;
            }
            //6273031: PIT. Choice drop down does not close once it is right clicked to show a popup menu
            //remember previous window having grab and if it's not null ungrab it.
            XBaseWindow prevGrabWindow = XAwtState.getGrabWindow();
            final int eventMask = (int) (XConstants.ButtonPressMask | XConstants.ButtonReleaseMask
                | XConstants.EnterWindowMask | XConstants.LeaveWindowMask | XConstants.PointerMotionMask
                | XConstants.ButtonMotionMask);
            final int ownerEvents = 1;


            //6714678: IDE (Netbeans, Eclipse, JDeveloper) Debugger hangs
            //process on Linux
            //The user must pass the sun.awt.disablegrab property to disable
            //taking grabs. This prevents hanging of the GUI when a breakpoint
            //is hit while a popup window taking the grab is open.
            if (!XToolkit.getSunAwtDisableGrab()) {
                int ptrGrab = XlibWrapper.XGrabPointer(XToolkit.getDisplay(),
                        getContentWindow(), ownerEvents, eventMask, XConstants.GrabModeAsync,
                        XConstants.GrabModeAsync, XConstants.None, (XWM.isMotif() ? XToolkit.arrowCursor : XConstants.None),
                        XConstants.CurrentTime);
                // Check grab results to be consistent with X server grab
                if (ptrGrab != XConstants.GrabSuccess) {
                    XlibWrapper.XUngrabPointer(XToolkit.getDisplay(), XConstants.CurrentTime);
                    XAwtState.setGrabWindow(null);
                    grabLog.fine("    Grab Failure - mouse");
                    return false;
                }

                int keyGrab = XlibWrapper.XGrabKeyboard(XToolkit.getDisplay(),
                        getContentWindow(), ownerEvents, XConstants.GrabModeAsync, XConstants.GrabModeAsync,
                        XConstants.CurrentTime);
                if (keyGrab != XConstants.GrabSuccess) {
                    XlibWrapper.XUngrabPointer(XToolkit.getDisplay(), XConstants.CurrentTime);
                    XlibWrapper.XUngrabKeyboard(XToolkit.getDisplay(), XConstants.CurrentTime);
                    XAwtState.setGrabWindow(null);
                    grabLog.fine("    Grab Failure - keyboard");
                    return false;
                }
            }
            if (prevGrabWindow != null) {
                prevGrabWindow.ungrabInputImpl();
            }
            XAwtState.setGrabWindow(this);
            grabLog.fine("    Grab - success");
            return true;
        } finally {
            XToolkit.awtUnlock();
        }
    }

    static void ungrabInput() {
        XToolkit.awtLock();
        try {
            XBaseWindow grabWindow = XAwtState.getGrabWindow();
            if (grabLog.isLoggable(PlatformLogger.Level.FINE)) {
                grabLog.fine("UnGrab input on {0}", grabWindow);
            }
            if (grabWindow != null) {
                grabWindow.ungrabInputImpl();
                if (!XToolkit.getSunAwtDisableGrab()) {
                    XlibWrapper.XUngrabPointer(XToolkit.getDisplay(), XConstants.CurrentTime);
                    XlibWrapper.XUngrabKeyboard(XToolkit.getDisplay(), XConstants.CurrentTime);
                }
                XAwtState.setGrabWindow(null);
                // we need to call XFlush() here to force ungrab
                // see 6384219 for details
                XlibWrapper.XFlush(XToolkit.getDisplay());
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

    // called from ungrabInput, used in popup windows to hide theirselfs in ungrabbing
    void ungrabInputImpl() {
    }

    static void checkSecurity() {
        if (XToolkit.isSecurityWarningEnabled() && XToolkit.isToolkitThread()) {
            StackTraceElement[] stack = (new Throwable()).getStackTrace();
            log.warning(stack[1] + ": Security violation: calling user code on toolkit thread");
        }
    }

    public Set<Long> getChildren() {
        synchronized (getStateLock()) {
            return new HashSet<Long>(children);
        }
    }

    // -------------- Event handling ----------------
    public void handleMapNotifyEvent(XEvent xev) {
        mapped = true;
    }
    public void handleUnmapNotifyEvent(XEvent xev) {
        mapped = false;
    }
    public void handleReparentNotifyEvent(XEvent xev) {
        if (eventLog.isLoggable(PlatformLogger.Level.FINER)) {
            XReparentEvent msg = xev.get_xreparent();
            eventLog.finer(msg.toString());
        }
    }
    public void handlePropertyNotify(XEvent xev) {
        XPropertyEvent msg = xev.get_xproperty();
        if (XPropertyCache.isCachingSupported()) {
            XPropertyCache.clearCache(window, XAtom.get(msg.get_atom()));
        }
        if (eventLog.isLoggable(PlatformLogger.Level.FINER)) {
            eventLog.finer("{0}", msg);
        }
    }

    public void handleDestroyNotify(XEvent xev) {
        XAnyEvent xany = xev.get_xany();
        if (xany.get_window() == getWindow()) {
            XToolkit.removeFromWinMap(getWindow(), this);
            if (XPropertyCache.isCachingSupported()) {
                XPropertyCache.clearCache(getWindow());
            }
        }
        if (xany.get_window() != getWindow()) {
            synchronized (getStateLock()) {
                children.remove(xany.get_window());
            }
        }
    }

    public void handleCreateNotify(XEvent xev) {
        XAnyEvent xany = xev.get_xany();
        if (xany.get_window() != getWindow()) {
            synchronized (getStateLock()) {
                children.add(xany.get_window());
            }
        }
    }

    public void handleClientMessage(XEvent xev) {
        if (eventLog.isLoggable(PlatformLogger.Level.FINER)) {
            XClientMessageEvent msg = xev.get_xclient();
            eventLog.finer(msg.toString());
        }
    }

    public void handleVisibilityEvent(XEvent xev) {
    }
    public void handleKeyPress(XEvent xev) {
    }
    public void handleKeyRelease(XEvent xev) {
    }
    public void handleExposeEvent(XEvent xev) {
    }
    /**
     * Activate automatic grab on first ButtonPress,
     * deactivate on full mouse release
     */
    public void handleButtonPressRelease(XEvent xev) {
        XButtonEvent xbe = xev.get_xbutton();
        /*
         * Ignore the buttons above 20 due to the bit limit for
         * InputEvent.BUTTON_DOWN_MASK.
         * One more bit is reserved for FIRST_HIGH_BIT.
         */
        int theButton = xbe.get_button();
        if (theButton > SunToolkit.MAX_BUTTONS_SUPPORTED) {
            return;
        }
        int buttonState = 0;
        buttonState = xbe.get_state() & XConstants.ALL_BUTTONS_MASK;

        boolean isWheel = (theButton == XConstants.MouseWheelUp ||
                           theButton == XConstants.MouseWheelDown);

        // don't give focus if it's just the mouse wheel turning
        if (!isWheel) {
            switch (xev.get_type()) {
                case XConstants.ButtonPress:
                    if (buttonState == 0) {
                        XWindowPeer parent = getToplevelXWindow();
                        // See 6385277, 6981400.
                        if (parent != null && parent.isFocusableWindow()) {
                            // A click in a client area drops the actual focused window retaining.
                            parent.setActualFocusedWindow(null);
                            parent.requestWindowFocus(xbe.get_time(), true);
                        }
                        XAwtState.setAutoGrabWindow(this);
                    }
                    break;
                case XConstants.ButtonRelease:
                    if (isFullRelease(buttonState, xbe.get_button())) {
                        XAwtState.setAutoGrabWindow(null);
                    }
                    break;
            }
        }
    }
    public void handleMotionNotify(XEvent xev) {
    }
    public void handleXCrossingEvent(XEvent xev) {
    }
    public void handleConfigureNotifyEvent(XEvent xev) {
        XConfigureEvent xe = xev.get_xconfigure();
        if (insLog.isLoggable(PlatformLogger.Level.FINER)) {
            insLog.finer("Configure, {0}", xe);
        }

        x = scaleDown(xe.get_x());
        y = scaleDown(xe.get_y());
        width = scaleDown(xe.get_width());
        height = scaleDown(xe.get_height());
    }
    /**
     * Checks ButtonRelease released all Mouse buttons
     */
    static boolean isFullRelease(int buttonState, int button) {
        final int buttonsNumber = XToolkit.getNumberOfButtonsForMask();

        if (button < 0 || button > buttonsNumber) {
            return buttonState == 0;
        } else {
            return buttonState == XlibUtil.getButtonMask(button);
        }
    }

    static boolean isGrabbedEvent(XEvent ev, XBaseWindow target) {
        switch (ev.get_type()) {
          case XConstants.ButtonPress:
          case XConstants.ButtonRelease:
          case XConstants.MotionNotify:
          case XConstants.KeyPress:
          case XConstants.KeyRelease:
              return true;
          case XConstants.LeaveNotify:
          case XConstants.EnterNotify:
              // We shouldn't dispatch this events to the grabbed components (see 6317481)
              // But this logic is important if the grabbed component is top-level (see realSync)
              return (target instanceof XWindowPeer);
          default:
              return false;
        }
    }
    /**
     * Dispatches event to the grab Window or event source window depending
     * on whether the grab is active and on the event type
     */
    static void dispatchToWindow(XEvent ev) {
        XBaseWindow target = XAwtState.getGrabWindow();
        if (target == null || !isGrabbedEvent(ev, target)) {
            target = XToolkit.windowToXWindow(ev.get_xany().get_window());
        }
        if (target != null && target.checkInitialised()) {
            target.dispatchEvent(ev);
        }
    }

    public void dispatchEvent(XEvent xev) {
        if (eventLog.isLoggable(PlatformLogger.Level.FINEST)) {
            eventLog.finest(xev.toString());
        }
        int type = xev.get_type();

        if (isDisposed()) {
            return;
        }

        switch (type)
        {
          case XConstants.VisibilityNotify:
              handleVisibilityEvent(xev);
              break;
          case XConstants.ClientMessage:
              handleClientMessage(xev);
              break;
          case XConstants.Expose :
          case XConstants.GraphicsExpose :
              handleExposeEvent(xev);
              break;
          case XConstants.ButtonPress:
          case XConstants.ButtonRelease:
              handleButtonPressRelease(xev);
              break;

          case XConstants.MotionNotify:
              handleMotionNotify(xev);
              break;
          case XConstants.KeyPress:
              handleKeyPress(xev);
              break;
          case XConstants.KeyRelease:
              handleKeyRelease(xev);
              break;
          case XConstants.EnterNotify:
          case XConstants.LeaveNotify:
              handleXCrossingEvent(xev);
              break;
          case XConstants.ConfigureNotify:
              handleConfigureNotifyEvent(xev);
              break;
          case XConstants.MapNotify:
              handleMapNotifyEvent(xev);
              break;
          case XConstants.UnmapNotify:
              handleUnmapNotifyEvent(xev);
              break;
          case XConstants.ReparentNotify:
              handleReparentNotifyEvent(xev);
              break;
          case XConstants.PropertyNotify:
              handlePropertyNotify(xev);
              break;
          case XConstants.DestroyNotify:
              handleDestroyNotify(xev);
              break;
          case XConstants.CreateNotify:
              handleCreateNotify(xev);
              break;
        }
    }
    protected boolean isEventDisabled(XEvent e) {
        return false;
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    int getWidth() {
        return width;
    }

    int getHeight() {
        return height;
    }

    void setDisposed(boolean d) {
        disposed = d;
    }

    boolean isDisposed() {
        return disposed;
    }

    public int getAbsoluteX() {
        XBaseWindow pw = getParentWindow();
        if (pw != null) {
            return pw.getAbsoluteX() + getX();
        } else {
            // Overridden for top-levels as their (x,y) is Java (x, y), not native location
            return getX();
        }
    }

    public int getAbsoluteY() {
        XBaseWindow pw = getParentWindow();
        if (pw != null) {
            return pw.getAbsoluteY() + getY();
        } else {
            return getY();
        }
    }

    public XBaseWindow getParentWindow() {
        return parentWindow;
    }

    public XWindowPeer getToplevelXWindow() {
        XBaseWindow bw = this;
        while (bw != null && !(bw instanceof XWindowPeer)) {
            bw = bw.getParentWindow();
        }
        return (XWindowPeer)bw;
    }
    public String toString() {
        return super.toString() + "(" + Long.toString(getWindow(), 16) + ")";
    }

    /**
     * Returns whether the given point is inside of the window.  Coordinates are local.
     */
    public boolean contains(int x, int y) {
        return x >= 0 && y >= 0 && x < getWidth() && y < getHeight();
    }

    /**
     * Returns whether the given point is inside of the window.  Coordinates are global.
     */
    public boolean containsGlobal(int x, int y) {
        return x >= getAbsoluteX() && y >= getAbsoluteY() && x < (getAbsoluteX()+getWidth()) && y < (getAbsoluteY()+getHeight());
    }

}
