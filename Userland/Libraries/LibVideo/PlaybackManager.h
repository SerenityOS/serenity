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
#include <LibCore/EventLoop.h>
#include <LibCore/SharedCircularQueue.h>
#include <LibGfx/Bitmap.h>
#include <LibThreading/ConditionVariable.h>
#include <LibThreading/Mutex.h>
#include <LibThreading/Thread.h>
#include <LibVideo/Containers/Demuxer.h>
#include <LibVideo/Containers/Matroska/Document.h>

#include "VideoDecoder.h"

namespace Video {

enum class PlaybackStatus {
    Playing,
    Paused,
    Buffering,
    SeekingPlaying,
    SeekingPaused,
    Stopped,
    Corrupted,
};

struct FrameQueueItem {
    enum class Type {
        Frame,
        Error,
    };

    static FrameQueueItem frame(RefPtr<Gfx::Bitmap> bitmap, Time timestamp)
    {
        return FrameQueueItem(move(bitmap), timestamp);
    }

    static FrameQueueItem error_marker(DecoderError&& error)
    {
        return FrameQueueItem(move(error));
    }

    bool is_frame() const { return m_data.has<FrameData>(); }
    RefPtr<Gfx::Bitmap> bitmap() const { return m_data.get<FrameData>().bitmap; }
    Time timestamp() const { return m_data.get<FrameData>().timestamp; }

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
            return error().string_literal();
        return DeprecatedString::formatted("frame at {}ms", timestamp().to_milliseconds());
    }

private:
    struct FrameData {
        RefPtr<Gfx::Bitmap> bitmap;
        Time timestamp;
    };

    FrameQueueItem(RefPtr<Gfx::Bitmap> bitmap, Time timestamp)
        : m_data(FrameData { move(bitmap), timestamp })
    {
    }

    FrameQueueItem(DecoderError&& error)
        : m_data(move(error))
    {
    }

    Variant<Empty, FrameData, DecoderError> m_data;
};

static constexpr size_t FRAME_BUFFER_COUNT = 4;
using VideoFrameQueue = Queue<FrameQueueItem, FRAME_BUFFER_COUNT>;

class PlaybackManager {
public:
    enum class SeekMode {
        Accurate,
        Fast,
    };

    static constexpr SeekMode DEFAULT_SEEK_MODE = SeekMode::Accurate;

    static DecoderErrorOr<NonnullOwnPtr<PlaybackManager>> from_file(Core::Object& event_handler, StringView file);
    static DecoderErrorOr<NonnullOwnPtr<PlaybackManager>> from_data(Core::Object& event_handler, Span<u8> data);

    PlaybackManager(Core::Object& event_handler, NonnullOwnPtr<Demuxer>& demuxer, Track video_track, NonnullOwnPtr<VideoDecoder>&& decoder);

    void resume_playback();
    void pause_playback();
    void restart_playback();
    void seek_to_timestamp(Time);
    bool is_playing() const { return m_status == PlaybackStatus::Playing || m_status == PlaybackStatus::SeekingPlaying || m_status == PlaybackStatus::Buffering; }
    bool is_seeking() const { return m_status == PlaybackStatus::SeekingPlaying || m_status == PlaybackStatus::SeekingPaused; }
    bool is_buffering() const { return m_status == PlaybackStatus::Buffering; }
    bool is_stopped() const { return m_status == PlaybackStatus::Stopped || m_status == PlaybackStatus::Corrupted; }

    SeekMode seek_mode() { return m_seek_mode; }
    void set_seek_mode(SeekMode mode) { m_seek_mode = mode; }

    u64 number_of_skipped_frames() const { return m_skipped_frames; }

    void on_decoder_error(DecoderError error);

    Time current_playback_time();
    Time duration();

    Function<void(NonnullRefPtr<Gfx::Bitmap>, Time)> on_frame_present;

private:
    void set_playback_status(PlaybackStatus status);

    void end_seek();
    void update_presented_frame();

    // May run off the main thread
    void post_decoder_error(DecoderError error);
    bool decode_and_queue_one_sample();
    void on_decode_timer();

    Core::Object& m_event_handler;
    Core::EventLoop& m_main_loop;

    PlaybackStatus m_status { PlaybackStatus::Stopped };
    Time m_last_present_in_media_time = Time::zero();
    Time m_last_present_in_real_time = Time::zero();

    Time m_seek_to_media_time = Time::min();
    SeekMode m_seek_mode = DEFAULT_SEEK_MODE;

    NonnullOwnPtr<Demuxer> m_demuxer;
    Track m_selected_video_track;
    NonnullOwnPtr<VideoDecoder> m_decoder;

    NonnullOwnPtr<VideoFrameQueue> m_frame_queue;
    Optional<FrameQueueItem> m_next_frame;

    NonnullRefPtr<Core::Timer> m_present_timer;
    unsigned m_decoding_buffer_time_ms = 16;

    NonnullRefPtr<Core::Timer> m_decode_timer;

    u64 m_skipped_frames;
};

enum EventType : unsigned {
    DecoderErrorOccurred = (('v' << 2) | ('i' << 1) | 'd') << 4,
    VideoFramePresent,
    PlaybackStatusChange,
};

class DecoderErrorEvent : public Core::Event {
public:
    explicit DecoderErrorEvent(DecoderError error)
        : Core::Event(DecoderErrorOccurred)
        , m_error(move(error))
    {
    }
    virtual ~DecoderErrorEvent() = default;

    DecoderError const& error() { return m_error; }

private:
    DecoderError m_error;
};

class VideoFramePresentEvent : public Core::Event {
public:
    VideoFramePresentEvent() = default;
    explicit VideoFramePresentEvent(RefPtr<Gfx::Bitmap> frame)
        : Core::Event(VideoFramePresent)
        , m_frame(move(frame))
    {
    }
    virtual ~VideoFramePresentEvent() = default;

    RefPtr<Gfx::Bitmap> frame() { return m_frame; }

private:
    RefPtr<Gfx::Bitmap> m_frame;
};

class PlaybackStatusChangeEvent : public Core::Event {
public:
    PlaybackStatusChangeEvent() = default;
    explicit PlaybackStatusChangeEvent(PlaybackStatus status, PlaybackStatus previous_status)
        : Core::Event(PlaybackStatusChange)
        , m_status(status)
        , m_previous_status(previous_status)
    {
    }
    virtual ~PlaybackStatusChangeEvent() = default;

    PlaybackStatus status();
    PlaybackStatus previous_status();

private:
    PlaybackStatus m_status;
    PlaybackStatus m_previous_status;
};

inline StringView playback_status_to_string(PlaybackStatus status)
{
    switch (status) {
    case PlaybackStatus::Playing:
        return "Playing"sv;
    case PlaybackStatus::Paused:
        return "Paused"sv;
    case PlaybackStatus::Buffering:
        return "Buffering"sv;
    case PlaybackStatus::SeekingPlaying:
        return "SeekingPlaying"sv;
    case PlaybackStatus::SeekingPaused:
        return "SeekingPaused"sv;
    case PlaybackStatus::Stopped:
        return "Stopped"sv;
    case PlaybackStatus::Corrupted:
        return "Corrupted"sv;
    }
    return "Unknown"sv;
};

}
