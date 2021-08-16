/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

#import <Cocoa/Cocoa.h>

#import "sun_lwawt_macosx_CPlatformWindow.h"
#import "com_apple_eawt_event_GestureHandler.h"
#import "com_apple_eawt_FullScreenHandler.h"
#import "ApplicationDelegate.h"

#import "AWTWindow.h"
#import "AWTView.h"
#import "GeomUtilities.h"
#import "ThreadUtilities.h"
#import "JNIUtilities.h"

#define MASK(KEY) \
    (sun_lwawt_macosx_CPlatformWindow_ ## KEY)

#define IS(BITS, KEY) \
    ((BITS & MASK(KEY)) != 0)

#define SET(BITS, KEY, VALUE) \
    BITS = VALUE ? BITS | MASK(KEY) : BITS & ~MASK(KEY)

static jclass jc_CPlatformWindow = NULL;
#define GET_CPLATFORM_WINDOW_CLASS() \
    GET_CLASS(jc_CPlatformWindow, "sun/lwawt/macosx/CPlatformWindow");

#define GET_CPLATFORM_WINDOW_CLASS_RETURN(ret) \
    GET_CLASS_RETURN(jc_CPlatformWindow, "sun/lwawt/macosx/CPlatformWindow", ret);

// Cocoa windowDidBecomeKey/windowDidResignKey notifications
// doesn't provide information about "opposite" window, so we
// have to do a bit of tracking. This variable points to a window
// which had been the key window just before a new key window
// was set. It would be nil if the new key window isn't an AWT
// window or the app currently has no key window.
static AWTWindow* lastKeyWindow = nil;

// This variable contains coordinates of a window's top left
// which was positioned via java.awt.Window.setLocationByPlatform.
// It would be NSZeroPoint if 'Location by Platform' is not used.
static NSPoint lastTopLeftPoint;

// --------------------------------------------------------------
// NSWindow/NSPanel descendants implementation
#define AWT_NS_WINDOW_IMPLEMENTATION                            \
- (id) initWithDelegate:(AWTWindow *)delegate                   \
              frameRect:(NSRect)contectRect                     \
              styleMask:(NSUInteger)styleMask                   \
            contentView:(NSView *)view                          \
{                                                               \
    self = [super initWithContentRect:contectRect               \
                            styleMask:styleMask                 \
                              backing:NSBackingStoreBuffered    \
                                defer:NO];                      \
                                                                \
    if (self == nil) return nil;                                \
                                                                \
    [self setDelegate:delegate];                                \
    [self setContentView:view];                                 \
    [self setInitialFirstResponder:view];                       \
    [self setReleasedWhenClosed:NO];                            \
    [self setPreservesContentDuringLiveResize:YES];             \
                                                                \
    return self;                                                \
}                                                               \
                                                                \
/* NSWindow overrides */                                        \
- (BOOL) canBecomeKeyWindow {                                   \
    return [(AWTWindow*)[self delegate] canBecomeKeyWindow];    \
}                                                               \
                                                                \
- (BOOL) canBecomeMainWindow {                                  \
    return [(AWTWindow*)[self delegate] canBecomeMainWindow];   \
}                                                               \
                                                                \
- (BOOL) worksWhenModal {                                       \
    return [(AWTWindow*)[self delegate] worksWhenModal];        \
}                                                               \
                                                                \
- (void)sendEvent:(NSEvent *)event {                            \
    [(AWTWindow*)[self delegate] sendEvent:event];              \
    [super sendEvent:event];                                    \
}

@implementation AWTWindow_Normal
AWT_NS_WINDOW_IMPLEMENTATION

// Gesture support
- (void)postGesture:(NSEvent *)event as:(jint)type a:(jdouble)a b:(jdouble)b {
    AWT_ASSERT_APPKIT_THREAD;

    JNIEnv *env = [ThreadUtilities getJNIEnv];
    jobject platformWindow = (*env)->NewLocalRef(env, ((AWTWindow *)self.delegate).javaPlatformWindow);
    if (platformWindow != NULL) {
        // extract the target AWT Window object out of the CPlatformWindow
        GET_CPLATFORM_WINDOW_CLASS();
        DECLARE_FIELD(jf_target, jc_CPlatformWindow, "target", "Ljava/awt/Window;");
        jobject awtWindow = (*env)->GetObjectField(env, platformWindow, jf_target);
        if (awtWindow != NULL) {
            // translate the point into Java coordinates
            NSPoint loc = [event locationInWindow];
            loc.y = [self frame].size.height - loc.y;

            // send up to the GestureHandler to recursively dispatch on the AWT event thread
            DECLARE_CLASS(jc_GestureHandler, "com/apple/eawt/event/GestureHandler");
            DECLARE_METHOD(sjm_handleGestureFromNative, jc_GestureHandler,
                            "handleGestureFromNative", "(Ljava/awt/Window;IDDDD)V");
            (*env)->CallStaticVoidMethod(env, jc_GestureHandler, sjm_handleGestureFromNative,
                               awtWindow, type, (jdouble)loc.x, (jdouble)loc.y, (jdouble)a, (jdouble)b);
            CHECK_EXCEPTION();
            (*env)->DeleteLocalRef(env, awtWindow);
        }
        (*env)->DeleteLocalRef(env, platformWindow);
    }
}

- (void)beginGestureWithEvent:(NSEvent *)event {
    [self postGesture:event
                   as:com_apple_eawt_event_GestureHandler_PHASE
                    a:-1.0
                    b:0.0];
}

- (void)endGestureWithEvent:(NSEvent *)event {
    [self postGesture:event
                   as:com_apple_eawt_event_GestureHandler_PHASE
                    a:1.0
                    b:0.0];
}

- (void)magnifyWithEvent:(NSEvent *)event {
    [self postGesture:event
                   as:com_apple_eawt_event_GestureHandler_MAGNIFY
                    a:[event magnification]
                    b:0.0];
}

- (void)rotateWithEvent:(NSEvent *)event {
    [self postGesture:event
                   as:com_apple_eawt_event_GestureHandler_ROTATE
                    a:[event rotation]
                    b:0.0];
}

- (void)swipeWithEvent:(NSEvent *)event {
    [self postGesture:event
                   as:com_apple_eawt_event_GestureHandler_SWIPE
                    a:[event deltaX]
                    b:[event deltaY]];
}

@end
@implementation AWTWindow_Panel
AWT_NS_WINDOW_IMPLEMENTATION
@end
// END of NSWindow/NSPanel descendants implementation
// --------------------------------------------------------------


@implementation AWTWindow

@synthesize nsWindow;
@synthesize javaPlatformWindow;
@synthesize javaMenuBar;
@synthesize javaMinSize;
@synthesize javaMaxSize;
@synthesize styleBits;
@synthesize isEnabled;
@synthesize ownerWindow;
@synthesize preFullScreenLevel;
@synthesize standardFrame;
@synthesize isMinimizing;
@synthesize keyNotificationRecd;

- (void) updateMinMaxSize:(BOOL)resizable {
    if (resizable) {
        [self.nsWindow setMinSize:self.javaMinSize];
        [self.nsWindow setMaxSize:self.javaMaxSize];
    } else {
        NSRect currentFrame = [self.nsWindow frame];
        [self.nsWindow setMinSize:currentFrame.size];
        [self.nsWindow setMaxSize:currentFrame.size];
    }
}

// creates a new NSWindow style mask based on the _STYLE_PROP_BITMASK bits
+ (NSUInteger) styleMaskForStyleBits:(jint)styleBits {
    NSUInteger type = 0;
    if (IS(styleBits, DECORATED)) {
        type |= NSTitledWindowMask;
        if (IS(styleBits, CLOSEABLE))            type |= NSClosableWindowMask;
        if (IS(styleBits, RESIZABLE))            type |= NSResizableWindowMask;
        if (IS(styleBits, FULL_WINDOW_CONTENT))  type |= NSFullSizeContentViewWindowMask;
    } else {
        type |= NSBorderlessWindowMask;
    }

    if (IS(styleBits, MINIMIZABLE))   type |= NSMiniaturizableWindowMask;
    if (IS(styleBits, TEXTURED))      type |= NSTexturedBackgroundWindowMask;
    if (IS(styleBits, UNIFIED))       type |= NSUnifiedTitleAndToolbarWindowMask;
    if (IS(styleBits, UTILITY))       type |= NSUtilityWindowMask;
    if (IS(styleBits, HUD))           type |= NSHUDWindowMask;
    if (IS(styleBits, SHEET))         type |= NSWindowStyleMaskDocModalWindow;
    if (IS(styleBits, NONACTIVATING)) type |= NSNonactivatingPanelMask;

    return type;
}

// updates _METHOD_PROP_BITMASK based properties on the window
- (void) setPropertiesForStyleBits:(jint)bits mask:(jint)mask {
    if (IS(mask, RESIZABLE)) {
        BOOL resizable = IS(bits, RESIZABLE);
        [self updateMinMaxSize:resizable];
        [self.nsWindow setShowsResizeIndicator:resizable];
        // Zoom button should be disabled, if the window is not resizable,
        // otherwise button should be restored to initial state.
        BOOL zoom = resizable && IS(bits, ZOOMABLE);
        [[self.nsWindow standardWindowButton:NSWindowZoomButton] setEnabled:zoom];
    }

    if (IS(mask, HAS_SHADOW)) {
        [self.nsWindow setHasShadow:IS(bits, HAS_SHADOW)];
    }

    if (IS(mask, ZOOMABLE)) {
        [[self.nsWindow standardWindowButton:NSWindowZoomButton] setEnabled:IS(bits, ZOOMABLE)];
    }

    if (IS(mask, ALWAYS_ON_TOP)) {
        [self.nsWindow setLevel:IS(bits, ALWAYS_ON_TOP) ? NSFloatingWindowLevel : NSNormalWindowLevel];
    }

    if (IS(mask, HIDES_ON_DEACTIVATE)) {
        [self.nsWindow setHidesOnDeactivate:IS(bits, HIDES_ON_DEACTIVATE)];
    }

    if (IS(mask, DRAGGABLE_BACKGROUND)) {
        [self.nsWindow setMovableByWindowBackground:IS(bits, DRAGGABLE_BACKGROUND)];
    }

    if (IS(mask, DOCUMENT_MODIFIED)) {
        [self.nsWindow setDocumentEdited:IS(bits, DOCUMENT_MODIFIED)];
    }

    if (IS(mask, FULLSCREENABLE) && [self.nsWindow respondsToSelector:@selector(toggleFullScreen:)]) {
        if (IS(bits, FULLSCREENABLE)) {
            [self.nsWindow setCollectionBehavior:(1 << 7) /*NSWindowCollectionBehaviorFullScreenPrimary*/];
        } else {
            [self.nsWindow setCollectionBehavior:NSWindowCollectionBehaviorDefault];
        }
    }

    if (IS(mask, TRANSPARENT_TITLE_BAR) && [self.nsWindow respondsToSelector:@selector(setTitlebarAppearsTransparent:)]) {
        [self.nsWindow setTitlebarAppearsTransparent:IS(bits, TRANSPARENT_TITLE_BAR)];
    }

    if (IS(mask, TITLE_VISIBLE) && [self.nsWindow respondsToSelector:@selector(setTitleVisibility:)]) {
        [self.nsWindow setTitleVisibility:(IS(bits, TITLE_VISIBLE)) ? NSWindowTitleVisible :NSWindowTitleHidden];
    }

}

- (id) initWithPlatformWindow:(jobject)platformWindow
                  ownerWindow:owner
                    styleBits:(jint)bits
                    frameRect:(NSRect)rect
                  contentView:(NSView *)view
{
AWT_ASSERT_APPKIT_THREAD;

    NSUInteger newBits = bits;
    if (IS(bits, SHEET) && owner == nil) {
        newBits = bits & ~NSWindowStyleMaskDocModalWindow;
    }
    NSUInteger styleMask = [AWTWindow styleMaskForStyleBits:newBits];

    NSRect contentRect = rect; //[NSWindow contentRectForFrameRect:rect styleMask:styleMask];
    if (contentRect.size.width <= 0.0) {
        contentRect.size.width = 1.0;
    }
    if (contentRect.size.height <= 0.0) {
        contentRect.size.height = 1.0;
    }

    self = [super init];

    if (self == nil) return nil; // no hope

    if (IS(bits, UTILITY) ||
        IS(bits, NONACTIVATING) ||
        IS(bits, HUD) ||
        IS(bits, HIDES_ON_DEACTIVATE) ||
        IS(bits, SHEET))
    {
        self.nsWindow = [[AWTWindow_Panel alloc] initWithDelegate:self
                            frameRect:contentRect
                            styleMask:styleMask
                          contentView:view];
    }
    else
    {
        // These windows will appear in the window list in the dock icon menu
        self.nsWindow = [[AWTWindow_Normal alloc] initWithDelegate:self
                            frameRect:contentRect
                            styleMask:styleMask
                          contentView:view];
    }

    if (self.nsWindow == nil) return nil; // no hope either
    [self.nsWindow release]; // the property retains the object already

    self.keyNotificationRecd = NO;
    self.isEnabled = YES;
    self.isMinimizing = NO;
    self.javaPlatformWindow = platformWindow;
    self.styleBits = bits;
    self.ownerWindow = owner;
    [self setPropertiesForStyleBits:styleBits mask:MASK(_METHOD_PROP_BITMASK)];

    if (IS(self.styleBits, IS_POPUP)) {
        [self.nsWindow setCollectionBehavior:(1 << 8) /*NSWindowCollectionBehaviorFullScreenAuxiliary*/];
    }

    if (IS(bits, SHEET) && owner != nil) {
        [self.nsWindow setStyleMask: NSWindowStyleMaskDocModalWindow];
    }

    return self;
}

+ (BOOL) isAWTWindow:(NSWindow *)window {
    return [window isKindOfClass: [AWTWindow_Panel class]] || [window isKindOfClass: [AWTWindow_Normal class]];
}

// Retrieves the list of possible window layers (levels)
+ (NSArray*) getWindowLayers {
    static NSArray *windowLayers;
    static dispatch_once_t token;

    // Initialize the list of possible window layers
    dispatch_once(&token, ^{
        // The layers are ordered from front to back, (i.e. the toppest one is the first)
        windowLayers = [NSArray arrayWithObjects:
                            [NSNumber numberWithInt:CGWindowLevelForKey(kCGPopUpMenuWindowLevelKey)],
                            [NSNumber numberWithInt:CGWindowLevelForKey(kCGFloatingWindowLevelKey)],
                            [NSNumber numberWithInt:CGWindowLevelForKey(kCGNormalWindowLevelKey)],
                            nil
                        ];
        [windowLayers retain];
    });
    return windowLayers;
}

// returns id for the topmost window under mouse
+ (NSInteger) getTopmostWindowUnderMouseID {
    NSInteger result = -1;

    NSArray *windowLayers = [AWTWindow getWindowLayers];
    // Looking for the window under mouse starting from the toppest layer
    for (NSNumber *layer in windowLayers) {
        result = [AWTWindow getTopmostWindowUnderMouseIDImpl:[layer integerValue]];
        if (result != -1) {
            break;
        }
    }
    return result;
}

+ (NSInteger) getTopmostWindowUnderMouseIDImpl:(NSInteger)windowLayer {
    NSInteger result = -1;

    NSRect screenRect = [[NSScreen mainScreen] frame];
    NSPoint nsMouseLocation = [NSEvent mouseLocation];
    CGPoint cgMouseLocation = CGPointMake(nsMouseLocation.x, screenRect.size.height - nsMouseLocation.y);

    NSMutableArray *windows = (NSMutableArray *)CGWindowListCopyWindowInfo(kCGWindowListOptionOnScreenOnly | kCGWindowListExcludeDesktopElements, kCGNullWindowID);

    for (NSDictionary *window in windows) {
        NSInteger layer = [[window objectForKey:(id)kCGWindowLayer] integerValue];
        if (layer == windowLayer) {
            CGRect rect;
            CGRectMakeWithDictionaryRepresentation((CFDictionaryRef)[window objectForKey:(id)kCGWindowBounds], &rect);
            if (CGRectContainsPoint(rect, cgMouseLocation)) {
                result = [[window objectForKey:(id)kCGWindowNumber] integerValue];
                break;
            }
        }
    }
    [windows release];
    return result;
}

// checks that this window is under the mouse cursor and this point is not overlapped by others windows
- (BOOL) isTopmostWindowUnderMouse {
    return [self.nsWindow windowNumber] == [AWTWindow getTopmostWindowUnderMouseID];
}

+ (AWTWindow *) getTopmostWindowUnderMouse {
    NSEnumerator *windowEnumerator = [[NSApp windows] objectEnumerator];
    NSWindow *window;

    NSInteger topmostWindowUnderMouseID = [AWTWindow getTopmostWindowUnderMouseID];

    while ((window = [windowEnumerator nextObject]) != nil) {
        if ([window windowNumber] == topmostWindowUnderMouseID) {
            BOOL isAWTWindow = [AWTWindow isAWTWindow: window];
            return isAWTWindow ? (AWTWindow *) [window delegate] : nil;
        }
    }
    return nil;
}

+ (void) synthesizeMouseEnteredExitedEvents:(NSWindow*)window withType:(NSEventType)eventType {

    NSPoint screenLocation = [NSEvent mouseLocation];
    NSPoint windowLocation = [window convertScreenToBase: screenLocation];
    int modifierFlags = (eventType == NSMouseEntered) ? NSMouseEnteredMask : NSMouseExitedMask;

    NSEvent *mouseEvent = [NSEvent enterExitEventWithType: eventType
                                                 location: windowLocation
                                            modifierFlags: modifierFlags
                                                timestamp: 0
                                             windowNumber: [window windowNumber]
                                                  context: nil
                                              eventNumber: 0
                                           trackingNumber: 0
                                                 userData: nil
                           ];

    [[window contentView] deliverJavaMouseEvent: mouseEvent];
}

+ (void) synthesizeMouseEnteredExitedEventsForAllWindows {

    NSInteger topmostWindowUnderMouseID = [AWTWindow getTopmostWindowUnderMouseID];
    NSArray *windows = [NSApp windows];
    NSWindow *window;

    NSEnumerator *windowEnumerator = [windows objectEnumerator];
    while ((window = [windowEnumerator nextObject]) != nil) {
        if ([AWTWindow isAWTWindow: window]) {
            BOOL isUnderMouse = ([window windowNumber] == topmostWindowUnderMouseID);
            BOOL mouseIsOver = [[window contentView] mouseIsOver];
            if (isUnderMouse && !mouseIsOver) {
                [AWTWindow synthesizeMouseEnteredExitedEvents:window withType:NSMouseEntered];
            } else if (!isUnderMouse && mouseIsOver) {
                [AWTWindow synthesizeMouseEnteredExitedEvents:window withType:NSMouseExited];
            }
        }
    }
}

+ (NSNumber *) getNSWindowDisplayID_AppKitThread:(NSWindow *)window {
    AWT_ASSERT_APPKIT_THREAD;
    NSScreen *screen = [window screen];
    NSDictionary *deviceDescription = [screen deviceDescription];
    return [deviceDescription objectForKey:@"NSScreenNumber"];
}

- (void) dealloc {
AWT_ASSERT_APPKIT_THREAD;

    JNIEnv *env = [ThreadUtilities getJNIEnvUncached];
    (*env)->DeleteWeakGlobalRef(env, self.javaPlatformWindow);
    self.javaPlatformWindow = nil;
    self.nsWindow = nil;
    self.ownerWindow = nil;
    [super dealloc];
}

// Tests whether window is blocked by modal dialog/window
- (BOOL) isBlocked {
    BOOL isBlocked = NO;

    JNIEnv *env = [ThreadUtilities getJNIEnv];
    jobject platformWindow = (*env)->NewLocalRef(env, self.javaPlatformWindow);
    if (platformWindow != NULL) {
        GET_CPLATFORM_WINDOW_CLASS_RETURN(isBlocked);
        DECLARE_METHOD_RETURN(jm_isBlocked, jc_CPlatformWindow, "isBlocked", "()Z", isBlocked);
        isBlocked = (*env)->CallBooleanMethod(env, platformWindow, jm_isBlocked) == JNI_TRUE ? YES : NO;
        CHECK_EXCEPTION();
        (*env)->DeleteLocalRef(env, platformWindow);
    }

    return isBlocked;
}

// Test whether window is simple window and owned by embedded frame
- (BOOL) isSimpleWindowOwnedByEmbeddedFrame {
    BOOL isSimpleWindowOwnedByEmbeddedFrame = NO;

    JNIEnv *env = [ThreadUtilities getJNIEnv];
    jobject platformWindow = (*env)->NewLocalRef(env, self.javaPlatformWindow);
    if (platformWindow != NULL) {
        GET_CPLATFORM_WINDOW_CLASS_RETURN(NO);
        DECLARE_METHOD_RETURN(jm_isBlocked, jc_CPlatformWindow, "isSimpleWindowOwnedByEmbeddedFrame", "()Z", NO);
        isSimpleWindowOwnedByEmbeddedFrame = (*env)->CallBooleanMethod(env, platformWindow, jm_isBlocked) == JNI_TRUE ? YES : NO;
        CHECK_EXCEPTION();
        (*env)->DeleteLocalRef(env, platformWindow);
    }

    return isSimpleWindowOwnedByEmbeddedFrame;
}

// Tests whether the corresponding Java platform window is visible or not
+ (BOOL) isJavaPlatformWindowVisible:(NSWindow *)window {
    BOOL isVisible = NO;

    if ([AWTWindow isAWTWindow:window] && [window delegate] != nil) {
        AWTWindow *awtWindow = (AWTWindow *)[window delegate];
        [AWTToolkit eventCountPlusPlus];

        JNIEnv *env = [ThreadUtilities getJNIEnv];
        jobject platformWindow = (*env)->NewLocalRef(env, awtWindow.javaPlatformWindow);
        if (platformWindow != NULL) {
            GET_CPLATFORM_WINDOW_CLASS_RETURN(isVisible);
            DECLARE_METHOD_RETURN(jm_isVisible, jc_CPlatformWindow, "isVisible", "()Z", isVisible)
            isVisible = (*env)->CallBooleanMethod(env, platformWindow, jm_isVisible) == JNI_TRUE ? YES : NO;
            CHECK_EXCEPTION();
            (*env)->DeleteLocalRef(env, platformWindow);

        }
    }
    return isVisible;
}

// Orders window's childs based on the current focus state
- (void) orderChildWindows:(BOOL)focus {
AWT_ASSERT_APPKIT_THREAD;

    if (self.isMinimizing || [self isBlocked]) {
        // Do not perform any ordering, if iconify is in progress
        // or the window is blocked by a modal window
        return;
    }

    NSEnumerator *windowEnumerator = [[NSApp windows]objectEnumerator];
    NSWindow *window;
    while ((window = [windowEnumerator nextObject]) != nil) {
        if ([AWTWindow isJavaPlatformWindowVisible:window]) {
            AWTWindow *awtWindow = (AWTWindow *)[window delegate];
            AWTWindow *owner = awtWindow.ownerWindow;
            if (IS(awtWindow.styleBits, ALWAYS_ON_TOP)) {
                // Do not order 'always on top' windows
                continue;
            }
            while (awtWindow.ownerWindow != nil) {
                if (awtWindow.ownerWindow == self) {
                    if (focus) {
                        // Move the childWindow to floating level
                        // so it will appear in front of its
                        // parent which owns the focus
                        [window setLevel:NSFloatingWindowLevel];
                    } else {
                        // Focus owner has changed, move the childWindow
                        // back to normal window level
                        [window setLevel:NSNormalWindowLevel];
                    }
                    // The childWindow should be displayed in front of
                    // its nearest parentWindow
                    [window orderWindow:NSWindowAbove relativeTo:[owner.nsWindow windowNumber]];
                    break;
                }
                awtWindow = awtWindow.ownerWindow;
            }
        }
    }
}

// NSWindow overrides
- (BOOL) canBecomeKeyWindow {
AWT_ASSERT_APPKIT_THREAD;
    return self.isEnabled && (IS(self.styleBits, SHOULD_BECOME_KEY) || [self isSimpleWindowOwnedByEmbeddedFrame]);
}

- (BOOL) canBecomeMainWindow {
AWT_ASSERT_APPKIT_THREAD;
    if (!self.isEnabled) {
        // Native system can bring up the NSWindow to
        // the top even if the window is not main.
        // We should bring up the modal dialog manually
        [AWTToolkit eventCountPlusPlus];

        JNIEnv *env = [ThreadUtilities getJNIEnv];
        jobject platformWindow = (*env)->NewLocalRef(env, self.javaPlatformWindow);
        if (platformWindow != NULL) {
            GET_CPLATFORM_WINDOW_CLASS_RETURN(NO);
            DECLARE_METHOD_RETURN(jm_checkBlockingAndOrder, jc_CPlatformWindow, "checkBlockingAndOrder", "()Z", NO);
            (*env)->CallBooleanMethod(env, platformWindow, jm_checkBlockingAndOrder);
            CHECK_EXCEPTION();
            (*env)->DeleteLocalRef(env, platformWindow);
        }
    }

    return self.isEnabled && IS(self.styleBits, SHOULD_BECOME_MAIN);
}

- (BOOL) worksWhenModal {
AWT_ASSERT_APPKIT_THREAD;
    return IS(self.styleBits, MODAL_EXCLUDED);
}


// NSWindowDelegate methods

- (void) _deliverMoveResizeEvent {
AWT_ASSERT_APPKIT_THREAD;

    // deliver the event if this is a user-initiated live resize or as a side-effect
    // of a Java initiated resize, because AppKit can override the bounds and force
    // the bounds of the window to avoid the Dock or remain on screen.
    [AWTToolkit eventCountPlusPlus];
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    jobject platformWindow = (*env)->NewLocalRef(env, self.javaPlatformWindow);
    if (platformWindow == NULL) {
        // TODO: create generic AWT assert
    }

    NSRect frame = ConvertNSScreenRect(env, [self.nsWindow frame]);

    GET_CPLATFORM_WINDOW_CLASS();
    DECLARE_METHOD(jm_deliverMoveResizeEvent, jc_CPlatformWindow, "deliverMoveResizeEvent", "(IIIIZ)V");
    (*env)->CallVoidMethod(env, platformWindow, jm_deliverMoveResizeEvent,
                      (jint)frame.origin.x,
                      (jint)frame.origin.y,
                      (jint)frame.size.width,
                      (jint)frame.size.height,
                      (jboolean)[self.nsWindow inLiveResize]);
    CHECK_EXCEPTION();
    (*env)->DeleteLocalRef(env, platformWindow);

    [AWTWindow synthesizeMouseEnteredExitedEventsForAllWindows];
}

- (void)windowDidMove:(NSNotification *)notification {
AWT_ASSERT_APPKIT_THREAD;

    [self _deliverMoveResizeEvent];
}

- (void)windowDidResize:(NSNotification *)notification {
AWT_ASSERT_APPKIT_THREAD;

    [self _deliverMoveResizeEvent];
}

- (void)windowDidExpose:(NSNotification *)notification {
AWT_ASSERT_APPKIT_THREAD;

    [AWTToolkit eventCountPlusPlus];
    // TODO: don't see this callback invoked anytime so we track
    // window exposing in _setVisible:(BOOL)
}

- (NSRect)windowWillUseStandardFrame:(NSWindow *)window
                        defaultFrame:(NSRect)newFrame {

    return NSEqualSizes(NSZeroSize, [self standardFrame].size)
                ? newFrame
                : [self standardFrame];
}

// Hides/shows window's childs during iconify/de-iconify operation
- (void) iconifyChildWindows:(BOOL)iconify {
AWT_ASSERT_APPKIT_THREAD;

    NSEnumerator *windowEnumerator = [[NSApp windows]objectEnumerator];
    NSWindow *window;
    while ((window = [windowEnumerator nextObject]) != nil) {
        if ([AWTWindow isJavaPlatformWindowVisible:window]) {
            AWTWindow *awtWindow = (AWTWindow *)[window delegate];
            while (awtWindow.ownerWindow != nil) {
                if (awtWindow.ownerWindow == self) {
                    if (iconify) {
                        [window orderOut:window];
                    } else {
                        [window orderFront:window];
                    }
                    break;
                }
                awtWindow = awtWindow.ownerWindow;
            }
        }
    }
}

- (void) _deliverIconify:(BOOL)iconify {
AWT_ASSERT_APPKIT_THREAD;

    [AWTToolkit eventCountPlusPlus];
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    jobject platformWindow = (*env)->NewLocalRef(env, self.javaPlatformWindow);
    if (platformWindow != NULL) {
        GET_CPLATFORM_WINDOW_CLASS();
        DECLARE_METHOD(jm_deliverIconify, jc_CPlatformWindow, "deliverIconify", "(Z)V");
        (*env)->CallVoidMethod(env, platformWindow, jm_deliverIconify, iconify);
        CHECK_EXCEPTION();
        (*env)->DeleteLocalRef(env, platformWindow);
    }
}

- (void)windowWillMiniaturize:(NSNotification *)notification {
AWT_ASSERT_APPKIT_THREAD;

    self.isMinimizing = YES;

    JNIEnv *env = [ThreadUtilities getJNIEnv];
    jobject platformWindow = (*env)->NewLocalRef(env, self.javaPlatformWindow);
    if (platformWindow != NULL) {
        GET_CPLATFORM_WINDOW_CLASS();
        DECLARE_METHOD(jm_windowWillMiniaturize, jc_CPlatformWindow, "windowWillMiniaturize", "()V");
        (*env)->CallVoidMethod(env, platformWindow, jm_windowWillMiniaturize);
        CHECK_EXCEPTION();
        (*env)->DeleteLocalRef(env, platformWindow);
    }
    // Explicitly make myself a key window to avoid possible
    // negative visual effects during iconify operation
    [self.nsWindow makeKeyAndOrderFront:self.nsWindow];
    [self iconifyChildWindows:YES];
}

- (void)windowDidMiniaturize:(NSNotification *)notification {
AWT_ASSERT_APPKIT_THREAD;

    [self _deliverIconify:JNI_TRUE];
    self.isMinimizing = NO;
}

- (void)windowDidDeminiaturize:(NSNotification *)notification {
AWT_ASSERT_APPKIT_THREAD;

    [self _deliverIconify:JNI_FALSE];
    [self iconifyChildWindows:NO];
}

- (void) _deliverWindowFocusEvent:(BOOL)focused oppositeWindow:(AWTWindow *)opposite {
//AWT_ASSERT_APPKIT_THREAD;
    JNIEnv *env = [ThreadUtilities getJNIEnvUncached];
    jobject platformWindow = (*env)->NewLocalRef(env, self.javaPlatformWindow);
    if (platformWindow != NULL) {
        jobject oppositeWindow = (*env)->NewLocalRef(env, opposite.javaPlatformWindow);
        GET_CPLATFORM_WINDOW_CLASS();
        DECLARE_METHOD(jm_deliverWindowFocusEvent, jc_CPlatformWindow, "deliverWindowFocusEvent", "(ZLsun/lwawt/macosx/CPlatformWindow;)V");
        (*env)->CallVoidMethod(env, platformWindow, jm_deliverWindowFocusEvent, (jboolean)focused, oppositeWindow);
        CHECK_EXCEPTION();
        (*env)->DeleteLocalRef(env, platformWindow);
        (*env)->DeleteLocalRef(env, oppositeWindow);
    }
}

- (void) windowDidBecomeMain: (NSNotification *) notification {
AWT_ASSERT_APPKIT_THREAD;
    [AWTToolkit eventCountPlusPlus];
#ifdef DEBUG
    NSLog(@"became main: %d %@ %@ %d", [self.nsWindow isKeyWindow], [self.nsWindow title], [self menuBarForWindow], self.keyNotificationRecd);
#endif

    // if for some reason, no KEY notification is received but this main window is also a key window
    // then we need to execute the KEY notification functionality.
    if(self.keyNotificationRecd != YES && [self.nsWindow isKeyWindow]) {
        [self doWindowDidBecomeKey];
    }
    self.keyNotificationRecd = NO;

    if (![self.nsWindow isKeyWindow]) {
        [self activateWindowMenuBar];
    }

    JNIEnv *env = [ThreadUtilities getJNIEnv];
    jobject platformWindow = (*env)->NewLocalRef(env, self.javaPlatformWindow);
    if (platformWindow != NULL) {
        GET_CPLATFORM_WINDOW_CLASS();
        DECLARE_METHOD(jm_windowDidBecomeMain, jc_CPlatformWindow, "windowDidBecomeMain", "()V");
        (*env)->CallVoidMethod(env, platformWindow, jm_windowDidBecomeMain);
        CHECK_EXCEPTION();
        (*env)->DeleteLocalRef(env, platformWindow);
    }
}

- (void) windowDidBecomeKey: (NSNotification *) notification {
AWT_ASSERT_APPKIT_THREAD;
    [AWTToolkit eventCountPlusPlus];
#ifdef DEBUG
    NSLog(@"became key: %d %@ %@", [self.nsWindow isMainWindow], [self.nsWindow title], [self menuBarForWindow]);
#endif
    [self doWindowDidBecomeKey];
    self.keyNotificationRecd = YES;
}

- (void) doWindowDidBecomeKey {
AWT_ASSERT_APPKIT_THREAD;
    AWTWindow *opposite = [AWTWindow lastKeyWindow];

    if (![self.nsWindow isMainWindow]) {
        [self activateWindowMenuBar];
    }

    [AWTWindow setLastKeyWindow:nil];

    [self _deliverWindowFocusEvent:YES oppositeWindow: opposite];
    [self orderChildWindows:YES];
}

- (void) activateWindowMenuBar {
AWT_ASSERT_APPKIT_THREAD;
    // Finds appropriate menubar in our hierarchy
    AWTWindow *awtWindow = self;
    while (awtWindow.ownerWindow != nil) {
        awtWindow = awtWindow.ownerWindow;
    }

    CMenuBar *menuBar = nil;
    BOOL isDisabled = NO;
    if ([awtWindow.nsWindow isVisible]){
        menuBar = awtWindow.javaMenuBar;
        isDisabled = !awtWindow.isEnabled;
    }

    if (menuBar == nil) {
        menuBar = [[ApplicationDelegate sharedDelegate] defaultMenuBar];
        isDisabled = NO;
    }

    [CMenuBar activate:menuBar modallyDisabled:isDisabled];
}

#ifdef DEBUG
- (CMenuBar *) menuBarForWindow {
AWT_ASSERT_APPKIT_THREAD;
    AWTWindow *awtWindow = self;
    while (awtWindow.ownerWindow != nil) {
        awtWindow = awtWindow.ownerWindow;
    }
    return awtWindow.javaMenuBar;
}
#endif

- (void) windowDidResignKey: (NSNotification *) notification {
    // TODO: check why sometimes at start is invoked *not* on AppKit main thread.
AWT_ASSERT_APPKIT_THREAD;
    [AWTToolkit eventCountPlusPlus];
#ifdef DEBUG
    NSLog(@"resigned key: %d %@ %@", [self.nsWindow isMainWindow], [self.nsWindow title], [self menuBarForWindow]);
#endif
    if (![self.nsWindow isMainWindow]) {
        [self deactivateWindow];
    }
}

- (void) windowDidResignMain: (NSNotification *) notification {
AWT_ASSERT_APPKIT_THREAD;
    [AWTToolkit eventCountPlusPlus];
#ifdef DEBUG
    NSLog(@"resigned main: %d %@ %@", [self.nsWindow isKeyWindow], [self.nsWindow title], [self menuBarForWindow]);
#endif
    if (![self.nsWindow isKeyWindow]) {
        [self deactivateWindow];
    }
}

- (void) deactivateWindow {
AWT_ASSERT_APPKIT_THREAD;
#ifdef DEBUG
    NSLog(@"deactivating window: %@", [self.nsWindow title]);
#endif
    [self.javaMenuBar deactivate];

    // the new key window
    NSWindow *keyWindow = [NSApp keyWindow];
    AWTWindow *opposite = nil;
    if ([AWTWindow isAWTWindow: keyWindow]) {
        opposite = (AWTWindow *)[keyWindow delegate];
        [AWTWindow setLastKeyWindow: self];
    } else {
        [AWTWindow setLastKeyWindow: nil];
    }

    [self _deliverWindowFocusEvent:NO oppositeWindow: opposite];
    [self orderChildWindows:NO];
}

- (BOOL)windowShouldClose:(id)sender {
AWT_ASSERT_APPKIT_THREAD;
    [AWTToolkit eventCountPlusPlus];
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    jobject platformWindow = (*env)->NewLocalRef(env, self.javaPlatformWindow);
    if (platformWindow != NULL) {
        GET_CPLATFORM_WINDOW_CLASS_RETURN(NO);
        DECLARE_METHOD_RETURN(jm_deliverWindowClosingEvent, jc_CPlatformWindow, "deliverWindowClosingEvent", "()V", NO);
        (*env)->CallVoidMethod(env, platformWindow, jm_deliverWindowClosingEvent);
        CHECK_EXCEPTION();
        (*env)->DeleteLocalRef(env, platformWindow);
    }
    // The window will be closed (if allowed) as result of sending Java event
    return NO;
}

- (void)_notifyFullScreenOp:(jint)op withEnv:(JNIEnv *)env {
    DECLARE_CLASS(jc_FullScreenHandler, "com/apple/eawt/FullScreenHandler");
    DECLARE_STATIC_METHOD(jm_notifyFullScreenOperation, jc_FullScreenHandler,
                           "handleFullScreenEventFromNative", "(Ljava/awt/Window;I)V");
    GET_CPLATFORM_WINDOW_CLASS();
    DECLARE_FIELD(jf_target, jc_CPlatformWindow, "target", "Ljava/awt/Window;");
    jobject platformWindow = (*env)->NewLocalRef(env, self.javaPlatformWindow);
    if (platformWindow != NULL) {
        jobject awtWindow = (*env)->GetObjectField(env, platformWindow, jf_target);
        if (awtWindow != NULL) {
            (*env)->CallStaticVoidMethod(env, jc_FullScreenHandler, jm_notifyFullScreenOperation, awtWindow, op);
            CHECK_EXCEPTION();
            (*env)->DeleteLocalRef(env, awtWindow);
        }
        (*env)->DeleteLocalRef(env, platformWindow);
    }
}


- (void)windowWillEnterFullScreen:(NSNotification *)notification {
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    GET_CPLATFORM_WINDOW_CLASS();
    DECLARE_METHOD(jm_windowWillEnterFullScreen, jc_CPlatformWindow, "windowWillEnterFullScreen", "()V");
    jobject platformWindow = (*env)->NewLocalRef(env, self.javaPlatformWindow);
    if (platformWindow != NULL) {
        (*env)->CallVoidMethod(env, platformWindow, jm_windowWillEnterFullScreen);
        CHECK_EXCEPTION();
        [self _notifyFullScreenOp:com_apple_eawt_FullScreenHandler_FULLSCREEN_WILL_ENTER withEnv:env];
        (*env)->DeleteLocalRef(env, platformWindow);
    }
}

- (void)windowDidEnterFullScreen:(NSNotification *)notification {
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    GET_CPLATFORM_WINDOW_CLASS();
    DECLARE_METHOD(jm_windowDidEnterFullScreen, jc_CPlatformWindow, "windowDidEnterFullScreen", "()V");
    jobject platformWindow = (*env)->NewLocalRef(env, self.javaPlatformWindow);
    if (platformWindow != NULL) {
        (*env)->CallVoidMethod(env, platformWindow, jm_windowDidEnterFullScreen);
        CHECK_EXCEPTION();
        [self _notifyFullScreenOp:com_apple_eawt_FullScreenHandler_FULLSCREEN_DID_ENTER withEnv:env];
        (*env)->DeleteLocalRef(env, platformWindow);
    }
    [AWTWindow synthesizeMouseEnteredExitedEventsForAllWindows];
}

- (void)windowWillExitFullScreen:(NSNotification *)notification {
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    GET_CPLATFORM_WINDOW_CLASS();
    DECLARE_METHOD(jm_windowWillExitFullScreen, jc_CPlatformWindow, "windowWillExitFullScreen", "()V");
    if (jm_windowWillExitFullScreen == NULL) {
        GET_CPLATFORM_WINDOW_CLASS();
        jm_windowWillExitFullScreen = (*env)->GetMethodID(env, jc_CPlatformWindow, "windowWillExitFullScreen", "()V");
    }
    CHECK_NULL(jm_windowWillExitFullScreen);
    jobject platformWindow = (*env)->NewLocalRef(env, self.javaPlatformWindow);
    if (platformWindow != NULL) {
        (*env)->CallVoidMethod(env, platformWindow, jm_windowWillExitFullScreen);
        CHECK_EXCEPTION();
        [self _notifyFullScreenOp:com_apple_eawt_FullScreenHandler_FULLSCREEN_WILL_EXIT withEnv:env];
        (*env)->DeleteLocalRef(env, platformWindow);
    }
}

- (void)windowDidExitFullScreen:(NSNotification *)notification {
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    jobject platformWindow = (*env)->NewLocalRef(env, self.javaPlatformWindow);
    if (platformWindow != NULL) {
        GET_CPLATFORM_WINDOW_CLASS();
        DECLARE_METHOD(jm_windowDidExitFullScreen, jc_CPlatformWindow, "windowDidExitFullScreen", "()V");
        (*env)->CallVoidMethod(env, platformWindow, jm_windowDidExitFullScreen);
        CHECK_EXCEPTION();
        [self _notifyFullScreenOp:com_apple_eawt_FullScreenHandler_FULLSCREEN_DID_EXIT withEnv:env];
        (*env)->DeleteLocalRef(env, platformWindow);
    }
    [AWTWindow synthesizeMouseEnteredExitedEventsForAllWindows];
}

- (void)sendEvent:(NSEvent *)event {
        if ([event type] == NSLeftMouseDown || [event type] == NSRightMouseDown || [event type] == NSOtherMouseDown) {
            if ([self isBlocked]) {
                // Move parent windows to front and make sure that a child window is displayed
                // in front of its nearest parent.
                if (self.ownerWindow != nil) {
                    JNIEnv *env = [ThreadUtilities getJNIEnvUncached];
                    jobject platformWindow = (*env)->NewLocalRef(env, self.javaPlatformWindow);
                    if (platformWindow != NULL) {
                        GET_CPLATFORM_WINDOW_CLASS();
                        DECLARE_METHOD(jm_orderAboveSiblings, jc_CPlatformWindow, "orderAboveSiblings", "()V");
                        (*env)->CallVoidMethod(env,platformWindow, jm_orderAboveSiblings);
                        CHECK_EXCEPTION();
                        (*env)->DeleteLocalRef(env, platformWindow);
                    }
                }
                [self orderChildWindows:YES];
            }

            NSPoint p = [NSEvent mouseLocation];
            NSRect frame = [self.nsWindow frame];
            NSRect contentRect = [self.nsWindow contentRectForFrameRect:frame];

            // Check if the click happened in the non-client area (title bar)
            if (p.y >= (frame.origin.y + contentRect.size.height)) {
                JNIEnv *env = [ThreadUtilities getJNIEnvUncached];
                jobject platformWindow = (*env)->NewLocalRef(env, self.javaPlatformWindow);
                if (platformWindow != NULL) {
                    // Currently, no need to deliver the whole NSEvent.
                    GET_CPLATFORM_WINDOW_CLASS();
                    DECLARE_METHOD(jm_deliverNCMouseDown, jc_CPlatformWindow, "deliverNCMouseDown", "()V");
                    (*env)->CallVoidMethod(env, platformWindow, jm_deliverNCMouseDown);
                    CHECK_EXCEPTION();
                    (*env)->DeleteLocalRef(env, platformWindow);
                }
            }
        }
}

- (void)constrainSize:(NSSize*)size {
    float minWidth = 0.f, minHeight = 0.f;

    if (IS(self.styleBits, DECORATED)) {
        NSRect frame = [self.nsWindow frame];
        NSRect contentRect = [NSWindow contentRectForFrameRect:frame styleMask:[self.nsWindow styleMask]];

        float top = frame.size.height - contentRect.size.height;
        float left = contentRect.origin.x - frame.origin.x;
        float bottom = contentRect.origin.y - frame.origin.y;
        float right = frame.size.width - (contentRect.size.width + left);

        // Speculative estimation: 80 - enough for window decorations controls
        minWidth += left + right + 80;
        minHeight += top + bottom;
    }

    minWidth = MAX(1.f, minWidth);
    minHeight = MAX(1.f, minHeight);

    size->width = MAX(size->width, minWidth);
    size->height = MAX(size->height, minHeight);
}

- (void) setEnabled: (BOOL)flag {
    self.isEnabled = flag;

    if (IS(self.styleBits, CLOSEABLE)) {
        [[self.nsWindow standardWindowButton:NSWindowCloseButton] setEnabled: flag];
    }

    if (IS(self.styleBits, MINIMIZABLE)) {
        [[self.nsWindow standardWindowButton:NSWindowMiniaturizeButton] setEnabled: flag];
    }

    if (IS(self.styleBits, ZOOMABLE)) {
        [[self.nsWindow standardWindowButton:NSWindowZoomButton] setEnabled: flag];
    }

    if (IS(self.styleBits, RESIZABLE)) {
        [self updateMinMaxSize:flag];
        [self.nsWindow setShowsResizeIndicator:flag];
    }
}

+ (void) setLastKeyWindow:(AWTWindow *)window {
    [window retain];
    [lastKeyWindow release];
    lastKeyWindow = window;
}

+ (AWTWindow *) lastKeyWindow {
    return lastKeyWindow;
}

@end // AWTWindow

/*
 * Class:     sun_lwawt_macosx_CPlatformWindow
 * Method:    nativeSetAllAllowAutomaticTabbingProperty
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativeSetAllowAutomaticTabbingProperty
        (JNIEnv *env, jclass clazz, jboolean allowAutomaticTabbing)
{
    JNI_COCOA_ENTER(env);
    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
        if (allowAutomaticTabbing) {
            [NSWindow setAllowsAutomaticWindowTabbing:YES];
        } else {
            [NSWindow setAllowsAutomaticWindowTabbing:NO];
        }
    }];
    JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CPlatformWindow
 * Method:    nativeCreateNSWindow
 * Signature: (JJIIII)J
 */
JNIEXPORT jlong JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativeCreateNSWindow
(JNIEnv *env, jobject obj, jlong contentViewPtr, jlong ownerPtr, jlong styleBits, jdouble x, jdouble y, jdouble w, jdouble h)
{
    __block AWTWindow *window = nil;

JNI_COCOA_ENTER(env);

    jobject platformWindow = (*env)->NewWeakGlobalRef(env, obj);
    NSView *contentView = OBJC(contentViewPtr);
    NSRect frameRect = NSMakeRect(x, y, w, h);
    AWTWindow *owner = [OBJC(ownerPtr) delegate];
    [ThreadUtilities performOnMainThreadWaiting:YES block:^(){

        window = [[AWTWindow alloc] initWithPlatformWindow:platformWindow
                                               ownerWindow:owner
                                                 styleBits:styleBits
                                                 frameRect:frameRect
                                               contentView:contentView];
        // the window is released is CPlatformWindow.nativeDispose()

        if (window) [window.nsWindow retain];
    }];

JNI_COCOA_EXIT(env);

    return ptr_to_jlong(window ? window.nsWindow : nil);
}

/*
 * Class:     sun_lwawt_macosx_CPlatformWindow
 * Method:    nativeSetNSWindowStyleBits
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativeSetNSWindowStyleBits
(JNIEnv *env, jclass clazz, jlong windowPtr, jint mask, jint bits)
{
JNI_COCOA_ENTER(env);

    NSWindow *nsWindow = OBJC(windowPtr);

    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){

        AWTWindow *window = (AWTWindow*)[nsWindow delegate];

        // scans the bit field, and only updates the values requested by the mask
        // (this implicitly handles the _CALLBACK_PROP_BITMASK case, since those are passive reads)
        jint newBits = window.styleBits & ~mask | bits & mask;

        BOOL resized = NO;

        // Check for a change to the full window content view option.
        // The content view must be resized first, otherwise the window will be resized to fit the existing
        // content view.
        if (IS(mask, FULL_WINDOW_CONTENT)) {
            if (IS(newBits, FULL_WINDOW_CONTENT) != IS(window.styleBits, FULL_WINDOW_CONTENT)) {
                NSRect frame = [nsWindow frame];
                NSUInteger styleMask = [AWTWindow styleMaskForStyleBits:newBits];
                NSRect screenContentRect = [NSWindow contentRectForFrameRect:frame styleMask:styleMask];
                NSRect contentFrame = NSMakeRect(screenContentRect.origin.x - frame.origin.x,
                    screenContentRect.origin.y - frame.origin.y,
                    screenContentRect.size.width,
                    screenContentRect.size.height);
                nsWindow.contentView.frame = contentFrame;
                resized = YES;
            }
        }

        // resets the NSWindow's style mask if the mask intersects any of those bits
        if (mask & MASK(_STYLE_PROP_BITMASK)) {
            [nsWindow setStyleMask:[AWTWindow styleMaskForStyleBits:newBits]];
        }

        // calls methods on NSWindow to change other properties, based on the mask
        if (mask & MASK(_METHOD_PROP_BITMASK)) {
            [window setPropertiesForStyleBits:newBits mask:mask];
        }

        window.styleBits = newBits;

        if (resized) {
            [window _deliverMoveResizeEvent];
        }
    }];

JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CPlatformWindow
 * Method:    nativeSetNSWindowMenuBar
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativeSetNSWindowMenuBar
(JNIEnv *env, jclass clazz, jlong windowPtr, jlong menuBarPtr)
{
JNI_COCOA_ENTER(env);

    NSWindow *nsWindow = OBJC(windowPtr);
    CMenuBar *menuBar = OBJC(menuBarPtr);
    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){

        AWTWindow *window = (AWTWindow*)[nsWindow delegate];

        if ([nsWindow isKeyWindow] || [nsWindow isMainWindow]) {
            [window.javaMenuBar deactivate];
        }

        window.javaMenuBar = menuBar;

        CMenuBar* actualMenuBar = menuBar;
        if (actualMenuBar == nil) {
            actualMenuBar = [[ApplicationDelegate sharedDelegate] defaultMenuBar];
        }

        if ([nsWindow isKeyWindow] || [nsWindow isMainWindow]) {
            [CMenuBar activate:actualMenuBar modallyDisabled:NO];
        }
    }];

JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CPlatformWindow
 * Method:    nativeGetNSWindowInsets
 * Signature: (J)Ljava/awt/Insets;
 */
JNIEXPORT jobject JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativeGetNSWindowInsets
(JNIEnv *env, jclass clazz, jlong windowPtr)
{
    jobject ret = NULL;

JNI_COCOA_ENTER(env);

    NSWindow *nsWindow = OBJC(windowPtr);
    __block NSRect contentRect = NSZeroRect;
    __block NSRect frame = NSZeroRect;

    [ThreadUtilities performOnMainThreadWaiting:YES block:^(){

        frame = [nsWindow frame];
        contentRect = [NSWindow contentRectForFrameRect:frame styleMask:[nsWindow styleMask]];
    }];

    jint top = (jint)(frame.size.height - contentRect.size.height);
    jint left = (jint)(contentRect.origin.x - frame.origin.x);
    jint bottom = (jint)(contentRect.origin.y - frame.origin.y);
    jint right = (jint)(frame.size.width - (contentRect.size.width + left));

    DECLARE_CLASS_RETURN(jc_Insets, "java/awt/Insets", NULL);
    DECLARE_METHOD_RETURN(jc_Insets_ctor, jc_Insets, "<init>", "(IIII)V", NULL);
    ret = (*env)->NewObject(env, jc_Insets, jc_Insets_ctor, top, left, bottom, right);

JNI_COCOA_EXIT(env);
    return ret;
}

/*
 * Class:     sun_lwawt_macosx_CPlatformWindow
 * Method:    nativeSetNSWindowBounds
 * Signature: (JDDDD)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativeSetNSWindowBounds
(JNIEnv *env, jclass clazz, jlong windowPtr, jdouble originX, jdouble originY, jdouble width, jdouble height)
{
JNI_COCOA_ENTER(env);

    NSRect jrect = NSMakeRect(originX, originY, width, height);

    // TODO: not sure we need displayIfNeeded message in our view
    NSWindow *nsWindow = OBJC(windowPtr);
    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){

        AWTWindow *window = (AWTWindow*)[nsWindow delegate];

        NSRect rect = ConvertNSScreenRect(NULL, jrect);
        [window constrainSize:&rect.size];

        [nsWindow setFrame:rect display:YES];

        // only start tracking events if pointer is above the toplevel
        // TODO: should post an Entered event if YES.
        NSPoint mLocation = [NSEvent mouseLocation];
        [nsWindow setAcceptsMouseMovedEvents:NSPointInRect(mLocation, rect)];

        // ensure we repaint the whole window after the resize operation
        // (this will also re-enable screen updates, which were disabled above)
        // TODO: send PaintEvent

        // the macOS may ignore our "setFrame" request, in this, case the
        // windowDidMove() will not come and we need to manually resync the
        // "java.awt.Window" and NSWindow locations, because "java.awt.Window"
        // already uses location ignored by the macOS.
        // see sun.lwawt.LWWindowPeer#notifyReshape()
        if (!NSEqualRects(rect, [nsWindow frame])) {
            [window _deliverMoveResizeEvent];
        }
    }];

JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CPlatformWindow
 * Method:    nativeSetNSWindowStandardFrame
 * Signature: (JDDDD)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativeSetNSWindowStandardFrame
(JNIEnv *env, jclass clazz, jlong windowPtr, jdouble originX, jdouble originY,
     jdouble width, jdouble height)
{
    JNI_COCOA_ENTER(env);

    NSRect jrect = NSMakeRect(originX, originY, width, height);

    NSWindow *nsWindow = OBJC(windowPtr);
    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){

        NSRect rect = ConvertNSScreenRect(NULL, jrect);
        AWTWindow *window = (AWTWindow*)[nsWindow delegate];
        window.standardFrame = rect;
    }];

    JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CPlatformWindow
 * Method:    nativeSetNSWindowLocationByPlatform
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativeSetNSWindowLocationByPlatform
(JNIEnv *env, jclass clazz, jlong windowPtr)
{
    JNI_COCOA_ENTER(env);

    NSWindow *nsWindow = OBJC(windowPtr);
    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){

        if (NSEqualPoints(lastTopLeftPoint, NSZeroPoint)) {
            // This is the first usage of lastTopLeftPoint. So invoke cascadeTopLeftFromPoint
            // twice to avoid positioning the window's top left to zero-point, since it may
            // cause negative user experience.
            lastTopLeftPoint = [nsWindow cascadeTopLeftFromPoint:lastTopLeftPoint];
        }
        lastTopLeftPoint = [nsWindow cascadeTopLeftFromPoint:lastTopLeftPoint];
    }];

    JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CPlatformWindow
 * Method:    nativeSetNSWindowMinMax
 * Signature: (JDDDD)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativeSetNSWindowMinMax
(JNIEnv *env, jclass clazz, jlong windowPtr, jdouble minW, jdouble minH, jdouble maxW, jdouble maxH)
{
JNI_COCOA_ENTER(env);

    if (minW < 1) minW = 1;
    if (minH < 1) minH = 1;
    if (maxW < 1) maxW = 1;
    if (maxH < 1) maxH = 1;

    NSWindow *nsWindow = OBJC(windowPtr);
    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){

        AWTWindow *window = (AWTWindow*)[nsWindow delegate];

        NSSize min = { minW, minH };
        NSSize max = { maxW, maxH };

        [window constrainSize:&min];
        [window constrainSize:&max];

        window.javaMinSize = min;
        window.javaMaxSize = max;
        [window updateMinMaxSize:IS(window.styleBits, RESIZABLE)];
    }];

JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CPlatformWindow
 * Method:    nativePushNSWindowToBack
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativePushNSWindowToBack
(JNIEnv *env, jclass clazz, jlong windowPtr)
{
JNI_COCOA_ENTER(env);

    NSWindow *nsWindow = OBJC(windowPtr);
    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
        [nsWindow orderBack:nil];
        // Order parent windows
        AWTWindow *awtWindow = (AWTWindow*)[nsWindow delegate];
        while (awtWindow.ownerWindow != nil) {
            awtWindow = awtWindow.ownerWindow;
            if ([AWTWindow isJavaPlatformWindowVisible:awtWindow.nsWindow]) {
                [awtWindow.nsWindow orderBack:nil];
            }
        }
        // Order child windows
        [(AWTWindow*)[nsWindow delegate] orderChildWindows:NO];
    }];

JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CPlatformWindow
 * Method:    nativePushNSWindowToFront
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativePushNSWindowToFront
(JNIEnv *env, jclass clazz, jlong windowPtr)
{
JNI_COCOA_ENTER(env);

    NSWindow *nsWindow = OBJC(windowPtr);
    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){

        if (![nsWindow isKeyWindow]) {
            [nsWindow makeKeyAndOrderFront:nsWindow];
        } else {
            [nsWindow orderFront:nsWindow];
        }
    }];

JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CPlatformWindow
 * Method:    nativeSetNSWindowTitle
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativeSetNSWindowTitle
(JNIEnv *env, jclass clazz, jlong windowPtr, jstring jtitle)
{
JNI_COCOA_ENTER(env);

    NSWindow *nsWindow = OBJC(windowPtr);
    [nsWindow performSelectorOnMainThread:@selector(setTitle:)
                              withObject:JavaStringToNSString(env, jtitle)
                           waitUntilDone:NO];

JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CPlatformWindow
 * Method:    nativeRevalidateNSWindowShadow
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativeRevalidateNSWindowShadow
(JNIEnv *env, jclass clazz, jlong windowPtr)
{
JNI_COCOA_ENTER(env);

    NSWindow *nsWindow = OBJC(windowPtr);
    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
        [nsWindow invalidateShadow];
    }];

JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CPlatformWindow
 * Method:    nativeScreenOn_AppKitThread
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativeScreenOn_1AppKitThread
(JNIEnv *env, jclass clazz, jlong windowPtr)
{
    jint ret = 0;

JNI_COCOA_ENTER(env);
AWT_ASSERT_APPKIT_THREAD;

    NSWindow *nsWindow = OBJC(windowPtr);
    NSDictionary *props = [[nsWindow screen] deviceDescription];
    ret = [[props objectForKey:@"NSScreenNumber"] intValue];

JNI_COCOA_EXIT(env);

    return ret;
}

/*
 * Class:     sun_lwawt_macosx_CPlatformWindow
 * Method:    nativeSetNSWindowMinimizedIcon
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativeSetNSWindowMinimizedIcon
(JNIEnv *env, jclass clazz, jlong windowPtr, jlong nsImagePtr)
{
JNI_COCOA_ENTER(env);

    NSWindow *nsWindow = OBJC(windowPtr);
    NSImage *image = OBJC(nsImagePtr);
    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
        [nsWindow setMiniwindowImage:image];
    }];

JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CPlatformWindow
 * Method:    nativeSetNSWindowRepresentedFilename
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativeSetNSWindowRepresentedFilename
(JNIEnv *env, jclass clazz, jlong windowPtr, jstring filename)
{
JNI_COCOA_ENTER(env);

    NSWindow *nsWindow = OBJC(windowPtr);
    NSURL *url = (filename == NULL) ? nil : [NSURL fileURLWithPath:NormalizedPathNSStringFromJavaString(env, filename)];
    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
        [nsWindow setRepresentedURL:url];
    }];

JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CPlatformWindow
 * Method:    nativeGetTopmostPlatformWindowUnderMouse
 * Signature: (J)V
 */
JNIEXPORT jobject
JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativeGetTopmostPlatformWindowUnderMouse
(JNIEnv *env, jclass clazz)
{
    __block jobject topmostWindowUnderMouse = nil;

    JNI_COCOA_ENTER(env);

    [ThreadUtilities performOnMainThreadWaiting:YES block:^{
        AWTWindow *awtWindow = [AWTWindow getTopmostWindowUnderMouse];
        if (awtWindow != nil) {
            topmostWindowUnderMouse = awtWindow.javaPlatformWindow;
        }
    }];

    JNI_COCOA_EXIT(env);

    return topmostWindowUnderMouse;
}

/*
 * Class:     sun_lwawt_macosx_CPlatformWindow
 * Method:    nativeSynthesizeMouseEnteredExitedEvents
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativeSynthesizeMouseEnteredExitedEvents__
(JNIEnv *env, jclass clazz)
{
    JNI_COCOA_ENTER(env);

    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
        [AWTWindow synthesizeMouseEnteredExitedEventsForAllWindows];
    }];

    JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CPlatformWindow
 * Method:    nativeSynthesizeMouseEnteredExitedEvents
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativeSynthesizeMouseEnteredExitedEvents__JI
(JNIEnv *env, jclass clazz, jlong windowPtr, jint eventType)
{
JNI_COCOA_ENTER(env);

    if (eventType == NSMouseEntered || eventType == NSMouseExited) {
        NSWindow *nsWindow = OBJC(windowPtr);

        [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
            [AWTWindow synthesizeMouseEnteredExitedEvents:nsWindow withType:eventType];
        }];
    } else {
        JNU_ThrowIllegalArgumentException(env, "unknown event type");
    }

JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CPlatformWindow
 * Method:    _toggleFullScreenMode
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPlatformWindow__1toggleFullScreenMode
(JNIEnv *env, jobject peer, jlong windowPtr)
{
JNI_COCOA_ENTER(env);

    NSWindow *nsWindow = OBJC(windowPtr);
    SEL toggleFullScreenSelector = @selector(toggleFullScreen:);
    if (![nsWindow respondsToSelector:toggleFullScreenSelector]) return;

    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
        [nsWindow performSelector:toggleFullScreenSelector withObject:nil];
    }];

JNI_COCOA_EXIT(env);
}

JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativeSetEnabled
(JNIEnv *env, jclass clazz, jlong windowPtr, jboolean isEnabled)
{
JNI_COCOA_ENTER(env);

    NSWindow *nsWindow = OBJC(windowPtr);
    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
        AWTWindow *window = (AWTWindow*)[nsWindow delegate];

        [window setEnabled: isEnabled];
    }];

JNI_COCOA_EXIT(env);
}

JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativeDispose
(JNIEnv *env, jclass clazz, jlong windowPtr)
{
JNI_COCOA_ENTER(env);

    NSWindow *nsWindow = OBJC(windowPtr);
    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
        AWTWindow *window = (AWTWindow*)[nsWindow delegate];

        if ([AWTWindow lastKeyWindow] == window) {
            [AWTWindow setLastKeyWindow: nil];
        }

        // AWTWindow holds a reference to the NSWindow in its nsWindow
        // property. Unsetting the delegate allows it to be deallocated
        // which releases the reference. This, in turn, allows the window
        // itself be deallocated.
        [nsWindow setDelegate: nil];

        [window release];
    }];

JNI_COCOA_EXIT(env);
}

JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativeEnterFullScreenMode
(JNIEnv *env, jclass clazz, jlong windowPtr)
{
JNI_COCOA_ENTER(env);

    NSWindow *nsWindow = OBJC(windowPtr);
    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
        AWTWindow *window = (AWTWindow*)[nsWindow delegate];
        NSNumber* screenID = [AWTWindow getNSWindowDisplayID_AppKitThread: nsWindow];
        CGDirectDisplayID aID = [screenID intValue];

        if (CGDisplayCapture(aID) == kCGErrorSuccess) {
            // remove window decoration
            NSUInteger styleMask = [AWTWindow styleMaskForStyleBits:window.styleBits];
            [nsWindow setStyleMask:(styleMask & ~NSTitledWindowMask) | NSBorderlessWindowMask];

            int shieldLevel = CGShieldingWindowLevel();
            window.preFullScreenLevel = [nsWindow level];
            [nsWindow setLevel: shieldLevel];

            NSRect screenRect = [[nsWindow screen] frame];
            [nsWindow setFrame:screenRect display:YES];
        } else {
            [NSException raise:@"Java Exception" reason:@"Failed to enter full screen." userInfo:nil];
        }
    }];

JNI_COCOA_EXIT(env);
}

JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPlatformWindow_nativeExitFullScreenMode
(JNIEnv *env, jclass clazz, jlong windowPtr)
{
JNI_COCOA_ENTER(env);

    NSWindow *nsWindow = OBJC(windowPtr);
    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
        AWTWindow *window = (AWTWindow*)[nsWindow delegate];
        NSNumber* screenID = [AWTWindow getNSWindowDisplayID_AppKitThread: nsWindow];
        CGDirectDisplayID aID = [screenID intValue];

        if (CGDisplayRelease(aID) == kCGErrorSuccess) {
            NSUInteger styleMask = [AWTWindow styleMaskForStyleBits:window.styleBits];
            [nsWindow setStyleMask:styleMask];
            [nsWindow setLevel: window.preFullScreenLevel];

            // GraphicsDevice takes care of restoring pre full screen bounds
        } else {
            [NSException raise:@"Java Exception" reason:@"Failed to exit full screen." userInfo:nil];
        }
    }];

JNI_COCOA_EXIT(env);
}

