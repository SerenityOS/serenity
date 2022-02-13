/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <LibGUI/ConnectionToWindowMangerServer.h>

namespace GUI {

static Singleton<ConnectionToWindowMangerServer> s_the;
ConnectionToWindowMangerServer& ConnectionToWindowMangerServer::the()
{
    return *s_the;
}

void ConnectionToWindowMangerServer::async_set_event_mask(i32)
{
    // NOP
}

void ConnectionToWindowMangerServer::async_set_manager_window(i32)
{
    // NOP
}

}
