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
#include <AK/Endian.h>
#include <AK/LexicalPath.h>
#include <AK/MappedFile.h>
#include <AK/StringBuilder.h>
#include <string.h>

namespace Gfx {

struct PGMLoadingContext {
    enum Type {
        Unknown,
        P2_ASCII,
        P5_RAWBITS
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

    Type type { Type::Unknown };
    State state { State::NotDecoded };
    const u8* data { nullptr };
    size_t data_size { 0 };
    u16 width { 0 };
    u16 height { 0 };
    u16 max_val { 0 };
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

ALWAYS_INLINE static Color adjust_color(u16 max_val, Color& color)
{
    color.set_red((color.red() * 255) / max_val);
    color.set_green((color.green() * 255) / max_val);
    color.set_blue((color.blue() * 255) / max_val);

    return color;
}

static bool read_number(Streamer& streamer, u16* value)
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

    auto opt_value = sb.to_string().to_uint();
    if (!opt_value.has_value()) {
        return false;
    }

    *value = (u16)opt_value.value();
    return true;
}

static bool read_comment(PGMLoadingContext& context, Streamer& streamer)
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

static bool read_magic_number(PGMLoadingContext& context, Streamer& streamer)
{
    if (context.state >= PGMLoadingContext::MagicNumber)
        return true;

    if (!context.data || context.data_size < 2) {
        context.state = PGMLoadingContext::State::Error;
        dbg() << "There is no enough data.";
        return false;
    }

    u8 magic_number[2];
    if (!streamer.read_bytes(magic_number, 2)) {
        context.state = PGMLoadingContext::State::Error;
        dbg() << "We can't read magic number.";
        return false;
    }

    if (magic_number[0] == 'P' && magic_number[1] == '2') {
        context.type = PGMLoadingContext::P2_ASCII;
        context.state = PGMLoadingContext::MagicNumber;
        return true;
    }

    if (magic_number[0] == 'P' && magic_number[1] == '5') {
        context.type = PGMLoadingContext::P5_RAWBITS;
        context.state = PGMLoadingContext::MagicNumber;
        return true;
    }

    context.state = PGMLoadingContext::State::Error;
    dbg() << "Magic number is not valid:" << (char)magic_number[0] << (char)magic_number[1];
    return false;
}

static bool read_white_space(PGMLoadingContext& context, Streamer& streamer)
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

static bool read_width(PGMLoadingContext& context, Streamer& streamer)
{
    bool result = read_number(streamer, &context.width);
    if (!result || context.width == 0) {
        return false;
    }

    context.state = PGMLoadingContext::Width;
    return true;
}

static bool read_height(PGMLoadingContext& context, Streamer& streamer)
{
    bool result = read_number(streamer, &context.height);
    if (!result || context.height == 0) {
        return false;
    }

    context.state = PGMLoadingContext::Height;
    return true;
}

static bool read_max_val(PGMLoadingContext& context, Streamer& streamer)
{
    bool result = read_number(streamer, &context.max_val);
    if (!result || context.max_val == 0) {
        return false;
    }

    if (context.max_val > 255) {
        dbg() << "We can't pars 2 byte color.";
        context.state = PGMLoadingContext::Error;
        return false;
    }

    context.state = PGMLoadingContext::Maxval;
    return true;
}

static bool read_image_data(PGMLoadingContext& context, Streamer& streamer)
{
    Vector<Gfx::Color> color_data;

    if (context.type == PGMLoadingContext::P2_ASCII) {
        u16 value;

        while (true) {
            if (!read_number(streamer, &value))
                break;

            if (!read_white_space(context, streamer))
                break;

            color_data.append({ (u8)value, (u8)value, (u8)value });
        }
    } else if (context.type == PGMLoadingContext::P5_RAWBITS) {
        u8 pixel;
        while (streamer.read(pixel)) {
            color_data.append({ pixel, pixel, pixel });
        }
    }

    context.bitmap = Bitmap::create_purgeable(BitmapFormat::RGB32, { context.width, context.height });

    size_t index = 0;
    for (int y = 0; y < context.height; ++y) {
        for (int x = 0; x < context.width; ++x) {
            Color color = color_data.at(index);
            if (context.max_val < 255)
                color = adjust_color(context.max_val, color);
            context.bitmap->set_pixel(x, y, color);
            index++;
        }
    }

    context.state = PGMLoadingContext::State::Bitmap;
    return true;
}

static bool decode_pgm(PGMLoadingContext& context)
{
    if (context.state >= PGMLoadingContext::State::Decoded)
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

    if (!read_max_val(context, streamer))
        return false;

    if (!read_white_space(context, streamer))
        return false;

    if (!read_image_data(context, streamer))
        return false;

    context.state = PGMLoadingContext::State::Decoded;
    return true;
}

static RefPtr<Gfx::Bitmap> load_pgm_impl(const u8* data, size_t data_size)
{
    PGMLoadingContext context;
    context.data = data;
    context.data_size = data_size;

    if (!decode_pgm(context))
        return nullptr;

    return context.bitmap;
}

RefPtr<Gfx::Bitmap> load_pgm(const StringView& path)
{
    MappedFile mapped_file(path);
    if (!mapped_file.is_valid()) {
        return nullptr;
    }

    auto bitmap = load_pgm_impl((const u8*)mapped_file.data(), mapped_file.size());
    if (bitmap)
        bitmap->set_mmap_name(String::format("Gfx::Bitmap [%dx%d] - Decoded PGM: %s", bitmap->width(), bitmap->height(), LexicalPath::canonicalized_path(path).characters()));
    return bitmap;
}

RefPtr<Gfx::Bitmap> load_pgm_from_memory(const u8* data, size_t length)
{
    auto bitmap = load_pgm_impl(data, length);
    if (bitmap)
        bitmap->set_mmap_name(String::format("Gfx::Bitmap [%dx%d] - Decoded PGM: <memory>", bitmap->width(), bitmap->height()));
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
        bool success = decode_pgm(*m_context);
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
        bool success = decode_pgm(*m_context);
        if (!success)
            return nullptr;
    }

    ASSERT(m_context->bitmap);
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
