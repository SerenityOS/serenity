/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibGfx/Point.h>
#include <LibGfx/Size.h>

#include "Decoder.h"
#include "Parser.h"
#include "Utilities.h"

namespace Video::VP9 {

#define TRY_READ(expression) DECODER_TRY(DecoderErrorCategory::Corrupted, expression)

Parser::Parser(Decoder& decoder)
    : m_probability_tables(make<ProbabilityTables>())
    , m_tree_parser(make<TreeParser>(*this))
    , m_decoder(decoder)
{
}

Parser::~Parser()
{
}

void Parser::cleanup_tile_allocations()
{
    // FIXME: Is this necessary? Data should be truncated and
    //        overwritten by the next tile.
    m_skips.clear_with_capacity();
    m_tx_sizes.clear_with_capacity();
    m_mi_sizes.clear_with_capacity();
    m_y_modes.clear_with_capacity();
    m_segment_ids.clear_with_capacity();
    m_ref_frames.clear_with_capacity();
    m_interp_filters.clear_with_capacity();
    m_mvs.clear_with_capacity();
    m_sub_mvs.clear_with_capacity();
    m_sub_modes.clear_with_capacity();
}

/* (6.1) */
DecoderErrorOr<void> Parser::parse_frame(ByteBuffer const& frame_data)
{
    m_bit_stream = make<BitStream>(frame_data.data(), frame_data.size());
    m_syntax_element_counter = make<SyntaxElementCounter>();

    TRY(uncompressed_header());
    dbgln("Finished reading uncompressed header");
    if (!trailing_bits())
        return DecoderError::corrupted("Trailing bits were non-zero"sv);
    if (m_header_size_in_bytes == 0)
        return DecoderError::corrupted("Frame header is zero-sized"sv);
    m_probability_tables->load_probs(m_frame_context_idx);
    m_probability_tables->load_probs2(m_frame_context_idx);
    m_syntax_element_counter->clear_counts();

    TRY_READ(m_bit_stream->init_bool(m_header_size_in_bytes));
    dbgln("Reading compressed header with size {}", m_header_size_in_bytes);
    TRY(compressed_header());
    dbgln("Finished reading compressed header");
    TRY_READ(m_bit_stream->exit_bool());

    TRY(m_decoder.allocate_buffers());

    TRY(decode_tiles());
    TRY(refresh_probs());

    dbgln("Finished reading frame!");
    return {};
}

bool Parser::trailing_bits()
{
    while (m_bit_stream->bits_remaining() & 7u) {
        if (MUST(m_bit_stream->read_bit()))
            return false;
    }
    return true;
}

DecoderErrorOr<void> Parser::refresh_probs()
{
    if (!m_error_resilient_mode && !m_frame_parallel_decoding_mode) {
        m_probability_tables->load_probs(m_frame_context_idx);
        TRY(m_decoder.adapt_coef_probs());
        if (!m_frame_is_intra) {
            m_probability_tables->load_probs2(m_frame_context_idx);
            TRY(m_decoder.adapt_non_coef_probs());
        }
    }
    if (m_refresh_frame_context)
        m_probability_tables->save_probs(m_frame_context_idx);
    return {};
}

DecoderErrorOr<FrameType> Parser::read_frame_type()
{
    if (TRY_READ(m_bit_stream->read_bit()))
        return NonKeyFrame;
    return KeyFrame;
}

DecoderErrorOr<ColorRange> Parser::read_color_range()
{
    if (TRY_READ(m_bit_stream->read_bit()))
        return FullSwing;
    return StudioSwing;
}

/* (6.2) */
DecoderErrorOr<void> Parser::uncompressed_header()
{
    auto frame_marker = TRY_READ(m_bit_stream->read_bits(2));
    if (frame_marker != 2)
        return DecoderError::corrupted("uncompressed_header: Frame marker must be 2"sv);
    auto profile_low_bit = TRY_READ(m_bit_stream->read_bit());
    auto profile_high_bit = TRY_READ(m_bit_stream->read_bit());
    m_profile = (profile_high_bit << 1u) + profile_low_bit;
    if (m_profile == 3 && TRY_READ(m_bit_stream->read_bit()))
        return DecoderError::corrupted("uncompressed_header: Profile 3 reserved bit was non-zero"sv);
    auto show_existing_frame = TRY_READ(m_bit_stream->read_bit());
    if (show_existing_frame) {
        m_frame_to_show_map_index = TRY_READ(m_bit_stream->read_bits(3));
        m_header_size_in_bytes = 0;
        m_refresh_frame_flags = 0;
        m_loop_filter_level = 0;
        return {};
    }

    m_last_frame_type = m_frame_type;
    m_frame_type = TRY(read_frame_type());
    m_show_frame = TRY_READ(m_bit_stream->read_bit());
    m_error_resilient_mode = TRY_READ(m_bit_stream->read_bit());

    if (m_frame_type == KeyFrame) {
        TRY(frame_sync_code());
        TRY(color_config());
        TRY(frame_size());
        TRY(render_size());
        m_refresh_frame_flags = 0xFF;
        m_frame_is_intra = true;
    } else {
        m_frame_is_intra = !m_show_frame && TRY_READ(m_bit_stream->read_bit());

        if (!m_error_resilient_mode) {
            m_reset_frame_context = TRY_READ(m_bit_stream->read_bits(2));
        } else {
            m_reset_frame_context = 0;
        }

        if (m_frame_is_intra) {
            TRY(frame_sync_code());
            if (m_profile > 0) {
                TRY(color_config());
            } else {
                m_color_space = Bt601;
                m_subsampling_x = true;
                m_subsampling_y = true;
                m_bit_depth = 8;
            }

            m_refresh_frame_flags = TRY_READ(m_bit_stream->read_f8());
            TRY(frame_size());
            TRY(render_size());
        } else {
            m_refresh_frame_flags = TRY_READ(m_bit_stream->read_f8());
            for (auto i = 0; i < 3; i++) {
                m_ref_frame_idx[i] = TRY_READ(m_bit_stream->read_bits(3));
                m_ref_frame_sign_bias[LastFrame + i] = TRY_READ(m_bit_stream->read_bit());
            }
            TRY(frame_size_with_refs());
            m_allow_high_precision_mv = TRY_READ(m_bit_stream->read_bit());
            TRY(read_interpolation_filter());
        }
    }

    if (!m_error_resilient_mode) {
        m_refresh_frame_context = TRY_READ(m_bit_stream->read_bit());
        m_frame_parallel_decoding_mode = TRY_READ(m_bit_stream->read_bit());
    } else {
        m_refresh_frame_context = false;
        m_frame_parallel_decoding_mode = true;
    }

    m_frame_context_idx = TRY_READ(m_bit_stream->read_bits(2));
    if (m_frame_is_intra || m_error_resilient_mode) {
        setup_past_independence();
        if (m_frame_type == KeyFrame || m_error_resilient_mode || m_reset_frame_context == 3) {
            for (auto i = 0; i < 4; i++) {
                m_probability_tables->save_probs(i);
            }
        } else if (m_reset_frame_context == 2) {
            m_probability_tables->save_probs(m_frame_context_idx);
        }
        m_frame_context_idx = 0;
    }

    TRY(loop_filter_params());
    TRY(quantization_params());
    TRY(segmentation_params());
    TRY(tile_info());

    m_header_size_in_bytes = TRY_READ(m_bit_stream->read_f16());

    return {};
}

DecoderErrorOr<void> Parser::frame_sync_code()
{
    if (TRY_READ(m_bit_stream->read_f8()) != 0x49)
        return DecoderError::corrupted("frame_sync_code: Byte 0 was not 0x49."sv);
    if (TRY_READ(m_bit_stream->read_f8()) != 0x83)
        return DecoderError::corrupted("frame_sync_code: Byte 1 was not 0x83."sv);
    if (TRY_READ(m_bit_stream->read_f8()) != 0x42)
        return DecoderError::corrupted("frame_sync_code: Byte 2 was not 0x42."sv);
    return {};
}

DecoderErrorOr<void> Parser::color_config()
{
    if (m_profile >= 2) {
        m_bit_depth = TRY_READ(m_bit_stream->read_bit()) ? 12 : 10;
    } else {
        m_bit_depth = 8;
    }

    auto color_space = TRY_READ(m_bit_stream->read_bits(3));
    VERIFY(color_space <= RGB);
    m_color_space = static_cast<ColorSpace>(color_space);

    if (color_space != RGB) {
        m_color_range = TRY(read_color_range());
        if (m_profile == 1 || m_profile == 3) {
            m_subsampling_x = TRY_READ(m_bit_stream->read_bit());
            m_subsampling_y = TRY_READ(m_bit_stream->read_bit());
            if (TRY_READ(m_bit_stream->read_bit()))
                return DecoderError::corrupted("color_config: Subsampling reserved zero was set"sv);
        } else {
            m_subsampling_x = true;
            m_subsampling_y = true;
        }
    } else {
        m_color_range = FullSwing;
        if (m_profile == 1 || m_profile == 3) {
            m_subsampling_x = false;
            m_subsampling_y = false;
            if (TRY_READ(m_bit_stream->read_bit()))
                return DecoderError::corrupted("color_config: RGB reserved zero was set"sv);
        }
    }
    return {};
}

DecoderErrorOr<void> Parser::frame_size()
{
    m_frame_width = TRY_READ(m_bit_stream->read_f16()) + 1;
    m_frame_height = TRY_READ(m_bit_stream->read_f16()) + 1;
    compute_image_size();
    return {};
}

DecoderErrorOr<void> Parser::render_size()
{
    if (TRY_READ(m_bit_stream->read_bit())) {
        m_render_width = TRY_READ(m_bit_stream->read_f16()) + 1;
        m_render_height = TRY_READ(m_bit_stream->read_f16()) + 1;
    } else {
        m_render_width = m_frame_width;
        m_render_height = m_frame_height;
    }
    return {};
}

DecoderErrorOr<void> Parser::frame_size_with_refs()
{
    bool found_ref;
    for (auto frame_index : m_ref_frame_idx) {
        found_ref = TRY_READ(m_bit_stream->read_bit());
        if (found_ref) {
            dbgln("Reading size from ref frame {}", frame_index);
            m_frame_width = m_ref_frame_width[frame_index];
            m_frame_height = m_ref_frame_height[frame_index];
            break;
        }
    }

    if (!found_ref) {
        TRY(frame_size());
    } else {
        compute_image_size();
    }

    return render_size();
}

void Parser::compute_image_size()
{
    m_mi_cols = (m_frame_width + 7u) >> 3u;
    m_mi_rows = (m_frame_height + 7u) >> 3u;
    m_sb64_cols = (m_mi_cols + 7u) >> 3u;
    m_sb64_rows = (m_mi_rows + 7u) >> 3u;
}

DecoderErrorOr<void> Parser::read_interpolation_filter()
{
    if (TRY_READ(m_bit_stream->read_bit())) {
        m_interpolation_filter = Switchable;
    } else {
        m_interpolation_filter = literal_to_type[TRY_READ(m_bit_stream->read_bits(2))];
    }
    return {};
}

DecoderErrorOr<void> Parser::loop_filter_params()
{
    m_loop_filter_level = TRY_READ(m_bit_stream->read_bits(6));
    m_loop_filter_sharpness = TRY_READ(m_bit_stream->read_bits(3));
    m_loop_filter_delta_enabled = TRY_READ(m_bit_stream->read_bit());
    if (m_loop_filter_delta_enabled) {
        if (TRY_READ(m_bit_stream->read_bit())) {
            for (auto& loop_filter_ref_delta : m_loop_filter_ref_deltas) {
                if (TRY_READ(m_bit_stream->read_bit()))
                    loop_filter_ref_delta = TRY_READ(m_bit_stream->read_s(6));
            }
            for (auto& loop_filter_mode_delta : m_loop_filter_mode_deltas) {
                if (TRY_READ(m_bit_stream->read_bit()))
                    loop_filter_mode_delta = TRY_READ(m_bit_stream->read_s(6));
            }
        }
    }

    return {};
}

DecoderErrorOr<void> Parser::quantization_params()
{
    m_base_q_idx = TRY_READ(m_bit_stream->read_f8());
    m_delta_q_y_dc = TRY(read_delta_q());
    m_delta_q_uv_dc = TRY(read_delta_q());
    m_delta_q_uv_ac = TRY(read_delta_q());
    m_lossless = m_base_q_idx == 0 && m_delta_q_y_dc == 0 && m_delta_q_uv_dc == 0 && m_delta_q_uv_ac == 0;
    return {};
}

DecoderErrorOr<i8> Parser::read_delta_q()
{
    if (TRY_READ(m_bit_stream->read_bit()))
        return TRY_READ(m_bit_stream->read_s(4));
    return 0;
}

DecoderErrorOr<void> Parser::segmentation_params()
{
    m_segmentation_enabled = TRY_READ(m_bit_stream->read_bit());
    if (!m_segmentation_enabled)
        return {};

    m_segmentation_update_map = TRY_READ(m_bit_stream->read_bit());
    if (m_segmentation_update_map) {
        for (auto& segmentation_tree_prob : m_segmentation_tree_probs)
            segmentation_tree_prob = TRY(read_prob());
        m_segmentation_temporal_update = TRY_READ(m_bit_stream->read_bit());
        for (auto& segmentation_pred_prob : m_segmentation_pred_prob)
            segmentation_pred_prob = m_segmentation_temporal_update ? TRY(read_prob()) : 255;
    }

    auto segmentation_update_data = (TRY_READ(m_bit_stream->read_bit()));

    if (!segmentation_update_data)
        return {};

    m_segmentation_abs_or_delta_update = TRY_READ(m_bit_stream->read_bit());
    for (auto i = 0; i < MAX_SEGMENTS; i++) {
        for (auto j = 0; j < SEG_LVL_MAX; j++) {
            auto feature_value = 0;
            auto feature_enabled = TRY_READ(m_bit_stream->read_bit());
            m_feature_enabled[i][j] = feature_enabled;
            if (feature_enabled) {
                auto bits_to_read = segmentation_feature_bits[j];
                feature_value = TRY_READ(m_bit_stream->read_bits(bits_to_read));
                if (segmentation_feature_signed[j]) {
                    if (TRY_READ(m_bit_stream->read_bit()))
                        feature_value = -feature_value;
                }
            }
            m_feature_data[i][j] = feature_value;
        }
    }

    return {};
}

DecoderErrorOr<u8> Parser::read_prob()
{
    if (TRY_READ(m_bit_stream->read_bit()))
        return TRY_READ(m_bit_stream->read_f8());
    return 255;
}

DecoderErrorOr<void> Parser::tile_info()
{
    auto min_log2_tile_cols = calc_min_log2_tile_cols();
    auto max_log2_tile_cols = calc_max_log2_tile_cols();
    m_tile_cols_log2 = min_log2_tile_cols;
    while (m_tile_cols_log2 < max_log2_tile_cols) {
        if (TRY_READ(m_bit_stream->read_bit()))
            m_tile_cols_log2++;
        else
            break;
    }
    m_tile_rows_log2 = TRY_READ(m_bit_stream->read_bit());
    if (m_tile_rows_log2) {
        m_tile_rows_log2 += TRY_READ(m_bit_stream->read_bit());
    }
    return {};
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

void Parser::setup_past_independence()
{
    for (auto i = 0; i < 8; i++) {
        for (auto j = 0; j < 4; j++) {
            m_feature_data[i][j] = 0;
            m_feature_enabled[i][j] = false;
        }
    }
    m_segmentation_abs_or_delta_update = false;
    m_prev_segment_ids.clear_with_capacity();
    m_prev_segment_ids.resize_and_keep_capacity(m_mi_rows * m_mi_cols);
    m_loop_filter_delta_enabled = true;
    m_loop_filter_ref_deltas[IntraFrame] = 1;
    m_loop_filter_ref_deltas[LastFrame] = 0;
    m_loop_filter_ref_deltas[GoldenFrame] = -1;
    m_loop_filter_ref_deltas[AltRefFrame] = -1;
    for (auto& loop_filter_mode_delta : m_loop_filter_mode_deltas)
        loop_filter_mode_delta = 0;
    m_probability_tables->reset_probs();
}

DecoderErrorOr<void> Parser::compressed_header()
{
    TRY(read_tx_mode());
    if (m_tx_mode == TXModeSelect)
        TRY(tx_mode_probs());
    TRY(read_coef_probs());
    TRY(read_skip_prob());
    if (!m_frame_is_intra) {
        TRY(read_inter_mode_probs());
        if (m_interpolation_filter == Switchable)
            TRY(read_interp_filter_probs());
        TRY(read_is_inter_probs());
        TRY(frame_reference_mode());
        TRY(frame_reference_mode_probs());
        TRY(read_y_mode_probs());
        TRY(read_partition_probs());
        TRY(mv_probs());
    }
    return {};
}

DecoderErrorOr<void> Parser::read_tx_mode()
{
    if (m_lossless) {
        m_tx_mode = Only_4x4;
    } else {
        auto tx_mode = TRY_READ(m_bit_stream->read_literal(2));
        if (tx_mode == Allow_32x32)
            tx_mode += TRY_READ(m_bit_stream->read_literal(1));
        m_tx_mode = static_cast<TXMode>(tx_mode);
    }
    return {};
}

DecoderErrorOr<void> Parser::tx_mode_probs()
{
    auto& tx_probs = m_probability_tables->tx_probs();
    for (auto i = 0; i < TX_SIZE_CONTEXTS; i++) {
        for (auto j = 0; j < TX_SIZES - 3; j++)
            tx_probs[TX_8x8][i][j] = TRY(diff_update_prob(tx_probs[TX_8x8][i][j]));
    }
    for (auto i = 0; i < TX_SIZE_CONTEXTS; i++) {
        for (auto j = 0; j < TX_SIZES - 2; j++)
            tx_probs[TX_16x16][i][j] = TRY(diff_update_prob(tx_probs[TX_16x16][i][j]));
    }
    for (auto i = 0; i < TX_SIZE_CONTEXTS; i++) {
        for (auto j = 0; j < TX_SIZES - 1; j++)
            tx_probs[TX_32x32][i][j] = TRY(diff_update_prob(tx_probs[TX_32x32][i][j]));
    }
    return {};
}

DecoderErrorOr<u8> Parser::diff_update_prob(u8 prob)
{
    auto update_prob = TRY_READ(m_bit_stream->read_bool(252));
    if (update_prob) {
        auto delta_prob = TRY(decode_term_subexp());
        prob = inv_remap_prob(delta_prob, prob);
    }
    return prob;
}

DecoderErrorOr<u8> Parser::decode_term_subexp()
{
    if (TRY_READ(m_bit_stream->read_literal(1)) == 0)
        return TRY_READ(m_bit_stream->read_literal(4));
    if (TRY_READ(m_bit_stream->read_literal(1)) == 0)
        return TRY_READ(m_bit_stream->read_literal(4)) + 16;
    if (TRY_READ(m_bit_stream->read_literal(1)) == 0)
        return TRY_READ(m_bit_stream->read_literal(5)) + 32;

    auto v = TRY_READ(m_bit_stream->read_literal(7));
    if (v < 65)
        return v + 64;
    return (v << 1u) - 1 + TRY_READ(m_bit_stream->read_literal(1));
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

DecoderErrorOr<void> Parser::read_coef_probs()
{
    m_max_tx_size = tx_mode_to_biggest_tx_size[m_tx_mode];
    for (u8 tx_size = 0; tx_size <= m_max_tx_size; tx_size++) {
        auto update_probs = TRY_READ(m_bit_stream->read_literal(1));
        if (update_probs == 1) {
            for (auto i = 0; i < 2; i++) {
                for (auto j = 0; j < 2; j++) {
                    for (auto k = 0; k < 6; k++) {
                        auto max_l = (k == 0) ? 3 : 6;
                        for (auto l = 0; l < max_l; l++) {
                            for (auto m = 0; m < 3; m++) {
                                auto& prob = m_probability_tables->coef_probs()[tx_size][i][j][k][l][m];
                                prob = TRY(diff_update_prob(prob));
                            }
                        }
                    }
                }
            }
        }
    }
    return {};
}

DecoderErrorOr<void> Parser::read_skip_prob()
{
    for (auto i = 0; i < SKIP_CONTEXTS; i++)
        m_probability_tables->skip_prob()[i] = TRY(diff_update_prob(m_probability_tables->skip_prob()[i]));
    return {};
}

DecoderErrorOr<void> Parser::read_inter_mode_probs()
{
    for (auto i = 0; i < INTER_MODE_CONTEXTS; i++) {
        for (auto j = 0; j < INTER_MODES - 1; j++)
            m_probability_tables->inter_mode_probs()[i][j] = TRY(diff_update_prob(m_probability_tables->inter_mode_probs()[i][j]));
    }
    return {};
}

DecoderErrorOr<void> Parser::read_interp_filter_probs()
{
    for (auto i = 0; i < INTERP_FILTER_CONTEXTS; i++) {
        for (auto j = 0; j < SWITCHABLE_FILTERS - 1; j++)
            m_probability_tables->interp_filter_probs()[i][j] = TRY(diff_update_prob(m_probability_tables->interp_filter_probs()[i][j]));
    }
    return {};
}

DecoderErrorOr<void> Parser::read_is_inter_probs()
{
    for (auto i = 0; i < IS_INTER_CONTEXTS; i++)
        m_probability_tables->is_inter_prob()[i] = TRY(diff_update_prob(m_probability_tables->is_inter_prob()[i]));
    return {};
}

DecoderErrorOr<void> Parser::frame_reference_mode()
{
    auto compound_reference_allowed = false;
    for (size_t i = 2; i <= REFS_PER_FRAME; i++) {
        if (m_ref_frame_sign_bias[i] != m_ref_frame_sign_bias[1])
            compound_reference_allowed = true;
    }
    if (compound_reference_allowed) {
        auto non_single_reference = TRY_READ(m_bit_stream->read_literal(1));
        if (non_single_reference == 0) {
            m_reference_mode = SingleReference;
        } else {
            auto reference_select = TRY_READ(m_bit_stream->read_literal(1));
            if (reference_select == 0)
                m_reference_mode = CompoundReference;
            else
                m_reference_mode = ReferenceModeSelect;
            setup_compound_reference_mode();
        }
    } else {
        m_reference_mode = SingleReference;
    }
    return {};
}

DecoderErrorOr<void> Parser::frame_reference_mode_probs()
{
    if (m_reference_mode == ReferenceModeSelect) {
        for (auto i = 0; i < COMP_MODE_CONTEXTS; i++) {
            auto& comp_mode_prob = m_probability_tables->comp_mode_prob();
            comp_mode_prob[i] = TRY(diff_update_prob(comp_mode_prob[i]));
        }
    }
    if (m_reference_mode != CompoundReference) {
        for (auto i = 0; i < REF_CONTEXTS; i++) {
            auto& single_ref_prob = m_probability_tables->single_ref_prob();
            single_ref_prob[i][0] = TRY(diff_update_prob(single_ref_prob[i][0]));
            single_ref_prob[i][1] = TRY(diff_update_prob(single_ref_prob[i][1]));
        }
    }
    if (m_reference_mode != SingleReference) {
        for (auto i = 0; i < REF_CONTEXTS; i++) {
            auto& comp_ref_prob = m_probability_tables->comp_ref_prob();
            comp_ref_prob[i] = TRY(diff_update_prob(comp_ref_prob[i]));
        }
    }
    return {};
}

DecoderErrorOr<void> Parser::read_y_mode_probs()
{
    for (auto i = 0; i < BLOCK_SIZE_GROUPS; i++) {
        for (auto j = 0; j < INTRA_MODES - 1; j++) {
            auto& y_mode_probs = m_probability_tables->y_mode_probs();
            y_mode_probs[i][j] = TRY(diff_update_prob(y_mode_probs[i][j]));
        }
    }
    return {};
}

DecoderErrorOr<void> Parser::read_partition_probs()
{
    for (auto i = 0; i < PARTITION_CONTEXTS; i++) {
        for (auto j = 0; j < PARTITION_TYPES - 1; j++) {
            auto& partition_probs = m_probability_tables->partition_probs();
            partition_probs[i][j] = TRY(diff_update_prob(partition_probs[i][j]));
        }
    }
    return {};
}

DecoderErrorOr<void> Parser::mv_probs()
{
    for (auto j = 0; j < MV_JOINTS - 1; j++) {
        auto& mv_joint_probs = m_probability_tables->mv_joint_probs();
        mv_joint_probs[j] = TRY(update_mv_prob(mv_joint_probs[j]));
    }

    for (auto i = 0; i < 2; i++) {
        auto& mv_sign_prob = m_probability_tables->mv_sign_prob();
        mv_sign_prob[i] = TRY(update_mv_prob(mv_sign_prob[i]));
        for (auto j = 0; j < MV_CLASSES - 1; j++) {
            auto& mv_class_probs = m_probability_tables->mv_class_probs();
            mv_class_probs[i][j] = TRY(update_mv_prob(mv_class_probs[i][j]));
        }
        auto& mv_class0_bit_prob = m_probability_tables->mv_class0_bit_prob();
        mv_class0_bit_prob[i] = TRY(update_mv_prob(mv_class0_bit_prob[i]));
        for (auto j = 0; j < MV_OFFSET_BITS; j++) {
            auto& mv_bits_prob = m_probability_tables->mv_bits_prob();
            mv_bits_prob[i][j] = TRY(update_mv_prob(mv_bits_prob[i][j]));
        }
    }

    for (auto i = 0; i < 2; i++) {
        for (auto j = 0; j < CLASS0_SIZE; j++) {
            for (auto k = 0; k < MV_FR_SIZE - 1; k++) {
                auto& mv_class0_fr_probs = m_probability_tables->mv_class0_fr_probs();
                mv_class0_fr_probs[i][j][k] = TRY(update_mv_prob(mv_class0_fr_probs[i][j][k]));
            }
        }
        for (auto k = 0; k < MV_FR_SIZE - 1; k++) {
            auto& mv_fr_probs = m_probability_tables->mv_fr_probs();
            mv_fr_probs[i][k] = TRY(update_mv_prob(mv_fr_probs[i][k]));
        }
    }

    if (m_allow_high_precision_mv) {
        for (auto i = 0; i < 2; i++) {
            auto& mv_class0_hp_prob = m_probability_tables->mv_class0_hp_prob();
            auto& mv_hp_prob = m_probability_tables->mv_hp_prob();
            mv_class0_hp_prob[i] = TRY(update_mv_prob(mv_class0_hp_prob[i]));
            mv_hp_prob[i] = TRY(update_mv_prob(mv_hp_prob[i]));
        }
    }

    return {};
}

DecoderErrorOr<u8> Parser::update_mv_prob(u8 prob)
{
    if (TRY_READ(m_bit_stream->read_bool(252))) {
        return (TRY_READ(m_bit_stream->read_literal(7)) << 1u) | 1u;
    }
    return prob;
}

void Parser::setup_compound_reference_mode()
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
}

DecoderErrorOr<void> Parser::allocate_tile_data()
{
    auto dimensions = m_mi_rows * m_mi_cols;
    cleanup_tile_allocations();
    DECODER_TRY_ALLOC(m_skips.try_resize_and_keep_capacity(dimensions));
    DECODER_TRY_ALLOC(m_tx_sizes.try_resize_and_keep_capacity(dimensions));
    DECODER_TRY_ALLOC(m_mi_sizes.try_resize_and_keep_capacity(dimensions));
    DECODER_TRY_ALLOC(m_y_modes.try_resize_and_keep_capacity(dimensions));
    DECODER_TRY_ALLOC(m_segment_ids.try_resize_and_keep_capacity(dimensions));
    DECODER_TRY_ALLOC(m_ref_frames.try_resize_and_keep_capacity(dimensions));
    DECODER_TRY_ALLOC(m_interp_filters.try_resize_and_keep_capacity(dimensions));
    DECODER_TRY_ALLOC(m_mvs.try_resize_and_keep_capacity(dimensions));
    DECODER_TRY_ALLOC(m_sub_mvs.try_resize_and_keep_capacity(dimensions));
    DECODER_TRY_ALLOC(m_sub_modes.try_resize_and_keep_capacity(dimensions));
    return {};
}

DecoderErrorOr<void> Parser::decode_tiles()
{
    auto tile_cols = 1 << m_tile_cols_log2;
    auto tile_rows = 1 << m_tile_rows_log2;
    TRY(allocate_tile_data());
    clear_above_context();
    for (auto tile_row = 0; tile_row < tile_rows; tile_row++) {
        for (auto tile_col = 0; tile_col < tile_cols; tile_col++) {
            auto last_tile = (tile_row == tile_rows - 1) && (tile_col == tile_cols - 1);
            u64 tile_size;
            if (last_tile)
                tile_size = m_bit_stream->bytes_remaining();
            else
                tile_size = TRY_READ(m_bit_stream->read_bits(32));

            m_mi_row_start = get_tile_offset(tile_row, m_mi_rows, m_tile_rows_log2);
            m_mi_row_end = get_tile_offset(tile_row + 1, m_mi_rows, m_tile_rows_log2);
            m_mi_col_start = get_tile_offset(tile_col, m_mi_cols, m_tile_cols_log2);
            m_mi_col_end = get_tile_offset(tile_col + 1, m_mi_cols, m_tile_cols_log2);
            TRY_READ(m_bit_stream->init_bool(tile_size));
            TRY(decode_tile());
            TRY_READ(m_bit_stream->exit_bool());
        }
    }
    return {};
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

void Parser::clear_above_context()
{
    clear_context(m_above_nonzero_context, 3, 2 * m_mi_cols);
    clear_context(m_above_seg_pred_context, m_mi_cols);
    clear_context(m_above_partition_context, m_sb64_cols * 8);
}

u32 Parser::get_tile_offset(u32 tile_num, u32 mis, u32 tile_size_log2)
{
    u32 super_blocks = (mis + 7) >> 3u;
    u32 offset = ((tile_num * super_blocks) >> tile_size_log2) << 3u;
    return min(offset, mis);
}

DecoderErrorOr<void> Parser::decode_tile()
{
    for (auto row = m_mi_row_start; row < m_mi_row_end; row += 8) {
        clear_left_context();
        for (auto col = m_mi_col_start; col < m_mi_col_end; col += 8) {
            TRY(decode_partition(row, col, Block_64x64));
        }
    }
    return {};
}

void Parser::clear_left_context()
{
    clear_context(m_left_nonzero_context, 3, 2 * m_mi_rows);
    clear_context(m_left_seg_pred_context, m_mi_rows);
    clear_context(m_left_partition_context, m_sb64_rows * 8);
}

DecoderErrorOr<void> Parser::decode_partition(u32 row, u32 col, u8 block_subsize)
{
    if (row >= m_mi_rows || col >= m_mi_cols)
        return {};
    m_block_subsize = block_subsize;
    m_num_8x8 = num_8x8_blocks_wide_lookup[block_subsize];
    auto half_block_8x8 = m_num_8x8 >> 1;
    m_has_rows = (row + half_block_8x8) < m_mi_rows;
    m_has_cols = (col + half_block_8x8) < m_mi_cols;
    m_row = row;
    m_col = col;
    auto partition = TRY_READ(m_tree_parser->parse_tree(SyntaxElementType::Partition));

    auto subsize = subsize_lookup[partition][block_subsize];
    if (subsize < Block_8x8 || partition == PartitionNone) {
        TRY(decode_block(row, col, subsize));
    } else if (partition == PartitionHorizontal) {
        TRY(decode_block(row, col, subsize));
        if (m_has_rows)
            TRY(decode_block(row + half_block_8x8, col, subsize));
    } else if (partition == PartitionVertical) {
        TRY(decode_block(row, col, subsize));
        if (m_has_cols)
            TRY(decode_block(row, col + half_block_8x8, subsize));
    } else {
        TRY(decode_partition(row, col, subsize));
        TRY(decode_partition(row, col + half_block_8x8, subsize));
        TRY(decode_partition(row + half_block_8x8, col, subsize));
        TRY(decode_partition(row + half_block_8x8, col + half_block_8x8, subsize));
    }
    if (block_subsize == Block_8x8 || partition != PartitionSplit) {
        auto above_context = 15 >> b_width_log2_lookup[subsize];
        auto left_context = 15 >> b_height_log2_lookup[subsize];
        for (size_t i = 0; i < m_num_8x8; i++) {
            m_above_partition_context[col + i] = above_context;
            m_left_partition_context[row + i] = left_context;
        }
    }
    return {};
}

size_t Parser::get_image_index(u32 row, u32 column)
{
    return row * m_mi_cols + column;
}

DecoderErrorOr<void> Parser::decode_block(u32 row, u32 col, BlockSubsize subsize)
{
    m_mi_row = row;
    m_mi_col = col;
    m_mi_size = subsize;
    m_available_u = row > 0;
    m_available_l = col > m_mi_col_start;
    TRY(mode_info());
    m_eob_total = 0;
    TRY(residual());
    if (m_is_inter && subsize >= Block_8x8 && m_eob_total == 0)
        m_skip = true;
    for (size_t y = 0; y < num_8x8_blocks_high_lookup[subsize]; y++) {
        for (size_t x = 0; x < num_8x8_blocks_wide_lookup[subsize]; x++) {
            auto pos = get_image_index(row + y, col + x);
            m_skips[pos] = m_skip;
            m_tx_sizes[pos] = m_tx_size;
            m_mi_sizes[pos] = m_mi_size;
            m_y_modes[pos] = m_y_mode;
            m_segment_ids[pos] = m_segment_id;
            for (size_t ref_list = 0; ref_list < 2; ref_list++)
                m_ref_frames[pos][ref_list] = m_ref_frame[ref_list];
            if (m_is_inter) {
                m_interp_filters[pos] = m_interp_filter;
                for (size_t ref_list = 0; ref_list < 2; ref_list++) {
                    m_mvs[pos][ref_list] = m_block_mvs[ref_list][3];
                    for (size_t b = 0; b < 4; b++)
                        m_sub_mvs[pos][ref_list][b] = m_block_mvs[ref_list][b];
                }
            } else {
                for (size_t b = 0; b < 4; b++)
                    m_sub_modes[pos][b] = static_cast<IntraMode>(m_block_sub_modes[b]);
            }
        }
    }
    return {};
}

DecoderErrorOr<void> Parser::mode_info()
{
    if (m_frame_is_intra)
        TRY(intra_frame_mode_info());
    else
        TRY(inter_frame_mode_info());
    return {};
}

DecoderErrorOr<void> Parser::intra_frame_mode_info()
{
    TRY(intra_segment_id());
    TRY(read_skip());
    TRY(read_tx_size(true));
    m_ref_frame[0] = IntraFrame;
    m_ref_frame[1] = None;
    m_is_inter = false;
    if (m_mi_size >= Block_8x8) {
        m_default_intra_mode = TRY_READ(m_tree_parser->parse_tree<IntraMode>(SyntaxElementType::DefaultIntraMode));
        m_y_mode = m_default_intra_mode;
        for (auto& block_sub_mode : m_block_sub_modes)
            block_sub_mode = m_y_mode;
    } else {
        m_num_4x4_w = num_4x4_blocks_wide_lookup[m_mi_size];
        m_num_4x4_h = num_4x4_blocks_high_lookup[m_mi_size];
        for (auto idy = 0; idy < 2; idy += m_num_4x4_h) {
            for (auto idx = 0; idx < 2; idx += m_num_4x4_w) {
                m_tree_parser->set_default_intra_mode_variables(idx, idy);
                m_default_intra_mode = TRY_READ(m_tree_parser->parse_tree<IntraMode>(SyntaxElementType::DefaultIntraMode));
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
    m_uv_mode = TRY_READ(m_tree_parser->parse_tree<u8>(SyntaxElementType::DefaultUVMode));
    return {};
}

DecoderErrorOr<void> Parser::intra_segment_id()
{
    if (m_segmentation_enabled && m_segmentation_update_map)
        m_segment_id = TRY_READ(m_tree_parser->parse_tree<u8>(SyntaxElementType::SegmentID));
    else
        m_segment_id = 0;
    return {};
}

DecoderErrorOr<void> Parser::read_skip()
{
    if (seg_feature_active(SEG_LVL_SKIP))
        m_skip = true;
    else
        m_skip = TRY_READ(m_tree_parser->parse_tree<bool>(SyntaxElementType::Skip));
    return {};
}

bool Parser::seg_feature_active(u8 feature)
{
    return m_segmentation_enabled && m_feature_enabled[m_segment_id][feature];
}

DecoderErrorOr<void> Parser::read_tx_size(bool allow_select)
{
    m_max_tx_size = max_txsize_lookup[m_mi_size];
    if (allow_select && m_tx_mode == TXModeSelect && m_mi_size >= Block_8x8)
        m_tx_size = TRY_READ(m_tree_parser->parse_tree<TXSize>(SyntaxElementType::TXSize));
    else
        m_tx_size = min(m_max_tx_size, tx_mode_to_biggest_tx_size[m_tx_mode]);
    return {};
}

DecoderErrorOr<void> Parser::inter_frame_mode_info()
{
    m_left_ref_frame[0] = m_available_l ? m_ref_frames[get_image_index(m_mi_row, m_mi_col - 1)][0] : IntraFrame;
    m_above_ref_frame[0] = m_available_u ? m_ref_frames[get_image_index(m_mi_row - 1, m_mi_col)][0] : IntraFrame;
    m_left_ref_frame[1] = m_available_l ? m_ref_frames[get_image_index(m_mi_row, m_mi_col - 1)][1] : None;
    m_above_ref_frame[1] = m_available_u ? m_ref_frames[get_image_index(m_mi_row - 1, m_mi_col)][1] : None;
    m_left_intra = m_left_ref_frame[0] <= IntraFrame;
    m_above_intra = m_above_ref_frame[0] <= IntraFrame;
    m_left_single = m_left_ref_frame[1] <= None;
    m_above_single = m_above_ref_frame[1] <= None;
    TRY(inter_segment_id());
    TRY(read_skip());
    TRY(read_is_inter());
    TRY(read_tx_size(!m_skip || !m_is_inter));
    if (m_is_inter) {
        TRY(inter_block_mode_info());
    } else {
        TRY(intra_block_mode_info());
    }
    return {};
}

DecoderErrorOr<void> Parser::inter_segment_id()
{
    if (!m_segmentation_enabled) {
        m_segment_id = 0;
        return {};
    }
    auto predicted_segment_id = get_segment_id();
    if (!m_segmentation_update_map) {
        m_segment_id = predicted_segment_id;
        return {};
    }
    if (!m_segmentation_temporal_update) {
        m_segment_id = TRY_READ(m_tree_parser->parse_tree<u8>(SyntaxElementType::SegmentID));
        return {};
    }

    auto seg_id_predicted = TRY_READ(m_tree_parser->parse_tree<bool>(SyntaxElementType::SegIDPredicted));
    if (seg_id_predicted)
        m_segment_id = predicted_segment_id;
    else
        m_segment_id = TRY_READ(m_tree_parser->parse_tree<u8>(SyntaxElementType::SegmentID));
    for (size_t i = 0; i < num_8x8_blocks_wide_lookup[m_mi_size]; i++)
        m_above_seg_pred_context[m_mi_col + i] = seg_id_predicted;
    for (size_t i = 0; i < num_8x8_blocks_high_lookup[m_mi_size]; i++)
        m_left_seg_pred_context[m_mi_row + i] = seg_id_predicted;
    return {};
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

DecoderErrorOr<void> Parser::read_is_inter()
{
    if (seg_feature_active(SEG_LVL_REF_FRAME))
        m_is_inter = m_feature_data[m_segment_id][SEG_LVL_REF_FRAME] != IntraFrame;
    else
        m_is_inter = TRY_READ(m_tree_parser->parse_tree<bool>(SyntaxElementType::IsInter));
    return {};
}

DecoderErrorOr<void> Parser::intra_block_mode_info()
{
    m_ref_frame[0] = IntraFrame;
    m_ref_frame[1] = None;
    if (m_mi_size >= Block_8x8) {
        m_y_mode = TRY_READ(m_tree_parser->parse_tree<u8>(SyntaxElementType::IntraMode));
        for (auto& block_sub_mode : m_block_sub_modes)
            block_sub_mode = m_y_mode;
    } else {
        m_num_4x4_w = num_4x4_blocks_wide_lookup[m_mi_size];
        m_num_4x4_h = num_4x4_blocks_high_lookup[m_mi_size];
        u8 sub_intra_mode;
        for (auto idy = 0; idy < 2; idy += m_num_4x4_h) {
            for (auto idx = 0; idx < 2; idx += m_num_4x4_w) {
                sub_intra_mode = TRY_READ(m_tree_parser->parse_tree<u8>(SyntaxElementType::SubIntraMode));
                for (auto y = 0; y < m_num_4x4_h; y++) {
                    for (auto x = 0; x < m_num_4x4_w; x++)
                        m_block_sub_modes[(idy + y) * 2 + idx + x] = sub_intra_mode;
                }
            }
        }
        m_y_mode = sub_intra_mode;
    }
    m_uv_mode = TRY_READ(m_tree_parser->parse_tree<u8>(SyntaxElementType::UVMode));
    return {};
}

DecoderErrorOr<void> Parser::inter_block_mode_info()
{
    TRY(read_ref_frames());
    for (auto j = 0; j < 2; j++) {
        if (m_ref_frame[j] > IntraFrame) {
            TRY(find_mv_refs(m_ref_frame[j], -1));
            TRY(find_best_ref_mvs(j));
        }
    }
    auto is_compound = m_ref_frame[1] > IntraFrame;
    if (seg_feature_active(SEG_LVL_SKIP)) {
        m_y_mode = ZeroMv;
    } else if (m_mi_size >= Block_8x8) {
        auto inter_mode = TRY_READ(m_tree_parser->parse_tree(SyntaxElementType::InterMode));
        m_y_mode = NearestMv + inter_mode;
    }
    if (m_interpolation_filter == Switchable)
        m_interp_filter = TRY_READ(m_tree_parser->parse_tree<InterpolationFilter>(SyntaxElementType::InterpFilter));
    else
        m_interp_filter = m_interpolation_filter;
    if (m_mi_size < Block_8x8) {
        m_num_4x4_w = num_4x4_blocks_wide_lookup[m_mi_size];
        m_num_4x4_h = num_4x4_blocks_high_lookup[m_mi_size];
        for (auto idy = 0; idy < 2; idy += m_num_4x4_h) {
            for (auto idx = 0; idx < 2; idx += m_num_4x4_w) {
                auto inter_mode = TRY_READ(m_tree_parser->parse_tree(SyntaxElementType::InterMode));
                m_y_mode = NearestMv + inter_mode;
                if (m_y_mode == NearestMv || m_y_mode == NearMv) {
                    for (auto j = 0; j < 1 + is_compound; j++)
                        TRY(append_sub8x8_mvs(idy * 2 + idx, j));
                }
                TRY(assign_mv(is_compound));
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
        return {};
    }
    TRY(assign_mv(is_compound));
    for (auto ref_list = 0; ref_list < 1 + is_compound; ref_list++) {
        for (auto block = 0; block < 4; block++) {
            m_block_mvs[ref_list][block] = m_mv[ref_list];
        }
    }
    return {};
}

DecoderErrorOr<void> Parser::read_ref_frames()
{
    if (seg_feature_active(SEG_LVL_REF_FRAME)) {
        m_ref_frame[0] = static_cast<ReferenceFrame>(m_feature_data[m_segment_id][SEG_LVL_REF_FRAME]);
        m_ref_frame[1] = None;
        return {};
    }
    ReferenceMode comp_mode;
    if (m_reference_mode == ReferenceModeSelect)
        comp_mode = TRY_READ(m_tree_parser->parse_tree<ReferenceMode>(SyntaxElementType::CompMode));
    else
        comp_mode = m_reference_mode;
    if (comp_mode == CompoundReference) {
        auto idx = m_ref_frame_sign_bias[m_comp_fixed_ref];
        auto comp_ref = TRY_READ(m_tree_parser->parse_tree(SyntaxElementType::CompRef));
        m_ref_frame[idx] = m_comp_fixed_ref;
        m_ref_frame[!idx] = m_comp_var_ref[comp_ref];
        return {};
    }
    auto single_ref_p1 = TRY_READ(m_tree_parser->parse_tree<bool>(SyntaxElementType::SingleRefP1));
    if (single_ref_p1) {
        auto single_ref_p2 = TRY_READ(m_tree_parser->parse_tree<bool>(SyntaxElementType::SingleRefP2));
        m_ref_frame[0] = single_ref_p2 ? AltRefFrame : GoldenFrame;
    } else {
        m_ref_frame[0] = LastFrame;
    }
    m_ref_frame[1] = None;
    return {};
}

DecoderErrorOr<void> Parser::assign_mv(bool is_compound)
{
    m_mv[1] = {};
    for (auto i = 0; i < 1 + is_compound; i++) {
        if (m_y_mode == NewMv) {
            TRY(read_mv(i));
        } else if (m_y_mode == NearestMv) {
            m_mv[i] = m_nearest_mv[i];
        } else if (m_y_mode == NearMv) {
            m_mv[i] = m_near_mv[i];
        } else {
            m_mv[i] = {};
        }
    }
    return {};
}

DecoderErrorOr<void> Parser::read_mv(u8 ref)
{
    m_use_hp = m_allow_high_precision_mv && TRY(use_mv_hp(m_best_mv[ref]));
    MotionVector diff_mv;
    auto mv_joint = TRY_READ(m_tree_parser->parse_tree<MvJoint>(SyntaxElementType::MVJoint));
    if (mv_joint == MvJointHzvnz || mv_joint == MvJointHnzvnz)
        diff_mv.set_row(TRY(read_mv_component(0)));
    if (mv_joint == MvJointHnzvz || mv_joint == MvJointHnzvnz)
        diff_mv.set_column(TRY(read_mv_component(1)));
    m_mv[ref] = m_best_mv[ref] + diff_mv;
    return {};
}

DecoderErrorOr<i32> Parser::read_mv_component(u8)
{
    auto mv_sign = TRY_READ(m_tree_parser->parse_tree<bool>(SyntaxElementType::MVSign));
    auto mv_class = TRY_READ(m_tree_parser->parse_tree<MvClass>(SyntaxElementType::MVClass));
    u32 mag;
    if (mv_class == MvClass0) {
        auto mv_class0_bit = TRY_READ(m_tree_parser->parse_tree<u32>(SyntaxElementType::MVClass0Bit));
        auto mv_class0_fr = TRY_READ(m_tree_parser->parse_tree<u32>(SyntaxElementType::MVClass0FR));
        auto mv_class0_hp = TRY_READ(m_tree_parser->parse_tree<u32>(SyntaxElementType::MVClass0HP));
        mag = ((mv_class0_bit << 3) | (mv_class0_fr << 1) | mv_class0_hp) + 1;
    } else {
        auto d = 0;
        for (size_t i = 0; i < mv_class; i++) {
            auto mv_bit = TRY_READ(m_tree_parser->parse_tree<bool>(SyntaxElementType::MVBit));
            d |= mv_bit << i;
        }
        mag = CLASS0_SIZE << (mv_class + 2);
        auto mv_fr = TRY_READ(m_tree_parser->parse_tree<u32>(SyntaxElementType::MVFR));
        auto mv_hp = TRY_READ(m_tree_parser->parse_tree<u32>(SyntaxElementType::MVHP));
        mag += ((d << 3) | (mv_fr << 1) | mv_hp) + 1;
    }
    return (mv_sign ? -1 : 1) * static_cast<i32>(mag);
}

Gfx::Point<size_t> Parser::get_decoded_point_for_plane(u8 column, u8 row, u8 plane)
{
    if (plane == 0)
        return { column * 8, row * 8 };
    return { (column * 8) >> m_subsampling_x, (row * 8) >> m_subsampling_y };
}

Gfx::Size<size_t> Parser::get_decoded_size_for_plane(u8 plane)
{
    auto point = get_decoded_point_for_plane(m_mi_cols, m_mi_rows, plane);
    return { point.x(), point.y() };
}

DecoderErrorOr<void> Parser::residual()
{
    auto block_size = m_mi_size < Block_8x8 ? Block_8x8 : static_cast<BlockSubsize>(m_mi_size);
    for (u8 plane = 0; plane < 3; plane++) {
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
                        TRY(m_decoder.predict_inter(plane, base_x + (4 * x), base_y + (4 * y), 4, 4, (y * num_4x4_w) + x));
                    }
                }
            } else {
                TRY(m_decoder.predict_inter(plane, base_x, base_y, num_4x4_w * 4, num_4x4_h * 4, 0));
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
                        TRY(m_decoder.predict_intra(plane, start_x, start_y, m_available_l || x > 0, m_available_u || y > 0, (x + step) < num_4x4_w, tx_size, block_index));
                    if (!m_skip) {
                        non_zero = TRY(tokens(plane, start_x, start_y, tx_size, block_index));
                        TRY(m_decoder.reconstruct(plane, start_x, start_y, tx_size));
                    }
                }
                auto& above_sub_context = m_above_nonzero_context[plane];
                auto& left_sub_context = m_left_nonzero_context[plane];
                for (auto i = 0; i < step; i++) {
                    above_sub_context[(start_x >> 2) + i] = non_zero;
                    left_sub_context[(start_y >> 2) + i] = non_zero;
                }
                block_index++;
            }
        }
    }
    return {};
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

DecoderErrorOr<bool> Parser::tokens(size_t plane, u32 start_x, u32 start_y, TXSize tx_size, u32 block_index)
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
            auto more_coefs = TRY_READ(m_tree_parser->parse_tree<bool>(SyntaxElementType::MoreCoefs));
            if (!more_coefs)
                break;
        }
        auto token = TRY_READ(m_tree_parser->parse_tree<Token>(SyntaxElementType::Token));
        m_token_cache[pos] = energy_class[token];
        if (token == ZeroToken) {
            m_tokens[pos] = 0;
            check_eob = false;
        } else {
            i32 coef = TRY(read_coef(token));
            auto sign_bit = TRY_READ(m_bit_stream->read_literal(1));
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

DecoderErrorOr<i32> Parser::read_coef(Token token)
{
    auto cat = extra_bits[token][0];
    auto num_extra = extra_bits[token][1];
    u32 coef = extra_bits[token][2];
    if (token == DctValCat6) {
        for (size_t e = 0; e < (u8)(m_bit_depth - 8); e++) {
            auto high_bit = TRY_READ(m_bit_stream->read_bool(255));
            coef += high_bit << (5 + m_bit_depth - e);
        }
    }
    for (size_t e = 0; e < num_extra; e++) {
        auto coef_bit = TRY_READ(m_bit_stream->read_bool(cat_probs[cat][e]));
        coef += coef_bit << (num_extra - 1 - e);
    }
    return coef;
}

DecoderErrorOr<void> Parser::find_mv_refs(ReferenceFrame, int)
{
    // TODO: Implement
    return DecoderError::not_implemented();
}

DecoderErrorOr<void> Parser::find_best_ref_mvs(int)
{
    // TODO: Implement
    return DecoderError::not_implemented();
}

DecoderErrorOr<void> Parser::append_sub8x8_mvs(u8, u8)
{
    // TODO: Implement
    return DecoderError::not_implemented();
}

DecoderErrorOr<bool> Parser::use_mv_hp(const MotionVector&)
{
    // TODO: Implement
    return DecoderError::not_implemented();
}

void Parser::dump_info()
{
    outln("Frame dimensions: {}x{}", m_frame_width, m_frame_height);
    outln("Render dimensions: {}x{}", m_render_width, m_render_height);
    outln("Bit depth: {}", m_bit_depth);
}

}
