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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import jdk.internal.ref.CleanerFactory;
import sun.java2d.marlin.ArrayCacheConst.CacheStats;
import static sun.java2d.marlin.MarlinUtils.logInfo;
import sun.java2d.marlin.stats.Histogram;
import sun.java2d.marlin.stats.Monitor;
import sun.java2d.marlin.stats.StatLong;

/**
 * This class gathers global rendering statistics for debugging purposes only
 */
public final class RendererStats implements MarlinConst {

    static RendererStats createInstance(final Object parent, final String name)
    {
        final RendererStats stats = new RendererStats(name);

        // Keep a strong reference to dump it later:
        RendererStatsHolder.getInstance().add(parent, stats);

        return stats;
    }

    public static void dumpStats() {
        RendererStatsHolder.dumpStats();
    }

    // context name (debugging purposes)
    final String name;
    // stats
    final StatLong stat_cache_rowAA
        = new StatLong("cache.rowAA");
    final StatLong stat_cache_rowAAChunk
        = new StatLong("cache.rowAAChunk");
    final StatLong stat_cache_tiles
        = new StatLong("cache.tiles");
    final StatLong stat_rdr_addLine
        = new StatLong("renderer.addLine");
    final StatLong stat_rdr_addLine_skip
        = new StatLong("renderer.addLine.skip");
    final StatLong stat_rdr_curveBreak
        = new StatLong("renderer.curveBreakIntoLinesAndAdd");
    final StatLong stat_rdr_curveBreak_dec
        = new StatLong("renderer.curveBreakIntoLinesAndAdd.dec");
    final StatLong stat_rdr_curveBreak_inc
        = new StatLong("renderer.curveBreakIntoLinesAndAdd.inc");
    final StatLong stat_rdr_quadBreak
        = new StatLong("renderer.quadBreakIntoLinesAndAdd");
    final StatLong stat_rdr_quadBreak_dec
        = new StatLong("renderer.quadBreakIntoLinesAndAdd.dec");
    final StatLong stat_rdr_edges
        = new StatLong("renderer.edges");
    final StatLong stat_rdr_edges_count
        = new StatLong("renderer.edges.count");
    final StatLong stat_rdr_edges_resizes
        = new StatLong("renderer.edges.resize");
    final StatLong stat_rdr_activeEdges
        = new StatLong("renderer.activeEdges");
    final StatLong stat_rdr_activeEdges_updates
        = new StatLong("renderer.activeEdges.updates");
    final StatLong stat_rdr_activeEdges_adds
        = new StatLong("renderer.activeEdges.adds");
    final StatLong stat_rdr_activeEdges_adds_high
        = new StatLong("renderer.activeEdges.adds_high");
    final StatLong stat_rdr_crossings_updates
        = new StatLong("renderer.crossings.updates");
    final StatLong stat_rdr_crossings_sorts
        = new StatLong("renderer.crossings.sorts");
    final StatLong stat_rdr_crossings_bsearch
        = new StatLong("renderer.crossings.bsearch");
    final StatLong stat_rdr_crossings_msorts
        = new StatLong("renderer.crossings.msorts");
    final StatLong stat_str_polystack_curves
        = new StatLong("stroker.polystack.curves");
    final StatLong stat_str_polystack_types
        = new StatLong("stroker.polystack.types");
    final StatLong stat_cpd_polystack_curves
        = new StatLong("closedPathDetector.polystack.curves");
    final StatLong stat_cpd_polystack_types
        = new StatLong("closedPathDetector.polystack.types");
    final StatLong stat_pcf_idxstack_indices
        = new StatLong("pathClipFilter.stack.indices");
    // growable arrays
    final StatLong stat_array_dasher_dasher
        = new StatLong("array.dasher.dasher.d_float");
    final StatLong stat_array_dasher_firstSegmentsBuffer
        = new StatLong("array.dasher.firstSegmentsBuffer.d_float");
    final StatLong stat_array_marlincache_rowAAChunk
        = new StatLong("array.marlincache.rowAAChunk.resize");
    final StatLong stat_array_marlincache_touchedTile
        = new StatLong("array.marlincache.touchedTile.int");
    final StatLong stat_array_renderer_alphaline
        = new StatLong("array.renderer.alphaline.int");
    final StatLong stat_array_renderer_crossings
        = new StatLong("array.renderer.crossings.int");
    final StatLong stat_array_renderer_aux_crossings
        = new StatLong("array.renderer.aux_crossings.int");
    final StatLong stat_array_renderer_edgeBuckets
        = new StatLong("array.renderer.edgeBuckets.int");
    final StatLong stat_array_renderer_edgeBucketCounts
        = new StatLong("array.renderer.edgeBucketCounts.int");
    final StatLong stat_array_renderer_edgePtrs
        = new StatLong("array.renderer.edgePtrs.int");
    final StatLong stat_array_renderer_aux_edgePtrs
        = new StatLong("array.renderer.aux_edgePtrs.int");
    final StatLong stat_array_str_polystack_curves
        = new StatLong("array.stroker.polystack.curves.d_float");
    final StatLong stat_array_str_polystack_types
        = new StatLong("array.stroker.polystack.curveTypes.d_byte");
    final StatLong stat_array_cpd_polystack_curves
        = new StatLong("array.closedPathDetector.polystack.curves.d_float");
    final StatLong stat_array_cpd_polystack_types
        = new StatLong("array.closedPathDetector.polystack.curveTypes.d_byte");
    final StatLong stat_array_pcf_idxstack_indices
        = new StatLong("array.pathClipFilter.stack.indices.d_int");
    // histograms
    final Histogram hist_rdr_edges_count
        = new Histogram("renderer.edges.count");
    final Histogram hist_rdr_crossings
        = new Histogram("renderer.crossings");
    final Histogram hist_rdr_crossings_ratio
        = new Histogram("renderer.crossings.ratio");
    final Histogram hist_rdr_crossings_adds
        = new Histogram("renderer.crossings.adds");
    final Histogram hist_rdr_crossings_msorts
        = new Histogram("renderer.crossings.msorts");
    final Histogram hist_rdr_crossings_msorts_adds
        = new Histogram("renderer.crossings.msorts.adds");
    final Histogram hist_str_polystack_curves
        = new Histogram("stroker.polystack.curves");
    final Histogram hist_tile_generator_alpha
        = new Histogram("tile_generator.alpha");
    final Histogram hist_tile_generator_encoding
        = new Histogram("tile_generator.encoding");
    final Histogram hist_tile_generator_encoding_dist
        = new Histogram("tile_generator.encoding.dist");
    final Histogram hist_tile_generator_encoding_ratio
        = new Histogram("tile_generator.encoding.ratio");
    final Histogram hist_tile_generator_encoding_runLen
        = new Histogram("tile_generator.encoding.runLen");
    final Histogram hist_cpd_polystack_curves
        = new Histogram("closedPathDetector.polystack.curves");
    final Histogram hist_pcf_idxstack_indices
        = new Histogram("pathClipFilter.stack.indices");
    // all stats
    final StatLong[] statistics = new StatLong[]{
        stat_cache_rowAA,
        stat_cache_rowAAChunk,
        stat_cache_tiles,
        stat_rdr_addLine,
        stat_rdr_addLine_skip,
        stat_rdr_curveBreak,
        stat_rdr_curveBreak_dec,
        stat_rdr_curveBreak_inc,
        stat_rdr_quadBreak,
        stat_rdr_quadBreak_dec,
        stat_rdr_edges,
        stat_rdr_edges_count,
        stat_rdr_edges_resizes,
        stat_rdr_activeEdges,
        stat_rdr_activeEdges_updates,
        stat_rdr_activeEdges_adds,
        stat_rdr_activeEdges_adds_high,
        stat_rdr_crossings_updates,
        stat_rdr_crossings_sorts,
        stat_rdr_crossings_bsearch,
        stat_rdr_crossings_msorts,
        stat_str_polystack_types,
        stat_str_polystack_curves,
        stat_cpd_polystack_curves,
        stat_cpd_polystack_types,
        stat_pcf_idxstack_indices,
        hist_rdr_edges_count,
        hist_rdr_crossings,
        hist_rdr_crossings_ratio,
        hist_rdr_crossings_adds,
        hist_rdr_crossings_msorts,
        hist_rdr_crossings_msorts_adds,
        hist_tile_generator_alpha,
        hist_tile_generator_encoding,
        hist_tile_generator_encoding_dist,
        hist_tile_generator_encoding_ratio,
        hist_tile_generator_encoding_runLen,
        hist_str_polystack_curves,
        hist_cpd_polystack_curves,
        hist_pcf_idxstack_indices,
        stat_array_dasher_dasher,
        stat_array_dasher_firstSegmentsBuffer,
        stat_array_marlincache_rowAAChunk,
        stat_array_marlincache_touchedTile,
        stat_array_renderer_alphaline,
        stat_array_renderer_crossings,
        stat_array_renderer_aux_crossings,
        stat_array_renderer_edgeBuckets,
        stat_array_renderer_edgeBucketCounts,
        stat_array_renderer_edgePtrs,
        stat_array_renderer_aux_edgePtrs,
        stat_array_str_polystack_curves,
        stat_array_str_polystack_types,
        stat_array_cpd_polystack_curves,
        stat_array_cpd_polystack_types,
        stat_array_pcf_idxstack_indices
    };
    // monitors
    final Monitor mon_pre_getAATileGenerator
        = new Monitor("MarlinRenderingEngine.getAATileGenerator()");
    final Monitor mon_rdr_addLine
        = new Monitor("Renderer.addLine()");
    final Monitor mon_rdr_endRendering
        = new Monitor("Renderer.endRendering()");
    final Monitor mon_rdr_endRendering_Y
        = new Monitor("Renderer._endRendering(Y)");
    final Monitor mon_rdr_copyAARow
        = new Monitor("Renderer.copyAARow()");
    final Monitor mon_pipe_renderTiles
        = new Monitor("AAShapePipe.renderTiles()");
    final Monitor mon_ptg_getAlpha
        = new Monitor("MarlinTileGenerator.getAlpha()");
    final Monitor mon_debug
        = new Monitor("DEBUG()");
    // all monitors
    final Monitor[] monitors = new Monitor[]{
        mon_pre_getAATileGenerator,
        mon_rdr_addLine,
        mon_rdr_endRendering,
        mon_rdr_endRendering_Y,
        mon_rdr_copyAARow,
        mon_pipe_renderTiles,
        mon_ptg_getAlpha,
        mon_debug
    };
    // offheap stats
    long totalOffHeapInitial = 0L;
     // live accumulator
    long totalOffHeap = 0L;
    long totalOffHeapMax = 0L;
    // cache stats
    CacheStats[] cacheStats = null;

