/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTError;
import java.awt.AWTException;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.CheckboxMenuItem;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Insets;
import java.awt.JobAttributes;
import java.awt.Label;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.PageAttributes;
import java.awt.Panel;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.PrintJob;
import java.awt.Rectangle;
import java.awt.ScrollPane;
import java.awt.Scrollbar;
import java.awt.SystemColor;
import java.awt.SystemTray;
import java.awt.Taskbar;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.dnd.MouseDragGestureRecognizer;
import java.awt.dnd.peer.DragSourceContextPeer;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.im.InputMethodHighlight;
import java.awt.im.spi.InputMethodDescriptor;
import java.awt.peer.ButtonPeer;
import java.awt.peer.CanvasPeer;
import java.awt.peer.CheckboxMenuItemPeer;
import java.awt.peer.CheckboxPeer;
import java.awt.peer.ChoicePeer;
import java.awt.peer.DesktopPeer;
import java.awt.peer.DialogPeer;
import java.awt.peer.FileDialogPeer;
import java.awt.peer.FontPeer;
import java.awt.peer.FramePeer;
import java.awt.peer.KeyboardFocusManagerPeer;
import java.awt.peer.LabelPeer;
import java.awt.peer.ListPeer;
import java.awt.peer.MenuBarPeer;
import java.awt.peer.MenuItemPeer;
import java.awt.peer.MenuPeer;
import java.awt.peer.MouseInfoPeer;
import java.awt.peer.PanelPeer;
import java.awt.peer.PopupMenuPeer;
import java.awt.peer.RobotPeer;
import java.awt.peer.ScrollPanePeer;
import java.awt.peer.ScrollbarPeer;
import java.awt.peer.SystemTrayPeer;
import java.awt.peer.TaskbarPeer;
import java.awt.peer.TextAreaPeer;
import java.awt.peer.TextFieldPeer;
import java.awt.peer.TrayIconPeer;
import java.awt.peer.WindowPeer;
import java.beans.PropertyChangeListener;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;

import sun.awt.AWTAccessor;
import sun.awt.AWTPermissions;
import sun.awt.AppContext;
import sun.awt.DisplayChangedListener;
import sun.awt.LightweightFrame;
import sun.awt.SunToolkit;
import sun.awt.UNIXToolkit;
import sun.awt.X11GraphicsConfig;
import sun.awt.X11GraphicsDevice;
import sun.awt.X11GraphicsEnvironment;
import sun.awt.XSettings;
import sun.awt.datatransfer.DataTransferer;
import sun.awt.util.PerformanceLogger;
import sun.awt.util.ThreadGroupUtils;
import sun.font.FontConfigManager;
import sun.java2d.SunGraphicsEnvironment;
import sun.print.PrintJob2D;
import sun.security.action.GetBooleanAction;
import sun.security.action.GetPropertyAction;
import sun.util.logging.PlatformLogger;

import static sun.awt.X11.XlibUtil.scaleDown;

public final class XToolkit extends UNIXToolkit implements Runnable {
    private static final PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11.XToolkit");
    private static final PlatformLogger eventLog = PlatformLogger.getLogger("sun.awt.X11.event.XToolkit");
    private static final PlatformLogger timeoutTaskLog = PlatformLogger.getLogger("sun.awt.X11.timeoutTask.XToolkit");
    private static final PlatformLogger keyEventLog = PlatformLogger.getLogger("sun.awt.X11.kye.XToolkit");
    private static final PlatformLogger backingStoreLog = PlatformLogger.getLogger("sun.awt.X11.backingStore.XToolkit");

    //There is 400 ms is set by default on Windows and 500 by default on KDE and GNOME.
    //We use the same hardcoded constant.
    private static final int AWT_MULTICLICK_DEFAULT_TIME = 500;

    static final boolean PRIMARY_LOOP = false;
    static final boolean SECONDARY_LOOP = true;

    private static String awtAppClassName = null;

    // the system clipboard - CLIPBOARD selection
    XClipboard clipboard;
    // the system selection - PRIMARY selection
    XClipboard selection;

    // Dynamic Layout Resize client code setting
    protected static boolean dynamicLayoutSetting = false;

    //Is it allowed to generate events assigned to extra mouse buttons.
    //Set to true by default.
    private static boolean areExtraMouseButtonsEnabled = true;

    /**
     * True when the x settings have been loaded.
     */
    private boolean loadedXSettings;

    /**
    * XSETTINGS for the default screen.
     * <p>
     */
    private XSettings xs;

    private FontConfigManager fcManager = new FontConfigManager();

    static int arrowCursor;
    static TreeMap<Long, XBaseWindow> winMap = new TreeMap<>();
    static HashMap<Object, Object> specialPeerMap = new HashMap<>();
    static HashMap<Long, Collection<XEventDispatcher>> winToDispatcher = new HashMap<>();
    static UIDefaults uidefaults;
    static final X11GraphicsEnvironment localEnv;
    private static final X11GraphicsDevice device;
    private static final long display;
    static int awt_multiclick_time;
    static boolean securityWarningEnabled;

    /**
     * Dimensions of default virtual screen in pixels. These values are used to
     * limit the maximum size of the window.
     */
    private static volatile int maxWindowWidthInPixels = -1;
    private static volatile int maxWindowHeightInPixels = -1;

    private static XMouseInfoPeer xPeer;

    static {
        initSecurityWarning();
        if (GraphicsEnvironment.isHeadless()) {
            localEnv = null;
            device = null;
            display = 0;
        } else {
            localEnv = (X11GraphicsEnvironment) GraphicsEnvironment
                .getLocalGraphicsEnvironment();
            device = (X11GraphicsDevice) localEnv.getDefaultScreenDevice();
            display = device.getDisplay();
            setupModifierMap();
            initIDs();
            setBackingStoreType();
        }
    }

    /*
     * Return (potentially) platform specific display timeout for the
     * tray icon
     */
    static native long getTrayIconDisplayTimeout();

    private static native void initIDs();
    static native void waitForEvents(long nextTaskTime);
    static Thread toolkitThread;
    static boolean isToolkitThread() {
        return Thread.currentThread() == toolkitThread;
    }

    static void initSecurityWarning() {
        // Enable warning only for internal builds
        @SuppressWarnings("removal")
        String runtime = AccessController.doPrivileged(
                             new GetPropertyAction("java.runtime.version"));
        securityWarningEnabled = (runtime != null && runtime.contains("internal"));
    }

    static boolean isSecurityWarningEnabled() {
        return securityWarningEnabled;
    }

    static native void awt_output_flush();

    static void  awtFUnlock() {
        awtUnlock();
        awt_output_flush();
    }


    private native void nativeLoadSystemColors(int[] systemColors);

    static UIDefaults getUIDefaults() {
        if (uidefaults == null) {
            initUIDefaults();
        }
        return uidefaults;
    }

    @Override
    public void loadSystemColors(int[] systemColors) {
        nativeLoadSystemColors(systemColors);
        MotifColorUtilities.loadSystemColors(systemColors);
    }



