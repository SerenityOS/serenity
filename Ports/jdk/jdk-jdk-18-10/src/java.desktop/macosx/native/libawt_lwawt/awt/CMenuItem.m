/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include <Carbon/Carbon.h>
#import "CMenuItem.h"
#import "CMenu.h"
#import "AWTEvent.h"
#import "AWTWindow.h"
#import "ThreadUtilities.h"
#import "JNIUtilities.h"

#import "java_awt_Event.h"
#import "java_awt_event_KeyEvent.h"
#import "sun_lwawt_macosx_CMenuItem.h"

#define NOT_A_CHECKBOXMENU -2


@implementation CMenuItem

- (id) initWithPeer:(jobject)peer asSeparator: (BOOL) asSeparator{
    AWT_ASSERT_APPKIT_THREAD;
    self = [super initWithPeer:peer];
    if (self) {
        if (asSeparator) {
            fMenuItem = (NSMenuItem*)[NSMenuItem separatorItem];
            [fMenuItem retain];
        } else {
            fMenuItem = [[NSMenuItem alloc] init];
            [fMenuItem setAction:@selector(handleAction:)];
            [fMenuItem setTarget:self];
        }
        fIsCheckbox = NO;
        fIsEnabled = YES;
    }
    return self;
}

// This is because NSApplication doesn't check the target's window when sending
// actions; they only check the target itself.  We always return YES,
// since we shouldn't even be installed unless our window is active.
- (BOOL) worksWhenModal {
    return YES;
}

// Events
- (void)handleAction:(NSMenuItem *)sender {
    AWT_ASSERT_APPKIT_THREAD;
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    JNI_COCOA_ENTER(env);

    // If we are called as a result of user pressing a shortcut, do nothing,
    // because AVTView has already sent corresponding key event to the Java
    // layer from performKeyEquivalent.
    // There is an exception from the rule above, though: if a window with
    // a menu gets minimized by user and there are no other windows to take
    // focus, the window's menu won't be removed from the global menu bar.
    // However, the Java layer won't handle invocation by a shortcut coming
    // from this "frameless" menu, because there are no active windows. This
    // means we have to handle it here.
    NSEvent *currEvent = [[NSApplication sharedApplication] currentEvent];

    if ([currEvent type] == NSKeyDown) {
        // The action event can be ignored only if the key window is an AWT window.
        // Otherwise, the action event is the only notification and must be processed.
        NSWindow *keyWindow = [NSApp keyWindow];
        if (keyWindow != nil && [AWTWindow isAWTWindow: keyWindow]) {
            return;
        }
    }

    if (fIsCheckbox) {
        DECLARE_CLASS(jc_CCheckboxMenuItem, "sun/lwawt/macosx/CCheckboxMenuItem");
        DECLARE_METHOD(jm_ckHandleAction, jc_CCheckboxMenuItem, "handleAction", "(Z)V");

        // Send the opposite of what's currently checked -- the action
        // indicates what state we're going to.
        NSInteger state = [sender state];
        jboolean newState = (state == NSOnState ? JNI_FALSE : JNI_TRUE);
        (*env)->CallVoidMethod(env, fPeer, jm_ckHandleAction, newState);
    } else {
        DECLARE_CLASS(jc_CMenuItem, "sun/lwawt/macosx/CMenuItem");
        DECLARE_METHOD(jm_handleAction, jc_CMenuItem, "handleAction", "(JI)V"); // AWT_THREADING Safe (event)

        NSUInteger modifiers = [currEvent modifierFlags];
        jint javaModifiers = NsKeyModifiersToJavaModifiers(modifiers, NO);

        (*env)->CallVoidMethod(env, fPeer, jm_handleAction, UTC(currEvent), javaModifiers); // AWT_THREADING Safe (event)
    }
    CHECK_EXCEPTION();
    JNI_COCOA_EXIT(env);
}

