/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWebView/History.h>

#import <Application/ApplicationDelegate.h>
#import <LibWeb/Loader/ResourceLoader.h>
#import <LibWebView/UserAgent.h>
#import <UI/LadybirdWebView.h>
#import <UI/Tab.h>
#import <UI/TabController.h>
#import <Utilities/Conversions.h>
#import <Utilities/URL.h>

#if !__has_feature(objc_arc)
#    error "This project requires ARC"
#endif

static NSString* const TOOLBAR_IDENTIFIER = @"Toolbar";
static NSString* const TOOLBAR_NAVIGATE_BACK_IDENTIFIER = @"ToolbarNavigateBackIdentifier";
static NSString* const TOOLBAR_NAVIGATE_FORWARD_IDENTIFIER = @"ToolbarNavigateForwardIdentifier";
static NSString* const TOOLBAR_RELOAD_IDENTIFIER = @"ToolbarReloadIdentifier";
static NSString* const TOOLBAR_LOCATION_IDENTIFIER = @"ToolbarLocationIdentifier";
static NSString* const TOOLBAR_NEW_TAB_IDENTIFIER = @"ToolbarNewTabIdentifier";
static NSString* const TOOLBAR_TAB_OVERVIEW_IDENTIFIER = @"ToolbarTabOverviewIdentifer";

enum class IsHistoryNavigation {
    Yes,
    No,
};

@interface TabController () <NSToolbarDelegate, NSSearchFieldDelegate>
{
    DeprecatedString m_title;

    WebView::History m_history;
    IsHistoryNavigation m_is_history_navigation;

    TabSettings m_settings;
}

@property (nonatomic, strong) NSToolbar* toolbar;
@property (nonatomic, strong) NSArray* toolbar_identifiers;

@property (nonatomic, strong) NSToolbarItem* navigate_back_toolbar_item;
@property (nonatomic, strong) NSToolbarItem* navigate_forward_toolbar_item;
@property (nonatomic, strong) NSToolbarItem* reload_toolbar_item;
@property (nonatomic, strong) NSToolbarItem* location_toolbar_item;
@property (nonatomic, strong) NSToolbarItem* new_tab_toolbar_item;
@property (nonatomic, strong) NSToolbarItem* tab_overview_toolbar_item;

@property (nonatomic, assign) NSLayoutConstraint* location_toolbar_item_width;

@end

@implementation TabController

@synthesize toolbar_identifiers = _toolbar_identifiers;
@synthesize navigate_back_toolbar_item = _navigate_back_toolbar_item;
@synthesize navigate_forward_toolbar_item = _navigate_forward_toolbar_item;
@synthesize reload_toolbar_item = _reload_toolbar_item;
@synthesize location_toolbar_item = _location_toolbar_item;
@synthesize new_tab_toolbar_item = _new_tab_toolbar_item;
@synthesize tab_overview_toolbar_item = _tab_overview_toolbar_item;

- (instancetype)init
{
    if (self = [super init]) {
        self.toolbar = [[NSToolbar alloc] initWithIdentifier:TOOLBAR_IDENTIFIER];
        [self.toolbar setDelegate:self];
        [self.toolbar setDisplayMode:NSToolbarDisplayModeIconOnly];
        [self.toolbar setAllowsUserCustomization:NO];
        [self.toolbar setSizeMode:NSToolbarSizeModeRegular];

        m_is_history_navigation = IsHistoryNavigation::No;
        m_settings = {};
    }

    return self;
}

#pragma mark - Public methods

- (void)loadURL:(URL const&)url
{
    [[self tab].web_view loadURL:url];
}

- (void)loadHTML:(StringView)html url:(URL const&)url
{
    [[self tab].web_view loadHTML:html];
}

- (void)onLoadStart:(URL const&)url isRedirect:(BOOL)isRedirect
{
    if (isRedirect) {
        m_history.replace_current(url, m_title);
    }

    auto* url_string = Ladybird::string_to_ns_string(url.serialize());
    auto* location_search_field = (NSSearchField*)[self.location_toolbar_item view];
    [location_search_field setStringValue:url_string];

    if (m_is_history_navigation == IsHistoryNavigation::Yes) {
        m_is_history_navigation = IsHistoryNavigation::No;
    } else {
        m_history.push(url, m_title);
    }

    [self updateNavigationButtonStates];
}

- (void)onTitleChange:(DeprecatedString const&)title
{
    m_title = title;
    m_history.update_title(m_title);
}

- (void)navigateBack:(id)sender
{
    if (!m_history.can_go_back()) {
        return;
    }

    m_is_history_navigation = IsHistoryNavigation::Yes;
    m_history.go_back();

    auto url = m_history.current().url;
    [self loadURL:url];
}

