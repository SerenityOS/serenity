/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Try.h>
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibIPC/MultiServer.h>
#include <LibMain/Main.h>
#include <PreviewServer/ConnectionFromClient.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath cpath wpath unix thread accept"));

    Core::EventLoop loop;
    auto server = TRY(IPC::MultiServer<PreviewServer::ConnectionFromClient>::try_create("/tmp/session/%sid/portal/preview"));

    return loop.exec();
}
