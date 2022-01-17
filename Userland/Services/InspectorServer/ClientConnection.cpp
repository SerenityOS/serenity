/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InspectableProcess.h"
#include <AK/JsonObject.h>
#include <InspectorServer/ClientConnection.h>

namespace InspectorServer {

static HashMap<int, RefPtr<ClientConnection>> s_connections;

ClientConnection::ClientConnection(NonnullOwnPtr<Core::Stream::LocalSocket> socket, int client_id)
    : IPC::ClientConnection<InspectorClientEndpoint, InspectorServerEndpoint>(*this, move(socket), client_id)
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

Messages::InspectorServer::GetAllObjectsResponse ClientConnection::get_all_objects(pid_t pid)
{
    auto process = InspectableProcess::from_pid(pid);
    if (!process)
        return String {};

    JsonObject request;
    request.set("type", "GetAllObjects");
    process->send_request(request);
    auto response = process->wait_for_response();
    return response;
}

Messages::InspectorServer::SetInspectedObjectResponse ClientConnection::set_inspected_object(pid_t pid, u64 object_id)
{
    auto process = InspectableProcess::from_pid(pid);
    if (!process)
        return false;

    JsonObject request;
    request.set("type", "SetInspectedObject");
    request.set("address", object_id);
    process->send_request(request);
    return true;
}

Messages::InspectorServer::SetObjectPropertyResponse ClientConnection::set_object_property(pid_t pid, u64 object_id, String const& name, String const& value)
{
    auto process = InspectableProcess::from_pid(pid);
    if (!process)
        return false;

    JsonObject request;
    request.set("type", "SetProperty");
    request.set("address", object_id);
    request.set("name", name);
    request.set("value", value);
    process->send_request(request);
    return true;
}

Messages::InspectorServer::IdentifyResponse ClientConnection::identify(pid_t pid)
{
    auto process = InspectableProcess::from_pid(pid);
    if (!process)
        return String {};

    JsonObject request;
    request.set("type", "Identify");
    process->send_request(request);
    auto response = process->wait_for_response();
    return response;
}

Messages::InspectorServer::IsInspectableResponse ClientConnection::is_inspectable(pid_t pid)
{
    auto process = InspectableProcess::from_pid(pid);
    if (!process)
        return false;

    return true;
}

}
