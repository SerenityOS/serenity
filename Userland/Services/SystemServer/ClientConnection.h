/*
 * Copyright (c) 2021, Maciej Zygmanowski <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibIPC/ClientConnection.h>
#include <SystemServer/ServiceManagementClientEndpoint.h>
#include <SystemServer/ServiceManagementServerEndpoint.h>

namespace SystemServer {

class ClientConnection final
    : public IPC::ClientConnection<ServiceManagementClientEndpoint, ServiceManagementServerEndpoint>
    , public ServiceManagementServerEndpoint {
    C_OBJECT(ClientConnection);

public:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id);
    ~ClientConnection() override;

    virtual void die() override;

private:
    virtual OwnPtr<Messages::ServiceManagementServer::GreetResponse> handle(const Messages::ServiceManagementServer::Greet&) override;
    virtual OwnPtr<Messages::ServiceManagementServer::ServiceStatusResponse> handle(const Messages::ServiceManagementServer::ServiceStatus&) override;
    virtual OwnPtr<Messages::ServiceManagementServer::ServiceListResponse> handle(const Messages::ServiceManagementServer::ServiceList&) override;
    virtual OwnPtr<Messages::ServiceManagementServer::ServiceSetEnabledResponse> handle(const Messages::ServiceManagementServer::ServiceSetEnabled&) override;
    virtual OwnPtr<Messages::ServiceManagementServer::ServiceSetRunningResponse> handle(const Messages::ServiceManagementServer::ServiceSetRunning&) override;
};

}
