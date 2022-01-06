/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/Vector.h>
#include <LibCompress/Zlib.h>
#include <LibGfx/PNGLoader.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

#ifdef __serenity__
#    include <LibCompress/Deflate.h>
#    include <serenity.h>
#endif

namespace Gfx {

static const u8 png_header[8] = { 0x89, 'P', 'N', 'G', 13, 10, 26, 10 };

struct PNG_IHDR {
    NetworkOrdered<u32> width;
    NetworkOrdered<u32> height;
    u8 bit_depth { 0 };
    u8 color_type { 0 };
    u8 compression_method { 0 };
    u8 filter_method { 0 };
    u8 interlace_method { 0 };
};

static_assert(AssertSize<PNG_IHDR, 13>());

struct Scanline {
    u8 filter { 0 };
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
};

template<typename T>
struct [[gnu::packed]] Quad {
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
    const u8* data { nullptr };
    size_t data_size { 0 };
    int width { -1 };
    int height { -1 };
    u8 bit_depth { 0 };
    u8 color_type { 0 };
    u8 compression_method { 0 };
    u8 filter_method { 0 };
    u8 interlace_method { 0 };
    u8 channels { 0 };
    bool has_seen_zlib_header { false };
    bool has_alpha() const { return color_type & 4 || palette_transparency_data.size() > 0; }
    Vector<Scanline> scanlines;
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
    Streamer(const u8* data, size_t size)
        : m_data_ptr(data)
        , m_size_remaining(size)
    {
    }

