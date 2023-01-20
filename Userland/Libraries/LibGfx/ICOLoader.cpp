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
#include <LibGfx/BMPLoader.h>
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

struct ICOImageDescriptor {
    u16 width;
    u16 height;
    u16 bits_per_pixel;
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
    u8 const* data { nullptr };
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

    ICOImageDescriptor desc = { entry.width, entry.height, entry.bits_per_pixel, entry.offset, entry.size, nullptr };
    if (desc.width == 0)
        desc.width = 256;
    if (desc.height == 0)
        desc.height = 256;

    return { desc };
}

static size_t find_largest_image(ICOLoadingContext const& context)
{
    size_t max_area = 0;
    size_t index = 0;
    size_t largest_index = 0;
    u16 max_bits_per_pixel = 0;
    for (auto const& desc : context.images) {
        if (static_cast<size_t>(desc.width) * static_cast<size_t>(desc.height) >= max_area) {
            if (desc.bits_per_pixel > max_bits_per_pixel) {
                max_area = desc.width * desc.height;
                largest_index = index;
                max_bits_per_pixel = desc.bits_per_pixel;
            }
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

bool ICOImageDecoderPlugin::load_ico_bitmap(ICOLoadingContext& context, Optional<size_t> index)
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
    if (PNGImageDecoderPlugin::sniff({ context.data + desc.offset, desc.size }).release_value_but_fixme_should_propagate_errors()) {
        auto png_decoder = PNGImageDecoderPlugin::create({ context.data + desc.offset, desc.size }).release_value_but_fixme_should_propagate_errors();
        if (png_decoder->initialize()) {
            auto decoded_png_frame = png_decoder->frame(0);
            if (decoded_png_frame.is_error() || !decoded_png_frame.value().image) {
                dbgln_if(ICO_DEBUG, "load_ico_bitmap: failed to load PNG encoded image index: {}", real_index);
                return false;
            }
            desc.bitmap = decoded_png_frame.value().image;
            return true;
        }
        return false;
    } else {
        auto bmp_decoder = BMPImageDecoderPlugin::create_as_included_in_ico({}, { context.data + desc.offset, desc.size }).release_value_but_fixme_should_propagate_errors();
        // NOTE: We don't initialize a BMP decoder in the usual way, but rather
        // we just create an object and try to sniff for a frame when it's included
        // inside an ICO image.
        if (bmp_decoder->sniff_dib()) {
            auto decoded_bmp_frame = bmp_decoder->frame(0);
            if (decoded_bmp_frame.is_error() || !decoded_bmp_frame.value().image) {
                dbgln_if(ICO_DEBUG, "load_ico_bitmap: failed to load BMP encoded image index: {}", real_index);
                return false;
            }
            desc.bitmap = decoded_bmp_frame.value().image;
        } else {
            dbgln_if(ICO_DEBUG, "load_ico_bitmap: encoded image not supported at index: {}", real_index);
            return false;
        }
        return true;
    }
}

ErrorOr<bool> ICOImageDecoderPlugin::sniff(ReadonlyBytes data)
{
    InputMemoryStream stream { { data.data(), data.size() } };
    return decode_ico_header(stream).has_value();
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> ICOImageDecoderPlugin::create(ReadonlyBytes data)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) ICOImageDecoderPlugin(data.data(), data.size()));
}

ICOImageDecoderPlugin::ICOImageDecoderPlugin(u8 const* data, size_t size)
{
    m_context = make<ICOLoadingContext>();
    m_context->data = data;
    m_context->data_size = size;
}

ICOImageDecoderPlugin::~ICOImageDecoderPlugin() = default;

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

bool ICOImageDecoderPlugin::initialize()
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
        return Error::from_string_literal("ICOImageDecoderPlugin: Invalid frame index");

    if (m_context->state == ICOLoadingContext::State::Error)
        return Error::from_string_literal("ICOImageDecoderPlugin: Decoding failed");

    if (m_context->state < ICOLoadingContext::State::BitmapDecoded) {
        // NOTE: This forces the chunk decoding to happen.
        bool success = load_ico_bitmap(*m_context, {});
        if (!success) {
            m_context->state = ICOLoadingContext::State::Error;
            return Error::from_string_literal("ICOImageDecoderPlugin: Decoding failed");
        }
        m_context->state = ICOLoadingContext::State::BitmapDecoded;
    }

    VERIFY(m_context->images[m_context->largest_index].bitmap);
    return ImageFrameDescriptor { m_context->images[m_context->largest_index].bitmap, 0 };
}

}
