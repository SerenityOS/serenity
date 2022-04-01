/*
 * Copyright (c) 2020, Hüseyin ASLITÜRK <asliturk@hotmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/PortableImageLoaderCommon.h>

namespace Gfx {

template<class TFormatDetails>
struct PortableImageMapLoadingContext {
    using FormatDetails = TFormatDetails;

    enum class Type {
        Unknown,
        ASCII,
        RAWBITS
    };

    enum class State {
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
    u8 const* data { nullptr };
    size_t data_size { 0 };
    size_t width { 0 };
    size_t height { 0 };
    FormatDetails format_details {};
    RefPtr<Gfx::Bitmap> bitmap;
};

template<typename TContext>
class PortableImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    PortableImageDecoderPlugin(u8 const*, size_t);
    virtual ~PortableImageDecoderPlugin() override = default;

    virtual IntSize size() override;

    virtual void set_volatile() override;
    [[nodiscard]] virtual bool set_nonvolatile(bool& was_purged) override;

    virtual bool sniff() override;

    virtual bool is_animated() override;
    virtual size_t loop_count() override;
    virtual size_t frame_count() override;
    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index) override;

private:
    OwnPtr<TContext> m_context;
};

template<typename TContext>
PortableImageDecoderPlugin<TContext>::PortableImageDecoderPlugin(u8 const* data, size_t size)
{
    m_context = make<TContext>();
    m_context->data = data;
    m_context->data_size = size;
}

template<typename TContext>
IntSize PortableImageDecoderPlugin<TContext>::size()
{
    if (m_context->state == TContext::State::Error)
        return {};

    if (m_context->state < TContext::State::Decoded) {
        bool success = decode(*m_context);
        if (!success)
            return {};
    }

    return { m_context->width, m_context->height };
}

template<typename TContext>
void PortableImageDecoderPlugin<TContext>::set_volatile()
{
    if (m_context->bitmap)
        m_context->bitmap->set_volatile();
}

template<typename TContext>
bool PortableImageDecoderPlugin<TContext>::set_nonvolatile(bool& was_purged)
{
    if (!m_context->bitmap)
        return false;

    return m_context->bitmap->set_nonvolatile(was_purged);
}

template<typename TContext>
bool PortableImageDecoderPlugin<TContext>::sniff()
{
    using Context = TContext;
    if (m_context->data_size < 2)
        return false;

    if (m_context->data[0] == 'P' && m_context->data[1] == Context::FormatDetails::ascii_magic_number)
        return true;

    if (m_context->data[0] == 'P' && m_context->data[1] == Context::FormatDetails::binary_magic_number)
        return true;

    return false;
}

template<typename TContext>
bool PortableImageDecoderPlugin<TContext>::is_animated()
{
    return false;
}

template<typename TContext>
size_t PortableImageDecoderPlugin<TContext>::loop_count()
{
    return 0;
}

template<typename TContext>
size_t PortableImageDecoderPlugin<TContext>::frame_count()
{
    return 1;
}

template<typename TContext>
ErrorOr<ImageFrameDescriptor> PortableImageDecoderPlugin<TContext>::frame(size_t index)
{
    if (index > 0)
        return Error::from_string_literal("PortableImageDecoderPlugin: Invalid frame index"sv);

    if (m_context->state == TContext::State::Error)
        return Error::from_string_literal("PortableImageDecoderPlugin: Decoding failed"sv);

    if (m_context->state < TContext::State::Decoded) {
        bool success = decode(*m_context);
        if (!success)
            return Error::from_string_literal("PortableImageDecoderPlugin: Decoding failed"sv);
    }

    VERIFY(m_context->bitmap);
    return ImageFrameDescriptor { m_context->bitmap, 0 };
}
}
