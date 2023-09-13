/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <LibWebView/ViewImplementation.h>

#import <UI/Inspector.h>
#import <UI/LadybirdWebView.h>
#import <UI/Tab.h>
#import <Utilities/Conversions.h>
#import <Utilities/NSString+Ladybird.h>

#if !__has_feature(objc_arc)
#    error "This project requires ARC"
#endif

static constexpr CGFloat const WINDOW_WIDTH = 600;
static constexpr CGFloat const WINDOW_HEIGHT = 800;

@interface Inspector () <NSOutlineViewDataSource, NSOutlineViewDelegate>

@property (nonatomic, strong) Tab* tab;

@property (nonatomic, strong) NSOutlineView* dom_tree_outline_view;
@property (nonatomic, strong) NSDictionary* dom_tree;

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

        auto* split_view = [[NSSplitView alloc] initWithFrame:[self frame]];
        [split_view setDividerStyle:NSSplitViewDividerStylePaneSplitter];

        auto* top_tab_view = [[NSTabView alloc] init];
        [split_view addSubview:top_tab_view];

        [self initializeDOMTreeTab:top_tab_view];
        [self reset];

        auto& web_view = [[self.tab web_view] view];
        __weak Inspector* weak_self = self;

        web_view.on_received_dom_tree = [weak_self](auto const& dom_tree) {
            Inspector* strong_self = weak_self;
            if (strong_self == nil) {
                return;
            }

            strong_self.dom_tree = Ladybird::deserialize_json_to_dictionary(dom_tree);

            if (strong_self.dom_tree) {
                [strong_self.dom_tree_outline_view reloadItem:nil reloadChildren:YES];
                [strong_self.dom_tree_outline_view sizeToFit];
            } else {
                strong_self.dom_tree = @{};
            }
        };

        [self setContentView:split_view];
        [self setTitle:@"Inspector"];
        [self setIsVisible:YES];

        auto split_view_height = [split_view frame].size.height;
        [split_view setPosition:(split_view_height * 0.6f) ofDividerAtIndex:0];
    }

    return self;
}

- (void)dealloc
{
    auto& web_view = [[self.tab web_view] view];
    web_view.on_received_dom_tree = nullptr;
}

#pragma mark - Public methods

- (void)inspect
{
    auto& web_view = [[self.tab web_view] view];
    web_view.inspect_dom_tree();
}

- (void)reset
{
    self.dom_tree = @{};
    [self.dom_tree_outline_view reloadItem:nil reloadChildren:YES];
    [self.dom_tree_outline_view sizeToFit];
}

#pragma mark - Private methods

- (void)initializeDOMTreeTab:(NSTabView*)tab_view
{
    auto* tab = [[NSTabViewItem alloc] initWithIdentifier:@"DOM Tree"];
    [tab setLabel:@"DOM"];

    auto* scroll_view = [[NSScrollView alloc] init];
    [scroll_view setHasVerticalScroller:YES];
    [scroll_view setHasHorizontalScroller:YES];
    [scroll_view setLineScroll:24];
    [tab setView:scroll_view];

    self.dom_tree_outline_view = [[NSOutlineView alloc] initWithFrame:[tab_view frame]];
    [self.dom_tree_outline_view setDoubleAction:@selector(onTreeDoubleClick:)];
    [self.dom_tree_outline_view setDataSource:self];
    [self.dom_tree_outline_view setDelegate:self];
    [self.dom_tree_outline_view setHeaderView:nil];
    [scroll_view setDocumentView:self.dom_tree_outline_view];

    auto* column = [[NSTableColumn alloc] initWithIdentifier:@"DOM Tree"];
    [self.dom_tree_outline_view addTableColumn:column];

    [tab_view addTabViewItem:tab];
}

- (void)onTreeDoubleClick:(id)sender
{
    NSOutlineView* outline_view = sender;
    id item = [outline_view itemAtRow:[outline_view clickedRow]];

    if ([outline_view isItemExpanded:item]) {
        [outline_view collapseItem:item];
    } else {
        [outline_view expandItem:item];
    }
}

