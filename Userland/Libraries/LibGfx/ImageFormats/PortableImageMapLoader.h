/*
 * Copyright (c) 2020, Hüseyin ASLITÜRK <asliturk@hotmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BufferedStream.h>
#include <AK/MemoryStream.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/PortableImageLoaderCommon.h>

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
        HeaderDecoded,
        BitmapDecoded,
    };

    Type type { Type::Unknown };
    State state { State::NotDecoded };

    size_t width { 0 };
    size_t height { 0 };
    FormatDetails format_details {};
    RefPtr<Gfx::Bitmap> bitmap;

    NonnullOwnPtr<SeekableStream> stream;

    PortableImageMapLoadingContext(NonnullOwnPtr<SeekableStream> stream)
        : stream(move(stream))
    {
    }
};

template<typename TContext>
class PortableImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    static bool sniff(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);

    virtual ~PortableImageDecoderPlugin() override = default;

    virtual IntSize size() override;

    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index, Optional<IntSize> ideal_size = {}) override;

    virtual NaturalFrameFormat natural_frame_format() const override;
    virtual ErrorOr<NonnullRefPtr<CMYKBitmap>> cmyk_frame() override;

private:
    PortableImageDecoderPlugin(NonnullOwnPtr<SeekableStream> stream);

    OwnPtr<TContext> m_context;
};

template<typename TContext>
PortableImageDecoderPlugin<TContext>::PortableImageDecoderPlugin(NonnullOwnPtr<SeekableStream> stream)
{
    m_context = make<TContext>(move(stream));
}

template<typename TContext>
IntSize PortableImageDecoderPlugin<TContext>::size()
{
    return { m_context->width, m_context->height };
}

template<typename TContext>
ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> PortableImageDecoderPlugin<TContext>::create(ReadonlyBytes data)
{
    auto stream = TRY(try_make<FixedMemoryStream>(data));
    auto plugin = TRY(adopt_nonnull_own_or_enomem(new (nothrow) PortableImageDecoderPlugin<TContext>(move(stream))));
    if constexpr (TContext::FormatDetails::binary_magic_number == '7')
        TRY(read_pam_header(*plugin->m_context));
    else
        TRY(read_header(*plugin->m_context));
    return plugin;
}

template<typename TContext>
bool PortableImageDecoderPlugin<TContext>::sniff(ReadonlyBytes data)
{
    using Context = TContext;
    if (data.size() < 2)
        return false;

    if constexpr (requires { Context::FormatDetails::ascii_magic_number; }) {
        if (data.data()[0] == 'P' && data.data()[1] == Context::FormatDetails::ascii_magic_number)
            return true;
    }

    if (data.data()[0] == 'P' && data.data()[1] == Context::FormatDetails::binary_magic_number)
        return true;

    return false;
}

template<typename TContext>
ErrorOr<ImageFrameDescriptor> PortableImageDecoderPlugin<TContext>::frame(size_t index, Optional<IntSize>)
{
    if (index > 0)
        return Error::from_string_literal("PortableImageDecoderPlugin: Invalid frame index");

    if (m_context->state == TContext::State::Error)
        return Error::from_string_literal("PortableImageDecoderPlugin: Decoding failed");

    if (m_context->state < TContext::State::BitmapDecoded) {
        if (decode(*m_context).is_error()) {
            m_context->state = TContext::State::Error;
            return Error::from_string_literal("PortableImageDecoderPlugin: Decoding failed");
        }
    }

    if constexpr (requires { TContext::FormatDetails::cmyk_bitmap; }) {
        if (m_context->format_details.cmyk_bitmap.has_value())
            m_context->bitmap = TRY(m_context->format_details.cmyk_bitmap.value()->to_low_quality_rgb());
    }

    VERIFY(m_context->bitmap);
    return ImageFrameDescriptor { m_context->bitmap, 0 };
}

template<typename TContext>
NaturalFrameFormat PortableImageDecoderPlugin<TContext>::natural_frame_format() const
{
    if constexpr (requires { TContext::FormatDetails::cmyk_bitmap; }) {
        if (m_context->format_details.depth == 4 && m_context->format_details.tupl_type == "CMYK"sv)
            return NaturalFrameFormat::CMYK;
    }

    return NaturalFrameFormat::RGB;
}

template<typename TContext>
ErrorOr<NonnullRefPtr<CMYKBitmap>> PortableImageDecoderPlugin<TContext>::cmyk_frame()
{
    if constexpr (requires { TContext::FormatDetails::cmyk_bitmap; }) {
        VERIFY(natural_frame_format() == NaturalFrameFormat::CMYK);
        if (m_context->state == TContext::State::Error)
            return Error::from_string_literal("PortableImageDecoderPlugin: Decoding failed");

        if (m_context->state < TContext::State::BitmapDecoded) {
            if (decode(*m_context).is_error()) {
                m_context->state = TContext::State::Error;
                return Error::from_string_literal("PortableImageDecoderPlugin: Decoding failed");
            }
        }

        return *m_context->format_details.cmyk_bitmap.value();
    }

    VERIFY_NOT_REACHED();
}

}
