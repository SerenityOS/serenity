/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/SIMD.h>
#include <LibGPU/SamplerConfig.h>
#include <LibGfx/Vector2.h>
#include <LibGfx/Vector4.h>

namespace SoftGPU {

class Sampler final {
public:
    Vector4<AK::SIMD::f32x4> sample_2d(Vector2<AK::SIMD::f32x4> const& uv) const;

    void set_config(GPU::SamplerConfig const& config) { m_config = config; }
    GPU::SamplerConfig const& config() const { return m_config; }

private:
    Vector4<AK::SIMD::f32x4> sample_2d_lod(Vector2<AK::SIMD::f32x4> const& uv, AK::SIMD::u32x4 level, GPU::TextureFilter) const;

    GPU::SamplerConfig m_config;
};

}
