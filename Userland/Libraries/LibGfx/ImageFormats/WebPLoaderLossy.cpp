/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/Format.h>
#include <AK/MemoryStream.h>
#include <AK/Vector.h>
#include <LibGfx/ImageFormats/WebPLoaderLossy.h>

// Lossy format: https://datatracker.ietf.org/doc/html/rfc6386

// Summary:
// A lossy webp image is a VP8 keyframe.
// A VP8 keyframe consists of 16x16 pixel tiles called macroblocks. Each macroblock is subdivided into 4x4 pixel tiles called subblocks.
// Pixel values are stored as YUV 4:2:0. That is, each 4x4 luma pixels are covered by 1 pixel U chroma and 1 pixel V chroma.
// This means one macroblock is covered by 4x4 Y subblocks and 2x2 U and V subblocks each.
// VP8 data consists of:
// * A tiny bit of uncompressed data, storing image dimensions and the size of the first compressed chunk of data, called the first partition
// * The first partition, which is a entropy-coded bitstream storing:
//   1. A fixed-size header.
//      The main piece of data this stores is a probability distribution for how pixel values of each metablock are predicted from previously decoded data.
//      It also stores how may independent entropy-coded bitstreams are used to store the actual pixel data (for all images I've seen so far, just one).
//   2. For each metablock, it stores how that metablock's pixel values are predicted from previously decoded data (and some more per-metablock metadata).
//      There are independent prediction modes for Y, U, V.
//      U and V store a single prediction mode per macroblock.
//      Y can store a single prediction mode per macroblock, or it can store one subblock prediction mode for each of the 4x4 luma subblocks.
// * One or more additional entropy-coded bitstreams ("partitions") that store the discrete cosine transform ("DCT") coefficients for the actual pixel data for each metablock.
//   Each metablock is subdivided into 4x4 tiles called "subblocks". A 16x16 pixel metablock consists of:
//   0. If the metablock stores 4x4 luma subblock prediction modes, the 4x4 DC coefficients of each subblock's DCT are stored at the start of the macroblock's data,
//      as coefficients of an inverse Walsh-Hadamard Transform (WHT).
//   1. 4x4 luma subblocks
//   2. 2x2 U chrome subblocks
//   3. 2x2 U chrome subblocks
//   That is, each metablock stores 24 or 25 sets of coefficients.
//   Each set of coefficients stores 16 numbers, using a combination of a custom prefix tree and dequantization.
//   The inverse DCT output is added to the output of the prediction.

namespace Gfx {

// https://developers.google.com/speed/webp/docs/riff_container#simple_file_format_lossy
// https://datatracker.ietf.org/doc/html/rfc6386#section-19 "Annex A: Bitstream Syntax"
ErrorOr<VP8Header> decode_webp_chunk_VP8_header(ReadonlyBytes vp8_data)
{
    if (vp8_data.size() < 10)
        return Error::from_string_literal("WebPImageDecoderPlugin: 'VP8 ' chunk too small");

    // FIXME: Eventually, this should probably call into LibVideo/VP8,
    // and image decoders should move into LibImageDecoders which depends on both LibGfx and LibVideo.
    // (LibVideo depends on LibGfx, so LibGfx can't depend on LibVideo itself.)

    // https://datatracker.ietf.org/doc/html/rfc6386#section-4 "Overview of Compressed Data Format"
    // "The decoder is simply presented with a sequence of compressed frames [...]
    //  The first frame presented to the decompressor is [...] a key frame.  [...]
    //  [E]very compressed frame has three or more pieces. It begins with an uncompressed data chunk comprising 10 bytes in the case of key frames"

    u8 const* data = vp8_data.data();

    // https://datatracker.ietf.org/doc/html/rfc6386#section-9.1 "Uncompressed Data Chunk"
    u32 frame_tag = data[0] | (data[1] << 8) | (data[2] << 16);
    bool is_key_frame = (frame_tag & 1) == 0; // https://www.rfc-editor.org/errata/eid5534
    u8 version = (frame_tag & 0xe) >> 1;
    bool show_frame = (frame_tag & 0x10) != 0;
    u32 size_of_first_partition = frame_tag >> 5;

    if (!is_key_frame)
        return Error::from_string_literal("WebPImageDecoderPlugin: 'VP8 ' chunk not a key frame");

    if (!show_frame)
        return Error::from_string_literal("WebPImageDecoderPlugin: 'VP8 ' chunk has invalid visibility for webp image");

    if (version > 3)
        return Error::from_string_literal("WebPImageDecoderPlugin: unknown version number in 'VP8 ' chunk");

    u32 start_code = data[3] | (data[4] << 8) | (data[5] << 16);
    if (start_code != 0x2a019d) // https://www.rfc-editor.org/errata/eid7370
        return Error::from_string_literal("WebPImageDecoderPlugin: 'VP8 ' chunk invalid start_code");

    // "The scaling specifications for each dimension are encoded as follows.
    //   0     | No upscaling (the most common case).
    //   1     | Upscale by 5/4.
    //   2     | Upscale by 5/3.
    //   3     | Upscale by 2."
    // This is a display-time operation and doesn't affect decoding."
    u16 width_and_horizontal_scale = data[6] | (data[7] << 8);
    u16 width = width_and_horizontal_scale & 0x3fff;
    u8 horizontal_scale = width_and_horizontal_scale >> 14;

    u16 heigth_and_vertical_scale = data[8] | (data[9] << 8);
    u16 height = heigth_and_vertical_scale & 0x3fff;
    u8 vertical_scale = heigth_and_vertical_scale >> 14;

    dbgln_if(WEBP_DEBUG, "version {}, show_frame {}, size_of_first_partition {}, width {}, horizontal_scale {}, height {}, vertical_scale {}",
        version, show_frame, size_of_first_partition, width, horizontal_scale, height, vertical_scale);

    return VP8Header { version, show_frame, size_of_first_partition, width, horizontal_scale, height, vertical_scale, vp8_data.slice(10) };
}

ErrorOr<NonnullRefPtr<Bitmap>> decode_webp_chunk_VP8_contents(VP8Header const& vp8_header, bool include_alpha_channel)
{
    auto bitmap_format = include_alpha_channel ? BitmapFormat::BGRA8888 : BitmapFormat::BGRx8888;

    // Uncomment this to test ALPH decoding for WebP-lossy-with-alpha images while lossy decoding isn't implemented yet.
#if 0
    return Bitmap::create(bitmap_format, { vp8_header.width, vp8_header.height });
#else
    // FIXME: Implement webp lossy decoding.
    (void)vp8_header;
    (void)bitmap_format;
    return Error::from_string_literal("WebPImageDecoderPlugin: decoding lossy webps not yet implemented");
#endif
}

}
