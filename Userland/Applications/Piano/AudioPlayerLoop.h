/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 * Copyright (c) 2021, JJ Roberts-White <computerfido@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Music.h"
#include <AK/NonnullRefPtr.h>
#include <LibAudio/Buffer.h>
#include <LibAudio/ClientConnection.h>
#include <LibAudio/WavWriter.h>
#include <LibCore/Object.h>
#include <LibDSP/TrackManager.h>
#include <LibDSP/Transport.h>

// Wrapper class accepting custom events to advance the track playing and forward audio data to the system.
// This does not run on a separate thread, preventing IPC multithreading madness.
class AudioPlayerLoop final : public Core::Object {
    C_OBJECT(AudioPlayerLoop)
public:
    void enqueue_audio();

    void toggle_paused();
    bool is_playing() { return m_should_play_audio; }

private:
    AudioPlayerLoop(NonnullRefPtr<LibDSP::TrackManager> track_manager, NonnullRefPtr<LibDSP::Transport> transport, bool& need_to_write_wav, Audio::WavWriter& wav_writer);

    NonnullRefPtr<LibDSP::TrackManager> m_track_manager;
    NonnullRefPtr<LibDSP::Transport> m_transport;
    Array<Sample, sample_count> m_buffer;
    Optional<Audio::ResampleHelper<double>> m_resampler;
    RefPtr<Audio::ClientConnection> m_audio_client;

    bool m_should_play_audio = true;

    bool& m_need_to_write_wav;
    Audio::WavWriter& m_wav_writer;
};
