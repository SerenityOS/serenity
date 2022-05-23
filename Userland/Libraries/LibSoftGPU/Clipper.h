/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGPU/Vertex.h>
#include <LibGfx/Vector4.h>

namespace SoftGPU {

class Clipper final {
public:
    enum class ClipPlane : u8 {
        Left = 0,
        Right,
        Top,
        Bottom,
        Near,
        Far,
        User, // Within view space
    };

    Clipper() = default;

    void clip_points_against_frustum(Vector<GPU::Vertex>& vertices);
    bool clip_line_against_frustum(GPU::Vertex& from, GPU::Vertex& to);
    void clip_triangle_against_frustum(Vector<GPU::Vertex>& input_vecs);
    void clip_triangle_against_user_defined(Vector<GPU::Vertex>& input_verts, Vector<FloatVector4>& user_planes);

private:
    Vector<GPU::Vertex> m_vertex_buffer;
};

}
