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

#import "NSApplicationAWT.h"

#import <objc/runtime.h>
#import <JavaRuntimeSupport/JavaRuntimeSupport.h>

#import "PropertiesUtilities.h"
#import "ThreadUtilities.h"
#import "QueuingApplicationDelegate.h"
#import "AWTIconData.h"

/*
 * Declare library specific JNI_Onload entry if static build
 */
DEF_STATIC_JNI_OnLoad

static BOOL sUsingDefaultNIB = YES;
static NSString *SHARED_FRAMEWORK_BUNDLE = @"/System/Library/Frameworks/JavaVM.framework";
static id <NSApplicationDelegate> applicationDelegate = nil;
static QueuingApplicationDelegate * qad = nil;

// Flag used to indicate to the Plugin2 event synthesis code to do a postEvent instead of sendEvent
BOOL postEventDuringEventSynthesis = NO;

/**
 * Subtypes of NSApplicationDefined, which are used for custom events.
 */
enum {
    ExecuteBlockEvent = 777, NativeSyncQueueEvent
};

@implementation NSApplicationAWT

- (id) init
{
    // Headless: NO
    // Embedded: NO
    // Multiple Calls: NO
    //  Caller: +[NSApplication sharedApplication]

AWT_ASSERT_APPKIT_THREAD;
    fApplicationName = nil;
    dummyEventTimestamp = 0.0;
    seenDummyEventLock = nil;


    // NSApplication will call _RegisterApplication with the application's bundle, but there may not be one.
    // So, we need to call it ourselves to ensure the app is set up properly.
    [self registerWithProcessManager];

    return [super init];
}

- (void)dealloc
{
    [fApplicationName release];
    fApplicationName = nil;

    [super dealloc];
}

- (void)finishLaunching
{
AWT_ASSERT_APPKIT_THREAD;

    JNIEnv *env = [ThreadUtilities getJNIEnv];

    SEL appearanceSel = @selector(setAppearance:); // macOS 10.14+
    if ([self respondsToSelector:appearanceSel]) {
        NSString *appearanceProp = [PropertiesUtilities
                javaSystemPropertyForKey:@"apple.awt.application.appearance"
                                 withEnv:env];
        if (![@"system" isEqual:appearanceProp]) {
            // by default use light mode, because dark mode is not supported yet
            NSAppearance *appearance = [NSAppearance appearanceNamed:NSAppearanceNameAqua];
            if (appearanceProp != nil) {
                NSAppearance *requested = [NSAppearance appearanceNamed:appearanceProp];
                if (requested != nil) {
                    appearance = requested;
                }
            }
            // [self setAppearance:appearance];
            [self performSelector:appearanceSel withObject:appearance];
        }
    }

    // Get default nib file location
    // NOTE: This should learn about the current java.version. Probably best thru
    //  the Makefile system's -DFRAMEWORK_VERSION define. Need to be able to pass this
    //  thru to PB from the Makefile system and for local builds.
    NSString *defaultNibFile = [PropertiesUtilities javaSystemPropertyForKey:@"apple.awt.application.nib" withEnv:env];
    if (!defaultNibFile) {
        NSBundle *javaBundle = [NSBundle bundleWithPath:SHARED_FRAMEWORK_BUNDLE];
        defaultNibFile = [javaBundle pathForResource:@"DefaultApp" ofType:@"nib"];
    } else {
        sUsingDefaultNIB = NO;
    }

    [NSBundle loadNibFile:defaultNibFile externalNameTable: [NSDictionary dictionaryWithObject:self forKey:@"NSOwner"] withZone:nil];

    // Set user defaults to not try to parse application arguments.
    NSUserDefaults * defs = [NSUserDefaults standardUserDefaults];
    NSDictionary * noOpenDict = [NSDictionary dictionaryWithObject:@"NO" forKey:@"NSTreatUnknownArgumentsAsOpen"];
    [defs registerDefaults:noOpenDict];

    // Fix up the dock icon now that we are registered with CAS and the Dock.
    [self setDockIconWithEnv:env];

    // If we are using our nib (the default application NIB) we need to put the app name into
    // the application menu, which has placeholders for the name.
    if (sUsingDefaultNIB) {
        NSUInteger i, itemCount;
        NSMenu *theMainMenu = [NSApp mainMenu];

        // First submenu off the main menu is the application menu.
        NSMenuItem *appMenuItem = [theMainMenu itemAtIndex:0];
        NSMenu *appMenu = [appMenuItem submenu];
        itemCount = [appMenu numberOfItems];

        for (i = 0; i < itemCount; i++) {
            NSMenuItem *anItem = [appMenu itemAtIndex:i];
            NSString *oldTitle = [anItem title];
            [anItem setTitle:[NSString stringWithFormat:oldTitle, fApplicationName]];
        }
    }

    if (applicationDelegate) {
        [self setDelegate:applicationDelegate];
    } else {
        qad = [QueuingApplicationDelegate sharedDelegate];
        [self setDelegate:qad];
    }

    [super finishLaunching];
}