- (void)navigateForward:(id)sender
{
    if (!m_history.can_go_forward()) {
        return;
    }

    m_is_history_navigation = IsHistoryNavigation::Yes;
    m_history.go_forward();

    auto url = m_history.current().url;
    [self loadURL:url];
}

- (void)reload:(id)sender
{
    if (m_history.is_empty()) {
        return;
    }

    m_is_history_navigation = IsHistoryNavigation::Yes;

    auto url = m_history.current().url;
    [self loadURL:url];
}

- (void)clearHistory
{
    m_history.clear();
    [self updateNavigationButtonStates];
}

- (void)debugRequest:(DeprecatedString const&)request argument:(DeprecatedString const&)argument
{
    if (request == "dump-history") {
        m_history.dump();
    } else {
        [[[self tab] web_view] debugRequest:request argument:argument];
    }
}

- (void)viewSource:(id)sender
{
    [[[self tab] web_view] viewSource];
}

- (void)focusLocationToolbarItem
{
    [self.window makeFirstResponder:self.location_toolbar_item.view];
}

#pragma mark - Private methods

- (Tab*)tab
{
    return (Tab*)[self window];
}

- (void)createNewTab:(id)sender
{
    auto* delegate = (ApplicationDelegate*)[NSApp delegate];

    [delegate createNewTab:OptionalNone {}
                   fromTab:[self tab]
               activateTab:Web::HTML::ActivateTab::Yes];
}

- (void)updateNavigationButtonStates
{
    auto* navigate_back_button = (NSButton*)[[self navigate_back_toolbar_item] view];
    [navigate_back_button setEnabled:m_history.can_go_back()];

    auto* navigate_forward_button = (NSButton*)[[self navigate_forward_toolbar_item] view];
    [navigate_forward_button setEnabled:m_history.can_go_forward()];

    auto* reload_button = (NSButton*)[[self reload_toolbar_item] view];
    [reload_button setEnabled:!m_history.is_empty()];
}

- (void)showTabOverview:(id)sender
{
    [self.window toggleTabOverview:sender];
}

- (void)dumpDOMTree:(id)sender
{
    [self debugRequest:"dump-dom-tree" argument:""];
}

- (void)dumpLayoutTree:(id)sender
{
    [self debugRequest:"dump-layout-tree" argument:""];
}

- (void)dumpPaintTree:(id)sender
{
    [self debugRequest:"dump-paint-tree" argument:""];
}

- (void)dumpStackingContextTree:(id)sender
{
    [self debugRequest:"dump-stacking-context-tree" argument:""];
}

- (void)dumpStyleSheets:(id)sender
{
    [self debugRequest:"dump-style-sheets" argument:""];
}

- (void)dumpAllResolvedStyles:(id)sender
{
    [self debugRequest:"dump-all-resolved-styles" argument:""];
}

- (void)dumpHistory:(id)sender
{
    [self debugRequest:"dump-history" argument:""];
}

- (void)dumpLocalStorage:(id)sender
{
    [self debugRequest:"dump-local-storage" argument:""];
}

- (void)toggleLineBoxBorders:(id)sender
{
    m_settings.should_show_line_box_borders = !m_settings.should_show_line_box_borders;
    [self debugRequest:"set-line-box-borders" argument:m_settings.should_show_line_box_borders ? "on" : "off"];
}

- (void)collectGarbage:(id)sender
{
    [self debugRequest:"collect-garbage" argument:""];
}

- (void)dumpGCGraph:(id)sender
{
    [self debugRequest:"dump-gc-graph" argument:""];
}

- (void)clearCache:(id)sender
{
    [self debugRequest:"clear-cache" argument:""];
}

- (void)toggleScripting:(id)sender
{
    m_settings.scripting_enabled = !m_settings.scripting_enabled;
    [self debugRequest:"scripting" argument:m_settings.scripting_enabled ? "on" : "off"];
}

- (void)togglePopupBlocking:(id)sender
{
    m_settings.block_popups = !m_settings.block_popups;
    [self debugRequest:"block-pop-ups" argument:m_settings.block_popups ? "on" : "off"];
}

- (void)toggleSameOriginPolicy:(id)sender
{
    m_settings.same_origin_policy_enabled = !m_settings.same_origin_policy_enabled;
    [self debugRequest:"same-origin-policy" argument:m_settings.same_origin_policy_enabled ? "on" : "off"];
}

