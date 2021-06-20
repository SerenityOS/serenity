/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Decoder.h"

namespace Video::VP9 {

#define RESERVED_ZERO                  \
    if (m_bit_stream->read_bit() != 0) \
    return false

Decoder::Decoder()
{
    m_probability_tables = make<ProbabilityTables>();
}

bool Decoder::parse_frame(const ByteBuffer& frame_data)
{
    m_bit_stream = make<BitStream>(frame_data.data(), frame_data.size());
    m_syntax_element_counter = make<SyntaxElementCounter>();

    m_start_bit_pos = m_bit_stream->get_position();
    if (!uncompressed_header())
        return false;
    dbgln("Finished reading uncompressed header");
    if (!trailing_bits())
        return false;
    if (m_header_size_in_bytes == 0) {
        // FIXME: Do we really need to read all of these bits?
        // while (m_bit_stream->get_position() < m_start_bit_pos + (8 * frame_data.size()))
        //     RESERVED_ZERO;
        dbgln("No header");
        return true;
    }
    m_probability_tables->load_probs(m_frame_context_idx);
    m_probability_tables->load_probs2(m_frame_context_idx);
    m_syntax_element_counter->clear_counts();
    return true;
}

bool Decoder::uncompressed_header()
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
        if (!frame_sync_code())
            return false;
        if (!color_config())
            return false;
        if (!frame_size())
            return false;
        if (!render_size())
            return false;
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
            if (!frame_sync_code())
                return false;
            if (m_profile > 0) {
                if (!color_config())
                    return false;
            } else {
                m_color_space = Bt601;
                m_subsampling_x = true;
                m_subsampling_y = true;
                m_bit_depth = 8;
            }

            m_refresh_frame_flags = m_bit_stream->read_f8();
            if (!frame_size())
                return false;
            if (!render_size())
                return false;
        } else {
            m_refresh_frame_flags = m_bit_stream->read_f8();
            for (auto i = 0; i < 3; i++) {
                m_ref_frame_idx[i] = m_bit_stream->read_f(3);
                m_ref_frame_sign_bias[LastFrame + i] = m_bit_stream->read_bit();
            }
            frame_size_with_refs();
            m_allow_high_precision_mv = m_bit_stream->read_bit();
            read_interpolation_filter();
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

    loop_filter_params();
    quantization_params();
    segmentation_params();
    tile_info();

    m_header_size_in_bytes = m_bit_stream->read_f16();

    return true;
}

bool Decoder::frame_sync_code()
{
    if (m_bit_stream->read_byte() != 0x49)
        return false;
    if (m_bit_stream->read_byte() != 0x83)
        return false;
    return m_bit_stream->read_byte() == 0x42;
}

bool Decoder::color_config()
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

bool Decoder::frame_size()
{
    m_frame_width = m_bit_stream->read_f16() + 1;
    m_frame_height = m_bit_stream->read_f16() + 1;
    compute_image_size();
    return true;
}

bool Decoder::render_size()
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

bool Decoder::frame_size_with_refs()
{
    bool found_ref;
    for (auto i = 0; i < 3; i++) {
        found_ref = m_bit_stream->read_bit();
        if (found_ref) {
            // TODO:
            //  - FrameWidth = RefFrameWidth[ref_frame_idx[ i] ];
            //  - FrameHeight = RefFrameHeight[ref_frame_idx[ i] ];
            break;
        }
    }

    if (!found_ref)
        frame_size();
    else
        compute_image_size();

    render_size();
    return true;
}

bool Decoder::compute_image_size()
{
    m_mi_cols = (m_frame_width + 7u) >> 3u;
    m_mi_rows = (m_frame_height + 7u) >> 3u;
    m_sb64_cols = (m_mi_cols + 7u) >> 3u;
    m_sb64_rows = (m_mi_rows + 7u) >> 3u;
    return true;
}

bool Decoder::read_interpolation_filter()
{
    if (m_bit_stream->read_bit()) {
        m_interpolation_filter = Switchable;
    } else {
        m_interpolation_filter = literal_to_type[m_bit_stream->read_f(2)];
    }
    return true;
}

