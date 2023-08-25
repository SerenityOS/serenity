/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/Parser/HTMLTokenizer.h>

#import <UI/SourceView.h>
#import <UI/Tab.h>
#import <Utilities/Conversions.h>

#if !__has_feature(objc_arc)
#    error "This project requires ARC"
#endif

static constexpr CGFloat const WINDOW_WIDTH = 800;
static constexpr CGFloat const WINDOW_HEIGHT = 600;

@interface SourceView ()
@end

@implementation SourceView

- (instancetype)init:(Tab*)tab
                 url:(URL const&)url
              source:(StringView)source
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
        auto* scroll_view = [[NSScrollView alloc] initWithFrame:[[self contentView] frame]];
        [scroll_view setAutoresizingMask:NSViewWidthSizable | NSViewHeightSizable];
        [scroll_view setHasHorizontalScroller:NO];
        [scroll_view setHasVerticalScroller:YES];

        auto* font = [NSFont monospacedSystemFontOfSize:12.0
                                                 weight:NSFontWeightRegular];

        auto* text_view = [[NSTextView alloc] initWithFrame:[[scroll_view contentView] frame]];
        [text_view setAutoresizingMask:NSViewWidthSizable];
        [text_view setHorizontallyResizable:NO];
        [text_view setVerticallyResizable:YES];
        [text_view setEditable:NO];
        [text_view setFont:font];

        auto* ns_source = Ladybird::string_to_ns_string(source);
        [text_view setString:ns_source];

        [scroll_view setDocumentView:text_view];
        [self setContentView:scroll_view];

        auto title = MUST(String::formatted("View Source - {}", url));
        auto* ns_title = Ladybird::string_to_ns_string(title);
        [self setTitle:ns_title];

        [self highlightHTML:source textStorage:[text_view textStorage]];
        [self setIsVisible:YES];
    }

    return self;
}

#pragma mark - Private methods

- (void)highlightHTML:(StringView)source
          textStorage:(NSTextStorage*)storage
{
    Web::HTML::HTMLTokenizer tokenizer { source, "utf-8"sv };

    auto* bold_font = [NSFont monospacedSystemFontOfSize:12.0
                                                  weight:NSFontWeightBold];

    auto highlight = [&](auto* color, auto start, auto end) {
        if (end >= [storage length])
            return;

        [storage addAttribute:NSForegroundColorAttributeName
                        value:color
                        range:NSMakeRange(start, end - start)];
    };

    auto bolden = [&](auto start, auto end) {
        if (end >= [storage length])
            return;

        [storage addAttribute:NSFontAttributeName
                        value:bold_font
                        range:NSMakeRange(start, end - start)];
    };

    for (auto token = tokenizer.next_token(); token.has_value(); token = tokenizer.next_token()) {
        if (token->is_end_of_file())
            break;

        if (token->is_comment()) {
            auto start_offset = token->start_position().byte_offset;
            auto end_offset = token->end_position().byte_offset;

            highlight([NSColor systemGreenColor], start_offset, end_offset);
        } else if (token->is_start_tag() || token->is_end_tag()) {
            auto start_offset = token->start_position().byte_offset;
            auto end_offset = start_offset + token->tag_name().length();

            highlight([NSColor systemPinkColor], start_offset, end_offset);
            bolden(start_offset, end_offset);

            token->for_each_attribute([&](auto const& attribute) {
                start_offset = attribute.name_start_position.byte_offset;
                end_offset = attribute.name_end_position.byte_offset;
                highlight([NSColor systemOrangeColor], start_offset, end_offset);

                start_offset = attribute.value_start_position.byte_offset;
                end_offset = attribute.value_end_position.byte_offset;
                highlight([NSColor systemCyanColor], start_offset, end_offset);

                return IterationDecision::Continue;
            });
        }
    }
}

@end
