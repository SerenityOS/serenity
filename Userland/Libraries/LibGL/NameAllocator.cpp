/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGL/NameAllocator.h>

namespace GL {

void NameAllocator::allocate(GLsizei count, GLuint* names)
{
    for (auto i = 0; i < count; ++i) {
        if (!m_free_names.is_empty()) {
            names[i] = m_free_names.top();
            m_free_names.pop();
        } else {
            // We're out of free previously allocated names. Let's allocate a new contiguous amount from the
            // last known id
            names[i] = m_last_id++;
        }
    }
}

void NameAllocator::free(GLuint name)
{
    m_free_names.push(name);
}

}
