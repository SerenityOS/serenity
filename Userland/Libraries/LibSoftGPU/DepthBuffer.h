/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>

namespace SoftGPU {

class DepthBuffer final {
public:
    DepthBuffer(Gfx::IntSize const&);
    ~DepthBuffer();

    float* scanline(int y);

    void clear(float depth);
    void clear(Gfx::IntRect bounds, float depth);

private:
    Gfx::IntSize m_size;
    float* m_data { nullptr };
};

}
