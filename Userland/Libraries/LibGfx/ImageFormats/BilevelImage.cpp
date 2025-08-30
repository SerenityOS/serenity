/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullOwnPtr.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/BilevelImage.h>

namespace Gfx {

ErrorOr<NonnullOwnPtr<BilevelImage>> BilevelImage::create(size_t width, size_t height)
{
    size_t pitch = ceil_div(width, static_cast<size_t>(8));
    auto bits = TRY(ByteBuffer::create_uninitialized(pitch * height));
    return adopt_nonnull_own_or_enomem(new (nothrow) BilevelImage(move(bits), width, height, pitch));
}

bool BilevelImage::get_bit(size_t x, size_t y) const
{
    VERIFY(x < m_width);
    VERIFY(y < m_height);
    size_t byte_offset = x / 8;
    size_t bit_offset = x % 8;
    u8 byte = m_bits[y * m_pitch + byte_offset];
    byte = (byte >> (8 - 1 - bit_offset)) & 1;
    return byte != 0;
}

void BilevelImage::set_bit(size_t x, size_t y, bool b)
{
    VERIFY(x < m_width);
    VERIFY(y < m_height);
    size_t byte_offset = x / 8;
    size_t bit_offset = x % 8;
    u8 byte = m_bits[y * m_pitch + byte_offset];
    u8 mask = 1u << (8 - 1 - bit_offset);
    if (b)
        byte |= mask;
    else
        byte &= ~mask;
    m_bits[y * m_pitch + byte_offset] = byte;
}

void BilevelImage::fill(bool b)
{
    u8 fill_byte = b ? 0xff : 0;
    for (auto& byte : m_bits.bytes())
        byte = fill_byte;
}

ErrorOr<NonnullOwnPtr<BilevelImage>> BilevelImage::subbitmap(Gfx::IntRect const& rect) const
{
    VERIFY(rect.x() >= 0);
    VERIFY(rect.width() >= 0);
    VERIFY(static_cast<size_t>(rect.right()) <= width());

    VERIFY(rect.y() >= 0);
    VERIFY(rect.height() >= 0);
    VERIFY(static_cast<size_t>(rect.bottom()) <= height());

    auto subbitmap = TRY(create(rect.width(), rect.height()));
    for (int y = 0; y < rect.height(); ++y)
        for (int x = 0; x < rect.width(); ++x)
            subbitmap->set_bit(x, y, get_bit(rect.x() + x, rect.y() + y));
    return subbitmap;
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> BilevelImage::to_gfx_bitmap() const
{
    auto bitmap = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { m_width, m_height }));
    for (size_t y = 0; y < m_height; ++y) {
        for (size_t x = 0; x < m_width; ++x) {
            auto color = get_bit(x, y) ? Color::Black : Color::White;
            bitmap->set_pixel(x, y, color);
        }
    }
    return bitmap;
}

ErrorOr<ByteBuffer> BilevelImage::to_byte_buffer() const
{
    return ByteBuffer::copy(m_bits);
}

BilevelImage::BilevelImage(ByteBuffer bits, size_t width, size_t height, size_t pitch)
    : m_bits(move(bits))
    , m_width(width)
    , m_height(height)
    , m_pitch(pitch)
{
}

}
