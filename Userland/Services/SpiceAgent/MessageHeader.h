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

// An incoming or outgoing message header
// This contains information about the message, like how long it is, the type, etc.
class MessageHeader {
public:
    // Used for sending a message to the server
    static ErrorOr<MessageHeader> create(Message::Type type, u32 data_size, u32 protocol_version = AGENT_PROTOCOL, u64 opaque = 0);

    // Used when receiveing a message from the server
    static ErrorOr<MessageHeader> read_from_stream(AK::Stream& stream);

    // Writes the message header information to a stream
    ErrorOr<void> write_to_stream(AK::Stream& stream);

    u32 protocol_version() const { return m_protocol_version; };
    u32 data_size() const { return m_data_size; };
    u64 opaque() const { return m_opaque; };

    Message::Type type() const { return m_type; };

private:
    MessageHeader(u32 protocol_version, Message::Type type, u64 opaque, u32 data_size)
        : m_protocol_version(protocol_version)
        , m_type(type)
        , m_opaque(opaque)
        , m_data_size(data_size)
    {
    }

    // The protocol version being used
    u32 m_protocol_version;

    // The message type present in `data`
    Message::Type m_type;

    // A placeholder for message types which only need to pass a single integer as message data,
    // for message types which have more data it is always set to 0.
    u64 m_opaque;

    // The size of the data in the message following this header
    u32 m_data_size;
};

}

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
