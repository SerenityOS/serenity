/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.xr;

import java.awt.*;
import java.util.*;

/**
 * We render non-antialiased geometry (consisting of rectangles) into a mask,
 * which is later used in a composition step.
 * To avoid mask-allocations of large size, MaskTileManager splits
 * geometry larger than MASK_SIZE into several tiles,
 * and stores the geometry in instances of MaskTile.
 *
 * @author Clemens Eisserer
 */

public class MaskTileManager {

    public static final int MASK_SIZE = 256;

    MaskTile mainTile = new MaskTile();

    ArrayList<MaskTile> tileList;
    int allocatedTiles = 0;
    int xTiles, yTiles;

    XRCompositeManager xrMgr;
    XRBackend con;

    int maskPixmap;
    int maskPicture;
    long maskGC;

    public MaskTileManager(XRCompositeManager xrMgr, int parentXid) {
        tileList = new ArrayList<MaskTile>();
        this.xrMgr = xrMgr;
        this.con = xrMgr.getBackend();

        maskPixmap = con.createPixmap(parentXid, 8, MASK_SIZE, MASK_SIZE);
        maskPicture = con.createPicture(maskPixmap, XRUtils.PictStandardA8);
        con.renderRectangle(maskPicture, XRUtils.PictOpClear,
                            new XRColor(Color.black),
                            0, 0, MASK_SIZE, MASK_SIZE);
        maskGC = con.createGC(maskPixmap);
        con.setGCExposures(maskGC, false);
    }

    /**
     * Transfers the geometry stored (rectangles, lines) to one or more masks,
     * and renders the result to the destination surface.
     */
    public void fillMask(XRSurfaceData dst) {

        boolean maskRequired = xrMgr.maskRequired();
        boolean maskEvaluated = XRUtils.isMaskEvaluated(xrMgr.compRule);

        if (maskRequired && maskEvaluated) {
            mainTile.calculateDirtyAreas();
            DirtyRegion dirtyArea = mainTile.getDirtyArea().cloneRegion();
            mainTile.translate(-dirtyArea.x, -dirtyArea.y);

            XRColor maskColor = xrMgr.getMaskColor();

            // We don't need tiling if all geometry fits in a single tile
            if (dirtyArea.getWidth() <= MASK_SIZE &&
                dirtyArea.getHeight() <= MASK_SIZE)
            {
                compositeSingleTile(dst, mainTile, dirtyArea,
                                     maskRequired, 0, 0, maskColor);
            } else {
                allocTiles(dirtyArea);
                tileRects();

                for (int i = 0; i < yTiles; i++) {
                    for (int m = 0; m < xTiles; m++) {
                        MaskTile tile = tileList.get(i * xTiles + m);

                        int tileStartX = m * MASK_SIZE;
                        int tileStartY = i * MASK_SIZE;
                        compositeSingleTile(dst, tile, dirtyArea, maskRequired,
                                            tileStartX, tileStartY, maskColor);
                    }
                }
            }
        } else {
            /*
             * If a mask would be required to store geometry (maskRequired)
             * composition has to be done rectangle-by-rectagle.
             */
            if(xrMgr.isSolidPaintActive()) {
                xrMgr.XRRenderRectangles(dst, mainTile.getRects());
            } else {
                xrMgr.XRCompositeRectangles(dst, mainTile.getRects());
            }
        }

        mainTile.reset();
    }

    /**
     * Uploads aa geometry generated for maskblit/fill into the mask pixmap.
     */
    public int uploadMask(int w, int h, int maskscan, int maskoff, byte[] mask) {
        int maskPic = XRUtils.None;

        if (mask != null) {
            float maskAlpha =
                 xrMgr.isTexturePaintActive() ? xrMgr.getExtraAlpha() : 1.0f;
            con.putMaskImage(maskPixmap, maskGC, mask, 0, 0, 0, 0,
                             w, h, maskoff, maskscan, maskAlpha);
            maskPic = maskPicture;
        } else if (xrMgr.isTexturePaintActive()) {
            maskPic = xrMgr.getExtraAlphaMask();
         }

        return maskPic;
    }

    /**
     * Clears the area of the mask-pixmap used for uploading aa coverage values.
     */
    public void clearUploadMask(int mask, int w, int h) {
        if (mask == maskPicture) {
            con.renderRectangle(maskPicture, XRUtils.PictOpClear,
                                XRColor.NO_ALPHA, 0, 0, w, h);
        }
    }


