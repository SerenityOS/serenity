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

import java.awt.Color;
import java.awt.Component;
import java.awt.DefaultKeyboardFocusManager;
import java.awt.Dialog;
import java.awt.Dialog.ModalityType;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.Insets;
import java.awt.MenuBar;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.FocusEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JRootPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;

import com.apple.laf.ClientPropertyApplicator;
import com.apple.laf.ClientPropertyApplicator.Property;
import sun.awt.AWTAccessor;
import sun.awt.AWTAccessor.ComponentAccessor;
import sun.awt.AWTAccessor.WindowAccessor;
import sun.java2d.SurfaceData;
import sun.lwawt.LWLightweightFramePeer;
import sun.lwawt.LWToolkit;
import sun.lwawt.LWWindowPeer;
import sun.lwawt.LWWindowPeer.PeerType;
import sun.lwawt.PlatformWindow;
import sun.security.action.GetPropertyAction;
import sun.util.logging.PlatformLogger;

public class CPlatformWindow extends CFRetainedResource implements PlatformWindow {
    private native long nativeCreateNSWindow(long nsViewPtr,long ownerPtr, long styleBits, double x, double y, double w, double h);
    private static native void nativeSetNSWindowStyleBits(long nsWindowPtr, int mask, int data);
    private static native void nativeSetNSWindowMenuBar(long nsWindowPtr, long menuBarPtr);
    private static native Insets nativeGetNSWindowInsets(long nsWindowPtr);
    private static native void nativeSetNSWindowBounds(long nsWindowPtr, double x, double y, double w, double h);
    private static native void nativeSetNSWindowLocationByPlatform(long nsWindowPtr);
    private static native void nativeSetNSWindowStandardFrame(long nsWindowPtr,
            double x, double y, double w, double h);
    private static native void nativeSetNSWindowMinMax(long nsWindowPtr, double minW, double minH, double maxW, double maxH);
    private static native void nativePushNSWindowToBack(long nsWindowPtr);
    private static native void nativePushNSWindowToFront(long nsWindowPtr);
    private static native void nativeSetNSWindowTitle(long nsWindowPtr, String title);
    private static native void nativeRevalidateNSWindowShadow(long nsWindowPtr);
    private static native void nativeSetNSWindowMinimizedIcon(long nsWindowPtr, long nsImage);
    private static native void nativeSetNSWindowRepresentedFilename(long nsWindowPtr, String representedFilename);
    private static native void nativeSetAllowAutomaticTabbingProperty(boolean allowAutomaticWindowTabbing);
    private static native void nativeSetEnabled(long nsWindowPtr, boolean isEnabled);
    private static native void nativeSynthesizeMouseEnteredExitedEvents();
    private static native void nativeSynthesizeMouseEnteredExitedEvents(long nsWindowPtr, int eventType);
    private static native void nativeDispose(long nsWindowPtr);
    private static native void nativeEnterFullScreenMode(long nsWindowPtr);
    private static native void nativeExitFullScreenMode(long nsWindowPtr);
    static native CPlatformWindow nativeGetTopmostPlatformWindowUnderMouse();

    // Loger to report issues happened during execution but that do not affect functionality
    private static final PlatformLogger logger = PlatformLogger.getLogger("sun.lwawt.macosx.CPlatformWindow");
    private static final PlatformLogger focusLogger = PlatformLogger.getLogger("sun.lwawt.macosx.focus.CPlatformWindow");

    // for client properties
    public static final String WINDOW_BRUSH_METAL_LOOK = "apple.awt.brushMetalLook";
    public static final String WINDOW_DRAGGABLE_BACKGROUND = "apple.awt.draggableWindowBackground";

    public static final String WINDOW_ALPHA = "Window.alpha";
    public static final String WINDOW_SHADOW = "Window.shadow";

    public static final String WINDOW_STYLE = "Window.style";
    public static final String WINDOW_SHADOW_REVALIDATE_NOW = "apple.awt.windowShadow.revalidateNow";

    public static final String WINDOW_DOCUMENT_MODIFIED = "Window.documentModified";
    public static final String WINDOW_DOCUMENT_FILE = "Window.documentFile";

    public static final String WINDOW_CLOSEABLE = "Window.closeable";
    public static final String WINDOW_MINIMIZABLE = "Window.minimizable";
    public static final String WINDOW_ZOOMABLE = "Window.zoomable";
    public static final String WINDOW_HIDES_ON_DEACTIVATE="Window.hidesOnDeactivate";

    public static final String WINDOW_DOC_MODAL_SHEET = "apple.awt.documentModalSheet";
    public static final String WINDOW_FADE_DELEGATE = "apple.awt._windowFadeDelegate";
    public static final String WINDOW_FADE_IN = "apple.awt._windowFadeIn";
    public static final String WINDOW_FADE_OUT = "apple.awt._windowFadeOut";
    public static final String WINDOW_FULLSCREENABLE = "apple.awt.fullscreenable";
    public static final String WINDOW_FULL_CONTENT = "apple.awt.fullWindowContent";
    public static final String WINDOW_TRANSPARENT_TITLE_BAR = "apple.awt.transparentTitleBar";
    public static final String WINDOW_TITLE_VISIBLE = "apple.awt.windowTitleVisible";

    // This system property is named as jdk.* because it is not specific to AWT
    // and it is also used in JavaFX
    @SuppressWarnings("removal")
    public static final String MAC_OS_TABBED_WINDOW = AccessController.doPrivileged(
            new GetPropertyAction("jdk.allowMacOSTabbedWindows"));

    // Yeah, I know. But it's easier to deal with ints from JNI
    static final int MODELESS = 0;
    static final int DOCUMENT_MODAL = 1;
    static final int APPLICATION_MODAL = 2;
    static final int TOOLKIT_MODAL = 3;

    // window style bits
    static final int _RESERVED_FOR_DATA = 1 << 0;

    // corresponds to native style mask bits
    static final int DECORATED = 1 << 1;
    static final int TEXTURED = 1 << 2;
    static final int UNIFIED = 1 << 3;
    static final int UTILITY = 1 << 4;
    static final int HUD = 1 << 5;
    static final int SHEET = 1 << 6;

    static final int CLOSEABLE = 1 << 7;
    static final int MINIMIZABLE = 1 << 8;

    static final int RESIZABLE = 1 << 9; // both a style bit and prop bit
    static final int NONACTIVATING = 1 << 24;
    static final int IS_DIALOG = 1 << 25;
    static final int IS_MODAL = 1 << 26;
    static final int IS_POPUP = 1 << 27;