    private RendererStats(final String name) {
        this.name = name;
    }

    void dump() {
        logInfo("RendererContext: " + name);

        if (DO_MONITORS) {
            for (Monitor monitor : monitors) {
                if (monitor.count != 0) {
                    logInfo(monitor.toString());
                }
            }
            // As getAATileGenerator percents:
            final long total = mon_pre_getAATileGenerator.sum;
            if (total != 0L) {
                for (Monitor monitor : monitors) {
                    logInfo(monitor.name + " : "
                            + ((100d * monitor.sum) / total) + " %");
                }
            }
            if (DO_FLUSH_MONITORS) {
                for (Monitor m : monitors) {
                    m.reset();
                }
            }
        }

        if (DO_STATS) {
            for (StatLong stat : statistics) {
                if (stat.count != 0) {
                    logInfo(stat.toString());
                    if (DO_FLUSH_STATS) {
                        stat.reset();
                    }
                }
            }

            logInfo("OffHeap footprint: initial: " + totalOffHeapInitial
                + " bytes - max: " + totalOffHeapMax + " bytes");
            if (DO_FLUSH_STATS) {
                totalOffHeapMax = 0L;
            }

            logInfo("Array caches for RendererContext: " + name);

            long totalInitialBytes = totalOffHeapInitial;
            long totalCacheBytes   = 0L;

            if (cacheStats != null) {
                for (CacheStats stat : cacheStats) {
                    totalCacheBytes   += stat.dumpStats();
                    totalInitialBytes += stat.getTotalInitialBytes();
                    if (DO_FLUSH_STATS) {
                        stat.reset();
                    }
                }
            }
            logInfo("Heap footprint: initial: " + totalInitialBytes
                    + " bytes - cache: " + totalCacheBytes + " bytes");
        }
    }

