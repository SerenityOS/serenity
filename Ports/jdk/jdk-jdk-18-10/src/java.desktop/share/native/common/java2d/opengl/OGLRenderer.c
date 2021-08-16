/*
 * Copyright (c) 2003, 2008, Oracle and/or its affiliates. All rights reserved.
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

#ifndef HEADLESS

#include <jlong.h>
#include <jni_util.h>
#include <math.h>

#include "sun_java2d_opengl_OGLRenderer.h"

#include "OGLRenderer.h"
#include "OGLRenderQueue.h"
#include "OGLSurfaceData.h"

/**
 * Note: Some of the methods in this file apply a "magic number"
 * translation to line segments.  The OpenGL specification lays out the
 * "diamond exit rule" for line rasterization, but it is loose enough to
 * allow for a wide range of line rendering hardware.  (It appears that
 * some hardware, such as the Nvidia GeForce2 series, does not even meet
 * the spec in all cases.)  As such it is difficult to find a mapping
 * between the Java2D and OpenGL line specs that works consistently across
 * all hardware combinations.
 *
 * Therefore the "magic numbers" you see here have been empirically derived
 * after testing on a variety of graphics hardware in order to find some
 * reasonable middle ground between the two specifications.  The general
 * approach is to apply a fractional translation to vertices so that they
 * hit pixel centers and therefore touch the same pixels as in our other
 * pipelines.  Emphasis was placed on finding values so that OGL lines with
 * a slope of +/- 1 hit all the same pixels as our other (software) loops.
 * The stepping in other diagonal lines rendered with OGL may deviate
 * slightly from those rendered with our software loops, but the most
 * important thing is that these magic numbers ensure that all OGL lines
 * hit the same endpoints as our software loops.
 *
 * If you find it necessary to change any of these magic numbers in the
 * future, just be sure that you test the changes across a variety of
 * hardware to ensure consistent rendering everywhere.
 */

void
OGLRenderer_DrawLine(OGLContext *oglc, jint x1, jint y1, jint x2, jint y2)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLRenderer_DrawLine");

    RETURN_IF_NULL(oglc);

    CHECK_PREVIOUS_OP(GL_LINES);

    if (y1 == y2) {
        // horizontal
        GLfloat fx1 = (GLfloat)x1;
        GLfloat fx2 = (GLfloat)x2;
        GLfloat fy  = ((GLfloat)y1) + 0.2f;

        if (x1 > x2) {
            GLfloat t = fx1; fx1 = fx2; fx2 = t;
        }

        j2d_glVertex2f(fx1+0.2f, fy);
        j2d_glVertex2f(fx2+1.2f, fy);
    } else if (x1 == x2) {
        // vertical
        GLfloat fx  = ((GLfloat)x1) + 0.2f;
        GLfloat fy1 = (GLfloat)y1;
        GLfloat fy2 = (GLfloat)y2;

        if (y1 > y2) {
            GLfloat t = fy1; fy1 = fy2; fy2 = t;
        }

        j2d_glVertex2f(fx, fy1+0.2f);
        j2d_glVertex2f(fx, fy2+1.2f);
    } else {
        // diagonal
        GLfloat fx1 = (GLfloat)x1;
        GLfloat fy1 = (GLfloat)y1;
        GLfloat fx2 = (GLfloat)x2;
        GLfloat fy2 = (GLfloat)y2;

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

        j2d_glVertex2f(fx1, fy1);
        j2d_glVertex2f(fx2, fy2);
    }
}

