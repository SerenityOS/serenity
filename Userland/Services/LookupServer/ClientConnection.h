/*
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibIPC/ClientConnection.h>
#include <LookupServer/LookupClientEndpoint.h>
#include <LookupServer/LookupServerEndpoint.h>

namespace LookupServer {

class ClientConnection final
    : public IPC::ClientConnection<LookupClientEndpoint, LookupServerEndpoint> {
    C_OBJECT(ClientConnection);

public:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id);
    virtual ~ClientConnection() override;

    virtual void die() override;

private:
    virtual Messages::LookupServer::LookupNameResponse lookup_name(String const&) override;
    virtual Messages::LookupServer::LookupAddressResponse lookup_address(String const&) override;
};

}
