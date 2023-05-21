/*
 * Copyright (c) 2021, Kyle Pereira <kyle@xylepereira.me>
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ChunkHeader.h"
#include "Message.h"
#include "MessageHeader.h"
#include <AK/MemoryStream.h>
#include <AK/Vector.h>
#include <LibCore/File.h>
#include <LibCore/Notifier.h>

namespace SpiceAgent {

// The maximum amount of data that can be contained within a message's buffer.
// If the buffer's length is equal to this, then the next data recieved will be more data from the same buffer.
constexpr u32 message_buffer_threshold = 2048;

class SpiceAgent {
public:
    static ErrorOr<NonnullOwnPtr<SpiceAgent>> create(StringView device_path);
    SpiceAgent(NonnullOwnPtr<Core::File> spice_device, Vector<Capability> const& capabilities);

    ErrorOr<void> start();

    template<typename T>
    ErrorOr<void> send_message(T message)
    {
        // Attempt to write the message's data to a stream.
        auto message_stream = AK::AllocatingMemoryStream();
        TRY(message.write_to_stream(message_stream));

        // Create a header to be sent.
        auto message_header_stream = AK::AllocatingMemoryStream();
        auto message_header = MessageHeader(message.type(), message_stream.used_buffer_size());
        TRY(message_header_stream.write_value(message_header));

        // The length given in the chunk header is the length of the message header, and the message combined
        auto length = message_header_stream.used_buffer_size() + message_stream.used_buffer_size();

        // Currently, there are no messages from the agent which are meant for the server.
        // So, all messages sent by the agent with a port of Port::Server get dropped silently.
        auto chunk_header = ChunkHeader(ChunkHeader::Port::Client, length);
        TRY(m_spice_device->write_value(chunk_header));

        // The message's header.
        TRY(m_spice_device->write_until_depleted(TRY(message_header_stream.read_until_eof())));

        // The message content.
        TRY(m_spice_device->write_until_depleted(TRY(message_stream.read_until_eof())));

        return {};
    }

private:
    NonnullOwnPtr<Core::File> m_spice_device;
    Vector<Capability> m_capabilities;

    RefPtr<Core::Notifier> m_notifier;

    // Fired when we receive clipboard data from the spice server.
    ErrorOr<void> did_receive_clipboard_message(ClipboardMessage& message);

    ErrorOr<void> on_message_received();
    ErrorOr<ByteBuffer> read_message_buffer();
};
}
