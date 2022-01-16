/*
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSoftGPU/StencilBuffer.h>

namespace SoftGPU {

ErrorOr<NonnullOwnPtr<StencilBuffer>> StencilBuffer::try_create(Gfx::IntSize const& size)
{
    auto rect = Gfx::IntRect { 0, 0, size.width(), size.height() };
    auto data = TRY(FixedArray<u8>::try_create(size.area()));
    return adopt_own(*new StencilBuffer(rect, move(data)));
}

StencilBuffer::StencilBuffer(Gfx::IntRect const& rect, FixedArray<u8> data)
    : m_data(move(data))
    , m_rect(rect)
{
}

void StencilBuffer::clear(Gfx::IntRect rect, u8 value)
{
    rect.intersect(m_rect);

    for (int y = rect.top(); y <= rect.bottom(); ++y) {
        auto* line = scanline(y);
        for (int x = rect.left(); x <= rect.right(); ++x)
            line[x] = value;
    }
}

u8* StencilBuffer::scanline(int y)
{
    VERIFY(m_rect.contains_vertically(y));
    return &m_data[y * m_rect.width()];
}

}
