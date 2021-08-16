/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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

/**
 * This file contains a standalone program that is used to generate the
 * D3DShaders.h file.  The program invokes the fxc (D3D Shader Compiler)
 * utility, which is part of the DirectX 9/10 SDK.  Since most JDK
 * developers (other than some Java 2D engineers) do not have the full DXSDK
 * installed, and since we do not want to make the JDK build process
 * dependent on the full DXSDK installation, we have chosen not to make
 * this shader compilation step part of the build process.  Instead, it is
 * only necessary to compile and run this program when changes need to be
 * made to the shader code contained within.  Typically, this only happens
 * on an as-needed basis by someone familiar with the D3D pipeline.  Running
 * this program is fairly straightforward:
 *
 *   % rm D3DShaders.h
 *   % cl D3DShaderGen.c
 *   % D3DShaderGen.exe
 *
 * (And don't forget to putback the updated D3DShaders.h file!)
 */

#include <stdio.h>
#include <process.h>
#include <Windows.h>

static FILE *fpHeader = NULL;
static char *strHeaderFile = "D3DShaders.h";

/** Evaluates to true if the given bit is set on the local flags variable. */
#define IS_SET(flagbit) \
    (((flags) & (flagbit)) != 0)

// REMIND
//#define J2dTraceLn(a, b) fprintf(stderr, "%s\n", b);
//#define J2dTraceLn1(a, b, c) fprintf(stderr, b, c);
#define J2dTraceLn(a, b)
#define J2dTraceLn1(a, b, c)

/************************* General shader support ***************************/

static void
D3DShaderGen_WriteShader(char *source, char *target, char *name, int flags)
{
    FILE *fpTmp;
    char varname[50];
    char *args[8];
    int val;

    // write source to tmp.hlsl
    fpTmp = fopen("tmp.hlsl", "w");
    fprintf(fpTmp, "%s\n", source);
    fclose(fpTmp);

    {
        PROCESS_INFORMATION pi;
        STARTUPINFO si;
        char pargs[300];
        sprintf(pargs,
                "c:\\progra~1\\mi5889~1\\utilit~1\\bin\\x86\\fxc.exe "
                "/T %s /Vn %s%d /Fh tmp.h tmp.hlsl",
                // uncomment the following line to generate debug
                // info in the shader header file (may be useful
                // for testing/debuggging purposes, but it nearly
                // doubles the size of the header file and compiled
                // shader programs - off for production builds)
                //"/Zi /T %s /Vn %s%d /Fh tmp.h tmp.hlsl",
                target, name, flags);
        fprintf(stderr, "%s\n", pargs);
        memset(&si, 0, sizeof(si));
        si.cb = sizeof(si);
        si.dwFlags = STARTF_USESTDHANDLES;
        //si.hStdOutput = GetStdHandle(STD_OUTPUT_HANDLE);
        //fprintf(stderr, "%s\n", pargs);
        val = CreateProcess(0, pargs, 0, 0, TRUE,
                            CREATE_NO_WINDOW, NULL, NULL, &si, &pi);

        {
            DWORD code;
            do {
                GetExitCodeProcess(pi.hProcess, &code);
                //fprintf(stderr, "waiting...");
                Sleep(100);
            } while (code == STILL_ACTIVE);

            if (code != 0) {
                fprintf(stderr, "fxc failed for %s%d\n", name, flags);
            }
        }

        CloseHandle(pi.hThread);
        CloseHandle(pi.hProcess);
    }

    // append tmp.h to D3DShaders.h
    {
        int ch;
        fpTmp = fopen("tmp.h", "r");
        while ((ch = fgetc(fpTmp)) != EOF) {
            fputc(ch, fpHeader);
        }
        fclose(fpTmp);
    }
}

static void
D3DShaderGen_WritePixelShader(char *source, char *name, int flags)
{
    D3DShaderGen_WriteShader(source, "ps_2_0", name, flags);
}

#define MULTI_GRAD_CYCLE_METHOD (3 << 0)
/** Extracts the CycleMethod enum value from the given flags variable. */
#define EXTRACT_CYCLE_METHOD(flags) \
    ((flags) & MULTI_GRAD_CYCLE_METHOD)