- (void) setJavaLabel:(NSString *)theLabel shortcut:(NSString *)theKeyEquivalent modifierMask:(jint)modifiers {

    NSUInteger modifierMask = 0;

    if (![theKeyEquivalent isEqualToString:@""]) {
        // Force the key equivalent to lower case if not using the shift key.
        // Otherwise AppKit will draw a Shift glyph in the menu.
        if ((modifiers & java_awt_event_KeyEvent_SHIFT_MASK) == 0) {
            theKeyEquivalent = [theKeyEquivalent lowercaseString];
        }

        // Hack for the question mark -- SHIFT and / means use the question mark.
        if ((modifiers & java_awt_event_KeyEvent_SHIFT_MASK) != 0 &&
            [theKeyEquivalent isEqualToString:@"/"])
        {
            theKeyEquivalent = @"?";
            modifiers &= ~java_awt_event_KeyEvent_SHIFT_MASK;
        }

        modifierMask = JavaModifiersToNsKeyModifiers(modifiers, NO);
    }

    [ThreadUtilities performOnMainThreadWaiting:YES block:^(){
        [fMenuItem setKeyEquivalent:theKeyEquivalent];
        [fMenuItem setKeyEquivalentModifierMask:modifierMask];
        [fMenuItem setTitle:theLabel];
    }];
}

- (void) setJavaImage:(NSImage *)theImage {

    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
        [fMenuItem setImage:theImage];
    }];
}

- (void) setJavaToolTipText:(NSString *)theText {

    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
        [fMenuItem setToolTip:theText];
    }];
}


- (void)setJavaEnabled:(BOOL) enabled {

    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
        @synchronized(self) {
            fIsEnabled = enabled;

            // Warning:  This won't work if the parent menu is disabled.
            // See [CMenu syncFromJava]. We still need to call it here so
            // the NSMenuItem itself gets properly updated.
            [fMenuItem setEnabled:fIsEnabled];
        }
    }];
}

- (BOOL)isEnabled {

    BOOL enabled = NO;
    @synchronized(self) {
        enabled = fIsEnabled;
    }
    return enabled;
}


- (void)setJavaState:(BOOL)newState {

    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
        [fMenuItem setState:(newState ? NSOnState : NSOffState)];
    }];
}

- (void)dealloc {
    [fMenuItem setAction:NULL];
    [fMenuItem setTarget:nil];
    [fMenuItem release];
    fMenuItem = nil;

    [super dealloc];
}

- (void)addNSMenuItemToMenu:(NSMenu *)inMenu {
    [inMenu addItem:fMenuItem];
}

- (NSMenuItem *)menuItem {
    return [[fMenuItem retain] autorelease];
}

- (void)setIsCheckbox {
    fIsCheckbox = YES;
}

- (NSString *)description {
    return [NSString stringWithFormat:@"CMenuItem[ %@ ]", fMenuItem];
}

@end

