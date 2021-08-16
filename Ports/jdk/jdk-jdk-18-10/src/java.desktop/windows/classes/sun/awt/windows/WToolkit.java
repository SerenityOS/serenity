/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.windows;

import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.CheckboxMenuItem;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Insets;
import java.awt.JobAttributes;
import java.awt.Label;
import java.awt.List;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.PageAttributes;
import java.awt.Panel;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.PrintJob;
import java.awt.RenderingHints;
import java.awt.ScrollPane;
import java.awt.Scrollbar;
import java.awt.SystemTray;
import java.awt.Taskbar;
import java.awt.TextArea;
import java.awt.TextComponent;
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
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.im.InputMethodHighlight;
import java.awt.im.spi.InputMethodDescriptor;
import java.awt.image.ColorModel;
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
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.text.JTextComponent;

import sun.awt.AWTAccessor;
import sun.awt.AWTAutoShutdown;
import sun.awt.AWTPermissions;
import sun.awt.AppContext;
import sun.awt.DisplayChangedListener;
import sun.awt.LightweightFrame;
import sun.awt.SunToolkit;
import sun.awt.Win32GraphicsDevice;
import sun.awt.Win32GraphicsEnvironment;
import sun.awt.datatransfer.DataTransferer;
import sun.awt.util.PerformanceLogger;
import sun.awt.util.ThreadGroupUtils;
import sun.font.FontManager;
import sun.font.FontManagerFactory;
import sun.font.SunFontManager;
import sun.java2d.d3d.D3DRenderQueue;
import sun.java2d.opengl.OGLRenderQueue;
import sun.print.PrintJob2D;
import sun.util.logging.PlatformLogger;

public final class WToolkit extends SunToolkit implements Runnable {

    private static final PlatformLogger log = PlatformLogger.getLogger("sun.awt.windows.WToolkit");

    // Desktop property which specifies whether XP visual styles are in effect
    public static final String XPSTYLE_THEME_ACTIVE = "win.xpstyle.themeActive";

    // System clipboard.
    WClipboard clipboard;

    // cache of font peers
    private Hashtable<String,FontPeer> cacheFontPeer;

    // Windows properties
    private WDesktopProperties  wprops;

    // Dynamic Layout Resize client code setting
    protected boolean dynamicLayoutSetting = false;

    //Is it allowed to generate events assigned to extra mouse buttons.
    //Set to true by default.
    private static boolean areExtraMouseButtonsEnabled = true;

