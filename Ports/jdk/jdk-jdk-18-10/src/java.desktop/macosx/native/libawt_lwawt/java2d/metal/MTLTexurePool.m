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

#import "MTLTexturePool.h"
#import "Trace.h"

#define SCREEN_MEMORY_SIZE_5K (5120*4096*4) //~84 mb
#define MAX_POOL_ITEM_LIFETIME_SEC 30

#define CELL_WIDTH_BITS 5 // ~ 32 pixel
#define CELL_HEIGHT_BITS 5 // ~ 32 pixel

@implementation MTLTexturePoolItem

@synthesize texture, isBusy, lastUsed, isMultiSample, next, cell;

- (id) initWithTexture:(id<MTLTexture>)tex cell:(MTLPoolCell*)c{
    self = [super init];
    if (self == nil) return self;
    self.texture = tex;
    isBusy = NO;
    self.next = nil;
    self.prev = nil;
    self.cell = c;
    return self;
}

- (void) dealloc {
    [texture release];
    [super dealloc];
}

@end

@implementation MTLPooledTextureHandle
{
    MTLRegion _rect;
    id<MTLTexture> _texture;
    MTLTexturePoolItem * _poolItem;
}
@synthesize texture = _texture, rect = _rect;

- (id) initWithPoolItem:(id<MTLTexture>)texture rect:(MTLRegion)rectangle poolItem:(MTLTexturePoolItem *)poolItem {
    self = [super init];
    if (self == nil) return self;

    _rect = rectangle;
    _texture = texture;
    _poolItem = poolItem;
    return self;
}

- (void) releaseTexture {
    [_poolItem.cell releaseItem:_poolItem];
}

@end

@implementation MTLPoolCell {
    NSLock* _lock;
}
@synthesize available, availableTail, occupied;

- (instancetype)init {
    self = [super init];
    if (self) {
        self.available = nil;
        self.availableTail = nil;
        self.occupied = nil;
        _lock = [[NSLock alloc] init];
    }
    return self;
}

- (void)occupyItem:(MTLTexturePoolItem *)item {
    if (item.isBusy) return;
    [item retain];
    if (item.prev == nil) {
        self.available = item.next;
        if (item.next) {
            item.next.prev = nil;
        } else {
            self.availableTail = item.prev;
        }
    } else {
        item.prev.next = item.next;
        if (item.next) {
            item.next.prev = item.prev;
        } else {
            self.availableTail = item.prev;
        }
        item.prev = nil;
    }
    if (occupied) occupied.prev = item;
    item.next = occupied;
    self.occupied = item;
    [item release];
    item.isBusy = YES;
}

- (void)releaseItem:(MTLTexturePoolItem *)item {
    [_lock lock];
    @try {
        if (!item.isBusy) return;
        [item retain];
        if (item.prev == nil) {
            self.occupied = item.next;
            if (item.next) item.next.prev = nil;
        } else {
            item.prev.next = item.next;
            if (item.next) item.next.prev = item.prev;
            item.prev = nil;
        }
        if (self.available) {
            self.available.prev = item;
        } else {
            self.availableTail = item;
        }
        item.next = self.available;
        self.available = item;
        item.isBusy = NO;
        [item release];
    } @finally {
        [_lock unlock];
    }
}

- (void)addOccupiedItem:(MTLTexturePoolItem *)item {
    if (self.occupied) self.occupied.prev = item;
    item.next = self.occupied;
    item.isBusy = YES;
    self.occupied = item;
}

- (void)removeAvailableItem:(MTLTexturePoolItem*)item {
    [item retain];
    if (item.prev == nil) {
        self.available = item.next;
        if (item.next) {
            item.next.prev = nil;
            item.next = nil;
        } else {
            self.availableTail = item.prev;
        }
    } else {
        item.prev.next = item.next;
        if (item.next) {
            item.next.prev = item.prev;
            item.next = nil;
        } else {
            self.availableTail = item.prev;
        }
    }
    [item release];
}

- (void)removeAllItems {
    MTLTexturePoolItem *cur = self.available;
    while (cur != nil) {
        cur = cur.next;
        self.available = cur;
    }
    cur = self.occupied;
    while (cur != nil) {
        cur = cur.next;
        self.occupied = cur;
    }
    self.availableTail = nil;
}

- (MTLTexturePoolItem *)createItem:(id<MTLDevice>)dev
                             width:(int)width
                            height:(int)height
                            format:(MTLPixelFormat)format
                     isMultiSample:(bool)isMultiSample
{
    MTLTextureDescriptor *textureDescriptor =
            [MTLTextureDescriptor texture2DDescriptorWithPixelFormat:format
                                                               width:(NSUInteger) width
                                                              height:(NSUInteger) height
                                                           mipmapped:NO];
    textureDescriptor.usage = MTLTextureUsageRenderTarget |
        MTLTextureUsageShaderRead;
    if (isMultiSample) {
        textureDescriptor.textureType = MTLTextureType2DMultisample;
        textureDescriptor.sampleCount = MTLAASampleCount;
        textureDescriptor.storageMode = MTLStorageModePrivate;
    }

    id <MTLTexture> tex = (id <MTLTexture>) [[dev newTextureWithDescriptor:textureDescriptor] autorelease];
    MTLTexturePoolItem* item = [[[MTLTexturePoolItem alloc] initWithTexture:tex cell:self] autorelease];
    item.isMultiSample = isMultiSample;
    [_lock lock];
    @try {
        [self addOccupiedItem:item];
    } @finally {
        [_lock unlock];
    }
    return item;
}