/** Convert a Java keycode for SetMenuItemCmd */
static unichar AWTKeyToMacShortcut(jint awtKey, BOOL doShift) {
    unichar macKey = 0;

    if ((awtKey >= java_awt_event_KeyEvent_VK_0 && awtKey <= java_awt_event_KeyEvent_VK_9) ||
        (awtKey >= java_awt_event_KeyEvent_VK_A && awtKey <= java_awt_event_KeyEvent_VK_Z))
    {
        // These ranges are the same in ASCII
        macKey = awtKey;
    } else if (awtKey >= java_awt_event_KeyEvent_VK_F1 && awtKey <= java_awt_event_KeyEvent_VK_F12) {
        // Support for F1 - F12 has been around since Java 1.0 and fall into a lower range.
        macKey = awtKey - java_awt_event_KeyEvent_VK_F1 + NSF1FunctionKey;
    } else if (awtKey >= java_awt_event_KeyEvent_VK_F13 && awtKey <= java_awt_event_KeyEvent_VK_F24) {
        // Support for F13-F24 came in Java 1.2 and are at a different range.
        macKey = awtKey - java_awt_event_KeyEvent_VK_F13 + NSF13FunctionKey;
    } else {
        // Special characters
        switch (awtKey) {
            case java_awt_event_KeyEvent_VK_BACK_QUOTE      : macKey = '`'; break;
            case java_awt_event_KeyEvent_VK_QUOTE           : macKey = '\''; break;

            case java_awt_event_KeyEvent_VK_ESCAPE          : macKey = 0x1B; break;
            case java_awt_event_KeyEvent_VK_SPACE           : macKey = ' '; break;
            case java_awt_event_KeyEvent_VK_PAGE_UP         : macKey = NSPageUpFunctionKey; break;
            case java_awt_event_KeyEvent_VK_PAGE_DOWN       : macKey = NSPageDownFunctionKey; break;
            case java_awt_event_KeyEvent_VK_END             : macKey = NSEndFunctionKey; break;
            case java_awt_event_KeyEvent_VK_HOME            : macKey = NSHomeFunctionKey; break;

            case java_awt_event_KeyEvent_VK_LEFT            : macKey = NSLeftArrowFunctionKey; break;
            case java_awt_event_KeyEvent_VK_UP              : macKey = NSUpArrowFunctionKey; break;
            case java_awt_event_KeyEvent_VK_RIGHT           : macKey = NSRightArrowFunctionKey; break;
            case java_awt_event_KeyEvent_VK_DOWN            : macKey = NSDownArrowFunctionKey; break;

            case java_awt_event_KeyEvent_VK_COMMA           : macKey = ','; break;

                // Mac OS doesn't distinguish between the two '-' keys...
            case java_awt_event_KeyEvent_VK_MINUS           :
            case java_awt_event_KeyEvent_VK_SUBTRACT        : macKey = '-'; break;

                // or the two '.' keys...
            case java_awt_event_KeyEvent_VK_DECIMAL         :
            case java_awt_event_KeyEvent_VK_PERIOD          : macKey = '.'; break;

                // or the two '/' keys.
            case java_awt_event_KeyEvent_VK_DIVIDE          :
            case java_awt_event_KeyEvent_VK_SLASH           : macKey = '/'; break;

            case java_awt_event_KeyEvent_VK_SEMICOLON       : macKey = ';'; break;
            case java_awt_event_KeyEvent_VK_EQUALS          : macKey = '='; break;

            case java_awt_event_KeyEvent_VK_OPEN_BRACKET    : macKey = '['; break;
            case java_awt_event_KeyEvent_VK_BACK_SLASH      : macKey = '\\'; break;
            case java_awt_event_KeyEvent_VK_CLOSE_BRACKET   : macKey = ']'; break;

            case java_awt_event_KeyEvent_VK_MULTIPLY        : macKey = '*'; break;
            case java_awt_event_KeyEvent_VK_ADD             : macKey = '+'; break;

            case java_awt_event_KeyEvent_VK_HELP            : macKey = NSHelpFunctionKey; break;
            case java_awt_event_KeyEvent_VK_TAB             : macKey = NSTabCharacter; break;
            case java_awt_event_KeyEvent_VK_ENTER           : macKey = NSNewlineCharacter; break;
            case java_awt_event_KeyEvent_VK_BACK_SPACE      : macKey = NSBackspaceCharacter; break;
            case java_awt_event_KeyEvent_VK_DELETE          : macKey = NSDeleteCharacter; break;
            case java_awt_event_KeyEvent_VK_CLEAR           : macKey = NSClearDisplayFunctionKey; break;
            case java_awt_event_KeyEvent_VK_AMPERSAND       : macKey = '&'; break;
            case java_awt_event_KeyEvent_VK_ASTERISK        : macKey = '*'; break;
            case java_awt_event_KeyEvent_VK_QUOTEDBL        : macKey = '\"'; break;
            case java_awt_event_KeyEvent_VK_LESS            : macKey = '<'; break;
            case java_awt_event_KeyEvent_VK_GREATER         : macKey = '>'; break;
            case java_awt_event_KeyEvent_VK_BRACELEFT       : macKey = '{'; break;
            case java_awt_event_KeyEvent_VK_BRACERIGHT      : macKey = '}'; break;
            case java_awt_event_KeyEvent_VK_AT              : macKey = '@'; break;
            case java_awt_event_KeyEvent_VK_COLON           : macKey = ':'; break;
            case java_awt_event_KeyEvent_VK_CIRCUMFLEX      : macKey = '^'; break;
            case java_awt_event_KeyEvent_VK_DOLLAR          : macKey = '$'; break;
            case java_awt_event_KeyEvent_VK_EXCLAMATION_MARK : macKey = '!'; break;
            case java_awt_event_KeyEvent_VK_LEFT_PARENTHESIS : macKey = '('; break;
            case java_awt_event_KeyEvent_VK_NUMBER_SIGN     : macKey = '#'; break;
            case java_awt_event_KeyEvent_VK_PLUS            : macKey = '+'; break;
            case java_awt_event_KeyEvent_VK_RIGHT_PARENTHESIS: macKey = ')'; break;
            case java_awt_event_KeyEvent_VK_UNDERSCORE      : macKey = '_'; break;
        }
    }
    return macKey;
}

/*
 * Class:     sun_lwawt_macosx_CMenuItem
 * Method:    nativeSetLabel
 * Signature: (JLjava/lang/String;CII)V
 */
