/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <LibAudio/Forward.h>
#include <LibWeb/Platform/AudioCodecPlugin.h>

namespace WebContent {

class AudioCodecPluginSerenity final : public Web::Platform::AudioCodecPlugin {
public:
    static ErrorOr<NonnullOwnPtr<AudioCodecPluginSerenity>> create();
    virtual ~AudioCodecPluginSerenity() override;

    virtual size_t device_sample_rate() override;

    virtual void enqueue_samples(FixedArray<Audio::Sample>) override;
    virtual size_t remaining_samples() const override;

    virtual void resume_playback() override;
    virtual void pause_playback() override;
    virtual void playback_ended() override;

    virtual void set_volume(double) override;

private:
    explicit AudioCodecPluginSerenity(NonnullRefPtr<Audio::ConnectionToServer>);

    NonnullRefPtr<Audio::ConnectionToServer> m_connection;
    Optional<size_t> m_device_sample_rate;
};

}
