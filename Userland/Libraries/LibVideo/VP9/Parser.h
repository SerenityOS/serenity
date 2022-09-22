/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/ByteBuffer.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <LibGfx/Forward.h>
#include <LibVideo/DecoderError.h>

#include "BitStream.h"
#include "LookupTables.h"
#include "MotionVector.h"
#include "ProbabilityTables.h"
#include "SyntaxElementCounter.h"
#include "TreeParser.h"

namespace Video::VP9 {

class Decoder;

class Parser {
    friend class TreeParser;
    friend class Decoder;

public:
    explicit Parser(Decoder&);
    ~Parser();
    DecoderErrorOr<void> parse_frame(ByteBuffer const&);
    void dump_info();

private:
    DecoderErrorOr<FrameType> read_frame_type();
    DecoderErrorOr<ColorRange> read_color_range();

    /* Utilities */
    template<typename T>
    void clear_context(Vector<T>& context, size_t size);
    template<typename T>
    void clear_context(Vector<Vector<T>>& context, size_t outer_size, size_t inner_size);
    DecoderErrorOr<void> allocate_tile_data();
    void cleanup_tile_allocations();

    /* (6.1) Frame Syntax */
    bool trailing_bits();
    DecoderErrorOr<void> refresh_probs();

    /* (6.2) Uncompressed Header Syntax */
    DecoderErrorOr<void> uncompressed_header();
    DecoderErrorOr<void> frame_sync_code();
    DecoderErrorOr<void> color_config();
    DecoderErrorOr<void> frame_size();
    DecoderErrorOr<void> render_size();
    DecoderErrorOr<void> frame_size_with_refs();
    void compute_image_size();
    DecoderErrorOr<void> read_interpolation_filter();
    DecoderErrorOr<void> loop_filter_params();
    DecoderErrorOr<void> quantization_params();
    DecoderErrorOr<i8> read_delta_q();
    DecoderErrorOr<void> segmentation_params();
    DecoderErrorOr<u8> read_prob();
    DecoderErrorOr<void> tile_info();
    u16 calc_min_log2_tile_cols();
    u16 calc_max_log2_tile_cols();
    void setup_past_independence();

    /* (6.3) Compressed Header Syntax */
    DecoderErrorOr<void> compressed_header();
    DecoderErrorOr<void> read_tx_mode();
    DecoderErrorOr<void> tx_mode_probs();
    DecoderErrorOr<u8> diff_update_prob(u8 prob);
    DecoderErrorOr<u8> decode_term_subexp();
    u8 inv_remap_prob(u8 delta_prob, u8 prob);
    u8 inv_recenter_nonneg(u8 v, u8 m);
    DecoderErrorOr<void> read_coef_probs();
    DecoderErrorOr<void> read_skip_prob();
    DecoderErrorOr<void> read_inter_mode_probs();
    DecoderErrorOr<void> read_interp_filter_probs();
    DecoderErrorOr<void> read_is_inter_probs();
    DecoderErrorOr<void> frame_reference_mode();
    DecoderErrorOr<void> frame_reference_mode_probs();
    DecoderErrorOr<void> read_y_mode_probs();
    DecoderErrorOr<void> read_partition_probs();
    DecoderErrorOr<void> mv_probs();
    DecoderErrorOr<u8> update_mv_prob(u8 prob);
    void setup_compound_reference_mode();

    /* (6.4) Decode Tiles Syntax */
    DecoderErrorOr<void> decode_tiles();
    void clear_above_context();
    u32 get_tile_offset(u32 tile_num, u32 mis, u32 tile_size_log2);
    DecoderErrorOr<void> decode_tile();
    void clear_left_context();
    DecoderErrorOr<void> decode_partition(u32 row, u32 col, u8 block_subsize);
    DecoderErrorOr<void> decode_block(u32 row, u32 col, BlockSubsize subsize);
    DecoderErrorOr<void> mode_info();
    DecoderErrorOr<void> intra_frame_mode_info();
    DecoderErrorOr<void> intra_segment_id();
    DecoderErrorOr<void> read_skip();
    bool seg_feature_active(u8 feature);
    DecoderErrorOr<void> read_tx_size(bool allow_select);
    DecoderErrorOr<void> inter_frame_mode_info();
    DecoderErrorOr<void> inter_segment_id();
    u8 get_segment_id();
    DecoderErrorOr<void> read_is_inter();
    DecoderErrorOr<void> intra_block_mode_info();
    DecoderErrorOr<void> inter_block_mode_info();
    DecoderErrorOr<void> read_ref_frames();
    DecoderErrorOr<void> assign_mv(bool is_compound);
    DecoderErrorOr<void> read_mv(u8 ref);
    DecoderErrorOr<i32> read_mv_component(u8 component);
    DecoderErrorOr<void> residual();
    TXSize get_uv_tx_size();
    BlockSubsize get_plane_block_size(u32 subsize, u8 plane);
    DecoderErrorOr<bool> tokens(size_t plane, u32 x, u32 y, TXSize tx_size, u32 block_index);
    u32 const* get_scan(size_t plane, TXSize tx_size, u32 block_index);
    DecoderErrorOr<i32> read_coef(Token token);

