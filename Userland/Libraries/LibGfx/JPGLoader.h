/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibGfx/ImageDecoder.h>

namespace Gfx {

RefPtr<Gfx::Bitmap> load_jpg_from_memory(u8 const* data, size_t length, String const& mmap_name = "<memory>");

struct JPGLoadingContext;

class JPGImageDecoderPlugin : public ImageDecoderPlugin {
public:
    virtual ~JPGImageDecoderPlugin() override;
    JPGImageDecoderPlugin(const u8*, size_t);
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
    OwnPtr<JPGLoadingContext> m_context;
};
}
