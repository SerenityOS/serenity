/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

#import "CSystemColors.h"

#import "java_awt_SystemColor.h"
#import "sun_lwawt_macosx_LWCToolkit.h"

#import <JavaRuntimeSupport/JavaRuntimeSupport.h>

#import "ThreadUtilities.h"
#import "JNIUtilities.h"

NSColor **sColors = nil;
NSColor **appleColors = nil;

@implementation CSystemColors

+ (void)initialize {
    JNIEnv *env = [ThreadUtilities getJNIEnv];
JNI_COCOA_ENTER(env);
    [CSystemColors reloadColors];
    [[NSNotificationCenter defaultCenter] addObserver:[CSystemColors class] selector:@selector(systemColorsDidChange:) name:NSSystemColorsDidChangeNotification object:nil];
JNI_COCOA_EXIT(env);
}


+ (void)systemColorsDidChange:(NSNotification *)notification {
    AWT_ASSERT_APPKIT_THREAD;

    [CSystemColors reloadColors];

    // Call LWCToolkit with the news. LWCToolkit makes certain to do its duties
    // from a new thread.
    JNIEnv* env = [ThreadUtilities getJNIEnv];
    DECLARE_CLASS(jc_LWCToolkit, "sun/lwawt/macosx/LWCToolkit");
    DECLARE_STATIC_METHOD(jm_systemColorsChanged, jc_LWCToolkit, "systemColorsChanged", "()V");
    (*env)->CallStaticVoidMethod(env, jc_LWCToolkit, jm_systemColorsChanged); // AWT_THREADING Safe (event)
    CHECK_EXCEPTION();

}


+ (void)reloadColors {
    // NOTE: <rdar://problem/3447825> was filed to make this code even lazier. Each
    //  color below could be set lazily when it was first accessed. This way the
    //  arrays would be sparse, and filled in as SystemColors were used.
    int i;
    if (sColors == nil) {
        sColors = (NSColor**)malloc(sizeof(NSColor*) * java_awt_SystemColor_NUM_COLORS);
    } else {
        for (i = 0; i < java_awt_SystemColor_NUM_COLORS; i++) {
            if (sColors[i] != NULL) [sColors[i] release];
        }
    }

    sColors[java_awt_SystemColor_DESKTOP] =                    [NSColor greenColor];
    sColors[java_awt_SystemColor_ACTIVE_CAPTION] =            [NSColor whiteColor];
    sColors[java_awt_SystemColor_ACTIVE_CAPTION_TEXT] =        [NSColor blackColor];
    sColors[java_awt_SystemColor_ACTIVE_CAPTION_BORDER] =    [NSColor whiteColor];
    sColors[java_awt_SystemColor_INACTIVE_CAPTION] =        [NSColor grayColor];
    sColors[java_awt_SystemColor_INACTIVE_CAPTION_TEXT] =    [NSColor grayColor];
    sColors[java_awt_SystemColor_INACTIVE_CAPTION_BORDER] =    [NSColor grayColor];
    const CGFloat color = (CGFloat)0xEE/(CGFloat)0xFF;
    sColors[java_awt_SystemColor_WINDOW] = [NSColor colorWithCalibratedRed:color green:color blue:color alpha:1.0f];
    sColors[java_awt_SystemColor_WINDOW_BORDER] =            [NSColor windowFrameColor];
    sColors[java_awt_SystemColor_WINDOW_TEXT] =                [NSColor windowFrameTextColor];
    sColors[java_awt_SystemColor_MENU] =                    [NSColor controlBackgroundColor];
    sColors[java_awt_SystemColor_MENU_TEXT] =                [NSColor controlTextColor];
    sColors[java_awt_SystemColor_TEXT] =                    [NSColor textBackgroundColor];
    sColors[java_awt_SystemColor_TEXT_TEXT] =                [NSColor textColor];
    sColors[java_awt_SystemColor_TEXT_HIGHLIGHT] =            [NSColor selectedTextBackgroundColor];
    sColors[java_awt_SystemColor_TEXT_HIGHLIGHT_TEXT] =        [NSColor selectedTextColor];
    sColors[java_awt_SystemColor_TEXT_INACTIVE_TEXT] =        [NSColor disabledControlTextColor];
    sColors[java_awt_SystemColor_CONTROL] =                    [NSColor controlColor];
    sColors[java_awt_SystemColor_CONTROL_TEXT] =            [NSColor controlTextColor];
    sColors[java_awt_SystemColor_CONTROL_HIGHLIGHT] =        [NSColor alternateSelectedControlColor];
    sColors[java_awt_SystemColor_CONTROL_LT_HIGHLIGHT] =    [NSColor alternateSelectedControlTextColor];
    sColors[java_awt_SystemColor_CONTROL_SHADOW] =            [NSColor controlShadowColor];
    sColors[java_awt_SystemColor_CONTROL_DK_SHADOW] =        [NSColor controlDarkShadowColor];
    sColors[java_awt_SystemColor_SCROLLBAR] =                [NSColor scrollBarColor];
    sColors[java_awt_SystemColor_INFO] =                    [NSColor textBackgroundColor];
    sColors[java_awt_SystemColor_INFO_TEXT] =                [NSColor textColor];

    for (i = 0; i < java_awt_SystemColor_NUM_COLORS; i++) {
        [sColors[i] retain];
    }

    if (appleColors == nil) {
        appleColors = (NSColor**)malloc(sizeof(NSColor*) * sun_lwawt_macosx_LWCToolkit_NUM_APPLE_COLORS);
    } else {
        for (i = 0; i < sun_lwawt_macosx_LWCToolkit_NUM_APPLE_COLORS; i++) {
            if (appleColors[i] != NULL) [appleColors[i] release];
        }
    }

    appleColors[sun_lwawt_macosx_LWCToolkit_KEYBOARD_FOCUS_COLOR] =                    [NSColor keyboardFocusIndicatorColor];
    appleColors[sun_lwawt_macosx_LWCToolkit_INACTIVE_SELECTION_BACKGROUND_COLOR] =    [NSColor secondarySelectedControlColor];
    appleColors[sun_lwawt_macosx_LWCToolkit_INACTIVE_SELECTION_FOREGROUND_COLOR] =    [NSColor controlDarkShadowColor];
    appleColors[sun_lwawt_macosx_LWCToolkit_SELECTED_CONTROL_TEXT_COLOR] =            [NSColor controlTextColor];

    for (i = 0; i < sun_lwawt_macosx_LWCToolkit_NUM_APPLE_COLORS; i++) {
        [appleColors[i] retain];
    }
}

+ (NSColor*)getColor:(NSUInteger)colorIndex useAppleColor:(BOOL)useAppleColor {
    NSColor* result = nil;

    if (colorIndex < ((useAppleColor) ? sun_lwawt_macosx_LWCToolkit_NUM_APPLE_COLORS : java_awt_SystemColor_NUM_COLORS)) {
        result = (useAppleColor ? appleColors : sColors)[colorIndex];
    }
    else {
        NSLog(@"%s: %s %sColor: %ld not found, returning black.", __FILE__, __FUNCTION__, (useAppleColor) ? "Apple" : "System", colorIndex);
        result = [NSColor blackColor];
    }

    return result;
}
@end
