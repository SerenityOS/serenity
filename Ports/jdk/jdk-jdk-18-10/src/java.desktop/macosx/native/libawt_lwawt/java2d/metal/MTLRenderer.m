/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include <jlong.h>
#include <jni_util.h>
#include <math.h>

#include "sun_java2d_metal_MTLRenderer.h"

#include "MTLRenderer.h"
#include "MTLRenderQueue.h"
#include "MTLSurfaceData.h"
#include "MTLUtils.h"
#import "MTLLayer.h"

/**
 * Note: Some of the methods in this file apply a "magic number"
 * translation to line segments. It is same as what we have in
 * OGLrenderer.
 *
 * The "magic numbers" you see here have been empirically derived
 * after testing on a variety of graphics hardware in order to find some
 * reasonable middle ground between the two specifications.  The general
 * approach is to apply a fractional translation to vertices so that they
 * hit pixel centers and therefore touch the same pixels as in our other
 * pipelines.  Emphasis was placed on finding values so that MTL lines with
 * a slope of +/- 1 hit all the same pixels as our other (software) loops.
 * The stepping in other diagonal lines rendered with MTL may deviate
 * slightly from those rendered with our software loops, but the most
 * important thing is that these magic numbers ensure that all MTL lines
 * hit the same endpoints as our software loops.
 *
 * If you find it necessary to change any of these magic numbers in the
 * future, just be sure that you test the changes across a variety of
 * hardware to ensure consistent rendering everywhere.
 */

void MTLRenderer_DrawLine(MTLContext *mtlc, BMTLSDOps * dstOps, jint x1, jint y1, jint x2, jint y2) {
    if (mtlc == NULL || dstOps == NULL || dstOps->pTexture == NULL) {
        J2dTraceLn(J2D_TRACE_ERROR, "MTLRenderer_DrawLine: dest is null");
        return;
    }

    J2dTraceLn5(J2D_TRACE_INFO, "MTLRenderer_DrawLine (x1=%d y1=%d x2=%d y2=%d), dst tex=%p", x1, y1, x2, y2, dstOps->pTexture);

    id<MTLRenderCommandEncoder> mtlEncoder = [mtlc.encoderManager getRenderEncoder:dstOps];
    if (mtlEncoder == nil)
        return;

    // DrawLine implementation same as in OGLRenderer.c
    struct Vertex verts[2];
    if (y1 == y2) {
        // horizontal
        float fx1 = (float)x1;
        float fx2 = (float)x2;
        float fy  = ((float)y1) + 0.2f;

        if (x1 > x2) {
            float t = fx1; fx1 = fx2; fx2 = t;
        }

        verts[0].position[0] = fx1 + 0.2f;
        verts[0].position[1] = fy;
        verts[1].position[0] = fx2 + 1.2f;
        verts[1].position[1] = fy;
    } else if (x1 == x2) {
        // vertical
        float fx  = ((float)x1) + 0.2f;
        float fy1 = (float)y1;
        float fy2 = (float)y2;

        if (y1 > y2) {
            float t = fy1; fy1 = fy2; fy2 = t;
        }

        verts[0].position[0] = fx;
        verts[0].position[1] = fy1 + 0.2f;
        verts[1].position[0] = fx;
        verts[1].position[1] = fy2 + 1.2f;
    } else {
        // diagonal
        float fx1 = (float)x1;
        float fy1 = (float)y1;
        float fx2 = (float)x2;
        float fy2 = (float)y2;

        if (x1 < x2) {
            fx1 += 0.2f;
            fx2 += 1.0f;
        } else {
            fx1 += 0.8f;
            fx2 -= 0.2f;
        }

        if (y1 < y2) {
            fy1 += 0.2f;
            fy2 += 1.0f;
        } else {
            fy1 += 0.8f;
            fy2 -= 0.2f;
        }
        verts[0].position[0] = fx1;
        verts[0].position[1] = fy1;
        verts[1].position[0] = fx2;
        verts[1].position[1] = fy2;
    }

    [mtlEncoder setVertexBytes:verts length:sizeof(verts) atIndex:MeshVertexBuffer];
    [mtlEncoder drawPrimitives:MTLPrimitiveTypeLine vertexStart:0 vertexCount:2];
}

void MTLRenderer_DrawPixel(MTLContext *mtlc, BMTLSDOps * dstOps, jint x, jint y) {
    if (mtlc == NULL || dstOps == NULL || dstOps->pTexture == NULL) {
        J2dTraceLn(J2D_TRACE_ERROR, "MTLRenderer_DrawPixel: dest is null");
        return;
    }

    id<MTLTexture> dest = dstOps->pTexture;
    J2dTraceLn3(J2D_TRACE_INFO, "MTLRenderer_DrawPixel (x=%d y=%d), dst tex=%p", x, y, dest);

    id<MTLRenderCommandEncoder> mtlEncoder = [mtlc.encoderManager getRenderEncoder:dstOps];
    if (mtlEncoder == nil)
        return;

    // Translate each vertex by a fraction so
    // that we hit pixel centers.
    float fx = (float)x + 0.2f;
    float fy = (float)y + 0.5f;
    struct Vertex vert = {{fx, fy}};
    [mtlEncoder setVertexBytes:&vert length:sizeof(vert) atIndex:MeshVertexBuffer];
    [mtlEncoder drawPrimitives:MTLPrimitiveTypePoint vertexStart:0 vertexCount:1];
}

