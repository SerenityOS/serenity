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
static constexpr int MAX_TEXTURE_SIZE = 2048;
static constexpr float MAX_TEXTURE_LOD_BIAS = 2.f;
static constexpr int SUBPIXEL_BITS = 4;

static constexpr int NUM_SHADER_INPUTS = 64;

// Verify that we have enough inputs to hold vertex color and texture coordinates for all fixed function texture units
static_assert(NUM_SHADER_INPUTS >= 4 + GPU::NUM_TEXTURE_UNITS * 4);

static constexpr int SHADER_INPUT_VERTEX_COLOR = 0;
static constexpr int SHADER_INPUT_FIRST_TEXCOORD = 4;

static constexpr int NUM_SHADER_OUTPUTS = 4;

// Verify that we have enough outputs to hold the fragment's color
static_assert(NUM_SHADER_OUTPUTS >= 4);

static constexpr int SHADER_OUTPUT_FIRST_COLOR = 0;

// See: https://www.khronos.org/opengl/wiki/Common_Mistakes#Texture_edge_color_problem
// FIXME: make this dynamically configurable through ConfigServer
static constexpr bool CLAMP_DEPRECATED_BEHAVIOR = false;

}
