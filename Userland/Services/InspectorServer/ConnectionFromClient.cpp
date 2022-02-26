/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InspectableProcess.h"
#include <AK/JsonObject.h>
#include <InspectorServer/ConnectionFromClient.h>

namespace InspectorServer {

static HashMap<int, RefPtr<ConnectionFromClient>> s_connections;

ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::Stream::LocalSocket> socket, int client_id)
    : IPC::ConnectionFromClient<InspectorClientEndpoint, InspectorServerEndpoint>(*this, move(socket), client_id)
{
    s_connections.set(client_id, *this);
}

ConnectionFromClient::~ConnectionFromClient()
{
}

void ConnectionFromClient::die()
{
    s_connections.remove(client_id());
}

Messages::InspectorServer::GetAllObjectsResponse ConnectionFromClient::get_all_objects(pid_t pid)
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

Messages::InspectorServer::SetInspectedObjectResponse ConnectionFromClient::set_inspected_object(pid_t pid, u64 object_id)
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

Messages::InspectorServer::SetObjectPropertyResponse ConnectionFromClient::set_object_property(pid_t pid, u64 object_id, String const& name, String const& value)
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

Messages::InspectorServer::IdentifyResponse ConnectionFromClient::identify(pid_t pid)
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

Messages::InspectorServer::IsInspectableResponse ConnectionFromClient::is_inspectable(pid_t pid)
{
    auto process = InspectableProcess::from_pid(pid);
    if (!process)
        return false;

    return true;
}

}
