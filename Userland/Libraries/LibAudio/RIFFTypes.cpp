/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RIFFTypes.h"
#include <AK/Endian.h>
#include <AK/Stream.h>
#include <AK/Try.h>

namespace Audio::RIFF {

ErrorOr<ChunkID> ChunkID::read_from_stream(Stream& stream)
{
    Array<u8, chunk_id_size> id;
    TRY(stream.read_until_filled(id.span()));
    return ChunkID { id };
}

ErrorOr<Chunk> Chunk::read_from_stream(Stream& stream)
{
    auto id = TRY(stream.read_value<ChunkID>());

    u32 size = TRY(stream.read_value<LittleEndian<u32>>());
    auto data = TRY(FixedArray<u8>::create(size));
    TRY(stream.read_until_filled(data.span()));

    return Chunk {
        id,
        size,
        move(data),
    };
}

StringView ChunkID::as_ascii_string() const
{
    return StringView { id_data.span() };
}

bool ChunkID::operator==(StringView const& other_string) const
{
    return as_ascii_string() == other_string;
}

FixedMemoryStream Chunk::data_stream()
{
    return FixedMemoryStream { data.span() };
}

}
