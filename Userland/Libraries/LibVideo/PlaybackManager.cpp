/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibCore/Timer.h>
#include <LibVideo/Containers/Matroska/MatroskaDemuxer.h>
#include <LibVideo/VP9/Decoder.h>

#include "PlaybackManager.h"

namespace Video {

DecoderErrorOr<NonnullOwnPtr<PlaybackManager>> PlaybackManager::from_file(Core::Object& event_handler, StringView filename)
{
    NonnullOwnPtr<Demuxer> demuxer = TRY(Matroska::MatroskaDemuxer::from_file(filename));
    auto video_tracks = TRY(demuxer->get_tracks_for_type(TrackType::Video));
    if (video_tracks.is_empty())
        return DecoderError::with_description(DecoderErrorCategory::Invalid, "No video track is present"sv);
    auto track = video_tracks[0];

    dbgln_if(PLAYBACK_MANAGER_DEBUG, "Selecting video track number {}", track.identifier());

    return make<PlaybackManager>(event_handler, demuxer, track, make<VP9::Decoder>());
}

PlaybackManager::PlaybackManager(Core::Object& event_handler, NonnullOwnPtr<Demuxer>& demuxer, Track video_track, NonnullOwnPtr<VideoDecoder>&& decoder)
    : m_event_handler(event_handler)
    , m_main_loop(Core::EventLoop::current())
    , m_demuxer(move(demuxer))
    , m_selected_video_track(video_track)
    , m_decoder(move(decoder))
    , m_frame_queue(make<VideoFrameQueue>())
    , m_present_timer(Core::Timer::construct())
    , m_decode_timer(Core::Timer::construct())
{
    m_present_timer->set_single_shot(true);
    m_present_timer->set_interval(0);
    m_present_timer->on_timeout = [&] { update_presented_frame(); };

    m_decode_timer->set_single_shot(true);
    m_decode_timer->set_interval(0);
    m_decode_timer->on_timeout = [&] { on_decode_timer(); };
}

void PlaybackManager::set_playback_status(PlaybackStatus status)
{
    if (status != m_status) {
        bool was_stopped = is_stopped();
        auto old_status = m_status;
        m_status = status;
        dbgln_if(PLAYBACK_MANAGER_DEBUG, "Set playback status from {} to {}", playback_status_to_string(old_status), playback_status_to_string(m_status));

        if (m_status == PlaybackStatus::Playing) {
            if (was_stopped)
                restart_playback();
            m_last_present_in_real_time = Time::now_monotonic();
            m_present_timer->start(0);
        } else if (!is_seeking()) {
            m_last_present_in_media_time = current_playback_time();
            m_last_present_in_real_time = Time::zero();
            m_present_timer->stop();
        }

        m_main_loop.post_event(m_event_handler, make<PlaybackStatusChangeEvent>(status, old_status));
    }
}

void PlaybackManager::resume_playback()
{
    if (is_seeking()) {
        set_playback_status(PlaybackStatus::SeekingPlaying);
        return;
    }
    set_playback_status(PlaybackStatus::Playing);
}

void PlaybackManager::pause_playback()
{
    if (is_seeking()) {
        set_playback_status(PlaybackStatus::SeekingPaused);
        return;
    }
    set_playback_status(PlaybackStatus::Paused);
}

Time PlaybackManager::current_playback_time()
{
    if (is_seeking())
        return m_seek_to_media_time;
    VERIFY(!m_last_present_in_media_time.is_negative());
    if (m_status == PlaybackStatus::Playing)
        return m_last_present_in_media_time + (Time::now_monotonic() - m_last_present_in_real_time);
    return m_last_present_in_media_time;
}

Time PlaybackManager::duration()
{
    auto duration_result = m_demuxer->duration();
    if (duration_result.is_error())
        on_decoder_error(duration_result.release_error());
    return duration_result.release_value();
}

void PlaybackManager::on_decoder_error(DecoderError error)
{
    // If we don't switch to playing/paused before stopping/becoming corrupted, the player will crash
    // due to the invalid playback time.
    if (is_seeking())
        end_seek();

    switch (error.category()) {
    case DecoderErrorCategory::EndOfStream:
        dbgln_if(PLAYBACK_MANAGER_DEBUG, "{}", error.string_literal());
        set_playback_status(PlaybackStatus::Stopped);
        break;
    default:
        dbgln("Playback error encountered: {}", error.string_literal());
        set_playback_status(PlaybackStatus::Corrupted);
        m_main_loop.post_event(m_event_handler, make<DecoderErrorEvent>(move(error)));
        break;
    }
}

void PlaybackManager::end_seek()
{
    dbgln_if(PLAYBACK_MANAGER_DEBUG, "We've finished seeking, set media time to seek time at {}ms and change status", m_seek_to_media_time.to_milliseconds());
    VERIFY(!m_seek_to_media_time.is_negative());
    m_last_present_in_media_time = m_seek_to_media_time;
    m_seek_to_media_time = Time::min();
    if (m_status == PlaybackStatus::SeekingPlaying) {
        set_playback_status(PlaybackStatus::Playing);
        return;
    }

    VERIFY(m_status == PlaybackStatus::SeekingPaused);
    set_playback_status(PlaybackStatus::Paused);
}

void PlaybackManager::update_presented_frame()
{
    Optional<FrameQueueItem> future_frame_item;
    bool should_present_frame = false;

    // Skip frames until we find a frame past the current playback time, and keep the one that precedes it to display.
    while ((m_status == PlaybackStatus::Playing || is_seeking()) && !m_frame_queue->is_empty()) {
        future_frame_item.emplace(m_frame_queue->dequeue());
        m_decode_timer->start(0);

        if (future_frame_item->is_error() || future_frame_item->timestamp() >= current_playback_time()) {
            dbgln_if(PLAYBACK_MANAGER_DEBUG, "Should present frame, future {} is after {}ms", future_frame_item->debug_string(), current_playback_time().to_milliseconds());
            should_present_frame = true;
            break;
        }

        if (m_next_frame.has_value() && !is_seeking()) {
            dbgln_if(PLAYBACK_MANAGER_DEBUG, "At {}ms: Dropped {} in favor of {}", current_playback_time().to_milliseconds(), m_next_frame->debug_string(), future_frame_item->debug_string());
            m_skipped_frames++;
        }
        m_next_frame.emplace(future_frame_item.release_value());
    }

    // If we don't have both of these items, we can't present, since we need to set a timer for
    // the next frame. Check if we need to buffer based on the current state.
    if (!m_next_frame.has_value() || !future_frame_item.has_value()) {
#if PLAYBACK_MANAGER_DEBUG
        StringBuilder debug_string_builder;
        debug_string_builder.append("We don't have "sv);
        if (!m_next_frame.has_value()) {
            debug_string_builder.append("a frame to present"sv);
            if (!future_frame_item.has_value())
                debug_string_builder.append(" or a future frame"sv);
        } else {
            debug_string_builder.append("a future frame"sv);
        }
        debug_string_builder.append(", checking for error and buffering"sv);
        dbgln_if(PLAYBACK_MANAGER_DEBUG, debug_string_builder.to_string());
#endif
        if (future_frame_item.has_value()) {
            if (future_frame_item->is_error()) {
                on_decoder_error(future_frame_item.release_value().release_error());
                return;
            }
            m_next_frame.emplace(future_frame_item.release_value());
        }
        if (m_status == PlaybackStatus::Playing)
            set_playback_status(PlaybackStatus::Buffering);
        m_decode_timer->start(0);
        return;
    }

    // If we have a frame, send it for presentation.
    if (should_present_frame) {
        if (is_seeking())
            end_seek();
        else
            m_last_present_in_media_time = current_playback_time();
        m_last_present_in_real_time = Time::now_monotonic();
        m_main_loop.post_event(m_event_handler, make<VideoFramePresentEvent>(m_next_frame.value().bitmap()));
        dbgln_if(PLAYBACK_MANAGER_DEBUG, "Sent frame for presentation");
    }

    // Now that we've presented the current frame, we can throw whatever error is next in queue.
    // This way, we always display a frame before the stream ends, and should also show any frames
    // we already had when a real error occurs.
    if (future_frame_item->is_error()) {
        on_decoder_error(future_frame_item.release_value().release_error());
        return;
    }

    // The future frame item becomes the next one to present.
    m_next_frame.emplace(future_frame_item.release_value());

    if (m_status != PlaybackStatus::Playing) {
        dbgln_if(PLAYBACK_MANAGER_DEBUG, "We're not playing! Starting the decode timer");
        m_decode_timer->start(0);
        return;
    }

    auto frame_time_ms = (m_next_frame->timestamp() - current_playback_time()).to_milliseconds();
    VERIFY(frame_time_ms <= NumericLimits<int>::max());
    dbgln_if(PLAYBACK_MANAGER_DEBUG, "Time until next frame is {}ms", frame_time_ms);
    m_present_timer->start(max(static_cast<int>(frame_time_ms), 0));
}

void PlaybackManager::seek_to_timestamp(Time timestamp)
{
    dbgln_if(PLAYBACK_MANAGER_DEBUG, "Seeking to {}ms", timestamp.to_milliseconds());
    // FIXME: When the demuxer is getting samples off the main thread in the future, this needs to
    //        mutex so that seeking can't happen while that thread is getting a sample.
    auto result = m_demuxer->seek_to_most_recent_keyframe(m_selected_video_track, timestamp);
    if (result.is_error())
        on_decoder_error(result.release_error());

    if (is_playing())
        set_playback_status(PlaybackStatus::SeekingPlaying);
    else
        set_playback_status(PlaybackStatus::SeekingPaused);
    m_frame_queue->clear();
    m_next_frame.clear();
    m_skipped_frames = 0;
    if (m_seek_mode == SeekMode::Accurate)
        m_seek_to_media_time = timestamp;
    else
        m_seek_to_media_time = result.release_value();
    m_last_present_in_media_time = Time::min();
    m_last_present_in_real_time = Time::zero();
    m_present_timer->stop();
    m_decode_timer->start(0);
}

void PlaybackManager::restart_playback()
{
    seek_to_timestamp(Time::zero());
}

void PlaybackManager::post_decoder_error(DecoderError error)
{
    m_main_loop.post_event(m_event_handler, make<DecoderErrorEvent>(error));
}

bool PlaybackManager::decode_and_queue_one_sample()
{
    if (m_frame_queue->size() >= FRAME_BUFFER_COUNT) {
        dbgln_if(PLAYBACK_MANAGER_DEBUG, "Frame queue is full, stopping");
        return false;
    }
#if PLAYBACK_MANAGER_DEBUG
    auto start_time = Time::now_monotonic();
#endif

#define TRY_OR_ENQUEUE_ERROR(expression)                                                                                \
    ({                                                                                                                  \
        auto _temporary_result = ((expression));                                                                        \
        if (_temporary_result.is_error()) {                                                                             \
            dbgln_if(PLAYBACK_MANAGER_DEBUG, "Enqueued decoder error: {}", _temporary_result.error().string_literal()); \
            m_frame_queue->enqueue(FrameQueueItem::error_marker(_temporary_result.release_error()));                    \
            m_present_timer->start(0);                                                                                  \
            return false;                                                                                               \
        }                                                                                                               \
        _temporary_result.release_value();                                                                              \
    })

    auto frame_sample = TRY_OR_ENQUEUE_ERROR(m_demuxer->get_next_video_sample_for_track(m_selected_video_track));
    OwnPtr<VideoFrame> decoded_frame = nullptr;
    while (!decoded_frame) {
        TRY_OR_ENQUEUE_ERROR(m_decoder->receive_sample(frame_sample->data()));

        while (true) {
            auto frame_result = m_decoder->get_decoded_frame();

            if (frame_result.is_error()) {
                if (frame_result.error().category() == DecoderErrorCategory::NeedsMoreInput)
                    break;

                post_decoder_error(frame_result.release_error());
                return false;
            }

            decoded_frame = frame_result.release_value();
            VERIFY(decoded_frame);
        }
    }

    auto& cicp = decoded_frame->cicp();
    cicp.adopt_specified_values(frame_sample->container_cicp());
    cicp.default_code_points_if_unspecified({ ColorPrimaries::BT709, TransferCharacteristics::BT709, MatrixCoefficients::BT709, ColorRange::Studio });

    // BT.601, BT.709 and BT.2020 have a similar transfer function to sRGB, so other applications
    // (Chromium, VLC) forgo transfer characteristics conversion. We will emulate that behavior by
    // handling those as sRGB instead, which causes no transfer function change in the output,
    // unless display color management is later implemented.
    switch (cicp.transfer_characteristics()) {
    case TransferCharacteristics::BT601:
    case TransferCharacteristics::BT709:
    case TransferCharacteristics::BT2020BitDepth10:
    case TransferCharacteristics::BT2020BitDepth12:
        cicp.set_transfer_characteristics(TransferCharacteristics::SRGB);
        break;
    default:
        break;
    }

    auto bitmap = TRY_OR_ENQUEUE_ERROR(decoded_frame->to_bitmap());
    m_frame_queue->enqueue(FrameQueueItem::frame(bitmap, frame_sample->timestamp()));
    m_present_timer->start(0);

#if PLAYBACK_MANAGER_DEBUG
    auto end_time = Time::now_monotonic();
    dbgln("Decoding took {}ms, queue is {} items", (end_time - start_time).to_milliseconds(), m_frame_queue->size());
#endif

    return true;
}

void PlaybackManager::on_decode_timer()
{
    if (!decode_and_queue_one_sample() && is_buffering()) {
        set_playback_status(PlaybackStatus::Playing);
        return;
    }

    // Continually decode until buffering is complete
    if (is_buffering() || is_seeking())
        m_decode_timer->start(0);
}

}
