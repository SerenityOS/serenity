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
    const String& process_name() const { return m_process_name; }

    RemoteObjectGraphModel& object_graph_model() { return *m_object_graph_model; }
    const NonnullOwnPtrVector<RemoteObject>& roots() const { return m_roots; }

    void set_inspected_object(FlatPtr);

    void set_property(FlatPtr object, StringView name, const JsonValue& value);

    bool is_inspectable();

    Function<void()> on_update;

private:
    void handle_get_all_objects_response(const JsonObject&);
    void handle_identify_response(const JsonObject&);

    void send_request(const JsonObject&);

    pid_t m_pid { -1 };
    String m_process_name;
    NonnullRefPtr<RemoteObjectGraphModel> m_object_graph_model;
    NonnullOwnPtrVector<RemoteObject> m_roots;
    RefPtr<InspectorServerClient> m_client;
};

}
