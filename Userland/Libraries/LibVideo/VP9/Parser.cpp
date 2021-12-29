/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Parser.h"
#include "Decoder.h"
#include "Utilities.h"

namespace Video::VP9 {

#define RESERVED_ZERO                  \
    if (m_bit_stream->read_bit() != 0) \
    return false

Parser::Parser(Decoder& decoder)
    : m_probability_tables(make<ProbabilityTables>())
    , m_tree_parser(make<TreeParser>(*this))
    , m_decoder(decoder)
{
}

Parser::~Parser()
{
    cleanup_tile_allocations();
    free(m_prev_segment_ids);
}

void Parser::cleanup_tile_allocations()
{
    free(m_skips);
    free(m_tx_sizes);
    free(m_mi_sizes);
    free(m_y_modes);
    free(m_segment_ids);
    free(m_ref_frames);
    free(m_interp_filters);
    free(m_mvs);
    free(m_sub_mvs);
    free(m_sub_modes);
}

/* (6.1) */
bool Parser::parse_frame(ByteBuffer const& frame_data)
{
    m_bit_stream = make<BitStream>(frame_data.data(), frame_data.size());
    m_syntax_element_counter = make<SyntaxElementCounter>();

    SAFE_CALL(uncompressed_header());
    dbgln("Finished reading uncompressed header");
    SAFE_CALL(trailing_bits());
    if (m_header_size_in_bytes == 0) {
        dbgln("No header");
        return true;
    }
    m_probability_tables->load_probs(m_frame_context_idx);
    m_probability_tables->load_probs2(m_frame_context_idx);
    m_syntax_element_counter->clear_counts();

    SAFE_CALL(m_bit_stream->init_bool(m_header_size_in_bytes));
    dbgln("Reading compressed header");
    SAFE_CALL(compressed_header());
    dbgln("Finished reading compressed header");
    SAFE_CALL(m_bit_stream->exit_bool());

    SAFE_CALL(decode_tiles());
    SAFE_CALL(refresh_probs());

    dbgln("Finished reading frame!");
    return true;
}

bool Parser::trailing_bits()
{
    while (m_bit_stream->get_position() & 7u)
        RESERVED_ZERO;
    return true;
}

bool Parser::refresh_probs()
{
    if (!m_error_resilient_mode && !m_frame_parallel_decoding_mode) {
        m_probability_tables->load_probs(m_frame_context_idx);
        SAFE_CALL(m_decoder.adapt_coef_probs());
        if (!m_frame_is_intra) {
            m_probability_tables->load_probs2(m_frame_context_idx);
            SAFE_CALL(m_decoder.adapt_non_coef_probs());
        }
    }
    if (m_refresh_frame_context)
        m_probability_tables->save_probs(m_frame_context_idx);
    return true;
}

/* (6.2) */
bool Parser::uncompressed_header()
{
    auto frame_marker = m_bit_stream->read_f(2);
    if (frame_marker != 2)
        return false;
    auto profile_low_bit = m_bit_stream->read_bit();
    auto profile_high_bit = m_bit_stream->read_bit();
    m_profile = (profile_high_bit << 1u) + profile_low_bit;
    if (m_profile == 3)
        RESERVED_ZERO;
    auto show_existing_frame = m_bit_stream->read_bit();
    if (show_existing_frame) {
        m_frame_to_show_map_index = m_bit_stream->read_f(3);
        m_header_size_in_bytes = 0;
        m_refresh_frame_flags = 0;
        m_loop_filter_level = 0;
        return true;
    }

    m_last_frame_type = m_frame_type;
    m_frame_type = read_frame_type();
    m_show_frame = m_bit_stream->read_bit();
    m_error_resilient_mode = m_bit_stream->read_bit();

    if (m_frame_type == KeyFrame) {
        SAFE_CALL(frame_sync_code());
        SAFE_CALL(color_config());
        SAFE_CALL(frame_size());
        SAFE_CALL(render_size());
        m_refresh_frame_flags = 0xFF;
        m_frame_is_intra = true;
    } else {
        m_frame_is_intra = !m_show_frame && m_bit_stream->read_bit();

        if (!m_error_resilient_mode) {
            m_reset_frame_context = m_bit_stream->read_f(2);
        } else {
            m_reset_frame_context = 0;
        }

        if (m_frame_is_intra) {
            SAFE_CALL(frame_sync_code());
            if (m_profile > 0) {
                SAFE_CALL(color_config());
            } else {
                m_color_space = Bt601;
                m_subsampling_x = true;
                m_subsampling_y = true;
                m_bit_depth = 8;
            }

            m_refresh_frame_flags = m_bit_stream->read_f8();
            SAFE_CALL(frame_size());
            SAFE_CALL(render_size());
        } else {
            m_refresh_frame_flags = m_bit_stream->read_f8();
            for (auto i = 0; i < 3; i++) {
                m_ref_frame_idx[i] = m_bit_stream->read_f(3);
                m_ref_frame_sign_bias[LastFrame + i] = m_bit_stream->read_bit();
            }
            SAFE_CALL(frame_size_with_refs());
            m_allow_high_precision_mv = m_bit_stream->read_bit();
            SAFE_CALL(read_interpolation_filter());
        }
    }

    if (!m_error_resilient_mode) {
        m_refresh_frame_context = m_bit_stream->read_bit();
        m_frame_parallel_decoding_mode = m_bit_stream->read_bit();
    } else {
        m_refresh_frame_context = false;
        m_frame_parallel_decoding_mode = true;
    }

    m_frame_context_idx = m_bit_stream->read_f(2);
    if (m_frame_is_intra || m_error_resilient_mode) {
        SAFE_CALL(setup_past_independence());
        if (m_frame_type == KeyFrame || m_error_resilient_mode || m_reset_frame_context == 3) {
            for (auto i = 0; i < 4; i++) {
                m_probability_tables->save_probs(i);
            }
        } else if (m_reset_frame_context == 2) {
            m_probability_tables->save_probs(m_frame_context_idx);
        }
        m_frame_context_idx = 0;
    }

    SAFE_CALL(loop_filter_params());
    SAFE_CALL(quantization_params());
    SAFE_CALL(segmentation_params());
    SAFE_CALL(tile_info());

    m_header_size_in_bytes = m_bit_stream->read_f16();

    return true;
}

bool Parser::frame_sync_code()
{
    if (m_bit_stream->read_byte() != 0x49)
        return false;
    if (m_bit_stream->read_byte() != 0x83)
        return false;
    return m_bit_stream->read_byte() == 0x42;
}

bool Parser::color_config()
{
    if (m_profile >= 2) {
        m_bit_depth = m_bit_stream->read_bit() ? 12 : 10;
    } else {
        m_bit_depth = 8;
    }

    auto color_space = m_bit_stream->read_f(3);
    if (color_space > RGB)
        return false;
    m_color_space = static_cast<ColorSpace>(color_space);

    if (color_space != RGB) {
        m_color_range = read_color_range();
        if (m_profile == 1 || m_profile == 3) {
            m_subsampling_x = m_bit_stream->read_bit();
            m_subsampling_y = m_bit_stream->read_bit();
            RESERVED_ZERO;
        } else {
            m_subsampling_x = true;
            m_subsampling_y = true;
        }
    } else {
        m_color_range = FullSwing;
        if (m_profile == 1 || m_profile == 3) {
            m_subsampling_x = false;
            m_subsampling_y = false;
            RESERVED_ZERO;
        }
    }
    return true;
}

bool Parser::frame_size()
{
    m_frame_width = m_bit_stream->read_f16() + 1;
    m_frame_height = m_bit_stream->read_f16() + 1;
    SAFE_CALL(compute_image_size());
    return true;
}

bool Parser::render_size()
{
    if (m_bit_stream->read_bit()) {
        m_render_width = m_bit_stream->read_f16() + 1;
        m_render_height = m_bit_stream->read_f16() + 1;
    } else {
        m_render_width = m_frame_width;
        m_render_height = m_frame_height;
    }
    return true;
}

bool Parser::frame_size_with_refs()
{
    bool found_ref;
    for (auto frame_index : m_ref_frame_idx) {
        found_ref = m_bit_stream->read_bit();
        if (found_ref) {
            dbgln("Reading size from ref frame {}", frame_index);
            m_frame_width = m_ref_frame_width[frame_index];
            m_frame_height = m_ref_frame_height[frame_index];
            break;
        }
    }

    if (!found_ref) {
        SAFE_CALL(frame_size());
    } else {
        SAFE_CALL(compute_image_size());
    }

    SAFE_CALL(render_size());
    return true;
}

bool Parser::compute_image_size()
{
    m_mi_cols = (m_frame_width + 7u) >> 3u;
    m_mi_rows = (m_frame_height + 7u) >> 3u;
    m_sb64_cols = (m_mi_cols + 7u) >> 3u;
    m_sb64_rows = (m_mi_rows + 7u) >> 3u;
    return true;
}

bool Parser::read_interpolation_filter()
{
    if (m_bit_stream->read_bit()) {
        m_interpolation_filter = Switchable;
    } else {
        m_interpolation_filter = literal_to_type[m_bit_stream->read_f(2)];
    }
    return true;
}

bool Parser::loop_filter_params()
{
    m_loop_filter_level = m_bit_stream->read_f(6);
    m_loop_filter_sharpness = m_bit_stream->read_f(3);
    m_loop_filter_delta_enabled = m_bit_stream->read_bit();
    if (m_loop_filter_delta_enabled) {
        if (m_bit_stream->read_bit()) {
            for (auto& loop_filter_ref_delta : m_loop_filter_ref_deltas) {
                if (m_bit_stream->read_bit())
                    loop_filter_ref_delta = m_bit_stream->read_s(6);
            }
            for (auto& loop_filter_mode_delta : m_loop_filter_mode_deltas) {
                if (m_bit_stream->read_bit())
                    loop_filter_mode_delta = m_bit_stream->read_s(6);
            }
        }
    }
    return true;
}

bool Parser::quantization_params()
{
    auto base_q_idx = m_bit_stream->read_byte();
    auto delta_q_y_dc = read_delta_q();
    auto delta_q_uv_dc = read_delta_q();
    auto delta_q_uv_ac = read_delta_q();
    m_lossless = base_q_idx == 0 && delta_q_y_dc == 0 && delta_q_uv_dc == 0 && delta_q_uv_ac == 0;
    return true;
}

i8 Parser::read_delta_q()
{
    if (m_bit_stream->read_bit())
        return m_bit_stream->read_s(4);
    return 0;
}

bool Parser::segmentation_params()
{
    m_segmentation_enabled = m_bit_stream->read_bit();
    if (!m_segmentation_enabled)
        return true;

    m_segmentation_update_map = m_bit_stream->read_bit();
    if (m_segmentation_update_map) {
        for (auto& segmentation_tree_prob : m_segmentation_tree_probs)
            segmentation_tree_prob = read_prob();
        m_segmentation_temporal_update = m_bit_stream->read_bit();
        for (auto& segmentation_pred_prob : m_segmentation_pred_prob)
            segmentation_pred_prob = m_segmentation_temporal_update ? read_prob() : 255;
    }

    SAFE_CALL(m_bit_stream->read_bit());

    m_segmentation_abs_or_delta_update = m_bit_stream->read_bit();
    for (auto i = 0; i < MAX_SEGMENTS; i++) {
        for (auto j = 0; j < SEG_LVL_MAX; j++) {
            auto feature_value = 0;
            auto feature_enabled = m_bit_stream->read_bit();
            m_feature_enabled[i][j] = feature_enabled;
            if (feature_enabled) {
                auto bits_to_read = segmentation_feature_bits[j];
                feature_value = m_bit_stream->read_f(bits_to_read);
                if (segmentation_feature_signed[j]) {
                    if (m_bit_stream->read_bit())
                        feature_value = -feature_value;
                }
            }
            m_feature_data[i][j] = feature_value;
        }
    }
    return true;
}

u8 Parser::read_prob()
{
    if (m_bit_stream->read_bit())
        return m_bit_stream->read_byte();
    return 255;
}

bool Parser::tile_info()
{
    auto min_log2_tile_cols = calc_min_log2_tile_cols();
    auto max_log2_tile_cols = calc_max_log2_tile_cols();
    m_tile_cols_log2 = min_log2_tile_cols;
    while (m_tile_cols_log2 < max_log2_tile_cols) {
        if (m_bit_stream->read_bit())
            m_tile_cols_log2++;
        else
            break;
    }
    m_tile_rows_log2 = m_bit_stream->read_bit();
    if (m_tile_rows_log2) {
        m_tile_rows_log2 += m_bit_stream->read_bit();
    }
    return true;
}

u16 Parser::calc_min_log2_tile_cols()
{
    auto min_log_2 = 0u;
    while ((u32)(MAX_TILE_WIDTH_B64 << min_log_2) < m_sb64_cols)
        min_log_2++;
    return min_log_2;
}

u16 Parser::calc_max_log2_tile_cols()
{
    u16 max_log_2 = 1;
    while ((m_sb64_cols >> max_log_2) >= MIN_TILE_WIDTH_B64)
        max_log_2++;
    return max_log_2 - 1;
}

bool Parser::setup_past_independence()
{
    for (auto i = 0; i < 8; i++) {
        for (auto j = 0; j < 4; j++) {
            m_feature_data[i][j] = 0;
            m_feature_enabled[i][j] = false;
        }
    }
    m_segmentation_abs_or_delta_update = false;
    if (m_prev_segment_ids)
        free(m_prev_segment_ids);
    m_prev_segment_ids = static_cast<u8*>(kmalloc_array(m_mi_rows, m_mi_cols));
    m_loop_filter_delta_enabled = true;
    m_loop_filter_ref_deltas[IntraFrame] = 1;
    m_loop_filter_ref_deltas[LastFrame] = 0;
    m_loop_filter_ref_deltas[GoldenFrame] = -1;
    m_loop_filter_ref_deltas[AltRefFrame] = -1;
    for (auto& loop_filter_mode_delta : m_loop_filter_mode_deltas)
        loop_filter_mode_delta = 0;
    m_probability_tables->reset_probs();
    return true;
}

bool Parser::compressed_header()
{
    SAFE_CALL(read_tx_mode());
    if (m_tx_mode == TXModeSelect)
        SAFE_CALL(tx_mode_probs());
    SAFE_CALL(read_coef_probs());
    SAFE_CALL(read_skip_prob());
    if (!m_frame_is_intra) {
        SAFE_CALL(read_inter_mode_probs());
        if (m_interpolation_filter == Switchable)
            SAFE_CALL(read_interp_filter_probs());
        SAFE_CALL(read_is_inter_probs());
        SAFE_CALL(frame_reference_mode());
        SAFE_CALL(frame_reference_mode_probs());
        SAFE_CALL(read_y_mode_probs());
        SAFE_CALL(read_partition_probs());
        SAFE_CALL(mv_probs());
    }
    return true;
}

bool Parser::read_tx_mode()
{
    if (m_lossless) {
        m_tx_mode = Only_4x4;
    } else {
        auto tx_mode = m_bit_stream->read_literal(2);
        if (tx_mode == Allow_32x32)
            tx_mode += m_bit_stream->read_literal(1);
        m_tx_mode = static_cast<TXMode>(tx_mode);
    }
    return true;
}

bool Parser::tx_mode_probs()
{
    auto& tx_probs = m_probability_tables->tx_probs();
    for (auto i = 0; i < TX_SIZE_CONTEXTS; i++) {
        for (auto j = 0; j < TX_SIZES - 3; j++)
            tx_probs[TX_8x8][i][j] = diff_update_prob(tx_probs[TX_8x8][i][j]);
    }
    for (auto i = 0; i < TX_SIZE_CONTEXTS; i++) {
        for (auto j = 0; j < TX_SIZES - 2; j++)
            tx_probs[TX_16x16][i][j] = diff_update_prob(tx_probs[TX_16x16][i][j]);
    }
    for (auto i = 0; i < TX_SIZE_CONTEXTS; i++) {
        for (auto j = 0; j < TX_SIZES - 1; j++)
            tx_probs[TX_32x32][i][j] = diff_update_prob(tx_probs[TX_32x32][i][j]);
    }
    return true;
}

u8 Parser::diff_update_prob(u8 prob)
{
    if (m_bit_stream->read_bool(252)) {
        auto delta_prob = decode_term_subexp();
        prob = inv_remap_prob(delta_prob, prob);
    }
    return prob;
}

u8 Parser::decode_term_subexp()
{
    if (m_bit_stream->read_literal(1) == 0)
        return m_bit_stream->read_literal(4);
    if (m_bit_stream->read_literal(1) == 0)
        return m_bit_stream->read_literal(4) + 16;
    if (m_bit_stream->read_literal(1) == 0)
        return m_bit_stream->read_literal(4) + 32;

    auto v = m_bit_stream->read_literal(7);
    if (v < 65)
        return v + 64;
    return (v << 1u) - 1 + m_bit_stream->read_literal(1);
}

u8 Parser::inv_remap_prob(u8 delta_prob, u8 prob)
{
    u8 m = prob - 1;
    auto v = inv_map_table[delta_prob];
    if ((m << 1u) <= 255)
        return 1 + inv_recenter_nonneg(v, m);
    return 255 - inv_recenter_nonneg(v, 254 - m);
}

u8 Parser::inv_recenter_nonneg(u8 v, u8 m)
{
    if (v > 2 * m)
        return v;
    if (v & 1u)
        return m - ((v + 1u) >> 1u);
    return m + (v >> 1u);
}

bool Parser::read_coef_probs()
{
    m_max_tx_size = tx_mode_to_biggest_tx_size[m_tx_mode];
    for (auto tx_size = TX_4x4; tx_size <= m_max_tx_size; tx_size = static_cast<TXSize>(static_cast<int>(tx_size) + 1)) {
        auto update_probs = m_bit_stream->read_literal(1);
        if (update_probs == 1) {
            for (auto i = 0; i < 2; i++) {
                for (auto j = 0; j < 2; j++) {
                    for (auto k = 0; k < 6; k++) {
                        auto max_l = (k == 0) ? 3 : 6;
                        for (auto l = 0; l < max_l; l++) {
                            for (auto m = 0; m < 3; m++) {
                                auto& coef_probs = m_probability_tables->coef_probs()[tx_size];
                                coef_probs[i][j][k][l][m] = diff_update_prob(coef_probs[i][j][k][l][m]);
                            }
                        }
                    }
                }
            }
        }
    }
    return true;
}

bool Parser::read_skip_prob()
{
    for (auto i = 0; i < SKIP_CONTEXTS; i++)
        m_probability_tables->skip_prob()[i] = diff_update_prob(m_probability_tables->skip_prob()[i]);
    return true;
}

bool Parser::read_inter_mode_probs()
{
    for (auto i = 0; i < INTER_MODE_CONTEXTS; i++) {
        for (auto j = 0; j < INTER_MODES - 1; j++)
            m_probability_tables->inter_mode_probs()[i][j] = diff_update_prob(m_probability_tables->inter_mode_probs()[i][j]);
    }
    return true;
}

bool Parser::read_interp_filter_probs()
{
    for (auto i = 0; i < INTERP_FILTER_CONTEXTS; i++) {
        for (auto j = 0; j < SWITCHABLE_FILTERS - 1; j++)
            m_probability_tables->interp_filter_probs()[i][j] = diff_update_prob(m_probability_tables->interp_filter_probs()[i][j]);
    }
    return true;
}

bool Parser::read_is_inter_probs()
{
    for (auto i = 0; i < IS_INTER_CONTEXTS; i++)
        m_probability_tables->is_inter_prob()[i] = diff_update_prob(m_probability_tables->is_inter_prob()[i]);
    return true;
}

bool Parser::frame_reference_mode()
{
    auto compound_reference_allowed = false;
    for (size_t i = 2; i <= REFS_PER_FRAME; i++) {
        if (m_ref_frame_sign_bias[i] != m_ref_frame_sign_bias[1])
            compound_reference_allowed = true;
    }
    if (compound_reference_allowed) {
        auto non_single_reference = m_bit_stream->read_literal(1);
        if (non_single_reference == 0) {
            m_reference_mode = SingleReference;
        } else {
            auto reference_select = m_bit_stream->read_literal(1);
            if (reference_select == 0)
                m_reference_mode = CompoundReference;
            else
                m_reference_mode = ReferenceModeSelect;
            SAFE_CALL(setup_compound_reference_mode());
        }
    } else {
        m_reference_mode = SingleReference;
    }
    return true;
}

bool Parser::frame_reference_mode_probs()
{
    if (m_reference_mode == ReferenceModeSelect) {
        for (auto i = 0; i < COMP_MODE_CONTEXTS; i++) {
            auto& comp_mode_prob = m_probability_tables->comp_mode_prob();
            comp_mode_prob[i] = diff_update_prob(comp_mode_prob[i]);
        }
    }
    if (m_reference_mode != CompoundReference) {
        for (auto i = 0; i < REF_CONTEXTS; i++) {
            auto& single_ref_prob = m_probability_tables->single_ref_prob();
            single_ref_prob[i][0] = diff_update_prob(single_ref_prob[i][0]);
            single_ref_prob[i][1] = diff_update_prob(single_ref_prob[i][1]);
        }
    }
    if (m_reference_mode != SingleReference) {
        for (auto i = 0; i < REF_CONTEXTS; i++) {
            auto& comp_ref_prob = m_probability_tables->comp_ref_prob();
            comp_ref_prob[i] = diff_update_prob(comp_ref_prob[i]);
        }
    }
    return true;
}

bool Parser::read_y_mode_probs()
{
    for (auto i = 0; i < BLOCK_SIZE_GROUPS; i++) {
        for (auto j = 0; j < INTRA_MODES - 1; j++) {
            auto& y_mode_probs = m_probability_tables->y_mode_probs();
            y_mode_probs[i][j] = diff_update_prob(y_mode_probs[i][j]);
        }
    }
    return true;
}

bool Parser::read_partition_probs()
{
    for (auto i = 0; i < PARTITION_CONTEXTS; i++) {
        for (auto j = 0; j < PARTITION_TYPES - 1; j++) {
            auto& partition_probs = m_probability_tables->partition_probs();
            partition_probs[i][j] = diff_update_prob(partition_probs[i][j]);
        }
    }
    return true;
}

bool Parser::mv_probs()
{
    for (auto j = 0; j < MV_JOINTS - 1; j++) {
        auto& mv_joint_probs = m_probability_tables->mv_joint_probs();
        mv_joint_probs[j] = update_mv_prob(mv_joint_probs[j]);
    }

    for (auto i = 0; i < 2; i++) {
        auto& mv_sign_prob = m_probability_tables->mv_sign_prob();
        mv_sign_prob[i] = update_mv_prob(mv_sign_prob[i]);
        for (auto j = 0; j < MV_CLASSES - 1; j++) {
            auto& mv_class_probs = m_probability_tables->mv_class_probs();
            mv_class_probs[i][j] = update_mv_prob(mv_class_probs[i][j]);
        }
        auto& mv_class0_bit_prob = m_probability_tables->mv_class0_bit_prob();
        mv_class0_bit_prob[i] = update_mv_prob(mv_class0_bit_prob[i]);
        for (auto j = 0; j < MV_OFFSET_BITS; j++) {
            auto& mv_bits_prob = m_probability_tables->mv_bits_prob();
            mv_bits_prob[i][j] = update_mv_prob(mv_bits_prob[i][j]);
        }
    }

    for (auto i = 0; i < 2; i++) {
        for (auto j = 0; j < CLASS0_SIZE; j++) {
            for (auto k = 0; k < MV_FR_SIZE - 1; k++) {
                auto& mv_class0_fr_probs = m_probability_tables->mv_class0_fr_probs();
                mv_class0_fr_probs[i][j][k] = update_mv_prob(mv_class0_fr_probs[i][j][k]);
            }
        }
        for (auto k = 0; k < MV_FR_SIZE - 1; k++) {
            auto& mv_fr_probs = m_probability_tables->mv_fr_probs();
            mv_fr_probs[i][k] = update_mv_prob(mv_fr_probs[i][k]);
        }
    }

    if (m_allow_high_precision_mv) {
        for (auto i = 0; i < 2; i++) {
            auto& mv_class0_hp_prob = m_probability_tables->mv_class0_hp_prob();
            auto& mv_hp_prob = m_probability_tables->mv_hp_prob();
            mv_class0_hp_prob[i] = update_mv_prob(mv_class0_hp_prob[i]);
            mv_hp_prob[i] = update_mv_prob(mv_hp_prob[i]);
        }
    }

    return true;
}

u8 Parser::update_mv_prob(u8 prob)
{
    if (m_bit_stream->read_bool(252)) {
        return (m_bit_stream->read_literal(7) << 1u) | 1u;
    }
    return prob;
}

bool Parser::setup_compound_reference_mode()
{
    if (m_ref_frame_sign_bias[LastFrame] == m_ref_frame_sign_bias[GoldenFrame]) {
        m_comp_fixed_ref = AltRefFrame;
        m_comp_var_ref[0] = LastFrame;
        m_comp_var_ref[1] = GoldenFrame;
    } else if (m_ref_frame_sign_bias[LastFrame] == m_ref_frame_sign_bias[AltRefFrame]) {
        m_comp_fixed_ref = GoldenFrame;
        m_comp_var_ref[0] = LastFrame;
        m_comp_var_ref[1] = AltRefFrame;
    } else {
        m_comp_fixed_ref = LastFrame;
        m_comp_var_ref[0] = GoldenFrame;
        m_comp_var_ref[1] = AltRefFrame;
    }
    return true;
}

void Parser::allocate_tile_data()
{
    auto dimensions = m_mi_rows * m_mi_cols;
    if (dimensions == m_allocated_dimensions)
        return;
    cleanup_tile_allocations();
    m_skips = static_cast<bool*>(kmalloc_array(dimensions, sizeof(bool)));
    m_tx_sizes = static_cast<TXSize*>(kmalloc_array(dimensions, sizeof(TXSize)));
    m_mi_sizes = static_cast<u32*>(kmalloc_array(dimensions, sizeof(u32)));
    m_y_modes = static_cast<u8*>(kmalloc_array(dimensions, sizeof(u8)));
    m_segment_ids = static_cast<u8*>(kmalloc_array(dimensions, sizeof(u8)));
    m_ref_frames = static_cast<ReferenceFrame*>(kmalloc_array(dimensions, 2, sizeof(ReferenceFrame)));
    m_interp_filters = static_cast<InterpolationFilter*>(kmalloc_array(dimensions, sizeof(InterpolationFilter)));
    m_mvs = static_cast<MV*>(kmalloc_array(dimensions, 2, sizeof(MV)));
    m_sub_mvs = static_cast<MV*>(kmalloc_array(dimensions, 8, sizeof(MV)));
    m_sub_modes = static_cast<IntraMode*>(kmalloc_array(dimensions, 4, sizeof(IntraMode)));
    m_allocated_dimensions = dimensions;
}

bool Parser::decode_tiles()
{
    auto tile_cols = 1 << m_tile_cols_log2;
    auto tile_rows = 1 << m_tile_rows_log2;
    allocate_tile_data();
    SAFE_CALL(clear_above_context());
    for (auto tile_row = 0; tile_row < tile_rows; tile_row++) {
        for (auto tile_col = 0; tile_col < tile_cols; tile_col++) {
            auto last_tile = (tile_row == tile_rows - 1) && (tile_col == tile_cols - 1);
            auto tile_size = last_tile ? m_bit_stream->bytes_remaining() : m_bit_stream->read_f(32);
            m_mi_row_start = get_tile_offset(tile_row, m_mi_rows, m_tile_rows_log2);
            m_mi_row_end = get_tile_offset(tile_row + 1, m_mi_rows, m_tile_rows_log2);
            m_mi_col_start = get_tile_offset(tile_col, m_mi_cols, m_tile_cols_log2);
            m_mi_col_end = get_tile_offset(tile_col + 1, m_mi_cols, m_tile_cols_log2);
            SAFE_CALL(m_bit_stream->init_bool(tile_size));
            SAFE_CALL(decode_tile());
            SAFE_CALL(m_bit_stream->exit_bool());
        }
    }
    return true;
}

void Parser::clear_context(Vector<u8>& context, size_t size)
{
    context.resize_and_keep_capacity(size);
    __builtin_memset(context.data(), 0, sizeof(u8) * size);
}

void Parser::clear_context(Vector<Vector<u8>>& context, size_t outer_size, size_t inner_size)
{
    if (context.size() < outer_size)
        context.resize(outer_size);
    for (auto& sub_vector : context)
        clear_context(sub_vector, inner_size);
}

bool Parser::clear_above_context()
{
    clear_context(m_above_nonzero_context, 3, 2 * m_mi_cols);
    clear_context(m_above_seg_pred_context, m_mi_cols);
    clear_context(m_above_partition_context, m_sb64_cols * 8);
    return true;
}

u32 Parser::get_tile_offset(u32 tile_num, u32 mis, u32 tile_size_log2)
{
    u32 super_blocks = (mis + 7) >> 3u;
    u32 offset = ((tile_num * super_blocks) >> tile_size_log2) << 3u;
    return min(offset, mis);
}

bool Parser::decode_tile()
{
    for (auto row = m_mi_row_start; row < m_mi_row_end; row += 8) {
        SAFE_CALL(clear_left_context());
        m_row = row;
        for (auto col = m_mi_col_start; col < m_mi_col_end; col += 8) {
            m_col = col;
            SAFE_CALL(decode_partition(row, col, Block_64x64));
        }
    }
    return true;
}

bool Parser::clear_left_context()
{
    clear_context(m_left_nonzero_context, 3, 2 * m_mi_rows);
    clear_context(m_left_seg_pred_context, m_mi_rows);
    clear_context(m_left_partition_context, m_sb64_rows * 8);
    return true;
}

bool Parser::decode_partition(u32 row, u32 col, u8 block_subsize)
{
    if (row >= m_mi_rows || col >= m_mi_cols)
        return false;
    m_block_subsize = block_subsize;
    m_num_8x8 = num_8x8_blocks_wide_lookup[block_subsize];
    auto half_block_8x8 = m_num_8x8 >> 1;
    m_has_rows = (row + half_block_8x8) < m_mi_rows;
    m_has_cols = (col + half_block_8x8) < m_mi_cols;

    auto partition = m_tree_parser->parse_tree(SyntaxElementType::Partition);
    auto subsize = subsize_lookup[partition][block_subsize];
    if (subsize < Block_8x8 || partition == PartitionNone) {
        SAFE_CALL(decode_block(row, col, subsize));
    } else if (partition == PartitionHorizontal) {
        SAFE_CALL(decode_block(row, col, subsize));
        if (m_has_rows)
            SAFE_CALL(decode_block(row + half_block_8x8, col, subsize));
    } else if (partition == PartitionVertical) {
        SAFE_CALL(decode_block(row, col, subsize));
        if (m_has_cols)
            SAFE_CALL(decode_block(row, col + half_block_8x8, subsize));
    } else {
        SAFE_CALL(decode_partition(row, col, subsize));
        SAFE_CALL(decode_partition(row, col + half_block_8x8, subsize));
        SAFE_CALL(decode_partition(row + half_block_8x8, col, subsize));
        SAFE_CALL(decode_partition(row + half_block_8x8, col + half_block_8x8, subsize));
    }
    if (block_subsize == Block_8x8 || partition != PartitionSplit) {
        for (size_t i = 0; i < m_num_8x8; i++) {
            m_above_partition_context[col + i] = 15 >> b_width_log2_lookup[subsize];
            m_left_partition_context[row + i] = 15 >> b_width_log2_lookup[subsize];
        }
    }
    return true;
}

bool Parser::decode_block(u32 row, u32 col, u8 subsize)
{
    m_mi_row = row;
    m_mi_col = col;
    m_mi_size = subsize;
    m_available_u = row > 0;
    m_available_l = col > m_mi_col_start;
    SAFE_CALL(mode_info());
    m_eob_total = 0;
    SAFE_CALL(residual());
    if (m_is_inter && subsize >= Block_8x8 && m_eob_total == 0)
        m_skip = true;
    for (size_t y = 0; y < num_8x8_blocks_high_lookup[subsize]; y++) {
        for (size_t x = 0; x < num_8x8_blocks_wide_lookup[subsize]; x++) {
            auto pos = (row + y) * m_mi_cols + (col + x);
            m_skips[pos] = m_skip;
            m_tx_sizes[pos] = m_tx_size;
            m_mi_sizes[pos] = m_mi_size;
            m_y_modes[pos] = m_y_mode;
            m_segment_ids[pos] = m_segment_id;
            for (size_t ref_list = 0; ref_list < 2; ref_list++)
                m_ref_frames[(pos * 2) + ref_list] = m_ref_frame[ref_list];
            if (m_is_inter) {
                m_interp_filters[pos] = m_interp_filter;
                for (size_t ref_list = 0; ref_list < 2; ref_list++) {
                    auto pos_with_ref_list = (pos * 2 + ref_list) * sizeof(MV);
                    m_mvs[pos_with_ref_list] = m_block_mvs[ref_list][3];
                    for (size_t b = 0; b < 4; b++)
                        m_sub_mvs[pos_with_ref_list * 4 + b * sizeof(MV)] = m_block_mvs[ref_list][b];
                }
            } else {
                for (size_t b = 0; b < 4; b++)
                    m_sub_modes[pos * 4 + b] = static_cast<IntraMode>(m_block_sub_modes[b]);
            }
        }
    }
    return true;
}

bool Parser::mode_info()
{
    if (m_frame_is_intra)
        return intra_frame_mode_info();
    return inter_frame_mode_info();
}

bool Parser::intra_frame_mode_info()
{
    SAFE_CALL(intra_segment_id());
    SAFE_CALL(read_skip());
    SAFE_CALL(read_tx_size(true));
    m_ref_frame[0] = IntraFrame;
    m_ref_frame[1] = None;
    m_is_inter = false;
    if (m_mi_size >= Block_8x8) {
        m_default_intra_mode = m_tree_parser->parse_tree<IntraMode>(SyntaxElementType::DefaultIntraMode);
        m_y_mode = m_default_intra_mode;
        for (auto& block_sub_mode : m_block_sub_modes)
            block_sub_mode = m_y_mode;
    } else {
        m_num_4x4_w = num_4x4_blocks_wide_lookup[m_mi_size];
        m_num_4x4_h = num_4x4_blocks_high_lookup[m_mi_size];
        for (auto idy = 0; idy < 2; idy += m_num_4x4_h) {
            for (auto idx = 0; idx < 2; idx += m_num_4x4_w) {
                m_tree_parser->set_default_intra_mode_variables(idx, idy);
                m_default_intra_mode = m_tree_parser->parse_tree<IntraMode>(SyntaxElementType::DefaultIntraMode);
                for (auto y = 0; y < m_num_4x4_h; y++) {
                    for (auto x = 0; x < m_num_4x4_w; x++) {
                        auto index = (idy + y) * 2 + idx + x;
                        if (index > 3)
                            dbgln("Trying to access index {} on m_sub_modes", index);
                        m_block_sub_modes[index] = m_default_intra_mode;
                    }
                }
            }
        }
        m_y_mode = m_default_intra_mode;
    }
    m_uv_mode = m_tree_parser->parse_tree<u8>(SyntaxElementType::DefaultUVMode);
    return true;
}

bool Parser::intra_segment_id()
{
    if (m_segmentation_enabled && m_segmentation_update_map)
        m_segment_id = m_tree_parser->parse_tree<u8>(SyntaxElementType::SegmentID);
    else
        m_segment_id = 0;
    return true;
}

bool Parser::read_skip()
{
    if (seg_feature_active(SEG_LVL_SKIP))
        m_skip = true;
    else
        m_skip = m_tree_parser->parse_tree<bool>(SyntaxElementType::Skip);
    return true;
}

bool Parser::seg_feature_active(u8 feature)
{
    return m_segmentation_enabled && m_feature_enabled[m_segment_id][feature];
}

bool Parser::read_tx_size(bool allow_select)
{
    m_max_tx_size = max_txsize_lookup[m_mi_size];
    if (allow_select && m_tx_mode == TXModeSelect && m_mi_size >= Block_8x8)
        m_tx_size = m_tree_parser->parse_tree<TXSize>(SyntaxElementType::TXSize);
    else
        m_tx_size = min(m_max_tx_size, tx_mode_to_biggest_tx_size[m_tx_mode]);
    return true;
}

bool Parser::inter_frame_mode_info()
{
    m_left_ref_frame[0] = m_available_l ? m_ref_frames[m_mi_row * m_mi_cols + (m_mi_col - 1)] : IntraFrame;
    m_above_ref_frame[0] = m_available_u ? m_ref_frames[(m_mi_row - 1) * m_mi_cols + m_mi_col] : IntraFrame;
    m_left_ref_frame[1] = m_available_l ? m_ref_frames[m_mi_row * m_mi_cols + (m_mi_col - 1) + 1] : None;
    m_above_ref_frame[1] = m_available_u ? m_ref_frames[(m_mi_row - 1) * m_mi_cols + m_mi_col + 1] : None;
    m_left_intra = m_left_ref_frame[0] <= IntraFrame;
    m_above_intra = m_above_ref_frame[0] <= IntraFrame;
    m_left_single = m_left_ref_frame[1] <= None;
    m_above_single = m_above_ref_frame[1] <= None;
    SAFE_CALL(inter_segment_id());
    SAFE_CALL(read_skip());
    SAFE_CALL(read_is_inter());
    SAFE_CALL(read_tx_size(!m_skip || !m_is_inter));
    if (m_is_inter) {
        SAFE_CALL(inter_block_mode_info());
    } else {
        SAFE_CALL(intra_block_mode_info());
    }
    return true;
}

bool Parser::inter_segment_id()
{
    if (!m_segmentation_enabled) {
        m_segment_id = 0;
        return true;
    }
    auto predicted_segment_id = get_segment_id();
    if (!m_segmentation_update_map) {
        m_segment_id = predicted_segment_id;
        return true;
    }
    if (!m_segmentation_temporal_update) {
        m_segment_id = m_tree_parser->parse_tree<u8>(SyntaxElementType::SegmentID);
        return true;
    }

    auto seg_id_predicted = m_tree_parser->parse_tree<bool>(SyntaxElementType::SegIDPredicted);
    if (seg_id_predicted)
        m_segment_id = predicted_segment_id;
    else
        m_segment_id = m_tree_parser->parse_tree<u8>(SyntaxElementType::SegmentID);
    for (size_t i = 0; i < num_8x8_blocks_wide_lookup[m_mi_size]; i++)
        m_above_seg_pred_context[m_mi_col + i] = seg_id_predicted;
    for (size_t i = 0; i < num_8x8_blocks_high_lookup[m_mi_size]; i++)
        m_left_seg_pred_context[m_mi_row + i] = seg_id_predicted;
    return true;
}

u8 Parser::get_segment_id()
{
    auto bw = num_8x8_blocks_wide_lookup[m_mi_size];
    auto bh = num_8x8_blocks_high_lookup[m_mi_size];
    auto xmis = min(m_mi_cols - m_mi_col, (u32)bw);
    auto ymis = min(m_mi_rows - m_mi_row, (u32)bh);
    u8 segment = 7;
    for (size_t y = 0; y < ymis; y++) {
        for (size_t x = 0; x < xmis; x++) {
            segment = min(segment, m_prev_segment_ids[(m_mi_row + y) + (m_mi_col + x)]);
        }
    }
    return segment;
}

bool Parser::read_is_inter()
{
    if (seg_feature_active(SEG_LVL_REF_FRAME))
        m_is_inter = m_feature_data[m_segment_id][SEG_LVL_REF_FRAME] != IntraFrame;
    else
        m_is_inter = m_tree_parser->parse_tree<bool>(SyntaxElementType::IsInter);
    return true;
}

bool Parser::intra_block_mode_info()
{
    m_ref_frame[0] = IntraFrame;
    m_ref_frame[1] = None;
    if (m_mi_size >= Block_8x8) {
        m_y_mode = m_tree_parser->parse_tree<u8>(SyntaxElementType::IntraMode);
        for (auto& block_sub_mode : m_block_sub_modes)
            block_sub_mode = m_y_mode;
    } else {
        m_num_4x4_w = num_4x4_blocks_wide_lookup[m_mi_size];
        m_num_4x4_h = num_4x4_blocks_high_lookup[m_mi_size];
        u8 sub_intra_mode;
        for (auto idy = 0; idy < 2; idy += m_num_4x4_h) {
            for (auto idx = 0; idx < 2; idx += m_num_4x4_w) {
                sub_intra_mode = m_tree_parser->parse_tree<u8>(SyntaxElementType::SubIntraMode);
                for (auto y = 0; y < m_num_4x4_h; y++) {
                    for (auto x = 0; x < m_num_4x4_w; x++)
                        m_block_sub_modes[(idy + y) * 2 + idx + x] = sub_intra_mode;
                }
            }
        }
        m_y_mode = sub_intra_mode;
    }
    m_uv_mode = m_tree_parser->parse_tree<u8>(SyntaxElementType::UVMode);
    return true;
}

bool Parser::inter_block_mode_info()
{
    SAFE_CALL(read_ref_frames());
    for (auto j = 0; j < 2; j++) {
        if (m_ref_frame[j] > IntraFrame) {
            SAFE_CALL(find_mv_refs(m_ref_frame[j], -1));
            SAFE_CALL(find_best_ref_mvs(j));
        }
    }
    auto is_compound = m_ref_frame[1] > IntraFrame;
    if (seg_feature_active(SEG_LVL_SKIP)) {
        m_y_mode = ZeroMv;
    } else if (m_mi_size >= Block_8x8) {
        auto inter_mode = m_tree_parser->parse_tree(SyntaxElementType::InterMode);
        m_y_mode = NearestMv + inter_mode;
    }
    if (m_interpolation_filter == Switchable)
        m_interp_filter = m_tree_parser->parse_tree<InterpolationFilter>(SyntaxElementType::InterpFilter);
    else
        m_interp_filter = m_interpolation_filter;
    if (m_mi_size < Block_8x8) {
        m_num_4x4_w = num_4x4_blocks_wide_lookup[m_mi_size];
        m_num_4x4_h = num_4x4_blocks_high_lookup[m_mi_size];
        for (auto idy = 0; idy < 2; idy += m_num_4x4_h) {
            for (auto idx = 0; idx < 2; idx += m_num_4x4_w) {
                auto inter_mode = m_tree_parser->parse_tree(SyntaxElementType::InterMode);
                m_y_mode = NearestMv + inter_mode;
                if (m_y_mode == NearestMv || m_y_mode == NearMv) {
                    for (auto j = 0; j < 1 + is_compound; j++)
                        SAFE_CALL(append_sub8x8_mvs(idy * 2 + idx, j));
                }
                SAFE_CALL(assign_mv(is_compound));
                for (auto y = 0; y < m_num_4x4_h; y++) {
                    for (auto x = 0; x < m_num_4x4_w; x++) {
                        auto block = (idy + y) * 2 + idx + x;
                        for (auto ref_list = 0; ref_list < 1 + is_compound; ref_list++) {
                            m_block_mvs[ref_list][block] = m_mv[ref_list];
                        }
                    }
                }
            }
        }
        return true;
    }
    SAFE_CALL(assign_mv(is_compound));
    for (auto ref_list = 0; ref_list < 1 + is_compound; ref_list++) {
        for (auto block = 0; block < 4; block++) {
            m_block_mvs[ref_list][block] = m_mv[ref_list];
        }
    }
    return true;
}

bool Parser::read_ref_frames()
{
    if (seg_feature_active(SEG_LVL_REF_FRAME)) {
        m_ref_frame[0] = static_cast<ReferenceFrame>(m_feature_data[m_segment_id][SEG_LVL_REF_FRAME]);
        m_ref_frame[1] = None;
        return true;
    }
    ReferenceMode comp_mode;
    if (m_reference_mode == ReferenceModeSelect)
        comp_mode = m_tree_parser->parse_tree<ReferenceMode>(SyntaxElementType::CompMode);
    else
        comp_mode = m_reference_mode;
    if (comp_mode == CompoundReference) {
        auto idx = m_ref_frame_sign_bias[m_comp_fixed_ref];
        auto comp_ref = m_tree_parser->parse_tree(SyntaxElementType::CompRef);
        m_ref_frame[idx] = m_comp_fixed_ref;
        m_ref_frame[!idx] = m_comp_var_ref[comp_ref];
        return true;
    }
    auto single_ref_p1 = m_tree_parser->parse_tree<bool>(SyntaxElementType::SingleRefP1);
    if (single_ref_p1) {
        auto single_ref_p2 = m_tree_parser->parse_tree<bool>(SyntaxElementType::SingleRefP2);
        m_ref_frame[0] = single_ref_p2 ? AltRefFrame : GoldenFrame;
    } else {
        m_ref_frame[0] = LastFrame;
    }
    m_ref_frame[1] = None;
    return true;
}

bool Parser::assign_mv(bool is_compound)
{
    m_mv[1] = 0;
    for (auto i = 0; i < 1 + is_compound; i++) {
        if (m_y_mode == NewMv) {
            SAFE_CALL(read_mv(i));
        } else if (m_y_mode == NearestMv) {
            m_mv[i] = m_nearest_mv[i];
        } else if (m_y_mode == NearMv) {
            m_mv[i] = m_near_mv[i];
        } else {
            m_mv[i] = 0;
        }
    }
    return true;
}

bool Parser::read_mv(u8 ref)
{
    m_use_hp = m_allow_high_precision_mv && use_mv_hp(m_best_mv[ref]);
    MV diff_mv;
    auto mv_joint = m_tree_parser->parse_tree<MvJoint>(SyntaxElementType::MVJoint);
    if (mv_joint == MvJointHzvnz || mv_joint == MvJointHnzvnz)
        diff_mv.set_row(read_mv_component(0));
    if (mv_joint == MvJointHnzvz || mv_joint == MvJointHnzvnz)
        diff_mv.set_col(read_mv_component(1));
    m_mv[ref] = m_best_mv[ref] + diff_mv;
    return true;
}

i32 Parser::read_mv_component(u8)
{
    auto mv_sign = m_tree_parser->parse_tree<bool>(SyntaxElementType::MVSign);
    auto mv_class = m_tree_parser->parse_tree<MvClass>(SyntaxElementType::MVClass);
    u32 mag;
    if (mv_class == MvClass0) {
        auto mv_class0_bit = m_tree_parser->parse_tree<u32>(SyntaxElementType::MVClass0Bit);
        auto mv_class0_fr = m_tree_parser->parse_tree<u32>(SyntaxElementType::MVClass0FR);
        auto mv_class0_hp = m_tree_parser->parse_tree<u32>(SyntaxElementType::MVClass0HP);
        mag = ((mv_class0_bit << 3) | (mv_class0_fr << 1) | mv_class0_hp) + 1;
    } else {
        auto d = 0;
        for (size_t i = 0; i < mv_class; i++) {
            auto mv_bit = m_tree_parser->parse_tree<bool>(SyntaxElementType::MVBit);
            d |= mv_bit << i;
        }
        mag = CLASS0_SIZE << (mv_class + 2);
        auto mv_fr = m_tree_parser->parse_tree<u32>(SyntaxElementType::MVFR);
        auto mv_hp = m_tree_parser->parse_tree<u32>(SyntaxElementType::MVHP);
        mag += ((d << 3) | (mv_fr << 1) | mv_hp) + 1;
    }
    return mv_sign ? -static_cast<i32>(mag) : static_cast<i32>(mag);
}

bool Parser::residual()
{
    auto block_size = m_mi_size < Block_8x8 ? Block_8x8 : static_cast<BlockSubsize>(m_mi_size);
    for (size_t plane = 0; plane < 3; plane++) {
        auto tx_size = (plane > 0) ? get_uv_tx_size() : m_tx_size;
        auto step = 1 << tx_size;
        auto plane_size = get_plane_block_size(block_size, plane);
        auto num_4x4_w = num_4x4_blocks_wide_lookup[plane_size];
        auto num_4x4_h = num_4x4_blocks_high_lookup[plane_size];
        auto sub_x = (plane > 0) ? m_subsampling_x : 0;
        auto sub_y = (plane > 0) ? m_subsampling_y : 0;
        auto base_x = (m_mi_col * 8) >> sub_x;
        auto base_y = (m_mi_row * 8) >> sub_y;
        if (m_is_inter) {
            if (m_mi_size < Block_8x8) {
                for (auto y = 0; y < num_4x4_h; y++) {
                    for (auto x = 0; x < num_4x4_w; x++) {
                        SAFE_CALL(m_decoder.predict_inter(plane, base_x + (4 * x), base_y + (4 * y), 4, 4, (y * num_4x4_w) + x));
                    }
                }
            } else {
                SAFE_CALL(m_decoder.predict_inter(plane, base_x, base_y, num_4x4_w * 4, num_4x4_h * 4, 0));
            }
        }
        auto max_x = (m_mi_cols * 8) >> sub_x;
        auto max_y = (m_mi_rows * 8) >> sub_y;
        auto block_index = 0;
        for (auto y = 0; y < num_4x4_h; y += step) {
            for (auto x = 0; x < num_4x4_w; x += step) {
                auto start_x = base_x + (4 * x);
                auto start_y = base_y + (4 * y);
                auto non_zero = false;
                if (start_x < max_x && start_y < max_y) {
                    if (!m_is_inter)
                        SAFE_CALL(m_decoder.predict_intra(plane, start_x, start_y, m_available_l || x > 0, m_available_u || y > 0, (x + step) < num_4x4_w, tx_size, block_index));
                    if (!m_skip) {
                        non_zero = tokens(plane, start_x, start_y, tx_size, block_index);
                        SAFE_CALL(m_decoder.reconstruct(plane, start_x, start_y, tx_size));
                    }
                }
                auto above_sub_context = m_above_nonzero_context[plane];
                auto left_sub_context = m_left_nonzero_context[plane];
                above_sub_context.resize_and_keep_capacity((start_x >> 2) + step);
                left_sub_context.resize_and_keep_capacity((start_y >> 2) + step);
                for (auto i = 0; i < step; i++) {
                    above_sub_context[(start_x >> 2) + i] = non_zero;
                    left_sub_context[(start_y >> 2) + i] = non_zero;
                }
                block_index++;
            }
        }
    }
    return true;
}

TXSize Parser::get_uv_tx_size()
{
    if (m_mi_size < Block_8x8)
        return TX_4x4;
    return min(m_tx_size, max_txsize_lookup[get_plane_block_size(m_mi_size, 1)]);
}

BlockSubsize Parser::get_plane_block_size(u32 subsize, u8 plane)
{
    auto sub_x = (plane > 0) ? m_subsampling_x : 0;
    auto sub_y = (plane > 0) ? m_subsampling_y : 0;
    return ss_size_lookup[subsize][sub_x][sub_y];
}

bool Parser::tokens(size_t plane, u32 start_x, u32 start_y, TXSize tx_size, u32 block_index)
{
    m_tree_parser->set_start_x_and_y(start_x, start_y);
    size_t segment_eob = 16 << (tx_size << 1);
    auto scan = get_scan(plane, tx_size, block_index);
    auto check_eob = true;
    size_t c = 0;
    for (; c < segment_eob; c++) {
        auto pos = scan[c];
        auto band = (tx_size == TX_4x4) ? coefband_4x4[c] : coefband_8x8plus[c];
        m_tree_parser->set_tokens_variables(band, c, plane, tx_size, pos);
        if (check_eob) {
            auto more_coefs = m_tree_parser->parse_tree<bool>(SyntaxElementType::MoreCoefs);
            if (!more_coefs)
                break;
        }
        auto token = m_tree_parser->parse_tree<Token>(SyntaxElementType::Token);
        m_token_cache[pos] = energy_class[token];
        if (token == ZeroToken) {
            m_tokens[pos] = 0;
            check_eob = false;
        } else {
            i32 coef = static_cast<i32>(read_coef(token));
            auto sign_bit = m_bit_stream->read_literal(1);
            m_tokens[pos] = sign_bit ? -coef : coef;
            check_eob = true;
        }
    }
    auto non_zero = c > 0;
    m_eob_total += non_zero;
    for (size_t i = c; i < segment_eob; i++)
        m_tokens[scan[i]] = 0;
    return non_zero;
}

u32 const* Parser::get_scan(size_t plane, TXSize tx_size, u32 block_index)
{
    if (plane > 0 || tx_size == TX_32x32) {
        m_tx_type = DCT_DCT;
    } else if (tx_size == TX_4x4) {
        if (m_lossless || m_is_inter)
            m_tx_type = DCT_DCT;
        else
            m_tx_type = mode_to_txfm_map[m_mi_size < Block_8x8 ? m_block_sub_modes[block_index] : m_y_mode];
    } else {
        m_tx_type = mode_to_txfm_map[m_y_mode];
    }
    if (tx_size == TX_4x4) {
        if (m_tx_type == ADST_DCT)
            return row_scan_4x4;
        if (m_tx_type == DCT_ADST)
            return col_scan_4x4;
        return default_scan_4x4;
    }
    if (tx_size == TX_8x8) {
        if (m_tx_type == ADST_DCT)
            return row_scan_8x8;
        if (m_tx_type == DCT_ADST)
            return col_scan_8x8;
        return default_scan_8x8;
    }
    if (tx_size == TX_16x16) {
        if (m_tx_type == ADST_DCT)
            return row_scan_16x16;
        if (m_tx_type == DCT_ADST)
            return col_scan_16x16;
        return default_scan_16x16;
    }
    return default_scan_32x32;
}

u32 Parser::read_coef(Token token)
{
    auto cat = extra_bits[token][0];
    auto num_extra = extra_bits[token][1];
    auto coef = extra_bits[token][2];
    if (token == DctValCat6) {
        for (size_t e = 0; e < (u8)(m_bit_depth - 8); e++) {
            auto high_bit = m_bit_stream->read_bool(255);
            coef += high_bit << (5 + m_bit_depth - e);
        }
    }
    for (size_t e = 0; e < num_extra; e++) {
        auto coef_bit = m_bit_stream->read_bool(cat_probs[cat][e]);
        coef += coef_bit << (num_extra - 1 - e);
    }
    return coef;
}

bool Parser::find_mv_refs(ReferenceFrame, int)
{
    // TODO: Implement
    return true;
}

bool Parser::find_best_ref_mvs(int)
{
    // TODO: Implement
    return true;
}

bool Parser::append_sub8x8_mvs(u8, u8)
{
    // TODO: Implement
    return true;
}

bool Parser::use_mv_hp(const MV&)
{
    // TODO: Implement
    return true;
}

void Parser::dump_info()
{
    outln("Frame dimensions: {}x{}", m_frame_width, m_frame_height);
    outln("Render dimensions: {}x{}", m_render_width, m_render_height);
    outln("Bit depth: {}", m_bit_depth);
    outln("Interpolation filter: {}", (u8)m_interpolation_filter);
}

}
