/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/ScopeGuard.h>
#include <LibGL/Clipper.h>

namespace GL {

bool Clipper::point_within_clip_plane(const FloatVector4& vertex, ClipPlane plane)
{
    switch (plane) {
    case ClipPlane::LEFT:
        return vertex.x() >= -vertex.w();
    case ClipPlane::RIGHT:
        return vertex.x() <= vertex.w();
    case ClipPlane::TOP:
        return vertex.y() <= vertex.w();
    case ClipPlane::BOTTOM:
        return vertex.y() >= -vertex.w();
    case ClipPlane::NEAR:
        return vertex.z() >= -vertex.w();
    case ClipPlane::FAR:
        return vertex.z() <= vertex.w();
    }

    return false;
}

GLVertex Clipper::clip_intersection_point(const GLVertex& p1, const GLVertex& p2, ClipPlane plane_index)
{
    // See https://www.microsoft.com/en-us/research/wp-content/uploads/1978/01/p245-blinn.pdf
    // "Clipping Using Homogeneous Coordinates" Blinn/Newell, 1978

    float w1 = p1.position.w();
    float w2 = p2.position.w();
    float x1 = clip_plane_normals[plane_index].dot(p1.position);
    float x2 = clip_plane_normals[plane_index].dot(p2.position);
    float a = (w1 + x1) / ((w1 + x1) - (w2 + x2));

    GLVertex out;
    out.position = p1.position * (1 - a) + p2.position * a;
    out.color = p1.color * (1 - a) + p2.color * a;
    out.tex_coord = p1.tex_coord * (1 - a) + p2.tex_coord * a;
    return out;
}

void Clipper::clip_triangle_against_frustum(Vector<GLVertex>& input_verts)
{
    list_a = input_verts;
    list_b.clear_with_capacity();

    auto read_from = &list_a;
    auto write_to = &list_b;

    for (size_t plane = 0; plane < NUMBER_OF_CLIPPING_PLANES; plane++) {
        write_to->clear_with_capacity();
        // Save me, C++23
        for (size_t i = 0; i < read_from->size(); i++) {
            const auto& curr_vec = read_from->at((i + 1) % read_from->size());
            const auto& prev_vec = read_from->at(i);

            if (point_within_clip_plane(curr_vec.position, static_cast<ClipPlane>(plane))) {
                if (!point_within_clip_plane(prev_vec.position, static_cast<ClipPlane>(plane))) {
                    auto intersect = clip_intersection_point(prev_vec, curr_vec, static_cast<ClipPlane>(plane));
                    write_to->append(intersect);
                }
                write_to->append(curr_vec);
            } else if (point_within_clip_plane(prev_vec.position, static_cast<ClipPlane>(plane))) {
                auto intersect = clip_intersection_point(prev_vec, curr_vec, static_cast<ClipPlane>(plane));
                write_to->append(intersect);
            }
        }
        swap(write_to, read_from);
    }

    input_verts = *read_from;
}
}
