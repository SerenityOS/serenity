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

// Used to communicate what the client/or server is capable of
// TODO(Caoimhe): Document these types
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

// Used to describe the type of data which is present on the user's clipboard
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
    // TODO(Caoimhe): Document these types
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
        : m_type(type) {};

    Type const& type() { return m_type; };

    // Used for sending messages to the spice server
    virtual ErrorOr<void> write_to_stream(AK::Stream& stream);

    // Used for debugging
    virtual ErrorOr<String> debug_description();

    virtual ~Message() = default;

private:
    Type m_type;
};

// Sent to the server to tell it what we are capable of
// See the Capabilities enum to see the available capabilities
class AnnounceCapabilitiesMessage : public Message {

public:
    static ErrorOr<AnnounceCapabilitiesMessage> create(Vector<Capability> const& capabilities);
    static ErrorOr<AnnounceCapabilitiesMessage> read_from_stream(AK::Stream& stream);

    ErrorOr<void> write_to_stream(AK::Stream& stream) override;
    ErrorOr<String> debug_description() override;

    // If this message was a request for capabilities or not
    bool is_requesting() const& { return m_is_requesting; };

    // The capabilities that this client/server supports
    Vector<Capability> const& capabilities() { return m_capabilities; };

private:
    AnnounceCapabilitiesMessage(bool is_requesting);
    AnnounceCapabilitiesMessage(Vector<Capability>);

    bool m_is_requesting { false };
    Vector<Capability> m_capabilities {};
};

// Sent to tell the server that clipboard data is available
// Received to tell the agent... ^^
class ClipboardGrabMessage : public Message {
public:
    static ErrorOr<ClipboardGrabMessage> create(Vector<ClipboardDataType> const& types);
    static ErrorOr<ClipboardGrabMessage> read_from_stream(AK::Stream& stream);

    ErrorOr<void> write_to_stream(AK::Stream& stream) override;
    ErrorOr<String> debug_description() override;

    // The types of data present on the clipboard
    Vector<ClipboardDataType> const& types() { return m_types; };

private:
    ClipboardGrabMessage(Vector<ClipboardDataType> const& types);

    Vector<ClipboardDataType> m_types;
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
