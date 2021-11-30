/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGL/GL/gl.h>
#include <LibGL/Tex/TextureUnit.h>

namespace GL {

void TextureUnit::bind_texture_to_target(GLenum texture_target, const RefPtr<Texture>& texture)
{
    if (!texture) {
        m_texture_target_2d = nullptr;
        m_currently_bound_target = GL_NONE;
        m_currently_bound_texture = nullptr;
        return;
    }

    switch (texture_target) {
    case GL_TEXTURE_2D:
        m_texture_target_2d = static_ptr_cast<Texture2D>(texture);
        m_currently_bound_target = GL_TEXTURE_2D;
        m_currently_bound_texture = texture;
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

}
