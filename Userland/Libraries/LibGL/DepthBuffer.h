/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Size.h>

namespace GL {

class DepthBuffer final {
public:
    DepthBuffer(Gfx::IntSize const&);
    ~DepthBuffer();

    float* scanline(int y);

    void clear(float depth);

private:
    Gfx::IntSize m_size;
    float* m_data { nullptr };
};

}
