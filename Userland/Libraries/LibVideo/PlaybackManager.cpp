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

#define TRY_OR_FATAL_ERROR(expression)                                                               \
    ({                                                                                               \
        auto&& _fatal_expression = (expression);                                                     \
        if (_fatal_expression.is_error()) {                                                          \
            dispatch_fatal_error(_fatal_expression.release_error());                                 \
            return;                                                                                  \
        }                                                                                            \
        static_assert(!::AK::Detail::IsLvalueReference<decltype(_fatal_expression.release_value())>, \
            "Do not return a reference from a fallible expression");                                 \
        _fatal_expression.release_value();                                                           \
    })

class DefaultPlaybackTimer final : public PlaybackTimer {
public:
    static ErrorOr<NonnullOwnPtr<DefaultPlaybackTimer>> create(int interval_ms, Function<void()>&& timeout_handler)
    {
        auto timer = TRY(Core::Timer::create_single_shot(interval_ms, move(timeout_handler)));
        return adopt_nonnull_own_or_enomem(new (nothrow) DefaultPlaybackTimer(move(timer)));
    }

    virtual void start() override { m_timer->start(); }
    virtual void start(int interval_ms) override { m_timer->start(interval_ms); }

private:
    explicit DefaultPlaybackTimer(NonnullRefPtr<Core::Timer> timer)
        : m_timer(move(timer))
    {
    }

    NonnullRefPtr<Core::Timer> m_timer;
};

DecoderErrorOr<NonnullOwnPtr<PlaybackManager>> PlaybackManager::from_file(StringView filename, PlaybackTimerCreator playback_timer_creator)
{
    NonnullOwnPtr<Demuxer> demuxer = TRY(Matroska::MatroskaDemuxer::from_file(filename));
    auto video_tracks = TRY(demuxer->get_tracks_for_type(TrackType::Video));
    if (video_tracks.is_empty())
        return DecoderError::with_description(DecoderErrorCategory::Invalid, "No video track is present"sv);
    auto track = video_tracks[0];

    dbgln_if(PLAYBACK_MANAGER_DEBUG, "Selecting video track number {}", track.identifier());

    return make<PlaybackManager>(demuxer, track, make<VP9::Decoder>(), move(playback_timer_creator));
}

PlaybackManager::PlaybackManager(NonnullOwnPtr<Demuxer>& demuxer, Track video_track, NonnullOwnPtr<VideoDecoder>&& decoder, PlaybackTimerCreator playback_timer_creator)
    : m_demuxer(move(demuxer))
    , m_selected_video_track(video_track)
    , m_decoder(move(decoder))
    , m_frame_queue(make<VideoFrameQueue>())
    , m_playback_handler(make<StartingStateHandler>(*this, false))
{
    if (playback_timer_creator) {
        m_present_timer = playback_timer_creator(0, [&] { timer_callback(); }).release_value_but_fixme_should_propagate_errors();
        m_decode_timer = playback_timer_creator(0, [&] { on_decode_timer(); }).release_value_but_fixme_should_propagate_errors();
    } else {
        m_present_timer = DefaultPlaybackTimer::create(0, [&] { timer_callback(); }).release_value_but_fixme_should_propagate_errors();
        m_decode_timer = DefaultPlaybackTimer::create(0, [&] { on_decode_timer(); }).release_value_but_fixme_should_propagate_errors();
    }

    TRY_OR_FATAL_ERROR(m_playback_handler->on_enter());
}

void PlaybackManager::resume_playback()
{
    dbgln_if(PLAYBACK_MANAGER_DEBUG, "Resuming playback.");
    TRY_OR_FATAL_ERROR(m_playback_handler->play());
}

void PlaybackManager::pause_playback()
{
    dbgln_if(PLAYBACK_MANAGER_DEBUG, "Pausing playback.");
    if (!m_playback_handler->is_playing())
        warnln("Cannot pause.");
    TRY_OR_FATAL_ERROR(m_playback_handler->pause());
}

Time PlaybackManager::current_playback_time()
{
    return m_playback_handler->current_time();
}

Time PlaybackManager::duration()
{
    auto duration_result = m_demuxer->duration();
    if (duration_result.is_error())
        dispatch_decoder_error(duration_result.release_error());
    return duration_result.release_value();
}

