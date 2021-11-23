/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibIPC/ClientConnection.h>
#include <LibMain/Main.h>
#include <LibSystem/Wrappers.h>
#include <WebContent/ClientConnection.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    Core::EventLoop event_loop;
    TRY(System::pledge("stdio recvfd sendfd accept unix rpath", nullptr));
    TRY(System::unveil("/res", "r"));
    TRY(System::unveil("/tmp/portal/request", "rw"));
    TRY(System::unveil("/tmp/portal/image", "rw"));
    TRY(System::unveil("/tmp/portal/websocket", "rw"));
    TRY(System::unveil(nullptr, nullptr));

    auto socket = Core::LocalSocket::take_over_accepted_socket_from_system_server();
    VERIFY(socket);
    IPC::new_client_connection<WebContent::ClientConnection>(socket.release_nonnull(), 1);
    return event_loop.exec();
}
