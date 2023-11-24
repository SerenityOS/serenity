/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <AK/Traits.h>
#include <LibWeb/CSS/Selector.h>
#include <LibWebView/InspectorClient.h>
#include <LibWebView/ViewImplementation.h>

#import <UI/Inspector.h>
#import <UI/LadybirdWebView.h>
#import <UI/Tab.h>
#import <Utilities/Conversions.h>

#if !__has_feature(objc_arc)
#    error "This project requires ARC"
#endif

static constexpr CGFloat const WINDOW_WIDTH = 875;
static constexpr CGFloat const WINDOW_HEIGHT = 825;

static NSString* const CSS_PROPERTY_COLUMN = @"Property";
static NSString* const CSS_VALUE_COLUMN = @"Value";

template<>
struct AK::Traits<NSDictionary*> : public DefaultTraits<NSDictionary*> {
    static unsigned hash(NSDictionary* dictionary)
    {
        return [dictionary hash];
    }
};

@interface Inspector () <NSTableViewDataSource>
{
    OwnPtr<WebView::InspectorClient> m_inspector_client;
}

@property (nonatomic, strong) Tab* tab;

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

        self.web_view = [[LadybirdWebView alloc] init:nil];
        [self.web_view setPostsBoundsChangedNotifications:YES];

        m_inspector_client = make<WebView::InspectorClient>([[tab web_view] view], [[self web_view] view]);

        __weak Inspector* weak_self = self;

        m_inspector_client->on_dom_node_properties_received = [weak_self](auto properties) {
            Inspector* strong_self = weak_self;
            if (strong_self == nil) {
                return;
            }

            [strong_self onDOMNodePropertiesReceived:move(properties)];
        };

        auto* split_view = [[NSSplitView alloc] initWithFrame:[self frame]];
        [split_view setDividerStyle:NSSplitViewDividerStylePaneSplitter];

        [self initializeInspectorView:split_view];

        auto* bottom_tab_view = [[NSTabView alloc] init];
        [split_view addSubview:bottom_tab_view];

        [self initializeCSSTables:bottom_tab_view];
        [self reset];

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

- (void)selectHoveredElement
{
    m_inspector_client->select_hovered_node();
}

#pragma mark - Private methods

- (void)initializeInspectorView:(NSSplitView*)split_view
{
    auto* scroll_view = [[NSScrollView alloc] init];
    [scroll_view setHasVerticalScroller:YES];
    [scroll_view setHasHorizontalScroller:YES];
    [scroll_view setLineScroll:24];

    [scroll_view setContentView:self.web_view];
    [scroll_view setDocumentView:[[NSView alloc] init]];

    [split_view addSubview:scroll_view];
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

- (void)onDOMNodePropertiesReceived:(ErrorOr<WebView::ViewImplementation::DOMNodeProperties>)properties
{
    auto deserialize_json = [](auto const& json) {
        auto* dictionary = Ladybird::deserialize_json_to_dictionary(json);
        if (!dictionary) {
            return @{};
        }

        return dictionary;
    };

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
