/*
 * Copyright (c) 2021, Pierre Hoffmeister
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGfx/Forward.h>
#include <LibGfx/PNGShared.h>

namespace Gfx {

class PNGChunk;

class PNGWriter {
public:
    static ErrorOr<ByteBuffer> encode(Gfx::Bitmap const&);

private:
    PNGWriter() = default;

    Vector<u8> m_data;
    ErrorOr<void> add_chunk(PNGChunk&);
    ErrorOr<void> add_png_header();
    ErrorOr<void> add_IHDR_chunk(u32 width, u32 height, u8 bit_depth, PNG::ColorType color_type, u8 compression_method, u8 filter_method, u8 interlace_method);
    ErrorOr<void> add_IDAT_chunk(Gfx::Bitmap const&);
    ErrorOr<void> add_IEND_chunk();
};

}
