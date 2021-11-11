/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageDecoder.h>

namespace Gfx {

RefPtr<Gfx::Bitmap> load_gif_from_memory(u8 const*, size_t, String const& mmap_name = "<memory>");

struct GIFLoadingContext;

class GIFImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    virtual ~GIFImageDecoderPlugin() override;
    GIFImageDecoderPlugin(const u8*, size_t);

    virtual IntSize size() override;
    virtual RefPtr<Gfx::Bitmap> bitmap() override;
    virtual void set_volatile() override;
    [[nodiscard]] virtual bool set_nonvolatile(bool& was_purged) override;
    virtual bool sniff() override;
    virtual bool is_animated() override;
    virtual size_t loop_count() override;
    virtual size_t frame_count() override;
    virtual ImageFrameDescriptor frame(size_t i) override;

private:
    OwnPtr<GIFLoadingContext> m_context;
};

}
