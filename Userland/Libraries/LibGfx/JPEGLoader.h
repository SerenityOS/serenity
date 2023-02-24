/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/ImageDecoder.h>

namespace Gfx {

struct JPEGLoadingContext;

// For the specification, see: https://www.w3.org/Graphics/JPEG/itu-t81.pdf

class JPEGImageDecoderPlugin : public ImageDecoderPlugin {
public:
    static ErrorOr<bool> sniff(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);

    virtual ~JPEGImageDecoderPlugin() override;
    virtual IntSize size() override;
    virtual void set_volatile() override;
    [[nodiscard]] virtual bool set_nonvolatile(bool& was_purged) override;
    virtual bool initialize() override;
    virtual bool is_animated() override;
    virtual size_t loop_count() override;
    virtual size_t frame_count() override;
    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index) override;
    virtual ErrorOr<Optional<ReadonlyBytes>> icc_data() override;

private:
    JPEGImageDecoderPlugin(u8 const*, size_t);

    OwnPtr<JPEGLoadingContext> m_context;
};

}
