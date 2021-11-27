/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 * Copyright (c) 2021, JJ Roberts-White <computerfido@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AudioPlayerLoop.h"

#include "TrackManager.h"

// Converts Piano-internal data to an Audio::Buffer that AudioServer receives
static NonnullRefPtr<Audio::Buffer> music_samples_to_buffer(Array<Sample, sample_count> samples)
{
    Vector<Audio::Sample, sample_count> frames;
    frames.ensure_capacity(sample_count);
    for (auto sample : samples) {
        Audio::Sample frame = { sample.left / (double)NumericLimits<i16>::max(), sample.right / (double)NumericLimits<i16>::max() };
        frames.unchecked_append(frame);
    }
    // FIXME: Handle OOM better.
    return MUST(Audio::Buffer::create_with_samples(frames));
}

AudioPlayerLoop::AudioPlayerLoop(TrackManager& track_manager, bool& need_to_write_wav, Audio::WavWriter& wav_writer)
    : m_track_manager(track_manager)
    , m_need_to_write_wav(need_to_write_wav)
    , m_wav_writer(wav_writer)
{
    m_audio_client = Audio::ClientConnection::construct();
    m_audio_client->on_finish_playing_buffer = [this](int buffer_id) {
        (void)buffer_id;
        enqueue_audio();
    };

    auto target_sample_rate = m_audio_client->get_sample_rate();
    if (target_sample_rate == 0)
        target_sample_rate = Music::sample_rate;
    m_resampler = Audio::ResampleHelper<double>(Music::sample_rate, target_sample_rate);
}

void AudioPlayerLoop::enqueue_audio()
{
    m_track_manager.fill_buffer(m_buffer);
    NonnullRefPtr<Audio::Buffer> audio_buffer = music_samples_to_buffer(m_buffer);
    // FIXME: Handle OOM better.
    audio_buffer = MUST(Audio::resample_buffer(m_resampler.value(), *audio_buffer));
    m_audio_client->async_enqueue(audio_buffer);

    // FIXME: This should be done somewhere else.
    if (m_need_to_write_wav) {
        m_need_to_write_wav = false;
        m_track_manager.reset();
        m_track_manager.set_should_loop(false);
        do {
            m_track_manager.fill_buffer(m_buffer);
            m_wav_writer.write_samples(reinterpret_cast<u8*>(m_buffer.data()), buffer_size);
        } while (m_track_manager.time());
        m_track_manager.reset();
        m_track_manager.set_should_loop(true);
        m_wav_writer.finalize();
    }
}

void AudioPlayerLoop::toggle_paused()
{
    m_should_play_audio = !m_should_play_audio;

    m_audio_client->set_paused(!m_should_play_audio);
}
