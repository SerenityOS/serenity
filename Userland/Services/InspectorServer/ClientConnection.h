/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <InspectorServer/InspectorClientEndpoint.h>
#include <InspectorServer/InspectorServerEndpoint.h>
#include <LibIPC/ClientConnection.h>

namespace InspectorServer {

class ClientConnection final
    : public IPC::ClientConnection<InspectorClientEndpoint, InspectorServerEndpoint> {
    C_OBJECT(ClientConnection);

public:
    ~ClientConnection() override;

    virtual void die() override;

private:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id);

    virtual Messages::InspectorServer::GetAllObjectsResponse get_all_objects(pid_t) override;
    virtual Messages::InspectorServer::SetInspectedObjectResponse set_inspected_object(pid_t, u64 object_id) override;
    virtual Messages::InspectorServer::SetObjectPropertyResponse set_object_property(pid_t, u64 object_id, String const& name, String const& value) override;
    virtual Messages::InspectorServer::IdentifyResponse identify(pid_t) override;
    virtual Messages::InspectorServer::IsInspectableResponse is_inspectable(pid_t) override;
};

}
