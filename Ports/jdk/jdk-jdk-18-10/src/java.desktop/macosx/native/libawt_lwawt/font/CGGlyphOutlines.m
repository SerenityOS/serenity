/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

#import "CGGlyphOutlines.h"

static void
AWTPathGetMoreSpaceIfNecessary(AWTPathRef path)
{
    while ((path->fAllocatedSegmentTypeSpace - path->fNumberOfSegments) < 1) {
        size_t growth = sizeof(jbyte)*path->fAllocatedSegmentTypeSpace*kStorageSizeChangeOnGetMoreFactor;
        path->fSegmentType = (jbyte*) realloc(path->fSegmentType, growth);
        path->fAllocatedSegmentTypeSpace *= kStorageSizeChangeOnGetMoreFactor;
    }

    while ((path->fAllocatedSegmentDataSpace - path->fNumberOfDataElements) < 7) {
        size_t growth = sizeof(jfloat)*path->fAllocatedSegmentDataSpace*kStorageSizeChangeOnGetMoreFactor;
        path->fSegmentData = (jfloat*) realloc(path->fSegmentData, growth);
        path->fAllocatedSegmentDataSpace *= kStorageSizeChangeOnGetMoreFactor;
    }
}

static void
AWTPathMoveTo(void* data, CGPoint p)
{
    CGFloat x = p.x;
    CGFloat y = p.y;

    AWTPathRef path = (AWTPathRef)data;
    CGFloat tx    = path->fTranslate.width;
    CGFloat ty    = path->fTranslate.height;
    CGFloat pathX =  x+tx;
    CGFloat pathY = -y+ty;

#ifdef AWT_GV_DEBUG
    fprintf(stderr, "eMoveTo \n");
    fprintf(stderr, "    tx=%f, ty=%f\n", tx, ty);
    fprintf(stderr, "    x=%f, y=%f\n", x, y);
    fprintf(stderr, "    pathX=%f, pathY=%f\n", pathX, pathY);
#endif

    AWTPathGetMoreSpaceIfNecessary(path);

    path->fSegmentType[path->fNumberOfSegments++] = (jbyte)eMoveTo;

    path->fSegmentData[path->fNumberOfDataElements++] = pathX;
    path->fSegmentData[path->fNumberOfDataElements++] = pathY;
}

static void
AWTPathLineTo(void* data, CGPoint p)
{
    CGFloat x = p.x;
    CGFloat y = p.y;

    AWTPathRef path = (AWTPathRef)data;
    CGFloat tx    = path->fTranslate.width;
    CGFloat ty    = path->fTranslate.height;
    CGFloat pathX =  x+tx;
    CGFloat pathY = -y+ty;

#ifdef AWT_GV_DEBUG
    fprintf(stderr, "eLineTo \n");
    fprintf(stderr, "    tx=%f, ty=%f\n", tx, ty);
    fprintf(stderr, "    x=%f, y=%f\n", x, y);
    fprintf(stderr, "    pathX=%f, pathY=%f\n", pathX, pathY);
#endif

    AWTPathGetMoreSpaceIfNecessary(path);

    path->fSegmentType[path->fNumberOfSegments++] = (jbyte)eLineTo;

    path->fSegmentData[path->fNumberOfDataElements++] = pathX;
    path->fSegmentData[path->fNumberOfDataElements++] = pathY;
}

static void
AWTPathQuadTo(void* data, CGPoint p1, CGPoint p2)
{
    CGFloat x1 = p1.x;
    CGFloat y1 = p1.y;
    CGFloat x2 = p2.x;
    CGFloat y2 = p2.y;

    AWTPathRef path = (AWTPathRef)data;
    CGFloat tx     = path->fTranslate.width;
    CGFloat ty     = path->fTranslate.height;
    CGFloat pathX1 =  x1+tx;
    CGFloat pathY1 = -y1+ty;
    CGFloat pathX2 =  x2+tx;
    CGFloat pathY2 = -y2+ty;

#ifdef AWT_GV_DEBUG
    fprintf(stderr, "eQuadTo \n");
    fprintf(stderr, "    tx=%f, ty=%f\n", tx, ty);
    fprintf(stderr, "    x1=%f, y1=%f\n", x1, y1);
    fprintf(stderr, "    x2=%f, y2=%f\n", x2, y2);
    fprintf(stderr, "    pathX1=%f, path1Y=%f\n", pathX1, pathY1);
    fprintf(stderr, "    pathX2=%f, pathY2=%f\n", pathX2, pathY2);
#endif

    AWTPathGetMoreSpaceIfNecessary(path);

    path->fSegmentType[path->fNumberOfSegments++] = (jbyte)eQuadTo;

    path->fSegmentData[path->fNumberOfDataElements++] = pathX1;
    path->fSegmentData[path->fNumberOfDataElements++] = pathY1;
    path->fSegmentData[path->fNumberOfDataElements++] = pathX2;
    path->fSegmentData[path->fNumberOfDataElements++] = pathY2;
}

