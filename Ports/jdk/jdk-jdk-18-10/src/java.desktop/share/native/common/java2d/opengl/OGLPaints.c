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
#include <string.h>

#include "sun_java2d_SunGraphics2D.h"
#include "sun_java2d_pipe_BufferedPaints.h"

#include "OGLPaints.h"
#include "OGLContext.h"
#include "OGLRenderQueue.h"
#include "OGLSurfaceData.h"

void
OGLPaints_ResetPaint(OGLContext *oglc)
{
    jubyte ea;

    J2dTraceLn(J2D_TRACE_INFO, "OGLPaints_ResetPaint");

    RETURN_IF_NULL(oglc);
    J2dTraceLn1(J2D_TRACE_VERBOSE, "  state=%d", oglc->paintState);
    RESET_PREVIOUS_OP();

    if (oglc->useMask) {
        // switch to texture unit 1, where paint state is currently enabled
        j2d_glActiveTextureARB(GL_TEXTURE1_ARB);
    }

    switch (oglc->paintState) {
    case sun_java2d_SunGraphics2D_PAINT_GRADIENT:
        j2d_glDisable(GL_TEXTURE_1D);
        j2d_glDisable(GL_TEXTURE_GEN_S);
        break;

    case sun_java2d_SunGraphics2D_PAINT_TEXTURE:
        // Note: The texture object used in SetTexturePaint() will
        // still be bound at this point, so it is safe to call the following.
        OGLSD_RESET_TEXTURE_WRAP(GL_TEXTURE_2D);
        j2d_glDisable(GL_TEXTURE_2D);
        j2d_glDisable(GL_TEXTURE_GEN_S);
        j2d_glDisable(GL_TEXTURE_GEN_T);
        break;

    case sun_java2d_SunGraphics2D_PAINT_LIN_GRADIENT:
    case sun_java2d_SunGraphics2D_PAINT_RAD_GRADIENT:
        j2d_glUseProgramObjectARB(0);
        j2d_glDisable(GL_TEXTURE_1D);
        break;

    case sun_java2d_SunGraphics2D_PAINT_ALPHACOLOR:
    default:
        break;
    }

    if (oglc->useMask) {
        // restore control to texture unit 0
        j2d_glActiveTextureARB(GL_TEXTURE0_ARB);
    }

    // set each component of the current color state to the extra alpha
    // value, which will effectively apply the extra alpha to each fragment
    // in paint/texturing operations
    ea = (jubyte)(oglc->extraAlpha * 0xff + 0.5f);
    j2d_glColor4ub(ea, ea, ea, ea);
    oglc->pixel = (ea << 24) | (ea << 16) | (ea << 8) | (ea << 0);
    oglc->r = ea;
    oglc->g = ea;
    oglc->b = ea;
    oglc->a = ea;
    oglc->useMask = JNI_FALSE;
    oglc->paintState = -1;
}

void
OGLPaints_SetColor(OGLContext *oglc, jint pixel)
{
    jubyte r, g, b, a;

    J2dTraceLn1(J2D_TRACE_INFO, "OGLPaints_SetColor: pixel=%08x", pixel);

    RETURN_IF_NULL(oglc);

    // glColor*() is allowed within glBegin()/glEnd() pairs, so
    // no need to reset the current op state here unless the paint
    // state really needs to be changed
    if (oglc->paintState > sun_java2d_SunGraphics2D_PAINT_ALPHACOLOR) {
        OGLPaints_ResetPaint(oglc);
    }

    // store the raw (unmodified) pixel value, which may be used for
    // special operations later
    oglc->pixel = pixel;

    if (oglc->compState != sun_java2d_SunGraphics2D_COMP_XOR) {
        r = (jubyte)(pixel >> 16);
        g = (jubyte)(pixel >>  8);
        b = (jubyte)(pixel >>  0);
        a = (jubyte)(pixel >> 24);

        J2dTraceLn4(J2D_TRACE_VERBOSE,
                    "  updating color: r=%02x g=%02x b=%02x a=%02x",
                    r, g, b, a);
    } else {
        pixel ^= oglc->xorPixel;

        r = (jubyte)(pixel >> 16);
        g = (jubyte)(pixel >>  8);
        b = (jubyte)(pixel >>  0);
        a = 0xff;

        J2dTraceLn4(J2D_TRACE_VERBOSE,
                    "  updating xor color: r=%02x g=%02x b=%02x xorpixel=%08x",
                    r, g, b, oglc->xorPixel);
    }

    j2d_glColor4ub(r, g, b, a);
    oglc->r = r;
    oglc->g = g;
    oglc->b = b;
    oglc->a = a;
    oglc->useMask = JNI_FALSE;
    oglc->paintState = sun_java2d_SunGraphics2D_PAINT_ALPHACOLOR;
}

