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

#include "Enums.h"
#include "LookupTables.h"
#include "MotionVector.h"

namespace Video::VP9 {

template<typename T>
struct Pair {
    T a;
    T b;

    T& operator[](u8 index)
    {
        if (index == 0)
            return a;
        if (index == 1)
            return b;
        VERIFY_NOT_REACHED();
    }

    T const& operator[](u8 index) const
    {
        return const_cast<Pair<T>&>(*this)[index];
    }
};

typedef Pair<ReferenceFrameType> ReferenceFramePair;
typedef Pair<MotionVector> MotionVectorPair;

template<typename T>
class Vector2D;

template<typename T>
class Vector2DView {
public:
    u32 top() const { return m_top; }
    u32 left() const { return m_left; }
    u32 height() const { return m_height; }
    u32 width() const { return m_width; }

    T const& operator[](size_t index) const { return m_storage[index]; }
    size_t size() const { return m_storage->size(); }

    T& at(u32 relative_row, u32 relative_column)
    {
        VERIFY(relative_row < height());
        VERIFY(relative_column < width());
        return m_storage->at(top() + relative_row, left() + relative_column);
    }
    T const& at(u32 relative_row, u32 relative_column) const
    {
        VERIFY(relative_row < height());
        VERIFY(relative_column < width());
        return m_storage->at(top() + relative_row, left() + relative_column);
    }

    Vector2DView<T> view(u32 top, u32 left, u32 height, u32 width)
    {
        VERIFY(top + height <= this->height());
        VERIFY(left + width <= this->width());
        return Vector2DView<T>(m_storage, this->top() + top, this->left() + left, height, width);
    }

private:
    friend class Vector2D<T>;

    Vector2DView(Vector2D<T>* const storage, u32 top, u32 left, u32 height, u32 width)
        : m_storage(storage)
        , m_top(top)
        , m_left(left)
        , m_height(height)
        , m_width(width)
    {
    }

    Vector2D<T>* const m_storage;
    u32 const m_top { 0 };
    u32 const m_left { 0 };
    u32 const m_height { 0 };
    u32 const m_width { 0 };
};

template<typename T>
class Vector2D {
public:
    ~Vector2D()
    {
        if (m_storage)
            free(m_storage);
        m_storage = nullptr;
        m_width = 0;
        m_height = 0;
    }

    ErrorOr<void> try_resize(u32 height, u32 width)
    {
        if (height != m_height && width != m_width) {
            this->~Vector2D();
            size_t size = height * width;
            auto* new_storage = static_cast<T*>(malloc(size * sizeof(T)));
            if (!new_storage)
                return Error::from_errno(ENOMEM);
            m_storage = new_storage;
            m_height = height;
            m_width = width;
        }

        return {};
    }

    u32 height() const { return m_height; }
    u32 width() const { return m_width; }

    size_t index_at(u32 row, u32 column) const
    {
        VERIFY(row < height());
        VERIFY(column < width());
        return row * width() + column;
    }

    T& operator[](size_t index) { return m_storage[index]; }
    T const& operator[](size_t index) const { return m_storage[index]; }
    size_t size() const { return m_height * m_width; }

    T& at(u32 row, u32 column)
    {
        return m_storage[index_at(row, column)];
    }
    T const& at(u32 row, u32 column) const
    {
        return m_storage[index_at(row, column)];
    }

    template<typename OtherT>
    ErrorOr<void> try_resize_to_match_other_vector2d(Vector2D<OtherT> const& other)
    {
        return try_resize(other.height(), other.width());
    }

    template<typename OtherT, typename Function>
    void copy_to(Vector2D<OtherT>& other, Function function) const
    {
        VERIFY(width() <= other.width());
        VERIFY(height() <= other.height());
        for (u32 row = 0; row < height(); row++) {
            for (u32 column = 0; column < width(); column++)
                other.at(row, column) = function(at(row, column));
        }
    }

    void copy_to(Vector2D<T>& other) const
    {
        VERIFY(width() <= other.width());
        VERIFY(height() <= other.height());
        for (u32 row = 0; row < height(); row++) {
            auto other_index = other.index_at(row, 0);
            AK::TypedTransfer<T>::copy(&m_storage[index_at(row, 0)], &other[other_index], width());
        }
    }

    void reset()
    {
        for (size_t i = 0; i < size(); i++)
            m_storage[i] = T();
    }

    Vector2DView<T> view(u32 top, u32 left, u32 height, u32 width)
    {
        VERIFY(top + height <= this->height());
        VERIFY(left + width <= this->width());
        return Vector2DView<T>(this, top, left, height, width);
    }

private:
    u32 m_height { 0 };
    u32 m_width { 0 };
    T* m_storage { nullptr };
};

struct TokensContext {
    TXSize m_tx_size;
    bool m_is_uv_plane;
    bool m_is_inter;
    u8 m_band;
    u8 m_context_index;
};

// Block context that is kept for the lifetime of a frame.
struct FrameBlockContext {
    bool is_intra_predicted() const { return ref_frames[0] == ReferenceFrameType::None; }
    bool is_single_reference() const { return ref_frames[1] == ReferenceFrameType::None; }
    MotionVectorPair primary_motion_vector_pair() const { return { sub_block_motion_vectors[0][3], sub_block_motion_vectors[1][3] }; }

    bool is_available { false };
    bool skip_coefficients { false };
    TXSize tx_size { TXSize::TX_4x4 };
    PredictionMode y_mode { PredictionMode::DcPred };
    Array<PredictionMode, 4> sub_modes { PredictionMode::DcPred, PredictionMode::DcPred, PredictionMode::DcPred, PredictionMode::DcPred };
    InterpolationFilter interpolation_filter { InterpolationFilter::EightTap };
    ReferenceFramePair ref_frames { ReferenceFrameType::None, ReferenceFrameType::None };
    Array<Array<MotionVector, 4>, 2> sub_block_motion_vectors {};
    u8 segment_id { 0 };
};

// Block context that is kept between frames until explictly cleared.
struct PersistentBlockContext {
    PersistentBlockContext()
        : available(false)
    {
    }

    PersistentBlockContext(FrameBlockContext const& frame_context)
        : available(frame_context.is_available)
        , ref_frames(frame_context.ref_frames)
        , primary_motion_vector_pair(frame_context.primary_motion_vector_pair())
        , segment_id(frame_context.segment_id)
    {
    }

    bool available { false };
    ReferenceFramePair ref_frames { ReferenceFrameType::None, ReferenceFrameType::None };
    MotionVectorPair primary_motion_vector_pair {};
    u8 segment_id { 0 };
};

enum class FrameShowMode {
    CreateAndShowNewFrame,
    ShowExistingFrame,
    DoNotShowFrame,
};

struct ColorConfig {
    u8 bit_depth { 8 };
    ColorSpace color_space { ColorSpace::Bt601 };
    ColorRange color_range { ColorRange::Studio };
    bool subsampling_x { true };
    bool subsampling_y { true };
};

struct FrameContext {
public:
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

    // This group of fields is only needed for inter-predicted frames.
    Array<u8, 3> reference_frame_indices;
    Array<bool, LastFrame + 3> reference_frame_sign_biases;
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

    u16 header_size_in_bytes { 0 };

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
    Vector2D<FrameBlockContext> m_block_contexts;
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

    Vector2DView<FrameBlockContext> contexts_view;
};

}
