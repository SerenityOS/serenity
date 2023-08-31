/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <BrowserSettings/Defaults.h>

#import <Application/ApplicationDelegate.h>
#import <UI/LadybirdWebView.h>
#import <UI/Tab.h>
#import <UI/TabController.h>
#import <Utilities/URL.h>

#if !__has_feature(objc_arc)
#    error "This project requires ARC"
#endif

@interface ApplicationDelegate ()
{
    Optional<URL> m_initial_url;
    URL m_new_tab_page_url;

    // This will always be populated, but we cannot have a non-default constructible instance variable.
    Optional<WebView::CookieJar> m_cookie_jar;

    Optional<StringView> m_webdriver_content_ipc_path;

    Web::CSS::PreferredColorScheme m_preferred_color_scheme;
}

@property (nonatomic, strong) NSMutableArray<TabController*>* managed_tabs;

- (NSMenuItem*)createApplicationMenu;
- (NSMenuItem*)createFileMenu;
- (NSMenuItem*)createEditMenu;
- (NSMenuItem*)createViewMenu;
- (NSMenuItem*)createHistoryMenu;
- (NSMenuItem*)createInspectMenu;
- (NSMenuItem*)createDebugMenu;
- (NSMenuItem*)createWindowsMenu;
- (NSMenuItem*)createHelpMenu;

@end

@implementation ApplicationDelegate

- (instancetype)init:(Optional<URL>)initial_url
              withCookieJar:(WebView::CookieJar)cookie_jar
    webdriverContentIPCPath:(StringView)webdriver_content_ipc_path
{
    if (self = [super init]) {
        [NSApp setMainMenu:[[NSMenu alloc] init]];

        [[NSApp mainMenu] addItem:[self createApplicationMenu]];
        [[NSApp mainMenu] addItem:[self createFileMenu]];
        [[NSApp mainMenu] addItem:[self createEditMenu]];
        [[NSApp mainMenu] addItem:[self createViewMenu]];
        [[NSApp mainMenu] addItem:[self createHistoryMenu]];
        [[NSApp mainMenu] addItem:[self createInspectMenu]];
        [[NSApp mainMenu] addItem:[self createDebugMenu]];
        [[NSApp mainMenu] addItem:[self createWindowsMenu]];
        [[NSApp mainMenu] addItem:[self createHelpMenu]];

        self.managed_tabs = [[NSMutableArray alloc] init];

        m_initial_url = move(initial_url);
        m_new_tab_page_url = Ladybird::rebase_url_on_serenity_resource_root(Browser::default_new_tab_url);

        m_cookie_jar = move(cookie_jar);

        if (!webdriver_content_ipc_path.is_empty()) {
            m_webdriver_content_ipc_path = webdriver_content_ipc_path;
        }

        m_preferred_color_scheme = Web::CSS::PreferredColorScheme::Auto;

        // Reduce the tooltip delay, as the default delay feels quite long.
        [[NSUserDefaults standardUserDefaults] setObject:@100 forKey:@"NSInitialToolTipDelay"];
    }

    return self;
}

#pragma mark - Public methods

- (TabController*)createNewTab:(Optional<URL> const&)url
                       fromTab:(Tab*)tab
                   activateTab:(Web::HTML::ActivateTab)activate_tab
{
    auto* controller = [self createNewTab:activate_tab fromTab:tab];
    [controller loadURL:url.value_or(m_new_tab_page_url)];

    return controller;
}

- (nonnull TabController*)createNewTab:(StringView)html
                                   url:(URL const&)url
                               fromTab:(nullable Tab*)tab
                           activateTab:(Web::HTML::ActivateTab)activate_tab
{
    auto* controller = [self createNewTab:activate_tab fromTab:tab];
    [controller loadHTML:html url:url];

    return controller;
}

- (void)removeTab:(TabController*)controller
{
    [self.managed_tabs removeObject:controller];
}

- (WebView::CookieJar&)cookieJar
{
    return *m_cookie_jar;
}

- (Optional<StringView> const&)webdriverContentIPCPath
{
    return m_webdriver_content_ipc_path;
}

- (Web::CSS::PreferredColorScheme)preferredColorScheme
{
    return m_preferred_color_scheme;
}

#pragma mark - Private methods

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

    [self.managed_tabs addObject:controller];
    return controller;
}

- (void)closeCurrentTab:(id)sender
{
    auto* current_window = [NSApp keyWindow];
    [current_window close];
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
                                                action:@selector(orderFrontStandardAboutPanel:)
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

    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Select all"
                                                action:@selector(selectAll:)
                                         keyEquivalent:@"a"]];

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

    [submenu addItem:color_scheme_menu_item];
    [submenu addItem:[NSMenuItem separatorItem]];

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
    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Open Console"
                                                action:@selector(openConsole:)
                                         keyEquivalent:@"J"]];

    [menu setSubmenu:submenu];
    return menu;
}

- (NSMenuItem*)createDebugMenu
{
    auto* menu = [[NSMenuItem alloc] init];
    auto* submenu = [[NSMenu alloc] initWithTitle:@"Debug"];

    [submenu addItem:[[NSMenuItem alloc] initWithTitle:@"Dump Cookies"
                                                action:@selector(dumpCookies:)
                                         keyEquivalent:@""]];

    [menu setSubmenu:submenu];
    return menu;
}

- (NSMenuItem*)createWindowsMenu
{
    auto* menu = [[NSMenuItem alloc] init];
    auto* submenu = [[NSMenu alloc] initWithTitle:@"Windows"];

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
    [self createNewTab:m_initial_url
               fromTab:nil
           activateTab:Web::HTML::ActivateTab::Yes];
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
    }
    if ([item action] == @selector(setDarkPreferredColorScheme:)) {
        [item setState:(m_preferred_color_scheme == Dark) ? NSControlStateValueOn : NSControlStateValueOff];
    }
    if ([item action] == @selector(setLightPreferredColorScheme:)) {
        [item setState:(m_preferred_color_scheme == Light) ? NSControlStateValueOn : NSControlStateValueOff];
    }

    return YES;
}

@end
