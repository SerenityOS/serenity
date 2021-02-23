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

#include "PBMLoader.h"
#include "PortableImageLoaderCommon.h"
#include "Streamer.h"
#include <AK/Endian.h>
#include <AK/LexicalPath.h>
#include <AK/MappedFile.h>
#include <AK/StringBuilder.h>
#include <string.h>

namespace Gfx {

struct PBMLoadingContext {
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
        Bitmap,
        Decoded
    };

    static constexpr auto ascii_magic_number = '1';
    static constexpr auto binary_magic_number = '4';
    static constexpr auto image_type = "PBM";

    Type type { Type::Unknown };
    State state { State::NotDecoded };
    const u8* data { nullptr };
    size_t data_size { 0 };
    size_t width { 0 };
    size_t height { 0 };
    RefPtr<Gfx::Bitmap> bitmap;
};

static bool read_image_data(PBMLoadingContext& context, Streamer& streamer)
{
    u8 byte;
    Vector<Gfx::Color> color_data;

    if (context.type == PBMLoadingContext::ASCII) {
        while (streamer.read(byte)) {
            if (byte == '0') {
                color_data.append(Color::White);
            } else if (byte == '1') {
                color_data.append(Color::Black);
            }
        }
    } else if (context.type == PBMLoadingContext::RAWBITS) {
        size_t color_index = 0;

        while (streamer.read(byte)) {
            for (int i = 0; i < 8; i++) {
                int val = byte & 0x80;

                if (val == 0) {
                    color_data.append(Color::White);
                } else {
                    color_data.append(Color::Black);
                }

                byte = byte << 1;
                color_index++;

                if (color_index % context.width == 0) {
                    break;
                }
            }
        }
    }

    size_t context_size = (u32)context.width * (u32)context.height;
    if (context_size != color_data.size()) {
        dbgln("Not enough color data in image.");
        return false;
    }

    if (!create_bitmap(context)) {
        return false;
    }

    set_pixels(context, color_data);

    context.state = PBMLoadingContext::State::Bitmap;
    return true;
}

RefPtr<Gfx::Bitmap> load_pbm(const StringView& path)
{
    return load<PBMLoadingContext>(path);
}

RefPtr<Gfx::Bitmap> load_pbm_from_memory(const u8* data, size_t length)
{
    auto bitmap = load_impl<PBMLoadingContext>(data, length);
    if (bitmap)
        bitmap->set_mmap_name(String::formatted("Gfx::Bitmap [{}] - Decoded PBM: <memory>", bitmap->size()));
    return bitmap;
}

PBMImageDecoderPlugin::PBMImageDecoderPlugin(const u8* data, size_t size)
{
    m_context = make<PBMLoadingContext>();
    m_context->data = data;
    m_context->data_size = size;
}

PBMImageDecoderPlugin::~PBMImageDecoderPlugin()
{
}

IntSize PBMImageDecoderPlugin::size()
{
    if (m_context->state == PBMLoadingContext::State::Error)
        return {};

    if (m_context->state < PBMLoadingContext::State::Decoded) {
        bool success = decode(*m_context);
        if (!success)
            return {};
    }

    return { m_context->width, m_context->height };
}

RefPtr<Gfx::Bitmap> PBMImageDecoderPlugin::bitmap()
{
    if (m_context->state == PBMLoadingContext::State::Error)
        return nullptr;

    if (m_context->state < PBMLoadingContext::State::Decoded) {
        bool success = decode(*m_context);
        if (!success)
            return nullptr;
    }

    VERIFY(m_context->bitmap);
    return m_context->bitmap;
}

void PBMImageDecoderPlugin::set_volatile()
{
    if (m_context->bitmap)
        m_context->bitmap->set_volatile();
}

bool PBMImageDecoderPlugin::set_nonvolatile()
{
    if (!m_context->bitmap)
        return false;

    return m_context->bitmap->set_nonvolatile();
}

bool PBMImageDecoderPlugin::sniff()
{
    if (m_context->data_size < 2)
        return false;

    if (m_context->data[0] == 'P' && m_context->data[1] == '1')
        return true;

    if (m_context->data[0] == 'P' && m_context->data[1] == '4')
        return true;

    return false;
}

bool PBMImageDecoderPlugin::is_animated()
{
    return false;
}

size_t PBMImageDecoderPlugin::loop_count()
{
    return 0;
}

size_t PBMImageDecoderPlugin::frame_count()
{
    return 1;
}

ImageFrameDescriptor PBMImageDecoderPlugin::frame(size_t i)
{
    if (i > 0) {
        return { bitmap(), 0 };
    }

    return {};
}

}
