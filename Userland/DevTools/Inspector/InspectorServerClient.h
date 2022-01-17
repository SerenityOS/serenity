/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <InspectorServer/InspectorClientEndpoint.h>
#include <InspectorServer/InspectorServerEndpoint.h>
#include <LibIPC/ServerConnection.h>

namespace Inspector {

class InspectorServerClient final
    : public IPC::ServerConnection<InspectorClientEndpoint, InspectorServerEndpoint>
    , public InspectorClientEndpoint {
    IPC_CLIENT_CONNECTION(InspectorServerClient, "/tmp/portal/inspector")

public:
    virtual ~InspectorServerClient() override = default;

private:
    InspectorServerClient(NonnullOwnPtr<Core::Stream::LocalSocket> socket)
        : IPC::ServerConnection<InspectorClientEndpoint, InspectorServerEndpoint>(*this, move(socket))
    {
    }
};

}
