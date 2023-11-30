/*
 * Copyright (c) 2022, Tom Needham <06needhamt@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibMedia/ImageFormats/ImageDecoder.h>

namespace Media {

struct TGALoadingContext;

class TGAImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    static ErrorOr<bool> validate_before_create(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);

    virtual ~TGAImageDecoderPlugin() override;

    virtual Gfx::IntSize size() override;

    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index, Optional<Gfx::IntSize> ideal_size = {}) override;

private:
    TGAImageDecoderPlugin(NonnullOwnPtr<TGALoadingContext>);

    ErrorOr<void> decode_tga_header();
    NonnullOwnPtr<TGALoadingContext> m_context;
};

}