void MTLRenderer_DrawRect(MTLContext *mtlc, BMTLSDOps * dstOps, jint x, jint y, jint w, jint h) {
    if (mtlc == NULL || dstOps == NULL || dstOps->pTexture == NULL) {
        J2dTraceLn(J2D_TRACE_ERROR, "MTLRenderer_DrawRect: dest is null");
        return;
    }

    id<MTLTexture> dest = dstOps->pTexture;
    J2dTraceLn5(J2D_TRACE_INFO, "MTLRenderer_DrawRect (x=%d y=%d w=%d h=%d), dst tex=%p", x, y, w, h, dest);

    // TODO: use DrawParallelogram(x, y, w, h, lw=1, lh=1)
    id<MTLRenderCommandEncoder> mtlEncoder = [mtlc.encoderManager getRenderEncoder:dstOps];
    if (mtlEncoder == nil)
        return;

    // Translate each vertex by a fraction so
    // that we hit pixel centers.
    const int verticesCount = 5;
    float fx = (float)x + 0.2f;
    float fy = (float)y + 0.5f;
    float fw = (float)w;
    float fh = (float)h;
    struct Vertex vertices[5] = {
            {{fx, fy}},
            {{fx + fw, fy}},
            {{fx + fw, fy + fh}},
            {{fx, fy + fh}},
            {{fx, fy}},
    };
    [mtlEncoder setVertexBytes:vertices length:sizeof(vertices) atIndex:MeshVertexBuffer];
    [mtlEncoder drawPrimitives:MTLPrimitiveTypeLineStrip vertexStart:0 vertexCount:verticesCount];
}

const int POLYLINE_BUF_SIZE = 64;

NS_INLINE void fillVertex(struct Vertex * vertex, int x, int y) {
    vertex->position[0] = x;
    vertex->position[1] = y;
}

void MTLRenderer_DrawPoly(MTLContext *mtlc, BMTLSDOps * dstOps,
                     jint nPoints, jint isClosed,
                     jint transX, jint transY,
                     jint *xPoints, jint *yPoints)
{
    // Note that BufferedRenderPipe.drawPoly() has already rejected polys
    // with nPoints<2, so we can be certain here that we have nPoints>=2.
    if (xPoints == NULL || yPoints == NULL || nPoints < 2) { // just for insurance
        J2dRlsTraceLn(J2D_TRACE_ERROR, "MTLRenderer_DrawPoly: points array is empty");
        return;
    }

    if (mtlc == NULL || dstOps == NULL || dstOps->pTexture == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "MTLRenderer_DrawPoly: dest is null");
        return;
    }

    J2dTraceLn4(J2D_TRACE_INFO, "MTLRenderer_DrawPoly: %d points, transX=%d, transY=%d, dst tex=%p", nPoints, transX, transY, dstOps->pTexture);

    __block struct {
        struct Vertex verts[POLYLINE_BUF_SIZE];
    } pointsChunk;

    // We intend to submit draw commands in batches of POLYLINE_BUF_SIZE vertices at a time
    // Subsequent batches need to be connected - so end point in one batch is repeated as first point in subsequent batch
    // This inflates the total number of points by a factor of number of batches of size POLYLINE_BUF_SIZE
    nPoints += (nPoints/POLYLINE_BUF_SIZE);

    jint prevX = *(xPoints++);
    jint prevY = *(yPoints++);
    const jint firstX = prevX;
    const jint firstY = prevY;
    while (nPoints > 0) {
        const bool isLastChunk = nPoints <= POLYLINE_BUF_SIZE;
        __block int chunkSize = isLastChunk ? nPoints : POLYLINE_BUF_SIZE;

        fillVertex(pointsChunk.verts, prevX + transX + 0.5f, prevY + transY + 0.5f);
        J2dTraceLn2(J2D_TRACE_INFO, "MTLRenderer_DrawPoly: Point - (%1.2f, %1.2f)", prevX + transX + 0.5f, prevY + transY + 0.5f);

        for (int i = 1; i < chunkSize; i++) {
            prevX = *(xPoints++);
            prevY = *(yPoints++);
            fillVertex(pointsChunk.verts + i, prevX + transX + 0.5f, prevY + transY + 0.5f);
            J2dTraceLn2(J2D_TRACE_INFO, "MTLRenderer_DrawPoly: Point - (%1.2f, %1.2f)", prevX + transX + 0.5f,prevY + transY + 0.5f);
        }

        bool drawCloseSegment = false;
        if (isClosed && isLastChunk) {
            if (chunkSize + 2 <= POLYLINE_BUF_SIZE) {
                fillVertex(pointsChunk.verts + chunkSize, firstX + transX + 0.5f, firstY + transY + 0.5f);
                J2dTraceLn2(J2D_TRACE_INFO, "MTLRenderer_DrawPoly: Point - (%1.2f, %1.2f)",firstX + transX + 0.5f, firstY + transY + 0.5f);

                ++chunkSize;
            } else
                drawCloseSegment = true;
        }

        nPoints -= chunkSize;
        id<MTLRenderCommandEncoder> mtlEncoder = [mtlc.encoderManager getRenderEncoder:dstOps];
        if (mtlEncoder == nil)
            return;

        [mtlEncoder setVertexBytes:pointsChunk.verts length:sizeof(pointsChunk.verts) atIndex:MeshVertexBuffer];
        [mtlEncoder drawPrimitives:MTLPrimitiveTypeLineStrip vertexStart:0 vertexCount:chunkSize];

        if (drawCloseSegment) {
            struct Vertex vertices[2] = {
                    {{prevX + transX + 0.5f, prevY + transY + 0.5f}},
                    {{firstX + transX + 0.5f, firstY + transY + 0.5f}}
            };

            J2dTraceLn2(J2D_TRACE_INFO, "MTLRenderer_DrawPoly: last segment Point1 - (%1.2f, %1.2f)",prevX + transX + 0.5f, prevY + transY + 0.5f);
            J2dTraceLn2(J2D_TRACE_INFO, "MTLRenderer_DrawPoly: last segment Point2 - (%1.2f, %1.2f)",firstX + transX + 0.5f, firstY + transY + 0.5f);

            [mtlEncoder setVertexBytes:vertices length:sizeof(vertices) atIndex:MeshVertexBuffer];
            [mtlEncoder drawPrimitives:MTLPrimitiveTypeLine vertexStart:0 vertexCount:2];
        }
    }
}

