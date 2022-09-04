/*
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGPU/Enums.h>
#include <LibGfx/Matrix4x4.h>
#include <LibGfx/Vector4.h>

namespace GPU {

typedef u8 TextureUnitIndex;

struct TexCoordGeneration {
    bool enabled;
    TexCoordGenerationMode mode;
    FloatVector4 coefficients;
};

struct TextureUnitConfiguration {
    bool enabled { false };
    FloatMatrix4x4 transformation_matrix { FloatMatrix4x4::identity() };
    u8 tex_coord_generation_enabled;
    TexCoordGeneration tex_coord_generation[4];
};

}
