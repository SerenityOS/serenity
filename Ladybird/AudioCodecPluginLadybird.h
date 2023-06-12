/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullOwnPtr.h>
#include <LibAudio/Forward.h>
#include <LibWeb/Platform/AudioCodecPlugin.h>

class QAudioSink;
class QIODevice;
class QMediaDevices;

namespace Ladybird {

class AudioCodecPluginLadybird final : public Web::Platform::AudioCodecPlugin {
public:
    static ErrorOr<NonnullOwnPtr<AudioCodecPluginLadybird>> create();
    virtual ~AudioCodecPluginLadybird() override;

    virtual size_t device_sample_rate() override;

    virtual void enqueue_samples(FixedArray<Audio::Sample>) override;
    virtual size_t remaining_samples() const override;

    virtual void resume_playback() override;
    virtual void pause_playback() override;
    virtual void playback_ended() override;

private:
    AudioCodecPluginLadybird(NonnullOwnPtr<QMediaDevices>, NonnullOwnPtr<QAudioSink>);

    NonnullOwnPtr<QMediaDevices> m_devices;
    NonnullOwnPtr<QAudioSink> m_audio_output;
    QIODevice* m_io_device { nullptr };
};

}
