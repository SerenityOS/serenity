/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <ImageDecoder/ClientConnection.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibIPC/ClientConnection.h>
#include <LibMain/Main.h>
#include <LibSystem/Wrappers.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    Core::EventLoop event_loop;
    TRY(System::pledge("stdio recvfd sendfd unix", nullptr));
    TRY(System::unveil(nullptr, nullptr));

    auto socket = Core::LocalSocket::take_over_accepted_socket_from_system_server();
    IPC::new_client_connection<ImageDecoder::ClientConnection>(socket.release_nonnull(), 1);
    TRY(System::pledge("stdio recvfd sendfd", nullptr));
    return event_loop.exec();
}
