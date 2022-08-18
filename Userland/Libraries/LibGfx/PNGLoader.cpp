/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/Vector.h>
#include <LibCompress/Zlib.h>
#include <LibGfx/PNGLoader.h>
#include <LibGfx/PNGShared.h>
#include <string.h>

#ifdef __serenity__
#    include <LibCompress/Deflate.h>
#endif

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

struct PNGLoadingContext {
    enum State {
        NotDecoded = 0,
        Error,
        HeaderDecoded,
        SizeDecoded,
        ChunksDecoded,
        BitmapDecoded,
    };
    State state { State::NotDecoded };
    u8 const* data { nullptr };
    size_t data_size { 0 };
    int width { -1 };
    int height { -1 };
    u8 bit_depth { 0 };
    PNG::ColorType color_type { 0 };
    u8 compression_method { 0 };
    u8 filter_method { 0 };
    u8 interlace_method { 0 };
    u8 channels { 0 };
    bool has_seen_zlib_header { false };
    bool has_alpha() const { return to_underlying(color_type) & 4 || palette_transparency_data.size() > 0; }
    Vector<Scanline> scanlines;
    ByteBuffer unfiltered_data;
    RefPtr<Gfx::Bitmap> bitmap;
    ByteBuffer* decompression_buffer { nullptr };
    Vector<u8> compressed_data;
    Vector<PaletteEntry> palette_data;
    Vector<u8> palette_transparency_data;

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

    bool at_end() const { return !m_size_remaining; }

private:
    u8 const* m_data_ptr { nullptr };
    size_t m_size_remaining { 0 };
};

static bool process_chunk(Streamer&, PNGLoadingContext& context);

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