/************************* GradientPaint support ****************************/

static GLuint gradientTexID = 0;

static void
OGLPaints_InitGradientTexture()
{
    GLclampf priority = 1.0f;

    J2dTraceLn(J2D_TRACE_INFO, "OGLPaints_InitGradientTexture");

    j2d_glGenTextures(1, &gradientTexID);
    j2d_glBindTexture(GL_TEXTURE_1D, gradientTexID);
    j2d_glPrioritizeTextures(1, &gradientTexID, &priority);
    j2d_glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    j2d_glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    j2d_glTexImage1D(GL_TEXTURE_1D, 0,
                     GL_RGBA8, 2, 0,
                     GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, NULL);
}

void
OGLPaints_SetGradientPaint(OGLContext *oglc,
                           jboolean useMask, jboolean cyclic,
                           jdouble p0, jdouble p1, jdouble p3,
                           jint pixel1, jint pixel2)
{
    GLdouble texParams[4];
    GLuint pixels[2];

    J2dTraceLn(J2D_TRACE_INFO, "OGLPaints_SetGradientPaint");

    RETURN_IF_NULL(oglc);
    OGLPaints_ResetPaint(oglc);

    texParams[0] = p0;
    texParams[1] = p1;
    texParams[2] = 0.0;
    texParams[3] = p3;

    pixels[0] = pixel1;
    pixels[1] = pixel2;

    if (useMask) {
        // set up the paint on texture unit 1 (instead of the usual unit 0)
        j2d_glActiveTextureARB(GL_TEXTURE1_ARB);
        j2d_glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
    } else {
        // texture unit 0 is already active; we can use the helper macro here
        OGLC_UPDATE_TEXTURE_FUNCTION(oglc, GL_MODULATE);
    }

    if (gradientTexID == 0) {
        OGLPaints_InitGradientTexture();
    }

    j2d_glEnable(GL_TEXTURE_1D);
    j2d_glEnable(GL_TEXTURE_GEN_S);
    j2d_glBindTexture(GL_TEXTURE_1D, gradientTexID);
    j2d_glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S,
                        cyclic ? GL_REPEAT : GL_CLAMP_TO_EDGE);
    j2d_glTexGeni(GL_S, GL_TEXTURE_GEN_MODE, GL_OBJECT_LINEAR);
    j2d_glTexGendv(GL_S, GL_OBJECT_PLANE, texParams);

    j2d_glTexSubImage1D(GL_TEXTURE_1D, 0,
                        0, 2, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, pixels);

    if (useMask) {
        // restore control to texture unit 0
        j2d_glActiveTextureARB(GL_TEXTURE0_ARB);
    }

    // oglc->pixel has been set appropriately in OGLPaints_ResetPaint()
    oglc->useMask = useMask;
    oglc->paintState = sun_java2d_SunGraphics2D_PAINT_GRADIENT;
}

/************************** TexturePaint support ****************************/

