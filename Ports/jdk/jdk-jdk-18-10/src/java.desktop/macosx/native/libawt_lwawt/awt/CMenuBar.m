/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

#import "JNIUtilities.h"

#import <AppKit/AppKit.h>
#import <JavaRuntimeSupport/JavaRuntimeSupport.h>


#import "CMenuBar.h"
#import "CMenu.h"
#import "ThreadUtilities.h"
#import "ApplicationDelegate.h"

#import "sun_lwawt_macosx_CMenuBar.h"

__attribute__((visibility("default")))
NSString *CMenuBarDidReuseItemNotification =
    @"CMenuBarDidReuseItemNotification";

static CMenuBar *sActiveMenuBar = nil;
static NSMenu *sDefaultHelpMenu = nil;
static BOOL sSetupHelpMenu = NO;

@interface CMenuBar (CMenuBar_Private)
+ (void) addDefaultHelpMenu;
@end

@implementation CMenuBar

+ (void)clearMenuBarExcludingAppleMenu_OnAppKitThread:(BOOL) excludingAppleMenu {
    AWT_ASSERT_APPKIT_THREAD;
    // Remove all Java menus from the main bar.
    NSMenu *theMainMenu = [NSApp mainMenu];
    NSUInteger i, menuCount = [theMainMenu numberOfItems];

    for (i = menuCount; i > 1; i--) {
        NSUInteger index = i-1;

        NSMenuItem *currItem = [theMainMenu itemAtIndex:index];
        NSMenu *currMenu = [currItem submenu];

        if (excludingAppleMenu && ![currMenu isJavaMenu]) {
            continue;
        }
        [currItem setSubmenu:nil];
        [theMainMenu removeItemAtIndex:index];
    }

    [CMenuBar addDefaultHelpMenu];
}

+ (BOOL) isActiveMenuBar:(CMenuBar *)inMenuBar {
    return (sActiveMenuBar == inMenuBar);
}

- (id) initWithPeer:(jobject)peer {
    AWT_ASSERT_APPKIT_THREAD;
    self = [super initWithPeer: peer];
    if (self) {
        fMenuList = [[NSMutableArray alloc] init];
    }
    return self;
}

-(void) dealloc {
    [fMenuList release];
    fMenuList = nil;

    [fHelpMenu release];
    fHelpMenu = nil;

    [super dealloc];
}

