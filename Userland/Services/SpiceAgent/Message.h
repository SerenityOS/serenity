/*
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/Forward.h>
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
}