void
OGLRenderer_DrawRect(OGLContext *oglc, jint x, jint y, jint w, jint h)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLRenderer_DrawRect");

    if (w < 0 || h < 0) {
        return;
    }

    RETURN_IF_NULL(oglc);

    if (w < 2 || h < 2) {
        // If one dimension is less than 2 then there is no
        // gap in the middle - draw a solid filled rectangle.
        CHECK_PREVIOUS_OP(GL_QUADS);
        GLRECT_BODY_XYWH(x, y, w+1, h+1);
    } else {
        GLfloat fx1 = ((GLfloat)x) + 0.2f;
        GLfloat fy1 = ((GLfloat)y) + 0.2f;
        GLfloat fx2 = fx1 + ((GLfloat)w);
        GLfloat fy2 = fy1 + ((GLfloat)h);

        // Avoid drawing the endpoints twice.
        // Also prefer including the endpoints in the
        // horizontal sections which draw pixels faster.

        CHECK_PREVIOUS_OP(GL_LINES);
        // top
        j2d_glVertex2f(fx1,      fy1);
        j2d_glVertex2f(fx2+1.0f, fy1);
        // right
        j2d_glVertex2f(fx2,      fy1+1.0f);
        j2d_glVertex2f(fx2,      fy2);
        // bottom
        j2d_glVertex2f(fx1,      fy2);
        j2d_glVertex2f(fx2+1.0f, fy2);
        // left
        j2d_glVertex2f(fx1,      fy1+1.0f);
        j2d_glVertex2f(fx1,      fy2);
    }
}

void
OGLRenderer_DrawPoly(OGLContext *oglc,
                     jint nPoints, jint isClosed,
                     jint transX, jint transY,
                     jint *xPoints, jint *yPoints)
{
    jboolean isEmpty = JNI_TRUE;
    jint mx, my;
    jint i;

    J2dTraceLn(J2D_TRACE_INFO, "OGLRenderer_DrawPoly");

    if (xPoints == NULL || yPoints == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "OGLRenderer_DrawPoly: points array is null");
        return;
    }

    RETURN_IF_NULL(oglc);

    // Note that BufferedRenderPipe.drawPoly() has already rejected polys
    // with nPoints<2, so we can be certain here that we have nPoints>=2.

    mx = xPoints[0];
    my = yPoints[0];

    CHECK_PREVIOUS_OP(GL_LINE_STRIP);
    for (i = 0; i < nPoints; i++) {
        jint x = xPoints[i];
        jint y = yPoints[i];

        isEmpty = isEmpty && (x == mx && y == my);

        // Translate each vertex by a fraction so that we hit pixel centers.
        j2d_glVertex2f((GLfloat)(x + transX) + 0.5f,
                       (GLfloat)(y + transY) + 0.5f);
    }

    if (isClosed && !isEmpty &&
        (xPoints[nPoints-1] != mx ||
         yPoints[nPoints-1] != my))
    {
        // In this case, the polyline's start and end positions are
        // different and need to be closed manually; we do this by adding
        // one more segment back to the starting position.  Note that we
        // do not need to fill in the last pixel (as we do in the following
        // block) because we are returning to the starting pixel, which
        // has already been filled in.
        j2d_glVertex2f((GLfloat)(mx + transX) + 0.5f,
                       (GLfloat)(my + transY) + 0.5f);
        RESET_PREVIOUS_OP(); // so that we don't leave the line strip open
    } else if (!isClosed || isEmpty) {
        // OpenGL omits the last pixel in a polyline, so we fix this by
        // adding a one-pixel segment at the end.  Also, if the polyline
        // never went anywhere (isEmpty is true), we need to use this
        // workaround to ensure that a single pixel is touched.
        CHECK_PREVIOUS_OP(GL_LINES); // this closes the line strip first
        mx = xPoints[nPoints-1] + transX;
        my = yPoints[nPoints-1] + transY;
        j2d_glVertex2i(mx, my);
        j2d_glVertex2i(mx+1, my+1);
        // no need for RESET_PREVIOUS_OP, as the line strip is no longer open
    } else {
        RESET_PREVIOUS_OP(); // so that we don't leave the line strip open
    }
}

JNIEXPORT void JNICALL
Java_sun_java2d_opengl_OGLRenderer_drawPoly
    (JNIEnv *env, jobject oglr,
     jintArray xpointsArray, jintArray ypointsArray,
     jint nPoints, jboolean isClosed,
     jint transX, jint transY)
{
    jint *xPoints, *yPoints;

    J2dTraceLn(J2D_TRACE_INFO, "OGLRenderer_drawPoly");

    xPoints = (jint *)
        (*env)->GetPrimitiveArrayCritical(env, xpointsArray, NULL);
    if (xPoints != NULL) {
        yPoints = (jint *)
            (*env)->GetPrimitiveArrayCritical(env, ypointsArray, NULL);
        if (yPoints != NULL) {
            OGLContext *oglc = OGLRenderQueue_GetCurrentContext();

            OGLRenderer_DrawPoly(oglc,
                                 nPoints, isClosed,
                                 transX, transY,
                                 xPoints, yPoints);

            // 6358147: reset current state, and ensure rendering is
            // flushed to dest
            if (oglc != NULL) {
                RESET_PREVIOUS_OP();
                j2d_glFlush();
            }

            (*env)->ReleasePrimitiveArrayCritical(env, ypointsArray, yPoints,
                                                  JNI_ABORT);
        }
        (*env)->ReleasePrimitiveArrayCritical(env, xpointsArray, xPoints,
                                              JNI_ABORT);
    }
}

