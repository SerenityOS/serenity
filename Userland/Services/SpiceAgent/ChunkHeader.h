/*
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Traits.h>
#include <AK/Types.h>

namespace SpiceAgent {

class [[gnu::packed]] ChunkHeader {
public:
    // Indicates where the message has come from
    enum class Port : u32 {
        Client = 1,

        // There are currently no messages which are meant for the server, so all messages sent by the agent (us) with this port are discarded.
        Server
    };

    ChunkHeader(Port port, u32 size)
        : m_port(port)
        , m_size(size)
    {
    }

    Port port() const { return m_port; }
    u32 size() const { return m_size; }

private:
    Port m_port { Port::Client };
    u32 m_size { 0 };
};

}

template<>
struct AK::Traits<SpiceAgent::ChunkHeader> : public AK::DefaultTraits<SpiceAgent::ChunkHeader> {
    static constexpr bool is_trivially_serializable() { return true; }
};
