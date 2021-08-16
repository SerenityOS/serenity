/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing;


import java.awt.*;
import java.awt.event.*;
import java.awt.image.VolatileImage;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.applet.*;

import jdk.internal.access.JavaSecurityAccess;
import jdk.internal.access.SharedSecrets;
import sun.awt.AWTAccessor;
import sun.awt.AppContext;
import sun.awt.DisplayChangedListener;
import sun.awt.SunToolkit;
import sun.java2d.SunGraphicsEnvironment;
import sun.security.action.GetPropertyAction;

import com.sun.java.swing.SwingUtilities3;
import java.awt.geom.AffineTransform;
import sun.java2d.SunGraphics2D;
import sun.java2d.pipe.Region;
import sun.swing.SwingAccessor;
import sun.swing.SwingUtilities2;
import sun.swing.SwingUtilities2.RepaintListener;

/**
 * This class manages repaint requests, allowing the number
 * of repaints to be minimized, for example by collapsing multiple
 * requests into a single repaint for members of a component tree.
 * <p>
 * As of 1.6 <code>RepaintManager</code> handles repaint requests
 * for Swing's top level components (<code>JApplet</code>,
 * <code>JWindow</code>, <code>JFrame</code> and <code>JDialog</code>).
 * Any calls to <code>repaint</code> on one of these will call into the
 * appropriate <code>addDirtyRegion</code> method.
 *
 * @author Arnaud Weber
 * @since 1.2
 */
public class RepaintManager
{
    /**
     * Whether or not the RepaintManager should handle paint requests
     * for top levels.
     */
    static final boolean HANDLE_TOP_LEVEL_PAINT;

    private static final short BUFFER_STRATEGY_NOT_SPECIFIED = 0;
    private static final short BUFFER_STRATEGY_SPECIFIED_ON = 1;
    private static final short BUFFER_STRATEGY_SPECIFIED_OFF = 2;

    private static final short BUFFER_STRATEGY_TYPE;

    /**
     * Maps from GraphicsConfiguration to VolatileImage.
     */
    private Map<GraphicsConfiguration,VolatileImage> volatileMap = new
                        HashMap<GraphicsConfiguration,VolatileImage>(1);

    //
    // As of 1.6 Swing handles scheduling of paint events from native code.
    // That is, SwingPaintEventDispatcher is invoked on the toolkit thread,
    // which in turn invokes nativeAddDirtyRegion.  Because this is invoked
    // from the native thread we can not invoke any public methods and so
    // we introduce these added maps.  So, any time nativeAddDirtyRegion is
    // invoked the region is added to hwDirtyComponents and a work request
    // is scheduled.  When the work request is processed all entries in
    // this map are pushed to the real map (dirtyComponents) and then
    // painted with the rest of the components.
    //
    private Map<Container,Rectangle> hwDirtyComponents;

    private Map<Component,Rectangle> dirtyComponents;
    private Map<Component,Rectangle> tmpDirtyComponents;
    private java.util.List<Component> invalidComponents;

    // List of Runnables that need to be processed before painting from AWT.
    private java.util.List<Runnable> runnableList;

    boolean   doubleBufferingEnabled = true;

    private Dimension doubleBufferMaxSize;

    private boolean isCustomMaxBufferSizeSet = false;

    // Support for both the standard and volatile offscreen buffers exists to
    // provide backwards compatibility for the [rare] programs which may be
    // calling getOffScreenBuffer() and not expecting to get a VolatileImage.
    // Swing internally is migrating to use *only* the volatile image buffer.

    // Support for standard offscreen buffer
    //
    DoubleBufferInfo standardDoubleBuffer;

    /**
     * Object responsible for hanlding core paint functionality.
     */
    private PaintManager paintManager;

    private static final Object repaintManagerKey = RepaintManager.class;

    // Whether or not a VolatileImage should be used for double-buffered painting
    static boolean volatileImageBufferEnabled = true;
    /**
     * Type of VolatileImage which should be used for double-buffered
     * painting.
     */
    private static final int volatileBufferType;
    /**
     * Value of the system property awt.nativeDoubleBuffering.
     */
    private static boolean nativeDoubleBuffering;

    // The maximum number of times Swing will attempt to use the VolatileImage
    // buffer during a paint operation.
    private static final int VOLATILE_LOOP_MAX = 2;

    /**
     * Number of <code>beginPaint</code> that have been invoked.
     */
    private int paintDepth = 0;

    /**
     * Type of buffer strategy to use.  Will be one of the BUFFER_STRATEGY_
     * constants.
     */
    private short bufferStrategyType;

    //
    // BufferStrategyPaintManager has the unique characteristic that it
    // must deal with the buffer being lost while painting to it.  For
    // example, if we paint a component and show it and the buffer has
    // become lost we must repaint the whole window.  To deal with that
    // the PaintManager calls into repaintRoot, and if we're still in
    // the process of painting the repaintRoot field is set to the JRootPane
    // and after the current JComponent.paintImmediately call finishes
    // paintImmediately will be invoked on the repaintRoot.  In this
    // way we don't try to show garbage to the screen.
    //
    /**
     * True if we're in the process of painting the dirty regions.  This is
     * set to true in <code>paintDirtyRegions</code>.
     */
    private boolean painting;
    /**
     * If the PaintManager calls into repaintRoot during painting this field
     * will be set to the root.
     */
    private JComponent repaintRoot;

    /**
     * The Thread that has initiated painting.  If null it
     * indicates painting is not currently in progress.
     */
    private Thread paintThread;

    /**
     * Runnable used to process all repaint/revalidate requests.
     */
    private final ProcessingRunnable processingRunnable;

    private static final JavaSecurityAccess javaSecurityAccess =
            SharedSecrets.getJavaSecurityAccess();

    /**
     * Listener installed to detect display changes. When display changes,
     * schedules a callback to notify all RepaintManagers of the display
     * changes.
     */
    private static final DisplayChangedListener displayChangedHandler =
            new DisplayChangedHandler();

    static {
        SwingAccessor.setRepaintManagerAccessor(new SwingAccessor.RepaintManagerAccessor() {
            @Override
            public void addRepaintListener(RepaintManager rm, RepaintListener l) {
                rm.addRepaintListener(l);
            }
            @Override
            public void removeRepaintListener(RepaintManager rm, RepaintListener l) {
                rm.removeRepaintListener(l);
            }
        });

        @SuppressWarnings("removal")
        var t1 = "true".equals(AccessController.
                doPrivileged(new GetPropertyAction(
                "swing.volatileImageBufferEnabled", "true")));
        volatileImageBufferEnabled = t1;
        boolean headless = GraphicsEnvironment.isHeadless();
        if (volatileImageBufferEnabled && headless) {
            volatileImageBufferEnabled = false;
        }
        @SuppressWarnings("removal")
        var t2 = "true".equals(AccessController.doPrivileged(
                    new GetPropertyAction("awt.nativeDoubleBuffering")));
        nativeDoubleBuffering = t2;
        @SuppressWarnings("removal")
        String bs = AccessController.doPrivileged(
                          new GetPropertyAction("swing.bufferPerWindow"));
        if (headless) {
            BUFFER_STRATEGY_TYPE = BUFFER_STRATEGY_SPECIFIED_OFF;
        }
        else if (bs == null) {
            BUFFER_STRATEGY_TYPE = BUFFER_STRATEGY_NOT_SPECIFIED;
        }
        else if ("true".equals(bs)) {
            BUFFER_STRATEGY_TYPE = BUFFER_STRATEGY_SPECIFIED_ON;
        }
        else {
            BUFFER_STRATEGY_TYPE = BUFFER_STRATEGY_SPECIFIED_OFF;
        }
        @SuppressWarnings("removal")
        var t3 = "true".equals(AccessController.doPrivileged(
               new GetPropertyAction("swing.handleTopLevelPaint", "true")));
        HANDLE_TOP_LEVEL_PAINT = t3;
        GraphicsEnvironment ge = GraphicsEnvironment.
                getLocalGraphicsEnvironment();
        if (ge instanceof SunGraphicsEnvironment) {
            ((SunGraphicsEnvironment) ge).addDisplayChangedListener(
                    displayChangedHandler);
        }
        Toolkit tk = Toolkit.getDefaultToolkit();
        if ((tk instanceof SunToolkit)
                && ((SunToolkit) tk).isSwingBackbufferTranslucencySupported()) {
            volatileBufferType = Transparency.TRANSLUCENT;
        } else {
            volatileBufferType = Transparency.OPAQUE;
        }
    }