void PlaybackManager::dispatch_fatal_error(Error error)
{
    dbgln_if(PLAYBACK_MANAGER_DEBUG, "Encountered fatal error: {}", error.string_literal());
    // FIXME: For threading, this will have to use a pre-allocated event to send to the main loop
    //        to be able to gracefully handle OOM.
    if (on_fatal_playback_error)
        on_fatal_playback_error(move(error));
}

void PlaybackManager::dispatch_decoder_error(DecoderError error)
{
    switch (error.category()) {
    case DecoderErrorCategory::EndOfStream:
        dbgln_if(PLAYBACK_MANAGER_DEBUG, "{}", error.string_literal());
        TRY_OR_FATAL_ERROR(m_playback_handler->stop());
        break;
    default:
        dbgln("Playback error encountered: {}", error.string_literal());
        TRY_OR_FATAL_ERROR(m_playback_handler->stop());

        if (on_decoder_error)
            on_decoder_error(move(error));

        break;
    }
}

void PlaybackManager::dispatch_new_frame(RefPtr<Gfx::Bitmap> frame)
{
    if (on_video_frame)
        on_video_frame(move(frame));
}

bool PlaybackManager::dispatch_frame_queue_item(FrameQueueItem&& item)
{
    if (item.is_error()) {
        dispatch_decoder_error(item.release_error());
        return true;
    }

    dbgln_if(PLAYBACK_MANAGER_DEBUG, "Sent frame for presentation");
    dispatch_new_frame(item.bitmap());
    return false;
}

void PlaybackManager::dispatch_state_change()
{
    if (on_playback_state_change)
        on_playback_state_change();
}

void PlaybackManager::timer_callback()
{
    TRY_OR_FATAL_ERROR(m_playback_handler->on_timer_callback());
}

void PlaybackManager::seek_to_timestamp(Time target_timestamp, SeekMode seek_mode)
{
    TRY_OR_FATAL_ERROR(m_playback_handler->seek(target_timestamp, seek_mode));
}

Optional<Time> PlaybackManager::seek_demuxer_to_most_recent_keyframe(Time timestamp, Optional<Time> earliest_available_sample)
{
    // FIXME: When the demuxer is getting samples off the main thread in the future, this needs to
    //        mutex so that seeking can't happen while that thread is getting a sample.
    auto result = m_demuxer->seek_to_most_recent_keyframe(m_selected_video_track, timestamp, move(earliest_available_sample));
    if (result.is_error())
        dispatch_decoder_error(result.release_error());
    return result.release_value();
}

void PlaybackManager::restart_playback()
{
    seek_to_timestamp(Time::zero());
}

void PlaybackManager::start_timer(int milliseconds)
{
    m_present_timer->start(milliseconds);
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

    auto enqueue_error = [&](DecoderError&& error, Time timestamp) {
        dbgln_if(PLAYBACK_MANAGER_DEBUG, "Enqueued decoder error: {}", error.string_literal());
        m_frame_queue->enqueue(FrameQueueItem::error_marker(move(error), timestamp));
    };

#define TRY_OR_ENQUEUE_ERROR(expression, timestamp)                                                  \
    ({                                                                                               \
        auto&& _temporary_result = (expression);                                                     \
        if (_temporary_result.is_error()) {                                                          \
            enqueue_error(_temporary_result.release_error(), (timestamp));                           \
            return false;                                                                            \
        }                                                                                            \
        static_assert(!::AK::Detail::IsLvalueReference<decltype(_temporary_result.release_value())>, \
            "Do not return a reference from a fallible expression");                                 \
        _temporary_result.release_value();                                                           \
    })

    auto frame_sample = TRY_OR_ENQUEUE_ERROR(m_demuxer->get_next_video_sample_for_track(m_selected_video_track), Time::min());
    OwnPtr<VideoFrame> decoded_frame = nullptr;
    while (!decoded_frame) {
        TRY_OR_ENQUEUE_ERROR(m_decoder->receive_sample(frame_sample->data()), frame_sample->timestamp());

        while (true) {
            auto frame_result = m_decoder->get_decoded_frame();

            if (frame_result.is_error()) {
                if (frame_result.error().category() == DecoderErrorCategory::NeedsMoreInput)
                    break;

                enqueue_error(frame_result.release_error(), frame_sample->timestamp());
                return false;
            }

            decoded_frame = frame_result.release_value();
            VERIFY(decoded_frame);
        }
    }

    auto& cicp = decoded_frame->cicp();
    cicp.adopt_specified_values(frame_sample->container_cicp());
    cicp.default_code_points_if_unspecified({ ColorPrimaries::BT709, TransferCharacteristics::BT709, MatrixCoefficients::BT709, VideoFullRangeFlag::Studio });

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

    auto bitmap = TRY_OR_ENQUEUE_ERROR(decoded_frame->to_bitmap(), frame_sample->timestamp());
    m_frame_queue->enqueue(FrameQueueItem::frame(bitmap, frame_sample->timestamp()));

#if PLAYBACK_MANAGER_DEBUG
    auto end_time = Time::now_monotonic();
    dbgln("Decoding sample at {}ms took {}ms, queue contains {} items", frame_sample->timestamp().to_milliseconds(), (end_time - start_time).to_milliseconds(), m_frame_queue->size());
#endif

    return true;
}