static void
D3DShaderGen_WriteShaderArray(char *name, int num)
{
    char array[5000];
    char elem[30];
    int i;

    sprintf(array, "const DWORD *%sShaders[] =\n{\n", name);
    for (i = 0; i < num; i++) {
        if (num == 32 && EXTRACT_CYCLE_METHOD(i) == 3) {
            // REMIND: what a hack!
            sprintf(elem, "    NULL,\n");
        } else {
            sprintf(elem, "    %s%d,\n", name, i);
        }
        strcat(array, elem);
    }
    strcat(array, "};\n");

    // append to D3DShaders.h
    fprintf(fpHeader, "%s\n", array);
}

/**************************** ConvolveOp support ****************************/

static const char *convolveShaderSource =
    // image to be convolved
    "sampler2D baseImage   : register(s0);"
    // image edge limits:
    //   imgEdge.xy = imgMin.xy (anything < will be treated as edge case)
    //   imgEdge.zw = imgMax.xy (anything > will be treated as edge case)
    "float4 imgEdge        : register(c0);"
    // value for each location in the convolution kernel:
    //   kernelVals[i].x = offsetX[i]
    //   kernelVals[i].y = offsetY[i]
    //   kernelVals[i].z = kernel[i]
    "float3 kernelVals[%d] : register(c1);"
    ""
    "void main(in float2 tc : TEXCOORD0,"
    "          inout float4 color : COLOR0)"
    "{"
    "    float4 sum = imgEdge - tc.xyxy;"
    ""
    "    if (sum.x > 0 || sum.y > 0 || sum.z < 0 || sum.w < 0) {"
             // (placeholder for edge condition code)
    "        color = %s;"
    "    } else {"
    "        int i;"
    "        sum = float4(0, 0, 0, 0);"
    "        for (i = 0; i < %d; i++) {"
    "            sum +="
    "                kernelVals[i].z *"
    "                tex2D(baseImage, tc + kernelVals[i].xy);"
    "        }"
             // modulate with current color in order to apply extra alpha
    "        color *= sum;"
    "    }"
    ""
    "}";

/**
 * Flags that can be bitwise-or'ed together to control how the shader
 * source code is generated.
 */
#define CONVOLVE_EDGE_ZERO_FILL (1 << 0)
#define CONVOLVE_5X5            (1 << 1)
#define MAX_CONVOLVE            (1 << 2)

static void
D3DShaderGen_GenerateConvolveShader(int flags)
{
    int kernelMax = IS_SET(CONVOLVE_5X5) ? 25 : 9;
    char *edge;
    char finalSource[2000];

    J2dTraceLn1(J2D_TRACE_INFO,
                "D3DShaderGen_GenerateConvolveShader: flags=%d",
                flags);

    if (IS_SET(CONVOLVE_EDGE_ZERO_FILL)) {
        // EDGE_ZERO_FILL: fill in zero at the edges
        edge = "float4(0, 0, 0, 0)";
    } else {
        // EDGE_NO_OP: use the source pixel color at the edges
        edge = "tex2D(baseImage, tc)";
    }

    // compose the final source code string from the various pieces
    sprintf(finalSource, convolveShaderSource,
            kernelMax, edge, kernelMax);

    D3DShaderGen_WritePixelShader(finalSource, "convolve", flags);
}

/**************************** RescaleOp support *****************************/

static const char *rescaleShaderSource =
    // image to be rescaled
    "sampler2D baseImage : register(s0);"
    // vector containing scale factors
    "float4 scaleFactors : register(c0);"
    // vector containing offsets
    "float4 offsets      : register(c1);"
    ""
    "void main(in float2 tc : TEXCOORD0,"
    "          inout float4 color : COLOR0)"
    "{"
    "    float4 srcColor = tex2D(baseImage, tc);"
    ""
         // (placeholder for un-premult code)
    "    %s"
    ""
         // rescale source value
    "    float4 result = (srcColor * scaleFactors) + offsets;"
    ""
         // (placeholder for re-premult code)
    "    %s"
    ""
         // modulate with current color in order to apply extra alpha
    "    color *= result;"
    "}";

/**
 * Flags that can be bitwise-or'ed together to control how the shader
 * source code is generated.
 */