JNIEXPORT void JNICALL
Java_sun_java2d_metal_MTLRenderer_drawPoly
    (JNIEnv *env, jobject mtlr,
     jintArray xpointsArray, jintArray ypointsArray,
     jint nPoints, jboolean isClosed,
     jint transX, jint transY)
{
    jint *xPoints, *yPoints;

    J2dTraceLn(J2D_TRACE_INFO, "MTLRenderer_drawPoly");

    xPoints = (jint *)
        (*env)->GetPrimitiveArrayCritical(env, xpointsArray, NULL);
    if (xPoints != NULL) {
        yPoints = (jint *)
            (*env)->GetPrimitiveArrayCritical(env, ypointsArray, NULL);
        if (yPoints != NULL) {
            MTLContext *mtlc = MTLRenderQueue_GetCurrentContext();
            BMTLSDOps *dstOps = MTLRenderQueue_GetCurrentDestination();

            MTLRenderer_DrawPoly(mtlc, dstOps,
                                 nPoints, isClosed,
                                 transX, transY,
                                 xPoints, yPoints);
            if (mtlc != NULL) {
                RESET_PREVIOUS_OP();
                [mtlc.encoderManager endEncoder];
                MTLCommandBufferWrapper * cbwrapper = [mtlc pullCommandBufferWrapper];
                id<MTLCommandBuffer> commandbuf = [cbwrapper getCommandBuffer];
                [commandbuf addCompletedHandler:^(id <MTLCommandBuffer> commandbuf) {
                    [cbwrapper release];
                }];
                [commandbuf commit];
            }

            (*env)->ReleasePrimitiveArrayCritical(env, ypointsArray, yPoints,
                                                  JNI_ABORT);
        }
        (*env)->ReleasePrimitiveArrayCritical(env, xpointsArray, xPoints,
                                              JNI_ABORT);
    }
}

const int SCANLINE_MAX_VERTEX_SIZE = 4096;
const int VERTEX_STRUCT_SIZE = 8;
const int NUM_OF_VERTICES_PER_SCANLINE = 2;

void
MTLRenderer_DrawScanlines(MTLContext *mtlc, BMTLSDOps * dstOps,
                          jint scanlineCount, jint *scanlines)
{

    J2dTraceLn2(J2D_TRACE_INFO, "MTLRenderer_DrawScanlines (scanlineCount=%d), dst tex=%p", scanlineCount, dstOps->pTexture);
    if (mtlc == NULL || dstOps == NULL || dstOps->pTexture == NULL) {
            J2dTraceLn(J2D_TRACE_ERROR, "MTLRenderer_DrawScanlines: dest is null");
            return;
    }
    RETURN_IF_NULL(scanlines);
    int vertexSize = NUM_OF_VERTICES_PER_SCANLINE
        * scanlineCount * VERTEX_STRUCT_SIZE;
    J2dTraceLn1(J2D_TRACE_INFO, "MTLRenderer_DrawScanlines: Total vertex size : %d", vertexSize);
    if (vertexSize == 0) return;

    id<MTLRenderCommandEncoder> mtlEncoder = [mtlc.encoderManager getRenderEncoder:dstOps];

    if (mtlEncoder == nil) return;

    if (vertexSize <= SCANLINE_MAX_VERTEX_SIZE) {
        struct Vertex verts[NUM_OF_VERTICES_PER_SCANLINE * scanlineCount];

        for (int j = 0, i = 0; j < scanlineCount; j++) {
            // Translate each vertex by a fraction so
            // that we hit pixel centers.
            float x1 = ((float)*(scanlines++)) + 0.2f;
            float x2 = ((float)*(scanlines++)) + 1.2f;
            float y  = ((float)*(scanlines++)) + 0.5f;
            struct Vertex v1 = {{x1, y}};
            struct Vertex v2 = {{x2, y}};
            verts[i++] = v1;
            verts[i++] = v2;
        }

        [mtlEncoder setVertexBytes:verts length:sizeof(verts) atIndex:MeshVertexBuffer];
        [mtlEncoder drawPrimitives:MTLPrimitiveTypeLine vertexStart:0
            vertexCount:NUM_OF_VERTICES_PER_SCANLINE * scanlineCount];
    } else {
        int remainingScanlineCount = vertexSize;
        do {
            if (remainingScanlineCount > SCANLINE_MAX_VERTEX_SIZE) {
                struct Vertex verts[SCANLINE_MAX_VERTEX_SIZE/ VERTEX_STRUCT_SIZE];

                for (int j = 0, i = 0; j < (SCANLINE_MAX_VERTEX_SIZE / (VERTEX_STRUCT_SIZE * 2)); j++) {
                    // Translate each vertex by a fraction so
                    // that we hit pixel centers.
                    float x1 = ((float)*(scanlines++)) + 0.2f;
                    float x2 = ((float)*(scanlines++)) + 1.2f;
                    float y  = ((float)*(scanlines++)) + 0.5f;
                    struct Vertex v1 = {{x1, y}};
                    struct Vertex v2 = {{x2, y}};
                    verts[i++] = v1;
                    verts[i++] = v2;
                }

                [mtlEncoder setVertexBytes:verts length:sizeof(verts) atIndex:MeshVertexBuffer];
                [mtlEncoder drawPrimitives:MTLPrimitiveTypeLine vertexStart:0
                    vertexCount:(SCANLINE_MAX_VERTEX_SIZE / VERTEX_STRUCT_SIZE)];
                remainingScanlineCount -= SCANLINE_MAX_VERTEX_SIZE;
            } else {
                struct Vertex verts[remainingScanlineCount / VERTEX_STRUCT_SIZE];

                for (int j = 0, i = 0; j < (remainingScanlineCount / (VERTEX_STRUCT_SIZE * 2)); j++) {
                    // Translate each vertex by a fraction so
                    // that we hit pixel centers.
                    float x1 = ((float)*(scanlines++)) + 0.2f;
                    float x2 = ((float)*(scanlines++)) + 1.2f;
                    float y  = ((float)*(scanlines++)) + 0.5f;
                    struct Vertex v1 = {{x1, y}};
                    struct Vertex v2 = {{x2, y}};
                    verts[i++] = v1;
                    verts[i++] = v2;
                }

                [mtlEncoder setVertexBytes:verts length:sizeof(verts) atIndex:MeshVertexBuffer];
                [mtlEncoder drawPrimitives:MTLPrimitiveTypeLine vertexStart:0
                    vertexCount:(remainingScanlineCount / VERTEX_STRUCT_SIZE)];
                remainingScanlineCount -= remainingScanlineCount;
            }
            J2dTraceLn1(J2D_TRACE_INFO,
                "MTLRenderer_DrawScanlines: Remaining vertex size %d", remainingScanlineCount);
        } while (remainingScanlineCount != 0);
    }
}

