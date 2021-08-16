/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.misc.Unsafe;

/**
 * An object used to cache pre-rendered complex paths.
 *
 * @see Renderer
 */
public final class MarlinCache implements MarlinConst {

    static final boolean FORCE_RLE = MarlinProperties.isForceRLE();
    static final boolean FORCE_NO_RLE = MarlinProperties.isForceNoRLE();
    // minimum width to try using RLE encoding:
    static final int RLE_MIN_WIDTH
        = Math.max(BLOCK_SIZE, MarlinProperties.getRLEMinWidth());
    // maximum width for RLE encoding:
    // values are stored as int [x|alpha] where alpha is 8 bits
    static final int RLE_MAX_WIDTH = 1 << (24 - 1);

    // 4096 (pixels) alpha values (width) x 64 rows / 4 (tile) = 64K bytes
    // x1 instead of 4 bytes (RLE) ie 1/4 capacity or average good RLE compression
    static final long INITIAL_CHUNK_ARRAY = TILE_H * INITIAL_PIXEL_WIDTH >> 2; // 64K

    // The alpha map used by this object (taken out of our map cache) to convert
    // pixel coverage counts gotten from MarlinCache (which are in the range
    // [0, maxalpha]) into alpha values, which are in [0,256).
    static final byte[] ALPHA_MAP;

    static final OffHeapArray ALPHA_MAP_UNSAFE;

    static {
        final byte[] _ALPHA_MAP = buildAlphaMap(MAX_AA_ALPHA);

        ALPHA_MAP_UNSAFE = new OffHeapArray(_ALPHA_MAP, _ALPHA_MAP.length); // 1K
        ALPHA_MAP =_ALPHA_MAP;

        final Unsafe _unsafe = OffHeapArray.UNSAFE;
        final long addr = ALPHA_MAP_UNSAFE.address;

        for (int i = 0; i < _ALPHA_MAP.length; i++) {
            _unsafe.putByte(addr + i, _ALPHA_MAP[i]);
        }
    }

    int bboxX0, bboxY0, bboxX1, bboxY1;

    // 1D dirty arrays
    // row index in rowAAChunk[]
    final long[] rowAAChunkIndex = new long[TILE_H];
    // first pixel (inclusive) for each row
    final int[] rowAAx0 = new int[TILE_H];
    // last pixel (exclusive) for each row
    final int[] rowAAx1 = new int[TILE_H];
    // encoding mode (0=raw, 1=RLE encoding) for each row
    final int[] rowAAEnc = new int[TILE_H];
    // coded length (RLE encoding) for each row
    final long[] rowAALen = new long[TILE_H];
    // last position in RLE decoding for each row (getAlpha):
    final long[] rowAAPos = new long[TILE_H];

    // dirty off-heap array containing pixel coverages for (32) rows (packed)
    // if encoding=raw, it contains alpha coverage values (val) as integer
    // if encoding=RLE, it contains tuples (val, last x-coordinate exclusive)
    // use rowAAx0/rowAAx1 to get row indices within this chunk
    final OffHeapArray rowAAChunk;

    // current position in rowAAChunk array
    long rowAAChunkPos;

    // touchedTile[i] is the sum of all the alphas in the tile with
    // x=j*TILE_SIZE+bboxX0.
    int[] touchedTile;

    // per-thread renderer stats
    final RendererStats rdrStats;

    // touchedTile ref (clean)
    private final IntArrayCache.Reference touchedTile_ref;

    int tileMin, tileMax;

    boolean useRLE = false;

    MarlinCache(final RendererContext rdrCtx) {
        this.rdrStats = rdrCtx.stats();

        rowAAChunk = rdrCtx.newOffHeapArray(INITIAL_CHUNK_ARRAY); // 64K

        touchedTile_ref = rdrCtx.newCleanIntArrayRef(INITIAL_ARRAY); // 1K = 1 tile line
        touchedTile     = touchedTile_ref.initial;

        // tile used marks:
        tileMin = Integer.MAX_VALUE;
        tileMax = Integer.MIN_VALUE;
    }

