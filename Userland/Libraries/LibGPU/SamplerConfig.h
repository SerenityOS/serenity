/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
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
    Add,
    Blend,
    Combine,
    Decal,
    Modulate,
    Replace,
};

enum class TextureCombinator {
    Add,
    AddSigned,
    Dot3RGB,
    Dot3RGBA,
    Interpolate,
    Modulate,
    Replace,
    Subtract,
};

enum class TextureOperand {
    OneMinusSourceAlpha,
    OneMinusSourceColor,
    SourceAlpha,
    SourceColor,
};

enum class TextureSource {
    Constant,
    Previous,
    PrimaryColor,
    Texture,
    TextureStage,
};

struct FixedFunctionTextureEnvironment final {
    TextureCombinator alpha_combinator { TextureCombinator::Modulate };
    Array<TextureOperand, 3> alpha_operand { TextureOperand::SourceAlpha, TextureOperand::SourceAlpha, TextureOperand::SourceAlpha };
    float alpha_scale { 1.f };
    Array<TextureSource, 3> alpha_source { TextureSource::Texture, TextureSource::Previous, TextureSource::Constant };
    u8 alpha_source_texture_stage { 0 };
    FloatVector4 color { 0.f, 0.f, 0.f, 0.f };
    TextureEnvMode env_mode { TextureEnvMode::Modulate };
    TextureCombinator rgb_combinator { TextureCombinator::Modulate };
    Array<TextureOperand, 3> rgb_operand { TextureOperand::SourceColor, TextureOperand::SourceColor, TextureOperand::SourceAlpha };
    float rgb_scale { 1.f };
    Array<TextureSource, 3> rgb_source { TextureSource::Texture, TextureSource::Previous, TextureSource::Constant };
    u8 rgb_source_texture_stage { 0 };
};

struct SamplerConfig final {
    RefPtr<Image> bound_image;
    float level_of_detail_bias { 0.f };
    MipMapFilter mipmap_filter { MipMapFilter::Nearest };
    TextureFilter texture_mag_filter { TextureFilter::Linear };
    TextureFilter texture_min_filter { TextureFilter::Linear };
    TextureWrapMode texture_wrap_u { TextureWrapMode::Repeat };
    TextureWrapMode texture_wrap_v { TextureWrapMode::Repeat };
    TextureWrapMode texture_wrap_w { TextureWrapMode::Repeat };
    FloatVector4 border_color { 0.f, 0.f, 0.f, 1.f };
    FixedFunctionTextureEnvironment fixed_function_texture_environment {};
};

}
