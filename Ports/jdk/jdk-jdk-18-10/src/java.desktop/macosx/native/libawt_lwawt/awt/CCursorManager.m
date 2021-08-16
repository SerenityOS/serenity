/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

#include "sun_lwawt_macosx_CCursorManager.h"

#include <Cocoa/Cocoa.h>

#include "GeomUtilities.h"
#include "ThreadUtilities.h"

#include "java_awt_Cursor.h"


static SEL lookupCursorSelectorForType(jint type) {
    switch (type) {
        case java_awt_Cursor_DEFAULT_CURSOR:        return @selector(arrowCursor);
        case java_awt_Cursor_CROSSHAIR_CURSOR:      return @selector(crosshairCursor);
        case java_awt_Cursor_TEXT_CURSOR:           return @selector(IBeamCursor);
        case java_awt_Cursor_WAIT_CURSOR:           return @selector(javaBusyButClickableCursor);
        case java_awt_Cursor_SW_RESIZE_CURSOR:      return @selector(javaResizeSWCursor);
        case java_awt_Cursor_SE_RESIZE_CURSOR:      return @selector(javaResizeSECursor);
        case java_awt_Cursor_NW_RESIZE_CURSOR:      return @selector(javaResizeNWCursor);
        case java_awt_Cursor_NE_RESIZE_CURSOR:      return @selector(javaResizeNECursor);
        case java_awt_Cursor_N_RESIZE_CURSOR:       return @selector(resizeUpDownCursor);
        case java_awt_Cursor_S_RESIZE_CURSOR:       return @selector(resizeUpDownCursor);
        case java_awt_Cursor_W_RESIZE_CURSOR:       return @selector(resizeLeftRightCursor);
        case java_awt_Cursor_E_RESIZE_CURSOR:       return @selector(resizeLeftRightCursor);
        case java_awt_Cursor_HAND_CURSOR:           return @selector(pointingHandCursor);
        case java_awt_Cursor_MOVE_CURSOR:           return @selector(javaMoveCursor);
    }

    return nil;
}

static SEL lookupCursorSelectorForName(NSString *name) {
    if ([@"DnD.Cursor.CopyDrop" isEqual:name]) return @selector(dragCopyCursor);
    if ([@"DnD.Cursor.LinkDrop" isEqual:name]) return @selector(dragLinkCursor);
    if ([@"DnD.Cursor.MoveDrop" isEqual:name]) return @selector(_genericDragCursor);
    if ([@"DnD.Cursor.CopyNoDrop" isEqual:name]) return @selector(operationNotAllowedCursor);
    if ([@"DnD.Cursor.LinkNoDrop" isEqual:name]) return @selector(operationNotAllowedCursor);
    if ([@"DnD.Cursor.MoveNoDrop" isEqual:name]) return @selector(operationNotAllowedCursor);
    return nil;
}

static void setCursorOnAppKitThread(NSCursor *cursor) {
    [cursor set];
}

JNIEXPORT void JNICALL
Java_sun_lwawt_macosx_CCursorManager_nativeSetBuiltInCursor
(JNIEnv *env, jclass class, jint type, jstring name)
{
JNI_COCOA_ENTER(env);

    NSString *cursorName = JavaStringToNSString(env, name);
    SEL cursorSelector = (type == sun_lwawt_macosx_CCursorManager_NAMED_CURSOR) ? lookupCursorSelectorForName(cursorName) : lookupCursorSelectorForType(type);
    if (cursorSelector == nil) {
        NSString *reason = [NSString stringWithFormat:@"unimplemented built-in cursor type: %d / %@", type, cursorName];
        JNU_ThrowIllegalArgumentException(env, [reason UTF8String]);
        return;
    }

    if (![[NSCursor class] respondsToSelector:cursorSelector]) {
        JNU_ThrowByName(env, "java/lang/NoSuchMethodException", "missing NSCursor selector");
        return;
    }

    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
        setCursorOnAppKitThread([[NSCursor class] performSelector:cursorSelector]);
    }];

JNI_COCOA_EXIT(env);
}

JNIEXPORT void JNICALL
Java_sun_lwawt_macosx_CCursorManager_nativeSetCustomCursor
(JNIEnv *env, jclass class, jlong imgPtr, jdouble x, jdouble y)
{
JNI_COCOA_ENTER(env);
    NSImage *image = (NSImage *)jlong_to_ptr(imgPtr);

    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
        NSCursor *cursor = [[NSCursor alloc] initWithImage:image
                                                   hotSpot:(NSPoint){ x, y }];
        setCursorOnAppKitThread(cursor);
        [cursor release];
    }];

JNI_COCOA_EXIT(env);
}

JNIEXPORT jobject JNICALL
Java_sun_lwawt_macosx_CCursorManager_nativeGetCursorPosition
(JNIEnv *env, jclass class)
{
    jobject jpt = NULL;

JNI_COCOA_ENTER(env);

    CGEventRef event = CGEventCreate(NULL);
    CGPoint globalPos = CGEventGetLocation(event);
    CFRelease(event);

    jpt = NSToJavaPoint(env, globalPos);

JNI_COCOA_EXIT(env);

    return jpt;
}


JNIEXPORT void JNICALL
Java_sun_lwawt_macosx_CCursorManager_nativeSetAllowsCursorSetInBackground
(JNIEnv *env, jclass class, jboolean allows)
{
JNI_COCOA_ENTER(env);

    SEL allowsSetInBackground_SEL = @selector(javaSetAllowsCursorSetInBackground:);
    if ([[NSCursor class] respondsToSelector:allowsSetInBackground_SEL]) {
        [ThreadUtilities performOnMainThreadWaiting:YES block:^(){
            NSMethodSignature *allowsSetInBackground_sig =
                [[NSCursor class] methodSignatureForSelector:allowsSetInBackground_SEL];
            NSInvocation *invocation =
                [NSInvocation invocationWithMethodSignature:allowsSetInBackground_sig];
            BOOL arg = (BOOL)allows;
            [invocation setSelector:allowsSetInBackground_SEL];
            [invocation setArgument:&arg atIndex:2];
            [invocation invokeWithTarget:[NSCursor class]];
        }];
    }

JNI_COCOA_EXIT(env);

}
