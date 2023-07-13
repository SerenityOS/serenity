/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <LibGL/Tex/Texture2D.h>
#include <LibGfx/Matrix4x4.h>

namespace GL {

class TextureUnit {
public:
    TextureUnit() = default;

    RefPtr<Texture2D> texture_2d_target_texture() const { return m_texture_2d_target_texture; }
    void set_texture_2d_target_texture(RefPtr<Texture2D> const& texture) { m_texture_2d_target_texture = texture; }

    void set_alpha_combinator(GLenum combinator) { m_alpha_combinator = combinator; }
    GLenum alpha_combinator() const { return m_alpha_combinator; }
    void set_alpha_operand(size_t index, GLenum operand) { m_alpha_operand[index] = operand; }
    GLenum alpha_operand(size_t index) const { return m_alpha_operand[index]; }
    void set_alpha_scale(float scale) { m_alpha_scale = scale; }
    float alpha_scale() const { return m_alpha_scale; }
    void set_alpha_source(size_t index, GLenum source) { m_alpha_source[index] = source; }
    GLenum alpha_source(size_t index) const { return m_alpha_source[index]; }
    void set_color(FloatVector4 color) { m_color = color; }
    FloatVector4 color() const { return m_color; }
    void set_env_mode(GLenum mode) { m_env_mode = mode; }
    GLenum env_mode() const { return m_env_mode; }
    void set_level_of_detail_bias(float bias) { m_level_of_detail_bias = bias; }
    float level_of_detail_bias() const { return m_level_of_detail_bias; }
    void set_rgb_combinator(GLenum combinator) { m_rgb_combinator = combinator; }
    GLenum rgb_combinator() const { return m_rgb_combinator; }
    void set_rgb_operand(size_t index, GLenum operand) { m_rgb_operand[index] = operand; }
    GLenum rgb_operand(size_t index) const { return m_rgb_operand[index]; }
    void set_rgb_scale(float scale) { m_rgb_scale = scale; }
    float rgb_scale() const { return m_rgb_scale; }
    void set_rgb_source(size_t index, GLenum source) { m_rgb_source[index] = source; }
    GLenum rgb_source(size_t index) const { return m_rgb_source[index]; }

    bool texture_1d_enabled() const { return m_texture_1d_enabled; }
    void set_texture_1d_enabled(bool texture_1d_enabled) { m_texture_1d_enabled = texture_1d_enabled; }
    bool texture_2d_enabled() const { return m_texture_2d_enabled; }
    void set_texture_2d_enabled(bool texture_2d_enabled) { m_texture_2d_enabled = texture_2d_enabled; }
    bool texture_3d_enabled() const { return m_texture_3d_enabled; }
    void set_texture_3d_enabled(bool texture_3d_enabled) { m_texture_3d_enabled = texture_3d_enabled; }
    bool texture_cube_map_enabled() const { return m_texture_cube_map_enabled; }
    void set_texture_cube_map_enabled(bool texture_cube_map_enabled) { m_texture_cube_map_enabled = texture_cube_map_enabled; }

    FloatMatrix4x4& texture_matrix() { return m_texture_matrix_stack.last(); }
    Vector<FloatMatrix4x4>& texture_matrix_stack() { return m_texture_matrix_stack; }

private:
    GLenum m_alpha_combinator { GL_MODULATE };
    Array<GLenum, 3> m_alpha_operand { GL_SRC_ALPHA, GL_SRC_ALPHA, GL_SRC_ALPHA };
    float m_alpha_scale { 1.f };
    Array<GLenum, 3> m_alpha_source { GL_TEXTURE, GL_PREVIOUS, GL_CONSTANT };
    FloatVector4 m_color { 0.f, 0.f, 0.f, 0.f };
    GLenum m_env_mode { GL_MODULATE };
    float m_level_of_detail_bias { 0.f };
    GLenum m_rgb_combinator { GL_MODULATE };
    Array<GLenum, 3> m_rgb_operand { GL_SRC_COLOR, GL_SRC_COLOR, GL_SRC_ALPHA };
    float m_rgb_scale { 1.f };
    Array<GLenum, 3> m_rgb_source { GL_TEXTURE, GL_PREVIOUS, GL_CONSTANT };

    // Bound textures
    RefPtr<Texture2D> m_texture_2d_target_texture {};

    // Texturing state per unit, in increasing priority:
    bool m_texture_1d_enabled { false };
    bool m_texture_2d_enabled { false };
    bool m_texture_3d_enabled { false };
    bool m_texture_cube_map_enabled { false };

    // Matrix stack for this unit
    Vector<FloatMatrix4x4> m_texture_matrix_stack { FloatMatrix4x4::identity() };
};

}
