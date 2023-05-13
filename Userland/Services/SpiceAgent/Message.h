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

}