    template<typename T>
    bool read(T& value)
    {
        if (m_size_remaining < sizeof(T))
            return false;
        value = *((const NetworkOrdered<T>*)m_data_ptr);
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
    const u8* m_data_ptr { nullptr };
    size_t m_size_remaining { 0 };
};

static bool process_chunk(Streamer&, PNGLoadingContext& context);

ALWAYS_INLINE static u8 paeth_predictor(int a, int b, int c)
{
    int p = a + b - c;
    int pa = abs(p - a);
    int pb = abs(p - b);
    int pc = abs(p - c);
    if (pa <= pb && pa <= pc)
        return a;
    if (pb <= pc)
        return b;
    return c;
}

union [[gnu::packed]] Pixel {
    RGBA32 rgba { 0 };
    u8 v[4];
    struct {
        u8 r;
        u8 g;
        u8 b;
        u8 a;
    };
};
static_assert(AssertSize<Pixel, 4>());

template<bool has_alpha, u8 filter_type>
ALWAYS_INLINE static void unfilter_impl(Gfx::Bitmap& bitmap, int y, const void* dummy_scanline_data)
{
    auto* dummy_scanline = (const Pixel*)dummy_scanline_data;
    if constexpr (filter_type == 0) {
        auto* pixels = (Pixel*)bitmap.scanline(y);
        for (int i = 0; i < bitmap.width(); ++i) {
            auto& x = pixels[i];
            swap(x.r, x.b);
        }
    }

    if constexpr (filter_type == 1) {
        auto* pixels = (Pixel*)bitmap.scanline(y);
        swap(pixels[0].r, pixels[0].b);
        for (int i = 1; i < bitmap.width(); ++i) {
            auto& x = pixels[i];
            swap(x.r, x.b);
            auto& a = (const Pixel&)pixels[i - 1];
            x.v[0] += a.v[0];
            x.v[1] += a.v[1];
            x.v[2] += a.v[2];
            if constexpr (has_alpha)
                x.v[3] += a.v[3];
        }
        return;
    }
    if constexpr (filter_type == 2) {
        auto* pixels = (Pixel*)bitmap.scanline(y);
        auto* pixels_y_minus_1 = y == 0 ? dummy_scanline : (const Pixel*)bitmap.scanline(y - 1);
        for (int i = 0; i < bitmap.width(); ++i) {
            auto& x = pixels[i];
            swap(x.r, x.b);
            const Pixel& b = pixels_y_minus_1[i];
            x.v[0] += b.v[0];
            x.v[1] += b.v[1];
            x.v[2] += b.v[2];
            if constexpr (has_alpha)
                x.v[3] += b.v[3];
        }
        return;
    }
    if constexpr (filter_type == 3) {
        auto* pixels = (Pixel*)bitmap.scanline(y);
        auto* pixels_y_minus_1 = y == 0 ? dummy_scanline : (const Pixel*)bitmap.scanline(y - 1);
        for (int i = 0; i < bitmap.width(); ++i) {
            auto& x = pixels[i];
            swap(x.r, x.b);
            Pixel a;
            if (i != 0)
                a = pixels[i - 1];
            const Pixel& b = pixels_y_minus_1[i];
            x.v[0] = x.v[0] + ((a.v[0] + b.v[0]) / 2);
            x.v[1] = x.v[1] + ((a.v[1] + b.v[1]) / 2);
            x.v[2] = x.v[2] + ((a.v[2] + b.v[2]) / 2);
            if constexpr (has_alpha)
                x.v[3] = x.v[3] + ((a.v[3] + b.v[3]) / 2);
        }
        return;
    }
    if constexpr (filter_type == 4) {
        auto* pixels = (Pixel*)bitmap.scanline(y);
        auto* pixels_y_minus_1 = y == 0 ? dummy_scanline : (Pixel*)bitmap.scanline(y - 1);
        for (int i = 0; i < bitmap.width(); ++i) {
            auto& x = pixels[i];
            swap(x.r, x.b);
            Pixel a;
            const Pixel& b = pixels_y_minus_1[i];
            Pixel c;
            if (i != 0) {
                a = pixels[i - 1];
                c = pixels_y_minus_1[i - 1];
            }
            x.v[0] += paeth_predictor(a.v[0], b.v[0], c.v[0]);
            x.v[1] += paeth_predictor(a.v[1], b.v[1], c.v[1]);
            x.v[2] += paeth_predictor(a.v[2], b.v[2], c.v[2]);
            if constexpr (has_alpha)
                x.v[3] += paeth_predictor(a.v[3], b.v[3], c.v[3]);
        }
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
        auto* tuples = reinterpret_cast<const Tuple<T>*>(context.scanlines[y].data.data());
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
        auto* triplets = reinterpret_cast<const Triplet<T>*>(context.scanlines[y].data.data());
        for (int i = 0; i < context.width; ++i) {
            auto& pixel = (Pixel&)context.bitmap->scanline(y)[i];
            pixel.r = triplets[i].r;
            pixel.g = triplets[i].g;
            pixel.b = triplets[i].b;
            pixel.a = 0xff;
        }
    }
}

NEVER_INLINE FLATTEN static ErrorOr<void> unfilter(PNGLoadingContext& context)
{
    // First unpack the scanlines to RGBA:
    switch (context.color_type) {
    case 0:
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
    case 4:
        if (context.bit_depth == 8) {
            unpack_grayscale_with_alpha<u8>(context);
        } else if (context.bit_depth == 16) {
            unpack_grayscale_with_alpha<u16>(context);
        } else {
            VERIFY_NOT_REACHED();
        }
        break;
    case 2:
        if (context.bit_depth == 8) {
            unpack_triplets_without_alpha<u8>(context);
        } else if (context.bit_depth == 16) {
            unpack_triplets_without_alpha<u16>(context);
        } else {
            VERIFY_NOT_REACHED();
        }
        break;
    case 6:
        if (context.bit_depth == 8) {
            for (int y = 0; y < context.height; ++y) {
                memcpy(context.bitmap->scanline(y), context.scanlines[y].data.data(), context.scanlines[y].data.size());
            }
        } else if (context.bit_depth == 16) {
            for (int y = 0; y < context.height; ++y) {
                auto* triplets = reinterpret_cast<const Quad<u16>*>(context.scanlines[y].data.data());
                for (int i = 0; i < context.width; ++i) {
                    auto& pixel = (Pixel&)context.bitmap->scanline(y)[i];
                    pixel.r = triplets[i].r & 0xFF;
                    pixel.g = triplets[i].g & 0xFF;
                    pixel.b = triplets[i].b & 0xFF;
                    pixel.a = triplets[i].a & 0xFF;
                }
            }
        } else {
            VERIFY_NOT_REACHED();
        }
        break;
    case 3:
        if (context.bit_depth == 8) {
            for (int y = 0; y < context.height; ++y) {
                auto* palette_index = context.scanlines[y].data.data();
                for (int i = 0; i < context.width; ++i) {
                    auto& pixel = (Pixel&)context.bitmap->scanline(y)[i];
                    if (palette_index[i] >= context.palette_data.size())
                        return Error::from_string_literal("PNGImageDecoderPlugin: Palette index out of range"sv);
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
                        return Error::from_string_literal("PNGImageDecoderPlugin: Palette index out of range"sv);
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

    u8 dummy_scanline[context.width * sizeof(RGBA32)];
    memset(dummy_scanline, 0, sizeof(dummy_scanline));

    for (int y = 0; y < context.height; ++y) {
        auto filter = context.scanlines[y].filter;
        if (filter == 0) {
            if (context.has_alpha())
                unfilter_impl<true, 0>(*context.bitmap, y, dummy_scanline);
            else
                unfilter_impl<false, 0>(*context.bitmap, y, dummy_scanline);
            continue;
        }
        if (filter == 1) {
            if (context.has_alpha())
                unfilter_impl<true, 1>(*context.bitmap, y, dummy_scanline);
            else
                unfilter_impl<false, 1>(*context.bitmap, y, dummy_scanline);
            continue;
        }
        if (filter == 2) {
            if (context.has_alpha())
                unfilter_impl<true, 2>(*context.bitmap, y, dummy_scanline);
            else
                unfilter_impl<false, 2>(*context.bitmap, y, dummy_scanline);
            continue;
        }
        if (filter == 3) {
            if (context.has_alpha())
                unfilter_impl<true, 3>(*context.bitmap, y, dummy_scanline);
            else
                unfilter_impl<false, 3>(*context.bitmap, y, dummy_scanline);
            continue;
        }
        if (filter == 4) {
            if (context.has_alpha())
                unfilter_impl<true, 4>(*context.bitmap, y, dummy_scanline);
            else
                unfilter_impl<false, 4>(*context.bitmap, y, dummy_scanline);
            continue;
        }
    }

    return {};
}

static bool decode_png_header(PNGLoadingContext& context)
{
    if (context.state >= PNGLoadingContext::HeaderDecoded)
        return true;

    if (!context.data || context.data_size < sizeof(png_header)) {
        dbgln_if(PNG_DEBUG, "Missing PNG header");
        context.state = PNGLoadingContext::State::Error;
        return false;
    }

    if (memcmp(context.data, png_header, sizeof(png_header)) != 0) {
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

    const u8* data_ptr = context.data + sizeof(png_header);
    size_t data_remaining = context.data_size - sizeof(png_header);

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

    const u8* data_ptr = context.data + sizeof(png_header);
    int data_remaining = context.data_size - sizeof(png_header);

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
        u8 filter;
        if (!streamer.read(filter)) {
            context.state = PNGLoadingContext::State::Error;
            return Error::from_string_literal("PNGImageDecoderPlugin: Decoding failed"sv);
        }

        if (filter > 4) {
            context.state = PNGLoadingContext::State::Error;
            return Error::from_string_literal("PNGImageDecoderPlugin: Invalid PNG filter"sv);
        }

        context.scanlines.append({ filter });
        auto& scanline_buffer = context.scanlines.last().data;
        auto row_size = context.compute_row_size_for_width(context.width);
        if (row_size.has_overflow())
            return Error::from_string_literal("PNGImageDecoderPlugin: Row size overflow"sv);

        if (!streamer.wrap_bytes(scanline_buffer, row_size.value())) {
            context.state = PNGLoadingContext::State::Error;
            return Error::from_string_literal("PNGImageDecoderPlugin: Decoding failed"sv);
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
        u8 filter;
        if (!streamer.read(filter)) {
            context.state = PNGLoadingContext::State::Error;
            return Error::from_string_literal("PNGImageDecoderPlugin: Decoding failed"sv);
        }

        if (filter > 4) {
            context.state = PNGLoadingContext::State::Error;
            return Error::from_string_literal("PNGImageDecoderPlugin: Invalid PNG filter"sv);
        }

        subimage_context.scanlines.append({ filter });
        auto& scanline_buffer = subimage_context.scanlines.last().data;

        auto row_size = context.compute_row_size_for_width(subimage_context.width);
        if (row_size.has_overflow())
            return Error::from_string_literal("PNGImageDecoderPlugin: Row size overflow"sv);
        if (!streamer.wrap_bytes(scanline_buffer, row_size.value())) {
            context.state = PNGLoadingContext::State::Error;
            return Error::from_string_literal("PNGImageDecoderPlugin: Decoding failed"sv);
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
            return Error::from_string_literal("PNGImageDecoderPlugin: Decoding failed"sv);
    }

    if (context.state >= PNGLoadingContext::State::BitmapDecoded)
        return {};

    if (context.width == -1 || context.height == -1)
        return Error::from_string_literal("PNGImageDecoderPlugin: Didn't see an IHDR chunk."sv);

    if (context.color_type == 3 && context.palette_data.is_empty())
        return Error::from_string_literal("PNGImageDecoderPlugin: Didn't see a PLTE chunk for a palletized image, or it was empty."sv);

    auto result = Compress::Zlib::decompress_all(context.compressed_data.span());
    if (!result.has_value()) {
        context.state = PNGLoadingContext::State::Error;
        return Error::from_string_literal("PNGImageDecoderPlugin: Decompression failed"sv);
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
        return Error::from_string_literal("PNGImageDecoderPlugin: Invalid interlace method"sv);
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
    dbgln_if(PNG_DEBUG, "     Color type: {}", context.color_type);
    dbgln_if(PNG_DEBUG, "Compress Method: {}", context.compression_method);
    dbgln_if(PNG_DEBUG, "  Filter Method: {}", context.filter_method);
    dbgln_if(PNG_DEBUG, " Interlace type: {}", context.interlace_method);

    if (context.interlace_method != PngInterlaceMethod::Null && context.interlace_method != PngInterlaceMethod::Adam7) {
        dbgln_if(PNG_DEBUG, "PNGLoader::process_IHDR: unknown interlace method: {}", context.interlace_method);
        return false;
    }

    switch (context.color_type) {
    case 0: // Each pixel is a grayscale sample.
        if (context.bit_depth != 1 && context.bit_depth != 2 && context.bit_depth != 4 && context.bit_depth != 8 && context.bit_depth != 16)
            return false;
        context.channels = 1;
        break;
    case 4: // Each pixel is a grayscale sample, followed by an alpha sample.
        if (context.bit_depth != 8 && context.bit_depth != 16)
            return false;
        context.channels = 2;
        break;
    case 2: // Each pixel is an RGB sample
        if (context.bit_depth != 8 && context.bit_depth != 16)
            return false;
        context.channels = 3;
        break;
    case 3: // Each pixel is a palette index; a PLTE chunk must appear.
        if (context.bit_depth != 1 && context.bit_depth != 2 && context.bit_depth != 4 && context.bit_depth != 8)
            return false;
        context.channels = 1;
        break;
    case 6: // Each pixel is an RGB sample, followed by an alpha sample.
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
    context.palette_data.append((const PaletteEntry*)data.data(), data.size() / 3);
    return true;
}

static bool process_tRNS(ReadonlyBytes data, PNGLoadingContext& context)
{
    switch (context.color_type) {
    case 3:
        context.palette_transparency_data.append(data.data(), data.size());
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

    if (!strcmp((const char*)chunk_type, "IHDR"))
        return process_IHDR(chunk_data, context);
    if (!strcmp((const char*)chunk_type, "IDAT"))
        return process_IDAT(chunk_data, context);
    if (!strcmp((const char*)chunk_type, "PLTE"))
        return process_PLTE(chunk_data, context);
    if (!strcmp((const char*)chunk_type, "tRNS"))
        return process_tRNS(chunk_data, context);
    return true;
}

PNGImageDecoderPlugin::PNGImageDecoderPlugin(const u8* data, size_t size)
{
    m_context = make<PNGLoadingContext>();
    m_context->data = data;
    m_context->data_size = size;
}

PNGImageDecoderPlugin::~PNGImageDecoderPlugin()
{
}

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
        return Error::from_string_literal("PNGImageDecoderPlugin: Invalid frame index"sv);

    if (m_context->state == PNGLoadingContext::State::Error)
        return Error::from_string_literal("PNGImageDecoderPlugin: Decoding failed"sv);

    if (m_context->state < PNGLoadingContext::State::BitmapDecoded) {
        // NOTE: This forces the chunk decoding to happen.
        TRY(decode_png_bitmap(*m_context));
    }

    VERIFY(m_context->bitmap);
    return ImageFrameDescriptor { m_context->bitmap, 0 };
}

}
