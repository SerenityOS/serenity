/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibCore/Timer.h>
#include <LibVideo/MatroskaReader.h>
#include <LibVideo/VP9/Decoder.h>

#include "MatroskaDemuxer.h"
#include "PlaybackManager.h"

namespace Video {

// We post DecoderErrors to the event queue to be handled, since some will occur off the main thread.
#define TRY_OR_POST_ERROR_AND_RETURN(expression, return_value)                                         \
    ({                                                                                                 \
        auto _temporary_result = ((expression));                                                       \
        if (_temporary_result.is_error()) {                                                            \
            dbgln("Playback error encountered: {}", _temporary_result.error().string_literal());       \
            m_main_loop.post_event(*this, make<DecoderErrorEvent>(_temporary_result.release_error())); \
            return return_value;                                                                       \
        }                                                                                              \
        _temporary_result.release_value();                                                             \
    })

#define TRY_OR_POST_ERROR(expression) TRY_OR_POST_ERROR_AND_RETURN(expression, )

DecoderErrorOr<NonnullRefPtr<PlaybackManager>> PlaybackManager::from_file(Object* event_handler, StringView filename)
{
    NonnullOwnPtr<Demuxer> demuxer = TRY(MatroskaDemuxer::from_file(filename));
    auto video_tracks = demuxer->get_tracks_for_type(TrackType::Video);
    if (video_tracks.is_empty())
        return DecoderError::with_description(DecoderErrorCategory::Invalid, "No video track is present"sv);
    auto track = video_tracks[0];

    dbgln_if(PLAYBACK_MANAGER_DEBUG, "Selecting video track number {}", track.identifier());

    NonnullOwnPtr<VideoDecoder> decoder = make<VP9::Decoder>();
    return PlaybackManager::construct(event_handler, demuxer, track, decoder);
}

PlaybackManager::PlaybackManager(Object* event_handler, NonnullOwnPtr<Demuxer>& demuxer, Track video_track, NonnullOwnPtr<VideoDecoder>& decoder)
    : Object(event_handler)
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
        auto old_status = m_status;
        m_status = status;
        dbgln_if(PLAYBACK_MANAGER_DEBUG, "Set playback status from {} to {}", playback_status_to_string(old_status), playback_status_to_string(m_status));

        if (status == PlaybackStatus::Playing) {
            if (old_status == PlaybackStatus::Stopped) {
                restart_playback();
                m_frame_queue->clear();
                m_skipped_frames = 0;
            }
            m_last_present_in_real_time = Time::now_monotonic();
            m_present_timer->start();
        } else {
            m_last_present_in_media_time = current_playback_time();
            m_last_present_in_real_time = Time::zero();
            m_present_timer->stop();
        }

        m_main_loop.post_event(*this, make<PlaybackStatusChangeEvent>(status, old_status));
    }
}

void PlaybackManager::event(Core::Event& event)
{
    if (event.type() == DecoderErrorOccurred) {
        auto& error_event = static_cast<DecoderErrorEvent&>(event);
        VERIFY(error_event.error().category() != DecoderErrorCategory::EndOfStream);
    }

    // Allow events to bubble up in all cases.
    event.ignore();
}

void PlaybackManager::resume_playback()
{
    set_playback_status(PlaybackStatus::Playing);
}

void PlaybackManager::pause_playback()
{
    set_playback_status(PlaybackStatus::Paused);
}

bool PlaybackManager::prepare_next_frame()
{
    if (m_next_frame.has_value())
        return true;
    if (m_frame_queue->is_empty())
        return false;
    auto frame_item = m_frame_queue->dequeue();
    m_next_frame.emplace(frame_item);
    m_decode_timer->start();
    return true;
}

Time PlaybackManager::current_playback_time()
{
    if (is_playing())
        return m_last_present_in_media_time + (Time::now_monotonic() - m_last_present_in_real_time);
    return m_last_present_in_media_time;
}

Time PlaybackManager::duration()
{
    return m_demuxer->duration();
}

