/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/MemoryStream.h>
#include <AK/OwnPtr.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>

namespace Gfx {

struct JBIG2LoadingContext;

struct JBIG2DecoderOptions {
    enum class LogComments {
        No,
        Yes,
    };
    LogComments log_comments { LogComments::Yes };
};

class JBIG2ImageDecoderPlugin : public ImageDecoderPlugin {
public:
    static bool sniff(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create(ReadonlyBytes);
    static ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> create_with_options(ReadonlyBytes, JBIG2DecoderOptions);

    virtual ~JBIG2ImageDecoderPlugin() override;

    virtual IntSize size() override;

    virtual size_t frame_count() override;
    virtual ErrorOr<ImageFrameDescriptor> frame(size_t index, Optional<IntSize> ideal_size = {}) override;

    static ErrorOr<ByteBuffer> decode_embedded(Vector<ReadonlyBytes>);

private:
    JBIG2ImageDecoderPlugin(JBIG2DecoderOptions);

    OwnPtr<JBIG2LoadingContext> m_context;
};

}