    /**
     * Return the RepaintManager for the calling thread given a Component.
     *
     * @param c a Component -- unused in the default implementation, but could
     *          be used by an overridden version to return a different RepaintManager
     *          depending on the Component
     * @return the RepaintManager object
     */
    public static RepaintManager currentManager(Component c) {
        // Note: DisplayChangedRunnable passes in null as the component, so if
        // component is ever used to determine the current
        // RepaintManager, DisplayChangedRunnable will need to be modified
        // accordingly.
        return currentManager(AppContext.getAppContext());
    }

    /**
     * Returns the RepaintManager for the specified AppContext.  If
     * a RepaintManager has not been created for the specified
     * AppContext this will return null.
     */
    static RepaintManager currentManager(AppContext appContext) {
        RepaintManager rm = (RepaintManager)appContext.get(repaintManagerKey);
        if (rm == null) {
            rm = new RepaintManager(BUFFER_STRATEGY_TYPE);
            appContext.put(repaintManagerKey, rm);
        }
        return rm;
    }

    /**
     * Return the RepaintManager for the calling thread given a JComponent.
     * <p>
    * Note: This method exists for backward binary compatibility with earlier
     * versions of the Swing library. It simply returns the result returned by
     * {@link #currentManager(Component)}.
     *
     * @param c a JComponent -- unused
     * @return the RepaintManager object
     */
    public static RepaintManager currentManager(JComponent c) {
        return currentManager((Component)c);
    }


    /**
     * Set the RepaintManager that should be used for the calling
     * thread. <b>aRepaintManager</b> will become the current RepaintManager
     * for the calling thread's thread group.
     * @param aRepaintManager  the RepaintManager object to use
     */
    public static void setCurrentManager(RepaintManager aRepaintManager) {
        if (aRepaintManager != null) {
            SwingUtilities.appContextPut(repaintManagerKey, aRepaintManager);
        } else {
            SwingUtilities.appContextRemove(repaintManagerKey);
        }
    }

    /**
     * Create a new RepaintManager instance. You rarely call this constructor.
     * directly. To get the default RepaintManager, use
     * RepaintManager.currentManager(JComponent) (normally "this").
     */
    public RepaintManager() {
        // Because we can't know what a subclass is doing with the
        // volatile image we immediately punt in subclasses.  If this
        // poses a problem we'll need a more sophisticated detection algorithm,
        // or API.
        this(BUFFER_STRATEGY_SPECIFIED_OFF);
    }

    private RepaintManager(short bufferStrategyType) {
        // If native doublebuffering is being used, do NOT use
        // Swing doublebuffering.
        doubleBufferingEnabled = !nativeDoubleBuffering;
        synchronized(this) {
            dirtyComponents = new IdentityHashMap<Component,Rectangle>();
            tmpDirtyComponents = new IdentityHashMap<Component,Rectangle>();
            this.bufferStrategyType = bufferStrategyType;
            hwDirtyComponents = new IdentityHashMap<Container,Rectangle>();
        }
        processingRunnable = new ProcessingRunnable();
    }

    private void displayChanged() {
        if (isCustomMaxBufferSizeSet) {
            clearImages();
        } else {
            // Reset buffer maximum size to get valid size from updated graphics
            // environment in getDoubleBufferMaximumSize()
            setDoubleBufferMaximumSize(null);
        }
    }

    /**
     * Mark the component as in need of layout and queue a runnable
     * for the event dispatching thread that will validate the components
     * first isValidateRoot() ancestor.
     *
     * @param invalidComponent a component
     * @see JComponent#isValidateRoot
     * @see #removeInvalidComponent
     */
    public synchronized void addInvalidComponent(JComponent invalidComponent)
    {
        RepaintManager delegate = getDelegate(invalidComponent);
        if (delegate != null) {
            delegate.addInvalidComponent(invalidComponent);
            return;
        }
        Component validateRoot =
            SwingUtilities.getValidateRoot(invalidComponent, true);

        if (validateRoot == null) {
            return;
        }

        /* Lazily create the invalidateComponents vector and add the
         * validateRoot if it's not there already.  If this validateRoot
         * is already in the vector, we're done.
         */
        if (invalidComponents == null) {
            invalidComponents = new ArrayList<Component>();
        }
        else {
            int n = invalidComponents.size();
            for(int i = 0; i < n; i++) {
                if(validateRoot == invalidComponents.get(i)) {
                    return;
                }
            }
        }
        invalidComponents.add(validateRoot);

        // Queue a Runnable to invoke paintDirtyRegions and
        // validateInvalidComponents.
        scheduleProcessingRunnable(SunToolkit.targetToAppContext(invalidComponent));
    }


    /**
     * Remove a component from the list of invalid components.
     *
     * @param component a component
     * @see #addInvalidComponent
     */
    public synchronized void removeInvalidComponent(JComponent component) {
        RepaintManager delegate = getDelegate(component);
        if (delegate != null) {
            delegate.removeInvalidComponent(component);
            return;
        }
        if(invalidComponents != null) {
            int index = invalidComponents.indexOf(component);
            if(index != -1) {
                invalidComponents.remove(index);
            }
        }
    }


    /**
     * Add a component in the list of components that should be refreshed.
     * If <i>c</i> already has a dirty region, the rectangle <i>(x,y,w,h)</i>
     * will be unioned with the region that should be redrawn.
     *
     * @see JComponent#repaint
     */
    @SuppressWarnings("removal")
    private void addDirtyRegion0(Container c, int x, int y, int w, int h) {
        /* Special cases we don't have to bother with.
         */
        if ((w <= 0) || (h <= 0) || (c == null)) {
            return;
        }

        if ((c.getWidth() <= 0) || (c.getHeight() <= 0)) {
            return;
        }

        if (extendDirtyRegion(c, x, y, w, h)) {
            // Component was already marked as dirty, region has been
            // extended, no need to continue.
            return;
        }

        /* Make sure that c and all it ancestors (up to an Applet or
         * Window) are visible.  This loop has the same effect as
         * checking c.isShowing() (and note that it's still possible
         * that c is completely obscured by an opaque ancestor in
         * the specified rectangle).
         */
        Component root = null;

        // Note: We can't synchronize around this, Frame.getExtendedState
        // is synchronized so that if we were to synchronize around this
        // it could lead to the possibility of getting locks out
        // of order and deadlocking.
        for (Container p = c; p != null; p = p.getParent()) {
            if (!p.isVisible() || !p.isDisplayable()) {
                return;
            }
            if ((p instanceof Window) || (p instanceof Applet)) {
                // Iconified frames are still visible!
                if (p instanceof Frame &&
                        (((Frame)p).getExtendedState() & Frame.ICONIFIED) ==
                                    Frame.ICONIFIED) {
                    return;
                }
                root = p;
                break;
            }
        }

        if (root == null) return;

        synchronized(this) {
            if (extendDirtyRegion(c, x, y, w, h)) {
                // In between last check and this check another thread
                // queued up runnable, can bail here.
                return;
            }
            dirtyComponents.put(c, new Rectangle(x, y, w, h));
        }

        // Queue a Runnable to invoke paintDirtyRegions and
        // validateInvalidComponents.
        scheduleProcessingRunnable(SunToolkit.targetToAppContext(c));
    }