void
OGLRenderer_DrawScanlines(OGLContext *oglc,
                          jint scanlineCount, jint *scanlines)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLRenderer_DrawScanlines");

    RETURN_IF_NULL(oglc);
    RETURN_IF_NULL(scanlines);

    CHECK_PREVIOUS_OP(GL_LINES);
    while (scanlineCount > 0) {
        // Translate each vertex by a fraction so
        // that we hit pixel centers.
        GLfloat x1 = ((GLfloat)*(scanlines++)) + 0.2f;
        GLfloat x2 = ((GLfloat)*(scanlines++)) + 1.2f;
        GLfloat y  = ((GLfloat)*(scanlines++)) + 0.5f;
        j2d_glVertex2f(x1, y);
        j2d_glVertex2f(x2, y);
        scanlineCount--;
    }
}

void
OGLRenderer_FillRect(OGLContext *oglc, jint x, jint y, jint w, jint h)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLRenderer_FillRect");

    if (w <= 0 || h <= 0) {
        return;
    }

    RETURN_IF_NULL(oglc);

    CHECK_PREVIOUS_OP(GL_QUADS);
    GLRECT_BODY_XYWH(x, y, w, h);
}

void
OGLRenderer_FillSpans(OGLContext *oglc, jint spanCount, jint *spans)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLRenderer_FillSpans");

    RETURN_IF_NULL(oglc);
    RETURN_IF_NULL(spans);

    CHECK_PREVIOUS_OP(GL_QUADS);
    while (spanCount > 0) {
        jint x1 = *(spans++);
        jint y1 = *(spans++);
        jint x2 = *(spans++);
        jint y2 = *(spans++);
        GLRECT_BODY_XYXY(x1, y1, x2, y2);
        spanCount--;
    }
}

#define FILL_PGRAM(fx11, fy11, dx21, dy21, dx12, dy12) \
    do { \
        j2d_glVertex2f(fx11,               fy11); \
        j2d_glVertex2f(fx11 + dx21,        fy11 + dy21); \
        j2d_glVertex2f(fx11 + dx21 + dx12, fy11 + dy21 + dy12); \
        j2d_glVertex2f(fx11 + dx12,        fy11 + dy12); \
    } while (0)

void
OGLRenderer_FillParallelogram(OGLContext *oglc,
                              jfloat fx11, jfloat fy11,
                              jfloat dx21, jfloat dy21,
                              jfloat dx12, jfloat dy12)
{
    J2dTraceLn6(J2D_TRACE_INFO,
                "OGLRenderer_FillParallelogram "
                "(x=%6.2f y=%6.2f "
                "dx1=%6.2f dy1=%6.2f "
                "dx2=%6.2f dy2=%6.2f)",
                fx11, fy11,
                dx21, dy21,
                dx12, dy12);

    RETURN_IF_NULL(oglc);

    CHECK_PREVIOUS_OP(GL_QUADS);

    FILL_PGRAM(fx11, fy11, dx21, dy21, dx12, dy12);
}