JNIEXPORT void JNICALL
Java_sun_lwawt_macosx_CMenuItem_nativeSetLabel
(JNIEnv *env, jobject peer,
 jlong menuItemObj, jstring label,
 jchar shortcutKey, jint shortcutKeyCode, jint mods)
{
    JNI_COCOA_ENTER(env);
    NSString *theLabel = JavaStringToNSString(env, label);
    NSString *theKeyEquivalent = nil;
    unichar macKey = shortcutKey;

    if (macKey == 0) {
        macKey = AWTKeyToMacShortcut(shortcutKeyCode, (mods & java_awt_event_KeyEvent_SHIFT_MASK) != 0);
    }

    if (macKey != 0) {
        unichar equivalent[1] = {macKey};
        theKeyEquivalent = [NSString stringWithCharacters:equivalent length:1];
    } else {
        theKeyEquivalent = @"";
    }

    [((CMenuItem *)jlong_to_ptr(menuItemObj)) setJavaLabel:theLabel shortcut:theKeyEquivalent modifierMask:mods];
    JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CMenuItem
 * Method:    nativeSetTooltip
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_sun_lwawt_macosx_CMenuItem_nativeSetTooltip
(JNIEnv *env, jobject peer, jlong menuItemObj, jstring tooltip)
{
    JNI_COCOA_ENTER(env);
    NSString *theTooltip = JavaStringToNSString(env, tooltip);
    [((CMenuItem *)jlong_to_ptr(menuItemObj)) setJavaToolTipText:theTooltip];
    JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CMenuItem
 * Method:    nativeSetImage
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL
Java_sun_lwawt_macosx_CMenuItem_nativeSetImage
(JNIEnv *env, jobject peer, jlong menuItemObj, jlong image)
{
    JNI_COCOA_ENTER(env);
    [((CMenuItem *)jlong_to_ptr(menuItemObj)) setJavaImage:(NSImage*)jlong_to_ptr(image)];
    JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CMenuItem
 * Method:    nativeCreate
 * Signature: (JZ)J
 */
JNIEXPORT jlong JNICALL
Java_sun_lwawt_macosx_CMenuItem_nativeCreate
(JNIEnv *env, jobject peer, jlong parentCMenuObj, jboolean isSeparator)
{

    __block CMenuItem *aCMenuItem = nil;
    BOOL asSeparator = (isSeparator == JNI_TRUE) ? YES: NO;
    CMenu *parentCMenu = (CMenu *)jlong_to_ptr(parentCMenuObj);
    JNI_COCOA_ENTER(env);

    jobject cPeerObjGlobal = (*env)->NewGlobalRef(env, peer);

    [ThreadUtilities performOnMainThreadWaiting:YES block:^(){
        aCMenuItem = [[CMenuItem alloc] initWithPeer: cPeerObjGlobal
                                         asSeparator: asSeparator];
        // the CMenuItem is released in CMenuComponent.dispose()
    }];

    if (aCMenuItem == nil) {
        return 0L;
    }

    // and add it to the parent item.
    [parentCMenu addJavaMenuItem: aCMenuItem];

    // setLabel will be called after creation completes.

    JNI_COCOA_EXIT(env);
    return ptr_to_jlong(aCMenuItem);
}

/*
 * Class:     sun_lwawt_macosx_CMenuItem
 * Method:    nativeSetEnabled
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL
Java_sun_lwawt_macosx_CMenuItem_nativeSetEnabled
(JNIEnv *env, jobject peer, jlong menuItemObj, jboolean enable)
{
    JNI_COCOA_ENTER(env);
    CMenuItem *item = (CMenuItem *)jlong_to_ptr(menuItemObj);
    [item setJavaEnabled: (enable == JNI_TRUE)];
    JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CCheckboxMenuItem
 * Method:    nativeSetState
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL
Java_sun_lwawt_macosx_CCheckboxMenuItem_nativeSetState
(JNIEnv *env, jobject peer, jlong menuItemObj, jboolean state)
{
    JNI_COCOA_ENTER(env);
    CMenuItem *item = (CMenuItem *)jlong_to_ptr(menuItemObj);
    [item setJavaState: (state == JNI_TRUE)];
    JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CCheckboxMenuItem
 * Method:    nativeSetState
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL
Java_sun_lwawt_macosx_CCheckboxMenuItem_nativeSetIsCheckbox
(JNIEnv *env, jobject peer, jlong menuItemObj)
{
    JNI_COCOA_ENTER(env);
    CMenuItem *item = (CMenuItem *)jlong_to_ptr(menuItemObj);
    [item setIsCheckbox];
    JNI_COCOA_EXIT(env);
}