- (void) registerWithProcessManager
{
    // Headless: NO
    // Embedded: NO
    // Multiple Calls: NO
    //  Caller: -[NSApplicationAWT init]

AWT_ASSERT_APPKIT_THREAD;
    JNIEnv *env = [ThreadUtilities getJNIEnv];

    char envVar[80];

    // The following environment variable is set from the -Xdock:name param. It should be UTF8.
    snprintf(envVar, sizeof(envVar), "APP_NAME_%d", getpid());
    char *appName = getenv(envVar);
    if (appName != NULL) {
        fApplicationName = [NSString stringWithUTF8String:appName];
        unsetenv(envVar);
    }

    // If it wasn't specified as an argument, see if it was specified as a system property.
    if (fApplicationName == nil) {
        fApplicationName = [PropertiesUtilities javaSystemPropertyForKey:@"apple.awt.application.name" withEnv:env];
    }

    // If we STILL don't have it, the app name is retrieved from an environment variable (set in java.c) It should be UTF8.
    if (fApplicationName == nil) {
        char mainClassEnvVar[80];
        snprintf(mainClassEnvVar, sizeof(mainClassEnvVar), "JAVA_MAIN_CLASS_%d", getpid());
        char *mainClass = getenv(mainClassEnvVar);
        if (mainClass != NULL) {
            fApplicationName = [NSString stringWithUTF8String:mainClass];
            unsetenv(mainClassEnvVar);

            NSRange lastPeriod = [fApplicationName rangeOfString:@"." options:NSBackwardsSearch];
            if (lastPeriod.location != NSNotFound) {
                fApplicationName = [fApplicationName substringFromIndex:lastPeriod.location + 1];
            }
        }
    }

    // The dock name is nil for double-clickable Java apps (bundled and Web Start apps)
    // When that happens get the display name, and if that's not available fall back to
    // CFBundleName.
    NSBundle *mainBundle = [NSBundle mainBundle];
    if (fApplicationName == nil) {
        fApplicationName = (NSString *)[mainBundle objectForInfoDictionaryKey:@"CFBundleDisplayName"];

        if (fApplicationName == nil) {
            fApplicationName = (NSString *)[mainBundle objectForInfoDictionaryKey:(NSString *)kCFBundleNameKey];

            if (fApplicationName == nil) {
                fApplicationName = (NSString *)[mainBundle objectForInfoDictionaryKey: (NSString *)kCFBundleExecutableKey];

                if (fApplicationName == nil) {
                    // Name of last resort is the last part of the applicatoin name without the .app (consistent with CopyProcessName)
                    fApplicationName = [[mainBundle bundlePath] lastPathComponent];

                    if ([fApplicationName hasSuffix:@".app"]) {
                        fApplicationName = [fApplicationName stringByDeletingPathExtension];
                    }
                }
            }
        }
    }

    // We're all done trying to determine the app name.  Hold on to it.
    [fApplicationName retain];

    NSDictionary *registrationOptions = [NSMutableDictionary dictionaryWithObject:fApplicationName forKey:@"JRSAppNameKey"];

    NSString *launcherType = [PropertiesUtilities javaSystemPropertyForKey:@"sun.java.launcher" withEnv:env];
    if ([@"SUN_STANDARD" isEqualToString:launcherType]) {
        [registrationOptions setValue:[NSNumber numberWithBool:YES] forKey:@"JRSAppIsCommandLineKey"];
    }

    NSString *uiElementProp = [PropertiesUtilities javaSystemPropertyForKey:@"apple.awt.UIElement" withEnv:env];
    if ([@"true" isCaseInsensitiveLike:uiElementProp]) {
        [registrationOptions setValue:[NSNumber numberWithBool:YES] forKey:@"JRSAppIsUIElementKey"];
    }

    NSString *backgroundOnlyProp = [PropertiesUtilities javaSystemPropertyForKey:@"apple.awt.BackgroundOnly" withEnv:env];
    if ([@"true" isCaseInsensitiveLike:backgroundOnlyProp]) {
        [registrationOptions setValue:[NSNumber numberWithBool:YES] forKey:@"JRSAppIsBackgroundOnlyKey"];
    }

    // TODO replace with direct call
    // [JRSAppKitAWT registerAWTAppWithOptions:registrationOptions];
    // and remove below transform/activate/run hack

    id jrsAppKitAWTClass = objc_getClass("JRSAppKitAWT");
    SEL registerSel = @selector(registerAWTAppWithOptions:);
    if ([jrsAppKitAWTClass respondsToSelector:registerSel]) {
        [jrsAppKitAWTClass performSelector:registerSel withObject:registrationOptions];
        return;
    }

// HACK BEGIN
    // The following is necessary to make the java process behave like a
    // proper foreground application...
    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
        ProcessSerialNumber psn;
        GetCurrentProcess(&psn);
        TransformProcessType(&psn, kProcessTransformToForegroundApplication);

        [NSApp activateIgnoringOtherApps:YES];
        [NSApp run];
    }];
