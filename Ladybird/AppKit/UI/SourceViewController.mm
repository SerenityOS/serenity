/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#import <UI/SourceView.h>
#import <UI/SourceViewController.h>
#import <UI/Tab.h>
#import <UI/TabController.h>

#if !__has_feature(objc_arc)
#    error "This project requires ARC"
#endif

@interface SourceViewController ()
{
    URL m_url;
    DeprecatedString m_source;
}

@property (nonatomic, strong) TabController* tab_controller;

@end

@implementation SourceViewController

- (instancetype)init:(TabController*)tab_controller
                 url:(URL)url
              source:(DeprecatedString)source
{
    if (self = [super init]) {
        self.tab_controller = tab_controller;

        m_url = move(url);
        m_source = move(source);
    }

    return self;
}

#pragma mark - NSWindowController

- (IBAction)showWindow:(id)sender
{
    auto* tab = (Tab*)[self.tab_controller window];

    self.window = [[SourceView alloc] init:tab
                                       url:m_url
                                    source:m_source];

    [self.window setDelegate:self];
    [self.window makeKeyAndOrderFront:sender];
}

#pragma mark - NSWindowDelegate

- (void)windowWillClose:(NSNotification*)notification
{
    [self.tab_controller onSourceViewClosed];
}

@end
