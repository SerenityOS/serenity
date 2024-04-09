/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022-2024, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGPU/Config.h>
#include <LibGPU/Enums.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Vector4.h>

namespace GPU {

struct RasterizerOptions {
    bool shade_smooth { true };
    bool enable_stencil_test { false };
    bool enable_depth_test { false };
    bool enable_depth_write { true };
    bool enable_alpha_test { false };
    AlphaTestFunction alpha_test_func { AlphaTestFunction::Always };
    float alpha_test_ref_value { 0 };
    bool enable_blending { false };
    FloatVector4 blend_color { 0.f, 0.f, 0.f, 0.f };
    BlendEquation blend_equation_rgb { BlendEquation::Add };
    BlendEquation blend_equation_alpha { BlendEquation::Add };
    BlendFactor blend_source_factor { BlendFactor::One };
    BlendFactor blend_destination_factor { BlendFactor::One };
    u32 color_mask { 0xffffffff };
    float depth_min { 0.f };
    float depth_max { 1.f };
    DepthTestFunction depth_func { DepthTestFunction::Less };
    PolygonMode polygon_mode { PolygonMode::Fill };
    FloatVector4 fog_color { 0.0f, 0.0f, 0.0f, 0.0f };
    float fog_density { 1.0f };
    FogMode fog_mode { FogMode::Exp };
    bool fog_enabled { false };
    float fog_start { 0.0f };
    float fog_end { 1.0f };
    bool line_smooth { false };
    float line_width { 1.f };
    bool point_smooth { false };
    float point_size { 1.f };
    bool scissor_enabled { false };
    bool normalization_enabled { false };
    Gfx::IntRect scissor_box;
    bool enable_color_write { true };
    float depth_offset_factor { 0 };
    float depth_offset_constant { 0 };
    bool depth_offset_enabled { false };
    bool enable_culling { false };
    WindingOrder front_face { WindingOrder::CounterClockwise };
    bool cull_back { true };
    bool cull_front { false };
    Gfx::IntRect viewport;
    bool lighting_enabled { false };
    bool color_material_enabled { false };
    ColorMaterialFace color_material_face { ColorMaterialFace::FrontAndBack };
    ColorMaterialMode color_material_mode { ColorMaterialMode::AmbientAndDiffuse };
};

}
