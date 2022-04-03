/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BitStream.h>
#include <AK/ByteBuffer.h>
#include <AK/CircularDuplexStream.h>
#include <AK/Endian.h>
#include <AK/Vector.h>
#include <LibCompress/DeflateTables.h>

namespace Compress {

class CanonicalCode {
public:
    CanonicalCode() = default;
    u32 read_symbol(InputBitStream&) const;
    void write_symbol(OutputBitStream&, u32) const;

    static CanonicalCode const& fixed_literal_codes();
    static CanonicalCode const& fixed_distance_codes();

    static Optional<CanonicalCode> from_bytes(ReadonlyBytes);

private:
    // Decompression - indexed by code
    Vector<u16> m_symbol_codes;
    Vector<u16> m_symbol_values;

    // Compression - indexed by symbol
    Array<u16, 288> m_bit_codes {}; // deflate uses a maximum of 288 symbols (maximum of 32 for distances)
    Array<u16, 288> m_bit_code_lengths {};
};

class DeflateDecompressor final : public InputStream {
private:
    class CompressedBlock {
    public:
        CompressedBlock(DeflateDecompressor&, CanonicalCode literal_codes, Optional<CanonicalCode> distance_codes);

        bool try_read_more();

    private:
        bool m_eof { false };

        DeflateDecompressor& m_decompressor;
        CanonicalCode m_literal_codes;
        Optional<CanonicalCode> m_distance_codes;
    };

    class UncompressedBlock {
    public:
        UncompressedBlock(DeflateDecompressor&, size_t);

        bool try_read_more();

    private:
        DeflateDecompressor& m_decompressor;
        size_t m_bytes_remaining;
    };

    enum class State {
        Idle,
        ReadingCompressedBlock,
        ReadingUncompressedBlock
    };

public:
    friend CompressedBlock;
    friend UncompressedBlock;

    DeflateDecompressor(InputStream&);
    ~DeflateDecompressor();

    size_t read(Bytes) override;
    bool read_or_error(Bytes) override;
    bool discard_or_error(size_t) override;

    bool unreliable_eof() const override;
    bool handle_any_error() override;

    static Optional<ByteBuffer> decompress_all(ReadonlyBytes);

private:
    u32 decode_length(u32);
    u32 decode_distance(u32);
    void decode_codes(CanonicalCode& literal_code, Optional<CanonicalCode>& distance_code);

    bool m_read_final_bock { false };

    State m_state { State::Idle };
    union {
        CompressedBlock m_compressed_block;
        UncompressedBlock m_uncompressed_block;
    };

    InputBitStream m_input_stream;
    CircularDuplexStream<32 * KiB> m_output_stream;
};

class DeflateCompressor final : public OutputStream {
public:
    static constexpr size_t block_size = 32 * KiB - 1; // TODO: this can theoretically be increased to 64 KiB - 2
    static constexpr size_t window_size = block_size * 2;
    static constexpr size_t hash_bits = 15;
    static constexpr size_t max_huffman_literals = 288;
    static constexpr size_t max_huffman_distances = 32;
    static constexpr size_t min_match_length = 4;   // matches smaller than these are not worth the size of the back reference
    static constexpr size_t max_match_length = 258; // matches longer than these cannot be encoded using huffman codes
    static constexpr u16 empty_slot = UINT16_MAX;

    struct CompressionConstants {
        size_t good_match_length;  // Once we find a match of at least this length (a good enough match) we reduce max_chain to lower processing time
        size_t max_lazy_length;    // If the match is at least this long we dont defer matching to the next byte (which takes time) as its good enough
        size_t great_match_length; // Once we find a match of at least this length (a great match) we can just stop searching for longer ones
        size_t max_chain;          // We only check the actual length of the max_chain closest matches
    };

    // These constants were shamelessly "borrowed" from zlib
    static constexpr CompressionConstants compression_constants[] = {
        { 0, 0, 0, 0 },
        { 4, 4, 8, 4 },
        { 8, 16, 128, 128 },
        { 32, 258, 258, 4096 },
        { max_match_length, max_match_length, max_match_length, 1 << hash_bits } // disable all limits
    };

