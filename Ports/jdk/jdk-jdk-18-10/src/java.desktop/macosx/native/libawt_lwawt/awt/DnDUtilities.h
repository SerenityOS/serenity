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

#ifndef DnDUtilities_h
#define DnDUtilities_h

#import <Cocoa/Cocoa.h>
#include <jni.h>

@interface DnDUtilities : NSObject {
}

// Common methods:
+ (NSString *) javaPboardType;

// Dragging action mapping:
+ (jint)mapNSDragOperationToJava:(NSDragOperation)dragOperation;
+ (NSDragOperation)mapJavaDragOperationToNS:(jint)dragOperation;
+ (jint)mapNSDragOperationMaskToJava:(NSDragOperation)dragOperation;
+ (jint)narrowJavaDropActions:(jint)actions;

// Mouse and key modifiers mapping:
+ (NSUInteger)mapJavaExtModifiersToNSMouseDownButtons:(jint)modifiers;
+ (NSUInteger)mapJavaExtModifiersToNSMouseUpButtons:(jint)modifiers;

// Specialized key and mouse modifiers mapping (for operationChanged)
+ (jint)extractJavaExtKeyModifiersFromJavaExtModifiers:(jint)modifiers;
+ (jint)extractJavaExtMouseModifiersFromJavaExtModifiers:(jint)modifiers;

// Getting the state of the current Drag
+ (NSDragOperation)nsDragOperationForModifiers:(NSUInteger)modifiers;
+ (jint) javaKeyModifiersForNSDragOperation:(NSDragOperation)dragOp;
@end


// Global debugging flag (for drag-and-drop) - this can be overriden locally per file:
#ifndef DND_DEBUG
//    #define DND_DEBUG TRUE
#endif

#if DND_DEBUG
    // Turn DLog (debug log) on for debugging:
    #define    DLog(arg1)                        NSLog(arg1)
    #define    DLog2(arg1, arg2)                NSLog(arg1, arg2)
    #define    DLog3(arg1, arg2, arg3)            NSLog(arg1, arg2, arg3)
    #define    DLog4(arg1, arg2, arg3, arg4)    NSLog(arg1, arg2, arg3, arg4)
    #define    DLog5(arg1, arg2, arg3, arg4, arg5)            NSLog(arg1, arg2, arg3, arg4, arg5)
    #define    DLog6(arg1, arg2, arg3, arg4, arg5, arg6)    NSLog(arg1, arg2, arg3, arg4, arg5, arg6)
#else
    #define    DLog(arg1);
    #define    DLog2(arg1, arg2);
    #define    DLog3(arg1, arg2, arg3);
    #define    DLog4(arg1, arg2, arg3, arg4);
    #define    DLog5(arg1, arg2, arg3, arg4, arg5);
    #define    DLog6(arg1, arg2, arg3, arg4, arg5, arg6);
#endif

#endif // DnDUtilities_h
