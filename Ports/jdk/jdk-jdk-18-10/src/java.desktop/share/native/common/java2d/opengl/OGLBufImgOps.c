/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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

#include "OGLBufImgOps.h"
#include "OGLContext.h"
#include "OGLRenderQueue.h"
#include "OGLSurfaceData.h"
#include "GraphicsPrimitiveMgr.h"

/** Evaluates to true if the given bit is set on the local flags variable. */
#define IS_SET(flagbit) \
    (((flags) & (flagbit)) != 0)

/**************************** ConvolveOp support ****************************/

/**
 * The ConvolveOp shader is fairly straightforward.  For each texel in
 * the source texture, the shader samples the MxN texels in the surrounding
 * area, multiplies each by its corresponding kernel value, and then sums
 * them all together to produce a single color result.  Finally, the
 * resulting value is multiplied by the current OpenGL color, which contains
 * the extra alpha value.
 *
 * Note that this shader source code includes some "holes" marked by "%s".
 * This allows us to build different shader programs (e.g. one for
 * 3x3, one for 5x5, and so on) simply by filling in these "holes" with
 * a call to sprintf().  See the OGLBufImgOps_CreateConvolveProgram() method
 * for more details.
 *
 * REMIND: Currently this shader (and the supporting code in the
 *         EnableConvolveOp() method) only supports 3x3 and 5x5 filters.
 *         Early shader-level hardware did not support non-constant sized
 *         arrays but modern hardware should support them (although I
 *         don't know of any simple way to find out, other than to compile
 *         the shader at runtime and see if the drivers complain).
 */
static const char *convolveShaderSource =
    // maximum size supported by this shader
    "const int MAX_KERNEL_SIZE = %s;"
    // image to be convolved
    "uniform sampler%s baseImage;"
    // image edge limits:
    //   imgEdge.xy = imgMin.xy (anything < will be treated as edge case)
    //   imgEdge.zw = imgMax.xy (anything > will be treated as edge case)
    "uniform vec4 imgEdge;"
    // value for each location in the convolution kernel:
    //   kernelVals[i].x = offsetX[i]
    //   kernelVals[i].y = offsetY[i]
    //   kernelVals[i].z = kernel[i]
    "uniform vec3 kernelVals[MAX_KERNEL_SIZE];"
    ""
    "void main(void)"
    "{"
    "    int i;"
    "    vec4 sum;"
    ""
    "    if (any(lessThan(gl_TexCoord[0].st, imgEdge.xy)) ||"
    "        any(greaterThan(gl_TexCoord[0].st, imgEdge.zw)))"
    "    {"
             // (placeholder for edge condition code)
    "        %s"
    "    } else {"
    "        sum = vec4(0.0);"
    "        for (i = 0; i < MAX_KERNEL_SIZE; i++) {"
    "            sum +="
    "                kernelVals[i].z *"
    "                texture%s(baseImage,"
    "                          gl_TexCoord[0].st + kernelVals[i].xy);"
    "        }"
    "    }"
    ""
         // modulate with gl_Color in order to apply extra alpha
    "    gl_FragColor = sum * gl_Color;"
    "}";

/**
 * Flags that can be bitwise-or'ed together to control how the shader
 * source code is generated.
 */
#define CONVOLVE_RECT            (1 << 0)
#define CONVOLVE_EDGE_ZERO_FILL  (1 << 1)
#define CONVOLVE_5X5             (1 << 2)

/**
 * The handles to the ConvolveOp fragment program objects.  The index to
 * the array should be a bitwise-or'ing of the CONVOLVE_* flags defined
 * above.  Note that most applications will likely need to initialize one
 * or two of these elements, so the array is usually sparsely populated.
 */
static GLhandleARB convolvePrograms[8];

/**
 * The maximum kernel size supported by the ConvolveOp shader.
 */
#define MAX_KERNEL_SIZE 25

/**
 * Compiles and links the ConvolveOp shader program.  If successful, this
 * function returns a handle to the newly created shader program; otherwise
 * returns 0.
 */
