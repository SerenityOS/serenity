/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Vector.h>

#include "Common.h"

// NOTE: We don't support indexed
class Mesh : public RefCounted<Mesh> {
public:
    Mesh() = delete;
    Mesh(const Vector<Triangle>& triangles)
        : m_triangle_list(triangles)
    {
    }

    void draw();

    size_t triangle_count() const { return m_triangle_list.size(); }

private:
    Vector<Triangle> m_triangle_list;
};