    static final class RendererStatsHolder {

        // singleton
        private static volatile RendererStatsHolder SINGLETON = null;

        static synchronized RendererStatsHolder getInstance() {
            if (SINGLETON == null) {
                SINGLETON = new RendererStatsHolder();
            }
            return SINGLETON;
        }

        static void dumpStats() {
            if (SINGLETON != null) {
                SINGLETON.dump();
            }
        }

        /* RendererStats collection as hard references
           (only used for debugging purposes) */
        private final ConcurrentLinkedQueue<RendererStats> allStats
            = new ConcurrentLinkedQueue<RendererStats>();

        @SuppressWarnings("removal")
        private RendererStatsHolder() {
            AccessController.doPrivileged(
                (PrivilegedAction<Void>) () -> {
                    final Thread hook = new Thread(
                        MarlinUtils.getRootThreadGroup(),
                        new Runnable() {
                            @Override
                            public void run() {
                                dump();
                            }
                        },
                        "MarlinStatsHook"
                    );
                    hook.setContextClassLoader(null);
                    Runtime.getRuntime().addShutdownHook(hook);

                    if (USE_DUMP_THREAD) {
                        final Timer statTimer = new Timer("RendererStats");
                        statTimer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                dump();
                            }
                        }, DUMP_INTERVAL, DUMP_INTERVAL);
                    }
                    return null;
                }
            );
        }

        void add(final Object parent, final RendererStats stats) {
            allStats.add(stats);

            // Register a cleaning function to ensure removing dead entries:
            CleanerFactory.cleaner().register(parent, () -> remove(stats));
        }

        void remove(final RendererStats stats) {
            stats.dump(); // dump anyway
            allStats.remove(stats);
        }

        void dump() {
            for (RendererStats stats : allStats) {
                stats.dump();
            }
        }
    }
}
