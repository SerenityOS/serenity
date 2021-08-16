/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.image.*;
import java.lang.ref.WeakReference;
import java.util.*;

import com.sun.java.swing.SwingUtilities3;
import sun.awt.AWTAccessor;

import sun.awt.SubRegionShowable;
import sun.java2d.SunGraphics2D;
import sun.java2d.pipe.hw.ExtendedBufferCapabilities;
import sun.awt.SunToolkit;
import sun.util.logging.PlatformLogger;

/**
 * A PaintManager implementation that uses a BufferStrategy for
 * rendering.
 *
 * @author Scott Violet
 */
class BufferStrategyPaintManager extends RepaintManager.PaintManager {
    //
    // All drawing is done to a BufferStrategy.  At the end of painting
    // (endPaint) the region that was painted is flushed to the screen
    // (using BufferStrategy.show).
    //
    // PaintManager.show is overriden to show directly from the
    // BufferStrategy (when using blit), if successful true is
    // returned and a paint event will not be generated.  To avoid
    // showing from the buffer while painting a locking scheme is
    // implemented.  When beginPaint is invoked the field painting is
    // set to true.  If painting is true and show is invoked we
    // immediately return false.  This is done to avoid blocking the
    // toolkit thread while painting happens.  In a similar way when
    // show is invoked the field showing is set to true, beginPaint
    // will then block until showing is true.  This scheme ensures we
    // only ever have one thread using the BufferStrategy and it also
    // ensures the toolkit thread remains as responsive as possible.
    //
    // If we're using a flip strategy the contents of the backbuffer may
    // have changed and so show only attempts to show from the backbuffer
    // if we get a blit strategy.
    //

    private static final PlatformLogger LOGGER = PlatformLogger.getLogger(
                           "javax.swing.BufferStrategyPaintManager");

    /**
     * List of BufferInfos.  We don't use a Map primarily because
     * there are typically only a handful of top level components making
     * a Map overkill.
     */
    private ArrayList<BufferInfo> bufferInfos;

    /**
     * Indicates <code>beginPaint</code> has been invoked.  This is
     * set to true for the life of beginPaint/endPaint pair.
     */
    private boolean painting;
    /**
     * Indicates we're in the process of showing.  All painting, on the EDT,
     * is blocked while this is true.
     */
    private boolean showing;

    //
    // Region that we need to flush.  When beginPaint is called these are
    // reset and any subsequent calls to paint/copyArea then update these
    // fields accordingly.  When endPaint is called we then try and show
    // the accumulated region.
    // These fields are in the coordinate system of the root.
    //
    private int accumulatedX;
    private int accumulatedY;
    private int accumulatedMaxX;
    private int accumulatedMaxY;

    //
    // The following fields are set by prepare
    //

    /**
     * Farthest JComponent ancestor for the current paint/copyArea.
     */
    private JComponent rootJ;
    /**
     * Location of component being painted relative to root.
     */
    private int xOffset;
    /**
     * Location of component being painted relative to root.
     */
    private int yOffset;
    /**
     * Graphics from the BufferStrategy.
     */
    private Graphics bsg;
    /**
     * BufferStrategy currently being used.
     */
    private BufferStrategy bufferStrategy;
    /**
     * BufferInfo corresponding to root.
     */
    private BufferInfo bufferInfo;

    /**
     * Set to true if the bufferInfo needs to be disposed when current
     * paint loop is done.
     */
    private boolean disposeBufferOnEnd;

    BufferStrategyPaintManager() {
        bufferInfos = new ArrayList<BufferInfo>(1);
    }

    //
    // PaintManager methods
    //