#define RESCALE_NON_PREMULT (1 << 0)
#define MAX_RESCALE         (1 << 1)

static void
D3DShaderGen_GenerateRescaleShader(int flags)
{
    char *preRescale = "";
    char *postRescale = "";
    char finalSource[2000];

    J2dTraceLn1(J2D_TRACE_INFO,
                "D3DShaderGen_GenerateRescaleShader: flags=%d",
                flags);

    if (IS_SET(RESCALE_NON_PREMULT)) {
        preRescale  = "srcColor.rgb /= srcColor.a;";
        postRescale = "result.rgb *= result.a;";
    }

    // compose the final source code string from the various pieces
    sprintf(finalSource, rescaleShaderSource,
            preRescale, postRescale);

    D3DShaderGen_WritePixelShader(finalSource, "rescale", flags);
}

/**************************** LookupOp support ******************************/

static const char *lookupShaderSource =
    // source image (bound to texture unit 0)
    "sampler2D baseImage   : register(s0);"
    // lookup table (bound to texture unit 1)
    "sampler2D lookupTable : register(s1);"
    // offset subtracted from source index prior to lookup step
    "float4 offset         : register(c0);"
    ""
    "void main(in float2 tc : TEXCOORD0,"
    "          inout float4 color : COLOR0)"
    "{"
    "    float4 srcColor = tex2D(baseImage, tc);"
         // (placeholder for un-premult code)
    "    %s"
         // subtract offset from original index
    "    float4 srcIndex = srcColor - offset;"
         // use source value as input to lookup table (note that
         // "v" texcoords are hardcoded to hit texel centers of
         // each row/band in texture)
    "    float4 result;"
    "    result.r = tex2D(lookupTable, float2(srcIndex.r, 0.125)).r;"
    "    result.g = tex2D(lookupTable, float2(srcIndex.g, 0.375)).r;"
    "    result.b = tex2D(lookupTable, float2(srcIndex.b, 0.625)).r;"
         // (placeholder for alpha store code)
    "    %s"
         // (placeholder for re-premult code)
    "    %s"
         // modulate with current color in order to apply extra alpha
    "    color *= result;"
    "}";

/**
 * Flags that can be bitwise-or'ed together to control how the shader
 * source code is generated.
 */
#define LOOKUP_USE_SRC_ALPHA (1 << 0)
#define LOOKUP_NON_PREMULT   (1 << 1)
#define MAX_LOOKUP           (1 << 2)

static void
D3DShaderGen_GenerateLookupShader(int flags)
{
    char *alpha;
    char *preLookup = "";
    char *postLookup = "";
    char finalSource[2000];

    J2dTraceLn1(J2D_TRACE_INFO,
                "D3DShaderGen_GenerateLookupShader: flags=%d",
                flags);

    if (IS_SET(LOOKUP_USE_SRC_ALPHA)) {
        // when numComps is 1 or 3, the alpha is not looked up in the table;
        // just keep the alpha from the source fragment
        alpha = "result.a = srcColor.a;";
    } else {
        // when numComps is 4, the alpha is looked up in the table, just
        // like the other color components from the source fragment
        alpha = "result.a = tex2D(lookupTable, float2(srcIndex.a, 0.875)).r;";
    }
    if (IS_SET(LOOKUP_NON_PREMULT)) {
        preLookup  = "srcColor.rgb /= srcColor.a;";
        postLookup = "result.rgb *= result.a;";
    }

    // compose the final source code string from the various pieces
    sprintf(finalSource, lookupShaderSource,
            preLookup, alpha, postLookup);

    D3DShaderGen_WritePixelShader(finalSource, "lookup", flags);
}

/************************* GradientPaint support ****************************/

/*
 * To simplify the code and to make it easier to upload a number of
 * uniform values at once, we pack a bunch of scalar (float) values
 * into a single float3 below.  Here's how the values are related:
 *
 *   params.x = p0
 *   params.y = p1
 *   params.z = p3
 */
