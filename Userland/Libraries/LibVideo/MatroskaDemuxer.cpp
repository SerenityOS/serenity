/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MatroskaDemuxer.h"

namespace Video {

DecoderErrorOr<NonnullOwnPtr<MatroskaDemuxer>> MatroskaDemuxer::from_file(StringView filename)
{
    // FIXME: MatroskaReader should return errors.
    auto nullable_document = MatroskaReader::parse_matroska_from_file(filename);
    if (!nullable_document)
        return DecoderError::format(DecoderErrorCategory::IO, "Failed to open matroska from file '{}'", filename);
    auto document = nullable_document.release_nonnull();
    return make<MatroskaDemuxer>(document);
}

DecoderErrorOr<NonnullOwnPtr<MatroskaDemuxer>> MatroskaDemuxer::from_data(ReadonlyBytes data)
{
    // FIXME: MatroskaReader should return errors.
    auto nullable_document = MatroskaReader::parse_matroska_from_data(data.data(), data.size());
    if (!nullable_document)
        return DecoderError::format(DecoderErrorCategory::IO, "Failed to open matroska from data");
    auto document = nullable_document.release_nonnull();
    return make<MatroskaDemuxer>(document);
}

Vector<Track> MatroskaDemuxer::get_tracks_for_type(TrackType type)
{
    Video::TrackEntry::TrackType matroska_track_type;

    switch (type) {
    case TrackType::Video:
        matroska_track_type = Video::TrackEntry::TrackType::Video;
        break;
    case TrackType::Audio:
        matroska_track_type = Video::TrackEntry::TrackType::Audio;
        break;
    case TrackType::Subtitles:
        matroska_track_type = Video::TrackEntry::TrackType::Subtitle;
        break;
    }

    Vector<Track> tracks;

    for (auto const& track_table_entry : m_document->tracks()) {
        auto const& track_entry = track_table_entry.value;
        if (matroska_track_type == track_entry->track_type())
            tracks.append(Track(type, track_entry->track_number()));
    }

    // FIXME: Sort the vector, presumably the hashtable will not have a consistent order.
    return tracks;
}

ErrorOr<MatroskaDemuxer::TrackStatus*> MatroskaDemuxer::get_track_status(Track track)
{
    if (!m_track_statuses.contains(track))
        TRY(m_track_statuses.try_set(track, TrackStatus()));

    return &m_track_statuses.get(track).release_value();
}

DecoderErrorOr<void> MatroskaDemuxer::seek_to_most_recent_keyframe(Track track, size_t timestamp)
{
    if (timestamp == 0) {
        auto track_status = DECODER_TRY_ALLOC(get_track_status(track));
        track_status->m_cluster_index = 0;
        track_status->m_block_index = 0;
        track_status->m_frame_index = 0;
        return {};
    }

    return DecoderError::not_implemented();
}

DecoderErrorOr<NonnullOwnPtr<Sample>> MatroskaDemuxer::get_next_sample_for_track(Track track)
{
    auto track_status = DECODER_TRY_ALLOC(get_track_status(track));

    for (; track_status->m_cluster_index < m_document->clusters().size(); track_status->m_cluster_index++) {
        auto const& cluster = m_document->clusters()[track_status->m_cluster_index];
        for (; track_status->m_block_index < cluster.blocks().size(); track_status->m_block_index++) {
            auto const& block = cluster.blocks()[track_status->m_block_index];
            if (block.track_number() != track.identifier())
                continue;
            if (track_status->m_frame_index < block.frame_count()) {
                switch (track.type()) {
                case TrackType::Video: {
                    // FIXME: This makes a copy of the sample, which shouldn't be necessary.
                    //        Matroska should make a RefPtr<ByteBuffer>, probably.
                    auto cicp = m_document->track_for_track_number(track.identifier())->video_track()->color_format.to_cicp();
                    Time timestamp = Time::from_nanoseconds((cluster.timestamp() + block.timestamp()) * m_document->segment_information()->timestamp_scale());
                    return make<VideoSample>(block.frame(track_status->m_frame_index++), cicp, timestamp);
                }
                default:
                    return DecoderError::not_implemented();
                }
            }
            track_status->m_frame_index = 0;
        }
        track_status->m_block_index = 0;
    }
    return DecoderError::with_description(DecoderErrorCategory::EndOfStream, "End of stream reached."sv);
}

Time MatroskaDemuxer::duration()
{
    if (!m_document->segment_information().has_value())
        return Time::zero();
    if (!m_document->segment_information()->duration().has_value())
        return Time::zero();
    return Time::from_nanoseconds(m_document->segment_information()->duration().value() * m_document->segment_information()->timestamp_scale());
}

}
