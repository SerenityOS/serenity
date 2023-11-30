/*
 * Copyright (c) 2020, Paul Roukema <roukemap@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibMedia/ImageFormats/ImageDecoder.h>

namespace Media {

struct ICOLoadingContext;

class ICOImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    static bool sniff(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);

    virtual ~ICOImageDecoderPlugin() override;

    virtual Gfx::IntSize size() override;

    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index, Optional<Gfx::IntSize> ideal_size = {}) override;

private:
    ICOImageDecoderPlugin(u8 const*, size_t);
    static ErrorOr<void> load_ico_bitmap(ICOLoadingContext& context, Optional<size_t> index);

    OwnPtr<ICOLoadingContext> m_context;
};

}
