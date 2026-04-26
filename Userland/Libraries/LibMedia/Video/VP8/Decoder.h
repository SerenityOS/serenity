/*
 * Copyright (c) 2026, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Error.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Queue.h>
#include <AK/Span.h>
#include <LibMedia/DecoderError.h>
#include <LibMedia/VideoDecoder.h>
#include <LibMedia/VideoFrame.h>

namespace Media::Video::VP8 {

// FIXME: Instead, teach the webp decoder to return YUV data and use SubsampledYUVFrame here.
class BitmapVideoFrame final : public VideoFrame {
public:
    BitmapVideoFrame(Duration timestamp, Gfx::Size<u32> size, u8 bit_depth, CodingIndependentCodePoints cicp, NonnullRefPtr<Gfx::Bitmap> bitmap)
        : VideoFrame(timestamp, size, bit_depth, cicp)
        , m_bitmap(move(bitmap))
    {
    }

    virtual DecoderErrorOr<void> output_to_bitmap(Gfx::Bitmap& bitmap) override;

private:
    NonnullRefPtr<Gfx::Bitmap> m_bitmap;
};

class Decoder final : public VideoDecoder {
public:
    Decoder();
    ~Decoder() override;

    DecoderErrorOr<void> receive_sample(Duration timestamp, ReadonlyBytes) override;

    DecoderErrorOr<NonnullOwnPtr<VideoFrame>> get_decoded_frame() override;

    void flush() override;

private:
    RefPtr<Gfx::Bitmap> m_last_keyframe;
    Queue<NonnullOwnPtr<BitmapVideoFrame>, 1> m_video_frame_queue;
};

}