void
OGLPaints_SetTexturePaint(OGLContext *oglc,
                          jboolean useMask,
                          jlong pSrcOps, jboolean filter,
                          jdouble xp0, jdouble xp1, jdouble xp3,
                          jdouble yp0, jdouble yp1, jdouble yp3)
{
    OGLSDOps *srcOps = (OGLSDOps *)jlong_to_ptr(pSrcOps);
    GLdouble xParams[4];
    GLdouble yParams[4];
    GLint hint = (filter ? GL_LINEAR : GL_NEAREST);

    J2dTraceLn(J2D_TRACE_INFO, "OGLPaints_SetTexturePaint");

    RETURN_IF_NULL(srcOps);
    RETURN_IF_NULL(oglc);
    OGLPaints_ResetPaint(oglc);

    xParams[0] = xp0;
    xParams[1] = xp1;
    xParams[2] = 0.0;
    xParams[3] = xp3;

    yParams[0] = yp0;
    yParams[1] = yp1;
    yParams[2] = 0.0;
    yParams[3] = yp3;

    /*
     * Note that we explicitly use GL_TEXTURE_2D below rather than using
     * srcOps->textureTarget.  This is because the texture wrap mode employed
     * here (GL_REPEAT) is not available for GL_TEXTURE_RECTANGLE_ARB targets.
     * The setup code in OGLPaints.Texture.isPaintValid() and in
     * OGLSurfaceData.initTexture() ensures that we only get here for
     * GL_TEXTURE_2D targets.
     */

    if (useMask) {
        // set up the paint on texture unit 1 (instead of the usual unit 0)
        j2d_glActiveTextureARB(GL_TEXTURE1_ARB);
        j2d_glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
    } else {
        // texture unit 0 is already active; we can use the helper macro here
        OGLC_UPDATE_TEXTURE_FUNCTION(oglc, GL_MODULATE);
    }

    j2d_glEnable(GL_TEXTURE_2D);
    j2d_glEnable(GL_TEXTURE_GEN_S);
    j2d_glEnable(GL_TEXTURE_GEN_T);
    j2d_glBindTexture(GL_TEXTURE_2D, srcOps->textureID);
    OGLSD_UPDATE_TEXTURE_FILTER(srcOps, hint);
    OGLSD_UPDATE_TEXTURE_WRAP(GL_TEXTURE_2D, GL_REPEAT);
    j2d_glTexGeni(GL_S, GL_TEXTURE_GEN_MODE, GL_OBJECT_LINEAR);
    j2d_glTexGendv(GL_S, GL_OBJECT_PLANE, xParams);
    j2d_glTexGeni(GL_T, GL_TEXTURE_GEN_MODE, GL_OBJECT_LINEAR);
    j2d_glTexGendv(GL_T, GL_OBJECT_PLANE, yParams);

    if (useMask) {
        // restore control to texture unit 0
        j2d_glActiveTextureARB(GL_TEXTURE0_ARB);
    }

    // oglc->pixel has been set appropriately in OGLPaints_ResetPaint()
    oglc->useMask = useMask;
    oglc->paintState = sun_java2d_SunGraphics2D_PAINT_TEXTURE;
}

/****************** Shared MultipleGradientPaint support ********************/

/**
 * These constants are identical to those defined in the
 * MultipleGradientPaint.CycleMethod enum; they are copied here for
 * convenience (ideally we would pull them directly from the Java level,
 * but that entails more hassle than it is worth).
 */
#define CYCLE_NONE    0
#define CYCLE_REFLECT 1
#define CYCLE_REPEAT  2

/**
 * The following constants are flags that can be bitwise-or'ed together
 * to control how the MultipleGradientPaint shader source code is generated:
 *
 *   MULTI_CYCLE_METHOD
 *     Placeholder for the CycleMethod enum constant.
 *
 *   MULTI_LARGE
 *     If set, use the (slower) shader that supports a larger number of
 *     gradient colors; otherwise, use the optimized codepath.  See
 *     the MAX_FRACTIONS_SMALL/LARGE constants below for more details.
 *
 *   MULTI_USE_MASK
 *     If set, apply the alpha mask value from texture unit 0 to the
 *     final color result (only used in the MaskFill case).
 *
 *   MULTI_LINEAR_RGB
 *     If set, convert the linear RGB result back into the sRGB color space.
 */
#define MULTI_CYCLE_METHOD (3 << 0)
#define MULTI_LARGE        (1 << 2)
#define MULTI_USE_MASK     (1 << 3)
#define MULTI_LINEAR_RGB   (1 << 4)

/**
 * This value determines the size of the array of programs for each
 * MultipleGradientPaint type.  This value reflects the maximum value that
 * can be represented by performing a bitwise-or of all the MULTI_*
 * constants defined above.
 */
#define MAX_PROGRAMS 32

/** Evaluates to true if the given bit is set on the local flags variable. */
#define IS_SET(flagbit) \
    (((flags) & (flagbit)) != 0)

/** Composes the given parameters as flags into the given flags variable.*/
#define COMPOSE_FLAGS(flags, cycleMethod, large, useMask, linear) \
    do {                                                   \
        flags |= ((cycleMethod) & MULTI_CYCLE_METHOD);     \
        if (large)   flags |= MULTI_LARGE;                 \
        if (useMask) flags |= MULTI_USE_MASK;              \
        if (linear)  flags |= MULTI_LINEAR_RGB;            \
    } while (0)

/** Extracts the CycleMethod enum value from the given flags variable. */
#define EXTRACT_CYCLE_METHOD(flags) \
    ((flags) & MULTI_CYCLE_METHOD)

/**
 * The maximum number of gradient "stops" supported by the fragment shader
 * and related code.  When the MULTI_LARGE flag is set, we will use
 * MAX_FRACTIONS_LARGE; otherwise, we use MAX_FRACTIONS_SMALL.  By having
 * two separate values, we can have one highly optimized shader (SMALL) that
 * supports only a few fractions/colors, and then another, less optimal
 * shader that supports more stops.
 */