static void unfilter_scanline(PNG::FilterType filter, Bytes scanline_data, ReadonlyBytes previous_scanlines_data, u8 bytes_per_complete_pixel)
{
    VERIFY(filter != PNG::FilterType::None);

    switch (filter) {
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
            i32 predictor = left + above - upper_left;
            u32 predictor_left = abs(predictor - left);
            u32 predictor_above = abs(predictor - above);
            u32 predictor_upper_left = abs(predictor - upper_left);
            u8 nearest;
            if (predictor_left <= predictor_above && predictor_left <= predictor_upper_left) {
                nearest = left;
            } else if (predictor_above <= predictor_upper_left) {
                nearest = above;
            } else {
                nearest = upper_left;
            }
            scanline_data[i] += nearest;
        }
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

template<typename T>
ALWAYS_INLINE static void unpack_grayscale_without_alpha(PNGLoadingContext& context)
{
    for (int y = 0; y < context.height; ++y) {
        auto* gray_values = reinterpret_cast<const T*>(context.scanlines[y].data.data());
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
    u8 bytes_per_complete_pixel = (context.bit_depth + 7) / 8 * context.channels;

    u8 dummy_scanline_bytes[bytes_per_scanline];
    memset(dummy_scanline_bytes, 0, sizeof(dummy_scanline_bytes));
    auto previous_scanlines_data = ReadonlyBytes { dummy_scanline_bytes, sizeof(dummy_scanline_bytes) };

    for (int y = 0, data_start = 0; y < context.height; ++y) {
        if (context.scanlines[y].filter != PNG::FilterType::None) {
            auto scanline_data_slice = context.unfiltered_data.bytes().slice(data_start, bytes_per_scanline);

            // Copy the current values over and set the scanline's data to the to-be-mutated slice
            context.scanlines[y].data.copy_to(scanline_data_slice);
            context.scanlines[y].data = scanline_data_slice;

            unfilter_scanline(context.scanlines[y].filter, scanline_data_slice, previous_scanlines_data, bytes_per_complete_pixel);

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
                        ? context.palette_transparency_data.data()[palette_index[i]]
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
                        ? context.palette_transparency_data.data()[palette_index]
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
    if (context.state >= PNGLoadingContext::HeaderDecoded)
        return true;

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

    context.state = PNGLoadingContext::HeaderDecoded;
    return true;
}

static bool decode_png_size(PNGLoadingContext& context)
{
    if (context.state >= PNGLoadingContext::SizeDecoded)
        return true;

    if (context.state < PNGLoadingContext::HeaderDecoded) {
        if (!decode_png_header(context))
            return false;
    }

    u8 const* data_ptr = context.data + sizeof(PNG::header);
    size_t data_remaining = context.data_size - sizeof(PNG::header);

    Streamer streamer(data_ptr, data_remaining);
    while (!streamer.at_end()) {
        if (!process_chunk(streamer, context)) {
            context.state = PNGLoadingContext::State::Error;
            return false;
        }
        if (context.width && context.height) {
            context.state = PNGLoadingContext::State::SizeDecoded;
            return true;
        }
    }

    return false;
}

static bool decode_png_chunks(PNGLoadingContext& context)
{
    if (context.state >= PNGLoadingContext::State::ChunksDecoded)
        return true;

    if (context.state < PNGLoadingContext::HeaderDecoded) {
        if (!decode_png_header(context))
            return false;
    }

    u8 const* data_ptr = context.data + sizeof(PNG::header);
    int data_remaining = context.data_size - sizeof(PNG::header);

    context.compressed_data.ensure_capacity(context.data_size);

    Streamer streamer(data_ptr, data_remaining);
    while (!streamer.at_end()) {
        if (!process_chunk(streamer, context)) {
            // Ignore failed chunk and just consider chunk decoding being done.
            // decode_png_bitmap() will check whether we got all required ones anyway.
            break;
        }
    }

    context.state = PNGLoadingContext::State::ChunksDecoded;
    return true;
}

static ErrorOr<void> decode_png_bitmap_simple(PNGLoadingContext& context)
{
    Streamer streamer(context.decompression_buffer->data(), context.decompression_buffer->size());

    for (int y = 0; y < context.height; ++y) {
        PNG::FilterType filter;
        if (!streamer.read(filter)) {
            context.state = PNGLoadingContext::State::Error;
            return Error::from_string_literal("PNGImageDecoderPlugin: Decoding failed");
        }

        if (to_underlying(filter) > 4) {
            context.state = PNGLoadingContext::State::Error;
            return Error::from_string_literal("PNGImageDecoderPlugin: Invalid PNG filter");
        }

        context.scanlines.append({ filter });
        auto& scanline_buffer = context.scanlines.last().data;
        auto row_size = context.compute_row_size_for_width(context.width);
        if (row_size.has_overflow())
            return Error::from_string_literal("PNGImageDecoderPlugin: Row size overflow");

        if (!streamer.wrap_bytes(scanline_buffer, row_size.value())) {
            context.state = PNGLoadingContext::State::Error;
            return Error::from_string_literal("PNGImageDecoderPlugin: Decoding failed");
        }
    }

    context.bitmap = TRY(Bitmap::try_create(context.has_alpha() ? BitmapFormat::BGRA8888 : BitmapFormat::BGRx8888, { context.width, context.height }));
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
    PNGLoadingContext subimage_context;
    subimage_context.width = adam7_width(context, pass);
    subimage_context.height = adam7_height(context, pass);
    subimage_context.channels = context.channels;
    subimage_context.color_type = context.color_type;
    subimage_context.palette_data = context.palette_data;
    subimage_context.palette_transparency_data = context.palette_transparency_data;
    subimage_context.bit_depth = context.bit_depth;
    subimage_context.filter_method = context.filter_method;

    // For small images, some passes might be empty
    if (!subimage_context.width || !subimage_context.height)
        return {};

    subimage_context.scanlines.clear_with_capacity();
    for (int y = 0; y < subimage_context.height; ++y) {
        PNG::FilterType filter;
        if (!streamer.read(filter)) {
            context.state = PNGLoadingContext::State::Error;
            return Error::from_string_literal("PNGImageDecoderPlugin: Decoding failed");
        }

        if (to_underlying(filter) > 4) {
            context.state = PNGLoadingContext::State::Error;
            return Error::from_string_literal("PNGImageDecoderPlugin: Invalid PNG filter");
        }

        subimage_context.scanlines.append({ filter });
        auto& scanline_buffer = subimage_context.scanlines.last().data;

        auto row_size = context.compute_row_size_for_width(subimage_context.width);
        if (row_size.has_overflow())
            return Error::from_string_literal("PNGImageDecoderPlugin: Row size overflow");
        if (!streamer.wrap_bytes(scanline_buffer, row_size.value())) {
            context.state = PNGLoadingContext::State::Error;
            return Error::from_string_literal("PNGImageDecoderPlugin: Decoding failed");
        }
    }

    subimage_context.bitmap = TRY(Bitmap::try_create(context.bitmap->format(), { subimage_context.width, subimage_context.height }));
    TRY(unfilter(subimage_context));

    // Copy the subimage data into the main image according to the pass pattern
    for (int y = 0, dy = adam7_starty[pass]; y < subimage_context.height && dy < context.height; ++y, dy += adam7_stepy[pass]) {
        for (int x = 0, dx = adam7_startx[pass]; x < subimage_context.width && dy < context.width; ++x, dx += adam7_stepx[pass]) {
            context.bitmap->set_pixel(dx, dy, subimage_context.bitmap->get_pixel(x, y));
        }
    }
    return {};
}

static ErrorOr<void> decode_png_adam7(PNGLoadingContext& context)
{
    Streamer streamer(context.decompression_buffer->data(), context.decompression_buffer->size());
    context.bitmap = TRY(Bitmap::try_create(context.has_alpha() ? BitmapFormat::BGRA8888 : BitmapFormat::BGRx8888, { context.width, context.height }));
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

    if (context.width == -1 || context.height == -1)
        return Error::from_string_literal("PNGImageDecoderPlugin: Didn't see an IHDR chunk.");

    if (context.color_type == PNG::ColorType::IndexedColor && context.palette_data.is_empty())
        return Error::from_string_literal("PNGImageDecoderPlugin: Didn't see a PLTE chunk for a palletized image, or it was empty.");

    auto result = Compress::Zlib::decompress_all(context.compressed_data.span());
    if (!result.has_value()) {
        context.state = PNGLoadingContext::State::Error;
        return Error::from_string_literal("PNGImageDecoderPlugin: Decompression failed");
    }
    context.decompression_buffer = &result.value();
    context.compressed_data.clear();

    context.scanlines.ensure_capacity(context.height);
    switch (context.interlace_method) {
    case PngInterlaceMethod::Null:
        TRY(decode_png_bitmap_simple(context));
        break;
    case PngInterlaceMethod::Adam7:
        TRY(decode_png_adam7(context));
        break;
    default:
        context.state = PNGLoadingContext::State::Error;
        return Error::from_string_literal("PNGImageDecoderPlugin: Invalid interlace method");
    }

    context.decompression_buffer = nullptr;

    context.state = PNGLoadingContext::State::BitmapDecoded;
    return {};
}

static bool is_valid_compression_method(u8 compression_method)
{
    return compression_method == 0;
}

static bool is_valid_filter_method(u8 filter_method)
{
    return filter_method == 0;
}

static bool process_IHDR(ReadonlyBytes data, PNGLoadingContext& context)
{
    if (data.size() < (int)sizeof(PNG_IHDR))
        return false;
    auto& ihdr = *(const PNG_IHDR*)data.data();

    if (ihdr.width > maximum_width_for_decoded_images || ihdr.height > maximum_height_for_decoded_images) {
        dbgln("This PNG is too large for comfort: {}x{}", (u32)ihdr.width, (u32)ihdr.height);
        return false;
    }

    if (!is_valid_compression_method(ihdr.compression_method)) {
        dbgln("PNG has invalid compression method {}", ihdr.compression_method);
        return false;
    }

    if (!is_valid_filter_method(ihdr.filter_method)) {
        dbgln("PNG has invalid filter method {}", ihdr.filter_method);
        return false;
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
        return false;
    }

    switch (context.color_type) {
    case PNG::ColorType::Greyscale:
        if (context.bit_depth != 1 && context.bit_depth != 2 && context.bit_depth != 4 && context.bit_depth != 8 && context.bit_depth != 16)
            return false;
        context.channels = 1;
        break;
    case PNG::ColorType::GreyscaleWithAlpha:
        if (context.bit_depth != 8 && context.bit_depth != 16)
            return false;
        context.channels = 2;
        break;
    case PNG::ColorType::Truecolor:
        if (context.bit_depth != 8 && context.bit_depth != 16)
            return false;
        context.channels = 3;
        break;
    case PNG::ColorType::IndexedColor:
        if (context.bit_depth != 1 && context.bit_depth != 2 && context.bit_depth != 4 && context.bit_depth != 8)
            return false;
        context.channels = 1;
        break;
    case PNG::ColorType::TruecolorWithAlpha:
        if (context.bit_depth != 8 && context.bit_depth != 16)
            return false;
        context.channels = 4;
        break;
    default:
        return false;
    }
    return true;
}

static bool process_IDAT(ReadonlyBytes data, PNGLoadingContext& context)
{
    context.compressed_data.append(data.data(), data.size());
    return true;
}

static bool process_PLTE(ReadonlyBytes data, PNGLoadingContext& context)
{
    context.palette_data.append((PaletteEntry const*)data.data(), data.size() / 3);
    return true;
}

static bool process_tRNS(ReadonlyBytes data, PNGLoadingContext& context)
{
    switch (context.color_type) {
    case PNG::ColorType::Greyscale:
    case PNG::ColorType::Truecolor:
    case PNG::ColorType::IndexedColor:
        context.palette_transparency_data.append(data.data(), data.size());
        break;
    default:
        break;
    }
    return true;
}

static bool process_chunk(Streamer& streamer, PNGLoadingContext& context)
{
    u32 chunk_size;
    if (!streamer.read(chunk_size)) {
        dbgln_if(PNG_DEBUG, "Bail at chunk_size");
        return false;
    }
    u8 chunk_type[5];
    chunk_type[4] = '\0';
    if (!streamer.read_bytes(chunk_type, 4)) {
        dbgln_if(PNG_DEBUG, "Bail at chunk_type");
        return false;
    }
    ReadonlyBytes chunk_data;
    if (!streamer.wrap_bytes(chunk_data, chunk_size)) {
        dbgln_if(PNG_DEBUG, "Bail at chunk_data");
        return false;
    }
    u32 chunk_crc;
    if (!streamer.read(chunk_crc)) {
        dbgln_if(PNG_DEBUG, "Bail at chunk_crc");
        return false;
    }
    dbgln_if(PNG_DEBUG, "Chunk type: '{}', size: {}, crc: {:x}", chunk_type, chunk_size, chunk_crc);

    if (!strcmp((char const*)chunk_type, "IHDR"))
        return process_IHDR(chunk_data, context);
    if (!strcmp((char const*)chunk_type, "IDAT"))
        return process_IDAT(chunk_data, context);
    if (!strcmp((char const*)chunk_type, "PLTE"))
        return process_PLTE(chunk_data, context);
    if (!strcmp((char const*)chunk_type, "tRNS"))
        return process_tRNS(chunk_data, context);
    return true;
}

PNGImageDecoderPlugin::PNGImageDecoderPlugin(u8 const* data, size_t size)
{
    m_context = make<PNGLoadingContext>();
    m_context->data = data;
    m_context->data_size = size;
}

PNGImageDecoderPlugin::~PNGImageDecoderPlugin() = default;

IntSize PNGImageDecoderPlugin::size()
{
    if (m_context->state == PNGLoadingContext::State::Error)
        return {};

    if (m_context->state < PNGLoadingContext::State::SizeDecoded) {
        bool success = decode_png_size(*m_context);
        if (!success)
            return {};
    }

    return { m_context->width, m_context->height };
}

void PNGImageDecoderPlugin::set_volatile()
{
    if (m_context->bitmap)
        m_context->bitmap->set_volatile();
}

bool PNGImageDecoderPlugin::set_nonvolatile(bool& was_purged)
{
    if (!m_context->bitmap)
        return false;
    return m_context->bitmap->set_nonvolatile(was_purged);
}

bool PNGImageDecoderPlugin::sniff()
{
    return decode_png_header(*m_context);
}

bool PNGImageDecoderPlugin::is_animated()
{
    return false;
}

size_t PNGImageDecoderPlugin::loop_count()
{
    return 0;
}

size_t PNGImageDecoderPlugin::frame_count()
{
    return 1;
}

ErrorOr<ImageFrameDescriptor> PNGImageDecoderPlugin::frame(size_t index)
{
    if (index > 0)
        return Error::from_string_literal("PNGImageDecoderPlugin: Invalid frame index");

    if (m_context->state == PNGLoadingContext::State::Error)
        return Error::from_string_literal("PNGImageDecoderPlugin: Decoding failed");

    if (m_context->state < PNGLoadingContext::State::BitmapDecoded) {
        // NOTE: This forces the chunk decoding to happen.
        TRY(decode_png_bitmap(*m_context));
    }

    VERIFY(m_context->bitmap);
    return ImageFrameDescriptor { m_context->bitmap, 0 };
}

}
