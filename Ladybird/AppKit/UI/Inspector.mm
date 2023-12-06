/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWebView/Attribute.h>
#include <LibWebView/InspectorClient.h>
#include <LibWebView/ViewImplementation.h>

#import <UI/Event.h>
#import <UI/Inspector.h>
#import <UI/LadybirdWebView.h>
#import <UI/Tab.h>
#import <Utilities/Conversions.h>

#if !__has_feature(objc_arc)
#    error "This project requires ARC"
#endif

static constexpr CGFloat const WINDOW_WIDTH = 875;
static constexpr CGFloat const WINDOW_HEIGHT = 825;

static constexpr NSInteger CONTEXT_MENU_EDIT_NODE_TAG = 1;
static constexpr NSInteger CONTEXT_MENU_REMOVE_ATTRIBUTE_TAG = 2;
static constexpr NSInteger CONTEXT_MENU_COPY_ATTRIBUTE_VALUE_TAG = 3;

@interface Inspector ()
{
    OwnPtr<WebView::InspectorClient> m_inspector_client;
}

@property (nonatomic, strong) Tab* tab;

@property (nonatomic, strong) NSMenu* dom_node_text_context_menu;
@property (nonatomic, strong) NSMenu* dom_node_tag_context_menu;
@property (nonatomic, strong) NSMenu* dom_node_attribute_context_menu;

@end

@implementation Inspector

@synthesize tab = _tab;
@synthesize dom_node_text_context_menu = _dom_node_text_context_menu;
@synthesize dom_node_tag_context_menu = _dom_node_tag_context_menu;
@synthesize dom_node_attribute_context_menu = _dom_node_attribute_context_menu;

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

        m_inspector_client->on_requested_dom_node_text_context_menu = [weak_self](auto position) {
            Inspector* strong_self = weak_self;
            if (strong_self == nil) {
                return;
            }

            auto* event = Ladybird::create_context_menu_mouse_event(strong_self.web_view, position);
            [NSMenu popUpContextMenu:strong_self.dom_node_text_context_menu withEvent:event forView:strong_self.web_view];
        };

        m_inspector_client->on_requested_dom_node_tag_context_menu = [weak_self](auto position, auto const& tag) {
            Inspector* strong_self = weak_self;
            if (strong_self == nil) {
                return;
            }

            auto edit_node_text = MUST(String::formatted("Edit \"{}\"", tag));

            auto* edit_node_menu_item = [strong_self.dom_node_tag_context_menu itemWithTag:CONTEXT_MENU_EDIT_NODE_TAG];
            [edit_node_menu_item setTitle:Ladybird::string_to_ns_string(edit_node_text)];

            auto* event = Ladybird::create_context_menu_mouse_event(strong_self.web_view, position);
            [NSMenu popUpContextMenu:strong_self.dom_node_tag_context_menu withEvent:event forView:strong_self.web_view];
        };

        m_inspector_client->on_requested_dom_node_attribute_context_menu = [weak_self](auto position, auto const&, auto const& attribute) {
            Inspector* strong_self = weak_self;
            if (strong_self == nil) {
                return;
            }

            static constexpr size_t MAX_ATTRIBUTE_VALUE_LENGTH = 32;

            auto edit_attribute_text = MUST(String::formatted("Edit attribute \"{}\"", attribute.name));
            auto remove_attribute_text = MUST(String::formatted("Remove attribute \"{}\"", attribute.name));
            auto copy_attribute_value_text = MUST(String::formatted("Copy attribute value \"{:.{}}{}\"",
                attribute.value, MAX_ATTRIBUTE_VALUE_LENGTH,
                attribute.value.bytes_as_string_view().length() > MAX_ATTRIBUTE_VALUE_LENGTH ? "..."sv : ""sv));

            auto* edit_node_menu_item = [strong_self.dom_node_attribute_context_menu itemWithTag:CONTEXT_MENU_EDIT_NODE_TAG];
            [edit_node_menu_item setTitle:Ladybird::string_to_ns_string(edit_attribute_text)];

            auto* remove_attribute_menu_item = [strong_self.dom_node_attribute_context_menu itemWithTag:CONTEXT_MENU_REMOVE_ATTRIBUTE_TAG];
            [remove_attribute_menu_item setTitle:Ladybird::string_to_ns_string(remove_attribute_text)];

            auto* copy_attribute_value_menu_item = [strong_self.dom_node_attribute_context_menu itemWithTag:CONTEXT_MENU_COPY_ATTRIBUTE_VALUE_TAG];
            [copy_attribute_value_menu_item setTitle:Ladybird::string_to_ns_string(copy_attribute_value_text)];

            auto* event = Ladybird::create_context_menu_mouse_event(strong_self.web_view, position);
            [NSMenu popUpContextMenu:strong_self.dom_node_attribute_context_menu withEvent:event forView:strong_self.web_view];
        };

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

#pragma mark - Private methods

- (void)editDOMNode:(id)sender
{
    m_inspector_client->context_menu_edit_dom_node();
}

- (void)copyDOMNode:(id)sender
{
    m_inspector_client->context_menu_copy_dom_node();
}

