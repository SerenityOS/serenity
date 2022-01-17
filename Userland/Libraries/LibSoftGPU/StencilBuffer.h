/*
 * Copyright (c) 2021, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/FixedArray.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Try.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>

namespace SoftGPU {

class StencilBuffer final {
public:
    static ErrorOr<NonnullOwnPtr<StencilBuffer>> try_create(Gfx::IntSize const& size);

    void clear(Gfx::IntRect rect, u8 value);
    Gfx::IntRect const& rect() const { return m_rect; }
    u8* scanline(int y);

private:
    StencilBuffer(Gfx::IntRect const& rect, FixedArray<u8> data);

    FixedArray<u8> m_data;
    Gfx::IntRect m_rect;
};

}
