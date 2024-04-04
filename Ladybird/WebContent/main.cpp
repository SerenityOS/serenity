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
#include <LibCore/Process.h>
#include <LibCore/Resource.h>
#include <LibCore/System.h>
#include <LibCore/SystemServerTakeover.h>
#include <LibIPC/ConnectionFromClient.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibMain/Main.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Loader/ContentFilter.h>
#include <LibWeb/Loader/GeneratedPagesLoader.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/PermissionsPolicy/AutoplayAllowlist.h>
#include <LibWeb/Platform/AudioCodecPluginAgnostic.h>
#include <LibWeb/Platform/EventLoopPluginSerenity.h>
#include <LibWeb/WebSockets/WebSocket.h>
#include <LibWebView/RequestServerAdapter.h>
#include <LibWebView/WebSocketClientAdapter.h>
#include <WebContent/ConnectionFromClient.h>
#include <WebContent/PageClient.h>
#include <WebContent/WebDriverConnection.h>

#if defined(HAVE_QT)
#    include <Ladybird/Qt/EventLoopImplementationQt.h>
#    include <Ladybird/Qt/RequestManagerQt.h>
#    include <QCoreApplication>

#    if defined(HAVE_QT_MULTIMEDIA)
#        include <Ladybird/Qt/AudioCodecPluginQt.h>
#    endif
#endif

#if defined(AK_OS_MACOS)
#    include <LibWebView/Platform/ProcessStatisticsMach.h>
#endif

static ErrorOr<void> load_content_filters();
static ErrorOr<void> load_autoplay_allowlist();
static ErrorOr<void> initialize_lagom_networking(Vector<ByteString> const& certificates);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    AK::set_rich_debug_enabled(true);

#if defined(HAVE_QT)
    QCoreApplication app(arguments.argc, arguments.argv);

    Core::EventLoopManager::install(*new Ladybird::EventLoopManagerQt);
#endif
    Core::EventLoop event_loop;

    platform_init();

    Web::Platform::EventLoopPlugin::install(*new Web::Platform::EventLoopPluginSerenity);
    Web::Platform::ImageCodecPlugin::install(*new Ladybird::ImageCodecPlugin);

    Web::Platform::AudioCodecPlugin::install_creation_hook([](auto loader) {
#if defined(HAVE_QT_MULTIMEDIA)
        return Ladybird::AudioCodecPluginQt::create(move(loader));
#elif defined(AK_OS_MACOS) || defined(HAVE_PULSEAUDIO)
        return Web::Platform::AudioCodecPluginAgnostic::create(move(loader));
#else
        (void)loader;
        return Error::from_string_literal("Don't know how to initialize audio in this configuration!");
#endif
    });

    StringView command_line {};
    StringView executable_path {};
    StringView mach_server_name {};
    Vector<ByteString> certificates;
    int webcontent_fd_passing_socket { -1 };
    bool is_layout_test_mode = false;
    bool use_lagom_networking = false;
    bool use_gpu_painting = false;
    bool wait_for_debugger = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(command_line, "Chrome process command line", "command-line", 0, "command_line");
    args_parser.add_option(executable_path, "Chrome process executable path", "executable-path", 0, "executable_path");
    args_parser.add_option(certificates, "Path to a certificate file", "certificate", 'C', "certificate");
    args_parser.add_option(webcontent_fd_passing_socket, "File descriptor of the passing socket for the WebContent connection", "webcontent-fd-passing-socket", 'c', "webcontent_fd_passing_socket");
    args_parser.add_option(is_layout_test_mode, "Is layout test mode", "layout-test-mode", 0);
    args_parser.add_option(use_lagom_networking, "Enable Lagom servers for networking", "use-lagom-networking", 0);
    args_parser.add_option(use_gpu_painting, "Enable GPU painting", "use-gpu-painting", 0);
    args_parser.add_option(wait_for_debugger, "Wait for debugger", "wait-for-debugger", 0);
    args_parser.add_option(mach_server_name, "Mach server name", "mach-server-name", 0, "mach_server_name");

    args_parser.parse(arguments);

    if (wait_for_debugger) {
        Core::Process::wait_for_debugger_and_break();
    }

    Web::set_chrome_process_command_line(command_line);
    Web::set_chrome_process_executable_path(executable_path);
    if (use_gpu_painting) {
        WebContent::PageClient::set_use_gpu_painter();
    }

#if defined(AK_OS_MACOS)
    if (!mach_server_name.is_empty()) {
        WebView::register_with_mach_server(mach_server_name);
    }
#endif

#if defined(HAVE_QT)
    if (!use_lagom_networking)
        Web::ResourceLoader::initialize(Ladybird::RequestManagerQt::create());
    else
#endif
        TRY(initialize_lagom_networking(certificates));

    Web::HTML::Window::set_internals_object_exposed(is_layout_test_mode);

    VERIFY(webcontent_fd_passing_socket >= 0);

    Web::Platform::FontPlugin::install(*new Ladybird::FontPlugin(is_layout_test_mode));

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
    auto buffer = TRY(ByteBuffer::create_uninitialized(4096));

    auto resource = TRY(Core::Resource::load_from_uri("resource://ladybird/BrowserContentFilters.txt"sv));
    auto ad_filter_list = TRY(InputBufferedSeekable<FixedMemoryStream>::create(make<FixedMemoryStream>(resource->data())));

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
    auto buffer = TRY(ByteBuffer::create_uninitialized(4096));

    auto resource = TRY(Core::Resource::load_from_uri("resource://ladybird/BrowserAutoplayAllowlist.txt"sv));
    auto allowlist = TRY(InputBufferedSeekable<FixedMemoryStream>::create(make<FixedMemoryStream>(resource->data())));

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

static ErrorOr<void> initialize_lagom_networking(Vector<ByteString> const& certificates)
{
    auto candidate_request_server_paths = TRY(get_paths_for_helper_process("RequestServer"sv));
    auto request_server_client = TRY(launch_request_server_process(candidate_request_server_paths, s_serenity_resource_root, certificates));
    Web::ResourceLoader::initialize(TRY(WebView::RequestServerAdapter::try_create(move(request_server_client))));

    return {};
}