void
MTLRenderer_FillRect(MTLContext *mtlc, BMTLSDOps * dstOps, jint x, jint y, jint w, jint h)
{
    J2dTraceLn(J2D_TRACE_INFO, "MTLRenderer_FillRect");

    if (mtlc == NULL || dstOps == NULL || dstOps->pTexture == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "MTLRenderer_FillRect: current dest is null");
        return;
    }

    struct Vertex verts[QUAD_VERTEX_COUNT] = {
        { {x, y}},
        { {x, y+h}},
        { {x+w, y}},
        { {x+w, y+h}
    }};


    id<MTLTexture> dest = dstOps->pTexture;
    J2dTraceLn5(J2D_TRACE_INFO, "MTLRenderer_FillRect (x=%d y=%d w=%d h=%d), dst tex=%p", x, y, w, h, dest);

    // Encode render command.
    id<MTLRenderCommandEncoder> mtlEncoder = [mtlc.encoderManager getRenderEncoder:dstOps];
    if (mtlEncoder == nil)
        return;

    [mtlEncoder setVertexBytes:verts length:sizeof(verts) atIndex:MeshVertexBuffer];
    [mtlEncoder drawPrimitives:MTLPrimitiveTypeTriangleStrip vertexStart:0 vertexCount: QUAD_VERTEX_COUNT];
}

void MTLRenderer_FillSpans(MTLContext *mtlc, BMTLSDOps * dstOps, jint spanCount, jint *spans)
{
    J2dTraceLn(J2D_TRACE_INFO, "MTLRenderer_FillSpans");
    if (mtlc == NULL || dstOps == NULL || dstOps->pTexture == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "MTLRenderer_FillSpans: dest is null");
        return;
    }

    // MTLRenderCommandEncoder setVertexBytes usage is recommended if the data is of 4KB.

    // We use a buffer that closely matches the 4KB limit size
    // This buffer is resued multiple times to encode draw calls of a triangle list
    // NOTE : Due to nature of *spans data - it is not possible to use triangle strip.
    // We use triangle list to draw spans

    // Destination texture to which render commands are encoded
    id<MTLTexture> dest = dstOps->pTexture;
    id<MTLTexture> destAA = nil;
    BOOL isDestOpaque = dstOps->isOpaque;
    if (mtlc.clip.stencilMaskGenerationInProgress == JNI_TRUE) {
        dest = dstOps->pStencilData;
        isDestOpaque = NO;
    }
    id<MTLRenderCommandEncoder> mtlEncoder = [mtlc.encoderManager getRenderEncoder:dest isDstOpaque:isDestOpaque];
    if (mtlEncoder == nil) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "MTLRenderer_FillSpans: mtlEncoder is nil");
        return;
    }

    // This is the max no of vertices (of struct Vertex - 8 bytes) we can accomodate in 4KB
    const int TOTAL_VERTICES_IN_BLOCK = 510;
    struct Vertex vertexList[TOTAL_VERTICES_IN_BLOCK]; // a total of 170 triangles ==> 85 spans

    jfloat shapeX1 = mtlc.clip.shapeX;
    jfloat shapeY1 = mtlc.clip.shapeY;
    jfloat shapeX2 = (mtlc.clip.shapeWidth > 0)?  shapeX1 + mtlc.clip.shapeWidth : 0;
    jfloat shapeY2 = (mtlc.clip.shapeHeight > 0)? shapeY1 + mtlc.clip.shapeHeight : 0;

    int counter = 0;
    for (int i = 0; i < spanCount; i++) {
        jfloat x1 = *(spans++);
        jfloat y1 = *(spans++);
        jfloat x2 = *(spans++);
        jfloat y2 = *(spans++);

        if (mtlc.clip.stencilMaskGenerationInProgress == JNI_TRUE) {
            if (shapeX1 > x1) shapeX1 = x1;
            if (shapeY1 > y1) shapeY1 = y1;
            if (shapeX2 < x2) shapeX2 = x2;
            if (shapeY2 < y2) shapeY2 = y2;
        }

        struct Vertex verts[6] = {
            {{x1, y1}},
            {{x1, y2}},
            {{x2, y1}},

            {{x1, y2}},
            {{x2, y1}},
            {{x2, y2}
        }};

        memcpy(&vertexList[counter], &verts, sizeof(verts));
        counter += 6;

        // If vertexList buffer full
        if (counter % TOTAL_VERTICES_IN_BLOCK == 0) {
            [mtlEncoder setVertexBytes:vertexList length:sizeof(vertexList) atIndex:MeshVertexBuffer];
            [mtlEncoder drawPrimitives:MTLPrimitiveTypeTriangle vertexStart:0 vertexCount:TOTAL_VERTICES_IN_BLOCK];
            counter = 0;
        }
    }

    // Draw triangles using remaining vertices if any
    if (counter != 0) {
        [mtlEncoder setVertexBytes:vertexList length:sizeof(vertexList) atIndex:MeshVertexBuffer];
        [mtlEncoder drawPrimitives:MTLPrimitiveTypeTriangle vertexStart:0 vertexCount:counter];
    }

    if (mtlc.clip.stencilMaskGenerationInProgress == JNI_TRUE) {
        if (shapeX1 < 0) shapeX1 = 0;
        if (shapeY1 < 0) shapeY1 = 0;
        if (shapeX1 > dest.width) shapeX1 = dest.width;
        if (shapeY1 > dest.height) shapeY1 = dest.height;
        if (shapeX2 < 0) shapeX2 = 0;
        if (shapeY2 < 0) shapeY2 = 0;
        if (shapeX2 > dest.width) shapeX2 = dest.width;
        if (shapeY2 > dest.height) shapeY2 = dest.height;

        mtlc.clip.shapeX = (NSUInteger) shapeX1;
        mtlc.clip.shapeY = (NSUInteger) shapeY1;
        mtlc.clip.shapeWidth = (NSUInteger) (shapeX2 - shapeX1);
        mtlc.clip.shapeHeight = (NSUInteger) (shapeY2 - shapeY1);
    }
}

