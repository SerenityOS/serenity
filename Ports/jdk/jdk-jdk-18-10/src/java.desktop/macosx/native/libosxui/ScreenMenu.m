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

#import "ScreenMenu.h"

#import "com_apple_laf_ScreenMenu.h"
#import "java_awt_Event.h"
#import "java_awt_event_KeyEvent.h"
#import "java_awt_event_InputEvent.h"
#import "java_awt_event_MouseEvent.h"

#import <JavaRuntimeSupport/JavaRuntimeSupport.h>

#import "ThreadUtilities.h"
#import "CMenuBar.h"
#import "JNIUtilities.h"

static jclass sjc_ScreenMenu = NULL;
#define GET_SCREENMENU_CLASS() \
     GET_CLASS(sjc_ScreenMenu, "com/apple/laf/ScreenMenu");

static jint ns2awtModifiers(NSUInteger keyMods) {
    jint result = 0;
    if (keyMods & NSShiftKeyMask)        result |= java_awt_Event_SHIFT_MASK;
    if (keyMods & NSControlKeyMask)        result |= java_awt_Event_CTRL_MASK;
    if (keyMods & NSAlternateKeyMask)    result |= java_awt_Event_ALT_MASK;
    if (keyMods & NSCommandKeyMask)        result |= java_awt_Event_META_MASK;
    return result;
}

static jint ns2awtMouseButton(NSInteger mouseButton) {
    switch (mouseButton) {
        case 1: return java_awt_event_InputEvent_BUTTON1_MASK;
        case 2: return java_awt_event_InputEvent_BUTTON2_MASK;
        case 3: return java_awt_event_InputEvent_BUTTON3_MASK;
    }
    return 0;
}


@interface NativeToJavaDelegate : NSObject <JRSMenuDelegate, NSMenuDelegate>
{
@public
    NSMenu *nsmenu;
    jobject javaObject;
}

@property (nonatomic, retain) NSMenu *nsmenu;
@property (nonatomic) jobject javaObject;

- (id)initFromMenu:(NSMenu *)menu javaObj:(jobject)obj;
- (NSMenu*)menu;
@end


@implementation NativeToJavaDelegate

@synthesize nsmenu;
@synthesize javaObject;

- (id)initFromMenu:(NSMenu *)aMenu javaObj:(jobject)obj
{
    self = [super init];
    if (self) {
        self.nsmenu = aMenu;
        self.javaObject = obj;
    }
    return self;
}

- (NSMenu *)menu {
    return self.nsmenu;
}

- (void)menuWillOpen:(NSMenu *)menu
{
    if (self.javaObject == nil) {
#ifdef DEBUG
        NSLog(@"_javaObject is NULL: (%s - %s : %d)", __FILE__, __FUNCTION__, __LINE__);
#endif
        return;
    }

    JNIEnv *env = [ThreadUtilities getJNIEnv];
JNI_COCOA_ENTER(env);
    //NSLog(@"menuWillOpen %@", [menu title]);
    GET_SCREENMENU_CLASS();
    DECLARE_METHOD(jm_ScreenMenu_invokeOpenLater, sjc_ScreenMenu, "invokeOpenLater", "()V");
    (*env)->CallVoidMethod(env, self.javaObject, jm_ScreenMenu_invokeOpenLater);
    CHECK_EXCEPTION();
JNI_COCOA_EXIT(env);

}

- (void)menuDidClose:(NSMenu *)menu
{
    if (self.javaObject == nil) {
#ifdef DEBUG
        NSLog(@"_javaObject is NULL: (%s - %s : %d)", __FILE__, __FUNCTION__, __LINE__);
#endif
        return;
    }

    JNIEnv *env = [ThreadUtilities getJNIEnv];
JNI_COCOA_ENTER(env);
    //NSLog(@"menuDidClose %@", [menu title]);
    GET_SCREENMENU_CLASS();
    DECLARE_METHOD(jm_ScreenMenu_invokeMenuClosing, sjc_ScreenMenu, "invokeMenuClosing", "()V");
    (*env)->CallVoidMethod(env, self.javaObject, jm_ScreenMenu_invokeMenuClosing);
    CHECK_EXCEPTION();
JNI_COCOA_EXIT(env);
}


- (void)handleJavaMenuItemTargetedAtIndex:(NSUInteger)menuIndex rect:(NSRect)rect
{
    if (self.javaObject== nil) {
#ifdef DEBUG
        NSLog(@"_javaObject is NULL: (%s - %s : %d)", __FILE__, __FUNCTION__, __LINE__);
#endif
        return;
    }

    JNIEnv *env = [ThreadUtilities getJNIEnv];
JNI_COCOA_ENTER(env);
    // Send that to Java so we can test which item was hit.
    GET_SCREENMENU_CLASS();
    DECLARE_METHOD(jm_ScreenMenu_updateSelectedItem, sjc_ScreenMenu, "handleItemTargeted", "(IIIII)V");
    (*env)->CallVoidMethod(env, self.javaObject, jm_ScreenMenu_updateSelectedItem, menuIndex,
                    NSMinY(rect), NSMinX(rect), NSMaxY(rect), NSMaxX(rect));
    CHECK_EXCEPTION();

JNI_COCOA_EXIT(env);
}