#define MAX_FRACTIONS sun_java2d_pipe_BufferedPaints_MULTI_MAX_FRACTIONS
#define MAX_FRACTIONS_LARGE MAX_FRACTIONS
#define MAX_FRACTIONS_SMALL 4

/**
 * The maximum number of gradient colors supported by all of the gradient
 * fragment shaders.  Note that this value must be a power of two, as it
 * determines the size of the 1D texture created below.  It also must be
 * greater than or equal to MAX_FRACTIONS (there is no strict requirement
 * that the two values be equal).
 */
#define MAX_COLORS 16

/**
 * The handle to the gradient color table texture object used by the shaders.
 */
static GLuint multiGradientTexID = 0;

/**
 * This is essentially a template of the shader source code that can be used
 * for either LinearGradientPaint or RadialGradientPaint.  It includes the
 * structure and some variables that are common to each; the remaining
 * code snippets (for CycleMethod, ColorSpaceType, and mask modulation)
 * are filled in prior to compiling the shader at runtime depending on the
 * paint parameters.  See OGLPaints_CreateMultiGradProgram() for more details.
 */
static const char *multiGradientShaderSource =
    // gradient texture size (in texels)
    "const int TEXTURE_SIZE = %d;"
    // maximum number of fractions/colors supported by this shader
    "const int MAX_FRACTIONS = %d;"
    // size of a single texel
    "const float FULL_TEXEL = (1.0 / float(TEXTURE_SIZE));"
    // size of half of a single texel
    "const float HALF_TEXEL = (FULL_TEXEL / 2.0);"
    // texture containing the gradient colors
    "uniform sampler1D colors;"
    // array of gradient stops/fractions
    "uniform float fractions[MAX_FRACTIONS];"
    // array of scale factors (one for each interval)
    "uniform float scaleFactors[MAX_FRACTIONS-1];"
    // (placeholder for mask variable)
    "%s"
    // (placeholder for Linear/RadialGP-specific variables)
    "%s"
    ""
    "void main(void)"
    "{"
    "    float dist;"
         // (placeholder for Linear/RadialGradientPaint-specific code)
    "    %s"
    ""
    "    float tc;"
         // (placeholder for CycleMethod-specific code)
    "    %s"
    ""
         // calculate interpolated color
    "    vec4 result = texture1D(colors, tc);"
    ""
         // (placeholder for ColorSpace conversion code)
    "    %s"
    ""
         // (placeholder for mask modulation code)
    "    %s"
    ""
         // modulate with gl_Color in order to apply extra alpha
    "    gl_FragColor = result * gl_Color;"
    "}";

/**
 * This code takes a "dist" value as input (as calculated earlier by the
 * LGP/RGP-specific code) in the range [0,1] and produces a texture
 * coordinate value "tc" that represents the position of the chosen color
 * in the one-dimensional gradient texture (also in the range [0,1]).
 *
 * One naive way to implement this would be to iterate through the fractions
 * to figure out in which interval "dist" falls, and then compute the
 * relative distance between the two nearest stops.  This approach would
 * require an "if" check on every iteration, and it is best to avoid
 * conditionals in fragment shaders for performance reasons.  Also, one might
 * be tempted to use a break statement to jump out of the loop once the
 * interval was found, but break statements (and non-constant loop bounds)
 * are not natively available on most graphics hardware today, so that is
 * a non-starter.
 *
 * The more optimal approach used here avoids these issues entirely by using
 * an accumulation function that is equivalent to the process described above.
 * The scaleFactors array is pre-initialized at enable time as follows:
 *     scaleFactors[i] = 1.0 / (fractions[i+1] - fractions[i]);
 *
 * For each iteration, we subtract fractions[i] from dist and then multiply
 * that value by scaleFactors[i].  If we are within the target interval,
 * this value will be a fraction in the range [0,1] indicating the relative
 * distance between fraction[i] and fraction[i+1].  If we are below the
 * target interval, this value will be negative, so we clamp it to zero
 * to avoid accumulating any value.  If we are above the target interval,
 * the value will be greater than one, so we clamp it to one.  Upon exiting
 * the loop, we will have accumulated zero or more 1.0's and a single
 * fractional value.  This accumulated value tells us the position of the
 * fragment color in the one-dimensional gradient texture, i.e., the
 * texcoord called "tc".
 */