    static final int FULL_WINDOW_CONTENT = 1 << 14;

    static final int _STYLE_PROP_BITMASK = DECORATED | TEXTURED | UNIFIED | UTILITY | HUD | SHEET | CLOSEABLE
                                             | MINIMIZABLE | RESIZABLE | FULL_WINDOW_CONTENT;

    // corresponds to method-based properties
    static final int HAS_SHADOW = 1 << 10;
    static final int ZOOMABLE = 1 << 11;

    static final int ALWAYS_ON_TOP = 1 << 15;
    static final int HIDES_ON_DEACTIVATE = 1 << 17;
    static final int DRAGGABLE_BACKGROUND = 1 << 19;
    static final int DOCUMENT_MODIFIED = 1 << 21;
    static final int FULLSCREENABLE = 1 << 23;
    static final int TRANSPARENT_TITLE_BAR = 1 << 18;
    static final int TITLE_VISIBLE = 1 << 25;

    static final int _METHOD_PROP_BITMASK = RESIZABLE | HAS_SHADOW | ZOOMABLE | ALWAYS_ON_TOP | HIDES_ON_DEACTIVATE
                                              | DRAGGABLE_BACKGROUND | DOCUMENT_MODIFIED | FULLSCREENABLE
                                              | TRANSPARENT_TITLE_BAR | TITLE_VISIBLE;

    // corresponds to callback-based properties
    static final int SHOULD_BECOME_KEY = 1 << 12;
    static final int SHOULD_BECOME_MAIN = 1 << 13;
    static final int MODAL_EXCLUDED = 1 << 16;

    static final int _CALLBACK_PROP_BITMASK = SHOULD_BECOME_KEY | SHOULD_BECOME_MAIN | MODAL_EXCLUDED;

    static int SET(final int bits, final int mask, final boolean value) {
        if (value) return (bits | mask);
        return bits & ~mask;
    }

    static boolean IS(final int bits, final int mask) {
        return (bits & mask) != 0;
    }