    void init(int minx, int miny, int maxx, int maxy)
    {
        // assert maxy >= miny && maxx >= minx;
        bboxX0 = minx;
        bboxY0 = miny;
        bboxX1 = maxx;
        bboxY1 = maxy;

        final int width = (maxx - minx);

        if (FORCE_NO_RLE) {
            useRLE = false;
        } else if (FORCE_RLE) {
            useRLE = true;
        } else {
            // heuristics: use both bbox area and complexity
            // ie number of primitives:

            // fast check min and max width (maxx < 23bits):
            useRLE = (width > RLE_MIN_WIDTH && width < RLE_MAX_WIDTH);
        }

        // the ceiling of (maxy - miny + 1) / TILE_SIZE;
        final int nxTiles = (width + TILE_W) >> TILE_W_LG;

        if (nxTiles > INITIAL_ARRAY) {
            if (DO_STATS) {
                rdrStats.stat_array_marlincache_touchedTile.add(nxTiles);
            }
            touchedTile = touchedTile_ref.getArray(nxTiles);
        }
    }

    /**
     * Disposes this cache:
     * clean up before reusing this instance
     */
    void dispose() {
        // Reset touchedTile if needed:
        resetTileLine(0);

        if (DO_STATS) {
            rdrStats.totalOffHeap += rowAAChunk.length;
        }

        // Return arrays:
        touchedTile = touchedTile_ref.putArray(touchedTile, 0, 0); // already zero filled

        // At last: resize back off-heap rowAA to initial size
        if (rowAAChunk.length != INITIAL_CHUNK_ARRAY) {
            // note: may throw OOME:
            rowAAChunk.resize(INITIAL_CHUNK_ARRAY);
        }
        if (DO_CLEAN_DIRTY) {
            // Force zero-fill dirty arrays:
            rowAAChunk.fill(BYTE_0);
        }
    }

    void resetTileLine(final int pminY) {
        // update bboxY0 to process a complete tile line [0 - 32]
        bboxY0 = pminY;

        // reset current pos
        if (DO_STATS) {
            rdrStats.stat_cache_rowAAChunk.add(rowAAChunkPos);
        }
        rowAAChunkPos = 0L;

        // Reset touchedTile:
        if (tileMin != Integer.MAX_VALUE) {
            if (DO_STATS) {
                rdrStats.stat_cache_tiles.add(tileMax - tileMin);
            }
            // clean only dirty touchedTile:
            if (tileMax == 1) {
                touchedTile[0] = 0;
            } else {
                IntArrayCache.fill(touchedTile, tileMin, tileMax, 0);
            }
            // reset tile used marks:
            tileMin = Integer.MAX_VALUE;
            tileMax = Integer.MIN_VALUE;
        }

        if (DO_CLEAN_DIRTY) {
            // Force zero-fill dirty arrays:
            rowAAChunk.fill(BYTE_0);
        }
    }

    void clearAARow(final int y) {
        // process tile line [0 - 32]
        final int row = y - bboxY0;

        // update pixel range:
        rowAAx0[row]  = 0; // first pixel inclusive
        rowAAx1[row]  = 0; //  last pixel exclusive
        rowAAEnc[row] = 0; // raw encoding

        // note: leave rowAAChunkIndex[row] undefined
        // and rowAALen[row] & rowAAPos[row] (RLE)
    }

