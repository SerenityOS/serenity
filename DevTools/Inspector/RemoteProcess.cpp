#include "RemoteProcess.h"
#include "RemoteObject.h"
#include "RemoteObjectGraphModel.h"
#include "RemoteObjectPropertyModel.h"
#include <stdio.h>
#include <stdlib.h>

RemoteProcess::RemoteProcess(pid_t pid)
    : m_pid(pid)
    , m_object_graph_model(RemoteObjectGraphModel::create(*this))
{
}

void RemoteProcess::update()
{
    m_socket.on_connected = [this] {
        dbg() << "Connected to PID " << m_pid;
    };

    m_socket.on_ready_to_read = [this] {
        if (m_socket.eof()) {
            dbg() << "Disconnected from PID " << m_pid;
            m_socket.close();
            return;
        }

        auto data = m_socket.read_all();
        auto json_value = JsonValue::from_string(data);
        ASSERT(json_value.is_array());
        auto& object_array = json_value.as_array();

        Vector<NonnullOwnPtr<RemoteObject>> remote_objects;
        HashMap<String, RemoteObject*> objects_by_address;

        for (auto& value : object_array.values()) {
            ASSERT(value.is_object());
            auto& object = value.as_object();
            auto remote_object = make<RemoteObject>();
            remote_object->address = object.get("address").to_string();
            remote_object->parent_address = object.get("parent").to_string();
            remote_object->name = object.get("name").to_string();
            remote_object->class_name = object.get("class_name").to_string();
            remote_object->json = object;
            objects_by_address.set(remote_object->address, remote_object);
            remote_objects.append(move(remote_object));
        }

        for (int i = 0; i < remote_objects.size(); ++i) {
            auto& remote_object = remote_objects[i];
            auto* parent = objects_by_address.get(remote_object->parent_address).value_or(nullptr);
            if (!parent) {
                m_roots.append(move(remote_object));
            } else {
                remote_object->parent = parent;
                parent->children.append(move(remote_object));
            }
        }

        m_object_graph_model->update();
    };

    auto success = m_socket.connect(CSocketAddress::local(String::format("/tmp/rpc.%d", m_pid)));
    if (!success) {
        fprintf(stderr, "Couldn't connect to PID %d\n", m_pid);
        exit(1);
    }
}
