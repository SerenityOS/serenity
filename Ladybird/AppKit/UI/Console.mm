/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OwnPtr.h>
#include <LibWebView/ConsoleClient.h>

#import <UI/Console.h>
#import <UI/LadybirdWebView.h>
#import <UI/Tab.h>
#import <Utilities/Conversions.h>

#if !__has_feature(objc_arc)
#    error "This project requires ARC"
#endif

static constexpr CGFloat const WINDOW_WIDTH = 520;
static constexpr CGFloat const WINDOW_HEIGHT = 600;

@interface Console () <NSTextFieldDelegate>
{
    OwnPtr<WebView::ConsoleClient> m_console_client;
}

@property (nonatomic, strong) Tab* tab;
@property (nonatomic, strong) NSScrollView* scroll_view;

@end

@implementation Console

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

        m_console_client = make<WebView::ConsoleClient>([[tab web_view] view], [[self web_view] view]);

        self.scroll_view = [[NSScrollView alloc] initWithFrame:[self frame]];
        [self.scroll_view setHasVerticalScroller:YES];
        [self.scroll_view setHasHorizontalScroller:YES];
        [self.scroll_view setLineScroll:24];

        [self.scroll_view setContentView:self.web_view];
        [self.scroll_view setDocumentView:[[NSView alloc] init]];

        auto* font = [NSFont monospacedSystemFontOfSize:12.0
                                                 weight:NSFontWeightRegular];

        auto* prompt_indicator_attributes = @{
            NSForegroundColorAttributeName : [NSColor systemCyanColor],
            NSFontAttributeName : font,
        };
        auto* prompt_indicator_attribute = [[NSAttributedString alloc] initWithString:@">>"
                                                                           attributes:prompt_indicator_attributes];
        auto* prompt_indicator = [NSTextField labelWithAttributedString:prompt_indicator_attribute];

        auto* prompt_text = [[NSTextField alloc] init];
        [prompt_text setPlaceholderString:@"Enter JavaScript statement"];
        [prompt_text setDelegate:self];
        [prompt_text setBordered:YES];
        [prompt_text setBezeled:YES];
        [prompt_text setFont:font];

        auto* clear_button = [NSButton buttonWithImage:[NSImage imageNamed:NSImageNameStopProgressTemplate]
                                                target:self
                                                action:@selector(clearConsole:)];
        [clear_button setToolTip:@"Clear the console output"];

        auto* controls_stack_view = [NSStackView stackViewWithViews:@[ prompt_indicator, prompt_text, clear_button ]];
        [controls_stack_view setOrientation:NSUserInterfaceLayoutOrientationHorizontal];
        [controls_stack_view setEdgeInsets:NSEdgeInsetsMake(8, 8, 8, 8)];

        auto* content_stack_view = [NSStackView stackViewWithViews:@[ self.scroll_view, controls_stack_view ]];
        [content_stack_view setOrientation:NSUserInterfaceLayoutOrientationVertical];
        [content_stack_view setSpacing:0];

        [self setContentView:content_stack_view];
        [self setTitle:@"Console"];
        [self setIsVisible:YES];

        [[NSNotificationCenter defaultCenter]
            addObserver:self
               selector:@selector(onContentScroll:)
                   name:NSViewBoundsDidChangeNotification
                 object:[self.scroll_view contentView]];
    }

    return self;
}

#pragma mark - Public methods

- (void)reset
{
    m_console_client->reset();
}

#pragma mark - Private methods

- (void)onContentScroll:(NSNotification*)notification
{
    [[self web_view] handleScroll];
}

- (void)clearConsole:(id)sender
{
    m_console_client->clear();
}

#pragma mark - NSTextFieldDelegate

- (BOOL)control:(NSControl*)control
               textView:(NSTextView*)text_view
    doCommandBySelector:(SEL)selector
{
    if (selector != @selector(insertNewline:)) {
        return NO;
    }

    auto* ns_script = [[text_view textStorage] string];
    auto script = Ladybird::ns_string_to_string(ns_script);

    if (!script.bytes_as_string_view().is_whitespace()) {
        m_console_client->execute(move(script));
        [text_view setString:@""];
    }

    return YES;
}

@end
