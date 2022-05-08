/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibGPU/Vertex.h>
#include <LibGfx/Vector4.h>
#include <LibSoftGPU/Clipper.h>

namespace SoftGPU {

template<Clipper::ClipPlane plane>
static constexpr bool point_within_clip_plane(FloatVector4 const& vertex)
{
    if constexpr (plane == Clipper::ClipPlane::LEFT)
        return vertex.x() >= -vertex.w();
    else if constexpr (plane == Clipper::ClipPlane::RIGHT)
        return vertex.x() <= vertex.w();
    else if constexpr (plane == Clipper::ClipPlane::TOP)
        return vertex.y() <= vertex.w();
    else if constexpr (plane == Clipper::ClipPlane::BOTTOM)
        return vertex.y() >= -vertex.w();
    else if constexpr (plane == Clipper::ClipPlane::NEAR)
        return vertex.z() >= -vertex.w();
    else if constexpr (plane == Clipper::ClipPlane::FAR)
        return vertex.z() <= vertex.w();
    return false;
}

template<Clipper::ClipPlane plane>
static constexpr GPU::Vertex clip_intersection_point(GPU::Vertex const& p1, GPU::Vertex const& p2)
{
    constexpr FloatVector4 clip_plane_normals[] = {
        { 1, 0, 0, 1 },  // Left Plane
        { -1, 0, 0, 1 }, // Right Plane
        { 0, -1, 0, 1 }, // Top Plane
        { 0, 1, 0, 1 },  // Bottom plane
        { 0, 0, 1, 1 },  // Near Plane
        { 0, 0, -1, 1 }  // Far Plane
    };
    constexpr auto clip_plane_normal = clip_plane_normals[to_underlying(plane)];

    // See https://www.microsoft.com/en-us/research/wp-content/uploads/1978/01/p245-blinn.pdf
    // "Clipping Using Homogeneous Coordinates" Blinn/Newell, 1978
    // Clip plane normals have W=1 so the vertices' W coordinates are included in x1 and x2.

    auto const x1 = clip_plane_normal.dot(p1.clip_coordinates);
    auto const x2 = clip_plane_normal.dot(p2.clip_coordinates);
    auto const a = x1 / (x1 - x2);

    GPU::Vertex out;
    out.position = mix(p1.position, p2.position, a);
    out.eye_coordinates = mix(p1.eye_coordinates, p2.eye_coordinates, a);
    out.clip_coordinates = mix(p1.clip_coordinates, p2.clip_coordinates, a);
    out.color = mix(p1.color, p2.color, a);
    for (size_t i = 0; i < GPU::NUM_SAMPLERS; ++i)
        out.tex_coords[i] = mix(p1.tex_coords[i], p2.tex_coords[i], a);
    out.normal = mix(p1.normal, p2.normal, a);
    return out;
}

template<Clipper::ClipPlane plane>
FLATTEN static constexpr void clip_plane(Vector<GPU::Vertex>& input_list, Vector<GPU::Vertex>& output_list)
{
    output_list.clear_with_capacity();

    auto input_list_size = input_list.size();
    if (input_list_size == 0)
        return;

    auto const* prev_vec = &input_list.data()[0];
    auto is_prev_point_within_clip_plane = point_within_clip_plane<plane>(prev_vec->clip_coordinates);

    for (size_t i = 1; i <= input_list_size; i++) {
        auto const& curr_vec = input_list[i % input_list_size];
        auto const is_curr_point_within_clip_plane = point_within_clip_plane<plane>(curr_vec.clip_coordinates);

        if (is_curr_point_within_clip_plane != is_prev_point_within_clip_plane)
            output_list.append(clip_intersection_point<plane>(*prev_vec, curr_vec));

        if (is_curr_point_within_clip_plane)
            output_list.append(curr_vec);

        prev_vec = &curr_vec;
        is_prev_point_within_clip_plane = is_curr_point_within_clip_plane;
    }
}

void Clipper::clip_points_against_frustum(Vector<GPU::Vertex>& vertices)
{
    m_vertex_buffer.clear_with_capacity();

    for (auto& vertex : vertices) {
        auto const coords = vertex.clip_coordinates;
        if (point_within_clip_plane<ClipPlane::LEFT>(coords) && point_within_clip_plane<ClipPlane::RIGHT>(coords)
            && point_within_clip_plane<ClipPlane::TOP>(coords) && point_within_clip_plane<ClipPlane::BOTTOM>(coords)
            && point_within_clip_plane<ClipPlane::NEAR>(coords) && point_within_clip_plane<ClipPlane::FAR>(coords))
            m_vertex_buffer.append(vertex);
    }

    vertices.clear_with_capacity();
    vertices.extend(m_vertex_buffer);
}

template<Clipper::ClipPlane plane>
static constexpr bool constrain_line_within_plane(GPU::Vertex& from, GPU::Vertex& to)
{
    auto from_within_plane = point_within_clip_plane<plane>(from.clip_coordinates);
    auto to_within_plane = point_within_clip_plane<plane>(to.clip_coordinates);
    if (!from_within_plane && !to_within_plane)
        return false;
    if (!from_within_plane)
        from = clip_intersection_point<plane>(from, to);
    else if (!to_within_plane)
        to = clip_intersection_point<plane>(from, to);
    return true;
}

bool Clipper::clip_line_against_frustum(GPU::Vertex& from, GPU::Vertex& to)
{
    return constrain_line_within_plane<ClipPlane::LEFT>(from, to)
        && constrain_line_within_plane<ClipPlane::RIGHT>(from, to)
        && constrain_line_within_plane<ClipPlane::TOP>(from, to)
        && constrain_line_within_plane<ClipPlane::BOTTOM>(from, to)
        && constrain_line_within_plane<ClipPlane::NEAR>(from, to)
        && constrain_line_within_plane<ClipPlane::FAR>(from, to);
}

void Clipper::clip_triangle_against_frustum(Vector<GPU::Vertex>& input_verts)
{
    // FIXME C++23. Static reflection will provide looping over all enum values.
    clip_plane<ClipPlane::LEFT>(input_verts, m_vertex_buffer);
    clip_plane<ClipPlane::RIGHT>(m_vertex_buffer, input_verts);
    clip_plane<ClipPlane::TOP>(input_verts, m_vertex_buffer);
    clip_plane<ClipPlane::BOTTOM>(m_vertex_buffer, input_verts);
    clip_plane<ClipPlane::NEAR>(input_verts, m_vertex_buffer);
    clip_plane<ClipPlane::FAR>(m_vertex_buffer, input_verts);
}

}
