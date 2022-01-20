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

InspectableProcess::InspectableProcess(pid_t pid, NonnullOwnPtr<Core::Stream::LocalSocket> socket)
    : m_pid(pid)
    , m_socket(move(socket))
{
    // FIXME: Propagate errors
    MUST(m_socket->set_blocking(true));

    m_socket->on_ready_to_read = [this] {
        char c;
        [[maybe_unused]] auto buffer = m_socket->read({ &c, 1 });
        if (m_socket->is_eof()) {
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
    if (m_socket->is_eof()) {
        dbgln("InspectableProcess disconnected: PID {}", m_pid);
        m_socket->close();
        return {};
    }

    u32 length {};
    auto nread = m_socket->read({ (u8*)&length, sizeof(length) }).release_value_but_fixme_should_propagate_errors();
    if (nread != sizeof(length)) {
        dbgln("InspectableProcess got malformed data: PID {}", m_pid);
        m_socket->close();
        return {};
    }

    auto data_buffer = ByteBuffer::create_uninitialized(length).release_value_but_fixme_should_propagate_errors();
    auto remaining_data_buffer = data_buffer.bytes();

    while (!remaining_data_buffer.is_empty()) {
        auto maybe_nread = m_socket->read(remaining_data_buffer);
        if (maybe_nread.is_error()) {
            dbgln("InspectableProcess::wait_for_response: Failed to read data: {}", maybe_nread.error());
            break;
        }

        auto nread = maybe_nread.release_value();
        if (nread == 0)
            break;

        remaining_data_buffer = remaining_data_buffer.slice(nread);
    }

    VERIFY(data_buffer.size() == length);
    dbgln("Got data size {} and read that many bytes", length);

    return String::copy(data_buffer);
}

void InspectableProcess::send_request(JsonObject const& request)
{
    auto serialized = request.to_string();
    u32 length = serialized.length();

    // FIXME: Propagate errors
    MUST(m_socket->write({ (u8 const*)&length, sizeof(length) }));
    MUST(m_socket->write(serialized.bytes()));
}

}
