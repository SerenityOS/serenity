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
    auto device = TRY(Core::File::open(device_path, Core::File::OpenMode::ReadWrite | Core::File::OpenMode::DontCreate | Core::File::OpenMode::Nonblocking));
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
        auto result = read_chunks();
        if (result.is_error() && result.error().code() != EAGAIN) {
            dbgln("Failed to read chunk(s): {}", result.release_error());
        }
    };
}

ErrorOr<void> SpiceAgent::start()
{
    // The server usually requests this from us anyways, but there's no harm in sending it.
    auto capabilities_message = AnnounceCapabilitiesMessage(false, m_capabilities);
    TRY(this->send_message(capabilities_message));

    GUI::Clipboard::the().on_change = [this](auto const& mime_type) {
        auto result = this->on_clipboard_update(String::from_byte_string(mime_type).release_value_but_fixme_should_propagate_errors());
        if (result.is_error()) {
            dbgln("Failed to inform the spice server of a clipboard update: {}", result.release_error());
        }
    };

    return {};
}

ErrorOr<void> SpiceAgent::on_clipboard_update(String const& mime_type)
{
    // NOTE: If we indicate that we don't support clipboard by demand, the spice server will ignore our messages,
    //       but it will do some ugly debug logging.. so let's just not send anything instead.
    if (!m_capabilities.contains_slow(Capability::ClipboardByDemand)) {
        return {};
    }

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
    if (!is_serenity_image && requested_mime_type.to_byte_string() != data_and_type.mime_type) {
        // If the requested mime type doesn't match what's on the clipboard, we won't send anything back.
        return Error::from_string_literal("Requested mime type doesn't match the clipboard's contents!");
    }

    // If the mime type is `image/x-serenityos`, we need to encode the image that's on the clipboard as a PNG.
    auto clipboard_data = data_and_type.data;
    if (is_serenity_image) {
        auto bitmap = data_and_type.as_bitmap();
        clipboard_data = TRY(Gfx::PNGWriter::encode(*bitmap));
    }

    auto message = ClipboardMessage(data_type, move(clipboard_data));
    TRY(this->send_message(move(message)));

    return {};
}

ErrorOr<void> SpiceAgent::on_chunk_received(Bytes chunk_buffer)
{
    auto stream = FixedMemoryStream(chunk_buffer);
    if (!m_message.header.has_value()) {
        // Read the header (the chunk must at least contain the header).
        m_message.header = MUST(stream.read_value<MessageHeader>());
        m_message.buffer.resize(m_message.header->data_size());
        m_message.recv_offset = 0;
    }

    // Read message data. Most messages are one chunk, but some, such as file transfers, can be
    // split over multiple chunks. In that case, we wait until we've received all the chunks.
    Bytes result = TRY(stream.read_some(Bytes(m_message.buffer).slice(m_message.recv_offset)));
    m_message.recv_offset += result.size();
    if (m_message.recv_offset < m_message.header->data_size())
        return {};

    ScopeGuard cleanup_message = [&] {
        m_message.buffer.trim(0, true);
        m_message.recv_offset = 0;
        m_message.header = {};
    };
    TRY(on_message_received(*m_message.header, m_message.buffer));
    return {};
}

ErrorOr<void> SpiceAgent::on_message_received(MessageHeader const& header, Bytes data_buffer)
{
    auto stream = FixedMemoryStream(data_buffer);
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

    case Message::Type::FileTransferStatus: {
        auto message = TRY(FileTransferStatusMessage::read_from_stream(stream));
        dbgln("File transfer {} has been cancelled: {}", message.id(), message.status());

        m_file_transfer_operations.remove(message.id());

        break;
    }

    // Received when the user drags a file onto the virtual machine.
    case Message::Type::FileTransferStart: {
        auto message = TRY(FileTransferStartMessage::read_from_stream(stream));
        auto operation = TRY(FileTransferOperation::create(message));

        // Tell the operation to start the file transfer.
        TRY(operation->begin_transfer(*this));
        m_file_transfer_operations.set(message.id(), operation);

        break;
    }

    // Received when the server has data related to a file transfer for us.
    case Message::Type::FileTransferData: {
        auto message = TRY(FileTransferDataMessage::read_from_stream(stream));
        auto optional_operation = m_file_transfer_operations.get(message.id());
        if (!optional_operation.has_value()) {
            return Error::from_string_literal("Attempt to supply data to a file transfer operation which doesn't exist!");
        }

        // Inform the operation that we have received new data.
        auto* operation = optional_operation.release_value();
        auto result = operation->on_data_received(message);
        if (result.is_error()) {
            // We can also discard of this transfer operation, since it will be cancelled by the server after our status message.
            m_file_transfer_operations.remove(message.id());

            // Inform the server that the operation has failed
            auto status_message = FileTransferStatusMessage(message.id(), FileTransferStatus::Error);
            TRY(this->send_message(status_message));

            return result.release_error();
        }

        // The maximum amount of data that a FileTransferData message can hold is 65536.
        // If it's less than 65536, this is the only (or last) message in relation to this transfer.
        // Otherwise, we must wait for more data to be received.
        auto transfer_is_complete = message.contents().size() < file_transfer_buffer_threshold;
        if (!transfer_is_complete) {
            return {};
        }

        // The transfer is now complete, let's write the data to the file!
        TRY(operation->complete_transfer(*this));
        m_file_transfer_operations.remove(message.id());

        break;
    }

    // We ignore certain messages to prevent it from clogging up the logs.
    case Message::Type::MonitorsConfig:
        dbgln_if(SPICE_AGENT_DEBUG, "Ignored message: {}", header);
        break;

    case Message::Type::Disconnected:
        dbgln_if(SPICE_AGENT_DEBUG, "Spice server disconnected");
        if (on_disconnected_from_spice_server)
            on_disconnected_from_spice_server();
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

        // FIXME: It should be trivial to make `try_create_for_raw_bytes` take a `StringView` instead of a direct `ByteString`.
        auto decoder = TRY(Gfx::ImageDecoder::try_create_for_raw_bytes(message.contents(), mime_type.to_byte_string()));
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

ErrorOr<void> SpiceAgent::read_chunks()
{
    while (!m_spice_device->is_eof()) {
        if (m_chunk.buffer.is_empty()) {
            // Cautiously, try to read the chunk header. If it's (somehow) incomplete, wait.
            Bytes result = TRY(m_spice_device->read_some(Bytes(m_chunk.header).slice(m_chunk.recv_offset)));
            m_chunk.recv_offset += result.size();
            if (m_chunk.recv_offset < sizeof(ChunkHeader))
                return {};
            auto header = bit_cast<ChunkHeader>(m_chunk.header);
            m_chunk.buffer.resize(header.size());
            m_chunk.recv_offset = 0;
        }

        // Read chunk data, notify chunk receipt once the buffer is full.
        Bytes result = TRY(m_spice_device->read_some(Bytes(m_chunk.buffer).slice(m_chunk.recv_offset)));
        m_chunk.recv_offset += result.size();
        if (m_chunk.recv_offset < m_chunk.buffer.size())
            return {};

        ScopeGuard cleanup_chunk = [&] {
            m_chunk.buffer.trim(0, true);
            m_chunk.recv_offset = 0;
        };
        TRY(on_chunk_received(m_chunk.buffer));
    }
    return {};
}
};