/*
 * The input is an NSTimeInterval (a double representing seconds and fractions of seconds)
 * 0.0 means midnight Jan 1, 2001.
 * The output is a Java long representing time in milliseconds since midnight Jan 1st 1970.
 * There is a Cocoa constant representing that difference : NSTimeIntervalSince1970
 */
static jlong NSTimeIntervalToJavaMilliseconds(NSTimeInterval interval) {
    NSTimeInterval interval1970 = interval + NSTimeIntervalSince1970;
    return (jlong)(interval1970 * 1000);
}

// Called from event handler callback
- (void)handleJavaMouseEvent:(NSEvent *)event
{
    NSInteger kind = [event type];
    jint javaKind = 0;

    switch (kind) {
        case NSLeftMouseUp: case NSRightMouseUp: case NSOtherMouseUp:
            javaKind = java_awt_event_MouseEvent_MOUSE_RELEASED;
            break;
        case NSLeftMouseDown: case NSRightMouseDown: case NSOtherMouseDown:
            javaKind = java_awt_event_MouseEvent_MOUSE_PRESSED;
            break;
        case NSMouseMoved:
            javaKind = java_awt_event_MouseEvent_MOUSE_MOVED;
            break;
        case NSLeftMouseDragged: case NSRightMouseDragged: case NSOtherMouseDragged:
            javaKind = java_awt_event_MouseEvent_MOUSE_DRAGGED;
            break;
    }

    // Get the coordinates of the mouse in global coordinates (must be global, since our tracking rects are global.)
    NSPoint globalPoint = [event locationInWindow];
    jint javaX = globalPoint.x;
    jint javaY = globalPoint.y;

    // Convert the event modifiers into Java modifiers
    jint javaModifiers = ns2awtModifiers([event modifierFlags]) | ns2awtMouseButton([event buttonNumber]);

    // Get the event time
    jlong javaWhen = NSTimeIntervalToJavaMilliseconds([event timestamp]);

    // Call the mouse event handler, which will generate Java mouse events.
    JNIEnv *env = [ThreadUtilities getJNIEnv];
JNI_COCOA_ENTER(env);
    GET_SCREENMENU_CLASS();
    DECLARE_METHOD(jm_ScreenMenu_handleMouseEvent, sjc_ScreenMenu, "handleMouseEvent", "(IIIIJ)V");
    (*env)->CallVoidMethod(env, self.javaObject, jm_ScreenMenu_handleMouseEvent,
             javaKind, javaX, javaY, javaModifiers, javaWhen);
    CHECK_EXCEPTION();
JNI_COCOA_EXIT(env);
}

@end


/*
 * Class:     com_apple_laf_ScreenMenu
 * Method:    addMenuListeners
 * Signature: (Lcom/apple/laf/ScreenMenu;J[J)V
 */
JNIEXPORT jlong JNICALL Java_com_apple_laf_ScreenMenu_addMenuListeners
(JNIEnv *env, jclass clz, jobject listener, jlong nativeMenu)
{
    NativeToJavaDelegate *delegate = nil;

JNI_COCOA_ENTER(env);

    jobject listenerRef = (*env)->NewGlobalRef(env, listener);
    NSMenu *menu = jlong_to_ptr(nativeMenu);

    delegate = [[[NativeToJavaDelegate alloc] initFromMenu:menu javaObj:listenerRef] autorelease];
    CFRetain(delegate); // GC

    [ThreadUtilities performOnMainThreadWaiting:YES block:^{
        NSMenu *menu = delegate.nsmenu;
        if ([menu isJavaMenu]) {
            [menu setDelegate:delegate];
            [menu setJavaMenuDelegate:delegate];
        }
    }];

JNI_COCOA_EXIT(env);

    return ptr_to_jlong(delegate);
}

/*
 * Class:     com_apple_laf_ScreenMenu
 * Method:    removeMenuListeners
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_apple_laf_ScreenMenu_removeMenuListeners
(JNIEnv *env, jclass clz, jlong fModelPtr)
{
    if (fModelPtr == 0L) return;

JNI_COCOA_ENTER(env);

    NativeToJavaDelegate *delegate = (NativeToJavaDelegate *)jlong_to_ptr(fModelPtr);

    [ThreadUtilities performOnMainThreadWaiting:YES block:^{
        NSMenu *menu = delegate.nsmenu;
        [menu setJavaMenuDelegate:nil];
        [menu setDelegate:nil];
        delegate.nsmenu = nil;
    }];

    (*env)->DeleteGlobalRef(env, delegate.javaObject);
    delegate.javaObject = nil;

    CFRelease(delegate); // GC

JNI_COCOA_EXIT(env);
}
