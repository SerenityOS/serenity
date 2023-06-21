/*
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Format.h>
#include <AK/Forward.h>
#include <AK/String.h>
#include <AK/Vector.h>

namespace SpiceAgent {

static constexpr u32 AGENT_PROTOCOL = 1;

// Used to communicate what the client/or server is capable of.
// Not a lot of documentation is available for all of these, but the headers contain some information:
// https://gitlab.freedesktop.org/spice/spice-protocol/-/blob/master/spice/vd_agent.h
enum class Capability : u32 {
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
    ClipboardGrabSerial
};

// Used to describe the type of data which is present on the user's clipboard.
enum class ClipboardDataType : u32 {
    None = 0,
    Text,
    PNG,
    BMP,
    TIFF,
    JPG,
    __End
};

ErrorOr<String> clipboard_data_type_to_mime_type(ClipboardDataType type);
ErrorOr<ClipboardDataType> clipboard_data_type_from_raw_value(u32 value);
ErrorOr<ClipboardDataType> clipboard_data_type_from_mime_type(String const& mime_type);

// Used to describe what state the current file transfer is in
enum class FileTransferStatus : u32 {
    CanSendData = 0,
    Cancelled,
    Error,
    Success,
    NotEnoughSpace,
    SessionLocked,
    AgentNotConnected,
    Disabled,
    __End
};

class Message {
public:
    // The spice protocol headers contain a bit of documentation about these, but nothing major:
    // https://gitlab.freedesktop.org/spice/spice-protocol/-/blob/master/spice/vd_agent.h
    enum class Type : u32 {
        MouseState = 1,
        MonitorsConfig,
        Reply,
        Clipboard,
        DisplayConfig,
        AnnounceCapabilities,
        ClipboardGrab,
        ClipboardRequest,
        ClipboardRelease,
        FileTransferStart,
        FileTransferStatus,
        FileTransferData,
        Disconnected,
        MaxClipboard,
        VolumeSync,
        GraphicsDeviceInfo
    };

    Message(Type type)
        : m_type(type)
    {
    }

    Type type() { return m_type; }

    virtual ErrorOr<String> debug_description() = 0;
    virtual ~Message() = default;

private:
    Type m_type;
};

// Sent to the server to tell it what we are capable of.
// See the Capabilities enum to see the available capabilities.
class AnnounceCapabilitiesMessage : public Message {
public:
    AnnounceCapabilitiesMessage(bool is_request, Vector<Capability> capabilities = {})
        : Message(Type::AnnounceCapabilities)
        , m_is_request(is_request)
        , m_capabilities(move(capabilities))
    {
    }

    static ErrorOr<AnnounceCapabilitiesMessage> read_from_stream(AK::Stream& stream);

    ErrorOr<void> write_to_stream(AK::Stream& stream);
    ErrorOr<String> debug_description() override;

    bool is_request() const& { return m_is_request; }
    Vector<Capability> const& capabilities() { return m_capabilities; }

private:
    bool m_is_request { false };
    Vector<Capability> m_capabilities;
};

// Sent/received to tell the server/client that clipboard data is available.
class ClipboardGrabMessage : public Message {
public:
    ClipboardGrabMessage(Vector<ClipboardDataType> const& types)
        : Message(Type::ClipboardGrab)
        , m_types(types)
    {
    }

    static ErrorOr<ClipboardGrabMessage> read_from_stream(AK::Stream& stream);

    ErrorOr<void> write_to_stream(AK::Stream& stream);
    ErrorOr<String> debug_description() override;

    Vector<ClipboardDataType> const& types() { return m_types; }

private:
    Vector<ClipboardDataType> m_types;
};

// Request clipboard data with the specified type.
class ClipboardRequestMessage : public Message {
public:
    ClipboardRequestMessage(ClipboardDataType data_type)
        : Message(Type::ClipboardRequest)
        , m_data_type(data_type)
    {
    }

    static ErrorOr<ClipboardRequestMessage> read_from_stream(AK::Stream& stream);

    ErrorOr<void> write_to_stream(AK::Stream& stream);
    ErrorOr<String> debug_description() override;

    ClipboardDataType data_type() { return m_data_type; }

private:
    ClipboardDataType m_data_type;
};

// Used to send the clipboard's contents to the client/server.
class ClipboardMessage : public Message {
public:
    ClipboardMessage(ClipboardDataType data_type, ByteBuffer contents)
        : Message(Type::Clipboard)
        , m_data_type(data_type)
        , m_contents(move(contents))
    {
    }

    ClipboardMessage(ClipboardMessage const&) = delete;
    ClipboardMessage& operator=(ClipboardMessage const&) = delete;

    ClipboardMessage(ClipboardMessage&&) = default;
    ClipboardMessage& operator=(ClipboardMessage&&) = default;

    static ErrorOr<ClipboardMessage> read_from_stream(AK::Stream& stream);

    ErrorOr<void> write_to_stream(AK::Stream& stream);
    ErrorOr<String> debug_description() override;

    ClipboardDataType data_type() { return m_data_type; }
    ByteBuffer const& contents() { return m_contents; }

private:
    ClipboardDataType m_data_type;
    ByteBuffer m_contents;
};

// Sent to the agent to indicate that a file transfer has been requested.
class FileTransferStartMessage : public Message {
public:
    struct Metadata {
        String name;
        u32 size;
    };

    static ErrorOr<FileTransferStartMessage> read_from_stream(AK::Stream& stream);

    ErrorOr<String> debug_description() override;

    u32 id() const { return m_id; }
    Metadata const& metadata() { return m_metadata; }

private:
    FileTransferStartMessage(u32 id, FileTransferStartMessage::Metadata const& metadata)
        : Message(Type::FileTransferStart)
        , m_id(id)
        , m_metadata(metadata)
    {
    }

    u32 m_id { 0 };
    Metadata m_metadata;
};

// Sent/recieved to indicate the status of the current file transfer.
class FileTransferStatusMessage : public Message {
public:
    FileTransferStatusMessage(u32 id, FileTransferStatus status)
        : Message(Type::FileTransferStatus)
        , m_id(id)
        , m_status(status)
    {
    }

    static ErrorOr<FileTransferStatusMessage> read_from_stream(AK::Stream& stream);

    ErrorOr<void> write_to_stream(AK::Stream& stream);
    ErrorOr<String> debug_description() override;

    u32 id() const { return m_id; }
    FileTransferStatus const& status() { return m_status; }

private:
    u32 m_id { 0 };
    FileTransferStatus m_status;
};

// Contains the file data sent from a file transfer request after it has been approved.
class FileTransferDataMessage : public Message {
public:
    static ErrorOr<FileTransferDataMessage> read_from_stream(AK::Stream& stream);

    FileTransferDataMessage(FileTransferDataMessage const&) = delete;
    FileTransferDataMessage& operator=(FileTransferDataMessage const&) = delete;

    FileTransferDataMessage(FileTransferDataMessage&&) = default;
    FileTransferDataMessage& operator=(FileTransferDataMessage&&) = default;

    ErrorOr<String> debug_description() override;

    u32 id() const { return m_id; }
    ByteBuffer const& contents() { return m_contents; }

private:
    FileTransferDataMessage(u32 id, ByteBuffer contents)
        : Message(Type::FileTransferData)
        , m_id(id)
        , m_contents(move(contents))
    {
    }

    u32 m_id { 0 };
    ByteBuffer m_contents;
};

}

namespace AK {
template<>
struct Formatter<SpiceAgent::ClipboardDataType> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, SpiceAgent::ClipboardDataType const& header)
    {
        auto string = "Unknown"sv;
        switch (header) {
        case SpiceAgent::ClipboardDataType::None:
            string = "None"sv;
            break;

        case SpiceAgent::ClipboardDataType::Text:
            string = "Text"sv;
            break;

        case SpiceAgent::ClipboardDataType::PNG:
            string = "PNG"sv;
            break;

        case SpiceAgent::ClipboardDataType::BMP:
            string = "BMP"sv;
            break;

        case SpiceAgent::ClipboardDataType::TIFF:
            string = "TIFF"sv;
            break;

        case SpiceAgent::ClipboardDataType::JPG:
            string = "JPG"sv;
            break;

        default:
            break;
        }

        return Formatter<StringView>::format(builder, string);
    }
};

template<>
struct Formatter<SpiceAgent::FileTransferStatus> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, SpiceAgent::FileTransferStatus const& header)
    {
        auto string = "Unknown"sv;
        switch (header) {
        case SpiceAgent::FileTransferStatus::AgentNotConnected:
            string = "AgentNotConnected"sv;
            break;

        case SpiceAgent::FileTransferStatus::Cancelled:
            string = "Cancelled"sv;
            break;

        case SpiceAgent::FileTransferStatus::CanSendData:
            string = "CanSendData"sv;
            break;

        case SpiceAgent::FileTransferStatus::Disabled:
            string = "Disabled"sv;
            break;

        case SpiceAgent::FileTransferStatus::Error:
            string = "Error"sv;
            break;

        case SpiceAgent::FileTransferStatus::NotEnoughSpace:
            string = "NotEnoughSpace"sv;
            break;

        case SpiceAgent::FileTransferStatus::SessionLocked:
            string = "SessionLocked"sv;
            break;

        case SpiceAgent::FileTransferStatus::Success:
            string = "Success"sv;
            break;

        default:
            break;
        }

        return Formatter<StringView>::format(builder, string);
    }
};
}
