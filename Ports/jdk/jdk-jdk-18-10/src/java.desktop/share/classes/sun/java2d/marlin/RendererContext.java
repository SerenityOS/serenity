/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.marlin;

import java.awt.geom.Path2D;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;
import sun.java2d.ReentrantContext;
import sun.java2d.marlin.ArrayCacheConst.CacheStats;
import sun.java2d.marlin.DMarlinRenderingEngine.NormalizingPathIterator;
import sun.java2d.marlin.TransformingPathConsumer2D.CurveBasicMonotonizer;
import sun.java2d.marlin.TransformingPathConsumer2D.CurveClipSplitter;

/**
 * This class is a renderer context dedicated to a single thread
 */
final class RendererContext extends ReentrantContext implements MarlinConst {

    // RendererContext creation counter
    private static final AtomicInteger CTX_COUNT = new AtomicInteger(1);

    /**
     * Create a new renderer context
     *
     * @return new RendererContext instance
     */
    static RendererContext createContext() {
        return new RendererContext("ctx"
                       + Integer.toString(CTX_COUNT.getAndIncrement()));
    }

    // Smallest object used as Cleaner's parent reference
    private final Object cleanerObj;
    // dirty flag indicating an exception occured during pipeline in pathTo()
    boolean dirty = false;
    // shared data
    final double[] double6 = new double[6];
    // shared curve (dirty) (Renderer / Stroker)
    final Curve curve = new Curve();
    // MarlinRenderingEngine NormalizingPathIterator NearestPixelCenter:
    final NormalizingPathIterator nPCPathIterator;
    // MarlinRenderingEngine NearestPixelQuarter NormalizingPathIterator:
    final NormalizingPathIterator nPQPathIterator;
    // MarlinRenderingEngine.TransformingPathConsumer2D
    final TransformingPathConsumer2D transformerPC2D;
    // recycled Path2D instance (weak)
    private WeakReference<Path2D.Double> refPath2D = null;
    final Renderer renderer;
    final Stroker stroker;
    // Simplifies out collinear lines
    final CollinearSimplifier simplifier = new CollinearSimplifier();
    // Simplifies path
    final PathSimplifier pathSimplifier = new PathSimplifier();
    final Dasher dasher;
    final MarlinTileGenerator ptg;
    final MarlinCache cache;
    // flag indicating the shape is stroked (1) or filled (0)
    int stroking = 0;
    // flag indicating to clip the shape
    boolean doClip = false;
    // flag indicating if the path is closed or not (in advance) to handle properly caps
    boolean closedPath = false;
    // clip rectangle (ymin, ymax, xmin, xmax):
    final double[] clipRect = new double[4];
    // clip inverse scale (mean) to adjust length checks
    double clipInvScale = 0.0d;
    // CurveBasicMonotonizer instance
    final CurveBasicMonotonizer monotonizer;
    // CurveClipSplitter instance
    final CurveClipSplitter curveClipSplitter;

    // Array caches:
    /* clean int[] cache (zero-filled) = 5 refs */
    private final IntArrayCache cleanIntCache = new IntArrayCache(true, 5);
    /* dirty int[] cache = 5 refs */
    private final IntArrayCache dirtyIntCache = new IntArrayCache(false, 5);
    /* dirty double[] cache = 4 refs (2 polystack) */
    private final DoubleArrayCache dirtyDoubleCache = new DoubleArrayCache(false, 4);
    /* dirty byte[] cache = 2 ref (2 polystack) */
    private final ByteArrayCache dirtyByteCache = new ByteArrayCache(false, 2);

    // RendererContext statistics
    final RendererStats stats;

    final PathConsumer2DAdapter p2dAdapter = new PathConsumer2DAdapter();

