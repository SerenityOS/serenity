/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <LibGL/Tex/Texture2D.h>

namespace GL {

class TextureUnit {
public:
    TextureUnit() = default;

    RefPtr<Texture2D> texture_2d_target_texture() const { return m_texture_2d_target_texture; }
    void set_texture_2d_target_texture(RefPtr<Texture2D> const& texture) { m_texture_2d_target_texture = texture; }

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
    GLenum m_env_mode { GL_MODULATE };

    // Bound textures
    RefPtr<Texture2D> m_texture_2d_target_texture {};

    // Texturing state per unit, in increasing priority:
    bool m_texture_1d_enabled { false };
    bool m_texture_2d_enabled { false };
    bool m_texture_3d_enabled { false };
    bool m_texture_cube_map_enabled { false };
};

}
