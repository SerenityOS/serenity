/*
 * Copyright (c) 2023-2024, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWebView/SearchEngine.h>

#import <Application/ApplicationDelegate.h>
#import <LibWebView/UserAgent.h>
#import <UI/LadybirdWebView.h>
#import <UI/Tab.h>
#import <UI/TabController.h>
#import <UI/TaskManagerController.h>
#import <Utilities/Conversions.h>

#if !__has_feature(objc_arc)
#    error "This project requires ARC"
#endif

@interface ApplicationDelegate () <TaskManagerDelegate>
{
    Vector<URL::URL> m_initial_urls;
    URL::URL m_new_tab_page_url;

    // This will always be populated, but we cannot have a non-default constructible instance variable.
    OwnPtr<WebView::CookieJar> m_cookie_jar;

    Ladybird::WebContentOptions m_web_content_options;
    Optional<StringView> m_webdriver_content_ipc_path;

    Web::CSS::PreferredColorScheme m_preferred_color_scheme;

    WebView::SearchEngine m_search_engine;
}

@property (nonatomic, strong) NSMutableArray<TabController*>* managed_tabs;
@property (nonatomic, strong) Tab* active_tab;

@property (nonatomic, strong) TaskManagerController* task_manager_controller;

- (NSMenuItem*)createApplicationMenu;
- (NSMenuItem*)createFileMenu;
- (NSMenuItem*)createEditMenu;
- (NSMenuItem*)createViewMenu;
- (NSMenuItem*)createSettingsMenu;
- (NSMenuItem*)createHistoryMenu;
- (NSMenuItem*)createInspectMenu;
- (NSMenuItem*)createDebugMenu;
- (NSMenuItem*)createWindowMenu;
- (NSMenuItem*)createHelpMenu;

@end

@implementation ApplicationDelegate

- (instancetype)init:(Vector<URL::URL>)initial_urls
              newTabPageURL:(URL::URL)new_tab_page_url
              withCookieJar:(NonnullOwnPtr<WebView::CookieJar>)cookie_jar
          webContentOptions:(Ladybird::WebContentOptions const&)web_content_options
    webdriverContentIPCPath:(StringView)webdriver_content_ipc_path
{
    if (self = [super init]) {
        [NSApp setMainMenu:[[NSMenu alloc] init]];

        [[NSApp mainMenu] addItem:[self createApplicationMenu]];
        [[NSApp mainMenu] addItem:[self createFileMenu]];
        [[NSApp mainMenu] addItem:[self createEditMenu]];
        [[NSApp mainMenu] addItem:[self createViewMenu]];
        [[NSApp mainMenu] addItem:[self createSettingsMenu]];
        [[NSApp mainMenu] addItem:[self createHistoryMenu]];
        [[NSApp mainMenu] addItem:[self createInspectMenu]];
        [[NSApp mainMenu] addItem:[self createDebugMenu]];
        [[NSApp mainMenu] addItem:[self createWindowMenu]];
        [[NSApp mainMenu] addItem:[self createHelpMenu]];

        self.managed_tabs = [[NSMutableArray alloc] init];

        m_initial_urls = move(initial_urls);
        m_new_tab_page_url = move(new_tab_page_url);

        m_cookie_jar = move(cookie_jar);

        m_web_content_options = web_content_options;

        if (!webdriver_content_ipc_path.is_empty()) {
            m_webdriver_content_ipc_path = webdriver_content_ipc_path;
        }

        m_preferred_color_scheme = Web::CSS::PreferredColorScheme::Auto;
        m_search_engine = WebView::default_search_engine();

        // Reduce the tooltip delay, as the default delay feels quite long.
        [[NSUserDefaults standardUserDefaults] setObject:@100 forKey:@"NSInitialToolTipDelay"];
    }

    return self;
}

#pragma mark - Public methods

- (TabController*)createNewTab:(Optional<URL::URL> const&)url
                       fromTab:(Tab*)tab
                   activateTab:(Web::HTML::ActivateTab)activate_tab
{
    auto* controller = [self createNewTab:activate_tab fromTab:tab];
    [controller loadURL:url.value_or(m_new_tab_page_url)];

    return controller;
}

- (nonnull TabController*)createNewTab:(StringView)html
                                   url:(URL::URL const&)url
                               fromTab:(nullable Tab*)tab
                           activateTab:(Web::HTML::ActivateTab)activate_tab
{
    auto* controller = [self createNewTab:activate_tab fromTab:tab];
    [controller loadHTML:html url:url];

    return controller;
}

- (void)setActiveTab:(Tab*)tab
{
    self.active_tab = tab;
}

- (Tab*)activeTab
{
    return self.active_tab;
}

- (void)removeTab:(TabController*)controller
{
    [self.managed_tabs removeObject:controller];

    if ([self.managed_tabs count] == 0u) {
        if (self.task_manager_controller != nil) {
            [self.task_manager_controller.window close];
        }
    }
}

- (WebView::CookieJar&)cookieJar
{
    return *m_cookie_jar;
}

- (Ladybird::WebContentOptions const&)webContentOptions
{
    return m_web_content_options;
}

- (Optional<StringView> const&)webdriverContentIPCPath
{
    return m_webdriver_content_ipc_path;
}

- (Web::CSS::PreferredColorScheme)preferredColorScheme
{
    return m_preferred_color_scheme;
}

- (WebView::SearchEngine const&)searchEngine
{
    return m_search_engine;
}

#pragma mark - Private methods

- (void)openAboutVersionPage:(id)sender
{
    auto* current_tab = [NSApp keyWindow];
    if (![current_tab isKindOfClass:[Tab class]]) {
        return;
    }

    [self createNewTab:URL::URL("about:version"sv)
               fromTab:(Tab*)current_tab
           activateTab:Web::HTML::ActivateTab::Yes];
}

- (nonnull TabController*)createNewTab:(Web::HTML::ActivateTab)activate_tab
                               fromTab:(nullable Tab*)tab
{
    auto* controller = [[TabController alloc] init];
    [controller showWindow:nil];

    if (tab) {
        [[tab tabGroup] addWindow:controller.window];

        // FIXME: Can we create the tabbed window above without it becoming active in the first place?
        if (activate_tab == Web::HTML::ActivateTab::No) {
            [tab orderFront:nil];
        }
    }

    if (activate_tab == Web::HTML::ActivateTab::Yes) {
        [[controller window] orderFrontRegardless];
    }

    [self.managed_tabs addObject:controller];
    return controller;
}

- (void)closeCurrentTab:(id)sender
{
    auto* current_window = [NSApp keyWindow];
    [current_window close];
}

- (void)openTaskManager:(id)sender
{
    if (self.task_manager_controller != nil) {
        [self.task_manager_controller.window makeKeyAndOrderFront:sender];
        return;
    }

    self.task_manager_controller = [[TaskManagerController alloc] init:self];
    [self.task_manager_controller showWindow:nil];
}

- (void)openLocation:(id)sender
{
    auto* current_tab = [NSApp keyWindow];

    if (![current_tab isKindOfClass:[Tab class]]) {
        return;
    }

    auto* controller = (TabController*)[current_tab windowController];
    [controller focusLocationToolbarItem];
}

- (void)setAutoPreferredColorScheme:(id)sender
{
    m_preferred_color_scheme = Web::CSS::PreferredColorScheme::Auto;
    [self broadcastPreferredColorSchemeUpdate];
}

- (void)setDarkPreferredColorScheme:(id)sender
{
    m_preferred_color_scheme = Web::CSS::PreferredColorScheme::Dark;
    [self broadcastPreferredColorSchemeUpdate];
}

- (void)setLightPreferredColorScheme:(id)sender
{
    m_preferred_color_scheme = Web::CSS::PreferredColorScheme::Light;
    [self broadcastPreferredColorSchemeUpdate];
}

- (void)broadcastPreferredColorSchemeUpdate
{
    for (TabController* controller in self.managed_tabs) {
        auto* tab = (Tab*)[controller window];
        [[tab web_view] setPreferredColorScheme:m_preferred_color_scheme];
    }
}

- (void)setSearchEngine:(id)sender
{
    auto* item = (NSMenuItem*)sender;
    auto title = Ladybird::ns_string_to_string([item title]);

    if (auto search_engine = WebView::find_search_engine_by_name(title); search_engine.has_value())
        m_search_engine = search_engine.release_value();
    else
        m_search_engine = WebView::default_search_engine();
}

- (void)clearHistory:(id)sender
{
    for (TabController* controller in self.managed_tabs) {
        [controller clearHistory];
    }
}

- (void)dumpCookies:(id)sender
{
    m_cookie_jar->dump_cookies();
}

- (NSMenuItem*)createApplicationMenu
{
    auto* menu = [[NSMenuItem alloc] init];

    auto* process_name = [[NSProcessInfo processInfo] processName];
    auto* submenu = [[NSMenu alloc] initWithTitle:process_name];

    [submenu addItem:[[NSMenuItem alloc] initWithTitle:[NSString stringWithFormat:@"About %@", process_name]
                                                action:@selector(openAboutVersionPage:)
                                         keyEquivalent:@""]];
    [submenu addItem:[NSMenuItem separatorItem]];

    [submenu addItem:[[NSMenuItem alloc] initWithTitle:[NSString stringWithFormat:@"Hide %@", process_name]
                                                action:@selector(hide:)
                                         keyEquivalent:@"h"]];
    [submenu addItem:[NSMenuItem separatorItem]];

    [submenu addItem:[[NSMenuItem alloc] initWithTitle:[NSString stringWithFormat:@"Quit %@", process_name]
                                                action:@selector(terminate:)
                                         keyEquivalent:@"q"]];

    [menu setSubmenu:submenu];
    return menu;
}

- (NSMenuItem*)createFileMenu
{
    auto* menu = [[NSMenuItem alloc] init];
    auto* submenu = [[NSMenu alloc] initWithTitle:@"File"];

    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"New Tab"
                                                action:@selector(createNewTab:)
                                         keyEquivalent:@"t"]];
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Close Tab"
                                                action:@selector(closeCurrentTab:)
                                         keyEquivalent:@"w"]];
    [submenu addItem:[NSMenuItem separatorItem]];

    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Open Location"
                                                action:@selector(openLocation:)
                                         keyEquivalent:@"l"]];

    [menu setSubmenu:submenu];
    return menu;
}

- (NSMenuItem*)createEditMenu
{
    auto* menu = [[NSMenuItem alloc] init];
    auto* submenu = [[NSMenu alloc] initWithTitle:@"Edit"];

    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Undo"
                                                action:@selector(undo:)
                                         keyEquivalent:@"z"]];
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Redo"
                                                action:@selector(redo:)
                                         keyEquivalent:@"y"]];
    [submenu addItem:[NSMenuItem separatorItem]];

    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Cut"
                                                action:@selector(cut:)
                                         keyEquivalent:@"x"]];
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Copy"
                                                action:@selector(copy:)
                                         keyEquivalent:@"c"]];
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Paste"
                                                action:@selector(paste:)
                                         keyEquivalent:@"v"]];
    [submenu addItem:[NSMenuItem separatorItem]];

    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Select All"
                                                action:@selector(selectAll:)
                                         keyEquivalent:@"a"]];
    [submenu addItem:[NSMenuItem separatorItem]];

    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Find..."
                                                action:@selector(find:)
                                         keyEquivalent:@"f"]];
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Find Next"
                                                action:@selector(findNextMatch:)
                                         keyEquivalent:@"g"]];
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Find Previous"
                                                action:@selector(findPreviousMatch:)
                                         keyEquivalent:@"G"]];
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Use Selection for Find"
                                                action:@selector(useSelectionForFind:)
                                         keyEquivalent:@"e"]];

    [menu setSubmenu:submenu];
    return menu;
}

- (NSMenuItem*)createViewMenu
{
    auto* menu = [[NSMenuItem alloc] init];
    auto* submenu = [[NSMenu alloc] initWithTitle:@"View"];

    auto* color_scheme_menu = [[NSMenu alloc] init];
    [color_scheme_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Auto"
                                                          action:@selector(setAutoPreferredColorScheme:)
                                                   keyEquivalent:@""]];
    [color_scheme_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Dark"
                                                          action:@selector(setDarkPreferredColorScheme:)
                                                   keyEquivalent:@""]];
    [color_scheme_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Light"
                                                          action:@selector(setLightPreferredColorScheme:)
                                                   keyEquivalent:@""]];

    auto* color_scheme_menu_item = [[NSMenuItem alloc] initWithTitle:@"Color Scheme"
                                                              action:nil
                                                       keyEquivalent:@""];
    [color_scheme_menu_item setSubmenu:color_scheme_menu];

    auto* zoom_menu = [[NSMenu alloc] init];
    [zoom_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Zoom In"
                                                  action:@selector(zoomIn:)
                                           keyEquivalent:@"+"]];
    [zoom_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Zoom Out"
                                                  action:@selector(zoomOut:)
                                           keyEquivalent:@"-"]];
    [zoom_menu addItem:[[NSMenuItem alloc] initWithTitle:@"Actual Size"
                                                  action:@selector(resetZoom:)
                                           keyEquivalent:@"0"]];

    auto* zoom_menu_item = [[NSMenuItem alloc] initWithTitle:@"Zoom"
                                                      action:nil
                                               keyEquivalent:@""];
    [zoom_menu_item setSubmenu:zoom_menu];

    [submenu addItem:color_scheme_menu_item];
    [submenu addItem:zoom_menu_item];
    [submenu addItem:[NSMenuItem separatorItem]];

    [menu setSubmenu:submenu];
    return menu;
}

- (NSMenuItem*)createSettingsMenu
{
    auto* menu = [[NSMenuItem alloc] init];
    auto* submenu = [[NSMenu alloc] initWithTitle:@"Settings"];

    auto* search_engine_menu = [[NSMenu alloc] init];

    for (auto const& search_engine : WebView::search_engines()) {
        [search_engine_menu addItem:[[NSMenuItem alloc] initWithTitle:Ladybird::string_to_ns_string(search_engine.name)
                                                               action:@selector(setSearchEngine:)
                                                        keyEquivalent:@""]];
    }

    auto* search_engine_menu_item = [[NSMenuItem alloc] initWithTitle:@"Search Engine"
                                                               action:nil
                                                        keyEquivalent:@""];
    [search_engine_menu_item setSubmenu:search_engine_menu];

    [submenu addItem:search_engine_menu_item];

    [menu setSubmenu:submenu];
    return menu;
}

- (NSMenuItem*)createHistoryMenu
{
    auto* menu = [[NSMenuItem alloc] init];
    auto* submenu = [[NSMenu alloc] initWithTitle:@"History"];

    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Reload Page"
                                                action:@selector(reload:)
                                         keyEquivalent:@"r"]];
    [submenu addItem:[NSMenuItem separatorItem]];

    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Navigate Back"
                                                action:@selector(navigateBack:)
                                         keyEquivalent:@"["]];
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Navigate Forward"
                                                action:@selector(navigateForward:)
                                         keyEquivalent:@"]"]];
    [submenu addItem:[NSMenuItem separatorItem]];

    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Clear History"
                                                action:@selector(clearHistory:)
                                         keyEquivalent:@""]];

    [menu setSubmenu:submenu];
    return menu;
}

- (NSMenuItem*)createInspectMenu
{
    auto* menu = [[NSMenuItem alloc] init];
    auto* submenu = [[NSMenu alloc] initWithTitle:@"Inspect"];

    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"View Source"
                                                action:@selector(viewSource:)
                                         keyEquivalent:@""]];
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Open Inspector"
                                                action:@selector(openInspector:)
                                         keyEquivalent:@"I"]];
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Open Task Manager"
                                                action:@selector(openTaskManager:)
                                         keyEquivalent:@"M"]];

    [menu setSubmenu:submenu];
    return menu;
}

- (NSMenuItem*)createDebugMenu
{
    auto* menu = [[NSMenuItem alloc] init];
    auto* submenu = [[NSMenu alloc] initWithTitle:@"Debug"];

    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Dump DOM Tree"
                                                action:@selector(dumpDOMTree:)
                                         keyEquivalent:@""]];
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Dump Layout Tree"
                                                action:@selector(dumpLayoutTree:)
                                         keyEquivalent:@""]];
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Dump Paint Tree"
                                                action:@selector(dumpPaintTree:)
                                         keyEquivalent:@""]];
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Dump Stacking Context Tree"
                                                action:@selector(dumpStackingContextTree:)
                                         keyEquivalent:@""]];
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Dump Style Sheets"
                                                action:@selector(dumpStyleSheets:)
                                         keyEquivalent:@""]];
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Dump All Resolved Styles"
                                                action:@selector(dumpAllResolvedStyles:)
                                         keyEquivalent:@""]];
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Dump History"
                                                action:@selector(dumpHistory:)
                                         keyEquivalent:@""]];
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Dump Cookies"
                                                action:@selector(dumpCookies:)
                                         keyEquivalent:@""]];
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Dump Local Storage"
                                                action:@selector(dumpLocalStorage:)
                                         keyEquivalent:@""]];
    [submenu addItem:[NSMenuItem separatorItem]];

    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Show Line Box Borders"
                                                action:@selector(toggleLineBoxBorders:)
                                         keyEquivalent:@""]];
    [submenu addItem:[NSMenuItem separatorItem]];

    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Collect Garbage"
                                                action:@selector(collectGarbage:)
                                         keyEquivalent:@""]];
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Dump GC Graph"
                                                action:@selector(dumpGCGraph:)
                                         keyEquivalent:@""]];
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Clear Cache"
                                                action:@selector(clearCache:)
                                         keyEquivalent:@""]];
    [submenu addItem:[NSMenuItem separatorItem]];

    auto* spoof_user_agent_menu = [[NSMenu alloc] init];
    auto add_user_agent = [spoof_user_agent_menu](ByteString name) {
        [spoof_user_agent_menu addItem:[[NSMenuItem alloc] initWithTitle:Ladybird::string_to_ns_string(name)
                                                                  action:@selector(setUserAgentSpoof:)
                                                           keyEquivalent:@""]];
    };

    add_user_agent("Disabled");
    for (auto const& userAgent : WebView::user_agents)
        add_user_agent(userAgent.key);

    auto* spoof_user_agent_menu_item = [[NSMenuItem alloc] initWithTitle:@"Spoof User Agent"
                                                                  action:nil
                                                           keyEquivalent:@""];
    [spoof_user_agent_menu_item setSubmenu:spoof_user_agent_menu];

    [submenu addItem:spoof_user_agent_menu_item];
    [submenu addItem:[NSMenuItem separatorItem]];

    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Enable Scripting"
                                                action:@selector(toggleScripting:)
                                         keyEquivalent:@""]];
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Block Pop-ups"
                                                action:@selector(togglePopupBlocking:)
                                         keyEquivalent:@""]];
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Enable Same-Origin Policy"
                                                action:@selector(toggleSameOriginPolicy:)
                                         keyEquivalent:@""]];

    [menu setSubmenu:submenu];
    return menu;
}

- (NSMenuItem*)createWindowMenu
{
    auto* menu = [[NSMenuItem alloc] init];
    auto* submenu = [[NSMenu alloc] initWithTitle:@"Window"];

    [NSApp setWindowsMenu:submenu];

    [menu setSubmenu:submenu];
    return menu;
}

- (NSMenuItem*)createHelpMenu
{
    auto* menu = [[NSMenuItem alloc] init];
    auto* submenu = [[NSMenu alloc] initWithTitle:@"Help"];

    [NSApp setHelpMenu:submenu];

    [menu setSubmenu:submenu];
    return menu;
}

#pragma mark - NSApplicationDelegate

- (void)applicationDidFinishLaunching:(NSNotification*)notification
{
    Tab* tab = nil;

    for (auto const& url : m_initial_urls) {
        auto activate_tab = tab == nil ? Web::HTML::ActivateTab::Yes : Web::HTML::ActivateTab::No;

        auto* controller = [self createNewTab:url
                                      fromTab:tab
                                  activateTab:activate_tab];

        tab = (Tab*)[controller window];
    }

    m_initial_urls.clear();
}

- (void)applicationWillTerminate:(NSNotification*)notification
{
}

- (BOOL)applicationShouldTerminateAfterLastWindowClosed:(NSApplication*)sender
{
    return YES;
}

- (BOOL)validateMenuItem:(NSMenuItem*)item
{
    using enum Web::CSS::PreferredColorScheme;

    if ([item action] == @selector(setAutoPreferredColorScheme:)) {
        [item setState:(m_preferred_color_scheme == Auto) ? NSControlStateValueOn : NSControlStateValueOff];
    } else if ([item action] == @selector(setDarkPreferredColorScheme:)) {
        [item setState:(m_preferred_color_scheme == Dark) ? NSControlStateValueOn : NSControlStateValueOff];
    } else if ([item action] == @selector(setLightPreferredColorScheme:)) {
        [item setState:(m_preferred_color_scheme == Light) ? NSControlStateValueOn : NSControlStateValueOff];
    } else if ([item action] == @selector(setSearchEngine:)) {
        auto title = Ladybird::ns_string_to_string([item title]);
        [item setState:(m_search_engine.name == title) ? NSControlStateValueOn : NSControlStateValueOff];
    }

    return YES;
}

#pragma mark - TaskManagerDelegate

- (void)onTaskManagerClosed
{
    self.task_manager_controller = nil;
}

@end
