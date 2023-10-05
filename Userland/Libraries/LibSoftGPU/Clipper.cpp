/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StdLibExtras.h>
#include <AK/Vector.h>
#include <LibGPU/Vertex.h>
#include <LibGfx/Vector4.h>
#include <LibSoftGPU/Clipper.h>

namespace SoftGPU {

template<Clipper::ClipPlane plane>
static constexpr bool point_within_clip_plane(FloatVector4 const& vertex)
{
    if constexpr (plane == Clipper::ClipPlane::Left)
        return vertex.x() >= -vertex.w();
    else if constexpr (plane == Clipper::ClipPlane::Right)
        return vertex.x() <= vertex.w();
    else if constexpr (plane == Clipper::ClipPlane::Top)
        return vertex.y() <= vertex.w();
    else if constexpr (plane == Clipper::ClipPlane::Bottom)
        return vertex.y() >= -vertex.w();
    else if constexpr (plane == Clipper::ClipPlane::Near)
        return vertex.z() >= -vertex.w();
    else if constexpr (plane == Clipper::ClipPlane::Far)
        return vertex.z() <= vertex.w();
    return false;
}

static bool point_within_user_plane(FloatVector4 const& vertex, FloatVector4 const& user_plane)
{
    return vertex.dot(user_plane) >= 0;
}

template<Clipper::ClipPlane plane>
bool point_within_plane(GPU::Vertex const& vertex, FloatVector4 const& user_plane)
{
    if constexpr (plane == Clipper::ClipPlane::User)
        return point_within_user_plane(vertex.eye_coordinates, user_plane);
    else
        return point_within_clip_plane<plane>(vertex.clip_coordinates);
}

template<Clipper::ClipPlane plane>
static GPU::Vertex clip_intersection_point(GPU::Vertex const& p1, GPU::Vertex const& p2, FloatVector4 const& plane_normal)
{
    auto p1_coordinates = (plane == Clipper::ClipPlane::User) ? p1.eye_coordinates : p1.clip_coordinates;
    auto p2_coordinates = (plane == Clipper::ClipPlane::User) ? p2.eye_coordinates : p2.clip_coordinates;
    auto x1 = plane_normal.dot(p1_coordinates);
    auto x2 = plane_normal.dot(p2_coordinates);
    auto const a = x1 / (x1 - x2);

    GPU::Vertex out;
    out.position = mix(p1.position, p2.position, a);
    out.eye_coordinates = mix(p1.eye_coordinates, p2.eye_coordinates, a);
    out.clip_coordinates = mix(p1.clip_coordinates, p2.clip_coordinates, a);
    out.color = mix(p1.color, p2.color, a);
    for (size_t i = 0; i < GPU::NUM_TEXTURE_UNITS; ++i)
        out.tex_coords[i] = mix(p1.tex_coords[i], p2.tex_coords[i], a);
    out.normal = mix(p1.normal, p2.normal, a);
    return out;
}

template<Clipper::ClipPlane plane>
FLATTEN static void clip_plane(Vector<GPU::Vertex>& input_list, Vector<GPU::Vertex>& output_list, FloatVector4 const& clip_plane)
{
    output_list.clear_with_capacity();

    auto input_list_size = input_list.size();
    if (input_list_size == 0)
        return;

    // Ensure we can perform unchecked appends in the loop below
    if (input_list_size * 2 > output_list.capacity())
        output_list.ensure_capacity(input_list_size * 2);

    auto const* prev_vec = &input_list.data()[0];
    auto is_prev_point_within_plane = point_within_plane<plane>(*prev_vec, clip_plane);

    for (size_t i = 1; i <= input_list_size; i++) {
        auto const& curr_vec = input_list[i % input_list_size];
        auto const is_curr_point_within_plane = point_within_plane<plane>(curr_vec, clip_plane);

        if (is_curr_point_within_plane != is_prev_point_within_plane)
            output_list.unchecked_append(clip_intersection_point<plane>(*prev_vec, curr_vec, clip_plane));

        if (is_curr_point_within_plane)
            output_list.unchecked_append(curr_vec);

        prev_vec = &curr_vec;
        is_prev_point_within_plane = is_curr_point_within_plane;
    }
}

void Clipper::clip_points_against_frustum(Vector<GPU::Vertex>& vertices)
{
    m_vertex_buffer.clear_with_capacity();

    for (auto& vertex : vertices) {
        auto const coords = vertex.clip_coordinates;
        if (point_within_clip_plane<ClipPlane::Left>(coords) && point_within_clip_plane<ClipPlane::Right>(coords)
            && point_within_clip_plane<ClipPlane::Top>(coords) && point_within_clip_plane<ClipPlane::Bottom>(coords)
            && point_within_clip_plane<ClipPlane::Near>(coords) && point_within_clip_plane<ClipPlane::Far>(coords))
            m_vertex_buffer.append(vertex);
    }

    vertices.clear_with_capacity();
    vertices.extend(m_vertex_buffer);
}

constexpr FloatVector4 clip_plane_eqns[] = {
    { 1, 0, 0, 1 },  // Left Plane
    { -1, 0, 0, 1 }, // Right Plane
    { 0, -1, 0, 1 }, // Top Plane
    { 0, 1, 0, 1 },  // Bottom plane
    { 0, 0, 1, 1 },  // Near Plane
    { 0, 0, -1, 1 }  // Far Plane
};

template<Clipper::ClipPlane plane>
static constexpr bool constrain_line_within_plane(GPU::Vertex& from, GPU::Vertex& to)
{
    constexpr auto clip_plane_eqn = clip_plane_eqns[to_underlying(plane)];

    auto from_within_plane = point_within_clip_plane<plane>(from.clip_coordinates);
    auto to_within_plane = point_within_clip_plane<plane>(to.clip_coordinates);
    if (!from_within_plane && !to_within_plane)
        return false;
    if (!from_within_plane)
        from = clip_intersection_point<plane>(from, to, clip_plane_eqn);
    else if (!to_within_plane)
        to = clip_intersection_point<plane>(from, to, clip_plane_eqn);
    return true;
}

bool Clipper::clip_line_against_frustum(GPU::Vertex& from, GPU::Vertex& to)
{
    return constrain_line_within_plane<ClipPlane::Left>(from, to)
        && constrain_line_within_plane<ClipPlane::Right>(from, to)
        && constrain_line_within_plane<ClipPlane::Top>(from, to)
        && constrain_line_within_plane<ClipPlane::Bottom>(from, to)
        && constrain_line_within_plane<ClipPlane::Near>(from, to)
        && constrain_line_within_plane<ClipPlane::Far>(from, to);
}

void Clipper::clip_triangle_against_frustum(Vector<GPU::Vertex>& input_verts)
{
    // FIXME C++23. Static reflection will provide looping over all enum values.
    clip_plane<ClipPlane::Left>(input_verts, m_vertex_buffer, clip_plane_eqns[0]);
    clip_plane<ClipPlane::Right>(m_vertex_buffer, input_verts, clip_plane_eqns[1]);
    clip_plane<ClipPlane::Top>(input_verts, m_vertex_buffer, clip_plane_eqns[2]);
    clip_plane<ClipPlane::Bottom>(m_vertex_buffer, input_verts, clip_plane_eqns[3]);
    clip_plane<ClipPlane::Near>(input_verts, m_vertex_buffer, clip_plane_eqns[4]);
    clip_plane<ClipPlane::Far>(m_vertex_buffer, input_verts, clip_plane_eqns[5]);
}

void Clipper::clip_triangle_against_user_defined(Vector<GPU::Vertex>& input_verts, Vector<FloatVector4>& user_planes)
{
    // FIXME: Also implement user plane support for points and lines
    auto& in = input_verts;
    auto& out = m_vertex_buffer;
    for (auto const& plane : user_planes) {
        clip_plane<ClipPlane::User>(in, out, plane);
        swap(in, out);
    }
}

}
