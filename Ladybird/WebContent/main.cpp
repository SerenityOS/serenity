/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <Ladybird/FontPlugin.h>
#include <Ladybird/HelperProcess.h>
#include <Ladybird/ImageCodecPlugin.h>
#include <Ladybird/Utilities.h>
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
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Loader/ContentFilter.h>
#include <LibWeb/Loader/FrameLoader.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/PermissionsPolicy/AutoplayAllowlist.h>
#include <LibWeb/Platform/AudioCodecPluginAgnostic.h>
#include <LibWeb/Platform/EventLoopPluginSerenity.h>
#include <LibWeb/WebSockets/WebSocket.h>
#include <LibWebView/RequestServerAdapter.h>
#include <LibWebView/WebSocketClientAdapter.h>
#include <WebContent/ConnectionFromClient.h>
#include <WebContent/PageHost.h>
#include <WebContent/WebDriverConnection.h>

#if defined(HAVE_QT)
#    include <Ladybird/Qt/AudioCodecPluginQt.h>
#    include <Ladybird/Qt/EventLoopImplementationQt.h>
#    include <Ladybird/Qt/RequestManagerQt.h>
#    include <Ladybird/Qt/WebSocketClientManagerQt.h>
#    include <QCoreApplication>
#endif

static ErrorOr<void> load_content_filters();
static ErrorOr<void> load_autoplay_allowlist();
static ErrorOr<void> initialize_lagom_networking();

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
#if defined(HAVE_QT)
    QCoreApplication app(arguments.argc, arguments.argv);

    Core::EventLoopManager::install(*new Ladybird::EventLoopManagerQt);
#endif
    Core::EventLoop event_loop;

    platform_init();

    Web::Platform::EventLoopPlugin::install(*new Web::Platform::EventLoopPluginSerenity);
    Web::Platform::ImageCodecPlugin::install(*new Ladybird::ImageCodecPlugin);

    Web::Platform::AudioCodecPlugin::install_creation_hook([](auto loader) {
#if defined(HAVE_PULSEAUDIO)
        return Web::Platform::AudioCodecPluginAgnostic::create(move(loader));
#elif defined(HAVE_QT)
        return Ladybird::AudioCodecPluginQt::create(move(loader));
#else
        (void)loader;
        return Error::from_string_literal("Don't know how to initialize audio in this configuration!");
#endif
    });

    Web::FrameLoader::set_default_favicon_path(DeprecatedString::formatted("{}/res/icons/16x16/app-browser.png", s_serenity_resource_root));

    int webcontent_fd_passing_socket { -1 };
    bool is_layout_test_mode = false;
    bool use_lagom_networking = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(webcontent_fd_passing_socket, "File descriptor of the passing socket for the WebContent connection", "webcontent-fd-passing-socket", 'c', "webcontent_fd_passing_socket");
    args_parser.add_option(is_layout_test_mode, "Is layout test mode", "layout-test-mode", 0);
    args_parser.add_option(use_lagom_networking, "Enable Lagom servers for networking", "use-lagom-networking", 0);
    args_parser.parse(arguments);

#if defined(HAVE_QT)
    if (!use_lagom_networking) {
        Web::ResourceLoader::initialize(Ladybird::RequestManagerQt::create());
        Web::WebSockets::WebSocketClientManager::initialize(Ladybird::WebSocketClientManagerQt::create());
    } else
#endif
    {
        TRY(initialize_lagom_networking());
    }

    Web::HTML::Window::set_internals_object_exposed(is_layout_test_mode);

    VERIFY(webcontent_fd_passing_socket >= 0);

    Web::Platform::FontPlugin::install(*new Ladybird::FontPlugin(is_layout_test_mode));

    Web::FrameLoader::set_resource_directory_url(DeprecatedString::formatted("file://{}/res", s_serenity_resource_root));
    Web::FrameLoader::set_error_page_url(DeprecatedString::formatted("file://{}/res/html/error.html", s_serenity_resource_root));
    Web::FrameLoader::set_directory_page_url(DeprecatedString::formatted("file://{}/res/html/directory.html", s_serenity_resource_root));

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

static ErrorOr<void> initialize_lagom_networking()
{
    auto candidate_request_server_paths = TRY(get_paths_for_helper_process("RequestServer"sv));
    auto request_server_client = TRY(launch_request_server_process(candidate_request_server_paths, s_serenity_resource_root));
    Web::ResourceLoader::initialize(TRY(WebView::RequestServerAdapter::try_create(move(request_server_client))));

    auto candidate_web_socket_paths = TRY(get_paths_for_helper_process("WebSocket"sv));
    auto web_socket_client = TRY(launch_web_socket_process(candidate_web_socket_paths, s_serenity_resource_root));
    Web::WebSockets::WebSocketClientManager::initialize(TRY(WebView::WebSocketClientManagerAdapter::try_create(move(web_socket_client))));

    return {};
}