    /* (6.5) Motion Vector Prediction */
    void find_mv_refs(ReferenceFrame, i32 block);
    void find_best_ref_mvs(u8 ref_list);
    bool use_mv_hp(MotionVector const& delta_mv);
    void append_sub8x8_mvs(i32 block, u8 ref_list);
    bool is_inside(i32 row, i32 column);
    void clamp_mv_ref(u8 i);
    MotionVector clamp_mv(MotionVector mvec, i32 border);
    size_t get_image_index(u32 row, u32 column);
    void get_block_mv(u32 candidate_row, u32 candidate_column, u8 ref_list, bool use_prev);
    void if_same_ref_frame_add_mv(u32 candidate_row, u32 candidate_column, ReferenceFrame ref_frame, bool use_prev);
    void if_diff_ref_frame_add_mv(u32 candidate_row, u32 candidate_column, ReferenceFrame ref_frame, bool use_prev);
    void scale_mv(u8 ref_list, ReferenceFrame ref_frame);
    void add_mv_ref_list(u8 ref_list);

    Gfx::Point<size_t> get_decoded_point_for_plane(u8 row, u8 column, u8 plane);
    Gfx::Size<size_t> get_decoded_size_for_plane(u8 plane);

    u8 m_profile { 0 };
    bool m_show_existing_frame { false };
    u8 m_frame_to_show_map_index { 0 };
    u16 m_header_size_in_bytes { 0 };
    u8 m_refresh_frame_flags { 0 };
    u8 m_loop_filter_level { 0 };
    u8 m_loop_filter_sharpness { 0 };
    bool m_loop_filter_delta_enabled { false };
    FrameType m_frame_type { FrameType::KeyFrame };
    FrameType m_last_frame_type { FrameType::KeyFrame };
    bool m_show_frame { false };
    bool m_prev_show_frame { false };
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
    InterpolationFilter m_interpolation_filter { 0xf };
    u8 m_base_q_idx { 0 };
    i8 m_delta_q_y_dc { 0 };
    i8 m_delta_q_uv_dc { 0 };
    i8 m_delta_q_uv_ac { 0 };
    bool m_lossless { false };
    u8 m_segmentation_tree_probs[7];
    u8 m_segmentation_pred_prob[3];
    bool m_feature_enabled[8][4];
    u8 m_feature_data[8][4];
    bool m_segmentation_enabled { false };
    bool m_segmentation_update_map { false };
    bool m_segmentation_temporal_update { false };
    bool m_segmentation_abs_or_delta_update { false };
    u16 m_tile_cols_log2 { 0 };
    u16 m_tile_rows_log2 { 0 };
    i8 m_loop_filter_ref_deltas[MAX_REF_FRAMES];
    i8 m_loop_filter_mode_deltas[2];

    // FIXME: Move above and left contexts to structs
    Array<Vector<bool>, 3> m_above_nonzero_context;
    Array<Vector<bool>, 3> m_left_nonzero_context;
    Vector<u8> m_above_seg_pred_context;
    Vector<u8> m_left_seg_pred_context;
    Vector<u8> m_above_partition_context;
    Vector<u8> m_left_partition_context;

