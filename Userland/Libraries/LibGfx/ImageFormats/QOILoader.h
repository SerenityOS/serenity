/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibGfx/Forward.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>

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
    OwnPtr<Stream> stream {};
    QOIHeader header {};
    RefPtr<Bitmap> bitmap;
};

class QOIImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    static bool sniff(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);

    virtual ~QOIImageDecoderPlugin() override = default;

    virtual IntSize size() override;
    virtual void set_volatile() override;
    [[nodiscard]] virtual bool set_nonvolatile(bool& was_purged) override;
    virtual ErrorOr<void> initialize() override;
    virtual bool is_animated() override { return false; }
    virtual size_t loop_count() override { return 0; }
    virtual size_t frame_count() override { return 1; }
    virtual size_t first_animated_frame_index() override { return 0; }
    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index) override;
    virtual ErrorOr<Optional<ReadonlyBytes>> icc_data() override;

private:
    ErrorOr<void> decode_header_and_update_context(Stream&);
    ErrorOr<void> decode_image_and_update_context(Stream&);

    QOIImageDecoderPlugin(NonnullOwnPtr<Stream>);

    OwnPtr<QOILoadingContext> m_context;
};

}

template<>
struct AK::Traits<Gfx::QOIHeader> : public GenericTraits<Gfx::QOIHeader> {
    static constexpr bool is_trivially_serializable() { return true; }
};
