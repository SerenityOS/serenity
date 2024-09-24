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
#include <LibGfx/FourCC.h>
#include <LibGfx/ImageFormats/WebPLoader.h>
#include <LibGfx/ImageFormats/WebPLoaderLossless.h>
#include <LibGfx/ImageFormats/WebPLoaderLossy.h>
#include <LibGfx/ImageFormats/WebPShared.h>
#include <LibGfx/Painter.h>
#include <LibRIFF/ChunkID.h>
#include <LibRIFF/RIFF.h>

// Overview: https://developers.google.com/speed/webp/docs/compression
// Container: https://developers.google.com/speed/webp/docs/riff_container

namespace Gfx {

namespace {

// "For a still image, the image data consists of a single frame, which is made up of:
//     An optional alpha subchunk.
//     A bitstream subchunk."
struct ImageData {
    // "This optional chunk contains encoded alpha data for this frame. A frame containing a 'VP8L' chunk SHOULD NOT contain this chunk."
    Optional<RIFF::Chunk> alpha_chunk; // 'ALPH'
    RIFF::Chunk image_data_chunk;      // Either 'VP8 ' or 'VP8L'. For 'VP8L', alpha_chunk will not have a value.
};

}

struct WebPLoadingContext {
    enum State {
        NotDecoded = 0,
        Error,
        HeaderDecoded,
        FirstChunkRead,
        FirstChunkDecoded,
        ChunksDecoded,
        AnimationFrameChunksDecoded,
        BitmapDecoded,
    };
    State state { State::NotDecoded };
    ReadonlyBytes data;

    ReadonlyBytes chunks_cursor;

    Optional<IntSize> size;

    RefPtr<Gfx::Bitmap> bitmap;

    // Either 'VP8 ' (simple lossy file), 'VP8L' (simple lossless file), or 'VP8X' (extended file).
    Optional<RIFF::Chunk> first_chunk;

    // Only valid if first_chunk->type == 'VP8X'.
    VP8XHeader vp8x_header;

    // If first_chunk is not a VP8X chunk, then only image_data.image_data_chunk is set and all the other Chunks are not set.
    // Once state is >= ChunksDecoded, for non-animated images, this will have a value, or decoding will have failed.
    Optional<ImageData> image_data;

    Optional<RIFF::Chunk> animation_header_chunk; // 'ANIM'
    Vector<RIFF::Chunk> animation_frame_chunks;   // 'ANMF'

    // These are set in state >= AnimationFrameChunksDecoded, if first_chunk.type == 'VP8X' && vp8x_header.has_animation.
    Optional<ANIMChunk> animation_header_chunk_data;
    Optional<Vector<ANMFChunk>> animation_frame_chunks_data;
    size_t current_frame { 0 };

