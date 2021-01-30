/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibVideo/MatroskaReader.h>
#include <stdio.h>

int main(int, char**)
{
    auto document = Video::MatroskaReader::parse_matroska_from_file("/home/anon/Videos/test-webm.webm");
    if (document) {
        printf("DocType is %s\n", document->header().doc_type.characters());
        printf("DocTypeVersion is %d\n", document->header().doc_type_version);
        auto segment_information = document->segment_information();
        if (segment_information.has_value()) {
            printf("Timestamp scale is %llu\n", segment_information.value().timestamp_scale());
            printf("Muxing app is \"%s\"\n", segment_information.value().muxing_app().as_string().to_string().characters());
            printf("Writing app is \"%s\"\n", segment_information.value().writing_app().as_string().to_string().characters());
        }
        printf("Document has %zu tracks\n", document->tracks().size());
        for (const auto& track_entry : document->tracks()) {
            const auto& track = *track_entry.value;
            printf("\tTrack #%llu with TrackID %llu\n", track.track_number(), track.track_uid());
            printf("\tTrack has TrackType %d\n", track.track_type());
            printf("\tTrack has Language \"%s\"\n", track.language().characters());
            printf("\tTrack has CodecID \"%s\"\n", track.codec_id().characters());

            if (track.track_type() == Video::TrackEntry::TrackType::Video) {
                const auto& video_track = track.video_track().value();
                printf("\t\tVideo is %llu pixels wide by %llu pixels tall\n", video_track.pixel_width, video_track.pixel_height);
            } else if (track.track_type() == Video::TrackEntry::TrackType::Audio) {
                const auto& audio_track = track.audio_track().value();
                printf("\t\tAudio has %llu channels with a bit depth of %llu\n", audio_track.channels, audio_track.bit_depth);
            }
        }

        printf("Document has %zu clusters\n", document->clusters().size());
        for (const auto& cluster : document->clusters()) {
            printf("\tCluster timestamp is %llu\n", cluster.timestamp());

            printf("\tCluster has %zu blocks\n", cluster.blocks().size());
            for (const auto& block : cluster.blocks()) {
                (void)block;
                printf("\t\tBlock for track #%llu has %llu frames\n", block.track_number(), block.frame_count());
                printf("\t\tBlock's timestamp is %d\n", block.timestamp());
                printf("\t\tBlock has lacing %d\n", block.lacing());
            }
        }
    } else {
        printf("Null :(\n");
    }
}
