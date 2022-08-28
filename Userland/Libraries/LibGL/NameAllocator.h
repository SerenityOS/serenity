/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
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

private:
    Stack<GLuint, 512> m_free_names;
    GLuint m_last_id { 1 };
};

}
