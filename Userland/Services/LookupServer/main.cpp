/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LookupServer.h"
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <stdio.h>
#include <unistd.h>

int main([[maybe_unused]] int argc, [[maybe_unused]] char** argv)
{
    if (pledge("stdio accept unix inet cpath rpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Core::EventLoop event_loop;
    auto server = LookupServer::LookupServer::construct();

    if (pledge("stdio accept inet rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    unveil("/proc/net/adapters", "r");
    unveil(nullptr, nullptr);

    return event_loop.exec();
}