void PlaybackManager::on_decode_timer()
{
    if (!decode_and_queue_one_sample()) {
        // Note: When threading is implemented, this must be dispatched via an event loop.
        TRY_OR_FATAL_ERROR(m_playback_handler->on_buffer_filled());
        return;
    }

    // Continually decode until buffering is complete
    m_decode_timer->start(0);
}

Time PlaybackManager::PlaybackStateHandler::current_time() const
{
    return m_manager.m_last_present_in_media_time;
}

ErrorOr<void> PlaybackManager::PlaybackStateHandler::seek(Time target_timestamp, SeekMode seek_mode)
{
    return replace_handler_and_delete_this<SeekingStateHandler>(is_playing(), target_timestamp, seek_mode);
}

ErrorOr<void> PlaybackManager::PlaybackStateHandler::stop()
{
    return replace_handler_and_delete_this<StoppedStateHandler>();
}

template<class T, class... Args>
ErrorOr<void> PlaybackManager::PlaybackStateHandler::replace_handler_and_delete_this(Args... args)
{
    auto temp_handler = TRY(adopt_nonnull_own_or_enomem<PlaybackStateHandler>(new (nothrow) T(m_manager, args...)));
    m_manager.m_playback_handler.swap(temp_handler);
#if PLAYBACK_MANAGER_DEBUG
    m_has_exited = true;
    dbgln("Changing state from {} to {}", temp_handler->name(), m_manager.m_playback_handler->name());
#endif
    m_manager.dispatch_state_change();
    TRY(m_manager.m_playback_handler->on_enter());
    return {};
}

PlaybackManager& PlaybackManager::PlaybackStateHandler::manager() const
{
#if PLAYBACK_MANAGER_DEBUG
    VERIFY(!m_has_exited);
#endif
    return m_manager;
}

class PlaybackManager::ResumingStateHandler : public PlaybackManager::PlaybackStateHandler {
public:
    ResumingStateHandler(PlaybackManager& manager, bool playing)
        : PlaybackStateHandler(manager)
        , m_playing(playing)
    {
    }
    ~ResumingStateHandler() override = default;

protected:
    ErrorOr<void> assume_next_state()
    {
        if (!m_playing)
            return replace_handler_and_delete_this<PausedStateHandler>();
        return replace_handler_and_delete_this<PlayingStateHandler>();
    }

    ErrorOr<void> play() override
    {
        m_playing = true;
        manager().dispatch_state_change();
        return {};
    }
    bool is_playing() override { return m_playing; }
    ErrorOr<void> pause() override
    {
        m_playing = false;
        manager().dispatch_state_change();
        return {};
    }

    bool m_playing { false };
};

class PlaybackManager::StartingStateHandler : public PlaybackManager::ResumingStateHandler {
    using PlaybackManager::ResumingStateHandler::ResumingStateHandler;

    ErrorOr<void> on_enter() override
    {
        return on_timer_callback();
    }

    StringView name() override { return "Starting"sv; }

