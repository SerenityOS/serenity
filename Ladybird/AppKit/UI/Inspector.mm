/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWebView/InspectorClient.h>
#include <LibWebView/ViewImplementation.h>

#import <UI/Inspector.h>
#import <UI/LadybirdWebView.h>
#import <UI/Tab.h>

#if !__has_feature(objc_arc)
#    error "This project requires ARC"
#endif

static constexpr CGFloat const WINDOW_WIDTH = 875;
static constexpr CGFloat const WINDOW_HEIGHT = 825;

@interface Inspector ()
{
    OwnPtr<WebView::InspectorClient> m_inspector_client;
}

@property (nonatomic, strong) Tab* tab;

@end

@implementation Inspector

@synthesize tab = _tab;

- (instancetype)init:(Tab*)tab
{
    auto tab_rect = [tab frame];
    auto position_x = tab_rect.origin.x + (tab_rect.size.width - WINDOW_WIDTH) / 2;
    auto position_y = tab_rect.origin.y + (tab_rect.size.height - WINDOW_HEIGHT) / 2;

    auto window_rect = NSMakeRect(position_x, position_y, WINDOW_WIDTH, WINDOW_HEIGHT);
    auto style_mask = NSWindowStyleMaskTitled | NSWindowStyleMaskClosable | NSWindowStyleMaskMiniaturizable | NSWindowStyleMaskResizable;

    self = [super initWithContentRect:window_rect
                            styleMask:style_mask
                              backing:NSBackingStoreBuffered
                                defer:NO];

    if (self) {
        self.tab = tab;

        self.web_view = [[LadybirdWebView alloc] init:nil];
        [self.web_view setPostsBoundsChangedNotifications:YES];

        m_inspector_client = make<WebView::InspectorClient>([[tab web_view] view], [[self web_view] view]);

        auto* scroll_view = [[NSScrollView alloc] init];
        [scroll_view setHasVerticalScroller:YES];
        [scroll_view setHasHorizontalScroller:YES];
        [scroll_view setLineScroll:24];

        [scroll_view setContentView:self.web_view];
        [scroll_view setDocumentView:[[NSView alloc] init]];

        [self setContentView:scroll_view];
        [self setTitle:@"Inspector"];
        [self setIsVisible:YES];
    }

    return self;
}

- (void)dealloc
{
    auto& web_view = [[self.tab web_view] view];
    web_view.clear_inspected_dom_node();
}

#pragma mark - Public methods

- (void)inspect
{
    m_inspector_client->inspect();
}

- (void)reset
{
    m_inspector_client->reset();
}

- (void)selectHoveredElement
{
    m_inspector_client->select_hovered_node();
}

@end
