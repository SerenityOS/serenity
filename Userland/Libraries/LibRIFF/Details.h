/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/MemoryStream.h>
#include <LibRIFF/ChunkID.h>

// Despite the name, this header contains details for both RIFF and IFF
namespace RIFF::Detail {

// http://www-mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/Docs/riffmci.pdf page 11 (Chunks)
struct ChunkHeader {
    RIFF::ChunkID id;
    u32 size;
};
static_assert(AssertSize<ChunkHeader, 8>());

// Standard RIFF/IFF file formats use a global chunk with a chunk ID (magic bytes) such as "RIFF" or "FORM".
// A chunk ID right at the start of the global chunk specifies the subformat specific to the file type.
// Example for RIFF from WebP: https://developers.google.com/speed/webp/docs/riff_container#webp_file_header
struct FileHeader {
    ChunkHeader global_header;
    RIFF::ChunkID subformat;

    constexpr ChunkID magic() const { return global_header.id; }
    constexpr u32 file_size() const { return global_header.size; }
};
static_assert(AssertSize<FileHeader, 12>());

// An RIFF or IFF chunk.
class Chunk {
public:
    Chunk(ChunkHeader header, ReadonlyBytes data);

    RIFF::ChunkID id() const { return m_header.id; }
    u32 size() const { return m_header.size; }
    ReadonlyBytes data() const { return m_data; }
    FixedMemoryStream data_stream() const;

    constexpr u8 operator[](size_t index) const { return data()[index]; }

private:
    ChunkHeader m_header;
    ReadonlyBytes m_data;
};

// Owns the chunk data and can therefore be parsed from a stream.
class OwnedChunk : public Chunk {
public:
    using Buffer = AK::Detail::ByteBuffer<0>;

    OwnedChunk(ChunkHeader, Buffer);

private:
    Buffer m_backing_data;
};

}
