/*
 * Copyright (c) 2023, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <LibVideo/Containers/Matroska/Reader.h>

TEST_CASE(master_elements_containing_crc32)
{
    auto matroska_reader = MUST(Video::Matroska::Reader::from_file("master_elements_containing_crc32.mkv"sv));
    u64 video_track = 0;
    MUST(matroska_reader.for_each_track_of_type(Video::Matroska::TrackEntry::TrackType::Video, [&](Video::Matroska::TrackEntry const& track_entry) -> Video::DecoderErrorOr<IterationDecision> {
        video_track = track_entry.track_number();
        return IterationDecision::Break;
    }));
    VERIFY(video_track == 1);

    auto iterator = MUST(matroska_reader.create_sample_iterator(video_track));
    MUST(iterator.next_block());
    MUST(matroska_reader.seek_to_random_access_point(iterator, Duration::from_seconds(7)));
    MUST(iterator.next_block());
}