void
OGLRenderer_DrawParallelogram(OGLContext *oglc,
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
                "OGLRenderer_DrawParallelogram "
                "(x=%6.2f y=%6.2f "
                "dx1=%6.2f dy1=%6.2f lwr1=%6.2f "
                "dx2=%6.2f dy2=%6.2f lwr2=%6.2f)",
                fx11, fy11,
                dx21, dy21, lwr21,
                dx12, dy12, lwr12);

    RETURN_IF_NULL(oglc);

    CHECK_PREVIOUS_OP(GL_QUADS);

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

        // TOP segment, to left side of RIGHT edge
        // "width" of original pgram, "height" of hor. line size
        fx11 = ox11;
        fy11 = oy11;
        FILL_PGRAM(fx11, fy11, dx21, dy21, ldx12, ldy12);

        // RIGHT segment, to top of BOTTOM edge
        // "width" of vert. line size , "height" of original pgram
        fx11 = ox11 + dx21;
        fy11 = oy11 + dy21;
        FILL_PGRAM(fx11, fy11, ldx21, ldy21, dx12, dy12);

        // BOTTOM segment, from right side of LEFT edge
        // "width" of original pgram, "height" of hor. line size
        fx11 = ox11 + dx12 + ldx21;
        fy11 = oy11 + dy12 + ldy21;
        FILL_PGRAM(fx11, fy11, dx21, dy21, ldx12, ldy12);

        // LEFT segment, from bottom of TOP edge
        // "width" of vert. line size , "height" of inner pgram
        fx11 = ox11 + ldx12;
        fy11 = oy11 + ldy12;
        FILL_PGRAM(fx11, fy11, ldx21, ldy21, dx12, dy12);
    } else {
        // The line width ratios were large enough to consume
        // the entire hole in the middle of the parallelogram
        // so we can just issue one large quad for the outer
        // parallelogram.
        dx21 += ldx21;
        dy21 += ldy21;
        dx12 += ldx12;
        dy12 += ldy12;
        FILL_PGRAM(ox11, oy11, dx21, dy21, dx12, dy12);
    }
}

static GLhandleARB aaPgramProgram = 0;

/*
 * This shader fills the space between an outer and inner parallelogram.
 * It can be used to draw an outline by specifying both inner and outer
 * values.  It fills pixels by estimating what portion falls inside the
 * outer shape, and subtracting an estimate of what portion falls inside
 * the inner shape.  Specifying both inner and outer values produces a
 * standard "wide outline".  Specifying an inner shape that falls far
 * outside the outer shape allows the same shader to fill the outer
 * shape entirely since pixels that fall within the outer shape are never
 * inside the inner shape and so they are filled based solely on their
 * coverage of the outer shape.
 *
 * The setup code renders this shader over the bounds of the outer
 * shape (or the only shape in the case of a fill operation) and
 * sets the texture 0 coordinates so that 0,0=>0,1=>1,1=>1,0 in those
 * texture coordinates map to the four corners of the parallelogram.
 * Similarly the texture 1 coordinates map the inner shape to the
 * unit square as well, but in a different coordinate system.
 *
 * When viewed in the texture coordinate systems the parallelograms
 * we are filling are unit squares, but the pixels have then become
 * tiny parallelograms themselves.  Both of the texture coordinate
 * systems are affine transforms so the rate of change in X and Y
 * of the texture coordinates are essentially constants and happen
 * to correspond to the size and direction of the slanted sides of
 * the distorted pixels relative to the "square mapped" boundary
 * of the parallelograms.
 *
 * The shader uses the dFdx() and dFdy() functions to measure the "rate
 * of change" of these texture coordinates and thus gets an accurate
 * measure of the size and shape of a pixel relative to the two
 * parallelograms.  It then uses the bounds of the size and shape
 * of a pixel to intersect with the unit square to estimate the
 * coverage of the pixel.  Unfortunately, without a lot more work
 * to calculate the exact area of intersection between a unit
 * square (the original parallelogram) and a parallelogram (the
 * distorted pixel), this shader only approximates the pixel
 * coverage, but emperically the estimate is very useful and
 * produces visually pleasing results, if not theoretically accurate.
 */
