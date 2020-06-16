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

#include <AK/BufferStream.h>
#include <AK/ByteBuffer.h>
#include <AK/LexicalPath.h>
#include <AK/MappedFile.h>
#include <AK/NonnullOwnPtrVector.h>
#include <LibGfx/ICOLoader.h>
#include <LibGfx/PNGLoader.h>
#include <LibM/math.h>
#include <stdio.h>
#include <string.h>

//#define ICO_DEBUG

namespace Gfx {

// FIXME: This is in little-endian order. Maybe need a NetworkOrdered<T> equivalent eventually.
struct ICONDIR {
    u16 must_be_0;
    u16 must_be_1;
    u16 image_count;
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

static Optional<size_t> decode_ico_header(BufferStream& stream)
{
    ICONDIR header;
    stream.read_raw((uint8_t*)&header, sizeof(header));
    if (stream.handle_read_failure())
        return {};

    if (header.must_be_0 != 0 || header.must_be_1 != 1)
        return {};
    return { header.image_count };
}

static Optional<ImageDescriptor> decode_ico_direntry(BufferStream& stream)
{
    ICONDIRENTRY entry;
    stream.read_raw((uint8_t*)&entry, sizeof(entry));
    if (stream.handle_read_failure())
        return {};

    ImageDescriptor desc = { entry.width, entry.height, entry.offset, entry.size, nullptr };
    if (desc.width == 0)
        desc.width = 256;
    if (desc.height == 0)
        desc.height = 256;

    return { desc };
}

static bool load_ico_directory(ICOLoadingContext& context)
{
    auto buffer = ByteBuffer::wrap(context.data, context.data_size);
    auto stream = BufferStream(buffer);
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
    return true;
}

static bool load_ico_bitmap(ICOLoadingContext& context, size_t index)
{
    if (context.state < ICOLoadingContext::State::DirectoryDecoded) {
        if (!load_ico_directory(context)) {
            context.state = ICOLoadingContext::State::Error;
            return false;
        }
        context.state = ICOLoadingContext::State::DirectoryDecoded;
    }
    if (index >= context.images.size()) {
        return false;
    }

    ImageDescriptor& desc = context.images[index];

    PNGImageDecoderPlugin png_decoder(context.data + desc.offset, desc.size);
    if (png_decoder.sniff()) {
        desc.bitmap = png_decoder.bitmap();
        if (!desc.bitmap) {
#ifdef ICO_DEBUG
            printf("load_ico_bitmap: failed to load PNG encoded image index: %lu\n", index);
#endif
            return false;
        }
        return true;
    } else {
        dbg() << "load_ico_bitmap: image index: " << index << " must be BMP encoded, not implemented\n";
        return false;
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

    return { m_context->images[0].width, m_context->images[0].height };
}

RefPtr<Gfx::Bitmap> ICOImageDecoderPlugin::bitmap()
{
    if (m_context->state == ICOLoadingContext::State::Error)
        return nullptr;

    if (m_context->state < ICOLoadingContext::State::BitmapDecoded) {
        // NOTE: This forces the chunk decoding to happen.
        bool success = load_ico_bitmap(*m_context, 0);
        if (!success) {
            m_context->state = ICOLoadingContext::State::Error;
            return nullptr;
        }
        m_context->state = ICOLoadingContext::State::BitmapDecoded;
    }

    ASSERT(m_context->images[0].bitmap);
    return m_context->images[0].bitmap;
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
    auto buffer = ByteBuffer::wrap(m_context->data, m_context->data_size);
    BufferStream stream(buffer);
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
