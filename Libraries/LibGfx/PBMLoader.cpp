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
#include <AK/Endian.h>
#include <AK/LexicalPath.h>
#include <AK/MappedFile.h>
#include <AK/StringBuilder.h>
#include <string.h>

namespace Gfx {

struct PBMLoadingContext {
    enum Type {
        Unknown,
        P1_ASCII,
        P4_RAWBITS
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

    Type type { Type::Unknown };
    State state { State::NotDecoded };
    const u8* data { nullptr };
    size_t data_size { 0 };
    int width { -1 };
    int height { -1 };
    RefPtr<Gfx::Bitmap> bitmap;
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

    bool at_end() const { return !m_size_remaining; }

    void step_back()
    {
        m_data_ptr -= 1;
        m_size_remaining += 1;
    }

private:
    const u8* m_data_ptr { nullptr };
    size_t m_size_remaining { 0 };
};

static int read_number(Streamer& streamer)
{
    u8 byte;
    StringBuilder sb;

    while (streamer.read(byte)) {
        if (byte == ' ' || byte == '\t' || byte == '\n' || byte == '\r') {
            streamer.step_back();
            break;
        }

        sb.append(byte);
    }

    return sb.to_string().to_uint().value_or(0);
}

static bool read_comment(PBMLoadingContext& context, Streamer& streamer)
{
    (void)context;

    bool exist = false;
    u8 byte;

    while (streamer.read(byte)) {
        switch (byte) {
        case '#': {
            exist = true;
            break;
        }
        case '\t':
        case '\n': {
            return exist;
        }
        default:
            break;
        }
    }

    return exist;
}

static bool read_magic_number(PBMLoadingContext& context, Streamer& streamer)
{
    if (context.state >= PBMLoadingContext::MagicNumber)
        return true;

    if (!context.data || context.data_size < 2) {
        context.state = PBMLoadingContext::State::Error;
        dbg() << "There is no enough data.";
        return false;
    }

    u8 magic_number[2];
    if (!streamer.read_bytes(magic_number, 2)) {
        context.state = PBMLoadingContext::State::Error;
        dbg() << "We can't read magic number.";
        return false;
    }

    if (magic_number[0] == 'P' && magic_number[1] == '1') {
        context.type = PBMLoadingContext::P1_ASCII;
        context.state = PBMLoadingContext::MagicNumber;
        return true;
    }

    if (magic_number[0] == 'P' && magic_number[1] == '4') {
        context.type = PBMLoadingContext::P4_RAWBITS;
        context.state = PBMLoadingContext::MagicNumber;
        return true;
    }

    context.state = PBMLoadingContext::State::Error;
    dbg() << "Magic number is not valid." << (char)magic_number[0] << (char)magic_number[1];
    return false;
}

static bool read_white_space(PBMLoadingContext& context, Streamer& streamer)
{
    bool exist = false;
    u8 byte;

    while (streamer.read(byte)) {
        switch (byte) {
        case ' ':
        case '\t':
        case '\n':
        case '\r': {
            exist = true;
            break;
        }
        case '#': {
            streamer.step_back();
            read_comment(context, streamer);
            break;
        }
        default: {
            streamer.step_back();
            return exist;
        }
        }
    }

    return exist;
}

static bool read_width(PBMLoadingContext& context, Streamer& streamer)
{
    context.width = read_number(streamer);
    if (context.width == 0) {
        return false;
    }

    context.state = PBMLoadingContext::Width;
    return true;
}

static bool read_height(PBMLoadingContext& context, Streamer& streamer)
{
    context.height = read_number(streamer);
    if (context.height == 0) {
        return false;
    }

    context.state = PBMLoadingContext::Height;
    return true;
}

static bool read_image_data(PBMLoadingContext& context, Streamer& streamer)
{
    u8 byte;
    Vector<Gfx::Color> color_data;

    if (context.type == PBMLoadingContext::P1_ASCII) {
        while (streamer.read(byte)) {
            if (byte == '0') {
                color_data.append(Color::White);
            } else if (byte == '1') {
                color_data.append(Color::Black);
            }
        }
    } else if (context.type == PBMLoadingContext::P4_RAWBITS) {
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

    context.bitmap = Bitmap::create_purgeable(BitmapFormat::RGB32, { context.width, context.height });

    size_t index = 0;
    for (int y = 0; y < context.height; ++y) {
        for (int x = 0; x < context.width; ++x) {
            context.bitmap->set_pixel(x, y, color_data.at(index));
            index++;
        }
    }

    context.state = PBMLoadingContext::State::Bitmap;
    return true;
}

static bool decode_pbm(PBMLoadingContext& context)
{
    if (context.state >= PBMLoadingContext::State::Decoded)
        return true;

    Streamer streamer(context.data, context.data_size);

    if (!read_magic_number(context, streamer))
        return false;
    if (!read_white_space(context, streamer))
        return false;

    if (!read_width(context, streamer))
        return false;

    if (!read_white_space(context, streamer))
        return false;

    if (!read_height(context, streamer))
        return false;

    if (!read_white_space(context, streamer))
        return false;

    if (!read_image_data(context, streamer))
        return false;

    context.state = PBMLoadingContext::State::Decoded;
    return true;
}

static RefPtr<Gfx::Bitmap> load_pbm_impl(const u8* data, size_t data_size)
{
    PBMLoadingContext context;
    context.data = data;
    context.data_size = data_size;

    if (!decode_pbm(context))
        return nullptr;

    return context.bitmap;
}

RefPtr<Gfx::Bitmap> load_pbm(const StringView& path)
{
    MappedFile mapped_file(path);
    if (!mapped_file.is_valid()) {
        return nullptr;
    }

    auto bitmap = load_pbm_impl((const u8*)mapped_file.data(), mapped_file.size());
    if (bitmap)
        bitmap->set_mmap_name(String::format("Gfx::Bitmap [%dx%d] - Decoded PBM: %s", bitmap->width(), bitmap->height(), LexicalPath::canonicalized_path(path).characters()));
    return bitmap;
}

RefPtr<Gfx::Bitmap> load_pbm_from_memory(const u8* data, size_t length)
{
    auto bitmap = load_pbm_impl(data, length);
    if (bitmap)
        bitmap->set_mmap_name(String::format("Gfx::Bitmap [%dx%d] - Decoded PBM: <memory>", bitmap->width(), bitmap->height()));
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
        bool success = decode_pbm(*m_context);
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
        bool success = decode_pbm(*m_context);
        if (!success)
            return nullptr;
    }

    ASSERT(m_context->bitmap);
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
