/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/LocalSocket.h>

namespace InspectorServer {

class InspectableProcess {
public:
    InspectableProcess(pid_t, NonnullRefPtr<Core::LocalSocket>);
    ~InspectableProcess();

    void send_request(JsonObject const& request);
    String wait_for_response();

    static InspectableProcess* from_pid(pid_t);

private:
    pid_t m_pid { 0 };
    NonnullRefPtr<Core::LocalSocket> m_socket;
};

extern HashMap<pid_t, NonnullOwnPtr<InspectorServer::InspectableProcess>> g_processes;

}
