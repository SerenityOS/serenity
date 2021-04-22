/*
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibIPC/ClientConnection.h>
#include <LookupServer/LookupClientEndpoint.h>
#include <LookupServer/LookupServerEndpoint.h>

namespace LookupServer {

class ClientConnection final
    : public IPC::ClientConnection<LookupClientEndpoint, LookupServerEndpoint>
    , public LookupServerEndpoint {

    C_OBJECT(ClientConnection);

public:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id);
    virtual ~ClientConnection() override;

    virtual void die() override;

private:
    virtual OwnPtr<Messages::LookupServer::LookupNameResponse> handle(const Messages::LookupServer::LookupName&) override;
    virtual OwnPtr<Messages::LookupServer::LookupAddressResponse> handle(const Messages::LookupServer::LookupAddress&) override;
};

}
