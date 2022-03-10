/*
 * Copyright (c) 2022, Nikita Utkin <shockck84@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConnectionFromClient.h"
#include <AK/Error.h>
#include <LibCore/EventLoop.h>
#include <LibIPC/MultiServer.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    Core::EventLoop event_loop;
    auto server = TRY(IPC::MultiServer<SessionServer::ConnectionFromClient>::try_create());
    return event_loop.exec();
}