    Optional<RIFF::Chunk> iccp_chunk; // 'ICCP'
    Optional<RIFF::Chunk> exif_chunk; // 'EXIF'
    Optional<RIFF::Chunk> xmp_chunk;  // 'XMP '
};

// https://developers.google.com/speed/webp/docs/riff_container#webp_file_header
static ErrorOr<void> decode_webp_header(WebPLoadingContext& context)
{
    if (context.state >= WebPLoadingContext::HeaderDecoded)
        return {};

    FixedMemoryStream header_stream { context.data };
    auto header = TRY(header_stream.read_value<RIFF::FileHeader>());
    if (header.magic() != RIFF::riff_magic || header.subformat != "WEBP"sv)
        return Error::from_string_literal("Invalid WebP header");

    // "File Size: [...] The size of the file in bytes starting at offset 8. The maximum value of this field is 2^32 minus 10 bytes."
    u32 const maximum_webp_file_size = 0xffff'ffff - 9;
    if (header.file_size() > maximum_webp_file_size)
        return Error::from_string_literal("WebP header file size over maximum");

    // "The file size in the header is the total size of the chunks that follow plus 4 bytes for the 'WEBP' RIFF::ChunkID.
    //  The file SHOULD NOT contain any data after the data specified by File Size.
    //  Readers MAY parse such files, ignoring the trailing data."
    if (context.data.size() - 8 < header.file_size())
        return Error::from_string_literal("WebP data too small for size in header");
    if (header.file_size() < 4) // Need at least 4 bytes for 'WEBP', else we'll trim to less than the header size below.
        return Error::from_string_literal("WebP stored file size too small for header it's stored in");
    if (context.data.size() - 8 > header.file_size()) {
        dbgln_if(WEBP_DEBUG, "WebP has {} bytes of data, but header needs only {}. Trimming.", context.data.size(), header.file_size() + 8);
        context.data = context.data.trim(header.file_size() + 8);
    }

    context.state = WebPLoadingContext::HeaderDecoded;
    return {};
}

// https://developers.google.com/speed/webp/docs/riff_container#alpha
static ErrorOr<void> decode_webp_chunk_ALPH(RIFF::Chunk const& alph_chunk, Bitmap& bitmap)
{
    VERIFY(alph_chunk.id() == "ALPH"sv);

    if (alph_chunk.size() < 1)
        return Error::from_string_literal("WebPImageDecoderPlugin: ALPH chunk too small");

    u8 flags = alph_chunk[0];
    u8 preprocessing = (flags >> 4) & 3;
    u8 filtering_method = (flags >> 2) & 3;
    u8 compression_method = flags & 3;

    dbgln_if(WEBP_DEBUG, "ALPH: preprocessing {} filtering_method {} compression_method {}", preprocessing, filtering_method, compression_method);

    ReadonlyBytes alpha_data = alph_chunk.data().slice(1);

    size_t pixel_count = bitmap.width() * bitmap.height();

    auto alpha = TRY(ByteBuffer::create_uninitialized(pixel_count));

    if (compression_method == 0) {
        // "Raw data: consists of a byte sequence of length width * height, containing all the 8-bit transparency values in scan order."
        if (alpha_data.size() < pixel_count)
            return Error::from_string_literal("WebPImageDecoderPlugin: uncompressed ALPH data too small");
        memcpy(alpha.data(), alpha_data.data(), pixel_count);
    } else {
        // "Lossless format compression: the byte sequence is a compressed image-stream (as described in the WebP Lossless Bitstream Format)
        //  of implicit dimension width x height. That is, this image-stream does NOT contain any headers describing the image dimension.
        //  Once the image-stream is decoded into ARGB color values, following the process described in the lossless format specification,
        //  the transparency information must be extracted from the green channel of the ARGB quadruplet."
        VP8LHeader vp8l_header { static_cast<u16>(bitmap.width()), static_cast<u16>(bitmap.height()), /*is_alpha_used=*/false, alpha_data };
        auto lossless_bitmap = TRY(decode_webp_chunk_VP8L_contents(vp8l_header));

        if (pixel_count != static_cast<size_t>(lossless_bitmap->width() * lossless_bitmap->height()))
            return Error::from_string_literal("WebPImageDecoderPlugin: decompressed ALPH dimensions don't match VP8 dimensions");

        for (size_t i = 0; i < pixel_count; ++i)
            alpha[i] = (lossless_bitmap->begin()[i] & 0xff00) >> 8;
    }

    // "For each pixel, filtering is performed using the following calculations. Assume the alpha values surrounding the current X position are labeled as:
    //
    //  C | B |
    // ---+---+
    //  A | X |
    // [...]
    //
    // The final value is derived by adding the decompressed value X to the predictor and using modulo-256 arithmetic"
    switch (filtering_method) {
    case 0:
        // "Method 0: predictor = 0"
        // Nothing to do.
        break;

    case 1:
        // "Method 1: predictor = A"
        // "The top-left value at location (0, 0) uses 0 as predictor value. Otherwise,
        //  For horizontal or gradient filtering methods, the left-most pixels at location (0, y) are predicted using the location (0, y-1) just above."
        for (int y = 1; y < bitmap.height(); ++y)
            alpha[y * bitmap.width()] += alpha[(y - 1) * bitmap.width()];
        for (int y = 0; y < bitmap.height(); ++y) {
            for (int x = 1; x < bitmap.width(); ++x) {
                u8 A = alpha[y * bitmap.width() + (x - 1)];
                alpha[y * bitmap.width() + x] += A;
            }
        }
        break;

    case 2:
        // "Method 2: predictor = B"
        // "The top-left value at location (0, 0) uses 0 as predictor value. Otherwise,
        //  For vertical or gradient filtering methods, the top-most pixels at location (x, 0) are predicted using the location (x-1, 0) on the left."
        for (int x = 1; x < bitmap.width(); ++x)
            alpha[x] += alpha[x - 1];
        for (int y = 1; y < bitmap.height(); ++y) {
            for (int x = 0; x < bitmap.width(); ++x) {
                u8 B = alpha[(y - 1) * bitmap.width() + x];
                alpha[y * bitmap.width() + x] += B;
            }
        }
        break;

    case 3:
        // "Method 3: predictor = clip(A + B - C)"
        //  where clip(v) is equal to:
        //  * 0 if v < 0
        //  * 255 if v > 255
        //  * v otherwise"
        // "The top-left value at location (0, 0) uses 0 as predictor value. Otherwise,
        //  For horizontal or gradient filtering methods, the left-most pixels at location (0, y) are predicted using the location (0, y-1) just above.
        //  For vertical or gradient filtering methods, the top-most pixels at location (x, 0) are predicted using the location (x-1, 0) on the left."
        for (int x = 1; x < bitmap.width(); ++x)
            alpha[x] += alpha[x - 1];
        for (int y = 1; y < bitmap.height(); ++y)
            alpha[y * bitmap.width()] += alpha[(y - 1) * bitmap.width()];
        for (int y = 1; y < bitmap.height(); ++y) {
            for (int x = 1; x < bitmap.width(); ++x) {
                u8 A = alpha[y * bitmap.width() + (x - 1)];
                u8 B = alpha[(y - 1) * bitmap.width() + x];
                u8 C = alpha[(y - 1) * bitmap.width() + (x - 1)];
                alpha[y * bitmap.width() + x] += clamp(A + B - C, 0, 255);
            }
        }
        break;

    default:
        return Error::from_string_literal("WebPImageDecoderPlugin: uncompressed ALPH invalid filtering method");
    }

    for (size_t i = 0; i < pixel_count; ++i)
        bitmap.begin()[i] = alpha[i] << 24 | (bitmap.begin()[i] & 0xffffff);

    return {};
}

static ErrorOr<VP8XHeader> decode_webp_chunk_VP8X(RIFF::Chunk const& vp8x_chunk)
{
    VERIFY(vp8x_chunk.id() == "VP8X"sv);

    // The VP8X chunk is documented at "Extended WebP file header:" at the end of
    // https://developers.google.com/speed/webp/docs/riff_container#extended_file_format
    if (vp8x_chunk.size() < 10)
        return Error::from_string_literal("WebPImageDecoderPlugin: VP8X chunk too small");

    // 1 byte flags
    // "Reserved (Rsv): 2 bits   MUST be 0. Readers MUST ignore this field.
    //  ICC profile (I): 1 bit   Set if the file contains an ICC profile.
    //  Alpha (L): 1 bit         Set if any of the frames of the image contain transparency information ("alpha").
    //  Exif metadata (E): 1 bit Set if the file contains Exif metadata.
    //  XMP metadata (X): 1 bit  Set if the file contains XMP metadata.
    //  Animation (A): 1 bit     Set if this is an animated image. Data in 'ANIM' and 'ANMF' chunks should be used to control the animation.
    //  Reserved (R): 1 bit      MUST be 0. Readers MUST ignore this field."
    u8 flags = vp8x_chunk[0];
    bool has_icc = flags & 0x20;
    bool has_alpha = flags & 0x10;
    bool has_exif = flags & 0x8;
    bool has_xmp = flags & 0x4;
    bool has_animation = flags & 0x2;

    // 3 bytes reserved
    // 3 bytes width minus one
    u32 width = (vp8x_chunk[4] | (vp8x_chunk[5] << 8) | (vp8x_chunk[6] << 16)) + 1;

    // 3 bytes height minus one
    u32 height = (vp8x_chunk[7] | (vp8x_chunk[8] << 8) | (vp8x_chunk[9] << 16)) + 1;

    dbgln_if(WEBP_DEBUG, "VP8X: flags {:#x} --{}{}{}{}{}{}, width {}, height {}",
        flags,
        has_icc ? " icc" : "",
        has_alpha ? " alpha" : "",
        has_exif ? " exif" : "",
        has_xmp ? " xmp" : "",
        has_animation ? " anim" : "",
        (flags & 0x3e) == 0 ? " none" : "",
        width, height);

    return VP8XHeader { has_icc, has_alpha, has_exif, has_xmp, has_animation, width, height };
}

// https://developers.google.com/speed/webp/docs/riff_container#animation
static ErrorOr<ANIMChunk> decode_webp_chunk_ANIM(RIFF::Chunk const& anim_chunk)
{
    VERIFY(anim_chunk.id() == "ANIM"sv);
    if (anim_chunk.size() < 6)
        return Error::from_string_literal("WebPImageDecoderPlugin: ANIM chunk too small");

    u32 background_color = (u32)anim_chunk[0] | ((u32)anim_chunk[1] << 8) | ((u32)anim_chunk[2] << 16) | ((u32)anim_chunk[3] << 24);
    u16 loop_count = anim_chunk[4] | (anim_chunk[5] << 8);

    dbgln_if(WEBP_DEBUG, "ANIM: background_color {:x} loop_count {}", background_color, loop_count);

    return ANIMChunk { background_color, loop_count };
}

// https://developers.google.com/speed/webp/docs/riff_container#animation
static ErrorOr<ANMFChunk> decode_webp_chunk_ANMF(WebPLoadingContext& context, RIFF::Chunk const& anmf_chunk)
{
    VERIFY(anmf_chunk.id() == "ANMF"sv);
    if (anmf_chunk.size() < 16)
        return Error::from_string_literal("WebPImageDecoderPlugin: ANMF chunk too small");

    // "The X coordinate of the upper left corner of the frame is Frame X * 2."
    u32 frame_x = ((u32)anmf_chunk[0] | ((u32)anmf_chunk[1] << 8) | ((u32)anmf_chunk[2] << 16)) * 2;

    // "The Y coordinate of the upper left corner of the frame is Frame Y * 2."
    u32 frame_y = ((u32)anmf_chunk[3] | ((u32)anmf_chunk[4] << 8) | ((u32)anmf_chunk[5] << 16)) * 2;

    // "The frame width is 1 + Frame Width Minus One."
    u32 frame_width = ((u32)anmf_chunk[6] | ((u32)anmf_chunk[7] << 8) | ((u32)anmf_chunk[8] << 16)) + 1;

    // "The frame height is 1 + Frame Height Minus One."
    u32 frame_height = ((u32)anmf_chunk[9] | ((u32)anmf_chunk[10] << 8) | ((u32)anmf_chunk[11] << 16)) + 1;

    // "The time to wait before displaying the next frame, in 1 millisecond units.
    //  Note the interpretation of frame duration of 0 (and often <= 10) is implementation defined.
    //  Many tools and browsers assign a minimum duration similar to GIF."
    u32 frame_duration = (u32)anmf_chunk[12] | ((u32)anmf_chunk[13] << 8) | ((u32)anmf_chunk[14] << 16);

    u8 flags = anmf_chunk[15];
    auto blending_method = static_cast<ANMFChunkHeader::BlendingMethod>((flags >> 1) & 1);
    auto disposal_method = static_cast<ANMFChunkHeader::DisposalMethod>(flags & 1);

    dbgln_if(WEBP_DEBUG, "ANMF: frame_x {} frame_y {} frame_width {} frame_height {} frame_duration {} blending_method {} disposal_method {}",
        frame_x, frame_y, frame_width, frame_height, frame_duration, (int)blending_method, (int)disposal_method);

    // https://developers.google.com/speed/webp/docs/riff_container#assembling_the_canvas_from_frames
    // "assert VP8X.canvasWidth >= frame_right
    //  assert VP8X.canvasHeight >= frame_bottom"
    VERIFY(context.first_chunk->id() == "VP8X"sv);
    if (frame_x + frame_width > context.vp8x_header.width || frame_y + frame_height > context.vp8x_header.height)
        return Error::from_string_literal("WebPImageDecoderPlugin: ANMF dimensions out of bounds");

    auto header = ANMFChunkHeader { frame_x, frame_y, frame_width, frame_height, frame_duration, blending_method, disposal_method };
    ReadonlyBytes frame_data = anmf_chunk.data().slice(16);
    return ANMFChunk { header, frame_data };
}

static ErrorOr<ImageData> decode_webp_set_image_data(Optional<RIFF::Chunk> alpha, Optional<RIFF::Chunk> image_data)
{
    if (!image_data.has_value())
        return Error::from_string_literal("WebPImageDecoderPlugin: missing image data");

    // https://developers.google.com/speed/webp/docs/riff_container#alpha
    // "A frame containing a 'VP8L' chunk SHOULD NOT contain this chunk."
    if (alpha.has_value() && image_data->id() == "VP8L"sv) {
        dbgln_if(WEBP_DEBUG, "WebPImageDecoderPlugin: VP8L frames should not have ALPH chunks. Ignoring ALPH chunk.");
        alpha.clear();
    }

    return ImageData { move(alpha), image_data.value() };
}

// https://developers.google.com/speed/webp/docs/riff_container#extended_file_format
static ErrorOr<void> decode_webp_extended(WebPLoadingContext& context, ReadonlyBytes chunks)
{
    VERIFY(context.first_chunk->id() == "VP8X"sv);

    Optional<RIFF::Chunk> alpha, image_data;

    // FIXME: This isn't quite to spec, which says
    // "All chunks SHOULD be placed in the same order as listed above.
    //  If a chunk appears in the wrong place, the file is invalid, but readers MAY parse the file, ignoring the chunks that are out of order."
    auto store = [](auto& field, RIFF::Chunk const& chunk) {
        if (!field.has_value())
            field = chunk;
    };
    while (!chunks.is_empty()) {
        auto chunk = TRY(RIFF::Chunk::decode_and_advance(chunks));

        if (chunk.id() == "ICCP"sv)
            store(context.iccp_chunk, chunk);
        else if (chunk.id() == "ALPH"sv)
            store(alpha, chunk);
        else if (chunk.id() == "ANIM"sv)
            store(context.animation_header_chunk, chunk);
        else if (chunk.id() == "ANMF"sv)
            TRY(context.animation_frame_chunks.try_append(chunk));
        else if (chunk.id() == "EXIF"sv)
            store(context.exif_chunk, chunk);
        else if (chunk.id() == "XMP "sv)
            store(context.xmp_chunk, chunk);
        else if (chunk.id() == "VP8 "sv || chunk.id() == "VP8L"sv)
            store(image_data, chunk);
    }

    // Validate chunks.

    // https://developers.google.com/speed/webp/docs/riff_container#animation
    // "ANIM Chunk: [...] This chunk MUST appear if the Animation flag in the VP8X chunk is set. If the Animation flag is not set and this chunk is present, it MUST be ignored."
    if (context.vp8x_header.has_animation && !context.animation_header_chunk.has_value())
        return Error::from_string_literal("WebPImageDecoderPlugin: Header claims animation, but no ANIM chunk");
    if (!context.vp8x_header.has_animation && context.animation_header_chunk.has_value()) {
        dbgln_if(WEBP_DEBUG, "WebPImageDecoderPlugin: Header claims no animation, but ANIM chunk present. Ignoring ANIM chunk.");
        context.animation_header_chunk.clear();
    }

    // "ANMF Chunk: [...] If the Animation flag is not set, then this chunk SHOULD NOT be present."
    if (!context.vp8x_header.has_animation && !context.animation_frame_chunks.is_empty()) {
        dbgln_if(WEBP_DEBUG, "WebPImageDecoderPlugin: Header claims no animation, but ANMF chunks present. Ignoring ANMF chunks.");
        context.animation_frame_chunks.clear();
    }

    // Image data is not optional -- but the spec doesn't explicitly say that an animated image must have more than 0 frames.
    // The spec also doesn't say that animated images must not contain a regular image data segment.
    if (!context.vp8x_header.has_animation || image_data.has_value())
        context.image_data = TRY(decode_webp_set_image_data(move(alpha), move(image_data)));

    // https://developers.google.com/speed/webp/docs/riff_container#color_profile
    // "This chunk MUST appear before the image data."
    if (context.iccp_chunk.has_value()
        && ((context.image_data.has_value()
                && (context.iccp_chunk->data().data() > context.image_data->image_data_chunk.data().data()
                    || (context.image_data->alpha_chunk.has_value() && context.iccp_chunk->data().data() > context.image_data->alpha_chunk->data().data())))
            || (!context.animation_frame_chunks.is_empty() && context.iccp_chunk->data().data() > context.animation_frame_chunks[0].data().data()))) {
        return Error::from_string_literal("WebPImageDecoderPlugin: ICCP chunk is after image data");
    }

    if (context.iccp_chunk.has_value() && !context.vp8x_header.has_icc)
        return Error::from_string_literal("WebPImageDecoderPlugin: ICCP chunk present, but VP8X header claims no ICC profile");
    if (!context.iccp_chunk.has_value() && context.vp8x_header.has_icc)
        return Error::from_string_literal("WebPImageDecoderPlugin: VP8X header claims ICC profile, but no ICCP chunk present");

    context.state = WebPLoadingContext::State::ChunksDecoded;
    return {};
}

static ErrorOr<void> read_webp_first_chunk(WebPLoadingContext& context)
{
    if (context.state >= WebPLoadingContext::State::FirstChunkRead)
        return {};

    context.chunks_cursor = context.data.slice(sizeof(RIFF::FileHeader));
    auto first_chunk = TRY(RIFF::Chunk::decode_and_advance(context.chunks_cursor));

    if (first_chunk.id() != "VP8 "sv && first_chunk.id() != "VP8L"sv && first_chunk.id() != "VP8X"sv)
        return Error::from_string_literal("WebPImageDecoderPlugin: Invalid first chunk type");

    context.first_chunk = first_chunk;
    context.state = WebPLoadingContext::State::FirstChunkRead;

    if (first_chunk.id() == "VP8 "sv || first_chunk.id() == "VP8L"sv)
        context.image_data = TRY(decode_webp_set_image_data(OptionalNone {}, first_chunk));

    return {};
}

static ErrorOr<void> decode_webp_first_chunk(WebPLoadingContext& context)
{
    if (context.state >= WebPLoadingContext::State::FirstChunkDecoded)
        return {};

    if (context.state < WebPLoadingContext::FirstChunkRead)
        TRY(read_webp_first_chunk(context));

    if (context.first_chunk->id() == "VP8 "sv) {
        auto vp8_header = TRY(decode_webp_chunk_VP8_header(context.first_chunk->data()));
        context.size = IntSize { vp8_header.width, vp8_header.height };
        context.state = WebPLoadingContext::State::FirstChunkDecoded;
        return {};
    }
    if (context.first_chunk->id() == "VP8L"sv) {
        auto vp8l_header = TRY(decode_webp_chunk_VP8L_header(context.first_chunk->data()));
        context.size = IntSize { vp8l_header.width, vp8l_header.height };
        context.state = WebPLoadingContext::State::FirstChunkDecoded;
        return {};
    }
    VERIFY(context.first_chunk->id() == "VP8X"sv);
    context.vp8x_header = TRY(decode_webp_chunk_VP8X(context.first_chunk.value()));
    context.size = IntSize { context.vp8x_header.width, context.vp8x_header.height };
    context.state = WebPLoadingContext::State::FirstChunkDecoded;
    return {};
}

static ErrorOr<void> decode_webp_chunks(WebPLoadingContext& context)
{
    if (context.state >= WebPLoadingContext::State::ChunksDecoded)
        return {};

    VERIFY(context.state >= WebPLoadingContext::FirstChunkDecoded);

    if (context.first_chunk->id() == "VP8X"sv)
        return decode_webp_extended(context, context.chunks_cursor);

    context.state = WebPLoadingContext::State::ChunksDecoded;
    return {};
}

static ErrorOr<void> decode_webp_animation_frame_chunks(WebPLoadingContext& context)
{
    if (context.state >= WebPLoadingContext::State::AnimationFrameChunksDecoded)
        return {};

    VERIFY(context.state == WebPLoadingContext::State::ChunksDecoded);

    context.animation_header_chunk_data = TRY(decode_webp_chunk_ANIM(context.animation_header_chunk.value()));

    Vector<ANMFChunk> decoded_chunks;
    TRY(decoded_chunks.try_ensure_capacity(context.animation_frame_chunks.size()));
    for (auto const& chunk : context.animation_frame_chunks)
        TRY(decoded_chunks.try_append(TRY(decode_webp_chunk_ANMF(context, chunk))));
    context.animation_frame_chunks_data = move(decoded_chunks);

    context.state = WebPLoadingContext::State::AnimationFrameChunksDecoded;
    return {};
}

static ErrorOr<ImageData> decode_webp_animation_frame_image_data(ANMFChunk const& frame)
{
    ReadonlyBytes chunks = frame.frame_data;
    auto chunk = TRY(RIFF::Chunk::decode_and_advance(chunks));

    Optional<RIFF::Chunk> alpha, image_data;
    if (chunk.id() == "ALPH"sv) {
        alpha = chunk;
        chunk = TRY(RIFF::Chunk::decode_and_advance(chunks));
    }
    if (chunk.id() == "VP8 "sv || chunk.id() == "VP8L"sv)
        image_data = chunk;

    return decode_webp_set_image_data(move(alpha), move(image_data));
}

static ErrorOr<NonnullRefPtr<Bitmap>> decode_webp_image_data(WebPLoadingContext& context, ImageData const& image_data)
{
    if (image_data.image_data_chunk.id() == "VP8L"sv) {
        VERIFY(!image_data.alpha_chunk.has_value());
        auto vp8l_header = TRY(decode_webp_chunk_VP8L_header(image_data.image_data_chunk.data()));

        // Check that the VP8X header alpha flag matches the VP8L header alpha flag.
        // FIXME: For animated images, if VP8X has alpha then at least one frame should have alpha. But we currently don't check this for animations.
        if (context.first_chunk->id() == "VP8X" && !context.animation_frame_chunks_data.has_value() && context.vp8x_header.has_alpha != vp8l_header.is_alpha_used)
            return Error::from_string_literal("WebPImageDecoderPlugin: VP8X header alpha flag doesn't match VP8L header");

        return decode_webp_chunk_VP8L_contents(vp8l_header);
    }

    VERIFY(image_data.image_data_chunk.id() == "VP8 "sv);
    auto vp8_header = TRY(decode_webp_chunk_VP8_header(image_data.image_data_chunk.data()));
    auto bitmap = TRY(decode_webp_chunk_VP8_contents(vp8_header, image_data.alpha_chunk.has_value()));

    if (image_data.alpha_chunk.has_value())
        TRY(decode_webp_chunk_ALPH(image_data.alpha_chunk.value(), *bitmap));

    return bitmap;
}

// https://developers.google.com/speed/webp/docs/riff_container#canvas_assembly_from_frames
static ErrorOr<ImageFrameDescriptor> decode_webp_animation_frame(WebPLoadingContext& context, size_t frame_index)
{
    if (frame_index >= context.animation_frame_chunks_data->size())
        return Error::from_string_literal("frame_index size too high");

    VERIFY(context.first_chunk->id() == "VP8X"sv);
    VERIFY(context.vp8x_header.has_animation);

    // The spec says
    // "canvas ← new image of size VP8X.canvasWidth x VP8X.canvasHeight with
    //     background color ANIM.background_color."
    // But:
    // * libwebp always fills with transparent black (#00000000)
    // * some images (e.g. images written by Aseprite) set the background color to fully opaque white
    // These images then end up with a nice transparent background in libwebp-based decoders (i.e. basically everywhere)
    // but show a silly opaque border in ours. So don't use context.animation_header_chunk_data->background_color here.
    Color clear_color(Color::Transparent);

    size_t start_frame = context.current_frame + 1;
    dbgln_if(WEBP_DEBUG, "start_frame {} context.current_frame {}", start_frame, context.current_frame);
    if (context.state < WebPLoadingContext::State::BitmapDecoded) {
        start_frame = 0;
        auto format = context.vp8x_header.has_alpha ? BitmapFormat::BGRA8888 : BitmapFormat::BGRx8888;
        context.bitmap = TRY(Bitmap::create(format, { context.vp8x_header.width, context.vp8x_header.height }));
        if (clear_color != Color(Color::Transparent)) // Bitmaps start out transparent, so only fill if not transparent.
            context.bitmap->fill(clear_color);
    } else if (frame_index < context.current_frame) {
        start_frame = 0;
    }

    Painter painter(*context.bitmap);

    for (size_t i = start_frame; i <= frame_index; ++i) {
        dbgln_if(WEBP_DEBUG, "drawing frame {} to produce frame {}", i, frame_index);

        auto const& frame = context.animation_frame_chunks_data.value()[i];
        auto const& frame_description = frame.header;

        if (i > 0) {
            auto const& previous_frame = context.animation_frame_chunks_data.value()[i - 1].header;
            if (previous_frame.disposal_method == ANMFChunkHeader::DisposalMethod::DisposeToBackgroundColor)
                painter.clear_rect({ previous_frame.frame_x, previous_frame.frame_y, previous_frame.frame_width, previous_frame.frame_height }, clear_color);
        }

        auto frame_image_data = TRY(decode_webp_animation_frame_image_data(frame));
        auto frame_bitmap = TRY(decode_webp_image_data(context, frame_image_data));
        if (static_cast<u32>(frame_bitmap->width()) != frame_description.frame_width || static_cast<u32>(frame_bitmap->height()) != frame_description.frame_height)
            return Error::from_string_literal("WebPImageDecoderPlugin: decoded frame bitmap size doesn't match frame description size");

        // FIXME: "Alpha-blending SHOULD be done in linear color space..."
        bool apply_alpha = frame_description.blending_method == ANMFChunkHeader::BlendingMethod::UseAlphaBlending;
        painter.blit({ frame_description.frame_x, frame_description.frame_y }, *frame_bitmap, { {}, frame_bitmap->size() }, /*opacity=*/1.0, apply_alpha);

        context.current_frame = i;
        context.state = WebPLoadingContext::State::BitmapDecoded;
    }

    return ImageFrameDescriptor { context.bitmap, static_cast<int>(context.animation_frame_chunks_data.value()[frame_index].header.frame_duration_in_milliseconds) };
}

WebPImageDecoderPlugin::WebPImageDecoderPlugin(ReadonlyBytes data, OwnPtr<WebPLoadingContext> context)
    : m_context(move(context))
{
    m_context->data = data;
}

WebPImageDecoderPlugin::~WebPImageDecoderPlugin() = default;

bool WebPImageDecoderPlugin::set_error(ErrorOr<void> const& error_or)
{
    if (error_or.is_error()) {
        dbgln("WebPLoadingContext error: {}", error_or.error());
        m_context->state = WebPLoadingContext::State::Error;
        return true;
    }
    return false;
}

IntSize WebPImageDecoderPlugin::size()
{
    return m_context->size.value();
}

bool WebPImageDecoderPlugin::sniff(ReadonlyBytes data)
{
    WebPLoadingContext context;
    context.data = data;

    // Intentionally no set_error() call: We're just sniffing `data` passed to the function, not reading m_context->data.
    return !decode_webp_header(context).is_error();
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> WebPImageDecoderPlugin::create(ReadonlyBytes data)
{
    auto context = TRY(try_make<WebPLoadingContext>());
    auto plugin = TRY(adopt_nonnull_own_or_enomem(new (nothrow) WebPImageDecoderPlugin(data, move(context))));
    TRY(decode_webp_header(*plugin->m_context));
    TRY(decode_webp_first_chunk(*plugin->m_context));
    return plugin;
}

bool WebPImageDecoderPlugin::is_animated()
{
    return m_context->first_chunk->id() == "VP8X"sv && m_context->vp8x_header.has_animation;
}

size_t WebPImageDecoderPlugin::loop_count()
{
    if (!is_animated())
        return 0;

    if (m_context->state < WebPLoadingContext::State::ChunksDecoded) {
        if (set_error(decode_webp_chunks(*m_context)))
            return 0;
    }

    if (m_context->state < WebPLoadingContext::State::AnimationFrameChunksDecoded) {
        if (set_error(decode_webp_animation_frame_chunks(*m_context)))
            return 0;
    }

    return m_context->animation_header_chunk_data->loop_count;
}

size_t WebPImageDecoderPlugin::frame_count()
{
    if (!is_animated())
        return 1;

    if (m_context->state < WebPLoadingContext::State::ChunksDecoded) {
        if (set_error(decode_webp_chunks(*m_context)))
            return 1;
    }

    return m_context->animation_frame_chunks.size();
}

size_t WebPImageDecoderPlugin::first_animated_frame_index()
{
    return 0;
}

ErrorOr<ImageFrameDescriptor> WebPImageDecoderPlugin::frame(size_t index, Optional<IntSize>)
{
    if (index >= frame_count())
        return Error::from_string_literal("WebPImageDecoderPlugin: Invalid frame index");

    if (m_context->state == WebPLoadingContext::State::Error)
        return Error::from_string_literal("WebPImageDecoderPlugin: Decoding failed");

    // In a lambda so that only one check to set State::Error is needed, instead of one per TRY.
    auto decode_frame = [this](size_t index) -> ErrorOr<ImageFrameDescriptor> {
        if (m_context->state < WebPLoadingContext::State::ChunksDecoded)
            TRY(decode_webp_chunks(*m_context));

        if (is_animated()) {
            if (m_context->state < WebPLoadingContext::State::AnimationFrameChunksDecoded)
                TRY(decode_webp_animation_frame_chunks(*m_context));
            return decode_webp_animation_frame(*m_context, index);
        }

        if (m_context->state < WebPLoadingContext::State::BitmapDecoded) {
            auto bitmap = TRY(decode_webp_image_data(*m_context, m_context->image_data.value()));

            // Check that size in VP8X chunk matches dimensions in VP8 or VP8L chunk if both are present.
            if (m_context->first_chunk->id() == "VP8X") {
                if (static_cast<u32>(bitmap->width()) != m_context->vp8x_header.width || static_cast<u32>(bitmap->height()) != m_context->vp8x_header.height)
                    return Error::from_string_literal("WebPImageDecoderPlugin: VP8X and VP8/VP8L chunks store different dimensions");
            }

            m_context->bitmap = move(bitmap);
            m_context->state = WebPLoadingContext::State::BitmapDecoded;
        }

        VERIFY(m_context->bitmap);
        return ImageFrameDescriptor { m_context->bitmap, 0 };
    };

    auto result = decode_frame(index);
    if (result.is_error()) {
        m_context->state = WebPLoadingContext::State::Error;
        return result.release_error();
    }
    return result.release_value();
}

ErrorOr<Optional<ReadonlyBytes>> WebPImageDecoderPlugin::icc_data()
{
    if (auto result = decode_webp_chunks(*m_context); result.is_error()) {
        m_context->state = WebPLoadingContext::State::Error;
        return result.release_error();
    }

    // FIXME: "If this chunk is not present, sRGB SHOULD be assumed."

    return m_context->iccp_chunk.map([](auto iccp_chunk) { return iccp_chunk.data(); });
}

}
