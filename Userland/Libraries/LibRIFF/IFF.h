/*
 * Copyright (c) 2018-2023, the SerenityOS developers.
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibRIFF/ChunkID.h>
#include <LibRIFF/Details.h>

// IFF chunks (as often used by Amiga, EA and more modern formats) use big-endian fields.
namespace IFF {

using ChunkID = RIFF::ChunkID;

struct ChunkHeader : public RIFF::Detail::ChunkHeader {
    using WordType = BigEndian<u32>;
    static ErrorOr<ChunkHeader> read_from_stream(Stream& stream);
};

struct FileHeader : public RIFF::Detail::FileHeader {
    using HeaderType = ChunkHeader;
    static ErrorOr<FileHeader> read_from_stream(Stream& stream);
};

class Chunk : public RIFF::Detail::Chunk {
public:
    using HeaderType = ChunkHeader;
    // Note that the resulting chunk will refer to the provided data.
    static ErrorOr<Chunk> decode(ReadonlyBytes data);
    static ErrorOr<Chunk> decode_and_advance(ReadonlyBytes& data);
};

class OwnedChunk : public RIFF::Detail::OwnedChunk {
public:
    using HeaderType = ChunkHeader;
    static ErrorOr<OwnedChunk> read_from_stream(Stream& stream);
};

}
