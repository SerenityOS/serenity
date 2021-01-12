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

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <LibCore/LocalSocket.h>

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

    void set_property(FlatPtr object, const StringView& name, const JsonValue& value);

    Function<void()> on_update;

private:
    void handle_get_all_objects_response(const AK::JsonObject&);
    void handle_identify_response(const AK::JsonObject&);

    void send_request(const AK::JsonObject&);

    pid_t m_pid { -1 };
    String m_process_name;
    NonnullRefPtr<RemoteObjectGraphModel> m_object_graph_model;
    RefPtr<Core::LocalSocket> m_socket;
    NonnullOwnPtrVector<RemoteObject> m_roots;
};

}