static GLhandleARB
OGLBufImgOps_CreateConvolveProgram(jint flags)
{
    GLhandleARB convolveProgram;
    GLint loc;
    char *kernelMax = IS_SET(CONVOLVE_5X5) ? "25" : "9";
    char *target = IS_SET(CONVOLVE_RECT) ? "2DRect" : "2D";
    char edge[100];
    char finalSource[2000];

    J2dTraceLn1(J2D_TRACE_INFO,
                "OGLBufImgOps_CreateConvolveProgram: flags=%d",
                flags);

    if (IS_SET(CONVOLVE_EDGE_ZERO_FILL)) {
        // EDGE_ZERO_FILL: fill in zero at the edges
        sprintf(edge, "sum = vec4(0.0);");
    } else {
        // EDGE_NO_OP: use the source pixel color at the edges
        sprintf(edge,
                "sum = texture%s(baseImage, gl_TexCoord[0].st);",
                target);
    }

    // compose the final source code string from the various pieces
    sprintf(finalSource, convolveShaderSource,
            kernelMax, target, edge, target);

    convolveProgram = OGLContext_CreateFragmentProgram(finalSource);
    if (convolveProgram == 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLBufImgOps_CreateConvolveProgram: error creating program");
        return 0;
    }

    // "use" the program object temporarily so that we can set the uniforms
    j2d_glUseProgramObjectARB(convolveProgram);

    // set the "uniform" texture unit binding
    loc = j2d_glGetUniformLocationARB(convolveProgram, "baseImage");
    j2d_glUniform1iARB(loc, 0); // texture unit 0

    // "unuse" the program object; it will be re-bound later as needed
    j2d_glUseProgramObjectARB(0);

    return convolveProgram;
}

void
OGLBufImgOps_EnableConvolveOp(OGLContext *oglc, jlong pSrcOps,
                              jboolean edgeZeroFill,
                              jint kernelWidth, jint kernelHeight,
                              unsigned char *kernel)
{
    OGLSDOps *srcOps = (OGLSDOps *)jlong_to_ptr(pSrcOps);
    jint kernelSize = kernelWidth * kernelHeight;
    GLhandleARB convolveProgram;
    GLfloat xoff, yoff;
    GLfloat edgeX, edgeY, minX, minY, maxX, maxY;
    GLfloat kernelVals[MAX_KERNEL_SIZE*3];
    jint i, j, kIndex;
    GLint loc;
    jint flags = 0;

    J2dTraceLn2(J2D_TRACE_INFO,
                "OGLBufImgOps_EnableConvolveOp: kernelW=%d kernelH=%d",
                kernelWidth, kernelHeight);

    RETURN_IF_NULL(oglc);
    RETURN_IF_NULL(srcOps);
    RESET_PREVIOUS_OP();

    if (srcOps->textureTarget == GL_TEXTURE_RECTANGLE_ARB) {
        flags |= CONVOLVE_RECT;

        // for GL_TEXTURE_RECTANGLE_ARB, texcoords are specified in the
        // range [0,srcw] and [0,srch], so to achieve an x/y offset of
        // exactly one pixel we simply use the value 1 here
        xoff = 1.0f;
        yoff = 1.0f;
    } else {
        // for GL_TEXTURE_2D, texcoords are specified in the range [0,1],
        // so to achieve an x/y offset of approximately one pixel we have
        // to normalize to that range here
        xoff = 1.0f / srcOps->textureWidth;
        yoff = 1.0f / srcOps->textureHeight;
    }
    if (edgeZeroFill) {
        flags |= CONVOLVE_EDGE_ZERO_FILL;
    }
    if (kernelWidth == 5 && kernelHeight == 5) {
        flags |= CONVOLVE_5X5;
    }

    // locate/initialize the shader program for the given flags
    if (convolvePrograms[flags] == 0) {
        convolvePrograms[flags] = OGLBufImgOps_CreateConvolveProgram(flags);
        if (convolvePrograms[flags] == 0) {
            // shouldn't happen, but just in case...
            return;
        }
    }
    convolveProgram = convolvePrograms[flags];

    // enable the convolve shader
    j2d_glUseProgramObjectARB(convolveProgram);

    // update the "uniform" image min/max values
    edgeX = (kernelWidth/2) * xoff;
    edgeY = (kernelHeight/2) * yoff;
    minX = edgeX;
    minY = edgeY;
    if (srcOps->textureTarget == GL_TEXTURE_RECTANGLE_ARB) {
        // texcoords are in the range [0,srcw] and [0,srch]
        maxX = ((GLfloat)srcOps->width)  - edgeX;
        maxY = ((GLfloat)srcOps->height) - edgeY;
    } else {
        // texcoords are in the range [0,1]
        maxX = (((GLfloat)srcOps->width) / srcOps->textureWidth) - edgeX;
        maxY = (((GLfloat)srcOps->height) / srcOps->textureHeight) - edgeY;
    }
    loc = j2d_glGetUniformLocationARB(convolveProgram, "imgEdge");
    j2d_glUniform4fARB(loc, minX, minY, maxX, maxY);

    // update the "uniform" kernel offsets and values
    loc = j2d_glGetUniformLocationARB(convolveProgram, "kernelVals");
    kIndex = 0;
    for (i = -kernelHeight/2; i < kernelHeight/2+1; i++) {
        for (j = -kernelWidth/2; j < kernelWidth/2+1; j++) {
            kernelVals[kIndex+0] = j*xoff;
            kernelVals[kIndex+1] = i*yoff;
            kernelVals[kIndex+2] = NEXT_FLOAT(kernel);
            kIndex += 3;
        }
    }
    j2d_glUniform3fvARB(loc, kernelSize, kernelVals);
}

