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

#include "PPMLoader.h"
#include "PortableImageLoaderCommon.h"
#include "Streamer.h"
#include <AK/Endian.h>
#include <AK/LexicalPath.h>
#include <AK/MappedFile.h>
#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <string.h>

namespace Gfx {

struct PPMLoadingContext {
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

    static constexpr auto ascii_magic_number = '3';
    static constexpr auto binary_magic_number = '6';
    static constexpr auto image_type = "PPM";

    Type type { Type::Unknown };
    State state { State::NotDecoded };
    const u8* data { nullptr };
    size_t data_size { 0 };
    u16 width { 0 };
    u16 height { 0 };
    u16 max_val { 0 };
    RefPtr<Gfx::Bitmap> bitmap;
};

static bool read_image_data(PPMLoadingContext& context, Streamer& streamer)
{
    Vector<Gfx::Color> color_data;
    color_data.ensure_capacity(context.width * context.height);

    if (context.type == PPMLoadingContext::ASCII) {
        u16 red;
        u16 green;
        u16 blue;

        while (true) {
            if (!read_number(streamer, &red))
                break;

            if (!read_white_space(context, streamer))
                break;

            if (!read_number(streamer, &green))
                break;

            if (!read_white_space(context, streamer))
                break;

            if (!read_number(streamer, &blue))
                break;

            if (!read_white_space(context, streamer))
                break;

            Color color { (u8)red, (u8)green, (u8)blue };
            if (context.max_val < 255)
                color = adjust_color(context.max_val, color);
            color_data.append(color);
        }
    } else if (context.type == PPMLoadingContext::RAWBITS) {
        u8 pixel[3];
        while (streamer.read_bytes(pixel, 3)) {
            color_data.append({ pixel[0], pixel[1], pixel[2] });
        }
    }

    if (context.width * context.height != color_data.size())
        return false;

    if (!create_bitmap(context)) {
        return false;
    }

    set_pixels(context, color_data);

    context.state = PPMLoadingContext::State::Bitmap;
    return true;
}

RefPtr<Gfx::Bitmap> load_ppm(const StringView& path)
{
    return load<PPMLoadingContext>(path);
}

RefPtr<Gfx::Bitmap> load_ppm_from_memory(const u8* data, size_t length)
{
    auto bitmap = load_impl<PPMLoadingContext>(data, length);
    if (bitmap)
        bitmap->set_mmap_name(String::formatted("Gfx::Bitmap [{}] - Decoded PPM: <memory>", bitmap->size()));
    return bitmap;
}

PPMImageDecoderPlugin::PPMImageDecoderPlugin(const u8* data, size_t size)
{
    m_context = make<PPMLoadingContext>();
    m_context->data = data;
    m_context->data_size = size;
}

PPMImageDecoderPlugin::~PPMImageDecoderPlugin()
{
}

IntSize PPMImageDecoderPlugin::size()
{
    if (m_context->state == PPMLoadingContext::State::Error)
        return {};

    if (m_context->state < PPMLoadingContext::State::Decoded) {
        bool success = decode(*m_context);
        if (!success)
            return {};
    }

    return { m_context->width, m_context->height };
}

RefPtr<Gfx::Bitmap> PPMImageDecoderPlugin::bitmap()
{
    if (m_context->state == PPMLoadingContext::State::Error)
        return nullptr;

    if (m_context->state < PPMLoadingContext::State::Decoded) {
        bool success = decode(*m_context);
        if (!success)
            return nullptr;
    }

    VERIFY(m_context->bitmap);
    return m_context->bitmap;
}

void PPMImageDecoderPlugin::set_volatile()
{
    if (m_context->bitmap)
        m_context->bitmap->set_volatile();
}

bool PPMImageDecoderPlugin::set_nonvolatile()
{
    if (!m_context->bitmap)
        return false;

    return m_context->bitmap->set_nonvolatile();
}

bool PPMImageDecoderPlugin::sniff()
{
    if (m_context->data_size < 2)
        return false;

    if (m_context->data[0] == 'P' && m_context->data[1] == '3')
        return true;

    if (m_context->data[0] == 'P' && m_context->data[1] == '6')
        return true;

    return false;
}

bool PPMImageDecoderPlugin::is_animated()
{
    return false;
}

size_t PPMImageDecoderPlugin::loop_count()
{
    return 0;
}

size_t PPMImageDecoderPlugin::frame_count()
{
    return 1;
}

ImageFrameDescriptor PPMImageDecoderPlugin::frame(size_t i)
{
    if (i > 0) {
        return { bitmap(), 0 };
    }

    return {};
}

}
