/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>
#include "jni.h"
#include "MTLGlyphCache.h"
#include "Trace.h"

/**
 * When the cache is full, we will try to reuse the cache cells that have
 * been used relatively less than the others (and we will save the cells that
 * have been rendered more than the threshold defined here).
 */
#define TIMES_RENDERED_THRESHOLD 5

/**
 * Creates a new GlyphCacheInfo structure, fills in the initial values, and
 * then returns a pointer to the GlyphCacheInfo record.
 *
 * Note that this method only sets up a data structure describing a
 * rectangular region of accelerated memory, containing "virtual" cells of
 * the requested size.  The cell information is added lazily to the linked
 * list describing the cache as new glyphs are added.  Platform specific
 * glyph caching code is responsible for actually creating the accelerated
 * memory surface that will contain the individual glyph images.
 *
 * Each glyph contains a reference to a list of cell infos - one per glyph
 * cache. There may be multiple glyph caches (for example, one per graphics
 * adapter), so if the glyph is cached on two devices its cell list will
 * consists of two elements corresponding to different glyph caches.
 *
 * The platform-specific glyph caching code is supposed to use
 * GetCellInfoForCache method for retrieving cache infos from the glyph's list.
 *
 * Note that if it is guaranteed that there will be only one global glyph
 * cache then it one does not have to use AccelGlyphCache_GetCellInfoForCache
 * for retrieving cell info for the glyph, but instead just use the struct's
 * field directly.
 */
MTLGlyphCacheInfo *
MTLGlyphCache_Init(jint width, jint height,
                     jint cellWidth, jint cellHeight,
                     MTLFlushFunc *func)
{
    MTLGlyphCacheInfo *gcinfo;

    J2dTraceLn(J2D_TRACE_INFO, "MTLGlyphCache_Init");

    gcinfo = (MTLGlyphCacheInfo *)malloc(sizeof(MTLGlyphCacheInfo));
    if (gcinfo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "MTLGlyphCache_Init: could not allocate MTLGlyphCacheInfo");
        return NULL;
    }

    gcinfo->head = NULL;
    gcinfo->tail = NULL;
    gcinfo->width = width;
    gcinfo->height = height;
    gcinfo->cellWidth = cellWidth;
    gcinfo->cellHeight = cellHeight;
    gcinfo->Flush = func;

    return gcinfo;
}

/**
 * Attempts to add the provided glyph to the specified cache.  If the
 * operation is successful, a pointer to the newly occupied cache cell is
 * stored in the glyph's cellInfo field; otherwise, its cellInfo field is
 * set to NULL, indicating that the glyph's original bits should be rendered
 * instead.  If the cache is full, the least-recently-used glyph is
 * invalidated and its cache cell is reassigned to the new glyph being added.
 *
 * Note that this method only ensures that a rectangular region in the
 * "virtual" glyph cache is available for the glyph image.  Platform specific
 * glyph caching code is responsible for actually caching the glyph image
 * in the associated accelerated memory surface.
 *
 * Returns created cell info if it was successfully created and added to the
 * cache and glyph's cell lists, NULL otherwise.
 */
