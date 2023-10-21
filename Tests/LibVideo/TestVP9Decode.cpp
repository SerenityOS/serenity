/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <LibVideo/Containers/Matroska/Reader.h>
#include <LibVideo/VP9/Decoder.h>

static void decode_video(StringView path, size_t expected_frame_count)
{
    auto matroska_reader = MUST(Video::Matroska::Reader::from_file(path));
    u64 video_track = 0;
    MUST(matroska_reader.for_each_track_of_type(Video::Matroska::TrackEntry::TrackType::Video, [&](Video::Matroska::TrackEntry const& track_entry) -> Video::DecoderErrorOr<IterationDecision> {
        video_track = track_entry.track_number();
        return IterationDecision::Break;
    }));
    VERIFY(video_track != 0);

    auto iterator = MUST(matroska_reader.create_sample_iterator(video_track));
    size_t frame_count = 0;
    Video::VP9::Decoder vp9_decoder;

    while (frame_count <= expected_frame_count) {
        auto block_result = iterator.next_block();
        if (block_result.is_error() && block_result.error().category() == Video::DecoderErrorCategory::EndOfStream) {
            VERIFY(frame_count == expected_frame_count);
            return;
        }

        auto block = block_result.release_value();
        for (auto const& frame : block.frames()) {
            MUST(vp9_decoder.receive_sample(frame));
            while (true) {
                auto frame_result = vp9_decoder.get_decoded_frame();
                if (frame_result.is_error()) {
                    if (frame_result.error().category() == Video::DecoderErrorCategory::NeedsMoreInput) {
                        break;
                    }
                    VERIFY_NOT_REACHED();
                }
            }
            frame_count++;
        }
    }

    VERIFY_NOT_REACHED();
}

TEST_CASE(webm_in_vp9)
{
    decode_video("./vp9_in_webm.webm"sv, 25);
}

TEST_CASE(vp9_oob_blocks)
{
    decode_video("./vp9_oob_blocks.webm"sv, 240);
}

TEST_CASE(vp9_malformed_frame)
{
    Array test_inputs = {
        "./oss-fuzz-testcase-52630.vp9"sv,
        "./oss-fuzz-testcase-53977.vp9"sv,
        "./oss-fuzz-testcase-62054.vp9"sv,
        "./oss-fuzz-testcase-63182.vp9"sv
    };

    for (auto test_input : test_inputs) {
        auto file = MUST(Core::MappedFile::map(test_input));
        Video::VP9::Decoder vp9_decoder;
        auto maybe_decoder_error = vp9_decoder.receive_sample(file->bytes());
        EXPECT(maybe_decoder_error.is_error());
    }
}

BENCHMARK_CASE(vp9_4k)
{
    decode_video("./vp9_4k.webm"sv, 2);
}

BENCHMARK_CASE(vp9_clamp_reference_mvs)
{
    decode_video("./vp9_clamp_reference_mvs.webm"sv, 92);
}
