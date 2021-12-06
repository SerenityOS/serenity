/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <ImageDecoder/ClientConnection.h>
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibIPC/SingleServer.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    Core::EventLoop event_loop;
    TRY(Core::System::pledge("stdio recvfd sendfd unix"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto client = TRY(IPC::take_over_accepted_client_from_system_server<ImageDecoder::ClientConnection>());

    TRY(Core::System::pledge("stdio recvfd sendfd"));
    return event_loop.exec();
}