    /**
     * Cleans up any created BufferStrategies.
     */
    protected void dispose() {
        // dipose can be invoked at any random time. To avoid
        // threading dependancies we do the actual diposing via an
        // invokeLater.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                java.util.List<BufferInfo> bufferInfos;
                synchronized(BufferStrategyPaintManager.this) {
                    while (showing) {
                        try {
                            BufferStrategyPaintManager.this.wait();
                        } catch (InterruptedException ie) {
                        }
                    }
                    bufferInfos = BufferStrategyPaintManager.this.bufferInfos;
                    BufferStrategyPaintManager.this.bufferInfos = null;
                }
                dispose(bufferInfos);
            }
        });
    }

    private void dispose(java.util.List<BufferInfo> bufferInfos) {
        if (LOGGER.isLoggable(PlatformLogger.Level.FINER)) {
            LOGGER.finer("BufferStrategyPaintManager disposed",
                         new RuntimeException());
        }
        if (bufferInfos != null) {
            for (BufferInfo bufferInfo : bufferInfos) {
                bufferInfo.dispose();
            }
        }
    }

    /**
     * Shows the specified region of the back buffer.  This will return
     * true if successful, false otherwise.  This is invoked on the
     * toolkit thread in response to an expose event.
     */
    public boolean show(Container c, int x, int y, int w, int h) {
        synchronized(this) {
            if (painting) {
                // Don't show from backbuffer while in the process of
                // painting.
                return false;
            }
            showing = true;
        }
        try {
            BufferInfo info = getBufferInfo(c);
            BufferStrategy bufferStrategy;
            if (info != null && info.isInSync() &&
                (bufferStrategy = info.getBufferStrategy(false)) != null) {
                SubRegionShowable bsSubRegion =
                        (SubRegionShowable)bufferStrategy;
                boolean paintAllOnExpose = info.getPaintAllOnExpose();
                info.setPaintAllOnExpose(false);
                if (bsSubRegion.showIfNotLost(x, y, (x + w), (y + h))) {
                    return !paintAllOnExpose;
                }
                // Mark the buffer as needing to be repainted.  We don't
                // immediately do a repaint as this method will return false
                // indicating a PaintEvent should be generated which will
                // trigger a complete repaint.
                bufferInfo.setContentsLostDuringExpose(true);
            }
        }
        finally {
            synchronized(this) {
                showing = false;
                notifyAll();
            }
        }
        return false;
    }

    public boolean paint(JComponent paintingComponent,
                         JComponent bufferComponent, Graphics g,
                         int x, int y, int w, int h) {
        Container root = fetchRoot(paintingComponent);

        if (prepare(paintingComponent, root, true, x, y, w, h)) {
            if ((g instanceof SunGraphics2D) &&
                    ((SunGraphics2D)g).getDestination() == root) {
                // BufferStrategy may have already constrained the Graphics. To
                // account for that we revert the constrain, then apply a
                // constrain for Swing on top of that.
                int cx = ((SunGraphics2D)bsg).constrainX;
                int cy = ((SunGraphics2D)bsg).constrainY;
                if (cx != 0 || cy != 0) {
                    bsg.translate(-cx, -cy);
                }
                ((SunGraphics2D)bsg).constrain(xOffset + cx, yOffset + cy,
                                               x + w, y + h);
                bsg.setClip(x, y, w, h);
                paintingComponent.paintToOffscreen(bsg, x, y, w, h,
                                                   x + w, y + h);
                accumulate(xOffset + x, yOffset + y, w, h);
                return true;
            } else {
                // Assume they are going to eventually render to the screen.
                // This disables showing from backbuffer until a complete
                // repaint occurs.
                bufferInfo.setInSync(false);
                // Fall through to old rendering.
            }
        }
        // Invalid root, do what Swing has always done.
        if (LOGGER.isLoggable(PlatformLogger.Level.FINER)) {
            LOGGER.finer("prepare failed");
        }
        return super.paint(paintingComponent, bufferComponent, g, x, y, w, h);
    }

    public void copyArea(JComponent c, Graphics g, int x, int y, int w, int h,
                         int deltaX, int deltaY, boolean clip) {
        // Note: this method is only called internally and we know that
        // g is from a heavyweight Component, so no check is necessary as
        // it is in paint() above.
        //
        // If the buffer isn't in sync there is no point in doing a copyArea,
        // it has garbage.
        Container root = fetchRoot(c);

        if (prepare(c, root, false, 0, 0, 0, 0) && bufferInfo.isInSync()) {
            if (clip) {
                Rectangle cBounds = c.getVisibleRect();
                int relX = xOffset + x;
                int relY = yOffset + y;
                bsg.clipRect(xOffset + cBounds.x,
                             yOffset + cBounds.y,
                             cBounds.width, cBounds.height);
                bsg.copyArea(relX, relY, w, h, deltaX, deltaY);
            }
            else {
                bsg.copyArea(xOffset + x, yOffset + y, w, h, deltaX,
                             deltaY);
            }
            accumulate(x + xOffset + deltaX, y + yOffset + deltaY, w, h);
        } else {
            if (LOGGER.isLoggable(PlatformLogger.Level.FINER)) {
                LOGGER.finer("copyArea: prepare failed or not in sync");
            }
            // Prepare failed, or not in sync. By calling super.copyArea
            // we'll copy on screen. We need to flush any pending paint to
            // the screen otherwise we'll do a copyArea on the wrong thing.
            if (!flushAccumulatedRegion()) {
                // Flush failed, copyArea will be copying garbage,
                // force repaint of all.
                rootJ.repaint();
            } else {
                super.copyArea(c, g, x, y, w, h, deltaX, deltaY, clip);
            }
        }
    }

    public void beginPaint() {
        synchronized(this) {
            painting = true;
            // Make sure another thread isn't attempting to show from
            // the back buffer.
            while(showing) {
                try {
                    wait();
                } catch (InterruptedException ie) {
                }
            }
        }
        if (LOGGER.isLoggable(PlatformLogger.Level.FINEST)) {
            LOGGER.finest("beginPaint");
        }
        // Reset the area that needs to be painted.
        resetAccumulated();
    }

    public void endPaint() {
        if (LOGGER.isLoggable(PlatformLogger.Level.FINEST)) {
            LOGGER.finest("endPaint: region " + accumulatedX + " " +
                       accumulatedY + " " +  accumulatedMaxX + " " +
                       accumulatedMaxY);
        }
        if (painting) {
            if (!flushAccumulatedRegion()) {
                if (!isRepaintingRoot()) {
                    repaintRoot(rootJ);
                }
                else {
                    // Contents lost twice in a row, punt.
                    resetDoubleBufferPerWindow();
                    // In case we've left junk on the screen, force a repaint.
                    rootJ.repaint();
                }
            }
        }

        BufferInfo toDispose = null;
        synchronized(this) {
            painting = false;
            if (disposeBufferOnEnd) {
                disposeBufferOnEnd = false;
                toDispose = bufferInfo;
                bufferInfos.remove(toDispose);
            }
        }
        if (toDispose != null) {
            toDispose.dispose();
        }
    }

    /**
     * Renders the BufferStrategy to the screen.
     *
     * @return true if successful, false otherwise.
     */
    private boolean flushAccumulatedRegion() {
        boolean success = true;
        if (accumulatedX != Integer.MAX_VALUE) {
            SubRegionShowable bsSubRegion = (SubRegionShowable)bufferStrategy;
            boolean contentsLost = bufferStrategy.contentsLost();
            if (!contentsLost) {
                bsSubRegion.show(accumulatedX, accumulatedY,
                                 accumulatedMaxX, accumulatedMaxY);
                contentsLost = bufferStrategy.contentsLost();
            }
            if (contentsLost) {
                if (LOGGER.isLoggable(PlatformLogger.Level.FINER)) {
                    LOGGER.finer("endPaint: contents lost");
                }
                // Shown region was bogus, mark buffer as out of sync.
                bufferInfo.setInSync(false);
                success = false;
            }
        }
        resetAccumulated();
        return success;
    }

    private void resetAccumulated() {
        accumulatedX = Integer.MAX_VALUE;
        accumulatedY = Integer.MAX_VALUE;
        accumulatedMaxX = 0;
        accumulatedMaxY = 0;
    }

    /**
     * Invoked when the double buffering or useTrueDoubleBuffering
     * changes for a JRootPane.  If the rootpane is not double
     * buffered, or true double buffering changes we throw out any
     * cache we may have.
     */
    public void doubleBufferingChanged(final JRootPane rootPane) {
        if ((!rootPane.isDoubleBuffered() ||
                !rootPane.getUseTrueDoubleBuffering()) &&
                rootPane.getParent() != null) {
            if (!SwingUtilities.isEventDispatchThread()) {
                Runnable updater = new Runnable() {
                    public void run() {
                        doubleBufferingChanged0(rootPane);
                    }
                };
                SwingUtilities.invokeLater(updater);
            }
            else {
                doubleBufferingChanged0(rootPane);
            }
        }
    }

    /**
     * Does the work for doubleBufferingChanged.
     */
    private void doubleBufferingChanged0(JRootPane rootPane) {
        // This will only happen on the EDT.
        BufferInfo info;
        synchronized(this) {
            // Make sure another thread isn't attempting to show from
            // the back buffer.
            while(showing) {
                try {
                    wait();
                } catch (InterruptedException ie) {
                }
            }
            info = getBufferInfo(rootPane.getParent());
            if (painting && bufferInfo == info) {
                // We're in the process of painting and the user grabbed
                // the Graphics. If we dispose now, endPaint will attempt
                // to show a bogus BufferStrategy. Set a flag so that
                // endPaint knows it needs to dispose this buffer.
                disposeBufferOnEnd = true;
                info = null;
            } else if (info != null) {
                bufferInfos.remove(info);
            }
        }
        if (info != null) {
            info.dispose();
        }
    }

    /**
     * Calculates information common to paint/copyArea.
     *
     * @return true if should use buffering per window in painting.
     */
    private boolean prepare(JComponent c, Container root, boolean isPaint, int x, int y,
                            int w, int h) {
        if (bsg != null) {
            bsg.dispose();
            bsg = null;
        }
        bufferStrategy = null;
        if (root != null) {
            boolean contentsLost = false;
            BufferInfo bufferInfo = getBufferInfo(root);
            if (bufferInfo == null) {
                contentsLost = true;
                bufferInfo = new BufferInfo(root);
                bufferInfos.add(bufferInfo);
                if (LOGGER.isLoggable(PlatformLogger.Level.FINER)) {
                    LOGGER.finer("prepare: new BufferInfo: " + root);
                }
            }
            this.bufferInfo = bufferInfo;
            if (!bufferInfo.hasBufferStrategyChanged()) {
                bufferStrategy = bufferInfo.getBufferStrategy(true);
                if (bufferStrategy != null) {
                    bsg = bufferStrategy.getDrawGraphics();
                    if (bufferStrategy.contentsRestored()) {
                        contentsLost = true;
                        if (LOGGER.isLoggable(PlatformLogger.Level.FINER)) {
                            LOGGER.finer("prepare: contents restored in prepare");
                        }
                    }
                }
                else {
                    // Couldn't create BufferStrategy, fallback to normal
                    // painting.
                    return false;
                }
                if (bufferInfo.getContentsLostDuringExpose()) {
                    contentsLost = true;
                    bufferInfo.setContentsLostDuringExpose(false);
                    if (LOGGER.isLoggable(PlatformLogger.Level.FINER)) {
                        LOGGER.finer("prepare: contents lost on expose");
                    }
                }
                if (isPaint && c == rootJ && x == 0 && y == 0 &&
                      c.getWidth() == w && c.getHeight() == h) {
                    bufferInfo.setInSync(true);
                }
                else if (contentsLost) {
                    // We either recreated the BufferStrategy, or the contents
                    // of the buffer strategy were restored.  We need to
                    // repaint the root pane so that the back buffer is in sync
                    // again.
                    bufferInfo.setInSync(false);
                    if (!isRepaintingRoot()) {
                        repaintRoot(rootJ);
                    }
                    else {
                        // Contents lost twice in a row, punt
                        resetDoubleBufferPerWindow();
                    }
                }
                return (bufferInfos != null);
            }
        }
        return false;
    }

    private Container fetchRoot(JComponent c) {
        boolean encounteredHW = false;
        rootJ = c;
        Container root = c;
        xOffset = yOffset = 0;
        while (root != null &&
               (!(root instanceof Window) &&
                !SunToolkit.isInstanceOf(root, "java.applet.Applet"))) {
            xOffset += root.getX();
            yOffset += root.getY();
            root = root.getParent();
            if (root != null) {
                if (root instanceof JComponent) {
                    rootJ = (JComponent)root;
                }
                else if (!root.isLightweight()) {
                    if (!encounteredHW) {
                        encounteredHW = true;
                    }
                    else {
                        // We've encountered two hws now and may have
                        // a containment hierarchy with lightweights containing
                        // heavyweights containing other lightweights.
                        // Heavyweights poke holes in lightweight
                        // rendering so that if we call show on the BS
                        // (which is associated with the Window) you will
                        // not see the contents over any child
                        // heavyweights.  If we didn't do this when we
                        // went to show the descendants of the nested hw
                        // you would see nothing, so, we bail out here.
                        return null;
                    }
                }
            }
        }
        if ((root instanceof RootPaneContainer) &&
            (rootJ instanceof JRootPane)) {
            // We're in a Swing heavyeight (JFrame/JWindow...), use double
            // buffering if double buffering enabled on the JRootPane and
            // the JRootPane wants true double buffering.
            if (rootJ.isDoubleBuffered() &&
                    ((JRootPane)rootJ).getUseTrueDoubleBuffering()) {
                // Whether or not a component is double buffered is a
                // bit tricky with Swing. This gives a good approximation
                // of the various ways to turn on double buffering for
                // components.
                return root;
            }
        }
        // Don't do true double buffering.
        return null;
    }

    /**
     * Turns off double buffering per window.
     */
    private void resetDoubleBufferPerWindow() {
        if (bufferInfos != null) {
            dispose(bufferInfos);
            bufferInfos = null;
            repaintManager.setPaintManager(null);
        }
    }

    /**
     * Returns the BufferInfo for the specified root or null if one
     * hasn't been created yet.
     */
    private BufferInfo getBufferInfo(Container root) {
        for (int counter = bufferInfos.size() - 1; counter >= 0; counter--) {
            BufferInfo bufferInfo = bufferInfos.get(counter);
            Container biRoot = bufferInfo.getRoot();
            if (biRoot == null) {
                // Window gc'ed
                bufferInfos.remove(counter);
                if (LOGGER.isLoggable(PlatformLogger.Level.FINER)) {
                    LOGGER.finer("BufferInfo pruned, root null");
                }
            }
            else if (biRoot == root) {
                return bufferInfo;
            }
        }
        return null;
    }

    private void accumulate(int x, int y, int w, int h) {
        accumulatedX = Math.min(x, accumulatedX);
        accumulatedY = Math.min(y, accumulatedY);
        accumulatedMaxX = Math.max(accumulatedMaxX, x + w);
        accumulatedMaxY = Math.max(accumulatedMaxY, y + h);
    }



    /**
     * BufferInfo is used to track the BufferStrategy being used for
     * a particular Component.  In addition to tracking the BufferStrategy
     * it will install a WindowListener and ComponentListener.  When the
     * component is hidden/iconified the buffer is marked as needing to be
     * completely repainted.
     */
    private class BufferInfo extends ComponentAdapter implements
                               WindowListener {
        // NOTE: This class does NOT hold a direct reference to the root, if it
        // did there would be a cycle between the BufferPerWindowPaintManager
        // and the Window so that it could never be GC'ed
        //
        // Reference to BufferStrategy is referenced via WeakReference for
        // same reason.
        private WeakReference<BufferStrategy> weakBS;
        private WeakReference<Container> root;
        // Indicates whether or not the backbuffer and display are in sync.
        // This is set to true when a full repaint on the rootpane is done.
        private boolean inSync;
        // Indicates the contents were lost during and expose event.
        private boolean contentsLostDuringExpose;
        // Indicates we need to generate a paint event on expose.
        private boolean paintAllOnExpose;


        public BufferInfo(Container root) {
            this.root = new WeakReference<Container>(root);
            root.addComponentListener(this);
            if (root instanceof Window) {
                ((Window)root).addWindowListener(this);
            }
        }

        public void setPaintAllOnExpose(boolean paintAllOnExpose) {
            this.paintAllOnExpose = paintAllOnExpose;
        }

        public boolean getPaintAllOnExpose() {
            return paintAllOnExpose;
        }

        public void setContentsLostDuringExpose(boolean value) {
            contentsLostDuringExpose = value;
        }

        public boolean getContentsLostDuringExpose() {
            return contentsLostDuringExpose;
        }

        public void setInSync(boolean inSync) {
            this.inSync = inSync;
        }

        /**
         * Whether or not the contents of the buffer strategy
         * is in sync with the window.  This is set to true when the root
         * pane paints all, and false when contents are lost/restored.
         */
        public boolean isInSync() {
            return inSync;
        }

        /**
         * Returns the Root (Window or Applet) that this BufferInfo references.
         */
        public Container getRoot() {
            return (root == null) ? null : root.get();
        }

        /**
         * Returns the BufferStartegy.  This will return null if
         * the BufferStartegy hasn't been created and <code>create</code> is
         * false, or if there is a problem in creating the
         * <code>BufferStartegy</code>.
         *
         * @param create If true, and the BufferStartegy is currently null,
         *               one will be created.
         */
        public BufferStrategy getBufferStrategy(boolean create) {
            BufferStrategy bs = (weakBS == null) ? null : weakBS.get();
            if (bs == null && create) {
                bs = createBufferStrategy();
                if (bs != null) {
                    weakBS = new WeakReference<BufferStrategy>(bs);
                }
                if (LOGGER.isLoggable(PlatformLogger.Level.FINER)) {
                    LOGGER.finer("getBufferStrategy: created bs: " + bs);
                }
            }
            return bs;
        }

        /**
         * Returns true if the buffer strategy of the component differs
         * from current buffer strategy.
         */
        public boolean hasBufferStrategyChanged() {
            Container root = getRoot();
            if (root != null) {
                BufferStrategy ourBS = null;
                BufferStrategy componentBS = null;

                ourBS = getBufferStrategy(false);
                if (root instanceof Window) {
                    componentBS = ((Window)root).getBufferStrategy();
                }
                else {
                    componentBS = AWTAccessor.getComponentAccessor().getBufferStrategy(root);
                }
                if (componentBS != ourBS) {
                    // Component has a different BS, dispose ours.
                    if (ourBS != null) {
                        ourBS.dispose();
                    }
                    weakBS = null;
                    return true;
                }
            }
            return false;
        }

        /**
         * Creates the BufferStrategy.  If the appropriate system property
         * has been set we'll try for flip first and then we'll try for
         * blit.
         */
        private BufferStrategy createBufferStrategy() {
            Container root = getRoot();
            if (root == null) {
                return null;
            }
            BufferStrategy bs = null;
            if (SwingUtilities3.isVsyncRequested(root)) {
                bs = createBufferStrategy(root, true);
                if (LOGGER.isLoggable(PlatformLogger.Level.FINER)) {
                    LOGGER.finer("createBufferStrategy: using vsynced strategy");
                }
            }
            if (bs == null) {
                bs = createBufferStrategy(root, false);
            }
            if (!(bs instanceof SubRegionShowable)) {
                // We do this for two reasons:
                // 1. So that we know we can cast to SubRegionShowable and
                //    invoke show with the minimal region to update
                // 2. To avoid the possibility of invoking client code
                //    on the toolkit thread.
                bs = null;
            }
            return bs;
        }

        // Creates and returns a buffer strategy.  If
        // there is a problem creating the buffer strategy this will
        // eat the exception and return null.
        private BufferStrategy createBufferStrategy(Container root,
                boolean isVsynced) {
            BufferCapabilities caps;
            if (isVsynced) {
                caps = new ExtendedBufferCapabilities(
                    new ImageCapabilities(true), new ImageCapabilities(true),
                    BufferCapabilities.FlipContents.COPIED,
                    ExtendedBufferCapabilities.VSyncType.VSYNC_ON);
            } else {
                caps = new BufferCapabilities(
                    new ImageCapabilities(true), new ImageCapabilities(true),
                    null);
            }
            BufferStrategy bs = null;
            if (SunToolkit.isInstanceOf(root, "java.applet.Applet")) {
                try {
                    AWTAccessor.ComponentAccessor componentAccessor
                            = AWTAccessor.getComponentAccessor();
                    componentAccessor.createBufferStrategy(root, 2, caps);
                    bs = componentAccessor.getBufferStrategy(root);
                } catch (AWTException e) {
                    // Type is not supported
                    if (LOGGER.isLoggable(PlatformLogger.Level.FINER)) {
                        LOGGER.finer("createBufferStratety failed",
                                     e);
                    }
                }
            }
            else {
                try {
                    ((Window)root).createBufferStrategy(2, caps);
                    bs = ((Window)root).getBufferStrategy();
                } catch (AWTException e) {
                    // Type not supported
                    if (LOGGER.isLoggable(PlatformLogger.Level.FINER)) {
                        LOGGER.finer("createBufferStratety failed",
                                     e);
                    }
                }
            }
            return bs;
        }

        /**
         * Cleans up and removes any references.
         */
        public void dispose() {
            Container root = getRoot();
            if (LOGGER.isLoggable(PlatformLogger.Level.FINER)) {
                LOGGER.finer("disposed BufferInfo for: " + root);
            }
            if (root != null) {
                root.removeComponentListener(this);
                if (root instanceof Window) {
                    ((Window)root).removeWindowListener(this);
                }
                BufferStrategy bs = getBufferStrategy(false);
                if (bs != null) {
                    bs.dispose();
                }
            }
            this.root = null;
            weakBS = null;
        }

        // We mark the buffer as needing to be painted on a hide/iconify
        // because the developer may have conditionalized painting based on
        // visibility.
        // Ideally we would also move to having the BufferStrategy being
        // a SoftReference in Component here, but that requires changes to
        // Component and the like.
        public void componentHidden(ComponentEvent e) {
            Container root = getRoot();
            if (root != null && root.isVisible()) {
                // This case will only happen if a developer calls
                // hide immediately followed by show.  In this case
                // the event is delivered after show and the window
                // will still be visible.  If a developer altered the
                // contents of the window between the hide/show
                // invocations we won't recognize we need to paint and
                // the contents would be bogus.  Calling repaint here
                // fixs everything up.
                root.repaint();
            }
            else {
                setPaintAllOnExpose(true);
            }
        }

        public void windowIconified(WindowEvent e) {
            setPaintAllOnExpose(true);
        }

        // On a dispose we chuck everything.
        public void windowClosed(WindowEvent e) {
            // Make sure we're not showing.
            synchronized(BufferStrategyPaintManager.this) {
                while (showing) {
                    try {
                        BufferStrategyPaintManager.this.wait();
                    } catch (InterruptedException ie) {
                    }
                }
                bufferInfos.remove(this);
            }
            dispose();
        }

        public void windowOpened(WindowEvent e) {
        }

        public void windowClosing(WindowEvent e) {
        }

        public void windowDeiconified(WindowEvent e) {
        }

        public void windowActivated(WindowEvent e) {
        }

        public void windowDeactivated(WindowEvent e) {
        }
    }
}
