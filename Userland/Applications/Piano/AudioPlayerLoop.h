/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 * Copyright (c) 2021, JJ Roberts-White <computerfido@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Music.h"
#include <LibAudio/ClientConnection.h>
#include <LibAudio/WavWriter.h>
#include <LibCore/Object.h>

class TrackManager;

// Wrapper class accepting custom events to advance the track playing and forward audio data to the system.
// This does not run on a separate thread, preventing IPC multithreading madness.
class AudioPlayerLoop : public Core::Object {
    C_OBJECT(AudioPlayerLoop)
public:
    AudioPlayerLoop(TrackManager& track_manager, bool& need_to_write_wav, Audio::WavWriter& wav_writer);

    void enqueue_audio();

    void toggle_paused();
    bool is_playing() { return m_should_play_audio; }

private:
    TrackManager& m_track_manager;
    Array<Sample, sample_count> m_buffer;
    RefPtr<Audio::ClientConnection> m_audio_client;

    bool m_should_play_audio = true;

    bool& m_need_to_write_wav;
    Audio::WavWriter& m_wav_writer;
};
