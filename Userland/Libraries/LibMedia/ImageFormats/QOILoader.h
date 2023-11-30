/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibGfx/Forward.h>
#include <LibMedia/ImageFormats/ImageDecoder.h>

namespace Media {

// Decoder for the "Quite OK Image" format (v1.0).
// https://qoiformat.org/qoi-specification.pdf

struct [[gnu::packed]] QOIHeader {
    char magic[4];
    u32 width;
    u32 height;
    u8 channels;
    u8 colorspace;
};

struct QOILoadingContext {
    enum class State {
        NotDecoded = 0,
        HeaderDecoded,
        ImageDecoded,
        Error,
    };
    State state { State::NotDecoded };
    OwnPtr<Stream> stream {};
    QOIHeader header {};
    RefPtr<Gfx::Bitmap> bitmap;
};

class QOIImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    static bool sniff(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);

    virtual ~QOIImageDecoderPlugin() override = default;

    virtual Gfx::IntSize size() override;

    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index, Optional<Gfx::IntSize> ideal_size = {}) override;

private:
    ErrorOr<void> decode_header_and_update_context();
    ErrorOr<void> decode_image_and_update_context();

    QOIImageDecoderPlugin(NonnullOwnPtr<Stream>);

    OwnPtr<QOILoadingContext> m_context;
};

}

template<>
struct AK::Traits<Media::QOIHeader> : public DefaultTraits<Media::QOIHeader> {
    static constexpr bool is_trivially_serializable() { return true; }
};
