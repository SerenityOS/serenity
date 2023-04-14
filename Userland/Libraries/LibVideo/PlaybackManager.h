/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <AK/Function.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Queue.h>
#include <AK/Time.h>
#include <LibCore/SharedCircularQueue.h>
#include <LibGfx/Bitmap.h>
#include <LibThreading/ConditionVariable.h>
#include <LibThreading/Mutex.h>
#include <LibThreading/Thread.h>
#include <LibVideo/Containers/Demuxer.h>
#include <LibVideo/Containers/Matroska/Document.h>

#include "VideoDecoder.h"

namespace Video {

class FrameQueueItem {
public:
    static constexpr Time no_timestamp = Time::min();

    enum class Type {
        Frame,
        Error,
    };

    static FrameQueueItem frame(RefPtr<Gfx::Bitmap> bitmap, Time timestamp)
    {
        return FrameQueueItem(move(bitmap), timestamp);
    }

    static FrameQueueItem error_marker(DecoderError&& error, Time timestamp)
    {
        return FrameQueueItem(move(error), timestamp);
    }

    bool is_frame() const { return m_data.has<RefPtr<Gfx::Bitmap>>(); }
    RefPtr<Gfx::Bitmap> bitmap() const { return m_data.get<RefPtr<Gfx::Bitmap>>(); }
    Time timestamp() const { return m_timestamp; }

    bool is_error() const { return m_data.has<DecoderError>(); }
    DecoderError const& error() const { return m_data.get<DecoderError>(); }
    DecoderError release_error()
    {
        auto error = move(m_data.get<DecoderError>());
        m_data.set(Empty());
        return error;
    }

    DeprecatedString debug_string() const
    {
        if (is_error())
            return DeprecatedString::formatted("{} at {}ms", error().string_literal(), timestamp().to_milliseconds());
        return DeprecatedString::formatted("frame at {}ms", timestamp().to_milliseconds());
    }

private:
    FrameQueueItem(RefPtr<Gfx::Bitmap> bitmap, Time timestamp)
        : m_data(move(bitmap))
        , m_timestamp(timestamp)
    {
        VERIFY(m_timestamp != no_timestamp);
    }

    FrameQueueItem(DecoderError&& error, Time timestamp)
        : m_data(move(error))
        , m_timestamp(timestamp)
    {
    }

    Variant<Empty, RefPtr<Gfx::Bitmap>, DecoderError> m_data;
    Time m_timestamp;
};

static constexpr size_t FRAME_BUFFER_COUNT = 4;
using VideoFrameQueue = Queue<FrameQueueItem, FRAME_BUFFER_COUNT>;

class PlaybackTimer {
public:
    virtual ~PlaybackTimer() = default;

    virtual void start() = 0;
    virtual void start(int interval_ms) = 0;
};

enum class PlaybackState {
    Playing,
    Paused,
    Buffering,
    Seeking,
    Stopped,
};

class PlaybackManager {
public:
    enum class SeekMode {
        Accurate,
        Fast,
    };

    static constexpr SeekMode DEFAULT_SEEK_MODE = SeekMode::Accurate;

    using PlaybackTimerCreator = Function<ErrorOr<NonnullOwnPtr<PlaybackTimer>>(int, Function<void()>)>;

    static DecoderErrorOr<NonnullOwnPtr<PlaybackManager>> from_file(StringView file, PlaybackTimerCreator = nullptr);
    static DecoderErrorOr<NonnullOwnPtr<PlaybackManager>> from_data(ReadonlyBytes data, PlaybackTimerCreator = nullptr);

    PlaybackManager(NonnullOwnPtr<Demuxer>& demuxer, Track video_track, NonnullOwnPtr<VideoDecoder>&& decoder, PlaybackTimerCreator);

    void resume_playback();
    void pause_playback();
    void restart_playback();
    void seek_to_timestamp(Time, SeekMode = DEFAULT_SEEK_MODE);
    bool is_playing() const
    {
        return m_playback_handler->is_playing();
    }
    PlaybackState get_state() const
    {
        return m_playback_handler->get_state();
    }

    u64 number_of_skipped_frames() const { return m_skipped_frames; }

    Time current_playback_time();
    Time duration();

    Function<void(RefPtr<Gfx::Bitmap>)> on_video_frame;
    Function<void()> on_playback_state_change;
    Function<void(DecoderError)> on_decoder_error;
    Function<void(Error)> on_fatal_playback_error;

    Track const& selected_video_track() const { return m_selected_video_track; }

private:
    class PlaybackStateHandler;
    // Abstract class to allow resuming play/pause after the state is completed.
    class ResumingStateHandler;
    class PlayingStateHandler;
    class PausedStateHandler;
    class BufferingStateHandler;
    class SeekingStateHandler;
    class StoppedStateHandler;

    static DecoderErrorOr<NonnullOwnPtr<PlaybackManager>> create_with_demuxer(NonnullOwnPtr<Demuxer> demuxer, PlaybackTimerCreator playback_timer_creator);

    void start_timer(int milliseconds);
    void timer_callback();
    Optional<Time> seek_demuxer_to_most_recent_keyframe(Time timestamp, Optional<Time> earliest_available_sample = OptionalNone());

    bool decode_and_queue_one_sample();
    void on_decode_timer();

    void dispatch_decoder_error(DecoderError error);
    void dispatch_new_frame(RefPtr<Gfx::Bitmap> frame);
    // Returns whether we changed playback states. If so, any PlaybackStateHandler processing must cease.
    [[nodiscard]] bool dispatch_frame_queue_item(FrameQueueItem&&);
    void dispatch_state_change();
    void dispatch_fatal_error(Error);

    Time m_last_present_in_media_time = Time::zero();

    NonnullOwnPtr<Demuxer> m_demuxer;
    Track m_selected_video_track;
    NonnullOwnPtr<VideoDecoder> m_decoder;

    NonnullOwnPtr<VideoFrameQueue> m_frame_queue;

    OwnPtr<PlaybackTimer> m_present_timer;
    unsigned m_decoding_buffer_time_ms = 16;

    OwnPtr<PlaybackTimer> m_decode_timer;

    NonnullOwnPtr<PlaybackStateHandler> m_playback_handler;
    Optional<FrameQueueItem> m_next_frame;

    u64 m_skipped_frames { 0 };

    // This is a nested class to allow private access.
    class PlaybackStateHandler {
    public:
        PlaybackStateHandler(PlaybackManager& manager)
            : m_manager(manager)
        {
        }
        virtual ~PlaybackStateHandler() = default;
        virtual StringView name() = 0;

        virtual ErrorOr<void> on_enter() { return {}; }

        virtual ErrorOr<void> play() { return {}; };
        virtual bool is_playing() const = 0;
        virtual PlaybackState get_state() const = 0;
        virtual ErrorOr<void> pause() { return {}; };
        virtual ErrorOr<void> buffer() { return {}; };
        virtual ErrorOr<void> seek(Time target_timestamp, SeekMode);
        virtual ErrorOr<void> stop();

        virtual Time current_time() const;

        virtual ErrorOr<void> on_timer_callback() { return {}; };
        virtual ErrorOr<void> on_buffer_filled() { return {}; };

    protected:
        template<class T, class... Args>
        ErrorOr<void> replace_handler_and_delete_this(Args... args);

        PlaybackManager& manager() const;

        PlaybackManager& manager()
        {
            return const_cast<PlaybackManager&>(const_cast<PlaybackStateHandler const*>(this)->manager());
        }

    private:
        PlaybackManager& m_manager;
#if PLAYBACK_MANAGER_DEBUG
        bool m_has_exited { false };
#endif
    };
};

}