static const char *aaPgramShaderSource =
    "void main() {"
    // Calculate the vectors for the "legs" of the pixel parallelogram
    // for the outer parallelogram.
    "    vec2 oleg1 = dFdx(gl_TexCoord[0].st);"
    "    vec2 oleg2 = dFdy(gl_TexCoord[0].st);"
    // Calculate the bounds of the distorted pixel parallelogram.
    "    vec2 corner = gl_TexCoord[0].st - (oleg1+oleg2)/2.0;"
    "    vec2 omin = min(corner, corner+oleg1);"
    "    omin = min(omin, corner+oleg2);"
    "    omin = min(omin, corner+oleg1+oleg2);"
    "    vec2 omax = max(corner, corner+oleg1);"
    "    omax = max(omax, corner+oleg2);"
    "    omax = max(omax, corner+oleg1+oleg2);"
    // Calculate the vectors for the "legs" of the pixel parallelogram
    // for the inner parallelogram.
    "    vec2 ileg1 = dFdx(gl_TexCoord[1].st);"
    "    vec2 ileg2 = dFdy(gl_TexCoord[1].st);"
    // Calculate the bounds of the distorted pixel parallelogram.
    "    corner = gl_TexCoord[1].st - (ileg1+ileg2)/2.0;"
    "    vec2 imin = min(corner, corner+ileg1);"
    "    imin = min(imin, corner+ileg2);"
    "    imin = min(imin, corner+ileg1+ileg2);"
    "    vec2 imax = max(corner, corner+ileg1);"
    "    imax = max(imax, corner+ileg2);"
    "    imax = max(imax, corner+ileg1+ileg2);"
    // Clamp the bounds of the parallelograms to the unit square to
    // estimate the intersection of the pixel parallelogram with
    // the unit square.  The ratio of the 2 rectangle areas is a
    // reasonable estimate of the proportion of coverage.
    "    vec2 o1 = clamp(omin, 0.0, 1.0);"
    "    vec2 o2 = clamp(omax, 0.0, 1.0);"
    "    float oint = (o2.y-o1.y)*(o2.x-o1.x);"
    "    float oarea = (omax.y-omin.y)*(omax.x-omin.x);"
    "    vec2 i1 = clamp(imin, 0.0, 1.0);"
    "    vec2 i2 = clamp(imax, 0.0, 1.0);"
    "    float iint = (i2.y-i1.y)*(i2.x-i1.x);"
    "    float iarea = (imax.y-imin.y)*(imax.x-imin.x);"
    // Proportion of pixel in outer shape minus the proportion
    // of pixel in the inner shape == the coverage of the pixel
    // in the area between the two.
    "    float coverage = oint/oarea - iint / iarea;"
    "    gl_FragColor = gl_Color * coverage;"
    "}";

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
OGLRenderer_FillAAParallelogram(OGLContext *oglc, OGLSDOps *dstOps,
                                jfloat fx11, jfloat fy11,
                                jfloat dx21, jfloat dy21,
                                jfloat dx12, jfloat dy12)
{
    DECLARE_MATRIX(om);
    // parameters for parallelogram bounding box
    jfloat bx11, by11, bx22, by22;
    // parameters for uv texture coordinates of parallelogram corners
    jfloat u11, v11, u12, v12, u21, v21, u22, v22;

    J2dTraceLn6(J2D_TRACE_INFO,
                "OGLRenderer_FillAAParallelogram "
                "(x=%6.2f y=%6.2f "
                "dx1=%6.2f dy1=%6.2f "
                "dx2=%6.2f dy2=%6.2f)",
                fx11, fy11,
                dx21, dy21,
                dx12, dy12);

    RETURN_IF_NULL(oglc);
    RETURN_IF_NULL(dstOps);

    GET_INVERTED_MATRIX(om, fx11, fy11, dx21, dy21, dx12, dy12,
                        return);

    CHECK_PREVIOUS_OP(OGL_STATE_PGRAM_OP);

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

    TRANSFORM(om, u11, v11, bx11, by11);
    TRANSFORM(om, u21, v21, bx22, by11);
    TRANSFORM(om, u12, v12, bx11, by22);
    TRANSFORM(om, u22, v22, bx22, by22);

    j2d_glBegin(GL_QUADS);
    j2d_glMultiTexCoord2fARB(GL_TEXTURE0_ARB, u11, v11);
    j2d_glMultiTexCoord2fARB(GL_TEXTURE1_ARB, 5.f, 5.f);
    j2d_glVertex2f(bx11, by11);
    j2d_glMultiTexCoord2fARB(GL_TEXTURE0_ARB, u21, v21);
    j2d_glMultiTexCoord2fARB(GL_TEXTURE1_ARB, 6.f, 5.f);
    j2d_glVertex2f(bx22, by11);
    j2d_glMultiTexCoord2fARB(GL_TEXTURE0_ARB, u22, v22);
    j2d_glMultiTexCoord2fARB(GL_TEXTURE1_ARB, 6.f, 6.f);
    j2d_glVertex2f(bx22, by22);
    j2d_glMultiTexCoord2fARB(GL_TEXTURE0_ARB, u12, v12);
    j2d_glMultiTexCoord2fARB(GL_TEXTURE1_ARB, 5.f, 6.f);
    j2d_glVertex2f(bx11, by22);
    j2d_glEnd();
}

