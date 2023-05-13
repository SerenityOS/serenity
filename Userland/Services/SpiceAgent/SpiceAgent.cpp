/*
 * Copyright (c) 2021, Kyle Pereira <kyle@xylepereira.me>
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SpiceAgent.h"
#include <AK/Debug.h>
#include <LibGUI/Clipboard.h>

namespace SpiceAgent {

ErrorOr<NonnullOwnPtr<SpiceAgent>> SpiceAgent::create(StringView device_path)
{
    auto device = TRY(Core::File::open(device_path, Core::File::OpenMode::ReadWrite | Core::File::OpenMode::Nonblocking));
    return try_make<SpiceAgent>(move(device), Vector { Capability::ClipboardByDemand });
}

SpiceAgent::SpiceAgent(NonnullOwnPtr<Core::File> spice_device, Vector<Capability> const& capabilities)
    : m_spice_device(move(spice_device))
    , m_capabilities(capabilities)
{
    m_notifier = Core::Notifier::construct(
        m_spice_device->fd(),
        Core::Notifier::Type::Read);

    m_notifier->on_activation = [this] {
        auto result = on_message_received();
        if (result.is_error()) {
            dbgln("Failed to handle message: {}", result.release_error());
        }
    };
}

ErrorOr<void> SpiceAgent::start()
{
    // The server usually requests this from us anyways, but there's no harm in sending it.
    auto capabilities_message = AnnounceCapabilitiesMessage(false, m_capabilities);
    TRY(this->send_message(capabilities_message));

    return {};
}

ErrorOr<void> SpiceAgent::on_message_received()
{
    auto buffer = TRY(this->read_message_buffer());
    auto stream = FixedMemoryStream(buffer.bytes());

    auto header = TRY(stream.read_value<MessageHeader>());
    switch (header.type()) {
    case Message::Type::AnnounceCapabilities: {
        auto message = TRY(AnnounceCapabilitiesMessage::read_from_stream(stream));
        if (!message.is_request())
            return {};

        dbgln("The spice server has requested our capabilities");

        auto capabilities_message = AnnounceCapabilitiesMessage(false, m_capabilities);
        TRY(this->send_message(capabilities_message));

        break;
    }

    case Message::Type::ClipboardGrab: {
        auto message = TRY(ClipboardGrabMessage::read_from_stream(stream));
        if (message.types().is_empty())
            break;

        auto data_type = message.types().first();
        if (data_type == ClipboardDataType::None)
            break;

        dbgln_if(SPICE_AGENT_DEBUG, "The spice server has notified us of new clipboard data of type: {}", data_type);
        dbgln_if(SPICE_AGENT_DEBUG, "Sending a request for data of type: {}", data_type);

        auto request_message = ClipboardRequestMessage(data_type);
        TRY(this->send_message(request_message));

        break;
    }

    case Message::Type::Clipboard: {
        auto message = TRY(ClipboardMessage::read_from_stream(stream));
        if (message.data_type() == ClipboardDataType::None)
            break;

        TRY(this->did_receive_clipboard_message(message));

        break;
    }

    // We ignore certain messages to prevent it from clogging up the logs.
    case Message::Type::MonitorsConfig:
        dbgln_if(SPICE_AGENT_DEBUG, "Ignored message: {}", header);
        break;

    default:
        dbgln("Unknown message received: {}", header);
        break;
    }

    return {};
}

ErrorOr<void> SpiceAgent::did_receive_clipboard_message(ClipboardMessage& message)
{
    dbgln_if(SPICE_AGENT_DEBUG, "Attempting to parse clipboard data of type: {}", message.data_type());

    switch (message.data_type()) {
    case ClipboardDataType::Text: {
        // The default mime_type for set_data is `text/plain`.
        GUI::Clipboard::the().set_data(message.contents());
        return {};
    }
    default:
        return Error::from_string_literal("Unsupported clipboard data type!");
    }
}

ErrorOr<ByteBuffer> SpiceAgent::read_message_buffer()
{
    auto header = TRY(m_spice_device->read_value<ChunkHeader>());
    auto buffer = TRY(ByteBuffer::create_uninitialized(header.size()));
    TRY(m_spice_device->read_until_filled(buffer));

    // If the header's size is bigger than or equal to 2048, we may have more data incoming.
    while (header.size() >= message_buffer_threshold) {
        header = TRY(m_spice_device->read_value<ChunkHeader>());

        auto new_buffer = TRY(ByteBuffer::create_uninitialized(header.size()));
        TRY(m_spice_device->read_until_filled(new_buffer));
        TRY(buffer.try_append(new_buffer));
    }

    return buffer;
}
};
