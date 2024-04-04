/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <BrowserSettings/Defaults.h>
#include <Ladybird/MachPortServer.h>
#include <Ladybird/Types.h>
#include <Ladybird/Utilities.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibMain/Main.h>
#include <LibWebView/CookieJar.h>
#include <LibWebView/Database.h>
#include <LibWebView/ProcessManager.h>
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
    AK::set_rich_debug_enabled(true);

    [Application sharedApplication];

    Core::EventLoopManager::install(*new Ladybird::CFEventLoopManager);
    Core::EventLoop event_loop;

    platform_init();

    // NOTE: We only instantiate this to ensure that Gfx::FontDatabase has its default queries initialized.
    Gfx::FontDatabase::set_default_font_query("Katica 10 400 0");
    Gfx::FontDatabase::set_fixed_width_font_query("Csilla 10 400 0");

    Vector<StringView> raw_urls;
    Vector<ByteString> certificates;
    StringView webdriver_content_ipc_path;
    bool use_gpu_painting = false;
    bool debug_web_content = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("The Ladybird web browser");
    args_parser.add_positional_argument(raw_urls, "URLs to open", "url", Core::ArgsParser::Required::No);
    args_parser.add_option(webdriver_content_ipc_path, "Path to WebDriver IPC for WebContent", "webdriver-content-path", 0, "path", Core::ArgsParser::OptionHideMode::CommandLineAndMarkdown);
    args_parser.add_option(use_gpu_painting, "Enable GPU painting", "enable-gpu-painting", 0);
    args_parser.add_option(debug_web_content, "Wait for debugger to attach to WebContent", "debug-web-content", 0);
    args_parser.add_option(certificates, "Path to a certificate file", "certificate", 'C', "certificate");
    args_parser.parse(arguments);

    WebView::ProcessManager::initialize();

    auto mach_port_server = make<Ladybird::MachPortServer>();
    set_mach_server_name(mach_port_server->server_port_name());
    mach_port_server->on_receive_child_mach_port = [](auto pid, auto port) {
        WebView::ProcessManager::the().add_process(pid, move(port));
    };

    auto sql_server_paths = TRY(get_paths_for_helper_process("SQLServer"sv));
    auto database = TRY(WebView::Database::create(move(sql_server_paths)));
    auto cookie_jar = TRY(WebView::CookieJar::create(*database));

    URL::URL new_tab_page_url = Browser::default_new_tab_url;
    Vector<URL::URL> initial_urls;

    for (auto const& raw_url : raw_urls) {
        if (auto url = WebView::sanitize_url(raw_url); url.has_value())
            initial_urls.append(url.release_value());
    }

    if (initial_urls.is_empty())
        initial_urls.append(new_tab_page_url);

    StringBuilder command_line_builder;
    command_line_builder.join(' ', arguments.strings);
    Ladybird::WebContentOptions web_content_options {
        .command_line = MUST(command_line_builder.to_string()),
        .executable_path = MUST(String::from_byte_string(MUST(Core::System::current_executable_path()))),
        .certificates = move(certificates),
        .enable_gpu_painting = use_gpu_painting ? Ladybird::EnableGPUPainting::Yes : Ladybird::EnableGPUPainting::No,
        .use_lagom_networking = Ladybird::UseLagomNetworking::Yes,
        .wait_for_debugger = debug_web_content ? Ladybird::WaitForDebugger::Yes : Ladybird::WaitForDebugger::No,
    };

    auto* delegate = [[ApplicationDelegate alloc] init:move(initial_urls)
                                         newTabPageURL:move(new_tab_page_url)
                                         withCookieJar:move(cookie_jar)
                                     webContentOptions:web_content_options
                               webdriverContentIPCPath:webdriver_content_ipc_path];

    [NSApp setDelegate:delegate];

    return event_loop.exec();
}
