/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace SoftGPU {

static constexpr int RASTERIZER_BLOCK_SIZE = 8;
static constexpr int NUM_SAMPLERS = 32;

// See: https://www.khronos.org/opengl/wiki/Common_Mistakes#Texture_edge_color_problem
// FIXME: make this dynamically configurable through ConfigServer
static constexpr bool CLAMP_DEPRECATED_BEHAVIOR = false;

}