- (void)setUserAgentSpoof:(NSMenuItem*)sender
{
    DeprecatedString const user_agent_name = [[sender title] UTF8String];
    DeprecatedString user_agent = "";
    if (user_agent_name == "Disabled"sv) {
        user_agent = Web::default_user_agent;
    } else {
        user_agent = WebView::user_agents.get(user_agent_name).value();
    }
    m_settings.user_agent_name = user_agent_name;

    [self debugRequest:"spoof-user-agent" argument:user_agent];
    [self debugRequest:"clear-cache" argument:""]; // clear the cache to ensure requests are re-done with the new user agent
}

#pragma mark - Properties

- (NSButton*)create_button:(NSImageName)image
               with_action:(nonnull SEL)action
              with_tooltip:(NSString*)tooltip
{
    auto* button = [NSButton buttonWithImage:[NSImage imageNamed:image]
                                      target:self
                                      action:action];
    if (tooltip) {
        [button setToolTip:tooltip];
    }
    [button setBordered:NO];

    return button;
}

- (NSToolbarItem*)navigate_back_toolbar_item
{
    if (!_navigate_back_toolbar_item) {
        auto* button = [self create_button:NSImageNameGoBackTemplate
                               with_action:@selector(navigateBack:)
                              with_tooltip:@"Navigate back"];
        [button setEnabled:NO];

        _navigate_back_toolbar_item = [[NSToolbarItem alloc] initWithItemIdentifier:TOOLBAR_NAVIGATE_BACK_IDENTIFIER];
        [_navigate_back_toolbar_item setView:button];
    }

    return _navigate_back_toolbar_item;
}

- (NSToolbarItem*)navigate_forward_toolbar_item
{
    if (!_navigate_forward_toolbar_item) {
        auto* button = [self create_button:NSImageNameGoForwardTemplate
                               with_action:@selector(navigateForward:)
                              with_tooltip:@"Navigate forward"];
        [button setEnabled:NO];

        _navigate_forward_toolbar_item = [[NSToolbarItem alloc] initWithItemIdentifier:TOOLBAR_NAVIGATE_FORWARD_IDENTIFIER];
        [_navigate_forward_toolbar_item setView:button];
    }

    return _navigate_forward_toolbar_item;
}

- (NSToolbarItem*)reload_toolbar_item
{
    if (!_reload_toolbar_item) {
        auto* button = [self create_button:NSImageNameRefreshTemplate
                               with_action:@selector(reload:)
                              with_tooltip:@"Reload page"];
        [button setEnabled:NO];

        _reload_toolbar_item = [[NSToolbarItem alloc] initWithItemIdentifier:TOOLBAR_RELOAD_IDENTIFIER];
        [_reload_toolbar_item setView:button];
    }

    return _reload_toolbar_item;
}

- (NSToolbarItem*)location_toolbar_item
{
    if (!_location_toolbar_item) {
        auto* location_search_field = [[NSSearchField alloc] init];
        [location_search_field setPlaceholderString:@"Enter web address"];
        [location_search_field setTextColor:[NSColor textColor]];
        [location_search_field setDelegate:self];

        _location_toolbar_item = [[NSToolbarItem alloc] initWithItemIdentifier:TOOLBAR_LOCATION_IDENTIFIER];
        [_location_toolbar_item setView:location_search_field];
    }

    return _location_toolbar_item;
}

- (NSToolbarItem*)new_tab_toolbar_item
{
    if (!_new_tab_toolbar_item) {
        auto* button = [self create_button:NSImageNameAddTemplate
                               with_action:@selector(createNewTab:)
                              with_tooltip:@"New tab"];

        _new_tab_toolbar_item = [[NSToolbarItem alloc] initWithItemIdentifier:TOOLBAR_NEW_TAB_IDENTIFIER];
        [_new_tab_toolbar_item setView:button];
    }

    return _new_tab_toolbar_item;
}

- (NSToolbarItem*)tab_overview_toolbar_item
{
    if (!_tab_overview_toolbar_item) {
        auto* button = [self create_button:NSImageNameIconViewTemplate
                               with_action:@selector(showTabOverview:)
                              with_tooltip:@"Show all tabs"];

        _tab_overview_toolbar_item = [[NSToolbarItem alloc] initWithItemIdentifier:TOOLBAR_TAB_OVERVIEW_IDENTIFIER];
        [_tab_overview_toolbar_item setView:button];
    }

    return _tab_overview_toolbar_item;
}

- (NSArray*)toolbar_identifiers
{
    if (!_toolbar_identifiers) {
        _toolbar_identifiers = @[
            TOOLBAR_NAVIGATE_BACK_IDENTIFIER,
            TOOLBAR_NAVIGATE_FORWARD_IDENTIFIER,
            NSToolbarFlexibleSpaceItemIdentifier,
            TOOLBAR_RELOAD_IDENTIFIER,
            TOOLBAR_LOCATION_IDENTIFIER,
            NSToolbarFlexibleSpaceItemIdentifier,
            TOOLBAR_NEW_TAB_IDENTIFIER,
            TOOLBAR_TAB_OVERVIEW_IDENTIFIER,
        ];
    }

    return _toolbar_identifiers;
}