- (NSUInteger)cleanIfBefore:(time_t)lastUsedTimeToRemove {
    NSUInteger deallocMem = 0;
    [_lock lock];
    MTLTexturePoolItem *cur = availableTail;
    @try {
        while (cur != nil) {
            MTLTexturePoolItem *prev = cur.prev;
            if (lastUsedTimeToRemove <= 0 ||
                cur.lastUsed < lastUsedTimeToRemove) {
#ifdef DEBUG
                J2dTraceImpl(J2D_TRACE_VERBOSE, JNI_TRUE,
                             "MTLTexturePool: remove pool item: tex=%p, w=%d h=%d, elapsed=%d",
                             cur.texture, cur.texture.width, cur.texture.height,
                             time(NULL) - cur.lastUsed);
#endif //DEBUG
                deallocMem += cur.texture.width * cur.texture.height * 4;
                [self removeAvailableItem:cur];
            } else {
                if (lastUsedTimeToRemove > 0) break;
            }
            cur = prev;
        }
    } @finally {
        [_lock unlock];
    }
    return deallocMem;
}

- (MTLTexturePoolItem *)occupyItem:(int)width height:(int)height format:(MTLPixelFormat)format
                     isMultiSample:(bool)isMultiSample {
    int minDeltaArea = -1;
    const int requestedPixels = width*height;
    MTLTexturePoolItem *minDeltaTpi = nil;
    [_lock lock];
    @try {
        for (MTLTexturePoolItem *cur = available; cur != nil; cur = cur.next) {
            if (cur.texture.pixelFormat != format
                || cur.isMultiSample != isMultiSample) { // TODO: use swizzle when formats are not equal
                continue;
            }
            if (cur.texture.width < width || cur.texture.height < height) {
                continue;
            }
            const int deltaArea = (const int) (cur.texture.width * cur.texture.height - requestedPixels);
            if (minDeltaArea < 0 || deltaArea < minDeltaArea) {
                minDeltaArea = deltaArea;
                minDeltaTpi = cur;
                if (deltaArea == 0) {
                    // found exact match in current cell
                    break;
                }
            }
        }

        if (minDeltaTpi) {
            [self occupyItem:minDeltaTpi];
        }
    } @finally {
        [_lock unlock];
    }
    return minDeltaTpi;
}

- (void) dealloc {
    [_lock lock];
    @try {
        [self removeAllItems];
    } @finally {
        [_lock unlock];
    }
    [_lock release];
    [super dealloc];
}

@end

@implementation MTLTexturePool {
    int _memoryTotalAllocated;

    void ** _cells;
    int _poolCellWidth;
    int _poolCellHeight;
    uint64_t _maxPoolMemory;
}

@synthesize device;

- (id) initWithDevice:(id<MTLDevice>)dev {
    self = [super init];
    if (self == nil) return self;

    _memoryTotalAllocated = 0;
    _poolCellWidth = 10;
    _poolCellHeight = 10;
    const int cellsCount = _poolCellWidth * _poolCellHeight;
    _cells = (void **)malloc(cellsCount * sizeof(void*));
    memset(_cells, 0, cellsCount * sizeof(void*));
    self.device = dev;

    // recommendedMaxWorkingSetSize typically greatly exceeds SCREEN_MEMORY_SIZE_5K constant.
    // It usually corresponds to the VRAM available to the graphics card
    _maxPoolMemory = self.device.recommendedMaxWorkingSetSize/2;

    // Set maximum to handle at least 5K screen size
    if (_maxPoolMemory < SCREEN_MEMORY_SIZE_5K) {
        _maxPoolMemory = SCREEN_MEMORY_SIZE_5K;
    }

    return self;
}

- (void) dealloc {
    for (int c = 0; c < _poolCellWidth * _poolCellHeight; ++c) {
        MTLPoolCell * cell = _cells[c];
        if (cell != NULL) {
            [cell release];
        }
    }
    free(_cells);
    [super dealloc];
}

// NOTE: called from RQ-thread (on blit operations)
- (MTLPooledTextureHandle *) getTexture:(int)width height:(int)height format:(MTLPixelFormat)format {
    return [self getTexture:width height:height format:format isMultiSample:NO];
}

