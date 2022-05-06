/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2021, JJ Roberts-White <computerfido@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AudioPlayerLoop.h"

#include "TrackManager.h"
#include <AK/FixedArray.h>
#include <AK/NumericLimits.h>
#include <LibAudio/ConnectionFromClient.h>
#include <LibAudio/Resampler.h>
#include <LibAudio/Sample.h>
#include <LibCore/EventLoop.h>

static FixedArray<Audio::Sample> music_samples_to_buffer(Vector<Music::Sample>& music_samples)
{
    FixedArray<Audio::Sample> samples = MUST(FixedArray<Audio::Sample>::try_create(music_samples.size()));
    for (size_t i = 0; i < music_samples.size(); ++i)
        samples[i] = { static_cast<float>(music_samples[i].left) / AK::NumericLimits<i16>::max(), static_cast<float>(music_samples[i].right) / AK::NumericLimits<i16>::max() };

    return samples;
}

AudioPlayerLoop::AudioPlayerLoop(TrackManager& track_manager, bool& need_to_write_wav, Audio::WavWriter& wav_writer)
    : m_track_manager(track_manager)
    , m_need_to_write_wav(need_to_write_wav)
    , m_wav_writer(wav_writer)
{
    m_audio_client = Audio::ConnectionFromClient::try_create().release_value_but_fixme_should_propagate_errors();

    auto target_sample_rate = m_audio_client->get_sample_rate();
    if (target_sample_rate == 0)
        target_sample_rate = Music::sample_rate;
    m_resampler = Audio::ResampleHelper<Sample>(Music::sample_rate, target_sample_rate);

    // FIXME: I said I would never write such a hack again, but here we are.
    // This code should die as soon as possible anyways, so it doesn't matter.
    // Please don't use this as an example to write good audio code; it's just here as a temporary hack.
    Core::EventLoop::register_timer(*this, 10, true, Core::TimerShouldFireWhenNotVisible::Yes);
}

void AudioPlayerLoop::timer_event(Core::TimerEvent&)
{
    if (m_audio_client->remaining_samples() < buffer_size)
        enqueue_audio();
}

void AudioPlayerLoop::enqueue_audio()
{
    m_track_manager.fill_buffer(m_buffer);
    // FIXME: Handle OOM better.
    auto audio_buffer = m_resampler->resample(m_buffer);
    auto real_buffer = music_samples_to_buffer(audio_buffer);
    (void)m_audio_client->async_enqueue(real_buffer);

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

    if (m_should_play_audio)
        m_audio_client->async_start_playback();
    else
        m_audio_client->async_pause_playback();
}
