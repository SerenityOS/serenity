/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/OwnPtr.h>
#include <AK/Span.h>
#include <AK/Vector.h>
#include <LibGfx/Size.h>
#include <LibVideo/Color/CodingIndependentCodePoints.h>
#include <LibVideo/DecoderError.h>

#include "BitStream.h"
#include "ContextStorage.h"
#include "LookupTables.h"
#include "MotionVector.h"
#include "ProbabilityTables.h"
#include "SyntaxElementCounter.h"
#include "TreeParser.h"

namespace Video::VP9 {

class Decoder;

struct FrameContext;
struct TileContext;
struct BlockContext;
struct MotionVectorCandidate;

class Parser {
    friend class TreeParser;
    friend class Decoder;

public:
    explicit Parser(Decoder&);
    ~Parser();
    DecoderErrorOr<FrameContext> parse_frame(ReadonlyBytes);

private:
    /* Annex B: Superframes are a method of storing multiple coded frames into a single chunk
     * See also section 5.26. */
    Vector<size_t> parse_superframe_sizes(ReadonlyBytes);

    DecoderErrorOr<FrameType> read_frame_type();
    DecoderErrorOr<ColorRange> read_color_range();

    /* (6.1) Frame Syntax */
    bool trailing_bits();
    DecoderErrorOr<void> refresh_probs(FrameContext const&);

    /* (6.2) Uncompressed Header Syntax */
    DecoderErrorOr<FrameContext> uncompressed_header();
    DecoderErrorOr<void> frame_sync_code();
    DecoderErrorOr<ColorConfig> parse_color_config(FrameContext const&);
    DecoderErrorOr<void> set_frame_size_and_compute_image_size();
    DecoderErrorOr<Gfx::Size<u32>> parse_frame_size();
    DecoderErrorOr<Gfx::Size<u32>> parse_frame_size_with_refs(Array<u8, 3> const& reference_indices);
    DecoderErrorOr<Gfx::Size<u32>> parse_render_size(Gfx::Size<u32> frame_size);
    DecoderErrorOr<void> compute_image_size(FrameContext&);
    DecoderErrorOr<InterpolationFilter> read_interpolation_filter();
    DecoderErrorOr<void> loop_filter_params(FrameContext&);
    DecoderErrorOr<void> quantization_params(FrameContext&);
    DecoderErrorOr<i8> read_delta_q();
    DecoderErrorOr<void> segmentation_params(FrameContext&);
    DecoderErrorOr<u8> read_prob();
    DecoderErrorOr<void> parse_tile_counts(FrameContext&);
    void setup_past_independence();

    /* (6.3) Compressed Header Syntax */
    DecoderErrorOr<void> compressed_header(FrameContext&);
    DecoderErrorOr<TransformMode> read_tx_mode(FrameContext const&);
    DecoderErrorOr<void> tx_mode_probs();
    DecoderErrorOr<u8> diff_update_prob(u8 prob);
    DecoderErrorOr<u8> decode_term_subexp();
    u8 inv_remap_prob(u8 delta_prob, u8 prob);
    u8 inv_recenter_nonneg(u8 v, u8 m);
    DecoderErrorOr<void> read_coef_probs(TransformMode);
    DecoderErrorOr<void> read_skip_prob();
    DecoderErrorOr<void> read_inter_mode_probs();
    DecoderErrorOr<void> read_interp_filter_probs();
    DecoderErrorOr<void> read_is_inter_probs();
    DecoderErrorOr<void> frame_reference_mode(FrameContext&);
    DecoderErrorOr<void> frame_reference_mode_probs(FrameContext const&);
    DecoderErrorOr<void> read_y_mode_probs();
    DecoderErrorOr<void> read_partition_probs();
    DecoderErrorOr<void> mv_probs(FrameContext const&);
    DecoderErrorOr<u8> update_mv_prob(u8 prob);

