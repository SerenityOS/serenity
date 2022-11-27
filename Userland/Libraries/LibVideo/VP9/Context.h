/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Error.h>
#include <LibGfx/Size.h>
#include <LibVideo/Color/CodingIndependentCodePoints.h>

#include "ContextStorage.h"
#include "Enums.h"
#include "LookupTables.h"
#include "MotionVector.h"
#include "Utilities.h"

namespace Video::VP9 {

enum class FrameShowMode {
    CreateAndShowNewFrame,
    ShowExistingFrame,
    DoNotShowFrame,
};

struct FrameContext {
public:
    FrameContext(Vector2D<FrameBlockContext>& contexts)
        : m_block_contexts(contexts)
    {
    }

    u8 profile { 0 };

    FrameType type { FrameType::KeyFrame };
    bool is_inter_predicted() const { return type == FrameType::InterFrame; }

    bool error_resilient_mode { false };
    bool parallel_decoding_mode { false };
    bool should_replace_probability_context { false };

    bool shows_a_frame() const { return m_frame_show_mode != FrameShowMode::DoNotShowFrame; }
    bool shows_a_new_frame() const { return m_frame_show_mode == FrameShowMode::CreateAndShowNewFrame; }
    bool shows_existing_frame() const { return m_frame_show_mode == FrameShowMode::ShowExistingFrame; }
    void set_frame_hidden() { m_frame_show_mode = FrameShowMode::DoNotShowFrame; }
    void set_existing_frame_to_show(u8 index)
    {
        m_frame_show_mode = FrameShowMode::ShowExistingFrame;
        m_existing_frame_index = index;
    }
    u8 existing_frame_index() const { return m_existing_frame_index; }

    ColorConfig color_config {};

    u8 reference_frames_to_update_flags { 0 };
    bool should_update_reference_frame_at_index(u8 index) const { return (reference_frames_to_update_flags & (1 << index)) != 0; }

    u8 probability_context_index { 0 };

    Gfx::Size<u32> size() const { return m_size; }
    ErrorOr<void> set_size(Gfx::Size<u32> size)
    {
        m_size = size;

        // From spec, compute_image_size( )
        m_rows = (size.height() + 7u) >> 3u;
        m_columns = (size.width() + 7u) >> 3u;
        return m_block_contexts.try_resize(m_rows, m_columns);
    }
    u32 rows() const { return m_rows; }
    u32 columns() const { return m_columns; }
    u32 superblock_rows() const { return (rows() + 7u) >> 3u; }
    u32 superblock_columns() const { return (columns() + 7u) >> 3u; }

    Vector2D<FrameBlockContext> const& block_contexts() const { return m_block_contexts; }

    Gfx::Size<u32> render_size { 0, 0 };
    Gfx::Size<u16> log2_of_tile_counts { 0, 0 };

    // This group of fields is only needed for inter-predicted frames.
    Array<u8, 3> reference_frame_indices;
    Array<bool, ReferenceFrameType::LastFrame + 3> reference_frame_sign_biases;
    bool high_precision_motion_vectors_allowed { false };
    InterpolationFilter interpolation_filter { InterpolationFilter::Switchable };

    u8 loop_filter_level { 0 };
    u8 loop_filter_sharpness { 0 };
    bool loop_filter_delta_enabled { false };
    Array<i8, MAX_REF_FRAMES> loop_filter_reference_deltas;
    Array<i8, 2> loop_filter_mode_deltas;

    u8 base_quantizer_index { 0 };
    i8 y_dc_quantizer_index_delta { 0 };
    i8 uv_dc_quantizer_index_delta { 0 };
    i8 uv_ac_quantizer_index_delta { 0 };
    bool is_lossless() const
    {
        // From quantization_params( ) in the spec.
        return base_quantizer_index == 0 && y_dc_quantizer_index_delta == 0 && uv_dc_quantizer_index_delta == 0 && uv_ac_quantizer_index_delta == 0;
    }

    bool segmentation_enabled { false };
    // Note: We can use Optional<Array<...>> for these tree probabilities, but unfortunately it seems to have measurable performance overhead.
    bool use_full_segment_id_tree { false };
    Array<u8, 7> full_segment_id_tree_probabilities;
    bool use_predicted_segment_id_tree { false };
    Array<u8, 3> predicted_segment_id_tree_probabilities;
    bool should_use_absolute_segment_base_quantizer { false };
    Array<Array<SegmentFeature, SEG_LVL_MAX>, MAX_SEGMENTS> segmentation_features;