bool Decoder::loop_filter_params()
{
    m_loop_filter_level = m_bit_stream->read_f(6);
    m_loop_filter_sharpness = m_bit_stream->read_f(3);
    m_loop_filter_delta_enabled = m_bit_stream->read_bit();
    if (m_loop_filter_delta_enabled) {
        if (m_bit_stream->read_bit()) {
            for (auto i = 0; i < 4; i++) {
                if (m_bit_stream->read_bit()) {
                    // TODO: loop_filter_ref_deltas[i] = s(6);
                }
            }
            for (auto i = 0; i < 2; i++) {
                if (m_bit_stream->read_bit()) {
                    // TODO: loop_filter_mode_deltas[i] = s(6);
                }
            }
        }
    }
    return true;
}

bool Decoder::quantization_params()
{
    auto base_q_idx = m_bit_stream->read_byte();
    auto delta_q_y_dc = read_delta_q();
    auto delta_q_uv_dc = read_delta_q();
    auto delta_q_uv_ac = read_delta_q();
    m_lossless = base_q_idx == 0 && delta_q_y_dc == 0 && delta_q_uv_dc == 0 && delta_q_uv_ac == 0;
    return true;
}

i8 Decoder::read_delta_q()
{
    if (m_bit_stream->read_bit())
        return m_bit_stream->read_s(4);
    return 0;
}

bool Decoder::segmentation_params()
{
    auto segmentation_enabled = m_bit_stream->read_bit();
    if (!segmentation_enabled)
        return true;

    auto segmentation_update_map = m_bit_stream->read_bit();
    if (segmentation_update_map) {
        for (auto i = 0; i < 7; i++) {
            m_segmentation_tree_probs[i] = read_prob();
        }
        auto segmentation_temporal_update = m_bit_stream->read_bit();
        for (auto i = 0; i < 3; i++) {
            m_segmentation_pred_prob[i] = segmentation_temporal_update ? read_prob() : 255;
        }
    }

    if (!m_bit_stream->read_bit())
        return true;

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

u8 Decoder::read_prob()
{
    if (m_bit_stream->read_bit())
        return m_bit_stream->read_byte();
    return 255;
}

bool Decoder::tile_info()
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

u16 Decoder::calc_min_log2_tile_cols()
{
    auto min_log_2 = 0;
    while ((MAX_TILE_WIDTH_B64 << min_log_2) < m_sb64_cols)
        min_log_2++;
    return min_log_2;
}

u16 Decoder::calc_max_log2_tile_cols()
{
    u16 max_log_2 = 1;
    while ((m_sb64_cols >> max_log_2) >= MIN_TILE_WIDTH_B64)
        max_log_2++;
    return max_log_2 - 1;
}

bool Decoder::setup_past_independence()
{
    for (auto i = 0; i < 8; i++) {
        for (auto j = 0; j < 4; j++) {
            m_feature_data[i][j] = 0;
            m_feature_enabled[i][j] = false;
        }
    }
    m_segmentation_abs_or_delta_update = false;
    for (auto row = 0; row < m_mi_rows; row++) {
        for (auto col = 0; col < m_mi_cols; col++) {
            // TODO: m_prev_segment_ids[row][col] = 0;
        }
    }
    m_loop_filter_delta_enabled = true;
    m_loop_filter_ref_deltas[IntraFrame] = 1;
    m_loop_filter_ref_deltas[LastFrame] = 0;
    m_loop_filter_ref_deltas[GoldenFrame] = -1;
    m_loop_filter_ref_deltas[AltRefFrame] = -1;
    for (auto i = 0; i < 2; i++) {
        m_loop_filter_mode_deltas[i] = 0;
    }
    m_probability_tables->reset_probs();
    return true;
}

bool Decoder::trailing_bits()
{
    while (m_bit_stream->get_position() & 7u)
        RESERVED_ZERO;
    return true;
}

void Decoder::dump_info()
{
    dbgln("Frame dimensions: {}x{}", m_frame_width, m_frame_height);
    dbgln("Render dimensions: {}x{}", m_render_width, m_render_height);
    dbgln("Bit depth: {}", m_bit_depth);
    dbgln("Interpolation filter: {}", (u8)m_interpolation_filter);
}

}
