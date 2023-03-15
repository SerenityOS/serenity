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

// RIFF-specific type definitions necessary for handling WAVE files.
namespace Audio::RIFF {

static constexpr StringView const riff_magic = "RIFF"sv;
static constexpr StringView const wave_subformat_id = "WAVE"sv;
static constexpr StringView const data_chunk_id = "data"sv;
static constexpr StringView const list_chunk_id = "LIST"sv;
static constexpr StringView const info_chunk_id = "INFO"sv;
static constexpr StringView const format_chunk_id = "fmt "sv;

// Constants for handling WAVE header data.
enum class WaveFormat : u32 {
    Pcm = 0x0001,        // WAVE_FORMAT_PCM
    IEEEFloat = 0x0003,  // WAVE_FORMAT_IEEE_FLOAT
    ALaw = 0x0006,       // 8-bit ITU-T G.711 A-law
    MuLaw = 0x0007,      // 8-bit ITU-T G.711 µ-law
    Extensible = 0xFFFE, // Determined by SubFormat
};

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

}