void
OGLBufImgOps_DisableConvolveOp(OGLContext *oglc)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLBufImgOps_DisableConvolveOp");

    RETURN_IF_NULL(oglc);

    // disable the ConvolveOp shader
    j2d_glUseProgramObjectARB(0);
}

/**************************** RescaleOp support *****************************/

/**
 * The RescaleOp shader is one of the simplest possible.  Each fragment
 * from the source image is multiplied by the user's scale factor and added
 * to the user's offset value (these are component-wise operations).
 * Finally, the resulting value is multiplied by the current OpenGL color,
 * which contains the extra alpha value.
 *
 * The RescaleOp spec says that the operation is performed regardless of
 * whether the source data is premultiplied or non-premultiplied.  This is
 * a problem for the OpenGL pipeline in that a non-premultiplied
 * BufferedImage will have already been converted into premultiplied
 * when uploaded to an OpenGL texture.  Therefore, we have a special mode
 * called RESCALE_NON_PREMULT (used only for source images that were
 * originally non-premultiplied) that un-premultiplies the source color
 * prior to the rescale operation, then re-premultiplies the resulting
 * color before returning from the fragment shader.
 *
 * Note that this shader source code includes some "holes" marked by "%s".
 * This allows us to build different shader programs (e.g. one for
 * GL_TEXTURE_2D targets, one for GL_TEXTURE_RECTANGLE_ARB targets, and so on)
 * simply by filling in these "holes" with a call to sprintf().  See the
 * OGLBufImgOps_CreateRescaleProgram() method for more details.
 */
static const char *rescaleShaderSource =
    // image to be rescaled
    "uniform sampler%s baseImage;"
    // vector containing scale factors
    "uniform vec4 scaleFactors;"
    // vector containing offsets
    "uniform vec4 offsets;"
    ""
    "void main(void)"
    "{"
    "    vec4 srcColor = texture%s(baseImage, gl_TexCoord[0].st);"
         // (placeholder for un-premult code)
    "    %s"
         // rescale source value
    "    vec4 result = (srcColor * scaleFactors) + offsets;"
         // (placeholder for re-premult code)
    "    %s"
         // modulate with gl_Color in order to apply extra alpha
    "    gl_FragColor = result * gl_Color;"
    "}";

/**
 * Flags that can be bitwise-or'ed together to control how the shader
 * source code is generated.
 */
#define RESCALE_RECT        (1 << 0)
#define RESCALE_NON_PREMULT (1 << 1)

/**
 * The handles to the RescaleOp fragment program objects.  The index to
 * the array should be a bitwise-or'ing of the RESCALE_* flags defined
 * above.  Note that most applications will likely need to initialize one
 * or two of these elements, so the array is usually sparsely populated.
 */
