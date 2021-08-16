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

#import <JavaRuntimeSupport/JavaRuntimeSupport.h>


#import "CMenu.h"
#import "CMenuBar.h"
#import "ThreadUtilities.h"
#import "JNIUtilities.h"

#import "sun_lwawt_macosx_CMenu.h"

@implementation CMenu

- (id)initWithPeer:(jobject)peer {
AWT_ASSERT_APPKIT_THREAD;
    // Create the new NSMenu
    self = [super initWithPeer:peer asSeparator:NO];
    if (self) {
        fMenu = [NSMenu javaMenuWithTitle:@""];
        [fMenu retain];
        [fMenu setAutoenablesItems:NO];
    }
    return self;
}

- (void)dealloc {
    [fMenu release];
    fMenu = nil;
    [super dealloc];
}

- (void)addJavaSubmenu:(CMenu *)submenu {
    [ThreadUtilities performOnMainThread:@selector(addNativeItem_OnAppKitThread:) on:self withObject:submenu waitUntilDone:YES];
}

- (void)addJavaMenuItem:(CMenuItem *)theMenuItem {
    [ThreadUtilities performOnMainThread:@selector(addNativeItem_OnAppKitThread:) on:self withObject:theMenuItem waitUntilDone:YES];
}

- (void)addNativeItem_OnAppKitThread:(CMenuItem *)itemModified {
AWT_ASSERT_APPKIT_THREAD;
    [itemModified addNSMenuItemToMenu:[self menu]];
}

- (void)setJavaMenuTitle:(NSString *)title {

    if (title) {
        [ThreadUtilities performOnMainThread:@selector(setNativeMenuTitle_OnAppKitThread:) on:self withObject:title waitUntilDone:YES];
    }
}

- (void)setNativeMenuTitle_OnAppKitThread:(NSString *)title {
AWT_ASSERT_APPKIT_THREAD;

    [fMenu setTitle:title];
    // If we are a submenu we need to set our name in the parent menu's menu item.
    NSMenu *parent = [fMenu supermenu];
    if (parent) {
        NSInteger index = [parent indexOfItemWithSubmenu:fMenu];
        NSMenuItem *menuItem = [parent itemAtIndex:index];
        [menuItem setTitle:title];
    }
}

- (void)deleteJavaItem:(jint)index {

    [ThreadUtilities performOnMainThread:@selector(deleteNativeJavaItem_OnAppKitThread:) on:self withObject:[NSNumber numberWithInt:index] waitUntilDone:YES];
}

- (void)deleteNativeJavaItem_OnAppKitThread:(NSNumber *)number {
AWT_ASSERT_APPKIT_THREAD;

    int n = [number intValue];
    if (n < [[self menu] numberOfItems]) {
        [[self menu] removeItemAtIndex:n];
    }
}

- (void)addNSMenuItemToMenu:(NSMenu *)inMenu {
    if (fMenuItem == nil) return;
    [fMenuItem setSubmenu:fMenu];
    [inMenu addItem:fMenuItem];
}

- (NSMenu *)menu {
    return [[fMenu retain] autorelease];
}

- (void)setNativeEnabled_OnAppKitThread:(NSNumber *)boolNumber {
AWT_ASSERT_APPKIT_THREAD;

    @synchronized(self) {
        fIsEnabled = [boolNumber boolValue];

        NSMenu* supermenu = [fMenu supermenu];
        [[supermenu itemAtIndex:[supermenu indexOfItemWithSubmenu:fMenu]] setEnabled:fIsEnabled];
    }
}

- (NSString *)description {
    return [NSString stringWithFormat:@"CMenu[ %@ ]", fMenu];
}

@end

CMenu * createCMenu (jobject cPeerObjGlobal) {

    __block CMenu *aCMenu = nil;

    [ThreadUtilities performOnMainThreadWaiting:YES block:^(){

        aCMenu = [[CMenu alloc] initWithPeer:cPeerObjGlobal];
        // the aCMenu is released in CMenuComponent.dispose()
    }];

    if (aCMenu == nil) {
        return 0L;
    }

    return aCMenu;

}

/*
 * Class:     sun_lwawt_macosx_CMenu
 * Method:    nativeCreateSubMenu
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_sun_lwawt_macosx_CMenu_nativeCreateSubMenu
(JNIEnv *env, jobject peer, jlong parentMenu)
{
    CMenu *aCMenu = nil;
JNI_COCOA_ENTER(env);

    jobject cPeerObjGlobal = (*env)->NewGlobalRef(env, peer);

    aCMenu = createCMenu (cPeerObjGlobal);

    // Add it to the parent menu
    [((CMenu *)jlong_to_ptr(parentMenu)) addJavaSubmenu: aCMenu];

JNI_COCOA_EXIT(env);

    return ptr_to_jlong(aCMenu);
}



/*
 * Class:     sun_lwawt_macosx_CMenu
 * Method:    nativeCreateMenu
 * Signature: (JZ)J
 */
JNIEXPORT jlong JNICALL
Java_sun_lwawt_macosx_CMenu_nativeCreateMenu
(JNIEnv *env, jobject peer,
        jlong parentMenuBar, jboolean isHelpMenu, jint insertLocation)
{
    CMenu *aCMenu = nil;
    CMenuBar *parent = (CMenuBar *)jlong_to_ptr(parentMenuBar);
JNI_COCOA_ENTER(env);

    jobject cPeerObjGlobal = (*env)->NewGlobalRef(env, peer);

    aCMenu = createCMenu (cPeerObjGlobal);

    // Add it to the menu bar.
    [parent javaAddMenu:aCMenu atIndex:insertLocation];

    // If the menu is already the help menu (because we are creating an entire
    // menu bar) we need to note that now, because we can't rely on
    // setHelpMenu() being called again.
    if (isHelpMenu == JNI_TRUE) {
        [parent javaSetHelpMenu: aCMenu];
    }

JNI_COCOA_EXIT(env);
    return ptr_to_jlong(aCMenu);
}


/*
 * Class:     sun_lwawt_macosx_CMenu
 * Method:    nativeSetMenuTitle
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_sun_lwawt_macosx_CMenu_nativeSetMenuTitle
(JNIEnv *env, jobject peer, jlong menuObject, jstring label)
{
JNI_COCOA_ENTER(env);
    // Set the menu's title.
    [((CMenu *)jlong_to_ptr(menuObject)) setJavaMenuTitle:JavaStringToNSString(env, label)];
JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CMenu
 * Method:    nativeDeleteItem
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL
Java_sun_lwawt_macosx_CMenu_nativeDeleteItem
(JNIEnv *env, jobject peer, jlong menuObject, jint index)
{
JNI_COCOA_ENTER(env);
    // Remove the specified item.
    [((CMenu *)jlong_to_ptr(menuObject)) deleteJavaItem: index];
JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CMenu
 * Method:    nativeGetNSMenu
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_sun_lwawt_macosx_CMenu_nativeGetNSMenu
(JNIEnv *env, jobject peer, jlong menuObject)
{
    NSMenu* nsMenu = NULL;

JNI_COCOA_ENTER(env);
    // Strong retain this menu; it'll get released in Java_apple_laf_ScreenMenu_addMenuListeners
    nsMenu = [[((CMenu *)jlong_to_ptr(menuObject)) menu] retain];
JNI_COCOA_EXIT(env);

    return ptr_to_jlong(nsMenu);
}
