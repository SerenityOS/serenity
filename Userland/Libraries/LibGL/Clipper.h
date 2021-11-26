/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGL/GLStruct.h>
#include <LibGfx/Vector4.h>

namespace GL {

class Clipper final {
    enum ClipPlane : u8 {
        LEFT = 0,
        RIGHT,
        TOP,
        BOTTOM,
        NEAR,
        FAR
    };

    static constexpr u8 NUMBER_OF_CLIPPING_PLANES = 6;
    static constexpr u8 MAX_CLIPPED_VERTS = 6;

    static constexpr FloatVector4 clip_planes[] = {
        { -1, 0, 0, 1 }, // Left Plane
        { 1, 0, 0, 1 },  // Right Plane
        { 0, 1, 0, 1 },  // Top Plane
        { 0, -1, 0, 1 }, // Bottom plane
        { 0, 0, 1, 1 },  // Near Plane
        { 0, 0, -1, 1 }  // Far Plane
    };

    static constexpr FloatVector4 clip_plane_normals[] = {
        { 1, 0, 0, 0 },  // Left Plane
        { -1, 0, 0, 0 }, // Right Plane
        { 0, -1, 0, 0 }, // Top Plane
        { 0, 1, 0, 0 },  // Bottom plane
        { 0, 0, 1, 0 },  // Near Plane
        { 0, 0, -1, 0 }  // Far Plane
    };

public:
    Clipper() { }

    void clip_triangle_against_frustum(Vector<GLVertex>& input_vecs);

private:
    bool point_within_clip_plane(const FloatVector4& vertex, ClipPlane plane);
    GLVertex clip_intersection_point(const GLVertex& vec, const GLVertex& prev_vec, ClipPlane plane_index);
    Vector<GLVertex> list_a;
    Vector<GLVertex> list_b;
};

}
