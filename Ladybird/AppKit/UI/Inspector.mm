/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <LibWeb/CSS/Selector.h>
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

static NSString* const CSS_PROPERTY_COLUMN = @"Property";
static NSString* const CSS_VALUE_COLUMN = @"Value";

struct Selection {
    bool operator==(Selection const& other) const = default;

    i32 dom_node_id { 0 };
    Optional<Web::CSS::Selector::PseudoElement> pseudo_element {};
};

@interface Inspector () <NSOutlineViewDataSource, NSOutlineViewDelegate, NSTableViewDataSource>
{
    Selection m_selection;
}

@property (nonatomic, strong) Tab* tab;

@property (nonatomic, strong) NSOutlineView* dom_tree_outline_view;
@property (nonatomic, strong) NSDictionary* dom_tree;

@property (nonatomic, strong) NSTableView* computed_style_table_view;
@property (nonatomic, strong) NSDictionary* computed_style;
@property (nonatomic, strong) NSArray* computed_style_keys;

@property (nonatomic, strong) NSTableView* resolved_style_table_view;
@property (nonatomic, strong) NSDictionary* resolved_style;
@property (nonatomic, strong) NSArray* resolved_style_keys;

@property (nonatomic, strong) NSTableView* variables_table_view;
@property (nonatomic, strong) NSDictionary* variables;
@property (nonatomic, strong) NSArray* variables_keys;

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

        auto* bottom_tab_view = [[NSTabView alloc] init];
        [split_view addSubview:bottom_tab_view];

        [self initializeDOMTreeTab:top_tab_view];
        [self initializeCSSTables:bottom_tab_view];
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
    web_view.clear_inspected_dom_node();
}

#pragma mark - Public methods

- (void)inspect
{
    auto& web_view = [[self.tab web_view] view];
    web_view.inspect_dom_tree();
}

- (void)reset
{
    m_selection = {};

    self.dom_tree = @{};
    [self.dom_tree_outline_view reloadItem:nil reloadChildren:YES];
    [self.dom_tree_outline_view sizeToFit];

    self.computed_style = @{};
    self.computed_style_keys = @[];
    [self.computed_style_table_view reloadData];

    self.resolved_style = @{};
    self.resolved_style_keys = @[];
    [self.resolved_style_table_view reloadData];

    self.variables = @{};
    self.variables_keys = @[];
    [self.variables_table_view reloadData];
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

- (NSTableView*)createCSSTable:(NSTabView*)tab_view identifier:(NSString*)identifier
{
    auto* tab = [[NSTabViewItem alloc] initWithIdentifier:identifier];
    [tab setLabel:identifier];

    auto* scroll_view = [[NSScrollView alloc] init];
    [scroll_view setHasVerticalScroller:YES];
    [scroll_view setHasHorizontalScroller:YES];
    [scroll_view setLineScroll:24];
    [tab setView:scroll_view];

    auto* table_view = [[NSTableView alloc] initWithFrame:[tab_view frame]];
    [table_view setDataSource:self];
    [table_view setColumnAutoresizingStyle:NSTableViewUniformColumnAutoresizingStyle];
    [scroll_view setDocumentView:table_view];

    auto* property_column = [[NSTableColumn alloc] initWithIdentifier:CSS_PROPERTY_COLUMN];
    [property_column setTitle:CSS_PROPERTY_COLUMN];
    [table_view addTableColumn:property_column];

    auto* value_column = [[NSTableColumn alloc] initWithIdentifier:CSS_VALUE_COLUMN];
    [value_column setTitle:CSS_VALUE_COLUMN];
    [table_view addTableColumn:value_column];

    [tab_view addTabViewItem:tab];

    return table_view;
}

- (void)initializeCSSTables:(NSTabView*)tab_view
{
    self.computed_style_table_view = [self createCSSTable:tab_view identifier:@"Computed Style"];
    self.resolved_style_table_view = [self createCSSTable:tab_view identifier:@"Resolved Style"];
    self.variables_table_view = [self createCSSTable:tab_view identifier:@"Variables"];
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

- (void)setSelection:(Selection)selection
{
    if (selection == m_selection)
        return;

    m_selection = move(selection);

    auto deserialize_json = [](auto const& json) {
        auto* dictionary = Ladybird::deserialize_json_to_dictionary(json);
        if (!dictionary) {
            return @{};
        }

        return dictionary;
    };

    auto& web_view = [[self.tab web_view] view];
    auto properties = web_view.inspect_dom_node(m_selection.dom_node_id, m_selection.pseudo_element);

    if (!properties.is_error()) {
        self.computed_style = deserialize_json(properties.value().computed_style_json);
        self.resolved_style = deserialize_json(properties.value().resolved_style_json);
        self.variables = deserialize_json(properties.value().custom_properties_json);
    } else {
        self.computed_style = @{};
        self.resolved_style = @{};
        self.variables = @{};
    }

    self.computed_style_keys = [[self.computed_style allKeys] sortedArrayUsingSelector:@selector(compare:)];
    [self.computed_style_table_view reloadData];

    self.resolved_style_keys = [[self.resolved_style allKeys] sortedArrayUsingSelector:@selector(compare:)];
    [self.resolved_style_table_view reloadData];

    self.variables_keys = [[self.variables allKeys] sortedArrayUsingSelector:@selector(compare:)];
    [self.variables_table_view reloadData];
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

- (BOOL)outlineView:(NSOutlineView*)outline_view
    shouldSelectItem:(id)item
{
    i32 dom_node_id { 0 };
    Optional<Web::CSS::Selector::PseudoElement> pseudo_element;

    if (id element = [item objectForKey:@"pseudo-element"]) {
        dom_node_id = [[item objectForKey:@"parent-id"] intValue];
        pseudo_element = static_cast<Web::CSS::Selector::PseudoElement>([element intValue]);
    } else {
        dom_node_id = [[item objectForKey:@"id"] intValue];
    }

    [self setSelection: { dom_node_id, move(pseudo_element) }];
    return YES;
}

#pragma mark - NSTableViewDataSource

- (NSInteger)numberOfRowsInTableView:(NSTableView*)table_view
{
    if (table_view == self.computed_style_table_view) {
        return static_cast<NSInteger>(self.computed_style.count);
    }
    if (table_view == self.resolved_style_table_view) {
        return static_cast<NSInteger>(self.resolved_style.count);
    }
    if (table_view == self.variables_table_view) {
        return static_cast<NSInteger>(self.variables.count);
    }

    return 0;
}

- (id)tableView:(NSTableView*)table_view
    objectValueForTableColumn:(NSTableColumn*)table_column
                          row:(NSInteger)row
{
    NSDictionary* values = nil;
    NSArray* keys = nil;

    if (table_view == self.computed_style_table_view) {
        values = self.computed_style;
        keys = self.computed_style_keys;
    } else if (table_view == self.resolved_style_table_view) {
        values = self.resolved_style;
        keys = self.resolved_style_keys;
    } else if (table_view == self.variables_table_view) {
        values = self.variables;
        keys = self.variables_keys;
    } else {
        return nil;
    }

    if ([[table_column identifier] isEqualToString:CSS_PROPERTY_COLUMN]) {
        return keys[row];
    }
    if ([[table_column identifier] isEqualToString:CSS_VALUE_COLUMN]) {
        return values[keys[row]];
    }

    return nil;
}

@end
