/*
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ChunkHeader.h"
#include <AK/Stream.h>

namespace SpiceAgent {

ChunkHeader ChunkHeader::create(Port port, u32 size)
{
    return ChunkHeader(port, size);
}

ErrorOr<ChunkHeader> ChunkHeader::read_from_stream(AK::Stream& stream)
{
    auto port = TRY(stream.read_value<Port>());
    auto size = TRY(stream.read_value<u32>());

    return ChunkHeader::create(port, size);
}

ErrorOr<void> ChunkHeader::write_to_stream(AK::Stream& stream) const
{
    TRY(stream.write_value(port()));
    TRY(stream.write_value(size()));

    return {};
}

}
