/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/ByteBuffer.h>
#include <AK/Endian.h>
#include <AK/LexicalPath.h>
#include <AK/MappedFile.h>
#include <LibCore/puff.h>
#include <LibGfx/PNGLoader.h>
#include <fcntl.h>
#include <math.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

#ifdef __serenity__
#    include <serenity.h>
#endif

//#define PNG_DEBUG

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

static_assert(sizeof(PNG_IHDR) == 13);

struct Scanline {
    u8 filter { 0 };
    ByteBuffer data {};
};

struct [[gnu::packed]] PaletteEntry
{
    u8 r;
    u8 g;
    u8 b;
    //u8 a;
};

template<typename T>
struct [[gnu::packed]] Tuple
{
    T gray;
    T a;
};

template<typename T>
struct [[gnu::packed]] Triplet
{
    T r;
    T g;
    T b;
};

template<typename T>
struct [[gnu::packed]] Quad
{
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
    u8* decompression_buffer { nullptr };
    size_t decompression_buffer_size { 0 };
    Vector<u8> compressed_data;
    Vector<PaletteEntry> palette_data;
    Vector<u8> palette_transparency_data;
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

    bool wrap_bytes(ByteBuffer& buffer, size_t count)
    {
        if (m_size_remaining < count)
            return false;
        buffer = ByteBuffer::wrap(const_cast<u8*>(m_data_ptr), count);
        m_data_ptr += count;
        m_size_remaining -= count;
        return true;
    }

    bool at_end() const { return !m_size_remaining; }

private:
    const u8* m_data_ptr { nullptr };
    size_t m_size_remaining { 0 };
};

static RefPtr<Gfx::Bitmap> load_png_impl(const u8*, size_t);
static bool process_chunk(Streamer&, PNGLoadingContext& context);

RefPtr<Gfx::Bitmap> load_png(const StringView& path)
{
    MappedFile mapped_file(path);
    if (!mapped_file.is_valid())
        return nullptr;
    auto bitmap = load_png_impl((const u8*)mapped_file.data(), mapped_file.size());
    if (bitmap)
        bitmap->set_mmap_name(String::format("Gfx::Bitmap [%dx%d] - Decoded PNG: %s", bitmap->width(), bitmap->height(), LexicalPath::canonicalized_path(path).characters()));
    return bitmap;
}

RefPtr<Gfx::Bitmap> load_png_from_memory(const u8* data, size_t length)
{
    auto bitmap = load_png_impl(data, length);
    if (bitmap)
        bitmap->set_mmap_name(String::format("Gfx::Bitmap [%dx%d] - Decoded PNG: <memory>", bitmap->width(), bitmap->height()));
    return bitmap;
}

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

union [[gnu::packed]] Pixel
{
    RGBA32 rgba { 0 };
    u8 v[4];
    struct {
        u8 r;
        u8 g;
        u8 b;
        u8 a;
    };
};
static_assert(sizeof(Pixel) == 4);

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

