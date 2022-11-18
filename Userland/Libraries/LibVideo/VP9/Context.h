/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>

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

}
