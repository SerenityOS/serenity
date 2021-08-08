/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <RemoteDesktopServer/Configuration.h>

namespace RemoteDesktopServer {

static Configuration* s_configuration = nullptr;

Configuration::Configuration()
{
    VERIFY(!s_configuration);
    s_configuration = this;
}

Configuration const& Configuration::the()
{
    VERIFY(s_configuration);
    return *s_configuration;
}

}
