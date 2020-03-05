/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "RemoteProcess.h"
#include "RemoteObject.h"
#include "RemoteObjectGraphModel.h"
#include "RemoteObjectPropertyModel.h"
#include <stdio.h>
#include <stdlib.h>

RemoteProcess* s_the;

RemoteProcess& RemoteProcess::the()
{
    return *s_the;
}

RemoteProcess::RemoteProcess(pid_t pid)
    : m_pid(pid)
    , m_object_graph_model(RemoteObjectGraphModel::create(*this))
    , m_socket(Core::LocalSocket::construct())
{
    s_the = this;
}

void RemoteProcess::handle_identify_response(const JsonObject& response)
{
    int pid = response.get("pid").to_int();
    ASSERT(pid == m_pid);

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

    m_object_graph_model->update();

    if (on_update)
        on_update();
}

void RemoteProcess::send_request(const JsonObject& request)
{
    auto serialized = request.to_string();
    i32 length = serialized.length();
    m_socket->write((const u8*)&length, sizeof(length));
    m_socket->write(serialized);
}

void RemoteProcess::set_inspected_object(uintptr_t address)
{
    JsonObject request;
    request.set("type", "SetInspectedObject");
    request.set("address", address);
    send_request(request);
}

void RemoteProcess::set_property(uintptr_t object, const StringView& name, const JsonValue& value)
{
    JsonObject request;
    request.set("type", "SetProperty");
    request.set("address", object);
    request.set("name", JsonValue(name));
    request.set("value", value);
    send_request(request);
}

void RemoteProcess::update()
{
    m_socket->on_connected = [this] {
        dbg() << "Connected to PID " << m_pid;

        {
            JsonObject request;
            request.set("type", "Identify");
            send_request(request);
        }

        {
            JsonObject request;
            request.set("type", "GetAllObjects");
            send_request(request);
        }
    };

    m_socket->on_ready_to_read = [this] {
        if (m_socket->eof()) {
            dbg() << "Disconnected from PID " << m_pid;
            m_socket->close();
            return;
        }

        u32 length;
        int nread = m_socket->read((u8*)&length, sizeof(length));
        ASSERT(nread == sizeof(length));

        auto data = m_socket->read(length);
        ASSERT(data.size() == length);

        dbg() << "Got packet size " << length << " and read that many bytes";

        auto json_value = JsonValue::from_string(data);
        ASSERT(json_value.is_object());

        dbg() << "Got JSON response " << json_value.to_string();

        auto& response = json_value.as_object();

        auto response_type = response.get("type").as_string_or({});
        if (response_type.is_null())
            return;

        if (response_type == "GetAllObjects") {
            handle_get_all_objects_response(response);
            return;
        }

        if (response_type == "Identify") {
            handle_identify_response(response);
            return;
        }
    };

    auto success = m_socket->connect(Core::SocketAddress::local(String::format("/tmp/rpc.%d", m_pid)));
    if (!success) {
        fprintf(stderr, "Couldn't connect to PID %d\n", m_pid);
        exit(1);
    }
}
