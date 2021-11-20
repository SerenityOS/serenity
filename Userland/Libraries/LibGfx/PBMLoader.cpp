/*
 * Copyright (c) 2020, Hüseyin ASLITÜRK <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PBMLoader.h"
#include "PortableImageLoaderCommon.h"
#include "Streamer.h"
#include <AK/Endian.h>
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

void PBMImageDecoderPlugin::set_volatile()
{
    if (m_context->bitmap)
        m_context->bitmap->set_volatile();
}

bool PBMImageDecoderPlugin::set_nonvolatile(bool& was_purged)
{
    if (!m_context->bitmap)
        return false;

    return m_context->bitmap->set_nonvolatile(was_purged);
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

ErrorOr<ImageFrameDescriptor> PBMImageDecoderPlugin::frame(size_t index)
{
    if (index > 0)
        return Error::from_string_literal("PBMImageDecoderPlugin: Invalid frame index"sv);

    if (m_context->state == PBMLoadingContext::State::Error)
        return Error::from_string_literal("PBMImageDecoderPlugin: Decoding failed"sv);

    if (m_context->state < PBMLoadingContext::State::Decoded) {
        bool success = decode(*m_context);
        if (!success)
            return Error::from_string_literal("PBMImageDecoderPlugin: Decoding failed"sv);
    }

    VERIFY(m_context->bitmap);
    return ImageFrameDescriptor { m_context->bitmap, 0 };
}

}
