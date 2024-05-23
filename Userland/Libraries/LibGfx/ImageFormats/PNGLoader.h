/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/ImageFormats/ImageDecoder.h>
#include <LibGfx/ImageFormats/PNGShared.h>

namespace Gfx {

struct PNGLoadingContext;

class PNGImageDecoderPlugin final : public ImageDecoderPlugin {
public:
    static bool sniff(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);

    virtual ~PNGImageDecoderPlugin() override;

    virtual IntSize size() override;

    virtual bool is_animated() override;
    virtual size_t loop_count() override;
    virtual size_t frame_count() override;
    virtual size_t first_animated_frame_index() override;
    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index, Optional<IntSize> ideal_size = {}) override;
    virtual Optional<Metadata const&> metadata() override;
    virtual ErrorOr<Optional<ReadonlyBytes>> icc_data() override;

    static void unfilter_scanline(PNG::FilterType filter, Bytes scanline_data, ReadonlyBytes previous_scanlines_data, u8 bytes_per_complete_pixel);

private:
    PNGImageDecoderPlugin(u8 const*, size_t);
    bool ensure_image_data_chunk_was_decoded();
    bool ensure_animation_frame_was_decoded(u32);

    OwnPtr<PNGLoadingContext> m_context;
};

}
