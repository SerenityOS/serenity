/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTEvent;
import java.awt.AWTKeyStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.PaintEvent;
import java.awt.image.ColorModel;
import java.awt.peer.ComponentPeer;
import java.lang.ref.WeakReference;

import sun.awt.AWTAccessor;
import sun.awt.AWTAccessor.ComponentAccessor;
import sun.awt.PaintEventDispatcher;
import sun.awt.PeerEvent;
import sun.awt.SunToolkit;
import sun.awt.X11ComponentPeer;
import sun.awt.X11GraphicsConfig;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.util.logging.PlatformLogger;

class XWindow extends XBaseWindow implements X11ComponentPeer {
    private static PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11.XWindow");
    private static PlatformLogger insLog = PlatformLogger.getLogger("sun.awt.X11.insets.XWindow");
    private static PlatformLogger eventLog = PlatformLogger.getLogger("sun.awt.X11.event.XWindow");
    private static final PlatformLogger focusLog = PlatformLogger.getLogger("sun.awt.X11.focus.XWindow");
    private static PlatformLogger keyEventLog = PlatformLogger.getLogger("sun.awt.X11.kye.XWindow");
  /* If a motion comes in while a multi-click is pending,
   * allow a smudge factor so that moving the mouse by a small
   * amount does not wipe out the multi-click state variables.
   */
    private static final int AWT_MULTICLICK_SMUDGE = 4;
    // ButtonXXX events stuff
    static int lastX = 0, lastY = 0;
    static long lastTime = 0;
    static long lastButton = 0;
    static WeakReference<XWindow> lastWindowRef = null;
    static int clickCount = 0;

    // used to check if we need to re-create surfaceData.
    int oldWidth = -1;
    int oldHeight = -1;

    protected PropMwmHints mwm_hints;
    protected static XAtom wm_protocols;
    protected static XAtom wm_delete_window;
    protected static XAtom wm_take_focus;

    private boolean stateChanged; // Indicates whether the value on savedState is valid
    private int savedState; // Holds last known state of the top-level window

    XWindowAttributesData winAttr;

    protected X11GraphicsConfig graphicsConfig;
    protected AwtGraphicsConfigData graphicsConfigData;

    private boolean reparented;

    XWindow parent;

    Component target;

    private static int JAWT_LOCK_ERROR=0x00000001;
    private static int JAWT_LOCK_CLIP_CHANGED=0x00000002;
    private static int JAWT_LOCK_BOUNDS_CHANGED=0x00000004;
    private static int JAWT_LOCK_SURFACE_CHANGED=0x00000008;
    private int drawState = JAWT_LOCK_CLIP_CHANGED |
    JAWT_LOCK_BOUNDS_CHANGED |
    JAWT_LOCK_SURFACE_CHANGED;

    public static final String TARGET = "target",
        REPARENTED = "reparented"; // whether it is reparented by default

    SurfaceData surfaceData;

    XRepaintArea paintArea;

    // fallback default font object
    private static Font defaultFont;

    static synchronized Font getDefaultFont() {
        if (null == defaultFont) {
            defaultFont = new Font(Font.DIALOG, Font.PLAIN, 12);
        }
        return defaultFont;
    }

    /* A bitmask keeps the button's numbers as Button1Mask, Button2Mask, Button3Mask
     * which are allowed to
     * generate the CLICK event after the RELEASE has happened.
     * There are conditions that must be true for that sending CLICK event:
     * 1) button was initially PRESSED
     * 2) no movement or drag has happened until RELEASE
    */
    private int mouseButtonClickAllowed = 0;

    private static native void initIDs();

    static {
        initIDs();
    }

    XWindow(XCreateWindowParams params) {
        super(params);
    }

    XWindow() {
    }

    XWindow(long parentWindow, Rectangle bounds) {
        super(new XCreateWindowParams(new Object[] {
            BOUNDS, bounds,
            PARENT_WINDOW, Long.valueOf(parentWindow)}));
    }

    XWindow(Component target, long parentWindow, Rectangle bounds) {
        super(new XCreateWindowParams(new Object[] {
            BOUNDS, bounds,
            PARENT_WINDOW, Long.valueOf(parentWindow),
            TARGET, target}));
    }

    XWindow(Component target, long parentWindow) {
        this(target, parentWindow, new Rectangle(target.getBounds()));
    }

    XWindow(Component target) {
        this(target, (target.getParent() == null) ? 0 : getParentWindowID(target), new Rectangle(target.getBounds()));
    }

    XWindow(Object target) {
        this(null, 0, null);
    }

    /* This create is used by the XEmbeddedFramePeer since it has to create the window
       as a child of the netscape window. This netscape window is passed in as wid */
    XWindow(long parentWindow) {
        super(new XCreateWindowParams(new Object[] {
            PARENT_WINDOW, Long.valueOf(parentWindow),
            REPARENTED, Boolean.TRUE,
            EMBEDDED, Boolean.TRUE}));
    }

    protected void initGraphicsConfiguration() {
        graphicsConfig = (X11GraphicsConfig) target.getGraphicsConfiguration();
        graphicsConfigData = new AwtGraphicsConfigData(graphicsConfig.getAData());
    }

    void preInit(XCreateWindowParams params) {
        super.preInit(params);
        reparented = Boolean.TRUE.equals(params.get(REPARENTED));

        target = (Component)params.get(TARGET);

        initGraphicsConfiguration();

        AwtGraphicsConfigData gData = getGraphicsConfigurationData();
        X11GraphicsConfig config = (X11GraphicsConfig) getGraphicsConfiguration();
        XVisualInfo visInfo = gData.get_awt_visInfo();
        params.putIfNull(EVENT_MASK, XConstants.KeyPressMask | XConstants.KeyReleaseMask
            | XConstants.FocusChangeMask | XConstants.ButtonPressMask | XConstants.ButtonReleaseMask
            | XConstants.EnterWindowMask | XConstants.LeaveWindowMask | XConstants.PointerMotionMask
            | XConstants.ButtonMotionMask | XConstants.ExposureMask | XConstants.StructureNotifyMask);

        if (target != null) {
            params.putIfNull(BOUNDS, new Rectangle(target.getBounds()));
        } else {
            params.putIfNull(BOUNDS, new Rectangle(0, 0, MIN_SIZE, MIN_SIZE));
        }
        params.putIfNull(BORDER_PIXEL, Long.valueOf(0));
        getColorModel(); // fix 4948833: this call forces the color map to be initialized
        params.putIfNull(COLORMAP, gData.get_awt_cmap());
        params.putIfNull(DEPTH, gData.get_awt_depth());
        params.putIfNull(VISUAL_CLASS, Integer.valueOf(XConstants.InputOutput));
        params.putIfNull(VISUAL, visInfo.get_visual());
        params.putIfNull(VALUE_MASK, XConstants.CWBorderPixel | XConstants.CWEventMask | XConstants.CWColormap);
        Long parentWindow = (Long)params.get(PARENT_WINDOW);
        if (parentWindow == null || parentWindow.longValue() == 0) {
            XToolkit.awtLock();
            try {
                int screen = visInfo.get_screen();
                if (screen != -1) {
                    params.add(PARENT_WINDOW, XlibWrapper.RootWindow(XToolkit.getDisplay(), screen));
                } else {
                    params.add(PARENT_WINDOW, XToolkit.getDefaultRootWindow());
                }
            } finally {
                XToolkit.awtUnlock();
            }
        }

        paintArea = new XRepaintArea();
        if (target != null) {
            this.parent = getParentXWindowObject(target.getParent());
        }

        params.putIfNull(BACKING_STORE, XToolkit.getBackingStoreType());

        XToolkit.awtLock();
        try {
            if (wm_protocols == null) {
                wm_protocols = XAtom.get("WM_PROTOCOLS");
                wm_delete_window = XAtom.get("WM_DELETE_WINDOW");
                wm_take_focus = XAtom.get("WM_TAKE_FOCUS");
            }
        }
        finally {
            XToolkit.awtUnlock();
        }
        winAttr = new XWindowAttributesData();
        savedState = XUtilConstants.WithdrawnState;
    }

