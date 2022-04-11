/*
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGPU/Enums.h>
#include <LibGfx/Vector4.h>

namespace GPU {

struct TexCoordGenerationConfig {
    GPU::TexCoordGenerationMode mode { GPU::TexCoordGenerationMode::EyeLinear };
    FloatVector4 coefficients {};
};

}