MTLCacheCellInfo *
MTLGlyphCache_AddGlyph(MTLGlyphCacheInfo *cache, GlyphInfo *glyph)
{
    MTLCacheCellInfo *cellinfo = NULL;
    jint w = glyph->width;
    jint h = glyph->height;

    J2dTraceLn(J2D_TRACE_INFO, "MTLGlyphCache_AddGlyph");

    if ((glyph->width > cache->cellWidth) ||
        (glyph->height > cache->cellHeight))
    {
        return NULL;
    }

    jint x, y;

    if (cache->head == NULL) {
        x = 0;
        y = 0;
    } else {
        x = cache->tail->x + cache->cellWidth;
        y = cache->tail->y;
        if ((x + cache->cellWidth) > cache->width) {
            x = 0;
            y += cache->cellHeight;
        }
    }

    // create new CacheCellInfo
    cellinfo = (MTLCacheCellInfo *)malloc(sizeof(MTLCacheCellInfo));
    if (cellinfo == NULL) {
        J2dTraceLn(J2D_TRACE_ERROR, "could not allocate CellInfo");
        return NULL;
    }

    cellinfo->cacheInfo = cache;
    cellinfo->glyphInfo = glyph;
    cellinfo->timesRendered = 0;
    cellinfo->x = x;
    cellinfo->y = y;
    cellinfo->leftOff = 0;
    cellinfo->rightOff = 0;
    cellinfo->tx1 = (jfloat)cellinfo->x / cache->width;
    cellinfo->ty1 = (jfloat)cellinfo->y / cache->height;
    cellinfo->tx2 = cellinfo->tx1 + ((jfloat)w / cache->width);
    cellinfo->ty2 = cellinfo->ty1 + ((jfloat)h / cache->height);

    if (cache->head == NULL) {
        // initialize the head cell
        cache->head = cellinfo;
    } else {
        // update existing tail cell
        cache->tail->next = cellinfo;
    }

    // add the new cell to the end of the list
    cache->tail = cellinfo;
    cellinfo->next = NULL;
    cellinfo->nextGCI = NULL;

    // add cache cell to the glyph's cells list
    MTLGlyphCache_AddCellInfo(glyph, cellinfo);
    return cellinfo;
}


bool
MTLGlyphCache_IsCacheFull(MTLGlyphCacheInfo *cache, GlyphInfo *glyph)
{
    jint w = glyph->width;
    jint h = glyph->height;

    J2dTraceLn(J2D_TRACE_INFO, "MTLGlyphCache_IsCacheFull");

    jint x, y;

    if (cache->head == NULL) {
        return JNI_FALSE;
    } else {
        x = cache->tail->x + cache->cellWidth;
        y = cache->tail->y;
        if ((x + cache->cellWidth) > cache->width) {
            x = 0;
            y += cache->cellHeight;
            if ((y + cache->cellHeight) > cache->height) {
                return JNI_TRUE;
            }
        }
    }
    return JNI_FALSE;
}
/**
 * Invalidates all cells in the cache.  Note that this method does not
 * attempt to compact the cache in any way; it just invalidates any cells
 * that already exist.
 */
void
MTLGlyphCache_Invalidate(MTLGlyphCacheInfo *cache)
{
    MTLCacheCellInfo *cellinfo;

    J2dTraceLn(J2D_TRACE_INFO, "MTLGlyphCache_Invalidate");

    if (cache == NULL) {
        return;
    }

    // flush any pending vertices that may be depending on the current
    // glyph cache layout
    if (cache->Flush != NULL) {
        cache->Flush();
    }

    cellinfo = cache->head;
    while (cellinfo != NULL) {
        if (cellinfo->glyphInfo != NULL) {
            // if the cell is occupied, notify the base glyph that its
            // cached version for this cache is about to be invalidated
            MTLGlyphCache_RemoveCellInfo(cellinfo->glyphInfo, cellinfo);
        }
        cellinfo = cellinfo->next;
    }
}

/**
 * Invalidates and frees all cells and the cache itself. The "cache" pointer
 * becomes invalid after this function returns.
 */
void
MTLGlyphCache_Free(MTLGlyphCacheInfo *cache)
{
    MTLCacheCellInfo *cellinfo;

    J2dTraceLn(J2D_TRACE_INFO, "MTLGlyphCache_Free");

    if (cache == NULL) {
        return;
    }

    // flush any pending vertices that may be depending on the current
    // glyph cache
    if (cache->Flush != NULL) {
        cache->Flush();
    }

    while (cache->head != NULL) {
        cellinfo = cache->head;
        if (cellinfo->glyphInfo != NULL) {
            // if the cell is occupied, notify the base glyph that its
            // cached version for this cache is about to be invalidated
            MTLGlyphCache_RemoveCellInfo(cellinfo->glyphInfo, cellinfo);
        }
        cache->head = cellinfo->next;
        free(cellinfo);
    }
    free(cache);
}

/**
 * Add cell info to the head of the glyph's list of cached cells.
 */
