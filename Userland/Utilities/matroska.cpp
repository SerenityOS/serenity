/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibMain/Main.h>
#include <LibVideo/Containers/Matroska/Reader.h>

#define TRY_PARSE(expression)                                                                     \
    ({                                                                                            \
        auto _temporary_result = ((expression));                                                  \
        if (_temporary_result.is_error()) [[unlikely]] {                                          \
            outln("Encountered a parsing error: {}", _temporary_result.error().string_literal()); \
            return Error::from_string_literal("Failed to parse :(");                              \
        }                                                                                         \
        _temporary_result.release_value();                                                        \
    })

ErrorOr<int> serenity_main(Main::Arguments)
{
    auto reader = TRY_PARSE(Video::Matroska::Reader::from_file("/home/anon/Videos/test-webm.webm"sv));

    outln("DocType is {}", reader.header().doc_type.characters());
    outln("DocTypeVersion is {}", reader.header().doc_type_version);
    auto segment_information = TRY_PARSE(reader.segment_information());
    outln("Timestamp scale is {}", segment_information.timestamp_scale());
    outln("Muxing app is \"{}\"", segment_information.muxing_app().as_string().to_string().characters());
    outln("Writing app is \"{}\"", segment_information.writing_app().as_string().to_string().characters());

    outln("Document has {} tracks", TRY_PARSE(reader.track_count()));
    TRY_PARSE(reader.for_each_track([&](Video::Matroska::TrackEntry const& track_entry) -> Video::DecoderErrorOr<IterationDecision> {
        outln("\tTrack #{} with TrackID {}", track_entry.track_number(), track_entry.track_uid());
        outln("\tTrack has TrackType {}", static_cast<u8>(track_entry.track_type()));
        outln("\tTrack has Language \"{}\"", track_entry.language().characters());
        outln("\tTrack has CodecID \"{}\"", track_entry.codec_id().characters());

        if (track_entry.track_type() == Video::Matroska::TrackEntry::TrackType::Video) {
            auto const video_track = track_entry.video_track().value();
            outln("\t\tVideo is {} pixels wide by {} pixels tall", video_track.pixel_width, video_track.pixel_height);
        } else if (track_entry.track_type() == Video::Matroska::TrackEntry::TrackType::Audio) {
            auto const audio_track = track_entry.audio_track().value();
            outln("\t\tAudio has {} channels with a bit depth of {}", audio_track.channels, audio_track.bit_depth);
        }

        outln("\tBlocks:");
        auto iterator = TRY(reader.create_sample_iterator(track_entry.track_number()));

        while (true) {
            auto block_result = iterator.next_block();
            if (block_result.is_error()) {
                if (block_result.error().category() == Video::DecoderErrorCategory::EndOfStream)
                    break;
                return block_result.release_error();
            }
            auto block = block_result.release_value();
            outln("\t\tBlock at timestamp {}:", iterator.current_cluster().timestamp() + block.timestamp());
            outln("\t\t\tContains {} frames", block.frame_count());
            outln("\t\t\tLacing is {}", static_cast<u8>(block.lacing()));
        }

        return IterationDecision::Continue;
    }));

    return 0;
}
