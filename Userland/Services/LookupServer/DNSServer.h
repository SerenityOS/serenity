/*
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/UDPServer.h>

namespace LookupServer {

class DNSServer : public Core::UDPServer {
    C_OBJECT(DNSServer)

private:
    explicit DNSServer(Core::EventReceiver* parent = nullptr);

    ErrorOr<void> handle_client();
};

}
