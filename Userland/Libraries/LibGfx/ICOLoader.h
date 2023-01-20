/*
 * Copyright (c) 2020, Paul Roukema <roukemap@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/ImageDecoder.h>

namespace Gfx {

struct ICOLoadingContext;

class ICOImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    static ErrorOr<bool> sniff(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);

    virtual ~ICOImageDecoderPlugin() override;

    virtual IntSize size() override;
    virtual void set_volatile() override;
    [[nodiscard]] virtual bool set_nonvolatile(bool& was_purged) override;
    virtual bool initialize() override;
    virtual bool is_animated() override;
    virtual size_t loop_count() override;
    virtual size_t frame_count() override;
    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index) override;

private:
    ICOImageDecoderPlugin(u8 const*, size_t);
    static bool load_ico_bitmap(ICOLoadingContext& context, Optional<size_t> index);

    OwnPtr<ICOLoadingContext> m_context;
};

}
