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
    static bool validate_before_create(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);

    virtual ~TGAImageDecoderPlugin() override;

    virtual IntSize size() override;

    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index, Optional<IntSize> ideal_size = {}) override;

private:
    TGAImageDecoderPlugin(NonnullOwnPtr<TGALoadingContext>);

    ErrorOr<void> decode_tga_header();
    NonnullOwnPtr<TGALoadingContext> m_context;
};

}
