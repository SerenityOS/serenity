/*
 * Copyright (c) 2024, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IntegralMath.h>
#include <LibGfx/ImageFormats/JPEGXLCommon.h>
#include <LibGfx/ImageFormats/JPEGXLEntropyDecoder.h>

namespace Gfx {

ErrorOr<ANSHistogram> ANSHistogram::read_histogram(LittleEndianInputBitStream& stream, u8 log_alphabet_size)
{
    ANSHistogram histogram;

    auto const alphabet_size = TRY(histogram.read_ans_distribution(stream, log_alphabet_size));

    // C.2.6 - Alias mapping

    histogram.m_log_bucket_size = 12 - log_alphabet_size;
    histogram.m_bucket_size = 1 << histogram.m_log_bucket_size;
    auto const table_size = 1 << log_alphabet_size;

    Optional<u64> index_of_unique_symbol {};
    for (u64 i {}; i < histogram.m_distribution.size(); ++i) {
        if (histogram.m_distribution[i] == 1 << 12)
            index_of_unique_symbol = i;
    }

    TRY(histogram.m_symbols.try_resize(table_size));
    TRY(histogram.m_offsets.try_resize(table_size));
    TRY(histogram.m_cutoffs.try_resize(table_size));

    if (index_of_unique_symbol.has_value()) {
        auto const s = *index_of_unique_symbol;
        for (i32 i = 0; i < table_size; i++) {
            histogram.m_symbols[i] = s;
            histogram.m_offsets[i] = histogram.m_bucket_size * i;
            histogram.m_cutoffs[i] = 0;
        }
        return histogram;
    }

    Vector<u16> overfull;
    Vector<u16> underfull;

    for (u16 i {}; i < alphabet_size; i++) {
        histogram.m_cutoffs[i] = histogram.m_distribution[i];
        histogram.m_symbols[i] = i;
        if (histogram.m_cutoffs[i] > histogram.m_bucket_size)
            TRY(overfull.try_append(i));
        else if (histogram.m_cutoffs[i] < histogram.m_bucket_size)
            TRY(underfull.try_append(i));
    }

    for (u16 i = alphabet_size; i < table_size; i++) {
        histogram.m_cutoffs[i] = 0;
        TRY(underfull.try_append(i));
    }

    while (overfull.size() > 0) {
        VERIFY(underfull.size() > 0);
        auto const o = overfull.take_last();
        auto const u = underfull.take_last();

        auto const by = histogram.m_bucket_size - histogram.m_cutoffs[u];
        histogram.m_cutoffs[o] -= by;
        histogram.m_symbols[u] = o;
        histogram.m_offsets[u] = histogram.m_cutoffs[o];
        if (histogram.m_cutoffs[o] < histogram.m_bucket_size)
            TRY(underfull.try_append(o));
        else if (histogram.m_cutoffs[o] > histogram.m_bucket_size)
            TRY(overfull.try_append(o));
    }

    for (u16 i {}; i < table_size; i++) {
        if (histogram.m_cutoffs[i] == histogram.m_bucket_size) {
            histogram.m_symbols[i] = i;
            histogram.m_offsets[i] = 0;
            histogram.m_cutoffs[i] = 0;
        } else {
            histogram.m_offsets[i] -= histogram.m_cutoffs[i];
        }
    }

    return histogram;
}

ErrorOr<u16> ANSHistogram::read_symbol(LittleEndianInputBitStream& stream, Optional<u32>& state) const
{
    if (!state.has_value())
        state = TRY(stream.read_bits(32));

    auto const index = *state & 0xFFF;
    auto const symbol_and_offset = alias_mapping(index);
    state = m_distribution[symbol_and_offset.symbol] * (*state >> 12) + symbol_and_offset.offset;
    if (*state < (1 << 16))
        state = (*state << 16) | TRY(stream.read_bits(16));
    return symbol_and_offset.symbol;
}

ErrorOr<u8> ANSHistogram::U8(LittleEndianInputBitStream& stream)
{
    if (TRY(stream.read_bit()) == 0)
        return 0;
    auto const n = TRY(stream.read_bits(3));
    return TRY(stream.read_bits(n)) + (1 << n);
}

ANSHistogram::SymbolAndOffset ANSHistogram::alias_mapping(u32 x) const
{
    // C.2.6 - Alias mapping
    auto const i = x >> m_log_bucket_size;
    auto const pos = x & (m_bucket_size - 1);
    u16 const symbol = pos >= m_cutoffs[i] ? m_symbols[i] : i;
    u16 const offset = pos >= m_cutoffs[i] ? m_offsets[i] + pos : pos;

    return { symbol, offset };
}

ErrorOr<u16> ANSHistogram::read_with_prefix(LittleEndianInputBitStream& stream)
{
    auto const prefix = TRY(stream.read_bits(3));

    switch (prefix) {
    case 0:
        return 10;
    case 1:
        for (auto const possibility : { 4, 0, 11, 13 }) {
            if (TRY(stream.read_bit()))
                return possibility;
        }
        return 12;
    case 2:
        return 7;
    case 3:
        return TRY(stream.read_bit()) ? 1 : 3;
    case 4:
        return 6;
    case 5:
        return 8;
    case 6:
        return 9;
    case 7:
        return TRY(stream.read_bit()) ? 2 : 5;
    default:
        VERIFY_NOT_REACHED();
    }
}

ErrorOr<u16> ANSHistogram::read_ans_distribution(LittleEndianInputBitStream& stream, u8 log_alphabet_size)
{
    // C.2.5  ANS distribution decoding
    auto const table_size = 1 << log_alphabet_size;

    m_distribution = TRY(FixedArray<i32>::create(table_size));

    if (TRY(stream.read_bit())) {
        u16 alphabet_size {};
        if (TRY(stream.read_bit())) {
            auto const v1 = TRY(U8(stream));
            auto const v2 = TRY(U8(stream));
            VERIFY(v1 != v2);
            m_distribution[v1] = TRY(stream.read_bits(12));
            m_distribution[v2] = (1 << 12) - m_distribution[v1];
            alphabet_size = 1 + max(v1, v2);
        } else {
            auto const x = TRY(U8(stream));
            m_distribution[x] = 1 << 12;
            alphabet_size = 1 + x;
        }
        return alphabet_size;
    }

    if (TRY(stream.read_bit())) {
        auto const alphabet_size = TRY(U8(stream)) + 1;
        for (u16 i = 0; i < alphabet_size; i++)
            m_distribution[i] = (1 << 12) / alphabet_size;
        for (u16 i = 0; i < ((1 << 12) % alphabet_size); i++)
            m_distribution[i]++;
        return alphabet_size;
    }

    u8 len = 0;
    while (len < 3) {
        if (TRY(stream.read_bit()))
            len++;
        else
            break;
    }

    u8 const shift = TRY(stream.read_bits(len)) + (1 << len) - 1;
    VERIFY(shift <= 13);

    auto const alphabet_size = TRY(U8(stream)) + 3;

    i32 omit_log = -1;
    i32 omit_pos = -1;

    auto same = TRY(FixedArray<i32>::create(alphabet_size));
    auto logcounts = TRY(FixedArray<i32>::create(alphabet_size));

    u8 rle {};
    for (u16 i = 0; i < alphabet_size; i++) {
        logcounts[i] = TRY(read_with_prefix(stream));

        if (logcounts[i] == 13) {
            rle = TRY(U8(stream));
            same[i] = rle + 5;
            i += rle + 3;
            continue;
        }
        if (logcounts[i] > omit_log) {
            omit_log = logcounts[i];
            omit_pos = i;
        }
    }

    VERIFY(m_distribution[omit_pos] >= 0);
    VERIFY(omit_pos + 1 >= alphabet_size || logcounts[omit_pos + 1] != 13);

    i32 prev = 0;
    i32 numsame = 0;
    i64 total_count {};
    for (u16 i = 0; i < alphabet_size; i++) {
        if (same[i] != 0) {
            numsame = same[i] - 1;
            prev = i > 0 ? m_distribution[i - 1] : 0;
        }
        if (numsame > 0) {
            m_distribution[i] = prev;
            numsame--;
        } else {
            auto const code = logcounts[i];
            if (i == omit_pos || code == 0)
                continue;

            if (code == 1) {
                m_distribution[i] = 1;
            } else {
                auto const bitcount = min(max(0, shift - ((12 - code + 1) >> 1)), code - 1);
                m_distribution[i] = (1 << (code - 1)) + (TRY(stream.read_bits(bitcount)) << (code - 1 - bitcount));
            }
        }
        total_count += m_distribution[i];
    }
    m_distribution[omit_pos] = (1 << 12) - total_count;
    VERIFY(m_distribution[omit_pos] >= 0);

    return alphabet_size;
}

namespace {
ErrorOr<LZ77> read_lz77(LittleEndianInputBitStream& stream)
{
    LZ77 lz77;

    lz77.lz77_enabled = TRY(stream.read_bit());

    if (lz77.lz77_enabled) {
        lz77.min_symbol = U32(224, 512, 4096, 8 + TRY(stream.read_bits(15)));
        lz77.min_length = U32(3, 4, 5 + TRY(stream.read_bits(2)), 9 + TRY(stream.read_bits(8)));
    }

    return lz77;
}
}

ErrorOr<EntropyDecoder> EntropyDecoder::create(LittleEndianInputBitStream& stream, u32 initial_num_distrib)

{
    EntropyDecoder entropy_decoder;
    // C.2 - Distribution decoding
    entropy_decoder.m_lz77 = TRY(read_lz77(stream));

    if (entropy_decoder.m_lz77.lz77_enabled) {
        entropy_decoder.m_lz_dist_ctx = initial_num_distrib++;
        entropy_decoder.m_lz_len_conf = TRY(read_config(stream, 8));

        entropy_decoder.m_lz77_window = TRY(FixedArray<u32>::create(1 << 20));
    }

    TRY(entropy_decoder.read_pre_clustered_distributions(stream, initial_num_distrib));

    bool const use_prefix_code = TRY(stream.read_bit());

    if (!use_prefix_code)
        entropy_decoder.m_log_alphabet_size = 5 + TRY(stream.read_bits(2));

    for (auto& config : entropy_decoder.m_configs)
        config = TRY(read_config(stream, entropy_decoder.m_log_alphabet_size));

    if (use_prefix_code) {
        entropy_decoder.m_distributions = Vector<BrotliCanonicalCode> {};
        auto& distributions = entropy_decoder.m_distributions.get<Vector<BrotliCanonicalCode>>();
        TRY(distributions.try_resize(entropy_decoder.m_configs.size()));

        Vector<u16> counts;
        TRY(counts.try_resize(entropy_decoder.m_configs.size()));

        for (auto& count : counts) {
            if (TRY(stream.read_bit())) {
                auto const n = TRY(stream.read_bits(4));
                count = 1 + (1 << n) + TRY(stream.read_bits(n));
            } else {
                count = 1;
            }
        }

        // After reading the counts, the decoder reads each D[i] (implicitly
        // described by a prefix code) as specified in C.2.4, with alphabet_size = count[i].
        for (u32 i {}; i < distributions.size(); ++i) {
            // The alphabet size mentioned in the [Brotli] RFC is explicitly specified as parameter alphabet_size
            // when the histogram is being decoded, except in the special case of alphabet_size == 1, where no
            // histogram is read, and all decoded symbols are zero without reading any bits at all.
            if (counts[i] != 1)
                distributions[i] = TRY(BrotliCanonicalCode::read_prefix_code(stream, counts[i]));
            else
                distributions[i] = BrotliCanonicalCode { { 1 }, { 0 } };
        }
    } else {
        entropy_decoder.m_distributions = Vector<ANSHistogram> {};
        auto& distributions = entropy_decoder.m_distributions.get<Vector<ANSHistogram>>();
        TRY(distributions.try_ensure_capacity(entropy_decoder.m_configs.size()));

        for (u32 i = 0; i < entropy_decoder.m_configs.size(); ++i)
            distributions.empend(TRY(ANSHistogram::read_histogram(stream, entropy_decoder.m_log_alphabet_size)));
    }

    return entropy_decoder;
}

ErrorOr<u32> EntropyDecoder::decode_hybrid_uint(LittleEndianInputBitStream& stream, u32 context)
{
    // C.3.3 - Hybrid integer decoding

    static constexpr Array<Array<i8, 2>, 120> kSpecialDistances = {
        Array<i8, 2> { 0, 1 }, { 1, 0 }, { 1, 1 }, { -1, 1 }, { 0, 2 }, { 2, 0 }, { 1, 2 }, { -1, 2 }, { 2, 1 }, { -2, 1 }, { 2, 2 },
        { -2, 2 }, { 0, 3 }, { 3, 0 }, { 1, 3 }, { -1, 3 }, { 3, 1 }, { -3, 1 }, { 2, 3 }, { -2, 3 }, { 3, 2 },
        { -3, 2 }, { 0, 4 }, { 4, 0 }, { 1, 4 }, { -1, 4 }, { 4, 1 }, { -4, 1 }, { 3, 3 }, { -3, 3 }, { 2, 4 },
        { -2, 4 }, { 4, 2 }, { -4, 2 }, { 0, 5 }, { 3, 4 }, { -3, 4 }, { 4, 3 }, { -4, 3 }, { 5, 0 }, { 1, 5 },
        { -1, 5 }, { 5, 1 }, { -5, 1 }, { 2, 5 }, { -2, 5 }, { 5, 2 }, { -5, 2 }, { 4, 4 }, { -4, 4 }, { 3, 5 },
        { -3, 5 }, { 5, 3 }, { -5, 3 }, { 0, 6 }, { 6, 0 }, { 1, 6 }, { -1, 6 }, { 6, 1 }, { -6, 1 }, { 2, 6 },
        { -2, 6 }, { 6, 2 }, { -6, 2 }, { 4, 5 }, { -4, 5 }, { 5, 4 }, { -5, 4 }, { 3, 6 }, { -3, 6 }, { 6, 3 },
        { -6, 3 }, { 0, 7 }, { 7, 0 }, { 1, 7 }, { -1, 7 }, { 5, 5 }, { -5, 5 }, { 7, 1 }, { -7, 1 }, { 4, 6 },
        { -4, 6 }, { 6, 4 }, { -6, 4 }, { 2, 7 }, { -2, 7 }, { 7, 2 }, { -7, 2 }, { 3, 7 }, { -3, 7 }, { 7, 3 },
        { -7, 3 }, { 5, 6 }, { -5, 6 }, { 6, 5 }, { -6, 5 }, { 8, 0 }, { 4, 7 }, { -4, 7 }, { 7, 4 }, { -7, 4 },
        { 8, 1 }, { 8, 2 }, { 6, 6 }, { -6, 6 }, { 8, 3 }, { 5, 7 }, { -5, 7 }, { 7, 5 }, { -7, 5 }, { 8, 4 }, { 6, 7 },
        { -6, 7 }, { 7, 6 }, { -7, 6 }, { 8, 5 }, { 7, 7 }, { -7, 7 }, { 8, 6 }, { 8, 7 }
    };

    u32 r {};
    if (m_lz77_num_to_copy > 0) {
        r = m_lz77_window[(m_lz77_copy_pos++) & 0xFFFFF];
        m_lz77_num_to_copy--;
    } else {
        // Read symbol from entropy coded stream using D[clusters[ctx]]
        auto token = TRY(read_symbol(stream, context));

        if (m_lz77.lz77_enabled && token >= m_lz77.min_symbol) {
            m_lz77_num_to_copy = TRY(read_uint(stream, m_lz_len_conf, token - m_lz77.min_symbol)) + m_lz77.min_length;
            // Read symbol using D[clusters[lz_dist_ctx]]
            token = TRY(read_symbol(stream, m_lz_dist_ctx));
            auto distance = TRY(read_uint(stream, m_configs[m_clusters[m_lz_dist_ctx]], token));
            if (m_dist_multiplier == 0) {
                distance++;
            } else if (distance < 120) {
                auto const offset = kSpecialDistances[distance][0];
                distance = offset + m_dist_multiplier * kSpecialDistances[distance][1];
                if (distance < 1)
                    distance = 1;
            } else {
                distance -= 119;
            }
            distance = min(distance, min(m_lz77_num_decoded, 1 << 20));
            m_lz77_copy_pos = m_lz77_num_decoded - distance;
            return decode_hybrid_uint(stream, m_clusters[context]);
        }
        r = TRY(read_uint(stream, m_configs[m_clusters[context]], token));
    }

    if (m_lz77.lz77_enabled)
        m_lz77_window[(m_lz77_num_decoded++) & 0xFFFFF] = r;

    return r;
}

ErrorOr<u32> EntropyDecoder::read_uint(LittleEndianInputBitStream& stream, HybridUint const& config, u32 token)
{
    if (token < config.split)
        return token;

    auto const n = config.split_exponent
        - config.msb_in_token
        - config.lsb_in_token
        + ((token - config.split) >> (config.msb_in_token + config.lsb_in_token));

    VERIFY(n < 32);

    u32 const low_bits = token & ((1 << config.lsb_in_token) - 1);
    token = token >> config.lsb_in_token;
    token &= (1 << config.msb_in_token) - 1;
    token |= (1 << config.msb_in_token);

    auto const result = ((token << n | TRY(stream.read_bits(n))) << config.lsb_in_token) | low_bits;

    VERIFY(result < (1ull << 32));

    return result;
}

ErrorOr<EntropyDecoder::HybridUint> EntropyDecoder::read_config(LittleEndianInputBitStream& stream, u8 log_alphabet_size)
{
    // C.2.3 - Hybrid integer configuration
    HybridUint config {};
    config.split_exponent = TRY(stream.read_bits(AK::ceil_log2(log_alphabet_size + 1)));
    if (config.split_exponent != log_alphabet_size) {
        auto nbits = AK::ceil_log2(config.split_exponent + 1);
        config.msb_in_token = TRY(stream.read_bits(nbits));
        nbits = AK::ceil_log2(config.split_exponent - config.msb_in_token + 1);
        config.lsb_in_token = TRY(stream.read_bits(nbits));
    } else {
        config.msb_in_token = 0;
        config.lsb_in_token = 0;
    }

    config.split = 1 << config.split_exponent;
    return config;
}

ErrorOr<u32> EntropyDecoder::read_symbol(LittleEndianInputBitStream& stream, u32 context)
{
    u32 token {};
    TRY(m_distributions.visit(
        [&](Vector<BrotliCanonicalCode> const& distributions) -> ErrorOr<void> {
            token = TRY(distributions[m_clusters[context]].read_symbol(stream));
            return {};
        },
        [&](Vector<ANSHistogram> const& distributions) -> ErrorOr<void> {
            token = TRY(distributions[m_clusters[context]].read_symbol(stream, m_state));
            return {};
        }));
    return token;
}

ErrorOr<void> EntropyDecoder::read_pre_clustered_distributions(LittleEndianInputBitStream& stream, u32 num_distrib)
{
    // C.2.2  Distribution clustering
    if (num_distrib == 1) {
        // If num_dist == 1, then num_clusters = 1 and clusters[0] = 0, and the remainder of this subclause is skipped.
        m_clusters = { 0 };
        TRY(m_configs.try_resize(1));
        return {};
    };

    TRY(m_clusters.try_resize(num_distrib));

    bool const is_simple = TRY(stream.read_bit());

    u16 num_clusters = 0;

    auto const read_clusters = [&](auto&& reader) -> ErrorOr<void> {
        for (u32 i {}; i < num_distrib; ++i) {
            m_clusters[i] = TRY(reader());
            if (m_clusters[i] >= num_clusters)
                num_clusters = m_clusters[i] + 1;
        }
        return {};
    };

    if (is_simple) {
        u8 const nbits = TRY(stream.read_bits(2));
        TRY(read_clusters([nbits, &stream]() { return stream.read_bits(nbits); }));
    } else {
        auto const use_mtf = TRY(stream.read_bit());
        if (num_distrib == 2)
            TODO();

        auto decoder = TRY(EntropyDecoder::create(stream, 1));

        TRY(read_clusters([&]() { return decoder.decode_hybrid_uint(stream, 0); }));

        if (use_mtf)
            TODO();
    }
    TRY(m_configs.try_resize(num_clusters));
    return {};
}

}
