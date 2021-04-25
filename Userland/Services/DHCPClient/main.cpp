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
#ifdef __serenity__
    if (pledge("stdio unix inet cpath rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
#endif

    Core::EventLoop event_loop;

#ifdef __serenity__
    if (unveil("/proc/net/", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);
#endif

    auto client = DHCPv4Client::construct();

#ifdef __serenity__
    if (pledge("stdio inet cpath rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
#endif

    return event_loop.exec();
}
