/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "InspectorServerClient.h"
#include <AK/NonnullOwnPtrVector.h>

namespace Inspector {

class RemoteObjectGraphModel;
class RemoteObject;

class RemoteProcess {
public:
    static RemoteProcess& the();

    explicit RemoteProcess(pid_t);
    void update();

    pid_t pid() const { return m_pid; }
    String const& process_name() const { return m_process_name; }

    RemoteObjectGraphModel& object_graph_model() { return *m_object_graph_model; }
    NonnullOwnPtrVector<RemoteObject> const& roots() const { return m_roots; }

    void set_inspected_object(FlatPtr);

    void set_property(FlatPtr object, StringView name, JsonValue const& value);

    bool is_inspectable();

    Function<void()> on_update;

private:
    void handle_get_all_objects_response(JsonObject const&);
    void handle_identify_response(JsonObject const&);

    void send_request(JsonObject const&);

    pid_t m_pid { -1 };
    String m_process_name;
    NonnullRefPtr<RemoteObjectGraphModel> m_object_graph_model;
    NonnullOwnPtrVector<RemoteObject> m_roots;
    RefPtr<InspectorServerClient> m_client;
};

}
