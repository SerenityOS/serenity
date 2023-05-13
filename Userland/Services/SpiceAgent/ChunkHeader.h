/*
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Types.h>

namespace SpiceAgent {

class ChunkHeader {
public:
    // Indicates where the message has come from
    enum class Port : u32 {
        Client = 1,

        // There are currently no messages which are meant for the server, so all messages sent by the agent (us) with this port are discarded.
        Server
    };

    static ChunkHeader create(Port port, u32 size);
    static ErrorOr<ChunkHeader> read_from_stream(AK::Stream& stream);

    ErrorOr<void> write_to_stream(AK::Stream& stream) const;

    u32 size() const { return m_size; };
    Port port() const { return m_port; };

private:
    ChunkHeader(Port port, u32 size)
        : m_port(port)
        , m_size(size)
    {
    }

    Port m_port;
    u32 m_size;
};

}
