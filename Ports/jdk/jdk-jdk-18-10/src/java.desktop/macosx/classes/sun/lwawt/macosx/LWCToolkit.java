/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.lwawt.macosx;

import java.awt.AWTError;
import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.dnd.MouseDragGestureRecognizer;
import java.awt.dnd.peer.DragSourceContextPeer;
import java.awt.event.InputEvent;
import java.awt.event.InvocationEvent;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.awt.im.InputMethodHighlight;
import java.awt.im.spi.InputMethodDescriptor;
import java.awt.peer.CheckboxMenuItemPeer;
import java.awt.peer.DesktopPeer;
import java.awt.peer.DialogPeer;
import java.awt.peer.FileDialogPeer;
import java.awt.peer.FontPeer;
import java.awt.peer.MenuBarPeer;
import java.awt.peer.MenuItemPeer;
import java.awt.peer.MenuPeer;
import java.awt.peer.PopupMenuPeer;
import java.awt.peer.RobotPeer;
import java.awt.peer.SystemTrayPeer;
import java.awt.peer.TaskbarPeer;
import java.awt.peer.TrayIconPeer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import javax.swing.UIManager;

import com.apple.laf.AquaMenuBarUI;
import sun.awt.AWTAccessor;
import sun.awt.AppContext;
import sun.awt.CGraphicsDevice;
import sun.awt.LightweightFrame;
import sun.awt.PlatformGraphicsInfo;
import sun.awt.SunToolkit;
import sun.awt.datatransfer.DataTransferer;
import sun.awt.dnd.SunDragSourceContextPeer;
import sun.awt.util.ThreadGroupUtils;
import sun.java2d.metal.MTLRenderQueue;
import sun.java2d.opengl.OGLRenderQueue;
import sun.lwawt.LWComponentPeer;
import sun.lwawt.LWCursorManager;
import sun.lwawt.LWToolkit;
import sun.lwawt.LWWindowPeer;
import sun.lwawt.LWWindowPeer.PeerType;
import sun.lwawt.PlatformComponent;
import sun.lwawt.PlatformDropTarget;
import sun.lwawt.PlatformWindow;
import sun.lwawt.SecurityWarningWindow;

@SuppressWarnings("serial") // JDK implementation class
final class NamedCursor extends Cursor {
    NamedCursor(String name) {
        super(name);
    }
}

/**
 * Mac OS X Cocoa-based AWT Toolkit.
 */
public final class LWCToolkit extends LWToolkit {
    // While it is possible to enumerate all mouse devices
    // and query them for the number of buttons, the code
    // that does it is rather complex. Instead, we opt for
    // the easy way and just support up to 5 mouse buttons,
    // like Windows.
    private static final int BUTTONS = 5;

    private static native void initIDs();
    private static native void initAppkit(ThreadGroup appKitThreadGroup, boolean headless);
    private static CInputMethodDescriptor sInputMethodDescriptor;

    static {
        System.err.flush();

        @SuppressWarnings("removal")
        ResourceBundle platformResources = java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<ResourceBundle>() {
            @Override
            public ResourceBundle run() {
                ResourceBundle platformResources = null;
                try {
                    platformResources = ResourceBundle.getBundle("sun.awt.resources.awtosx");
                } catch (MissingResourceException e) {
                    // No resource file; defaults will be used.
                }

                System.loadLibrary("awt");
                System.loadLibrary("fontmanager");

                return platformResources;
            }
        });

        if (!GraphicsEnvironment.isHeadless() &&
            !PlatformGraphicsInfo.isInAquaSession())
        {
            throw new AWTError("WindowServer is not available");
        }

        AWTAccessor.getToolkitAccessor().setPlatformResources(platformResources);

