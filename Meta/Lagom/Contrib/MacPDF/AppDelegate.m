//
//  AppDelegate.m
//  SerenityPDF
//
//  Created by Nico Weber on 7/22/23.
//

#import "AppDelegate.h"

@interface AppDelegate ()

@property (strong) IBOutlet NSWindow* window;
@end

@implementation AppDelegate

- (void)applicationDidFinishLaunching:(NSNotification*)aNotification
{
    // Insert code here to initialize your application
}

- (void)applicationWillTerminate:(NSNotification*)aNotification
{
    // Insert code here to tear down your application
}

- (BOOL)applicationSupportsSecureRestorableState:(NSApplication*)app
{
    return YES;
}

- (IBAction)openDocument:(id)sender
{
    NSLog(@"open");
}

@end
