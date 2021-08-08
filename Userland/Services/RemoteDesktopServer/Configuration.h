/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <LibHTTP/HttpRequest.h>

namespace RemoteDesktopServer {

class Configuration {
public:
    Configuration();

    static Configuration const& the();
};

}
