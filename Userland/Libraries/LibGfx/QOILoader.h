/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibGfx/Forward.h>
#include <LibGfx/ImageDecoder.h>

namespace Gfx {

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
    u8 const* data { nullptr };
    size_t data_size { 0 };
    QOIHeader header {};
    RefPtr<Bitmap> bitmap;
    Optional<Error> error;
};

class QOIImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    virtual ~QOIImageDecoderPlugin() override = default;
    QOIImageDecoderPlugin(u8 const*, size_t);

    virtual IntSize size() override;
    virtual void set_volatile() override;
    [[nodiscard]] virtual bool set_nonvolatile(bool& was_purged) override;
    virtual bool sniff() override;
    virtual bool is_animated() override { return false; }
    virtual size_t loop_count() override { return 0; }
    virtual size_t frame_count() override { return 1; }
    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index) override;

private:
    ErrorOr<void> decode_header_and_update_context(InputMemoryStream&);
    ErrorOr<void> decode_image_and_update_context(InputMemoryStream&);

    OwnPtr<QOILoadingContext> m_context;
};

}