    enum class CompressionLevel : int {
        STORE = 0,
        FAST,
        GOOD,
        GREAT,
        BEST // WARNING: this one can take an unreasonable amount of time!
    };

    DeflateCompressor(OutputStream&, CompressionLevel = CompressionLevel::GOOD);
    ~DeflateCompressor();

    size_t write(ReadonlyBytes) override;
    bool write_or_error(ReadonlyBytes) override;
    void final_flush();

    static Optional<ByteBuffer> compress_all(ReadonlyBytes bytes, CompressionLevel = CompressionLevel::GOOD);

private:
    Bytes pending_block() { return { m_rolling_window + block_size, block_size }; }

    // LZ77 Compression
    static u16 hash_sequence(u8 const* bytes);
    size_t compare_match_candidate(size_t start, size_t candidate, size_t prev_match_length, size_t max_match_length);
    size_t find_back_match(size_t start, u16 hash, size_t previous_match_length, size_t max_match_length, size_t& match_position);
    void lz77_compress_block();

    // Huffman Coding
    struct code_length_symbol {
        u8 symbol;
        u8 count; // used for special symbols 16-18
    };
    static u8 distance_to_base(u16 distance);
    template<size_t Size>
    static void generate_huffman_lengths(Array<u8, Size>& lengths, Array<u16, Size> const& frequencies, size_t max_bit_length, u16 frequency_cap = UINT16_MAX);
    size_t huffman_block_length(Array<u8, max_huffman_literals> const& literal_bit_lengths, Array<u8, max_huffman_distances> const& distance_bit_lengths);
    void write_huffman(CanonicalCode const& literal_code, Optional<CanonicalCode> const& distance_code);
    static size_t encode_huffman_lengths(Array<u8, max_huffman_literals + max_huffman_distances> const& lengths, size_t lengths_count, Array<code_length_symbol, max_huffman_literals + max_huffman_distances>& encoded_lengths);
    size_t encode_block_lengths(Array<u8, max_huffman_literals> const& literal_bit_lengths, Array<u8, max_huffman_distances> const& distance_bit_lengths, Array<code_length_symbol, max_huffman_literals + max_huffman_distances>& encoded_lengths, size_t& literal_code_count, size_t& distance_code_count);
    void write_dynamic_huffman(CanonicalCode const& literal_code, size_t literal_code_count, Optional<CanonicalCode> const& distance_code, size_t distance_code_count, Array<u8, 19> const& code_lengths_bit_lengths, size_t code_length_count, Array<code_length_symbol, max_huffman_literals + max_huffman_distances> const& encoded_lengths, size_t encoded_lengths_count);

    size_t uncompressed_block_length();
    size_t fixed_block_length();
    size_t dynamic_block_length(Array<u8, max_huffman_literals> const& literal_bit_lengths, Array<u8, max_huffman_distances> const& distance_bit_lengths, Array<u8, 19> const& code_lengths_bit_lengths, Array<u16, 19> const& code_lengths_frequencies, size_t code_lengths_count);
    void flush();

    bool m_finished { false };
    CompressionLevel m_compression_level;
    CompressionConstants m_compression_constants;
    OutputBitStream m_output_stream;

    u8 m_rolling_window[window_size];
    size_t m_pending_block_size { 0 };

    struct [[gnu::packed]] {
        u16 distance; // back reference length
        union {
            u16 literal; // literal byte or on of block symbol
            u16 length;  // back reference length (if distance != 0)
        };
    } m_symbol_buffer[block_size + 1];
    size_t m_pending_symbol_size { 0 };
    Array<u16, max_huffman_literals> m_symbol_frequencies;    // there are 286 valid symbol values (symbols 286-287 never occur)
    Array<u16, max_huffman_distances> m_distance_frequencies; // there are 30 valid distance values (distances 30-31 never occur)

    // LZ77 Chained hash table
    u16 m_hash_head[1 << hash_bits];
    u16 m_hash_prev[window_size];
};

}