    /**
     * Renders the rectangles provided to the mask, and does a composition
     * operation with the properties set inXRCompositeManager.
     */
    protected void compositeSingleTile(XRSurfaceData dst, MaskTile tile,
                                       DirtyRegion dirtyArea,
                                       boolean maskRequired,
                                       int tileStartX, int tileStartY,
                                       XRColor maskColor) {
        if (tile.rects.getSize() > 0) {
            DirtyRegion tileDirtyArea = tile.getDirtyArea();

            int x = tileDirtyArea.x + tileStartX + dirtyArea.x;
            int y = tileDirtyArea.y + tileStartY + dirtyArea.y;
            int width = tileDirtyArea.x2 - tileDirtyArea.x;
            int height = tileDirtyArea.y2 - tileDirtyArea.y;
            width = Math.min(width, MASK_SIZE);
            height = Math.min(height, MASK_SIZE);

            int rectCnt = tile.rects.getSize();

            if (maskRequired) {
                int mask = XRUtils.None;

                /*
                 * Optimization: When the tile only contains one rectangle, the
                 * composite-operation boundaries can be used as geometry
                 */
                if (rectCnt > 1) {
                    con.renderRectangles(maskPicture, XRUtils.PictOpSrc,
                                         maskColor, tile.rects);
                    mask = maskPicture;
                } else {
                    if (xrMgr.isTexturePaintActive()) {
                        mask = xrMgr.getExtraAlphaMask();
                    }
                }

                xrMgr.XRComposite(XRUtils.None, mask, dst.getPicture(),
                                  x, y, tileDirtyArea.x, tileDirtyArea.y,
                                  x, y, width, height);

                /* Clear dirty rectangle of the rect-mask */
                if (rectCnt > 1) {
                    con.renderRectangle(maskPicture, XRUtils.PictOpClear,
                                        XRColor.NO_ALPHA,
                                        tileDirtyArea.x, tileDirtyArea.y,
                                        width, height);
                }

                tile.reset();
            } else if (rectCnt > 0) {
                tile.rects.translateRects(tileStartX + dirtyArea.x,
                                          tileStartY + dirtyArea.y);
                xrMgr.XRRenderRectangles(dst, tile.rects);
            }
        }
    }


    /**
     * Allocates enough MaskTile instances, to cover the whole
     * mask area, or resets existing ones.
     */
    protected void allocTiles(DirtyRegion maskArea) {
        xTiles = (maskArea.getWidth() / MASK_SIZE) + 1;
        yTiles = (maskArea.getHeight() / MASK_SIZE) + 1;
        int tileCnt = xTiles * yTiles;

        if (tileCnt > allocatedTiles) {
            for (int i = 0; i < tileCnt; i++) {
                if (i < allocatedTiles) {
                    tileList.get(i).reset();
                } else {
                    tileList.add(new MaskTile());
                }
            }

            allocatedTiles = tileCnt;
        }
    }

    /**
     * Tiles the stored rectangles, if they are larger than the MASK_SIZE
     */
    protected void tileRects() {
        GrowableRectArray rects = mainTile.rects;

        for (int i = 0; i < rects.getSize(); i++) {
            int tileXStartIndex = rects.getX(i) / MASK_SIZE;
            int tileYStartIndex = rects.getY(i) / MASK_SIZE;
            int tileXLength =
                ((rects.getX(i) + rects.getWidth(i)) / MASK_SIZE + 1) -
                 tileXStartIndex;
            int tileYLength =
                 ((rects.getY(i) + rects.getHeight(i)) / MASK_SIZE + 1) -
                 tileYStartIndex;

            for (int n = 0; n < tileYLength; n++) {
                for (int m = 0; m < tileXLength; m++) {

                    int tileIndex =
                         xTiles * (tileYStartIndex + n) + tileXStartIndex + m;
                    MaskTile tile = tileList.get(tileIndex);

                    GrowableRectArray rectTileList = tile.getRects();
                    int tileArrayIndex = rectTileList.getNextIndex();

                    int tileStartPosX = (tileXStartIndex + m) * MASK_SIZE;
                    int tileStartPosY = (tileYStartIndex + n) * MASK_SIZE;

                    rectTileList.setX(tileArrayIndex, rects.getX(i) - tileStartPosX);
                    rectTileList.setY(tileArrayIndex, rects.getY(i) - tileStartPosY);
                    rectTileList.setWidth(tileArrayIndex, rects.getWidth(i));
                    rectTileList.setHeight(tileArrayIndex, rects.getHeight(i));

                    limitRectCoords(rectTileList, tileArrayIndex);

                    tile.getDirtyArea().growDirtyRegion
                       (rectTileList.getX(tileArrayIndex),
                        rectTileList.getY(tileArrayIndex),
                        rectTileList.getWidth(tileArrayIndex) +
                             rectTileList.getX(tileArrayIndex),
                        rectTileList.getHeight(tileArrayIndex) +
                            rectTileList.getY(tileArrayIndex));
                }
            }
        }
    }

    /**
     * Limits the rect's coordinates to the mask coordinates. The result is used
     * by growDirtyRegion.
     */
    private void limitRectCoords(GrowableRectArray rects, int index) {
        if ((rects.getX(index) + rects.getWidth(index)) > MASK_SIZE) {
            rects.setWidth(index, MASK_SIZE - rects.getX(index));
        }
        if ((rects.getY(index) + rects.getHeight(index)) > MASK_SIZE) {
            rects.setHeight(index, MASK_SIZE - rects.getY(index));
        }
        if (rects.getX(index) < 0) {
            rects.setWidth(index, rects.getWidth(index) + rects.getX(index));
            rects.setX(index, 0);
        }
        if (rects.getY(index) < 0) {
            rects.setHeight(index, rects.getHeight(index) + rects.getY(index));
            rects.setY(index, 0);
        }
    }

    /**
     * @return MainTile to which rectangles are added before composition.
     */
    public MaskTile getMainTile() {
        return mainTile;
     }
}
