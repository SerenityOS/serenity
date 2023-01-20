/*
 * Copyright (c) 2022, Tom Needham <06needhamt@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/ImageDecoder.h>

namespace Gfx {

struct TGALoadingContext;

class TGAImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    static ErrorOr<bool> validate_before_create(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);

    virtual ~TGAImageDecoderPlugin() override;
    TGAImageDecoderPlugin(u8 const*, size_t);

    virtual IntSize size() override;
    virtual void set_volatile() override;
    [[nodiscard]] virtual bool set_nonvolatile(bool& was_purged) override;
    virtual bool initialize() override;
    virtual bool is_animated() override;
    virtual size_t loop_count() override;
    virtual size_t frame_count() override;
    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index) override;

private:
    bool decode_tga_header();
    OwnPtr<TGALoadingContext> m_context;
};

}
