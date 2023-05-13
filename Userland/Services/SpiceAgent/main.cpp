/*
 * Copyright (c) 2021, Kyle Pereira <kyle@xylepereira.me>
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SpiceAgent.h"
#include <LibCore/System.h>
#include <LibIPC/ConnectionToServer.h>
#include <LibMain/Main.h>
#include <fcntl.h>

static constexpr auto SPICE_DEVICE = "/dev/hvc0p1"sv;

ErrorOr<int> serenity_main(Main::Arguments)
{
    Core::EventLoop loop;

    // FIXME: Make Core::File support reading and writing, but without creating:
    //        By default, Core::File opens the file descriptor with O_CREAT when using OpenMode::Write (and subsequently, OpenMode::ReadWrite).
    //        To minimise confusion for people that have already used Core::File, we can probably just do `OpenMode::ReadWrite | OpenMode::DontCreate`.
    TRY(Core::System::pledge("unix rpath wpath stdio sendfd recvfd cpath"));
    TRY(Core::System::unveil(SPICE_DEVICE, "rwc"sv));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/clipboard", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto agent = TRY(SpiceAgent::SpiceAgent::create(SPICE_DEVICE));
    TRY(agent->start());

    return loop.exec();
}