    /**
     * Initialize JNI field and method IDs
     */
    private static native void initIDs();
    private static boolean loaded = false;
    @SuppressWarnings("removal")
    public static void loadLibraries() {
        if (!loaded) {
            java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        System.loadLibrary("awt");
                        return null;
                    }
                });
            loaded = true;
        }
    }

    private static native String getWindowsVersion();

    static {
        loadLibraries();
        initIDs();

        // Print out which version of Windows is running
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("Win version: " + getWindowsVersion());
        }
    }

    /*
     * NOTE: The following embedded*() methods are non-public API intended
     * for internal use only.  The methods are unsupported and could go
     * away in future releases.
     *
     * New hook functions for using the AWT as an embedded service. These
     * functions replace the global C function AwtInit() which was previously
     * exported by awt.dll.
     *
     * When used as an embedded service, the AWT does NOT have its own
     * message pump. It instead relies on the parent application to provide
     * this functionality. embeddedInit() assumes that the thread on which it
     * is called is the message pumping thread. Violating this assumption
     * will lead to undefined behavior.
     *
     * embeddedInit must be called before the WToolkit() constructor.
     * embeddedDispose should be called before the applicaton terminates the
     * Java VM. It is currently unsafe to reinitialize the toolkit again
     * after it has been disposed. Instead, awt.dll must be reloaded and the
     * class loader which loaded WToolkit must be finalized before it is
     * safe to reuse AWT. Dynamic reusability may be added to the toolkit in
     * the future.
     */

    /**
     * Initializes the Toolkit for use in an embedded environment.
     *
     * @return true if the initialization succeeded; false if it failed.
     *         The function will fail if the Toolkit was already initialized.
     * @since 1.3
     */
    public static native boolean embeddedInit();

    /**
     * Disposes the Toolkit in an embedded environment. This method should
     * not be called on exit unless the Toolkit was constructed with
     * embeddedInit.
     *
     * @return true if the disposal succeeded; false if it failed. The
     *         function will fail if the calling thread is not the same
     *         thread which called embeddedInit(), or if the Toolkit was
     *         already disposed.
     * @since 1.3
     */
    public static native boolean embeddedDispose();

    /**
     * To be called after processing the event queue by users of the above
     * embeddedInit() function.  The reason for this additional call is that
     * there are some operations performed during idle time in the AwtToolkit
     * event loop which should also be performed during idle time in any
     * other native event loop.  Failure to do so could result in
     * deadlocks.
     *
     * This method was added at the last minute of the jdk1.4 release
     * to work around a specific customer problem.  As with the above
     * embedded*() class, this method is non-public and should not be
     * used by external applications.
     *
     * See bug #4526587 for more information.
     */
    public native void embeddedEventLoopIdleProcessing();

    static class ToolkitDisposer implements sun.java2d.DisposerRecord {
        @Override
        public void dispose() {
            WToolkit.postDispose();
        }
    }

    private final Object anchor = new Object();

    private static native void postDispose();

    private static native boolean startToolkitThread(Runnable thread, ThreadGroup rootThreadGroup);

    @SuppressWarnings("removal")
    public WToolkit() {
        // Startup toolkit threads
        if (PerformanceLogger.loggingEnabled()) {
            PerformanceLogger.setTime("WToolkit construction");
        }

        sun.java2d.Disposer.addRecord(anchor, new ToolkitDisposer());

        /*
         * Fix for 4701990.
         * AWTAutoShutdown state must be changed before the toolkit thread
         * starts to avoid race condition.
         */
        AWTAutoShutdown.notifyToolkitThreadBusy();

        // Find a root TG and attach toolkit thread to it
        ThreadGroup rootTG = AccessController.doPrivileged(
                (PrivilegedAction<ThreadGroup>) ThreadGroupUtils::getRootThreadGroup);
        if (!startToolkitThread(this, rootTG)) {
            final String name = "AWT-Windows";
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                Thread toolkitThread = new Thread(rootTG, this, name, 0, false);
                toolkitThread.setDaemon(true);
                toolkitThread.start();
                return null;
            });
        }

        try {
            synchronized(this) {
                while(!inited) {
                    wait();
                }
            }
        } catch (InterruptedException x) {
            // swallow the exception
        }

        // Enabled "live resizing" by default.  It remains controlled
        // by the native system though.
        setDynamicLayout(true);
        final String extraButtons = "sun.awt.enableExtraMouseButtons";
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            areExtraMouseButtonsEnabled =
                 Boolean.parseBoolean(System.getProperty(extraButtons, "true"));
            //set system property if not yet assigned
            System.setProperty(extraButtons, ""+areExtraMouseButtonsEnabled);
            return null;
        });
        setExtraMouseButtonsEnabledNative(areExtraMouseButtonsEnabled);
    }

    @SuppressWarnings("removal")
    private void registerShutdownHook() {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            Thread shutdown = new Thread(
                    ThreadGroupUtils.getRootThreadGroup(), this::shutdown,
                    "ToolkitShutdown", 0, false);
            shutdown.setContextClassLoader(null);
            Runtime.getRuntime().addShutdownHook(shutdown);
            return null;
        });
     }

    @SuppressWarnings("removal")
    @Override
    public void run() {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            Thread.currentThread().setContextClassLoader(null);
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);
            return null;
        });

        boolean startPump = init();

        if (startPump) {
            registerShutdownHook();
        }

        synchronized(this) {
            inited = true;
            notifyAll();
        }

        if (startPump) {
            eventLoop(); // will Dispose Toolkit when shutdown hook executes
        }
    }

    /*
     * eventLoop() begins the native message pump which retrieves and processes
     * native events.
     *
     * When shutdown() is called by the ShutdownHook added in run(), a
     * WM_QUIT message is posted to the Toolkit thread indicating that
     * eventLoop() should Dispose the toolkit and exit.
     */
    private native boolean init();
    private boolean inited = false;

    private native void eventLoop();
    private native void shutdown();

    /*
     * Instead of blocking the "AWT-Windows" thread uselessly on a semaphore,
     * use these functions. startSecondaryEventLoop() corresponds to wait()
     * and quitSecondaryEventLoop() corresponds to notify.
     *
     * These functions simulate blocking while allowing the AWT to continue
     * processing native events, eliminating a potential deadlock situation
     * with SendMessage.
     *
     * WARNING: startSecondaryEventLoop must only be called from the "AWT-
     * Windows" thread.
     */
    static native void startSecondaryEventLoop();
    static native void quitSecondaryEventLoop();

    /*
     * Create peer objects.
     */

    @Override
    public ButtonPeer createButton(Button target) {
        ButtonPeer peer = new WButtonPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public TextFieldPeer createTextField(TextField target) {
        TextFieldPeer peer = new WTextFieldPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public LabelPeer createLabel(Label target) {
        LabelPeer peer = new WLabelPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public ListPeer createList(List target) {
        ListPeer peer = new WListPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public CheckboxPeer createCheckbox(Checkbox target) {
        CheckboxPeer peer = new WCheckboxPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public ScrollbarPeer createScrollbar(Scrollbar target) {
        ScrollbarPeer peer = new WScrollbarPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public ScrollPanePeer createScrollPane(ScrollPane target) {
        ScrollPanePeer peer = new WScrollPanePeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public TextAreaPeer createTextArea(TextArea target) {
        TextAreaPeer peer = new WTextAreaPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public ChoicePeer createChoice(Choice target) {
        ChoicePeer peer = new WChoicePeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public FramePeer  createFrame(Frame target) {
        FramePeer peer = new WFramePeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public FramePeer createLightweightFrame(LightweightFrame target) {
        FramePeer peer = new WLightweightFramePeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public CanvasPeer createCanvas(Canvas target) {
        CanvasPeer peer = new WCanvasPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public void disableBackgroundErase(Canvas canvas) {
        WCanvasPeer peer = AWTAccessor.getComponentAccessor().getPeer(canvas);
        if (peer == null) {
            throw new IllegalStateException("Canvas must have a valid peer");
        }
        peer.disableBackgroundErase();
    }

    @Override
    public PanelPeer createPanel(Panel target) {
        PanelPeer peer = new WPanelPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public WindowPeer createWindow(Window target) {
        WindowPeer peer = new WWindowPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public DialogPeer createDialog(Dialog target) {
        DialogPeer peer = new WDialogPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public FileDialogPeer createFileDialog(FileDialog target) {
        FileDialogPeer peer = new WFileDialogPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public MenuBarPeer createMenuBar(MenuBar target) {
        MenuBarPeer peer = new WMenuBarPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public MenuPeer createMenu(Menu target) {
        MenuPeer peer = new WMenuPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public PopupMenuPeer createPopupMenu(PopupMenu target) {
        PopupMenuPeer peer = new WPopupMenuPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public MenuItemPeer createMenuItem(MenuItem target) {
        MenuItemPeer peer = new WMenuItemPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public CheckboxMenuItemPeer createCheckboxMenuItem(CheckboxMenuItem target) {
        CheckboxMenuItemPeer peer = new WCheckboxMenuItemPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public RobotPeer createRobot(GraphicsDevice screen) throws AWTException {
        if (screen instanceof Win32GraphicsDevice) {
            return new WRobotPeer();
        }
        return super.createRobot(screen);
    }

    public WEmbeddedFramePeer createEmbeddedFrame(WEmbeddedFrame target) {
        WEmbeddedFramePeer peer = new WEmbeddedFramePeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    WPrintDialogPeer createWPrintDialog(WPrintDialog target) {
        WPrintDialogPeer peer = new WPrintDialogPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    WPageDialogPeer createWPageDialog(WPageDialog target) {
        WPageDialogPeer peer = new WPageDialogPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public TrayIconPeer createTrayIcon(TrayIcon target) {
        WTrayIconPeer peer = new WTrayIconPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    @Override
    public SystemTrayPeer createSystemTray(SystemTray target) {
        return new WSystemTrayPeer(target);
    }

    @Override
    public boolean isTraySupported() {
        return true;
    }

    @Override
    public DataTransferer getDataTransferer() {
        return WDataTransferer.getInstanceImpl();
    }

    @Override
    public KeyboardFocusManagerPeer getKeyboardFocusManagerPeer()
      throws HeadlessException
    {
        return WKeyboardFocusManagerPeer.getInstance();
    }

    private static WMouseInfoPeer wPeer = null;

    @Override
    public synchronized MouseInfoPeer getMouseInfoPeer() {
        if (wPeer == null) {
            wPeer = new WMouseInfoPeer();
        }
        return wPeer;
    }

    private native void setDynamicLayoutNative(boolean b);

    @Override
    public void setDynamicLayout(boolean b) {
        if (b == dynamicLayoutSetting) {
            return;
        }

        dynamicLayoutSetting = b;
        setDynamicLayoutNative(b);
    }

    @Override
    protected boolean isDynamicLayoutSet() {
        return dynamicLayoutSetting;
    }

    /*
     * Called from lazilyLoadDynamicLayoutSupportedProperty because
     * Windows doesn't always send WM_SETTINGCHANGE when it should.
     */
    private native boolean isDynamicLayoutSupportedNative();

    @Override
    public boolean isDynamicLayoutActive() {
        return (isDynamicLayoutSet() && isDynamicLayoutSupported());
    }

    /**
     * Returns {@code true} if this frame state is supported.
     */
    @Override
    public boolean isFrameStateSupported(int state) {
        switch (state) {
          case Frame.NORMAL:
          case Frame.ICONIFIED:
          case Frame.MAXIMIZED_BOTH:
              return true;
          default:
              return false;
        }
    }

    static native ColorModel makeColorModel();
    static ColorModel screenmodel;

    @Override
    public Insets getScreenInsets(final GraphicsConfiguration gc) {
        GraphicsDevice gd = gc.getDevice();
        if (!(gd instanceof Win32GraphicsDevice)) {
            return super.getScreenInsets(gc);
        }
        return getScreenInsets(((Win32GraphicsDevice) gd).getScreen());
    }

    @Override
    public int getScreenResolution() {
        Win32GraphicsEnvironment ge = (Win32GraphicsEnvironment)
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        return ge.getXResolution();
    }

    private native Insets getScreenInsets(int screen);


    @Override
    public FontMetrics getFontMetrics(Font font) {
        // This is an unsupported hack, but left in for a customer.
        // Do not remove.
        FontManager fm = FontManagerFactory.getInstance();
        if (fm instanceof SunFontManager
            && ((SunFontManager) fm).usePlatformFontMetrics()) {
            return WFontMetrics.getFontMetrics(font);
        }
        return super.getFontMetrics(font);
    }

    @Override
    public FontPeer getFontPeer(String name, int style) {
        FontPeer retval = null;
        String lcName = name.toLowerCase();
        if (null != cacheFontPeer) {
            retval = cacheFontPeer.get(lcName + style);
            if (null != retval) {
                return retval;
            }
        }
        retval = new WFontPeer(name, style);
        if (retval != null) {
            if (null == cacheFontPeer) {
                cacheFontPeer = new Hashtable<>(5, 0.9f);
            }
            if (null != cacheFontPeer) {
                cacheFontPeer.put(lcName + style, retval);
            }
        }
        return retval;
    }

    private native void nativeSync();

    @Override
    public void sync() {
        // flush the GDI/DD buffers
        nativeSync();
        // now flush the OGL pipeline (this is a no-op if OGL is not enabled)
        OGLRenderQueue.sync();
        // now flush the D3D pipeline (this is a no-op if D3D is not enabled)
        D3DRenderQueue.sync();
    }

    @Override
    public PrintJob getPrintJob(Frame frame, String doctitle,
                                Properties props) {
        return getPrintJob(frame, doctitle, null, null);
    }

    @Override
    public PrintJob getPrintJob(Frame frame, String doctitle,
                                JobAttributes jobAttributes,
                                PageAttributes pageAttributes)
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

    @Override
    public native void beep();

    @Override
    public boolean getLockingKeyState(int key) {
        if (! (key == KeyEvent.VK_CAPS_LOCK || key == KeyEvent.VK_NUM_LOCK ||
               key == KeyEvent.VK_SCROLL_LOCK || key == KeyEvent.VK_KANA_LOCK)) {
            throw new IllegalArgumentException("invalid key for Toolkit.getLockingKeyState");
        }
        return getLockingKeyStateNative(key);
    }

    private native boolean getLockingKeyStateNative(int key);

    @Override
    public void setLockingKeyState(int key, boolean on) {
        if (! (key == KeyEvent.VK_CAPS_LOCK || key == KeyEvent.VK_NUM_LOCK ||
               key == KeyEvent.VK_SCROLL_LOCK || key == KeyEvent.VK_KANA_LOCK)) {
            throw new IllegalArgumentException("invalid key for Toolkit.setLockingKeyState");
        }
        setLockingKeyStateNative(key, on);
    }

    private native void setLockingKeyStateNative(int key, boolean on);

    @Override
    public Clipboard getSystemClipboard() {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(AWTPermissions.ACCESS_CLIPBOARD_PERMISSION);
        }
        synchronized (this) {
            if (clipboard == null) {
                clipboard = new WClipboard();
            }
        }
        return clipboard;
    }

    @Override
    protected native void loadSystemColors(int[] systemColors);

    public static Object targetToPeer(Object target) {
        return SunToolkit.targetToPeer(target);
    }

    public static void targetDisposedPeer(Object target, Object peer) {
        SunToolkit.targetDisposedPeer(target, peer);
    }

    /**
     * Returns a new input method adapter descriptor for native input methods.
     */
    @Override
    public InputMethodDescriptor getInputMethodAdapterDescriptor() {
        return new WInputMethodDescriptor();
    }

    /**
     * Returns a style map for the input method highlight.
     */
    @Override
    public Map<java.awt.font.TextAttribute,?> mapInputMethodHighlight(
        InputMethodHighlight highlight)
    {
        return WInputMethod.mapInputMethodHighlight(highlight);
    }

    /**
     * Returns whether enableInputMethods should be set to true for peered
     * TextComponent instances on this platform.
     */
    @Override
    public boolean enableInputMethodsForTextComponent() {
        return true;
    }

    /**
     * Returns the default keyboard locale of the underlying operating system
     */
    @Override
    public Locale getDefaultKeyboardLocale() {
        Locale locale = WInputMethod.getNativeLocale();

        if (locale == null) {
            return super.getDefaultKeyboardLocale();
        } else {
            return locale;
        }
    }

    /**
     * Returns a new custom cursor.
     */
    @Override
    public Cursor createCustomCursor(Image cursor, Point hotSpot, String name)
        throws IndexOutOfBoundsException {
        return new WCustomCursor(cursor, hotSpot, name);
    }

    /**
     * Returns the supported cursor size (Win32 only has one).
     */
    @Override
    public Dimension getBestCursorSize(int preferredWidth, int preferredHeight) {
        return new Dimension(WCustomCursor.getCursorWidth(),
                             WCustomCursor.getCursorHeight());
    }

    @Override
    public native int getMaximumCursorColors();

    static void paletteChanged() {
        Object lge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        if (lge instanceof DisplayChangedListener) {
            ((DisplayChangedListener) lge).paletteChanged();
        }
    }

    private static ExecutorService displayChangeExecutor;

    /*
     * Called from Toolkit native code when a WM_DISPLAYCHANGE occurs.
     * Have Win32GraphicsEnvironment execute the display change code on the
     * Event thread.
     */
    public static void displayChanged() {
        final Runnable runnable = () -> {
            Object lge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            if (lge instanceof DisplayChangedListener) {
                ((DisplayChangedListener) lge).displayChanged();
            }
        };
        if (AppContext.getAppContext() != null) {
            // Common case, standalone application
            EventQueue.invokeLater(runnable);
        } else {
            if (displayChangeExecutor == null) {
                // No synchronization, called on the Toolkit thread only
                displayChangeExecutor = Executors.newFixedThreadPool(1, r -> {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                });
            }
            displayChangeExecutor.submit(runnable);
        }
    }

    /**
     * create the peer for a DragSourceContext
     */

    @Override
    public DragSourceContextPeer createDragSourceContextPeer(DragGestureEvent dge) throws InvalidDnDOperationException {
        final LightweightFrame f = SunToolkit.getLightweightFrame(dge.getComponent());
        if (f != null) {
            return f.createDragSourceContextPeer(dge);
        }

        return WDragSourceContextPeer.createDragSourceContextPeer(dge);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends DragGestureRecognizer> T
        createDragGestureRecognizer(Class<T> abstractRecognizerClass,
                                    DragSource ds, Component c, int srcActions,
                                    DragGestureListener dgl)
    {
        final LightweightFrame f = SunToolkit.getLightweightFrame(c);
        if (f != null) {
            return f.createDragGestureRecognizer(abstractRecognizerClass, ds, c, srcActions, dgl);
        }

        if (MouseDragGestureRecognizer.class.equals(abstractRecognizerClass))
            return (T)new WMouseDragGestureRecognizer(ds, c, srcActions, dgl);
        else
            return null;
    }

    /**
     *
     */

    private static final String prefix  = "DnD.Cursor.";
    private static final String postfix = ".32x32";
    private static final String awtPrefix  = "awt.";
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

        if (WDesktopProperties.isWindowsProperty(name) ||
            name.startsWith(awtPrefix) || name.startsWith(dndPrefix))
        {
            synchronized(this) {
                lazilyInitWProps();
                return desktopProperties.get(name);
            }
        }

        return super.lazilyLoadDesktopProperty(name);
    }

    private synchronized void lazilyInitWProps() {
        if (wprops == null) {
            wprops = new WDesktopProperties(this);
            updateProperties(wprops.getProperties());
        }
    }

    /*
     * Called from lazilyLoadDesktopProperty because Windows doesn't
     * always send WM_SETTINGCHANGE when it should.
     */
    private synchronized boolean isDynamicLayoutSupported() {
        boolean nativeDynamic = isDynamicLayoutSupportedNative();
        lazilyInitWProps();
        Boolean prop = (Boolean) desktopProperties.get("awt.dynamicLayoutSupported");

        if (log.isLoggable(PlatformLogger.Level.FINER)) {
            log.finer("In WTK.isDynamicLayoutSupported()" +
                      "   nativeDynamic == " + nativeDynamic +
                      "   wprops.dynamic == " + prop);
        }

        if ((prop == null) || (nativeDynamic != prop.booleanValue())) {
            // We missed the WM_SETTINGCHANGE, so we pretend
            // we just got one - fire the propertyChange, etc.
            windowsSettingChange();
            return nativeDynamic;
        }

        return prop.booleanValue();
    }

    /*
     * Called from native toolkit code when WM_SETTINGCHANGE message received
     * Also called from lazilyLoadDynamicLayoutSupportedProperty because
     * Windows doesn't always send WM_SETTINGCHANGE when it should.
     */
    private void windowsSettingChange() {
        // JDK-8039383: Have to update the value of XPSTYLE_THEME_ACTIVE property
        // as soon as possible to prevent NPE and other errors because theme data
        // has become unavailable.
        final Map<String, Object> props = getWProps();
        if (props == null) {
            // props has not been initialized, so we have nothing to update
            return;
        }

        updateXPStyleEnabled(props.get(XPSTYLE_THEME_ACTIVE));

        if (AppContext.getAppContext() == null) {
            // We cannot post the update to any EventQueue. Listeners will
            // be called on EDTs by DesktopPropertyChangeSupport
            updateProperties(props);
        } else {
            // Cannot update on Toolkit thread.
            // DesktopPropertyChangeSupport will call listeners on Toolkit
            // thread if it has AppContext (standalone mode)
            EventQueue.invokeLater(() -> updateProperties(props));
        }
    }

    private synchronized void updateProperties(final Map<String, Object> props) {
        if (null == props) {
            return;
        }

        updateXPStyleEnabled(props.get(XPSTYLE_THEME_ACTIVE));

        for (String propName : props.keySet()) {
            Object val = props.get(propName);
            if (log.isLoggable(PlatformLogger.Level.FINER)) {
                log.finer("changed " + propName + " to " + val);
            }
            setDesktopProperty(propName, val);
        }
    }

    private synchronized Map<String, Object> getWProps() {
        return (wprops != null) ? wprops.getProperties() : null;
    }

    private void updateXPStyleEnabled(final Object dskProp) {
        ThemeReader.xpStyleEnabled = Boolean.TRUE.equals(dskProp);
    }

    @Override
    public synchronized void addPropertyChangeListener(String name, PropertyChangeListener pcl) {
        if (name == null) {
            // See JavaDoc for the Toolkit.addPropertyChangeListener() method
            return;
        }
        if ( WDesktopProperties.isWindowsProperty(name)
             || name.startsWith(awtPrefix)
             || name.startsWith(dndPrefix))
        {
            // someone is interested in Windows-specific desktop properties
            // we should initialize wprops
            lazilyInitWProps();
        }
        super.addPropertyChangeListener(name, pcl);
    }

    /*
     * initialize only static props here and do not try to initialize props which depends on wprops,
     * this should be done in lazilyLoadDesktopProperty() only.
     */
    @Override
    protected synchronized void initializeDesktopProperties() {
        desktopProperties.put("DnD.Autoscroll.initialDelay",
                              Integer.valueOf(50));
        desktopProperties.put("DnD.Autoscroll.interval",
                              Integer.valueOf(50));
        desktopProperties.put("DnD.isDragImageSupported",
                              Boolean.TRUE);
        desktopProperties.put("Shell.shellFolderManager",
                              "sun.awt.shell.Win32ShellFolderManager2");
    }

    /*
     * This returns the value for the desktop property "awt.font.desktophints"
     * This requires that the Windows properties have already been gathered.
     */
    @Override
    protected synchronized RenderingHints getDesktopAAHints() {
        if (wprops == null) {
            return null;
        } else {
            return wprops.getDesktopAAHints();
        }
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

    public static WToolkit getWToolkit() {
        WToolkit toolkit = (WToolkit)Toolkit.getDefaultToolkit();
        return toolkit;
    }

    /**
     * There are two reasons why we don't use buffer per window when
     * Vista's DWM (aka Aero) is enabled:
     * - since with DWM all windows are already double-buffered, the application
     *   doesn't get expose events so we don't get to use our true back-buffer,
     *   wasting memory and performance (this is valid for both d3d and gdi
     *   pipelines)
     * - in some cases with buffer per window enabled it is possible for the
     *   paint manager to redirect rendering to the screen for some operations
     *   (like copyArea), and since bpw uses its own BufferStrategy the
     *   d3d onscreen rendering support is disabled and rendering goes through
     *   GDI. This doesn't work well with Vista's DWM since one
     *   can not perform GDI and D3D operations on the same surface
     *   (see 6630702 for more info)
     *
     * Note: even though DWM composition state can change during the lifetime
     * of the application it is a rare event, and it is more often that it
     * is temporarily disabled (because of some app) than it is getting
     * permanently enabled so we can live with this approach without the
     * complexity of dwm state listeners and such. This can be revisited if
     * proved otherwise.
     */
    @Override
    public boolean useBufferPerWindow() {
        return !Win32GraphicsEnvironment.isDWMCompositionEnabled();
    }

    @Override
    public void grab(Window w) {
        final Object peer = AWTAccessor.getComponentAccessor().getPeer(w);
        if (peer != null) {
            ((WWindowPeer) peer).grab();
        }
    }

    @Override
    public void ungrab(Window w) {
        final Object peer = AWTAccessor.getComponentAccessor().getPeer(w);
        if (peer != null) {
            ((WWindowPeer) peer).ungrab();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // The following code is used for support of automatic showing of the touch
    // keyboard for text components and is accessed only from EDT.
    ///////////////////////////////////////////////////////////////////////////
    private static final WeakReference<Component> NULL_COMPONENT_WR =
        new WeakReference<>(null);
    private volatile WeakReference<Component> compOnTouchDownEvent =
        NULL_COMPONENT_WR;
    private volatile WeakReference<Component> compOnMousePressedEvent =
        NULL_COMPONENT_WR;

    private boolean isComponentValidForTouchKeyboard(Component comp) {
        if ((comp != null) && comp.isEnabled() && comp.isFocusable() &&
            (((comp instanceof TextComponent) &&
                    ((TextComponent) comp).isEditable()) ||
                ((comp instanceof JTextComponent) &&
                    ((JTextComponent) comp).isEditable()))) {
            return true;
        }
        return false;
    }

    @Override
    public void showOrHideTouchKeyboard(Component comp, AWTEvent e) {
        if (!(comp instanceof TextComponent) &&
            !(comp instanceof JTextComponent)) {
            return;
        }

        if ((e instanceof MouseEvent) && isComponentValidForTouchKeyboard(comp)) {
            MouseEvent me = (MouseEvent) e;
            if (me.getID() == MouseEvent.MOUSE_PRESSED) {
                if (AWTAccessor.getMouseEventAccessor().isCausedByTouchEvent(me)) {
                    compOnTouchDownEvent = new WeakReference<>(comp);
                } else {
                    compOnMousePressedEvent = new WeakReference<>(comp);
                }
            } else if (me.getID() == MouseEvent.MOUSE_RELEASED) {
                if (AWTAccessor.getMouseEventAccessor().isCausedByTouchEvent(me)) {
                    if (compOnTouchDownEvent.get() == comp) {
                        showTouchKeyboard(true);
                    }
                    compOnTouchDownEvent = NULL_COMPONENT_WR;
                } else {
                    if (compOnMousePressedEvent.get() == comp) {
                        showTouchKeyboard(false);
                    }
                    compOnMousePressedEvent = NULL_COMPONENT_WR;
                }
            }
        } else if (e instanceof FocusEvent) {
            FocusEvent fe = (FocusEvent) e;
            if (fe.getID() == FocusEvent.FOCUS_LOST) {
                // Hide the touch keyboard, if not a text component gains focus.
                if (!isComponentValidForTouchKeyboard(fe.getOppositeComponent())) {
                    hideTouchKeyboard();
                }
            }
        }
    }

    private native void showTouchKeyboard(boolean causedByTouchEvent);
    private native void hideTouchKeyboard();
    ///////////////////////////////////////////////////////////////////////////
    // End of the touch keyboard related code.
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public native boolean syncNativeQueue(long timeout);

    @Override
    public boolean isDesktopSupported() {
        return true;
    }

    @Override
    public DesktopPeer createDesktopPeer(Desktop target) {
        return new WDesktopPeer();
    }

    @Override
    public boolean isTaskbarSupported() {
        return WTaskbarPeer.isTaskbarSupported();
    }

    @Override
    public TaskbarPeer createTaskbarPeer(Taskbar target) {
        return new WTaskbarPeer();
    }

    private static native void setExtraMouseButtonsEnabledNative(boolean enable);

    @Override
    public boolean areExtraMouseButtonsEnabled() throws HeadlessException {
        return areExtraMouseButtonsEnabled;
    }

    private synchronized native int getNumberOfButtonsImpl();

    @Override
    public int getNumberOfButtons(){
        if (numberOfButtons == 0) {
            numberOfButtons = getNumberOfButtonsImpl();
        }
        return (numberOfButtons > MAX_BUTTONS_SUPPORTED)? MAX_BUTTONS_SUPPORTED : numberOfButtons;
    }

    @Override
    public boolean isWindowOpacitySupported() {
        // supported in Win2K and later
        return true;
    }

    @Override
    public boolean isWindowShapingSupported() {
        return true;
    }

    @Override
    public boolean isWindowTranslucencySupported() {
        // supported in Win2K and later
        return true;
    }

    @Override
    public boolean isTranslucencyCapable(GraphicsConfiguration gc) {
        //XXX: worth checking if 8-bit? Anyway, it doesn't hurt.
        return true;
    }

    // On MS Windows one must use the peer.updateWindow() to implement
    // non-opaque windows.
    @Override
    public boolean needUpdateWindow() {
        return true;
    }
}
