/*
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Vector4.h>

namespace GPU {

struct RasterPosition {
    FloatVector4 window_coordinates { 0.0f, 0.0f, 0.0f, 1.0f };
    float eye_coordinate_distance { 0.0f };
    bool valid { true };
    FloatVector4 color_rgba { 1.0f, 1.0f, 1.0f, 1.0f };
    float color_index { 1.0f };
    FloatVector4 texture_coordinates { 0.0f, 0.0f, 0.0f, 1.0f };
};

}
