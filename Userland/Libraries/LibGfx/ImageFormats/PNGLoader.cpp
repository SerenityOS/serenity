/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/MemoryStream.h>
#include <AK/Vector.h>
#include <LibCompress/Zlib.h>
#include <LibGfx/ImageFormats/PNGLoader.h>
#include <LibGfx/ImageFormats/TIFFLoader.h>
#include <LibGfx/ImageFormats/TIFFMetadata.h>
#include <LibGfx/Painter.h>

namespace Gfx {

struct PNG_IHDR {
    NetworkOrdered<u32> width;
    NetworkOrdered<u32> height;
    u8 bit_depth { 0 };
    PNG::ColorType color_type { 0 };
    u8 compression_method { 0 };
    u8 filter_method { 0 };
    u8 interlace_method { 0 };
};

static_assert(AssertSize<PNG_IHDR, 13>());

struct acTL_Chunk {
    NetworkOrdered<u32> num_frames;
    NetworkOrdered<u32> num_plays;
};
static_assert(AssertSize<acTL_Chunk, 8>());

struct fcTL_Chunk {
    enum class DisposeOp : u8 {
        APNG_DISPOSE_OP_NONE = 0,
        APNG_DISPOSE_OP_BACKGROUND,
        APNG_DISPOSE_OP_PREVIOUS
    };
    enum class BlendOp : u8 {
        APNG_BLEND_OP_SOURCE = 0,
        APNG_BLEND_OP_OVER
    };
    NetworkOrdered<u32> sequence_number;
    NetworkOrdered<u32> width;
    NetworkOrdered<u32> height;
    NetworkOrdered<u32> x_offset;
    NetworkOrdered<u32> y_offset;
    NetworkOrdered<u16> delay_num;
    NetworkOrdered<u16> delay_den;
    DisposeOp dispose_op { DisposeOp::APNG_DISPOSE_OP_NONE };
    BlendOp blend_op { BlendOp::APNG_BLEND_OP_SOURCE };
};
static_assert(AssertSize<fcTL_Chunk, 26>());

struct ChromaticitiesAndWhitepoint {
    NetworkOrdered<u32> white_point_x;
    NetworkOrdered<u32> white_point_y;
    NetworkOrdered<u32> red_x;
    NetworkOrdered<u32> red_y;
    NetworkOrdered<u32> green_x;
    NetworkOrdered<u32> green_y;
    NetworkOrdered<u32> blue_x;
    NetworkOrdered<u32> blue_y;
};
static_assert(AssertSize<ChromaticitiesAndWhitepoint, 32>());

struct CodingIndependentCodePoints {
    u8 color_primaries;
    u8 transfer_function;
    u8 matrix_coefficients;
    u8 video_full_range_flag;
};
static_assert(AssertSize<CodingIndependentCodePoints, 4>());

struct EmbeddedICCProfile {
    StringView profile_name;
    ReadonlyBytes compressed_data;
};

struct Scanline {
    PNG::FilterType filter;
    ReadonlyBytes data {};
};

struct [[gnu::packed]] PaletteEntry {
    u8 r;
    u8 g;
    u8 b;
    // u8 a;
};

template<typename T>
struct [[gnu::packed]] Tuple {
    T gray;
    T a;
};

template<typename T>
struct [[gnu::packed]] Triplet {
    T r;
    T g;
    T b;

    bool operator==(Triplet const& other) const = default;
};

template<typename T>
struct [[gnu::packed]] Quartet {
    T r;
    T g;
    T b;
    T a;
};

enum PngInterlaceMethod {
    Null = 0,
    Adam7 = 1
};

enum RenderingIntent {
    Perceptual = 0,
    RelativeColorimetric = 1,
    Saturation = 2,
    AbsoluteColorimetric = 3,
};

struct AnimationFrame {
    fcTL_Chunk const& fcTL;
    RefPtr<Bitmap> bitmap;
    ByteBuffer compressed_data;

    AnimationFrame(fcTL_Chunk const& fcTL)
        : fcTL(fcTL)
    {
    }

    u32 duration_ms() const
    {
        u32 num = fcTL.delay_num;
        if (num == 0)
            return 1;
        u32 denom = fcTL.delay_den != 0 ? static_cast<u32>(fcTL.delay_den) : 100u;
        return (num * 1000) / denom;
    }

    IntRect rect() const
    {
        return { fcTL.x_offset, fcTL.y_offset, fcTL.width, fcTL.height };
    }
};

struct PNGLoadingContext {
    enum State {
        NotDecoded = 0,
        Error,
        IHDRDecoded,
        ImageDataChunkDecoded,
        ChunksDecoded,
        BitmapDecoded,
    };
    State state { State::NotDecoded };
    u8 const* data { nullptr };
    u8 const* data_current_ptr { nullptr };
    size_t data_size { 0 };
    i32 width { -1 };
    i32 height { -1 };
    u8 bit_depth { 0 };
    PNG::ColorType color_type { 0 };
    u8 compression_method { 0 };
    u8 filter_method { 0 };
    u8 interlace_method { 0 };
    u8 channels { 0 };
    u32 animation_next_expected_seq { 0 };
    u32 animation_next_frame_to_render { 0 };
    u32 animation_frame_count { 0 };
    u32 animation_loop_count { 0 };
    Optional<u32> last_completed_animation_frame_index;
    bool is_first_idat_part_of_animation { false };
    bool has_seen_iend { false };
    bool has_seen_idat_chunk { false };
    bool has_seen_actl_chunk_before_idat { false };
    bool has_alpha() const { return to_underlying(color_type) & 4 || palette_transparency_data.size() > 0; }
    Vector<Scanline> scanlines;
    ByteBuffer unfiltered_data;
    RefPtr<Gfx::Bitmap> bitmap;
    ByteBuffer compressed_data;
    Vector<PaletteEntry> palette_data;
    ByteBuffer palette_transparency_data;
    Vector<AnimationFrame> animation_frames;

    Optional<ChromaticitiesAndWhitepoint> chromaticities_and_whitepoint;
    Optional<CodingIndependentCodePoints> coding_independent_code_points;
    Optional<u32> gamma;
    Optional<EmbeddedICCProfile> embedded_icc_profile;
    Optional<ByteBuffer> decompressed_icc_profile;
    Optional<RenderingIntent> sRGB_rendering_intent;

    OwnPtr<ExifMetadata> exif_metadata;

    Checked<int> compute_row_size_for_width(int width)
    {
        Checked<int> row_size = width;
        row_size *= channels;
        row_size *= bit_depth;
        row_size += 7;
        row_size /= 8;
        if (row_size.has_overflow()) {
            dbgln("PNG too large, integer overflow while computing row size");
            state = State::Error;
        }
        return row_size;
    }

    PNGLoadingContext create_subimage_context(int width, int height)
    {
        PNGLoadingContext subimage_context;
        subimage_context.state = State::ChunksDecoded;
        subimage_context.width = width;
        subimage_context.height = height;
        subimage_context.channels = channels;
        subimage_context.color_type = color_type;
        subimage_context.palette_data = palette_data;
        subimage_context.palette_transparency_data = palette_transparency_data;
        subimage_context.bit_depth = bit_depth;
        subimage_context.filter_method = filter_method;
        return subimage_context;
    }
};

class Streamer {
public:
    Streamer(u8 const* data, size_t size)
        : m_data_ptr(data)
        , m_size_remaining(size)
    {
    }

