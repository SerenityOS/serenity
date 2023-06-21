/*
 * Copyright (c) 2022, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGPU/Enums.h>
#include <LibGfx/Vector4.h>

namespace GPU {

struct LightModelParameters {
    FloatVector4 scene_ambient_color { 0.2f, 0.2f, 0.2f, 1.0f };
    bool viewer_at_infinity { false };
    GPU::ColorControl color_control { GPU::ColorControl::SingleColor };
    bool two_sided_lighting { false };
};

}
