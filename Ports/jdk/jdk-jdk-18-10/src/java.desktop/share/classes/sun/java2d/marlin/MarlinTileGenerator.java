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

import java.util.Arrays;
import sun.java2d.pipe.AATileGenerator;
import jdk.internal.misc.Unsafe;

final class MarlinTileGenerator implements AATileGenerator, MarlinConst {

    private static final boolean DISABLE_BLEND = false;

    private static final int MAX_TILE_ALPHA_SUM = TILE_W * TILE_H * MAX_AA_ALPHA;

    private static final int TH_AA_ALPHA_FILL_EMPTY = ((MAX_AA_ALPHA + 1) / 3); // 33%
    private static final int TH_AA_ALPHA_FILL_FULL  = ((MAX_AA_ALPHA + 1) * 2 / 3); // 66%

    private static final int FILL_TILE_W = TILE_W >> 1; // half tile width

    static {
        if (MAX_TILE_ALPHA_SUM <= 0) {
            throw new IllegalStateException("Invalid MAX_TILE_ALPHA_SUM: " + MAX_TILE_ALPHA_SUM);
        }
        if (DO_TRACE) {
            MarlinUtils.logInfo("MAX_AA_ALPHA           : " + MAX_AA_ALPHA);
            MarlinUtils.logInfo("TH_AA_ALPHA_FILL_EMPTY : " + TH_AA_ALPHA_FILL_EMPTY);
            MarlinUtils.logInfo("TH_AA_ALPHA_FILL_FULL  : " + TH_AA_ALPHA_FILL_FULL);
            MarlinUtils.logInfo("FILL_TILE_W            : " + FILL_TILE_W);
        }
    }

    private final Renderer renderer;
    private final MarlinCache cache;
    private int x, y;

    // per-thread renderer stats
    final RendererStats rdrStats;

    MarlinTileGenerator(final RendererStats stats, final Renderer r,
                        final MarlinCache cache)
    {
        this.rdrStats = stats;
        this.renderer = r;
        this.cache = cache;
    }

    MarlinTileGenerator init() {
        this.x = cache.bboxX0;
        this.y = cache.bboxY0;

        return this; // fluent API
    }

    /**
     * Disposes this tile generator:
     * clean up before reusing this instance
     */
    @Override
    public void dispose() {
        if (DO_MONITORS) {
            // called from AAShapePipe.renderTiles() (render tiles end):
            rdrStats.mon_pipe_renderTiles.stop();
        }
        // dispose cache:
        cache.dispose();
        // dispose renderer and recycle the RendererContext instance:
        renderer.dispose();
    }

    void getBbox(int[] bbox) {
        bbox[0] = cache.bboxX0;
        bbox[1] = cache.bboxY0;
        bbox[2] = cache.bboxX1;
        bbox[3] = cache.bboxY1;
    }

    /**
     * Gets the width of the tiles that the generator batches output into.
     * @return the width of the standard alpha tile
     */
    @Override
    public int getTileWidth() {
        if (DO_MONITORS) {
            // called from AAShapePipe.renderTiles() (render tiles start):
            rdrStats.mon_pipe_renderTiles.start();
        }
        return TILE_W;
    }

    /**
     * Gets the height of the tiles that the generator batches output into.
     * @return the height of the standard alpha tile
     */
    @Override
    public int getTileHeight() {
        return TILE_H;
    }