    /**
     * Add a component in the list of components that should be refreshed.
     * If <i>c</i> already has a dirty region, the rectangle <i>(x,y,w,h)</i>
     * will be unioned with the region that should be redrawn.
     *
     * @param c Component to repaint, null results in nothing happening.
     * @param x X coordinate of the region to repaint
     * @param y Y coordinate of the region to repaint
     * @param w Width of the region to repaint
     * @param h Height of the region to repaint
     * @see JComponent#repaint
     */
    public void addDirtyRegion(JComponent c, int x, int y, int w, int h)
    {
        RepaintManager delegate = getDelegate(c);
        if (delegate != null) {
            delegate.addDirtyRegion(c, x, y, w, h);
            return;
        }
        addDirtyRegion0(c, x, y, w, h);
    }

    /**
     * Adds <code>window</code> to the list of <code>Component</code>s that
     * need to be repainted.
     *
     * @param window Window to repaint, null results in nothing happening.
     * @param x X coordinate of the region to repaint
     * @param y Y coordinate of the region to repaint
     * @param w Width of the region to repaint
     * @param h Height of the region to repaint
     * @see JFrame#repaint
     * @see JWindow#repaint
     * @see JDialog#repaint
     * @since 1.6
     */
    public void addDirtyRegion(Window window, int x, int y, int w, int h) {
        addDirtyRegion0(window, x, y, w, h);
    }

    /**
     * Adds <code>applet</code> to the list of <code>Component</code>s that
     * need to be repainted.
     *
     * @param applet Applet to repaint, null results in nothing happening.
     * @param x X coordinate of the region to repaint
     * @param y Y coordinate of the region to repaint
     * @param w Width of the region to repaint
     * @param h Height of the region to repaint
     * @see JApplet#repaint
     * @since 1.6
     *
     * @deprecated The Applet API is deprecated. See the
     * <a href="../../java/applet/package-summary.html"> java.applet package
     * documentation</a> for further information.
     */
    @Deprecated(since = "9", forRemoval = true)
    @SuppressWarnings("removal")
    public void addDirtyRegion(Applet applet, int x, int y, int w, int h) {
        addDirtyRegion0(applet, x, y, w, h);
    }

    @SuppressWarnings("removal")
    void scheduleHeavyWeightPaints() {
        Map<Container,Rectangle> hws;

        synchronized(this) {
            if (hwDirtyComponents.size() == 0) {
                return;
            }
            hws = hwDirtyComponents;
            hwDirtyComponents =  new IdentityHashMap<Container,Rectangle>();
        }
        for (Container hw : hws.keySet()) {
            Rectangle dirty = hws.get(hw);
            if (hw instanceof Window) {
                addDirtyRegion((Window)hw, dirty.x, dirty.y,
                               dirty.width, dirty.height);
            }
            else if (hw instanceof Applet) {
                addDirtyRegion((Applet)hw, dirty.x, dirty.y,
                               dirty.width, dirty.height);
            }
            else { // SwingHeavyWeight
                addDirtyRegion0(hw, dirty.x, dirty.y,
                                dirty.width, dirty.height);
            }
        }
    }

    //
    // This is called from the toolkit thread when a native expose is
    // received.
    //
    void nativeAddDirtyRegion(AppContext appContext, Container c,
                              int x, int y, int w, int h) {
        if (w > 0 && h > 0) {
            synchronized(this) {
                Rectangle dirty = hwDirtyComponents.get(c);
                if (dirty == null) {
                    hwDirtyComponents.put(c, new Rectangle(x, y, w, h));
                }
                else {
                    hwDirtyComponents.put(c, SwingUtilities.computeUnion(
                                              x, y, w, h, dirty));
                }
            }
            scheduleProcessingRunnable(appContext);
        }
    }

    //
    // This is called from the toolkit thread when awt needs to run a
    // Runnable before we paint.
    //
    void nativeQueueSurfaceDataRunnable(AppContext appContext,
                                        final Component c, final Runnable r)
    {
        synchronized(this) {
            if (runnableList == null) {
                runnableList = new LinkedList<Runnable>();
            }
            runnableList.add(new Runnable() {
                public void run() {
                    @SuppressWarnings("removal")
                    AccessControlContext stack = AccessController.getContext();
                    @SuppressWarnings("removal")
                    AccessControlContext acc =
                        AWTAccessor.getComponentAccessor().getAccessControlContext(c);
                    javaSecurityAccess.doIntersectionPrivilege(new PrivilegedAction<Void>() {
                        public Void run() {
                            r.run();
                            return null;
                        }
                    }, stack, acc);
                }
            });
        }
        scheduleProcessingRunnable(appContext);
    }

    /**
     * Extends the dirty region for the specified component to include
     * the new region.
     *
     * @return false if <code>c</code> is not yet marked dirty.
     */
    private synchronized boolean extendDirtyRegion(
        Component c, int x, int y, int w, int h) {
        Rectangle r = dirtyComponents.get(c);
        if (r != null) {
            // A non-null r implies c is already marked as dirty,
            // and that the parent is valid. Therefore we can
            // just union the rect and bail.
            SwingUtilities.computeUnion(x, y, w, h, r);
            return true;
        }
        return false;
    }

    /**
     * Return the current dirty region for a component.
     * Return an empty rectangle if the component is not
     * dirty.
     *
     * @param aComponent a component
     * @return the region
     */
    public Rectangle getDirtyRegion(JComponent aComponent) {
        RepaintManager delegate = getDelegate(aComponent);
        if (delegate != null) {
            return delegate.getDirtyRegion(aComponent);
        }
        Rectangle r;
        synchronized(this) {
            r = dirtyComponents.get(aComponent);
        }
        if(r == null)
            return new Rectangle(0,0,0,0);
        else
            return new Rectangle(r);
    }

    /**
     * Mark a component completely dirty. <b>aComponent</b> will be
     * completely painted during the next paintDirtyRegions() call.
     *
     * @param aComponent a component
     */
    public void markCompletelyDirty(JComponent aComponent) {
        RepaintManager delegate = getDelegate(aComponent);
        if (delegate != null) {
            delegate.markCompletelyDirty(aComponent);
            return;
        }
        addDirtyRegion(aComponent,0,0,Integer.MAX_VALUE,Integer.MAX_VALUE);
    }

    /**
     * Mark a component completely clean. <b>aComponent</b> will not
     * get painted during the next paintDirtyRegions() call.
     *
     * @param aComponent a component
     */
    public void markCompletelyClean(JComponent aComponent) {
        RepaintManager delegate = getDelegate(aComponent);
        if (delegate != null) {
            delegate.markCompletelyClean(aComponent);
            return;
        }
        synchronized(this) {
                dirtyComponents.remove(aComponent);
        }
    }