+ (void) activate:(CMenuBar *)menubar modallyDisabled:(BOOL)modallyDisabled {
    AWT_ASSERT_APPKIT_THREAD;

    if (!menubar) {
        [CMenuBar clearMenuBarExcludingAppleMenu_OnAppKitThread:YES];
        return;
    }

#ifdef DEBUG
    NSLog(@"activating menu bar: %@", menubar);
#endif

    @synchronized([CMenuBar class]) {
        sActiveMenuBar = menubar;
    }

    @synchronized(menubar) {
        menubar->fModallyDisabled = modallyDisabled;
    }

    NSUInteger i = 0, newMenuListSize = [menubar->fMenuList count];

    NSMenu *theMainMenu = [NSApp mainMenu];
    NSUInteger menuIndex, menuCount = [theMainMenu numberOfItems];

    NSUInteger cmenuIndex = 0, cmenuCount = newMenuListSize;
    NSMutableArray *removedMenuArray = [NSMutableArray array];

    for (menuIndex = 0; menuIndex < menuCount; menuIndex++) {
        NSMenuItem *currItem = [theMainMenu itemAtIndex:menuIndex];
        NSMenu *currMenu = [currItem submenu];

        if ([currMenu isJavaMenu]) {
            // Ready to replace, find next candidate
            CMenu *newMenu = nil;
            if (cmenuIndex < cmenuCount) {
                newMenu = (CMenu *)[menubar->fMenuList objectAtIndex:cmenuIndex];
                if (newMenu == menubar->fHelpMenu) {
                    cmenuIndex++;
                    if (cmenuIndex < cmenuCount) {
                        newMenu = (CMenu *)[menubar->fMenuList objectAtIndex:cmenuIndex];
                    }
                }
            }
            if (newMenu) {
                NSMenu *menuToAdd = [newMenu menu];
                if ([theMainMenu indexOfItemWithSubmenu:menuToAdd] == -1) {
                    [[NSNotificationCenter defaultCenter] postNotificationName:CMenuBarDidReuseItemNotification object:theMainMenu];

                    [currItem setSubmenu:menuToAdd];
                    [currItem setTitle:[menuToAdd title]];
                    cmenuIndex++;
                }

                BOOL newEnabledState = [newMenu isEnabled] && !menubar->fModallyDisabled;
                [currItem setEnabled:newEnabledState];
            } else {
                [removedMenuArray addObject:[NSNumber numberWithInteger:menuIndex]];
            }
        }
    }

    // Clean up extra items
    NSUInteger removedIndex, removedCount = [removedMenuArray count];
    for (removedIndex=removedCount; removedIndex > 0; removedIndex--) {
        NSUInteger index = [[removedMenuArray objectAtIndex:(removedIndex-1)] integerValue];
        NSMenuItem *currItem = [theMainMenu itemAtIndex:index];
        [currItem setSubmenu:nil];
        [theMainMenu removeItemAtIndex:index];
    }

    i = cmenuIndex;

    // Add all of the menus in the menu list.
    for (; i < newMenuListSize; i++) {
        CMenu *newMenu = (CMenu *)[menubar->fMenuList objectAtIndex:i];

        if (newMenu != menubar->fHelpMenu) {
            NSArray *args = [NSArray arrayWithObjects:newMenu, [NSNumber numberWithInt:-1], nil];
            [menubar nativeAddMenuAtIndex_OnAppKitThread:args];
        }
    }

    // Add the help menu last.
    if (menubar->fHelpMenu) {
        NSArray *args = [NSArray arrayWithObjects:menubar->fHelpMenu, [NSNumber numberWithInt:-1], nil];
        [menubar nativeAddMenuAtIndex_OnAppKitThread:args];
    } else {
        [CMenuBar addDefaultHelpMenu];
    }
}

-(void) deactivate {
    AWT_ASSERT_APPKIT_THREAD;

    BOOL isDeactivated = NO;
    @synchronized([CMenuBar class]) {
        if (sActiveMenuBar == self) {
            sActiveMenuBar = nil;
            isDeactivated = YES;
        }
    }

    if (isDeactivated) {
#ifdef DEBUG
        NSLog(@"deactivating menu bar: %@", self);
#endif

        @synchronized(self) {
            fModallyDisabled = NO;
        }

        // In theory, this might cause flickering if the window gaining focus
        // has its own menu. However, I couldn't reproduce it on practice, so
        // perhaps this is a non issue.
        CMenuBar* defaultMenu = [[ApplicationDelegate sharedDelegate] defaultMenuBar];
        if (defaultMenu != nil) {
            [CMenuBar activate:defaultMenu modallyDisabled:NO];
        }
    }
}

-(void) javaAddMenu: (CMenu *)theMenu {
    @synchronized(self) {
        [fMenuList addObject: theMenu];
    }

    if (self == sActiveMenuBar) {
        NSArray *args = [[NSArray alloc] initWithObjects:theMenu, [NSNumber numberWithInt:-1], nil];
        [ThreadUtilities performOnMainThread:@selector(nativeAddMenuAtIndex_OnAppKitThread:) on:self withObject:args waitUntilDone:YES];
        [args release];
    }
}

// This method is a special case for use by the screen menu bar.
// See ScreenMenuBar.java -- used to implement setVisible(boolean) by
// removing or adding the menu from the current menu bar's list.
-(void) javaAddMenu: (CMenu *)theMenu atIndex:(jint)index {
    @synchronized(self) {
        if (index == -1){
            [fMenuList addObject:theMenu];
        }else{
            [fMenuList insertObject:theMenu atIndex:index];
        }
    }

    if (self == sActiveMenuBar) {
        NSArray *args = [[NSArray alloc] initWithObjects:theMenu, [NSNumber numberWithInt:index], nil];
        [ThreadUtilities performOnMainThread:@selector(nativeAddMenuAtIndex_OnAppKitThread:) on:self withObject:args waitUntilDone:YES];
        [args release];
    }
}

