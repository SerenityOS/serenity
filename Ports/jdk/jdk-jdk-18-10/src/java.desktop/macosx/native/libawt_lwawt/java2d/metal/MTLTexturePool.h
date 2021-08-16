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

#ifndef MTLTexturePool_h_Included
#define MTLTexturePool_h_Included
#include <time.h>
#import "MTLUtils.h"

@class MTLPoolCell;

@interface MTLTexturePoolItem : NSObject
@property (readwrite, retain) id<MTLTexture> texture;
@property (readwrite) bool isBusy;
@property (readwrite) time_t lastUsed;
@property (readwrite) bool isMultiSample;
@property (readwrite, assign) MTLTexturePoolItem* prev;
@property (readwrite, retain) MTLTexturePoolItem* next;
@property (readwrite, assign) MTLPoolCell* cell;

- (id) initWithTexture:(id<MTLTexture>)tex cell:(MTLPoolCell*)cell;
@end

@interface MTLPooledTextureHandle : NSObject
@property (readonly, assign) id<MTLTexture> texture;
@property (readonly) MTLRegion rect;
- (void) releaseTexture;
@end

// NOTE: owns all MTLTexture objects
@interface MTLTexturePool : NSObject
@property (readwrite, retain) id<MTLDevice> device;

- (id) initWithDevice:(id<MTLDevice>)device;
- (MTLPooledTextureHandle *) getTexture:(int)width height:(int)height format:(MTLPixelFormat)format;
- (MTLPooledTextureHandle *) getTexture:(int)width height:(int)height format:(MTLPixelFormat)format
                          isMultiSample:(bool)isMultiSample;
@end

@interface MTLPoolCell : NSObject
@property (readwrite, retain) MTLTexturePoolItem* available;
@property (readwrite, assign) MTLTexturePoolItem* availableTail;
@property (readwrite, retain) MTLTexturePoolItem* occupied;
- (MTLTexturePoolItem *)createItem:(id<MTLDevice>)dev
                             width:(int)width
                            height:(int)height
                            format:(MTLPixelFormat)format
                     isMultiSample:(bool)isMultiSample;
- (NSUInteger)cleanIfBefore:(time_t)lastUsedTimeToRemove;
- (void)releaseItem:(MTLTexturePoolItem *)item;
@end

#endif /* MTLTexturePool_h_Included */
