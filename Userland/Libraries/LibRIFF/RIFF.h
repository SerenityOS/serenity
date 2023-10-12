/*
 * Copyright (c) 2018-2023, the SerenityOS developers.
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Vector.h>
#include <LibRIFF/ChunkID.h>
#include <LibRIFF/Details.h>

// RIFF chunks (as often used by Microsoft's older formats) use little-endian fields.
namespace RIFF {

static constexpr StringView const riff_magic = "RIFF"sv;
static constexpr StringView const list_chunk_id = "LIST"sv;

struct ChunkHeader : public Detail::ChunkHeader {
    using WordType = LittleEndian<u32>;
    static ErrorOr<ChunkHeader> read_from_stream(Stream& stream);
};

struct FileHeader : public Detail::FileHeader {
    using HeaderType = ChunkHeader;
    static ErrorOr<FileHeader> read_from_stream(Stream& stream);
};

class Chunk : public Detail::Chunk {
public:
    using HeaderType = ChunkHeader;
    // Note that the resulting chunk will refer to the provided data.
    static ErrorOr<Chunk> decode(ReadonlyBytes data);
    static ErrorOr<Chunk> decode_and_advance(ReadonlyBytes& data);
};

class OwnedChunk : public Detail::OwnedChunk {
public:
    using HeaderType = ChunkHeader;
    static ErrorOr<OwnedChunk> read_from_stream(Stream& stream);
};

// http://www-mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/Docs/riffmci.pdf page 23 (LIST type)
struct OwnedList {
    static ErrorOr<OwnedList> read_from_stream(Stream& stream);

    ChunkID type;
    Vector<OwnedChunk> chunks;
};

}
