/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/ScopeGuard.h>
#include <LibSoftGPU/Clipper.h>

namespace SoftGPU {

Vertex Clipper::clip_intersection_point(Vertex const& p1, Vertex const& p2, ClipPlane plane)
{
    // See https://www.microsoft.com/en-us/research/wp-content/uploads/1978/01/p245-blinn.pdf
    // "Clipping Using Homogeneous Coordinates" Blinn/Newell, 1978

    float const w1 = p1.clip_coordinates.w();
    float const w2 = p2.clip_coordinates.w();
    float const x1 = clip_plane_normals[static_cast<u8>(plane)].dot(p1.clip_coordinates);
    float const x2 = clip_plane_normals[static_cast<u8>(plane)].dot(p2.clip_coordinates);
    float const a = (w1 + x1) / ((w1 + x1) - (w2 + x2));

    Vertex out;
    out.position = mix(p1.position, p2.position, a);
    out.eye_coordinates = mix(p1.eye_coordinates, p2.eye_coordinates, a);
    out.clip_coordinates = mix(p1.clip_coordinates, p2.clip_coordinates, a);
    out.color = mix(p1.color, p2.color, a);
    out.tex_coord = mix(p1.tex_coord, p2.tex_coord, a);
    return out;
}

void Clipper::clip_triangle_against_frustum(Vector<Vertex>& input_verts)
{
    list_a = input_verts;
    list_b.clear_with_capacity();

    auto read_from = &list_a;
    auto write_to = &list_b;

    // Save me, C++23. With enum reflection it will be possible to loop
    // over all the available enum values.
    clip_plane<ClipPlane::LEFT>(write_to, read_from);
    clip_plane<ClipPlane::RIGHT>(write_to, read_from);
    clip_plane<ClipPlane::TOP>(write_to, read_from);
    clip_plane<ClipPlane::BOTTOM>(write_to, read_from);
    clip_plane<ClipPlane::NEAR>(write_to, read_from);
    clip_plane<ClipPlane::FAR>(write_to, read_from);

    input_verts = *read_from;
}
}