void
OGLRenderer_FillAAParallelogramInnerOuter(OGLContext *oglc, OGLSDOps *dstOps,
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

    RETURN_IF_NULL(oglc);
    RETURN_IF_NULL(dstOps);

    GET_INVERTED_MATRIX(im, ix11, iy11, ix21, iy21, ix12, iy12,
                        // inner parallelogram is degenerate
                        // therefore it encloses no area
                        // fill outer
                        OGLRenderer_FillAAParallelogram(oglc, dstOps,
                                                        ox11, oy11,
                                                        ox21, oy21,
                                                        ox12, oy12);
                        return);
    GET_INVERTED_MATRIX(om, ox11, oy11, ox21, oy21, ox12, oy12,
                        return);

    CHECK_PREVIOUS_OP(OGL_STATE_PGRAM_OP);

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

    j2d_glBegin(GL_QUADS);
    j2d_glMultiTexCoord2fARB(GL_TEXTURE0_ARB, ou11, ov11);
    j2d_glMultiTexCoord2fARB(GL_TEXTURE1_ARB, iu11, iv11);
    j2d_glVertex2f(bx11, by11);
    j2d_glMultiTexCoord2fARB(GL_TEXTURE0_ARB, ou21, ov21);
    j2d_glMultiTexCoord2fARB(GL_TEXTURE1_ARB, iu21, iv21);
    j2d_glVertex2f(bx22, by11);
    j2d_glMultiTexCoord2fARB(GL_TEXTURE0_ARB, ou22, ov22);
    j2d_glMultiTexCoord2fARB(GL_TEXTURE1_ARB, iu22, iv22);
    j2d_glVertex2f(bx22, by22);
    j2d_glMultiTexCoord2fARB(GL_TEXTURE0_ARB, ou12, ov12);
    j2d_glMultiTexCoord2fARB(GL_TEXTURE1_ARB, iu12, iv12);
    j2d_glVertex2f(bx11, by22);
    j2d_glEnd();
}

void
OGLRenderer_DrawAAParallelogram(OGLContext *oglc, OGLSDOps *dstOps,
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
                "OGLRenderer_DrawAAParallelogram "
                "(x=%6.2f y=%6.2f "
                "dx1=%6.2f dy1=%6.2f lwr1=%6.2f "
                "dx2=%6.2f dy2=%6.2f lwr2=%6.2f)",
                fx11, fy11,
                dx21, dy21, lwr21,
                dx12, dy12, lwr12);

    RETURN_IF_NULL(oglc);
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

        OGLRenderer_FillAAParallelogramInnerOuter(oglc, dstOps,
                                                  ofx11, ofy11,
                                                  odx21, ody21,
                                                  odx12, ody12,
                                                  ifx11, ify11,
                                                  idx21, idy21,
                                                  idx12, idy12);
    } else {
        OGLRenderer_FillAAParallelogram(oglc, dstOps,
                                        ofx11, ofy11,
                                        odx21, ody21,
                                        odx12, ody12);
    }
}

void
OGLRenderer_EnableAAParallelogramProgram()
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLRenderer_EnableAAParallelogramProgram");

    if (aaPgramProgram == 0) {
        aaPgramProgram = OGLContext_CreateFragmentProgram(aaPgramShaderSource);
        if (aaPgramProgram == 0) {
            J2dRlsTraceLn(J2D_TRACE_ERROR,
                          "OGLRenderer_EnableAAParallelogramProgram: "
                          "error creating program");
            return;
        }
    }
    j2d_glUseProgramObjectARB(aaPgramProgram);
}

void
OGLRenderer_DisableAAParallelogramProgram()
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLRenderer_DisableAAParallelogramProgram");

    j2d_glUseProgramObjectARB(0);
}

#endif /* !HEADLESS */
