/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/ScopeGuard.h>
#include <LibGL/Clipper.h>

bool GL::Clipper::point_within_clip_plane(const FloatVector4& vertex, ClipPlane plane)
{
    switch (plane) {
    case ClipPlane::LEFT:
        return vertex.x() > -vertex.w();
    case ClipPlane::RIGHT:
        return vertex.x() < vertex.w();
    case ClipPlane::TOP:
        return vertex.y() < vertex.w();
    case ClipPlane::BOTTOM:
        return vertex.y() > -vertex.w();
    case ClipPlane::NEAR:
        return vertex.z() > -vertex.w();
    case ClipPlane::FAR:
        return vertex.z() < vertex.w();
    }

    return false;
}

// TODO: This needs to interpolate color/UV data as well!
FloatVector4 GL::Clipper::clip_intersection_point(const FloatVector4& vec, const FloatVector4& prev_vec, ClipPlane plane_index)
{

    //
    // This is an implementation of the Cyrus-Beck algorithm to clip lines against a plane
    // using the triangle's line segment in parametric form.
    // In this case, we find that n . [P(t) - plane] == 0 if the point lies on
    // the boundary, which in this case is the clip plane. We then substitute
    // P(t)= P1 + (P2-P1)*t (where P2 is a point inside the clipping boundary, and P1 is,
    // in this case, the point that lies outside the boundary due to our implementation of Sutherland-Hogdman)
    // into P(t) to arrive at the equation:
    //
    //  n · [P1 + ((P2 - P1) * t) - plane] = 0.
    //  Substitude seg_length = P2 - P1 (length of line segment) and dist = P1 - plane (distance from P1 to plane)
    //
    //  By rearranging, we now end up with
    //
    //  n·[P1 + (seg_length * t) - plane] = 0
    //  n·(dist) + n·(seg_length * t) = 0
    //  n·(seg_length*t) = -n·(dist)
    //  Therefore
    //  t = (-n·(dist))/(n·seg_length)
    //
    // NOTE: n is the normal vector to the plane we are clipping against.
    //
    // Proof of algorithm found here
    // http://studymaterial.unipune.ac.in:8080/jspui/bitstream/123456789/4843/1/Cyrus_Beck_Algo.pdf
    // And here (slightly more intuitive with a better diagram)
    // https://www.csee.umbc.edu/~rheingan/435/pages/res/gen-5.Clipping-single-page-0.html

    FloatVector4 seg_length = vec - prev_vec;                // P2 - P1
    FloatVector4 dist = prev_vec - clip_planes[plane_index]; // Distance from "out of bounds" vector to plane

    float plane_normal_line_segment_dot_product = clip_plane_normals[plane_index].dot(seg_length);
    float neg_plane_normal_dist_dot_procut = -clip_plane_normals[plane_index].dot(dist);
    float t = (plane_normal_line_segment_dot_product / neg_plane_normal_dist_dot_procut);

    // P(t) = P1 + (t * (P2 - P1))
    FloatVector4 lerped_vec = prev_vec + (seg_length * t);

    return lerped_vec;
}

// FIXME: Getting too close to zNear causes VERY serious artifacting. Should we cull the whole triangle??
void GL::Clipper::clip_triangle_against_frustum(Vector<FloatVector4>& input_verts)
{
    Vector<FloatVector4, 6> clipped_polygon;
    Vector<FloatVector4, 6> input = input_verts;
    Vector<FloatVector4, 6>* current = &clipped_polygon;
    Vector<FloatVector4, 6>* output_list = &input;
    ScopeGuard guard { [&] { input_verts = *output_list; } };

    for (u8 plane = 0; plane < NUMBER_OF_CLIPPING_PLANES; plane++) {
        swap(current, output_list);
        clipped_polygon.clear();

        if (current->size() == 0) {
            return;
        }

        // Save me, C++23
        for (size_t i = 0; i < current->size(); i++) {
            const auto& curr_vec = current->at(i);
            const auto& prev_vec = current->at((i - 1) % current->size());

            if (point_within_clip_plane(curr_vec, static_cast<ClipPlane>(plane))) {
                if (!point_within_clip_plane(prev_vec, static_cast<ClipPlane>(plane))) {
                    FloatVector4 intersect = clip_intersection_point(prev_vec, curr_vec, static_cast<ClipPlane>(plane));
                    clipped_polygon.append(intersect);
                }

                clipped_polygon.append(curr_vec);
            } else if (point_within_clip_plane(prev_vec, static_cast<ClipPlane>(plane))) {
                FloatVector4 intersect = clip_intersection_point(prev_vec, curr_vec, static_cast<ClipPlane>(plane));
                clipped_polygon.append(intersect);
            }
        }
    }
}
