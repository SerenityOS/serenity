/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Enums.h"
#include "MotionVector.h"

namespace Video::VP9 {

template<typename T>
struct Pair {
    T a;
    T b;

    T& operator[](size_t index)
    {
        if (index == 0)
            return a;
        if (index == 1)
            return b;
        VERIFY_NOT_REACHED();
    }
};

typedef Pair<ReferenceFrameType> ReferenceFramePair;
typedef Pair<MotionVector> MotionVectorPair;

}
