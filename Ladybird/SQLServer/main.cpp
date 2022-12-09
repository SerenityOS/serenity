/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#define AK_DONT_REPLACE_STD

#include <AK/DeprecatedString.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Directory.h>
#include <LibCore/EventLoop.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/Stream.h>
#include <LibCore/SystemServerTakeover.h>
#include <LibMain/Main.h>
#include <QSocketNotifier>
#include <SQLServer/ConnectionFromClient.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    int sql_server_fd_passing_socket { -1 };

    Core::ArgsParser args_parser;
    args_parser.add_option(sql_server_fd_passing_socket, "File descriptor of the passing socket for the SQLServer connection", "sql-server-fd-passing-socket", 's', "sql_server_fd_passing_socket");
    args_parser.parse(arguments);

    VERIFY(sql_server_fd_passing_socket >= 0);

    auto database_path = DeprecatedString::formatted("{}/Ladybird", Core::StandardPaths::data_directory());
    TRY(Core::Directory::create(database_path, Core::Directory::CreateDirectories::Yes));

    Core::EventLoop loop;

    auto socket = TRY(Core::take_over_socket_from_system_server("SQLServer"sv));
    auto client = TRY(SQLServer::ConnectionFromClient::try_create(move(socket), 1));
    client->set_fd_passing_socket(TRY(Core::Stream::LocalSocket::adopt_fd(sql_server_fd_passing_socket)));
    client->set_database_path(move(database_path));
    client->on_disconnect = [&]() {
        loop.quit(0);
    };

    return loop.exec();
}