void
MTLGlyphCache_AddCellInfo(GlyphInfo *glyph, MTLCacheCellInfo *cellInfo)
{
    // assert (glyph != NULL && cellInfo != NULL)
    J2dTraceLn(J2D_TRACE_INFO, "MTLGlyphCache_AddCellInfo");
    J2dTraceLn2(J2D_TRACE_VERBOSE, "  glyph 0x%x: adding cell 0x%x to the list",
                glyph, cellInfo);

    cellInfo->glyphInfo = glyph;
    cellInfo->nextGCI = glyph->cellInfo;
    glyph->cellInfo = cellInfo;
    glyph->managed = MANAGED_GLYPH;
}

/**
 * Removes cell info from the glyph's list of cached cells.
 */
void
MTLGlyphCache_RemoveCellInfo(GlyphInfo *glyph, MTLCacheCellInfo *cellInfo)
{
    MTLCacheCellInfo *currCellInfo = glyph->cellInfo;
    MTLCacheCellInfo *prevInfo = NULL;
    // assert (glyph!= NULL && glyph->cellInfo != NULL && cellInfo != NULL)
    J2dTraceLn(J2D_TRACE_INFO, "MTLGlyphCache_RemoveCellInfo");
    do {
        if (currCellInfo == cellInfo) {
            J2dTraceLn2(J2D_TRACE_VERBOSE,
                        "  glyph 0x%x: removing cell 0x%x from glyph's list",
                        glyph, currCellInfo);
            if (prevInfo == NULL) { // it's the head, chop-chop
                glyph->cellInfo = currCellInfo->nextGCI;
            } else {
                prevInfo->nextGCI = currCellInfo->nextGCI;
            }
            currCellInfo->glyphInfo = NULL;
            currCellInfo->nextGCI = NULL;
            return;
        }
        prevInfo = currCellInfo;
        currCellInfo = currCellInfo->nextGCI;
    } while (currCellInfo != NULL);
    J2dTraceLn2(J2D_TRACE_WARNING, "MTLGlyphCache_RemoveCellInfo: "\
                "no cell 0x%x in glyph 0x%x's cell list",
                cellInfo, glyph);
}

/**
 * Removes cell info from the glyph's list of cached cells.
 */
JNIEXPORT void
MTLGlyphCache_RemoveAllCellInfos(GlyphInfo *glyph)
{
    MTLCacheCellInfo *currCell, *prevCell;

    J2dTraceLn(J2D_TRACE_INFO, "MTLGlyphCache_RemoveAllCellInfos");

    if (glyph == NULL || glyph->cellInfo == NULL) {
        return;
    }

    // invalidate all of this glyph's accelerated cache cells
    currCell = glyph->cellInfo;
    do {
        currCell->glyphInfo = NULL;
        prevCell = currCell;
        currCell = currCell->nextGCI;
        prevCell->nextGCI = NULL;
    } while (currCell != NULL);

    glyph->cellInfo = NULL;
}

/**
 * Returns cell info associated with particular cache from the glyph's list of
 * cached cells.
 */
MTLCacheCellInfo *
MTLGlyphCache_GetCellInfoForCache(GlyphInfo *glyph, MTLGlyphCacheInfo *cache)
{
    // assert (glyph != NULL && cache != NULL)
    J2dTraceLn(J2D_TRACE_VERBOSE2, "MTLGlyphCache_GetCellInfoForCache");

    if (glyph->cellInfo != NULL) {
        MTLCacheCellInfo *cellInfo = glyph->cellInfo;
        do {
            if (cellInfo->cacheInfo == cache) {
                J2dTraceLn3(J2D_TRACE_VERBOSE2,
                            "  glyph 0x%x: found cell 0x%x for cache 0x%x",
                            glyph, cellInfo, cache);
                return cellInfo;
            }
            cellInfo = cellInfo->nextGCI;
        } while (cellInfo != NULL);
    }
    J2dTraceLn2(J2D_TRACE_VERBOSE2, "  glyph 0x%x: no cell for cache 0x%x",
                glyph, cache);
    return NULL;
}
