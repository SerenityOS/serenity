/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/ImageFormats/ICOLoader.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>

namespace Gfx {

struct BMPLoadingContext;
class ICOImageDecoderPlugin;

class BMPImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    static bool sniff(ReadonlyBytes);
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
    virtual ErrorOr<void> initialize() override;
    bool sniff_dib();
    virtual bool is_animated() override;
    virtual size_t loop_count() override;
    virtual size_t frame_count() override;
    virtual size_t first_animated_frame_index() override;
    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index) override;
    virtual ErrorOr<Optional<ReadonlyBytes>> icc_data() override;

private:
    BMPImageDecoderPlugin(u8 const*, size_t, IncludedInICO included_in_ico = IncludedInICO::No);

    OwnPtr<BMPLoadingContext> m_context;
};

}
