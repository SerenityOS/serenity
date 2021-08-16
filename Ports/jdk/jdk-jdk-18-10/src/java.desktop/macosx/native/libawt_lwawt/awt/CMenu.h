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

#import "CMenuItem.h"

extern NSString *JAVA_MENU_NAME;

@interface CMenu : CMenuItem {
    NSMenu *fMenu;
}

// Initializers
- (id)initWithPeer:(jobject)peer;

- (void)setNativeMenuTitle_OnAppKitThread:(NSString *)title;
- (void)addNativeItem_OnAppKitThread:(CMenuItem *)itemModified;
- (void)deleteNativeJavaItem_OnAppKitThread:(NSNumber *)number;
- (void)setNativeEnabled_OnAppKitThread:(NSNumber *)boolNumber;

// Actions
- (void)addJavaSubmenu:(CMenu *)submenu;
- (void)addJavaMenuItem:(CMenuItem *)newItem;
- (void)setJavaMenuTitle:(NSString *)title;
- (void)deleteJavaItem:(jint)index;
- (void)addNSMenuItemToMenu:(NSMenu *)inMenu;

// Accessors
- (NSMenu *)menu;
@end
