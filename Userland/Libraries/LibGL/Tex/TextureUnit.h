/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibGL/Tex/Texture2D.h>

namespace GL {

class TextureUnit {
public:
    TextureUnit() = default;

    void bind_texture_to_target(GLenum texture_target, const RefPtr<Texture>& texture);

    RefPtr<Texture2D>& bound_texture_2d() const { return m_texture_target_2d; }
    RefPtr<Texture>& bound_texture() const { return m_currently_bound_texture; }

    GLenum currently_bound_target() const { return m_currently_bound_target; }
    bool is_bound() const { return !m_currently_bound_texture.is_null(); }

    void set_env_mode(GLenum mode) { m_env_mode = mode; }
    GLenum env_mode() const { return m_env_mode; }

    bool texture_1d_enabled() const { return m_texture_1d_enabled; };
    void set_texture_1d_enabled(bool texture_1d_enabled) { m_texture_1d_enabled = texture_1d_enabled; }
    bool texture_2d_enabled() const { return m_texture_2d_enabled; };
    void set_texture_2d_enabled(bool texture_2d_enabled) { m_texture_2d_enabled = texture_2d_enabled; }
    bool texture_3d_enabled() const { return m_texture_3d_enabled; };
    void set_texture_3d_enabled(bool texture_3d_enabled) { m_texture_3d_enabled = texture_3d_enabled; }
    bool texture_cube_map_enabled() const { return m_texture_cube_map_enabled; };
    void set_texture_cube_map_enabled(bool texture_cube_map_enabled) { m_texture_cube_map_enabled = texture_cube_map_enabled; }

private:
    mutable RefPtr<Texture2D> m_texture_target_2d { nullptr };
    mutable RefPtr<Texture> m_currently_bound_texture { nullptr };
    GLenum m_currently_bound_target;
    GLenum m_env_mode { GL_MODULATE };

    // Texturing state per unit, in increasing priority:
    bool m_texture_1d_enabled { false };
    bool m_texture_2d_enabled { false };
    bool m_texture_3d_enabled { false };
    bool m_texture_cube_map_enabled { false };
};

}
