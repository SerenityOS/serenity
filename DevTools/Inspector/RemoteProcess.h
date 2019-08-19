#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <LibCore/CLocalSocket.h>

class RemoteObjectGraphModel;
class RemoteObject;

class RemoteProcess {
public:
    explicit RemoteProcess(pid_t);
    void update();

    RemoteObjectGraphModel& object_graph_model() { return *m_object_graph_model; }
    const NonnullOwnPtrVector<RemoteObject>& roots() const { return m_roots; }

private:
    pid_t m_pid { -1 };
    NonnullRefPtr<RemoteObjectGraphModel> m_object_graph_model;
    CLocalSocket m_socket;
    NonnullOwnPtrVector<RemoteObject> m_roots;
};