// HACK END
}

- (void) setDockIconWithEnv:(JNIEnv *)env {
    NSString *theIconPath = nil;

    // The following environment variable is set in java.c. It is probably UTF8.
    char envVar[80];
    snprintf(envVar, sizeof(envVar), "APP_ICON_%d", getpid());
    char *appIcon = getenv(envVar);
    if (appIcon != NULL) {
        theIconPath = [NSString stringWithUTF8String:appIcon];
        unsetenv(envVar);
    }

    if (theIconPath == nil) {
        theIconPath = [PropertiesUtilities javaSystemPropertyForKey:@"apple.awt.application.icon" withEnv:env];
    }

    // Use the path specified to get the icon image
    NSImage* iconImage = nil;
    if (theIconPath != nil) {
        iconImage = [[NSImage alloc] initWithContentsOfFile:theIconPath];
    }

    // If no icon file was specified or we failed to get the icon image
    // and there is no bundle's icon, then use the default icon
    if (iconImage == nil) {
        NSString* bundleIcon = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleIconFile"];
        if (bundleIcon == nil) {
            NSData* iconData;
            iconData = [[NSData alloc] initWithBytesNoCopy: sAWTIconData length: sizeof(sAWTIconData) freeWhenDone: NO];
            iconImage = [[NSImage alloc] initWithData: iconData];
            [iconData release];
        }
    }

    // Set up the dock icon if we have an icon image.
    if (iconImage != nil) {
        [NSApp setApplicationIconImage:iconImage];
        [iconImage release];
    }
}

+ (void) runAWTLoopWithApp:(NSApplication*)app {
    NSAutoreleasePool *pool = [NSAutoreleasePool new];

    // Make sure that when we run in javaRunLoopMode we don't exit randomly
    [[NSRunLoop currentRunLoop] addPort:[NSPort port] forMode:[ThreadUtilities javaRunLoopMode]];

    do {
        @try {
            [app run];
        } @catch (NSException* e) {
            NSLog(@"Apple AWT Startup Exception: %@", [e description]);
            NSLog(@"Apple AWT Startup Exception callstack: %@", [e callStackSymbols]);
            NSLog(@"Apple AWT Restarting Native Event Thread");

            [app stop:app];
        }
    } while (YES);

    [pool drain];
}

- (BOOL)usingDefaultNib {
    return sUsingDefaultNIB;
}

- (void)orderFrontStandardAboutPanelWithOptions:(NSDictionary *)optionsDictionary {
    if (!optionsDictionary) {
        optionsDictionary = [NSMutableDictionary dictionaryWithCapacity:2];
        [optionsDictionary setValue:[[[[[NSApp mainMenu] itemAtIndex:0] submenu] itemAtIndex:0] title] forKey:@"ApplicationName"];
        if (![NSImage imageNamed:@"NSApplicationIcon"]) {
            [optionsDictionary setValue:[NSApp applicationIconImage] forKey:@"ApplicationIcon"];
        }
    }

    [super orderFrontStandardAboutPanelWithOptions:optionsDictionary];
}

#define DRAGMASK (NSMouseMovedMask | NSLeftMouseDraggedMask | NSRightMouseDownMask | NSRightMouseDraggedMask | NSLeftMouseUpMask | NSRightMouseUpMask | NSFlagsChangedMask | NSKeyDownMask)

#if defined(MAC_OS_X_VERSION_10_12) && __LP64__
   // 10.12 changed `mask` to NSEventMask (unsigned long long) for x86_64 builds.
