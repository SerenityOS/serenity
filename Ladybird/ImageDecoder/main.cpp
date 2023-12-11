/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <ImageDecoder/ConnectionFromClient.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibIPC/SingleServer.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    AK::set_rich_debug_enabled(true);

    int fd_passing_socket { -1 };
    StringView serenity_resource_root;

    Core::ArgsParser args_parser;
    args_parser.add_option(fd_passing_socket, "File descriptor of the fd passing socket", "fd-passing-socket", 'c', "fd-passing-socket");
    args_parser.add_option(serenity_resource_root, "Absolute path to directory for serenity resources", "serenity-resource-root", 'r', "serenity-resource-root");
    args_parser.parse(arguments);

    Core::EventLoop event_loop;

    auto client = TRY(IPC::take_over_accepted_client_from_system_server<ImageDecoder::ConnectionFromClient>());
    client->set_fd_passing_socket(TRY(Core::LocalSocket::adopt_fd(fd_passing_socket)));

    auto result = event_loop.exec();

    return result;
}
