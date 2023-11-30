/*
 * Copyright (c) 2023, Nicolas Ramz <nicolas.ramz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibMedia/ImageFormats/ImageDecoder.h>

namespace Media {

struct ILBMLoadingContext;

class ILBMImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    static bool sniff(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);

    virtual ~ILBMImageDecoderPlugin() override;

    virtual Gfx::IntSize size() override;

    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index, Optional<Gfx::IntSize> ideal_size = {}) override;

private:
    ILBMImageDecoderPlugin(ReadonlyBytes, NonnullOwnPtr<ILBMLoadingContext>);
    NonnullOwnPtr<ILBMLoadingContext> m_context;
};

}
