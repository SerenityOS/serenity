/*
 * Copyright (c) 2020, Paul Roukema <roukemap@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/Debug.h>
#include <AK/MemoryStream.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/Types.h>
#include <LibGfx/ICOLoader.h>
#include <LibGfx/PNGLoader.h>
#include <string.h>

namespace Gfx {

// FIXME: This is in little-endian order. Maybe need a NetworkOrdered<T> equivalent eventually.
struct ICONDIR {
    u16 must_be_0 = 0;
    u16 must_be_1 = 0;
    u16 image_count = 0;
};
static_assert(AssertSize<ICONDIR, 6>());

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
static_assert(AssertSize<ICONDIRENTRY, 16>());

struct [[gnu::packed]] BMPFILEHEADER {
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

struct [[gnu::packed]] BMP_ARGB {
    u8 b;
    u8 g;
    u8 r;
    u8 a;
};
static_assert(sizeof(BMP_ARGB) == 4);

struct ICOImageDescriptor {
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
    Vector<ICOImageDescriptor> images;
    size_t largest_index;
};

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

static Optional<ICOImageDescriptor> decode_ico_direntry(InputMemoryStream& stream)
{
    ICONDIRENTRY entry;
    stream >> Bytes { &entry, sizeof(entry) };
    if (stream.handle_any_error())
        return {};

    ICOImageDescriptor desc = { entry.width, entry.height, entry.offset, entry.size, nullptr };
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
            dbgln_if(ICO_DEBUG, "load_ico_directory: error loading entry: {}", i);
            return false;
        }

        auto& desc = maybe_desc.value();
        if (desc.offset + desc.size < desc.offset // detect integer overflow
            || (desc.offset + desc.size) > context.data_size) {
            dbgln_if(ICO_DEBUG, "load_ico_directory: offset: {} size: {} doesn't fit in ICO size: {}", desc.offset, desc.size, context.data_size);
            return false;
        }
        dbgln_if(ICO_DEBUG, "load_ico_directory: index {} width: {} height: {} offset: {} size: {}", i, desc.width, desc.height, desc.offset, desc.size);
        context.images.append(desc);
    }
    context.largest_index = find_largest_image(context);
    context.state = ICOLoadingContext::State::DirectoryDecoded;
    return true;
}

static bool load_ico_bmp(ICOLoadingContext& context, ICOImageDescriptor& desc)
{
    BITMAPINFOHEADER info;
    if (desc.size < sizeof(info))
        return false;

    memcpy(&info, context.data + desc.offset, sizeof(info));
    if (info.size != sizeof(info)) {
        dbgln_if(ICO_DEBUG, "load_ico_bmp: info size: {}, expected: {}", info.size, sizeof(info));
        return false;
    }

    if (info.width < 0) {
        dbgln_if(ICO_DEBUG, "load_ico_bmp: width {} < 0", info.width);
        return false;
    }

    if (info.height == NumericLimits<i32>::min()) {
        dbgln_if(ICO_DEBUG, "load_ico_bmp: height == NumericLimits<i32>::min()");
        return false;
    }

    bool topdown = false;
    if (info.height < 0) {
        topdown = true;
        info.height = -info.height;
    }

    if (info.planes != 1) {
        dbgln_if(ICO_DEBUG, "load_ico_bmp: planes: {} != 1", info.planes);
        return false;
    }

    if (info.bpp != 32) {
        dbgln_if(ICO_DEBUG, "load_ico_bmp: unsupported bpp: {}", info.bpp);
        return false;
    }

    dbgln_if(ICO_DEBUG, "load_ico_bmp: width: {} height: {} direction: {} bpp: {} size_image: {}",
        info.width, info.height, topdown ? "TopDown" : "BottomUp", info.bpp, info.size_image);

    if (info.compression != 0 || info.palette_size != 0 || info.important_colors != 0) {
        dbgln_if(ICO_DEBUG, "load_ico_bmp: following fields must be 0: compression: {} palette_size: {} important_colors: {}", info.compression, info.palette_size, info.important_colors);
        return false;
    }

    if (info.width != desc.width || info.height != 2 * desc.height) {
        dbgln_if(ICO_DEBUG, "load_ico_bmp: size mismatch: ico {}x{}, bmp {}x{}", desc.width, desc.height, info.width, info.height);
        return false;
    }

    // Mask is 1bpp, and each row must be 4-byte aligned
    size_t mask_row_len = align_up_to(align_up_to(desc.width, 8) / 8, 4);
    size_t required_len = desc.height * (desc.width * sizeof(BMP_ARGB) + mask_row_len);
    size_t available_len = desc.size - sizeof(info);
    if (required_len > available_len) {
        dbgln_if(ICO_DEBUG, "load_ico_bmp: required_len: {} > available_len: {}", required_len, available_len);
        return false;
    }

    auto bitmap_or_error = Bitmap::try_create(BitmapFormat::BGRA8888, { desc.width, desc.height });
    if (bitmap_or_error.is_error())
        return false;
    desc.bitmap = bitmap_or_error.release_value_but_fixme_should_propagate_errors();
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

    ICOImageDescriptor& desc = context.images[real_index];

    PNGImageDecoderPlugin png_decoder(context.data + desc.offset, desc.size);
    if (png_decoder.sniff()) {
        auto decoded_png_frame = png_decoder.frame(0);
        if (decoded_png_frame.is_error() || !decoded_png_frame.value().image) {
            dbgln_if(ICO_DEBUG, "load_ico_bitmap: failed to load PNG encoded image index: {}", real_index);
            return false;
        }
        desc.bitmap = decoded_png_frame.value().image;
        return true;
    } else {
        if (!load_ico_bmp(context, desc)) {
            dbgln_if(ICO_DEBUG, "load_ico_bitmap: failed to load BMP encoded image index: {}", real_index);
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

void ICOImageDecoderPlugin::set_volatile()
{
    if (m_context->images[0].bitmap)
        m_context->images[0].bitmap->set_volatile();
}

bool ICOImageDecoderPlugin::set_nonvolatile(bool& was_purged)
{
    if (!m_context->images[0].bitmap)
        return false;
    return m_context->images[0].bitmap->set_nonvolatile(was_purged);
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

ErrorOr<ImageFrameDescriptor> ICOImageDecoderPlugin::frame(size_t index)
{
    if (index > 0)
        return Error::from_string_literal("ICOImageDecoderPlugin: Invalid frame index"sv);

    if (m_context->state == ICOLoadingContext::State::Error)
        return Error::from_string_literal("ICOImageDecoderPlugin: Decoding failed"sv);

    if (m_context->state < ICOLoadingContext::State::BitmapDecoded) {
        // NOTE: This forces the chunk decoding to happen.
        bool success = load_ico_bitmap(*m_context, {});
        if (!success) {
            m_context->state = ICOLoadingContext::State::Error;
            return Error::from_string_literal("ICOImageDecoderPlugin: Decoding failed"sv);
        }
        m_context->state = ICOLoadingContext::State::BitmapDecoded;
    }

    VERIFY(m_context->images[m_context->largest_index].bitmap);
    return ImageFrameDescriptor { m_context->images[m_context->largest_index].bitmap, 0 };
}

}