- (void)screenshotDOMNode:(id)sender
{
    m_inspector_client->context_menu_screenshot_dom_node();
}

- (void)deleteDOMNode:(id)sender
{
    m_inspector_client->context_menu_remove_dom_node();
}

- (void)addDOMAttribute:(id)sender
{
    m_inspector_client->context_menu_add_dom_node_attribute();
}

- (void)removeDOMAttribute:(id)sender
{
    m_inspector_client->context_menu_remove_dom_node_attribute();
}

- (void)copyDOMAttributeValue:(id)sender
{
    m_inspector_client->context_menu_copy_dom_node_attribute_value();
}

#pragma mark - Properties

- (NSMenu*)dom_node_text_context_menu
{
    if (!_dom_node_text_context_menu) {
        _dom_node_text_context_menu = [[NSMenu alloc] initWithTitle:@"DOM Text Context Menu"];

        [_dom_node_text_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Edit text"
                                                                        action:@selector(editDOMNode:)
                                                                 keyEquivalent:@""]];
        [_dom_node_text_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Copy text"
                                                                        action:@selector(copyDOMNode:)
                                                                 keyEquivalent:@""]];

        [_dom_node_text_context_menu addItem:[NSMenuItem separatorItem]];

        [_dom_node_text_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Delete node"
                                                                        action:@selector(deleteDOMNode:)
                                                                 keyEquivalent:@""]];
    }

    return _dom_node_text_context_menu;
}

- (NSMenu*)dom_node_tag_context_menu
{
    if (!_dom_node_tag_context_menu) {
        _dom_node_tag_context_menu = [[NSMenu alloc] initWithTitle:@"DOM Tag Context Menu"];

        auto* edit_node_menu_item = [[NSMenuItem alloc] initWithTitle:@"Edit tag"
                                                               action:@selector(editDOMNode:)
                                                        keyEquivalent:@""];
        [edit_node_menu_item setTag:CONTEXT_MENU_EDIT_NODE_TAG];
        [_dom_node_tag_context_menu addItem:edit_node_menu_item];

        [_dom_node_tag_context_menu addItem:[NSMenuItem separatorItem]];

        [_dom_node_tag_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Add attribute"
                                                                       action:@selector(addDOMAttribute:)
                                                                keyEquivalent:@""]];
        [_dom_node_tag_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Delete node"
                                                                       action:@selector(deleteDOMNode:)
                                                                keyEquivalent:@""]];

        [_dom_node_tag_context_menu addItem:[NSMenuItem separatorItem]];

        [_dom_node_tag_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Copy HTML"
                                                                       action:@selector(copyDOMNode:)
                                                                keyEquivalent:@""]];
        [_dom_node_tag_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Take node screenshot"
                                                                       action:@selector(screenshotDOMNode:)
                                                                keyEquivalent:@""]];
    }

    return _dom_node_tag_context_menu;
}

- (NSMenu*)dom_node_attribute_context_menu
{
    if (!_dom_node_attribute_context_menu) {
        _dom_node_attribute_context_menu = [[NSMenu alloc] initWithTitle:@"DOM Attribute Context Menu"];

        auto* edit_node_menu_item = [[NSMenuItem alloc] initWithTitle:@"Edit attribute"
                                                               action:@selector(editDOMNode:)
                                                        keyEquivalent:@""];
        [edit_node_menu_item setTag:CONTEXT_MENU_EDIT_NODE_TAG];
        [_dom_node_attribute_context_menu addItem:edit_node_menu_item];

        auto* remove_attribute_menu_item = [[NSMenuItem alloc] initWithTitle:@"Remove attribute"
                                                                      action:@selector(removeDOMAttribute:)
                                                               keyEquivalent:@""];
        [remove_attribute_menu_item setTag:CONTEXT_MENU_REMOVE_ATTRIBUTE_TAG];
        [_dom_node_attribute_context_menu addItem:remove_attribute_menu_item];

        auto* copy_attribute_value_menu_item = [[NSMenuItem alloc] initWithTitle:@"Copy attribute value"
                                                                          action:@selector(copyDOMAttributeValue:)
                                                                   keyEquivalent:@""];
        [copy_attribute_value_menu_item setTag:CONTEXT_MENU_COPY_ATTRIBUTE_VALUE_TAG];
        [_dom_node_attribute_context_menu addItem:copy_attribute_value_menu_item];

        [_dom_node_attribute_context_menu addItem:[NSMenuItem separatorItem]];

        [_dom_node_attribute_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Add attribute"
                                                                             action:@selector(addDOMAttribute:)
                                                                      keyEquivalent:@""]];
        [_dom_node_attribute_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Delete node"
                                                                             action:@selector(deleteDOMNode:)
                                                                      keyEquivalent:@""]];

        [_dom_node_attribute_context_menu addItem:[NSMenuItem separatorItem]];

        [_dom_node_attribute_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Copy HTML"
                                                                             action:@selector(copyDOMNode:)
                                                                      keyEquivalent:@""]];
        [_dom_node_attribute_context_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Take node screenshot"
                                                                             action:@selector(screenshotDOMNode:)
                                                                      keyEquivalent:@""]];
    }

    return _dom_node_attribute_context_menu;
}

@end