static const char *basicGradientShaderSource =
    "float3 params : register (c0);"
    "float4 color1 : register (c1);"
    "float4 color2 : register (c2);"
    // (placeholder for mask variable)
    "%s"
    ""
    // (placeholder for mask texcoord input)
    "void main(%s"
    "          in float4 winCoord : TEXCOORD%d,"
    "          inout float4 color : COLOR0)"
    "{"
    "    float3 fragCoord = float3(winCoord.x, winCoord.y, 1.0);"
    "    float dist = dot(params.xyz, fragCoord);"
    ""
         // the setup code for p0/p1/p3 translates/scales to hit texel
         // centers (at 0.25 and 0.75) because it is needed for the
         // original/fast texture-based implementation, but it is not
         // desirable for this shader-based implementation, so we
         // re-transform the value here...
    "    dist = (dist - 0.25) * 2.0;"
    ""
    "    float fraction;"
         // (placeholder for cycle code)
    "    %s"
    ""
    "    float4 result = lerp(color1, color2, fraction);"
    ""
         // (placeholder for mask modulation code)
    "    %s"
    ""
         // modulate with current color in order to apply extra alpha
    "    color *= result;"
    "}";

/**
 * Flags that can be bitwise-or'ed together to control how the shader
 * source code is generated.
 */
#define BASIC_GRAD_IS_CYCLIC (1 << 0)
#define BASIC_GRAD_USE_MASK  (1 << 1)
#define MAX_BASIC_GRAD       (1 << 2)

