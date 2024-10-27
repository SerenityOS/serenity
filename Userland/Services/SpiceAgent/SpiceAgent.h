/*
 * Copyright (c) 2021, Kyle Pereira <kyle@xylepereira.me>
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ChunkHeader.h"
#include "FileTransferOperation.h"
#include "Message.h"
#include "MessageHeader.h"
#include <AK/MemoryStream.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/File.h>
#include <LibCore/Notifier.h>

namespace SpiceAgent {

// The maximum amount of data that can be contained within a message's buffer.
// If the buffer's length is equal to this, then the next data recieved will be more data from the same buffer.
constexpr u32 message_buffer_threshold = 2048;

// The maximum amount of data that can be received in one file transfer message
constexpr u32 file_transfer_buffer_threshold = 65536;

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

    Function<void()> on_disconnected_from_spice_server;

private:
    NonnullOwnPtr<Core::File> m_spice_device;
    Vector<Capability> m_capabilities;
    HashMap<u32, NonnullRefPtr<FileTransferOperation>> m_file_transfer_operations;

    RefPtr<Core::Notifier> m_notifier;

    bool m_clipboard_dirty { false };

    struct {
        Optional<MessageHeader> header;
        size_t recv_offset { 0 };
        ByteBuffer buffer;
    } m_message;

    struct {
        u8 header[sizeof(ChunkHeader)] = {};
        size_t recv_offset { 0 };
        ByteBuffer buffer;
    } m_chunk;

    // Fired when we receive clipboard data from the spice server.
    ErrorOr<void> did_receive_clipboard_message(ClipboardMessage& message);

    // Fired when the user's clipboard changes
    ErrorOr<void> on_clipboard_update(String const& mime_type);

    // Sends the GUI::Clipboard's current contents to the spice server
    ErrorOr<void> send_clipboard_contents(ClipboardDataType data_type);

    ErrorOr<void> on_message_received(MessageHeader const& header, Bytes data_buffer);

    ErrorOr<void> on_chunk_received(Bytes chunk_buffer);

    ErrorOr<void> read_chunks();
};
}