    /**
     * Copy the given alpha data into the rowAA cache
     * @param alphaRow alpha data to copy from
     * @param y y pixel coordinate
     * @param px0 first pixel inclusive x0
     * @param px1 last pixel exclusive x1
     */
    void copyAARowNoRLE(final int[] alphaRow, final int y,
                   final int px0, final int px1)
    {
        // skip useless pixels above boundary
        final int px_bbox1 = FloatMath.min(px1, bboxX1);

        if (DO_LOG_BOUNDS) {
            MarlinUtils.logInfo("row = [" + px0 + " ... " + px_bbox1
                                + " (" + px1 + ") [ for y=" + y);
        }

        final int row = y - bboxY0;

        // update pixel range:
        rowAAx0[row]  = px0;      // first pixel inclusive
        rowAAx1[row]  = px_bbox1; //  last pixel exclusive
        rowAAEnc[row] = 0; // raw encoding

        // get current position (bytes):
        final long pos = rowAAChunkPos;
        // update row index to current position:
        rowAAChunkIndex[row] = pos;

        // determine need array size:
        // for RLE encoding, position must be aligned to 4 bytes (int):
        // align - 1 = 3 so add +3 and round-off by mask ~3 = -4
        final long needSize = pos + ((px_bbox1 - px0 + 3) & -4);

        // update next position (bytes):
        rowAAChunkPos = needSize;

        // update row data:
        final OffHeapArray _rowAAChunk = rowAAChunk;
        // ensure rowAAChunk capacity:
        if (_rowAAChunk.length < needSize) {
            expandRowAAChunk(needSize);
        }
        if (DO_STATS) {
            rdrStats.stat_cache_rowAA.add(px_bbox1 - px0);
        }

        // rowAA contains only alpha values for range[x0; x1[
        final int[] _touchedTile = touchedTile;
        final int _TILE_SIZE_LG = TILE_W_LG;

        final int from = px0      - bboxX0; // first pixel inclusive
        final int to   = px_bbox1 - bboxX0; //  last pixel exclusive

        final Unsafe _unsafe = OffHeapArray.UNSAFE;
        final long SIZE_BYTE = 1L;
        final long addr_alpha = ALPHA_MAP_UNSAFE.address;
        long addr_off = _rowAAChunk.address + pos;

        // compute alpha sum into rowAA:
        for (int x = from, val = 0; x < to; x++) {
            // alphaRow is in [0; MAX_COVERAGE]
            val += alphaRow[x]; // [from; to[

            // ensure values are in [0; MAX_AA_ALPHA] range
            if (DO_AA_RANGE_CHECK) {
                if (val < 0) {
                    MarlinUtils.logInfo("Invalid coverage = " + val);
                    val = 0;
                }
                if (val > MAX_AA_ALPHA) {
                    MarlinUtils.logInfo("Invalid coverage = " + val);
                    val = MAX_AA_ALPHA;
                }
            }

            // store alpha sum (as byte):
            if (val == 0) {
                _unsafe.putByte(addr_off, (byte)0); // [0-255]
            } else {
                _unsafe.putByte(addr_off, _unsafe.getByte(addr_alpha + val)); // [0-255]

                // update touchedTile
                _touchedTile[x >> _TILE_SIZE_LG] += val;
            }
            addr_off += SIZE_BYTE;
        }

        // update tile used marks:
        int tx = from >> _TILE_SIZE_LG; // inclusive
        if (tx < tileMin) {
            tileMin = tx;
        }

        tx = ((to - 1) >> _TILE_SIZE_LG) + 1; // exclusive (+1 to be sure)
        if (tx > tileMax) {
            tileMax = tx;
        }

        if (DO_LOG_BOUNDS) {
            MarlinUtils.logInfo("clear = [" + from + " ... " + to + "[");
        }

        // Clear alpha row for reuse:
        IntArrayCache.fill(alphaRow, from, px1 + 1 - bboxX0, 0);
    }

