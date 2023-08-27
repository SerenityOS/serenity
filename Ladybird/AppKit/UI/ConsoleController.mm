/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#import <UI/Console.h>
#import <UI/ConsoleController.h>
#import <UI/LadybirdWebView.h>
#import <UI/Tab.h>

#if !__has_feature(objc_arc)
#    error "This project requires ARC"
#endif

@interface ConsoleController () <NSWindowDelegate>

@property (nonatomic, strong) Tab* tab;

@end

@implementation ConsoleController

- (instancetype)init:(Tab*)tab
{
    if (self = [super init]) {
        self.tab = tab;
    }

    return self;
}

#pragma mark - Private methods

- (Console*)console
{
    return (Console*)[self window];
}

#pragma mark - NSWindowController

- (IBAction)showWindow:(id)sender
{
    self.window = [[Console alloc] init:self.tab];
    [self.window setDelegate:self];
    [self.window makeKeyAndOrderFront:sender];
}

#pragma mark - NSWindowDelegate

- (void)windowWillClose:(NSNotification*)notification
{
    [self.tab onConsoleClosed];
}

- (void)windowDidResize:(NSNotification*)notification
{
    if (![[self window] inLiveResize]) {
        [[[self console] web_view] handleResize];
    }
}

@end
