/*
 * Copyright (c) 2020, Paul Roukema <roukemap@gmail.com>
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
#include <AK/LexicalPath.h>
#include <AK/MappedFile.h>
#include <AK/MemoryStream.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/Types.h>
#include <LibGfx/ICOLoader.h>
#include <LibGfx/PNGLoader.h>
#include <math.h>
#include <stdio.h>
#include <string.h>

//#define ICO_DEBUG

namespace Gfx {

// FIXME: This is in little-endian order. Maybe need a NetworkOrdered<T> equivalent eventually.
struct ICONDIR {
    u16 must_be_0 = 0;
    u16 must_be_1 = 0;
    u16 image_count = 0;
};
static_assert(sizeof(ICONDIR) == 6);

struct ICONDIRENTRY {
    u8 width;
    u8 height;
    u8 color_count;
    u8 reserved_0;
    u16 planes;
    u16 bits_per_pixel;
    u32 size;
    u32 offset;
};
static_assert(sizeof(ICONDIRENTRY) == 16);

struct [[gnu::packed]] BMPFILEHEADER
{
    u8 signature[2];
    u32 size;
    u16 reserved1;
    u16 reserved2;
    u32 offset;
};
static_assert(sizeof(BMPFILEHEADER) == 14);

struct BITMAPINFOHEADER {
    u32 size;
    i32 width;
    i32 height;
    u16 planes;
    u16 bpp;
    u32 compression;
    u32 size_image;
    u32 vres;
    u32 hres;
    u32 palette_size;
    u32 important_colors;
};
static_assert(sizeof(BITMAPINFOHEADER) == 40);

struct [[gnu::packed]] BMP_ARGB
{
    u8 b;
    u8 g;
    u8 r;
    u8 a;
};
static_assert(sizeof(BMP_ARGB) == 4);

struct ImageDescriptor {
    u16 width;
    u16 height;
    size_t offset;
    size_t size;
    RefPtr<Gfx::Bitmap> bitmap;
};

struct ICOLoadingContext {
    enum State {
        NotDecoded = 0,
        Error,
        DirectoryDecoded,
        BitmapDecoded
    };
    State state { NotDecoded };
    const u8* data { nullptr };
    size_t data_size { 0 };
    Vector<ImageDescriptor> images;
    size_t largest_index;
};

RefPtr<Gfx::Bitmap> load_ico(const StringView& path)
{
    MappedFile mapped_file(path);
    if (!mapped_file.is_valid())
        return nullptr;
    ICOImageDecoderPlugin decoder((const u8*)mapped_file.data(), mapped_file.size());
    auto bitmap = decoder.bitmap();
    if (bitmap)
        bitmap->set_mmap_name(String::format("Gfx::Bitmap [%dx%d] - Decoded ICO: %s", bitmap->width(), bitmap->height(), LexicalPath::canonicalized_path(path).characters()));
    return bitmap;
}

RefPtr<Gfx::Bitmap> load_ico_from_memory(const u8* data, size_t length)
{
    ICOImageDecoderPlugin decoder(data, length);
    auto bitmap = decoder.bitmap();
    if (bitmap)
        bitmap->set_mmap_name(String::format("Gfx::Bitmap [%dx%d] - Decoded ICO: <memory>", bitmap->width(), bitmap->height()));
    return bitmap;
}

static Optional<size_t> decode_ico_header(InputMemoryStream& stream)
{
    ICONDIR header;
    stream >> Bytes { &header, sizeof(header) };
    if (stream.handle_any_error())
        return {};

    if (header.must_be_0 != 0 || header.must_be_1 != 1)
        return {};
    return { header.image_count };
}

static Optional<ImageDescriptor> decode_ico_direntry(InputMemoryStream& stream)
{
    ICONDIRENTRY entry;
    stream >> Bytes { &entry, sizeof(entry) };
    if (stream.handle_any_error())
        return {};

    ImageDescriptor desc = { entry.width, entry.height, entry.offset, entry.size, nullptr };
    if (desc.width == 0)
        desc.width = 256;
    if (desc.height == 0)
        desc.height = 256;

    return { desc };
}

static size_t find_largest_image(const ICOLoadingContext& context)
{
    size_t max_area = 0;
    size_t index = 0;
    size_t largest_index = 0;
    for (const auto& desc : context.images) {
        if (desc.width * desc.height > max_area) {
            max_area = desc.width * desc.height;
            largest_index = index;
        }
        ++index;
    }
    return largest_index;
}

static bool load_ico_directory(ICOLoadingContext& context)
{
    InputMemoryStream stream { { context.data, context.data_size } };

    auto image_count = decode_ico_header(stream);
    if (!image_count.has_value() || image_count.value() == 0) {
        return false;
    }

    for (size_t i = 0; i < image_count.value(); ++i) {
        auto maybe_desc = decode_ico_direntry(stream);
        if (!maybe_desc.has_value()) {
#ifdef ICO_DEBUG
            printf("load_ico_directory: error loading entry: %lu\n", i);
#endif
            return false;
        }

        auto& desc = maybe_desc.value();
        if (desc.offset + desc.size < desc.offset // detect integer overflow
            || (desc.offset + desc.size) > context.data_size) {
#ifdef ICO_DEBUG
            printf("load_ico_directory: offset: %lu size: %lu doesn't fit in ICO size: %lu\n",
                desc.offset, desc.size, context.data_size);
#endif
            return false;
        }
#ifdef ICO_DEBUG
        printf("load_ico_directory: index %lu width: %u height: %u offset: %lu size: %lu\n",
            i, desc.width, desc.height, desc.offset, desc.size);
#endif
        context.images.append(desc);
    }
    context.largest_index = find_largest_image(context);
    context.state = ICOLoadingContext::State::DirectoryDecoded;
    return true;
}

static bool load_ico_bmp(ICOLoadingContext& context, ImageDescriptor& desc)
{
    BITMAPINFOHEADER info;
    if (desc.size < sizeof(info))
        return false;

    memcpy(&info, context.data + desc.offset, sizeof(info));
    if (info.size != sizeof(info)) {
#ifdef ICO_DEBUG
        printf("load_ico_bmp: info size: %u, expected: %lu\n", info.size, sizeof(info));
#endif
        return false;
    }

    if (info.width < 0) {
#ifdef ICO_DEBUG
        printf("load_ico_bmp: width %d < 0\n", info.width);
#endif
        return false;
    }
    bool topdown = false;
    if (info.height < 0) {
        topdown = true;
        info.height = -info.height;
    }

    if (info.planes != 1) {
#ifdef ICO_DEBUG
        printf("load_ico_bmp: planes: %d != 1", info.planes);
#endif
        return false;
    }

    if (info.bpp != 32) {
#ifdef ICO_DEBUG
        printf("load_ico_bmp: unsupported bpp: %u\n", info.bpp);
#endif
        return false;
    }

#ifdef ICO_DEBUG
    printf("load_ico_bmp: width: %d height: %d direction: %s bpp: %d size_image: %u\n",
        info.width, info.height, topdown ? "TopDown" : "BottomUp", info.bpp, info.size_image);
#endif

    if (info.compression != 0 || info.palette_size != 0 || info.important_colors != 0) {
#ifdef ICO_DEBUG
        printf("load_ico_bmp: following fields must be 0: compression: %u palette_size: %u important_colors: %u\n",
            info.compression, info.palette_size, info.important_colors);
#endif
        return false;
    }

    if (info.width != desc.width || info.height != 2 * desc.height) {
#ifdef ICO_DEBUG
        printf("load_ico_bmp: size mismatch: ico %dx%d, bmp %dx%d\n",
            desc.width, desc.height, info.width, info.height);
#endif
        return false;
    }

    // Mask is 1bpp, and each row must be 4-byte aligned
    size_t mask_row_len = align_up_to(align_up_to(desc.width, 8) / 8, 4);
    size_t required_len = desc.height * (desc.width * sizeof(BMP_ARGB) + mask_row_len);
    size_t available_len = desc.size - sizeof(info);
    if (required_len > available_len) {
#ifdef ICO_DEBUG
        printf("load_ico_bmp: required_len: %lu > available_len: %lu\n",
            required_len, available_len);
#endif
        return false;
    }

    desc.bitmap = Bitmap::create_purgeable(BitmapFormat::RGBA32, { desc.width, desc.height });
    Bitmap& bitmap = *desc.bitmap;
    const u8* image_base = context.data + desc.offset + sizeof(info);
    const BMP_ARGB* data_base = (const BMP_ARGB*)image_base;
    const u8* mask_base = image_base + desc.width * desc.height * sizeof(BMP_ARGB);
    for (int y = 0; y < desc.height; y++) {
        const u8* row_mask = mask_base + mask_row_len * y;
        const BMP_ARGB* row_data = data_base + desc.width * y;
        for (int x = 0; x < desc.width; x++) {
            u8 mask = !!(row_mask[x / 8] & (0x80 >> (x % 8)));
            BMP_ARGB data = row_data[x];
            bitmap.set_pixel(x, topdown ? y : desc.height - y - 1,
                Color(data.r, data.g, data.b, mask ? 0 : data.a));
        }
    }
    return true;
}

static bool load_ico_bitmap(ICOLoadingContext& context, Optional<size_t> index)
{
    if (context.state < ICOLoadingContext::State::DirectoryDecoded) {
        if (!load_ico_directory(context)) {
            context.state = ICOLoadingContext::State::Error;
            return false;
        }
        context.state = ICOLoadingContext::State::DirectoryDecoded;
    }
    size_t real_index = context.largest_index;
    if (index.has_value())
        real_index = index.value();
    if (real_index >= context.images.size()) {
        return false;
    }

    ImageDescriptor& desc = context.images[real_index];

    PNGImageDecoderPlugin png_decoder(context.data + desc.offset, desc.size);
    if (png_decoder.sniff()) {
        desc.bitmap = png_decoder.bitmap();
        if (!desc.bitmap) {
#ifdef ICO_DEBUG
            printf("load_ico_bitmap: failed to load PNG encoded image index: %lu\n", real_index);
#endif
            return false;
        }
        return true;
    } else {
        if (!load_ico_bmp(context, desc)) {
#ifdef ICO_DEBUG
            printf("load_ico_bitmap: failed to load BMP encoded image index: %lu\n", real_index);
#endif
            return false;
        }
        return true;
    }
}

ICOImageDecoderPlugin::ICOImageDecoderPlugin(const u8* data, size_t size)
{
    m_context = make<ICOLoadingContext>();
    m_context->data = data;
    m_context->data_size = size;
}

ICOImageDecoderPlugin::~ICOImageDecoderPlugin() { }

IntSize ICOImageDecoderPlugin::size()
{
    if (m_context->state == ICOLoadingContext::State::Error) {
        return {};
    }

    if (m_context->state < ICOLoadingContext::State::DirectoryDecoded) {
        if (!load_ico_directory(*m_context)) {
            m_context->state = ICOLoadingContext::State::Error;
            return {};
        }
        m_context->state = ICOLoadingContext::State::DirectoryDecoded;
    }

    return { m_context->images[m_context->largest_index].width, m_context->images[m_context->largest_index].height };
}

RefPtr<Gfx::Bitmap> ICOImageDecoderPlugin::bitmap()
{
    if (m_context->state == ICOLoadingContext::State::Error)
        return nullptr;

    if (m_context->state < ICOLoadingContext::State::BitmapDecoded) {
        // NOTE: This forces the chunk decoding to happen.
        bool success = load_ico_bitmap(*m_context, {});
        if (!success) {
            m_context->state = ICOLoadingContext::State::Error;
            return nullptr;
        }
        m_context->state = ICOLoadingContext::State::BitmapDecoded;
    }

    ASSERT(m_context->images[m_context->largest_index].bitmap);
    return m_context->images[m_context->largest_index].bitmap;
}

void ICOImageDecoderPlugin::set_volatile()
{
    if (m_context->images[0].bitmap)
        m_context->images[0].bitmap->set_volatile();
}

bool ICOImageDecoderPlugin::set_nonvolatile()
{
    if (!m_context->images[0].bitmap)
        return false;
    return m_context->images[0].bitmap->set_nonvolatile();
}

bool ICOImageDecoderPlugin::sniff()
{
    InputMemoryStream stream { { m_context->data, m_context->data_size } };
    return decode_ico_header(stream).has_value();
}

bool ICOImageDecoderPlugin::is_animated()
{
    return false;
}

size_t ICOImageDecoderPlugin::loop_count()
{
    return 0;
}

size_t ICOImageDecoderPlugin::frame_count()
{
    return 1;
}

ImageFrameDescriptor ICOImageDecoderPlugin::frame(size_t i)
{
    if (i > 0) {
        return { bitmap(), 0 };
    }
    return {};
}

}