- (NSInteger) javaIndexToNSMenuIndex_OnAppKitThread:(jint)javaIndex {
    AWT_ASSERT_APPKIT_THREAD;
    NSInteger returnValue = -1;
    NSMenu *theMainMenu = [NSApp mainMenu];

    if (javaIndex == -1) {
        if (fHelpMenu) {
            returnValue = [theMainMenu indexOfItemWithSubmenu:[fHelpMenu menu]];
        }
    } else {
        CMenu *requestedMenu = [fMenuList objectAtIndex:javaIndex];

        if (requestedMenu == fHelpMenu) {
            returnValue = [theMainMenu indexOfItemWithSubmenu:[fHelpMenu menu]];
        } else {
            NSUInteger i, menuCount = [theMainMenu numberOfItems];
            jint currJavaMenuIndex = 0;
            for (i = 0; i < menuCount; i++) {
                NSMenuItem *currItem = [theMainMenu itemAtIndex:i];
                NSMenu *currMenu = [currItem submenu];

                if ([currMenu isJavaMenu]) {
                    if (javaIndex == currJavaMenuIndex) {
                        returnValue = i;
                        break;
                    }

                    currJavaMenuIndex++;
                }
            }
        }
    }

    return returnValue;
}

- (void) nativeAddMenuAtIndex_OnAppKitThread:(NSArray *)args {
    AWT_ASSERT_APPKIT_THREAD;
    CMenu *theNewMenu = (CMenu*)[args objectAtIndex:0];
    jint index = [(NSNumber*)[args objectAtIndex:1] intValue];
    NSApplication *theApp = [NSApplication sharedApplication];
    NSMenu *theMainMenu = [theApp mainMenu];
    NSMenu *menuToAdd = [theNewMenu menu];

    if ([theMainMenu indexOfItemWithSubmenu:menuToAdd] == -1) {
        NSMenuItem *newItem = [[NSMenuItem alloc] init];
        [newItem setSubmenu:[theNewMenu menu]];
        [newItem setTitle:[[theNewMenu menu] title]];

        NSInteger nsMenuIndex = [self javaIndexToNSMenuIndex_OnAppKitThread:index];

        if (nsMenuIndex == -1) {
            [theMainMenu addItem:newItem];
        } else {
            [theMainMenu insertItem:newItem atIndex:nsMenuIndex];
        }

        BOOL newEnabledState = [theNewMenu isEnabled] && !fModallyDisabled;
        [newItem setEnabled:newEnabledState];
        [newItem release];
    }
}

- (void) javaDeleteMenu: (jint)index {
    if (self == sActiveMenuBar) {
        [ThreadUtilities performOnMainThread:@selector(nativeDeleteMenu_OnAppKitThread:) on:self withObject:[NSNumber numberWithInt:index] waitUntilDone:YES];
    }

    @synchronized(self) {
        CMenu *menuToRemove = [fMenuList objectAtIndex:index];

        if (menuToRemove == fHelpMenu) {
            [fHelpMenu release];
            fHelpMenu = nil;
        }

        [fMenuList removeObjectAtIndex:index];
    }
}

- (void) nativeDeleteMenu_OnAppKitThread:(id)indexObj {
    AWT_ASSERT_APPKIT_THREAD;
    NSApplication *theApp = [NSApplication sharedApplication];
    NSMenu *theMainMenu = [theApp mainMenu];
    jint menuToRemove = [(NSNumber *)indexObj intValue];
    NSInteger nsMenuToRemove = [self javaIndexToNSMenuIndex_OnAppKitThread:menuToRemove];

    if (nsMenuToRemove != -1) {
        [theMainMenu removeItemAtIndex:nsMenuToRemove];
    }
}

- (void) javaSetHelpMenu:(CMenu *)theMenu {
    @synchronized(self) {
        [theMenu retain];
        [fHelpMenu release];
        fHelpMenu = theMenu;
    }
}

