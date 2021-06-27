/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Decoder.h"
#include "Utilities.h"

namespace Video::VP9 {

Decoder::Decoder()
    : m_parser(make<Parser>(*this))
{
}

bool Decoder::decode_frame(ByteBuffer const& frame_data)
{
    SAFE_CALL(m_parser->parse_frame(frame_data));
    // TODO:
    //  - #2
    //  - #3
    //  - #4
    SAFE_CALL(update_reference_frames());
    return true;
}

void Decoder::dump_frame_info()
{
    m_parser->dump_info();
}

u8 Decoder::merge_prob(u8 pre_prob, u8 count_0, u8 count_1, u8 count_sat, u8 max_update_factor)
{
    auto total_decode_count = count_0 + count_1;
    auto prob = (total_decode_count == 0) ? 128 : clip_3(1, 255, (count_0 * 256 + (total_decode_count >> 1)) / total_decode_count);
    auto count = min(total_decode_count, count_sat);
    auto factor = (max_update_factor * count) / count_sat;
    return round_2(pre_prob * (256 - factor) + (prob * factor), 8);
}

u8 Decoder::merge_probs(const int* tree, int index, u8* probs, u8* counts, u8 count_sat, u8 max_update_factor)
{
    auto s = tree[index];
    auto left_count = (s <= 0) ? counts[-s] : merge_probs(tree, s, probs, counts, count_sat, max_update_factor);
    auto r = tree[index + 1];
    auto right_count = (r <= 0) ? counts[-r] : merge_probs(tree, r, probs, counts, count_sat, max_update_factor);
    probs[index >> 1] = merge_prob(probs[index >> 1], left_count, right_count, count_sat, max_update_factor);
    return left_count + right_count;
}

bool Decoder::adapt_coef_probs()
{
    u8 update_factor;
    if (m_parser->m_frame_is_intra || m_parser->m_last_frame_type != KeyFrame)
        update_factor = 112;
    else
        update_factor = 128;

    for (size_t t = 0; t < 4; t++) {
        for (size_t i = 0; i < 2; i++) {
            for (size_t j = 0; j < 2; j++) {
                for (size_t k = 0; k < 6; k++) {
                    size_t max_l = (k == 0) ? 3 : 6;
                    for (size_t l = 0; l < max_l; l++) {
                        auto& coef_probs = m_parser->m_probability_tables->coef_probs()[t][i][j][k][l];
                        merge_probs(small_token_tree, 2, coef_probs,
                            m_parser->m_syntax_element_counter->m_counts_token[t][i][j][k][l],
                            24, update_factor);
                        merge_probs(binary_tree, 0, coef_probs,
                            m_parser->m_syntax_element_counter->m_counts_more_coefs[t][i][j][k][l],
                            24, update_factor);
                    }
                }
            }
        }
    }

    return true;
}

#define ADAPT_PROB_TABLE(name, size)                                     \
    do {                                                                 \
        for (size_t i = 0; i < (size); i++) {                            \
            auto table = probs.name##_prob();                            \
            table[i] = adapt_prob(table[i], counter.m_counts_##name[i]); \
        }                                                                \
    } while (0)

#define ADAPT_TREE(tree_name, prob_name, count_name, size)                                                 \
    do {                                                                                                   \
        for (size_t i = 0; i < (size); i++) {                                                              \
            adapt_probs(tree_name##_tree, probs.prob_name##_probs()[i], counter.m_counts_##count_name[i]); \
        }                                                                                                  \
    } while (0)

bool Decoder::adapt_non_coef_probs()
{
    auto& probs = *m_parser->m_probability_tables;
    auto& counter = *m_parser->m_syntax_element_counter;
    ADAPT_PROB_TABLE(is_inter, IS_INTER_CONTEXTS);
    ADAPT_PROB_TABLE(comp_mode, COMP_MODE_CONTEXTS);
    ADAPT_PROB_TABLE(comp_ref, REF_CONTEXTS);
    for (size_t i = 0; i < REF_CONTEXTS; i++) {
        for (size_t j = 0; j < 2; j++)
            probs.single_ref_prob()[i][j] = adapt_prob(probs.single_ref_prob()[i][j], counter.m_counts_single_ref[i][j]);
    }
    ADAPT_TREE(inter_mode, inter_mode, inter_mode, INTER_MODE_CONTEXTS);
    ADAPT_TREE(intra_mode, y_mode, intra_mode, INTER_MODE_CONTEXTS);
    ADAPT_TREE(intra_mode, uv_mode, uv_mode, INTER_MODE_CONTEXTS);
    ADAPT_TREE(partition, partition, partition, INTER_MODE_CONTEXTS);
    ADAPT_PROB_TABLE(skip, SKIP_CONTEXTS);
    if (m_parser->m_interpolation_filter == Switchable) {
        ADAPT_TREE(interp_filter, interp_filter, interp_filter, INTERP_FILTER_CONTEXTS);
    }
    if (m_parser->m_tx_mode == TXModeSelect) {
        for (size_t i = 0; i < TX_SIZE_CONTEXTS; i++) {
            auto& tx_probs = probs.tx_probs();
            auto& tx_counts = counter.m_counts_tx_size;
            adapt_probs(tx_size_8_tree, tx_probs[TX_8x8][i], tx_counts[TX_8x8][i]);
            adapt_probs(tx_size_16_tree, tx_probs[TX_16x16][i], tx_counts[TX_16x16][i]);
            adapt_probs(tx_size_32_tree, tx_probs[TX_32x32][i], tx_counts[TX_32x32][i]);
        }
    }
    adapt_probs(mv_joint_tree, probs.mv_joint_probs(), counter.m_counts_mv_joint);
    for (size_t i = 0; i < 2; i++) {
        probs.mv_sign_prob()[i] = adapt_prob(probs.mv_sign_prob()[i], counter.m_counts_mv_sign[i]);
        adapt_probs(mv_class_tree, probs.mv_class_probs()[i], counter.m_counts_mv_class[i]);
        probs.mv_class0_bit_prob()[i] = adapt_prob(probs.mv_class0_bit_prob()[i], counter.m_counts_mv_class0_bit[i]);
        for (size_t j = 0; j < MV_OFFSET_BITS; j++)
            probs.mv_bits_prob()[i][j] = adapt_prob(probs.mv_bits_prob()[i][j], counter.m_counts_mv_bits[i][j]);
        for (size_t j = 0; j < CLASS0_SIZE; j++)
            adapt_probs(mv_fr_tree, probs.mv_class0_fr_probs()[i][j], counter.m_counts_mv_class0_fr[i][j]);
        adapt_probs(mv_fr_tree, probs.mv_fr_probs()[i], counter.m_counts_mv_fr[i]);
        if (m_parser->m_allow_high_precision_mv) {
            probs.mv_class0_hp_prob()[i] = adapt_prob(probs.mv_class0_hp_prob()[i], counter.m_counts_mv_class0_hp[i]);
            probs.mv_hp_prob()[i] = adapt_prob(probs.mv_hp_prob()[i], counter.m_counts_mv_hp[i]);
        }
    }
    return true;
}

void Decoder::adapt_probs(int const* tree, u8* probs, u8* counts)
{
    merge_probs(tree, 0, probs, counts, COUNT_SAT, MAX_UPDATE_FACTOR);
}

u8 Decoder::adapt_prob(u8 prob, u8 counts[2])
{
    return merge_prob(prob, counts[0], counts[1], COUNT_SAT, MAX_UPDATE_FACTOR);
}

bool Decoder::predict_intra(size_t, u32, u32, bool, bool, bool, TXSize, u32)
{
    // TODO: Implement
    return true;
}

bool Decoder::predict_inter(size_t, u32, u32, u32, u32, u32)
{
    // TODO: Implement
    return true;
}

bool Decoder::reconstruct(size_t, u32, u32, TXSize)
{
    // TODO: Implement
    return true;
}

bool Decoder::update_reference_frames()
{
    for (auto i = 0; i < NUM_REF_FRAMES; i++) {
        dbgln("updating frame {}? {}", i, (m_parser->m_refresh_frame_flags & (1 << i)) == 1);
        if ((m_parser->m_refresh_frame_flags & (1 << i)) != 1)
            continue;
        m_parser->m_ref_frame_width[i] = m_parser->m_frame_width;
        m_parser->m_ref_frame_height[i] = m_parser->m_frame_height;
        // TODO: 1.3-1.7
    }
    // TODO: 2.1-2.2
    return true;
}

}