    template<typename T>
    bool read(T& value)
    {
        if (m_size_remaining < sizeof(T))
            return false;
        value = *((NetworkOrdered<T> const*)m_data_ptr);
        m_data_ptr += sizeof(T);
        m_size_remaining -= sizeof(T);
        return true;
    }

    bool read_bytes(u8* buffer, size_t count)
    {
        if (m_size_remaining < count)
            return false;
        memcpy(buffer, m_data_ptr, count);
        m_data_ptr += count;
        m_size_remaining -= count;
        return true;
    }

    bool wrap_bytes(ReadonlyBytes& buffer, size_t count)
    {
        if (m_size_remaining < count)
            return false;
        buffer = ReadonlyBytes { m_data_ptr, count };
        m_data_ptr += count;
        m_size_remaining -= count;
        return true;
    }

    u8 const* current_data_ptr() const { return m_data_ptr; }
    bool at_end() const { return !m_size_remaining; }

private:
    u8 const* m_data_ptr { nullptr };
    size_t m_size_remaining { 0 };
};

static ErrorOr<void> process_chunk(Streamer&, PNGLoadingContext& context);

union [[gnu::packed]] Pixel {
    ARGB32 rgba { 0 };
    u8 v[4];
    struct {
        u8 r;
        u8 g;
        u8 b;
        u8 a;
    };
};
static_assert(AssertSize<Pixel, 4>());

void PNGImageDecoderPlugin::unfilter_scanline(PNG::FilterType filter, Bytes scanline_data, ReadonlyBytes previous_scanlines_data, u8 bytes_per_complete_pixel)
{
    // https://www.w3.org/TR/png-3/#9Filter-types
    // "Filters are applied to bytes, not to pixels, regardless of the bit depth or colour type of the image."
    switch (filter) {
    case PNG::FilterType::None:
        break;
    case PNG::FilterType::Sub:
        // This loop starts at bytes_per_complete_pixel because all bytes before that are
        // guaranteed to have no valid byte at index (i - bytes_per_complete pixel).
        // All such invalid byte indexes should be treated as 0, and adding 0 to the current
        // byte would do nothing, so the first bytes_per_complete_pixel bytes can instead
        // just be skipped.
        for (size_t i = bytes_per_complete_pixel; i < scanline_data.size(); ++i) {
            u8 left = scanline_data[i - bytes_per_complete_pixel];
            scanline_data[i] += left;
        }
        break;
    case PNG::FilterType::Up:
        for (size_t i = 0; i < scanline_data.size(); ++i) {
            u8 above = previous_scanlines_data[i];
            scanline_data[i] += above;
        }
        break;
    case PNG::FilterType::Average:
        for (size_t i = 0; i < scanline_data.size(); ++i) {
            u32 left = (i < bytes_per_complete_pixel) ? 0 : scanline_data[i - bytes_per_complete_pixel];
            u32 above = previous_scanlines_data[i];
            u8 average = (left + above) / 2;
            scanline_data[i] += average;
        }
        break;
    case PNG::FilterType::Paeth:
        for (size_t i = 0; i < scanline_data.size(); ++i) {
            u8 left = (i < bytes_per_complete_pixel) ? 0 : scanline_data[i - bytes_per_complete_pixel];
            u8 above = previous_scanlines_data[i];
            u8 upper_left = (i < bytes_per_complete_pixel) ? 0 : previous_scanlines_data[i - bytes_per_complete_pixel];
            scanline_data[i] += PNG::paeth_predictor(left, above, upper_left);
        }
        break;
    }
}

template<typename T>
ALWAYS_INLINE static void unpack_grayscale_without_alpha(PNGLoadingContext& context)
{
    for (int y = 0; y < context.height; ++y) {
        auto* gray_values = reinterpret_cast<T const*>(context.scanlines[y].data.data());
        for (int i = 0; i < context.width; ++i) {
            auto& pixel = (Pixel&)context.bitmap->scanline(y)[i];
            pixel.r = gray_values[i];
            pixel.g = gray_values[i];
            pixel.b = gray_values[i];
            pixel.a = 0xff;
        }
    }
}

template<typename T>
ALWAYS_INLINE static void unpack_grayscale_with_alpha(PNGLoadingContext& context)
{
    for (int y = 0; y < context.height; ++y) {
        auto* tuples = reinterpret_cast<Tuple<T> const*>(context.scanlines[y].data.data());
        for (int i = 0; i < context.width; ++i) {
            auto& pixel = (Pixel&)context.bitmap->scanline(y)[i];
            pixel.r = tuples[i].gray;
            pixel.g = tuples[i].gray;
            pixel.b = tuples[i].gray;
            pixel.a = tuples[i].a;
        }
    }
}

template<typename T>
ALWAYS_INLINE static void unpack_triplets_without_alpha(PNGLoadingContext& context)
{
    for (int y = 0; y < context.height; ++y) {
        auto* triplets = reinterpret_cast<Triplet<T> const*>(context.scanlines[y].data.data());
        for (int i = 0; i < context.width; ++i) {
            auto& pixel = (Pixel&)context.bitmap->scanline(y)[i];
            pixel.r = triplets[i].r;
            pixel.g = triplets[i].g;
            pixel.b = triplets[i].b;
            pixel.a = 0xff;
        }
    }
}

template<typename T>
ALWAYS_INLINE static void unpack_triplets_with_transparency_value(PNGLoadingContext& context, Triplet<T> transparency_value)
{
    for (int y = 0; y < context.height; ++y) {
        auto* triplets = reinterpret_cast<Triplet<T> const*>(context.scanlines[y].data.data());
        for (int i = 0; i < context.width; ++i) {
            auto& pixel = (Pixel&)context.bitmap->scanline(y)[i];
            pixel.r = triplets[i].r;
            pixel.g = triplets[i].g;
            pixel.b = triplets[i].b;
            if (triplets[i] == transparency_value)
                pixel.a = 0x00;
            else
                pixel.a = 0xff;
        }
    }
}

NEVER_INLINE FLATTEN static ErrorOr<void> unfilter(PNGLoadingContext& context)
{
    // First unfilter the scanlines:

    // FIXME: Instead of creating a separate buffer for the scanlines that need to be
    //        mutated, the mutation could be done in place (if the data was non-const).
    size_t bytes_per_scanline = context.scanlines[0].data.size();
    size_t bytes_needed_for_all_unfiltered_scanlines = 0;
    for (int y = 0; y < context.height; ++y) {
        if (context.scanlines[y].filter != PNG::FilterType::None) {
            bytes_needed_for_all_unfiltered_scanlines += bytes_per_scanline;
        }
    }
    context.unfiltered_data = TRY(ByteBuffer::create_uninitialized(bytes_needed_for_all_unfiltered_scanlines));

    // From section 6.3 of http://www.libpng.org/pub/png/spec/1.2/PNG-Filters.html
    // "bpp is defined as the number of bytes per complete pixel, rounding up to one.
    // For example, for color type 2 with a bit depth of 16, bpp is equal to 6
    // (three samples, two bytes per sample); for color type 0 with a bit depth of 2,
    // bpp is equal to 1 (rounding up); for color type 4 with a bit depth of 16, bpp
    // is equal to 4 (two-byte grayscale sample, plus two-byte alpha sample)."
    u8 bytes_per_complete_pixel = ceil_div(context.bit_depth, (u8)8) * context.channels;

    u8 dummy_scanline_bytes[bytes_per_scanline];
    memset(dummy_scanline_bytes, 0, sizeof(dummy_scanline_bytes));
    auto previous_scanlines_data = ReadonlyBytes { dummy_scanline_bytes, sizeof(dummy_scanline_bytes) };

    for (int y = 0, data_start = 0; y < context.height; ++y) {
        if (context.scanlines[y].filter != PNG::FilterType::None) {
            auto scanline_data_slice = context.unfiltered_data.bytes().slice(data_start, bytes_per_scanline);

            // Copy the current values over and set the scanline's data to the to-be-mutated slice
            context.scanlines[y].data.copy_to(scanline_data_slice);
            context.scanlines[y].data = scanline_data_slice;

            PNGImageDecoderPlugin::unfilter_scanline(context.scanlines[y].filter, scanline_data_slice, previous_scanlines_data, bytes_per_complete_pixel);

            data_start += bytes_per_scanline;
        }
        previous_scanlines_data = context.scanlines[y].data;
    }

    // Now unpack the scanlines to RGBA:
    switch (context.color_type) {
    case PNG::ColorType::Greyscale:
        if (context.bit_depth == 8) {
            unpack_grayscale_without_alpha<u8>(context);
        } else if (context.bit_depth == 16) {
            unpack_grayscale_without_alpha<u16>(context);
        } else if (context.bit_depth == 1 || context.bit_depth == 2 || context.bit_depth == 4) {
            auto bit_depth_squared = context.bit_depth * context.bit_depth;
            auto pixels_per_byte = 8 / context.bit_depth;
            auto mask = (1 << context.bit_depth) - 1;
            for (int y = 0; y < context.height; ++y) {
                auto* gray_values = context.scanlines[y].data.data();
                for (int x = 0; x < context.width; ++x) {
                    auto bit_offset = (8 - context.bit_depth) - (context.bit_depth * (x % pixels_per_byte));
                    auto value = (gray_values[x / pixels_per_byte] >> bit_offset) & mask;
                    auto& pixel = (Pixel&)context.bitmap->scanline(y)[x];
                    pixel.r = value * (0xff / bit_depth_squared);
                    pixel.g = value * (0xff / bit_depth_squared);
                    pixel.b = value * (0xff / bit_depth_squared);
                    pixel.a = 0xff;
                }
            }
        } else {
            VERIFY_NOT_REACHED();
        }
        break;
    case PNG::ColorType::GreyscaleWithAlpha:
        if (context.bit_depth == 8) {
            unpack_grayscale_with_alpha<u8>(context);
        } else if (context.bit_depth == 16) {
            unpack_grayscale_with_alpha<u16>(context);
        } else {
            VERIFY_NOT_REACHED();
        }
        break;
    case PNG::ColorType::Truecolor:
        if (context.palette_transparency_data.size() == 6) {
            if (context.bit_depth == 8) {
                unpack_triplets_with_transparency_value<u8>(context, Triplet<u8> { context.palette_transparency_data[0], context.palette_transparency_data[2], context.palette_transparency_data[4] });
            } else if (context.bit_depth == 16) {
                u16 tr = context.palette_transparency_data[0] | context.palette_transparency_data[1] << 8;
                u16 tg = context.palette_transparency_data[2] | context.palette_transparency_data[3] << 8;
                u16 tb = context.palette_transparency_data[4] | context.palette_transparency_data[5] << 8;
                unpack_triplets_with_transparency_value<u16>(context, Triplet<u16> { tr, tg, tb });
            } else {
                VERIFY_NOT_REACHED();
            }
        } else {
            if (context.bit_depth == 8)
                unpack_triplets_without_alpha<u8>(context);
            else if (context.bit_depth == 16)
                unpack_triplets_without_alpha<u16>(context);
            else
                VERIFY_NOT_REACHED();
        }
        break;
    case PNG::ColorType::TruecolorWithAlpha:
        if (context.bit_depth == 8) {
            for (int y = 0; y < context.height; ++y) {
                memcpy(context.bitmap->scanline(y), context.scanlines[y].data.data(), context.scanlines[y].data.size());
            }
        } else if (context.bit_depth == 16) {
            for (int y = 0; y < context.height; ++y) {
                auto* quartets = reinterpret_cast<Quartet<u16> const*>(context.scanlines[y].data.data());
                for (int i = 0; i < context.width; ++i) {
                    auto& pixel = (Pixel&)context.bitmap->scanline(y)[i];
                    pixel.r = quartets[i].r & 0xFF;
                    pixel.g = quartets[i].g & 0xFF;
                    pixel.b = quartets[i].b & 0xFF;
                    pixel.a = quartets[i].a & 0xFF;
                }
            }
        } else {
            VERIFY_NOT_REACHED();
        }
        break;
    case PNG::ColorType::IndexedColor:
        if (context.bit_depth == 8) {
            for (int y = 0; y < context.height; ++y) {
                auto* palette_index = context.scanlines[y].data.data();
                for (int i = 0; i < context.width; ++i) {
                    auto& pixel = (Pixel&)context.bitmap->scanline(y)[i];
                    if (palette_index[i] >= context.palette_data.size())
                        return Error::from_string_literal("PNGImageDecoderPlugin: Palette index out of range");
                    auto& color = context.palette_data.at((int)palette_index[i]);
                    auto transparency = context.palette_transparency_data.size() >= palette_index[i] + 1u
                        ? context.palette_transparency_data[palette_index[i]]
                        : 0xff;
                    pixel.r = color.r;
                    pixel.g = color.g;
                    pixel.b = color.b;
                    pixel.a = transparency;
                }
            }
        } else if (context.bit_depth == 1 || context.bit_depth == 2 || context.bit_depth == 4) {
            auto pixels_per_byte = 8 / context.bit_depth;
            auto mask = (1 << context.bit_depth) - 1;
            for (int y = 0; y < context.height; ++y) {
                auto* palette_indices = context.scanlines[y].data.data();
                for (int i = 0; i < context.width; ++i) {
                    auto bit_offset = (8 - context.bit_depth) - (context.bit_depth * (i % pixels_per_byte));
                    auto palette_index = (palette_indices[i / pixels_per_byte] >> bit_offset) & mask;
                    auto& pixel = (Pixel&)context.bitmap->scanline(y)[i];
                    if ((size_t)palette_index >= context.palette_data.size())
                        return Error::from_string_literal("PNGImageDecoderPlugin: Palette index out of range");
                    auto& color = context.palette_data.at(palette_index);
                    auto transparency = context.palette_transparency_data.size() >= palette_index + 1u
                        ? context.palette_transparency_data[palette_index]
                        : 0xff;
                    pixel.r = color.r;
                    pixel.g = color.g;
                    pixel.b = color.b;
                    pixel.a = transparency;
                }
            }
        } else {
            VERIFY_NOT_REACHED();
        }
        break;
    default:
        VERIFY_NOT_REACHED();
        break;
    }

    // Swap r and b values:
    for (int y = 0; y < context.height; ++y) {
        auto* pixels = (Pixel*)context.bitmap->scanline(y);
        for (int i = 0; i < context.bitmap->width(); ++i) {
            auto& x = pixels[i];
            swap(x.r, x.b);
        }
    }

    return {};
}

static bool decode_png_header(PNGLoadingContext& context)
{
    if (!context.data || context.data_size < sizeof(PNG::header)) {
        dbgln_if(PNG_DEBUG, "Missing PNG header");
        context.state = PNGLoadingContext::State::Error;
        return false;
    }

    if (memcmp(context.data, PNG::header.span().data(), sizeof(PNG::header)) != 0) {
        dbgln_if(PNG_DEBUG, "Invalid PNG header");
        context.state = PNGLoadingContext::State::Error;
        return false;
    }

    context.data_current_ptr = context.data + sizeof(PNG::header);
    return true;
}

static ErrorOr<void> decode_png_ihdr(PNGLoadingContext& context)
{
    size_t data_remaining = context.data_size - (context.data_current_ptr - context.data);

    Streamer streamer(context.data_current_ptr, data_remaining);

    // https://www.w3.org/TR/png/#11IHDR
    // The IHDR chunk shall be the first chunk in the PNG datastream.
    TRY(process_chunk(streamer, context));

    context.data_current_ptr = streamer.current_data_ptr();

    VERIFY(context.state == PNGLoadingContext::State::IHDRDecoded);
    return {};
}

static bool decode_png_image_data_chunk(PNGLoadingContext& context)
{
    VERIFY(context.state >= PNGLoadingContext::IHDRDecoded);

    if (context.state >= PNGLoadingContext::ImageDataChunkDecoded)
        return true;

    size_t data_remaining = context.data_size - (context.data_current_ptr - context.data);

    Streamer streamer(context.data_current_ptr, data_remaining);
    while (!streamer.at_end() && !context.has_seen_iend) {
        if (auto result = process_chunk(streamer, context); result.is_error()) {
            context.state = PNGLoadingContext::State::Error;
            // FIXME: Return this to caller instead of logging it.
            dbgln("PNGLoader: Error processing chunk: {}", result.error());
            return false;
        }

        context.data_current_ptr = streamer.current_data_ptr();

        if (context.state >= PNGLoadingContext::State::ImageDataChunkDecoded)
            return true;
    }

    return false;
}

static bool decode_png_animation_data_chunks(PNGLoadingContext& context, u32 requested_animation_frame_index)
{
    if (context.state >= PNGLoadingContext::ImageDataChunkDecoded) {
        if (context.last_completed_animation_frame_index.has_value()) {
            if (requested_animation_frame_index <= context.last_completed_animation_frame_index.value())
                return true;
        }
    } else if (!decode_png_image_data_chunk(context)) {
        return false;
    }

    size_t data_remaining = context.data_size - (context.data_current_ptr - context.data);

    Streamer streamer(context.data_current_ptr, data_remaining);
    while (!streamer.at_end() && !context.has_seen_iend) {
        if (auto result = process_chunk(streamer, context); result.is_error()) {
            // FIXME: Return this to caller instead of logging it.
            dbgln("PNGLoader: Error processing chunk: {}", result.error());
            context.state = PNGLoadingContext::State::Error;
            return false;
        }

        context.data_current_ptr = streamer.current_data_ptr();

        if (context.last_completed_animation_frame_index.has_value()) {
            if (requested_animation_frame_index <= context.last_completed_animation_frame_index.value())
                break;
        }
    }

    if (!context.last_completed_animation_frame_index.has_value())
        return false;
    return requested_animation_frame_index <= context.last_completed_animation_frame_index.value();
}

static bool decode_png_chunks(PNGLoadingContext& context)
{
    VERIFY(context.state >= PNGLoadingContext::IHDRDecoded);

    if (context.state >= PNGLoadingContext::State::ChunksDecoded)
        return true;

    size_t data_remaining = context.data_size - (context.data_current_ptr - context.data);

    context.compressed_data.ensure_capacity(context.data_size);

    Streamer streamer(context.data_current_ptr, data_remaining);
    while (!streamer.at_end() && !context.has_seen_iend) {
        if (auto result = process_chunk(streamer, context); result.is_error()) {
            // Ignore failed chunk and just consider chunk decoding being done.
            // decode_png_bitmap() will check whether we got all required ones anyway.
            break;
        }

        context.data_current_ptr = streamer.current_data_ptr();
    }

    context.state = PNGLoadingContext::State::ChunksDecoded;
    return true;
}

static ErrorOr<void> decode_png_bitmap_simple(PNGLoadingContext& context, ByteBuffer& decompression_buffer)
{
    Streamer streamer(decompression_buffer.data(), decompression_buffer.size());

    for (int y = 0; y < context.height; ++y) {
        u8 filter_byte;
        if (!streamer.read(filter_byte)) {
            context.state = PNGLoadingContext::State::Error;
            return Error::from_string_literal("PNGImageDecoderPlugin: Decoding failed");
        }

        if (filter_byte > 4) {
            context.state = PNGLoadingContext::State::Error;
            return Error::from_string_literal("PNGImageDecoderPlugin: Invalid PNG filter");
        }

        context.scanlines.append({ MUST(PNG::filter_type(filter_byte)) });
        auto& scanline_buffer = context.scanlines.last().data;
        auto row_size = context.compute_row_size_for_width(context.width);
        if (row_size.has_overflow())
            return Error::from_string_literal("PNGImageDecoderPlugin: Row size overflow");

        if (!streamer.wrap_bytes(scanline_buffer, row_size.value())) {
            context.state = PNGLoadingContext::State::Error;
            return Error::from_string_literal("PNGImageDecoderPlugin: Decoding failed");
        }
    }

    context.bitmap = TRY(Bitmap::create(context.has_alpha() ? BitmapFormat::BGRA8888 : BitmapFormat::BGRx8888, { context.width, context.height }));
    return unfilter(context);
}

static int adam7_height(PNGLoadingContext& context, int pass)
{
    switch (pass) {
    case 1:
        return (context.height + 7) / 8;
    case 2:
        return (context.height + 7) / 8;
    case 3:
        return (context.height + 3) / 8;
    case 4:
        return (context.height + 3) / 4;
    case 5:
        return (context.height + 1) / 4;
    case 6:
        return (context.height + 1) / 2;
    case 7:
        return context.height / 2;
    default:
        VERIFY_NOT_REACHED();
    }
}

static int adam7_width(PNGLoadingContext& context, int pass)
{
    switch (pass) {
    case 1:
        return (context.width + 7) / 8;
    case 2:
        return (context.width + 3) / 8;
    case 3:
        return (context.width + 3) / 4;
    case 4:
        return (context.width + 1) / 4;
    case 5:
        return (context.width + 1) / 2;
    case 6:
        return context.width / 2;
    case 7:
        return context.width;
    default:
        VERIFY_NOT_REACHED();
    }
}

// Index 0 unused (non-interlaced case)
static int adam7_starty[8] = { 0, 0, 0, 4, 0, 2, 0, 1 };
static int adam7_startx[8] = { 0, 0, 4, 0, 2, 0, 1, 0 };
static int adam7_stepy[8] = { 1, 8, 8, 8, 4, 4, 2, 2 };
static int adam7_stepx[8] = { 1, 8, 8, 4, 4, 2, 2, 1 };

static ErrorOr<void> decode_adam7_pass(PNGLoadingContext& context, Streamer& streamer, int pass)
{
    auto subimage_context = context.create_subimage_context(adam7_width(context, pass), adam7_height(context, pass));

    // For small images, some passes might be empty
    if (!subimage_context.width || !subimage_context.height)
        return {};

    for (int y = 0; y < subimage_context.height; ++y) {
        u8 filter_byte;
        if (!streamer.read(filter_byte)) {
            context.state = PNGLoadingContext::State::Error;
            return Error::from_string_literal("PNGImageDecoderPlugin: Decoding failed");
        }

        if (filter_byte > 4) {
            context.state = PNGLoadingContext::State::Error;
            return Error::from_string_literal("PNGImageDecoderPlugin: Invalid PNG filter");
        }

        subimage_context.scanlines.append({ MUST(PNG::filter_type(filter_byte)) });
        auto& scanline_buffer = subimage_context.scanlines.last().data;

        auto row_size = context.compute_row_size_for_width(subimage_context.width);
        if (row_size.has_overflow())
            return Error::from_string_literal("PNGImageDecoderPlugin: Row size overflow");
        if (!streamer.wrap_bytes(scanline_buffer, row_size.value())) {
            context.state = PNGLoadingContext::State::Error;
            return Error::from_string_literal("PNGImageDecoderPlugin: Decoding failed");
        }
    }

    subimage_context.bitmap = TRY(Bitmap::create(context.bitmap->format(), { subimage_context.width, subimage_context.height }));
    TRY(unfilter(subimage_context));

    // Copy the subimage data into the main image according to the pass pattern
    for (int y = 0, dy = adam7_starty[pass]; y < subimage_context.height && dy < context.height; ++y, dy += adam7_stepy[pass]) {
        for (int x = 0, dx = adam7_startx[pass]; x < subimage_context.width && dx < context.width; ++x, dx += adam7_stepx[pass]) {
            context.bitmap->set_pixel(dx, dy, subimage_context.bitmap->get_pixel(x, y));
        }
    }
    return {};
}

static ErrorOr<void> decode_png_adam7(PNGLoadingContext& context, ByteBuffer& decompression_buffer)
{
    Streamer streamer(decompression_buffer.data(), decompression_buffer.size());
    context.bitmap = TRY(Bitmap::create(context.has_alpha() ? BitmapFormat::BGRA8888 : BitmapFormat::BGRx8888, { context.width, context.height }));
    for (int pass = 1; pass <= 7; ++pass)
        TRY(decode_adam7_pass(context, streamer, pass));
    return {};
}

static ErrorOr<void> decode_png_bitmap(PNGLoadingContext& context)
{
    if (context.state < PNGLoadingContext::State::ChunksDecoded) {
        if (!decode_png_chunks(context))
            return Error::from_string_literal("PNGImageDecoderPlugin: Decoding failed");
    }

    if (context.state >= PNGLoadingContext::State::BitmapDecoded)
        return {};

    if (context.color_type == PNG::ColorType::IndexedColor && context.palette_data.is_empty())
        return Error::from_string_literal("PNGImageDecoderPlugin: Didn't see a PLTE chunk for a palletized image, or it was empty.");

    auto compressed_data_stream = make<FixedMemoryStream>(context.compressed_data.span());
    auto decompressor_or_error = Compress::ZlibDecompressor::create(move(compressed_data_stream));
    if (decompressor_or_error.is_error()) {
        context.state = PNGLoadingContext::State::Error;
        return decompressor_or_error.release_error();
    }
    auto decompressor = decompressor_or_error.release_value();
    auto result_or_error = decompressor->read_until_eof();
    if (result_or_error.is_error()) {
        context.state = PNGLoadingContext::State::Error;
        return result_or_error.release_error();
    }
    auto decompression_buffer = result_or_error.release_value();
    context.compressed_data.clear();

    context.scanlines.ensure_capacity(context.height);
    switch (context.interlace_method) {
    case PngInterlaceMethod::Null:
        TRY(decode_png_bitmap_simple(context, decompression_buffer));
        break;
    case PngInterlaceMethod::Adam7:
        TRY(decode_png_adam7(context, decompression_buffer));
        break;
    default:
        context.state = PNGLoadingContext::State::Error;
        return Error::from_string_literal("PNGImageDecoderPlugin: Invalid interlace method");
    }

    context.state = PNGLoadingContext::State::BitmapDecoded;
    return {};
}

static ErrorOr<RefPtr<Bitmap>> decode_png_animation_frame_bitmap(PNGLoadingContext& context, AnimationFrame& animation_frame)
{
    if (context.color_type == PNG::ColorType::IndexedColor && context.palette_data.is_empty())
        return Error::from_string_literal("PNGImageDecoderPlugin: Didn't see a PLTE chunk for a palletized image, or it was empty.");

    VERIFY(!animation_frame.bitmap);

    auto frame_rect = animation_frame.rect();
    auto frame_context = context.create_subimage_context(frame_rect.width(), frame_rect.height());

    auto compressed_data_stream = make<FixedMemoryStream>(animation_frame.compressed_data.span());
    auto decompressor = TRY(Compress::ZlibDecompressor::create(move(compressed_data_stream)));
    auto decompression_buffer = TRY(decompressor->read_until_eof());
    frame_context.compressed_data.clear();

    frame_context.scanlines.ensure_capacity(frame_context.height);
    switch (context.interlace_method) {
    case PngInterlaceMethod::Null:
        TRY(decode_png_bitmap_simple(frame_context, decompression_buffer));
        break;
    case PngInterlaceMethod::Adam7:
        TRY(decode_png_adam7(frame_context, decompression_buffer));
        break;
    default:
        return Error::from_string_literal("PNGImageDecoderPlugin: Invalid interlace method");
    }

    context.state = PNGLoadingContext::State::BitmapDecoded;
    return move(frame_context.bitmap);
}

static bool is_valid_compression_method(u8 compression_method)
{
    return compression_method == 0;
}

static bool is_valid_filter_method(u8 filter_method)
{
    return filter_method == 0;
}

static ErrorOr<void> process_IHDR(ReadonlyBytes data, PNGLoadingContext& context)
{
    if (data.size() < (int)sizeof(PNG_IHDR))
        return Error::from_string_literal("IHDR chunk has an abnormal size");

    auto const& ihdr = *(const PNG_IHDR*)data.data();

    if (ihdr.width == 0 || ihdr.width > NumericLimits<i32>::max()) {
        dbgln("PNG has invalid width {}", ihdr.width);
        return Error::from_string_literal("Invalid width");
    }

    if (ihdr.height == 0 || ihdr.height > NumericLimits<i32>::max()) {
        dbgln("PNG has invalid height {}", ihdr.height);
        return Error::from_string_literal("Invalid height");
    }

    if (!is_valid_compression_method(ihdr.compression_method)) {
        dbgln("PNG has invalid compression method {}", ihdr.compression_method);
        return Error::from_string_literal("Unsupported compression method");
    }

    if (!is_valid_filter_method(ihdr.filter_method)) {
        dbgln("PNG has invalid filter method {}", ihdr.filter_method);
        return Error::from_string_literal("Unsupported filter method");
    }

    context.width = ihdr.width;
    context.height = ihdr.height;
    context.bit_depth = ihdr.bit_depth;
    context.color_type = ihdr.color_type;
    context.compression_method = ihdr.compression_method;
    context.filter_method = ihdr.filter_method;
    context.interlace_method = ihdr.interlace_method;

    dbgln_if(PNG_DEBUG, "PNG: {}x{} ({} bpp)", context.width, context.height, context.bit_depth);
    dbgln_if(PNG_DEBUG, "     Color type: {}", to_underlying(context.color_type));
    dbgln_if(PNG_DEBUG, "Compress Method: {}", context.compression_method);
    dbgln_if(PNG_DEBUG, "  Filter Method: {}", context.filter_method);
    dbgln_if(PNG_DEBUG, " Interlace type: {}", context.interlace_method);

    if (context.interlace_method != PngInterlaceMethod::Null && context.interlace_method != PngInterlaceMethod::Adam7) {
        dbgln_if(PNG_DEBUG, "PNGLoader::process_IHDR: unknown interlace method: {}", context.interlace_method);
        return Error::from_string_literal("Unsupported interlacing method");
    }

    switch (context.color_type) {
    case PNG::ColorType::Greyscale:
        if (context.bit_depth != 1 && context.bit_depth != 2 && context.bit_depth != 4 && context.bit_depth != 8 && context.bit_depth != 16)
            return Error::from_string_literal("Unsupported bit depth for a greyscale image");
        context.channels = 1;
        break;
    case PNG::ColorType::GreyscaleWithAlpha:
        if (context.bit_depth != 8 && context.bit_depth != 16)
            return Error::from_string_literal("Unsupported bit depth for a greyscale image with alpha");
        context.channels = 2;
        break;
    case PNG::ColorType::Truecolor:
        if (context.bit_depth != 8 && context.bit_depth != 16)
            return Error::from_string_literal("Unsupported bit depth for a true color image");
        context.channels = 3;
        break;
    case PNG::ColorType::IndexedColor:
        if (context.bit_depth != 1 && context.bit_depth != 2 && context.bit_depth != 4 && context.bit_depth != 8)
            return Error::from_string_literal("Unsupported bit depth for a indexed color image");
        context.channels = 1;
        break;
    case PNG::ColorType::TruecolorWithAlpha:
        if (context.bit_depth != 8 && context.bit_depth != 16)
            return Error::from_string_literal("Unsupported bit depth for a true color image with alpha");
        context.channels = 4;
        break;
    default:
        return Error::from_string_literal("Unsupported color type");
    }

    context.state = PNGLoadingContext::IHDRDecoded;

    return {};
}

static ErrorOr<void> process_IDAT(ReadonlyBytes data, PNGLoadingContext& context)
{
    context.compressed_data.append(data);
    if (context.state < PNGLoadingContext::State::ImageDataChunkDecoded)
        context.state = PNGLoadingContext::State::ImageDataChunkDecoded;
    return {};
}

static ErrorOr<void> process_PLTE(ReadonlyBytes data, PNGLoadingContext& context)
{
    TRY(context.palette_data.try_append((PaletteEntry const*)data.data(), data.size() / 3));
    return {};
}

static ErrorOr<void> process_tRNS(ReadonlyBytes data, PNGLoadingContext& context)
{
    switch (context.color_type) {
    case PNG::ColorType::Greyscale:
    case PNG::ColorType::Truecolor:
    case PNG::ColorType::IndexedColor:
        TRY(context.palette_transparency_data.try_append(data));
        break;
    default:
        break;
    }
    return {};
}

static ErrorOr<void> process_cHRM(ReadonlyBytes data, PNGLoadingContext& context)
{
    // https://www.w3.org/TR/png/#11cHRM
    if (data.size() != 32)
        return Error::from_string_literal("cHRM chunk has an abnormal size");
    context.chromaticities_and_whitepoint = *bit_cast<ChromaticitiesAndWhitepoint* const>(data.data());
    return {};
}

static ErrorOr<void> process_cICP(ReadonlyBytes data, PNGLoadingContext& context)
{
    // https://www.w3.org/TR/png/#cICP-chunk
    if (data.size() != 4)
        return Error::from_string_literal("cICP chunk has an abnormal size");
    context.coding_independent_code_points = *bit_cast<CodingIndependentCodePoints* const>(data.data());
    return {};
}

static ErrorOr<void> process_iCCP(ReadonlyBytes data, PNGLoadingContext& context)
{
    // https://www.w3.org/TR/png/#11iCCP
    size_t profile_name_length_max = min(80u, data.size());
    size_t profile_name_length = strnlen((char const*)data.data(), profile_name_length_max);
    if (profile_name_length == 0 || profile_name_length == profile_name_length_max)
        return Error::from_string_literal("iCCP chunk does not contain a profile name");

    if (data.size() < profile_name_length + 2)
        return Error::from_string_literal("iCCP chunk is too small");

    u8 compression_method = data[profile_name_length + 1];
    if (compression_method != 0)
        return Error::from_string_literal("Unsupported compression method in the iCCP chunk");

    context.embedded_icc_profile = EmbeddedICCProfile { { data.data(), profile_name_length }, data.slice(profile_name_length + 2) };

    return {};
}

static ErrorOr<void> process_gAMA(ReadonlyBytes data, PNGLoadingContext& context)
{
    // https://www.w3.org/TR/png/#11gAMA
    if (data.size() != 4)
        return Error::from_string_literal("gAMA chunk has an abnormal size");

    u32 gamma = *bit_cast<NetworkOrdered<u32> const*>(data.data());
    if (gamma & 0x8000'0000)
        return Error::from_string_literal("Gamma value is too high");
    context.gamma = gamma;

    return {};
}

static ErrorOr<void> process_sRGB(ReadonlyBytes data, PNGLoadingContext& context)
{
    // https://www.w3.org/TR/png/#srgb-standard-colour-space
    if (data.size() != 1) {
        // Invalid per spec, but (rarely) happens in the wild. Log and ignore.
        warnln("warning: PNG sRGB chunk has an abnormal size; ignoring");
        return {};
    }

    u8 rendering_intent = data[0];
    if (rendering_intent > 3)
        return Error::from_string_literal("Unsupported rendering intent");

    context.sRGB_rendering_intent = (RenderingIntent)rendering_intent;

    return {};
}

static ErrorOr<void> process_acTL(ReadonlyBytes data, PNGLoadingContext& context)
{
    // https://www.w3.org/TR/png/#acTL-chunk
    if (context.has_seen_idat_chunk)
        return {}; // Ignore if we encounter it after the first idat
    if (data.size() != sizeof(acTL_Chunk))
        return Error::from_string_literal("acTL chunk has an abnormal size");

    auto const& acTL = *bit_cast<acTL_Chunk* const>(data.data());
    context.animation_frame_count = acTL.num_frames;
    context.animation_loop_count = acTL.num_plays;
    context.has_seen_actl_chunk_before_idat = true;
    TRY(context.animation_frames.try_ensure_capacity(context.animation_frame_count));
    return {};
}

static ErrorOr<void> process_fcTL(ReadonlyBytes data, PNGLoadingContext& context)
{
    // https://www.w3.org/TR/png/#fcTL-chunk
    if (!context.has_seen_actl_chunk_before_idat)
        return {}; // Ignore if it's not a valid animated png

    if (data.size() != sizeof(fcTL_Chunk))
        return Error::from_string_literal("fcTL chunk has an abnormal size");

    auto const& fcTL = *bit_cast<fcTL_Chunk* const>(data.data());
    if (fcTL.sequence_number != context.animation_next_expected_seq) {
        dbgln_if(PNG_DEBUG, "Expected fcTL sequence number: {}, got: {}", context.animation_next_expected_seq, fcTL.sequence_number);
        return Error::from_string_literal("Unexpected sequence number");
    }

    context.animation_next_expected_seq++;

    if (fcTL.width == 0 || fcTL.height == 0)
        return Error::from_string_literal("width and height must be greater than zero in fcTL chunk");

    Checked<int> left { static_cast<int>(fcTL.x_offset) };
    Checked<int> top { static_cast<int>(fcTL.y_offset) };
    Checked<int> width { static_cast<int>(fcTL.width) };
    Checked<int> height { static_cast<int>(fcTL.height) };
    auto right = left + width;
    auto bottom = top + height;
    if (left < 0 || width <= 0 || right.has_overflow() || right > context.width)
        return Error::from_string_literal("Invalid x_offset value in fcTL chunk");
    if (top < 0 || height <= 0 || bottom.has_overflow() || bottom > context.height)
        return Error::from_string_literal("Invalid y_offset value in fcTL chunk");

    bool is_first_animation_frame = context.animation_frames.is_empty();
    if (!is_first_animation_frame)
        context.last_completed_animation_frame_index = context.animation_frames.size() - 1;

    context.animation_frames.append({ fcTL });

    if (!context.has_seen_idat_chunk && is_first_animation_frame)
        context.is_first_idat_part_of_animation = true;
    return {};
}

static ErrorOr<void> process_fdAT(ReadonlyBytes data, PNGLoadingContext& context)
{
    // https://www.w3.org/TR/png/#fdAT-chunk

    if (data.size() <= 4)
        return Error::from_string_literal("fdAT chunk has an abnormal size");

    u32 sequence_number = *bit_cast<NetworkOrdered<u32> const*>(data.data());
    if (sequence_number != context.animation_next_expected_seq) {
        dbgln_if(PNG_DEBUG, "Expected fdAT sequence number: {}, got: {}", context.animation_next_expected_seq, sequence_number);
        return Error::from_string_literal("Unexpected sequence number");
    }
    context.animation_next_expected_seq++;

    if (context.animation_frames.is_empty())
        return Error::from_string_literal("No frame available");
    auto& current_animation_frame = context.animation_frames[context.animation_frames.size() - 1];
    auto compressed_data = data.slice(4);
    current_animation_frame.compressed_data.append(compressed_data.data(), compressed_data.size());
    return {};
}

static ErrorOr<void> process_eXIf(ReadonlyBytes bytes, PNGLoadingContext& context)
{
    context.exif_metadata = TRY(TIFFImageDecoderPlugin::read_exif_metadata(bytes));
    return {};
}

static void process_IEND(ReadonlyBytes, PNGLoadingContext& context)
{
    // https://www.w3.org/TR/png/#11IEND
    if (context.has_seen_actl_chunk_before_idat)
        context.last_completed_animation_frame_index = context.animation_frames.size();

    context.has_seen_iend = true;
}

static ErrorOr<void> process_chunk(Streamer& streamer, PNGLoadingContext& context)
{
    u32 chunk_size;
    if (!streamer.read(chunk_size)) {
        dbgln_if(PNG_DEBUG, "Bail at chunk_size");
        return Error::from_string_literal("Error while reading from Streamer");
    }

    Array<u8, 4> chunk_type_buffer;
    StringView const chunk_type { chunk_type_buffer.span() };
    if (!streamer.read_bytes(chunk_type_buffer.data(), chunk_type_buffer.size())) {
        dbgln_if(PNG_DEBUG, "Bail at chunk_type");
        return Error::from_string_literal("Error while reading from Streamer");
    }
    ReadonlyBytes chunk_data;
    if (!streamer.wrap_bytes(chunk_data, chunk_size)) {
        dbgln_if(PNG_DEBUG, "Bail at chunk_data");
        return Error::from_string_literal("Error while reading from Streamer");
    }
    u32 chunk_crc;
    if (!streamer.read(chunk_crc)) {
        dbgln_if(PNG_DEBUG, "Bail at chunk_crc");
        return Error::from_string_literal("Error while reading from Streamer");
    }
    dbgln_if(PNG_DEBUG, "Chunk type: '{}', size: {}, crc: {:x}", chunk_type, chunk_size, chunk_crc);

    if (chunk_type == "IHDR"sv) {
        if (context.state >= PNGLoadingContext::IHDRDecoded)
            return Error::from_string_literal("Multiple IHDR chunks");

        return process_IHDR(chunk_data, context);
    }

    if (context.state < PNGLoadingContext::IHDRDecoded)
        return Error::from_string_literal("IHDR is not the first chunk of the file");

    if (chunk_type == "IDAT"sv)
        return process_IDAT(chunk_data, context);
    if (chunk_type == "PLTE"sv)
        return process_PLTE(chunk_data, context);
    if (chunk_type == "cHRM"sv)
        return process_cHRM(chunk_data, context);
    if (chunk_type == "cICP"sv)
        return process_cICP(chunk_data, context);
    if (chunk_type == "iCCP"sv)
        return process_iCCP(chunk_data, context);
    if (chunk_type == "gAMA"sv)
        return process_gAMA(chunk_data, context);
    if (chunk_type == "sRGB"sv)
        return process_sRGB(chunk_data, context);
    if (chunk_type == "tRNS"sv)
        return process_tRNS(chunk_data, context);
    if (chunk_type == "acTL"sv)
        return process_acTL(chunk_data, context);
    if (chunk_type == "fcTL"sv)
        return process_fcTL(chunk_data, context);
    if (chunk_type == "fdAT"sv)
        return process_fdAT(chunk_data, context);
    if (chunk_type == "eXIf"sv)
        return process_eXIf(chunk_data, context);
    if (chunk_type == "IEND"sv)
        process_IEND(chunk_data, context);
    return {};
}

PNGImageDecoderPlugin::PNGImageDecoderPlugin(u8 const* data, size_t size)
{
    m_context = make<PNGLoadingContext>();
    m_context->data = m_context->data_current_ptr = data;
    m_context->data_size = size;
}

PNGImageDecoderPlugin::~PNGImageDecoderPlugin() = default;

bool PNGImageDecoderPlugin::ensure_image_data_chunk_was_decoded()
{
    if (m_context->state == PNGLoadingContext::State::Error)
        return false;

    if (m_context->state < PNGLoadingContext::State::ImageDataChunkDecoded) {
        if (!decode_png_image_data_chunk(*m_context))
            return false;
    }
    return true;
}

bool PNGImageDecoderPlugin::ensure_animation_frame_was_decoded(u32 animation_frame_index)
{
    if (m_context->state == PNGLoadingContext::State::Error)
        return false;

    if (m_context->state < PNGLoadingContext::State::ImageDataChunkDecoded) {
        if (!decode_png_image_data_chunk(*m_context))
            return false;
    }

    if (m_context->last_completed_animation_frame_index.has_value()) {
        if (m_context->last_completed_animation_frame_index.value() >= animation_frame_index)
            return true;
    }

    return decode_png_animation_data_chunks(*m_context, animation_frame_index);
}

IntSize PNGImageDecoderPlugin::size()
{
    return { m_context->width, m_context->height };
}

bool PNGImageDecoderPlugin::sniff(ReadonlyBytes data)
{
    PNGLoadingContext context;
    context.data = context.data_current_ptr = data.data();
    context.data_size = data.size();
    return decode_png_header(context);
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> PNGImageDecoderPlugin::create(ReadonlyBytes data)
{
    auto plugin = TRY(adopt_nonnull_own_or_enomem(new (nothrow) PNGImageDecoderPlugin(data.data(), data.size())));
    if (!decode_png_header(*plugin->m_context))
        return Error::from_string_literal("Invalid header for a PNG file");
    TRY(decode_png_ihdr(*plugin->m_context));
    return plugin;
}

bool PNGImageDecoderPlugin::is_animated()
{
    if (!ensure_image_data_chunk_was_decoded())
        return false;
    return m_context->has_seen_actl_chunk_before_idat;
}

size_t PNGImageDecoderPlugin::loop_count()
{
    if (!ensure_image_data_chunk_was_decoded())
        return 0;
    return m_context->animation_loop_count;
}

size_t PNGImageDecoderPlugin::frame_count()
{
    if (!ensure_image_data_chunk_was_decoded())
        return 0;

    if (!m_context->has_seen_actl_chunk_before_idat)
        return 1;

    auto total_frames = m_context->animation_frame_count;
    if (!m_context->is_first_idat_part_of_animation)
        total_frames++;
    return total_frames;
}

size_t PNGImageDecoderPlugin::first_animated_frame_index()
{
    if (!ensure_image_data_chunk_was_decoded())
        return 0;
    if (!m_context->has_seen_actl_chunk_before_idat)
        return 0;
    return m_context->is_first_idat_part_of_animation ? 0 : 1;
}

static ErrorOr<RefPtr<Bitmap>> render_animation_frame(AnimationFrame const& prev_animation_frame, AnimationFrame& animation_frame, Bitmap const& decoded_frame_bitmap)
{
    auto rendered_bitmap = TRY(prev_animation_frame.bitmap->clone());
    Painter painter(rendered_bitmap);

    static constexpr Color transparent_black = { 0, 0, 0, 0 };

    auto frame_rect = animation_frame.rect();
    switch (prev_animation_frame.fcTL.dispose_op) {
    case fcTL_Chunk::DisposeOp::APNG_DISPOSE_OP_NONE:
        break;
    case fcTL_Chunk::DisposeOp::APNG_DISPOSE_OP_BACKGROUND:
        painter.clear_rect(rendered_bitmap->rect(), transparent_black);
        break;
    case fcTL_Chunk::DisposeOp::APNG_DISPOSE_OP_PREVIOUS: {
        painter.blit(frame_rect.location(), *prev_animation_frame.bitmap, frame_rect, 1.0f, false);
        break;
    }
    }
    switch (animation_frame.fcTL.blend_op) {
    case fcTL_Chunk::BlendOp::APNG_BLEND_OP_SOURCE:
        painter.blit(frame_rect.location(), decoded_frame_bitmap, decoded_frame_bitmap.rect(), 1.0f, false);
        break;
    case fcTL_Chunk::BlendOp::APNG_BLEND_OP_OVER:
        painter.blit(frame_rect.location(), decoded_frame_bitmap, decoded_frame_bitmap.rect(), 1.0f, true);
        break;
    }
    return rendered_bitmap;
}

ErrorOr<ImageFrameDescriptor> PNGImageDecoderPlugin::frame(size_t index, Optional<IntSize>)
{
    if (m_context->state == PNGLoadingContext::State::Error)
        return Error::from_string_literal("PNGImageDecoderPlugin: Decoding failed");

    if (!ensure_image_data_chunk_was_decoded())
        return Error::from_string_literal("PNGImageDecoderPlugin: Decoding image data chunk");

    auto set_descriptor_duration = [](ImageFrameDescriptor& descriptor, AnimationFrame const& animation_frame) {
        descriptor.duration = static_cast<int>(animation_frame.duration_ms());
        if (descriptor.duration < 0)
            descriptor.duration = NumericLimits<int>::min();
    };
    auto load_default_image = [&]() -> ErrorOr<void> {
        if (m_context->state < PNGLoadingContext::State::BitmapDecoded) {
            // NOTE: This forces the chunk decoding to happen.
            TRY(decode_png_bitmap(*m_context));
        }

        VERIFY(m_context->bitmap);
        return {};
    };

    if (index == 0) {
        TRY(load_default_image());

        ImageFrameDescriptor descriptor { m_context->bitmap };
        if (m_context->has_seen_actl_chunk_before_idat && m_context->is_first_idat_part_of_animation)
            set_descriptor_duration(descriptor, m_context->animation_frames[0]);
        return descriptor;
    }

    if (!m_context->has_seen_actl_chunk_before_idat)
        return Error::from_string_literal("PNGImageDecoderPlugin: Invalid frame index");

    if (!ensure_animation_frame_was_decoded(index))
        return Error::from_string_literal("PNGImageDecoderPlugin: Decoding image data chunk");

    if (index >= m_context->animation_frames.size())
        return Error::from_string_literal("PNGImageDecoderPlugin: Invalid animation frame index");

    // We need to assemble each frame up until the one requested,
    // so decode all bitmaps that haven't been decoded yet.
    for (size_t i = m_context->animation_next_frame_to_render; i <= index; i++) {
        if (i == 0) {
            // If the default image hasn't been loaded, load it now
            TRY(load_default_image()); // May modify animation_frames!

            auto& animation_frame = m_context->animation_frames[i];
            animation_frame.bitmap = m_context->bitmap;
        } else {
            auto& animation_frame = m_context->animation_frames[i];
            VERIFY(!animation_frame.bitmap);

            auto decoded_bitmap = TRY(decode_png_animation_frame_bitmap(*m_context, animation_frame));

            auto prev_animation_frame = m_context->animation_frames[i - 1];
            animation_frame.bitmap = TRY(render_animation_frame(prev_animation_frame, animation_frame, *decoded_bitmap));
        }
        m_context->animation_next_frame_to_render = i + 1;
    }

    auto const& animation_frame = m_context->animation_frames[index];
    VERIFY(animation_frame.bitmap);

    ImageFrameDescriptor descriptor { animation_frame.bitmap };
    set_descriptor_duration(descriptor, animation_frame);
    return descriptor;
}

Optional<Metadata const&> PNGImageDecoderPlugin::metadata()
{
    if (m_context->exif_metadata)
        return *m_context->exif_metadata;
    return OptionalNone {};
}

ErrorOr<Optional<ReadonlyBytes>> PNGImageDecoderPlugin::icc_data()
{
    if (!decode_png_chunks(*m_context))
        return Error::from_string_literal("PNGImageDecoderPlugin: Decoding chunks failed");

    if (m_context->embedded_icc_profile.has_value()) {
        if (!m_context->decompressed_icc_profile.has_value()) {
            auto compressed_data_stream = make<FixedMemoryStream>(m_context->embedded_icc_profile->compressed_data);
            auto decompressor_or_error = Compress::ZlibDecompressor::create(move(compressed_data_stream));
            if (decompressor_or_error.is_error()) {
                m_context->embedded_icc_profile.clear();
                return decompressor_or_error.release_error();
            }
            auto decompressor = decompressor_or_error.release_value();
            auto result_or_error = decompressor->read_until_eof();
            if (result_or_error.is_error()) {
                m_context->embedded_icc_profile.clear();
                return result_or_error.release_error();
            }
            m_context->decompressed_icc_profile = result_or_error.release_value();
        }

        return m_context->decompressed_icc_profile.value();
    }

    // FIXME: Eventually, look at coding_independent_code_points, chromaticities_and_whitepoint, gamma, sRGB_rendering_intent too.
    // The order is:
    // 1. Use coding_independent_code_points if it exists, ignore the rest.
    // 2. Use embedded_icc_profile if it exists, ignore the rest.
    // 3. Use sRGB_rendering_intent if it exists, ignore the rest.
    // 4. Use gamma to adjust gamma and chromaticities_and_whitepoint to adjust color.
    // (Order between 2 and 3 isn't fully clear, but "It is recommended that the sRGB and iCCP chunks do not appear simultaneously in a PNG datastream."

    return OptionalNone {};
}

}
