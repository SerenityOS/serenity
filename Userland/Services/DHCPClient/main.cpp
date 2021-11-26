/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DHCPv4Client.h"
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <stdio.h>
#include <unistd.h>

int main([[maybe_unused]] int argc, [[maybe_unused]] char** argv)
{
    if (pledge("stdio unix inet cpath rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Core::EventLoop event_loop;

    if (unveil("/proc/net/", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    auto client = DHCPv4Client::construct();

    if (pledge("stdio inet cpath rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    return event_loop.exec();
}