static const char *texCoordCalcCode =
    "int i;"
    "float relFraction = 0.0;"
    "for (i = 0; i < MAX_FRACTIONS-1; i++) {"
    "    relFraction +="
    "        clamp((dist - fractions[i]) * scaleFactors[i], 0.0, 1.0);"
    "}"
    // we offset by half a texel so that we find the linearly interpolated
    // color between the two texel centers of interest
    "tc = HALF_TEXEL + (FULL_TEXEL * relFraction);";

/** Code for NO_CYCLE that gets plugged into the CycleMethod placeholder. */
static const char *noCycleCode =
    "if (dist <= 0.0) {"
    "    tc = 0.0;"
    "} else if (dist >= 1.0) {"
    "    tc = 1.0;"
    "} else {"
         // (placeholder for texcoord calculation)
    "    %s"
    "}";

/** Code for REFLECT that gets plugged into the CycleMethod placeholder. */
static const char *reflectCode =
    "dist = 1.0 - (abs(fract(dist * 0.5) - 0.5) * 2.0);"
    // (placeholder for texcoord calculation)
    "%s";

/** Code for REPEAT that gets plugged into the CycleMethod placeholder. */
static const char *repeatCode =
    "dist = fract(dist);"
    // (placeholder for texcoord calculation)
    "%s";

static void
OGLPaints_InitMultiGradientTexture()
{
    GLclampf priority = 1.0f;

    J2dTraceLn(J2D_TRACE_INFO, "OGLPaints_InitMultiGradientTexture");

    j2d_glGenTextures(1, &multiGradientTexID);
    j2d_glBindTexture(GL_TEXTURE_1D, multiGradientTexID);
    j2d_glPrioritizeTextures(1, &multiGradientTexID, &priority);
    j2d_glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    j2d_glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    j2d_glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    j2d_glTexImage1D(GL_TEXTURE_1D, 0,
                     GL_RGBA8, MAX_COLORS, 0,
                     GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, NULL);
}

/**
 * Compiles and links the MultipleGradientPaint shader program.  If
 * successful, this function returns a handle to the newly created
 * shader program; otherwise returns 0.
 */
static GLhandleARB
OGLPaints_CreateMultiGradProgram(jint flags,
                                 char *paintVars, char *distCode)
{
    GLhandleARB multiGradProgram;
    GLint loc;
    char *maskVars = "";
    char *maskCode = "";
    char *colorSpaceCode = "";
    char cycleCode[1500];
    char finalSource[3000];
    jint cycleMethod = EXTRACT_CYCLE_METHOD(flags);
    jint maxFractions = IS_SET(MULTI_LARGE) ?
        MAX_FRACTIONS_LARGE : MAX_FRACTIONS_SMALL;

    J2dTraceLn(J2D_TRACE_INFO, "OGLPaints_CreateMultiGradProgram");

    if (IS_SET(MULTI_USE_MASK)) {
        /*
         * This code modulates the calculated result color with the
         * corresponding alpha value from the alpha mask texture active
         * on texture unit 0.  Only needed when useMask is true (i.e., only
         * for MaskFill operations).
         */
        maskVars = "uniform sampler2D mask;";
        maskCode = "result *= texture2D(mask, gl_TexCoord[0].st);";
    } else {
        /*
         * REMIND: This is really wacky, but the gradient shaders will
         * produce completely incorrect results on ATI hardware (at least
         * on first-gen (R300-based) boards) if the shader program does not
         * try to access texture coordinates by using a gl_TexCoord[*]
         * variable.  This problem really should be addressed by ATI, but
         * in the meantime it seems we can workaround the issue by inserting
         * a benign operation that accesses gl_TexCoord[0].  Note that we
         * only need to do this for ATI boards and only in the !useMask case,
         * because the useMask case already does access gl_TexCoord[1] and
         * is therefore not affected by this driver bug.
         */
        const char *vendor = (const char *)j2d_glGetString(GL_VENDOR);
        if (vendor != NULL && strncmp(vendor, "ATI", 3) == 0) {
            maskCode = "dist = gl_TexCoord[0].s;";
        }
    }

    if (IS_SET(MULTI_LINEAR_RGB)) {
        /*
         * This code converts a single pixel in linear RGB space back
         * into sRGB (note: this code was adapted from the
         * MultipleGradientPaintContext.convertLinearRGBtoSRGB() method).
         */
        colorSpaceCode =
            "result.rgb = 1.055 * pow(result.rgb, vec3(0.416667)) - 0.055;";
    }

    if (cycleMethod == CYCLE_NONE) {
        sprintf(cycleCode, noCycleCode, texCoordCalcCode);
    } else if (cycleMethod == CYCLE_REFLECT) {
        sprintf(cycleCode, reflectCode, texCoordCalcCode);
    } else { // (cycleMethod == CYCLE_REPEAT)
        sprintf(cycleCode, repeatCode, texCoordCalcCode);
    }

    // compose the final source code string from the various pieces
    sprintf(finalSource, multiGradientShaderSource,
            MAX_COLORS, maxFractions,
            maskVars, paintVars, distCode,
            cycleCode, colorSpaceCode, maskCode);

    multiGradProgram = OGLContext_CreateFragmentProgram(finalSource);
    if (multiGradProgram == 0) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLPaints_CreateMultiGradProgram: error creating program");
        return 0;
    }

    // "use" the program object temporarily so that we can set the uniforms
    j2d_glUseProgramObjectARB(multiGradProgram);

    // set the "uniform" texture unit bindings
    if (IS_SET(MULTI_USE_MASK)) {
        loc = j2d_glGetUniformLocationARB(multiGradProgram, "mask");
        j2d_glUniform1iARB(loc, 0); // texture unit 0
        loc = j2d_glGetUniformLocationARB(multiGradProgram, "colors");
        j2d_glUniform1iARB(loc, 1); // texture unit 1
    } else {
        loc = j2d_glGetUniformLocationARB(multiGradProgram, "colors");
        j2d_glUniform1iARB(loc, 0); // texture unit 0
    }

    // "unuse" the program object; it will be re-bound later as needed
    j2d_glUseProgramObjectARB(0);

    if (multiGradientTexID == 0) {
        OGLPaints_InitMultiGradientTexture();
    }

    return multiGradProgram;
}