static void
D3DShaderGen_GenerateBasicGradShader(int flags)
{
    int colorSampler = IS_SET(BASIC_GRAD_USE_MASK) ? 1 : 0;
    char *cycleCode;
    char *maskVars = "";
    char *maskInput = "";
    char *maskCode = "";
    char finalSource[3000];

    J2dTraceLn1(J2D_TRACE_INFO,
                "D3DShaderGen_GenerateBasicGradShader",
                flags);

    if (IS_SET(BASIC_GRAD_IS_CYCLIC)) {
        cycleCode =
            "fraction = 1.0 - (abs(frac(dist * 0.5) - 0.5) * 2.0);";
    } else {
        cycleCode =
            "fraction = clamp(dist, 0.0, 1.0);";
    }

    if (IS_SET(BASIC_GRAD_USE_MASK)) {
        /*
         * This code modulates the calculated result color with the
         * corresponding alpha value from the alpha mask texture active
         * on texture unit 0.  Only needed when useMask is true (i.e., only
         * for MaskFill operations).
         */
        maskVars = "sampler2D mask : register(s0);";
        maskInput = "in float4 maskCoord : TEXCOORD0,";
        maskCode = "result *= tex2D(mask, maskCoord.xy).a;";
    }

    // compose the final source code string from the various pieces
    sprintf(finalSource, basicGradientShaderSource,
            maskVars, maskInput, colorSampler, cycleCode, maskCode);

    D3DShaderGen_WritePixelShader(finalSource, "grad", flags);
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
 *   MULTI_GRAD_CYCLE_METHOD
 *     Placeholder for the CycleMethod enum constant.
 *
 *   MULTI_GRAD_LARGE
 *     If set, use the (slower) shader that supports a larger number of
 *     gradient colors; otherwise, use the optimized codepath.  See
 *     the MAX_FRACTIONS_SMALL/LARGE constants below for more details.
 *
 *   MULTI_GRAD_USE_MASK
 *     If set, apply the alpha mask value from texture unit 1 to the
 *     final color result (only used in the MaskFill case).
 *
 *   MULTI_GRAD_LINEAR_RGB
 *     If set, convert the linear RGB result back into the sRGB color space.
 */
//#define MULTI_GRAD_CYCLE_METHOD (3 << 0)
#define MULTI_GRAD_LARGE        (1 << 2)
#define MULTI_GRAD_USE_MASK     (1 << 3)
#define MULTI_GRAD_LINEAR_RGB   (1 << 4)

// REMIND
#define MAX_MULTI_GRAD     (1 << 5)

/** Extracts the CycleMethod enum value from the given flags variable. */
//#define EXTRACT_CYCLE_METHOD(flags) \
//    ((flags) & MULTI_GRAD_CYCLE_METHOD)

/**
 * The maximum number of gradient "stops" supported by the fragment shader
 * and related code.  When the MULTI_GRAD_LARGE flag is set, we will use
 * MAX_FRACTIONS_LARGE; otherwise, we use MAX_FRACTIONS_SMALL.  By having
 * two separate values, we can have one highly optimized shader (SMALL) that
 * supports only a few fractions/colors, and then another, less optimal
 * shader that supports more stops.
 */
#define MAX_FRACTIONS 8
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

static const char *multiGradientShaderSource =
    // gradient texture size (in texels)
    "#define TEXTURE_SIZE  %d\n"
    // maximum number of fractions/colors supported by this shader
    "#define MAX_FRACTIONS %d\n"
    // size of a single texel
    "#define FULL_TEXEL    (1.0 / float(TEXTURE_SIZE))\n"
    // size of half of a single texel
    "#define HALF_TEXEL    (FULL_TEXEL / 2.0)\n"
    // texture containing the gradient colors
    "sampler2D colors                : register (s%d);"
    // array of gradient stops/fractions and corresponding scale factors
    //   fractions[i].x = gradientStop[i]
    //   fractions[i].y = scaleFactor[i]
    "float2 fractions[MAX_FRACTIONS] : register (c0);"
    // (placeholder for mask variable)
    "%s"
    // (placeholder for Linear/RadialGP-specific variables)
    "%s"
    ""
    // (placeholder for mask texcoord input)
    "void main(%s"
    "          in float4 winCoord : TEXCOORD%d,"
    "          inout float4 color : COLOR0)"
    "{"
    "    float dist;"
         // (placeholder for Linear/RadialGradientPaint-specific code)
    "    %s"
    ""
    "    float4 result;"
         // (placeholder for CycleMethod-specific code)
    "    %s"
    ""
         // (placeholder for ColorSpace conversion code)
    "    %s"
    ""
         // (placeholder for mask modulation code)
    "    %s"
    ""
         // modulate with current color in order to apply extra alpha
    "    color *= result;"
    "}";

/*
 * Note: An earlier version of this code would simply calculate a single
 * texcoord:
 *     "tc = HALF_TEXEL + (FULL_TEXEL * relFraction);"
 * and then use that value to do a single texture lookup, taking advantage
 * of the LINEAR texture filtering mode which in theory will do the
 * appropriate linear interpolation between adjacent texels, like this:
 *     "float4 result = tex2D(colors, float2(tc, 0.5));"
 *
 * The problem with that approach is that on certain hardware (from ATI,
 * notably) the LINEAR texture fetch unit has low precision, and would
 * for instance only produce 64 distinct grayscales between white and black,
 * instead of the expected 256.  The visual banding caused by this issue
 * is severe enough to likely cause complaints from developers, so we have
 * devised a new approach below that instead manually fetches the two
 * relevant neighboring texels and then performs the linear interpolation
 * using the lerp() instruction (which does not suffer from the precision
 * issues of the fixed-function texture filtering unit).  This new approach
 * requires a few more instructions and is therefore slightly slower than
 * the old approach (not more than 10% or so).
 */
static const char *texCoordCalcCode =
    "int i;"
    "float relFraction = 0.0;"
    "for (i = 0; i < MAX_FRACTIONS-1; i++) {"
    "    relFraction +="
    "        clamp((dist - fractions[i].x) * fractions[i].y, 0.0, 1.0);"
    "}"
    // we offset by half a texel so that we find the linearly interpolated
    // color between the two texel centers of interest
    "float intPart = floor(relFraction);"
    "float tc1 = HALF_TEXEL + (FULL_TEXEL * intPart);"
    "float tc2 = HALF_TEXEL + (FULL_TEXEL * (intPart + 1.0));"
    "float4 clr1 = tex2D(colors, float2(tc1, 0.5));"
    "float4 clr2 = tex2D(colors, float2(tc2, 0.5));"
    "result = lerp(clr1, clr2, frac(relFraction));";

/** Code for NO_CYCLE that gets plugged into the CycleMethod placeholder. */
static const char *noCycleCode =
    "if (dist <= 0.0) {"
    "    result = tex2D(colors, float2(0.0, 0.5));"
    "} else if (dist >= 1.0) {"
    "    result = tex2D(colors, float2(1.0, 0.5));"
    "} else {"
         // (placeholder for texcoord calculation)
    "    %s"
    "}";

/** Code for REFLECT that gets plugged into the CycleMethod placeholder. */
static const char *reflectCode =
    "dist = 1.0 - (abs(frac(dist * 0.5) - 0.5) * 2.0);"
    // (placeholder for texcoord calculation)
    "%s";

/** Code for REPEAT that gets plugged into the CycleMethod placeholder. */
static const char *repeatCode =
    "dist = frac(dist);"
    // (placeholder for texcoord calculation)
    "%s";

static void
D3DShaderGen_GenerateMultiGradShader(int flags, char *name,
                                     char *paintVars, char *distCode)
{
    char *maskVars = "";
    char *maskInput = "";
    char *maskCode = "";
    char *colorSpaceCode = "";
    char cycleCode[1500];
    char finalSource[3000];
    int colorSampler = IS_SET(MULTI_GRAD_USE_MASK) ? 1 : 0;
    int cycleMethod = EXTRACT_CYCLE_METHOD(flags);
    int maxFractions = IS_SET(MULTI_GRAD_LARGE) ?
        MAX_FRACTIONS_LARGE : MAX_FRACTIONS_SMALL;

    J2dTraceLn(J2D_TRACE_INFO, "OGLPaints_CreateMultiGradProgram");

    if (IS_SET(MULTI_GRAD_USE_MASK)) {
        /*
         * This code modulates the calculated result color with the
         * corresponding alpha value from the alpha mask texture active
         * on texture unit 0.  Only needed when useMask is true (i.e., only
         * for MaskFill operations).
         */
        maskVars = "sampler2D mask : register(s0);";
        maskInput = "in float4 maskCoord : TEXCOORD0,";
        maskCode = "result *= tex2D(mask, maskCoord.xy).a;";
    }

    if (IS_SET(MULTI_GRAD_LINEAR_RGB)) {
        /*
         * This code converts a single pixel in linear RGB space back
         * into sRGB (note: this code was adapted from the
         * MultipleGradientPaintContext.convertLinearRGBtoSRGB() method).
         */
        colorSpaceCode =
            "result.rgb = 1.055 * pow(result.rgb, 0.416667) - 0.055;";
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
            MAX_COLORS, maxFractions, colorSampler,
            maskVars, paintVars, maskInput, colorSampler,
            distCode, cycleCode, colorSpaceCode, maskCode);

    D3DShaderGen_WritePixelShader(finalSource, name, flags);
}

/********************** LinearGradientPaint support *************************/

static void
D3DShaderGen_GenerateLinearGradShader(int flags)
{
    char *paintVars;
    char *distCode;

    J2dTraceLn1(J2D_TRACE_INFO,
                "D3DShaderGen_GenerateLinearGradShader",
                flags);

    /*
     * To simplify the code and to make it easier to upload a number of
     * uniform values at once, we pack a bunch of scalar (float) values
     * into a single float3 below.  Here's how the values are related:
     *
     *   params.x = p0
     *   params.y = p1
     *   params.z = p3
     */
    paintVars =
        "float3 params : register(c16);";
    distCode =
        "float3 fragCoord = float3(winCoord.x, winCoord.y, 1.0);"
        "dist = dot(params.xyz, fragCoord);";

    D3DShaderGen_GenerateMultiGradShader(flags, "linear",
                                         paintVars, distCode);
}

/********************** RadialGradientPaint support *************************/

static void
D3DShaderGen_GenerateRadialGradShader(int flags)
{
    char *paintVars;
    char *distCode;

    J2dTraceLn1(J2D_TRACE_INFO,
                "D3DShaderGen_GenerateRadialGradShader",
                flags);

    /*
     * To simplify the code and to make it easier to upload a number of
     * uniform values at once, we pack a bunch of scalar (float) values
     * into float3 values below.  Here's how the values are related:
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
     *   precalc.y = 1.0 - (focusX * focusX)
     *   precalc.z = 1.0 / precalc.z
     */
    paintVars =
        "float3 m0      : register(c16);"
        "float3 m1      : register(c17);"
        "float3 precalc : register(c18);";

    /*
     * The following code is derived from Daniel Rice's whitepaper on
     * radial gradient performance (attached to the bug report for 6521533).
     * Refer to that document as well as the setup code in the Java-level
     * BufferedPaints.setRadialGradientPaint() method for more details.
     */
    distCode =
        "float3 fragCoord = float3(winCoord.x, winCoord.y, 1.0);"
        "float x = dot(fragCoord, m0);"
        "float y = dot(fragCoord, m1);"
        "float xfx = x - precalc.x;"
        "dist = (precalc.x*xfx + sqrt(xfx*xfx + y*y*precalc.y))*precalc.z;";

    D3DShaderGen_GenerateMultiGradShader(flags, "radial",
                                         paintVars, distCode);
}

/*************************** LCD text support *******************************/

// REMIND: Shader uses texture addressing operations in a dependency chain
//         that is too complex for the target shader model (ps_2_0) to handle
//         (ugh, I guess we can either require ps_3_0 or just use
//         the slower pow intrinsic)
#define POW_LUT 0

static const char *lcdTextShaderSource =
    "float3 srcAdj         : register(c0);"
    "sampler2D glyphTex    : register(s0);"
    "sampler2D dstTex      : register(s1);"
#if POW_LUT
    "sampler3D invgammaTex : register(s2);"
    "sampler3D gammaTex    : register(s3);"
#else
    "float3 invgamma       : register(c1);"
    "float3 gamma          : register(c2);"
#endif
    ""
    "void main(in float2 tc0 : TEXCOORD0,"
    "          in float2 tc1 : TEXCOORD1,"
    "          inout float4 color : COLOR0)"
    "{"
         // load the RGB value from the glyph image at the current texcoord
    "    float3 glyphClr = tex2D(glyphTex, tc0).rgb;"
    "    if (!any(glyphClr)) {"
             // zero coverage, so skip this fragment
    "        discard;"
    "    }"
         // load the RGB value from the corresponding destination pixel
    "    float3 dstClr = tex2D(dstTex, tc1).rgb;"
         // gamma adjust the dest color using the invgamma LUT
#if POW_LUT
    "    float3 dstAdj = tex3D(invgammaTex, dstClr).rgb;"
#else
    "    float3 dstAdj = pow(dstClr, invgamma);"
#endif
         // linearly interpolate the three color values
    "    float3 result = lerp(dstAdj, srcAdj, glyphClr);"
         // gamma re-adjust the resulting color (alpha is always set to 1.0)
#if POW_LUT
    "    color = float4(tex3D(gammaTex, result).rgb, 1.0);"
#else
    "    color = float4(pow(result, gamma), 1.0);"
#endif
    "}";

static void
D3DShaderGen_GenerateLCDTextShader()
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DShaderGen_GenerateLCDTextShader");

    D3DShaderGen_WritePixelShader((char *)lcdTextShaderSource, "lcdtext", 0);
}