    /**
     * Convenience method that returns true if <b>aComponent</b> will be completely
     * painted during the next paintDirtyRegions(). If computing dirty regions is
     * expensive for your component, use this method and avoid computing dirty region
     * if it return true.
     *
     * @param aComponent a component
     * @return {@code true} if <b>aComponent</b> will be completely
     *         painted during the next paintDirtyRegions().
     */
    public boolean isCompletelyDirty(JComponent aComponent) {
        RepaintManager delegate = getDelegate(aComponent);
        if (delegate != null) {
            return delegate.isCompletelyDirty(aComponent);
        }
        Rectangle r;

        r = getDirtyRegion(aComponent);
        if(r.width == Integer.MAX_VALUE &&
           r.height == Integer.MAX_VALUE)
            return true;
        else
            return false;
    }


    /**
     * Validate all of the components that have been marked invalid.
     * @see #addInvalidComponent
     */
    public void validateInvalidComponents() {
        final java.util.List<Component> ic;
        synchronized(this) {
            if (invalidComponents == null) {
                return;
            }
            ic = invalidComponents;
            invalidComponents = null;
        }
        int n = ic.size();
        for(int i = 0; i < n; i++) {
            final Component c = ic.get(i);
            @SuppressWarnings("removal")
            AccessControlContext stack = AccessController.getContext();
            @SuppressWarnings("removal")
            AccessControlContext acc =
                AWTAccessor.getComponentAccessor().getAccessControlContext(c);
            javaSecurityAccess.doIntersectionPrivilege(
                new PrivilegedAction<Void>() {
                    public Void run() {
                        c.validate();
                        return null;
                    }
                }, stack, acc);
        }
    }


    /**
     * This is invoked to process paint requests.  It's needed
     * for backward compatibility in so far as RepaintManager would previously
     * not see paint requests for top levels, so, we have to make sure
     * a subclass correctly paints any dirty top levels.
     */
    private void prePaintDirtyRegions() {
        Map<Component,Rectangle> dirtyComponents;
        java.util.List<Runnable> runnableList;
        synchronized(this) {
            dirtyComponents = this.dirtyComponents;
            runnableList = this.runnableList;
            this.runnableList = null;
        }
        if (runnableList != null) {
            for (Runnable runnable : runnableList) {
                runnable.run();
            }
        }
        paintDirtyRegions();
        if (dirtyComponents.size() > 0) {
            // This'll only happen if a subclass isn't correctly dealing
            // with toplevels.
            paintDirtyRegions(dirtyComponents);
        }
    }

    private void updateWindows(Map<Component,Rectangle> dirtyComponents) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        if (!(toolkit instanceof SunToolkit &&
              ((SunToolkit)toolkit).needUpdateWindow()))
        {
            return;
        }

        Set<Window> windows = new HashSet<Window>();
        Set<Component> dirtyComps = dirtyComponents.keySet();
        for (Iterator<Component> it = dirtyComps.iterator(); it.hasNext();) {
            Component dirty = it.next();
            Window window = dirty instanceof Window ?
                (Window)dirty :
                SwingUtilities.getWindowAncestor(dirty);
            if (window != null &&
                !window.isOpaque())
            {
                windows.add(window);
            }
        }