    ErrorOr<void> on_timer_callback() override
    {
        // Once we're threaded, instead of checking for the count here we can just mutex
        // in the queue until we display the first and then again for the second to store it.
        if (manager().m_frame_queue->size() < 3) {
            manager().m_decode_timer->start(0);
            manager().start_timer(0);
            return {};
        }

        auto frame_to_display = manager().m_frame_queue->dequeue();
        manager().m_last_present_in_media_time = frame_to_display.timestamp();
        if (manager().dispatch_frame_queue_item(move(frame_to_display)))
            return {};

        manager().m_next_frame.emplace(manager().m_frame_queue->dequeue());
        manager().m_decode_timer->start(0);
        dbgln_if(PLAYBACK_MANAGER_DEBUG, "Displayed frame at {}ms, emplaced second frame at {}ms, finishing start now", manager().m_last_present_in_media_time.to_milliseconds(), manager().m_next_frame->timestamp().to_milliseconds());
        if (!m_playing)
            return replace_handler_and_delete_this<PausedStateHandler>();
        return replace_handler_and_delete_this<PlayingStateHandler>();
    }

    ErrorOr<void> play() override
    {
        m_playing = true;
        return {};
    }
    bool is_playing() override { return m_playing; };
    ErrorOr<void> pause() override
    {
        m_playing = false;
        return {};
    }

    bool m_playing { false };
};

class PlaybackManager::PlayingStateHandler : public PlaybackManager::PlaybackStateHandler {
public:
    PlayingStateHandler(PlaybackManager& manager)
        : PlaybackStateHandler(manager)
    {
    }
    ~PlayingStateHandler() override = default;

private:
    ErrorOr<void> on_enter() override
    {
        m_last_present_in_real_time = Time::now_monotonic();
        return on_timer_callback();
    }

    StringView name() override { return "Playing"sv; }

    bool is_playing() override { return true; };
    ErrorOr<void> pause() override
    {
        manager().m_last_present_in_media_time = current_time();
        return replace_handler_and_delete_this<PausedStateHandler>();
    }
    ErrorOr<void> buffer() override
    {
        manager().m_last_present_in_media_time = current_time();
        return replace_handler_and_delete_this<BufferingStateHandler>(true);
    }

    Time current_time() const override
    {
        return manager().m_last_present_in_media_time + (Time::now_monotonic() - m_last_present_in_real_time);
    }

