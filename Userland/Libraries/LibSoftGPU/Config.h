/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define INCREASE_STATISTICS_COUNTER(stat, n)     \
    do {                                         \
        if constexpr (ENABLE_STATISTICS_OVERLAY) \
            stat += (n);                         \
    } while (0)

namespace SoftGPU {

static constexpr bool ENABLE_STATISTICS_OVERLAY = false;
static constexpr int NUM_SAMPLERS = 32;
static constexpr int SUBPIXEL_BITS = 5;

// See: https://www.khronos.org/opengl/wiki/Common_Mistakes#Texture_edge_color_problem
// FIXME: make this dynamically configurable through ConfigServer
static constexpr bool CLAMP_DEPRECATED_BEHAVIOR = false;

}
