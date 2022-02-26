/*
 * Copyright (c) 2021, Kyle Pereira <kyle@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConnectionToClipboardServer.h"
#include <AK/ByteBuffer.h>
#include <AK/Vector.h>
#include <LibCore/Notifier.h>

#pragma once

class SpiceAgent {
public:
    SpiceAgent(int fd, ConnectionToClipboardServer&);

    static constexpr u32 AGENT_PROTOCOL = 1;
    enum class Port {
        Client = 1,
        Server
    };

    struct [[gnu::packed]] ChunkHeader {
        u32 port {};
        u32 size {};
    };

    struct [[gnu::packed]] Message {
        u32 protocol;
        u32 type;
        u64 opaque;
        u32 size;
        u8 data[];
    };

    enum class MessageType {
        MouseState = 1,       // server -> client
        MonitorsConfig,       // client -> agent|server
        Reply,                // agent -> client
        Clipboard,            // both directions
        DisplayConfig,        // client -> agent
        AnnounceCapabilities, // both directions
        ClipboardGrab,        // both directions
        ClipboardRequest,     // both directions
        ClipboardRelease,     // both directions
        FileTransferStart,
        FileTransferStatus,
        FileTransferData,
        Disconnected,
        MaxClipboard,
        VolumeSync,
        GraphicsDeviceInfo,
    };

    enum class Capability {
        MouseState = 0,
        MonitorsConfig,
        Reply,
        Clipboard,
        DisplayConfig,
        ClipboardByDemand,
        ClipboardSelection,
        SparseMonitorsConfig,
        GuestLineEndLF,
        GuestLineEndCRLF,
        MaxClipboard,
        AudioVolumeSync,
        MonitorsConfigPosition,
        FileTransferDisabled,
        FileTransferDetailedErrors,
        GraphicsCardInfo,
        ClipboardNoReleaseOnRegrab,
        ClipboardGrabSerial,
        __End,
    };

    enum class ClipboardType {
        None = 0,
        Text,
        PNG,
        BMP,
        TIFF,
        JPG,
        FileList,
        __Count
    };

    constexpr static size_t CAPABILITIES_SIZE = ((size_t)Capability::__End + 31) / 32;

    struct [[gnu::packed]] AnnounceCapabilities {
        u32 request;
        u32 caps[CAPABILITIES_SIZE];

        static ByteBuffer make_buffer(bool request, const Vector<Capability>& capabilities);
    };

    struct [[gnu::packed]] ClipboardGrab {
        u32 types[0];

        static ByteBuffer make_buffer(Vector<ClipboardType> const&);
    };

    struct [[gnu::packed]] Clipboard {
        u32 type;
        u8 data[];

        static ByteBuffer make_buffer(ClipboardType, ReadonlyBytes);
    };

    struct [[gnu::packed]] ClipboardRequest {
        u32 type;

        static ByteBuffer make_buffer(ClipboardType);
    };

private:
    int m_fd { -1 };
    RefPtr<Core::Notifier> m_notifier;
    ConnectionToClipboardServer& m_clipboard_connection;

    void on_message_received();
    void send_message(const ByteBuffer& buffer);
    bool m_just_set_clip { false };
    void read_n(void* dest, size_t n);
    static Message* initialize_headers(u8* data, size_t additional_data_size, MessageType type);
    static Optional<ClipboardType> mime_type_to_clipboard_type(const String& mime);
};