    /**
     * Gets the typical alpha value that will characterize the current
     * tile.
     * The answer may be 0x00 to indicate that the current tile has
     * no coverage in any of its pixels, or it may be 0xff to indicate
     * that the current tile is completely covered by the path, or any
     * other value to indicate non-trivial coverage cases.
     * @return 0x00 for no coverage, 0xff for total coverage, or any other
     *         value for partial coverage of the tile
     */
    @Override
    public int getTypicalAlpha() {
        if (DISABLE_BLEND) {
            // always return empty tiles to disable blending operations
            return 0x00;
        }
        int al = cache.alphaSumInTile(x);
        // Note: if we have a filled rectangle that doesn't end on a tile
        // border, we could still return 0xff, even though al!=maxTileAlphaSum
        // This is because if we return 0xff, our users will fill a rectangle
        // starting at x,y that has width = Math.min(TILE_SIZE, bboxX1-x),
        // and height min(TILE_SIZE,bboxY1-y), which is what should happen.
        // However, to support this, we would have to use 2 Math.min's
        // and 2 multiplications per tile, instead of just 2 multiplications
        // to compute maxTileAlphaSum. The savings offered would probably
        // not be worth it, considering how rare this case is.
        // Note: I have not tested this, so in the future if it is determined
        // that it is worth it, it should be implemented. Perhaps this method's
        // interface should be changed to take arguments the width and height
        // of the current tile. This would eliminate the 2 Math.min calls that
        // would be needed here, since our caller needs to compute these 2
        // values anyway.
        final int alpha = (al == 0x00 ? 0x00
                              : (al == MAX_TILE_ALPHA_SUM ? 0xff : 0x80));
        if (DO_STATS) {
            rdrStats.hist_tile_generator_alpha.add(alpha);
        }
        return alpha;
    }

    /**
     * Skips the current tile and moves on to the next tile.
     * Either this method, or the getAlpha() method should be called
     * once per tile, but not both.
     */
    @Override
    public void nextTile() {
        if ((x += TILE_W) >= cache.bboxX1) {
            x = cache.bboxX0;
            y += TILE_H;

            if (y < cache.bboxY1) {
                // compute for the tile line
                // [ y; max(y + TILE_SIZE, bboxY1) ]
                renderer.endRendering(y);
            }
        }
    }

    /**
     * Gets the alpha coverage values for the current tile.
     * Either this method, or the nextTile() method should be called
     * once per tile, but not both.
     */
    @Override
    public void getAlpha(final byte[] tile, final int offset,
                                            final int rowstride)
    {
        if (cache.useRLE) {
            getAlphaRLE(tile, offset, rowstride);
        } else {
            getAlphaNoRLE(tile, offset, rowstride);
        }
    }

    /**
     * Gets the alpha coverage values for the current tile.
     * Either this method, or the nextTile() method should be called
     * once per tile, but not both.
     */
    private void getAlphaNoRLE(final byte[] tile, final int offset,
                               final int rowstride)
    {
        if (DO_MONITORS) {
            rdrStats.mon_ptg_getAlpha.start();
        }

        // local vars for performance:
        final MarlinCache _cache = this.cache;
        final long[] rowAAChunkIndex = _cache.rowAAChunkIndex;
        final int[] rowAAx0 = _cache.rowAAx0;
        final int[] rowAAx1 = _cache.rowAAx1;

        final int x0 = this.x;
        final int x1 = FloatMath.min(x0 + TILE_W, _cache.bboxX1);

        // note: process tile line [0 - 32[
        final int y0 = 0;
        final int y1 = FloatMath.min(this.y + TILE_H, _cache.bboxY1) - this.y;

        if (DO_LOG_BOUNDS) {
            MarlinUtils.logInfo("getAlpha = [" + x0 + " ... " + x1
                                + "[ [" + y0 + " ... " + y1 + "[");
        }

        final Unsafe _unsafe = OffHeapArray.UNSAFE;
        final long SIZE = 1L;
        final long addr_rowAA = _cache.rowAAChunk.address;
        long addr;

        final int skipRowPixels = (rowstride - (x1 - x0));

        int aax0, aax1, end;
        int idx = offset;

        for (int cy = y0, cx; cy < y1; cy++) {
            // empty line (default)
            cx = x0;

            aax1 = rowAAx1[cy]; // exclusive

            // quick check if there is AA data
            // corresponding to this tile [x0; x1[
            if (aax1 > x0) {
                aax0 = rowAAx0[cy]; // inclusive

                if (aax0 < x1) {
                    // note: cx is the cursor pointer in the tile array
                    // (left to right)
                    cx = aax0;

                    // ensure cx >= x0
                    if (cx <= x0) {
                        cx = x0;
                    } else {
                        // fill line start until first AA pixel rowAA exclusive:
                        for (end = x0; end < cx; end++) {
                            tile[idx++] = 0;
                        }
                    }

                    // now: cx >= x0 and cx >= aax0

                    // Copy AA data (sum alpha data):
                    addr = addr_rowAA + rowAAChunkIndex[cy] + (cx - aax0);

                    for (end = (aax1 <= x1) ? aax1 : x1; cx < end; cx++) {
                        // cx inside tile[x0; x1[ :
                        tile[idx++] = _unsafe.getByte(addr); // [0-255]
                        addr += SIZE;
                    }
                }
            }

            // fill line end
            while (cx < x1) {
                tile[idx++] = 0;
                cx++;
            }

            if (DO_TRACE) {
                for (int i = idx - (x1 - x0); i < idx; i++) {
                    System.out.print(hex(tile[i], 2));
                }
                System.out.println();
            }

            idx += skipRowPixels;
        }

        nextTile();

        if (DO_MONITORS) {
            rdrStats.mon_ptg_getAlpha.stop();
        }
    }

