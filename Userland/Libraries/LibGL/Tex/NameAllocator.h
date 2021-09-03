/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Stack.h>
#include <LibGL/GL/gl.h>

namespace GL {

class TextureNameAllocator {
public:
    TextureNameAllocator() = default;

    void allocate(GLsizei count, GLuint* textures);
    void free(GLsizei count, const GLuint* textures);

private:
    Stack<GLuint, 512> m_free_texture_names;
    GLuint m_last_texture_id { 1 };
};

}
