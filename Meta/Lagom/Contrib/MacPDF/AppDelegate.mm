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
    // FIXME: copy the fonts to the bundle or something
    auto source_root = DeprecatedString("/Users/thakis/src/serenity");
    Gfx::FontDatabase::set_default_fonts_lookup_path(DeprecatedString::formatted("{}/Base/res/fonts", source_root));
}

- (void)applicationWillTerminate:(NSNotification*)aNotification
{
}

- (BOOL)applicationSupportsSecureRestorableState:(NSApplication*)app
{
    return YES;
}

@end
