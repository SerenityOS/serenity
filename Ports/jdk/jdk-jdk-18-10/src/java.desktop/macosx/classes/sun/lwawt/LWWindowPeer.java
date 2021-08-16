/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.lwawt;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.MenuBar;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowEvent;
import java.awt.peer.ComponentPeer;
import java.awt.peer.DialogPeer;
import java.awt.peer.FramePeer;
import java.awt.peer.KeyboardFocusManagerPeer;
import java.awt.peer.WindowPeer;
import java.util.List;

import javax.swing.JComponent;

import sun.awt.AWTAccessor;
import sun.awt.AWTAccessor.ComponentAccessor;
import sun.awt.AppContext;
import sun.awt.CGraphicsDevice;
import sun.awt.DisplayChangedListener;
import sun.awt.ExtendedKeyCodes;
import sun.awt.FullScreenCapable;
import sun.awt.SunToolkit;
import sun.awt.TimedWindowEvent;
import sun.awt.UngrabEvent;
import sun.java2d.NullSurfaceData;
import sun.java2d.SunGraphics2D;
import sun.java2d.SunGraphicsEnvironment;
import sun.java2d.SurfaceData;
import sun.java2d.loops.Blit;
import sun.java2d.loops.CompositeType;
import sun.java2d.pipe.Region;
import sun.util.logging.PlatformLogger;

