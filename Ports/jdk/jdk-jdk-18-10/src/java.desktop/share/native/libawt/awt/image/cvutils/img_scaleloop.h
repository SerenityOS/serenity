/*
 * Copyright (c) 1996, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This file contains the skeleton code for generating functions to
 * convert image data for the Java AWT.  Nearly everything below is
 * a call to a macro that is defined in one of the header files
 * included in this directory.  A description of the various macro
 * packages available for customizing this skeleton and how to use
 * this file to construct specific conversion functions is available
 * in the README file that should also be included in this directory.
 */

ImgConvertFcn NAME;

int NAME(struct Hjava_awt_image_ColorModel *colormodel,
         int srcOX, int srcOY, int srcW, int srcH,
         void *srcpix, int srcOff, int srcBPP, int srcScan,
         int srcTotalWidth, int srcTotalHeight,
         int dstTotalWidth, int dstTotalHeight,
         ImgConvertData *cvdata, ImgColorData *clrdata)
{
    DeclareScaleVars
    DeclareInputVars
    DeclareDecodeVars
    DeclareAlphaVars
    DeclareDitherVars
    DeclareOutputVars
    unsigned int pixel;
    int red, green, blue;
    IfAlpha(int alpha;)

    InitInput(srcBPP);
    InitScale(srcpix, srcOff, srcScan,
              srcOX, srcOY, srcW, srcH,
              srcTotalWidth, srcTotalHeight,
              dstTotalWidth, dstTotalHeight);
    InitOutput(cvdata, clrdata, DSTX1, DSTY1);
    InitAlpha(cvdata, DSTY1, DSTX1, DSTX2);

    InitPixelDecode(colormodel);
    InitDither(cvdata, clrdata, dstTotalWidth);

    RowLoop(srcOY) {
        RowSetup(srcTotalHeight, dstTotalHeight,
                 srcTotalWidth, dstTotalWidth,
                 srcOY, srcpix, srcOff, srcScan);
        StartDitherLine(cvdata, DSTX1, DSTY);
        StartAlphaRow(cvdata, DSTX1, DSTY);
        ColLoop(srcOX) {
            ColSetup(srcTotalWidth, dstTotalWidth, pixel);
            PixelDecode(colormodel, pixel, red, green, blue, alpha);
            ApplyAlpha(cvdata, DSTX, DSTY, alpha);
            DitherPixel(DSTX, DSTY, pixel, red, green, blue);
            PutPixelInc(pixel, red, green, blue);
        }
        EndMaskLine();
        EndOutputRow(cvdata, DSTY, DSTX1, DSTX2);
        RowEnd(srcTotalHeight, dstTotalHeight, srcW, srcScan);
    }
    DitherBufComplete(cvdata, DSTX1);
    BufComplete(cvdata, DSTX1, DSTY1, DSTX2, DSTY2);
    return SCALESUCCESS;
}