static GLhandleARB rescalePrograms[4];

/**
 * Compiles and links the RescaleOp shader program.  If successful, this
 * function returns a handle to the newly created shader program; otherwise
 * returns 0.
 */
static GLhandleARB
OGLBufImgOps_CreateRescaleProgram(jint flags)
{
    GLhandleARB rescaleProgram;
    GLint loc;
    char *target = IS_SET(RESCALE_RECT) ? "2DRect" : "2D";
    char *preRescale = "";
    char *postRescale = "";
    char finalSource[2000];

    J2dTraceLn1(J2D_TRACE_INFO,
                "OGLBufImgOps_CreateRescaleProgram: flags=%d",
                flags);

    if (IS_SET(RESCALE_NON_PREMULT)) {
        preRescale  = "srcColor.rgb /= srcColor.a;";
        postRescale = "result.rgb *= result.a;";
    }

    // compose the final source code string from the various pieces
    sprintf(finalSource, rescaleShaderSource,
            target, target, preRescale, postRescale);

    rescaleProgram = OGLContext_CreateFragmentProgram(finalSource);
    if (rescaleProgram == 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLBufImgOps_CreateRescaleProgram: error creating program");
        return 0;
    }

    // "use" the program object temporarily so that we can set the uniforms
    j2d_glUseProgramObjectARB(rescaleProgram);

    // set the "uniform" values
    loc = j2d_glGetUniformLocationARB(rescaleProgram, "baseImage");
    j2d_glUniform1iARB(loc, 0); // texture unit 0

    // "unuse" the program object; it will be re-bound later as needed
    j2d_glUseProgramObjectARB(0);

    return rescaleProgram;
}

void
OGLBufImgOps_EnableRescaleOp(OGLContext *oglc, jlong pSrcOps,
                             jboolean nonPremult,
                             unsigned char *scaleFactors,
                             unsigned char *offsets)
{
    OGLSDOps *srcOps = (OGLSDOps *)jlong_to_ptr(pSrcOps);
    GLhandleARB rescaleProgram;
    GLint loc;
    jint flags = 0;

    J2dTraceLn(J2D_TRACE_INFO, "OGLBufImgOps_EnableRescaleOp");

    RETURN_IF_NULL(oglc);
    RETURN_IF_NULL(srcOps);
    RESET_PREVIOUS_OP();

    // choose the appropriate shader, depending on the source texture target
    if (srcOps->textureTarget == GL_TEXTURE_RECTANGLE_ARB) {
        flags |= RESCALE_RECT;
    }
    if (nonPremult) {
        flags |= RESCALE_NON_PREMULT;
    }

    // locate/initialize the shader program for the given flags
    if (rescalePrograms[flags] == 0) {
        rescalePrograms[flags] = OGLBufImgOps_CreateRescaleProgram(flags);
        if (rescalePrograms[flags] == 0) {
            // shouldn't happen, but just in case...
            return;
        }
    }
    rescaleProgram = rescalePrograms[flags];

    // enable the rescale shader
    j2d_glUseProgramObjectARB(rescaleProgram);

    // update the "uniform" scale factor values (note that the Java-level
    // dispatching code always passes down 4 values here, regardless of
    // the original source image type)
    loc = j2d_glGetUniformLocationARB(rescaleProgram, "scaleFactors");
    {
        GLfloat sf1 = NEXT_FLOAT(scaleFactors);
        GLfloat sf2 = NEXT_FLOAT(scaleFactors);
        GLfloat sf3 = NEXT_FLOAT(scaleFactors);
        GLfloat sf4 = NEXT_FLOAT(scaleFactors);
        j2d_glUniform4fARB(loc, sf1, sf2, sf3, sf4);
    }

    // update the "uniform" offset values (note that the Java-level
    // dispatching code always passes down 4 values here, and that the
    // offsets will have already been normalized to the range [0,1])
    loc = j2d_glGetUniformLocationARB(rescaleProgram, "offsets");
    {
        GLfloat off1 = NEXT_FLOAT(offsets);
        GLfloat off2 = NEXT_FLOAT(offsets);
        GLfloat off3 = NEXT_FLOAT(offsets);
        GLfloat off4 = NEXT_FLOAT(offsets);
        j2d_glUniform4fARB(loc, off1, off2, off3, off4);
    }
}

