/*
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

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

}