    static {
        nativeSetAllowAutomaticTabbingProperty(Boolean.parseBoolean(MAC_OS_TABBED_WINDOW));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static ClientPropertyApplicator<JRootPane, CPlatformWindow> CLIENT_PROPERTY_APPLICATOR = new ClientPropertyApplicator<JRootPane, CPlatformWindow>(new Property[] {
        new Property<CPlatformWindow>(WINDOW_DOCUMENT_MODIFIED) { public void applyProperty(final CPlatformWindow c, final Object value) {
            c.setStyleBits(DOCUMENT_MODIFIED, value == null ? false : Boolean.parseBoolean(value.toString()));
        }},
        new Property<CPlatformWindow>(WINDOW_BRUSH_METAL_LOOK) { public void applyProperty(final CPlatformWindow c, final Object value) {
            c.setStyleBits(TEXTURED, Boolean.parseBoolean(value.toString()));
        }},
        new Property<CPlatformWindow>(WINDOW_ALPHA) { public void applyProperty(final CPlatformWindow c, final Object value) {
            c.target.setOpacity(value == null ? 1.0f : Float.parseFloat(value.toString()));
        }},
        new Property<CPlatformWindow>(WINDOW_SHADOW) { public void applyProperty(final CPlatformWindow c, final Object value) {
            c.setStyleBits(HAS_SHADOW, value == null ? true : Boolean.parseBoolean(value.toString()));
        }},
        new Property<CPlatformWindow>(WINDOW_MINIMIZABLE) { public void applyProperty(final CPlatformWindow c, final Object value) {
            c.setStyleBits(MINIMIZABLE, Boolean.parseBoolean(value.toString()));
        }},
        new Property<CPlatformWindow>(WINDOW_CLOSEABLE) { public void applyProperty(final CPlatformWindow c, final Object value) {
            c.setStyleBits(CLOSEABLE, Boolean.parseBoolean(value.toString()));
        }},
        new Property<CPlatformWindow>(WINDOW_ZOOMABLE) { public void applyProperty(final CPlatformWindow c, final Object value) {
            boolean zoomable = Boolean.parseBoolean(value.toString());
            if (c.target instanceof RootPaneContainer
                    && c.getPeer().getPeerType() == PeerType.FRAME) {
                if (c.isInFullScreen && !zoomable) {
                    c.toggleFullScreen();
                }
            }
            c.setStyleBits(ZOOMABLE, zoomable);
        }},
        new Property<CPlatformWindow>(WINDOW_FULLSCREENABLE) { public void applyProperty(final CPlatformWindow c, final Object value) {
            boolean fullscrenable = Boolean.parseBoolean(value.toString());
            if (c.target instanceof RootPaneContainer
                    && c.getPeer().getPeerType() == PeerType.FRAME) {
                if (c.isInFullScreen && !fullscrenable) {
                    c.toggleFullScreen();
                }
            }
            c.setStyleBits(FULLSCREENABLE, fullscrenable);
        }},
        new Property<CPlatformWindow>(WINDOW_SHADOW_REVALIDATE_NOW) { public void applyProperty(final CPlatformWindow c, final Object value) {
            c.execute(ptr -> nativeRevalidateNSWindowShadow(ptr));
        }},
        new Property<CPlatformWindow>(WINDOW_DOCUMENT_FILE) { public void applyProperty(final CPlatformWindow c, final Object value) {
            if (value == null || !(value instanceof java.io.File)) {
                c.execute(ptr->nativeSetNSWindowRepresentedFilename(ptr, null));
                return;
            }

            final String filename = ((java.io.File)value).getAbsolutePath();
            c.execute(ptr->nativeSetNSWindowRepresentedFilename(ptr, filename));
        }},
        new Property<CPlatformWindow>(WINDOW_FULL_CONTENT) {
            public void applyProperty(final CPlatformWindow c, final Object value) {
                boolean isFullWindowContent = Boolean.parseBoolean(value.toString());
                c.setStyleBits(FULL_WINDOW_CONTENT, isFullWindowContent);
            }
        },
        new Property<CPlatformWindow>(WINDOW_TRANSPARENT_TITLE_BAR) {
            public void applyProperty(final CPlatformWindow c, final Object value) {
                boolean isTransparentTitleBar = Boolean.parseBoolean(value.toString());
                c.setStyleBits(TRANSPARENT_TITLE_BAR, isTransparentTitleBar);
            }
        },
        new Property<CPlatformWindow>(WINDOW_TITLE_VISIBLE) {
            public void applyProperty(final CPlatformWindow c, final Object value) {
                c.setStyleBits(TITLE_VISIBLE, value == null ? true : Boolean.parseBoolean(value.toString()));
            }
        }
    }) {
        @SuppressWarnings("deprecation")
        public CPlatformWindow convertJComponentToTarget(final JRootPane p) {
            Component root = SwingUtilities.getRoot(p);
            final ComponentAccessor acc = AWTAccessor.getComponentAccessor();
            if (root == null || acc.getPeer(root) == null) return null;
            return (CPlatformWindow)((LWWindowPeer)acc.getPeer(root)).getPlatformWindow();
        }
    };
    private final Comparator<Window> siblingsComparator = (w1, w2) -> {
        if (w1 == w2) {
            return 0;
        }
        ComponentAccessor componentAccessor = AWTAccessor.getComponentAccessor();
        Object p1 = componentAccessor.getPeer(w1);
        Object p2 = componentAccessor.getPeer(w2);
        long time1 = 0;
        if (p1 instanceof LWWindowPeer) {
            time1 = ((CPlatformWindow) (((LWWindowPeer) p1).getPlatformWindow())).lastBecomeMainTime;
        }
        long time2 = 0;
        if (p2 instanceof LWWindowPeer) {
            time2 = ((CPlatformWindow) (((LWWindowPeer) p2).getPlatformWindow())).lastBecomeMainTime;
        }
        return Long.compare(time1, time2);
    };

    // Bounds of the native widget but in the Java coordinate system.
    // In order to keep it up-to-date we will update them on
    // 1) setting native bounds via nativeSetBounds() call
    // 2) getting notification from the native level via deliverMoveResizeEvent()
    private Rectangle nativeBounds = new Rectangle(0, 0, 0, 0);
    private volatile boolean isFullScreenMode;
    private boolean isFullScreenAnimationOn;

    private volatile boolean isInFullScreen;
    private volatile boolean isIconifyAnimationActive;
    private volatile boolean isZoomed;

    private Window target;
    private LWWindowPeer peer;
    protected CPlatformView contentView;
    protected CPlatformWindow owner;
    protected boolean visible = false; // visibility status from native perspective
    private boolean undecorated; // initialized in getInitialStyleBits()
    private Rectangle normalBounds = null; // not-null only for undecorated maximized windows
    private CPlatformResponder responder;
    private long lastBecomeMainTime; // this is necessary to preserve right siblings order

    public CPlatformWindow() {
        super(0, true);
    }

    /*
     * Delegate initialization (create native window and all the
     * related resources).
     */
    @Override // PlatformWindow
    public void initialize(Window _target, LWWindowPeer _peer, PlatformWindow _owner) {
        initializeBase(_target, _peer, _owner);

        final int styleBits = getInitialStyleBits();

        responder = createPlatformResponder();
        contentView.initialize(peer, responder);

        Rectangle bounds;
        if (!IS(DECORATED, styleBits)) {
            // For undecorated frames the move/resize event does not come if the frame is centered on the screen
            // so we need to set a stub location to force an initial move/resize. Real bounds would be set later.
            bounds = new Rectangle(0, 0, 1, 1);
        } else {
            bounds = _peer.constrainBounds(_target.getBounds());
        }
        AtomicLong ref = new AtomicLong();
        contentView.execute(viewPtr -> {
            boolean hasOwnerPtr = false;

            if (owner != null) {
                hasOwnerPtr = 0L != owner.executeGet(ownerPtr -> {
                    ref.set(nativeCreateNSWindow(viewPtr, ownerPtr, styleBits,
                                                    bounds.x, bounds.y,
                                                    bounds.width, bounds.height));
                    return 1;
                });
            }

            if (!hasOwnerPtr) {
                ref.set(nativeCreateNSWindow(viewPtr, 0,
                                             styleBits, bounds.x, bounds.y,
                                             bounds.width, bounds.height));
            }
        });
        setPtr(ref.get());

        if (target instanceof javax.swing.RootPaneContainer) {
            final javax.swing.JRootPane rootpane = ((javax.swing.RootPaneContainer)target).getRootPane();
            if (rootpane != null) rootpane.addPropertyChangeListener("ancestor", new PropertyChangeListener() {
                public void propertyChange(final PropertyChangeEvent evt) {
                    CLIENT_PROPERTY_APPLICATOR.attachAndApplyClientProperties(rootpane);
                    rootpane.removePropertyChangeListener("ancestor", this);
                }
            });
        }
    }

    void initializeBase(Window target, LWWindowPeer peer, PlatformWindow owner) {
        this.peer = peer;
        this.target = target;
        if (owner instanceof CPlatformWindow) {
            this.owner = (CPlatformWindow)owner;
        }
        contentView = createContentView();
    }

    protected CPlatformResponder createPlatformResponder() {
        return new CPlatformResponder(peer, false);
    }

    CPlatformView createContentView() {
        return new CPlatformView();
    }

    protected int getInitialStyleBits() {
        // defaults style bits
        int styleBits = DECORATED | HAS_SHADOW | CLOSEABLE | MINIMIZABLE | ZOOMABLE | RESIZABLE | TITLE_VISIBLE;

        if (isNativelyFocusableWindow()) {
            styleBits = SET(styleBits, SHOULD_BECOME_KEY, true);
            styleBits = SET(styleBits, SHOULD_BECOME_MAIN, true);
        }

        final boolean isFrame = (target instanceof Frame);
        final boolean isDialog = (target instanceof Dialog);
        final boolean isPopup = (target.getType() == Window.Type.POPUP);
        if (isDialog) {
            styleBits = SET(styleBits, MINIMIZABLE, false);
        }

        // Either java.awt.Frame or java.awt.Dialog can be undecorated, however java.awt.Window always is undecorated.
        {
            this.undecorated = isFrame ? ((Frame)target).isUndecorated() : (isDialog ? ((Dialog)target).isUndecorated() : true);
            if (this.undecorated) styleBits = SET(styleBits, DECORATED, false);
        }

        // Either java.awt.Frame or java.awt.Dialog can be resizable, however java.awt.Window is never resizable
        {
            final boolean resizable = isFrame ? ((Frame)target).isResizable() : (isDialog ? ((Dialog)target).isResizable() : false);
            styleBits = SET(styleBits, RESIZABLE, resizable);
            if (!resizable) {
                styleBits = SET(styleBits, ZOOMABLE, false);
            }
        }

        if (target.isAlwaysOnTop()) {
            styleBits = SET(styleBits, ALWAYS_ON_TOP, true);
        }

        if (target.getModalExclusionType() == Dialog.ModalExclusionType.APPLICATION_EXCLUDE) {
            styleBits = SET(styleBits, MODAL_EXCLUDED, true);
        }

        // If the target is a dialog, popup or tooltip we want it to ignore the brushed metal look.
        if (isPopup) {
            styleBits = SET(styleBits, TEXTURED, false);
            // Popups in applets don't activate applet's process
            styleBits = SET(styleBits, NONACTIVATING, true);
            styleBits = SET(styleBits, IS_POPUP, true);
        }

        if (Window.Type.UTILITY.equals(target.getType())) {
            styleBits = SET(styleBits, UTILITY, true);
        }

        if (target instanceof javax.swing.RootPaneContainer) {
            javax.swing.JRootPane rootpane = ((javax.swing.RootPaneContainer)target).getRootPane();
            Object prop = null;

            prop = rootpane.getClientProperty(WINDOW_BRUSH_METAL_LOOK);
            if (prop != null) {
                styleBits = SET(styleBits, TEXTURED, Boolean.parseBoolean(prop.toString()));
            }

            if (isDialog && ((Dialog)target).getModalityType() == ModalityType.DOCUMENT_MODAL) {
                prop = rootpane.getClientProperty(WINDOW_DOC_MODAL_SHEET);
                if (prop != null) {
                    styleBits = SET(styleBits, SHEET, Boolean.parseBoolean(prop.toString()));
                }
            }

            prop = rootpane.getClientProperty(WINDOW_STYLE);
            if (prop != null) {
                if ("small".equals(prop))  {
                    styleBits = SET(styleBits, UTILITY, true);
                    if (target.isAlwaysOnTop() && rootpane.getClientProperty(WINDOW_HIDES_ON_DEACTIVATE) == null) {
                        styleBits = SET(styleBits, HIDES_ON_DEACTIVATE, true);
                    }
                }
                if ("textured".equals(prop)) styleBits = SET(styleBits, TEXTURED, true);
                if ("unified".equals(prop)) styleBits = SET(styleBits, UNIFIED, true);
                if ("hud".equals(prop)) styleBits = SET(styleBits, HUD, true);
            }

            prop = rootpane.getClientProperty(WINDOW_HIDES_ON_DEACTIVATE);
            if (prop != null) {
                styleBits = SET(styleBits, HIDES_ON_DEACTIVATE, Boolean.parseBoolean(prop.toString()));
            }

            prop = rootpane.getClientProperty(WINDOW_CLOSEABLE);
            if (prop != null) {
                styleBits = SET(styleBits, CLOSEABLE, Boolean.parseBoolean(prop.toString()));
            }

            prop = rootpane.getClientProperty(WINDOW_MINIMIZABLE);
            if (prop != null) {
                styleBits = SET(styleBits, MINIMIZABLE, Boolean.parseBoolean(prop.toString()));
            }

            prop = rootpane.getClientProperty(WINDOW_ZOOMABLE);
            if (prop != null) {
                styleBits = SET(styleBits, ZOOMABLE, Boolean.parseBoolean(prop.toString()));
            }

            prop = rootpane.getClientProperty(WINDOW_FULLSCREENABLE);
            if (prop != null) {
                styleBits = SET(styleBits, FULLSCREENABLE, Boolean.parseBoolean(prop.toString()));
            }

            prop = rootpane.getClientProperty(WINDOW_SHADOW);
            if (prop != null) {
                styleBits = SET(styleBits, HAS_SHADOW, Boolean.parseBoolean(prop.toString()));
            }

            prop = rootpane.getClientProperty(WINDOW_DRAGGABLE_BACKGROUND);
            if (prop != null) {
                styleBits = SET(styleBits, DRAGGABLE_BACKGROUND, Boolean.parseBoolean(prop.toString()));
            }

            prop = rootpane.getClientProperty(WINDOW_FULL_CONTENT);
            if (prop != null) {
                styleBits = SET(styleBits, FULL_WINDOW_CONTENT, Boolean.parseBoolean(prop.toString()));
            }

            prop = rootpane.getClientProperty(WINDOW_TRANSPARENT_TITLE_BAR);
            if (prop != null) {
                styleBits = SET(styleBits, TRANSPARENT_TITLE_BAR, Boolean.parseBoolean(prop.toString()));
            }

            prop = rootpane.getClientProperty(WINDOW_TITLE_VISIBLE);
            if (prop != null) {
                styleBits = SET(styleBits, TITLE_VISIBLE, Boolean.parseBoolean(prop.toString()));
            }
        }

        if (isDialog) {
            styleBits = SET(styleBits, IS_DIALOG, true);
            if (((Dialog) target).isModal()) {
                styleBits = SET(styleBits, IS_MODAL, true);
            }
        }

        peer.setTextured(IS(TEXTURED, styleBits));

        return styleBits;
    }

    // this is the counter-point to -[CWindow _nativeSetStyleBit:]
    private void setStyleBits(final int mask, final boolean value) {
        execute(ptr -> nativeSetNSWindowStyleBits(ptr, mask, value ? mask : 0));
    }

    private native void _toggleFullScreenMode(final long model);

    public void toggleFullScreen() {
        execute(this::_toggleFullScreenMode);
    }

    @Override // PlatformWindow
    public void setMenuBar(MenuBar mb) {
        CMenuBar mbPeer = (CMenuBar)LWToolkit.targetToPeer(mb);
        execute(nsWindowPtr->{
            if (mbPeer != null) {
                mbPeer.execute(ptr -> nativeSetNSWindowMenuBar(nsWindowPtr, ptr));
            } else {
                nativeSetNSWindowMenuBar(nsWindowPtr, 0);
            }
        });
    }

    @Override // PlatformWindow
    public void dispose() {
        contentView.dispose();
        execute(CPlatformWindow::nativeDispose);
        CPlatformWindow.super.dispose();
    }

    @Override // PlatformWindow
    public FontMetrics getFontMetrics(Font f) {
        // TODO: not implemented
        (new RuntimeException("unimplemented")).printStackTrace();
        return null;
    }

    @Override // PlatformWindow
    public Insets getInsets() {
        AtomicReference<Insets> ref = new AtomicReference<>();
        execute(ptr -> {
            ref.set(nativeGetNSWindowInsets(ptr));
        });
        return ref.get() != null ? ref.get() : new Insets(0, 0, 0, 0);
    }

    @Override // PlatformWindow
    public Point getLocationOnScreen() {
        return new Point(nativeBounds.x, nativeBounds.y);
    }

    @Override
    public GraphicsDevice getGraphicsDevice() {
        return contentView.getGraphicsDevice();
    }

    @Override // PlatformWindow
    public SurfaceData getScreenSurface() {
        // TODO: not implemented
        return null;
    }

    @Override // PlatformWindow
    public SurfaceData replaceSurfaceData() {
        return contentView.replaceSurfaceData();
    }

    @Override // PlatformWindow
    public void setBounds(int x, int y, int w, int h) {
        execute(ptr -> nativeSetNSWindowBounds(ptr, x, y, w, h));
    }

    public void setMaximizedBounds(int x, int y, int w, int h) {
        execute(ptr -> nativeSetNSWindowStandardFrame(ptr, x, y, w, h));
    }

    private boolean isMaximized() {
        return undecorated ? this.normalBounds != null
                : isZoomed;
    }

    private void maximize() {
        if (peer == null || isMaximized()) {
            return;
        }
        if (!undecorated) {
            execute(CWrapper.NSWindow::zoom);
        } else {
            deliverZoom(true);

            // We need an up to date size of the peer, so we flush the native events
            // to be sure that there are no setBounds requests in the queue.
            LWCToolkit.flushNativeSelectors();
            this.normalBounds = peer.getBounds();
            Rectangle maximizedBounds = peer.getMaximizedBounds();
            setBounds(maximizedBounds.x, maximizedBounds.y,
                    maximizedBounds.width, maximizedBounds.height);
        }
    }

    private void unmaximize() {
        if (!isMaximized()) {
            return;
        }
        if (!undecorated) {
            execute(CWrapper.NSWindow::zoom);
        } else {
            deliverZoom(false);

            Rectangle toBounds = this.normalBounds;
            this.normalBounds = null;
            setBounds(toBounds.x, toBounds.y, toBounds.width, toBounds.height);
        }
    }

    public boolean isVisible() {
        return this.visible;
    }

    @Override // PlatformWindow
    public void setVisible(boolean visible) {
        // Configure stuff
        updateIconImages();
        updateFocusabilityForAutoRequestFocus(false);

        boolean wasMaximized = isMaximized();

        if (visible && target.isLocationByPlatform()) {
            execute(CPlatformWindow::nativeSetNSWindowLocationByPlatform);
        }

        // Actually show or hide the window
        LWWindowPeer blocker = (peer == null)? null : peer.getBlocker();
        if (blocker == null || !visible) {
            // If it ain't blocked, or is being hidden, go regular way
            if (visible) {
                contentView.execute(viewPtr -> {
                    execute(ptr -> CWrapper.NSWindow.makeFirstResponder(ptr,
                                                                        viewPtr));
                });

                boolean isPopup = (target.getType() == Window.Type.POPUP);
                execute(ptr -> {
                    if (isPopup) {
                        // Popups in applets don't activate applet's process
                        CWrapper.NSWindow.orderFrontRegardless(ptr);
                    } else {
                        CWrapper.NSWindow.orderFront(ptr);
                    }

                    boolean isKeyWindow = CWrapper.NSWindow.isKeyWindow(ptr);
                    if (!isKeyWindow) {
                        CWrapper.NSWindow.makeKeyWindow(ptr);
                    }

                    if (owner != null
                            && owner.getPeer() instanceof LWLightweightFramePeer) {
                        LWLightweightFramePeer peer =
                                (LWLightweightFramePeer) owner.getPeer();

                        long ownerWindowPtr = peer.getOverriddenWindowHandle();
                        if (ownerWindowPtr != 0) {
                            //Place window above JavaFX stage
                            CWrapper.NSWindow.addChildWindow(
                                    ownerWindowPtr, ptr,
                                    CWrapper.NSWindow.NSWindowAbove);
                        }
                    }
                });
            } else {
                execute(ptr->{
                    // immediately hide the window
                    CWrapper.NSWindow.orderOut(ptr);
                    // process the close
                    CWrapper.NSWindow.close(ptr);
                });
            }
        } else {
            // otherwise, put it in a proper z-order
            CPlatformWindow bw
                    = (CPlatformWindow) blocker.getPlatformWindow();
            bw.execute(blockerPtr -> {
                execute(ptr -> {
                    CWrapper.NSWindow.orderWindow(ptr,
                                                  CWrapper.NSWindow.NSWindowBelow,
                                                  blockerPtr);
                });
            });
        }
        this.visible = visible;

        // Manage the extended state when showing
        if (visible) {
            /* Frame or Dialog should be set property WINDOW_FULLSCREENABLE to true if the
            Frame or Dialog is resizable.
            **/
            final boolean resizable = (target instanceof Frame) ? ((Frame)target).isResizable() :
            ((target instanceof Dialog) ? ((Dialog)target).isResizable() : false);
            if (resizable) {
                setCanFullscreen(true);
            }

            // Apply the extended state as expected in shared code
            if (target instanceof Frame) {
                if (!wasMaximized && isMaximized()) {
                    // setVisible could have changed the native maximized state
                    deliverZoom(true);
                } else {
                    int frameState = ((Frame)target).getExtendedState();
                    if ((frameState & Frame.ICONIFIED) != 0) {
                        // Treat all state bit masks with ICONIFIED bit as ICONIFIED state.
                        frameState = Frame.ICONIFIED;
                    }

                    switch (frameState) {
                        case Frame.ICONIFIED:
                            execute(CWrapper.NSWindow::miniaturize);
                            break;
                        case Frame.MAXIMIZED_BOTH:
                            maximize();
                            break;
                        default: // NORMAL
                            unmaximize(); // in case it was maximized, otherwise this is a no-op
                            break;
                    }
                }
            }
        }

        nativeSynthesizeMouseEnteredExitedEvents();

        // Configure stuff #2
        updateFocusabilityForAutoRequestFocus(true);

        // Manage parent-child relationship when showing
        final ComponentAccessor acc = AWTAccessor.getComponentAccessor();

        if (visible) {
            // Order myself above my parent
            if (owner != null && owner.isVisible()) {
                owner.execute(ownerPtr -> {
                    execute(ptr -> {
                        CWrapper.NSWindow.orderWindow(ptr, CWrapper.NSWindow.NSWindowAbove, ownerPtr);
                    });
                });
                execute(CWrapper.NSWindow::orderFront);
                applyWindowLevel(target);
            }

            // Order my own children above myself
            for (Window w : target.getOwnedWindows()) {
                final Object p = acc.getPeer(w);
                if (p instanceof LWWindowPeer) {
                    CPlatformWindow pw = (CPlatformWindow)((LWWindowPeer)p).getPlatformWindow();
                    if (pw != null && pw.isVisible()) {
                        pw.execute(childPtr -> {
                            execute(ptr -> {
                                CWrapper.NSWindow.orderWindow(childPtr, CWrapper.NSWindow.NSWindowAbove, ptr);
                            });
                        });
                        pw.applyWindowLevel(w);
                    }
                }
            }
        }

        // Deal with the blocker of the window being shown
        if (blocker != null && visible) {
            // Make sure the blocker is above its siblings
            ((CPlatformWindow)blocker.getPlatformWindow()).orderAboveSiblings();
        }
    }

    @Override // PlatformWindow
    public void setTitle(String title) {
        execute(ptr -> nativeSetNSWindowTitle(ptr, title));
    }

    // Should be called on every window key property change.
    @Override // PlatformWindow
    public void updateIconImages() {
        final CImage cImage = getImageForTarget();
        execute(ptr -> {
            if (cImage == null) {
                nativeSetNSWindowMinimizedIcon(ptr, 0L);
            } else {
                cImage.execute(imagePtr -> {
                    nativeSetNSWindowMinimizedIcon(ptr, imagePtr);
                });
            }
        });
    }

    public SurfaceData getSurfaceData() {
        return contentView.getSurfaceData();
    }

    @Override  // PlatformWindow
    public void toBack() {
        execute(CPlatformWindow::nativePushNSWindowToBack);
    }

    @Override  // PlatformWindow
    public void toFront() {
        LWCToolkit lwcToolkit = (LWCToolkit) Toolkit.getDefaultToolkit();
        Window w = DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        final ComponentAccessor acc = AWTAccessor.getComponentAccessor();
        if( w != null && acc.getPeer(w) != null
                && ((LWWindowPeer)acc.getPeer(w)).getPeerType() == LWWindowPeer.PeerType.EMBEDDED_FRAME
                && !lwcToolkit.isApplicationActive()) {
            lwcToolkit.activateApplicationIgnoringOtherApps();
        }
        updateFocusabilityForAutoRequestFocus(false);
        execute(CPlatformWindow::nativePushNSWindowToFront);
        updateFocusabilityForAutoRequestFocus(true);
    }

    private void setCanFullscreen(final boolean canFullScreen) {
        if (target instanceof RootPaneContainer
                && getPeer().getPeerType() == PeerType.FRAME) {

            if (isInFullScreen && !canFullScreen) {
                toggleFullScreen();
            }

            final RootPaneContainer rpc = (RootPaneContainer) target;
            rpc.getRootPane().putClientProperty(
                    CPlatformWindow.WINDOW_FULLSCREENABLE, canFullScreen);
        }
    }

    @Override
    public void setResizable(final boolean resizable) {
        setCanFullscreen(resizable);
        setStyleBits(RESIZABLE, resizable);
        setStyleBits(ZOOMABLE, resizable);
    }

    @Override
    public void setSizeConstraints(int minW, int minH, int maxW, int maxH) {
        execute(ptr -> nativeSetNSWindowMinMax(ptr, minW, minH, maxW, maxH));
    }

    @Override
    public boolean rejectFocusRequest(FocusEvent.Cause cause) {
        // Cross-app activation requests are not allowed.
        if (cause != FocusEvent.Cause.MOUSE_EVENT &&
            !((LWCToolkit)Toolkit.getDefaultToolkit()).isApplicationActive())
        {
            focusLogger.fine("the app is inactive, so the request is rejected");
            return true;
        }
        return false;
    }

    @Override
    public boolean requestWindowFocus() {
        execute(ptr -> {
            if (CWrapper.NSWindow.canBecomeMainWindow(ptr)) {
                CWrapper.NSWindow.makeMainWindow(ptr);
            }
            CWrapper.NSWindow.makeKeyAndOrderFront(ptr);
        });
        return true;
    }

    @Override
    public boolean isActive() {
        AtomicBoolean ref = new AtomicBoolean();
        execute(ptr -> {
            ref.set(CWrapper.NSWindow.isKeyWindow(ptr));
        });
        return ref.get();
    }

    @Override
    public void updateFocusableWindowState() {
        final boolean isFocusable = isNativelyFocusableWindow();
        setStyleBits(SHOULD_BECOME_KEY | SHOULD_BECOME_MAIN, isFocusable); // set both bits at once
    }

    @Override
    public void setAlwaysOnTop(boolean isAlwaysOnTop) {
        setStyleBits(ALWAYS_ON_TOP, isAlwaysOnTop);
    }

    @Override
    public void setOpacity(float opacity) {
        execute(ptr -> CWrapper.NSWindow.setAlphaValue(ptr, opacity));
    }

    @Override
    public void setOpaque(boolean isOpaque) {
        execute(ptr -> CWrapper.NSWindow.setOpaque(ptr, isOpaque));
        boolean isTextured = (peer == null) ? false : peer.isTextured();
        if (!isTextured) {
            if (!isOpaque) {
                execute(ptr -> CWrapper.NSWindow.setBackgroundColor(ptr, 0));
            } else if (peer != null) {
                Color color = peer.getBackground();
                if (color != null) {
                    int rgb = color.getRGB();
                    execute(ptr->CWrapper.NSWindow.setBackgroundColor(ptr, rgb));
                }
            }
        }

        //This is a temporary workaround. Looks like after 7124236 will be fixed
        //the correct place for invalidateShadow() is CGLayer.drawInCGLContext.
        SwingUtilities.invokeLater(this::invalidateShadow);
    }

    @Override
    public void enterFullScreenMode() {
        isFullScreenMode = true;
        execute(CPlatformWindow::nativeEnterFullScreenMode);
    }

    @Override
    public void exitFullScreenMode() {
        execute(CPlatformWindow::nativeExitFullScreenMode);
        isFullScreenMode = false;
    }

    @Override
    public boolean isFullScreenMode() {
        return isFullScreenMode;
    }

    @Override
    public void setWindowState(int windowState) {
        if (peer == null || !peer.isVisible()) {
            // setVisible() applies the state
            return;
        }

        int prevWindowState = peer.getState();
        if (prevWindowState == windowState) return;

        if ((windowState & Frame.ICONIFIED) != 0) {
            // Treat all state bit masks with ICONIFIED bit as ICONIFIED state.
            windowState = Frame.ICONIFIED;
        }

        switch (windowState) {
            case Frame.ICONIFIED:
                if (prevWindowState == Frame.MAXIMIZED_BOTH) {
                    // let's return into the normal states first
                    // the zoom call toggles between the normal and the max states
                    unmaximize();
                }
                execute(CWrapper.NSWindow::miniaturize);
                break;
            case Frame.MAXIMIZED_BOTH:
                if (prevWindowState == Frame.ICONIFIED) {
                    // let's return into the normal states first
                    execute(CWrapper.NSWindow::deminiaturize);
                }
                maximize();
                break;
            case Frame.NORMAL:
                if (prevWindowState == Frame.ICONIFIED) {
                    execute(CWrapper.NSWindow::deminiaturize);
                } else if (prevWindowState == Frame.MAXIMIZED_BOTH) {
                    // the zoom call toggles between the normal and the max states
                    unmaximize();
                }
                break;
            default:
                throw new RuntimeException("Unknown window state: " + windowState);
        }

        // NOTE: the SWP.windowState field gets updated to the newWindowState
        //       value when the native notification comes to us
    }

    @Override
    public void setModalBlocked(boolean blocked) {
        if (target.getModalExclusionType() == Dialog.ModalExclusionType.APPLICATION_EXCLUDE) {
            return;
        }

        if (blocked) {
            // We are going to show a modal window. Previously displayed window will be
            // blocked/disabled. So we have to send mouse exited event to it now, since
            // all mouse events are discarded for blocked/disabled windows.
            execute(ptr -> nativeSynthesizeMouseEnteredExitedEvents(ptr, CocoaConstants.NSMouseExited));
        }

        execute(ptr -> nativeSetEnabled(ptr, !blocked));
        checkBlockingAndOrder();
    }

    public final void invalidateShadow() {
        execute(ptr -> nativeRevalidateNSWindowShadow(ptr));
    }

    // ----------------------------------------------------------------------
    //                          UTILITY METHODS
    // ----------------------------------------------------------------------

    /**
     * Find image to install into Title or into Application icon. First try
     * icons installed for toplevel. Null is returned, if there is no icon and
     * default Duke image should be used.
     */
    private CImage getImageForTarget() {
        CImage icon = null;
        try {
            icon = CImage.getCreator().createFromImages(target.getIconImages());
        } catch (Exception ignored) {
            // Perhaps the icon passed into Java is broken. Skipping this icon.
        }
        return icon;
    }

    /*
     * Returns LWWindowPeer associated with this delegate.
     */
    @Override
    public LWWindowPeer getPeer() {
        return peer;
    }

    @Override
    public boolean isUnderMouse() {
        return contentView.isUnderMouse();
    }

    public CPlatformView getContentView() {
        return contentView;
    }

    @Override
    public long getLayerPtr() {
        return contentView.getWindowLayerPtr();
    }

    void flushBuffers() {
        if (isVisible() && !nativeBounds.isEmpty() && !isFullScreenMode) {
            try {
                LWCToolkit.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        //Posting an empty to flush the EventQueue without blocking the main thread
                    }
                }, target);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Helper method to get a pointer to the native view from the PlatformWindow.
     */
    static long getNativeViewPtr(PlatformWindow platformWindow) {
        long nativePeer = 0L;
        if (platformWindow instanceof CPlatformWindow) {
            nativePeer = ((CPlatformWindow) platformWindow).getContentView().getAWTView();
        } else if (platformWindow instanceof CViewPlatformEmbeddedFrame){
            nativePeer = ((CViewPlatformEmbeddedFrame) platformWindow).getNSViewPtr();
        }
        return nativePeer;
    }

    /*************************************************************
     * Callbacks from the AWTWindow and AWTView objc classes.
     *************************************************************/
    private void deliverWindowFocusEvent(boolean gained, CPlatformWindow opposite){
        // Fix for 7150349: ingore "gained" notifications when the app is inactive.
        if (gained && !((LWCToolkit)Toolkit.getDefaultToolkit()).isApplicationActive()) {
            focusLogger.fine("the app is inactive, so the notification is ignored");
            return;
        }

        LWWindowPeer oppositePeer = (opposite == null)? null : opposite.getPeer();
        responder.handleWindowFocusEvent(gained, oppositePeer);
    }

    protected void deliverMoveResizeEvent(int x, int y, int width, int height,
                                        boolean byUser) {
        AtomicBoolean ref = new AtomicBoolean();
        execute(ptr -> {
            ref.set(CWrapper.NSWindow.isZoomed(ptr));
        });
        isZoomed = ref.get();
        checkZoom();

        final Rectangle oldB = nativeBounds;
        nativeBounds = new Rectangle(x, y, width, height);
        if (peer != null) {
            peer.notifyReshape(x, y, width, height);
            // System-dependent appearance optimization.
            if ((byUser && !oldB.getSize().equals(nativeBounds.getSize()))
                    || isFullScreenAnimationOn) {
                flushBuffers();
            }
        }
    }

    private void deliverWindowClosingEvent() {
        if (peer != null && peer.getBlocker() == null) {
            peer.postEvent(new WindowEvent(target, WindowEvent.WINDOW_CLOSING));
        }
    }

    private void deliverIconify(final boolean iconify) {
        if (peer != null) {
            peer.notifyIconify(iconify);
        }
        if (iconify) {
            isIconifyAnimationActive = false;
        }
    }

    private void deliverZoom(final boolean isZoomed) {
        if (peer != null) {
            peer.notifyZoom(isZoomed);
        }
    }

    private void checkZoom() {
        if (peer != null) {
            int state = peer.getState();
            if (state != Frame.MAXIMIZED_BOTH && isMaximized()) {
                deliverZoom(true);
            } else if (state == Frame.MAXIMIZED_BOTH && !isMaximized()) {
                deliverZoom(false);
            }
        }
    }

    private void deliverNCMouseDown() {
        if (peer != null) {
            peer.notifyNCMouseDown();
        }
    }

    /*
     * Our focus model is synthetic and only non-simple window
     * may become natively focusable window.
     */
    private boolean isNativelyFocusableWindow() {
        if (peer == null) {
            return false;
        }

        return !peer.isSimpleWindow() && target.getFocusableWindowState();
    }

    private boolean isBlocked() {
        LWWindowPeer blocker = (peer != null) ? peer.getBlocker() : null;
        return (blocker != null);
    }

    /*
     * An utility method for the support of the auto request focus.
     * Updates the focusable state of the window under certain
     * circumstances.
     */
    private void updateFocusabilityForAutoRequestFocus(boolean isFocusable) {
        if (target.isAutoRequestFocus() || !isNativelyFocusableWindow()) return;
        setStyleBits(SHOULD_BECOME_KEY | SHOULD_BECOME_MAIN, isFocusable); // set both bits at once
    }

    private boolean checkBlockingAndOrder() {
        LWWindowPeer blocker = (peer == null)? null : peer.getBlocker();
        if (blocker == null) {
            return false;
        }

        if (blocker instanceof CPrinterDialogPeer) {
            return true;
        }

        CPlatformWindow pWindow = (CPlatformWindow)blocker.getPlatformWindow();

        pWindow.orderAboveSiblings();

        pWindow.execute(ptr -> {
            CWrapper.NSWindow.orderFrontRegardless(ptr);
            CWrapper.NSWindow.makeKeyAndOrderFront(ptr);
            CWrapper.NSWindow.makeMainWindow(ptr);
        });
        return true;
    }

    private boolean isIconified() {
        boolean isIconified = false;
        if (target instanceof Frame) {
            int state = ((Frame)target).getExtendedState();
            if ((state & Frame.ICONIFIED) != 0) {
                isIconified = true;
            }
        }
        return isIconifyAnimationActive || isIconified;
    }

    private boolean isOneOfOwnersOrSelf(CPlatformWindow window) {
        while (window != null) {
            if (this == window) {
                return true;
            }
            window = window.owner;
        }
        return false;
    }

    private CPlatformWindow getRootOwner() {
        CPlatformWindow rootOwner = this;
        while (rootOwner.owner != null) {
            rootOwner = rootOwner.owner;
        }
        return rootOwner;
    }

    private void orderAboveSiblings() {
        // Recursively pop up the windows from the very bottom, (i.e. root owner) so that
        // the windows are ordered above their nearest owner; ancestors of the window,
        // which is going to become 'main window', are placed above their siblings.
        CPlatformWindow rootOwner = getRootOwner();
        if (rootOwner.isVisible() && !rootOwner.isIconified() && !rootOwner.isActive()) {
            rootOwner.execute(CWrapper.NSWindow::orderFront);
        }

        // Do not order child windows of iconified owner.
        if (!rootOwner.isIconified()) {
            final WindowAccessor windowAccessor = AWTAccessor.getWindowAccessor();
            orderAboveSiblingsImpl(windowAccessor.getOwnedWindows(rootOwner.target));
        }
    }

    private void orderAboveSiblingsImpl(Window[] windows) {
        ArrayList<Window> childWindows = new ArrayList<Window>();

        final ComponentAccessor componentAccessor = AWTAccessor.getComponentAccessor();
        final WindowAccessor windowAccessor = AWTAccessor.getWindowAccessor();
        Arrays.sort(windows, siblingsComparator);
        // Go through the list of windows and perform ordering.
        CPlatformWindow pwUnder = null;
        for (Window w : windows) {
            boolean iconified = false;
            final Object p = componentAccessor.getPeer(w);
            if (p instanceof LWWindowPeer) {
                CPlatformWindow pw = (CPlatformWindow)((LWWindowPeer)p).getPlatformWindow();
                iconified = isIconified();
                if (pw != null && pw.isVisible() && !iconified) {
                    // If the window is one of ancestors of 'main window' or is going to become main by itself,
                    // the window should be ordered above its siblings; otherwise the window is just ordered
                    // above its nearest parent.
                    if (pw.isOneOfOwnersOrSelf(this)) {
                        pw.execute(CWrapper.NSWindow::orderFront);
                    } else {
                        if (pwUnder == null) {
                            pwUnder = pw.owner;
                        }
                        pwUnder.execute(underPtr -> {
                            pw.execute(ptr -> {
                                CWrapper.NSWindow.orderWindow(ptr, CWrapper.NSWindow.NSWindowAbove, underPtr);
                            });
                        });
                        pwUnder = pw;
                    }
                    pw.applyWindowLevel(w);
                }
            }
            // Retrieve the child windows for each window from the list except iconified ones
            // and store them for future use.
            // Note: we collect data about child windows even for invisible owners, since they may have
            // visible children.
            if (!iconified) {
                childWindows.addAll(Arrays.asList(windowAccessor.getOwnedWindows(w)));
            }
        }
        // If some windows, which have just been ordered, have any child windows, let's start new iteration
        // and order these child windows.
        if (!childWindows.isEmpty()) {
            orderAboveSiblingsImpl(childWindows.toArray(new Window[0]));
        }
    }

    protected void applyWindowLevel(Window target) {
        if (target.isAlwaysOnTop() && target.getType() != Window.Type.POPUP) {
            execute(ptr->CWrapper.NSWindow.setLevel(ptr, CWrapper.NSWindow.NSFloatingWindowLevel));
        } else if (target.getType() == Window.Type.POPUP) {
            execute(ptr->CWrapper.NSWindow.setLevel(ptr, CWrapper.NSWindow.NSPopUpMenuWindowLevel));
        }
    }

    private Window getOwnerFrameOrDialog(Window window) {
        Window owner = window.getOwner();
        while (owner != null && !(owner instanceof Frame || owner instanceof Dialog)) {
            owner = owner.getOwner();
        }
        return owner;
    }

    private boolean isSimpleWindowOwnedByEmbeddedFrame() {
        if (peer != null && peer.isSimpleWindow()) {
            return (getOwnerFrameOrDialog(target) instanceof CEmbeddedFrame);
        }
        return false;
    }
    // ----------------------------------------------------------------------
    //                          NATIVE CALLBACKS
    // ----------------------------------------------------------------------

    private void windowWillMiniaturize() {
        isIconifyAnimationActive = true;
    }

    private void windowDidBecomeMain() {
        lastBecomeMainTime = System.currentTimeMillis();
        if (checkBlockingAndOrder()) return;
        // If it's not blocked, make sure it's above its siblings
        orderAboveSiblings();
    }

    private void windowWillEnterFullScreen() {
        isFullScreenAnimationOn = true;
    }

    private void windowDidEnterFullScreen() {
        isInFullScreen = true;
        isFullScreenAnimationOn = false;
    }

    private void windowWillExitFullScreen() {
        isFullScreenAnimationOn = true;
    }

    private void windowDidExitFullScreen() {
        isInFullScreen = false;
        isFullScreenAnimationOn = false;
    }
}
