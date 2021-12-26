/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <SQLServer/ClientConnection.h>
#include <stdio.h>
#include <sys/stat.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio accept unix rpath wpath cpath"));

    if (mkdir("/home/anon/sql", 0700) < 0 && errno != EEXIST) {
        perror("mkdir");
        return 1;
    }

    TRY(Core::System::unveil("/home/anon/sql", "rwc"));
    TRY(Core::System::unveil(nullptr, nullptr));

    Core::EventLoop event_loop;
    auto server = TRY(Core::LocalServer::try_create());
    bool ok = server->take_over_from_system_server();
    VERIFY(ok);

    server->on_accept = [&](auto client_socket) {
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        (void)IPC::new_client_connection<SQLServer::ClientConnection>(client_socket, client_id);
    };

    return event_loop.exec();
}
