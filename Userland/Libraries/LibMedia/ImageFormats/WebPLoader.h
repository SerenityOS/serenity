/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibMedia/ImageFormats/ImageDecoder.h>

namespace Media {

struct WebPLoadingContext;

class WebPImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    static bool sniff(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);

    virtual ~WebPImageDecoderPlugin() override;

    virtual Gfx::IntSize size() override;

    virtual bool is_animated() override;
    virtual size_t loop_count() override;
    virtual size_t frame_count() override;
    virtual size_t first_animated_frame_index() override;
    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index, Optional<Gfx::IntSize> ideal_size = {}) override;
    virtual ErrorOr<Optional<ReadonlyBytes>> icc_data() override;

private:
    bool set_error(ErrorOr<void> const&);

    WebPImageDecoderPlugin(ReadonlyBytes, OwnPtr<WebPLoadingContext>);

    OwnPtr<WebPLoadingContext> m_context;
};

}
