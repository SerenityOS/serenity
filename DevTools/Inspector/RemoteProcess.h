#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <LibCore/CLocalSocket.h>

namespace AK {
class JsonObject;
}

class RemoteObjectGraphModel;
class RemoteObject;

class RemoteProcess {
public:
    explicit RemoteProcess(pid_t);
    void update();

    pid_t pid() const { return m_pid; }
    const String& process_name() const { return m_process_name; }

    RemoteObjectGraphModel& object_graph_model() { return *m_object_graph_model; }
    const NonnullOwnPtrVector<RemoteObject>& roots() const { return m_roots; }

    Function<void()> on_update;

private:
    void handle_get_all_objects_response(const AK::JsonObject&);
    void handle_identify_response(const AK::JsonObject&);

    void send_request(const AK::JsonObject&);

    pid_t m_pid { -1 };
    String m_process_name;
    NonnullRefPtr<RemoteObjectGraphModel> m_object_graph_model;
    RefPtr<CLocalSocket> m_socket;
    NonnullOwnPtrVector<RemoteObject> m_roots;
};
