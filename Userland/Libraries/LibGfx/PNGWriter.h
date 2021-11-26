/*
 * Copyright (c) 2021, Pierre Hoffmeister
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGfx/Forward.h>

namespace Gfx {

class PNGChunk;

class PNGWriter {
public:
    static ByteBuffer encode(Gfx::Bitmap const&);

private:
    PNGWriter() { }

    Vector<u8> m_data;
    void add_chunk(PNGChunk&);
    void add_png_header();
    void add_IHDR_chunk(u32 width, u32 height, u8 bit_depth, u8 color_type, u8 compression_method, u8 filter_method, u8 interlace_method);
    void add_IDAT_chunk(Gfx::Bitmap const&);
    void add_IEND_chunk();
};

}
