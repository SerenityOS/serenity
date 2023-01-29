/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Stack.h>
#include <LibGL/GL/gl.h>

namespace GL {

class NameAllocator {
public:
    NameAllocator() = default;

    void allocate(GLsizei count, GLuint* names);
    void free(GLuint name);
    bool has_allocated_name(GLuint name) const;

private:
    Stack<GLuint, 512> m_free_names;
    GLuint m_last_id { 1 };
};

}