void
MTLRenderer_FillParallelogram(MTLContext *mtlc, BMTLSDOps * dstOps,
                              jfloat fx11, jfloat fy11,
                              jfloat dx21, jfloat dy21,
                              jfloat dx12, jfloat dy12)
{

    if (mtlc == NULL || dstOps == NULL || dstOps->pTexture == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "MTLRenderer_FillParallelogram: current dest is null");
        return;
    }

    id<MTLTexture> dest = dstOps->pTexture;
    J2dTraceLn7(J2D_TRACE_INFO,
                "MTLRenderer_FillParallelogram"
                "(x=%6.2f y=%6.2f "
                "dx1=%6.2f dy1=%6.2f "
                "dx2=%6.2f dy2=%6.2f dst tex=%p)",
                fx11, fy11,
                dx21, dy21,
                dx12, dy12, dest);

    struct Vertex verts[QUAD_VERTEX_COUNT] = {
            { {fx11, fy11}},
            { {fx11+dx21, fy11+dy21}},
            { {fx11+dx12, fy11+dy12}},
            { {fx11 + dx21 + dx12, fy11+ dy21 + dy12}
        }};

    // Encode render command.
    id<MTLRenderCommandEncoder> mtlEncoder = [mtlc.encoderManager getRenderEncoder:dstOps];;

    if (mtlEncoder == nil) {
        J2dRlsTraceLn(J2D_TRACE_ERROR, "MTLRenderer_FillParallelogram: error creating MTLRenderCommandEncoder.");
        return;
    }

    [mtlEncoder setVertexBytes:verts length:sizeof(verts) atIndex:MeshVertexBuffer];
    [mtlEncoder drawPrimitives:MTLPrimitiveTypeTriangleStrip vertexStart:0 vertexCount: QUAD_VERTEX_COUNT];
}

