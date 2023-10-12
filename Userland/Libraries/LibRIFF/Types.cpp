/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Types.h"
#include <AK/Endian.h>
#include <AK/Stream.h>
#include <AK/Try.h>
#include <AK/TypeCasts.h>

namespace RIFF {

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

    // RIFF chunks may have trailing padding to align to x86 "words" (i.e. 2 bytes).
    if (is<SeekableStream>(stream)) {
        if (!stream.is_eof()) {
            auto stream_position = TRY(static_cast<SeekableStream&>(stream).tell());
            if (stream_position % 2 != 0)
                TRY(static_cast<SeekableStream&>(stream).seek(1, SeekMode::FromCurrentPosition));
        }
    } else {
        dbgln("RIFF Warning: Cannot align stream to 2-byte boundary, next chunk may be bogus!");
    }

    return Chunk {
        id,
        size,
        move(data),
    };
}

ErrorOr<List> List::read_from_stream(Stream& stream)
{
    auto type = TRY(stream.read_value<ChunkID>());
    Vector<Chunk> chunks;
    while (!stream.is_eof())
        TRY(chunks.try_append(TRY(stream.read_value<Chunk>())));

    return List {
        .type = type,
        .chunks = move(chunks),
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
