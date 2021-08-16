/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
#import <jni_util.h>

static NSWindow *testWindow;
static NSColorPanel *colorPanel;

#define JNI_COCOA_ENTER(env) \
 NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init]; \
 @try {

#define JNI_COCOA_EXIT(env) \
 } \
 @catch (NSException *e) { \
     NSLog(@"%@", [e callStackSymbols]); \
 } \
 @finally { \
    [pool drain]; \
  };

/*
 * Pass the block to a selector of a class that extends NSObject
 * There is no need to copy the block since this class always waits.
 */
@interface BlockRunner : NSObject { }

+ (void)invokeBlock:(void (^)())block;
@end

@implementation BlockRunner

+ (void)invokeBlock:(void (^)())block{
  block();
}

+ (void)performBlock:(void (^)())block {
  [self performSelectorOnMainThread:@selector(invokeBlock:) withObject:block waitUntilDone:YES];
}

@end

/*
 * Class:     TestMainKeyWindow
 * Method:    setup
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_TestMainKeyWindow_setup(JNIEnv *env, jclass cl)
{
    JNI_COCOA_ENTER(env);

    void (^block)() = ^(){
        NSScreen *mainScreen = [[NSScreen screens] objectAtIndex:0];
        NSRect screenFrame = [mainScreen frame];
        NSRect frame = NSMakeRect(130, screenFrame.size.height - 280, 200, 100);

#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 101200
        NSWindowStyleMask style = NSWindowStyleMaskTitled;
#else
        NSInteger style = NSTitledWindowMask;
#endif

        NSRect rect = [NSWindow contentRectForFrameRect:frame styleMask:style];
        NSBackingStoreType bt = NSBackingStoreBuffered;
        testWindow = [[[NSWindow alloc] initWithContentRect:rect styleMask:style backing:bt defer:NO] retain];
        testWindow.title = @"NSWindow";
        [testWindow setBackgroundColor:[NSColor blueColor]];
        [testWindow makeKeyAndOrderFront:nil];
        // Java coordinates are 100, 200

        colorPanel = [[NSColorPanel sharedColorPanel] retain];
        [colorPanel setReleasedWhenClosed: YES];
        colorPanel.restorable = NO;
        [colorPanel setFrame:NSMakeRect(130, screenFrame.size.height - 500, 200, 200) display:NO];
        // Java coordinates are 100, 400
        [colorPanel makeKeyAndOrderFront: nil];
    };

    if ([NSThread isMainThread]) {
        block();
    } else {
        [BlockRunner performBlock:block];
    }

    JNI_COCOA_EXIT(env);
}

/*
 * Class:     TestMainKeyWindow
 * Method:    takedown
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_TestMainKeyWindow_takedown(JNIEnv *env, jclass cl)
{
    JNI_COCOA_ENTER(env);

    void (^block)() = ^(){
        if (testWindow != nil) {
            [testWindow close];
            testWindow = nil;
        }
        if (colorPanel != nil) {
            [colorPanel orderOut:nil];
            colorPanel = nil;
        }
    };

    if ([NSThread isMainThread]) {
        block();
    } else {
        [BlockRunner performBlock:block];
    }

    JNI_COCOA_EXIT(env);
}

/*
 * Class:     TestMainKeyWindow
 * Method:    activateApplication
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_TestMainKeyWindow_activateApplication
  (JNIEnv *env, jclass cl)
{
    JNI_COCOA_ENTER(env);

    void (^block)() = ^(){
        [NSApp activateIgnoringOtherApps:YES];
    };

    [BlockRunner performBlock:block];

  JNI_COCOA_EXIT(env);
}
