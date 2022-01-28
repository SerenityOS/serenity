/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGfx/Vector4.h>
#include <LibSoftGPU/Vertex.h>

namespace SoftGPU {

class Clipper final {
    static constexpr u8 NUMBER_OF_CLIPPING_PLANES = 6;

public:
    enum class ClipPlane : u8 {
        LEFT = 0,
        RIGHT,
        TOP,
        BOTTOM,
        NEAR,
        FAR
    };

    Clipper() = default;

    void clip_triangle_against_frustum(Vector<Vertex>& input_vecs);

private:
    Vector<Vertex> list_a;
    Vector<Vertex> list_b;
};

}