NEVER_INLINE FLATTEN static void unfilter(PNGLoadingContext& context)
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
                auto* gray_values = (u8*)context.scanlines[y].data.data();
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
            ASSERT_NOT_REACHED();
        }
        break;
    case 4:
        if (context.bit_depth == 8) {
            unpack_grayscale_with_alpha<u8>(context);
        } else if (context.bit_depth == 16) {
            unpack_grayscale_with_alpha<u16>(context);
        } else {
            ASSERT_NOT_REACHED();
        }
        break;
    case 2:
        if (context.bit_depth == 8) {
            unpack_triplets_without_alpha<u8>(context);
        } else if (context.bit_depth == 16) {
            unpack_triplets_without_alpha<u16>(context);
        } else {
            ASSERT_NOT_REACHED();
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
            ASSERT_NOT_REACHED();
        }
        break;
    case 3:
        if (context.bit_depth == 8) {
            for (int y = 0; y < context.height; ++y) {
                auto* palette_index = (u8*)context.scanlines[y].data.data();
                for (int i = 0; i < context.width; ++i) {
                    auto& pixel = (Pixel&)context.bitmap->scanline(y)[i];
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
                auto* palette_indexes = (u8*)context.scanlines[y].data.data();
                for (int i = 0; i < context.width; ++i) {
                    auto bit_offset = (8 - context.bit_depth) - (context.bit_depth * (i % pixels_per_byte));
                    auto palette_index = (palette_indexes[i / pixels_per_byte] >> bit_offset) & mask;
                    auto& pixel = (Pixel&)context.bitmap->scanline(y)[i];
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
            ASSERT_NOT_REACHED();
        }
        break;
    default:
        ASSERT_NOT_REACHED();
        break;
    }

    auto dummy_scanline = ByteBuffer::create_zeroed(context.width * sizeof(RGBA32));

    for (int y = 0; y < context.height; ++y) {
        auto filter = context.scanlines[y].filter;
        if (filter == 0) {
            if (context.has_alpha())
                unfilter_impl<true, 0>(*context.bitmap, y, dummy_scanline.data());
            else
                unfilter_impl<false, 0>(*context.bitmap, y, dummy_scanline.data());
            continue;
        }
        if (filter == 1) {
            if (context.has_alpha())
                unfilter_impl<true, 1>(*context.bitmap, y, dummy_scanline.data());
            else
                unfilter_impl<false, 1>(*context.bitmap, y, dummy_scanline.data());
            continue;
        }
        if (filter == 2) {
            if (context.has_alpha())
                unfilter_impl<true, 2>(*context.bitmap, y, dummy_scanline.data());
            else
                unfilter_impl<false, 2>(*context.bitmap, y, dummy_scanline.data());
            continue;
        }
        if (filter == 3) {
            if (context.has_alpha())
                unfilter_impl<true, 3>(*context.bitmap, y, dummy_scanline.data());
            else
                unfilter_impl<false, 3>(*context.bitmap, y, dummy_scanline.data());
            continue;
        }
        if (filter == 4) {
            if (context.has_alpha())
                unfilter_impl<true, 4>(*context.bitmap, y, dummy_scanline.data());
            else
                unfilter_impl<false, 4>(*context.bitmap, y, dummy_scanline.data());
            continue;
        }
    }
}

static bool decode_png_header(PNGLoadingContext& context)
{
    if (context.state >= PNGLoadingContext::HeaderDecoded)
        return true;

    if (!context.data || context.data_size < sizeof(png_header)) {
#ifdef PNG_DEBUG
        dbg() << "Missing PNG header";
#endif
        context.state = PNGLoadingContext::State::Error;
        return false;
    }

    if (memcmp(context.data, png_header, sizeof(png_header)) != 0) {
#ifdef PNG_DEBUG
        dbg() << "Invalid PNG header";
#endif
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
            context.state = PNGLoadingContext::State::Error;
            return false;
        }
    }

    context.state = PNGLoadingContext::State::ChunksDecoded;
    return true;
}

static bool decode_png_bitmap_simple(PNGLoadingContext& context)
{
    Streamer streamer(context.decompression_buffer, context.decompression_buffer_size);

    for (int y = 0; y < context.height; ++y) {
        u8 filter;
        if (!streamer.read(filter)) {
            context.state = PNGLoadingContext::State::Error;
            return false;
        }

        if (filter > 4) {
            dbg() << "Invalid PNG filter: " << filter;
            context.state = PNGLoadingContext::State::Error;
            return false;
        }

        context.scanlines.append({ filter });
        auto& scanline_buffer = context.scanlines.last().data;
        auto row_size = ((context.width * context.channels * context.bit_depth) + 7) / 8;
        if (!streamer.wrap_bytes(scanline_buffer, row_size)) {
            context.state = PNGLoadingContext::State::Error;
            return false;
        }
    }

    context.bitmap = Bitmap::create_purgeable(context.has_alpha() ? BitmapFormat::RGBA32 : BitmapFormat::RGB32, { context.width, context.height });

    unfilter(context);

    return true;
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
        ASSERT_NOT_REACHED();
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
        ASSERT_NOT_REACHED();
    }
}

// Index 0 unused (non-interlaced case)
static int adam7_starty[8] = { 0, 0, 0, 4, 0, 2, 0, 1 };
static int adam7_startx[8] = { 0, 0, 4, 0, 2, 0, 1, 0 };
static int adam7_stepy[8] = { 1, 8, 8, 8, 4, 4, 2, 2 };
static int adam7_stepx[8] = { 1, 8, 8, 4, 4, 2, 2, 1 };

