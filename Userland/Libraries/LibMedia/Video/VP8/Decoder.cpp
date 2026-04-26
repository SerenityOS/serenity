/*
 * Copyright (c) 2026, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Decoder.h"

#if defined(AK_COMPILER_GCC)
#    pragma GCC optimize("O3")
#endif

namespace Media::Video::VP8 {

Decoder::Decoder()
{
}

Decoder::~Decoder()
{
}

DecoderErrorOr<void> Decoder::receive_sample(Duration, ReadonlyBytes)
{
    return DecoderError::format(DecoderErrorCategory::NotImplemented, "Can't receive VP8 samples yet");
}

DecoderErrorOr<NonnullOwnPtr<VideoFrame>> Decoder::get_decoded_frame()
{
    return DecoderError::format(DecoderErrorCategory::NotImplemented, "Can't decode VP8 yet");
}

void Decoder::flush()
{
}

}
