/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/ImageDecoder.h>

namespace Gfx {

struct BMPLoadingContext;

class BMPImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    virtual ~BMPImageDecoderPlugin() override;
    BMPImageDecoderPlugin(const u8*, size_t);

    virtual IntSize size() override;
    virtual void set_volatile() override;
    [[nodiscard]] virtual bool set_nonvolatile(bool& was_purged) override;
    virtual bool sniff() override;
    virtual bool is_animated() override;
    virtual size_t loop_count() override;
    virtual size_t frame_count() override;
    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index) override;

private:
    OwnPtr<BMPLoadingContext> m_context;
};

}