+ (void) addDefaultHelpMenu {
    AWT_ASSERT_APPKIT_THREAD;

    // Look for a help book tag. If it's there, add the help menu.
    @synchronized ([CMenuBar class]) {
        if (!sSetupHelpMenu) {
            if (sDefaultHelpMenu == nil) {
                // If we are embedded, don't make a help menu.
                // TODO(cpc): we don't have NSApplicationAWT yet...
                //if (![NSApp isKindOfClass:[NSApplicationAWT class]]) {
                //    sSetupHelpMenu = YES;
                //    return;
                //}

                // If the developer specified a NIB, don't make a help menu.
                // TODO(cpc): usingDefaultNib only defined on NSApplicationAWT
                //if (![NSApp usingDefaultNib]) {
                //    sSetupHelpMenu = YES;
                //    return;
                //}

            // TODO: not implemented
            }

            sSetupHelpMenu = YES;
        }
    }

    if (sDefaultHelpMenu) {
        NSMenu *theMainMenu = [NSApp mainMenu];

        if ([theMainMenu indexOfItemWithSubmenu:sDefaultHelpMenu] == -1) {
            // Since we're re-using this NSMenu, we need to clear its parent before
            // adding it to a new menu item, or else AppKit will complain.
            [sDefaultHelpMenu setSupermenu:nil];

            // Add the help menu to the main menu.
            NSMenuItem *newItem = [[NSMenuItem alloc] init];
            [newItem setSubmenu:sDefaultHelpMenu];
            [newItem setTitle:[sDefaultHelpMenu title]];
            [theMainMenu addItem:newItem];

            // Release it so the main menu owns it.
            [newItem release];
        }
    }
}

@end

/*
 * Class:     sun_lwawt_macosx_CMenuBar
 * Method:    nativeCreateMenuBar
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_sun_lwawt_macosx_CMenuBar_nativeCreateMenuBar
    (JNIEnv *env, jobject peer)
{
    __block CMenuBar *aCMenuBar = nil;
    JNI_COCOA_ENTER(env);

    jobject cPeerObjGlobal = (*env)->NewGlobalRef(env, peer);

    [ThreadUtilities performOnMainThreadWaiting:YES block:^(){

        aCMenuBar = [[CMenuBar alloc] initWithPeer:cPeerObjGlobal];
        // the aCMenuBar is released in CMenuComponent.dispose()
    }];
    if (aCMenuBar == nil) {
        return 0L;
    }

    JNI_COCOA_EXIT(env);
    return ptr_to_jlong(aCMenuBar);
}

/*
 * Class:     sun_lwawt_macosx_CMenuBar
 * Method:    nativeAddAtIndex
 * Signature: (JJI)V
 */
JNIEXPORT void JNICALL
Java_sun_lwawt_macosx_CMenuBar_nativeAddAtIndex
    (JNIEnv *env, jobject peer,
     jlong menuBarObject, jlong menuObject, jint index)
{
    JNI_COCOA_ENTER(env);
    // Remove the specified item.
    [((CMenuBar *) jlong_to_ptr(menuBarObject)) javaAddMenu:(CMenu *) jlong_to_ptr(menuObject) atIndex:index];
    JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CMenuBar
 * Method:    nativeDelMenu
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL
Java_sun_lwawt_macosx_CMenuBar_nativeDelMenu
    (JNIEnv *env, jobject peer, jlong menuBarObject, jint index)
{
    JNI_COCOA_ENTER(env);
    // Remove the specified item.
    [((CMenuBar *) jlong_to_ptr(menuBarObject)) javaDeleteMenu: index];
    JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CMenuBar
 * Method:    nativeSetHelpMenu
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL
Java_sun_lwawt_macosx_CMenuBar_nativeSetHelpMenu
    (JNIEnv *env, jobject peer, jlong menuBarObject, jlong menuObject)
{
    JNI_COCOA_ENTER(env);
    // Remove the specified item.
    [((CMenuBar *) jlong_to_ptr(menuBarObject)) javaSetHelpMenu: ((CMenu *)jlong_to_ptr(menuObject))];
    JNI_COCOA_EXIT(env);
}
