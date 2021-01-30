/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "BitStream.h"
#include "LookupTables.h"
#include "ProbabilityTables.h"
#include "SyntaxElementCounter.h"
#include <AK/ByteBuffer.h>
#include <AK/OwnPtr.h>

namespace Video::VP9 {

class Decoder {
public:
    Decoder();
    bool parse_frame(const ByteBuffer&);
    void dump_info();

private:
    FrameType read_frame_type()
    {
        if (m_bit_stream->read_bit())
            return NonKeyFrame;
        return KeyFrame;
    }

    ColorRange read_color_range()
    {
        if (m_bit_stream->read_bit())
            return FullSwing;
        return StudioSwing;
    }

    bool uncompressed_header();
    bool frame_sync_code();
    bool color_config();
    bool frame_size();
    bool render_size();
    bool frame_size_with_refs();
    bool compute_image_size();
    bool read_interpolation_filter();
    bool loop_filter_params();
    bool quantization_params();
    i8 read_delta_q();
    bool segmentation_params();
    u8 read_prob();
    bool tile_info();
    u16 calc_min_log2_tile_cols();
    u16 calc_max_log2_tile_cols();
    bool setup_past_independence();
    bool trailing_bits();

    bool compressed_header();
    bool read_tx_mode();
    bool tx_mode_probs();
    u8 diff_update_prob(u8 prob);
    u8 decode_term_subexp();
    u8 inv_remap_prob(u8 delta_prob, u8 prob);
    u8 inv_recenter_nonneg(u8 v, u8 m);
    bool read_coef_probs();
    bool read_skip_prob();
    bool read_inter_mode_probs();
    bool read_interp_filter_probs();
    bool read_is_inter_probs();
    bool frame_reference_mode();
    bool frame_reference_mode_probs();
    bool read_y_mode_probs();
    bool read_partition_probs();
    bool mv_probs();
    u8 update_mv_prob(u8 prob);
    bool setup_compound_reference_mode();

    bool decode_tiles();
    bool clear_above_context();
    u32 get_tile_offset(u32 tile_num, u32 mis, u32 tile_size_log2);
    bool decode_tile();
    bool clear_left_context();
    bool decode_partition(u32 row, u32 col, u8 block_subsize);

    u8 m_profile { 0 };
    u8 m_frame_to_show_map_index { 0 };
    u16 m_header_size_in_bytes { 0 };
    u8 m_refresh_frame_flags { 0 };
    u8 m_loop_filter_level { 0 };
    u8 m_loop_filter_sharpness { 0 };
    bool m_loop_filter_delta_enabled { false };
    FrameType m_frame_type;
    FrameType m_last_frame_type;
    bool m_show_frame { false };
    bool m_error_resilient_mode { false };
    bool m_frame_is_intra { false };
    u8 m_reset_frame_context { 0 };
    bool m_allow_high_precision_mv { false };
    u8 m_ref_frame_idx[3];
    u8 m_ref_frame_sign_bias[LastFrame + 3];
    bool m_refresh_frame_context { false };
    bool m_frame_parallel_decoding_mode { false };
    u8 m_frame_context_idx { 0 };
    u8 m_bit_depth { 0 };
    ColorSpace m_color_space;
    ColorRange m_color_range;
    bool m_subsampling_x { false };
    bool m_subsampling_y { false };
    u32 m_frame_width { 0 };
    u32 m_frame_height { 0 };
    u16 m_render_width { 0 };
    u16 m_render_height { 0 };
    bool m_render_and_frame_size_different { false };
    u32 m_mi_cols { 0 };
    u32 m_mi_rows { 0 };
    u32 m_sb64_cols { 0 };
    u32 m_sb64_rows { 0 };
    InterpolationFilter m_interpolation_filter;
    bool m_lossless { false };
    u8 m_segmentation_tree_probs[7];
    u8 m_segmentation_pred_prob[3];
    bool m_feature_enabled[8][4];
    u8 m_feature_data[8][4];
    bool m_segmentation_abs_or_delta_update { false };
    u16 m_tile_cols_log2 { 0 };
    u16 m_tile_rows_log2 { 0 };
    i8 m_loop_filter_ref_deltas[MAX_REF_FRAMES];
    i8 m_loop_filter_mode_deltas[2];

    u32 m_mi_row_start { 0 };
    u32 m_mi_row_end { 0 };
    u32 m_mi_col_start { 0 };
    u32 m_mi_col_end { 0 };

    TXMode m_tx_mode;
    ReferenceMode m_reference_mode;
    ReferenceFrame m_comp_fixed_ref;
    ReferenceFrame m_comp_var_ref[2];

    OwnPtr<BitStream> m_bit_stream;
    OwnPtr<ProbabilityTables> m_probability_tables;
    OwnPtr<SyntaxElementCounter> m_syntax_element_counter;
};

}
