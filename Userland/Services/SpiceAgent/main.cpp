/*
 * Copyright (c) 2021, Kyle Pereira <kyle@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SpiceAgent.h"
#include <YAK/Format.h>
#include <LibC/fcntl.h>
#include <LibC/unistd.h>
#include <LibIPC/ServerConnection.h>

static constexpr auto SPICE_DEVICE = "/dev/hvc0p1";

int main()
{
    Core::EventLoop loop;

    if (pledge("unix rpath wpath stdio sendfd recvfd", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil(SPICE_DEVICE, "rw") < 0) {
        perror("unveil");
        return 1;
    }
    if (unveil("/tmp/portal/clipboard", "rw") < 0) {
        perror("unveil");
        return 1;
    }
    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    int serial_port_fd = open(SPICE_DEVICE, O_RDWR);
    if (serial_port_fd < 0) {
        dbgln("Couldn't open spice serial port!");
        return 1;
    }

    auto conn = ClipboardServerConnection::construct();
    auto agent = SpiceAgent(serial_port_fd, conn);

    return loop.exec();
}
