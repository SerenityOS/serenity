/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <LibCore/Object.h>

#include "DecoderError.h"
#include "Sample.h"
#include "Track.h"

namespace Video {

class Demuxer {
public:
    virtual ~Demuxer() = default;

    virtual Vector<Track> get_tracks_for_type(TrackType type) = 0;

    DecoderErrorOr<NonnullOwnPtr<VideoSample>> get_next_video_sample_for_track(Track track)
    {
        VERIFY(track.type() == TrackType::Video);
        auto sample = TRY(get_next_sample_for_track(track));
        VERIFY(sample->is_video_sample());
        return sample.release_nonnull<VideoSample>();
    }

    virtual DecoderErrorOr<void> seek_to_most_recent_keyframe(Track track, size_t timestamp) = 0;

    virtual Time duration() = 0;

protected:
    virtual DecoderErrorOr<NonnullOwnPtr<Sample>> get_next_sample_for_track(Track track) = 0;
};

}
