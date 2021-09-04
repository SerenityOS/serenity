/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageDecoder.h>

namespace Gfx {

RefPtr<Gfx::Bitmap> load_bmp(String const& path);
RefPtr<Gfx::Bitmap> load_bmp_from_memory(u8 const*, size_t);

struct BMPLoadingContext;

class BMPImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    virtual ~BMPImageDecoderPlugin() override;
    BMPImageDecoderPlugin(u8 const*, size_t);

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
    OwnPtr<BMPLoadingContext> m_context;
};

}