void
MTLRenderer_DrawParallelogram(MTLContext *mtlc, BMTLSDOps * dstOps,
                              jfloat fx11, jfloat fy11,
                              jfloat dx21, jfloat dy21,
                              jfloat dx12, jfloat dy12,
                              jfloat lwr21, jfloat lwr12)
{
    // dx,dy for line width in the "21" and "12" directions.
    jfloat ldx21 = dx21 * lwr21;
    jfloat ldy21 = dy21 * lwr21;
    jfloat ldx12 = dx12 * lwr12;
    jfloat ldy12 = dy12 * lwr12;

    // calculate origin of the outer parallelogram
    jfloat ox11 = fx11 - (ldx21 + ldx12) / 2.0f;
    jfloat oy11 = fy11 - (ldy21 + ldy12) / 2.0f;

    J2dTraceLn8(J2D_TRACE_INFO,
                "MTLRenderer_DrawParallelogram"
                "(x=%6.2f y=%6.2f "
                "dx1=%6.2f dy1=%6.2f lwr1=%6.2f "
                "dx2=%6.2f dy2=%6.2f lwr2=%6.2f)",
                fx11, fy11,
                dx21, dy21, lwr21,
                dx12, dy12, lwr12);


    // Only need to generate 4 quads if the interior still
    // has a hole in it (i.e. if the line width ratio was
    // less than 1.0)
    if (lwr21 < 1.0f && lwr12 < 1.0f) {

        // Note: "TOP", "BOTTOM", "LEFT" and "RIGHT" here are
        // relative to whether the dxNN variables are positive
        // and negative.  The math works fine regardless of
        // their signs, but for conceptual simplicity the
        // comments will refer to the sides as if the dxNN
        // were all positive.  "TOP" and "BOTTOM" segments
        // are defined by the dxy21 deltas.  "LEFT" and "RIGHT"
        // segments are defined by the dxy12 deltas.

        // Each segment includes its starting corner and comes
        // to just short of the following corner.  Thus, each
        // corner is included just once and the only lengths
        // needed are the original parallelogram delta lengths
        // and the "line width deltas".  The sides will cover
        // the following relative territories:
        //
        //     T T T T T R
        //      L         R
        //       L         R
        //        L         R
        //         L         R
        //          L B B B B B

        // Every segment is drawn as a filled Parallelogram quad
        // Each quad is encoded using two triangles
        // For 4 segments - there are 8 triangles in total
        // Each triangle has 3 vertices
        const int TOTAL_VERTICES = 8 * 3;
        struct Vertex vertexList[TOTAL_VERTICES];
        int i = 0;

        // TOP segment, to left side of RIGHT edge
        // "width" of original pgram, "height" of hor. line size
        fx11 = ox11;
        fy11 = oy11;

        fillVertex(vertexList + (i++), fx11, fy11);
        fillVertex(vertexList + (i++), fx11 + dx21, fy11 + dy21);
        fillVertex(vertexList + (i++), fx11 + dx21 + ldx12, fy11 + dy21 + ldy12);

        fillVertex(vertexList + (i++), fx11 + dx21 + ldx12, fy11 + dy21 + ldy12);
        fillVertex(vertexList + (i++), fx11 + ldx12, fy11 + ldy12);
        fillVertex(vertexList + (i++), fx11, fy11);

        // RIGHT segment, to top of BOTTOM edge
        // "width" of vert. line size , "height" of original pgram
        fx11 = ox11 + dx21;
        fy11 = oy11 + dy21;
        fillVertex(vertexList + (i++), fx11, fy11);
        fillVertex(vertexList + (i++), fx11 + ldx21, fy11 + ldy21);
        fillVertex(vertexList + (i++), fx11 + ldx21 + dx12, fy11 + ldy21 + dy12);

        fillVertex(vertexList + (i++), fx11 + ldx21 + dx12, fy11 + ldy21 + dy12);
        fillVertex(vertexList + (i++), fx11 + dx12, fy11 + dy12);
        fillVertex(vertexList + (i++), fx11, fy11);

        // BOTTOM segment, from right side of LEFT edge
        // "width" of original pgram, "height" of hor. line size
        fx11 = ox11 + dx12 + ldx21;
        fy11 = oy11 + dy12 + ldy21;
        fillVertex(vertexList + (i++), fx11, fy11);
        fillVertex(vertexList + (i++), fx11 + dx21, fy11 + dy21);
        fillVertex(vertexList + (i++), fx11 + dx21 + ldx12, fy11 + dy21 + ldy12);

        fillVertex(vertexList + (i++), fx11 + dx21 + ldx12, fy11 + dy21 + ldy12);
        fillVertex(vertexList + (i++), fx11 + ldx12, fy11 + ldy12);
        fillVertex(vertexList + (i++), fx11, fy11);

        // LEFT segment, from bottom of TOP edge
        // "width" of vert. line size , "height" of inner pgram
        fx11 = ox11 + ldx12;
        fy11 = oy11 + ldy12;
        fillVertex(vertexList + (i++), fx11, fy11);
        fillVertex(vertexList + (i++), fx11 + ldx21, fy11 + ldy21);
        fillVertex(vertexList + (i++), fx11 + ldx21 + dx12, fy11 + ldy21 + dy12);

        fillVertex(vertexList + (i++), fx11 + ldx21 + dx12, fy11 + ldy21 + dy12);
        fillVertex(vertexList + (i++), fx11 + dx12, fy11 + dy12);
        fillVertex(vertexList + (i++), fx11, fy11);

        // Encode render command.
        id<MTLRenderCommandEncoder> mtlEncoder = [mtlc.encoderManager getRenderEncoder:dstOps];

        if (mtlEncoder == nil) {
            J2dRlsTraceLn(J2D_TRACE_ERROR, "MTLRenderer_DrawParallelogram: error creating MTLRenderCommandEncoder.");
            return;
        }

        [mtlEncoder setVertexBytes:vertexList length:sizeof(vertexList) atIndex:MeshVertexBuffer];
        [mtlEncoder drawPrimitives:MTLPrimitiveTypeTriangle vertexStart:0 vertexCount:TOTAL_VERTICES];
    } else {
        // The line width ratios were large enough to consume
        // the entire hole in the middle of the parallelogram
        // so we can just issue one large quad for the outer
        // parallelogram.
        dx21 += ldx21;
        dy21 += ldy21;
        dx12 += ldx12;
        dy12 += ldy12;
        MTLRenderer_FillParallelogram(mtlc, dstOps, ox11, oy11, dx21, dy21, dx12, dy12);
    }
}

static struct AAVertex aaVertices[6];
static jint vertexCacheIndex = 0;

#define AA_ADD_VERTEX(OU, OV, IU, IV, DX, DY) \
    do { \
        struct AAVertex *v = &aaVertices[vertexCacheIndex++]; \
        v->otxtpos[0] = OU; \
        v->otxtpos[1] = OV; \
        v->itxtpos[0] = IU; \
        v->itxtpos[1] = IV; \
        v->position[0]= DX; \
        v->position[1] = DY; \
    } while (0)

#define AA_ADD_TRIANGLES(ou11, ov11, iu11, iv11, ou21, ov21, iu21, iv21, ou22, ov22, iu22, iv22, ou12, ov12, iu12, iv12, DX1, DY1, DX2, DY2) \
    do { \
        AA_ADD_VERTEX(ou11, ov11, iu11, iv11, DX1, DY1); \
        AA_ADD_VERTEX(ou21, ov21, iu21, iv21, DX2, DY1); \
        AA_ADD_VERTEX(ou22, ov22, iu22, iv22, DX2, DY2); \
        AA_ADD_VERTEX(ou22, ov22, iu22, iv22, DX2, DY2); \
        AA_ADD_VERTEX(ou12, ov12, iu12, iv12, DX1, DY2); \
        AA_ADD_VERTEX(ou11, ov11, iu11, iv11, DX1, DY1); \
    } while (0)

