/*
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MessageHeader.h"
#include <AK/Stream.h>
#include <AK/String.h>

namespace SpiceAgent {

ErrorOr<MessageHeader> MessageHeader::create(Message::Type type, u32 data_size, u32 protocol_version, u64 opaque)
{
    return MessageHeader(protocol_version, type, opaque, data_size);
}

ErrorOr<MessageHeader> MessageHeader::read_from_stream(AK::Stream& stream)
{
    // The protocol version must match our agent's
    auto protocol_version = TRY(stream.read_value<u32>());
    if (protocol_version != AGENT_PROTOCOL) {
        return Error::from_string_literal("Received mismatched protocol version when reading a message's header!");
    }

    // The type indicates how we should parse the message's data
    auto type = TRY(stream.read_value<Message::Type>());

    // A placeholder for messages that only pass one integer as their data
    auto opaque = TRY(stream.read_value<u64>());

    // The size of the message's data, which is after this u32
    auto size = TRY(stream.read_value<u32>());

    return MessageHeader::create(type, size, protocol_version, opaque);
}

ErrorOr<void> MessageHeader::write_to_stream(AK::Stream& stream)
{
    // Write all of our data to the stream
    TRY(stream.write_value(m_protocol_version));
    TRY(stream.write_value(m_type));
    TRY(stream.write_value(m_opaque));
    TRY(stream.write_value(m_data_size));

    return {};
}

}