        for (Window window : windows) {
            AWTAccessor.getWindowAccessor().updateWindow(window);
        }
    }

    boolean isPainting() {
        return painting;
    }

    /**
     * Paint all of the components that have been marked dirty.
     *
     * @see #addDirtyRegion
     */
    public void paintDirtyRegions() {
        synchronized(this) {  // swap for thread safety
            Map<Component,Rectangle> tmp = tmpDirtyComponents;
            tmpDirtyComponents = dirtyComponents;
            dirtyComponents = tmp;
            dirtyComponents.clear();
        }
        paintDirtyRegions(tmpDirtyComponents);
    }

    private void paintDirtyRegions(
        final Map<Component,Rectangle> tmpDirtyComponents)
    {
        if (tmpDirtyComponents.isEmpty()) {
            return;
        }

        final java.util.List<Component> roots =
            new ArrayList<Component>(tmpDirtyComponents.size());
        for (Component dirty : tmpDirtyComponents.keySet()) {
            collectDirtyComponents(tmpDirtyComponents, dirty, roots);
        }

        final AtomicInteger count = new AtomicInteger(roots.size());
        painting = true;
        try {
            for (int j=0 ; j < count.get(); j++) {
                final int i = j;
                final Component dirtyComponent = roots.get(j);
                @SuppressWarnings("removal")
                AccessControlContext stack = AccessController.getContext();
                @SuppressWarnings("removal")
                AccessControlContext acc =
                    AWTAccessor.getComponentAccessor().getAccessControlContext(dirtyComponent);
                javaSecurityAccess.doIntersectionPrivilege(new PrivilegedAction<Void>() {
                    public Void run() {
                        Rectangle rect = tmpDirtyComponents.get(dirtyComponent);
                        // Sometimes when RepaintManager is changed during the painting
                        // we may get null here, see #6995769 for details
                        if (rect == null) {
                            return null;
                        }

                        int localBoundsH = dirtyComponent.getHeight();
                        int localBoundsW = dirtyComponent.getWidth();
                        SwingUtilities.computeIntersection(0,
                                                           0,
                                                           localBoundsW,
                                                           localBoundsH,
                                                           rect);
                        if (dirtyComponent instanceof JComponent) {
                            ((JComponent)dirtyComponent).paintImmediately(
                                rect.x,rect.y,rect.width, rect.height);
                        }
                        else if (dirtyComponent.isShowing()) {
                            Graphics g = JComponent.safelyGetGraphics(
                                    dirtyComponent, dirtyComponent);
                            // If the Graphics goes away, it means someone disposed of
                            // the window, don't do anything.
                            if (g != null) {
                                g.setClip(rect.x, rect.y, rect.width, rect.height);
                                try {
                                    dirtyComponent.paint(g);
                                } finally {
                                    g.dispose();
                                }
                            }
                        }
                        // If the repaintRoot has been set, service it now and
                        // remove any components that are children of repaintRoot.
                        if (repaintRoot != null) {
                            adjustRoots(repaintRoot, roots, i + 1);
                            count.set(roots.size());
                            paintManager.isRepaintingRoot = true;
                            repaintRoot.paintImmediately(0, 0, repaintRoot.getWidth(),
                                                         repaintRoot.getHeight());
                            paintManager.isRepaintingRoot = false;
                            // Only service repaintRoot once.
                            repaintRoot = null;
                        }

                        return null;
                    }
                }, stack, acc);
            }
        } finally {
            painting = false;
        }

        updateWindows(tmpDirtyComponents);

        tmpDirtyComponents.clear();
    }


    /**
     * Removes any components from roots that are children of
     * root.
     */
    private void adjustRoots(JComponent root,
                             java.util.List<Component> roots, int index) {
        for (int i = roots.size() - 1; i >= index; i--) {
            Component c = roots.get(i);
            for(;;) {
                if (c == root || c == null || !(c instanceof JComponent)) {
                    break;
                }
                c = c.getParent();
            }
            if (c == root) {
                roots.remove(i);
            }
        }
    }

    Rectangle tmp = new Rectangle();

    void collectDirtyComponents(Map<Component,Rectangle> dirtyComponents,
                                Component dirtyComponent,
                                java.util.List<Component> roots) {
        int dx, dy, rootDx, rootDy;
        Component component, rootDirtyComponent,parent;
        Rectangle cBounds;

        // Find the highest parent which is dirty.  When we get out of this
        // rootDx and rootDy will contain the translation from the
        // rootDirtyComponent's coordinate system to the coordinates of the
        // original dirty component.  The tmp Rect is also used to compute the
        // visible portion of the dirtyRect.

        component = rootDirtyComponent = dirtyComponent;

        int x = dirtyComponent.getX();
        int y = dirtyComponent.getY();
        int w = dirtyComponent.getWidth();
        int h = dirtyComponent.getHeight();

        dx = rootDx = 0;
        dy = rootDy = 0;
        tmp.setBounds(dirtyComponents.get(dirtyComponent));

        // System.out.println("Collect dirty component for bound " + tmp +
        //                                   "component bounds is " + cBounds);;
        SwingUtilities.computeIntersection(0,0,w,h,tmp);

        if (tmp.isEmpty()) {
            // System.out.println("Empty 1");
            return;
        }

        for(;;) {
            if(!(component instanceof JComponent))
                break;

            parent = component.getParent();
            if(parent == null)
                break;

            component = parent;

            dx += x;
            dy += y;
            tmp.setLocation(tmp.x + x, tmp.y + y);

            x = component.getX();
            y = component.getY();
            w = component.getWidth();
            h = component.getHeight();
            tmp = SwingUtilities.computeIntersection(0,0,w,h,tmp);

            if (tmp.isEmpty()) {
                // System.out.println("Empty 2");
                return;
            }

            if (dirtyComponents.get(component) != null) {
                rootDirtyComponent = component;
                rootDx = dx;
                rootDy = dy;
            }
        }

        if (dirtyComponent != rootDirtyComponent) {
            Rectangle r;
            tmp.setLocation(tmp.x + rootDx - dx,
                            tmp.y + rootDy - dy);
            r = dirtyComponents.get(rootDirtyComponent);
            SwingUtilities.computeUnion(tmp.x,tmp.y,tmp.width,tmp.height,r);
        }

        // If we haven't seen this root before, then we need to add it to the
        // list of root dirty Views.

        if (!roots.contains(rootDirtyComponent))
            roots.add(rootDirtyComponent);
    }


    /**
     * Returns a string that displays and identifies this
     * object's properties.
     *
     * @return a String representation of this object
     */
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder();
        if(dirtyComponents != null)
            sb.append("" + dirtyComponents);
        return sb.toString();
    }


    /**
     * Return the offscreen buffer that should be used as a double buffer with
     * the component <code>c</code>.
     * By default there is a double buffer per RepaintManager.
     * The buffer might be smaller than <code>(proposedWidth,proposedHeight)</code>
     * This happens when the maximum double buffer size as been set for the receiving
     * repaint manager.
     *
     * @param c the component
     * @param proposedWidth the width of the buffer
     * @param proposedHeight the height of the buffer
     *
     * @return the image
     */
    public Image getOffscreenBuffer(Component c,int proposedWidth,int proposedHeight) {
        RepaintManager delegate = getDelegate(c);
        if (delegate != null) {
            return delegate.getOffscreenBuffer(c, proposedWidth, proposedHeight);
        }
        return _getOffscreenBuffer(c, proposedWidth, proposedHeight);
    }

    /**
     * Return a volatile offscreen buffer that should be used as a
     * double buffer with the specified component <code>c</code>.
     * The image returned will be an instance of VolatileImage, or null
     * if a VolatileImage object could not be instantiated.
     * This buffer might be smaller than <code>(proposedWidth,proposedHeight)</code>.
     * This happens when the maximum double buffer size has been set for this
     * repaint manager.
     *
     * @param c the component
     * @param proposedWidth the width of the buffer
     * @param proposedHeight the height of the buffer
     *
     * @return the volatile image
     * @see java.awt.image.VolatileImage
     * @since 1.4
     */
    public Image getVolatileOffscreenBuffer(Component c,
                                            int proposedWidth,int proposedHeight) {
        RepaintManager delegate = getDelegate(c);
        if (delegate != null) {
            return delegate.getVolatileOffscreenBuffer(c, proposedWidth,
                                                        proposedHeight);
        }

        // If the window is non-opaque, it's double-buffered at peer's level
        Window w = (c instanceof Window) ? (Window)c : SwingUtilities.getWindowAncestor(c);
        if (!w.isOpaque()) {
            Toolkit tk = Toolkit.getDefaultToolkit();
            if ((tk instanceof SunToolkit) && (((SunToolkit)tk).needUpdateWindow())) {
                return null;
            }
        }

        GraphicsConfiguration config = c.getGraphicsConfiguration();
        if (config == null) {
            config = GraphicsEnvironment.getLocalGraphicsEnvironment().
                            getDefaultScreenDevice().getDefaultConfiguration();
        }
        Dimension maxSize = getDoubleBufferMaximumSize();
        int width = proposedWidth < 1 ? 1 :
            (proposedWidth > maxSize.width? maxSize.width : proposedWidth);
        int height = proposedHeight < 1 ? 1 :
            (proposedHeight > maxSize.height? maxSize.height : proposedHeight);
        VolatileImage image = volatileMap.get(config);
        if (image == null || image.getWidth() < width ||
                             image.getHeight() < height) {
            if (image != null) {
                image.flush();
            }
            image = config.createCompatibleVolatileImage(width, height,
                                                         volatileBufferType);
            volatileMap.put(config, image);
        }
        return image;
    }

    private Image _getOffscreenBuffer(Component c, int proposedWidth, int proposedHeight) {
        Dimension maxSize = getDoubleBufferMaximumSize();
        DoubleBufferInfo doubleBuffer;
        int width, height;

        // If the window is non-opaque, it's double-buffered at peer's level
        Window w = (c instanceof Window) ? (Window)c : SwingUtilities.getWindowAncestor(c);
        if (!w.isOpaque()) {
            Toolkit tk = Toolkit.getDefaultToolkit();
            if ((tk instanceof SunToolkit) && (((SunToolkit)tk).needUpdateWindow())) {
                return null;
            }
        }

        if (standardDoubleBuffer == null) {
            standardDoubleBuffer = new DoubleBufferInfo();
        }
        doubleBuffer = standardDoubleBuffer;

        width = proposedWidth < 1? 1 :
                  (proposedWidth > maxSize.width? maxSize.width : proposedWidth);
        height = proposedHeight < 1? 1 :
                  (proposedHeight > maxSize.height? maxSize.height : proposedHeight);

        if (doubleBuffer.needsReset || (doubleBuffer.image != null &&
                                        (doubleBuffer.size.width < width ||
                                         doubleBuffer.size.height < height))) {
            doubleBuffer.needsReset = false;
            if (doubleBuffer.image != null) {
                doubleBuffer.image.flush();
                doubleBuffer.image = null;
            }
            width = Math.max(doubleBuffer.size.width, width);
            height = Math.max(doubleBuffer.size.height, height);
        }

        Image result = doubleBuffer.image;

        if (doubleBuffer.image == null) {
            result = c.createImage(width , height);
            doubleBuffer.size = new Dimension(width, height);
            if (c instanceof JComponent) {
                ((JComponent)c).setCreatedDoubleBuffer(true);
                doubleBuffer.image = result;
            }
            // JComponent will inform us when it is no longer valid
            // (via removeNotify) we have no such hook to other components,
            // therefore we don't keep a ref to the Component
            // (indirectly through the Image) by stashing the image.
        }
        return result;
    }


    /**
     * Set the maximum double buffer size.
     *
     * @param d the dimension
     */
    public void setDoubleBufferMaximumSize(Dimension d) {
        doubleBufferMaxSize = d;
        if (doubleBufferMaxSize == null) {
            isCustomMaxBufferSizeSet = false;
            clearImages();
        } else {
            isCustomMaxBufferSizeSet = true;
            clearImages(d.width, d.height);
        }
    }

    private void clearImages() {
        clearImages(0, 0);
    }

    private void clearImages(int width, int height) {
        if (standardDoubleBuffer != null && standardDoubleBuffer.image != null) {
            if (standardDoubleBuffer.image.getWidth(null) > width ||
                standardDoubleBuffer.image.getHeight(null) > height) {
                standardDoubleBuffer.image.flush();
                standardDoubleBuffer.image = null;
            }
        }
        // Clear out the VolatileImages
        Iterator<GraphicsConfiguration> gcs = volatileMap.keySet().iterator();
        while (gcs.hasNext()) {
            GraphicsConfiguration gc = gcs.next();
            VolatileImage image = volatileMap.get(gc);
            if (image.getWidth() > width || image.getHeight() > height) {
                image.flush();
                gcs.remove();
            }
        }
    }

    /**
     * Returns the maximum double buffer size.
     *
     * @return a Dimension object representing the maximum size
     */
    public Dimension getDoubleBufferMaximumSize() {
        if (doubleBufferMaxSize == null) {
            try {
                Rectangle virtualBounds = new Rectangle();
                GraphicsEnvironment ge = GraphicsEnvironment.
                                                 getLocalGraphicsEnvironment();
                for (GraphicsDevice gd : ge.getScreenDevices()) {
                    GraphicsConfiguration gc = gd.getDefaultConfiguration();
                    virtualBounds = virtualBounds.union(gc.getBounds());
                }
                doubleBufferMaxSize = new Dimension(virtualBounds.width,
                                                    virtualBounds.height);
            } catch (HeadlessException e) {
                doubleBufferMaxSize = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
            }
        }
        return doubleBufferMaxSize;
    }

    /**
     * Enables or disables double buffering in this RepaintManager.
     * CAUTION: The default value for this property is set for optimal
     * paint performance on the given platform and it is not recommended
     * that programs modify this property directly.
     *
     * @param aFlag  true to activate double buffering
     * @see #isDoubleBufferingEnabled
     */
    public void setDoubleBufferingEnabled(boolean aFlag) {
        doubleBufferingEnabled = aFlag;
        PaintManager paintManager = getPaintManager();
        if (!aFlag && paintManager.getClass() != PaintManager.class) {
            setPaintManager(new PaintManager());
        }
    }

    /**
     * Returns true if this RepaintManager is double buffered.
     * The default value for this property may vary from platform
     * to platform.  On platforms where native double buffering
     * is supported in the AWT, the default value will be <code>false</code>
     * to avoid unnecessary buffering in Swing.
     * On platforms where native double buffering is not supported,
     * the default value will be <code>true</code>.
     *
     * @return true if this object is double buffered
     */
    public boolean isDoubleBufferingEnabled() {
        return doubleBufferingEnabled;
    }

    /**
     * This resets the double buffer. Actually, it marks the double buffer
     * as invalid, the double buffer will then be recreated on the next
     * invocation of getOffscreenBuffer.
     */
    void resetDoubleBuffer() {
        if (standardDoubleBuffer != null) {
            standardDoubleBuffer.needsReset = true;
        }
    }

    /**
     * This resets the volatile double buffer.
     */
    void resetVolatileDoubleBuffer(GraphicsConfiguration gc) {
        Image image = volatileMap.remove(gc);
        if (image != null) {
            image.flush();
        }
    }

    /**
     * Returns true if we should use the <code>Image</code> returned
     * from <code>getVolatileOffscreenBuffer</code> to do double buffering.
     */
    boolean useVolatileDoubleBuffer() {
        return volatileImageBufferEnabled;
    }

    /**
     * Returns true if the current thread is the thread painting.  This
     * will return false if no threads are painting.
     */
    private synchronized boolean isPaintingThread() {
        return (Thread.currentThread() == paintThread);
    }
    //
    // Paint methods.  You very, VERY rarely need to invoke these.
    // They are invoked directly from JComponent's painting code and
    // when painting happens outside the normal flow: DefaultDesktopManager
    // and JViewport.  If you end up needing these methods in other places be
    // careful that you don't get stuck in a paint loop.
    //

    /**
     * Paints a region of a component
     *
     * @param paintingComponent Component to paint
     * @param bufferComponent Component to obtain buffer for
     * @param g Graphics to paint to
     * @param x X-coordinate
     * @param y Y-coordinate
     * @param w Width
     * @param h Height
     */
    void paint(JComponent paintingComponent,
               JComponent bufferComponent, Graphics g,
               int x, int y, int w, int h) {
        PaintManager paintManager = getPaintManager();
        if (!isPaintingThread()) {
            // We're painting to two threads at once.  PaintManager deals
            // with this a bit better than BufferStrategyPaintManager, use
            // it to avoid possible exceptions/corruption.
            if (paintManager.getClass() != PaintManager.class) {
                paintManager = new PaintManager();
                paintManager.repaintManager = this;
            }
        }
        if (!paintManager.paint(paintingComponent, bufferComponent, g,
                                x, y, w, h)) {
            g.setClip(x, y, w, h);
            paintingComponent.paintToOffscreen(g, x, y, w, h, x + w, y + h);
        }
    }

    /**
     * Does a copy area on the specified region.
     *
     * @param clip Whether or not the copyArea needs to be clipped to the
     *             Component's bounds.
     */
    void copyArea(JComponent c, Graphics g, int x, int y, int w, int h,
                  int deltaX, int deltaY, boolean clip) {
        getPaintManager().copyArea(c, g, x, y, w, h, deltaX, deltaY, clip);
    }

    private java.util.List<RepaintListener> repaintListeners = new ArrayList<>(1);

    private void addRepaintListener(RepaintListener l) {
        repaintListeners.add(l);
    }

    private void removeRepaintListener(RepaintListener l) {
        repaintListeners.remove(l);
    }

    /**
     * Notify the attached repaint listeners that an area of the {@code c} component
     * has been immediately repainted, that is without scheduling a repaint runnable,
     * due to performing a "blit" (via calling the {@code copyArea} method).
     *
     * @param c the component
     * @param x the x coordinate of the area
     * @param y the y coordinate of the area
     * @param w the width of the area
     * @param h the height of the area
     */
    void notifyRepaintPerformed(JComponent c, int x, int y, int w, int h) {
        for (RepaintListener l : repaintListeners) {
            l.repaintPerformed(c, x, y, w, h);
        }
    }

    /**
     * Invoked prior to any paint/copyArea method calls.  This will
     * be followed by an invocation of <code>endPaint</code>.
     * <b>WARNING</b>: Callers of this method need to wrap the call
     * in a <code>try/finally</code>, otherwise if an exception is thrown
     * during the course of painting the RepaintManager may
     * be left in a state in which the screen is not updated, eg:
     * <pre>
     * repaintManager.beginPaint();
     * try {
     *   repaintManager.paint(...);
     * } finally {
     *   repaintManager.endPaint();
     * }
     * </pre>
     */
    void beginPaint() {
        boolean multiThreadedPaint = false;
        int paintDepth;
        Thread currentThread = Thread.currentThread();
        synchronized(this) {
            paintDepth = this.paintDepth;
            if (paintThread == null || currentThread == paintThread) {
                paintThread = currentThread;
                this.paintDepth++;
            } else {
                multiThreadedPaint = true;
            }
        }
        if (!multiThreadedPaint && paintDepth == 0) {
            getPaintManager().beginPaint();
        }
    }

    /**
     * Invoked after <code>beginPaint</code> has been invoked.
     */
    void endPaint() {
        if (isPaintingThread()) {
            PaintManager paintManager = null;
            synchronized(this) {
                if (--paintDepth == 0) {
                    paintManager = getPaintManager();
                }
            }
            if (paintManager != null) {
                paintManager.endPaint();
                synchronized(this) {
                    paintThread = null;
                }
            }
        }
    }

    /**
     * If possible this will show a previously rendered portion of
     * a Component.  If successful, this will return true, otherwise false.
     * <p>
     * WARNING: This method is invoked from the native toolkit thread, be
     * very careful as to what methods this invokes!
     */
    boolean show(Container c, int x, int y, int w, int h) {
        return getPaintManager().show(c, x, y, w, h);
    }

    /**
     * Invoked when the doubleBuffered or useTrueDoubleBuffering
     * properties of a JRootPane change.  This may come in on any thread.
     */
    void doubleBufferingChanged(JRootPane rootPane) {
        getPaintManager().doubleBufferingChanged(rootPane);
    }

    /**
     * Sets the <code>PaintManager</code> that is used to handle all
     * double buffered painting.
     *
     * @param paintManager The PaintManager to use.  Passing in null indicates
     *        the fallback PaintManager should be used.
     */
    void setPaintManager(PaintManager paintManager) {
        if (paintManager == null) {
            paintManager = new PaintManager();
        }
        PaintManager oldPaintManager;
        synchronized(this) {
            oldPaintManager = this.paintManager;
            this.paintManager = paintManager;
            paintManager.repaintManager = this;
        }
        if (oldPaintManager != null) {
            oldPaintManager.dispose();
        }
    }

    private synchronized PaintManager getPaintManager() {
        if (paintManager == null) {
            PaintManager paintManager = null;
            if (doubleBufferingEnabled && !nativeDoubleBuffering) {
                switch (bufferStrategyType) {
                case BUFFER_STRATEGY_NOT_SPECIFIED:
                    Toolkit tk = Toolkit.getDefaultToolkit();
                    if (tk instanceof SunToolkit) {
                        SunToolkit stk = (SunToolkit) tk;
                        if (stk.useBufferPerWindow()) {
                            paintManager = new BufferStrategyPaintManager();
                        }
                    }
                    break;
                case BUFFER_STRATEGY_SPECIFIED_ON:
                    paintManager = new BufferStrategyPaintManager();
                    break;
                default:
                    break;
                }
            }
            // null case handled in setPaintManager
            setPaintManager(paintManager);
        }
        return paintManager;
    }

    private void scheduleProcessingRunnable(AppContext context) {
        if (processingRunnable.markPending()) {
            Toolkit tk = Toolkit.getDefaultToolkit();
            if (tk instanceof SunToolkit) {
                SunToolkit.getSystemEventQueueImplPP(context).
                  postEvent(new InvocationEvent(Toolkit.getDefaultToolkit(),
                                                processingRunnable));
            } else {
                Toolkit.getDefaultToolkit().getSystemEventQueue().
                      postEvent(new InvocationEvent(Toolkit.getDefaultToolkit(),
                                                    processingRunnable));
            }
        }
    }


    /**
     * PaintManager is used to handle all double buffered painting for
     * Swing.  Subclasses should call back into the JComponent method
     * <code>paintToOffscreen</code> to handle the actual painting.
     */
    static class PaintManager {
        /**
         * RepaintManager the PaintManager has been installed on.
         */
        protected RepaintManager repaintManager;
        boolean isRepaintingRoot;

        /**
         * Paints a region of a component
         *
         * @param paintingComponent Component to paint
         * @param bufferComponent Component to obtain buffer for
         * @param g Graphics to paint to
         * @param x X-coordinate
         * @param y Y-coordinate
         * @param w Width
         * @param h Height
         * @return true if painting was successful.
         */
        public boolean paint(JComponent paintingComponent,
                             JComponent bufferComponent, Graphics g,
                             int x, int y, int w, int h) {
            // First attempt to use VolatileImage buffer for performance.
            // If this fails (which should rarely occur), fallback to a
            // standard Image buffer.
            boolean paintCompleted = false;
            Image offscreen;
            int sw = w + 1;
            int sh = h + 1;

            if (repaintManager.useVolatileDoubleBuffer() &&
                (offscreen = getValidImage(repaintManager.
                getVolatileOffscreenBuffer(bufferComponent, sw, sh))) != null) {
                VolatileImage vImage = (java.awt.image.VolatileImage)offscreen;
                GraphicsConfiguration gc = bufferComponent.
                                            getGraphicsConfiguration();
                for (int i = 0; !paintCompleted &&
                         i < RepaintManager.VOLATILE_LOOP_MAX; i++) {
                    if (vImage.validate(gc) ==
                                   VolatileImage.IMAGE_INCOMPATIBLE) {
                        repaintManager.resetVolatileDoubleBuffer(gc);
                        offscreen = repaintManager.getVolatileOffscreenBuffer(
                            bufferComponent, sw, sh);
                        vImage = (java.awt.image.VolatileImage)offscreen;
                    }
                    paintDoubleBuffered(paintingComponent, vImage, g, x, y,
                                        w, h);
                    paintCompleted = !vImage.contentsLost();
                }
            }
            // VolatileImage painting loop failed, fallback to regular
            // offscreen buffer
            if (!paintCompleted && (offscreen = getValidImage(
                      repaintManager.getOffscreenBuffer(
                      bufferComponent, w, h))) != null) {
                paintDoubleBuffered(paintingComponent, offscreen, g, x, y, w,
                                    h);
                paintCompleted = true;
            }
            return paintCompleted;
        }

        /**
         * Does a copy area on the specified region.
         */
        public void copyArea(JComponent c, Graphics g, int x, int y, int w,
                             int h, int deltaX, int deltaY, boolean clip) {
            g.copyArea(x, y, w, h, deltaX, deltaY);
        }

        /**
         * Invoked prior to any calls to paint or copyArea.
         */
        public void beginPaint() {
        }

        /**
         * Invoked to indicate painting has been completed.
         */
        public void endPaint() {
        }

        /**
         * Shows a region of a previously rendered component.  This
         * will return true if successful, false otherwise.  The default
         * implementation returns false.
         */
        public boolean show(Container c, int x, int y, int w, int h) {
            return false;
        }

        /**
         * Invoked when the doubleBuffered or useTrueDoubleBuffering
         * properties of a JRootPane change.  This may come in on any thread.
         */
        public void doubleBufferingChanged(JRootPane rootPane) {
        }

        /**
         * Paints a portion of a component to an offscreen buffer.
         */
        protected void paintDoubleBuffered(JComponent c, Image image,
                Graphics g, int clipX, int clipY,
                int clipW, int clipH) {
            if (image instanceof VolatileImage && isPixelsCopying(c, g)) {
                paintDoubleBufferedFPScales(c, image, g, clipX, clipY, clipW, clipH);
            } else {
                paintDoubleBufferedImpl(c, image, g, clipX, clipY, clipW, clipH);
            }
        }

        private void paintDoubleBufferedImpl(JComponent c, Image image,
                                             Graphics g, int clipX, int clipY,
                                             int clipW, int clipH) {
            Graphics osg = image.getGraphics();
            int bw = Math.min(clipW, image.getWidth(null));
            int bh = Math.min(clipH, image.getHeight(null));
            int x,y,maxx,maxy;

            try {
                for(x = clipX, maxx = clipX+clipW; x < maxx ;  x += bw ) {
                    for(y=clipY, maxy = clipY + clipH; y < maxy ; y += bh) {
                        osg.translate(-x, -y);
                        osg.setClip(x,y,bw,bh);
                        if (volatileBufferType != Transparency.OPAQUE
                                && osg instanceof Graphics2D) {
                            final Graphics2D g2d = (Graphics2D) osg;
                            final Color oldBg = g2d.getBackground();
                            g2d.setBackground(c.getBackground());
                            g2d.clearRect(x, y, bw, bh);
                            g2d.setBackground(oldBg);
                        }
                        c.paintToOffscreen(osg, x, y, bw, bh, maxx, maxy);
                        g.setClip(x, y, bw, bh);
                        if (volatileBufferType != Transparency.OPAQUE
                                && g instanceof Graphics2D) {
                            final Graphics2D g2d = (Graphics2D) g;
                            final Composite oldComposite = g2d.getComposite();
                            g2d.setComposite(AlphaComposite.Src);
                            g2d.drawImage(image, x, y, c);
                            g2d.setComposite(oldComposite);
                        } else {
                            g.drawImage(image, x, y, c);
                        }
                        osg.translate(x, y);
                    }
                }
            } finally {
                osg.dispose();
            }
        }

        private void paintDoubleBufferedFPScales(JComponent c, Image image,
                                                 Graphics g, int clipX, int clipY,
                                                 int clipW, int clipH) {
            Graphics osg = image.getGraphics();
            Graphics2D g2d = (Graphics2D) g;
            Graphics2D osg2d = (Graphics2D) osg;

            AffineTransform identity = new AffineTransform();
            int bw = Math.min(clipW, image.getWidth(null));
            int bh = Math.min(clipH, image.getHeight(null));
            int x, y, maxx, maxy;

            AffineTransform tx = g2d.getTransform();
            double scaleX = tx.getScaleX();
            double scaleY = tx.getScaleY();
            double trX = tx.getTranslateX();
            double trY = tx.getTranslateY();

            boolean translucent = volatileBufferType != Transparency.OPAQUE;
            Composite oldComposite = g2d.getComposite();

            try {
                for (x = clipX, maxx = clipX + clipW; x < maxx; x += bw) {
                    for (y = clipY, maxy = clipY + clipH; y < maxy; y += bh) {

                        // draw x, y, bw, bh
                        int pixelx1 = Region.clipRound(x * scaleX + trX);
                        int pixely1 = Region.clipRound(y * scaleY + trY);
                        int pixelx2 = Region.clipRound((x + bw) * scaleX + trX);
                        int pixely2 = Region.clipRound((y + bh) * scaleY + trY);
                        int pixelw = pixelx2 - pixelx1;
                        int pixelh = pixely2 - pixely1;

                        osg2d.setTransform(identity);
                        if (translucent) {
                            final Color oldBg = g2d.getBackground();
                            g2d.setBackground(c.getBackground());
                            g2d.clearRect(pixelx1, pixely1, pixelw, pixelh);
                            g2d.setBackground(oldBg);
                        }

                        osg2d.setClip(0, 0, pixelw, pixelh);
                        osg2d.translate(trX - pixelx1, trY - pixely1);
                        osg2d.scale(scaleX, scaleY);
                        c.paintToOffscreen(osg, x, y, bw, bh, maxx, maxy);

                        g2d.setTransform(identity);
                        g2d.setClip(pixelx1, pixely1, pixelw, pixelh);
                        AffineTransform stx = new AffineTransform();
                        stx.translate(pixelx1, pixely1);
                        stx.scale(scaleX, scaleY);
                        g2d.setTransform(stx);

                        if (translucent) {
                            g2d.setComposite(AlphaComposite.Src);
                        }

                        g2d.drawImage(image, 0, 0, c);

                        if (translucent) {
                            g2d.setComposite(oldComposite);
                        }
                        g2d.setTransform(tx);
                    }
                }
            } finally {
                osg.dispose();
            }
        }

        /**
         * If <code>image</code> is non-null with a positive size it
         * is returned, otherwise null is returned.
         */
        private Image getValidImage(Image image) {
            if (image != null && image.getWidth(null) > 0 &&
                                 image.getHeight(null) > 0) {
                return image;
            }
            return null;
        }

        /**
         * Schedules a repaint for the specified component.  This differs
         * from <code>root.repaint</code> in that if the RepaintManager is
         * currently processing paint requests it'll process this request
         * with the current set of requests.
         */
        protected void repaintRoot(JComponent root) {
            assert (repaintManager.repaintRoot == null);
            if (repaintManager.painting) {
                repaintManager.repaintRoot = root;
            }
            else {
                root.repaint();
            }
        }

        /**
         * Returns true if the component being painted is the root component
         * that was previously passed to <code>repaintRoot</code>.
         */
        protected boolean isRepaintingRoot() {
            return isRepaintingRoot;
        }

        /**
         * Cleans up any state.  After invoked the PaintManager will no
         * longer be used anymore.
         */
        protected void dispose() {
        }

        private boolean isPixelsCopying(JComponent c, Graphics g) {

            AffineTransform tx = getTransform(g);
            GraphicsConfiguration gc = c.getGraphicsConfiguration();

            if (tx == null || gc == null
                    || !SwingUtilities2.isFloatingPointScale(tx)) {
                return false;
            }

            AffineTransform gcTx = gc.getDefaultTransform();

            return gcTx.getScaleX() == tx.getScaleX()
                    && gcTx.getScaleY() == tx.getScaleY();
        }

        private static AffineTransform getTransform(Graphics g) {
            if (g instanceof SunGraphics2D) {
                return ((SunGraphics2D) g).transform;
            } else if (g instanceof Graphics2D) {
                return ((Graphics2D) g).getTransform();
            }
            return null;
        }
    }

    private class DoubleBufferInfo {
        public Image image;
        public Dimension size;
        public boolean needsReset = false;
    }


    /**
     * Listener installed to detect display changes. When display changes,
     * schedules a callback to notify all RepaintManagers of the display
     * changes. Only one DisplayChangedHandler is ever installed. The
     * singleton instance will schedule notification for all AppContexts.
     */
    private static final class DisplayChangedHandler implements
                                             DisplayChangedListener {
        public void displayChanged() {
            scheduleDisplayChanges();
        }

        public void paletteChanged() {
        }

        private static void scheduleDisplayChanges() {
            // To avoid threading problems, we notify each RepaintManager
            // on the thread it was created on.
            for (AppContext context : AppContext.getAppContexts()) {
                synchronized(context) {
                    if (!context.isDisposed()) {
                        EventQueue eventQueue = (EventQueue)context.get(
                            AppContext.EVENT_QUEUE_KEY);
                        if (eventQueue != null) {
                            eventQueue.postEvent(new InvocationEvent(
                                Toolkit.getDefaultToolkit(),
                                new DisplayChangedRunnable()));
                        }
                    }
                }
            }
        }
    }


    private static final class DisplayChangedRunnable implements Runnable {
        public void run() {
            RepaintManager.currentManager((JComponent)null).displayChanged();
        }
    }


    /**
     * Runnable used to process all repaint/revalidate requests.
     */
    private final class ProcessingRunnable implements Runnable {
        // If true, we're wainting on the EventQueue.
        private boolean pending;

        /**
         * Marks this processing runnable as pending. If this was not
         * already marked as pending, true is returned.
         */
        public synchronized boolean markPending() {
            if (!pending) {
                pending = true;
                return true;
            }
            return false;
        }

        public void run() {
            synchronized (this) {
                pending = false;
            }
            // First pass, flush any heavy paint events into real paint
            // events.  If there are pending heavy weight requests this will
            // result in q'ing this request up one more time.  As
            // long as no other requests come in between now and the time
            // the second one is processed nothing will happen.  This is not
            // ideal, but the logic needed to suppress the second request is
            // more headache than it's worth.
            scheduleHeavyWeightPaints();
            // Do the actual validation and painting.
            validateInvalidComponents();
            prePaintDirtyRegions();
        }
    }
    private RepaintManager getDelegate(Component c) {
        RepaintManager delegate = SwingUtilities3.getDelegateRepaintManager(c);
        if (this == delegate) {
            delegate = null;
        }
        return delegate;
    }
}