#define ADJUST_PGRAM(V1, DV, V2) \
    do { \
        if ((DV) >= 0) { \
            (V2) += (DV); \
        } else { \
            (V1) += (DV); \
        } \
    } while (0)

// Invert the following transform:
// DeltaT(0, 0) == (0,       0)
// DeltaT(1, 0) == (DX1,     DY1)
// DeltaT(0, 1) == (DX2,     DY2)
// DeltaT(1, 1) == (DX1+DX2, DY1+DY2)
// TM00 = DX1,   TM01 = DX2,   (TM02 = X11)
// TM10 = DY1,   TM11 = DY2,   (TM12 = Y11)
// Determinant = TM00*TM11 - TM01*TM10
//             =  DX1*DY2  -  DX2*DY1
// Inverse is:
// IM00 =  TM11/det,   IM01 = -TM01/det
// IM10 = -TM10/det,   IM11 =  TM00/det
// IM02 = (TM01 * TM12 - TM11 * TM02) / det,
// IM12 = (TM10 * TM02 - TM00 * TM12) / det,

#define DECLARE_MATRIX(MAT) \
    jfloat MAT ## 00, MAT ## 01, MAT ## 02, MAT ## 10, MAT ## 11, MAT ## 12

#define GET_INVERTED_MATRIX(MAT, X11, Y11, DX1, DY1, DX2, DY2, RET_CODE) \
    do { \
        jfloat det = DX1*DY2 - DX2*DY1; \
        if (det == 0) { \
            RET_CODE; \
        } \
        MAT ## 00 = DY2/det; \
        MAT ## 01 = -DX2/det; \
        MAT ## 10 = -DY1/det; \
        MAT ## 11 = DX1/det; \
        MAT ## 02 = (DX2 * Y11 - DY2 * X11) / det; \
        MAT ## 12 = (DY1 * X11 - DX1 * Y11) / det; \
    } while (0)

#define TRANSFORM(MAT, TX, TY, X, Y) \
    do { \
        TX = (X) * MAT ## 00 + (Y) * MAT ## 01 + MAT ## 02; \
        TY = (X) * MAT ## 10 + (Y) * MAT ## 11 + MAT ## 12; \
    } while (0)

void
MTLRenderer_FillAAParallelogram(MTLContext *mtlc, BMTLSDOps * dstOps,
                              jfloat fx11, jfloat fy11,
                              jfloat dx21, jfloat dy21,
                              jfloat dx12, jfloat dy12)
{
    DECLARE_MATRIX(om);
    // parameters for parallelogram bounding box
    jfloat bx11, by11, bx22, by22;
    // parameters for uv texture coordinates of parallelogram corners
    jfloat ou11, ov11, ou12, ov12, ou21, ov21, ou22, ov22;

    J2dTraceLn6(J2D_TRACE_INFO,
                "MTLRenderer_FillAAParallelogram "
                "(x=%6.2f y=%6.2f "
                "dx1=%6.2f dy1=%6.2f "
                "dx2=%6.2f dy2=%6.2f)",
                fx11, fy11,
                dx21, dy21,
                dx12, dy12);

    RETURN_IF_NULL(mtlc);
    RETURN_IF_NULL(dstOps);

    GET_INVERTED_MATRIX(om, fx11, fy11, dx21, dy21, dx12, dy12,
                        return);

    bx11 = bx22 = fx11;
    by11 = by22 = fy11;
    ADJUST_PGRAM(bx11, dx21, bx22);
    ADJUST_PGRAM(by11, dy21, by22);
    ADJUST_PGRAM(bx11, dx12, bx22);
    ADJUST_PGRAM(by11, dy12, by22);
    bx11 = (jfloat) floor(bx11);
    by11 = (jfloat) floor(by11);
    bx22 = (jfloat) ceil(bx22);
    by22 = (jfloat) ceil(by22);

    TRANSFORM(om, ou11, ov11, bx11, by11);
    TRANSFORM(om, ou21, ov21, bx22, by11);
    TRANSFORM(om, ou12, ov12, bx11, by22);
    TRANSFORM(om, ou22, ov22, bx22, by22);

    id<MTLRenderCommandEncoder> encoder =
        [mtlc.encoderManager getAAShaderRenderEncoder:dstOps];

    AA_ADD_TRIANGLES(ou11, ov11, 5.f, 5.f, ou21, ov21, 6.f, 5.f, ou22, ov22, 6.f, 6.f, ou12, ov12, 5.f, 5.f, bx11, by11, bx22, by22);
    [encoder setVertexBytes:aaVertices length:sizeof(aaVertices) atIndex:MeshVertexBuffer];
    [encoder drawPrimitives:MTLPrimitiveTypeTriangle vertexStart:0 vertexCount:6];
    vertexCacheIndex = 0;
}

