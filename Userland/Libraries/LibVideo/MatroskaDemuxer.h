/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>

#include "Demuxer.h"
#include "MatroskaReader.h"

namespace Video {

class MatroskaDemuxer final : public Demuxer {
public:
    // FIXME: We should instead accept some abstract data streaming type so that the demuxer
    //        can work with non-contiguous data.
    static DecoderErrorOr<NonnullOwnPtr<MatroskaDemuxer>> from_file(StringView filename);
    static DecoderErrorOr<NonnullOwnPtr<MatroskaDemuxer>> from_data(ReadonlyBytes data);

    MatroskaDemuxer(NonnullOwnPtr<MatroskaDocument>& document)
        : m_document(move(document))
    {
    }

    Vector<Track> get_tracks_for_type(TrackType type) override;

    DecoderErrorOr<void> seek_to_most_recent_keyframe(Track track, size_t timestamp) override;

    Time duration() override;

protected:
    DecoderErrorOr<NonnullOwnPtr<Sample>> get_next_sample_for_track(Track track) override;

private:
    struct TrackStatus {
        size_t m_cluster_index { 0 };
        size_t m_block_index { 0 };
        size_t m_frame_index { 0 };
    };

    ErrorOr<TrackStatus*> get_track_status(Track track);

    NonnullOwnPtr<MatroskaDocument> m_document;

    HashMap<Track, TrackStatus> m_track_statuses;
};

}
