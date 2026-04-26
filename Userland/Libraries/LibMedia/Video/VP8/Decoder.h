/*
 * Copyright (c) 2026, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Error.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Span.h>
#include <LibMedia/DecoderError.h>
#include <LibMedia/VideoDecoder.h>
#include <LibMedia/VideoFrame.h>

namespace Media::Video::VP8 {

class Decoder final : public VideoDecoder {
public:
    Decoder();
    ~Decoder() override;

    DecoderErrorOr<void> receive_sample(Duration timestamp, ReadonlyBytes) override;

    DecoderErrorOr<NonnullOwnPtr<VideoFrame>> get_decoded_frame() override;

    void flush() override;
};

}
