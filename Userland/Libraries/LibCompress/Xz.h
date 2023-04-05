/*
 * Copyright (c) 2023, Tim Schumacher <timschumi@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ConstrainedStream.h>
#include <AK/CountingStream.h>
#include <AK/Endian.h>
#include <AK/Error.h>
#include <AK/MaybeOwned.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <AK/Stream.h>
#include <AK/Vector.h>

namespace Compress {

// This implementation is based on the "The .xz File Format" specification version 1.1.0:
// https://tukaani.org/xz/xz-file-format-1.1.0.txt

// 1.2. Multibyte Integers
class [[gnu::packed]] XzMultibyteInteger {
public:
    constexpr XzMultibyteInteger() = default;

    constexpr XzMultibyteInteger(u64 value)
        : m_value(value)
    {
    }

    constexpr operator u64() const { return m_value; }

    static ErrorOr<XzMultibyteInteger> read_from_stream(Stream& stream);

private:
    u64 m_value { 0 };
};

// 2.1.1.2. Stream Flags
enum XzStreamCheckType : u8 {
    None = 0x00,
    CRC32 = 0x01,
    CRC64 = 0x04,
    SHA256 = 0x0A,
};

// 2.1.1.2. Stream Flags
struct [[gnu::packed]] XzStreamFlags {
    u8 reserved;
    XzStreamCheckType check_type : 4;
    u8 reserved_bits : 4;
};
static_assert(sizeof(XzStreamFlags) == 2);

// 2.1.1. Stream Header
struct [[gnu::packed]] XzStreamHeader {
    u8 magic[6];
    XzStreamFlags flags;
    LittleEndian<u32> flags_crc32;

    ErrorOr<void> validate();
};
static_assert(sizeof(XzStreamHeader) == 12);

// 2.1.2. Stream Footer
struct [[gnu::packed]] XzStreamFooter {
    LittleEndian<u32> size_and_flags_crc32;
    LittleEndian<u32> encoded_backward_size;
    XzStreamFlags flags;
    u8 magic[2];

    ErrorOr<void> validate();
    u32 backward_size();
};
static_assert(sizeof(XzStreamFooter) == 12);

// 3.1.2. Block Flags
struct [[gnu::packed]] XzBlockFlags {
    u8 encoded_number_of_filters : 2;
    u8 reserved : 4;
    bool compressed_size_present : 1;
    bool uncompressed_size_present : 1;

    u8 number_of_filters();
};
static_assert(sizeof(XzBlockFlags) == 1);

// 5.3.1. LZMA2
struct [[gnu::packed]] XzFilterLzma2Properties {
    u8 encoded_dictionary_size : 6;
    u8 reserved : 2;

    ErrorOr<void> validate();
    u32 dictionary_size();
};
static_assert(sizeof(XzFilterLzma2Properties) == 1);

class XzDecompressor : public Stream {
public:
    static ErrorOr<NonnullOwnPtr<XzDecompressor>> create(MaybeOwned<Stream>);

    virtual ErrorOr<Bytes> read_some(Bytes) override;
    virtual ErrorOr<size_t> write_some(ReadonlyBytes) override;
    virtual bool is_eof() const override;
    virtual bool is_open() const override;
    virtual void close() override;

private:
    XzDecompressor(NonnullOwnPtr<CountingStream>);

    ErrorOr<bool> load_next_stream();
    ErrorOr<void> finish_current_block();
    ErrorOr<void> finish_current_stream();

    NonnullOwnPtr<CountingStream> m_stream;
    Optional<XzStreamFlags> m_stream_flags;
    bool m_found_first_stream_header { false };
    bool m_found_last_stream_footer { false };

    Optional<MaybeOwned<Stream>> m_current_block_stream {};
    Optional<u64> m_current_block_expected_uncompressed_size {};
    u64 m_current_block_uncompressed_size {};
    u64 m_current_block_start_offset {};

    struct BlockMetadata {
        u64 uncompressed_size {};
        u64 unpadded_size {};
    };
    Vector<BlockMetadata> m_processed_blocks;
};

}

template<>
struct AK::Traits<Compress::XzStreamHeader> : public AK::GenericTraits<Compress::XzStreamHeader> {
    static constexpr bool is_trivially_serializable() { return true; }
};

template<>
struct AK::Traits<Compress::XzStreamFooter> : public AK::GenericTraits<Compress::XzStreamFooter> {
    static constexpr bool is_trivially_serializable() { return true; }
};

template<>
struct AK::Traits<Compress::XzBlockFlags> : public AK::GenericTraits<Compress::XzBlockFlags> {
    static constexpr bool is_trivially_serializable() { return true; }
};
