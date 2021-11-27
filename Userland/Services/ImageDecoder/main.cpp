/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <ImageDecoder/ClientConnection.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/System.h>
#include <LibIPC/ClientConnection.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    Core::EventLoop event_loop;
    TRY(Core::System::pledge("stdio recvfd sendfd unix"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto socket = TRY(Core::LocalSocket::take_over_accepted_socket_from_system_server());
    IPC::new_client_connection<ImageDecoder::ClientConnection>(move(socket), 1);
    TRY(Core::System::pledge("stdio recvfd sendfd"));
    return event_loop.exec();
}
