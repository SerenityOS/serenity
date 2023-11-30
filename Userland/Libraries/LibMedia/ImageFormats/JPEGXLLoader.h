/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/MemoryStream.h>
#include <LibMedia/ImageFormats/ImageDecoder.h>

namespace Media {

class JPEGXLLoadingContext;

class JPEGXLImageDecoderPlugin : public ImageDecoderPlugin {
public:
    static bool sniff(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);

    virtual ~JPEGXLImageDecoderPlugin() override;

    virtual Gfx::IntSize size() override;
    virtual bool is_animated() override;
    virtual size_t loop_count() override;
    virtual size_t frame_count() override;
    virtual size_t first_animated_frame_index() override;
    virtual ErrorOr<Optional<ReadonlyBytes>> icc_data() override;

    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index, Optional<Gfx::IntSize> ideal_size = {}) override;

private:
    JPEGXLImageDecoderPlugin(NonnullOwnPtr<FixedMemoryStream>);

    OwnPtr<JPEGXLLoadingContext> m_context;
};

}