    void postInit(XCreateWindowParams params) {
        super.postInit(params);

        setWMClass(getWMClass());

        surfaceData = graphicsConfig.createSurfaceData(this);
        Color c;
        if (target != null && (c = target.getBackground()) != null) {
            // We need a version of setBackground that does not call repaint !!
            // and one that does not get overridden. The problem is that in postInit
            // we call setBackground and we don't have all the stuff initialized to
            // do a full paint for most peers. So we cannot call setBackground in postInit.
            // instead we need to call xSetBackground.
            xSetBackground(c);
        }
    }

    public GraphicsConfiguration getGraphicsConfiguration() {
        if (graphicsConfig == null) {
            initGraphicsConfiguration();
        }
        return graphicsConfig;
    }

    public AwtGraphicsConfigData getGraphicsConfigurationData() {
        if (graphicsConfigData == null) {
            initGraphicsConfiguration();
        }
        return graphicsConfigData;
    }

    protected String[] getWMClass() {
        return new String[] {XToolkit.getAWTAppClassName(),
                XToolkit.getAWTAppClassName()};
    }

    void setReparented(boolean newValue) {
        reparented = newValue;
    }

    boolean isReparented() {
        return reparented;
    }

    static long getParentWindowID(Component target) {

        Component temp = target.getParent();
        final ComponentAccessor acc = AWTAccessor.getComponentAccessor();
        ComponentPeer peer = acc.getPeer(temp);
        while (!(peer instanceof XWindow))
        {
            temp = temp.getParent();
            peer = acc.getPeer(temp);
        }

        if (peer != null && peer instanceof XWindow)
            return ((XWindow)peer).getContentWindow();
        else return 0;
    }


    static XWindow getParentXWindowObject(Component target) {
        if (target == null) return null;
        Component temp = target.getParent();
        if (temp == null) return null;
        final ComponentAccessor acc = AWTAccessor.getComponentAccessor();
        ComponentPeer peer = acc.getPeer(temp);
        if (peer == null) return null;
        while ((peer != null) && !(peer instanceof XWindow))
        {
            temp = temp.getParent();
            peer = acc.getPeer(temp);
        }
        if (peer != null && peer instanceof XWindow)
            return (XWindow) peer;
        else return null;
    }


    boolean isParentOf(XWindow win) {
        if (!(target instanceof Container) || win == null || win.getTarget() == null) {
            return false;
        }
        Container parent = AWTAccessor.getComponentAccessor().getParent(win.target);
        while (parent != null && parent != target) {
            parent = AWTAccessor.getComponentAccessor().getParent(parent);
        }
        return (parent == target);
    }

    public Object getTarget() {
        return target;
    }
    public Component getEventSource() {
        return target;
    }

    public ColorModel getColorModel(int transparency) {
        return graphicsConfig.getColorModel (transparency);
    }

    @Override
    public ColorModel getColorModel() {
        if (graphicsConfig != null) {
            return graphicsConfig.getColorModel ();
        }
        else {
            return Toolkit.getDefaultToolkit().getColorModel();
        }
    }

    Graphics getGraphics(SurfaceData surfData, Color afore, Color aback, Font afont) {
        if (surfData == null) return null;

        Component target = this.target;

        /* Fix for bug 4746122. Color and Font shouldn't be null */
        Color bgColor = aback;
        if (bgColor == null) {
            bgColor = SystemColor.window;
        }
        Color fgColor = afore;
        if (fgColor == null) {
            fgColor = SystemColor.windowText;
        }
        Font font = afont;
        if (font == null) {
            font = XWindow.getDefaultFont();
        }
        return new SunGraphics2D(surfData, fgColor, bgColor, font);
    }

    public Graphics getGraphics() {
        return getGraphics(surfaceData,
                           target.getForeground(),
                           target.getBackground(),
                           target.getFont());
    }

    @SuppressWarnings("deprecation")
    public FontMetrics getFontMetrics(Font font) {
        return Toolkit.getDefaultToolkit().getFontMetrics(font);
    }

    public Rectangle getTargetBounds() {
        return target.getBounds();
    }

    /**
     * Returns true if the event has been handled and should not be
     * posted to Java.
     */
    boolean prePostEvent(AWTEvent e) {
        return false;
    }

    static void sendEvent(final AWTEvent e) {
        // The uses of this method imply that the incoming event is system-generated
        SunToolkit.setSystemGenerated(e);
        PeerEvent pe = new PeerEvent(Toolkit.getDefaultToolkit(), new Runnable() {
                public void run() {
                    AWTAccessor.getAWTEventAccessor().setPosted(e);
                    ((Component)e.getSource()).dispatchEvent(e);
                }
            }, PeerEvent.ULTIMATE_PRIORITY_EVENT);
        if (focusLog.isLoggable(PlatformLogger.Level.FINER) && (e instanceof FocusEvent)) {
            focusLog.finer("Sending " + e);
        }
        XToolkit.postEvent(XToolkit.targetToAppContext(e.getSource()), pe);
    }


/*
 * Post an event to the event queue.
 */
// NOTE: This method may be called by privileged threads.
//       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
    void postEvent(AWTEvent event) {
        XToolkit.postEvent(XToolkit.targetToAppContext(event.getSource()), event);
    }

    static void postEventStatic(AWTEvent event) {
        XToolkit.postEvent(XToolkit.targetToAppContext(event.getSource()), event);
    }

    public void postEventToEventQueue(final AWTEvent event) {
        //fix for 6239938 : Choice drop-down does not disappear when it loses focus, on XToolkit
        if (!prePostEvent(event)) {
            //event hasn't been handled and must be posted to EventQueue
            postEvent(event);
        }
    }

    // overriden in XCanvasPeer
    protected boolean doEraseBackground() {
        return true;
    }

