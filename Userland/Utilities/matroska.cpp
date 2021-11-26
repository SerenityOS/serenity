/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibVideo/MatroskaReader.h>

int main(int, char**)
{
    auto document = Video::MatroskaReader::parse_matroska_from_file("/home/anon/Videos/test-webm.webm");
    if (!document) {
        outln("Failed to parse :(");
        return 1;
    }

    outln("DocType is {}", document->header().doc_type.characters());
    outln("DocTypeVersion is {}", document->header().doc_type_version);
    auto segment_information = document->segment_information();
    if (segment_information.has_value()) {
        outln("Timestamp scale is {}", segment_information.value().timestamp_scale());
        outln("Muxing app is \"{}\"", segment_information.value().muxing_app().as_string().to_string().characters());
        outln("Writing app is \"{}\"", segment_information.value().writing_app().as_string().to_string().characters());
    }
    outln("Document has {} tracks", document->tracks().size());
    for (auto const& track_entry : document->tracks()) {
        auto const& track = *track_entry.value;
        outln("\tTrack #{} with TrackID {}", track.track_number(), track.track_uid());
        outln("\tTrack has TrackType {}", static_cast<u8>(track.track_type()));
        outln("\tTrack has Language \"{}\"", track.language().characters());
        outln("\tTrack has CodecID \"{}\"", track.codec_id().characters());

        if (track.track_type() == Video::TrackEntry::TrackType::Video) {
            auto const video_track = track.video_track().value();
            outln("\t\tVideo is {} pixels wide by {} pixels tall", video_track.pixel_width, video_track.pixel_height);
        } else if (track.track_type() == Video::TrackEntry::TrackType::Audio) {
            auto const audio_track = track.audio_track().value();
            outln("\t\tAudio has {} channels with a bit depth of {}", audio_track.channels, audio_track.bit_depth);
        }
    }

    outln("Document has {} clusters", document->clusters().size());
    for (auto const& cluster : document->clusters()) {
        outln("\tCluster timestamp is {}", cluster.timestamp());

        outln("\tCluster has {} blocks", cluster.blocks().size());
        for (auto const& block : cluster.blocks()) {
            (void)block;
            outln("\t\tBlock for track #{} has {} frames", block.track_number(), block.frame_count());
            outln("\t\tBlock's timestamp is {}", block.timestamp());
            outln("\t\tBlock has lacing {}", static_cast<u8>(block.lacing()));
        }
    }
}
