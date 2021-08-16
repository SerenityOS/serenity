/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

@class CMenuBar;

//
// This class supplies the native implementation for the com.apple.eawt.Application class.  We
// implement this as a delegate rather than extend NSApplication because we can not rely on AWT always
// being the creator of the NSApplication NSApp instance.
//
@interface ApplicationDelegate : NSObject<NSApplicationDelegate>
{
    NSMenuItem *fPreferencesMenu;
    NSMenuItem *fAboutMenu;

    NSMenu *fDockMenu;
    CMenuBar *fDefaultMenuBar;

    NSProgressIndicator *fProgressIndicator;

    BOOL fHandlesDocumentTypes;
    BOOL fHandlesURLTypes;
}

@property (nonatomic, retain) NSMenuItem *fPreferencesMenu;
@property (nonatomic, retain) NSMenuItem *fAboutMenu;

@property (nonatomic, retain) NSProgressIndicator *fProgressIndicator;

@property (nonatomic, retain) NSMenu *fDockMenu;
@property (nonatomic, retain) CMenuBar *fDefaultMenuBar;

// Returns the shared delegate, creating if necessary
+ (ApplicationDelegate *)sharedDelegate;

// called by the window machinery to setup a default menu bar
- (CMenuBar *)defaultMenuBar;

@end
