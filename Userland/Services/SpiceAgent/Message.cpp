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

ErrorOr<AnnounceCapabilitiesMessage> AnnounceCapabilitiesMessage::read_from_stream(AK::Stream& stream)
{
    // If this message is a capabilities request, we don't have to parse anything else.
    auto is_requesting = TRY(stream.read_value<u32>()) == 1;
    if (is_requesting) {
        return AnnounceCapabilitiesMessage(is_requesting);
    }

    return Error::from_string_literal("Unexpected non-requesting announce capabilities message received!");
}

ErrorOr<void> AnnounceCapabilitiesMessage::write_to_stream(AK::Stream& stream)
{
    TRY(stream.write_value<u32>(is_request()));

    // Each bit in this u32 indicates if a certain capability is enabled or not.
    u32 capabilities_bits = 0;
    for (auto capability : capabilities()) {
        // FIXME: At the moment, we only support up to 32 capabilities as the Spice protocol
        //        only contains 17 capabilities.
        auto capability_value = to_underlying(capability);
        VERIFY(capability_value < 32);

        capabilities_bits |= 1 << capability_value;
    }

    TRY(stream.write_value(capabilities_bits));

    return {};
}

ErrorOr<String> AnnounceCapabilitiesMessage::debug_description()
{
    StringBuilder builder;
    TRY(builder.try_append("AnnounceCapabilities { "sv));
    TRY(builder.try_appendff("is_request = {}, ", is_request()));
    TRY(builder.try_appendff("capabilities.size() = {}", capabilities().size()));
    TRY(builder.try_append(" }"sv));
    return builder.to_string();
}

}