static bool decode_adam7_pass(PNGLoadingContext& context, Streamer& streamer, int pass)
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
        return true;

    subimage_context.scanlines.clear_with_capacity();
    for (int y = 0; y < subimage_context.height; ++y) {
        u8 filter;
        if (!streamer.read(filter)) {
            context.state = PNGLoadingContext::State::Error;
            return false;
        }

        if (filter > 4) {
            dbg() << "Invalid PNG filter: " << filter;
            context.state = PNGLoadingContext::State::Error;
            return false;
        }

        subimage_context.scanlines.append({ filter });
        auto& scanline_buffer = subimage_context.scanlines.last().data;
        auto row_size = ((subimage_context.width * context.channels * context.bit_depth) + 7) / 8;
        if (!streamer.wrap_bytes(scanline_buffer, row_size)) {
            context.state = PNGLoadingContext::State::Error;
            return false;
        }
    }

    subimage_context.bitmap = Bitmap::create(context.bitmap->format(), { subimage_context.width, subimage_context.height });
    unfilter(subimage_context);

    // Copy the subimage data into the main image according to the pass pattern
    for (int y = 0, dy = adam7_starty[pass]; y < subimage_context.height && dy < context.height; ++y, dy += adam7_stepy[pass]) {
        for (int x = 0, dx = adam7_startx[pass]; x < subimage_context.width && dy < context.width; ++x, dx += adam7_stepx[pass]) {
            context.bitmap->set_pixel(dx, dy, subimage_context.bitmap->get_pixel(x, y));
        }
    }
    return true;
}

static bool decode_png_adam7(PNGLoadingContext& context)
{
    Streamer streamer(context.decompression_buffer, context.decompression_buffer_size);
    context.bitmap = Bitmap::create_purgeable(context.has_alpha() ? BitmapFormat::RGBA32 : BitmapFormat::RGB32, { context.width, context.height });

    for (int pass = 1; pass <= 7; ++pass) {
        if (!decode_adam7_pass(context, streamer, pass))
            return false;
    }
    return true;
}

