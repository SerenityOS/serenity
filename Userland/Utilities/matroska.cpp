/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibCore/ArgsParser.h>
#include <LibMain/Main.h>
#include <LibMedia/Containers/Matroska/Reader.h>

#define TRY_PARSE(expression)                                                                        \
    ({                                                                                               \
        auto&& _temporary_result = ((expression));                                                   \
        if (_temporary_result.is_error()) [[unlikely]] {                                             \
            outln("Encountered a parsing error: {}", _temporary_result.error().string_literal());    \
            return Error::from_string_literal("Failed to parse :(");                                 \
        }                                                                                            \
        static_assert(!::AK::Detail::IsLvalueReference<decltype(_temporary_result.release_value())>, \
            "Do not return a reference from a fallible expression");                                 \
        _temporary_result.release_value();                                                           \
    })

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView filename;
    bool blocks = false;
    bool cues = false;
    u64 track_number = 0;

    Core::ArgsParser args_parser;
    args_parser.add_option(blocks, "Print blocks for each track.", "blocks", 'b');
    args_parser.add_option(cues, "Print cue points for each track.", "cues", 'c');
    args_parser.add_option<u64>(track_number, "Specify a track number to print info for, omit to print all of them.", "track", 't', "tracknumber");
    args_parser.add_positional_argument(filename, "The video file to display.", "filename", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    auto reader = TRY_PARSE(Media::Matroska::Reader::from_file(filename));

    outln("DocType is {}", reader.header().doc_type.characters());
    outln("DocTypeVersion is {}", reader.header().doc_type_version);
    auto segment_information = TRY_PARSE(reader.segment_information());
    outln("Timestamp scale is {}", segment_information.timestamp_scale());
    outln("Muxing app is \"{}\"", segment_information.muxing_app().as_string().to_byte_string().characters());
    outln("Writing app is \"{}\"", segment_information.writing_app().as_string().to_byte_string().characters());

    outln("Document has {} tracks", TRY_PARSE(reader.track_count()));
    TRY_PARSE(reader.for_each_track([&](Media::Matroska::TrackEntry const& track_entry) -> Media::DecoderErrorOr<IterationDecision> {
        if (track_number != 0 && track_entry.track_number() != track_number)
            return IterationDecision::Continue;

        outln("\tTrack #{} with TrackID {}", track_entry.track_number(), track_entry.track_uid());
        outln("\tTrack has TrackType {}", static_cast<u8>(track_entry.track_type()));
        outln("\tTrack has Language \"{}\"", track_entry.language());
        outln("\tTrack has CodecID \"{}\"", track_entry.codec_id());
        outln("\tTrack has TrackTimestampScale {}", track_entry.timestamp_scale());
        outln("\tTrack has CodecDelay {}", track_entry.codec_delay());

        if (track_entry.track_type() == Media::Matroska::TrackEntry::TrackType::Video) {
            auto const video_track = track_entry.video_track().value();
            outln("\t\tVideo is {} pixels wide by {} pixels tall", video_track.pixel_width, video_track.pixel_height);
        } else if (track_entry.track_type() == Media::Matroska::TrackEntry::TrackType::Audio) {
            auto const audio_track = track_entry.audio_track().value();
            outln("\t\tAudio has {} channels with a bit depth of {}", audio_track.channels, audio_track.bit_depth);
        }

        if (cues) {
            auto const& cue_points = TRY(reader.cue_points_for_track(track_entry.track_number()));

            if (cue_points.has_value()) {
                outln("\tCues points:");

                for (auto const& cue_point : cue_points.value()) {
                    outln("\t\tCue point at {}ms:", cue_point.timestamp().to_milliseconds());
                    auto const& track_position = cue_point.position_for_track(track_entry.track_number());

                    if (!track_position.has_value()) {
                        outln("\t\t\tCue point has no positions for this track, this should not happen");
                        continue;
                    }
                    outln("\t\t\tCluster position {}", track_position->cluster_position());
                    outln("\t\t\tBlock offset {}", track_position->block_offset());
                }
            } else {
                outln("\tNo cue points exist for this track");
            }
        }

        if (blocks) {
            outln("\tBlocks:");
            auto iterator = TRY(reader.create_sample_iterator(track_entry.track_number()));

            while (true) {
                auto block_result = iterator.next_block();
                if (block_result.is_error()) {
                    if (block_result.error().category() == Media::DecoderErrorCategory::EndOfStream)
                        break;
                    return block_result.release_error();
                }
                auto block = block_result.release_value();
                outln("\t\tBlock at timestamp {}ms:", block.timestamp().to_milliseconds());
                if (block.only_keyframes())
                    outln("\t\t\tThis block contains only keyframes");
                outln("\t\t\tContains {} frames", block.frame_count());
                outln("\t\t\tLacing is {}", static_cast<u8>(block.lacing()));
            }
        }

        if (track_number != 0)
            return IterationDecision::Break;

        return IterationDecision::Continue;
    }));

    return 0;
}