/**
 * Called from the OGLPaints_SetLinear/RadialGradientPaint() methods
 * in order to setup the fraction/color values that are common to both.
 */
static void
OGLPaints_SetMultiGradientPaint(GLhandleARB multiGradProgram,
                                jint numStops,
                                void *pFractions, void *pPixels)
{
    jint maxFractions = (numStops > MAX_FRACTIONS_SMALL) ?
        MAX_FRACTIONS_LARGE : MAX_FRACTIONS_SMALL;
    GLfloat scaleFactors[MAX_FRACTIONS-1];
    GLfloat *fractions = (GLfloat *)pFractions;
    GLint *pixels = (GLint *)pPixels;
    GLint loc;
    int i;

    // enable the MultipleGradientPaint shader
    j2d_glUseProgramObjectARB(multiGradProgram);

    // update the "uniform" fraction values
    loc = j2d_glGetUniformLocationARB(multiGradProgram, "fractions");
    if (numStops < maxFractions) {
        // fill the remainder of the fractions array with all zeros to
        // prevent using garbage values from previous paints
        GLfloat allZeros[MAX_FRACTIONS];
        memset(allZeros, 0, sizeof(GLfloat)*MAX_FRACTIONS);
        j2d_glUniform1fvARB(loc, maxFractions, allZeros);
    }
    j2d_glUniform1fvARB(loc, numStops, fractions);

    // update the "uniform" scale values
    loc = j2d_glGetUniformLocationARB(multiGradProgram, "scaleFactors");
    for (i = 0; i < numStops-1; i++) {
        // calculate a scale factor for each interval
        scaleFactors[i] = 1.0f / (fractions[i+1] - fractions[i]);
    }
    for (; i < maxFractions-1; i++) {
        // fill remaining scale factors with zero
        scaleFactors[i] = 0.0f;
    }
    j2d_glUniform1fvARB(loc, maxFractions-1, scaleFactors);

    // update the texture containing the gradient colors
    j2d_glEnable(GL_TEXTURE_1D);
    j2d_glBindTexture(GL_TEXTURE_1D, multiGradientTexID);
    j2d_glTexSubImage1D(GL_TEXTURE_1D, 0,
                        0, numStops,
                        GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV,
                        pixels);
    if (numStops < MAX_COLORS) {
        // when we don't have enough colors to fill the entire color gradient,
        // we have to replicate the last color in the right-most texel for
        // the NO_CYCLE case where the texcoord is sometimes forced to 1.0
        j2d_glTexSubImage1D(GL_TEXTURE_1D, 0,
                            MAX_COLORS-1, 1,
                            GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV,
                            pixels+(numStops-1));
    }
}

/********************** LinearGradientPaint support *************************/

/**
 * The handles to the LinearGradientPaint fragment program objects.  The
 * index to the array should be a bitwise-or'ing of the MULTI_* flags defined
 * above.  Note that most applications will likely need to initialize one
 * or two of these elements, so the array is usually sparsely populated.
 */