    void copyAARowRLE_WithBlockFlags(final int[] blkFlags, final int[] alphaRow,
                      final int y, final int px0, final int px1)
    {
        // Copy rowAA data into the piscesCache if one is present
        final int _bboxX0 = bboxX0;

        // process tile line [0 - 32]
        final int row  =   y -  bboxY0;
        final int from = px0 - _bboxX0; // first pixel inclusive

        // skip useless pixels above boundary
        final int px_bbox1 = FloatMath.min(px1, bboxX1);
        final int to       = px_bbox1 - _bboxX0; //  last pixel exclusive

        if (DO_LOG_BOUNDS) {
            MarlinUtils.logInfo("row = [" + px0 + " ... " + px_bbox1
                                + " (" + px1 + ") [ for y=" + y);
        }

        // get current position:
        final long initialPos = startRLERow(row, px0, px_bbox1);

        // determine need array size:
        // pessimistic: max needed size = deltaX x 4 (1 int)
        final long needSize = initialPos + ((to - from) << 2);

        // update row data:
        OffHeapArray _rowAAChunk = rowAAChunk;
        // ensure rowAAChunk capacity:
        if (_rowAAChunk.length < needSize) {
            expandRowAAChunk(needSize);
        }

        final Unsafe _unsafe = OffHeapArray.UNSAFE;
        final long SIZE_INT = 4L;
        final long addr_alpha = ALPHA_MAP_UNSAFE.address;
        long addr_off = _rowAAChunk.address + initialPos;

        final int[] _touchedTile = touchedTile;
        final int _TILE_SIZE_LG = TILE_W_LG;
        final int _BLK_SIZE_LG  = BLOCK_SIZE_LG;

        // traverse flagged blocks:
        final int blkW = (from >> _BLK_SIZE_LG);
        final int blkE = (to   >> _BLK_SIZE_LG) + 1;
        // ensure last block flag = 0 to process final block:
        blkFlags[blkE] = 0;

        // Perform run-length encoding and store results in the piscesCache
        int val = 0;
        int cx0 = from;
        int runLen;

        final int _MAX_VALUE = Integer.MAX_VALUE;
        int last_t0 = _MAX_VALUE;

        int skip = 0;

        for (int t = blkW, blk_x0, blk_x1, cx, delta; t <= blkE; t++) {
            if (blkFlags[t] != 0) {
                blkFlags[t] = 0;

                if (last_t0 == _MAX_VALUE) {
                    last_t0 = t;
                }
                continue;
            }
            if (last_t0 != _MAX_VALUE) {
                // emit blocks:
                blk_x0 = FloatMath.max(last_t0 << _BLK_SIZE_LG, from);
                last_t0 = _MAX_VALUE;

                // (last block pixel+1) inclusive => +1
                blk_x1 = FloatMath.min((t << _BLK_SIZE_LG) + 1, to);

                for (cx = blk_x0; cx < blk_x1; cx++) {
                    if ((delta = alphaRow[cx]) != 0) {
                        alphaRow[cx] = 0;

                        // not first rle entry:
                        if (cx != cx0) {
                            runLen = cx - cx0;

                            // store alpha coverage (ensure within bounds):
                            // as [absX|val] where:
                            // absX is the absolute x-coordinate:
                            // note: last pixel exclusive (>= 0)
                            // note: it should check X is smaller than 23bits (overflow)!

                            // check address alignment to 4 bytes:
                            if (DO_CHECK_UNSAFE) {
                                if ((addr_off & 3) != 0) {
                                    MarlinUtils.logInfo("Misaligned Unsafe address: " + addr_off);
                                }
                            }

                            // special case to encode entries into a single int:
                            if (val == 0) {
                                _unsafe.putInt(addr_off,
                                    ((_bboxX0 + cx) << 8)
                                );
                            } else {
                                _unsafe.putInt(addr_off,
                                    ((_bboxX0 + cx) << 8)
                                    | (((int) _unsafe.getByte(addr_alpha + val)) & 0xFF) // [0-255]
                                );

                                if (runLen == 1) {
                                    _touchedTile[cx0 >> _TILE_SIZE_LG] += val;
                                } else {
                                    touchTile(cx0, val, cx, runLen, _touchedTile);
                                }
                            }
                            addr_off += SIZE_INT;

                            if (DO_STATS) {
                                rdrStats.hist_tile_generator_encoding_runLen
                                    .add(runLen);
                            }
                            cx0 = cx;
                        }

                        // alpha value = running sum of coverage delta:
                        val += delta;

                        // ensure values are in [0; MAX_AA_ALPHA] range
                        if (DO_AA_RANGE_CHECK) {
                            if (val < 0) {
                                MarlinUtils.logInfo("Invalid coverage = " + val);
                                val = 0;
                            }
                            if (val > MAX_AA_ALPHA) {
                                MarlinUtils.logInfo("Invalid coverage = " + val);
                                val = MAX_AA_ALPHA;
                            }
                        }
                    }
                }
            } else if (DO_STATS) {
                skip++;
            }
        }

        // Process remaining RLE run:
        runLen = to - cx0;

        // store alpha coverage (ensure within bounds):
        // as (int)[absX|val] where:
        // absX is the absolute x-coordinate in bits 31 to 8 and val in bits 0..7
        // note: last pixel exclusive (>= 0)
        // note: it should check X is smaller than 23bits (overflow)!

        // check address alignment to 4 bytes:
        if (DO_CHECK_UNSAFE) {
            if ((addr_off & 3) != 0) {
                MarlinUtils.logInfo("Misaligned Unsafe address: " + addr_off);
            }
        }

        // special case to encode entries into a single int:
        if (val == 0) {
            _unsafe.putInt(addr_off,
                ((_bboxX0 + to) << 8)
            );
        } else {
            _unsafe.putInt(addr_off,
                ((_bboxX0 + to) << 8)
                | (((int) _unsafe.getByte(addr_alpha + val)) & 0xFF) // [0-255]
            );

            if (runLen == 1) {
                _touchedTile[cx0 >> _TILE_SIZE_LG] += val;
            } else {
                touchTile(cx0, val, to, runLen, _touchedTile);
            }
        }
        addr_off += SIZE_INT;

        if (DO_STATS) {
            rdrStats.hist_tile_generator_encoding_runLen.add(runLen);
        }

        long len = (addr_off - _rowAAChunk.address);

        // update coded length as bytes:
        rowAALen[row] = (len - initialPos);

        // update current position:
        rowAAChunkPos = len;

        if (DO_STATS) {
            rdrStats.stat_cache_rowAA.add(rowAALen[row]);
            rdrStats.hist_tile_generator_encoding_ratio.add(
                (100 * skip) / (blkE - blkW)
            );
        }

        // update tile used marks:
        int tx = from >> _TILE_SIZE_LG; // inclusive
        if (tx < tileMin) {
            tileMin = tx;
        }

        tx = ((to - 1) >> _TILE_SIZE_LG) + 1; // exclusive (+1 to be sure)
        if (tx > tileMax) {
            tileMax = tx;
        }

        // Clear alpha row for reuse:
        alphaRow[to] = 0;
        if (DO_CHECKS) {
            IntArrayCache.check(blkFlags, blkW, blkE, 0);
            IntArrayCache.check(alphaRow, from, px1 + 1 - bboxX0, 0);
        }
    }