    u16 header_size_in_bytes { 0 };

    TransformMode transform_mode;

    // This group also is only needed for inter-predicted frames.
    ReferenceMode reference_mode;
    ReferenceFrameType fixed_reference_type;
    ReferenceFramePair variable_reference_types;

private:
    friend struct TileContext;

    FrameShowMode m_frame_show_mode { FrameShowMode::CreateAndShowNewFrame };
    u8 m_existing_frame_index { 0 };

    Gfx::Size<u32> m_size { 0, 0 };
    u32 m_rows { 0 };
    u32 m_columns { 0 };
    // FIXME: From spec: NOTE – We are using a 2D array to store the SubModes for clarity. It is possible to reduce memory
    //        consumption by only storing one intra mode for each 8x8 horizontal and vertical position, i.e. to use two 1D
    //        arrays instead.
    //        I think should also apply to other fields that are only accessed relative to the current block. Worth looking
    //        into how much of this context needs to be stored for the whole frame vs a row or column from the current tile.
    Vector2D<FrameBlockContext>& m_block_contexts;
};

struct TileContext {
public:
    TileContext(FrameContext& frame_context, u32 rows_start, u32 rows_end, u32 columns_start, u32 columns_end)
        : frame_context(frame_context)
        , rows_start(rows_start)
        , rows_end(rows_end)
        , columns_start(columns_start)
        , columns_end(columns_end)
        , block_contexts_view(frame_context.m_block_contexts.view(rows_start, columns_start, rows_end - rows_start, columns_end - columns_start))
    {
    }

    Vector2D<FrameBlockContext> const& frame_block_contexts() const { return frame_context.block_contexts(); }

    FrameContext const& frame_context;
    u32 rows_start { 0 };
    u32 rows_end { 0 };
    u32 columns_start { 0 };
    u32 columns_end { 0 };
    Vector2DView<FrameBlockContext> block_contexts_view;
};

struct BlockContext {
    BlockContext(TileContext& tile_context, u32 row, u32 column, BlockSubsize size)
        : frame_context(tile_context.frame_context)
        , tile_context(tile_context)
        , row(row)
        , column(column)
        , size(size)
        , contexts_view(tile_context.block_contexts_view.view(row - tile_context.rows_start, column - tile_context.columns_start,
              min<u32>(num_8x8_blocks_high_lookup[size], tile_context.frame_context.rows() - row),
              min<u32>(num_8x8_blocks_wide_lookup[size], tile_context.frame_context.columns() - column)))
    {
    }

    Vector2D<FrameBlockContext> const& frame_block_contexts() const { return frame_context.block_contexts(); }

    FrameContext const& frame_context;
    TileContext const& tile_context;
    u32 row { 0 };
    u32 column { 0 };
    BlockSubsize size;
    Gfx::Size<u8> get_size_in_sub_blocks() const
    {
        return block_size_to_sub_blocks(size);
    }

    Vector2DView<FrameBlockContext> contexts_view;

    u8 segment_id { 0 };
    bool should_skip_residuals { false };

    TransformSize transform_size { Transform_4x4 };

    ReferenceFramePair reference_frame_types;
    bool is_inter_predicted() const { return reference_frame_types.primary != ReferenceFrameType::None; }
    bool is_compound() const { return reference_frame_types.secondary != ReferenceFrameType::None; }

    Array<PredictionMode, 4> sub_block_prediction_modes;
    PredictionMode y_prediction_mode() const { return sub_block_prediction_modes.last(); }
    PredictionMode& y_prediction_mode() { return sub_block_prediction_modes.last(); }
    PredictionMode uv_prediction_mode { 0 };

    InterpolationFilter interpolation_filter { EightTap };
    Array<MotionVectorPair, 4> sub_block_motion_vectors;

    Array<i32, 1024> residual_tokens;
};

struct BlockMotionVectorCandidateSet {
    MotionVector near_vector;
    MotionVector nearest_vector;
    MotionVector best_vector;
};

struct MotionVectorCandidate {
    ReferenceFrameType type;
    MotionVector vector;
};

}
