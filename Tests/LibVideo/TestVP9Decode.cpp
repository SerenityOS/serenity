/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <LibVideo/MatroskaReader.h>
#include <LibVideo/VP9/Decoder.h>

static void decode_video(StringView path, size_t expected_frame_count)
{
    auto matroska_document = Video::MatroskaReader::MatroskaReader::parse_matroska_from_file(path);
    VERIFY(matroska_document);
    auto video_track_optional = matroska_document->track_for_track_type(Video::TrackEntry::TrackType::Video);
    VERIFY(video_track_optional.has_value());
    auto video_track_entry = video_track_optional.value();

    size_t frame_count = 0;
    size_t cluster_index, block_index, frame_index;
    Video::VP9::Decoder vp9_decoder;

    for (cluster_index = 0; cluster_index < matroska_document->clusters().size(); cluster_index++) {
        auto const& cluster = matroska_document->clusters()[cluster_index];
        for (block_index = 0; block_index < cluster.blocks().size(); block_index++) {
            auto const& block = cluster.blocks()[block_index];
            if (block.track_number() != video_track_entry.track_number())
                continue;

            for (frame_index = 0; frame_index < block.frames().size(); frame_index++) {
                MUST(vp9_decoder.decode(block.frames()[frame_index]));
                frame_count++;
            }
        }
    }
    VERIFY(frame_count == expected_frame_count);
}

TEST_CASE(webm_in_vp9)
{
    decode_video("./vp9_in_webm.webm"sv, 25);
}

BENCHMARK_CASE(vp9_4k)
{
    decode_video("./vp9_4k.webm"sv, 2);
}
