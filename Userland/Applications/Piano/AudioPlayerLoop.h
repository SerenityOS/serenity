/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2021, JJ Roberts-White <computerfido@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Music.h"
#include <LibAudio/ConnectionToServer.h>
#include <LibAudio/Resampler.h>
#include <LibAudio/Sample.h>
#include <LibAudio/WavWriter.h>
#include <LibCore/Event.h>
#include <LibCore/EventReceiver.h>
#include <LibDSP/Music.h>
#include <LibThreading/MutexProtected.h>
#include <LibThreading/Thread.h>

class TrackManager;

// Wrapper class accepting custom events to advance the track playing and forward audio data to the system.
// This does not run on a separate thread, preventing IPC multithreading madness.
class AudioPlayerLoop final : public Core::EventReceiver {
    C_OBJECT(AudioPlayerLoop)
public:
    virtual ~AudioPlayerLoop() override;

    void toggle_paused();
    bool is_playing() const { return m_should_play_audio; }

private:
    AudioPlayerLoop(TrackManager& track_manager, Atomic<bool>& need_to_write_wav, Atomic<int>& wav_percent_written, Threading::MutexProtected<Audio::WavWriter>& wav_writer);

    intptr_t pipeline_thread_main();
    ErrorOr<void> send_audio_to_server();
    ErrorOr<void> write_wav_if_needed();

    TrackManager& m_track_manager;
    FixedArray<DSP::Sample> m_buffer;
    RefPtr<Audio::ConnectionToServer> m_audio_client;
    NonnullRefPtr<Threading::Thread> m_pipeline_thread;

    Atomic<bool> m_should_play_audio { true };
    Atomic<bool> m_exit_requested { false };

    Atomic<bool>& m_need_to_write_wav;
    Atomic<int>& m_wav_percent_written;
    Threading::MutexProtected<Audio::WavWriter>& m_wav_writer;
};
