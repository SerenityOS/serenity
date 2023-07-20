/*
 * Copyright (c) 2022, Tom Needham <06needhamt@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/ImageFormats/ImageDecoder.h>

namespace Gfx {

struct TGALoadingContext;

class TGAImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    static ErrorOr<bool> validate_before_create(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes, RequestType);

    virtual ~TGAImageDecoderPlugin() override;
    TGAImageDecoderPlugin(u8 const*, size_t, RequestType);

    virtual IntSize size() override;

    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index, Optional<IntSize> ideal_size = {}) override;

private:
    ErrorOr<void> decode_tga_header();
    OwnPtr<TGALoadingContext> m_context;
};

}