    ErrorOr<void> on_timer_callback() override
    {
        auto set_presentation_timer = [&]() {
            auto frame_time_ms = (manager().m_next_frame->timestamp() - current_time()).to_milliseconds();
            VERIFY(frame_time_ms <= NumericLimits<int>::max());
            dbgln_if(PLAYBACK_MANAGER_DEBUG, "Time until next frame is {}ms", frame_time_ms);
            manager().start_timer(max(static_cast<int>(frame_time_ms), 0));
        };

        if (manager().m_next_frame.has_value() && current_time() < manager().m_next_frame->timestamp()) {
            dbgln_if(PLAYBACK_MANAGER_DEBUG, "Current time {}ms is too early to present the next frame at {}ms, delaying", current_time().to_milliseconds(), manager().m_next_frame->timestamp().to_milliseconds());
            set_presentation_timer();
            return {};
        }

        Optional<FrameQueueItem> future_frame_item;
        bool should_present_frame = false;

        // Skip frames until we find a frame past the current playback time, and keep the one that precedes it to display.
        while (!manager().m_frame_queue->is_empty()) {
            future_frame_item.emplace(manager().m_frame_queue->dequeue());
            manager().m_decode_timer->start(0);

            if (future_frame_item->timestamp() >= current_time() || future_frame_item->timestamp() == FrameQueueItem::no_timestamp) {
                dbgln_if(PLAYBACK_MANAGER_DEBUG, "Should present frame, future {} is error or after {}ms", future_frame_item->debug_string(), current_time().to_milliseconds());
                should_present_frame = true;
                break;
            }

            if (manager().m_next_frame.has_value()) {
                dbgln_if(PLAYBACK_MANAGER_DEBUG, "At {}ms: Dropped {} in favor of {}", current_time().to_milliseconds(), manager().m_next_frame->debug_string(), future_frame_item->debug_string());
                manager().m_skipped_frames++;
            }
            manager().m_next_frame.emplace(future_frame_item.release_value());
        }

        // If we don't have both of these items, we can't present, since we need to set a timer for
        // the next frame. Check if we need to buffer based on the current state.
        if (!manager().m_next_frame.has_value() || !future_frame_item.has_value()) {
#if PLAYBACK_MANAGER_DEBUG
            StringBuilder debug_string_builder;
            debug_string_builder.append("We don't have "sv);
            if (!manager().m_next_frame.has_value()) {
                debug_string_builder.append("a frame to present"sv);
                if (!future_frame_item.has_value())
                    debug_string_builder.append(" or a future frame"sv);
            } else {
                debug_string_builder.append("a future frame"sv);
            }
            debug_string_builder.append(", checking for error and buffering"sv);
            dbgln_if(PLAYBACK_MANAGER_DEBUG, debug_string_builder.to_deprecated_string());
#endif
            if (future_frame_item.has_value()) {
                if (future_frame_item->is_error()) {
                    manager().dispatch_decoder_error(future_frame_item.release_value().release_error());
                    return {};
                }
                manager().m_next_frame.emplace(future_frame_item.release_value());
            }
            TRY(buffer());
            return {};
        }

        // If we have a frame, send it for presentation.
        if (should_present_frame) {
            auto now = Time::now_monotonic();
            manager().m_last_present_in_media_time += now - m_last_present_in_real_time;
            m_last_present_in_real_time = now;

            if (manager().dispatch_frame_queue_item(manager().m_next_frame.release_value()))
                return {};
        }

        // Now that we've presented the current frame, we can throw whatever error is next in queue.
        // This way, we always display a frame before the stream ends, and should also show any frames
        // we already had when a real error occurs.
        if (future_frame_item->is_error()) {
            manager().dispatch_decoder_error(future_frame_item.release_value().release_error());
            return {};
        }

        // The future frame item becomes the next one to present.
        manager().m_next_frame.emplace(future_frame_item.release_value());
        set_presentation_timer();
        return {};
    }

    Time m_last_present_in_real_time = Time::zero();
};

class PlaybackManager::PausedStateHandler : public PlaybackManager::PlaybackStateHandler {
public:
    PausedStateHandler(PlaybackManager& manager)
        : PlaybackStateHandler(manager)
    {
    }
    ~PausedStateHandler() override = default;

private:
    StringView name() override { return "Paused"sv; }

    ErrorOr<void> play() override
    {
        return replace_handler_and_delete_this<PlayingStateHandler>();
    }
    bool is_playing() override { return false; };
};

class PlaybackManager::BufferingStateHandler : public PlaybackManager::ResumingStateHandler {
    using PlaybackManager::ResumingStateHandler::ResumingStateHandler;

    ErrorOr<void> on_enter() override
    {
        manager().m_decode_timer->start(0);
        return {};
    }

    StringView name() override { return "Buffering"sv; }

    ErrorOr<void> on_buffer_filled() override
    {
        return assume_next_state();
    }
};

class PlaybackManager::SeekingStateHandler : public PlaybackManager::ResumingStateHandler {
public:
    SeekingStateHandler(PlaybackManager& manager, bool playing, Time target_timestamp, SeekMode seek_mode)
        : ResumingStateHandler(manager, playing)
        , m_target_timestamp(target_timestamp)
        , m_seek_mode(seek_mode)
    {
    }
    ~SeekingStateHandler() override = default;

private:
    ErrorOr<void> on_enter() override
    {
        auto earliest_available_sample = manager().m_last_present_in_media_time;
        if (manager().m_next_frame.has_value() && manager().m_next_frame->timestamp() != FrameQueueItem::no_timestamp) {
            earliest_available_sample = min(earliest_available_sample, manager().m_next_frame->timestamp());
        }
        auto keyframe_timestamp = manager().seek_demuxer_to_most_recent_keyframe(m_target_timestamp, earliest_available_sample);

#if PLAYBACK_MANAGER_DEBUG
        auto seek_mode_name = m_seek_mode == SeekMode::Accurate ? "Accurate"sv : "Fast"sv;
        if (keyframe_timestamp.has_value()) {
            dbgln("{} seeking to timestamp target {}ms, selected keyframe at {}ms", seek_mode_name, m_target_timestamp.to_milliseconds(), keyframe_timestamp->to_milliseconds());
        } else {
            dbgln("{} seeking to timestamp target {}ms, demuxer kept its iterator position", seek_mode_name, m_target_timestamp.to_milliseconds());
        }
#endif

        if (m_seek_mode == SeekMode::Fast) {
            m_target_timestamp = keyframe_timestamp.value_or(earliest_available_sample);
        }

        if (keyframe_timestamp.has_value()) {
            dbgln_if(PLAYBACK_MANAGER_DEBUG, "Timestamp is earlier than current media time, clearing queue");
            manager().m_frame_queue->clear();
            manager().m_next_frame.clear();
        } else if (m_target_timestamp >= manager().m_last_present_in_media_time && manager().m_next_frame.has_value() && manager().m_next_frame.value().timestamp() > m_target_timestamp) {
            manager().m_last_present_in_media_time = m_target_timestamp;
            return assume_next_state();
        }

        return skip_samples_until_timestamp();
    }

