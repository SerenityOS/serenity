/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AudioCodecPluginLadybird.h"
#include "AudioThread.h"
#include <LibAudio/Loader.h>

namespace Ladybird {

ErrorOr<NonnullOwnPtr<AudioCodecPluginLadybird>> AudioCodecPluginLadybird::create(NonnullRefPtr<Audio::Loader> loader)
{
    auto audio_thread = TRY(AudioThread::create(move(loader)));
    audio_thread->start();

    return adopt_nonnull_own_or_enomem(new (nothrow) AudioCodecPluginLadybird(move(audio_thread)));
}

AudioCodecPluginLadybird::AudioCodecPluginLadybird(NonnullOwnPtr<AudioThread> audio_thread)
    : m_audio_thread(move(audio_thread))
{
    connect(m_audio_thread, &AudioThread::playback_position_updated, this, [this](auto position) {
        if (on_playback_position_updated)
            on_playback_position_updated(position);
    });
}

AudioCodecPluginLadybird::~AudioCodecPluginLadybird()
{
    m_audio_thread->stop().release_value_but_fixme_should_propagate_errors();
}

void AudioCodecPluginLadybird::resume_playback()
{
    m_audio_thread->queue_task({ AudioTask::Type::Play }).release_value_but_fixme_should_propagate_errors();
}

void AudioCodecPluginLadybird::pause_playback()
{
    m_audio_thread->queue_task({ AudioTask::Type::Pause }).release_value_but_fixme_should_propagate_errors();
}

void AudioCodecPluginLadybird::set_volume(double volume)
{

    AudioTask task { AudioTask::Type::Volume };
    task.data = volume;

    m_audio_thread->queue_task(move(task)).release_value_but_fixme_should_propagate_errors();
}

void AudioCodecPluginLadybird::seek(double position)
{
    AudioTask task { AudioTask::Type::Seek };
    task.data = position;

    m_audio_thread->queue_task(move(task)).release_value_but_fixme_should_propagate_errors();
}

Duration AudioCodecPluginLadybird::duration()
{
    return m_audio_thread->duration();
}

}