static void
AWTPathCubicTo(void* data, CGPoint p1, CGPoint p2, CGPoint p3)
{
    CGFloat x1 = p1.x;
    CGFloat y1 = p1.y;
    CGFloat x2 = p2.x;
    CGFloat y2 = p2.y;
    CGFloat x3 = p3.x;
    CGFloat y3 = p3.y;

    AWTPathRef path = (AWTPathRef)data;
    CGFloat tx     = path->fTranslate.width;
    CGFloat ty     = path->fTranslate.height;
    CGFloat pathX1 =  x1+tx;
    CGFloat pathY1 = -y1+ty;
    CGFloat pathX2 =  x2+tx;
    CGFloat pathY2 = -y2+ty;
    CGFloat pathX3 =  x3+tx;
    CGFloat pathY3 = -y3+ty;

#ifdef AWT_GV_DEBUG
    fprintf(stderr, "eCubicTo \n");
    fprintf(stderr, "    tx=%f, ty=%f\n", tx, ty);
    fprintf(stderr, "    x1=%f, y1=%f\n", x1, y1);
    fprintf(stderr, "    x2=%f, y2=%f\n", x2, y2);
    fprintf(stderr, "    x3=%f, y3=%f\n", x3, y3);
    fprintf(stderr, "    pathX1=%f, path1Y=%f\n", pathX1, pathY1);
    fprintf(stderr, "    pathX2=%f, pathY2=%f\n", pathX2, pathY2);
    fprintf(stderr, "    pathX3=%f, pathY3=%f\n", pathX3, pathY3);
#endif

    AWTPathGetMoreSpaceIfNecessary(path);

    path->fSegmentType[path->fNumberOfSegments++] = (jbyte)eCubicTo;

    path->fSegmentData[path->fNumberOfDataElements++] = pathX1;
    path->fSegmentData[path->fNumberOfDataElements++] = pathY1;
    path->fSegmentData[path->fNumberOfDataElements++] = pathX2;
    path->fSegmentData[path->fNumberOfDataElements++] = pathY2;
    path->fSegmentData[path->fNumberOfDataElements++] = pathX3;
    path->fSegmentData[path->fNumberOfDataElements++] = pathY3;
}

static void
AWTPathClose(void* data)
{
#ifdef AWT_GV_DEBUG
    fprintf(stderr, "GVGlyphPathCallBackClosePath \n");
#endif

    AWTPathRef path = (AWTPathRef) data;
    AWTPathGetMoreSpaceIfNecessary(path);

    path->fSegmentType[path->fNumberOfSegments++] = (jbyte)eClosePath;
}

AWTPathRef
AWTPathCreate(CGSize translate)
{
#ifdef AWT_GV_DEBUG
    fprintf(stderr, "AWTPathCreate \n");
    fprintf(stderr, "    translate.width=%f \n", translate.width);
    fprintf(stderr, "    translate.height=%f \n", translate.height);
#endif

    AWTPathRef path = (AWTPathRef) malloc(sizeof(AWTPath));
    path->fTranslate    = translate;
    path->fSegmentData  = (jfloat*)malloc(sizeof(jfloat) * kInitialAllocatedPathSegments);
    path->fSegmentType  = (jbyte*)malloc(sizeof(jbyte) * kInitialAllocatedPathSegments);
    path->fNumberOfDataElements = 0;
    path->fNumberOfSegments = 0;
    path->fAllocatedSegmentTypeSpace = kInitialAllocatedPathSegments;
    path->fAllocatedSegmentDataSpace = kInitialAllocatedPathSegments;

    return path;
}

void
AWTPathFree(AWTPathRef pathRef)
{
#ifdef AWT_GV_DEBUG
    fprintf(stderr, "--B--AWTPathFree\n");
    fprintf(stderr, "pathRef->fSegmentData (%p)\n",pathRef->fSegmentData);
#endif

    free(pathRef->fSegmentData);
    //fprintf(stderr, "pathRef->fSegmentType (%d)\n",pathRef->fSegmentType);
    free(pathRef->fSegmentType);
    //fprintf(stderr, "pathRef (%d)\n", pathRef);
    free(pathRef);
    //fprintf(stderr, "--E--AWTPathFree\n");
}

static void
AWTPathApplierCallback(void *info, const CGPathElement *element)
{
    switch (element->type) {
    case kCGPathElementMoveToPoint:
        AWTPathMoveTo(info, element->points[0]);
        break;
    case kCGPathElementAddLineToPoint:
        AWTPathLineTo(info, element->points[0]);
        break;
    case kCGPathElementAddQuadCurveToPoint:
        AWTPathQuadTo(info, element->points[0], element->points[1]);
        break;
    case kCGPathElementAddCurveToPoint:
        AWTPathCubicTo(info, element->points[0],
                       element->points[1], element->points[2]);
        break;
    case kCGPathElementCloseSubpath:
        AWTPathClose(info);
        break;
    }
}

OSStatus
AWTGetGlyphOutline(CGGlyph *glyphs, NSFont *font,
                   CGSize *advanceArray, CGAffineTransform *tx,
                   UInt32 inStartIndex, size_t length,
                   AWTPathRef* outPath)
{
#ifdef AWT_GV_DEBUG
    fprintf(stderr, "AWTGetGlyphOutline\n");
    fprintf(stderr, "    inAffineTransform a=%f, b=%f, c=%f, d=%f, tx=%f, ty=%f \n", tx->a, tx->b, tx->c, tx->d, tx->tx, tx->ty);
#endif

    OSStatus status = noErr;

    if ( isnan(tx->a) || isnan(tx->b) || isnan(tx->c) ||
         isnan(tx->d) || isnan(tx->tx) || isnan(tx->ty)) {
        return status;
    }
    glyphs = glyphs + inStartIndex;
//    advanceArray = advanceArray + inStartIndex; // TODO(cpc): use advance

    CGPathRef cgPath = CTFontCreatePathForGlyph((CTFontRef)font, glyphs[0], tx);
    CGPathApply(cgPath, *outPath, AWTPathApplierCallback);
    CGPathRelease(cgPath);

    return status;
}
