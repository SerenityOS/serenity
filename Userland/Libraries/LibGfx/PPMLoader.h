/*
 * Copyright (c) 2020, Hüseyin ASLITÜRK <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageDecoder.h>

namespace Gfx {

RefPtr<Gfx::Bitmap> load_ppm(const StringView& path);
RefPtr<Gfx::Bitmap> load_ppm_from_memory(const u8*, size_t);

struct PPMLoadingContext;

class PPMImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    PPMImageDecoderPlugin(const u8*, size_t);
    virtual ~PPMImageDecoderPlugin() override;

    virtual IntSize size() override;
    virtual RefPtr<Gfx::Bitmap> bitmap() override;

    virtual void set_volatile() override;
    [[nodiscard]] virtual bool set_nonvolatile() override;

    virtual bool sniff() override;

    virtual bool is_animated() override;
    virtual size_t loop_count() override;
    virtual size_t frame_count() override;
    virtual ImageFrameDescriptor frame(size_t i) override;

private:
    OwnPtr<PPMLoadingContext> m_context;
};

}