/*************************** AA support *******************************/

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
 * The shader uses the ddx() and ddy() functions to measure the "rate
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
static const char *aaShaderSource =
    "void main(in float2 tco : TEXCOORD0,"
    "          in float2 tci : TEXCOORD1,"
    "          inout float4 color : COLOR0)"
    "{"
    // Calculate the vectors for the "legs" of the pixel parallelogram
    // for the outer parallelogram.
    "    float2 oleg1 = ddx(tco);"
    "    float2 oleg2 = ddy(tco);"
    // Calculate the bounds of the distorted pixel parallelogram.
    "    float2 omin = min(tco, tco+oleg1);"
    "    omin = min(omin, tco+oleg2);"
    "    omin = min(omin, tco+oleg1+oleg2);"
    "    float2 omax = max(tco, tco+oleg1);"
    "    omax = max(omax, tco+oleg2);"
    "    omax = max(omax, tco+oleg1+oleg2);"
    // Calculate the vectors for the "legs" of the pixel parallelogram
    // for the inner parallelogram.
    "    float2 ileg1 = ddx(tci);"
    "    float2 ileg2 = ddy(tci);"
    // Calculate the bounds of the distorted pixel parallelogram.
    "    float2 imin = min(tci, tci+ileg1);"
    "    imin = min(imin, tci+ileg2);"
    "    imin = min(imin, tci+ileg1+ileg2);"
    "    float2 imax = max(tci, tci+ileg1);"
    "    imax = max(imax, tci+ileg2);"
    "    imax = max(imax, tci+ileg1+ileg2);"
    // Clamp the bounds of the parallelograms to the unit square to
    // estimate the intersection of the pixel parallelogram with
    // the unit square.  The ratio of the 2 rectangle areas is a
    // reasonable estimate of the proportion of coverage.
    "    float2 o1 = clamp(omin, 0.0, 1.0);"
    "    float2 o2 = clamp(omax, 0.0, 1.0);"
    "    float oint = (o2.y-o1.y)*(o2.x-o1.x);"
    "    float oarea = (omax.y-omin.y)*(omax.x-omin.x);"
    "    float2 i1 = clamp(imin, 0.0, 1.0);"
    "    float2 i2 = clamp(imax, 0.0, 1.0);"
    "    float iint = (i2.y-i1.y)*(i2.x-i1.x);"
    "    float iarea = (imax.y-imin.y)*(imax.x-imin.x);"
    // Proportion of pixel in outer shape minus the proportion
    // of pixel in the inner shape == the coverage of the pixel
    // in the area between the two.
    "    float coverage = oint/oarea - iint / iarea;"
    "    color *= coverage;"
    "}";

