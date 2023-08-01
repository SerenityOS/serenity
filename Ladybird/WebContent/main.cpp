/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "../AudioCodecPluginLadybird.h"
#include "../EventLoopImplementationQt.h"
#include "../FontPluginLadybird.h"
#include "../ImageCodecPluginLadybird.h"
#include "../RequestManagerQt.h"
#include "../Utilities.h"
#include "../WebSocketClientManagerLadybird.h"
#include <AK/LexicalPath.h>
#include <LibAudio/Loader.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/System.h>
#include <LibCore/SystemServerTakeover.h>
#include <LibIPC/ConnectionFromClient.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibMain/Main.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Loader/ContentFilter.h>
#include <LibWeb/Loader/FrameLoader.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/PermissionsPolicy/AutoplayAllowlist.h>
#include <LibWeb/Platform/EventLoopPluginSerenity.h>
#include <LibWeb/WebSockets/WebSocket.h>
#include <QCoreApplication>
#include <QTimer>
#include <WebContent/ConnectionFromClient.h>
#include <WebContent/PageHost.h>
#include <WebContent/WebDriverConnection.h>

static ErrorOr<void> load_content_filters();
static ErrorOr<void> load_autoplay_allowlist();

extern DeprecatedString s_serenity_resource_root;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    QCoreApplication app(arguments.argc, arguments.argv);

    Core::EventLoopManager::install(*new Ladybird::EventLoopManagerQt);
    Core::EventLoop event_loop;

    platform_init();

    Web::Platform::EventLoopPlugin::install(*new Web::Platform::EventLoopPluginSerenity);
    Web::Platform::ImageCodecPlugin::install(*new Ladybird::ImageCodecPluginLadybird);

    Web::Platform::AudioCodecPlugin::install_creation_hook([](auto loader) {
        return Ladybird::AudioCodecPluginLadybird::create(move(loader));
    });

    Web::ResourceLoader::initialize(RequestManagerQt::create());
    Web::WebSockets::WebSocketClientManager::initialize(Ladybird::WebSocketClientManagerLadybird::create());

    Web::FrameLoader::set_default_favicon_path(DeprecatedString::formatted("{}/res/icons/16x16/app-browser.png", s_serenity_resource_root));

    int webcontent_fd_passing_socket { -1 };
    bool is_layout_test_mode = false;
    bool use_javascript_bytecode = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(webcontent_fd_passing_socket, "File descriptor of the passing socket for the WebContent connection", "webcontent-fd-passing-socket", 'c', "webcontent_fd_passing_socket");
    args_parser.add_option(is_layout_test_mode, "Is layout test mode", "layout-test-mode", 0);
    args_parser.add_option(use_javascript_bytecode, "Enable JavaScript bytecode VM", "use-bytecode", 0);
    args_parser.parse(arguments);

    JS::Bytecode::Interpreter::set_enabled(use_javascript_bytecode);

    VERIFY(webcontent_fd_passing_socket >= 0);

    Web::Platform::FontPlugin::install(*new Ladybird::FontPluginLadybird(is_layout_test_mode));

    Web::FrameLoader::set_error_page_url(DeprecatedString::formatted("file://{}/res/html/error.html", s_serenity_resource_root));

    TRY(Web::Bindings::initialize_main_thread_vm());

    auto maybe_content_filter_error = load_content_filters();
    if (maybe_content_filter_error.is_error())
        dbgln("Failed to load content filters: {}", maybe_content_filter_error.error());

    auto maybe_autoplay_allowlist_error = load_autoplay_allowlist();
    if (maybe_autoplay_allowlist_error.is_error())
        dbgln("Failed to load autoplay allowlist: {}", maybe_autoplay_allowlist_error.error());

    auto webcontent_socket = TRY(Core::take_over_socket_from_system_server("WebContent"sv));
    auto webcontent_client = TRY(WebContent::ConnectionFromClient::try_create(move(webcontent_socket)));
    webcontent_client->set_fd_passing_socket(TRY(Core::LocalSocket::adopt_fd(webcontent_fd_passing_socket)));

    return event_loop.exec();
}

static ErrorOr<void> load_content_filters()
{
    auto file_or_error = Core::File::open(DeprecatedString::formatted("{}/home/anon/.config/BrowserContentFilters.txt", s_serenity_resource_root), Core::File::OpenMode::Read);
    if (file_or_error.is_error())
        file_or_error = Core::File::open(DeprecatedString::formatted("{}/res/ladybird/BrowserContentFilters.txt", s_serenity_resource_root), Core::File::OpenMode::Read);
    if (file_or_error.is_error())
        return file_or_error.release_error();

    auto file = file_or_error.release_value();
    auto ad_filter_list = TRY(Core::InputBufferedFile::create(move(file)));
    auto buffer = TRY(ByteBuffer::create_uninitialized(4096));

    Vector<String> patterns;

    while (TRY(ad_filter_list->can_read_line())) {
        auto line = TRY(ad_filter_list->read_line(buffer));
        if (line.is_empty())
            continue;

        auto pattern = TRY(String::from_utf8(line));
        TRY(patterns.try_append(move(pattern)));
    }

    auto& content_filter = Web::ContentFilter::the();
    TRY(content_filter.set_patterns(patterns));

    return {};
}

static ErrorOr<void> load_autoplay_allowlist()
{
    auto file_or_error = Core::File::open(TRY(String::formatted("{}/home/anon/.config/BrowserAutoplayAllowlist.txt", s_serenity_resource_root)), Core::File::OpenMode::Read);
    if (file_or_error.is_error())
        file_or_error = Core::File::open(TRY(String::formatted("{}/res/ladybird/BrowserAutoplayAllowlist.txt", s_serenity_resource_root)), Core::File::OpenMode::Read);
    if (file_or_error.is_error())
        return file_or_error.release_error();

    auto file = file_or_error.release_value();
    auto allowlist = TRY(Core::InputBufferedFile::create(move(file)));
    auto buffer = TRY(ByteBuffer::create_uninitialized(4096));

    Vector<String> origins;

    while (TRY(allowlist->can_read_line())) {
        auto line = TRY(allowlist->read_line(buffer));
        if (line.is_empty())
            continue;

        auto domain = TRY(String::from_utf8(line));
        TRY(origins.try_append(move(domain)));
    }

    auto& autoplay_allowlist = Web::PermissionsPolicy::AutoplayAllowlist::the();
    TRY(autoplay_allowlist.enable_for_origins(origins));

    return {};
}
