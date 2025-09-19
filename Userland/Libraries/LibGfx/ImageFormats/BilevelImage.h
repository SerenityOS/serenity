/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <LibGfx/Forward.h>

namespace Gfx {

enum class DitheringAlgorithm {
    None,
    FloydSteinberg,
    // FIXME: Add Atkinson, Bayer in 2-3 sizes, BlueNoise, Hilbert / Peano space-filling, ...
    // https://surma.dev/things/ditherpunk/
    // https://tannerhelland.com/2012/12/28/dithering-eleven-algorithms-source-code.html
};

class BilevelImage {
public:
    static ErrorOr<NonnullOwnPtr<BilevelImage>> create(size_t width, size_t height);
    static ErrorOr<NonnullOwnPtr<BilevelImage>> create_from_byte_buffer(ByteBuffer bitmap, size_t width, size_t height);
    static ErrorOr<NonnullOwnPtr<BilevelImage>> create_from_bitmap(Gfx::Bitmap const& bitmap, DitheringAlgorithm dithering_algorithm);

    bool get_bit(size_t x, size_t y) const;
    void set_bit(size_t x, size_t y, bool b);
    void fill(bool b);

    ErrorOr<NonnullOwnPtr<BilevelImage>> subbitmap(Gfx::IntRect const& rect) const;

    ErrorOr<NonnullRefPtr<Gfx::Bitmap>> to_gfx_bitmap() const;
    ErrorOr<ByteBuffer> to_byte_buffer() const;

    size_t width() const { return m_width; }
    size_t height() const { return m_height; }

    Bytes bytes() { return m_bits.bytes(); }

private:
    BilevelImage(ByteBuffer, size_t width, size_t height, size_t pitch);

    ByteBuffer m_bits;
    size_t m_width { 0 };
    size_t m_height { 0 };
    size_t m_pitch { 0 };
};

}
