/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <BrowserSettings/Defaults.h>

#import <Application/ApplicationDelegate.h>
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
    Optional<Browser::CookieJar> m_cookie_jar;

    Optional<StringView> m_webdriver_content_ipc_path;
}

@property (nonatomic, strong) NSMutableArray<TabController*>* managed_tabs;

- (NSMenuItem*)createApplicationMenu;
- (NSMenuItem*)createFileMenu;
- (NSMenuItem*)createEditMenu;
- (NSMenuItem*)createViewMenu;
- (NSMenuItem*)createHistoryMenu;
- (NSMenuItem*)createDebugMenu;
- (NSMenuItem*)createWindowsMenu;
- (NSMenuItem*)createHelpMenu;

@end

@implementation ApplicationDelegate

- (instancetype)init:(Optional<URL>)initial_url
              withCookieJar:(Browser::CookieJar)cookie_jar
    webdriverContentIPCPath:(StringView)webdriver_content_ipc_path
{
    if (self = [super init]) {
        [NSApp setMainMenu:[[NSMenu alloc] init]];

        [[NSApp mainMenu] addItem:[self createApplicationMenu]];
        [[NSApp mainMenu] addItem:[self createFileMenu]];
        [[NSApp mainMenu] addItem:[self createEditMenu]];
        [[NSApp mainMenu] addItem:[self createViewMenu]];
        [[NSApp mainMenu] addItem:[self createHistoryMenu]];
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

        // Reduce the tooltip delay, as the default delay feels quite long.
        [[NSUserDefaults standardUserDefaults] setObject:@100 forKey:@"NSInitialToolTipDelay"];
    }

    return self;
}

#pragma mark - Public methods

- (TabController*)createNewTab:(Optional<URL> const&)url
{
    return [self createNewTab:url activateTab:Web::HTML::ActivateTab::Yes];
}

- (TabController*)createNewTab:(Optional<URL> const&)url
                   activateTab:(Web::HTML::ActivateTab)activate_tab
{
    // This handle must be acquired before creating the new tab.
    auto* current_tab = (Tab*)[NSApp keyWindow];

    auto* controller = [[TabController alloc] init:url.value_or(m_new_tab_page_url)];
    [controller showWindow:nil];

    if (current_tab) {
        [[current_tab tabGroup] addWindow:controller.window];

        // FIXME: Can we create the tabbed window above without it becoming active in the first place?
        if (activate_tab == Web::HTML::ActivateTab::No) {
            [current_tab orderFront:nil];
        }
    }

    [self.managed_tabs addObject:controller];
    return controller;
}

- (void)removeTab:(TabController*)controller
{
    [self.managed_tabs removeObject:controller];
}

- (Browser::CookieJar&)cookieJar
{
    return *m_cookie_jar;
}

- (Optional<StringView> const&)webdriverContentIPCPath
{
    return m_webdriver_content_ipc_path;
}

#pragma mark - Private methods

- (void)closeCurrentTab:(id)sender
{
    auto* current_tab = (Tab*)[NSApp keyWindow];
    [current_tab close];
}

- (void)openLocation:(id)sender
{
    auto* current_tab = (Tab*)[NSApp keyWindow];
    auto* controller = (TabController*)[current_tab windowController];
    [controller focusLocationToolbarItem];
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
    [self createNewTab:m_initial_url];
}

- (void)applicationWillTerminate:(NSNotification*)notification
{
}

- (BOOL)applicationShouldTerminateAfterLastWindowClosed:(NSApplication*)sender
{
    return YES;
}

@end
