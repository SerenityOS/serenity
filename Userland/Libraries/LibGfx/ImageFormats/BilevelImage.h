/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/RefCounted.h>
#include <LibGfx/Forward.h>

namespace Gfx {

enum class DitheringAlgorithm {
    None,

    Bayer2x2,
    Bayer4x4,
    Bayer8x8,

    FloydSteinberg,

    // FIXME: Add Atkinson, BlueNoise, Hilbert / Peano space-filling, ...
    // https://surma.dev/things/ditherpunk/
    // https://tannerhelland.com/2012/12/28/dithering-eleven-algorithms-source-code.html
};

class BilevelImage : public RefCounted<BilevelImage> {
public:
    static ErrorOr<NonnullRefPtr<BilevelImage>> create(size_t width, size_t height);
    static ErrorOr<NonnullRefPtr<BilevelImage>> create_from_byte_buffer(ByteBuffer bitmap, size_t width, size_t height);
    static ErrorOr<NonnullRefPtr<BilevelImage>> create_from_bitmap(Gfx::Bitmap const& bitmap, DitheringAlgorithm dithering_algorithm);

    ALWAYS_INLINE bool get_bit(size_t x, size_t y) const
    {
        VERIFY(x < m_width);
        VERIFY(y < m_height);
        size_t byte_offset = x / 8;
        size_t bit_offset = x % 8;
        u8 byte = m_bits[y * m_pitch + byte_offset];
        byte = (byte >> (8 - 1 - bit_offset)) & 1;
        return byte != 0;
    }

    ALWAYS_INLINE void set_bit(size_t x, size_t y, bool b)
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

    void fill(bool b);

    enum class CompositionType : u8 {
        Or = 0,
        And = 1,
        Xor = 2,
        XNor = 3,
        Replace = 4,
    };

    void composite_onto(BilevelImage& out, IntPoint position, CompositionType) const;

    ErrorOr<NonnullRefPtr<BilevelImage>> subbitmap(Gfx::IntRect const& rect) const;

    ErrorOr<NonnullRefPtr<Gfx::Bitmap>> to_gfx_bitmap() const;
    ErrorOr<ByteBuffer> to_byte_buffer() const;

    size_t width() const { return m_width; }
    size_t height() const { return m_height; }

    Bytes bytes() { return m_bits.bytes(); }

private:
    BilevelImage(ByteBuffer, size_t width, size_t height, size_t pitch);

    template<CompositionType>
    void composite_onto(BilevelImage& out, IntPoint position) const;

    ByteBuffer m_bits;
    size_t m_width { 0 };
    size_t m_height { 0 };
    size_t m_pitch { 0 };
};

}
