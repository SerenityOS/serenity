/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <BrowserSettings/Defaults.h>
#include <Ladybird/Utilities.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibMain/Main.h>
#include <LibWebView/CookieJar.h>
#include <LibWebView/Database.h>
#include <LibWebView/URL.h>

#import <Application/Application.h>
#import <Application/ApplicationDelegate.h>
#import <Application/EventLoopImplementation.h>
#import <UI/Tab.h>
#import <UI/TabController.h>

#if !__has_feature(objc_arc)
#    error "This project requires ARC"
#endif

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    [Application sharedApplication];

    Core::EventLoopManager::install(*new Ladybird::CFEventLoopManager);
    Core::EventLoop event_loop;

    platform_init();

    // NOTE: We only instantiate this to ensure that Gfx::FontDatabase has its default queries initialized.
    Gfx::FontDatabase::set_default_font_query("Katica 10 400 0");
    Gfx::FontDatabase::set_fixed_width_font_query("Csilla 10 400 0");

    Vector<StringView> raw_urls;
    StringView webdriver_content_ipc_path;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("The Ladybird web browser");
    args_parser.add_positional_argument(raw_urls, "URLs to open", "url", Core::ArgsParser::Required::No);
    args_parser.add_option(webdriver_content_ipc_path, "Path to WebDriver IPC for WebContent", "webdriver-content-path", 0, "path", Core::ArgsParser::OptionHideMode::CommandLineAndMarkdown);
    args_parser.parse(arguments);

    auto sql_server_paths = TRY(get_paths_for_helper_process("SQLServer"sv));
    auto database = TRY(WebView::Database::create(move(sql_server_paths)));
    auto cookie_jar = TRY(WebView::CookieJar::create(*database));

    URL new_tab_page_url = Browser::default_new_tab_url();
    Vector<URL> initial_urls;

    for (auto const& raw_url : raw_urls) {
        if (auto url = WebView::sanitize_url(raw_url); url.has_value())
            initial_urls.append(url.release_value());
    }

    if (initial_urls.is_empty())
        initial_urls.append(new_tab_page_url);

    auto* delegate = [[ApplicationDelegate alloc] init:move(initial_urls)
                                         newTabPageURL:move(new_tab_page_url)
                                         withCookieJar:move(cookie_jar)
                               webdriverContentIPCPath:webdriver_content_ipc_path];

    [NSApp setDelegate:delegate];
    [NSApp activateIgnoringOtherApps:YES];

    return event_loop.exec();
}
