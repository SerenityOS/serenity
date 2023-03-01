/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InspectableProcess.h"
#include <AK/JsonObject.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Socket.h>

namespace InspectorServer {

HashMap<pid_t, NonnullOwnPtr<InspectableProcess>> g_processes;

InspectableProcess* InspectableProcess::from_pid(pid_t pid)
{
    return g_processes.get(pid).value_or(nullptr);
}

InspectableProcess::InspectableProcess(pid_t pid, NonnullOwnPtr<Core::LocalSocket> socket)
    : m_pid(pid)
    , m_socket(move(socket))
{
    // FIXME: Propagate errors
    MUST(m_socket->set_blocking(true));

    m_socket->on_ready_to_read = [this] {
        [[maybe_unused]] auto c = m_socket->read_value<char>().release_value_but_fixme_should_propagate_errors();

        if (m_socket->is_eof()) {
            Core::deferred_invoke([pid = this->m_pid] { g_processes.remove(pid); });
            return;
        }
    };
}

DeprecatedString InspectableProcess::wait_for_response()
{
    if (m_socket->is_eof()) {
        dbgln("InspectableProcess disconnected: PID {}", m_pid);
        m_socket->close();
        return {};
    }

    auto length = m_socket->read_value<u32>().release_value_but_fixme_should_propagate_errors();

    auto data_buffer = ByteBuffer::create_uninitialized(length).release_value_but_fixme_should_propagate_errors();
    auto remaining_data_buffer = data_buffer.bytes();

    while (!remaining_data_buffer.is_empty()) {
        auto maybe_bytes_read = m_socket->read_some(remaining_data_buffer);
        if (maybe_bytes_read.is_error()) {
            dbgln("InspectableProcess::wait_for_response: Failed to read data: {}", maybe_bytes_read.error());
            break;
        }

        auto bytes_read = maybe_bytes_read.release_value();
        if (bytes_read.is_empty())
            break;

        remaining_data_buffer = remaining_data_buffer.slice(bytes_read.size());
    }

    VERIFY(data_buffer.size() == length);
    dbgln("Got data size {} and read that many bytes", length);

    return DeprecatedString::copy(data_buffer);
}

void InspectableProcess::send_request(JsonObject const& request)
{
    auto serialized = request.to_deprecated_string();
    u32 length = serialized.length();

    // FIXME: Propagate errors
    MUST(m_socket->write_value(length));
    MUST(m_socket->write_until_depleted(serialized.bytes()));
}

}
