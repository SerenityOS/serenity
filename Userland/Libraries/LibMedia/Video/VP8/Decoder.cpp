/*
 * Copyright (c) 2026, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/ImageFormats/WebPLoaderLossy.h>
#include <LibGfx/Painter.h>

#include "Decoder.h"

#if defined(AK_COMPILER_GCC)
#    pragma GCC optimize("O3")
#endif

namespace Media::Video::VP8 {

DecoderErrorOr<void> BitmapVideoFrame::output_to_bitmap(Gfx::Bitmap& bitmap)
{
    // FIXME: This is a wasteful copy, see class-level FIXME.
    Gfx::Painter painter(bitmap);
    painter.blit({ 0, 0 }, *m_bitmap, m_bitmap->rect());
    return {};
}

Decoder::Decoder()
{
}

Decoder::~Decoder()
{
}

DecoderErrorOr<void> Decoder::receive_sample(Duration timestamp, ReadonlyBytes data)
{
    NonnullRefPtr<Gfx::Bitmap> bitmap = DECODER_TRY(DecoderErrorCategory::Invalid, [&] -> ErrorOr<NonnullRefPtr<Gfx::Bitmap>> {
        auto header = TRY(Gfx::decode_webp_chunk_VP8_header(data));
        if (!header.keyframe_data.has_value()) {
            if (!m_last_keyframe)
                return Error::from_string_literal("VP8: non-keyframe without previous keyframe");

            // For now, replace non-keyframes with the last keyframe.
            // FIXME: Add support for decoding non-keyframes.
            return *m_last_keyframe;
        }

        auto bitmap = TRY(Gfx::decode_webp_chunk_VP8_contents(header, false));
        m_last_keyframe = bitmap;
        return bitmap;
    }());

    // These values are only used by SubsampledYUVFrame::output_to_bitmap() as far as I can tell.
    // Since we don't currently use SubsampledYUVFrame, exact values here don't currently matter.
    CodingIndependentCodePoints cicp(ColorPrimaries::BT601, TransferCharacteristics::SRGB, MatrixCoefficients::BT601, VideoFullRangeFlag::Studio);
    auto video_frame = make<BitmapVideoFrame>(timestamp, Gfx::Size<u32>(bitmap->width(), bitmap->height()), 8, cicp, bitmap);
    m_video_frame_queue.enqueue(move(video_frame));

    return {};
}

DecoderErrorOr<NonnullOwnPtr<VideoFrame>> Decoder::get_decoded_frame()
{
    if (m_video_frame_queue.is_empty())
        return DecoderError::format(DecoderErrorCategory::NeedsMoreInput, "No video frame in queue.");

    return m_video_frame_queue.dequeue();
}

void Decoder::flush()
{
    m_video_frame_queue.clear();
}

}
