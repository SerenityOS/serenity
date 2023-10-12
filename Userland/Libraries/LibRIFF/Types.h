/*
 * Copyright (c) 2018-2023, the SerenityOS developers.
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedArray.h>
#include <AK/Forward.h>
#include <AK/MemoryStream.h>
#include <AK/Types.h>

namespace RIFF {

static constexpr StringView const riff_magic = "RIFF"sv;
static constexpr StringView const list_chunk_id = "LIST"sv;

static constexpr size_t const chunk_id_size = 4;

struct ChunkID {
    static ErrorOr<ChunkID> read_from_stream(Stream& stream);
    StringView as_ascii_string() const;
    bool operator==(ChunkID const&) const = default;
    bool operator==(StringView const&) const;

    Array<u8, chunk_id_size> id_data;
};

// http://www-mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/Docs/riffmci.pdf page 11 (Chunks)
struct Chunk {
    static ErrorOr<Chunk> read_from_stream(Stream& stream);
    FixedMemoryStream data_stream();

    ChunkID id;
    u32 size;
    FixedArray<u8> data;
};

// http://www-mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/Docs/riffmci.pdf page 23 (LIST type)
struct List {
    static ErrorOr<List> read_from_stream(Stream& stream);

    ChunkID type;
    Vector<Chunk> chunks;
};

}