void
OGLBufImgOps_DisableRescaleOp(OGLContext *oglc)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLBufImgOps_DisableRescaleOp");

    RETURN_IF_NULL(oglc);

    // disable the RescaleOp shader
    j2d_glUseProgramObjectARB(0);
}

/**************************** LookupOp support ******************************/

/**
 * The LookupOp shader takes a fragment color (from the source texture) as
 * input, subtracts the optional user offset value, and then uses the
 * resulting value to index into the lookup table texture to provide
 * a new color result.  Finally, the resulting value is multiplied by
 * the current OpenGL color, which contains the extra alpha value.
 *
 * The lookup step requires 3 texture accesses (or 4, when alpha is included),
 * which is somewhat unfortunate because it's not ideal from a performance
 * standpoint, but that sort of thing is getting faster with newer hardware.
 * In the 3-band case, we could consider using a three-dimensional texture
 * and performing the lookup with a single texture access step.  We already
 * use this approach in the LCD text shader, and it works well, but for the
 * purposes of this LookupOp shader, it's probably overkill.  Also, there's
 * a difference in that the LCD text shader only needs to populate the 3D LUT
 * once, but here we would need to populate it on every invocation, which
 * would likely be a waste of VRAM and CPU/GPU cycles.
 *
 * The LUT texture is currently hardcoded as 4 rows/bands, each containing
 * 256 elements.  This means that we currently only support user-provided
 * tables with no more than 256 elements in each band (this is checked at
 * at the Java level).  If the user provides a table with less than 256
 * elements per band, our shader will still work fine, but if elements are
 * accessed with an index >= the size of the LUT, then the shader will simply
 * produce undefined values.  Typically the user would provide an offset
 * value that would prevent this from happening, but it's worth pointing out
 * this fact because the software LookupOp implementation would usually
 * throw an ArrayIndexOutOfBoundsException in this scenario (although it is
 * not something demanded by the spec).
 *
 * The LookupOp spec says that the operation is performed regardless of
 * whether the source data is premultiplied or non-premultiplied.  This is
 * a problem for the OpenGL pipeline in that a non-premultiplied
 * BufferedImage will have already been converted into premultiplied
 * when uploaded to an OpenGL texture.  Therefore, we have a special mode
 * called LOOKUP_NON_PREMULT (used only for source images that were
 * originally non-premultiplied) that un-premultiplies the source color
 * prior to the lookup operation, then re-premultiplies the resulting
 * color before returning from the fragment shader.
 *
 * Note that this shader source code includes some "holes" marked by "%s".
 * This allows us to build different shader programs (e.g. one for
 * GL_TEXTURE_2D targets, one for GL_TEXTURE_RECTANGLE_ARB targets, and so on)
 * simply by filling in these "holes" with a call to sprintf().  See the
 * OGLBufImgOps_CreateLookupProgram() method for more details.
 */
static const char *lookupShaderSource =
    // source image (bound to texture unit 0)
    "uniform sampler%s baseImage;"
    // lookup table (bound to texture unit 1)
    "uniform sampler2D lookupTable;"
    // offset subtracted from source index prior to lookup step
    "uniform vec4 offset;"
    ""
    "void main(void)"
    "{"
    "    vec4 srcColor = texture%s(baseImage, gl_TexCoord[0].st);"
         // (placeholder for un-premult code)
    "    %s"
         // subtract offset from original index
    "    vec4 srcIndex = srcColor - offset;"
         // use source value as input to lookup table (note that
         // "v" texcoords are hardcoded to hit texel centers of
         // each row/band in texture)
    "    vec4 result;"
    "    result.r = texture2D(lookupTable, vec2(srcIndex.r, 0.125)).r;"
    "    result.g = texture2D(lookupTable, vec2(srcIndex.g, 0.375)).r;"
    "    result.b = texture2D(lookupTable, vec2(srcIndex.b, 0.625)).r;"
         // (placeholder for alpha store code)
    "    %s"
         // (placeholder for re-premult code)
    "    %s"
         // modulate with gl_Color in order to apply extra alpha
    "    gl_FragColor = result * gl_Color;"
    "}";

