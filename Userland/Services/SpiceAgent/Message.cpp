/*
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Message.h"
#include <AK/MemoryStream.h>
#include <AK/Stream.h>
#include <AK/String.h>

namespace SpiceAgent {

ErrorOr<void> Message::write_to_stream(AK::Stream&)
{
    return Error::from_string_literal("write_to_stream not implemented!");
}

ErrorOr<String> Message::debug_description()
{
    return TRY("Message {}"_string);
}

AnnounceCapabilitiesMessage::AnnounceCapabilitiesMessage(bool is_requesting)
    : Message(Type::AnnounceCapabilities)
    , m_is_requesting(is_requesting)
{
}

AnnounceCapabilitiesMessage::AnnounceCapabilitiesMessage(Vector<Capability> capabilities)
    : Message(Type::AnnounceCapabilities)
    , m_is_requesting(false)
    , m_capabilities(move(capabilities))
{
}

ErrorOr<AnnounceCapabilitiesMessage> AnnounceCapabilitiesMessage::create(Vector<Capability> const& capabilities)
{
    return AnnounceCapabilitiesMessage(capabilities);
}

ErrorOr<AnnounceCapabilitiesMessage> AnnounceCapabilitiesMessage::read_from_stream(AK::Stream& stream)
{
    // If this message is a capabilities request, we don't have to parse anything else
    auto is_requesting = TRY(stream.read_value<u32>()) == 1;
    if (is_requesting) {
        return AnnounceCapabilitiesMessage(is_requesting);
    }

    return Error::from_string_literal("Unexpected non-requesting announce capabilities message received!");
}

ErrorOr<void> AnnounceCapabilitiesMessage::write_to_stream(AK::Stream& stream)
{
    TRY(stream.write_value<u32>(is_requesting()));
    for (auto capability : capabilities()) {
        TRY(stream.write_value(1 << to_underlying(capability)));
    }

    return {};
}

ErrorOr<String> AnnounceCapabilitiesMessage::debug_description()
{
    StringBuilder builder;
    builder.append("AnnounceCapabilities { "sv);
    builder.appendff("is_requesting = {}, ", is_requesting());
    builder.appendff("capabilities.size() = {}", capabilities().size());
    builder.append(" }"sv);
    return builder.to_string();
}

ClipboardGrabMessage::ClipboardGrabMessage(Vector<ClipboardDataType> const& types)
    : Message(Type::ClipboardGrab)
    , m_types(types)
{
}

ErrorOr<ClipboardGrabMessage> ClipboardGrabMessage::create(Vector<ClipboardDataType> const& types)
{
    return ClipboardGrabMessage(types);
}

ErrorOr<ClipboardGrabMessage> ClipboardGrabMessage::read_from_stream(AK::Stream& stream)
{
    auto types = Vector<ClipboardDataType>();
    while (!stream.is_eof()) {
        auto value = TRY(stream.read_value<u32>());
        if (value >= to_underlying(ClipboardDataType::__End)) {
            return Error::from_string_literal("Unsupported clipboard type");
        }

        types.append(static_cast<ClipboardDataType>(value));
    }

    return ClipboardGrabMessage::create(types);
}

ErrorOr<void> ClipboardGrabMessage::write_to_stream(AK::Stream& stream)
{
    for (auto type : types()) {
        TRY(stream.write_value(type));
    }

    return {};
}

ErrorOr<String> ClipboardGrabMessage::debug_description()
{
    StringBuilder builder;
    builder.append("ClipboardGrabMessage { "sv);
    builder.appendff("types = {}", types());
    builder.append(" }"sv);
    return builder.to_string();
}

}