    // We need a version of setBackground that does not call repaint !!
    // and one that does not get overridden. The problem is that in postInit
    // we call setBackground and we don't have all the stuff initialized to
    // do a full paint for most peers. So we cannot call setBackground in postInit.
    public final void xSetBackground(Color c) {
        XToolkit.awtLock();
        try {
            winBackground(c);
            // fix for 6558510: handle sun.awt.noerasebackground flag,
            // see doEraseBackground() and preInit() methods in XCanvasPeer
            if (!doEraseBackground()) {
                return;
            }
            int pixel = surfaceData.pixelFor(c.getRGB());
            XlibWrapper.XSetWindowBackground(XToolkit.getDisplay(), getContentWindow(), pixel);
            XlibWrapper.XClearWindow(XToolkit.getDisplay(), getContentWindow());
        }
        finally {
            XToolkit.awtUnlock();
        }
    }

    public void setBackground(Color c) {
        xSetBackground(c);
    }

    Color backgroundColor;
    void winBackground(Color c) {
        backgroundColor = c;
    }

    public Color getWinBackground() {
        Color c = null;

        if (backgroundColor != null) {
            c = backgroundColor;
        } else if (parent != null) {
            c = parent.getWinBackground();
        }

        if (c instanceof SystemColor) {
            c = new Color(c.getRGB());
        }

        return c;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public final void repaint(int x, int y, int width, int height) {
        if (!isVisible() || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        Graphics g = getGraphics();
        if (g != null) {
            try {
                g.setClip(x, y, width, height);
                if (SunToolkit.isDispatchThreadForAppContext(getTarget())) {
                    paint(g); // The native and target will be painted in place.
                } else {
                    paintPeer(g);
                    postPaintEvent(target, x, y, width, height);
                }
            } finally {
                g.dispose();
            }
        }
    }

    void repaint() {
        repaint(0, 0, getWidth(), getHeight());
    }

    public void paint(final Graphics g) {
        // paint peer
        paintPeer(g);
    }

    void paintPeer(final Graphics g) {
    }
    //used by Peers to avoid flickering withing paint()
    protected void flush(){
        XToolkit.awtLock();
        try {
            XlibWrapper.XFlush(XToolkit.getDisplay());
        } finally {
            XToolkit.awtUnlock();
        }
    }

    public void popup(int x, int y, int width, int height) {
        // TBD: grab the pointer
        xSetBounds(x, y, width, height);
    }

    public void handleExposeEvent(XEvent xev) {
        super.handleExposeEvent(xev);
        XExposeEvent xe = xev.get_xexpose();
        if (isEventDisabled(xev)) {
            return;
        }

        int x = scaleDown(xe.get_x());
        int y = scaleDown(xe.get_y());
        int w = scaleDown(xe.get_width());
        int h = scaleDown(xe.get_height());

        Component target = getEventSource();
        ComponentAccessor compAccessor = AWTAccessor.getComponentAccessor();

        if (!compAccessor.getIgnoreRepaint(target)
            && compAccessor.getWidth(target) != 0
            && compAccessor.getHeight(target) != 0)
        {
            postPaintEvent(target, x, y, w, h);
        }
    }

    public void postPaintEvent(Component target, int x, int y, int w, int h) {
        PaintEvent event = PaintEventDispatcher.getPaintEventDispatcher().
            createPaintEvent(target, x, y, w, h);
        if (event != null) {
            postEventToEventQueue(event);
        }
    }

    static int getModifiers(int state, int button, int keyCode) {
        return getModifiers(state, button, keyCode, false);
    }

    static int getWheelModifiers(int state, int button) {
        return getModifiers(state, button, 0, true);
    }

    private static int getModifiers(int state, int button, int keyCode, boolean isWheelMouse) {
        int modifiers = 0;

        if (((state & XConstants.ShiftMask) != 0) ^ (keyCode == KeyEvent.VK_SHIFT)) {
            modifiers |= InputEvent.SHIFT_DOWN_MASK;
        }
        if (((state & XConstants.ControlMask) != 0) ^ (keyCode == KeyEvent.VK_CONTROL)) {
            modifiers |= InputEvent.CTRL_DOWN_MASK;
        }
        if (((state & XToolkit.metaMask) != 0) ^ (keyCode == KeyEvent.VK_META)) {
            modifiers |= InputEvent.META_DOWN_MASK;
        }
        if (((state & XToolkit.altMask) != 0) ^ (keyCode == KeyEvent.VK_ALT)) {
            modifiers |= InputEvent.ALT_DOWN_MASK;
        }
        if (((state & XToolkit.modeSwitchMask) != 0) ^ (keyCode == KeyEvent.VK_ALT_GRAPH)) {
            modifiers |= InputEvent.ALT_GRAPH_DOWN_MASK;
        }
        //InputEvent.BUTTON_DOWN_MASK array is starting from BUTTON1_DOWN_MASK on index == 0.
        // button currently reflects a real button number and starts from 1. (except NOBUTTON which is zero )

        /* this is an attempt to refactor button IDs in : MouseEvent, InputEvent, XlibWrapper and XWindow.*/

        //reflects a button number similar to MouseEvent.BUTTON1, 2, 3 etc.
        for (int i = 0; i < XConstants.buttons.length; i ++){
            //modifier should be added if :
            // 1) current button is now still in PRESSED state (means that user just pressed mouse but not released yet) or
            // 2) if Xsystem reports that "state" represents that button was just released. This only happens on RELEASE with 1,2,3 buttons.
            // ONLY one of these conditions should be TRUE to add that modifier.
            if (((state & XlibUtil.getButtonMask(i + 1)) != 0) != (button == XConstants.buttons[i])){
                //exclude wheel buttons from adding their numbers as modifiers
                if (!isWheelMouse || !isWheel(XConstants.buttons[i])) {
                    modifiers |= InputEvent.getMaskForButton(i+1);
                }
            }
        }
        return modifiers;
    }

    static boolean isWheel(int button) {
        // 4 and 5 buttons are usually considered assigned to a first wheel
        return button == XConstants.buttons[3] || button == XConstants.buttons[4];
    }
    @SuppressWarnings("deprecation")
    static int getXModifiers(AWTKeyStroke stroke) {
        int mods = stroke.getModifiers();
        int res = 0;
        if ((mods & (InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK)) != 0) {
            res |= XConstants.ShiftMask;
        }
        if ((mods & (InputEvent.CTRL_DOWN_MASK | InputEvent.CTRL_MASK)) != 0) {
            res |= XConstants.ControlMask;
        }
        if ((mods & (InputEvent.ALT_DOWN_MASK | InputEvent.ALT_MASK)) != 0) {
            res |= XToolkit.altMask;
        }
        if ((mods & (InputEvent.META_DOWN_MASK | InputEvent.META_MASK)) != 0) {
            res |= XToolkit.metaMask;
        }
        if ((mods & (InputEvent.ALT_GRAPH_DOWN_MASK | InputEvent.ALT_GRAPH_MASK)) != 0) {
            res |= XToolkit.modeSwitchMask;
        }
        return res;
    }

    static int getMouseMovementSmudge() {
        //TODO: It's possible to read corresponding settings
        return AWT_MULTICLICK_SMUDGE;
    }

    public void handleButtonPressRelease(XEvent xev) {
        super.handleButtonPressRelease(xev);
        XButtonEvent xbe = xev.get_xbutton();
        if (isEventDisabled(xev)) {
            return;
        }
        if (eventLog.isLoggable(PlatformLogger.Level.FINE)) {
            eventLog.fine(xbe.toString());
        }
        long when;
        int modifiers;
        boolean popupTrigger = false;
        int button=0;
        int lbutton = xbe.get_button();
        /*
         * Ignore the buttons above 20 due to the bit limit for
         * InputEvent.BUTTON_DOWN_MASK.
         * One more bit is reserved for FIRST_HIGH_BIT.
         */
        if (lbutton > SunToolkit.MAX_BUTTONS_SUPPORTED) {
            return;
        }
        int type = xev.get_type();
        when = xbe.get_time();
        long jWhen = System.currentTimeMillis();

        int x = scaleDown(xbe.get_x());
        int y = scaleDown(xbe.get_y());
        if (xev.get_xany().get_window() != window) {
            Point localXY = toLocal(scaleDown(xbe.get_x_root()),
                                    scaleDown(xbe.get_y_root()));
            x = localXY.x;
            y = localXY.y;
        }

        if (type == XConstants.ButtonPress) {
            //Allow this mouse button to generate CLICK event on next ButtonRelease
            mouseButtonClickAllowed |= XlibUtil.getButtonMask(lbutton);
            XWindow lastWindow = (lastWindowRef != null) ? (lastWindowRef.get()):(null);
            /*
               multiclick checking
            */
            if (eventLog.isLoggable(PlatformLogger.Level.FINEST)) {
                eventLog.finest("lastWindow = " + lastWindow + ", lastButton "
                + lastButton + ", lastTime " + lastTime + ", multiClickTime "
                + XToolkit.getMultiClickTime());
            }
            if (lastWindow == this && lastButton == lbutton && (when - lastTime) < XToolkit.getMultiClickTime()) {
                clickCount++;
            } else {
                clickCount = 1;
                lastWindowRef = new WeakReference<>(this);
                lastButton = lbutton;
                lastX = x;
                lastY = y;
            }
            lastTime = when;


            /*
               Check for popup trigger !!
            */
            popupTrigger = (lbutton == 3);
        }

        button = XConstants.buttons[lbutton - 1];

        // mapping extra buttons to numbers starting from 4.
        if ((button > XConstants.buttons[4]) && (!Toolkit.getDefaultToolkit().areExtraMouseButtonsEnabled())){
            return;
        }

        if (button > XConstants.buttons[4]){
            button -= 2;
        }

        if (!isWheel(lbutton)) {
            modifiers = getModifiers(xbe.get_state(), button, 0);
            MouseEvent me = new MouseEvent(getEventSource(),
                                           type == XConstants.ButtonPress ? MouseEvent.MOUSE_PRESSED : MouseEvent.MOUSE_RELEASED,
                                           jWhen,modifiers, x, y,
                                           scaleDown(xbe.get_x_root()),
                                           scaleDown(xbe.get_y_root()),
                                           clickCount,popupTrigger,button);

            postEventToEventQueue(me);

            if ((type == XConstants.ButtonRelease) &&
                ((mouseButtonClickAllowed & XlibUtil.getButtonMask(lbutton)) != 0) ) // No up-button in the drag-state
            {
                postEventToEventQueue(me = new MouseEvent(getEventSource(),
                                                     MouseEvent.MOUSE_CLICKED,
                                                     jWhen,
                                                     modifiers,
                                                     x, y,
                                                     scaleDown(xbe.get_x_root()),
                                                     scaleDown(xbe.get_y_root()),
                                                     clickCount,
                                                     false, button));
            }

        }
        else {
            modifiers = getWheelModifiers(xbe.get_state(), button);
            if (xev.get_type() == XConstants.ButtonPress) {
                MouseWheelEvent mwe = new MouseWheelEvent(getEventSource(),MouseEvent.MOUSE_WHEEL, jWhen,
                                                          modifiers,
                                                          x, y,
                                                          scaleDown(xbe.get_x_root()),
                                                          scaleDown(xbe.get_y_root()),
                                                          1,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,
                                                          3,button==4 ?  -1 : 1);
                postEventToEventQueue(mwe);
            }
        }

        /* Update the state variable AFTER the CLICKED event post. */
        if (type == XConstants.ButtonRelease) {
            /* Exclude this mouse button from allowed list.*/
            mouseButtonClickAllowed &= ~ XlibUtil.getButtonMask(lbutton);
        }
    }

    public void handleMotionNotify(XEvent xev) {
        super.handleMotionNotify(xev);
        XMotionEvent xme = xev.get_xmotion();
        if (isEventDisabled(xev)) {
            return;
        }

        int mouseKeyState = 0; //(xme.get_state() & (XConstants.buttonsMask[0] | XConstants.buttonsMask[1] | XConstants.buttonsMask[2]));

        //this doesn't work for extra buttons because Xsystem is sending state==0 for every extra button event.
        // we can't correct it in MouseEvent class as we done it with modifiers, because exact type (DRAG|MOVE)
        // should be passed from XWindow.
        final int buttonsNumber = XToolkit.getNumberOfButtonsForMask();

        for (int i = 0; i < buttonsNumber; i++){
            // TODO : here is the bug in WM: extra buttons doesn't have state!=0 as they should.
            if ((i != 4) && (i != 5)) {
                mouseKeyState = mouseKeyState | (xme.get_state() & XlibUtil.getButtonMask(i + 1));
            }
        }

        boolean isDragging = (mouseKeyState != 0);
        int mouseEventType = 0;

        if (isDragging) {
            mouseEventType = MouseEvent.MOUSE_DRAGGED;
        } else {
            mouseEventType = MouseEvent.MOUSE_MOVED;
        }

        /*
           Fix for 6176814 .  Add multiclick checking.
        */
        int x = scaleDown(xme.get_x());
        int y = scaleDown(xme.get_y());
        XWindow lastWindow = (lastWindowRef != null) ? (lastWindowRef.get()):(null);

        if (!(lastWindow == this &&
              (xme.get_time() - lastTime) < XToolkit.getMultiClickTime()  &&
              (Math.abs(lastX - x) < AWT_MULTICLICK_SMUDGE &&
               Math.abs(lastY - y) < AWT_MULTICLICK_SMUDGE))) {
          clickCount = 0;
          lastWindowRef = null;
          mouseButtonClickAllowed = 0;
          lastTime = 0;
          lastX = 0;
          lastY = 0;
        }

        long jWhen = System.currentTimeMillis();
        int modifiers = getModifiers(xme.get_state(), 0, 0);
        boolean popupTrigger = false;

        Component source = getEventSource();

        if (xme.get_window() != window) {
            Point localXY = toLocal(scaleDown(xme.get_x_root()),
                                    scaleDown(xme.get_y_root()));
            x = localXY.x;
            y = localXY.y;
        }
        /* Fix for 5039416.
         * According to canvas.c we shouldn't post any MouseEvent if mouse is dragging and clickCount!=0.
         */
        if ((isDragging && clickCount == 0) || !isDragging) {
            MouseEvent mme = new MouseEvent(source, mouseEventType, jWhen,
                                            modifiers, x, y,
                                            scaleDown(xme.get_x_root()),
                                            scaleDown(xme.get_y_root()),
                                            clickCount, popupTrigger, MouseEvent.NOBUTTON);
            postEventToEventQueue(mme);
        }
    }


    // REMIND: need to implement looking for disabled events
    private native boolean x11inputMethodLookupString(long event,
                                                      long[] keysymArray);

    private native boolean haveCurrentX11InputMethodInstance();

    private boolean mouseAboveMe;

    public boolean isMouseAbove() {
        synchronized (getStateLock()) {
            return mouseAboveMe;
        }
    }
    protected void setMouseAbove(boolean above) {
        synchronized (getStateLock()) {
            mouseAboveMe = above;
        }
    }

    protected void enterNotify(long window) {
        if (window == getWindow()) {
            setMouseAbove(true);
        }
    }
    protected void leaveNotify(long window) {
        if (window == getWindow()) {
            setMouseAbove(false);
        }
    }

    public void handleXCrossingEvent(XEvent xev) {
        super.handleXCrossingEvent(xev);
        XCrossingEvent xce = xev.get_xcrossing();

        if (eventLog.isLoggable(PlatformLogger.Level.FINEST)) {
            eventLog.finest(xce.toString());
        }

        if (xce.get_type() == XConstants.EnterNotify) {
            enterNotify(xce.get_window());
        } else { // LeaveNotify:
            leaveNotify(xce.get_window());
        }

        // Skip event If it was caused by a grab
        // This is needed because on displays with focus-follows-mouse on MousePress X system generates
        // two XCrossing events with mode != NormalNotify. First of them notifies that the mouse has left
        // current component. Second one notifies that it has entered into the same component.
        // This looks like the window under the mouse has actually changed and Java handle these  events
        // accordingly. This leads to impossibility to make a double click on Component (6404708)
        XWindowPeer toplevel = getToplevelXWindow();
        if (toplevel != null && !toplevel.isModalBlocked()){
            if (xce.get_mode() != XConstants.NotifyNormal) {
                // 6404708 : need update cursor in accordance with skipping Leave/EnterNotify event
                // whereas it doesn't need to handled further.
                if (xce.get_type() == XConstants.EnterNotify) {
                    XAwtState.setComponentMouseEntered(getEventSource());
                    XGlobalCursorManager.nativeUpdateCursor(getEventSource());
                } else { // LeaveNotify:
                    XAwtState.setComponentMouseEntered(null);
                }
                return;
            }
        }
        // X sends XCrossing to all hierarchy so if the edge of child equals to
        // ancestor and mouse enters child, the ancestor will get an event too.
        // From java point the event is bogus as ancestor is obscured, so if
        // the child can get java event itself, we skip it on ancestor.
        long childWnd = xce.get_subwindow();
        if (childWnd != XConstants.None) {
            XBaseWindow child = XToolkit.windowToXWindow(childWnd);
            if (child != null && child instanceof XWindow &&
                !child.isEventDisabled(xev))
            {
                return;
            }
        }

        // Remember old component with mouse to have the opportunity to send it MOUSE_EXITED.
        final Component compWithMouse = XAwtState.getComponentMouseEntered();
        if (toplevel != null) {
            if(!toplevel.isModalBlocked()){
                if (xce.get_type() == XConstants.EnterNotify) {
                    // Change XAwtState's component mouse entered to the up-to-date one before requesting
                    // to update the cursor since XAwtState.getComponentMouseEntered() is used when the
                    // cursor is updated (in XGlobalCursorManager.findHeavyweightUnderCursor()).
                    XAwtState.setComponentMouseEntered(getEventSource());
                    XGlobalCursorManager.nativeUpdateCursor(getEventSource());
                } else { // LeaveNotify:
                    XAwtState.setComponentMouseEntered(null);
                }
            } else {
                ((XComponentPeer) AWTAccessor.getComponentAccessor().getPeer(target))
                    .pSetCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }

        if (isEventDisabled(xev)) {
            return;
        }

        long jWhen = System.currentTimeMillis();
        int modifiers = getModifiers(xce.get_state(),0,0);
        int clickCount = 0;
        boolean popupTrigger = false;
        int x = scaleDown(xce.get_x());
        int y = scaleDown(xce.get_y());
        if (xce.get_window() != window) {
            Point localXY = toLocal(scaleDown(xce.get_x_root()),
                                    scaleDown(xce.get_y_root()));
            x = localXY.x;
            y = localXY.y;
        }

        // This code tracks boundary crossing and ensures MOUSE_ENTER/EXIT
        // are posted in alternate pairs
        if (compWithMouse != null) {
            MouseEvent me = new MouseEvent(compWithMouse, MouseEvent.MOUSE_EXITED,
                                           jWhen, modifiers,
                                           scaleDown(xce.get_x()),
                                           scaleDown(xce.get_y()),
                                           scaleDown(xce.get_x_root()),
                                           scaleDown(xce.get_y_root()),
                                           clickCount, popupTrigger,
                                           MouseEvent.NOBUTTON);
            postEventToEventQueue(me);
            eventLog.finest("Clearing last window ref");
            lastWindowRef = null;
        }
        if (xce.get_type() == XConstants.EnterNotify) {
            MouseEvent me = new MouseEvent(getEventSource(), MouseEvent.MOUSE_ENTERED,
                                           jWhen, modifiers,
                                           scaleDown(xce.get_x()),
                                           scaleDown(xce.get_y()),
                                           scaleDown(xce.get_x_root()),
                                           scaleDown(xce.get_y_root()),
                                           clickCount, popupTrigger,
                                           MouseEvent.NOBUTTON);
            postEventToEventQueue(me);
        }
    }

    public void doLayout(int x, int y, int width, int height) {}

    public void handleConfigureNotifyEvent(XEvent xev) {
        Rectangle oldBounds = getBounds();

        super.handleConfigureNotifyEvent(xev);
        if (insLog.isLoggable(PlatformLogger.Level.FINER)) {
            insLog.finer("Configure, {0}, event disabled: {1}",
                     xev.get_xconfigure(), isEventDisabled(xev));
        }
        if (isEventDisabled(xev)) {
            return;
        }

//  if ( Check if it's a resize, a move, or a stacking order change )
//  {
        Rectangle bounds = getBounds();
        if (!bounds.getSize().equals(oldBounds.getSize())) {
            postEventToEventQueue(new ComponentEvent(getEventSource(), ComponentEvent.COMPONENT_RESIZED));
        }
        if (!bounds.getLocation().equals(oldBounds.getLocation())) {
            postEventToEventQueue(new ComponentEvent(getEventSource(), ComponentEvent.COMPONENT_MOVED));
        }
//  }
    }

    public void handleMapNotifyEvent(XEvent xev) {
        super.handleMapNotifyEvent(xev);
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("Mapped {0}", this);
        }
        if (isEventDisabled(xev)) {
            return;
        }
        ComponentEvent ce;

        ce = new ComponentEvent(getEventSource(), ComponentEvent.COMPONENT_SHOWN);
        postEventToEventQueue(ce);
    }

    public void handleUnmapNotifyEvent(XEvent xev) {
        super.handleUnmapNotifyEvent(xev);
        if (isEventDisabled(xev)) {
            return;
        }
        ComponentEvent ce;

        ce = new ComponentEvent(target, ComponentEvent.COMPONENT_HIDDEN);
        postEventToEventQueue(ce);
    }

    private void dumpKeysymArray(XKeyEvent ev) {
        if (keyEventLog.isLoggable(PlatformLogger.Level.FINE)) {
            keyEventLog.fine("  "+Long.toHexString(XlibWrapper.XKeycodeToKeysym(XToolkit.getDisplay(), ev.get_keycode(), 0))+
                             "\n        "+Long.toHexString(XlibWrapper.XKeycodeToKeysym(XToolkit.getDisplay(), ev.get_keycode(), 1))+
                             "\n        "+Long.toHexString(XlibWrapper.XKeycodeToKeysym(XToolkit.getDisplay(), ev.get_keycode(), 2))+
                             "\n        "+Long.toHexString(XlibWrapper.XKeycodeToKeysym(XToolkit.getDisplay(), ev.get_keycode(), 3)));
        }
    }
    /**
       Return unicode character or 0 if no correspondent character found.
       Parameter is a keysym basically from keysymdef.h
       XXX: how about vendor keys? Is there some with Unicode value and not in the list?
    */
    int keysymToUnicode( long keysym, int state ) {
        return XKeysym.convertKeysym( keysym, state );
    }
    int keyEventType2Id( int xEventType ) {
        return xEventType == XConstants.KeyPress ? java.awt.event.KeyEvent.KEY_PRESSED :
               xEventType == XConstants.KeyRelease ? java.awt.event.KeyEvent.KEY_RELEASED : 0;
    }
    private static long xkeycodeToKeysym(XKeyEvent ev) {
        return XKeysym.getKeysym( ev );
    }
    private long xkeycodeToPrimaryKeysym(XKeyEvent ev) {
        return XKeysym.xkeycode2primary_keysym( ev );
    }
    private static int primaryUnicode2JavaKeycode(int uni) {
        return (uni > 0? sun.awt.ExtendedKeyCodes.getExtendedKeyCodeForChar(uni) : 0);
        //return (uni > 0? uni + 0x01000000 : 0);
    }
    void logIncomingKeyEvent(XKeyEvent ev) {
        if (keyEventLog.isLoggable(PlatformLogger.Level.FINE)) {
            keyEventLog.fine("--XWindow.java:handleKeyEvent:"+ev);
        }
        dumpKeysymArray(ev);
        if (keyEventLog.isLoggable(PlatformLogger.Level.FINE)) {
            keyEventLog.fine("XXXXXXXXXXXXXX javakeycode will be most probably:0x"+ Integer.toHexString(XKeysym.getJavaKeycodeOnly(ev)));
        }
    }
    public void handleKeyPress(XEvent xev) {
        super.handleKeyPress(xev);
        XKeyEvent ev = xev.get_xkey();
        if (eventLog.isLoggable(PlatformLogger.Level.FINE)) {
            eventLog.fine(ev.toString());
        }
        if (isEventDisabled(xev)) {
            return;
        }
        handleKeyPress(ev);
    }
    // called directly from this package, unlike handleKeyRelease.
    // un-final it if you need to override it in a subclass.
    final void handleKeyPress(XKeyEvent ev) {
        long[] keysym = new long[2];
        int unicodeKey = 0;
        keysym[0] = XConstants.NoSymbol;

        if (keyEventLog.isLoggable(PlatformLogger.Level.FINE)) {
            logIncomingKeyEvent( ev );
        }
        if ( //TODO check if there's an active input method instance
             // without calling a native method. Is it necessary though?
            haveCurrentX11InputMethodInstance()) {
            if (x11inputMethodLookupString(ev.pData, keysym)) {
                if (keyEventLog.isLoggable(PlatformLogger.Level.FINE)) {
                    keyEventLog.fine("--XWindow.java XIM did process event; return; dec keysym processed:"+(keysym[0])+
                                   "; hex keysym processed:"+Long.toHexString(keysym[0])
                                   );
                }
                return;
            }else {
                unicodeKey = keysymToUnicode( keysym[0], ev.get_state() );
                if (keyEventLog.isLoggable(PlatformLogger.Level.FINE)) {
                    keyEventLog.fine("--XWindow.java XIM did NOT process event, hex keysym:"+Long.toHexString(keysym[0])+"\n"+
                                     "                                         unicode key:"+Integer.toHexString(unicodeKey));
                }
            }
        }else  {
            // No input method instance found. For example, there's a Java Input Method.
            // Produce do-it-yourself keysym and perhaps unicode character.
            keysym[0] = xkeycodeToKeysym(ev);
            unicodeKey = keysymToUnicode( keysym[0], ev.get_state() );
            if (keyEventLog.isLoggable(PlatformLogger.Level.FINE)) {
                keyEventLog.fine("--XWindow.java XIM is absent;             hex keysym:"+Long.toHexString(keysym[0])+"\n"+
                                 "                                         unicode key:"+Integer.toHexString(unicodeKey));
            }
        }
        // Keysym should be converted to Unicode, if possible and necessary,
        // and Java KeyEvent keycode should be calculated.
        // For press we should post pressed & typed Java events.
        //
        // Press event might be not processed to this time because
        //  (1) either XIM could not handle it or
        //  (2) it was Latin 1:1 mapping.
        //
        // Preserve modifiers to get Java key code for dead keys
        boolean isDeadKey = isDeadKey(keysym[0]);
        XKeysym.Keysym2JavaKeycode jkc = isDeadKey ? XKeysym.getJavaKeycode(keysym[0])
                : XKeysym.getJavaKeycode(ev);
        if( jkc == null ) {
            jkc = new XKeysym.Keysym2JavaKeycode(java.awt.event.KeyEvent.VK_UNDEFINED, java.awt.event.KeyEvent.KEY_LOCATION_UNKNOWN);
        }

        // Take the first keysym from a keysym array associated with the XKeyevent
        // and convert it to Unicode. Then, even if a Java keycode for the keystroke
        // is undefined, we still have a guess of what has been engraved on a keytop.
        int unicodeFromPrimaryKeysym = keysymToUnicode( xkeycodeToPrimaryKeysym(ev) ,0);

        if (keyEventLog.isLoggable(PlatformLogger.Level.FINE)) {
            keyEventLog.fine(">>>Fire Event:"+
               (ev.get_type() == XConstants.KeyPress ? "KEY_PRESSED; " : "KEY_RELEASED; ")+
               "jkeycode:decimal="+jkc.getJavaKeycode()+
               ", hex=0x"+Integer.toHexString(jkc.getJavaKeycode())+"; "+
               " legacy jkeycode: decimal="+XKeysym.getLegacyJavaKeycodeOnly(ev)+
               ", hex=0x"+Integer.toHexString(XKeysym.getLegacyJavaKeycodeOnly(ev))+"; "
            );
        }

        int jkeyToReturn = XKeysym.getLegacyJavaKeycodeOnly(ev); // someway backward compatible
        int jkeyExtended = jkc.getJavaKeycode() == java.awt.event.KeyEvent.VK_UNDEFINED ?
                           primaryUnicode2JavaKeycode( unicodeFromPrimaryKeysym ) :
                             jkc.getJavaKeycode();
        postKeyEvent( java.awt.event.KeyEvent.KEY_PRESSED,
                          isDeadKey ? jkeyExtended : jkeyToReturn,
                          (unicodeKey == 0 ? java.awt.event.KeyEvent.CHAR_UNDEFINED : unicodeKey),
                          jkc.getKeyLocation(),
                          ev.get_state(),ev.getPData(), XKeyEvent.getSize(), (long)(ev.get_keycode()),
                          unicodeFromPrimaryKeysym,
                          jkeyExtended);


        if (unicodeKey > 0 && !isDeadKey) {
                if (keyEventLog.isLoggable(PlatformLogger.Level.FINE)) {
                    keyEventLog.fine("fire _TYPED on "+unicodeKey);
                }
                postKeyEvent( java.awt.event.KeyEvent.KEY_TYPED,
                              java.awt.event.KeyEvent.VK_UNDEFINED,
                              unicodeKey,
                              java.awt.event.KeyEvent.KEY_LOCATION_UNKNOWN,
                              ev.get_state(),ev.getPData(), XKeyEvent.getSize(), (long)0,
                              unicodeFromPrimaryKeysym,
                              java.awt.event.KeyEvent.VK_UNDEFINED);

        }


    }

    public void handleKeyRelease(XEvent xev) {
        super.handleKeyRelease(xev);
        XKeyEvent ev = xev.get_xkey();
        if (eventLog.isLoggable(PlatformLogger.Level.FINE)) {
            eventLog.fine(ev.toString());
        }
        if (isEventDisabled(xev)) {
            return;
        }
        handleKeyRelease(ev);
    }
    // un-private it if you need to call it from elsewhere
    private void handleKeyRelease(XKeyEvent ev) {
        int unicodeKey = 0;

        if (keyEventLog.isLoggable(PlatformLogger.Level.FINE)) {
            logIncomingKeyEvent( ev );
        }
        // Keysym should be converted to Unicode, if possible and necessary,
        // and Java KeyEvent keycode should be calculated.
        // For release we should post released event.
        //
        // Preserve modifiers to get Java key code for dead keys
        long keysym = xkeycodeToKeysym(ev);
        boolean isDeadKey = isDeadKey(keysym);
        XKeysym.Keysym2JavaKeycode jkc = isDeadKey ? XKeysym.getJavaKeycode(keysym)
                : XKeysym.getJavaKeycode(ev);
        if( jkc == null ) {
            jkc = new XKeysym.Keysym2JavaKeycode(java.awt.event.KeyEvent.VK_UNDEFINED, java.awt.event.KeyEvent.KEY_LOCATION_UNKNOWN);
        }
        if (keyEventLog.isLoggable(PlatformLogger.Level.FINE)) {
            keyEventLog.fine(">>>Fire Event:"+
               (ev.get_type() == XConstants.KeyPress ? "KEY_PRESSED; " : "KEY_RELEASED; ")+
               "jkeycode:decimal="+jkc.getJavaKeycode()+
               ", hex=0x"+Integer.toHexString(jkc.getJavaKeycode())+"; "+
               " legacy jkeycode: decimal="+XKeysym.getLegacyJavaKeycodeOnly(ev)+
               ", hex=0x"+Integer.toHexString(XKeysym.getLegacyJavaKeycodeOnly(ev))+"; "
            );
        }
        // We obtain keysym from IM and derive unicodeKey from it for KeyPress only.
        // We used to cache that value and retrieve it on KeyRelease,
        // but in case for example of a dead key+vowel pair, a vowel after a deadkey
        // might never be cached before.
        // Also, switching between keyboard layouts, we might cache a wrong letter.
        // That's why we use the same procedure as if there was no IM instance: do-it-yourself unicode.
        unicodeKey = keysymToUnicode( xkeycodeToKeysym(ev), ev.get_state() );

        // Take a first keysym from a keysym array associated with the XKeyevent
        // and convert it to Unicode. Then, even if Java keycode for the keystroke
        // is undefined, we still will have a guess of what was engraved on a keytop.
        int unicodeFromPrimaryKeysym = keysymToUnicode( xkeycodeToPrimaryKeysym(ev) ,0);

        int jkeyToReturn = XKeysym.getLegacyJavaKeycodeOnly(ev); // someway backward compatible
        int jkeyExtended = jkc.getJavaKeycode() == java.awt.event.KeyEvent.VK_UNDEFINED ?
                           primaryUnicode2JavaKeycode( unicodeFromPrimaryKeysym ) :
                             jkc.getJavaKeycode();
        postKeyEvent(  java.awt.event.KeyEvent.KEY_RELEASED,
                          isDeadKey ? jkeyExtended : jkeyToReturn,
                          (unicodeKey == 0 ? java.awt.event.KeyEvent.CHAR_UNDEFINED : unicodeKey),
                          jkc.getKeyLocation(),
                          ev.get_state(),ev.getPData(), XKeyEvent.getSize(), (long)(ev.get_keycode()),
                          unicodeFromPrimaryKeysym,
                          jkeyExtended);


    }


    private boolean isDeadKey(long keysym){
        return XKeySymConstants.XK_dead_grave <= keysym && keysym <= XKeySymConstants.XK_dead_semivoiced_sound;
    }

    /*
     * XmNiconic and Map/UnmapNotify (that XmNiconic relies on) are
     * unreliable, since mapping changes can happen for a virtual desktop
     * switch or MacOS style shading that became quite popular under X as
     * well.  Yes, it probably should not be this way, as it violates
     * ICCCM, but reality is that quite a lot of window managers abuse
     * mapping state.
     */
    int getWMState() {
        if (stateChanged) {
            stateChanged = false;
            WindowPropertyGetter getter =
                new WindowPropertyGetter(window, XWM.XA_WM_STATE, 0, 1, false,
                                         XWM.XA_WM_STATE);
            try {
                int status = getter.execute();
                if (status != XConstants.Success || getter.getData() == 0) {
                    return savedState = XUtilConstants.WithdrawnState;
                }

                if (getter.getActualType() != XWM.XA_WM_STATE.getAtom() && getter.getActualFormat() != 32) {
                    return savedState = XUtilConstants.WithdrawnState;
                }
                savedState = (int)Native.getCard32(getter.getData());
            } finally {
                getter.dispose();
            }
        }
        return savedState;
    }

    /**
     * Override this methods to get notifications when top-level window state changes. The state is
     * meant in terms of ICCCM: WithdrawnState, IconicState, NormalState
     */
    protected void stateChanged(long time, int oldState, int newState) {
    }

    @Override
    public void handlePropertyNotify(XEvent xev) {
        super.handlePropertyNotify(xev);
        XPropertyEvent ev = xev.get_xproperty();
        if (ev.get_atom() == XWM.XA_WM_STATE.getAtom()) {
            // State has changed, invalidate saved value
            stateChanged = true;
            stateChanged(ev.get_time(), savedState, getWMState());
        }
    }

    public void reshape(Rectangle bounds) {
        reshape(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public void reshape(int x, int y, int width, int height) {
        if (width <= 0) {
            width = 1;
        }
        if (height <= 0) {
            height = 1;
        }
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        xSetBounds(x, y, width, height);
        // Fixed 6322593, 6304251, 6315137:
        // XWindow's SurfaceData should be invalidated and recreated as part
        // of the process of resizing the window
        // see the evaluation of the bug 6304251 for more information
        validateSurface();
        layout();
    }

    public void layout() {}

    boolean isShowing() {
        return visible;
    }

    boolean isResizable() {
        return true;
    }

    boolean isLocationByPlatform() {
        return false;
    }

    void updateSizeHints() {
        updateSizeHints(x, y, width, height);
    }

    void updateSizeHints(int x, int y, int width, int height) {
        long flags = XUtilConstants.PSize | (isLocationByPlatform() ? 0 : (XUtilConstants.PPosition | XUtilConstants.USPosition));
        if (!isResizable()) {
            if (log.isLoggable(PlatformLogger.Level.FINER)) {
                log.finer("Window {0} is not resizable", this);
            }
            flags |= XUtilConstants.PMinSize | XUtilConstants.PMaxSize;
        } else {
            if (log.isLoggable(PlatformLogger.Level.FINER)) {
                log.finer("Window {0} is resizable", this);
            }
        }
        setSizeHints(flags, x, y, width, height);
    }

    void updateSizeHints(int x, int y) {
        long flags = isLocationByPlatform() ? 0 : (XUtilConstants.PPosition | XUtilConstants.USPosition);
        if (!isResizable()) {
            if (log.isLoggable(PlatformLogger.Level.FINER)) {
                log.finer("Window {0} is not resizable", this);
            }
            flags |= XUtilConstants.PMinSize | XUtilConstants.PMaxSize | XUtilConstants.PSize;
        } else {
            if (log.isLoggable(PlatformLogger.Level.FINER)) {
                log.finer("Window {0} is resizable", this);
            }
        }
        setSizeHints(flags, x, y, width, height);
    }

    void validateSurface() {
        if ((width != oldWidth) || (height != oldHeight)) {
            doValidateSurface();

            oldWidth = width;
            oldHeight = height;
        }
    }

    final void doValidateSurface() {
        SurfaceData oldData = surfaceData;
        if (oldData != null) {
            surfaceData = graphicsConfig.createSurfaceData(this);
            oldData.invalidate();
        }
    }

    public SurfaceData getSurfaceData() {
        return surfaceData;
    }

    public void dispose() {
        SurfaceData oldData = surfaceData;
        surfaceData = null;
        if (oldData != null) {
            oldData.invalidate();
        }
        XToolkit.targetDisposedPeer(target, this);
        destroy();
    }

    public Point getLocationOnScreen() {
        synchronized (target.getTreeLock()) {
            Component comp = target;

            while (comp != null && !(comp instanceof Window)) {
                comp = AWTAccessor.getComponentAccessor().getParent(comp);
            }

            // applets, embedded, etc - translate directly
            // XXX: override in subclass?
            if (comp == null || comp instanceof sun.awt.EmbeddedFrame) {
                return toGlobal(0, 0);
            }

            XToolkit.awtLock();
            try {
                Object wpeer = XToolkit.targetToPeer(comp);
                if (wpeer == null
                    || !(wpeer instanceof XDecoratedPeer)
                    || ((XDecoratedPeer)wpeer).configure_seen)
                {
                    return toGlobal(0, 0);
                }

                // wpeer is an XDecoratedPeer not yet fully adopted by WM
                Point pt = toOtherWindow(getContentWindow(),
                                         ((XDecoratedPeer)wpeer).getContentWindow(),
                                         0, 0);

                if (pt == null) {
                    pt = new Point(((XBaseWindow)wpeer).getAbsoluteX(), ((XBaseWindow)wpeer).getAbsoluteY());
                }
                pt.x += comp.getX();
                pt.y += comp.getY();
                return pt;
            } finally {
                XToolkit.awtUnlock();
            }
        }
    }


    static void setBData(KeyEvent e, byte[] data) {
        AWTAccessor.getAWTEventAccessor().setBData(e, data);
    }

    public void postKeyEvent(int id, int keyCode, int keyChar,
        int keyLocation, int state, long event, int eventSize, long rawCode,
        int unicodeFromPrimaryKeysym, int extendedKeyCode)

    {
        long jWhen = System.currentTimeMillis();
        int modifiers = getModifiers(state, 0, keyCode);

        KeyEvent ke = new KeyEvent(getEventSource(), id, jWhen,
                                   modifiers, keyCode, (char)keyChar, keyLocation);
        if (event != 0) {
            byte[] data = Native.toBytes(event, eventSize);
            setBData(ke, data);
        }

        AWTAccessor.KeyEventAccessor kea = AWTAccessor.getKeyEventAccessor();
        kea.setRawCode(ke, rawCode);
        kea.setPrimaryLevelUnicode(ke, (long)unicodeFromPrimaryKeysym);
        kea.setExtendedKeyCode(ke, (long)extendedKeyCode);
        postEventToEventQueue(ke);
    }

    static native int getAWTKeyCodeForKeySym(int keysym);
    static native int getKeySymForAWTKeyCode(int keycode);

    /* These two methods are actually applicable to toplevel windows only.
     * However, the functionality is required by both the XWindowPeer and
     * XWarningWindow, both of which have the XWindow as a common ancestor.
     * See XWM.setMotifDecor() for details.
     */
    public PropMwmHints getMWMHints() {
        if (mwm_hints == null) {
            mwm_hints = new PropMwmHints();
            if (!XWM.XA_MWM_HINTS.getAtomData(getWindow(), mwm_hints.pData, MWMConstants.PROP_MWM_HINTS_ELEMENTS)) {
                mwm_hints.zero();
            }
        }
        return mwm_hints;
    }

    public void setMWMHints(PropMwmHints hints) {
        mwm_hints = hints;
        if (hints != null) {
            XWM.XA_MWM_HINTS.setAtomData(getWindow(), mwm_hints.pData, MWMConstants.PROP_MWM_HINTS_ELEMENTS);
        }
    }

    protected final void initWMProtocols() {
        wm_protocols.setAtomListProperty(this, getWMProtocols());
    }

    /**
     * Returns list of protocols which should be installed on this window.
     * Descendants can override this method to add class-specific protocols
     */
    protected XAtomList getWMProtocols() {
        // No protocols on simple window
        return new XAtomList();
    }

    /**
     * Indicates if the window is currently in the FSEM.
     * Synchronization: state lock.
     */
    private boolean fullScreenExclusiveModeState = false;

    // Implementation of the X11ComponentPeer
    @Override
    public void setFullScreenExclusiveModeState(boolean state) {
        synchronized (getStateLock()) {
            fullScreenExclusiveModeState = state;
        }
    }

    public final boolean isFullScreenExclusiveMode() {
        synchronized (getStateLock()) {
            return fullScreenExclusiveModeState;
        }
    }

    @Override
    protected int getScale() {
        return graphicsConfig.getScale();
    }

    @Override
    protected int scaleUp(int x) {
        return graphicsConfig.scaleUp(x);
    }

    @Override
    protected int scaleDown(int x) {
        return graphicsConfig.scaleDown(x);
    }
}
