/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/System.h>
#include <LibCore/SystemServerTakeover.h>
#include <LibIPC/ConnectionFromClient.h>

namespace IPC {

template<typename ConnectionFromClientType>
ErrorOr<NonnullRefPtr<ConnectionFromClientType>> take_over_accepted_client_from_system_server()
{
    auto socket = TRY(Core::take_over_socket_from_system_server());
    return IPC::new_client_connection<ConnectionFromClientType>(move(socket));
}

}