public class LWWindowPeer
    extends LWContainerPeer<Window, JComponent>
    implements FramePeer, DialogPeer, FullScreenCapable, DisplayChangedListener, PlatformEventNotifier
{
    public enum PeerType {
        SIMPLEWINDOW,
        FRAME,
        DIALOG,
        EMBEDDED_FRAME,
        VIEW_EMBEDDED_FRAME,
        LW_FRAME
    }

    private static final PlatformLogger focusLog = PlatformLogger.getLogger("sun.lwawt.focus.LWWindowPeer");

    private final PlatformWindow platformWindow;

    private static final int MINIMUM_WIDTH = 1;
    private static final int MINIMUM_HEIGHT = 1;

    private Insets insets = new Insets(0, 0, 0, 0);
    private Rectangle maximizedBounds;

    private GraphicsDevice graphicsDevice;
    private GraphicsConfiguration graphicsConfig;

    private SurfaceData surfaceData;
    private final Object surfaceDataLock = new Object();

    private volatile int windowState = Frame.NORMAL;

    // check that the mouse is over the window
    private volatile boolean isMouseOver = false;

    // A peer where the last mouse event came to. Used by cursor manager to
    // find the component under cursor
    private static volatile LWComponentPeer<?, ?> lastCommonMouseEventPeer;

    // A peer where the last mouse event came to. Used to generate
    // MOUSE_ENTERED/EXITED notifications
    private volatile LWComponentPeer<?, ?> lastMouseEventPeer;

    // Peers where all dragged/released events should come to,
    // depending on what mouse button is being dragged according to Cocoa
    private static final LWComponentPeer<?, ?>[] mouseDownTarget = new LWComponentPeer<?, ?>[3];

    // A bitmask that indicates what mouse buttons produce MOUSE_CLICKED events
    // on MOUSE_RELEASE. Click events are only generated if there were no drag
    // events between MOUSE_PRESSED and MOUSE_RELEASED for particular button
    private static int mouseClickButtons = 0;

    private volatile boolean isOpaque = true;

    private static final Font DEFAULT_FONT = new Font("Lucida Grande", Font.PLAIN, 13);

    private static LWWindowPeer grabbingWindow;

    private volatile boolean skipNextFocusChange;

    private static final Color nonOpaqueBackground = new Color(0, 0, 0, 0);

    private volatile boolean textured;

    private final PeerType peerType;

    private final SecurityWarningWindow warningWindow;

    private volatile boolean targetFocusable;

    /**
     * Current modal blocker or null.
     *
     * Synchronization: peerTreeLock.
     */
    private LWWindowPeer blocker;

    public LWWindowPeer(Window target, PlatformComponent platformComponent,
                        PlatformWindow platformWindow, PeerType peerType)
    {
        super(target, platformComponent);
        this.platformWindow = platformWindow;
        this.peerType = peerType;

        Window owner = target.getOwner();
        LWWindowPeer ownerPeer = owner == null ? null :
             (LWWindowPeer) AWTAccessor.getComponentAccessor().getPeer(owner);
        PlatformWindow ownerDelegate = (ownerPeer != null) ? ownerPeer.getPlatformWindow() : null;

        // The delegate.initialize() needs a non-null GC on X11.
        GraphicsConfiguration gc = getTarget().getGraphicsConfiguration();
        synchronized (getStateLock()) {
            // graphicsConfig should be updated according to the real window
            // bounds when the window is shown, see 4868278
            this.graphicsConfig = gc;
        }

        if (!target.isFontSet()) {
            target.setFont(DEFAULT_FONT);
        }

        if (!target.isBackgroundSet()) {
            target.setBackground(SystemColor.window);
        } else {
            // first we check if user provided alpha for background. This is
            // similar to what Apple's Java do.
            // Since JDK7 we should rely on setOpacity() only.
            // this.opacity = c.getAlpha();
        }

        if (!target.isForegroundSet()) {
            target.setForeground(SystemColor.windowText);
            // we should not call setForeground because it will call a repaint
            // which the peer may not be ready to do yet.
        }

        platformWindow.initialize(target, this, ownerDelegate);

        // Init warning window(for applets)
        SecurityWarningWindow warn = null;
        if (target.getWarningString() != null) {
            // accessSystemTray permission allows to display TrayIcon, TrayIcon tooltip
            // and TrayIcon balloon windows without a warning window.
            if (!AWTAccessor.getWindowAccessor().isTrayIconWindow(target)) {
                LWToolkit toolkit = (LWToolkit)Toolkit.getDefaultToolkit();
                warn = toolkit.createSecurityWarning(target, this);
            }
        }

        warningWindow = warn;
    }

    @Override
    void initializeImpl() {
        super.initializeImpl();


        if (getTarget() instanceof Frame) {
            Frame frame = (Frame) getTarget();
            setTitle(frame.getTitle());
            setState(frame.getExtendedState());
            setMaximizedBounds(frame.getMaximizedBounds());
        } else if (getTarget() instanceof Dialog) {
            setTitle(((Dialog) getTarget()).getTitle());
        }

        updateAlwaysOnTopState();
        updateMinimumSize();
        updateFocusableWindowState();

        final Shape shape = getTarget().getShape();
        if (shape != null) {
            applyShape(Region.getInstance(shape, null));
        }

        final float opacity = getTarget().getOpacity();
        if (opacity < 1.0f) {
            setOpacity(opacity);
        }

        setOpaque(getTarget().isOpaque());

        updateInsets(platformWindow.getInsets());
        if (getSurfaceData() == null) {
            replaceSurfaceData(false);
        }
        activateDisplayListener();
    }

    // Just a helper method
    @Override
    public PlatformWindow getPlatformWindow() {
        return platformWindow;
    }

    @Override
    protected LWWindowPeer getWindowPeerOrSelf() {
        return this;
    }

    // ---- PEER METHODS ---- //

    @Override
    protected void disposeImpl() {
        deactivateDisplayListener();
        SurfaceData oldData = getSurfaceData();
        synchronized (surfaceDataLock){
            surfaceData = null;
        }
        if (oldData != null) {
            oldData.invalidate();
        }
        if (isGrabbing()) {
            ungrab();
        }
        if (warningWindow != null) {
            warningWindow.dispose();
        }

        platformWindow.dispose();
        super.disposeImpl();
    }

    @Override
    protected void setVisibleImpl(final boolean visible) {
        if (!visible && warningWindow != null) {
            warningWindow.setVisible(false, false);
        }
        updateFocusableWindowState();
        super.setVisibleImpl(visible);
        // TODO: update graphicsConfig, see 4868278
        platformWindow.setVisible(visible);
        if (isSimpleWindow()) {
            KeyboardFocusManagerPeer kfmPeer = LWKeyboardFocusManagerPeer.getInstance();
            if (visible) {
                if (!getTarget().isAutoRequestFocus()) {
                    return;
                } else {
                    requestWindowFocus(FocusEvent.Cause.ACTIVATION);
                }
            // Focus the owner in case this window is focused.
            } else if (kfmPeer.getCurrentFocusedWindow() == getTarget()) {
                // Transfer focus to the owner.
                LWWindowPeer owner = getOwnerFrameDialog(LWWindowPeer.this);
                if (owner != null) {
                    owner.requestWindowFocus(FocusEvent.Cause.ACTIVATION);
                }
            }
        }
    }

    @Override
    public final GraphicsConfiguration getGraphicsConfiguration() {
        synchronized (getStateLock()) {
            return graphicsConfig;
        }
    }

    @Override
    public boolean updateGraphicsData(GraphicsConfiguration gc) {
        setGraphicsConfig(gc);
        return false;
    }

    protected final Graphics getOnscreenGraphics(Color fg, Color bg, Font f) {
        SurfaceData surfaceData = getSurfaceData();
        if (surfaceData == null) {
            return null;
        }
        if (fg == null) {
            fg = SystemColor.windowText;
        }
        if (bg == null) {
            bg = SystemColor.window;
        }
        if (f == null) {
            f = DEFAULT_FONT;
        }
        return new SunGraphics2D(surfaceData, fg, bg, f);
    }

    @Override
    public void setBounds(int x, int y, int w, int h, int op) {

        if((op & NO_EMBEDDED_CHECK) == 0 && getPeerType() == PeerType.VIEW_EMBEDDED_FRAME) {
            return;
        }

        if ((op & SET_CLIENT_SIZE) != 0) {
            // SET_CLIENT_SIZE is only applicable to window peers, so handle it here
            // instead of pulling 'insets' field up to LWComponentPeer
            // no need to add insets since Window's notion of width and height includes insets.
            op &= ~SET_CLIENT_SIZE;
            op |= SET_SIZE;
        }

        // Don't post ComponentMoved/Resized and Paint events
        // until we've got a notification from the delegate
        Rectangle cb = constrainBounds(x, y, w, h);

        Rectangle newBounds = new Rectangle(getBounds());
        if ((op & (SET_LOCATION | SET_BOUNDS)) != 0) {
            newBounds.x = cb.x;
            newBounds.y = cb.y;
        }
        if ((op & (SET_SIZE | SET_BOUNDS)) != 0) {
            newBounds.width = cb.width;
            newBounds.height = cb.height;
        }
        // Native system could constraint bounds, so the peer wold be updated in the callback
        platformWindow.setBounds(newBounds.x, newBounds.y, newBounds.width, newBounds.height);
    }

    public Rectangle constrainBounds(Rectangle bounds) {
        return constrainBounds(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public Rectangle constrainBounds(int x, int y, int w, int h) {

        if (w < MINIMUM_WIDTH) {
            w = MINIMUM_WIDTH;
        }

        if (h < MINIMUM_HEIGHT) {
            h = MINIMUM_HEIGHT;
        }

        final int maxW = getLWGC().getMaxTextureWidth();
        final int maxH = getLWGC().getMaxTextureHeight();

        if (w > maxW) {
            w = maxW;
        }
        if (h > maxH) {
            h = maxH;
        }

        return new Rectangle(x, y, w, h);
    }

    @Override
    public Point getLocationOnScreen() {
        return platformWindow.getLocationOnScreen();
    }

    /**
     * Overridden from LWContainerPeer to return the correct insets.
     * Insets are queried from the delegate and are kept up to date by
     * requiering when needed (i.e. when the window geometry is changed).
     */
    @Override
    public Insets getInsets() {
        synchronized (getStateLock()) {
            return insets;
        }
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        // TODO: check for "use platform metrics" settings
        return platformWindow.getFontMetrics(f);
    }

    @Override
    public void toFront() {
        platformWindow.toFront();
    }

    @Override
    public void toBack() {
        platformWindow.toBack();
    }

    @Override
    public void setZOrder(ComponentPeer above) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void updateAlwaysOnTopState() {
        platformWindow.setAlwaysOnTop(getTarget().isAlwaysOnTop());
    }

    @Override
    public void updateFocusableWindowState() {
        targetFocusable = getTarget().isFocusableWindow();
        platformWindow.updateFocusableWindowState();
    }

    @Override
    public void setModalBlocked(Dialog blocker, boolean blocked) {
        synchronized (getPeerTreeLock()) {
            ComponentPeer peer =  AWTAccessor.getComponentAccessor().getPeer(blocker);
            if (blocked && (peer instanceof LWWindowPeer)) {
                this.blocker = (LWWindowPeer) peer;
            } else {
                this.blocker = null;
            }
        }

        platformWindow.setModalBlocked(blocked);
    }

    @Override
    public void updateMinimumSize() {
        final Dimension min;
        if (getTarget().isMinimumSizeSet()) {
            min = getTarget().getMinimumSize();
            min.width = Math.max(min.width, MINIMUM_WIDTH);
            min.height = Math.max(min.height, MINIMUM_HEIGHT);
        } else {
            min = new Dimension(MINIMUM_WIDTH, MINIMUM_HEIGHT);
        }

        final Dimension max;
        if (getTarget().isMaximumSizeSet()) {
            max = getTarget().getMaximumSize();
            max.width = Math.min(max.width, getLWGC().getMaxTextureWidth());
            max.height = Math.min(max.height, getLWGC().getMaxTextureHeight());
        } else {
            max = new Dimension(getLWGC().getMaxTextureWidth(),
                                getLWGC().getMaxTextureHeight());
        }

        platformWindow.setSizeConstraints(min.width, min.height, max.width, max.height);
    }

    @Override
    public void updateIconImages() {
        getPlatformWindow().updateIconImages();
    }

    @Override
    public void setOpacity(float opacity) {
        getPlatformWindow().setOpacity(opacity);
        repaintPeer();
    }

    @Override
    public final void setOpaque(final boolean isOpaque) {
        if (this.isOpaque != isOpaque) {
            this.isOpaque = isOpaque;
            updateOpaque();
        }
    }

    private void updateOpaque() {
        getPlatformWindow().setOpaque(!isTranslucent());
        replaceSurfaceData(false);
        repaintPeer();
    }

    @Override
    public void updateWindow() {
    }

    public final boolean isTextured() {
        return textured;
    }

    public final void setTextured(final boolean isTextured) {
        textured = isTextured;
    }

    @Override
    public final boolean isTranslucent() {
        synchronized (getStateLock()) {
            /*
             * Textured window is a special case of translucent window.
             * The difference is only in nswindow background. So when we set
             * texture property our peer became fully translucent. It doesn't
             * fill background, create non opaque backbuffers and layer etc.
             */
            return !isOpaque || isShaped() || isTextured();
        }
    }

    @Override
    final void applyShapeImpl(final Region shape) {
        super.applyShapeImpl(shape);
        updateOpaque();
    }

    @Override
    public void repositionSecurityWarning() {
        if (warningWindow != null) {
            ComponentAccessor compAccessor = AWTAccessor.getComponentAccessor();
            Window target = getTarget();
            int x = compAccessor.getX(target);
            int y = compAccessor.getY(target);
            int width = compAccessor.getWidth(target);
            int height = compAccessor.getHeight(target);
            warningWindow.reposition(x, y, width, height);
        }
    }

    // ---- FRAME PEER METHODS ---- //

    @Override // FramePeer and DialogPeer
    public void setTitle(String title) {
        platformWindow.setTitle(title == null ? "" : title);
    }

    @Override
    public void setMenuBar(MenuBar mb) {
         platformWindow.setMenuBar(mb);
    }

    @Override // FramePeer and DialogPeer
    public void setResizable(boolean resizable) {
        platformWindow.setResizable(resizable);
    }

    @Override
    public void setState(int state) {
        platformWindow.setWindowState(state);
    }

    @Override
    public int getState() {
        return windowState;
    }

    private boolean isMaximizedBoundsSet() {
        synchronized (getStateLock()) {
            return maximizedBounds != null;
        }
    }

    private Rectangle getDefaultMaximizedBounds() {
        GraphicsConfiguration config = getGraphicsConfiguration();
        Insets screenInsets = ((CGraphicsDevice) config.getDevice())
                .getScreenInsets();
        Rectangle gcBounds = config.getBounds();
        return new Rectangle(
                gcBounds.x + screenInsets.left,
                gcBounds.y + screenInsets.top,
                gcBounds.width - screenInsets.left - screenInsets.right,
                gcBounds.height - screenInsets.top - screenInsets.bottom);
    }

    @Override
    public void setMaximizedBounds(Rectangle bounds) {
        boolean isMaximizedBoundsSet;
        synchronized (getStateLock()) {
            this.maximizedBounds = (isMaximizedBoundsSet = (bounds != null))
                    ? constrainBounds(bounds) : null;
        }

        setPlatformMaximizedBounds(isMaximizedBoundsSet ? maximizedBounds
                : getDefaultMaximizedBounds());
    }

    public Rectangle getMaximizedBounds() {
        synchronized (getStateLock()) {
            return (maximizedBounds == null)
                    ? getDefaultMaximizedBounds()
                    : maximizedBounds;
        }
    }

    private void setPlatformMaximizedBounds(Rectangle bounds) {
        platformWindow.setMaximizedBounds(
                bounds.x, bounds.y,
                bounds.width, bounds.height);
    }

    @Override
    public void setBoundsPrivate(int x, int y, int width, int height) {
        setBounds(x, y, width, height, SET_BOUNDS | NO_EMBEDDED_CHECK);
    }

    @Override
    public Rectangle getBoundsPrivate() {
        throw new RuntimeException("not implemented");
    }

    // ---- DIALOG PEER METHODS ---- //

    @Override
    public void blockWindows(List<Window> windows) {
        //TODO: LWX will probably need some collectJavaToplevels to speed this up
        for (Window w : windows) {
            WindowPeer wp = AWTAccessor.getComponentAccessor().getPeer(w);
            if (wp != null) {
                wp.setModalBlocked((Dialog)getTarget(), true);
            }
        }
    }

    // ---- PEER NOTIFICATIONS ---- //

    @Override
    public void notifyIconify(boolean iconify) {
        //The toplevel target is Frame and states are applicable to it.
        //Otherwise, the target is Window and it don't have state property.
        //Hopefully, no such events are posted in the queue so consider the
        //target as Frame in all cases.

        // REMIND: should we send it anyway if the state not changed since last
        // time?
        WindowEvent iconifyEvent = new WindowEvent(getTarget(),
                iconify ? WindowEvent.WINDOW_ICONIFIED
                        : WindowEvent.WINDOW_DEICONIFIED);
        postEvent(iconifyEvent);

        int newWindowState = iconify ? Frame.ICONIFIED : Frame.NORMAL;
        postWindowStateChangedEvent(newWindowState);

        // REMIND: RepaintManager doesn't repaint iconified windows and
        // hence ignores any repaint request during deiconification.
        // So, we need to repaint window explicitly when it becomes normal.
        if (!iconify) {
            repaintPeer();
        }
    }

    @Override
    public void notifyZoom(boolean isZoomed) {
        int newWindowState = isZoomed ? Frame.MAXIMIZED_BOTH : Frame.NORMAL;
        postWindowStateChangedEvent(newWindowState);
    }

    /**
     * Called by the {@code PlatformWindow} when any part of the window should
     * be repainted.
     */
    @Override
    public void notifyExpose(final Rectangle r) {
        repaintPeer(r);
    }

    /**
     * Called by the {@code PlatformWindow} when this window is moved/resized by
     * user or window insets are changed. There's no notifyReshape() in
     * LWComponentPeer as the only components which could be resized by user are
     * top-level windows.
     * <p>
     * We need to update the target and post the events, if the peer was moved
     * or resized, or if the target is out of sync with this peer.
     */
    @Override
    public void notifyReshape(int x, int y, int w, int h) {
        final Rectangle pBounds = getBounds();
        final boolean invalid = updateInsets(platformWindow.getInsets());
        final boolean pMoved = (x != pBounds.x) || (y != pBounds.y);
        final boolean pResized = (w != pBounds.width) || (h != pBounds.height);

        final ComponentAccessor accessor = AWTAccessor.getComponentAccessor();
        final Rectangle tBounds = accessor.getBounds(getTarget());
        final boolean tMoved = (x != tBounds.x) || (y != tBounds.y);
        final boolean tResized = (w != tBounds.width) || (h != tBounds.height);

        // Check if anything changed
        if (!tMoved && !tResized && !pMoved && !pResized && !invalid) {
            // Native window(NSWindow)/LWWindowPeer/Target are in sync
            return;
        }
        // First, update peer's bounds
        setBounds(x, y, w, h, SET_BOUNDS, false, false);

        // Second, update the graphics config and surface data
        final boolean isNewDevice = updateGraphicsDevice();
        if (isNewDevice && !isMaximizedBoundsSet()) {
            setPlatformMaximizedBounds(getDefaultMaximizedBounds());
        }

        if (pResized || isNewDevice || invalid) {
            replaceSurfaceData();
            updateMinimumSize();
        }

        // Third, COMPONENT_MOVED/COMPONENT_RESIZED/PAINT events
        if (tMoved || pMoved || invalid) {
            handleMove(x, y, true);
        }
        if (tResized || pResized || invalid || isNewDevice) {
            handleResize(w, h, true);
            repaintPeer();
        }

        repositionSecurityWarning();
    }

    private void clearBackground(final int w, final int h) {
        final Graphics g = getOnscreenGraphics(getForeground(), getBackground(),
                                               getFont());
        if (g != null) {
            try {
                if (g instanceof Graphics2D) {
                    ((Graphics2D) g).setComposite(AlphaComposite.Src);
                }
                if (isTranslucent()) {
                    g.setColor(nonOpaqueBackground);
                    g.fillRect(0, 0, w, h);
                }
                if (!isTextured()) {
                    if (g instanceof SunGraphics2D) {
                        ((SunGraphics2D) g).constrain(0, 0, w, h, getRegion());
                    }
                    g.setColor(getBackground());
                    g.fillRect(0, 0, w, h);
                }
            } finally {
                g.dispose();
            }
        }
    }

    @Override
    public void notifyUpdateCursor() {
        getLWToolkit().getCursorManager().updateCursorLater(this);
    }

    @Override
    public void notifyActivation(boolean activation, LWWindowPeer opposite) {
        Window oppositeWindow = (opposite == null)? null : opposite.getTarget();
        changeFocusedWindow(activation, oppositeWindow);
    }

    // MouseDown in non-client area
    @Override
    public void notifyNCMouseDown() {
        // Ungrab except for a click on a Dialog with the grabbing owner
        if (grabbingWindow != null &&
            !grabbingWindow.isOneOfOwnersOf(this))
        {
            grabbingWindow.ungrab();
        }
    }

    // ---- EVENTS ---- //

    /*
     * Called by the delegate to dispatch the event to Java. Event
     * coordinates are relative to non-client window are, i.e. the top-left
     * point of the client area is (insets.top, insets.left).
     */
    @Override
    public void notifyMouseEvent(int id, long when, int button,
                                 int x, int y, int absX, int absY,
                                 int modifiers, int clickCount, boolean popupTrigger,
                                 byte[] bdata)
    {
        // TODO: fill "bdata" member of AWTEvent
        Rectangle r = getBounds();
        // findPeerAt() expects parent coordinates
        LWComponentPeer<?, ?> targetPeer = findPeerAt(r.x + x, r.y + y);

        if (id == MouseEvent.MOUSE_EXITED) {
            isMouseOver = false;
            if (lastMouseEventPeer != null) {
                if (lastMouseEventPeer.isEnabled()) {
                    Point lp = lastMouseEventPeer.windowToLocal(x, y,
                            this);
                    Component target = lastMouseEventPeer.getTarget();
                    postMouseExitedEvent(target, when, modifiers, lp,
                            absX, absY, clickCount, popupTrigger, button);
                }

                // Sometimes we may get MOUSE_EXITED after lastCommonMouseEventPeer is switched
                // to a peer from another window. So we must first check if this peer is
                // the same as lastWindowPeer
                if (lastCommonMouseEventPeer != null && lastCommonMouseEventPeer.getWindowPeerOrSelf() == this) {
                    lastCommonMouseEventPeer = null;
                }
                lastMouseEventPeer = null;
            }
        } else if(id == MouseEvent.MOUSE_ENTERED) {
            isMouseOver = true;
            if (targetPeer != null) {
                if (targetPeer.isEnabled()) {
                    Point lp = targetPeer.windowToLocal(x, y, this);
                    Component target = targetPeer.getTarget();
                    postMouseEnteredEvent(target, when, modifiers, lp,
                            absX, absY, clickCount, popupTrigger, button);
                }
                lastCommonMouseEventPeer = targetPeer;
                lastMouseEventPeer = targetPeer;
            }
        } else {
            PlatformWindow topmostPlatformWindow = LWToolkit.getLWToolkit().getPlatformWindowUnderMouse();

            LWWindowPeer topmostWindowPeer =
                    topmostPlatformWindow != null ? topmostPlatformWindow.getPeer() : null;

            // topmostWindowPeer == null condition is added for the backward
            // compatibility with applets. It can be removed when the
            // getTopmostPlatformWindowUnderMouse() method will be properly
            // implemented in CPlatformEmbeddedFrame class
            if (topmostWindowPeer == this || topmostWindowPeer == null) {
                generateMouseEnterExitEventsForComponents(when, button, x, y,
                        absX, absY, modifiers, clickCount, popupTrigger,
                        targetPeer);
            } else {
                LWComponentPeer<?, ?> topmostTargetPeer = topmostWindowPeer.findPeerAt(r.x + x, r.y + y);
                topmostWindowPeer.generateMouseEnterExitEventsForComponents(when, button, x, y,
                        absX, absY, modifiers, clickCount, popupTrigger,
                        topmostTargetPeer);
            }

            // TODO: fill "bdata" member of AWTEvent

            int eventButtonMask = (button > 0)? MouseEvent.getMaskForButton(button) : 0;
            int otherButtonsPressed = modifiers & ~eventButtonMask;

            // For pressed/dragged/released events OS X treats other
            // mouse buttons as if they were BUTTON2, so we do the same
            int targetIdx = (button > 3) ? MouseEvent.BUTTON2 - 1 : button - 1;

            // MOUSE_ENTERED/EXITED are generated for the components strictly under
            // mouse even when dragging. That's why we first update lastMouseEventPeer
            // based on initial targetPeer value and only then recalculate targetPeer
            // for MOUSE_DRAGGED/RELEASED events
            if (id == MouseEvent.MOUSE_PRESSED) {

                // Ungrab only if this window is not an owned window of the grabbing one.
                if (!isGrabbing() && grabbingWindow != null &&
                    !grabbingWindow.isOneOfOwnersOf(this))
                {
                    grabbingWindow.ungrab();
                }
                if (otherButtonsPressed == 0) {
                    mouseClickButtons = eventButtonMask;
                } else {
                    mouseClickButtons |= eventButtonMask;
                }

                // The window should be focused on mouse click. If it gets activated by the native platform,
                // this request will be no op. It will take effect when:
                // 1. A simple not focused window is clicked.
                // 2. An active but not focused owner frame/dialog is clicked.
                // The mouse event then will trigger a focus request "in window" to the component, so the window
                // should gain focus before.
                requestWindowFocus(FocusEvent.Cause.MOUSE_EVENT);

                mouseDownTarget[targetIdx] = targetPeer;
            } else if (id == MouseEvent.MOUSE_DRAGGED) {
                // Cocoa dragged event has the information about which mouse
                // button is being dragged. Use it to determine the peer that
                // should receive the dragged event.
                targetPeer = mouseDownTarget[targetIdx];
                mouseClickButtons &= ~modifiers;
            } else if (id == MouseEvent.MOUSE_RELEASED) {
                // TODO: currently, mouse released event goes to the same component
                // that received corresponding mouse pressed event. For most cases,
                // it's OK, however, we need to make sure that our behavior is consistent
                // with 1.6 for cases where component in question have been
                // hidden/removed in between of mouse pressed/released events.
                targetPeer = mouseDownTarget[targetIdx];

                if ((modifiers & eventButtonMask) == 0) {
                    mouseDownTarget[targetIdx] = null;
                }

                // mouseClickButtons is updated below, after MOUSE_CLICK is sent
            }

            if (targetPeer == null) {
                //TODO This can happen if this window is invisible. this is correct behavior in this case?
                targetPeer = this;
            }


            Point lp = targetPeer.windowToLocal(x, y, this);
            if (targetPeer.isEnabled()) {
                MouseEvent event = new MouseEvent(targetPeer.getTarget(), id,
                                                  when, modifiers, lp.x, lp.y,
                                                  absX, absY, clickCount,
                                                  popupTrigger, button);
                postEvent(event);
            }

            if (id == MouseEvent.MOUSE_RELEASED) {
                if ((mouseClickButtons & eventButtonMask) != 0
                    && targetPeer.isEnabled()) {
                    postEvent(new MouseEvent(targetPeer.getTarget(),
                                             MouseEvent.MOUSE_CLICKED,
                                             when, modifiers,
                                             lp.x, lp.y, absX, absY,
                                             clickCount, popupTrigger, button));
                }
                mouseClickButtons &= ~eventButtonMask;
            }
        }
        notifyUpdateCursor();
    }

    private void generateMouseEnterExitEventsForComponents(long when,
            int button, int x, int y, int screenX, int screenY,
            int modifiers, int clickCount, boolean popupTrigger,
            final LWComponentPeer<?, ?> targetPeer) {

        if (!isMouseOver || targetPeer == lastMouseEventPeer) {
            return;
        }

        // Generate Mouse Exit for components
        if (lastMouseEventPeer != null && lastMouseEventPeer.isEnabled()) {
            Point oldp = lastMouseEventPeer.windowToLocal(x, y, this);
            Component target = lastMouseEventPeer.getTarget();
            postMouseExitedEvent(target, when, modifiers, oldp, screenX, screenY,
                    clickCount, popupTrigger, button);
        }
        lastCommonMouseEventPeer = targetPeer;
        lastMouseEventPeer = targetPeer;

        // Generate Mouse Enter for components
        if (targetPeer != null && targetPeer.isEnabled()) {
            Point newp = targetPeer.windowToLocal(x, y, this);
            Component target = targetPeer.getTarget();
            postMouseEnteredEvent(target, when, modifiers, newp, screenX, screenY, clickCount, popupTrigger, button);
        }
    }

    private void postMouseEnteredEvent(Component target, long when, int modifiers,
                                       Point loc, int xAbs, int yAbs,
                                       int clickCount, boolean popupTrigger, int button) {

        updateSecurityWarningVisibility();

        postEvent(new MouseEvent(target,
                MouseEvent.MOUSE_ENTERED,
                when, modifiers,
                loc.x, loc.y, xAbs, yAbs,
                clickCount, popupTrigger, button));
    }

    private void postMouseExitedEvent(Component target, long when, int modifiers,
                                      Point loc, int xAbs, int yAbs,
                                      int clickCount, boolean popupTrigger, int button) {

        updateSecurityWarningVisibility();

        postEvent(new MouseEvent(target,
                MouseEvent.MOUSE_EXITED,
                when, modifiers,
                loc.x, loc.y, xAbs, yAbs,
                clickCount, popupTrigger, button));
    }

    @Override
    public void notifyMouseWheelEvent(long when, int x, int y, int absX,
                                      int absY, int modifiers, int scrollType,
                                      int scrollAmount, int wheelRotation,
                                      double preciseWheelRotation, byte[] bdata)
    {
        // TODO: could we just use the last mouse event target here?
        Rectangle r = getBounds();
        // findPeerAt() expects parent coordinates
        final LWComponentPeer<?, ?> targetPeer = findPeerAt(r.x + x, r.y + y);
        if (targetPeer == null || !targetPeer.isEnabled()) {
            return;
        }

        Point lp = targetPeer.windowToLocal(x, y, this);
        // TODO: fill "bdata" member of AWTEvent
        postEvent(new MouseWheelEvent(targetPeer.getTarget(),
                                      MouseEvent.MOUSE_WHEEL,
                                      when, modifiers,
                                      lp.x, lp.y,
                                      absX, absY, /* absX, absY */
                                      0 /* clickCount */, false /* popupTrigger */,
                                      scrollType, scrollAmount,
                                      wheelRotation, preciseWheelRotation));
    }

    /*
     * Called by the delegate when a key is pressed.
     */
    @Override
    public void notifyKeyEvent(int id, long when, int modifiers,
                               int keyCode, char keyChar, int keyLocation)
    {
        LWKeyboardFocusManagerPeer kfmPeer = LWKeyboardFocusManagerPeer.getInstance();
        Component focusOwner = kfmPeer.getCurrentFocusOwner();

        if (focusOwner == null) {
            focusOwner = kfmPeer.getCurrentFocusedWindow();
            if (focusOwner == null) {
                focusOwner = this.getTarget();
            }
        }

        KeyEvent keyEvent = new KeyEvent(focusOwner, id, when, modifiers,
            keyCode, keyChar, keyLocation);
        AWTAccessor.getKeyEventAccessor().setExtendedKeyCode(keyEvent,
                (keyChar == KeyEvent.CHAR_UNDEFINED) ? keyCode
                : ExtendedKeyCodes.getExtendedKeyCodeForChar(keyChar));
        postEvent(keyEvent);
    }

    // ---- UTILITY METHODS ---- //

    private void activateDisplayListener() {
        final GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        ((SunGraphicsEnvironment) ge).addDisplayChangedListener(this);
    }

    private void deactivateDisplayListener() {
        final GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        ((SunGraphicsEnvironment) ge).removeDisplayChangedListener(this);
    }

    private void postWindowStateChangedEvent(int newWindowState) {
        if (getTarget() instanceof Frame) {
            AWTAccessor.getFrameAccessor().setExtendedState(
                    (Frame)getTarget(), newWindowState);
        }

        WindowEvent stateChangedEvent = new WindowEvent(getTarget(),
                WindowEvent.WINDOW_STATE_CHANGED,
                windowState, newWindowState);
        postEvent(stateChangedEvent);
        windowState = newWindowState;

        updateSecurityWarningVisibility();
    }

    private static int getGraphicsConfigScreen(GraphicsConfiguration gc) {
        // TODO: this method can be implemented in a more
        // efficient way by forwarding to the delegate
        GraphicsDevice gd = gc.getDevice();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gds = ge.getScreenDevices();
        for (int i = 0; i < gds.length; i++) {
            if (gds[i] == gd) {
                return i;
            }
        }
        // Should never happen if gc is a screen device config
        return 0;
    }

    /*
     * This method is called when window's graphics config is changed from
     * the app code (e.g. when the window is made non-opaque) or when
     * the window is moved to another screen by user.
     *
     * Returns true if the graphics config has been changed, false otherwise.
     */
    private boolean setGraphicsConfig(GraphicsConfiguration gc) {
        synchronized (getStateLock()) {
            if (graphicsConfig == gc) {
                return false;
            }
            // If window's graphics config is changed from the app code, the
            // config correspond to the same device as before; when the window
            // is moved by user, graphicsDevice is updated in notifyReshape().
            // In either case, there's nothing to do with screenOn here
            graphicsConfig = gc;
        }
        // SurfaceData is replaced later in updateGraphicsData()
        return true;
    }

    /**
     * Returns true if the GraphicsDevice has been changed, false otherwise.
     */
    public boolean updateGraphicsDevice() {
        GraphicsDevice newGraphicsDevice = platformWindow.getGraphicsDevice();
        synchronized (getStateLock()) {
            if (graphicsDevice == newGraphicsDevice) {
                return false;
            }
            graphicsDevice = newGraphicsDevice;
        }

        final GraphicsConfiguration newGC = newGraphicsDevice.getDefaultConfiguration();

        if (!setGraphicsConfig(newGC)) return false;

        SunToolkit.executeOnEventHandlerThread(getTarget(), new Runnable() {
            public void run() {
                AWTAccessor.getComponentAccessor().setGraphicsConfiguration(getTarget(), newGC);
            }
        });
        return true;
    }

    @Override
    public final void displayChanged() {
        if (updateGraphicsDevice()) {
            updateMinimumSize();
            if (!isMaximizedBoundsSet()) {
                setPlatformMaximizedBounds(getDefaultMaximizedBounds());
            }
        }
        // Replace surface unconditionally, because internal state of the
        // GraphicsDevice could be changed.
        replaceSurfaceData();
        repaintPeer();
    }

    @Override
    public final void paletteChanged() {
        // components do not need to react to this event.
    }

    /*
     * May be called by delegate to provide SD to Java2D code.
     */
    public SurfaceData getSurfaceData() {
        synchronized (surfaceDataLock) {
            return surfaceData;
        }
    }

    private void replaceSurfaceData() {
        replaceSurfaceData(true);
    }

    private void replaceSurfaceData(final boolean blit) {
        synchronized (surfaceDataLock) {
            final SurfaceData oldData = getSurfaceData();
            surfaceData = platformWindow.replaceSurfaceData();
            final Rectangle size = getSize();
            if (getSurfaceData() != null && oldData != getSurfaceData()) {
                clearBackground(size.width, size.height);
            }

            if (blit) {
                blitSurfaceData(oldData, getSurfaceData());
            }

            if (oldData != null && oldData != getSurfaceData()) {
                // TODO: drop oldData for D3D/WGL pipelines
                // This can only happen when this peer is being created
                oldData.flush();
            }
        }
        flushOnscreenGraphics();
    }

    private void blitSurfaceData(final SurfaceData src, final SurfaceData dst) {
        //TODO blit. proof-of-concept
        if (src != dst && src != null && dst != null
            && !(dst instanceof NullSurfaceData)
            && !(src instanceof NullSurfaceData)
            && src.getSurfaceType().equals(dst.getSurfaceType())
            && src.getDefaultScaleX() == dst.getDefaultScaleX()
            && src.getDefaultScaleY() == dst.getDefaultScaleY())
        {
            final Rectangle size = src.getBounds();
            final Blit blit = Blit.locate(src.getSurfaceType(),
                                          CompositeType.Src,
                                          dst.getSurfaceType());
            if (blit != null) {
                blit.Blit(src, dst, AlphaComposite.Src, null, 0, 0, 0, 0,
                          size.width, size.height);
            }
        }
    }

    /**
     * Request the window insets from the delegate and compares it with the
     * current one. This method is mostly called by the delegate, e.g. when the
     * window state is changed and insets should be recalculated.
     * <p/>
     * This method may be called on the toolkit thread.
     */
    public final boolean updateInsets(final Insets newInsets) {
        synchronized (getStateLock()) {
            if (insets.equals(newInsets)) {
                return false;
            }
            insets = newInsets;
        }
        return true;
    }

    public static LWWindowPeer getWindowUnderCursor() {
        return lastCommonMouseEventPeer != null ? lastCommonMouseEventPeer.getWindowPeerOrSelf() : null;
    }

    public static LWComponentPeer<?, ?> getPeerUnderCursor() {
        return lastCommonMouseEventPeer;
    }

    /*
     * Requests platform to set native focus on a frame/dialog.
     * In case of a simple window, triggers appropriate java focus change.
     */
    public boolean requestWindowFocus(FocusEvent.Cause cause) {
        if (focusLog.isLoggable(PlatformLogger.Level.FINE)) {
            focusLog.fine("requesting native focus to " + this);
        }

        if (!focusAllowedFor()) {
            focusLog.fine("focus is not allowed");
            return false;
        }

        if (platformWindow.rejectFocusRequest(cause)) {
            return false;
        }

        AppContext targetAppContext = AWTAccessor.getComponentAccessor().getAppContext(getTarget());
        KeyboardFocusManager kfm = AWTAccessor.getKeyboardFocusManagerAccessor()
                .getCurrentKeyboardFocusManager(targetAppContext);
        Window currentActive = kfm.getActiveWindow();


        Window opposite = LWKeyboardFocusManagerPeer.getInstance().
            getCurrentFocusedWindow();

        // Make the owner active window.
        if (isSimpleWindow()) {
            LWWindowPeer owner = getOwnerFrameDialog(this);

            // If owner is not natively active, request native
            // activation on it w/o sending events up to java.
            if (owner != null && !owner.platformWindow.isActive()) {
                if (focusLog.isLoggable(PlatformLogger.Level.FINE)) {
                    focusLog.fine("requesting native focus to the owner " + owner);
                }
                LWWindowPeer currentActivePeer = currentActive == null ? null :
                (LWWindowPeer) AWTAccessor.getComponentAccessor().getPeer(
                        currentActive);

                // Ensure the opposite is natively active and suppress sending events.
                if (currentActivePeer != null && currentActivePeer.platformWindow.isActive()) {
                    if (focusLog.isLoggable(PlatformLogger.Level.FINE)) {
                        focusLog.fine("the opposite is " + currentActivePeer);
                    }
                    currentActivePeer.skipNextFocusChange = true;
                }
                owner.skipNextFocusChange = true;

                owner.platformWindow.requestWindowFocus();
            }

            // DKFM will synthesize all the focus/activation events correctly.
            changeFocusedWindow(true, opposite);
            return true;

        // In case the toplevel is active but not focused, change focus directly,
        // as requesting native focus on it will not have effect.
        } else if (getTarget() == currentActive && !getTarget().hasFocus()) {

            changeFocusedWindow(true, opposite);
            return true;
        }

        return platformWindow.requestWindowFocus();
    }

    protected boolean focusAllowedFor() {
        Window window = getTarget();
        // TODO: check if modal blocked
        return window.isVisible() && window.isEnabled() && isFocusableWindow();
    }

    private boolean isFocusableWindow() {
        boolean focusable  = targetFocusable;
        if (isSimpleWindow()) {
            LWWindowPeer ownerPeer = getOwnerFrameDialog(this);
            if (ownerPeer == null) {
                return false;
            }
            return focusable && ownerPeer.targetFocusable;
        }
        return focusable;
    }

    public boolean isSimpleWindow() {
        Window window = getTarget();
        return !(window instanceof Dialog || window instanceof Frame);
    }

    @Override
    public void emulateActivation(boolean activate) {
        changeFocusedWindow(activate, null);
    }

    @SuppressWarnings("deprecation")
    private boolean isOneOfOwnersOf(LWWindowPeer peer) {
        Window owner = (peer != null ? peer.getTarget().getOwner() : null);
        while (owner != null) {
            final ComponentAccessor acc = AWTAccessor.getComponentAccessor();
            if (acc.getPeer(owner) == this) {
                return true;
            }
            owner = owner.getOwner();
        }
        return false;
    }

    /*
     * Changes focused window on java level.
     */
    protected void changeFocusedWindow(boolean becomesFocused, Window opposite) {
        if (focusLog.isLoggable(PlatformLogger.Level.FINE)) {
            focusLog.fine((becomesFocused?"gaining":"loosing") + " focus window: " + this);
        }
        if (skipNextFocusChange) {
            focusLog.fine("skipping focus change");
            skipNextFocusChange = false;
            return;
        }
        if (!isFocusableWindow() && becomesFocused) {
            focusLog.fine("the window is not focusable");
            return;
        }
        if (becomesFocused) {
            synchronized (getPeerTreeLock()) {
                if (blocker != null) {
                    if (focusLog.isLoggable(PlatformLogger.Level.FINEST)) {
                        focusLog.finest("the window is blocked by " + blocker);
                    }
                    return;
                }
            }
        }

        // Note, the method is not called:
        // - when the opposite (gaining focus) window is an owned/owner window.
        // - for a simple window in any case.
        if (!becomesFocused &&
            (isGrabbing() || this.isOneOfOwnersOf(grabbingWindow)))
        {
            if (focusLog.isLoggable(PlatformLogger.Level.FINE)) {
                focusLog.fine("ungrabbing on " + grabbingWindow);
            }
            // ungrab a simple window if its owner looses activation.
            grabbingWindow.ungrab();
        }

        KeyboardFocusManagerPeer kfmPeer = LWKeyboardFocusManagerPeer.getInstance();

        if (!becomesFocused && kfmPeer.getCurrentFocusedWindow() != getTarget()) {
            // late window focus lost event - ingoring
            return;
        }

        kfmPeer.setCurrentFocusedWindow(becomesFocused ? getTarget() : null);

        int eventID = becomesFocused ? WindowEvent.WINDOW_GAINED_FOCUS : WindowEvent.WINDOW_LOST_FOCUS;
        WindowEvent windowEvent = new TimedWindowEvent(getTarget(), eventID, opposite, System.currentTimeMillis());

        // TODO: wrap in SequencedEvent
        postEvent(windowEvent);
    }

    /*
     * Retrieves the owner of the peer.
     * Note: this method returns the owner which can be activated, (i.e. the instance
     * of Frame or Dialog may be returned).
     */
    static LWWindowPeer getOwnerFrameDialog(LWWindowPeer peer) {
        Window owner = (peer != null ? peer.getTarget().getOwner() : null);
        while (owner != null && !(owner instanceof Frame || owner instanceof Dialog)) {
            owner = owner.getOwner();
        }
        return owner == null ? null : AWTAccessor.getComponentAccessor()
                                                 .getPeer(owner);
    }

    /**
     * Returns the foremost modal blocker of this window, or null.
     */
    public LWWindowPeer getBlocker() {
        synchronized (getPeerTreeLock()) {
            LWWindowPeer blocker = this.blocker;
            if (blocker == null) {
                return null;
            }
            while (blocker.blocker != null) {
                blocker = blocker.blocker;
            }
            return blocker;
        }
    }

    @Override
    public void enterFullScreenMode() {
        platformWindow.enterFullScreenMode();
        updateSecurityWarningVisibility();
    }

    @Override
    public void exitFullScreenMode() {
        platformWindow.exitFullScreenMode();
        updateSecurityWarningVisibility();
    }

    public long getLayerPtr() {
        return getPlatformWindow().getLayerPtr();
    }

    void grab() {
        if (grabbingWindow != null && !isGrabbing()) {
            grabbingWindow.ungrab();
        }
        grabbingWindow = this;
    }

    final void ungrab(boolean doPost) {
        if (isGrabbing()) {
            grabbingWindow = null;
            if (doPost) {
                postEvent(new UngrabEvent(getTarget()));
            }
        }
    }

    void ungrab() {
        ungrab(true);
    }

    private boolean isGrabbing() {
        return this == grabbingWindow;
    }

    public PeerType getPeerType() {
        return peerType;
    }

    public void updateSecurityWarningVisibility() {
        if (warningWindow == null) {
            return;
        }

        if (!isVisible()) {
            return; // The warning window should already be hidden.
        }

        boolean show = false;

        if (!platformWindow.isFullScreenMode()) {
            if (isVisible()) {
                if (LWKeyboardFocusManagerPeer.getInstance().getCurrentFocusedWindow() ==
                        getTarget()) {
                    show = true;
                }

                if (platformWindow.isUnderMouse() || warningWindow.isUnderMouse()) {
                    show = true;
                }
            }
        }

        warningWindow.setVisible(show, true);
    }

    @Override
    public String toString() {
        return super.toString() + " [target is " + getTarget() + "]";
    }
}
