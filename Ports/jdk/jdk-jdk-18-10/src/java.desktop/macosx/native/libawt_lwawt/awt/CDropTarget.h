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

#ifndef CDropTarget_h
#define CDropTarget_h

#import <AppKit/AppKit.h>
#import <jni.h>

@class ControlModel;

@class CDropTarget;

@protocol CDropTargetHolder
- (void) setDropTarget:(CDropTarget *)target;
@end

@interface CDropTarget : NSObject {
@private
    NSView<CDropTargetHolder>* fView;
    jobject            fComponent;
    jobject            fDropTarget;
    jobject            fDropTargetContextPeer;
}

+ (CDropTarget *) currentDropTarget;

// Common methods:
- (id)init:(jobject)dropTarget component:(jobject)jcomponent control:(id)control;
- (void)controlModelControlValid;
- (void)removeFromView:(JNIEnv *)env;

- (NSInteger)getDraggingSequenceNumber;
- (jobject)copyDraggingDataForFormat:(jlong)format;
- (void)javaDraggingEnded:(jlong)draggingSequenceNumber success:(BOOL)jsuccess action:(jint)jdropaction;

// dnd APIs (see AppKit/NSDragging.h, NSDraggingDestination):
- (NSDragOperation)draggingEntered:(id <NSDraggingInfo>)sender;
- (NSDragOperation)draggingUpdated:(id <NSDraggingInfo>)sender;
- (void)draggingExited:(id <NSDraggingInfo>)sender;
- (BOOL)prepareForDragOperation:(id <NSDraggingInfo>)sender;
- (BOOL)performDragOperation:(id <NSDraggingInfo>)sender;
- (void)concludeDragOperation:(id <NSDraggingInfo>)sender;
- (void)draggingEnded:(id <NSDraggingInfo>)sender;

- (jint)currentJavaActions;

@end

#endif // CDropTarget_h