    static void initUIDefaults() {
        try {
            // Load Defaults from MotifLookAndFeel

            // This dummy load is necessary to get SystemColor initialized. !!!!!!
            Color c = SystemColor.text;

            LookAndFeel lnf = new XAWTLookAndFeel();
            uidefaults = lnf.getDefaults();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Returns the X11 Display of the default screen device.
     *
     * @return X11 Display
     * @throws AWTError thrown if local GraphicsEnvironment is null, which
     *         means we are in the headless environment
     */
    public static long getDisplay() {
        if (localEnv == null) {
            throw new AWTError("Local GraphicsEnvironment must not be null");
        }
        return display;
    }

    public static long getDefaultRootWindow() {
        awtLock();
        try {
            long res = XlibWrapper.RootWindow(XToolkit.getDisplay(),
                XlibWrapper.DefaultScreen(XToolkit.getDisplay()));

            if (res == 0) {
               throw new IllegalStateException("Root window must not be null");
            }
            return res;
        } finally {
            awtUnlock();
        }
    }

    @SuppressWarnings("removal")
    void init() {
        awtLock();
        try {
            XlibWrapper.XSupportsLocale();
            if (XlibWrapper.XSetLocaleModifiers("") == null) {
                log.finer("X locale modifiers are not supported, using default");
            }
            tryXKB();

            arrowCursor = XlibWrapper.XCreateFontCursor(XToolkit.getDisplay(),
                XCursorFontConstants.XC_arrow);
            final String extraButtons = "sun.awt.enableExtraMouseButtons";
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                areExtraMouseButtonsEnabled =
                    Boolean.parseBoolean(System.getProperty(extraButtons, "true"));
                //set system property if not yet assigned
                System.setProperty(extraButtons, ""+areExtraMouseButtonsEnabled);
                return null;
            });
            // Detect display mode changes
            XlibWrapper.XSelectInput(XToolkit.getDisplay(), XToolkit.getDefaultRootWindow(), XConstants.StructureNotifyMask);
            XToolkit.addEventDispatcher(XToolkit.getDefaultRootWindow(), new XEventDispatcher() {
                @Override
                public void dispatchEvent(XEvent ev) {
                    if (ev.get_type() == XConstants.ConfigureNotify) {
                        awtUnlock();
                        try {
                            ((X11GraphicsEnvironment)GraphicsEnvironment.
                             getLocalGraphicsEnvironment()).rebuildDevices();
                        } finally {
                            awtLock();
                        }
                    }
                }
            });
        } finally {
            awtUnlock();
        }
        PrivilegedAction<Void> a = () -> {
            Runnable r = () -> {
                XSystemTrayPeer peer = XSystemTrayPeer.getPeerInstance();
                if (peer != null) {
                    peer.dispose();
                }
                if (xs != null) {
                    ((XAWTXSettings)xs).dispose();
                }
                freeXKB();
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    dumpPeers();
                }
            };
            String name = "XToolkt-Shutdown-Thread";
            Thread shutdownThread = new Thread(
                    ThreadGroupUtils.getRootThreadGroup(), r, name, 0, false);
            shutdownThread.setContextClassLoader(null);
            Runtime.getRuntime().addShutdownHook(shutdownThread);
            return null;
        };
        AccessController.doPrivileged(a);
    }

    static String getCorrectXIDString(String val) {
        if (val != null) {
            return val.replace('.', '-');
        } else {
            return val;
        }
    }

    static native String getEnv(String key);


    static String getAWTAppClassName() {
        return awtAppClassName;
    }

    @SuppressWarnings("removal")
    public XToolkit() {
        super();
        if (PerformanceLogger.loggingEnabled()) {
            PerformanceLogger.setTime("XToolkit construction");
        }

        if (!GraphicsEnvironment.isHeadless()) {
            String mainClassName = null;

            StackTraceElement[] trace = (new Throwable()).getStackTrace();
            int bottom = trace.length - 1;
            if (bottom >= 0) {
                mainClassName = trace[bottom].getClassName();
            }
            if (mainClassName == null || mainClassName.isEmpty()) {
                mainClassName = "AWT";
            }
            awtAppClassName = getCorrectXIDString(mainClassName);

            init();
            XWM.init();

            toolkitThread = AccessController.doPrivileged((PrivilegedAction<Thread>) () -> {
                String name = "AWT-XAWT";
                Thread thread = new Thread(
                        ThreadGroupUtils.getRootThreadGroup(), this, name,
                        0, false);
                thread.setContextClassLoader(null);
                thread.setPriority(Thread.NORM_PRIORITY + 1);
                thread.setDaemon(true);
                return thread;
            });
            toolkitThread.start();
        }
    }

    @Override
    public ButtonPeer createButton(Button target) {
        ButtonPeer peer = new XButtonPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public FramePeer createLightweightFrame(LightweightFrame target) {
        FramePeer peer = new XLightweightFramePeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public FramePeer createFrame(Frame target) {
        FramePeer peer = new XFramePeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    static void addToWinMap(long window, XBaseWindow xwin)
    {
        synchronized(winMap) {
            winMap.put(Long.valueOf(window),xwin);
        }
    }

    static void removeFromWinMap(long window, XBaseWindow xwin) {
        synchronized(winMap) {
            winMap.remove(Long.valueOf(window));
        }
    }
    static XBaseWindow windowToXWindow(long window) {
        synchronized(winMap) {
            return winMap.get(Long.valueOf(window));
        }
    }

    static void addEventDispatcher(long window, XEventDispatcher dispatcher) {
        synchronized(winToDispatcher) {
            Long key = Long.valueOf(window);
            Collection<XEventDispatcher> dispatchers = winToDispatcher.get(key);
            if (dispatchers == null) {
                dispatchers = new Vector<>();
                winToDispatcher.put(key, dispatchers);
            }
            dispatchers.add(dispatcher);
        }
    }
    static void removeEventDispatcher(long window, XEventDispatcher dispatcher) {
        synchronized(winToDispatcher) {
            Long key = Long.valueOf(window);
            Collection<XEventDispatcher> dispatchers = winToDispatcher.get(key);
            if (dispatchers != null) {
                dispatchers.remove(dispatcher);
            }
        }
    }

    private Point lastCursorPos;

    /**
     * Returns whether there is last remembered cursor position.  The
     * position is remembered from X mouse events on our peers.  The
     * position is stored in {@code p}.
     * @return true, if there is remembered last cursor position,
     * false otherwise
     */
    boolean getLastCursorPos(Point p) {
        awtLock();
        try {
            if (lastCursorPos == null) {
                return false;
            }
            p.setLocation(lastCursorPos);
            return true;
        } finally {
            awtUnlock();
        }
    }

    private void processGlobalMotionEvent(XEvent e, XBaseWindow win) {
        // Only our windows guaranteely generate MotionNotify, so we
        // should track enter/leave, to catch the moment when to
        // switch to XQueryPointer
        if (e.get_type() == XConstants.MotionNotify) {
            XMotionEvent ev = e.get_xmotion();
            awtLock();
            try {
                if (lastCursorPos == null) {
                    lastCursorPos = new Point(win.scaleDown(ev.get_x_root()),
                                              win.scaleDown(ev.get_y_root()));
                } else {
                    lastCursorPos.setLocation(win.scaleDown(ev.get_x_root()),
                                              win.scaleDown(ev.get_y_root()));
                }
            } finally {
                awtUnlock();
            }
        } else if (e.get_type() == XConstants.LeaveNotify) {
            // Leave from our window
            awtLock();
            try {
                lastCursorPos = null;
            } finally {
                awtUnlock();
            }
        } else if (e.get_type() == XConstants.EnterNotify) {
            // Entrance into our window
            XCrossingEvent ev = e.get_xcrossing();
            awtLock();
            try {
                if (lastCursorPos == null) {
                    lastCursorPos = new Point(win.scaleDown(ev.get_x_root()),
                                              win.scaleDown(ev.get_y_root()));
                } else {
                    lastCursorPos.setLocation(win.scaleDown(ev.get_x_root()),
                                              win.scaleDown(ev.get_y_root()));
                }
            } finally {
                awtUnlock();
            }
        }
    }

    public interface XEventListener {
        public void eventProcessed(XEvent e);
    }

    private Collection<XEventListener> listeners = new LinkedList<XEventListener>();

    public void addXEventListener(XEventListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    private void notifyListeners(XEvent xev) {
        synchronized (listeners) {
            if (listeners.size() == 0) return;

            XEvent copy = xev.clone();
            try {
                for (XEventListener listener : listeners) {
                    listener.eventProcessed(copy);
                }
            } finally {
                copy.dispose();
            }
        }
    }

    private void dispatchEvent(XEvent ev) {
        final XAnyEvent xany = ev.get_xany();

        XBaseWindow baseWindow = windowToXWindow(xany.get_window());
        if (baseWindow != null && (ev.get_type() == XConstants.MotionNotify
                || ev.get_type() == XConstants.EnterNotify
                || ev.get_type() == XConstants.LeaveNotify)) {
            processGlobalMotionEvent(ev, baseWindow);
        }

        if( ev.get_type() == XConstants.MappingNotify ) {
            // The 'window' field in this event is unused.
            // This application itself does nothing to initiate such an event
            // (no calls of XChangeKeyboardMapping etc.).
            // SunRay server sends this event to the application once on every
            // keyboard (not just layout) change which means, quite seldom.
            XlibWrapper.XRefreshKeyboardMapping(ev.pData);
            resetKeyboardSniffer();
            setupModifierMap();
        }
        XBaseWindow.dispatchToWindow(ev);

        Collection<XEventDispatcher> dispatchers = null;
        synchronized(winToDispatcher) {
            Long key = Long.valueOf(xany.get_window());
            dispatchers = winToDispatcher.get(key);
            if (dispatchers != null) { // Clone it to avoid synchronization during dispatching
                dispatchers = new Vector<>(dispatchers);
            }
        }
        if (dispatchers != null) {
            Iterator<XEventDispatcher> iter = dispatchers.iterator();
            while (iter.hasNext()) {
                XEventDispatcher disp = iter.next();
                disp.dispatchEvent(ev);
            }
        }
        notifyListeners(ev);
    }

    static void processException(Throwable thr) {
        if (log.isLoggable(PlatformLogger.Level.WARNING)) {
            log.warning("Exception on Toolkit thread", thr);
        }
    }

    static native void awt_toolkit_init();

    @Override
    public void run() {
        awt_toolkit_init();
        run(PRIMARY_LOOP);
    }

    public void run(boolean loop)
    {
        XEvent ev = new XEvent();
        while(true) {
            // Fix for 6829923: we should gracefully handle toolkit thread interruption
            if (Thread.currentThread().isInterrupted()) {
                // We expect interruption from the AppContext.dispose() method only.
                // If the thread is interrupted from another place, let's skip it
                // for compatibility reasons. Probably some time later we'll remove
                // the check for AppContext.isDisposed() and will unconditionally
                // break the loop here.
                if (AppContext.getAppContext().isDisposed()) {
                    break;
                }
            }
            awtLock();
            try {
                if (loop == SECONDARY_LOOP) {
                    // In the secondary loop we may have already acquired awt_lock
                    // several times, so waitForEvents() might be unable to release
                    // the awt_lock and this causes lock up.
                    // For now, we just avoid waitForEvents in the secondary loop.
                    if (!XlibWrapper.XNextSecondaryLoopEvent(getDisplay(),ev.pData)) {
                        break;
                    }
                } else {
                    callTimeoutTasks();
                    // If no events are queued, waitForEvents() causes calls to
                    // awtUnlock(), awtJNI_ThreadYield, poll, awtLock(),
                    // so it spends most of its time in poll, without holding the lock.
                    while ((XlibWrapper.XEventsQueued(getDisplay(), XConstants.QueuedAfterReading) == 0) &&
                           (XlibWrapper.XEventsQueued(getDisplay(), XConstants.QueuedAfterFlush) == 0)) {
                        callTimeoutTasks();
                        waitForEvents(getNextTaskTime());
                    }
                    XlibWrapper.XNextEvent(getDisplay(),ev.pData);
                }

                if (ev.get_type() != XConstants.NoExpose) {
                    eventNumber++;
                }
                if (awt_UseXKB_Calls && ev.get_type() ==  awt_XKBBaseEventCode) {
                    processXkbChanges(ev);
                }

                if (XDropTargetEventProcessor.processEvent(ev) ||
                    XDragSourceContextPeer.processEvent(ev)) {
                    continue;
                }

                if (eventLog.isLoggable(PlatformLogger.Level.FINER)) {
                    eventLog.finer("{0}", ev);
                }

                // Check if input method consumes the event
                long w = 0;
                if (windowToXWindow(ev.get_xany().get_window()) != null) {
                    Component owner =
                        XKeyboardFocusManagerPeer.getInstance().getCurrentFocusOwner();
                    if (owner != null) {
                        XWindow ownerWindow = AWTAccessor.getComponentAccessor().getPeer(owner);
                        if (ownerWindow != null) {
                            w = ownerWindow.getContentWindow();
                        }
                    }
                }
                if (keyEventLog.isLoggable(PlatformLogger.Level.FINE) && (
                        ev.get_type() == XConstants.KeyPress
                                || ev.get_type() == XConstants.KeyRelease)) {
                    keyEventLog.fine("before XFilterEvent:" + ev);
                }
                if (XlibWrapper.XFilterEvent(ev.getPData(), w)) {
                    continue;
                }
                if (keyEventLog.isLoggable(PlatformLogger.Level.FINE) && (
                        ev.get_type() == XConstants.KeyPress
                                || ev.get_type() == XConstants.KeyRelease)) {
                    keyEventLog.fine(
                            "after XFilterEvent:" + ev); // IS THIS CORRECT?
                }

                dispatchEvent(ev);
            } catch (ThreadDeath td) {
                XBaseWindow.ungrabInput();
                return;
            } catch (Throwable thr) {
                XBaseWindow.ungrabInput();
                processException(thr);
            } finally {
                awtUnlock();
            }
        }
    }

    /**
     * Listener installed to detect display changes.
     */
    private static final DisplayChangedListener displayChangedHandler =
            new DisplayChangedListener() {
                @Override
                public void displayChanged() {
                    // 7045370: Reset the cached values
                    XToolkit.maxWindowWidthInPixels = -1;
                    XToolkit.maxWindowHeightInPixels = -1;
                }

                @Override
                public void paletteChanged() {
                }
            };

    static {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        if (ge instanceof SunGraphicsEnvironment) {
            ((SunGraphicsEnvironment) ge).addDisplayChangedListener(
                    displayChangedHandler);
        }
    }

    private static void initScreenSize() {
        if (maxWindowWidthInPixels == -1 || maxWindowHeightInPixels == -1) {
            awtLock();
            try {
                XWindowAttributes pattr = new XWindowAttributes();
                try {
                    XlibWrapper.XGetWindowAttributes(XToolkit.getDisplay(),
                                                     XToolkit.getDefaultRootWindow(),
                                                     pattr.pData);
                    maxWindowWidthInPixels = pattr.get_width();
                    maxWindowHeightInPixels = pattr.get_height();
                } finally {
                    pattr.dispose();
                }
            } finally {
                awtUnlock();
            }
        }
    }

    static int getMaxWindowWidthInPixels() {
        initScreenSize();
        return maxWindowWidthInPixels;
    }

    static int getMaxWindowHeightInPixels() {
        initScreenSize();
        return maxWindowHeightInPixels;
    }

    private static Rectangle getWorkArea(long root, int scale)
    {
        XAtom XA_NET_WORKAREA = XAtom.get("_NET_WORKAREA");

        long native_ptr = Native.allocateLongArray(4);
        try
        {
            boolean workareaPresent = XA_NET_WORKAREA.getAtomData(root,
                XAtom.XA_CARDINAL, native_ptr, 4);
            if (workareaPresent)
            {
                int rootX = (int)Native.getLong(native_ptr, 0);
                int rootY = (int)Native.getLong(native_ptr, 1);
                int rootWidth = (int)Native.getLong(native_ptr, 2);
                int rootHeight = (int)Native.getLong(native_ptr, 3);

                return new Rectangle(scaleDown(rootX, scale),
                                     scaleDown(rootY, scale),
                                     scaleDown(rootWidth, scale),
                                     scaleDown(rootHeight, scale));
            }
        }
        finally
        {
            XlibWrapper.unsafe.freeMemory(native_ptr);
        }

        return null;
    }

    /*
     * If the current window manager supports _NET protocol then the screen
     * insets are calculated using _NET_WORKAREA property of the root window.
     * <p>
     * Note that _NET_WORKAREA is a rectangular area and it does not work
     * well in the Xinerama mode.
     * <p>
     * We will trust the part of this rectangular area only if it starts at the
     * requested graphics configuration. Below is an example when the
     * _NET_WORKAREA intersects with the requested graphics configuration but
     * produces wrong result.
     *
     *         //<-x1,y1///////
     *         //            // ////////////////
     *         //  SCREEN1   // // SCREEN2    //
     *         // ********** // //     x2,y2->//
     *         //////////////// //            //
     *                          ////////////////
     *
     * When two screens overlap and the first contains a dock(*****), then
     * _NET_WORKAREA may start at point x1,y1 and end at point x2,y2.
     */
    @Override
    public Insets getScreenInsets(final GraphicsConfiguration gc) {
        GraphicsDevice gd = gc.getDevice();
        XNETProtocol np = XWM.getWM().getNETProtocol();
        if (np == null || !(gd instanceof X11GraphicsDevice) || !np.active()) {
            return super.getScreenInsets(gc);
        }

        XToolkit.awtLock();
        try {
            X11GraphicsDevice x11gd = (X11GraphicsDevice) gd;
            long root = XlibUtil.getRootWindow(x11gd.getScreen());
            Rectangle workArea = getWorkArea(root, x11gd.getScaleFactor());
            Rectangle screen = gc.getBounds();
            if (workArea != null && screen.contains(workArea.getLocation())) {
                workArea = workArea.intersection(screen);
                int top = workArea.y - screen.y;
                int left = workArea.x - screen.x;
                int bottom = screen.height - workArea.height - top;
                int right = screen.width - workArea.width - left;
                return new Insets(top, left, bottom, right);
            }
            // Note that it is better to return zeros than inadequate values
            return new Insets(0, 0, 0, 0);
        } finally {
            XToolkit.awtUnlock();
        }
    }

    /*
     * The current implementation of disabling background erasing for
     * canvases is that we don't set any native background color
     * (with XSetWindowBackground) for the canvas window. However,
     * this color is set in the peer constructor - see
     * XWindow.postInit() for details. That's why this method from
     * SunToolkit is not overridden in XToolkit: it's too late to
     * disable background erasing :(
     */
    /*
    @Override
    public void disableBackgroundErase(Canvas canvas) {
        XCanvasPeer peer = (XCanvasPeer)canvas.getPeer();
        if (peer == null) {
            throw new IllegalStateException("Canvas must have a valid peer");
        }
        peer.disableBackgroundErase();
    }
    */

    // Need this for XMenuItemPeer.
    protected static Object targetToPeer(Object target) {
        Object p=null;
        if (target != null && !GraphicsEnvironment.isHeadless()) {
            p = specialPeerMap.get(target);
        }
        if (p != null) return p;
        else
            return SunToolkit.targetToPeer(target);
    }

    // Need this for XMenuItemPeer.
    protected static void targetDisposedPeer(Object target, Object peer) {
        SunToolkit.targetDisposedPeer(target, peer);
    }

    @Override
    public RobotPeer createRobot(GraphicsDevice screen) throws AWTException {
        if (screen instanceof X11GraphicsDevice) {
            return new XRobotPeer((X11GraphicsDevice) screen);
        }
        return super.createRobot(screen);
    }

  /*
     * On X, support for dynamic layout on resizing is governed by the
     * window manager.  If the window manager supports it, it happens
     * automatically.  The setter method for this property is
     * irrelevant on X.
     */
    @Override
    public void setDynamicLayout(boolean b) {
        dynamicLayoutSetting = b;
    }

    @Override
    protected boolean isDynamicLayoutSet() {
        return dynamicLayoutSetting;
    }

    /* Called from isDynamicLayoutActive() and from
     * lazilyLoadDynamicLayoutSupportedProperty()
     */
    protected boolean isDynamicLayoutSupported() {
        return XWM.getWM().supportsDynamicLayout();
    }

    @Override
    public boolean isDynamicLayoutActive() {
        return isDynamicLayoutSupported();
    }

    @Override
    public FontPeer getFontPeer(String name, int style){
        return new XFontPeer(name, style);
    }

    @Override
    public DragSourceContextPeer createDragSourceContextPeer(DragGestureEvent dge) throws InvalidDnDOperationException {
        final LightweightFrame f = SunToolkit.getLightweightFrame(dge.getComponent());
        if (f != null) {
            return f.createDragSourceContextPeer(dge);
        }

        return XDragSourceContextPeer.createDragSourceContextPeer(dge);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends DragGestureRecognizer> T
    createDragGestureRecognizer(Class<T> recognizerClass,
                    DragSource ds,
                    Component c,
                    int srcActions,
                    DragGestureListener dgl)
    {
        final LightweightFrame f = SunToolkit.getLightweightFrame(c);
        if (f != null) {
            return f.createDragGestureRecognizer(recognizerClass, ds, c, srcActions, dgl);
        }

        if (MouseDragGestureRecognizer.class.equals(recognizerClass))
            return (T)new XMouseDragGestureRecognizer(ds, c, srcActions, dgl);
        else
            return null;
    }

    @Override
    public CheckboxMenuItemPeer createCheckboxMenuItem(CheckboxMenuItem target) {
        XCheckboxMenuItemPeer peer = new XCheckboxMenuItemPeer(target);
        //vb157120: looks like we don't need to map menu items
        //in new menus implementation
        //targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public MenuItemPeer createMenuItem(MenuItem target) {
        XMenuItemPeer peer = new XMenuItemPeer(target);
        //vb157120: looks like we don't need to map menu items
        //in new menus implementation
        //targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public TextFieldPeer createTextField(TextField target) {
        TextFieldPeer  peer = new XTextFieldPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public LabelPeer createLabel(Label target) {
        LabelPeer  peer = new XLabelPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public ListPeer createList(java.awt.List target) {
        ListPeer peer = new XListPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public CheckboxPeer createCheckbox(Checkbox target) {
        CheckboxPeer peer = new XCheckboxPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public ScrollbarPeer createScrollbar(Scrollbar target) {
        XScrollbarPeer peer = new XScrollbarPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public ScrollPanePeer createScrollPane(ScrollPane target) {
        XScrollPanePeer peer = new XScrollPanePeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public TextAreaPeer createTextArea(TextArea target) {
        TextAreaPeer peer = new XTextAreaPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public ChoicePeer createChoice(Choice target) {
        XChoicePeer peer = new XChoicePeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public CanvasPeer createCanvas(Canvas target) {
        XCanvasPeer peer = (isXEmbedServerRequested() ? new XEmbedCanvasPeer(target) : new XCanvasPeer(target));
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public PanelPeer createPanel(Panel target) {
        PanelPeer peer = new XPanelPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public WindowPeer createWindow(Window target) {
        WindowPeer peer = new XWindowPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public DialogPeer createDialog(Dialog target) {
        DialogPeer peer = new XDialogPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    private static Boolean sunAwtDisableGtkFileDialogs = null;

    /**
     * Returns the value of "sun.awt.disableGtkFileDialogs" property. Default
     * value is {@code false}.
     */
    @SuppressWarnings("removal")
    public static synchronized boolean getSunAwtDisableGtkFileDialogs() {
        if (sunAwtDisableGtkFileDialogs == null) {
            sunAwtDisableGtkFileDialogs = AccessController.doPrivileged(
                                              new GetBooleanAction("sun.awt.disableGtkFileDialogs"));
        }
        return sunAwtDisableGtkFileDialogs.booleanValue();
    }

    @Override
    public FileDialogPeer createFileDialog(FileDialog target) {
        FileDialogPeer peer = null;
        // The current GtkFileChooser is available from GTK+ 2.4
        if (!getSunAwtDisableGtkFileDialogs() &&
                      (checkGtkVersion(2, 4, 0) || checkGtkVersion(3, 0, 0))) {
            peer = new GtkFileDialogPeer(target);
        } else {
            peer = new XFileDialogPeer(target);
        }
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public MenuBarPeer createMenuBar(MenuBar target) {
        XMenuBarPeer peer = new XMenuBarPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public MenuPeer createMenu(Menu target) {
        XMenuPeer peer = new XMenuPeer(target);
        //vb157120: looks like we don't need to map menu items
        //in new menus implementation
        //targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public PopupMenuPeer createPopupMenu(PopupMenu target) {
        XPopupMenuPeer peer = new XPopupMenuPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public synchronized MouseInfoPeer getMouseInfoPeer() {
        if (xPeer == null) {
            xPeer = new XMouseInfoPeer();
        }
        return xPeer;
    }

    public XEmbeddedFramePeer createEmbeddedFrame(XEmbeddedFrame target)
    {
        XEmbeddedFramePeer peer = new XEmbeddedFramePeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    XEmbedChildProxyPeer createEmbedProxy(XEmbedChildProxy target) {
        XEmbedChildProxyPeer peer = new XEmbedChildProxyPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public KeyboardFocusManagerPeer getKeyboardFocusManagerPeer() throws HeadlessException {
        return XKeyboardFocusManagerPeer.getInstance();
    }

    /**
     * Returns a new custom cursor.
     */
    @Override
    public Cursor createCustomCursor(Image cursor, Point hotSpot, String name)
      throws IndexOutOfBoundsException {
        return new XCustomCursor(cursor, hotSpot, name);
    }

    @Override
    public TrayIconPeer createTrayIcon(TrayIcon target)
      throws HeadlessException, AWTException
    {
        TrayIconPeer peer = new XTrayIconPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public SystemTrayPeer createSystemTray(SystemTray target) throws HeadlessException {
        SystemTrayPeer peer = new XSystemTrayPeer(target);
        return peer;
    }

    @Override
    public boolean isTraySupported() {
        XSystemTrayPeer peer = XSystemTrayPeer.getPeerInstance();
        if (peer != null) {
            return peer.isAvailable();
        }
        return false;
    }

    @Override
    public DataTransferer getDataTransferer() {
        return XDataTransferer.getInstanceImpl();
    }

    /**
     * Returns the supported cursor size
     */
    @Override
    public Dimension getBestCursorSize(int preferredWidth, int preferredHeight) {
        return XCustomCursor.getBestCursorSize(
                                               java.lang.Math.max(1,preferredWidth), java.lang.Math.max(1,preferredHeight));
    }


    @Override
    public int getMaximumCursorColors() {
        return 2;  // Black and white.
    }

    @Override
    public Map<TextAttribute, ?> mapInputMethodHighlight( InputMethodHighlight highlight) {
        return XInputMethod.mapInputMethodHighlight(highlight);
    }
    @Override
    public boolean getLockingKeyState(int key) {
        if (! (key == KeyEvent.VK_CAPS_LOCK || key == KeyEvent.VK_NUM_LOCK ||
               key == KeyEvent.VK_SCROLL_LOCK || key == KeyEvent.VK_KANA_LOCK)) {
            throw new IllegalArgumentException("invalid key for Toolkit.getLockingKeyState");
        }
        awtLock();
        try {
            return getModifierState( key );
        } finally {
            awtUnlock();
        }
    }

    @Override
    public  Clipboard getSystemClipboard() {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(AWTPermissions.ACCESS_CLIPBOARD_PERMISSION);
        }
        synchronized (this) {
            if (clipboard == null) {
                clipboard = new XClipboard("System", "CLIPBOARD");
            }
        }
        return clipboard;
    }

    @Override
    public Clipboard getSystemSelection() {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(AWTPermissions.ACCESS_CLIPBOARD_PERMISSION);
        }
        synchronized (this) {
            if (selection == null) {
                selection = new XClipboard("Selection", "PRIMARY");
            }
        }
        return selection;
    }

    @Override
    public void beep() {
        awtLock();
        try {
            XlibWrapper.XBell(getDisplay(), 0);
            XlibWrapper.XFlush(getDisplay());
        } finally {
            awtUnlock();
        }
    }

    @Override
    public PrintJob getPrintJob(final Frame frame, final String doctitle,
                                final Properties props) {

        if (frame == null) {
            throw new NullPointerException("frame must not be null");
        }

        PrintJob2D printJob = new PrintJob2D(frame, doctitle, props);

        if (printJob.printDialog() == false) {
            printJob = null;
        }
        return printJob;
    }

    @Override
    public PrintJob getPrintJob(final Frame frame, final String doctitle,
                final JobAttributes jobAttributes,
                final PageAttributes pageAttributes)
    {
        if (frame == null) {
            throw new NullPointerException("frame must not be null");
        }

        PrintJob2D printJob = new PrintJob2D(frame, doctitle,
                                             jobAttributes, pageAttributes);

        if (printJob.printDialog() == false) {
            printJob = null;
        }

        return printJob;
    }

    static void XSync() {
        awtLock();
        try {
            XlibWrapper.XSync(getDisplay(),0);
        } finally {
            awtUnlock();
        }
    }

    @Override
    public int getScreenResolution() {
        long display = getDisplay();
        awtLock();
        try {
            return (int) ((XlibWrapper.DisplayWidth(display,
                XlibWrapper.DefaultScreen(display)) * 25.4) /
                    XlibWrapper.DisplayWidthMM(display,
                XlibWrapper.DefaultScreen(display)));
        } finally {
            awtUnlock();
        }
    }

    static native long getDefaultXColormap();

    /**
     * Returns a new input method adapter descriptor for native input methods.
     */
    @Override
    public InputMethodDescriptor getInputMethodAdapterDescriptor() throws AWTException {
        return new XInputMethodDescriptor();
    }

    /**
     * Returns whether enableInputMethods should be set to true for peered
     * TextComponent instances on this platform. True by default.
     */
    @Override
    public boolean enableInputMethodsForTextComponent() {
        return true;
    }

    static int getMultiClickTime() {
        if (awt_multiclick_time == 0) {
            initializeMultiClickTime();
        }
        return awt_multiclick_time;
    }
    static void initializeMultiClickTime() {
        awtLock();
        try {
            try {
                String multiclick_time_query = XlibWrapper.XGetDefault(XToolkit.getDisplay(), "*", "multiClickTime");
                if (multiclick_time_query != null) {
                    awt_multiclick_time = (int)Long.parseLong(multiclick_time_query);
                } else {
                    multiclick_time_query = XlibWrapper.XGetDefault(XToolkit.getDisplay(),
                                                                    "OpenWindows", "MultiClickTimeout");
                    if (multiclick_time_query != null) {
                        /* Note: OpenWindows.MultiClickTimeout is in tenths of
                           a second, so we need to multiply by 100 to convert to
                           milliseconds */
                        awt_multiclick_time = (int)Long.parseLong(multiclick_time_query) * 100;
                    } else {
                        awt_multiclick_time = AWT_MULTICLICK_DEFAULT_TIME;
                    }
                }
            } catch (NumberFormatException nf) {
                awt_multiclick_time = AWT_MULTICLICK_DEFAULT_TIME;
            } catch (NullPointerException npe) {
                awt_multiclick_time = AWT_MULTICLICK_DEFAULT_TIME;
            }
        } finally {
            awtUnlock();
        }
        if (awt_multiclick_time == 0) {
            awt_multiclick_time = AWT_MULTICLICK_DEFAULT_TIME;
        }
    }

    @Override
    public boolean isFrameStateSupported(int state)
      throws HeadlessException
    {
        if (state == Frame.NORMAL || state == Frame.ICONIFIED) {
            return true;
        } else {
            return XWM.getWM().supportsExtendedState(state);
        }
    }

    static void dumpPeers() {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("Mapped windows:");
            winMap.forEach((k, v) -> {
                log.fine(k + "->" + v);
                if (v instanceof XComponentPeer) {
                    Component target = (Component)((XComponentPeer)v).getTarget();
                    log.fine("\ttarget: " + target);
                }
            });

            SunToolkit.dumpPeers(log);

            log.fine("Mapped special peers:");
            specialPeerMap.forEach((k, v) -> {
                log.fine(k + "->" + v);
            });

            log.fine("Mapped dispatchers:");
            winToDispatcher.forEach((k, v) -> {
                log.fine(k + "->" + v);
            });
        }
    }

    /* Protected with awt_lock. */
    private static boolean initialized;
    private static boolean timeStampUpdated;
    private static long timeStamp;

    private static final XEventDispatcher timeFetcher =
    new XEventDispatcher() {
            @Override
            public void dispatchEvent(XEvent ev) {
                switch (ev.get_type()) {
                  case XConstants.PropertyNotify:
                      XPropertyEvent xpe = ev.get_xproperty();

                      awtLock();
                      try {
                          timeStamp = xpe.get_time();
                          timeStampUpdated = true;
                          awtLockNotifyAll();
                      } finally {
                          awtUnlock();
                      }

                      break;
                }
            }
        };

    private static XAtom _XA_JAVA_TIME_PROPERTY_ATOM;

    static long getCurrentServerTime() {
        awtLock();
        try {
            try {
                if (!initialized) {
                    XToolkit.addEventDispatcher(XBaseWindow.getXAWTRootWindow().getWindow(),
                                                timeFetcher);
                    _XA_JAVA_TIME_PROPERTY_ATOM = XAtom.get("_SUNW_JAVA_AWT_TIME");
                    initialized = true;
                }
                timeStampUpdated = false;
                XlibWrapper.XChangeProperty(XToolkit.getDisplay(),
                                            XBaseWindow.getXAWTRootWindow().getWindow(),
                                            _XA_JAVA_TIME_PROPERTY_ATOM.getAtom(), XAtom.XA_ATOM, 32,
                                            XConstants.PropModeAppend,
                                            0, 0);
                XlibWrapper.XFlush(XToolkit.getDisplay());

                if (isToolkitThread()) {
                    XEvent event = new XEvent();
                    try {
                        XlibWrapper.XWindowEvent(XToolkit.getDisplay(),
                                                 XBaseWindow.getXAWTRootWindow().getWindow(),
                                                 XConstants.PropertyChangeMask,
                                                 event.pData);
                        timeFetcher.dispatchEvent(event);
                    }
                    finally {
                        event.dispose();
                    }
                }
                else {
                    while (!timeStampUpdated) {
                        awtLockWait();
                    }
                }
            } catch (InterruptedException ie) {
            // Note: the returned timeStamp can be incorrect in this case.
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("Catched exception, timeStamp may not be correct (ie = " + ie + ")");
                }
            }
        } finally {
            awtUnlock();
        }
        return timeStamp;
    }
    @Override
    protected void initializeDesktopProperties() {
        desktopProperties.put("DnD.Autoscroll.initialDelay",
                              Integer.valueOf(50));
        desktopProperties.put("DnD.Autoscroll.interval",
                              Integer.valueOf(50));
        desktopProperties.put("DnD.Autoscroll.cursorHysteresis",
                              Integer.valueOf(5));
        desktopProperties.put("Shell.shellFolderManager",
                              "sun.awt.shell.ShellFolderManager");
        // Don't want to call getMultiClickTime() if we are headless
        if (!GraphicsEnvironment.isHeadless()) {
            desktopProperties.put("awt.multiClickInterval",
                                  Integer.valueOf(getMultiClickTime()));
            desktopProperties.put("awt.mouse.numButtons",
                                  Integer.valueOf(getNumberOfButtons()));
            if(SunGraphicsEnvironment.isUIScaleEnabled()) {
                addPropertyChangeListener("gnome.Xft/DPI", evt ->
                                                     localEnv.displayChanged());
            }
        }
    }

    /**
     * This method runs through the XPointer and XExtendedPointer array.
     * XExtendedPointer has priority because on some systems XPointer
     * (which is assigned to the virtual pointer) reports the maximum
     * capabilities of the mouse pointer (i.e. 32 physical buttons).
     */
    private native int getNumberOfButtonsImpl();

    @Override
    public int getNumberOfButtons(){
        awtLock();
        try {
            if (numberOfButtons == 0) {
                numberOfButtons = getNumberOfButtonsImpl();
                numberOfButtons = (numberOfButtons > MAX_BUTTONS_SUPPORTED)? MAX_BUTTONS_SUPPORTED : numberOfButtons;
                //4th and 5th buttons are for wheel and shouldn't be reported as buttons.
                //If we have more than 3 physical buttons and a wheel, we report N-2 buttons.
                //If we have 3 physical buttons and a wheel, we report 3 buttons.
                //If we have 1,2,3 physical buttons, we report it as is i.e. 1,2 or 3 respectively.
                if (numberOfButtons >=5) {
                    numberOfButtons -= 2;
                } else if (numberOfButtons == 4 || numberOfButtons ==5){
                    numberOfButtons = 3;
                }
            }
            //Assume don't have to re-query the number again and again.
            return numberOfButtons;
        } finally {
            awtUnlock();
        }
    }

    static int getNumberOfButtonsForMask() {
        return Math.min(XConstants.MAX_BUTTONS, ((SunToolkit) (Toolkit.getDefaultToolkit())).getNumberOfButtons());
    }

    private static final String prefix  = "DnD.Cursor.";
    private static final String postfix = ".32x32";
    private static final String dndPrefix  = "DnD.";

    @Override
    protected Object lazilyLoadDesktopProperty(String name) {
        if (name.startsWith(prefix)) {
            String cursorName = name.substring(prefix.length(), name.length()) + postfix;

            try {
                return Cursor.getSystemCustomCursor(cursorName);
            } catch (AWTException awte) {
                throw new RuntimeException("cannot load system cursor: " + cursorName, awte);
            }
        }

        if (name.equals("awt.dynamicLayoutSupported")) {
            return  Boolean.valueOf(isDynamicLayoutSupported());
        }

        if (initXSettingsIfNeeded(name)) {
            return desktopProperties.get(name);
        }

        return super.lazilyLoadDesktopProperty(name);
    }

    @Override
    public synchronized void addPropertyChangeListener(String name, PropertyChangeListener pcl) {
        if (name == null) {
            // See JavaDoc for the Toolkit.addPropertyChangeListener() method
            return;
        }
        initXSettingsIfNeeded(name);
        super.addPropertyChangeListener(name, pcl);
    }

    /**
     * Initializes XAWTXSettings if a property for a given property name is provided by
     * XSettings and they are not initialized yet.
     *
     * @return true if the method has initialized XAWTXSettings.
     */
    private boolean initXSettingsIfNeeded(final String propName) {
        if (!loadedXSettings &&
            (propName.startsWith("gnome.") ||
             propName.equals(SunToolkit.DESKTOPFONTHINTS) ||
             propName.startsWith(dndPrefix)))
        {
            loadedXSettings = true;
            if (!GraphicsEnvironment.isHeadless()) {
                loadXSettings();
                /* If no desktop font hint could be retrieved, check for
                 * KDE running KWin and retrieve settings from fontconfig.
                 * If that isn't found let SunToolkit will see if there's a
                 * system property set by a user.
                 */
                if (desktopProperties.get(SunToolkit.DESKTOPFONTHINTS) == null) {
                    if (XWM.isKDE2()) {
                        Object hint = FontConfigManager.getFontConfigAAHint();
                        if (hint != null) {
                            /* set the fontconfig/KDE property so that
                             * getDesktopHints() below will see it
                             * and set the public property.
                             */
                            desktopProperties.put(UNIXToolkit.FONTCONFIGAAHINT,
                                                  hint);
                        }
                    }
                    desktopProperties.put(SunToolkit.DESKTOPFONTHINTS,
                                          SunToolkit.getDesktopFontHints());
                }

                return true;
            }
        }
        return false;
    }

    private void loadXSettings() {
       xs = new XAWTXSettings();
    }

    /**
     * Callback from the native side indicating some, or all, of the
     * desktop properties have changed and need to be reloaded.
     * {@code data} is the byte array directly from the x server and
     * may be in little endian format.
     * <p>
     * NB: This could be called from any thread if triggered by
     * {@code loadXSettings}.  It is called from the System EDT
     * if triggered by an XSETTINGS change.
     */
    void parseXSettings(int screen_XXX_ignored,Map<String, Object> updatedSettings) {

        if (updatedSettings == null || updatedSettings.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<String, Object>> i = updatedSettings.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, Object> e = i.next();
            String name = e.getKey();

            name = "gnome." + name;
            setDesktopProperty(name, e.getValue());
            if (log.isLoggable(PlatformLogger.Level.FINE)) {
                log.fine("name = " + name + " value = " + e.getValue());
            }

            // XXX: we probably want to do something smarter.  In
            // particular, "Net" properties are of interest to the
            // "core" AWT itself.  E.g.
            //
            // Net/DndDragThreshold -> ???
            // Net/DoubleClickTime  -> awt.multiClickInterval
        }

        setDesktopProperty(SunToolkit.DESKTOPFONTHINTS,
                           SunToolkit.getDesktopFontHints());

        Integer dragThreshold = null;
        synchronized (this) {
            dragThreshold = (Integer)desktopProperties.get("gnome.Net/DndDragThreshold");
        }
        if (dragThreshold != null) {
            setDesktopProperty("DnD.gestureMotionThreshold", dragThreshold);
        }

    }



    static int altMask;
    static int metaMask;
    static int numLockMask;
    static int modeSwitchMask;
    static int modLockIsShiftLock;

    /* Like XKeysymToKeycode, but ensures that keysym is the primary
    * symbol on the keycode returned.  Returns zero otherwise.
    */
    static int keysymToPrimaryKeycode(long sym) {
        awtLock();
        try {
            int code = XlibWrapper.XKeysymToKeycode(getDisplay(), sym);
            if (code == 0) {
                return 0;
            }
            long primary = XlibWrapper.XKeycodeToKeysym(getDisplay(), code, 0);
            if (sym != primary) {
                return 0;
            }
            return code;
        } finally {
            awtUnlock();
        }
    }
    static boolean getModifierState( int jkc ) {
        int iKeyMask = 0;
        long ks = XKeysym.javaKeycode2Keysym( jkc );
        int  kc = XlibWrapper.XKeysymToKeycode(getDisplay(), ks);
        if (kc == 0) {
            return false;
        }
        awtLock();
        try {
            XModifierKeymap modmap = new XModifierKeymap(
                 XlibWrapper.XGetModifierMapping(getDisplay()));

            int nkeys = modmap.get_max_keypermod();

            long map_ptr = modmap.get_modifiermap();
            for( int k = 0; k < 8; k++ ) {
                for (int i = 0; i < nkeys; ++i) {
                    int keycode = Native.getUByte(map_ptr, k * nkeys + i);
                    if (keycode == 0) {
                        continue; // ignore zero keycode
                    }
                    if (kc == keycode) {
                        iKeyMask = 1 << k;
                        break;
                    }
                }
                if( iKeyMask != 0 ) {
                    break;
                }
            }
            XlibWrapper.XFreeModifiermap(modmap.pData);
            if (iKeyMask == 0 ) {
                return false;
            }
            // Now we know to which modifier is assigned the keycode
            // correspondent to the keysym correspondent to the java
            // keycode. We are going to check a state of this modifier.
            // If a modifier is a weird one, we cannot help it.
            long window = 0;
            try{
                // get any application window
                window = winMap.firstKey().longValue();
            }catch(NoSuchElementException nex) {
                // get root window
                window = getDefaultRootWindow();
            }
            boolean res = XlibWrapper.XQueryPointer(getDisplay(), window,
                                            XlibWrapper.larg1, //root
                                            XlibWrapper.larg2, //child
                                            XlibWrapper.larg3, //root_x
                                            XlibWrapper.larg4, //root_y
                                            XlibWrapper.larg5, //child_x
                                            XlibWrapper.larg6, //child_y
                                            XlibWrapper.larg7);//mask
            int mask = Native.getInt(XlibWrapper.larg7);
            return ((mask & iKeyMask) != 0);
        } finally {
            awtUnlock();
        }
    }

    /* Assign meaning - alt, meta, etc. - to X modifiers mod1 ... mod5.
     * Only consider primary symbols on keycodes attached to modifiers.
     */
    static void setupModifierMap() {
        final int metaL = keysymToPrimaryKeycode(XKeySymConstants.XK_Meta_L);
        final int metaR = keysymToPrimaryKeycode(XKeySymConstants.XK_Meta_R);
        final int altL = keysymToPrimaryKeycode(XKeySymConstants.XK_Alt_L);
        final int altR = keysymToPrimaryKeycode(XKeySymConstants.XK_Alt_R);
        final int numLock = keysymToPrimaryKeycode(XKeySymConstants.XK_Num_Lock);
        final int modeSwitch = keysymToPrimaryKeycode(XKeySymConstants.XK_Mode_switch);
        final int shiftLock = keysymToPrimaryKeycode(XKeySymConstants.XK_Shift_Lock);
        final int capsLock  = keysymToPrimaryKeycode(XKeySymConstants.XK_Caps_Lock);

        final int[] modmask = { XConstants.ShiftMask, XConstants.LockMask, XConstants.ControlMask, XConstants.Mod1Mask,
            XConstants.Mod2Mask, XConstants.Mod3Mask, XConstants.Mod4Mask, XConstants.Mod5Mask };

        log.fine("In setupModifierMap");
        awtLock();
        try {
            XModifierKeymap modmap = new XModifierKeymap(
                 XlibWrapper.XGetModifierMapping(getDisplay()));

            int nkeys = modmap.get_max_keypermod();

            long map_ptr = modmap.get_modifiermap();

            for (int modn = XConstants.Mod1MapIndex;
                 modn <= XConstants.Mod5MapIndex;
                 ++modn)
            {
                for (int i = 0; i < nkeys; ++i) {
                    /* for each keycode attached to this modifier */
                    int keycode = Native.getUByte(map_ptr, modn * nkeys + i);

                    if (keycode == 0) {
                        break;
                    }
                    if (metaMask == 0 &&
                        (keycode == metaL || keycode == metaR))
                    {
                        metaMask = modmask[modn];
                        break;
                    }
                    if (altMask == 0 && (keycode == altL || keycode == altR)) {
                        altMask = modmask[modn];
                        break;
                    }
                    if (numLockMask == 0 && keycode == numLock) {
                        numLockMask = modmask[modn];
                        break;
                    }
                    if (modeSwitchMask == 0 && keycode == modeSwitch) {
                        modeSwitchMask = modmask[modn];
                        break;
                    }
                    continue;
                }
            }
            modLockIsShiftLock = 0;
            for (int j = 0; j < nkeys; ++j) {
                int keycode = Native.getUByte(map_ptr, XConstants.LockMapIndex * nkeys + j);
                if (keycode == 0) {
                    break;
                }
                if (keycode == shiftLock) {
                    modLockIsShiftLock = 1;
                    break;
                }
                if (keycode == capsLock) {
                    break;
                }
            }
            XlibWrapper.XFreeModifiermap(modmap.pData);
        } finally {
            awtUnlock();
        }
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("metaMask = " + metaMask);
            log.fine("altMask = " + altMask);
            log.fine("numLockMask = " + numLockMask);
            log.fine("modeSwitchMask = " + modeSwitchMask);
            log.fine("modLockIsShiftLock = " + modLockIsShiftLock);
        }
    }


    private static SortedMap<Long, java.util.List<Runnable>> timeoutTasks;

    /**
     * Removed the task from the list of waiting-to-be called tasks.
     * If the task has been scheduled several times removes only first one.
     */
    static void remove(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task is null");
        }
        awtLock();
        try {
            if (timeoutTaskLog.isLoggable(PlatformLogger.Level.FINER)) {
                timeoutTaskLog.finer("Removing task " + task);
            }
            if (timeoutTasks == null) {
                if (timeoutTaskLog.isLoggable(PlatformLogger.Level.FINER)) {
                    timeoutTaskLog.finer("Task is not scheduled");
                }
                return;
            }
            Collection<java.util.List<Runnable>> values = timeoutTasks.values();
            Iterator<java.util.List<Runnable>> iter = values.iterator();
            while (iter.hasNext()) {
                java.util.List<Runnable> list = iter.next();
                boolean removed = false;
                if (list.contains(task)) {
                    list.remove(task);
                    if (list.isEmpty()) {
                        iter.remove();
                    }
                    break;
                }
            }
        } finally {
            awtUnlock();
        }
    }

    static native void wakeup_poll();

    /**
     * Registers a Runnable which {@code run()} method will be called
     * once on the toolkit thread when a specified interval of time elapses.
     *
     * @param task a Runnable which {@code run} method will be called
     *        on the toolkit thread when {@code interval} milliseconds
     *        elapse
     * @param interval an interal in milliseconds
     *
     * @throws NullPointerException if {@code task} is {@code null}
     * @throws IllegalArgumentException if {@code interval} is not positive
     */
    static void schedule(Runnable task, long interval) {
        if (task == null) {
            throw new NullPointerException("task is null");
        }
        if (interval <= 0) {
            throw new IllegalArgumentException("interval " + interval + " is not positive");
        }

        awtLock();
        try {
            if (timeoutTaskLog.isLoggable(PlatformLogger.Level.FINER)) {
                timeoutTaskLog.finer("XToolkit.schedule(): current time={0}" +
                                     ";  interval={1}" +
                                     ";  task being added={2}" + ";  tasks before addition={3}",
                                     Long.valueOf(System.currentTimeMillis()), Long.valueOf(interval), task, timeoutTasks);
            }

            if (timeoutTasks == null) {
                timeoutTasks = new TreeMap<>();
            }

            Long time = Long.valueOf(System.currentTimeMillis() + interval);
            java.util.List<Runnable> tasks = timeoutTasks.get(time);
            if (tasks == null) {
                tasks = new ArrayList<>(1);
                timeoutTasks.put(time, tasks);
            }
            tasks.add(task);


            if (timeoutTasks.get(timeoutTasks.firstKey()) == tasks && tasks.size() == 1) {
                // Added task became first task - poll won't know
                // about it so we need to wake it up
                wakeup_poll();
            }
        }  finally {
            awtUnlock();
        }
    }

    private long getNextTaskTime() {
        awtLock();
        try {
            if (timeoutTasks == null || timeoutTasks.isEmpty()) {
                return -1L;
            }
            return timeoutTasks.firstKey();
        } finally {
            awtUnlock();
        }
    }

    /**
     * Executes mature timeout tasks registered with schedule().
     * Called from run() under awtLock.
     */
    private static void callTimeoutTasks() {
        if (timeoutTaskLog.isLoggable(PlatformLogger.Level.FINER)) {
            timeoutTaskLog.finer("XToolkit.callTimeoutTasks(): current time={0}" +
                                 ";  tasks={1}", Long.valueOf(System.currentTimeMillis()), timeoutTasks);
        }

        if (timeoutTasks == null || timeoutTasks.isEmpty()) {
            return;
        }

        Long currentTime = Long.valueOf(System.currentTimeMillis());
        Long time = timeoutTasks.firstKey();

        while (time.compareTo(currentTime) <= 0) {
            java.util.List<Runnable> tasks = timeoutTasks.remove(time);

            for (Iterator<Runnable> iter = tasks.iterator(); iter.hasNext();) {
                Runnable task = iter.next();

                if (timeoutTaskLog.isLoggable(PlatformLogger.Level.FINER)) {
                    timeoutTaskLog.finer("XToolkit.callTimeoutTasks(): current time={0}" +
                                         ";  about to run task={1}", Long.valueOf(currentTime), task);
                }

                try {
                    task.run();
                } catch (ThreadDeath td) {
                    throw td;
                } catch (Throwable thr) {
                    processException(thr);
                }
            }

            if (timeoutTasks.isEmpty()) {
                break;
            }
            time = timeoutTasks.firstKey();
        }
    }

    static boolean isLeftMouseButton(MouseEvent me) {
        switch (me.getID()) {
          case MouseEvent.MOUSE_PRESSED:
          case MouseEvent.MOUSE_RELEASED:
              return (me.getButton() == MouseEvent.BUTTON1);
          case MouseEvent.MOUSE_ENTERED:
          case MouseEvent.MOUSE_EXITED:
          case MouseEvent.MOUSE_CLICKED:
          case MouseEvent.MOUSE_DRAGGED:
              return ((me.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != 0);
        }
        return false;
    }

    static boolean isRightMouseButton(MouseEvent me) {
        int numButtons = ((Integer)getDefaultToolkit().getDesktopProperty("awt.mouse.numButtons")).intValue();
        switch (me.getID()) {
          case MouseEvent.MOUSE_PRESSED:
          case MouseEvent.MOUSE_RELEASED:
              return ((numButtons == 2 && me.getButton() == MouseEvent.BUTTON2) ||
                       (numButtons > 2 && me.getButton() == MouseEvent.BUTTON3));
          case MouseEvent.MOUSE_ENTERED:
          case MouseEvent.MOUSE_EXITED:
          case MouseEvent.MOUSE_CLICKED:
          case MouseEvent.MOUSE_DRAGGED:
              return ((numButtons == 2 && (me.getModifiersEx() & InputEvent.BUTTON2_DOWN_MASK) != 0) ||
                      (numButtons > 2 && (me.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) != 0));
        }
        return false;
    }

    /**
     * @see sun.awt.SunToolkit#needsXEmbedImpl
     */
    @Override
    protected boolean needsXEmbedImpl() {
        // XToolkit implements supports for XEmbed-client protocol and
        // requires the supports from the embedding host for it to work.
        return true;
    }

    @Override
    public boolean isModalityTypeSupported(Dialog.ModalityType modalityType) {
        return (modalityType == null) ||
               (modalityType == Dialog.ModalityType.MODELESS) ||
               (modalityType == Dialog.ModalityType.DOCUMENT_MODAL) ||
               (modalityType == Dialog.ModalityType.APPLICATION_MODAL) ||
               (modalityType == Dialog.ModalityType.TOOLKIT_MODAL);
    }

    @Override
    public boolean isModalExclusionTypeSupported(Dialog.ModalExclusionType exclusionType) {
        return (exclusionType == null) ||
               (exclusionType == Dialog.ModalExclusionType.NO_EXCLUDE) ||
               (exclusionType == Dialog.ModalExclusionType.APPLICATION_EXCLUDE) ||
               (exclusionType == Dialog.ModalExclusionType.TOOLKIT_EXCLUDE);
    }

    static EventQueue getEventQueue(Object target) {
        AppContext appContext = targetToAppContext(target);
        if (appContext != null) {
            return (EventQueue)appContext.get(AppContext.EVENT_QUEUE_KEY);
        }
        return null;
    }

    static void removeSourceEvents(EventQueue queue,
                                   Object source,
                                   boolean removeAllEvents) {
        AWTAccessor.getEventQueueAccessor()
            .removeSourceEvents(queue, source, removeAllEvents);
    }

    @Override
    public boolean isAlwaysOnTopSupported() {
        for (XLayerProtocol proto : XWM.getWM().getProtocols(XLayerProtocol.class)) {
            if (proto.supportsLayer(XLayerProtocol.LAYER_ALWAYS_ON_TOP)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean useBufferPerWindow() {
        return XToolkit.getBackingStoreType() == XConstants.NotUseful;
    }

    /**
     * Returns one of XConstants: NotUseful, WhenMapped or Always.
     * If backing store is not available on at least one screen, or
     * the string system property "sun.awt.backingStore" is neither "Always"
     * nor "WhenMapped", then the method returns XConstants.NotUseful.
     * Otherwise, if the system property "sun.awt.backingStore" is "WhenMapped",
     * then the method returns XConstants.WhenMapped.
     * Otherwise (i.e., if the system property "sun.awt.backingStore" is "Always"),
     * the method returns XConstants.Always.
     */
    static int getBackingStoreType() {
        return backingStoreType;
    }

    private static void setBackingStoreType() {
        @SuppressWarnings("removal")
        String prop = AccessController.doPrivileged(
                new sun.security.action.GetPropertyAction("sun.awt.backingStore"));

        if (prop == null) {
            backingStoreType = XConstants.NotUseful;
            if (backingStoreLog.isLoggable(PlatformLogger.Level.CONFIG)) {
                backingStoreLog.config("The system property sun.awt.backingStore is not set" +
                                       ", by default backingStore=NotUseful");
            }
            return;
        }

        if (backingStoreLog.isLoggable(PlatformLogger.Level.CONFIG)) {
            backingStoreLog.config("The system property sun.awt.backingStore is " + prop);
        }
        prop = prop.toLowerCase();
        if (prop.equals("always")) {
            backingStoreType = XConstants.Always;
        } else if (prop.equals("whenmapped")) {
            backingStoreType = XConstants.WhenMapped;
        } else {
            backingStoreType = XConstants.NotUseful;
        }

        if (backingStoreLog.isLoggable(PlatformLogger.Level.CONFIG)) {
            backingStoreLog.config("backingStore(as provided by the system property)=" +
                                   ( backingStoreType == XConstants.NotUseful ? "NotUseful"
                                     : backingStoreType == XConstants.WhenMapped ?
                                     "WhenMapped" : "Always") );
        }

        awtLock();
        try {
            int screenCount = XlibWrapper.ScreenCount(getDisplay());
            for (int i = 0; i < screenCount; i++) {
                if (XlibWrapper.DoesBackingStore(XlibWrapper.ScreenOfDisplay(getDisplay(), i))
                        == XConstants.NotUseful) {
                    backingStoreType = XConstants.NotUseful;

                    if (backingStoreLog.isLoggable(PlatformLogger.Level.CONFIG)) {
                        backingStoreLog.config("Backing store is not available on the screen " +
                                               i + ", backingStore=NotUseful");
                    }

                    return;
                }
            }
        } finally {
            awtUnlock();
        }
    }

    /**
     * One of XConstants: NotUseful, WhenMapped or Always.
     */
    private static int backingStoreType;

    static final int XSUN_KP_BEHAVIOR = 1;
    static final int XORG_KP_BEHAVIOR = 2;
    static final int    IS_SUN_KEYBOARD = 1;
    static final int IS_NONSUN_KEYBOARD = 2;
    static final int    IS_KANA_KEYBOARD = 1;
    static final int IS_NONKANA_KEYBOARD = 2;


    static int     awt_IsXsunKPBehavior = 0;
    static boolean awt_UseXKB         = false;
    static boolean awt_UseXKB_Calls   = false;
    static int     awt_XKBBaseEventCode = 0;
    static int     awt_XKBEffectiveGroup = 0; // so far, I don't use it leaving all calculations
                                              // to XkbTranslateKeyCode
    static long    awt_XKBDescPtr     = 0;

    /**
     * Check for Xsun convention regarding numpad keys.
     * Xsun and some other servers (i.e. derived from Xsun)
     * under certain conditions process numpad keys unlike Xorg.
     */
    static boolean isXsunKPBehavior() {
        awtLock();
        try {
            if( awt_IsXsunKPBehavior == 0 ) {
                if( XlibWrapper.IsXsunKPBehavior(getDisplay()) ) {
                    awt_IsXsunKPBehavior = XSUN_KP_BEHAVIOR;
                }else{
                    awt_IsXsunKPBehavior = XORG_KP_BEHAVIOR;
                }
            }
            return awt_IsXsunKPBehavior == XSUN_KP_BEHAVIOR ? true : false;
        } finally {
            awtUnlock();
        }
    }

    static int  sunOrNotKeyboard = 0;
    static int kanaOrNotKeyboard = 0;
    static void resetKeyboardSniffer() {
        sunOrNotKeyboard  = 0;
        kanaOrNotKeyboard = 0;
    }
    static boolean isSunKeyboard() {
        if( sunOrNotKeyboard == 0 ) {
            if( XlibWrapper.IsSunKeyboard( getDisplay() )) {
                sunOrNotKeyboard = IS_SUN_KEYBOARD;
            }else{
                sunOrNotKeyboard = IS_NONSUN_KEYBOARD;
            }
        }
        return (sunOrNotKeyboard == IS_SUN_KEYBOARD);
    }
    static boolean isKanaKeyboard() {
        if( kanaOrNotKeyboard == 0 ) {
            if( XlibWrapper.IsKanaKeyboard( getDisplay() )) {
                kanaOrNotKeyboard = IS_KANA_KEYBOARD;
            }else{
                kanaOrNotKeyboard = IS_NONKANA_KEYBOARD;
            }
        }
        return (kanaOrNotKeyboard == IS_KANA_KEYBOARD);
    }
    static boolean isXKBenabled() {
        awtLock();
        try {
            return awt_UseXKB;
        } finally {
            awtUnlock();
        }
    }

    /**
      Query XKEYBOARD extension.
      If possible, initialize xkb library.
    */
    static boolean tryXKB() {
        awtLock();
        try {
            String name = "XKEYBOARD";
            // First, if there is extension at all.
            awt_UseXKB = XlibWrapper.XQueryExtension( getDisplay(), name, XlibWrapper.larg1, XlibWrapper.larg2, XlibWrapper.larg3);
            if( awt_UseXKB ) {
                // There is a keyboard extension. Check if a client library is compatible.
                // If not, don't use xkb calls.
                // In this case we still may be Xkb-capable application.
                awt_UseXKB_Calls = XlibWrapper.XkbLibraryVersion( XlibWrapper.larg1, XlibWrapper.larg2);
                if( awt_UseXKB_Calls ) {
                    awt_UseXKB_Calls = XlibWrapper.XkbQueryExtension( getDisplay(),  XlibWrapper.larg1, XlibWrapper.larg2,
                                     XlibWrapper.larg3, XlibWrapper.larg4, XlibWrapper.larg5);
                    if( awt_UseXKB_Calls ) {
                        awt_XKBBaseEventCode = Native.getInt(XlibWrapper.larg2);
                        XlibWrapper.XkbSelectEvents (getDisplay(),
                                         XConstants.XkbUseCoreKbd,
                                         XConstants.XkbNewKeyboardNotifyMask |
                                                 XConstants.XkbMapNotifyMask ,//|
                                                 //XConstants.XkbStateNotifyMask,
                                         XConstants.XkbNewKeyboardNotifyMask |
                                                 XConstants.XkbMapNotifyMask );//|
                                                 //XConstants.XkbStateNotifyMask);

                        XlibWrapper.XkbSelectEventDetails(getDisplay(), XConstants.XkbUseCoreKbd,
                                                     XConstants.XkbStateNotify,
                                                     XConstants.XkbGroupStateMask,
                                                     XConstants.XkbGroupStateMask);
                                                     //XXX ? XkbGroupLockMask last, XkbAllStateComponentsMask before last?
                        awt_XKBDescPtr = XlibWrapper.XkbGetMap(getDisplay(),
                                                     XConstants.XkbKeyTypesMask    |
                                                     XConstants.XkbKeySymsMask     |
                                                     XConstants.XkbModifierMapMask |
                                                     XConstants.XkbVirtualModsMask,
                                                     XConstants.XkbUseCoreKbd);

                        XlibWrapper.XkbSetDetectableAutoRepeat(getDisplay(), true);
                    }
                }
            }
            return awt_UseXKB;
        } finally {
            awtUnlock();
        }
    }
    static boolean canUseXKBCalls() {
        awtLock();
        try {
            return awt_UseXKB_Calls;
        } finally {
            awtUnlock();
        }
    }
    static int getXKBEffectiveGroup() {
        awtLock();
        try {
            return awt_XKBEffectiveGroup;
        } finally {
            awtUnlock();
        }
    }
    static int getXKBBaseEventCode() {
        awtLock();
        try {
            return awt_XKBBaseEventCode;
        } finally {
            awtUnlock();
        }
    }
    static long getXKBKbdDesc() {
        awtLock();
        try {
            return awt_XKBDescPtr;
        } finally {
            awtUnlock();
        }
    }
    void freeXKB() {
        awtLock();
        try {
            if (awt_UseXKB_Calls && awt_XKBDescPtr != 0) {
                XlibWrapper.XkbFreeKeyboard(awt_XKBDescPtr, 0xFF, true);
                awt_XKBDescPtr = 0;
            }
        } finally {
            awtUnlock();
        }
    }
    private void processXkbChanges(XEvent ev) {
        // mapping change --> refresh kbd map
        // state change --> get a new effective group; do I really need it
        //  or that should be left for XkbTranslateKeyCode?
        XkbEvent xke = new XkbEvent( ev.getPData() );
        int xkb_type = xke.get_any().get_xkb_type();
        switch( xkb_type ) {
            case XConstants.XkbNewKeyboardNotify :
                 if( awt_XKBDescPtr != 0 ) {
                     freeXKB();
                 }
                 awt_XKBDescPtr = XlibWrapper.XkbGetMap(getDisplay(),
                                              XConstants.XkbKeyTypesMask    |
                                              XConstants.XkbKeySymsMask     |
                                              XConstants.XkbModifierMapMask |
                                              XConstants.XkbVirtualModsMask,
                                              XConstants.XkbUseCoreKbd);
                 //System.out.println("XkbNewKeyboard:"+(xke.get_new_kbd()));
                 break;
            case XConstants.XkbMapNotify :
                 if (awt_XKBDescPtr != 0) {
                    //TODO: provide a simple unit test.
                    XlibWrapper.XkbGetUpdatedMap(getDisplay(),
                                                 XConstants.XkbKeyTypesMask    |
                                                 XConstants.XkbKeySymsMask     |
                                                 XConstants.XkbModifierMapMask |
                                                 XConstants.XkbVirtualModsMask,
                                                 awt_XKBDescPtr);
                 }
                //System.out.println("XkbMap:"+(xke.get_map()));
                 break;
            case XConstants.XkbStateNotify :
                 // May use it later e.g. to obtain an effective group etc.
                 //System.out.println("XkbState:"+(xke.get_state()));
                 break;
            default:
                 //System.out.println("XkbEvent of xkb_type "+xkb_type);
                 break;
        }
    }

    private static long eventNumber;
    public static long getEventNumber() {
        awtLock();
        try {
            return eventNumber;
        } finally {
            awtUnlock();
        }
    }

    private static XEventDispatcher oops_waiter;
    private static boolean oops_updated;
    private static int oops_position = 0;

    /**
     * @inheritDoc
     */
    @Override
    protected boolean syncNativeQueue(long timeout) {
        if (timeout <= 0) {
            return false;
        }
        XBaseWindow win = XBaseWindow.getXAWTRootWindow();

        if (oops_waiter == null) {
            oops_waiter = new XEventDispatcher() {
                    @Override
                    public void dispatchEvent(XEvent e) {
                        if (e.get_type() == XConstants.ConfigureNotify) {
                            // OOPS ConfigureNotify event catched
                            oops_updated = true;
                            awtLockNotifyAll();
                        }
                    }
                };
        }

        awtLock();
        try {
            addEventDispatcher(win.getWindow(), oops_waiter);

            oops_updated = false;
            long event_number = getEventNumber();
            // Change win position each time to avoid system optimization
            oops_position += 5;
            if (oops_position > 50) {
                oops_position = 0;
            }
            // Generate OOPS ConfigureNotify event
            XlibWrapper.XMoveWindow(getDisplay(), win.getWindow(),
                                    oops_position, 0);

            XSync();

            eventLog.finer("Generated OOPS ConfigureNotify event");

            long end = TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) + timeout;
            // This "while" is a protection from spurious wake-ups.
            // However, we shouldn't wait for too long.
            while (!oops_updated) {
                timeout = timeout(end);
                if (timeout <= 0) {
                    break;
                }
                try {
                    // Wait for OOPS ConfigureNotify event
                    awtLockWait(timeout);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            // Don't take into account OOPS ConfigureNotify event
            return getEventNumber() - event_number > 1;
        } finally {
            removeEventDispatcher(win.getWindow(), oops_waiter);
            eventLog.finer("Exiting syncNativeQueue");
            awtUnlock();
        }
    }
    @Override
    public void grab(Window w) {
        final Object peer = AWTAccessor.getComponentAccessor().getPeer(w);
        if (peer != null) {
            ((XWindowPeer) peer).setGrab(true);
        }
    }

    @Override
    public void ungrab(Window w) {
        final Object peer = AWTAccessor.getComponentAccessor().getPeer(w);
        if (peer != null) {
            ((XWindowPeer) peer).setGrab(false);
        }
    }
    /**
     * Returns if the java.awt.Desktop class is supported on the current
     * desktop.
     * <p>
     * The methods of java.awt.Desktop class are supported on the Gnome desktop.
     * Check if the running desktop is Gnome by checking the window manager.
     */
    @Override
    public boolean isDesktopSupported(){
        return XDesktopPeer.isDesktopSupported();
    }

    @Override
    public DesktopPeer createDesktopPeer(Desktop target){
        return new XDesktopPeer();
    }

    @Override
    public boolean isTaskbarSupported(){
        return XTaskbarPeer.isTaskbarSupported();
    }

    @Override
    public TaskbarPeer createTaskbarPeer(Taskbar target){
        return new XTaskbarPeer();
    }

    @Override
    public boolean areExtraMouseButtonsEnabled() throws HeadlessException {
        return areExtraMouseButtonsEnabled;
    }

    @Override
    public boolean isWindowOpacitySupported() {
        XNETProtocol net_protocol = XWM.getWM().getNETProtocol();

        if (net_protocol == null) {
            return false;
        }

        return net_protocol.doOpacityProtocol();
    }

    @Override
    public boolean isWindowShapingSupported() {
        return XlibUtil.isShapingSupported();
    }

    @Override
    public boolean isWindowTranslucencySupported() {
        //NOTE: it may not be supported. The actual check is being performed
        //      at java.awt.GraphicsDevice. In X11 we need to check
        //      whether there's any translucency-capable GC available.
        return true;
    }

    @Override
    public boolean isTranslucencyCapable(GraphicsConfiguration gc) {
        if (!(gc instanceof X11GraphicsConfig)) {
            return false;
        }
        return ((X11GraphicsConfig)gc).isTranslucencyCapable();
    }

    /**
     * Returns the value of "sun.awt.disablegrab" property. Default
     * value is {@code false}.
     */
    @SuppressWarnings("removal")
    public static boolean getSunAwtDisableGrab() {
        return AccessController.doPrivileged(new GetBooleanAction("sun.awt.disablegrab"));
    }
}
