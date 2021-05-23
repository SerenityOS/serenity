/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Mathieu Gaillard <gaillard.mathieu.39@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Vector.h>

#include "Common.h"

class Mesh : public RefCounted<Mesh> {
public:
    Mesh() = delete;

    Mesh(Vector<Vertex> vertices, Vector<TexCoord> tex_coords, Vector<Triangle> triangles);

    size_t vertex_count() const { return m_vertex_list.size(); }

    size_t triangle_count() const { return m_triangle_list.size(); }

    void draw();

    bool is_textured() const { return m_tex_coords.size() > 0; }

private:
    Vector<Vertex> m_vertex_list;
    Vector<TexCoord> m_tex_coords;
    Vector<Triangle> m_triangle_list;
};