    // FIXME: Move (some?) mi_.. to an array of struct since they are usually used together.
    u32 m_mi_row_start { 0 };
    u32 m_mi_row_end { 0 };
    u32 m_mi_col_start { 0 };
    u32 m_mi_col_end { 0 };
    u32 m_mi_row { 0 };
    u32 m_mi_col { 0 };
    BlockSubsize m_mi_size { 0 };
    bool m_available_u { false };
    bool m_available_l { false };
    u8 m_segment_id { 0 };
    // FIXME: Should this be an enum?
    // skip equal to 0 indicates that there may be some transform coefficients to read for this block; skip equal to 1
    // indicates that there are no transform coefficients.
    //
    // skip may be set to 0 even if transform blocks contain immediate end of block markers.
    bool m_skip { false };
    u8 m_num_8x8 { 0 };
    bool m_has_rows { false };
    bool m_has_cols { false };
    TXSize m_max_tx_size { TX_4x4 };
    u8 m_block_subsize { 0 };
    // The row to use for getting partition tree probability lookups.
    u32 m_row { 0 };
    // The column to use for getting partition tree probability lookups.
    u32 m_col { 0 };
    TXSize m_tx_size { TX_4x4 };
    ReferenceFrame m_ref_frame[2];
    bool m_is_inter { false };
    bool m_is_compound { false };
    IntraMode m_default_intra_mode { DcPred };
    u8 m_y_mode { 0 };
    u8 m_block_sub_modes[4];
    u8 m_num_4x4_w { 0 };
    u8 m_num_4x4_h { 0 };
    u8 m_uv_mode { 0 }; // FIXME: Is u8 the right size?
    Vector<Array<IntraMode, 4>> m_sub_modes;
    ReferenceFrame m_left_ref_frame[2];
    ReferenceFrame m_above_ref_frame[2];
    bool m_left_intra { false };
    bool m_above_intra { false };
    bool m_left_single { false };
    bool m_above_single { false };
    // The current block's interpolation filter.
    InterpolationFilter m_interp_filter { EightTap };
    MotionVector m_mv[2];
    MotionVector m_near_mv[2];
    MotionVector m_nearest_mv[2];
    MotionVector m_best_mv[2];
    // FIXME: Move these to a struct to store together in one array.
    u32 m_ref_frame_width[NUM_REF_FRAMES];
    u32 m_ref_frame_height[NUM_REF_FRAMES];
    bool m_ref_subsampling_x[NUM_REF_FRAMES];
    bool m_ref_subsampling_y[NUM_REF_FRAMES];
    u8 m_ref_bit_depth[NUM_REF_FRAMES];

    Vector<u16> m_frame_store[NUM_REF_FRAMES][3];

    u32 m_eob_total { 0 };
    u8 m_tx_type { 0 };
    u8 m_token_cache[1024];
    i32 m_tokens[1024];
    bool m_use_hp { false };
    TXMode m_tx_mode;
    ReferenceMode m_reference_mode;
    ReferenceFrame m_comp_fixed_ref;
    ReferenceFrame m_comp_var_ref[2];
    MotionVector m_block_mvs[2][4];
    Vector<u8> m_prev_segment_ids;

    Vector<bool> m_skips;
    Vector<TXSize> m_tx_sizes;
    Vector<u32> m_mi_sizes;
    Vector<u8> m_y_modes;
    Vector<u8> m_segment_ids;
    Vector<Array<ReferenceFrame, 2>> m_ref_frames;
    Vector<Array<ReferenceFrame, 2>> m_prev_ref_frames;
    Vector<Array<MotionVector, 2>> m_mvs;
    Vector<Array<MotionVector, 2>> m_prev_mvs;
    MotionVector m_candidate_mv[2];
    ReferenceFrame m_candidate_frame[2];
    Vector<Array<Array<MotionVector, 4>, 2>> m_sub_mvs;
    u8 m_ref_mv_count { 0 };
    MotionVector m_ref_list_mv[2];
    bool m_use_prev_frame_mvs;
    Vector<InterpolationFilter> m_interp_filters;
    // Indexed by ReferenceFrame enum.
    u8 m_mode_context[4] { INVALID_CASE };

    OwnPtr<BitStream> m_bit_stream;
    OwnPtr<ProbabilityTables> m_probability_tables;
    OwnPtr<SyntaxElementCounter> m_syntax_element_counter;
    NonnullOwnPtr<TreeParser> m_tree_parser;
    Decoder& m_decoder;
};

}
