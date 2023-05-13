/*
 * Copyright (c) 2021, Kyle Pereira <kyle@xylepereira.me>
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SpiceAgent.h"
#include "ConnectionToClipboardServer.h"

namespace SpiceAgent {

ErrorOr<NonnullOwnPtr<SpiceAgent>> SpiceAgent::create(StringView device_path)
{
    auto device = TRY(Core::File::open(device_path, Core::File::OpenMode::ReadWrite | Core::File::OpenMode::Nonblocking));
    auto clipboard_connection = TRY(ConnectionToClipboardServer::try_create());

    return try_make<SpiceAgent>(move(device), clipboard_connection, Vector { Capability::ClipboardByDemand });
}

SpiceAgent::SpiceAgent(NonnullOwnPtr<Core::File> spice_device, ConnectionToClipboardServer& clipboard_connection, Vector<Capability> const& capabilities)
    : m_spice_device(move(spice_device))
    , m_clipboard_connection(clipboard_connection)
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

ErrorOr<ByteBuffer> SpiceAgent::read_message_buffer()
{
    auto port = TRY(m_spice_device->read_value<Port>());
    if (port != Port::Client) {
        return Error::from_string_literal("Attempted to read message bytes from a port that wasn't meant for the client!");
    }

    auto size = TRY(m_spice_device->read_value<u32>());
    auto buffer = TRY(ByteBuffer::create_uninitialized(size));
    TRY(m_spice_device->read_until_filled(buffer));

    return buffer;
}
};
