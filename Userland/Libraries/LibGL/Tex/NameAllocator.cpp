/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGL/Tex/NameAllocator.h>

namespace GL {

void TextureNameAllocator::allocate(GLsizei count, GLuint* textures)
{
    for (auto i = 0; i < count; ++i) {
        if (!m_free_texture_names.is_empty()) {
            textures[i] = m_free_texture_names.top();
            m_free_texture_names.pop();
        } else {
            // We're out of free previously allocated names. Let's allocate a new contiguous amount from the
            // last known texture name
            textures[i] = m_last_texture_id++;
        }
    }
}

void TextureNameAllocator::free(GLuint texture)
{
    m_free_texture_names.push(texture);
}

}
