/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#import "AppDelegate.h"

#include <LibGfx/Font/FontDatabase.h>

@interface AppDelegate ()
@property (strong) IBOutlet NSWindow* window;
@end

@implementation AppDelegate

- (void)applicationDidFinishLaunching:(NSNotification*)aNotification
{
    // FIXME: Copy the fonts to the bundle or something

    // Get from `Build/lagom/bin/MacPDF.app/Contents/MacOS/MacPDF` to `.`.
    NSString* source_root = [[NSBundle mainBundle] executablePath];
    for (int i = 0; i < 7; ++i)
        source_root = [source_root stringByDeletingLastPathComponent];
    auto source_root_string = DeprecatedString([source_root UTF8String]);
    Gfx::FontDatabase::set_default_fonts_lookup_path(DeprecatedString::formatted("{}/Base/res/fonts", source_root_string));
}

- (void)applicationWillTerminate:(NSNotification*)aNotification
{
}

- (BOOL)applicationSupportsSecureRestorableState:(NSApplication*)app
{
    return YES;
}

@end