#pragma mark - NSWindowController

- (IBAction)showWindow:(id)sender
{
    self.window = [[Tab alloc] init];
    [self.window setDelegate:self];

    [self.window setToolbar:self.toolbar];
    [self.window setToolbarStyle:NSWindowToolbarStyleUnified];

    [self.window makeKeyAndOrderFront:sender];

    [self focusLocationToolbarItem];
}

#pragma mark - NSWindowDelegate

- (void)windowWillClose:(NSNotification*)notification
{
    [[self tab] tabWillClose];

    auto* delegate = (ApplicationDelegate*)[NSApp delegate];
    [delegate removeTab:self];
}

- (void)windowDidResize:(NSNotification*)notification
{
    if (self.location_toolbar_item_width != nil) {
        self.location_toolbar_item_width.active = NO;
    }

    auto width = [self window].frame.size.width * 0.6;
    self.location_toolbar_item_width = [[[self.location_toolbar_item view] widthAnchor] constraintEqualToConstant:width];
    self.location_toolbar_item_width.active = YES;

    if (![[self window] inLiveResize]) {
        [[[self tab] web_view] handleResize];
    }
}

- (void)windowDidChangeBackingProperties:(NSNotification*)notification
{
    [[[self tab] web_view] handleDevicePixelRatioChange];
}

- (BOOL)validateMenuItem:(NSMenuItem*)item
{
    if ([item action] == @selector(toggleLineBoxBorders:)) {
        [item setState:m_settings.should_show_line_box_borders ? NSControlStateValueOn : NSControlStateValueOff];
    } else if ([item action] == @selector(toggleScripting:)) {
        [item setState:m_settings.scripting_enabled ? NSControlStateValueOn : NSControlStateValueOff];
    } else if ([item action] == @selector(togglePopupBlocking:)) {
        [item setState:m_settings.block_popups ? NSControlStateValueOn : NSControlStateValueOff];
    } else if ([item action] == @selector(toggleSameOriginPolicy:)) {
        [item setState:m_settings.same_origin_policy_enabled ? NSControlStateValueOn : NSControlStateValueOff];
    } else if ([item action] == @selector(setUserAgentSpoof:)) {
        [item setState:(m_settings.user_agent_name == [[item title] UTF8String]) ? NSControlStateValueOn : NSControlStateValueOff];
    }

    return YES;
}

#pragma mark - NSToolbarDelegate

- (NSToolbarItem*)toolbar:(NSToolbar*)toolbar
        itemForItemIdentifier:(NSString*)identifier
    willBeInsertedIntoToolbar:(BOOL)flag
{
    if ([identifier isEqual:TOOLBAR_NAVIGATE_BACK_IDENTIFIER]) {
        return self.navigate_back_toolbar_item;
    }
    if ([identifier isEqual:TOOLBAR_NAVIGATE_FORWARD_IDENTIFIER]) {
        return self.navigate_forward_toolbar_item;
    }
    if ([identifier isEqual:TOOLBAR_RELOAD_IDENTIFIER]) {
        return self.reload_toolbar_item;
    }
    if ([identifier isEqual:TOOLBAR_LOCATION_IDENTIFIER]) {
        return self.location_toolbar_item;
    }
    if ([identifier isEqual:TOOLBAR_NEW_TAB_IDENTIFIER]) {
        return self.new_tab_toolbar_item;
    }
    if ([identifier isEqual:TOOLBAR_TAB_OVERVIEW_IDENTIFIER]) {
        return self.tab_overview_toolbar_item;
    }

    return nil;
}

- (NSArray*)toolbarAllowedItemIdentifiers:(NSToolbar*)toolbar
{
    return self.toolbar_identifiers;
}

- (NSArray*)toolbarDefaultItemIdentifiers:(NSToolbar*)toolbar
{
    return self.toolbar_identifiers;
}

#pragma mark - NSSearchFieldDelegate

- (BOOL)control:(NSControl*)control
               textView:(NSTextView*)text_view
    doCommandBySelector:(SEL)selector
{
    if (selector != @selector(insertNewline:)) {
        return NO;
    }

    auto* url_string = [[text_view textStorage] string];
    auto url = Ladybird::sanitize_url(url_string);
    [self loadURL:url];

    [self.window makeFirstResponder:nil];
    return YES;
}

@end
