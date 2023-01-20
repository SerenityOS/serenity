/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/ICOLoader.h>
#include <LibGfx/ImageDecoder.h>

namespace Gfx {

struct BMPLoadingContext;
class ICOImageDecoderPlugin;

class BMPImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    static ErrorOr<bool> sniff(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<BMPImageDecoderPlugin>> create_as_included_in_ico(Badge<ICOImageDecoderPlugin>, ReadonlyBytes);

    enum class IncludedInICO {
        Yes,
        No,
    };

    virtual ~BMPImageDecoderPlugin() override;

    virtual IntSize size() override;
    virtual void set_volatile() override;
    [[nodiscard]] virtual bool set_nonvolatile(bool& was_purged) override;
    virtual bool initialize() override;
    bool sniff_dib();
    virtual bool is_animated() override;
    virtual size_t loop_count() override;
    virtual size_t frame_count() override;
    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index) override;

private:
    BMPImageDecoderPlugin(u8 const*, size_t, IncludedInICO included_in_ico = IncludedInICO::No);

    OwnPtr<BMPLoadingContext> m_context;
};

}
