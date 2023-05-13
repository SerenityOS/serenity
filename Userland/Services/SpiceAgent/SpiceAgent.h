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
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/File.h>
#include <LibCore/Notifier.h>

namespace SpiceAgent {

class SpiceAgent {
public:
    static ErrorOr<NonnullOwnPtr<SpiceAgent>> create(StringView device_path);
    SpiceAgent(NonnullOwnPtr<Core::File> spice_device, Vector<Capability> const& capabilities);

    ErrorOr<void> start();

    template<typename T>
    ErrorOr<void> send_message(T message)
    {
        // Attempt to write the message's data to a stream
        auto message_stream = AK::AllocatingMemoryStream();
        TRY(message.write_to_stream(message_stream));

        // Create a header to be sent
        auto message_header_stream = AK::AllocatingMemoryStream();
        auto message_header = TRY(MessageHeader::create(message.type(), message_stream.used_buffer_size()));
        TRY(message_header.write_to_stream(message_header_stream));

        // The length given in the chunk header is the length of the message header, and the message combined
        auto length = message_header_stream.used_buffer_size() + message_stream.used_buffer_size();

        // Currently, there are no messages from the agent which are meant for the server.
        // So, all messages sent by the agent with a port of Port::Server get dropped silently.
        auto chunk_header = ChunkHeader::create(ChunkHeader::Port::Client, length);

        TRY(m_spice_device->write_value(chunk_header));

        // The message's header
        TRY(m_spice_device->write_some(TRY(message_header_stream.read_until_eof())));

        // The message content
        TRY(m_spice_device->write_some(TRY(message_stream.read_until_eof())));

        return {};
    }

private:
    NonnullOwnPtr<Core::File> m_spice_device;
    Vector<Capability> m_capabilities;

    RefPtr<Core::Notifier> m_notifier;

    bool m_just_updated_clipboard { false };

    // Fired when we receive clipboard data from the spice server
    ErrorOr<void> on_clipboard_message(ClipboardMessage& message);

    // Fired when the user's clipboard changes
    ErrorOr<void> on_clipboard_update(String const& mime_type);

    // Sends the GUI::Clipboard's current contents to the spice server
    ErrorOr<void> send_clipboard_contents(ClipboardDataType data_type);

    ErrorOr<void> on_message_received();
    ErrorOr<ByteBuffer> read_message_buffer();
};
}
