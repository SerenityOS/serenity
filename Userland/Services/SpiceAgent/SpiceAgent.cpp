/*
 * Copyright (c) 2021, Kyle Pereira <kyle@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SpiceAgent.h"
#include "ClipboardServerConnection.h"
#include <AK/String.h>
#include <LibC/memory.h>
#include <LibC/unistd.h>

SpiceAgent::SpiceAgent(int fd, ClipboardServerConnection& connection)
    : m_fd(fd)
    , m_clipboard_connection(connection)
{
    m_notifier = Core::Notifier::construct(fd, Core::Notifier::Read);
    m_notifier->on_ready_to_read = [this] {
        on_message_received();
    };
    m_clipboard_connection.on_data_changed = [this] {
        if (m_just_set_clip) {
            m_just_set_clip = false;
            return;
        }
        auto grab_buffer = ClipboardGrab::make_buffer({ ClipboardType::Text });
        send_message(grab_buffer);
    };
    auto buffer = AnnounceCapabilities::make_buffer(true, { Capability::ClipboardByDemand });
    send_message(buffer);
}

void SpiceAgent::on_message_received()
{
    ChunkHeader header {};
    read_n(&header, sizeof(header));
    auto buffer = ByteBuffer::create_uninitialized(header.size);
    read_n(buffer.data(), buffer.size());
    auto* message = reinterpret_cast<Message*>(buffer.data());
    switch (message->type) {
    case (u32)MessageType::AnnounceCapabilities: {
        auto* capabilities_message = reinterpret_cast<AnnounceCapabilities*>(message->data);
        if (capabilities_message->request) {
            auto capabilities_buffer = AnnounceCapabilities::make_buffer(false, { Capability::ClipboardByDemand });
            send_message(capabilities_buffer);
        }
        break;
    }
    case (u32)MessageType::ClipboardRequest: {
        auto clip_data = m_clipboard_connection.get_clipboard_data().data();
        ByteBuffer byte_buffer = ByteBuffer::copy(clip_data.data<void>(), clip_data.size());
        auto clipboard_buffer = Clipboard::make_buffer(ClipboardType::Text, byte_buffer);
        send_message(clipboard_buffer);
        break;
    }
    case (u32)MessageType::ClipboardGrab: {
        auto request_buffer = ClipboardRequest::make_buffer(ClipboardType::Text);
        send_message(request_buffer);
        break;
    }
    case (u32)MessageType::Clipboard: {
        auto* clipboard_message = reinterpret_cast<Clipboard*>(message->data);
        auto data_buffer = ByteBuffer::create_uninitialized(message->size - sizeof(u32));

        const auto total_bytes = message->size - sizeof(Clipboard);
        auto bytes_copied = header.size - sizeof(Message) - sizeof(Clipboard);
        memcpy(data_buffer.data(), clipboard_message->data, bytes_copied);

        while (bytes_copied < total_bytes) {
            ChunkHeader next_header;
            read_n(&next_header, sizeof(ChunkHeader));
            read_n(data_buffer.data() + bytes_copied, next_header.size);
            bytes_copied += next_header.size;
        }

        m_just_set_clip = true;
        auto anon_buffer = Core::AnonymousBuffer::create_with_size(data_buffer.size());
        memcpy(anon_buffer.data<void>(), data_buffer.data(), data_buffer.size());
        m_clipboard_connection.async_set_clipboard_data(anon_buffer, "text/plain", {});
        break;
    }
    default:
        dbgln("Unhandled message type {}", message->type);
    }
}

void SpiceAgent::read_n(void* dest, size_t n)
{
    size_t bytes_read = 0;
    while (bytes_read < n) {
        int nread = read(m_fd, (u8*)dest + bytes_read, n - bytes_read);
        if (nread > 0) {
            bytes_read += nread;
        } else if (errno == EAGAIN) {
            continue;
        } else {
            dbgln("Failed to read: {}", errno);
            return;
        }
    }
}

void SpiceAgent::send_message(ByteBuffer const& buffer)
{
    size_t bytes_written = 0;
    while (bytes_written < buffer.size()) {
        int result = write(m_fd, buffer.data() + bytes_written, buffer.size() - bytes_written);
        if (result < 0) {
            dbgln("Failed to write: {}", errno);
            return;
        }
        bytes_written += result;
    }
}

SpiceAgent::Message* SpiceAgent::initialize_headers(u8* data, size_t additional_data_size, MessageType type)
{
    new (data) ChunkHeader {
        (u32)Port::Client,
        (u32)(sizeof(Message) + additional_data_size)
    };

    auto* message = new (data + sizeof(ChunkHeader)) Message {
        AGENT_PROTOCOL,
        (u32)type,
        0,
        (u32)additional_data_size
    };
    return message;
}

ByteBuffer SpiceAgent::AnnounceCapabilities::make_buffer(bool request, const Vector<Capability>& capabilities)
{
    size_t required_size = sizeof(ChunkHeader) + sizeof(Message) + sizeof(AnnounceCapabilities);
    auto buffer = ByteBuffer::create_uninitialized(required_size);
    u8* data = buffer.data();

    auto* message = initialize_headers(data, sizeof(AnnounceCapabilities), MessageType::AnnounceCapabilities);

    auto* announce_message = new (message->data) AnnounceCapabilities {
        request,
        {}
    };

    for (auto& cap : capabilities) {
        announce_message->caps[0] |= (1 << (u32)cap);
    }

    return buffer;
}

ByteBuffer SpiceAgent::ClipboardGrab::make_buffer(const Vector<ClipboardType>& types)
{
    VERIFY(types.size() > 0);
    size_t variable_data_size = sizeof(u32) * types.size();
    size_t required_size = sizeof(ChunkHeader) + sizeof(Message) + variable_data_size;
    auto buffer = ByteBuffer::create_uninitialized(required_size);
    u8* data = buffer.data();

    auto* message = initialize_headers(data, variable_data_size, MessageType::ClipboardGrab);

    auto* grab_message = new (message->data) ClipboardGrab {};

    for (auto i = 0u; i < types.size(); i++) {
        grab_message->types[i] = (u32)types[i];
    }

    return buffer;
}

ByteBuffer SpiceAgent::Clipboard::make_buffer(ClipboardType type, ReadonlyBytes contents)
{
    size_t data_size = sizeof(Clipboard) + contents.size();
    size_t required_size = sizeof(ChunkHeader) + sizeof(Message) + data_size;
    auto buffer = ByteBuffer::create_uninitialized(required_size);
    u8* data = buffer.data();

    auto* message = initialize_headers(data, data_size, MessageType::Clipboard);

    auto* clipboard_message = new (message->data) Clipboard {
        .type = (u32)type
    };

    memcpy(clipboard_message->data, contents.data(), contents.size());

    return buffer;
}

ByteBuffer SpiceAgent::ClipboardRequest::make_buffer(ClipboardType type)
{
    size_t data_size = sizeof(ClipboardRequest);
    size_t required_size = sizeof(ChunkHeader) + sizeof(Message) + data_size;
    auto buffer = ByteBuffer::create_uninitialized(required_size);
    u8* data = buffer.data();

    auto* message = initialize_headers(data, data_size, MessageType::ClipboardRequest);
    new (message->data) ClipboardRequest {
        .type = (u32)type
    };

    return buffer;
}
