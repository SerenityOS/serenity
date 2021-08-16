/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

#import "CRobotKeyCode.h"
#import "java_awt_event_KeyEvent.h"

@implementation CRobotKeyCodeMapping

@synthesize javaToMacKeyMap;

+(CRobotKeyCodeMapping *) sharedInstance {
    static CRobotKeyCodeMapping *instance = nil;
    static dispatch_once_t executeOnce;

    dispatch_once(&executeOnce, ^{
        instance = [[CRobotKeyCodeMapping alloc] init];
    });

    return instance;
}

-(id) init {
    self = [super init];

    if (nil != self) {
        self.javaToMacKeyMap = [NSDictionary dictionaryWithObjectsAndKeys :
            [NSNumber numberWithInt : OSX_Delete], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_BACK_SPACE],
            [NSNumber numberWithInt : OSX_kVK_Tab], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_TAB],
            [NSNumber numberWithInt : OSX_kVK_Return], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_ENTER],
            [NSNumber numberWithInt : OSX_kVK_ANSI_KeypadClear], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_CLEAR],
            [NSNumber numberWithInt : OSX_Shift], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_SHIFT],
            [NSNumber numberWithInt : OSX_Control], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_CONTROL],
            [NSNumber numberWithInt : OSX_Option], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_ALT],
            [NSNumber numberWithInt : OSX_RightOption], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_ALT_GRAPH],
            [NSNumber numberWithInt : OSX_CapsLock], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_CAPS_LOCK],
            [NSNumber numberWithInt : OSX_Escape], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_ESCAPE],
            [NSNumber numberWithInt : OSX_kVK_Space], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_SPACE],
            [NSNumber numberWithInt : OSX_PageUp], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_PAGE_UP],
            [NSNumber numberWithInt : OSX_PageDown], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_PAGE_DOWN],
            [NSNumber numberWithInt : OSX_End], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_END],
            [NSNumber numberWithInt : OSX_Home], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_HOME],
            [NSNumber numberWithInt : OSX_LeftArrow], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_LEFT],
            [NSNumber numberWithInt : OSX_UpArrow], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_UP],
            [NSNumber numberWithInt : OSX_RightArrow], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_RIGHT],
            [NSNumber numberWithInt : OSX_DownArrow], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_DOWN],
            [NSNumber numberWithInt : OSX_kVK_ANSI_Comma], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_COMMA],
            [NSNumber numberWithInt : OSX_kVK_ANSI_Minus], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_MINUS],
            [NSNumber numberWithInt : OSX_kVK_ANSI_Period], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_PERIOD],
            [NSNumber numberWithInt : OSX_kVK_ANSI_Slash], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_SLASH],

            [NSNumber numberWithInt : OSX_kVK_ANSI_0], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_0],
            [NSNumber numberWithInt : OSX_kVK_ANSI_1], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_1],
            [NSNumber numberWithInt : OSX_kVK_ANSI_2], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_2],
            [NSNumber numberWithInt : OSX_kVK_ANSI_3], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_3],
            [NSNumber numberWithInt : OSX_kVK_ANSI_4], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_4],
            [NSNumber numberWithInt : OSX_kVK_ANSI_5], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_5],
            [NSNumber numberWithInt : OSX_kVK_ANSI_6], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_6],
            [NSNumber numberWithInt : OSX_kVK_ANSI_7], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_7],
            [NSNumber numberWithInt : OSX_kVK_ANSI_8], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_8],
            [NSNumber numberWithInt : OSX_kVK_ANSI_9], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_9],

            [NSNumber numberWithInt : OSX_kVK_ANSI_Semicolon], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_SEMICOLON],
            [NSNumber numberWithInt : OSX_kVK_ANSI_Equal], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_EQUALS],

            [NSNumber numberWithInt : OSX_kVK_ANSI_A], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_A],
            [NSNumber numberWithInt : OSX_kVK_ANSI_B], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_B],
            [NSNumber numberWithInt : OSX_kVK_ANSI_C], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_C],
            [NSNumber numberWithInt : OSX_kVK_ANSI_D], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_D],
            [NSNumber numberWithInt : OSX_kVK_ANSI_E], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_E],
            [NSNumber numberWithInt : OSX_kVK_ANSI_F], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_F],
            [NSNumber numberWithInt : OSX_kVK_ANSI_G], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_G],
            [NSNumber numberWithInt : OSX_kVK_ANSI_H], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_H],
            [NSNumber numberWithInt : OSX_kVK_ANSI_I], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_I],
            [NSNumber numberWithInt : OSX_kVK_ANSI_J], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_J],
            [NSNumber numberWithInt : OSX_kVK_ANSI_K], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_K],
            [NSNumber numberWithInt : OSX_kVK_ANSI_L], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_L],
            [NSNumber numberWithInt : OSX_kVK_ANSI_M], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_M],
            [NSNumber numberWithInt : OSX_kVK_ANSI_N], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_N],
            [NSNumber numberWithInt : OSX_kVK_ANSI_O], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_O],
            [NSNumber numberWithInt : OSX_kVK_ANSI_P], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_P],
            [NSNumber numberWithInt : OSX_kVK_ANSI_Q], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_Q],
            [NSNumber numberWithInt : OSX_kVK_ANSI_R], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_R],
            [NSNumber numberWithInt : OSX_kVK_ANSI_S], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_S],
            [NSNumber numberWithInt : OSX_kVK_ANSI_T], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_T],
            [NSNumber numberWithInt : OSX_kVK_ANSI_U], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_U],
            [NSNumber numberWithInt : OSX_kVK_ANSI_V], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_V],
            [NSNumber numberWithInt : OSX_kVK_ANSI_W], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_W],
            [NSNumber numberWithInt : OSX_kVK_ANSI_X], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_X],
            [NSNumber numberWithInt : OSX_kVK_ANSI_Y], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_Y],
            [NSNumber numberWithInt : OSX_kVK_ANSI_Z], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_Z],

            [NSNumber numberWithInt : OSX_kVK_ANSI_LeftBracket], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_OPEN_BRACKET],
            [NSNumber numberWithInt : OSX_kVK_ANSI_Backslash], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_BACK_SLASH],
            [NSNumber numberWithInt : OSX_kVK_ANSI_RightBracket], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_CLOSE_BRACKET],

            [NSNumber numberWithInt : OSX_kVK_ANSI_Keypad0], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_NUMPAD0],
            [NSNumber numberWithInt : OSX_kVK_ANSI_Keypad1], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_NUMPAD1],
            [NSNumber numberWithInt : OSX_kVK_ANSI_Keypad2], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_NUMPAD2],
            [NSNumber numberWithInt : OSX_kVK_ANSI_Keypad3], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_NUMPAD3],
            [NSNumber numberWithInt : OSX_kVK_ANSI_Keypad4], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_NUMPAD4],
            [NSNumber numberWithInt : OSX_kVK_ANSI_Keypad5], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_NUMPAD5],
            [NSNumber numberWithInt : OSX_kVK_ANSI_Keypad6], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_NUMPAD6],
            [NSNumber numberWithInt : OSX_kVK_ANSI_Keypad7], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_NUMPAD7],
            [NSNumber numberWithInt : OSX_kVK_ANSI_Keypad8], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_NUMPAD8],
            [NSNumber numberWithInt : OSX_kVK_ANSI_Keypad9], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_NUMPAD9],

            [NSNumber numberWithInt : OSX_kVK_ANSI_KeypadMultiply], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_MULTIPLY],
            [NSNumber numberWithInt : OSX_kVK_ANSI_KeypadPlus], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_ADD],
            [NSNumber numberWithInt : OSX_kVK_ANSI_KeypadMinus], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_SUBTRACT],
            [NSNumber numberWithInt : OSX_kVK_ANSI_KeypadDecimal], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_DECIMAL],
            [NSNumber numberWithInt : OSX_kVK_ANSI_KeypadDivide], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_DIVIDE],

            [NSNumber numberWithInt : OSX_F1], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_F1],
            [NSNumber numberWithInt : OSX_F2], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_F2],
            [NSNumber numberWithInt : OSX_F3], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_F3],
            [NSNumber numberWithInt : OSX_F4], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_F4],
            [NSNumber numberWithInt : OSX_F5], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_F5],
            [NSNumber numberWithInt : OSX_F6], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_F6],
            [NSNumber numberWithInt : OSX_F7], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_F7],
            [NSNumber numberWithInt : OSX_F8], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_F8],
            [NSNumber numberWithInt : OSX_F9], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_F9],
            [NSNumber numberWithInt : OSX_F10], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_F10],
            [NSNumber numberWithInt : OSX_F11], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_F11],
            [NSNumber numberWithInt : OSX_F12], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_F12],

            [NSNumber numberWithInt : OSX_ForwardDelete], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_DELETE],
            [NSNumber numberWithInt : OSX_Help], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_HELP],
            [NSNumber numberWithInt : OSX_Command], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_META],
            [NSNumber numberWithInt : OSX_kVK_ANSI_Grave], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_BACK_QUOTE],
            [NSNumber numberWithInt : OSX_kVK_ANSI_Quote], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_QUOTE],

            [NSNumber numberWithInt : OSX_F13], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_F13],
            [NSNumber numberWithInt : OSX_F14], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_F14],
            [NSNumber numberWithInt : OSX_F15], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_F15],
            [NSNumber numberWithInt : OSX_F16], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_F16],
            [NSNumber numberWithInt : OSX_F17], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_F17],
            [NSNumber numberWithInt : OSX_F18], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_F18],
            [NSNumber numberWithInt : OSX_F19], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_F19],
            [NSNumber numberWithInt : OSX_F20], [NSNumber numberWithInt : java_awt_event_KeyEvent_VK_F20],

            nil];
    }

    return self;
}

-(int) getOSXKeyCodeForJavaKey : (int) javaKey {
    id val = [javaToMacKeyMap objectForKey : [NSNumber numberWithInt : javaKey]];

    if (nil != val) {
        return [val intValue];
    } else {
        return OSX_Undefined;
    }
}

@end
