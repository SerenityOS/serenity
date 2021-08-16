/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.pipe;

/**
 * The API for an object that generates alpha coverage tiles for a given
 * path.
 * The {@link RenderingEngine} will be consulted as a factory to return
 * one of these objects for a given Shape and a given set of rendering
 * attributes.
 * This object will iterate through the bounds of the rendering primitive
 * and return tiles of a constant size as specified by the getTileWidth()
 * and getTileHeight() parameters.
 * The iteration order of the tiles will be as specified by the pseudo-code:
 * <pre>
 *     int bbox[] = {left, top, right, bottom};
 *     AATileGenerator aatg = renderengine.getAATileGenerator(..., bbox);
 *     int tw = aatg.getTileWidth();
 *     int th = aatg.getTileHeight();
 *     byte tile[] = new byte[tw * th];
 *     for (y = top; y < bottom; y += th) {
 *         for (x = left; x < right; x += tw) {
 *             int a = aatg.getTypicalAlpha();
 *             int w = Math.min(tw, right-x);
 *             int h = Math.min(th, bottom-y);
 *             if (a == 0x00) {
 *                 // can skip this tile...
 *                 aatg.nextTile();
 *             } else if (a == 0xff) {
 *                 // can treat this tile like a fillRect
 *                 aatg.nextTile();
 *                 doFill(x, y, w, h);
 *             } else {
 *                 aatg.getAlpha(tile, 0, tw);
 *                 handleAlpha(tile, x, y, w, h);
 *             }
 *         }
 *     }
 *     aatg.dispose();
 * </pre>
 * The bounding box for the iteration will be returned by the
 * {@code RenderingEngine} via an argument to the getAATileGenerator() method.
 */
public interface AATileGenerator {
    /**
     * Gets the width of the tiles that the generator batches output into.
     * @return the width of the standard alpha tile
     */
    public int getTileWidth();

    /**
     * Gets the height of the tiles that the generator batches output into.
     * @return the height of the standard alpha tile
     */
    public int getTileHeight();

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
    public int getTypicalAlpha();

    /**
     * Skips the current tile and moves on to the next tile.
     * Either this method, or the getAlpha() method should be called
     * once per tile, but not both.
     */
    public void nextTile();

    /**
     * Gets the alpha coverage values for the current tile.
     * Either this method, or the nextTile() method should be called
     * once per tile, but not both.
     */
    public void getAlpha(byte[] tile, int offset, int rowstride);

    /**
     * Disposes this tile generator.
     * No further calls will be made on this instance.
     */
    public void dispose();
}