static GLhandleARB linearGradPrograms[MAX_PROGRAMS];

/**
 * Compiles and links the LinearGradientPaint shader program.  If successful,
 * this function returns a handle to the newly created shader program;
 * otherwise returns 0.
 */
static GLhandleARB
OGLPaints_CreateLinearGradProgram(jint flags)
{
    char *paintVars;
    char *distCode;

    J2dTraceLn1(J2D_TRACE_INFO,
                "OGLPaints_CreateLinearGradProgram",
                flags);

    /*
     * To simplify the code and to make it easier to upload a number of
     * uniform values at once, we pack a bunch of scalar (float) values
     * into vec3 values below.  Here's how the values are related:
     *
     *   params.x = p0
     *   params.y = p1
     *   params.z = p3
     *
     *   yoff = dstOps->yOffset + dstOps->height
     */
    paintVars =
        "uniform vec3 params;"
        "uniform float yoff;";
    distCode =
        // note that gl_FragCoord is in window space relative to the
        // lower-left corner, so we have to flip the y-coordinate here
        "vec3 fragCoord = vec3(gl_FragCoord.x, yoff-gl_FragCoord.y, 1.0);"
        "dist = dot(params, fragCoord);";

    return OGLPaints_CreateMultiGradProgram(flags, paintVars, distCode);
}

void
OGLPaints_SetLinearGradientPaint(OGLContext *oglc, OGLSDOps *dstOps,
                                 jboolean useMask, jboolean linear,
                                 jint cycleMethod, jint numStops,
                                 jfloat p0, jfloat p1, jfloat p3,
                                 void *fractions, void *pixels)
{
    GLhandleARB linearGradProgram;
    GLint loc;
    jboolean large = (numStops > MAX_FRACTIONS_SMALL);
    jint flags = 0;

    J2dTraceLn(J2D_TRACE_INFO, "OGLPaints_SetLinearGradientPaint");

    RETURN_IF_NULL(oglc);
    RETURN_IF_NULL(dstOps);
    OGLPaints_ResetPaint(oglc);

    COMPOSE_FLAGS(flags, cycleMethod, large, useMask, linear);

    if (useMask) {
        // set up the paint on texture unit 1 (instead of the usual unit 0)
        j2d_glActiveTextureARB(GL_TEXTURE1_ARB);
    }
    // no need to set GL_MODULATE here (it is ignored when shader is enabled)

    // locate/initialize the shader program for the given flags
    if (linearGradPrograms[flags] == 0) {
        linearGradPrograms[flags] = OGLPaints_CreateLinearGradProgram(flags);
        if (linearGradPrograms[flags] == 0) {
            // shouldn't happen, but just in case...
            return;
        }
    }
    linearGradProgram = linearGradPrograms[flags];

    // update the common "uniform" values (fractions and colors)
    OGLPaints_SetMultiGradientPaint(linearGradProgram,
                                    numStops, fractions, pixels);

    // update the other "uniform" values
    loc = j2d_glGetUniformLocationARB(linearGradProgram, "params");
    j2d_glUniform3fARB(loc, p0, p1, p3);
    loc = j2d_glGetUniformLocationARB(linearGradProgram, "yoff");
    j2d_glUniform1fARB(loc, (GLfloat)(dstOps->yOffset + dstOps->height));

    if (useMask) {
        // restore control to texture unit 0
        j2d_glActiveTextureARB(GL_TEXTURE0_ARB);
    }

    // oglc->pixel has been set appropriately in OGLPaints_ResetPaint()
    oglc->useMask = useMask;
    oglc->paintState = sun_java2d_SunGraphics2D_PAINT_LIN_GRADIENT;
}

/********************** RadialGradientPaint support *************************/

/**
 * The handles to the RadialGradientPaint fragment program objects.  The
 * index to the array should be a bitwise-or'ing of the MULTI_* flags defined
 * above.  Note that most applications will likely need to initialize one
 * or two of these elements, so the array is usually sparsely populated.
 */
static GLhandleARB radialGradPrograms[MAX_PROGRAMS];

/**
 * Compiles and links the RadialGradientPaint shader program.  If successful,
 * this function returns a handle to the newly created shader program;
 * otherwise returns 0.
 */
