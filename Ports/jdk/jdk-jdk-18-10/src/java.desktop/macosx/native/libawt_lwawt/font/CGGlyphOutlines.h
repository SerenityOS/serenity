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

#import <Foundation/Foundation.h>
#import <ApplicationServices/ApplicationServices.h>
#import <AppKit/NSFont.h>
#include <jni.h>

#define kStorageSizeChangeOnGetMoreFactor 2
#define kInitialAllocatedPathSegments 2048

typedef enum  {
    eMoveTo    = 0,
    eLineTo    = 1,
    eQuadTo    = 2,
    eCubicTo   = 3,
    eClosePath = 4
} AWTPathSegmentType;

typedef struct AWTPath {
    CGSize  fTranslate;
    UInt32  fNumberOfSegments;
    jfloat* fSegmentData;
    jbyte*  fSegmentType;
    UInt32  fNumberOfDataElements;
    UInt32  fAllocatedSegmentTypeSpace;
    UInt32  fAllocatedSegmentDataSpace;
} AWTPath, *AWTPathRef;

AWTPathRef AWTPathCreate(CGSize translate);
void AWTPathFree(AWTPathRef pathRef);
OSStatus AWTGetGlyphOutline(CGGlyph *glyphs, NSFont *font,
                            CGSize *advances,
                            CGAffineTransform *inAffineTransform,
                            UInt32 inStartIndex, size_t length,
                            AWTPathRef* outPath);
