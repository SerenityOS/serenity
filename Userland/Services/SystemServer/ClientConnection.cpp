/*
 * Copyright (c) 2021, Maciej Zygmanowski <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <AK/MappedFile.h>
#include <LibDebug/DebugInfo.h>
#include <LibELF/Image.h>
#include <SystemServer/ClientConnection.h>
#include <SystemServer/ServiceManagement.h>
#include <SystemServer/ServiceManagementClientEndpoint.h>

namespace SystemServer {

static HashMap<int, RefPtr<ClientConnection>> s_connections;

ClientConnection::ClientConnection(NonnullRefPtr<Core::LocalSocket> socket, int client_id)
    : IPC::ClientConnection<ServiceManagementClientEndpoint, ServiceManagementServerEndpoint>(*this, move(socket), client_id)
{
    s_connections.set(client_id, *this);
}

ClientConnection::~ClientConnection()
{
}

void ClientConnection::die()
{
    s_connections.remove(client_id());
}

OwnPtr<Messages::ServiceManagementServer::GreetResponse> ClientConnection::handle(const Messages::ServiceManagementServer::Greet&)
{
    return make<Messages::ServiceManagementServer::GreetResponse>();
}

OwnPtr<Messages::ServiceManagementServer::ServiceListResponse> ClientConnection::handle(const Messages::ServiceManagementServer::ServiceList&)
{
    // FIXME: Provide information about status
    auto& services = ServiceManagement::the().services();
    JsonArray json;

    for (auto& service : services) {
        JsonObject service_json;
        service_json.set("running", service.is_running());
        service_json.set("name", service.name());
        json.append(service_json);
    }

    return make<Messages::ServiceManagementServer::ServiceListResponse>(json.to_string());
}

OwnPtr<Messages::ServiceManagementServer::ServiceStatusResponse> ClientConnection::handle(const Messages::ServiceManagementServer::ServiceStatus& message)
{
    auto service = ServiceManagement::the().find_service_by_name(message.service_name());
    dbgln("{}", service);
    if (service) {
        // FIXME: Handle multi-instance processes
        JsonObject json;
        service->save_to(json);
        return make<Messages::ServiceManagementServer::ServiceStatusResponse>(json.to_string());
    }
    return make<Messages::ServiceManagementServer::ServiceStatusResponse>("{}");
}

OwnPtr<Messages::ServiceManagementServer::ServiceSetEnabledResponse> ClientConnection::handle(const Messages::ServiceManagementServer::ServiceSetEnabled& message)
{
    auto service = ServiceManagement::the().find_service_by_name(message.service_name());
    bool status = message.enabled();
    dbgln("Setting enabled status of {} to {}", service, status);
    service->set_enabled_for_current_boot_mode(status);
    return make<Messages::ServiceManagementServer::ServiceSetEnabledResponse>(service->boot_modes());
}

OwnPtr<Messages::ServiceManagementServer::ServiceSetRunningResponse> ClientConnection::handle(const Messages::ServiceManagementServer::ServiceSetRunning& message)
{
    auto service = ServiceManagement::the().find_service_by_name(message.service_name());
    if (message.running())
        service->activate();
    else
        service->deactivate();
    return make<Messages::ServiceManagementServer::ServiceSetRunningResponse>();
}

}