    /**
     * Gets the alpha coverage values for the current tile.
     * Either this method, or the nextTile() method should be called
     * once per tile, but not both.
     */
    private void getAlphaRLE(final byte[] tile, final int offset,
                             final int rowstride)
    {
        if (DO_MONITORS) {
            rdrStats.mon_ptg_getAlpha.start();
        }

        // Decode run-length encoded alpha mask data
        // The data for row j begins at cache.rowOffsetsRLE[j]
        // and is encoded as a set of 2-byte pairs (val, runLen)
        // terminated by a (0, 0) pair.

        // local vars for performance:
        final MarlinCache _cache = this.cache;
        final long[] rowAAChunkIndex = _cache.rowAAChunkIndex;
        final int[] rowAAx0 = _cache.rowAAx0;
        final int[] rowAAx1 = _cache.rowAAx1;
        final int[] rowAAEnc = _cache.rowAAEnc;
        final long[] rowAALen = _cache.rowAALen;
        final long[] rowAAPos = _cache.rowAAPos;

        final int x0 = this.x;
        final int x1 = FloatMath.min(x0 + TILE_W, _cache.bboxX1);
        final int w  = x1 - x0;

        // note: process tile line [0 - 32[
        final int y0 = 0;
        final int y1 = FloatMath.min(this.y + TILE_H, _cache.bboxY1) - this.y;

        if (DO_LOG_BOUNDS) {
            MarlinUtils.logInfo("getAlpha = [" + x0 + " ... " + x1
                                + "[ [" + y0 + " ... " + y1 + "[");
        }

        // avoid too small area: fill is not faster !
        final int clearTile;
        final byte refVal;
        final int area;

        if ((w >= FILL_TILE_W) && (area = w * y1) > 64) { // 64 / 4 ie 16 words min (faster)
            final int alphaSum = cache.alphaSumInTile(x0);

            if (alphaSum < area * TH_AA_ALPHA_FILL_EMPTY) {
                clearTile = 1;
                refVal = 0;
            } else if (alphaSum > area * TH_AA_ALPHA_FILL_FULL) {
                clearTile = 2;
                refVal = (byte)0xff;
            } else {
                clearTile = 0;
                refVal = 0;
            }
        } else {
            clearTile = 0;
            refVal = 0;
        }

        final Unsafe _unsafe = OffHeapArray.UNSAFE;
        final long SIZE_BYTE = 1L;
        final long SIZE_INT = 4L;
        final long addr_rowAA = _cache.rowAAChunk.address;
        long addr, addr_row, last_addr, addr_end;

        final int skipRowPixels = (rowstride - w);

        int cx, cy, cx1;
        int rx0, rx1, runLen, end;
        int packed;
        byte val;
        int idx = offset;

        switch (clearTile) {
        case 1: // 0x00
            // Clear full tile rows:
            Arrays.fill(tile, offset, offset + (y1 * rowstride), refVal);

            for (cy = y0; cy < y1; cy++) {
                // empty line (default)
                cx = x0;

                if (rowAAEnc[cy] == 0) {
                    // Raw encoding:

                    final int aax1 = rowAAx1[cy]; // exclusive

                    // quick check if there is AA data
                    // corresponding to this tile [x0; x1[
                    if (aax1 > x0) {
                        final int aax0 = rowAAx0[cy]; // inclusive

                        if (aax0 < x1) {
                            // note: cx is the cursor pointer in the tile array
                            // (left to right)
                            cx = aax0;

                            // ensure cx >= x0
                            if (cx <= x0) {
                                cx = x0;
                            } else {
                                // skip line start until first AA pixel rowAA exclusive:
                                idx += (cx - x0); // > 0
                            }

                            // now: cx >= x0 and cx >= aax0

                            // Copy AA data (sum alpha data):
                            addr = addr_rowAA + rowAAChunkIndex[cy] + (cx - aax0);

                            for (end = (aax1 <= x1) ? aax1 : x1; cx < end; cx++) {
                                tile[idx++] = _unsafe.getByte(addr); // [0-255]
                                addr += SIZE_BYTE;
                            }
                        }
                    }
                } else {
                    // RLE encoding:

                    // quick check if there is AA data
                    // corresponding to this tile [x0; x1[
                    if (rowAAx1[cy] > x0) { // last pixel exclusive

                        cx = rowAAx0[cy]; // inclusive
                        if (cx > x1) {
                            cx = x1;
                        }

                        // skip line start until first AA pixel rowAA exclusive:
                        if (cx > x0) {
                            idx += (cx - x0); // > 0
                        }

                        // get row address:
                        addr_row = addr_rowAA + rowAAChunkIndex[cy];
                        // get row end address:
                        addr_end = addr_row + rowAALen[cy]; // coded length

                        // reuse previous iteration position:
                        addr = addr_row + rowAAPos[cy];

                        last_addr = 0L;

                        while ((cx < x1) && (addr < addr_end)) {
                            // keep current position:
                            last_addr = addr;

                            // packed value:
                            packed = _unsafe.getInt(addr);

                            // last exclusive pixel x-coordinate:
                            cx1 = (packed >> 8);
                            // as bytes:
                            addr += SIZE_INT;

                            rx0 = cx;
                            if (rx0 < x0) {
                                rx0 = x0;
                            }
                            rx1 = cx = cx1;
                            if (rx1 > x1) {
                                rx1 = x1;
                                cx  = x1; // fix last x
                            }
                            // adjust runLen:
                            runLen = rx1 - rx0;

                            // ensure rx1 > rx0:
                            if (runLen > 0) {
                                packed &= 0xFF; // [0-255]

                                if (packed == 0)
                                {
                                    idx += runLen;
                                    continue;
                                }
                                val = (byte) packed; // [0-255]
                                do {
                                    tile[idx++] = val;
                                } while (--runLen > 0);
                            }
                        }

                        // Update last position in RLE entries:
                        if (last_addr != 0L) {
                            // Fix x0:
                            rowAAx0[cy]  = cx; // inclusive
                            // Fix position:
                            rowAAPos[cy] = (last_addr - addr_row);
                        }
                    }
                }

                // skip line end
                if (cx < x1) {
                    idx += (x1 - cx); // > 0
                }

                if (DO_TRACE) {
                    for (int i = idx - (x1 - x0); i < idx; i++) {
                        System.out.print(hex(tile[i], 2));
                    }
                    System.out.println();
                }

                idx += skipRowPixels;
            }
        break;

        case 0:
        default:
            for (cy = y0; cy < y1; cy++) {
                // empty line (default)
                cx = x0;

                if (rowAAEnc[cy] == 0) {
                    // Raw encoding:

                    final int aax1 = rowAAx1[cy]; // exclusive

                    // quick check if there is AA data
                    // corresponding to this tile [x0; x1[
                    if (aax1 > x0) {
                        final int aax0 = rowAAx0[cy]; // inclusive

                        if (aax0 < x1) {
                            // note: cx is the cursor pointer in the tile array
                            // (left to right)
                            cx = aax0;

                            // ensure cx >= x0
                            if (cx <= x0) {
                                cx = x0;
                            } else {
                                for (end = x0; end < cx; end++) {
                                    tile[idx++] = 0;
                                }
                            }

                            // now: cx >= x0 and cx >= aax0

                            // Copy AA data (sum alpha data):
                            addr = addr_rowAA + rowAAChunkIndex[cy] + (cx - aax0);

                            for (end = (aax1 <= x1) ? aax1 : x1; cx < end; cx++) {
                                tile[idx++] = _unsafe.getByte(addr); // [0-255]
                                addr += SIZE_BYTE;
                            }
                        }
                    }
                } else {
                    // RLE encoding:

                    // quick check if there is AA data
                    // corresponding to this tile [x0; x1[
                    if (rowAAx1[cy] > x0) { // last pixel exclusive

                        cx = rowAAx0[cy]; // inclusive
                        if (cx > x1) {
                            cx = x1;
                        }

                        // fill line start until first AA pixel rowAA exclusive:
                        for (end = x0; end < cx; end++) {
                            tile[idx++] = 0;
                        }

                        // get row address:
                        addr_row = addr_rowAA + rowAAChunkIndex[cy];
                        // get row end address:
                        addr_end = addr_row + rowAALen[cy]; // coded length

                        // reuse previous iteration position:
                        addr = addr_row + rowAAPos[cy];

                        last_addr = 0L;

                        while ((cx < x1) && (addr < addr_end)) {
                            // keep current position:
                            last_addr = addr;

                            // packed value:
                            packed = _unsafe.getInt(addr);

                            // last exclusive pixel x-coordinate:
                            cx1 = (packed >> 8);
                            // as bytes:
                            addr += SIZE_INT;

                            rx0 = cx;
                            if (rx0 < x0) {
                                rx0 = x0;
                            }
                            rx1 = cx = cx1;
                            if (rx1 > x1) {
                                rx1 = x1;
                                cx  = x1; // fix last x
                            }
                            // adjust runLen:
                            runLen = rx1 - rx0;

                            // ensure rx1 > rx0:
                            if (runLen > 0) {
                                packed &= 0xFF; // [0-255]

                                val = (byte) packed; // [0-255]
                                do {
                                    tile[idx++] = val;
                                } while (--runLen > 0);
                            }
                        }

                        // Update last position in RLE entries:
                        if (last_addr != 0L) {
                            // Fix x0:
                            rowAAx0[cy]  = cx; // inclusive
                            // Fix position:
                            rowAAPos[cy] = (last_addr - addr_row);
                        }
                    }
                }

                // fill line end
                while (cx < x1) {
                    tile[idx++] = 0;
                    cx++;
                }

                if (DO_TRACE) {
                    for (int i = idx - (x1 - x0); i < idx; i++) {
                        System.out.print(hex(tile[i], 2));
                    }
                    System.out.println();
                }

                idx += skipRowPixels;
            }
        break;

        case 2: // 0xFF
            // Fill full tile rows:
            Arrays.fill(tile, offset, offset + (y1 * rowstride), refVal);

            for (cy = y0; cy < y1; cy++) {
                // empty line (default)
                cx = x0;

                if (rowAAEnc[cy] == 0) {
                    // Raw encoding:

                    final int aax1 = rowAAx1[cy]; // exclusive

                    // quick check if there is AA data
                    // corresponding to this tile [x0; x1[
                    if (aax1 > x0) {
                        final int aax0 = rowAAx0[cy]; // inclusive

                        if (aax0 < x1) {
                            // note: cx is the cursor pointer in the tile array
                            // (left to right)
                            cx = aax0;

                            // ensure cx >= x0
                            if (cx <= x0) {
                                cx = x0;
                            } else {
                                // fill line start until first AA pixel rowAA exclusive:
                                for (end = x0; end < cx; end++) {
                                    tile[idx++] = 0;
                                }
                            }

                            // now: cx >= x0 and cx >= aax0

                            // Copy AA data (sum alpha data):
                            addr = addr_rowAA + rowAAChunkIndex[cy] + (cx - aax0);

                            for (end = (aax1 <= x1) ? aax1 : x1; cx < end; cx++) {
                                tile[idx++] = _unsafe.getByte(addr); // [0-255]
                                addr += SIZE_BYTE;
                            }
                        }
                    }
                } else {
                    // RLE encoding:

                    // quick check if there is AA data
                    // corresponding to this tile [x0; x1[
                    if (rowAAx1[cy] > x0) { // last pixel exclusive

                        cx = rowAAx0[cy]; // inclusive
                        if (cx > x1) {
                            cx = x1;
                        }

                        // fill line start until first AA pixel rowAA exclusive:
                        for (end = x0; end < cx; end++) {
                            tile[idx++] = 0;
                        }

                        // get row address:
                        addr_row = addr_rowAA + rowAAChunkIndex[cy];
                        // get row end address:
                        addr_end = addr_row + rowAALen[cy]; // coded length

                        // reuse previous iteration position:
                        addr = addr_row + rowAAPos[cy];

                        last_addr = 0L;

                        while ((cx < x1) && (addr < addr_end)) {
                            // keep current position:
                            last_addr = addr;

                            // packed value:
                            packed = _unsafe.getInt(addr);

                            // last exclusive pixel x-coordinate:
                            cx1 = (packed >> 8);
                            // as bytes:
                            addr += SIZE_INT;

                            rx0 = cx;
                            if (rx0 < x0) {
                                rx0 = x0;
                            }
                            rx1 = cx = cx1;
                            if (rx1 > x1) {
                                rx1 = x1;
                                cx  = x1; // fix last x
                            }
                            // adjust runLen:
                            runLen = rx1 - rx0;

                            // ensure rx1 > rx0:
                            if (runLen > 0) {
                                packed &= 0xFF; // [0-255]

                                if (packed == 0xFF)
                                {
                                    idx += runLen;
                                    continue;
                                }
                                val = (byte) packed; // [0-255]
                                do {
                                    tile[idx++] = val;
                                } while (--runLen > 0);
                            }
                        }

                        // Update last position in RLE entries:
                        if (last_addr != 0L) {
                            // Fix x0:
                            rowAAx0[cy]  = cx; // inclusive
                            // Fix position:
                            rowAAPos[cy] = (last_addr - addr_row);
                        }
                    }
                }

                // fill line end
                while (cx < x1) {
                    tile[idx++] = 0;
                    cx++;
                }

                if (DO_TRACE) {
                    for (int i = idx - (x1 - x0); i < idx; i++) {
                        System.out.print(hex(tile[i], 2));
                    }
                    System.out.println();
                }

                idx += skipRowPixels;
            }
        }

        nextTile();

        if (DO_MONITORS) {
            rdrStats.mon_ptg_getAlpha.stop();
        }
    }

    static String hex(int v, int d) {
        String s = Integer.toHexString(v);
        while (s.length() < d) {
            s = "0" + s;
        }
        return s.substring(0, d);
    }
}
