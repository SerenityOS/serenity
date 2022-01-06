/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGfx/Vector4.h>
#include <LibSoftGPU/Vertex.h>

namespace SoftGPU {

class Clipper final {
    enum class ClipPlane : u8 {
        LEFT = 0,
        RIGHT,
        TOP,
        BOTTOM,
        NEAR,
        FAR
    };

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
    Clipper() = default;

    void clip_triangle_against_frustum(Vector<Vertex>& input_vecs);

private:
    Vertex clip_intersection_point(const Vertex& vec, const Vertex& prev_vec, ClipPlane plane);

    template<ClipPlane plane>
    constexpr bool point_within_clip_plane(const FloatVector4& vertex)
    {
        if constexpr (plane == ClipPlane::LEFT) {
            return vertex.x() >= -vertex.w();
        } else if constexpr (plane == ClipPlane::RIGHT) {
            return vertex.x() <= vertex.w();
        } else if constexpr (plane == ClipPlane::TOP) {
            return vertex.y() <= vertex.w();
        } else if constexpr (plane == ClipPlane::BOTTOM) {
            return vertex.y() >= -vertex.w();
        } else if constexpr (plane == ClipPlane::NEAR) {
            return vertex.z() >= -vertex.w();
        } else if constexpr (plane == ClipPlane::FAR) {
            return vertex.z() <= vertex.w();
        }

        return false;
    }

    template<ClipPlane plane>
    void clip_plane(AK::Vector<SoftGPU::Vertex>* write_to, AK::Vector<SoftGPU::Vertex>* read_from)
    {
        write_to->clear_with_capacity();

        for (size_t i = 0; i < read_from->size(); i++) {
            const auto& curr_vec = read_from->at((i + 1) % read_from->size());
            const auto& prev_vec = read_from->at(i);

            if (point_within_clip_plane<plane>(curr_vec.clip_coordinates)) {
                if (!point_within_clip_plane<plane>(prev_vec.clip_coordinates)) {
                    auto intersect = clip_intersection_point(prev_vec, curr_vec, plane);
                    write_to->append(intersect);
                }
                write_to->append(curr_vec);
            } else if (point_within_clip_plane<plane>(prev_vec.clip_coordinates)) {
                auto intersect = clip_intersection_point(prev_vec, curr_vec, plane);
                write_to->append(intersect);
            }
        }
        swap(write_to, read_from);
    }

    Vector<Vertex> list_a;
    Vector<Vertex> list_b;
};
}
