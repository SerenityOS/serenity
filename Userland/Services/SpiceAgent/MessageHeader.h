/*
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Message.h"
#include <AK/ByteBuffer.h>
#include <AK/Format.h>
#include <AK/Forward.h>

namespace SpiceAgent {

// An incoming or outgoing message header.
// This contains information about the message, like how long it is, the type, etc.
class [[gnu::packed]] MessageHeader {
public:
    MessageHeader(Message::Type type, u32 data_size, u32 protocol_version = AGENT_PROTOCOL, u64 opaque = 0)
        : m_protocol_version(protocol_version)
        , m_type(type)
        , m_opaque(opaque)
        , m_data_size(data_size)
    {
    }

    Message::Type type() const { return m_type; }
    u32 data_size() const { return m_data_size; }
    u32 protocol_version() const { return m_protocol_version; }
    u64 opaque() const { return m_opaque; }

private:
    // The protocol version being used.
    u32 m_protocol_version { AGENT_PROTOCOL };

    // The message type present in `data`.
    Message::Type m_type { Message::Type::MouseState };

    // A placeholder for message types which only need to pass a single integer as message data,
    // for message types which have more data it is always set to 0.
    u64 m_opaque { 0 };

    // The size of the data in the message following this header.
    u32 m_data_size { 0 };
};

}

template<>
struct AK::Traits<SpiceAgent::MessageHeader> : public AK::DefaultTraits<SpiceAgent::MessageHeader> {
    static constexpr bool is_trivially_serializable() { return true; }
};

namespace AK {
template<>
struct Formatter<SpiceAgent::MessageHeader> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, SpiceAgent::MessageHeader const& header)
    {
        return Formatter<FormatString>::format(builder,
            "MessageHeader {{ protocol_version = {}, type = {}, opaque = {}, data_size = {} }}"sv,
            header.protocol_version(), to_underlying(header.type()), header.opaque(), header.data_size());
    }
};
}