    long startRLERow(final int row, final int x0, final int x1) {
        // rows are supposed to be added by increasing y.
        rowAAx0[row]  = x0; // first pixel inclusive
        rowAAx1[row]  = x1; // last pixel exclusive
        rowAAEnc[row] = 1; // RLE encoding
        rowAAPos[row] = 0L; // position = 0

        // update row index to current position:
        return (rowAAChunkIndex[row] = rowAAChunkPos);
    }

    private void expandRowAAChunk(final long needSize) {
        if (DO_STATS) {
            rdrStats.stat_array_marlincache_rowAAChunk.add(needSize);
        }

        // note: throw IOOB if neededSize > 2Gb:
        final long newSize = ArrayCacheConst.getNewLargeSize(rowAAChunk.length,
                                                             needSize);

        rowAAChunk.resize(newSize);
    }

    private void touchTile(final int x0, final int val, final int x1,
                           final int runLen,
                           final int[] _touchedTile)
    {
        // the x and y of the current row, minus bboxX0, bboxY0
        // process tile line [0 - 32]
        final int _TILE_SIZE_LG = TILE_W_LG;

        // update touchedTile
        int tx = (x0 >> _TILE_SIZE_LG);

        // handle trivial case: same tile (x0, x0+runLen)
        if (tx == (x1 >> _TILE_SIZE_LG)) {
            // same tile:
            _touchedTile[tx] += val * runLen;
            return;
        }

        final int tx1 = (x1 - 1) >> _TILE_SIZE_LG;

        if (tx <= tx1) {
            final int nextTileXCoord = (tx + 1) << _TILE_SIZE_LG;
            _touchedTile[tx++] += val * (nextTileXCoord - x0);
        }
        if (tx < tx1) {
            // don't go all the way to tx1 - we need to handle the last
            // tile as a special case (just like we did with the first
            final int tileVal = (val << _TILE_SIZE_LG);
            for (; tx < tx1; tx++) {
                _touchedTile[tx] += tileVal;
            }
        }
        // they will be equal unless x0 >> TILE_SIZE_LG == tx1
        if (tx == tx1) {
            final int txXCoord       =  tx      << _TILE_SIZE_LG;
            final int nextTileXCoord = (tx + 1) << _TILE_SIZE_LG;

            final int lastXCoord = (nextTileXCoord <= x1) ? nextTileXCoord : x1;
            _touchedTile[tx] += val * (lastXCoord - txXCoord);
        }
    }

    int alphaSumInTile(final int x) {
        return touchedTile[(x - bboxX0) >> TILE_W_LG];
    }

    @Override
    public String toString() {
        return "bbox = ["
            + bboxX0 + ", " + bboxY0 + " => "
            + bboxX1 + ", " + bboxY1 + "]\n";
    }

    private static byte[] buildAlphaMap(final int maxalpha) {
        // double size !
        final byte[] alMap = new byte[maxalpha << 1];
        final int halfmaxalpha = maxalpha >> 2;
        for (int i = 0; i <= maxalpha; i++) {
            alMap[i] = (byte) ((i * 255 + halfmaxalpha) / maxalpha);
        }
        return alMap;
    }
}
