/*
 * Copyright (c) 2021, Pierre Hoffmeister
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2024, Torben Jonas Virtmann
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/Vector.h>
#include <LibGfx/Forward.h>
#include <LibGfx/ImageFormats/AnimationWriter.h>
#include <LibGfx/ImageFormats/PNGShared.h>

namespace Gfx {

class PNGChunk;

// This is not a nested struct to work around https://llvm.org/PR36684
struct PNGWriterOptions {
    // Data for the iCCP chunk.
    // FIXME: Allow writing cICP, sRGB, or gAMA instead too.
    Optional<ReadonlyBytes> icc_data;
};

class PNGWriter {
public:
    using Options = PNGWriterOptions;

    static ErrorOr<ByteBuffer> encode(Gfx::Bitmap const&, Options options = Options {});
    static ErrorOr<NonnullOwnPtr<AnimationWriter>> start_encoding_animation(SeekableStream&, IntSize dimensions);

private:
    PNGWriter() = default;

    Vector<u8> m_data;
    ErrorOr<void> add_chunk(PNGChunk&);
    ErrorOr<void> add_png_header();
    ErrorOr<void> add_IHDR_chunk(u32 width, u32 height, u8 bit_depth, PNG::ColorType color_type, u8 compression_method, u8 filter_method, u8 interlace_method);
    ErrorOr<void> add_acTL_chunk(u32 num_frames, u32 num_plays);
    ErrorOr<void> add_fcTL_chunk(u32 sequence_number, u32 width, u32 height, u16 delay_num, u16 delay_den, u8 dispose_op, u8 blend_op, u32 x_offset, u32 y_offset);
    ErrorOr<void> add_iCCP_chunk(ReadonlyBytes icc_data);
    ErrorOr<void> add_IDAT_chunk(Gfx::Bitmap const&, bool animated, u32 sequence_number);
    ErrorOr<void> add_IEND_chunk();
};

}