void PlaybackManager::update_presented_frame()
{
    bool out_of_queued_frames = false;
    Optional<FrameQueueItem> frame_item_to_display;

    while (true) {
        out_of_queued_frames = out_of_queued_frames || !prepare_next_frame();
        if (out_of_queued_frames)
            break;
        VERIFY(m_next_frame.has_value());
        if (m_next_frame->timestamp > current_playback_time() || m_next_frame->is_eos_marker())
            break;

        if (frame_item_to_display.has_value()) {
            dbgln_if(PLAYBACK_MANAGER_DEBUG, "At {}ms: Dropped frame with timestamp {}ms for the next at {}ms", current_playback_time().to_milliseconds(), frame_item_to_display->timestamp.to_milliseconds(), m_next_frame->timestamp.to_milliseconds());
            m_skipped_frames++;
        }
        frame_item_to_display = m_next_frame.release_value();
    }

    if (!out_of_queued_frames && frame_item_to_display.has_value()) {
        m_main_loop.post_event(*this, make<VideoFramePresentEvent>(frame_item_to_display->bitmap));
        m_last_present_in_media_time = current_playback_time();
        m_last_present_in_real_time = Time::now_monotonic();
        frame_item_to_display.clear();
    }

    if (frame_item_to_display.has_value()) {
        VERIFY(!m_next_frame.has_value());
        m_next_frame = frame_item_to_display;
        dbgln_if(PLAYBACK_MANAGER_DEBUG, "Set next frame back to dequeued item at timestamp {}ms", m_next_frame->timestamp.to_milliseconds());
    }

    if (!is_playing())
        return;

    if (!out_of_queued_frames) {
        if (m_next_frame->is_eos_marker()) {
            set_playback_status(PlaybackStatus::Stopped);
            m_next_frame.clear();
            return;
        }

        auto frame_time_ms = (m_next_frame.value().timestamp - current_playback_time()).to_milliseconds();
        VERIFY(frame_time_ms <= NumericLimits<int>::max());
        dbgln_if(PLAYBACK_MANAGER_DEBUG, "Time until next frame is {}ms", frame_time_ms);
        m_present_timer->start(max(static_cast<int>(frame_time_ms), 0));
        return;
    }

    set_playback_status(PlaybackStatus::Buffering);
    m_decode_timer->start();
}

void PlaybackManager::restart_playback()
{
    m_last_present_in_media_time = Time::zero();
    m_last_present_in_real_time = Time::zero();
    TRY_OR_POST_ERROR(m_demuxer->seek_to_most_recent_keyframe(m_selected_video_track, 0));
}

bool PlaybackManager::decode_and_queue_one_sample()
{
    if (m_frame_queue->size() >= FRAME_BUFFER_COUNT)
        return false;
#if PLAYBACK_MANAGER_DEBUG
    auto start_time = Time::now_monotonic();
#endif

    auto frame_sample_result = m_demuxer->get_next_video_sample_for_track(m_selected_video_track);
    if (frame_sample_result.is_error()) {
        if (frame_sample_result.error().category() == DecoderErrorCategory::EndOfStream) {
            m_frame_queue->enqueue(FrameQueueItem::eos_marker());
            return false;
        }
        m_main_loop.post_event(*this, make<DecoderErrorEvent>(frame_sample_result.release_error()));
        return false;
    }
    auto frame_sample = frame_sample_result.release_value();

    TRY_OR_POST_ERROR_AND_RETURN(m_decoder->receive_sample(frame_sample->data()), false);
    auto decoded_frame = TRY_OR_POST_ERROR_AND_RETURN(m_decoder->get_decoded_frame(), false);

    auto& cicp = decoded_frame->cicp();
    cicp.adopt_specified_values(frame_sample->container_cicp());
    cicp.default_code_points_if_unspecified({ Video::ColorPrimaries::BT709, Video::TransferCharacteristics::BT709, Video::MatrixCoefficients::BT709, Video::ColorRange::Studio });

    auto bitmap = TRY_OR_POST_ERROR_AND_RETURN(decoded_frame->to_bitmap(), false);
    m_frame_queue->enqueue(FrameQueueItem { bitmap, frame_sample->timestamp() });

#if PLAYBACK_MANAGER_DEBUG
    auto end_time = Time::now_monotonic();
    dbgln("Decoding took {}ms", (end_time - start_time).to_milliseconds());
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
    if (is_buffering())
        m_decode_timer->start();
}

}
