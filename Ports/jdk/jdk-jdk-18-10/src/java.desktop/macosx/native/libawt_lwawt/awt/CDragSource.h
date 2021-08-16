/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CDragSource_h
#define CDragSource_h

#import <Cocoa/Cocoa.h>
#include <jni.h>

@class CDragSource;

@protocol CDragSourceHolder
- (void) setDragSource:(CDragSource *)source;
@end

@interface CDragSource : NSObject {
@private
    NSView<CDragSourceHolder>* fView;
    jobject            fComponent;
    jobject            fDragSourceContextPeer;

    jobject            fTransferable;
    jobject            fTriggerEvent;
    jlong            fTriggerEventTimeStamp;
    NSPoint            fDragPos;
    jint                fClickCount;
    jint                fModifiers;

    NSImage*        fDragImage;
    NSPoint            fDragImageOffset;

    jint                fSourceActions;
    jlongArray        fFormats;
    jobject            fFormatMap;

    jint                     fDragKeyModifiers;
    jint                     fDragMouseModifiers;
}

// Common methods:
- (id)        init:(jobject)jDragSourceContextPeer
         component:(jobject)jComponent
           control:(id)control
      transferable:(jobject)jTransferable
      triggerEvent:(jobject)jTrigger
          dragPosX:(jint)dragPosX
          dragPosY:(jint)dragPosY
         modifiers:(jint)extModifiers
        clickCount:(jint)clickCount
         timeStamp:(jlong)timeStamp
         dragImage:(jlong)nsDragImagePtr
  dragImageOffsetX:(jint)jDragImageOffsetX
  dragImageOffsetY:(jint)jDragImageOffsetY
     sourceActions:(jint)jSourceActions
           formats:(jlongArray)jFormats
         formatMap:(jobject)jFormatMap;

- (void)removeFromView:(JNIEnv *)env;

- (void)drag;

// dnd APIs (see AppKit/NSDragging.h, NSDraggingSource):
- (NSDragOperation)draggingSourceOperationMaskForLocal:(BOOL)flag;
- (void)draggedImage:(NSImage *)image beganAt:(NSPoint)screenPoint;
- (void)draggedImage:(NSImage *)image endedAt:(NSPoint)screenPoint operation:(NSDragOperation)operation;
- (void)draggedImage:(NSImage *)image movedTo:(NSPoint)screenPoint;
- (BOOL)ignoreModifierKeysWhileDragging;

@end

#endif // CDragSource_h