// NOTE: called from RQ-thread (on blit operations)
- (MTLPooledTextureHandle *) getTexture:(int)width height:(int)height format:(MTLPixelFormat)format
                          isMultiSample:(bool)isMultiSample {
        // 1. clean pool if necessary
        const int requestedPixels = width*height;
        const int requestedBytes = requestedPixels*4;
        if (_memoryTotalAllocated + requestedBytes > _maxPoolMemory) {
            [self cleanIfNecessary:0]; // release all free textures
        } else if (_memoryTotalAllocated + requestedBytes > _maxPoolMemory/2) {
            [self cleanIfNecessary:MAX_POOL_ITEM_LIFETIME_SEC]; // release only old free textures
        }

        // 2. find free item
        const int cellX0 = width    >> CELL_WIDTH_BITS;
        const int cellY0 = height   >> CELL_HEIGHT_BITS;
        const int cellX1 = cellX0 + 1;
        const int cellY1 = cellY0 + 1;
        if (cellX1 > _poolCellWidth || cellY1 > _poolCellHeight) {
            const int newCellWidth = cellX1 <= _poolCellWidth ? _poolCellWidth : cellX1;
            const int newCellHeight = cellY1 <= _poolCellHeight ? _poolCellHeight : cellY1;
            const int newCellsCount = newCellWidth*newCellHeight;
#ifdef DEBUG
            J2dTraceLn2(J2D_TRACE_VERBOSE, "MTLTexturePool: resize: %d -> %d", _poolCellWidth * _poolCellHeight, newCellsCount);
#endif
            void ** newcells = malloc(newCellsCount*sizeof(void*));
            const int strideBytes = _poolCellWidth * sizeof(void*);
            for (int cy = 0; cy < _poolCellHeight; ++cy) {
                void ** dst = newcells + cy*newCellWidth;
                void ** src = _cells + cy * _poolCellWidth;
                memcpy(dst, src, strideBytes);
                if (newCellWidth > _poolCellWidth)
                    memset(dst + _poolCellWidth, 0, (newCellWidth - _poolCellWidth) * sizeof(void*));
            }
            if (newCellHeight > _poolCellHeight) {
                void ** dst = newcells + _poolCellHeight * newCellWidth;
                memset(dst, 0, (newCellHeight - _poolCellHeight) * newCellWidth * sizeof(void*));
            }
            free(_cells);
            _cells = newcells;
            _poolCellWidth = newCellWidth;
            _poolCellHeight = newCellHeight;
        }

        MTLTexturePoolItem * minDeltaTpi = nil;
        int minDeltaArea = -1;
        for (int cy = cellY0; cy < cellY1; ++cy) {
            for (int cx = cellX0; cx < cellX1; ++cx) {
                MTLPoolCell * cell = _cells[cy * _poolCellWidth + cx];
                if (cell != NULL) {
                    MTLTexturePoolItem* tpi = [cell occupyItem:width height:height
                                                        format:format isMultiSample:isMultiSample];
                    if (!tpi) continue;
                    const int deltaArea = (const int) (tpi.texture.width * tpi.texture.height - requestedPixels);
                    if (minDeltaArea < 0 || deltaArea < minDeltaArea) {
                        minDeltaArea = deltaArea;
                        minDeltaTpi = tpi;
                        if (deltaArea == 0) {
                            // found exact match in current cell
                            break;
                        }
                    }
                }
            }
            if (minDeltaTpi != nil) {
                break;
            }
        }

        if (minDeltaTpi == NULL) {
            MTLPoolCell* cell = _cells[cellY0 * _poolCellWidth + cellX0];
            if (cell == NULL) {
                cell = [[MTLPoolCell alloc] init];
                _cells[cellY0 * _poolCellWidth + cellX0] = cell;
            }
            minDeltaTpi = [cell createItem:device width:width height:height format:format isMultiSample:isMultiSample];
            _memoryTotalAllocated += requestedBytes;
            J2dTraceLn5(J2D_TRACE_VERBOSE, "MTLTexturePool: created pool item: tex=%p, w=%d h=%d, pf=%d | total memory = %d Kb", minDeltaTpi.texture, width, height, format, _memoryTotalAllocated/1024);
        }

        minDeltaTpi.isBusy = YES;
        minDeltaTpi.lastUsed = time(NULL);
        return [[[MTLPooledTextureHandle alloc] initWithPoolItem:minDeltaTpi.texture
                                                            rect:MTLRegionMake2D(0, 0,
                                                                                 minDeltaTpi.texture.width,
                                                                                 minDeltaTpi.texture.height)
                                                        poolItem:minDeltaTpi] autorelease];
}

- (void) cleanIfNecessary:(int)lastUsedTimeThreshold {
    time_t lastUsedTimeToRemove =
            lastUsedTimeThreshold > 0 ?
                time(NULL) - lastUsedTimeThreshold :
                lastUsedTimeThreshold;
    for (int cy = 0; cy < _poolCellHeight; ++cy) {
        for (int cx = 0; cx < _poolCellWidth; ++cx) {
            MTLPoolCell * cell = _cells[cy * _poolCellWidth + cx];
            if (cell != NULL) {
                _memoryTotalAllocated -= [cell cleanIfBefore:lastUsedTimeToRemove];
            }
        }
    }
}

@end
