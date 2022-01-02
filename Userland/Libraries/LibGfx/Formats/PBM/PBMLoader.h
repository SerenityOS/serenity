/*
 * Copyright (c) 2020, Hüseyin ASLITÜRK <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/ImageDecoder.h>

namespace Gfx {

struct PBMLoadingContext;

class PBMImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    PBMImageDecoderPlugin(const u8*, size_t);
    virtual ~PBMImageDecoderPlugin() override;

    virtual IntSize size() override;

    virtual void set_volatile() override;
    [[nodiscard]] virtual bool set_nonvolatile(bool& was_purged) override;

    virtual bool sniff() override;

    virtual bool is_animated() override;
    virtual size_t loop_count() override;
    virtual size_t frame_count() override;
    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index) override;

private:
    OwnPtr<PBMLoadingContext> m_context;
};

}
