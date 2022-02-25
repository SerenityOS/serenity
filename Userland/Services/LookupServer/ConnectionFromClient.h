/*
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibIPC/ConnectionFromClient.h>
#include <LookupServer/LookupClientEndpoint.h>
#include <LookupServer/LookupServerEndpoint.h>

namespace LookupServer {

class ConnectionFromClient final
    : public IPC::ConnectionFromClient<LookupClientEndpoint, LookupServerEndpoint> {
    C_OBJECT(ConnectionFromClient);

public:
    virtual ~ConnectionFromClient() override;

    virtual void die() override;

private:
    explicit ConnectionFromClient(NonnullOwnPtr<Core::Stream::LocalSocket>, int client_id);

    virtual Messages::LookupServer::LookupNameResponse lookup_name(String const&) override;
    virtual Messages::LookupServer::LookupAddressResponse lookup_address(String const&) override;
};

}
