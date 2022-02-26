/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2021, JJ Roberts-White <computerfido@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Music.h"
#include <LibAudio/Buffer.h>
#include <LibAudio/ConnectionFromClient.h>
#include <LibAudio/WavWriter.h>
#include <LibCore/Object.h>

class TrackManager;

// Wrapper class accepting custom events to advance the track playing and forward audio data to the system.
// This does not run on a separate thread, preventing IPC multithreading madness.
class AudioPlayerLoop final : public Core::Object {
    C_OBJECT(AudioPlayerLoop)
public:
    void enqueue_audio();

    void toggle_paused();
    bool is_playing() { return m_should_play_audio; }

private:
    AudioPlayerLoop(TrackManager& track_manager, bool& need_to_write_wav, Audio::WavWriter& wav_writer);

    TrackManager& m_track_manager;
    Array<Sample, sample_count> m_buffer;
    Optional<Audio::ResampleHelper<double>> m_resampler;
    RefPtr<Audio::ConnectionFromClient> m_audio_client;

    bool m_should_play_audio = true;

    bool& m_need_to_write_wav;
    Audio::WavWriter& m_wav_writer;
};
