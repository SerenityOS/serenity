#include "RemoteObjectGraphModel.h"
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GApplication.h>
#include <stdio.h>

RemoteObjectGraphModel::RemoteObjectGraphModel(pid_t pid)
    : m_pid(pid)
{
    m_object_icon.set_bitmap_for_size(16, load_png("/res/icons/gear16.png"));
}

RemoteObjectGraphModel::~RemoteObjectGraphModel()
{
}

GModelIndex RemoteObjectGraphModel::index(int row, int column, const GModelIndex& parent) const
{
    if (!parent.is_valid()) {
        if (m_remote_roots.is_empty())
            return {};
        return create_index(row, column, &m_remote_roots.at(row));
    }
    auto& remote_parent = *static_cast<RemoteObject*>(parent.internal_data());
    return create_index(row, column, remote_parent.children.at(row));
}

int RemoteObjectGraphModel::row_count(const GModelIndex& index) const
{
    if (!index.is_valid())
        return m_remote_roots.size();
    auto& remote_object = *static_cast<RemoteObject*>(index.internal_data());
    return remote_object.children.size();
}

int RemoteObjectGraphModel::column_count(const GModelIndex&) const
{
    return 1;
}

GVariant RemoteObjectGraphModel::data(const GModelIndex& index, Role role) const
{
    auto* remote_object = static_cast<RemoteObject*>(index.internal_data());
    if (role == Role::Icon) {
        return m_object_icon;
    }
    if (role == Role::Display) {
        return String::format("%s{%s} (%d)", remote_object->class_name.characters(), remote_object->address.characters(), remote_object->children.size());
    }
    return {};
}

void RemoteObjectGraphModel::update()
{
    auto success = m_socket.connect(CSocketAddress::local(String::format("/tmp/rpc.%d", m_pid)));
    if (!success) {
        fprintf(stderr, "Couldn't connect to PID %d\n", m_pid);
        exit(1);
    }

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
        m_json = json_value.as_array();

        Vector<NonnullOwnPtr<RemoteObject>> remote_objects;
        HashMap<String, RemoteObject*> objects_by_address;

        for (auto& value : m_json.values()) {
            ASSERT(value.is_object());
            auto& object = value.as_object();
            auto remote_object = make<RemoteObject>();
            remote_object->address = object.get("address").to_string();
            remote_object->parent_address = object.get("parent").to_string();
            remote_object->name = object.get("name").to_string();
            remote_object->class_name = object.get("class_name").to_string();
            objects_by_address.set(remote_object->address, remote_object);
            remote_objects.append(move(remote_object));
        }

        for (int i = 0; i < remote_objects.size(); ++i) {
            auto& remote_object = remote_objects[i];
            auto* parent = objects_by_address.get(remote_object->parent_address).value_or(nullptr);
            if (!parent) {
                m_remote_roots.append(move(remote_object));
            } else {
                remote_object->parent = parent;
                parent->children.append(move(remote_object));
            }
        }

        //dbg() << m_json.to_string();
        did_update();
    };
}
