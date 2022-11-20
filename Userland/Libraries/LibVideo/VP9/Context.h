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

#include <AK/Format.h>

#include "Enums.h"
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
    void copy_to(Vector2D<OtherT>& other, Function function)
    {
        VERIFY(width() <= other.width());
        VERIFY(height() <= other.height());
        for (u32 row = 0; row < height(); row++) {
            for (u32 column = 0; column < width(); column++)
                other.at(row, column) = function(at(row, column));
        }
    }

    void copy_to(Vector2D<T>& other)
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

}
