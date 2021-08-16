/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

#import "QueuingApplicationDelegate.h"

@interface NSBundle (EAWTOverrides)
- (BOOL)_hasEAWTOverride:(NSString *)key;
@end


@implementation NSBundle (EAWTOverrides)

- (BOOL)_hasEAWTOverride:(NSString *)key {
    return [[[[self objectForInfoDictionaryKey:@"Java"] objectForKey:@"EAWTOverride"] objectForKey:key] boolValue];
}

@end

@implementation QueuingApplicationDelegate

@synthesize realDelegate;
@synthesize queue;

+ (QueuingApplicationDelegate*) sharedDelegate
{
    static QueuingApplicationDelegate * qad = nil;

    if (!qad) {
        qad = [QueuingApplicationDelegate new];
    }

    return qad;
}

- (id) init
{
    self = [super init];
    if (!self) {
        return self;
    }

    self.queue = [NSMutableArray arrayWithCapacity: 0];

    // If the java application has a bundle with an Info.plist file with
    //  a CFBundleDocumentTypes entry, then it is set up to handle Open Doc
    //  and Print Doc commands for these files. Therefore java AWT will
    //  cache Open Doc and Print Doc events that are sent prior to a
    //  listener being installed by the client java application.
    NSBundle *bundle = [NSBundle mainBundle];
    fHandlesDocumentTypes = [bundle objectForInfoDictionaryKey:@"CFBundleDocumentTypes"] != nil || [bundle _hasEAWTOverride:@"DocumentHandler"];
    fHandlesURLTypes = [bundle objectForInfoDictionaryKey:@"CFBundleURLTypes"] != nil || [bundle _hasEAWTOverride:@"URLHandler"];
    if (fHandlesURLTypes) {
        [[NSAppleEventManager sharedAppleEventManager] setEventHandler:self
                                                           andSelector:@selector(_handleOpenURLEvent:withReplyEvent:)
                                                         forEventClass:kInternetEventClass
                                                            andEventID:kAEGetURL];
    }

    NSNotificationCenter *ctr = [NSNotificationCenter defaultCenter];
    [ctr addObserver:self selector:@selector(_willFinishLaunching) name:NSApplicationWillFinishLaunchingNotification object:nil];
    [ctr addObserver:self selector:@selector(_systemWillPowerOff) name:NSWorkspaceWillPowerOffNotification object:nil];
    [ctr addObserver:self selector:@selector(_appDidActivate) name:NSApplicationDidBecomeActiveNotification object:nil];
    [ctr addObserver:self selector:@selector(_appDidDeactivate) name:NSApplicationDidResignActiveNotification object:nil];
    [ctr addObserver:self selector:@selector(_appDidHide) name:NSApplicationDidHideNotification object:nil];
    [ctr addObserver:self selector:@selector(_appDidUnhide) name:NSApplicationDidUnhideNotification object:nil];

    return self;
}

- (void)dealloc
{
    if (fHandlesURLTypes) {
        [[NSAppleEventManager sharedAppleEventManager] removeEventHandlerForEventClass: kInternetEventClass andEventID:kAEGetURL];
    }

    NSNotificationCenter *ctr = [NSNotificationCenter defaultCenter];
    Class clz = [QueuingApplicationDelegate class];
    [ctr removeObserver:clz];

    self.queue = nil;
    self.realDelegate = nil;

    [super dealloc];
}


- (void)_handleOpenURLEvent:(NSAppleEventDescriptor *)openURLEvent withReplyEvent:(NSAppleEventDescriptor *)replyEvent
{
    // Make an explicit copy of the passed events as they may be invalidated by the time they're processed
    NSAppleEventDescriptor *openURLEventCopy = [openURLEvent copy];
    NSAppleEventDescriptor *replyEventCopy = [replyEvent copy];

    [self.queue addObject:[^(){
        [self.realDelegate _handleOpenURLEvent:openURLEventCopy withReplyEvent:replyEventCopy];
        [openURLEventCopy release];
        [replyEventCopy release];
    } copy]];
}

- (void)application:(NSApplication *)theApplication openFiles:(NSArray *)fileNames
{
    [self.queue addObject:[^(){
        [self.realDelegate application:theApplication openFiles:fileNames];
    } copy]];
}

- (NSApplicationPrintReply)application:(NSApplication *)application printFiles:(NSArray *)fileNames withSettings:(NSDictionary *)printSettings showPrintPanels:(BOOL)showPrintPanels
{
    if (!fHandlesDocumentTypes) {
        return NSPrintingCancelled;
    }

    [self.queue addObject:[^(){
        [self.realDelegate application:application printFiles:fileNames withSettings:printSettings showPrintPanels:showPrintPanels];
    } copy]];

    // well, a bit premature, but what else can we do?..
    return NSPrintingSuccess;
}

- (void)_willFinishLaunching
{
    [self.queue addObject:[^(){
        [[self.realDelegate class] _willFinishLaunching];
    } copy]];
}

- (BOOL)applicationShouldHandleReopen:(NSApplication *)theApplication hasVisibleWindows:(BOOL)flag
{
    [self.queue addObject:[^(){
        [self.realDelegate applicationShouldHandleReopen:theApplication hasVisibleWindows:flag];
    } copy]];
    return YES;
}

- (NSApplicationTerminateReply)applicationShouldTerminate:(NSApplication *)app
{
    [self.queue addObject:[^(){
        [self.realDelegate applicationShouldTerminate:app];
    } copy]];
    return NSTerminateLater;
}

- (void)_systemWillPowerOff
{
    [self.queue addObject:[^(){
        [[self.realDelegate class] _systemWillPowerOff];
    } copy]];
}

- (void)_appDidActivate
{
    [self.queue addObject:[^(){
        [[self.realDelegate class] _appDidActivate];
    } copy]];
}

- (void)_appDidDeactivate
{
    [self.queue addObject:[^(){
        [[self.realDelegate class] _appDidDeactivate];
    } copy]];
}

- (void)_appDidHide
{
    [self.queue addObject:[^(){
        [[self.realDelegate class] _appDidHide];
    } copy]];
}

- (void)_appDidUnhide
{
    [self.queue addObject:[^(){
        [[self.realDelegate class] _appDidUnhide];
    } copy]];
}

- (void)processQueuedEventsWithTargetDelegate:(id <NSApplicationDelegate>)delegate
{
    self.realDelegate = delegate;

    NSUInteger i;
    NSUInteger count = [self.queue count];

    for (i = 0; i < count; i++) {
        void (^event)() = (void (^)())[self.queue objectAtIndex: i];
        event();
        [event release];
    }

    [self.queue removeAllObjects];
}

@end

