/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGPU/Config.h>

#define INCREASE_STATISTICS_COUNTER(stat, n)     \
    do {                                         \
        if constexpr (ENABLE_STATISTICS_OVERLAY) \
            stat += (n);                         \
    } while (0)

namespace SoftGPU {

static constexpr bool ENABLE_STATISTICS_OVERLAY = false;
static constexpr int MILLISECONDS_PER_STATISTICS_PERIOD = 500;
static constexpr int NUM_LIGHTS = 8;
static constexpr int MAX_CLIP_PLANES = 6;
static constexpr int SUBPIXEL_BITS = 6;

// See: https://www.khronos.org/opengl/wiki/Common_Mistakes#Texture_edge_color_problem
// FIXME: make this dynamically configurable through ConfigServer
static constexpr bool CLAMP_DEPRECATED_BEHAVIOR = false;

}
