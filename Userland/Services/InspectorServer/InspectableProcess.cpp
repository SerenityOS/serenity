/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InspectableProcess.h"
#include <AK/JsonObject.h>

namespace InspectorServer {

HashMap<pid_t, NonnullOwnPtr<InspectableProcess>> g_processes;

InspectableProcess* InspectableProcess::from_pid(pid_t pid)
{
    return g_processes.get(pid).value_or(nullptr);
}

InspectableProcess::InspectableProcess(pid_t pid, NonnullRefPtr<Core::LocalSocket> socket)
    : m_pid(pid)
    , m_socket(move(socket))
{
    m_socket->set_blocking(true);
    m_socket->on_ready_to_read = [this] {
        [[maybe_unused]] auto buffer = m_socket->read(1);
        if (m_socket->eof()) {
            g_processes.remove(m_pid);
            return;
        }
    };
}

InspectableProcess::~InspectableProcess()
{
}

String InspectableProcess::wait_for_response()
{
    if (m_socket->eof()) {
        dbgln("InspectableProcess disconnected: PID {}", m_pid);
        m_socket->close();
        return {};
    }

    u32 length {};
    auto nread = m_socket->read((u8*)&length, sizeof(length));
    if (nread != sizeof(length)) {
        dbgln("InspectableProcess got malformed data: PID {}", m_pid);
        m_socket->close();
        return {};
    }

    ByteBuffer data;
    size_t remaining_bytes = length;

    while (remaining_bytes) {
        auto packet = m_socket->read(remaining_bytes);
        if (packet.size() == 0)
            break;
        if (auto result = data.try_append(packet.data(), packet.size()); result.is_error()) {
            dbgln("Failed to append {} bytes to data buffer: {}", packet.size(), result.error());
            break;
        }
        remaining_bytes -= packet.size();
    }

    VERIFY(data.size() == length);
    dbgln("Got data size {} and read that many bytes", length);

    return String::copy(data);
}

void InspectableProcess::send_request(JsonObject const& request)
{
    auto serialized = request.to_string();
    auto length = serialized.length();
    m_socket->write((u8 const*)&length, sizeof(length));
    m_socket->write(serialized);
}

}