void
MTLRenderer_FillAAParallelogramInnerOuter(MTLContext *mtlc, MTLSDOps *dstOps,
                                          jfloat ox11, jfloat oy11,
                                          jfloat ox21, jfloat oy21,
                                          jfloat ox12, jfloat oy12,
                                          jfloat ix11, jfloat iy11,
                                          jfloat ix21, jfloat iy21,
                                          jfloat ix12, jfloat iy12)
{
    DECLARE_MATRIX(om);
    DECLARE_MATRIX(im);
    // parameters for parallelogram bounding box
    jfloat bx11, by11, bx22, by22;
    // parameters for uv texture coordinates of outer parallelogram corners
    jfloat ou11, ov11, ou12, ov12, ou21, ov21, ou22, ov22;
    // parameters for uv texture coordinates of inner parallelogram corners
    jfloat iu11, iv11, iu12, iv12, iu21, iv21, iu22, iv22;

    RETURN_IF_NULL(mtlc);
    RETURN_IF_NULL(dstOps);

    GET_INVERTED_MATRIX(im, ix11, iy11, ix21, iy21, ix12, iy12,
                        // inner parallelogram is degenerate
                        // therefore it encloses no area
                        // fill outer
                        MTLRenderer_FillAAParallelogram(mtlc, dstOps,
                                                        ox11, oy11,
                                                        ox21, oy21,
                                                        ox12, oy12);
                        return);
    GET_INVERTED_MATRIX(om, ox11, oy11, ox21, oy21, ox12, oy12,
                        return);

    bx11 = bx22 = ox11;
    by11 = by22 = oy11;
    ADJUST_PGRAM(bx11, ox21, bx22);
    ADJUST_PGRAM(by11, oy21, by22);
    ADJUST_PGRAM(bx11, ox12, bx22);
    ADJUST_PGRAM(by11, oy12, by22);
    bx11 = (jfloat) floor(bx11);
    by11 = (jfloat) floor(by11);
    bx22 = (jfloat) ceil(bx22);
    by22 = (jfloat) ceil(by22);

    TRANSFORM(om, ou11, ov11, bx11, by11);
    TRANSFORM(om, ou21, ov21, bx22, by11);
    TRANSFORM(om, ou12, ov12, bx11, by22);
    TRANSFORM(om, ou22, ov22, bx22, by22);

    TRANSFORM(im, iu11, iv11, bx11, by11);
    TRANSFORM(im, iu21, iv21, bx22, by11);
    TRANSFORM(im, iu12, iv12, bx11, by22);
    TRANSFORM(im, iu22, iv22, bx22, by22);

    id<MTLRenderCommandEncoder> encoder =
        [mtlc.encoderManager getAAShaderRenderEncoder:dstOps];

    AA_ADD_TRIANGLES(ou11, ov11, iu11, iv11, ou21, ov21, iu21, iv21, ou22, ov22, iu22, iv22, ou12, ov12, iu12, iv12, bx11, by11, bx22, by22);
    [encoder setVertexBytes:aaVertices length:sizeof(aaVertices) atIndex:MeshVertexBuffer];
    [encoder drawPrimitives:MTLPrimitiveTypeTriangle vertexStart:0 vertexCount:6];
    vertexCacheIndex = 0;
}

void
MTLRenderer_DrawAAParallelogram(MTLContext *mtlc, BMTLSDOps * dstOps,
                              jfloat fx11, jfloat fy11,
                              jfloat dx21, jfloat dy21,
                              jfloat dx12, jfloat dy12,
                              jfloat lwr21, jfloat lwr12)
{
    // dx,dy for line width in the "21" and "12" directions.
    jfloat ldx21, ldy21, ldx12, ldy12;
    // parameters for "outer" parallelogram
    jfloat ofx11, ofy11, odx21, ody21, odx12, ody12;
    // parameters for "inner" parallelogram
    jfloat ifx11, ify11, idx21, idy21, idx12, idy12;

    J2dTraceLn8(J2D_TRACE_INFO,
                "MTLRenderer_DrawAAParallelogram "
                "(x=%6.2f y=%6.2f "
                "dx1=%6.2f dy1=%6.2f lwr1=%6.2f "
                "dx2=%6.2f dy2=%6.2f lwr2=%6.2f)",
                fx11, fy11,
                dx21, dy21, lwr21,
                dx12, dy12, lwr12);

    RETURN_IF_NULL(mtlc);
    RETURN_IF_NULL(dstOps);

    // calculate true dx,dy for line widths from the "line width ratios"
    ldx21 = dx21 * lwr21;
    ldy21 = dy21 * lwr21;
    ldx12 = dx12 * lwr12;
    ldy12 = dy12 * lwr12;

    // calculate coordinates of the outer parallelogram
    ofx11 = fx11 - (ldx21 + ldx12) / 2.0f;
    ofy11 = fy11 - (ldy21 + ldy12) / 2.0f;
    odx21 = dx21 + ldx21;
    ody21 = dy21 + ldy21;
    odx12 = dx12 + ldx12;
    ody12 = dy12 + ldy12;

    // Only process the inner parallelogram if the line width ratio
    // did not consume the entire interior of the parallelogram
    // (i.e. if the width ratio was less than 1.0)
    if (lwr21 < 1.0f && lwr12 < 1.0f) {
        // calculate coordinates of the inner parallelogram
        ifx11 = fx11 + (ldx21 + ldx12) / 2.0f;
        ify11 = fy11 + (ldy21 + ldy12) / 2.0f;
        idx21 = dx21 - ldx21;
        idy21 = dy21 - ldy21;
        idx12 = dx12 - ldx12;
        idy12 = dy12 - ldy12;

        MTLRenderer_FillAAParallelogramInnerOuter(mtlc, dstOps,
                                                  ofx11, ofy11,
                                                  odx21, ody21,
                                                  odx12, ody12,
                                                  ifx11, ify11,
                                                  idx21, idy21,
                                                  idx12, idy12);
    } else {
        MTLRenderer_FillAAParallelogram(mtlc, dstOps,
                                        ofx11, ofy11,
                                        odx21, ody21,
                                        odx12, ody12);
    }
}