static GLhandleARB
OGLPaints_CreateRadialGradProgram(jint flags)
{
    char *paintVars;
    char *distCode;

    J2dTraceLn1(J2D_TRACE_INFO,
                "OGLPaints_CreateRadialGradProgram",
                flags);

    /*
     * To simplify the code and to make it easier to upload a number of
     * uniform values at once, we pack a bunch of scalar (float) values
     * into vec3 and vec4 values below.  Here's how the values are related:
     *
     *   m0.x = m00
     *   m0.y = m01
     *   m0.z = m02
     *
     *   m1.x = m10
     *   m1.y = m11
     *   m1.z = m12
     *
     *   precalc.x = focusX
     *   precalc.y = yoff = dstOps->yOffset + dstOps->height
     *   precalc.z = 1.0 - (focusX * focusX)
     *   precalc.w = 1.0 / precalc.z
     */
    paintVars =
        "uniform vec3 m0;"
        "uniform vec3 m1;"
        "uniform vec4 precalc;";

    /*
     * The following code is derived from Daniel Rice's whitepaper on
     * radial gradient performance (attached to the bug report for 6521533).
     * Refer to that document as well as the setup code in the Java-level
     * BufferedPaints.setRadialGradientPaint() method for more details.
     */
    distCode =
        // note that gl_FragCoord is in window space relative to the
        // lower-left corner, so we have to flip the y-coordinate here
        "vec3 fragCoord ="
        "    vec3(gl_FragCoord.x, precalc.y - gl_FragCoord.y, 1.0);"
        "float x = dot(fragCoord, m0);"
        "float y = dot(fragCoord, m1);"
        "float xfx = x - precalc.x;"
        "dist = (precalc.x*xfx + sqrt(xfx*xfx + y*y*precalc.z))*precalc.w;";

    return OGLPaints_CreateMultiGradProgram(flags, paintVars, distCode);
}

void
OGLPaints_SetRadialGradientPaint(OGLContext *oglc, OGLSDOps *dstOps,
                                 jboolean useMask, jboolean linear,
                                 jint cycleMethod, jint numStops,
                                 jfloat m00, jfloat m01, jfloat m02,
                                 jfloat m10, jfloat m11, jfloat m12,
                                 jfloat focusX,
                                 void *fractions, void *pixels)
{
    GLhandleARB radialGradProgram;
    GLint loc;
    GLfloat yoff, denom, inv_denom;
    jboolean large = (numStops > MAX_FRACTIONS_SMALL);
    jint flags = 0;

    J2dTraceLn(J2D_TRACE_INFO, "OGLPaints_SetRadialGradientPaint");

    RETURN_IF_NULL(oglc);
    RETURN_IF_NULL(dstOps);
    OGLPaints_ResetPaint(oglc);

    COMPOSE_FLAGS(flags, cycleMethod, large, useMask, linear);

    if (useMask) {
        // set up the paint on texture unit 1 (instead of the usual unit 0)
        j2d_glActiveTextureARB(GL_TEXTURE1_ARB);
    }
    // no need to set GL_MODULATE here (it is ignored when shader is enabled)

    // locate/initialize the shader program for the given flags
    if (radialGradPrograms[flags] == 0) {
        radialGradPrograms[flags] = OGLPaints_CreateRadialGradProgram(flags);
        if (radialGradPrograms[flags] == 0) {
            // shouldn't happen, but just in case...
            return;
        }
    }
    radialGradProgram = radialGradPrograms[flags];

    // update the common "uniform" values (fractions and colors)
    OGLPaints_SetMultiGradientPaint(radialGradProgram,
                                    numStops, fractions, pixels);

    // update the other "uniform" values
    loc = j2d_glGetUniformLocationARB(radialGradProgram, "m0");
    j2d_glUniform3fARB(loc, m00, m01, m02);
    loc = j2d_glGetUniformLocationARB(radialGradProgram, "m1");
    j2d_glUniform3fARB(loc, m10, m11, m12);

    // pack a few unrelated, precalculated values into a single vec4
    yoff = (GLfloat)(dstOps->yOffset + dstOps->height);
    denom = 1.0f - (focusX * focusX);
    inv_denom = 1.0f / denom;
    loc = j2d_glGetUniformLocationARB(radialGradProgram, "precalc");
    j2d_glUniform4fARB(loc, focusX, yoff, denom, inv_denom);

    if (useMask) {
        // restore control to texture unit 0
        j2d_glActiveTextureARB(GL_TEXTURE0_ARB);
    }

    // oglc->pixel has been set appropriately in OGLPaints_ResetPaint()
    oglc->useMask = useMask;
    oglc->paintState = sun_java2d_SunGraphics2D_PAINT_RAD_GRADIENT;
}

#endif /* !HEADLESS */
