/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RemoteProcess.h"
#include "RemoteObject.h"
#include "RemoteObjectGraphModel.h"
#include "RemoteObjectPropertyModel.h"
#include <stdlib.h>

namespace Inspector {

RemoteProcess* s_the;

RemoteProcess& RemoteProcess::the()
{
    return *s_the;
}

RemoteProcess::RemoteProcess(pid_t pid)
    : m_pid(pid)
    , m_object_graph_model(RemoteObjectGraphModel::create(*this))
{
    s_the = this;
    m_client = InspectorServerClient::construct();
}

void RemoteProcess::handle_identify_response(const JsonObject& response)
{
    int pid = response.get("pid").to_int();
    VERIFY(pid == m_pid);

    m_process_name = response.get("process_name").as_string_or({});

    if (on_update)
        on_update();
}

void RemoteProcess::handle_get_all_objects_response(const JsonObject& response)
{
    // FIXME: It would be good if we didn't have to make a local copy of the array value here!
    auto objects = response.get("objects");
    auto& object_array = objects.as_array();

    NonnullOwnPtrVector<RemoteObject> remote_objects;
    HashMap<FlatPtr, RemoteObject*> objects_by_address;

    for (auto& value : object_array.values()) {
        VERIFY(value.is_object());
        auto& object = value.as_object();
        auto remote_object = make<RemoteObject>();
        remote_object->address = object.get("address").to_number<FlatPtr>();
        remote_object->parent_address = object.get("parent").to_number<FlatPtr>();
        remote_object->name = object.get("name").to_string();
        remote_object->class_name = object.get("class_name").to_string();
        remote_object->json = object;
        objects_by_address.set(remote_object->address, remote_object);
        remote_objects.append(move(remote_object));
    }

    for (size_t i = 0; i < remote_objects.size(); ++i) {
        auto& remote_object = remote_objects.ptr_at(i);
        auto* parent = objects_by_address.get(remote_object->parent_address).value_or(nullptr);
        if (!parent) {
            m_roots.append(move(remote_object));
        } else {
            remote_object->parent = parent;
            parent->children.append(move(remote_object));
        }
    }

    m_object_graph_model->invalidate();

    if (on_update)
        on_update();
}

void RemoteProcess::set_inspected_object(FlatPtr address)
{
    m_client->async_set_inspected_object(m_pid, address);
}

void RemoteProcess::set_property(FlatPtr object, StringView name, const JsonValue& value)
{
    m_client->async_set_object_property(m_pid, object, name, value.to_string());
}

bool RemoteProcess::is_inspectable()
{
    return m_client->is_inspectable(m_pid);
}

void RemoteProcess::update()
{
    {
        auto raw_json = m_client->identify(m_pid);
        auto json = JsonValue::from_string(raw_json);
        handle_identify_response(json.value().as_object());
    }

    {
        auto raw_json = m_client->get_all_objects(m_pid);
        auto json = JsonValue::from_string(raw_json);
        handle_get_all_objects_response(json.value().as_object());
    }
}

}
