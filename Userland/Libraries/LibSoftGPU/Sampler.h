/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <LibGfx/Vector2.h>
#include <LibGfx/Vector4.h>
#include <LibSoftGPU/Image.h>

namespace SoftGPU {

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

struct SamplerConfig final {
    RefPtr<Image> bound_image;
    MipMapFilter mipmap_filter { MipMapFilter::Nearest };
    TextureFilter texture_mag_filter { TextureFilter::Linear };
    TextureFilter texture_min_filter { TextureFilter::Linear };
    TextureWrapMode texture_wrap_u { TextureWrapMode::Repeat };
    TextureWrapMode texture_wrap_v { TextureWrapMode::Repeat };
    TextureWrapMode texture_wrap_w { TextureWrapMode::Repeat };
    FloatVector4 border_color { 0, 0, 0, 1 };
};

class Sampler final {
public:
    FloatVector4 sample_2d(FloatVector2 const& uv) const;

    void set_config(SamplerConfig const& config) { m_config = config; }

private:
    SamplerConfig m_config;
};

}