- (NSEvent *)nextEventMatchingMask:(NSEventMask)mask
#else
- (NSEvent *)nextEventMatchingMask:(NSUInteger)mask
#endif
untilDate:(NSDate *)expiration inMode:(NSString *)mode dequeue:(BOOL)deqFlag {
    if (mask == DRAGMASK && [((NSString *)kCFRunLoopDefaultMode) isEqual:mode]) {
        postEventDuringEventSynthesis = YES;
    }

    NSEvent *event = [super nextEventMatchingMask:mask untilDate:expiration inMode:mode dequeue: deqFlag];
    postEventDuringEventSynthesis = NO;

    return event;
}

// NSTimeInterval has microseconds precision
#define TS_EQUAL(ts1, ts2) (fabs((ts1) - (ts2)) < 1e-6)

- (void)sendEvent:(NSEvent *)event
{
    if ([event type] == NSApplicationDefined
            && TS_EQUAL([event timestamp], dummyEventTimestamp)
            && (short)[event subtype] == NativeSyncQueueEvent
            && [event data1] == NativeSyncQueueEvent
            && [event data2] == NativeSyncQueueEvent) {
        [seenDummyEventLock lockWhenCondition:NO];
        [seenDummyEventLock unlockWithCondition:YES];
    } else if ([event type] == NSApplicationDefined
               && (short)[event subtype] == ExecuteBlockEvent
               && [event data1] != 0 && [event data2] == ExecuteBlockEvent) {
        void (^block)() = (void (^)()) [event data1];
        block();
        [block release];
    } else if ([event type] == NSKeyUp && ([event modifierFlags] & NSCommandKeyMask)) {
        // Cocoa won't send us key up event when releasing a key while Cmd is down,
        // so we have to do it ourselves.
        [[self keyWindow] sendEvent:event];
    } else {
        [super sendEvent:event];
    }
}

/*
 * Posts the block to the AppKit event queue which will be executed
 * on the main AppKit loop.
 * While running nested loops this event will be ignored.
 */
- (void)postRunnableEvent:(void (^)())block
{
    void (^copy)() = [block copy];
    NSInteger encode = (NSInteger) copy;
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSEvent* event = [NSEvent otherEventWithType: NSApplicationDefined
                                        location: NSMakePoint(0,0)
                                   modifierFlags: 0
                                       timestamp: 0
                                    windowNumber: 0
                                         context: nil
                                         subtype: ExecuteBlockEvent
                                           data1: encode
                                           data2: ExecuteBlockEvent];

    [NSApp postEvent: event atStart: NO];
    [pool drain];
}

- (void)postDummyEvent:(bool)useCocoa {
    seenDummyEventLock = [[NSConditionLock alloc] initWithCondition:NO];
    dummyEventTimestamp = [NSProcessInfo processInfo].systemUptime;

    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSEvent* event = [NSEvent otherEventWithType: NSApplicationDefined
                                        location: NSMakePoint(0,0)
                                   modifierFlags: 0
                                       timestamp: dummyEventTimestamp
                                    windowNumber: 0
                                         context: nil
                                         subtype: NativeSyncQueueEvent
                                           data1: NativeSyncQueueEvent
                                           data2: NativeSyncQueueEvent];
    if (useCocoa) {
        [NSApp postEvent:event atStart:NO];
    } else {
        ProcessSerialNumber psn;
        GetCurrentProcess(&psn);
        CGEventPostToPSN(&psn, [event CGEvent]);
    }
    [pool drain];
}

- (void)waitForDummyEvent:(double)timeout {
    bool unlock = true;
    if (timeout >= 0) {
        double sec = timeout / 1000;
        unlock = [seenDummyEventLock lockWhenCondition:YES
                               beforeDate:[NSDate dateWithTimeIntervalSinceNow:sec]];
    } else {
        [seenDummyEventLock lockWhenCondition:YES];
    }
    if (unlock) {
        [seenDummyEventLock unlock];
    }
    [seenDummyEventLock release];

    seenDummyEventLock = nil;
}

@end


void OSXAPP_SetApplicationDelegate(id <NSApplicationDelegate> newdelegate)
{
AWT_ASSERT_APPKIT_THREAD;
    applicationDelegate = newdelegate;

    if (NSApp != nil) {
        [NSApp setDelegate: applicationDelegate];

        if (applicationDelegate && qad) {
            [qad processQueuedEventsWithTargetDelegate: applicationDelegate];
            qad = nil;
        }
    }
}
