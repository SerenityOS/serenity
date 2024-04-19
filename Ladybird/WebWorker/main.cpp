/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Ladybird/FontPlugin.h>
#include <Ladybird/HelperProcess.h>
#include <Ladybird/Utilities.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibIPC/SingleServer.h>
#include <LibMain/Main.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Loader/GeneratedPagesLoader.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Platform/EventLoopPlugin.h>
#include <LibWeb/Platform/EventLoopPluginSerenity.h>
#include <LibWeb/Platform/FontPluginSerenity.h>
#include <LibWeb/WebSockets/WebSocket.h>
#include <LibWebView/RequestServerAdapter.h>
#include <LibWebView/WebSocketClientAdapter.h>
#include <WebWorker/ConnectionFromClient.h>

static ErrorOr<void> initialize_lagom_networking(int request_server_socket, int request_server_fd_passing_socket);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    AK::set_rich_debug_enabled(true);

    int fd_passing_socket { -1 };
    int request_server_socket { -1 };
    int request_server_fd_passing_socket { -1 };
    StringView serenity_resource_root;

    Core::ArgsParser args_parser;
    args_parser.add_option(fd_passing_socket, "File descriptor of the fd passing socket", "fd-passing-socket", 'c', "fd-passing-socket");
    args_parser.add_option(request_server_socket, "File descriptor of the request server socket", "request-server-socket", 's', "request-server-socket");
    args_parser.add_option(request_server_fd_passing_socket, "File descriptor of the request server fd passing socket", "request-server-fd-passing-socket", 'f', "request-server-fd-passing-socket");
    args_parser.add_option(serenity_resource_root, "Absolute path to directory for serenity resources", "serenity-resource-root", 'r', "serenity-resource-root");
    args_parser.parse(arguments);

    platform_init();

    Web::Platform::EventLoopPlugin::install(*new Web::Platform::EventLoopPluginSerenity);
    Core::EventLoop event_loop;

    Web::Platform::FontPlugin::install(*new Web::Platform::FontPluginSerenity);

    TRY(initialize_lagom_networking(request_server_socket, request_server_fd_passing_socket));

    VERIFY(fd_passing_socket >= 0);

    TRY(Web::Bindings::initialize_main_thread_vm());

    auto client = TRY(IPC::take_over_accepted_client_from_system_server<WebWorker::ConnectionFromClient>());
    client->set_fd_passing_socket(TRY(Core::LocalSocket::adopt_fd(fd_passing_socket)));

    return event_loop.exec();
}

static ErrorOr<void> initialize_lagom_networking(int request_server_socket, int request_server_fd_passing_socket)
{
    auto socket = TRY(Core::LocalSocket::adopt_fd(request_server_socket));
    TRY(socket->set_blocking(true));

    auto new_client = TRY(try_make_ref_counted<Protocol::RequestClient>(move(socket)));
    new_client->set_fd_passing_socket(TRY(Core::LocalSocket::adopt_fd(request_server_fd_passing_socket)));

    Web::ResourceLoader::initialize(TRY(WebView::RequestServerAdapter::try_create(move(new_client))));

    return {};
}