/**
 * Flags that can be bitwise-or'ed together to control how the shader
 * source code is generated.
 */
#define LOOKUP_RECT          (1 << 0)
#define LOOKUP_USE_SRC_ALPHA (1 << 1)
#define LOOKUP_NON_PREMULT   (1 << 2)

/**
 * The handles to the LookupOp fragment program objects.  The index to
 * the array should be a bitwise-or'ing of the LOOKUP_* flags defined
 * above.  Note that most applications will likely need to initialize one
 * or two of these elements, so the array is usually sparsely populated.
 */
static GLhandleARB lookupPrograms[8];

/**
 * The handle to the lookup table texture object used by the shader.
 */
static GLuint lutTextureID = 0;

/**
 * Compiles and links the LookupOp shader program.  If successful, this
 * function returns a handle to the newly created shader program; otherwise
 * returns 0.
 */
static GLhandleARB
OGLBufImgOps_CreateLookupProgram(jint flags)
{
    GLhandleARB lookupProgram;
    GLint loc;
    char *target = IS_SET(LOOKUP_RECT) ? "2DRect" : "2D";
    char *alpha;
    char *preLookup = "";
    char *postLookup = "";
    char finalSource[2000];

    J2dTraceLn1(J2D_TRACE_INFO,
                "OGLBufImgOps_CreateLookupProgram: flags=%d",
                flags);

    if (IS_SET(LOOKUP_USE_SRC_ALPHA)) {
        // when numComps is 1 or 3, the alpha is not looked up in the table;
        // just keep the alpha from the source fragment
        alpha = "result.a = srcColor.a;";
    } else {
        // when numComps is 4, the alpha is looked up in the table, just
        // like the other color components from the source fragment
        alpha =
            "result.a = texture2D(lookupTable, vec2(srcIndex.a, 0.875)).r;";
    }
    if (IS_SET(LOOKUP_NON_PREMULT)) {
        preLookup  = "srcColor.rgb /= srcColor.a;";
        postLookup = "result.rgb *= result.a;";
    }

    // compose the final source code string from the various pieces
    sprintf(finalSource, lookupShaderSource,
            target, target, preLookup, alpha, postLookup);

    lookupProgram = OGLContext_CreateFragmentProgram(finalSource);
    if (lookupProgram == 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLBufImgOps_CreateLookupProgram: error creating program");
        return 0;
    }

    // "use" the program object temporarily so that we can set the uniforms
    j2d_glUseProgramObjectARB(lookupProgram);

    // set the "uniform" values
    loc = j2d_glGetUniformLocationARB(lookupProgram, "baseImage");
    j2d_glUniform1iARB(loc, 0); // texture unit 0
    loc = j2d_glGetUniformLocationARB(lookupProgram, "lookupTable");
    j2d_glUniform1iARB(loc, 1); // texture unit 1

    // "unuse" the program object; it will be re-bound later as needed
    j2d_glUseProgramObjectARB(0);

    return lookupProgram;
}

