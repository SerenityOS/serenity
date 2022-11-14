/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibVideo/Containers/Demuxer.h>

#include "Reader.h"

namespace Video::Matroska {

class MatroskaDemuxer final : public Demuxer {
public:
    // FIXME: We should instead accept some abstract data streaming type so that the demuxer
    //        can work with non-contiguous data.
    static DecoderErrorOr<NonnullOwnPtr<MatroskaDemuxer>> from_file(StringView filename);
    static DecoderErrorOr<NonnullOwnPtr<MatroskaDemuxer>> from_data(ReadonlyBytes data);

    MatroskaDemuxer(Reader&& reader)
        : m_reader(move(reader))
    {
    }

    DecoderErrorOr<Vector<Track>> get_tracks_for_type(TrackType type) override;

    DecoderErrorOr<Time> seek_to_most_recent_keyframe(Track track, Time timestamp) override;

    DecoderErrorOr<Time> duration() override;

protected:
    DecoderErrorOr<NonnullOwnPtr<Sample>> get_next_sample_for_track(Track track) override;

private:
    struct TrackStatus {
        SampleIterator iterator;
        Optional<Block> block {};
        size_t frame_index { 0 };
    };

    DecoderErrorOr<TrackStatus*> get_track_status(Track track);

    Reader m_reader;

    HashMap<Track, TrackStatus> m_track_statuses;
};

}
