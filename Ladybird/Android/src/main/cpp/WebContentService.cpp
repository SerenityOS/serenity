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
#include <LibWeb/Loader/FrameLoader.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Platform/AudioCodecPluginAgnostic.h>
#include <LibWeb/Platform/EventLoopPluginSerenity.h>
#include <LibWebView/RequestServerAdapter.h>
#include <LibWebView/WebSocketClientAdapter.h>
#include <WebContent/ConnectionFromClient.h>
#include <WebContent/PageHost.h>

static ErrorOr<NonnullRefPtr<Protocol::RequestClient>> bind_request_server_service();

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

    bool is_layout_test_mode = false;

    Web::HTML::Window::set_internals_object_exposed(is_layout_test_mode);
    Web::Platform::FontPlugin::install(*new Ladybird::FontPlugin(is_layout_test_mode));

    Web::FrameLoader::set_resource_directory_url(DeprecatedString::formatted("file://{}/res", s_serenity_resource_root));
    Web::FrameLoader::set_error_page_url(DeprecatedString::formatted("file://{}/res/html/error.html", s_serenity_resource_root));
    Web::FrameLoader::set_directory_page_url(DeprecatedString::formatted("file://{}/res/html/directory.html", s_serenity_resource_root));

    TRY(Web::Bindings::initialize_main_thread_vm());

    auto webcontent_socket = TRY(Core::LocalSocket::adopt_fd(ipc_socket));
    auto webcontent_client = TRY(WebContent::ConnectionFromClient::try_create(move(webcontent_socket)));
    webcontent_client->set_fd_passing_socket(TRY(Core::LocalSocket::adopt_fd(fd_passing_socket)));

    return event_loop.exec();
}

ErrorOr<NonnullRefPtr<Protocol::RequestClient>> bind_request_server_service()
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
    bind_request_server_java(server_fd, server_fd_passing_fd);

    auto socket = TRY(Core::LocalSocket::adopt_fd(ui_fd));
    TRY(socket->set_blocking(true));

    auto new_client = TRY(try_make_ref_counted<Protocol::RequestClient>(move(socket)));
    new_client->set_fd_passing_socket(TRY(Core::LocalSocket::adopt_fd(ui_fd_passing_fd)));

    return new_client;
}