static bool decode_png_bitmap(PNGLoadingContext& context)
{
    if (context.state < PNGLoadingContext::State::ChunksDecoded) {
        if (!decode_png_chunks(context))
            return false;
    }

    if (context.state >= PNGLoadingContext::State::BitmapDecoded)
        return true;

    unsigned long srclen = context.compressed_data.size() - 6;
    unsigned long destlen = 0;
    int ret = puff(NULL, &destlen, context.compressed_data.data() + 2, &srclen);
    if (ret != 0) {
        context.state = PNGLoadingContext::State::Error;
        return false;
    }
    context.decompression_buffer_size = destlen;
#ifdef __serenity__
    context.decompression_buffer = (u8*)mmap_with_name(nullptr, context.decompression_buffer_size, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0, "PNG decompression buffer");
#else
    context.decompression_buffer = (u8*)mmap(nullptr, context.decompression_buffer_size, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
#endif

    ret = puff(context.decompression_buffer, &destlen, context.compressed_data.data() + 2, &srclen);
    if (ret != 0) {
        context.state = PNGLoadingContext::State::Error;
        return false;
    }
    context.compressed_data.clear();

    context.scanlines.ensure_capacity(context.height);
    switch (context.interlace_method) {
    case PngInterlaceMethod::Null:
        if (!decode_png_bitmap_simple(context))
            return false;
        break;
    case PngInterlaceMethod::Adam7:
        if (!decode_png_adam7(context))
            return false;
        break;
    default:
        ASSERT_NOT_REACHED();
    }

    munmap(context.decompression_buffer, context.decompression_buffer_size);
    context.decompression_buffer = nullptr;
    context.decompression_buffer_size = 0;

    context.state = PNGLoadingContext::State::BitmapDecoded;
    return true;
}

static RefPtr<Gfx::Bitmap> load_png_impl(const u8* data, size_t data_size)
{
    PNGLoadingContext context;
    context.data = data;
    context.data_size = data_size;

    if (!decode_png_chunks(context))
        return nullptr;

    if (!decode_png_bitmap(context))
        return nullptr;

    return context.bitmap;
}

static bool process_IHDR(const ByteBuffer& data, PNGLoadingContext& context)
{
    if (data.size() < (int)sizeof(PNG_IHDR))
        return false;
    auto& ihdr = *(const PNG_IHDR*)data.data();
    context.width = ihdr.width;
    context.height = ihdr.height;
    context.bit_depth = ihdr.bit_depth;
    context.color_type = ihdr.color_type;
    context.compression_method = ihdr.compression_method;
    context.filter_method = ihdr.filter_method;
    context.interlace_method = ihdr.interlace_method;

#ifdef PNG_DEBUG
    printf("PNG: %dx%d (%d bpp)\n", context.width, context.height, context.bit_depth);
    printf("     Color type: %d\n", context.color_type);
    printf("Compress Method: %d\n", context.compression_method);
    printf("  Filter Method: %d\n", context.filter_method);
    printf(" Interlace type: %d\n", context.interlace_method);
#endif

    if (context.interlace_method != PngInterlaceMethod::Null && context.interlace_method != PngInterlaceMethod::Adam7) {
        dbgprintf("PNGLoader::process_IHDR: unknown interlace method: %d\n", context.interlace_method);
        return false;
    }

    switch (context.color_type) {
    case 0: // Each pixel is a grayscale sample.
        context.channels = 1;
        break;
    case 4: // Each pixel is a grayscale sample, followed by an alpha sample.
        context.channels = 2;
        break;
    case 2: // Each pixel is an RGB sample
        context.channels = 3;
        break;
    case 3: // Each pixel is a palette index; a PLTE chunk must appear.
        context.channels = 1;
        break;
    case 6: // Each pixel is an RGB sample, followed by an alpha sample.
        context.channels = 4;
        break;
    default:
        ASSERT_NOT_REACHED();
    }
    return true;
}

static bool process_IDAT(const ByteBuffer& data, PNGLoadingContext& context)
{
    context.compressed_data.append(data.data(), data.size());
    return true;
}

static bool process_PLTE(const ByteBuffer& data, PNGLoadingContext& context)
{
    context.palette_data.append((const PaletteEntry*)data.data(), data.size() / 3);
    return true;
}

static bool process_tRNS(const ByteBuffer& data, PNGLoadingContext& context)
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
        printf("Bail at chunk_size\n");
        return false;
    }
    u8 chunk_type[5];
    chunk_type[4] = '\0';
    if (!streamer.read_bytes(chunk_type, 4)) {
        printf("Bail at chunk_type\n");
        return false;
    }
    ByteBuffer chunk_data;
    if (!streamer.wrap_bytes(chunk_data, chunk_size)) {
        printf("Bail at chunk_data\n");
        return false;
    }
    u32 chunk_crc;
    if (!streamer.read(chunk_crc)) {
        printf("Bail at chunk_crc\n");
        return false;
    }
#ifdef PNG_DEBUG
    printf("Chunk type: '%s', size: %u, crc: %x\n", chunk_type, chunk_size, chunk_crc);
#endif

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

RefPtr<Gfx::Bitmap> PNGImageDecoderPlugin::bitmap()
{
    if (m_context->state == PNGLoadingContext::State::Error)
        return nullptr;

    if (m_context->state < PNGLoadingContext::State::BitmapDecoded) {
        // NOTE: This forces the chunk decoding to happen.
        bool success = decode_png_bitmap(*m_context);
        if (!success)
            return nullptr;
    }

    ASSERT(m_context->bitmap);
    return m_context->bitmap;
}

void PNGImageDecoderPlugin::set_volatile()
{
    if (m_context->bitmap)
        m_context->bitmap->set_volatile();
}

bool PNGImageDecoderPlugin::set_nonvolatile()
{
    if (!m_context->bitmap)
        return false;
    return m_context->bitmap->set_nonvolatile();
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

ImageFrameDescriptor PNGImageDecoderPlugin::frame(size_t i)
{
    if (i > 0) {
        return { bitmap(), 0 };
    }
    return {};
}

}