    ErrorOr<void> skip_samples_until_timestamp()
    {
        while (!manager().m_frame_queue->is_empty()) {
            auto item = manager().m_frame_queue->dequeue();
            manager().m_decode_timer->start(0);

            dbgln_if(PLAYBACK_MANAGER_DEBUG, "Dequeuing frame at {}ms and comparing to seek target {}ms", item.timestamp().to_milliseconds(), m_target_timestamp.to_milliseconds());
            if (item.timestamp() > m_target_timestamp || item.timestamp() == FrameQueueItem::no_timestamp) {
                // Fast seeking will result in an equal timestamp, so we can exit as soon as we see the next frame.
                if (manager().m_next_frame.has_value()) {
                    manager().m_last_present_in_media_time = m_target_timestamp;

                    if (manager().dispatch_frame_queue_item(manager().m_next_frame.release_value()))
                        return {};
                }

                manager().m_next_frame.emplace(item);

                dbgln_if(PLAYBACK_MANAGER_DEBUG, "Exiting seek to {} state at {}ms", m_playing ? "Playing" : "Paused", manager().m_last_present_in_media_time.to_milliseconds());
                return assume_next_state();
            }
            manager().m_next_frame.emplace(item);
        }

        dbgln_if(PLAYBACK_MANAGER_DEBUG, "Frame queue is empty while seeking, waiting for buffer fill.");
        manager().m_decode_timer->start(0);
        return {};
    }

    StringView name() override { return "Seeking"sv; }

    ErrorOr<void> seek(Time target_timestamp, SeekMode seek_mode) override
    {
        m_target_timestamp = target_timestamp;
        m_seek_mode = seek_mode;
        return on_enter();
    }

    Time current_time() const override
    {
        return m_target_timestamp;
    }

    // We won't need this override when threaded, the queue can pause us in on_enter().
    ErrorOr<void> on_buffer_filled() override
    {
        dbgln_if(PLAYBACK_MANAGER_DEBUG, "Buffer filled while seeking, dequeuing until timestamp.");
        return skip_samples_until_timestamp();
    }

    bool m_playing { false };
    Time m_target_timestamp { Time::zero() };
    SeekMode m_seek_mode { SeekMode::Accurate };
};

class PlaybackManager::StoppedStateHandler : public PlaybackManager::PlaybackStateHandler {
public:
    StoppedStateHandler(PlaybackManager& manager)
        : PlaybackStateHandler(manager)
    {
    }
    ~StoppedStateHandler() override = default;

private:
    ErrorOr<void> on_enter() override
    {
        return {};
    }

    StringView name() override { return "Stopped"sv; }

    ErrorOr<void> play() override
    {
        manager().m_next_frame.clear();
        manager().m_frame_queue->clear();
        auto start_timestamp = manager().seek_demuxer_to_most_recent_keyframe(Time::zero());
        VERIFY(start_timestamp.has_value());
        manager().m_last_present_in_media_time = start_timestamp.release_value();
        return replace_handler_and_delete_this<StartingStateHandler>(true);
    }
    bool is_playing() override { return false; };
};

}
