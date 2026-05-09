/*
 * Copyright (c) 2026, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibMedia/Video/VP8/Decoder.h>

#include "TestMediaCommon.h"

static NonnullOwnPtr<Media::VideoDecoder> make_decoder(Media::Matroska::SampleIterator const&)
{
    return make<Media::Video::VP8::Decoder>();
}

TEST_CASE(webm_in_vp8)
{
    decode_video("./vp8_in_webm.webm"sv, 25, make_decoder);
}