    /* (6.4) Decode Tiles Syntax */
    DecoderErrorOr<void> decode_tiles(FrameContext&);
    u32 get_tile_offset(u32 tile_num, u32 mis, u32 tile_size_log2);
    DecoderErrorOr<void> decode_tile(TileContext&);
    void clear_left_context(TileContext&);
    DecoderErrorOr<void> decode_partition(TileContext&, u32 row, u32 column, BlockSubsize subsize);
    DecoderErrorOr<void> decode_block(TileContext&, u32 row, u32 column, BlockSubsize subsize);
    DecoderErrorOr<void> mode_info(BlockContext&, FrameBlockContext above_context, FrameBlockContext left_context);
    DecoderErrorOr<void> intra_frame_mode_info(BlockContext&, FrameBlockContext above_context, FrameBlockContext left_context);
    DecoderErrorOr<void> set_intra_segment_id(BlockContext&);
    DecoderErrorOr<bool> read_should_skip_residuals(BlockContext&, FrameBlockContext above_context, FrameBlockContext left_context);
    static bool seg_feature_active(BlockContext const&, u8 feature);
    DecoderErrorOr<TransformSize> read_tx_size(BlockContext&, FrameBlockContext above_context, FrameBlockContext left_context, bool allow_select);
    DecoderErrorOr<void> inter_frame_mode_info(BlockContext&, FrameBlockContext above_context, FrameBlockContext left_context);
    DecoderErrorOr<void> set_inter_segment_id(BlockContext&);
    u8 get_segment_id(BlockContext const&);
    DecoderErrorOr<bool> read_is_inter(BlockContext&, FrameBlockContext above_context, FrameBlockContext left_context);
    DecoderErrorOr<void> intra_block_mode_info(BlockContext&);
    DecoderErrorOr<void> inter_block_mode_info(BlockContext&, FrameBlockContext above_context, FrameBlockContext left_context);
    DecoderErrorOr<void> read_ref_frames(BlockContext&, FrameBlockContext above_context, FrameBlockContext left_context);
    DecoderErrorOr<MotionVectorPair> get_motion_vector(BlockContext const&, BlockMotionVectorCandidates const&);
    DecoderErrorOr<MotionVector> read_motion_vector(BlockContext const&, BlockMotionVectorCandidates const&, ReferenceIndex);
    DecoderErrorOr<i32> read_single_motion_vector_component(u8 component, bool use_high_precision);
    DecoderErrorOr<bool> residual(BlockContext&, bool has_block_above, bool has_block_left);
    DecoderErrorOr<bool> tokens(BlockContext&, size_t plane, u32 x, u32 y, TransformSize, TransformSet, Array<u8, 1024> token_cache);
    DecoderErrorOr<i32> read_coef(u8 bit_depth, Token token);

    /* (6.5) Motion Vector Prediction */
    MotionVectorPair find_reference_motion_vectors(BlockContext&, ReferenceFrameType, i32 block);
    void select_best_sub_block_reference_motion_vectors(BlockContext&, BlockMotionVectorCandidates&, i32 block, ReferenceIndex);
    size_t get_image_index(FrameContext const&, u32 row, u32 column) const;
    MotionVectorCandidate get_motion_vector_from_current_or_previous_frame(BlockContext const&, MotionVector candidate_vector, ReferenceIndex, bool use_prev);
    void add_motion_vector_if_reference_frame_type_is_same(BlockContext const&, MotionVector candidate_vector, ReferenceFrameType ref_frame, Vector<MotionVector, 2>& list, bool use_prev);
    void add_motion_vector_if_reference_frame_type_is_different(BlockContext const&, MotionVector candidate_vector, ReferenceFrameType ref_frame, Vector<MotionVector, 2>& list, bool use_prev);

    Gfx::Point<size_t> get_decoded_point_for_plane(FrameContext const&, u32 row, u32 column, u8 plane);
    Gfx::Size<size_t> get_decoded_size_for_plane(FrameContext const&, u8 plane);

    bool m_is_first_compute_image_size_invoke { true };
    Gfx::Size<u32> m_previous_frame_size { 0, 0 };
    bool m_previous_show_frame { false };
    ColorConfig m_previous_color_config;
    FrameType m_previous_frame_type { FrameType::KeyFrame };
    Array<i8, MAX_REF_FRAMES> m_previous_loop_filter_ref_deltas;
    Array<i8, 2> m_previous_loop_filter_mode_deltas;
    bool m_previous_should_use_absolute_segment_base_quantizer;
    Array<Array<SegmentFeature, SEG_LVL_MAX>, MAX_SEGMENTS> m_previous_segmentation_features;

    // FIXME: Move these to a struct to store together in one array.
    Gfx::Size<u32> m_ref_frame_size[NUM_REF_FRAMES];
    bool m_ref_subsampling_x[NUM_REF_FRAMES];
    bool m_ref_subsampling_y[NUM_REF_FRAMES];
    u8 m_ref_bit_depth[NUM_REF_FRAMES];

    Vector<u16> m_frame_store[NUM_REF_FRAMES][3];

    Vector2D<FrameBlockContext> m_reusable_frame_block_contexts;
    Vector2D<PersistentBlockContext> m_previous_block_contexts;

    OwnPtr<BitStream> m_bit_stream;
    OwnPtr<ProbabilityTables> m_probability_tables;
    OwnPtr<SyntaxElementCounter> m_syntax_element_counter;
    Decoder& m_decoder;
};

}
