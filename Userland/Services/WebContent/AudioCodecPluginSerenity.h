/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Time.h>
#include <LibAudio/Forward.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Platform/AudioCodecPlugin.h>

namespace WebContent {

class AudioCodecPluginSerenity final : public Web::Platform::AudioCodecPlugin {
public:
    static ErrorOr<NonnullOwnPtr<AudioCodecPluginSerenity>> create(NonnullRefPtr<Audio::Loader>);
    virtual ~AudioCodecPluginSerenity() override;

    virtual void resume_playback() override;
    virtual void pause_playback() override;
    virtual void set_volume(double) override;
    virtual void seek(double) override;

    virtual Duration duration() override;

private:
    AudioCodecPluginSerenity(NonnullRefPtr<Audio::ConnectionToServer>, NonnullRefPtr<Audio::Loader>);

    ErrorOr<void> play_next_samples();

    NonnullRefPtr<Audio::ConnectionToServer> m_connection;
    NonnullRefPtr<Audio::Loader> m_loader;
    NonnullRefPtr<Web::Platform::Timer> m_sample_timer;

    Duration m_duration;
    Duration m_position;

    size_t m_device_sample_rate { 0 };
    size_t m_device_samples_per_buffer { 0 };
    size_t m_samples_to_load_per_buffer { 0 };
};

}
