/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGPU/Image.h>
#include <LibGfx/Vector4.h>

namespace GPU {

enum class TextureFilter {
    Nearest,
    Linear,
};

enum class MipMapFilter {
    None,
    Nearest,
    Linear,
};

enum class TextureWrapMode {
    Repeat,
    MirroredRepeat,
    Clamp,
    ClampToBorder,
    ClampToEdge,
};

enum class TextureEnvMode {
    Modulate,
    Replace,
    Decal,
    Add,
};

struct SamplerConfig final {
    RefPtr<Image> bound_image;
    MipMapFilter mipmap_filter { MipMapFilter::Nearest };
    TextureFilter texture_mag_filter { TextureFilter::Linear };
    TextureFilter texture_min_filter { TextureFilter::Linear };
    TextureWrapMode texture_wrap_u { TextureWrapMode::Repeat };
    TextureWrapMode texture_wrap_v { TextureWrapMode::Repeat };
    TextureWrapMode texture_wrap_w { TextureWrapMode::Repeat };
    FloatVector4 border_color { 0, 0, 0, 1 };
    TextureEnvMode fixed_function_texture_env_mode { TextureEnvMode::Modulate };
};

}
