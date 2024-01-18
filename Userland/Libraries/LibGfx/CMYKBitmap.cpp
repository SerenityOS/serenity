/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/CMYKBitmap.h>

namespace Gfx {

ErrorOr<NonnullRefPtr<CMYKBitmap>> CMYKBitmap::create_with_size(IntSize const& size)
{
    VERIFY(size.width() >= 0 && size.height() >= 0);
    auto data = TRY(ByteBuffer::create_uninitialized(size.width() * size.height() * sizeof(CMYK)));
    return adopt_ref(*new CMYKBitmap(size, move(data)));
}

ErrorOr<RefPtr<Bitmap>> CMYKBitmap::to_low_quality_rgb() const
{
    if (!m_rgb_bitmap) {
        m_rgb_bitmap = TRY(Bitmap::create(BitmapFormat::BGRx8888, { m_size.width(), m_size.height() }));

        for (int y = 0; y < m_size.height(); ++y) {
            for (int x = 0; x < m_size.width(); ++x) {
                auto const& cmyk = scanline(y)[x];
                u8 k = 255 - cmyk.k;
                m_rgb_bitmap->scanline(y)[x] = Color((255 - cmyk.c) * k / 255, (255 - cmyk.m) * k / 255, (255 - cmyk.y) * k / 255).value();
            }
        }
    }

    return m_rgb_bitmap;
}

}
