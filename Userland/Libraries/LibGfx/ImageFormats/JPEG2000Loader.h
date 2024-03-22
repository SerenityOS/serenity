/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/ImageFormats/ImageDecoder.h>

namespace Gfx {

struct JPEG2000LoadingContext;

class JPEG2000ImageDecoderPlugin : public ImageDecoderPlugin {
public:
    static bool sniff(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);

    virtual ~JPEG2000ImageDecoderPlugin() override = default;

    virtual IntSize size() override;

    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index, Optional<IntSize> ideal_size = {}) override;

    virtual ErrorOr<Optional<ReadonlyBytes>> icc_data() override;

private:
    JPEG2000ImageDecoderPlugin();

    OwnPtr<JPEG2000LoadingContext> m_context;
};

}
