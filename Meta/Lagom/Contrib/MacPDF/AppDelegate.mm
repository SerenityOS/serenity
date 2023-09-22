//
//  AppDelegate.m
//  SerenityPDF
//
//  Created by Nico Weber on 7/22/23.
//

#import "AppDelegate.h"

#include <LibGfx/Font/FontDatabase.h>

@interface AppDelegate ()

@property (strong) IBOutlet NSWindow* window;
@end

@implementation AppDelegate

- (void)applicationDidFinishLaunching:(NSNotification*)aNotification
{
    // Insert code here to initialize your application
    // FIXME: copy the fonts to the bundle or something
    auto source_root = DeprecatedString("/Users/thakis/src/serenity");
    Gfx::FontDatabase::set_default_fonts_lookup_path(DeprecatedString::formatted("{}/Base/res/fonts", source_root));
}

- (void)applicationWillTerminate:(NSNotification*)aNotification
{
    // Insert code here to tear down your application
}

- (BOOL)applicationSupportsSecureRestorableState:(NSApplication*)app
{
    return YES;
}

@end
