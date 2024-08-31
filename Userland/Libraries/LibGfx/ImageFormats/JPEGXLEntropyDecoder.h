/*
 * Copyright (c) 2024, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BitStream.h>
#include <LibCompress/Brotli.h>

namespace Gfx {

/// C - Entropy decoding
class ANSHistogram {
public:
    static ErrorOr<ANSHistogram> read_histogram(LittleEndianInputBitStream& stream, u8 log_alphabet_size);

    ErrorOr<u16> read_symbol(LittleEndianInputBitStream& stream, Optional<u32>& state) const;

private:
    static ErrorOr<u8> U8(LittleEndianInputBitStream& stream);

    struct SymbolAndOffset {
        u16 symbol {};
        u16 offset {};
    };

    SymbolAndOffset alias_mapping(u32 x) const;

    static ErrorOr<u16> read_with_prefix(LittleEndianInputBitStream& stream);

    ErrorOr<u16> read_ans_distribution(LittleEndianInputBitStream& stream, u8 log_alphabet_size);

    Vector<u16> m_symbols;
    Vector<u16> m_offsets;
    Vector<u16> m_cutoffs;

    FixedArray<i32> m_distribution;

    u16 m_log_bucket_size {};
    u16 m_bucket_size {};
};

struct LZ77 {
    bool lz77_enabled {};

    u32 min_symbol {};
    u32 min_length {};
};

class EntropyDecoder {
    AK_MAKE_NONCOPYABLE(EntropyDecoder);
    AK_MAKE_DEFAULT_MOVABLE(EntropyDecoder);

public:
    EntropyDecoder() = default;
    ~EntropyDecoder()
    {
        if (m_state.has_value() && *m_state != 0x130000)
            dbgln("JPEGXLLoader: ANS decoder left in invalid state");
    }

    static ErrorOr<EntropyDecoder> create(LittleEndianInputBitStream& stream, u32 initial_num_distrib);

    ErrorOr<u32> decode_hybrid_uint(LittleEndianInputBitStream& stream, u32 context);

    void set_dist_multiplier(u32 dist_multiplier)
    {
        m_dist_multiplier = dist_multiplier;
    }

private:
    using BrotliCanonicalCode = Compress::Brotli::CanonicalCode;

    struct HybridUint {
        u32 split_exponent {};
        u32 split {};
        u32 msb_in_token {};
        u32 lsb_in_token {};
    };

    static ErrorOr<u32> read_uint(LittleEndianInputBitStream& stream, HybridUint const& config, u32 token);
    static ErrorOr<HybridUint> read_config(LittleEndianInputBitStream& stream, u8 log_alphabet_size);

    ErrorOr<u32> read_symbol(LittleEndianInputBitStream& stream, u32 context);
    ErrorOr<void> read_pre_clustered_distributions(LittleEndianInputBitStream& stream, u32 num_distrib);

    LZ77 m_lz77 {};
    u32 m_lz_dist_ctx {};
    HybridUint m_lz_len_conf {};
    FixedArray<u32> m_lz77_window {};
    u32 m_lz77_num_to_copy {};
    u32 m_lz77_copy_pos {};
    u32 m_lz77_num_decoded {};
    u32 m_dist_multiplier {};

    Vector<u32> m_clusters;
    Vector<HybridUint> m_configs;

    u8 m_log_alphabet_size { 15 };

    Variant<Vector<BrotliCanonicalCode>, Vector<ANSHistogram>> m_distributions { Vector<BrotliCanonicalCode> {} }; // D in the spec
    Optional<u32> m_state {};
};
///

}
