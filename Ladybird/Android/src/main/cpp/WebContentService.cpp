/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebContentService.h"
#include "LadybirdServiceBase.h"
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
#include <LibIPC/ConnectionFromClient.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Loader/ContentFilter.h>
#include <LibWeb/Loader/FrameLoader.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/PermissionsPolicy/AutoplayAllowlist.h>
#include <LibWeb/Platform/AudioCodecPluginAgnostic.h>
#include <LibWeb/Platform/EventLoopPluginSerenity.h>
#include <LibWebView/RequestServerAdapter.h>
#include <LibWebView/WebSocketClientAdapter.h>
#include <WebContent/ConnectionFromClient.h>
#include <WebContent/PageHost.h>

template<typename Client>
static ErrorOr<NonnullRefPtr<Client>> bind_service(void (*bind_method)(int, int));

static ErrorOr<NonnullRefPtr<Protocol::RequestClient>> bind_request_server_service()
{
    return bind_service<Protocol::RequestClient>(&bind_request_server_java);
}

static ErrorOr<NonnullRefPtr<Protocol::WebSocketClient>> bind_web_socket_service()
{
    return bind_service<Protocol::WebSocketClient>(&bind_web_socket_java);
}

static ErrorOr<void> load_content_filters();
static ErrorOr<void> load_autoplay_allowlist();

ErrorOr<int> service_main(int ipc_socket, int fd_passing_socket)
{
    Core::EventLoop event_loop;

    Web::Platform::EventLoopPlugin::install(*new Web::Platform::EventLoopPluginSerenity);
    Web::Platform::ImageCodecPlugin::install(*new Ladybird::ImageCodecPlugin);

    Web::Platform::AudioCodecPlugin::install_creation_hook([](auto loader) {
        (void)loader;
        return Error::from_string_literal("Don't know how to initialize audio in this configuration!");
    });

    Web::FrameLoader::set_default_favicon_path(DeprecatedString::formatted("{}/res/icons/16x16/app-browser.png", s_serenity_resource_root));

    auto request_server_client = TRY(bind_request_server_service());
    Web::ResourceLoader::initialize(TRY(WebView::RequestServerAdapter::try_create(move(request_server_client))));

    auto web_socket_client = TRY(bind_web_socket_service());
    Web::WebSockets::WebSocketClientManager::initialize(TRY(WebView::WebSocketClientManagerAdapter::try_create(move(web_socket_client))));

    bool is_layout_test_mode = false;

    Web::HTML::Window::set_internals_object_exposed(is_layout_test_mode);
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

    auto webcontent_socket = TRY(Core::LocalSocket::adopt_fd(ipc_socket));
    auto webcontent_client = TRY(WebContent::ConnectionFromClient::try_create(move(webcontent_socket)));
    webcontent_client->set_fd_passing_socket(TRY(Core::LocalSocket::adopt_fd(fd_passing_socket)));

    return event_loop.exec();
}

template<typename Client>
static ErrorOr<NonnullRefPtr<Client>> bind_service(void (*bind_method)(int, int))
{
    int socket_fds[2] {};
    TRY(Core::System::socketpair(AF_LOCAL, SOCK_STREAM, 0, socket_fds));

    int ui_fd = socket_fds[0];
    int server_fd = socket_fds[1];

    int fd_passing_socket_fds[2] {};
    TRY(Core::System::socketpair(AF_LOCAL, SOCK_STREAM, 0, fd_passing_socket_fds));

    int ui_fd_passing_fd = fd_passing_socket_fds[0];
    int server_fd_passing_fd = fd_passing_socket_fds[1];

    // NOTE: The java object takes ownership of the socket fds
    (*bind_method)(server_fd, server_fd_passing_fd);

    auto socket = TRY(Core::LocalSocket::adopt_fd(ui_fd));
    TRY(socket->set_blocking(true));

    auto new_client = TRY(try_make_ref_counted<Client>(move(socket)));
    new_client->set_fd_passing_socket(TRY(Core::LocalSocket::adopt_fd(ui_fd_passing_fd)));

    return new_client;
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
