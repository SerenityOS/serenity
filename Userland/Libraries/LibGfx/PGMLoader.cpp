/*
 * Copyright (c) 2020, Hüseyin ASLITÜRK <asliturk@hotmail.com>
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

#include "PGMLoader.h"
#include "PortableImageLoaderCommon.h"
#include "Streamer.h"
#include <AK/Endian.h>
#include <AK/LexicalPath.h>
#include <AK/MappedFile.h>
#include <AK/StringBuilder.h>
#include <string.h>

namespace Gfx {

struct PGMLoadingContext {
    enum Type {
        Unknown,
        ASCII,
        RAWBITS
    };

    enum State {
        NotDecoded = 0,
        Error,
        MagicNumber,
        Width,
        Height,
        Maxval,
        Bitmap,
        Decoded
    };

    static constexpr auto ascii_magic_number = '2';
    static constexpr auto binary_magic_number = '5';
    static constexpr auto image_type = "PGM";

    Type type { Type::Unknown };
    State state { State::NotDecoded };
    const u8* data { nullptr };
    size_t data_size { 0 };
    u16 width { 0 };
    u16 height { 0 };
    u16 max_val { 0 };
    RefPtr<Gfx::Bitmap> bitmap;
};

static void set_adjusted_pixels(PGMLoadingContext& context, const AK::Vector<Gfx::Color>& color_data)
{
    size_t index = 0;
    for (size_t y = 0; y < context.height; ++y) {
        for (size_t x = 0; x < context.width; ++x) {
            Color color = color_data.at(index);
            if (context.max_val < 255) {
                color = adjust_color(context.max_val, color);
            }
            context.bitmap->set_pixel(x, y, color);
            ++index;
        }
    }
}

static bool read_image_data(PGMLoadingContext& context, Streamer& streamer)
{
    Vector<Gfx::Color> color_data;

    if (context.type == PGMLoadingContext::ASCII) {
        u16 value;

        while (true) {
            if (!read_number(streamer, &value))
                break;

            if (!read_white_space(context, streamer))
                break;

            color_data.append({ (u8)value, (u8)value, (u8)value });
        }
    } else if (context.type == PGMLoadingContext::RAWBITS) {
        u8 pixel;
        while (streamer.read(pixel)) {
            color_data.append({ pixel, pixel, pixel });
        }
    }

    size_t context_size = (u32)context.width * (u32)context.height;
    if (context_size != color_data.size()) {
        dbgln("Not enough color data in image.");
        return false;
    }

    if (!create_bitmap(context))
        return false;

    set_adjusted_pixels(context, color_data);

    context.state = PGMLoadingContext::State::Bitmap;
    return true;
}

RefPtr<Gfx::Bitmap> load_pgm(const StringView& path)
{
    return load<PGMLoadingContext>(path);
}

RefPtr<Gfx::Bitmap> load_pgm_from_memory(const u8* data, size_t length)
{
    auto bitmap = load_impl<PGMLoadingContext>(data, length);
    if (bitmap)
        bitmap->set_mmap_name(String::formatted("Gfx::Bitmap [{}] - Decoded PGM: <memory>", bitmap->size()));
    return bitmap;
}

PGMImageDecoderPlugin::PGMImageDecoderPlugin(const u8* data, size_t size)
{
    m_context = make<PGMLoadingContext>();
    m_context->data = data;
    m_context->data_size = size;
}

PGMImageDecoderPlugin::~PGMImageDecoderPlugin()
{
}

IntSize PGMImageDecoderPlugin::size()
{
    if (m_context->state == PGMLoadingContext::State::Error)
        return {};

    if (m_context->state < PGMLoadingContext::State::Decoded) {
        bool success = decode(*m_context);
        if (!success)
            return {};
    }

    return { m_context->width, m_context->height };
}

RefPtr<Gfx::Bitmap> PGMImageDecoderPlugin::bitmap()
{
    if (m_context->state == PGMLoadingContext::State::Error)
        return nullptr;

    if (m_context->state < PGMLoadingContext::State::Decoded) {
        bool success = decode(*m_context);
        if (!success)
            return nullptr;
    }

    VERIFY(m_context->bitmap);
    return m_context->bitmap;
}

void PGMImageDecoderPlugin::set_volatile()
{
    if (m_context->bitmap)
        m_context->bitmap->set_volatile();
}

bool PGMImageDecoderPlugin::set_nonvolatile()
{
    if (!m_context->bitmap)
        return false;

    return m_context->bitmap->set_nonvolatile();
}

bool PGMImageDecoderPlugin::sniff()
{
    if (m_context->data_size < 2)
        return false;

    if (m_context->data[0] == 'P' && m_context->data[1] == '2')
        return true;

    if (m_context->data[0] == 'P' && m_context->data[1] == '5')
        return true;

    return false;
}

bool PGMImageDecoderPlugin::is_animated()
{
    return false;
}

size_t PGMImageDecoderPlugin::loop_count()
{
    return 0;
}

size_t PGMImageDecoderPlugin::frame_count()
{
    return 1;
}

ImageFrameDescriptor PGMImageDecoderPlugin::frame(size_t i)
{
    if (i > 0) {
        return { bitmap(), 0 };
    }

    return {};
}

}
