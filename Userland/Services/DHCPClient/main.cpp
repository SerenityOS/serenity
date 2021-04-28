/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DHCPv4Client.h"
#include <AK/Debug.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/String.h>
#include <AK/StringUtils.h>
#include <AK/Types.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/LocalServer.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

int main([[maybe_unused]] int argc, [[maybe_unused]] char** argv)
{
    if (pledge("stdio unix inet cpath rpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Core::EventLoop event_loop;

    if (unveil("/proc/net/", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    auto ifs_result = DHCPv4Client::get_discoverable_interfaces();
    if (ifs_result.is_error()) {
        warnln("Error: {}", ifs_result.error());
        return 1;
    }

    auto ifs = ifs_result.release_value();
    auto client = DHCPv4Client::construct(move(ifs.ready), move(ifs.not_ready));

    if (pledge("stdio inet cpath rpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    return event_loop.exec();
}