void
OGLBufImgOps_EnableLookupOp(OGLContext *oglc, jlong pSrcOps,
                            jboolean nonPremult, jboolean shortData,
                            jint numBands, jint bandLength, jint offset,
                            void *tableValues)
{
    OGLSDOps *srcOps = (OGLSDOps *)jlong_to_ptr(pSrcOps);
    int bytesPerElem = (shortData ? 2 : 1);
    GLhandleARB lookupProgram;
    GLfloat foff;
    GLint loc;
    void *bands[4];
    int i;
    jint flags = 0;

    J2dTraceLn4(J2D_TRACE_INFO,
                "OGLBufImgOps_EnableLookupOp: short=%d num=%d len=%d off=%d",
                shortData, numBands, bandLength, offset);

    for (i = 0; i < 4; i++) {
        bands[i] = NULL;
    }
    RETURN_IF_NULL(oglc);
    RETURN_IF_NULL(srcOps);
    RESET_PREVIOUS_OP();

    // choose the appropriate shader, depending on the source texture target
    // and the number of bands involved
    if (srcOps->textureTarget == GL_TEXTURE_RECTANGLE_ARB) {
        flags |= LOOKUP_RECT;
    }
    if (numBands != 4) {
        flags |= LOOKUP_USE_SRC_ALPHA;
    }
    if (nonPremult) {
        flags |= LOOKUP_NON_PREMULT;
    }

    // locate/initialize the shader program for the given flags
    if (lookupPrograms[flags] == 0) {
        lookupPrograms[flags] = OGLBufImgOps_CreateLookupProgram(flags);
        if (lookupPrograms[flags] == 0) {
            // shouldn't happen, but just in case...
            return;
        }
    }
    lookupProgram = lookupPrograms[flags];

    // enable the lookup shader
    j2d_glUseProgramObjectARB(lookupProgram);

    // update the "uniform" offset value
    loc = j2d_glGetUniformLocationARB(lookupProgram, "offset");
    foff = offset / 255.0f;
    j2d_glUniform4fARB(loc, foff, foff, foff, foff);

    // bind the lookup table to texture unit 1 and enable texturing
    j2d_glActiveTextureARB(GL_TEXTURE1_ARB);
    if (lutTextureID == 0) {
        /*
         * Create the lookup table texture with 4 rows (one band per row)
         * and 256 columns (one LUT band element per column) and with an
         * internal format of 16-bit luminance values, which will be
         * sufficient for either byte or short LUT data.  Note that the
         * texture wrap mode will be set to the default of GL_CLAMP_TO_EDGE,
         * which means that out-of-range index value will be clamped
         * appropriately.
         */
        lutTextureID =
            OGLContext_CreateBlitTexture(GL_LUMINANCE16, GL_LUMINANCE,
                                         256, 4);
        if (lutTextureID == 0) {
            // should never happen, but just to be safe...
            return;
        }
    }
    j2d_glBindTexture(GL_TEXTURE_2D, lutTextureID);
    j2d_glEnable(GL_TEXTURE_2D);

    // update the lookup table with the user-provided values
    if (numBands == 1) {
        // replicate the single band for R/G/B; alpha band is unused
        for (i = 0; i < 3; i++) {
            bands[i] = tableValues;
        }
        bands[3] = NULL;
    } else if (numBands == 3) {
        // user supplied band for each of R/G/B; alpha band is unused
        for (i = 0; i < 3; i++) {
            bands[i] = PtrAddBytes(tableValues, i*bandLength*bytesPerElem);
        }
        bands[3] = NULL;
    } else if (numBands == 4) {
        // user supplied band for each of R/G/B/A
        for (i = 0; i < 4; i++) {
            bands[i] = PtrAddBytes(tableValues, i*bandLength*bytesPerElem);
        }
    }

    // upload the bands one row at a time into our lookup table texture
    for (i = 0; i < 4; i++) {
        if (bands[i] == NULL) {
            continue;
        }
        j2d_glTexSubImage2D(GL_TEXTURE_2D, 0,
                            0, i, bandLength, 1,
                            GL_LUMINANCE,
                            shortData ? GL_UNSIGNED_SHORT : GL_UNSIGNED_BYTE,
                            bands[i]);
    }

    // restore texture unit 0 (the default) as the active one since
    // the OGLBlitTextureToSurface() method is responsible for binding the
    // source image texture, which will happen later
    j2d_glActiveTextureARB(GL_TEXTURE0_ARB);
}

void
OGLBufImgOps_DisableLookupOp(OGLContext *oglc)
{
    J2dTraceLn(J2D_TRACE_INFO, "OGLBufImgOps_DisableLookupOp");

    RETURN_IF_NULL(oglc);

    // disable the LookupOp shader
    j2d_glUseProgramObjectARB(0);

    // disable the lookup table on texture unit 1
    j2d_glActiveTextureARB(GL_TEXTURE1_ARB);
    j2d_glDisable(GL_TEXTURE_2D);
    j2d_glActiveTextureARB(GL_TEXTURE0_ARB);
}

#endif /* !HEADLESS */