    /**
     * Constructor
     *
     * @param name context name (debugging)
     */
    RendererContext(final String name) {
        if (LOG_CREATE_CONTEXT) {
            MarlinUtils.logInfo("new RendererContext = " + name);
        }
        this.cleanerObj = new Object();

        // create first stats (needed by newOffHeapArray):
        if (DO_STATS || DO_MONITORS) {
            stats = RendererStats.createInstance(cleanerObj, name);
            // push cache stats:
            stats.cacheStats = new CacheStats[] { cleanIntCache.stats,
                dirtyIntCache.stats, dirtyDoubleCache.stats, dirtyByteCache.stats
            };
        } else {
            stats = null;
        }

        // NormalizingPathIterator instances:
        nPCPathIterator = new NormalizingPathIterator.NearestPixelCenter(double6);
        nPQPathIterator  = new NormalizingPathIterator.NearestPixelQuarter(double6);

        // curve monotonizer & clip subdivider (before transformerPC2D init)
        monotonizer = new CurveBasicMonotonizer(this);
        curveClipSplitter = new CurveClipSplitter(this);

        // MarlinRenderingEngine.TransformingPathConsumer2D
        transformerPC2D = new TransformingPathConsumer2D(this);

        // Renderer:
        cache = new MarlinCache(this);
        renderer = new Renderer(this); // needs MarlinCache from rdrCtx.cache
        ptg = new MarlinTileGenerator(stats, renderer, cache);

        stroker = new Stroker(this);
        dasher = new Dasher(this);
    }

    /**
     * Disposes this renderer context:
     * clean up before reusing this context
     */
    void dispose() {
        if (DO_STATS) {
            if (stats.totalOffHeap > stats.totalOffHeapMax) {
                stats.totalOffHeapMax = stats.totalOffHeap;
            }
            stats.totalOffHeap = 0L;
        }
        stroking   = 0;
        doClip     = false;
        closedPath = false;
        clipInvScale = 0.0d;

        // if context is maked as DIRTY:
        if (dirty) {
            // may happen if an exception if thrown in the pipeline processing:
            // force cleanup of all possible pipelined blocks (except Renderer):

            // NormalizingPathIterator instances:
            this.nPCPathIterator.dispose();
            this.nPQPathIterator.dispose();
            // Dasher:
            this.dasher.dispose();
            // Stroker:
            this.stroker.dispose();

            // mark context as CLEAN:
            dirty = false;
        }
    }

    Path2D.Double getPath2D() {
        // resolve reference:
        Path2D.Double p2d = (refPath2D != null) ? refPath2D.get() : null;

        // create a new Path2D ?
        if (p2d == null) {
            p2d = new Path2D.Double(WIND_NON_ZERO, INITIAL_EDGES_COUNT); // 32K

            // update weak reference:
            refPath2D = new WeakReference<Path2D.Double>(p2d);
        }
        // reset the path anyway:
        p2d.reset();
        return p2d;
    }

    RendererStats stats() {
        return stats;
    }

    OffHeapArray newOffHeapArray(final long initialSize) {
        if (DO_STATS) {
            stats.totalOffHeapInitial += initialSize;
        }
        return new OffHeapArray(cleanerObj, initialSize);
    }

    IntArrayCache.Reference newCleanIntArrayRef(final int initialSize) {
        return cleanIntCache.createRef(initialSize);
    }

    IntArrayCache.Reference newDirtyIntArrayRef(final int initialSize) {
        return dirtyIntCache.createRef(initialSize);
    }

    DoubleArrayCache.Reference newDirtyDoubleArrayRef(final int initialSize) {
        return dirtyDoubleCache.createRef(initialSize);
    }

    ByteArrayCache.Reference newDirtyByteArrayRef(final int initialSize) {
        return dirtyByteCache.createRef(initialSize);
    }

    static final class PathConsumer2DAdapter implements DPathConsumer2D {
        private sun.awt.geom.PathConsumer2D out;

        PathConsumer2DAdapter() {}

        PathConsumer2DAdapter init(sun.awt.geom.PathConsumer2D out) {
            this.out = out;
            return this;
        }

        @Override
        public void moveTo(double x0, double y0) {
            out.moveTo((float)x0, (float)y0);
        }

        @Override
        public void lineTo(double x1, double y1) {
            out.lineTo((float)x1, (float)y1);
        }

        @Override
        public void closePath() {
            out.closePath();
        }

        @Override
        public void pathDone() {
            out.pathDone();
        }

        @Override
        public void curveTo(double x1, double y1,
                            double x2, double y2,
                            double x3, double y3)
        {
            out.curveTo((float)x1, (float)y1,
                    (float)x2, (float)y2,
                    (float)x3, (float)y3);
        }

        @Override
        public void quadTo(double x1, double y1, double x2, double y2) {
            out.quadTo((float)x1, (float)y1, (float)x2, (float)y2);
        }

        @Override
        public long getNativeConsumer() {
            throw new InternalError("Not using a native peer");
        }
    }
}