static void
D3DShaderGen_GenerateAAParallelogramShader()
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DShaderGen_GenerateAAParallelogramShader");

    D3DShaderGen_WriteShader((char *)aaShaderSource, "ps_2_a", "aapgram", 0);
}

/**************************** Main entrypoint *******************************/

static void
D3DShaderGen_GenerateAllShaders()
{
    int i;

#if 1
    // Generate BufferedImageOp shaders
    for (i = 0; i < MAX_RESCALE; i++) {
        D3DShaderGen_GenerateRescaleShader(i);
    }
    D3DShaderGen_WriteShaderArray("rescale", MAX_RESCALE);
    for (i = 0; i < MAX_CONVOLVE; i++) {
        D3DShaderGen_GenerateConvolveShader(i);
    }
    D3DShaderGen_WriteShaderArray("convolve", MAX_CONVOLVE);
    for (i = 0; i < MAX_LOOKUP; i++) {
        D3DShaderGen_GenerateLookupShader(i);
    }
    D3DShaderGen_WriteShaderArray("lookup", MAX_LOOKUP);

    // Generate Paint shaders
    for (i = 0; i < MAX_BASIC_GRAD; i++) {
        D3DShaderGen_GenerateBasicGradShader(i);
    }
    D3DShaderGen_WriteShaderArray("grad", MAX_BASIC_GRAD);
    for (i = 0; i < MAX_MULTI_GRAD; i++) {
        if (EXTRACT_CYCLE_METHOD(i) == 3) continue; // REMIND
        D3DShaderGen_GenerateLinearGradShader(i);
    }
    D3DShaderGen_WriteShaderArray("linear", MAX_MULTI_GRAD);
    for (i = 0; i < MAX_MULTI_GRAD; i++) {
        if (EXTRACT_CYCLE_METHOD(i) == 3) continue; // REMIND
        D3DShaderGen_GenerateRadialGradShader(i);
    }
    D3DShaderGen_WriteShaderArray("radial", MAX_MULTI_GRAD);

    // Generate LCD text shader
    D3DShaderGen_GenerateLCDTextShader();

    // Genereate Shader to fill Antialiased parallelograms
    D3DShaderGen_GenerateAAParallelogramShader();
#else
    /*
    for (i = 0; i < MAX_RESCALE; i++) {
        D3DShaderGen_GenerateRescaleShader(i);
    }
    D3DShaderGen_WriteShaderArray("rescale", MAX_RESCALE);
    */
    //D3DShaderGen_GenerateConvolveShader(2);
    //D3DShaderGen_GenerateLCDTextShader();
    //D3DShaderGen_GenerateLinearGradShader(16);
    D3DShaderGen_GenerateBasicGradShader(0);
#endif
}

int
main(int argc, char **argv)
{
    fpHeader = fopen(strHeaderFile, "a");

    D3DShaderGen_GenerateAllShaders();

    fclose(fpHeader);

    return 0;
}
