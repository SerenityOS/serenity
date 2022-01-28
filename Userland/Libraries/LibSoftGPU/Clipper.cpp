/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/ScopeGuard.h>
#include <LibSoftGPU/Clipper.h>

namespace SoftGPU {

static constexpr FloatVector4 clip_plane_normals[] = {
    { 1, 0, 0, 0 },  // Left Plane
    { -1, 0, 0, 0 }, // Right Plane
    { 0, -1, 0, 0 }, // Top Plane
    { 0, 1, 0, 0 },  // Bottom plane
    { 0, 0, 1, 0 },  // Near Plane
    { 0, 0, -1, 0 }  // Far Plane
};

static constexpr bool point_within_clip_plane(FloatVector4 const& vertex, Clipper::ClipPlane plane)
{
    switch (plane) {
    case Clipper::ClipPlane::LEFT:
        return vertex.x() >= -vertex.w();
    case Clipper::ClipPlane::RIGHT:
        return vertex.x() <= vertex.w();
    case Clipper::ClipPlane::TOP:
        return vertex.y() <= vertex.w();
    case Clipper::ClipPlane::BOTTOM:
        return vertex.y() >= -vertex.w();
    case Clipper::ClipPlane::NEAR:
        return vertex.z() >= -vertex.w();
    case Clipper::ClipPlane::FAR:
        return vertex.z() <= vertex.w();
    }

    return false;
}

static Vertex clip_intersection_point(Vertex const& p1, Vertex const& p2, Clipper::ClipPlane plane)
{
    // See https://www.microsoft.com/en-us/research/wp-content/uploads/1978/01/p245-blinn.pdf
    // "Clipping Using Homogeneous Coordinates" Blinn/Newell, 1978

    float const w1 = p1.clip_coordinates.w();
    float const w2 = p2.clip_coordinates.w();
    float const x1 = clip_plane_normals[to_underlying(plane)].dot(p1.clip_coordinates);
    float const x2 = clip_plane_normals[to_underlying(plane)].dot(p2.clip_coordinates);
    float const a = (w1 + x1) / ((w1 + x1) - (w2 + x2));

    Vertex out;
    out.position = mix(p1.position, p2.position, a);
    out.eye_coordinates = mix(p1.eye_coordinates, p2.eye_coordinates, a);
    out.clip_coordinates = mix(p1.clip_coordinates, p2.clip_coordinates, a);
    out.color = mix(p1.color, p2.color, a);
    for (size_t i = 0; i < NUM_SAMPLERS; ++i)
        out.tex_coords[i] = mix(p1.tex_coords[i], p2.tex_coords[i], a);
    out.normal = mix(p1.normal, p2.normal, a);
    return out;
}

static void clip_plane(Vector<Vertex>& read_list, Vector<Vertex>& write_list, Clipper::ClipPlane plane)
{
    auto read_from = &read_list;
    auto write_to = &write_list;

    write_to->clear_with_capacity();
    // FIXME C++23. Static reflection will provide looping over all enum values.
    for (size_t i = 0; i < read_from->size(); i++) {
        auto const& curr_vec = read_from->at((i + 1) % read_from->size());
        auto const& prev_vec = read_from->at(i);

        if (point_within_clip_plane(curr_vec.clip_coordinates, plane)) {
            if (!point_within_clip_plane(prev_vec.clip_coordinates, plane)) {
                auto const intersect = clip_intersection_point(prev_vec, curr_vec, plane);
                write_to->append(intersect);
            }
            write_to->append(curr_vec);
        } else if (point_within_clip_plane(prev_vec.clip_coordinates, plane)) {
            auto const intersect = clip_intersection_point(prev_vec, curr_vec, plane);
            write_to->append(intersect);
        }
    }
    swap(write_list, read_list);
}

void Clipper::clip_triangle_against_frustum(Vector<Vertex>& input_verts)
{
    list_a = input_verts;
    list_b.clear_with_capacity();

    for (size_t plane = 0; plane < NUMBER_OF_CLIPPING_PLANES; plane++) {
        clip_plane(list_a, list_b, static_cast<ClipPlane>(plane));
    }

    input_verts = list_a;
}
}
