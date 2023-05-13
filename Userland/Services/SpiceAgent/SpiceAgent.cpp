/*
 * Copyright (c) 2021, Kyle Pereira <kyle@xylepereira.me>
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SpiceAgent.h"
#include <AK/Debug.h>
#include <LibGUI/Clipboard.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>
#include <LibGfx/ImageFormats/PNGWriter.h>

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

    GUI::Clipboard::the().on_change = [this](auto const& mime_type) {
        auto result = this->on_clipboard_update(String::from_deprecated_string(mime_type).release_value_but_fixme_should_propagate_errors());
        if (result.is_error()) {
            dbgln("Failed to inform the spice server of a clipboard update: {}", result.release_error());
        }
    };

    return {};
}

ErrorOr<void> SpiceAgent::on_clipboard_update(String const& mime_type)
{
    // If we just copied something to the clipboard, we shouldn't do anything here.
    if (m_clipboard_dirty) {
        m_clipboard_dirty = false;
        return {};
    }

    // If the clipboard has just been cleared, we shouldn't send anything.
    if (mime_type.is_empty()) {
        return {};
    }

    // Notify the spice server about new content being available.
    auto clipboard_data_type = TRY(clipboard_data_type_from_mime_type(mime_type));
    auto message = ClipboardGrabMessage({ clipboard_data_type });
    TRY(this->send_message(message));

    return {};
}

ErrorOr<void> SpiceAgent::send_clipboard_contents(ClipboardDataType data_type)
{
    auto data_and_type = GUI::Clipboard::the().fetch_data_and_type();
    auto requested_mime_type = TRY(clipboard_data_type_to_mime_type(data_type));

    // We have an exception for `image/x-serenityos`, where we treat it as a PNG when talking to the spice server.
    auto is_serenity_image = data_and_type.mime_type == "image/x-serenityos" && data_type == ClipboardDataType::PNG;
    if (!is_serenity_image && requested_mime_type.to_deprecated_string() != data_and_type.mime_type) {
        // If the requested mime type doesn't match what's on the clipboard, we won't send anything back.
        return Error::from_string_literal("Requested mime type doesn't match the clipboard's contents!");
    }

    // If the mime type is `image/x-serenityos`, we need to encode the image that's on the clipboard as a PNG.
    auto clipboard_data = data_and_type.data;
    if (is_serenity_image) {
        auto bitmap = data_and_type.as_bitmap();
        clipboard_data = TRY(Gfx::PNGWriter::encode(*bitmap));
    }

    auto message = ClipboardMessage(data_type, clipboard_data);
    TRY(this->send_message(message));

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

    case Message::Type::ClipboardRequest: {
        dbgln("The spice server has requsted our clipboard's contents");

        auto message = TRY(ClipboardRequestMessage::read_from_stream(stream));
        TRY(this->send_clipboard_contents(message.data_type()));

        break;
    }

    case Message::Type::FileTransferStart: {
        auto message = TRY(FileTransferStartMessage::read_from_stream(stream));
        dbgln("File transfer request received: {}", TRY(message.debug_description()));

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
        break;
    }

    // For the image formats, let's try to find a decoder from LibGfx.
    case ClipboardDataType::PNG:
    case ClipboardDataType::BMP:
    case ClipboardDataType::JPG:
    case ClipboardDataType::TIFF: {
        auto mime_type = TRY(clipboard_data_type_to_mime_type(message.data_type()));

        // FIXME: It should be trivial to make `try_create_for_raw_bytes` take a `StringView` instead of a direct `DeprecatedString`.
        auto decoder = Gfx::ImageDecoder::try_create_for_raw_bytes(message.contents(), mime_type.to_deprecated_string());
        if (!decoder || (decoder->frame_count() == 0)) {
            return Error::from_string_literal("Failed to find a suitable decoder for a pasted image!");
        }

        auto frame = TRY(decoder->frame(0));
        GUI::Clipboard::the().set_bitmap(*frame.image);

        break;
    }

    default:
        return Error::from_string_literal("Unsupported clipboard data type!");
    }

    m_clipboard_dirty = true;
    return {};
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
