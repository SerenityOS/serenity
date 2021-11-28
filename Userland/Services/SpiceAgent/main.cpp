/*
 * Copyright (c) 2021, Kyle Pereira <kyle@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SpiceAgent.h"
#include <LibC/fcntl.h>
#include <LibCore/System.h>
#include <LibIPC/ServerConnection.h>
#include <LibMain/Main.h>

static constexpr auto SPICE_DEVICE = "/dev/hvc0p1"sv;

ErrorOr<int> serenity_main(Main::Arguments)
{
    Core::EventLoop loop;

    TRY(Core::System::pledge("unix rpath wpath stdio sendfd recvfd", nullptr));
    TRY(Core::System::unveil(SPICE_DEVICE, "rw"));
    TRY(Core::System::unveil("/tmp/portal/clipboard", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    int serial_port_fd = TRY(Core::System::open(SPICE_DEVICE, O_RDWR));

    auto conn = TRY(ClipboardServerConnection::try_create());
    auto agent = SpiceAgent(serial_port_fd, conn);

    return loop.exec();
}
