/*
 * Copyright (c) 2021, Maciej Zygmanowski <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <LibCore/IPCSockets.h>
#include <SystemServer/SystemServer.h>
#include <unistd.h>

static OwnPtr<SystemServer> s_the;

void SystemServer::initialize(Mode mode)
{
    VERIFY(!s_the);
    s_the = adopt_own(*new SystemServer(mode));
}

SystemServer& SystemServer::the()
{
    return *s_the;
}

String SystemServer::socket_directory() const
{
    if (m_mode == Mode::User)
        return Core::IPCSockets::user_socket_directory();
    return Core::IPCSockets::system_socket_directory();
}
