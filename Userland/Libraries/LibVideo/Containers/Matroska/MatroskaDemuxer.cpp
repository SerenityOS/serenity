/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MatroskaDemuxer.h"

namespace Video::Matroska {

DecoderErrorOr<NonnullOwnPtr<MatroskaDemuxer>> MatroskaDemuxer::from_file(StringView filename)
{
    return make<MatroskaDemuxer>(TRY(Reader::from_file(filename)));
}

DecoderErrorOr<NonnullOwnPtr<MatroskaDemuxer>> MatroskaDemuxer::from_data(ReadonlyBytes data)
{
    return make<MatroskaDemuxer>(TRY(Reader::from_data(data)));
}

DecoderErrorOr<Vector<Track>> MatroskaDemuxer::get_tracks_for_type(TrackType type)
{
    TrackEntry::TrackType matroska_track_type;

    switch (type) {
    case TrackType::Video:
        matroska_track_type = TrackEntry::TrackType::Video;
        break;
    case TrackType::Audio:
        matroska_track_type = TrackEntry::TrackType::Audio;
        break;
    case TrackType::Subtitles:
        matroska_track_type = TrackEntry::TrackType::Subtitle;
        break;
    }

    Vector<Track> tracks;
    TRY(m_reader.for_each_track_of_type(matroska_track_type, [&](TrackEntry const& track_entry) -> DecoderErrorOr<IterationDecision> {
        VERIFY(track_entry.track_type() == matroska_track_type);
        DECODER_TRY_ALLOC(tracks.try_append(Track(type, track_entry.track_number())));
        return IterationDecision::Continue;
    }));
    return tracks;
}

DecoderErrorOr<MatroskaDemuxer::TrackStatus*> MatroskaDemuxer::get_track_status(Track track)
{
    if (!m_track_statuses.contains(track)) {
        auto iterator = TRY(m_reader.create_sample_iterator(track.identifier()));
        DECODER_TRY_ALLOC(m_track_statuses.try_set(track, { iterator }));
    }

    return &m_track_statuses.get(track).release_value();
}

DecoderErrorOr<Time> MatroskaDemuxer::seek_to_most_recent_keyframe(Track track, Time timestamp)
{
    // Removing the track status will cause us to start from the beginning.
    if (timestamp.is_zero()) {
        m_track_statuses.remove(track);
        return timestamp;
    }

    auto& track_status = *TRY(get_track_status(track));
    TRY(m_reader.seek_to_random_access_point(track_status.iterator, timestamp));
    return track_status.iterator.last_timestamp();
}

DecoderErrorOr<NonnullOwnPtr<Sample>> MatroskaDemuxer::get_next_sample_for_track(Track track)
{
    // FIXME: This makes a copy of the sample, which shouldn't be necessary.
    //        Matroska should make a RefPtr<ByteBuffer>, probably.
    auto& status = *TRY(get_track_status(track));

    if (!status.block.has_value() || status.frame_index >= status.block->frame_count()) {
        status.block = TRY(status.iterator.next_block());
        status.frame_index = 0;
    }
    auto cicp = TRY(m_reader.track_for_track_number(track.identifier())).video_track()->color_format.to_cicp();
    return make<VideoSample>(status.block->frame(status.frame_index++), cicp, status.block->timestamp());
}

DecoderErrorOr<Time> MatroskaDemuxer::duration()
{
    auto duration = TRY(m_reader.segment_information()).duration();
    return duration.value_or(Time::zero());
}

}
