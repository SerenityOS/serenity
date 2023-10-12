/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Details.h"

namespace RIFF {

StringView ChunkID::as_ascii_string() const
{
    return StringView { id_data.span() };
}

bool ChunkID::operator==(StringView other_string) const
{
    return as_ascii_string() == other_string;
}

namespace Detail {

Chunk::Chunk(ChunkHeader header, ReadonlyBytes data)
    : m_header(header)
    , m_data(data)
{
    VERIFY(data.size() == header.size);
}

FixedMemoryStream Chunk::data_stream() const
{
    return FixedMemoryStream { m_data };
}

OwnedChunk::OwnedChunk(ChunkHeader header, Buffer backing_data)
    : Chunk(header, backing_data.span())
    , m_backing_data(move(backing_data))
{
}

}
}
