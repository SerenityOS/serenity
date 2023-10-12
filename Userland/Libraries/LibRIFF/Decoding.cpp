/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2023, Nicolas Ramz <nicolas.ramz@gmail.com>
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <AK/Stream.h>
#include <AK/Try.h>
#include <AK/TypeCasts.h>
#include <LibRIFF/ChunkID.h>
#include <LibRIFF/IFF.h>
#include <LibRIFF/RIFF.h>

ErrorOr<RIFF::ChunkID> RIFF::ChunkID::read_from_stream(Stream& stream)
{
    Array<u8, chunk_id_size> id;
    TRY(stream.read_until_filled(id.span()));
    return ChunkID { id };
}

template<typename ChunkType>
static ErrorOr<ChunkType> decode_chunk(ReadonlyBytes data)
{
    auto data_stream = FixedMemoryStream { data };
    auto const header = TRY(ChunkType::HeaderType::read_from_stream(data_stream));

    if (data.size() < sizeof(typename ChunkType::HeaderType) + header.size)
        return Error::from_string_literal("Not enough data for IFF/RIFF chunk");

    return static_cast<ChunkType>(RIFF::Detail::Chunk { header, data.slice(sizeof(typename ChunkType::HeaderType), header.size) });
}
ErrorOr<RIFF::Chunk> RIFF::Chunk::decode(ReadonlyBytes data) { return decode_chunk<RIFF::Chunk>(data); }
ErrorOr<IFF::Chunk> IFF::Chunk::decode(ReadonlyBytes data) { return decode_chunk<IFF::Chunk>(data); }

template<typename ChunkType>
static ErrorOr<ChunkType> decode_chunk_and_advance(ReadonlyBytes& data)
{
    auto chunk = TRY(decode_chunk<ChunkType>(data));
    data = data.slice(sizeof(typename ChunkType::HeaderType) + chunk.size());
    // add padding if needed
    if (chunk.size() % 2 != 0) {
        if (data.is_empty())
            return Error::from_string_literal("Missing data for padding byte");
        if (*data.data() != 0)
            return Error::from_string_literal("Padding byte is not 0");
        data = data.slice(1);
    }

    return chunk;
}
ErrorOr<RIFF::Chunk> RIFF::Chunk::decode_and_advance(ReadonlyBytes& data) { return decode_chunk_and_advance<RIFF::Chunk>(data); }
ErrorOr<IFF::Chunk> IFF::Chunk::decode_and_advance(ReadonlyBytes& data) { return decode_chunk_and_advance<IFF::Chunk>(data); }

template<typename ChunkType>
static ErrorOr<ChunkType> read_owned_chunk_from_stream(Stream& stream)
{
    auto header = TRY(stream.read_value<typename ChunkType::HeaderType>());

    auto data = TRY(ChunkType::Buffer::create_uninitialized(header.size));
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

    return static_cast<ChunkType>(RIFF::Detail::OwnedChunk { header, data });
}
ErrorOr<RIFF::OwnedChunk> RIFF::OwnedChunk::read_from_stream(Stream& stream) { return read_owned_chunk_from_stream<RIFF::OwnedChunk>(stream); }
ErrorOr<IFF::OwnedChunk> IFF::OwnedChunk::read_from_stream(Stream& stream) { return read_owned_chunk_from_stream<IFF::OwnedChunk>(stream); }

template<typename HeaderType>
static ErrorOr<HeaderType> read_chunk_header_from_stream(Stream& stream)
{
    auto id = TRY(stream.read_value<RIFF::ChunkID>());
    u32 size = TRY(stream.read_value<typename HeaderType::WordType>());
    return HeaderType { id, size };
}
ErrorOr<RIFF::ChunkHeader> RIFF::ChunkHeader::read_from_stream(Stream& stream) { return read_chunk_header_from_stream<RIFF::ChunkHeader>(stream); }
ErrorOr<IFF::ChunkHeader> IFF::ChunkHeader::read_from_stream(Stream& stream) { return read_chunk_header_from_stream<IFF::ChunkHeader>(stream); }

template<typename HeaderType>
static ErrorOr<HeaderType> read_file_header_from_stream(Stream& stream)
{
    auto header = TRY(stream.read_value<typename HeaderType::HeaderType>());
    auto subformat = TRY(stream.read_value<RIFF::ChunkID>());
    return HeaderType { header, subformat };
}
ErrorOr<RIFF::FileHeader> RIFF::FileHeader::read_from_stream(Stream& stream) { return read_file_header_from_stream<RIFF::FileHeader>(stream); }
ErrorOr<IFF::FileHeader> IFF::FileHeader::read_from_stream(Stream& stream) { return read_file_header_from_stream<IFF::FileHeader>(stream); }

ErrorOr<RIFF::OwnedList> RIFF::OwnedList::read_from_stream(Stream& stream)
{
    auto type = TRY(stream.read_value<ChunkID>());
    Vector<RIFF::OwnedChunk> chunks;
    while (!stream.is_eof())
        TRY(chunks.try_append(TRY(stream.read_value<RIFF::OwnedChunk>())));

    return RIFF::OwnedList { .type = type, .chunks = move(chunks) };
}