#pragma mark - NSOutlineViewDataSource

- (id)outlineView:(NSOutlineView*)view
            child:(NSInteger)index
           ofItem:(id)item
{
    if (item == nil) {
        item = self.dom_tree;
    }

    NSArray* children = [item objectForKey:@"children"];
    return [children objectAtIndex:index];
}

- (NSInteger)outlineView:(NSOutlineView*)view
    numberOfChildrenOfItem:(id)item
{
    if (item == nil) {
        item = self.dom_tree;
    }

    NSArray* children = [item objectForKey:@"children"];
    return static_cast<NSInteger>(children.count);
}

- (BOOL)outlineView:(NSOutlineView*)view
    isItemExpandable:(id)item
{
    NSArray* children = [item objectForKey:@"children"];
    return children.count != 0;
}

- (NSView*)outlineView:(NSOutlineView*)outline_view
    viewForTableColumn:(NSTableColumn*)table_column
                  item:(id)item
{
    auto* font = [NSFont monospacedSystemFontOfSize:12.0 weight:NSFontWeightRegular];
    auto* bold_font = [NSFont monospacedSystemFontOfSize:12.0 weight:NSFontWeightBold];

    auto attributed_text = [&](NSString* text, NSColor* color = nil, BOOL bold = false) {
        auto* attributes = [[NSMutableDictionary alloc] initWithDictionary:@{
            NSFontAttributeName : bold ? bold_font : font,
        }];

        if (color != nil) {
            [attributes setObject:color forKey:NSForegroundColorAttributeName];
        }

        return [[NSMutableAttributedString alloc] initWithString:text attributes:attributes];
    };

    NSString* type = [item objectForKey:@"type"];
    NSMutableAttributedString* text = nil;

    if ([type isEqualToString:@"text"]) {
        text = attributed_text([[item objectForKey:@"text"] stringByCollapsingConsecutiveWhitespace]);
    } else if ([type isEqualToString:@"comment"]) {
        auto* comment = [NSString stringWithFormat:@"<!--%@-->", [item objectForKey:@"data"]];
        text = attributed_text(comment, [NSColor systemGreenColor]);
    } else if ([type isEqualToString:@"shadow-root"]) {
        auto* shadow = [NSString stringWithFormat:@"%@ (%@)", [item objectForKey:@"name"], [item objectForKey:@"mode"]];
        text = attributed_text(shadow, [NSColor systemGrayColor]);
    } else if ([type isEqualToString:@"element"]) {
        text = attributed_text(@"<");

        auto* element = attributed_text(
            [[item objectForKey:@"name"] lowercaseString],
            [NSColor systemPinkColor],
            YES);
        [text appendAttributedString:element];

        NSDictionary* attributes = [item objectForKey:@"attributes"];

        [attributes enumerateKeysAndObjectsUsingBlock:^(id name, id value, BOOL*) {
            [text appendAttributedString:attributed_text(@" ")];

            name = attributed_text(name, [NSColor systemOrangeColor]);
            [text appendAttributedString:name];

            [text appendAttributedString:attributed_text(@"=")];

            value = [NSString stringWithFormat:@"\"%@\"", [value stringByCollapsingConsecutiveWhitespace]];
            value = attributed_text(value, [NSColor systemCyanColor]);
            [text appendAttributedString:value];
        }];

        [text appendAttributedString:attributed_text(@">")];
    } else {
        text = attributed_text([item objectForKey:@"name"], [NSColor systemGrayColor]);
    }

    auto* view = [NSTextField labelWithAttributedString:text];
    view.identifier = [NSString stringWithFormat:@"%@", [item objectForKey:@"id"]];

    return view;
}

#pragma mark - NSOutlineViewDelegate

- (BOOL)outlineView:(NSOutlineView*)outline_view
    shouldEditTableColumn:(NSTableColumn*)table_column
                     item:(id)item
{
    return NO;
}

@end