        if (!GraphicsEnvironment.isHeadless()) {
            initIDs();
        }
    }

    /*
     * If true  we operate in normal mode and nested runloop is executed in JavaRunLoopMode
     * If false we operate in singleThreaded FX/AWT interop mode and nested loop uses NSDefaultRunLoopMode
     */
    @SuppressWarnings("removal")
    private static final boolean inAWT
            = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
        @Override
        public Boolean run() {
            return !Boolean.parseBoolean(
                    System.getProperty("javafx.embed.singleThread", "false"));
        }
    });

    @SuppressWarnings("removal")
    public LWCToolkit() {
        final String extraButtons = "sun.awt.enableExtraMouseButtons";
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            areExtraMouseButtonsEnabled =
                 Boolean.parseBoolean(System.getProperty(extraButtons, "true"));
            //set system property if not yet assigned
            System.setProperty(extraButtons, ""+areExtraMouseButtonsEnabled);
            initAppkit(ThreadGroupUtils.getRootThreadGroup(),
                       GraphicsEnvironment.isHeadless());
            return null;
        });
    }

    /*
     * System colors with default initial values, overwritten by toolkit if system values differ and are available.
     */
    private static final int NUM_APPLE_COLORS = 4;
    public static final int KEYBOARD_FOCUS_COLOR = 0;
    public static final int INACTIVE_SELECTION_BACKGROUND_COLOR = 1;
    public static final int INACTIVE_SELECTION_FOREGROUND_COLOR = 2;
    public static final int SELECTED_CONTROL_TEXT_COLOR = 3;
    private static int[] appleColors = {
        0xFF808080, // keyboardFocusColor = Color.gray;
        0xFFC0C0C0, // secondarySelectedControlColor
        0xFF303030, // controlDarkShadowColor
        0xFFFFFFFF, // controlTextColor
    };

    private native void loadNativeColors(final int[] systemColors, final int[] appleColors);

    @Override
    protected void loadSystemColors(final int[] systemColors) {
        if (systemColors == null) return;
        loadNativeColors(systemColors, appleColors);
    }

    @SuppressWarnings("serial") // JDK implementation class
    private static class AppleSpecificColor extends Color {
        private final int index;
        AppleSpecificColor(int index) {
            super(appleColors[index]);
            this.index = index;
        }

        @Override
        public int getRGB() {
            return appleColors[index];
        }
    }

    /**
     * Returns Apple specific colors that we may expose going forward.
     */
    public static Color getAppleColor(int color) {
        return new AppleSpecificColor(color);
    }

    // This is only called from native code.
    @SuppressWarnings("removal")
    static void systemColorsChanged() {
        EventQueue.invokeLater(() -> {
            AccessController.doPrivileged( (PrivilegedAction<Object>) () -> {
                AWTAccessor.getSystemColorAccessor().updateSystemColors();
                return null;
            });
        });
    }

    public static LWCToolkit getLWCToolkit() {
        return (LWCToolkit)Toolkit.getDefaultToolkit();
    }

    @Override
    protected PlatformWindow createPlatformWindow(PeerType peerType) {
        if (peerType == PeerType.EMBEDDED_FRAME) {
            return new CPlatformEmbeddedFrame();
        } else if (peerType == PeerType.VIEW_EMBEDDED_FRAME) {
            return new CViewPlatformEmbeddedFrame();
        } else if (peerType == PeerType.LW_FRAME) {
            return new CPlatformLWWindow();
        } else {
            assert (peerType == PeerType.SIMPLEWINDOW
                    || peerType == PeerType.DIALOG
                    || peerType == PeerType.FRAME);
            return new CPlatformWindow();
        }
    }

    LWWindowPeer createEmbeddedFrame(CEmbeddedFrame target) {
        PlatformComponent platformComponent = createPlatformComponent();
        PlatformWindow platformWindow = createPlatformWindow(PeerType.EMBEDDED_FRAME);
        return createDelegatedPeer(target, platformComponent, platformWindow, PeerType.EMBEDDED_FRAME);
    }

    LWWindowPeer createEmbeddedFrame(CViewEmbeddedFrame target) {
        PlatformComponent platformComponent = createPlatformComponent();
        PlatformWindow platformWindow = createPlatformWindow(PeerType.VIEW_EMBEDDED_FRAME);
        return createDelegatedPeer(target, platformComponent, platformWindow, PeerType.VIEW_EMBEDDED_FRAME);
    }

    private CPrinterDialogPeer createCPrinterDialog(CPrinterDialog target) {
        PlatformComponent platformComponent = createPlatformComponent();
        PlatformWindow platformWindow = createPlatformWindow(PeerType.DIALOG);
        CPrinterDialogPeer peer = new CPrinterDialogPeer(target, platformComponent, platformWindow);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public DialogPeer createDialog(Dialog target) {
        if (target instanceof CPrinterDialog) {
            return createCPrinterDialog((CPrinterDialog)target);
        }
        return super.createDialog(target);
    }

    @Override
    protected SecurityWarningWindow createSecurityWarning(Window ownerWindow,
                                                          LWWindowPeer ownerPeer) {
        return new CWarningWindow(ownerWindow, ownerPeer);
    }

    @Override
    protected PlatformComponent createPlatformComponent() {
        return new CPlatformComponent();
    }

    @Override
    protected PlatformComponent createLwPlatformComponent() {
        return new CPlatformLWComponent();
    }

    @Override
    protected FileDialogPeer createFileDialogPeer(FileDialog target) {
        return new CFileDialog(target);
    }

    @Override
    public MenuPeer createMenu(Menu target) {
        MenuPeer peer = new CMenu(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public MenuBarPeer createMenuBar(MenuBar target) {
        MenuBarPeer peer = new CMenuBar(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public MenuItemPeer createMenuItem(MenuItem target) {
        MenuItemPeer peer = new CMenuItem(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public CheckboxMenuItemPeer createCheckboxMenuItem(CheckboxMenuItem target) {
        CheckboxMenuItemPeer peer = new CCheckboxMenuItem(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public PopupMenuPeer createPopupMenu(PopupMenu target) {
        PopupMenuPeer peer = new CPopupMenu(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public SystemTrayPeer createSystemTray(SystemTray target) {
        return new CSystemTray();
    }

    @Override
    public TrayIconPeer createTrayIcon(TrayIcon target) {
        TrayIconPeer peer = new CTrayIcon(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public DesktopPeer createDesktopPeer(Desktop target) {
        return new CDesktopPeer();
    }

    @Override
    public TaskbarPeer createTaskbarPeer(Taskbar target) {
        return new CTaskbarPeer();
    }

    @Override
    public LWCursorManager getCursorManager() {
        return CCursorManager.getInstance();
    }

    @Override
    public Cursor createCustomCursor(final Image cursor, final Point hotSpot,
                                     final String name)
            throws IndexOutOfBoundsException, HeadlessException {
        return new CCustomCursor(cursor, hotSpot, name);
    }

    @Override
    public Dimension getBestCursorSize(final int preferredWidth,
                                       final int preferredHeight)
            throws HeadlessException {
        return CCustomCursor.getBestCursorSize(preferredWidth, preferredHeight);
    }

    @Override
    protected void platformCleanup() {
        // TODO Auto-generated method stub
    }

    @Override
    protected void platformInit() {
        // TODO Auto-generated method stub
    }

    @Override
    protected void platformRunMessage() {
        // TODO Auto-generated method stub
    }

    @Override
    protected void platformShutdown() {
        // TODO Auto-generated method stub
    }

    class OSXPlatformFont extends sun.awt.PlatformFont
    {
        OSXPlatformFont(String name, int style)
        {
            super(name, style);
        }
        @Override
        protected char getMissingGlyphCharacter()
        {
            // Follow up for real implementation
            return (char)0xfff8; // see http://developer.apple.com/fonts/LastResortFont/
        }
    }
    @Override
    public FontPeer getFontPeer(String name, int style) {
        return new OSXPlatformFont(name, style);
    }

    @Override
    protected void initializeDesktopProperties() {
        super.initializeDesktopProperties();
        Map <Object, Object> fontHints = new HashMap<>();
        fontHints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        desktopProperties.put(SunToolkit.DESKTOPFONTHINTS, fontHints);
        desktopProperties.put("awt.mouse.numButtons", BUTTONS);
        desktopProperties.put("awt.multiClickInterval", getMultiClickTime());

        // These DnD properties must be set, otherwise Swing ends up spewing NPEs
        // all over the place. The values came straight off of MToolkit.
        desktopProperties.put("DnD.Autoscroll.initialDelay", Integer.valueOf(50));
        desktopProperties.put("DnD.Autoscroll.interval", Integer.valueOf(50));
        desktopProperties.put("DnD.Autoscroll.cursorHysteresis", Integer.valueOf(5));

        desktopProperties.put("DnD.isDragImageSupported", Boolean.TRUE);

        // Register DnD cursors
        desktopProperties.put("DnD.Cursor.CopyDrop", new NamedCursor("DnD.Cursor.CopyDrop"));
        desktopProperties.put("DnD.Cursor.MoveDrop", new NamedCursor("DnD.Cursor.MoveDrop"));
        desktopProperties.put("DnD.Cursor.LinkDrop", new NamedCursor("DnD.Cursor.LinkDrop"));
        desktopProperties.put("DnD.Cursor.CopyNoDrop", new NamedCursor("DnD.Cursor.CopyNoDrop"));
        desktopProperties.put("DnD.Cursor.MoveNoDrop", new NamedCursor("DnD.Cursor.MoveNoDrop"));
        desktopProperties.put("DnD.Cursor.LinkNoDrop", new NamedCursor("DnD.Cursor.LinkNoDrop"));
    }

    @Override
    protected boolean syncNativeQueue(long timeout) {
        if (timeout <= 0) {
            return false;
        }
        if (SunDragSourceContextPeer.isDragDropInProgress()
                || EventQueue.isDispatchThread()) {
            // The java code started the DnD, but the native drag may still not
            // start, the last attempt to flush the native events,
            // also do not block EDT for a long time
            timeout = 50;
        }
        return nativeSyncQueue(timeout);
    }

    @Override
    public native void beep();

    @Override
    public int getScreenResolution() throws HeadlessException {
        return (int) ((CGraphicsDevice) GraphicsEnvironment
                .getLocalGraphicsEnvironment().getDefaultScreenDevice())
                .getXResolution();
    }

    @Override
    public Insets getScreenInsets(final GraphicsConfiguration gc) {
        GraphicsDevice gd = gc.getDevice();
        if (!(gd instanceof CGraphicsDevice)) {
            return super.getScreenInsets(gc);
        }
        return ((CGraphicsDevice)gd).getScreenInsets();
    }

    @Override
    public void sync() {
        // flush the rendering pipeline
        if (CGraphicsDevice.usingMetalPipeline()) {
            MTLRenderQueue.sync();
        } else {
            OGLRenderQueue.sync();
        }
        // setNeedsDisplay() selector was sent to the appropriate CALayer so now
        // we have to flush the native selectors queue.
        flushNativeSelectors();
    }

    @Override
    public RobotPeer createRobot(GraphicsDevice screen) throws AWTException {
        if (screen instanceof CGraphicsDevice) {
            return new CRobot((CGraphicsDevice) screen);
        }
        return super.createRobot(screen);
    }

    private native boolean isCapsLockOn();

    /*
     * NOTE: Among the keys this method is supposed to check,
     * only Caps Lock works as a true locking key with OS X.
     * There is no Scroll Lock key on modern Apple keyboards,
     * and with a PC keyboard plugged in Scroll Lock is simply
     * ignored: no LED lights up if you press it.
     * The key located at the same position on Apple keyboards
     * as Num Lock on PC keyboards is called Clear, doesn't lock
     * anything and is used for entirely different purpose.
     */
    @Override
    public boolean getLockingKeyState(int keyCode) throws UnsupportedOperationException {
        switch (keyCode) {
            case KeyEvent.VK_NUM_LOCK:
            case KeyEvent.VK_SCROLL_LOCK:
            case KeyEvent.VK_KANA_LOCK:
                throw new UnsupportedOperationException("Toolkit.getLockingKeyState");

            case KeyEvent.VK_CAPS_LOCK:
                return isCapsLockOn();

            default:
                throw new IllegalArgumentException("invalid key for Toolkit.getLockingKeyState");
        }
    }

    //Is it allowed to generate events assigned to extra mouse buttons.
    //Set to true by default.
    private static boolean areExtraMouseButtonsEnabled = true;

    @Override
    public boolean areExtraMouseButtonsEnabled() throws HeadlessException {
        return areExtraMouseButtonsEnabled;
    }

    @Override
    public int getNumberOfButtons(){
        return BUTTONS;
    }

    /**
     * Returns the double-click time interval in ms.
     */
    private static native int getMultiClickTime();

    @Override
    public boolean isTraySupported() {
        return true;
    }

    @Override
    public DataTransferer getDataTransferer() {
        return CDataTransferer.getInstanceImpl();
    }

    @Override
    public boolean isAlwaysOnTopSupported() {
        return true;
    }

    private static final String APPKIT_THREAD_NAME = "AppKit Thread";

    // Intended to be called from the LWCToolkit.m only.
    @SuppressWarnings("removal")
    private static void installToolkitThreadInJava() {
        Thread.currentThread().setName(APPKIT_THREAD_NAME);
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            Thread.currentThread().setContextClassLoader(null);
            return null;
        });
    }

    @Override
    public boolean isWindowOpacitySupported() {
        return true;
    }

    @Override
    public boolean isFrameStateSupported(int state) throws HeadlessException {
        switch (state) {
            case Frame.NORMAL:
            case Frame.ICONIFIED:
            case Frame.MAXIMIZED_BOTH:
                return true;
            default:
                return false;
        }
    }

    @Override
    @Deprecated(since = "10")
    public int getMenuShortcutKeyMask() {
        return Event.META_MASK;
    }

    @Override
    public int getMenuShortcutKeyMaskEx() {
        return InputEvent.META_DOWN_MASK;
    }

    @Override
    public Image getImage(final String filename) {
        final Image nsImage = checkForNSImage(filename);
        if (nsImage != null) {
            return nsImage;
        }

        if (imageCached(filename)) {
            return super.getImage(filename);
        }

        String filename2x = getScaledImageName(filename);
        return (imageExists(filename2x))
                ? getImageWithResolutionVariant(filename, filename2x)
                : super.getImage(filename);
    }

    @Override
    public Image getImage(URL url) {

        if (imageCached(url)) {
            return super.getImage(url);
        }

        URL url2x = getScaledImageURL(url);
        return (imageExists(url2x))
                ? getImageWithResolutionVariant(url, url2x) : super.getImage(url);
    }

    private static final String nsImagePrefix = "NSImage://";
    private Image checkForNSImage(final String imageName) {
        if (imageName == null) return null;
        if (!imageName.startsWith(nsImagePrefix)) return null;
        return CImage.getCreator().createImageFromName(imageName.substring(nsImagePrefix.length()));
    }

    // Thread-safe Object.equals() called from native
    public static boolean doEquals(final Object a, final Object b, Component c) {
        if (a == b) return true;

        final boolean[] ret = new boolean[1];

        try {  invokeAndWait(new Runnable() { @Override
                                              public void run() { synchronized(ret) {
            ret[0] = a.equals(b);
        }}}, c); } catch (Exception e) { e.printStackTrace(); }

        synchronized(ret) { return ret[0]; }
    }

    public static <T> T invokeAndWait(final Callable<T> callable,
                                      Component component) throws Exception {
        final CallableWrapper<T> wrapper = new CallableWrapper<>(callable);
        invokeAndWait(wrapper, component);
        return wrapper.getResult();
    }

    static final class CallableWrapper<T> implements Runnable {
        final Callable<T> callable;
        T object;
        Exception e;

        CallableWrapper(final Callable<T> callable) {
            this.callable = callable;
        }

        @Override
        public void run() {
            try {
                object = callable.call();
            } catch (final Exception e) {
                this.e = e;
            }
        }

        public T getResult() throws Exception {
            if (e != null) throw e;
            return object;
        }
    }

    /**
     * Kicks an event over to the appropriate event queue and waits for it to
     * finish To avoid deadlocking, we manually run the NSRunLoop while waiting
     * Any selector invoked using ThreadUtilities performOnMainThread will be
     * processed in doAWTRunLoop The InvocationEvent will call
     * LWCToolkit.stopAWTRunLoop() when finished, which will stop our manual
     * run loop. Does not dispatch native events while in the loop
     */
    public static void invokeAndWait(Runnable runnable, Component component)
            throws InvocationTargetException {
        Objects.requireNonNull(component, "Null component provided to invokeAndWait");

        long mediator = createAWTRunLoopMediator();
        InvocationEvent invocationEvent =
                new InvocationEvent(component,
                        runnable,
                        () -> {
                            if (mediator != 0) {
                                stopAWTRunLoop(mediator);
                            }
                        },
                        true);

        AppContext appContext = SunToolkit.targetToAppContext(component);
        SunToolkit.postEvent(appContext, invocationEvent);
        // 3746956 - flush events from PostEventQueue to prevent them from getting stuck and causing a deadlock
        SunToolkit.flushPendingEvents(appContext);
        doAWTRunLoop(mediator, false);

        checkException(invocationEvent);
    }

    public static void invokeLater(Runnable event, Component component)
            throws InvocationTargetException {
        Objects.requireNonNull(component, "Null component provided to invokeLater");

        InvocationEvent invocationEvent = new InvocationEvent(component, event);

        AppContext appContext = SunToolkit.targetToAppContext(component);
        SunToolkit.postEvent(SunToolkit.targetToAppContext(component), invocationEvent);
        // 3746956 - flush events from PostEventQueue to prevent them from getting stuck and causing a deadlock
        SunToolkit.flushPendingEvents(appContext);

        checkException(invocationEvent);
    }

    /**
     * Checks if exception occurred while {@code InvocationEvent} was processed and rethrows it as
     * an {@code InvocationTargetException}
     *
     * @param event the event to check for an exception
     * @throws InvocationTargetException if exception occurred when event was processed
     */
    private static void checkException(InvocationEvent event) throws InvocationTargetException {
        Throwable eventException = event.getException();
        if (eventException == null) return;

        if (eventException instanceof UndeclaredThrowableException) {
            eventException = ((UndeclaredThrowableException)eventException).getUndeclaredThrowable();
        }
        throw new InvocationTargetException(eventException);
    }

    /**
     * Schedules a {@code Runnable} execution on the Appkit thread after a delay
     * @param r a {@code Runnable} to execute
     * @param delay a delay in milliseconds
     */
    static native void performOnMainThreadAfterDelay(Runnable r, long delay);

// DnD support

    @Override
    public DragSourceContextPeer createDragSourceContextPeer(
            DragGestureEvent dge) throws InvalidDnDOperationException {
        final LightweightFrame f = SunToolkit.getLightweightFrame(dge.getComponent());
        if (f != null) {
            return f.createDragSourceContextPeer(dge);
        }

        return CDragSourceContextPeer.createDragSourceContextPeer(dge);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends DragGestureRecognizer> T createDragGestureRecognizer(
            Class<T> abstractRecognizerClass, DragSource ds, Component c,
            int srcActions, DragGestureListener dgl) {
        final LightweightFrame f = SunToolkit.getLightweightFrame(c);
        if (f != null) {
            return f.createDragGestureRecognizer(abstractRecognizerClass, ds, c, srcActions, dgl);
        }

        DragGestureRecognizer dgr = null;

        // Create a new mouse drag gesture recognizer if we have a class match:
        if (MouseDragGestureRecognizer.class.equals(abstractRecognizerClass))
            dgr = new CMouseDragGestureRecognizer(ds, c, srcActions, dgl);

        return (T)dgr;
    }

    @Override
    protected PlatformDropTarget createDropTarget(DropTarget dropTarget,
                                                  Component component,
                                                  LWComponentPeer<?, ?> peer) {
        return new CDropTarget(dropTarget, component, peer);
    }

    // InputMethodSupport Method
    /**
     * Returns the default keyboard locale of the underlying operating system
     */
    @Override
    public Locale getDefaultKeyboardLocale() {
        Locale locale = CInputMethod.getNativeLocale();

        if (locale == null) {
            return super.getDefaultKeyboardLocale();
        }

        return locale;
    }

    public static boolean isLocaleUSInternationalPC(Locale locale) {
        return (locale != null ?
            locale.toString().equals("_US_UserDefined_15000") : false);
    }

    public static boolean isCharModifierKeyInUSInternationalPC(char ch) {
        // 5 characters: APOSTROPHE, QUOTATION MARK, ACCENT GRAVE, SMALL TILDE,
        // CIRCUMFLEX ACCENT
        final char[] modifierKeys = {'\'', '"', '`', '\u02DC', '\u02C6'};
        for (char modKey : modifierKeys) {
            if (modKey == ch) {
                return true;
            }
        }
        return false;
    }

    @Override
    public InputMethodDescriptor getInputMethodAdapterDescriptor() {
        if (sInputMethodDescriptor == null)
            sInputMethodDescriptor = new CInputMethodDescriptor();

        return sInputMethodDescriptor;
    }

    /**
     * Returns a map of visual attributes for thelevel description
     * of the given input method highlight, or null if no mapping is found.
     * The style field of the input method highlight is ignored. The map
     * returned is unmodifiable.
     * @param highlight input method highlight
     * @return style attribute map, or null
     * @since 1.3
     */
    @Override
    public Map<TextAttribute, ?> mapInputMethodHighlight(InputMethodHighlight highlight) {
        return CInputMethod.mapInputMethodHighlight(highlight);
    }

    /**
     * Returns key modifiers used by Swing to set up a focus accelerator key
     * stroke.
     */
    @Override
    @SuppressWarnings("deprecation")
    public int getFocusAcceleratorKeyMask() {
        return InputEvent.CTRL_MASK | InputEvent.ALT_MASK;
    }

    /**
     * Tests whether specified key modifiers mask can be used to enter a
     * printable character.
     */
    @Override
    @SuppressWarnings("deprecation")
    public boolean isPrintableCharacterModifiersMask(int mods) {
        return ((mods & (InputEvent.META_MASK | InputEvent.CTRL_MASK)) == 0);
    }

    /**
     * Returns whether popup is allowed to be shown above the task bar.
     */
    @Override
    public boolean canPopupOverlapTaskBar() {
        return false;
    }

    /*
     * Returns true if the application (one of its windows) owns keyboard focus.
     */
    native boolean isApplicationActive();

    /**
     * Returns true if AWT toolkit is embedded, false otherwise.
     *
     * @return true if AWT toolkit is embedded, false otherwise
     */
    public static native boolean isEmbedded();

    /*
     * Activates application ignoring other apps.
     */
    public native void activateApplicationIgnoringOtherApps();

    /************************
     * Native methods section
     ************************/

    static native long createAWTRunLoopMediator();
    /**
     * Method to run a nested run-loop. The nested loop is spinned in the javaRunLoop mode, so selectors sent
     * by [JNFRunLoop performOnMainThreadWaiting] are processed.
     * @param mediator a native pointer to the mediator object created by createAWTRunLoopMediator
     * @param processEvents if true - dispatches event while in the nested loop. Used in DnD.
     *                      Additional attention is needed when using this feature as we short-circuit normal event
     *                      processing which could break Appkit.
     *                      (One known example is when the window is resized with the mouse)
     *
     *                      if false - all events come after exit form the nested loop
     */
    static void doAWTRunLoop(long mediator, boolean processEvents) {
        doAWTRunLoopImpl(mediator, processEvents, inAWT);
    }
    private static native void doAWTRunLoopImpl(long mediator, boolean processEvents, boolean inAWT);
    static native void stopAWTRunLoop(long mediator);

    private native boolean nativeSyncQueue(long timeout);

    /**
     * Just spin a single empty block synchronously.
     */
    static native void flushNativeSelectors();

    @Override
    public Clipboard createPlatformClipboard() {
        return new CClipboard("System");
    }

    @Override
    public boolean isModalExclusionTypeSupported(Dialog.ModalExclusionType exclusionType) {
        return (exclusionType == null) ||
            (exclusionType == Dialog.ModalExclusionType.NO_EXCLUDE) ||
            (exclusionType == Dialog.ModalExclusionType.APPLICATION_EXCLUDE) ||
            (exclusionType == Dialog.ModalExclusionType.TOOLKIT_EXCLUDE);
    }

    @Override
    public boolean isModalityTypeSupported(Dialog.ModalityType modalityType) {
        //TODO: FileDialog blocks excluded windows...
        //TODO: Test: 2 file dialogs, separate AppContexts: a) Dialog 1 blocked, shouldn't be. Frame 4 blocked (shouldn't be).
        return (modalityType == null) ||
            (modalityType == Dialog.ModalityType.MODELESS) ||
            (modalityType == Dialog.ModalityType.DOCUMENT_MODAL) ||
            (modalityType == Dialog.ModalityType.APPLICATION_MODAL) ||
            (modalityType == Dialog.ModalityType.TOOLKIT_MODAL);
    }

    @Override
    public boolean isWindowShapingSupported() {
        return true;
    }

    @Override
    public boolean isWindowTranslucencySupported() {
        return true;
    }

    @Override
    public boolean isTranslucencyCapable(GraphicsConfiguration gc) {
        return true;
    }

    @Override
    public boolean isSwingBackbufferTranslucencySupported() {
        return true;
    }

    @Override
    public boolean enableInputMethodsForTextComponent() {
        return true;
    }

    private static URL getScaledImageURL(URL url) {
        try {
            String scaledImagePath = getScaledImageName(url.getPath());
            return scaledImagePath == null ? null : new URL(url.getProtocol(),
                    url.getHost(), url.getPort(), scaledImagePath);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private static String getScaledImageName(String path) {
        if (!isValidPath(path)) {
            return null;
        }

        int slash = path.lastIndexOf('/');
        String name = (slash < 0) ? path : path.substring(slash + 1);

        if (name.contains("@2x")) {
            return null;
        }

        int dot = name.lastIndexOf('.');
        String name2x = (dot < 0) ? name + "@2x"
                : name.substring(0, dot) + "@2x" + name.substring(dot);
        return (slash < 0) ? name2x : path.substring(0, slash + 1) + name2x;
    }

    private static boolean isValidPath(String path) {
        return path != null &&
                !path.isEmpty() &&
                !path.endsWith("/") &&
                !path.endsWith(".");
    }

    @Override
    protected PlatformWindow getPlatformWindowUnderMouse() {
        return CPlatformWindow.nativeGetTopmostPlatformWindowUnderMouse();
    }

    @Override
    public void updateScreenMenuBarUI() {
        if (AquaMenuBarUI.getScreenMenuBarProperty())  {
            UIManager.put("MenuBarUI", "com.apple.laf.AquaMenuBarUI");
        } else {
            UIManager.put("MenuBarUI", null);
        }
    }
}
