/*
 * Copyright (c) 2021, Kyle Pereira <kyle@xylepereira.me>
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Message.h"
#include "MessageHeader.h"
#include <AK/MemoryStream.h>
#include <AK/Vector.h>
#include <LibCore/File.h>
#include <LibCore/Notifier.h>

namespace SpiceAgent {

class SpiceAgent {
public:
    // Indicates where the message has come from.
    enum class Port : u32 {
        Client = 1,

        // There are currently no messages which are meant for the server, so all messages sent by the agent (us) with this port are discarded.
        Server
    };

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
        auto header_stream = AK::AllocatingMemoryStream();
        auto header = MessageHeader(message.type(), message_stream.used_buffer_size());
        TRY(header_stream.write_value(header));

        // Currently, there are no messages from the agent which are meant for the server.
        // So, all messages sent by the agent with a port of Port::Server get dropped silently.
        TRY(m_spice_device->write_value(Port::Client));

        // The length of the subsequent data.
        auto length = header_stream.used_buffer_size() + message_stream.used_buffer_size();
        TRY(m_spice_device->write_value<u32>(length));

        // The message's header.
        TRY(m_spice_device->write_until_depleted(TRY(header_stream.read_until_eof())));

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
