/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibAudio/Loader.h>
#include <LibAudio/Resampler.h>
#include <LibAudio/Sample.h>
#include <LibWeb/Platform/AudioCodecPlugin.h>

namespace Web::Platform {

static AudioCodecPlugin::AudioCodecPluginCreator s_creation_hook;

AudioCodecPlugin::AudioCodecPlugin() = default;
AudioCodecPlugin::~AudioCodecPlugin() = default;

void AudioCodecPlugin::install_creation_hook(AudioCodecPluginCreator creation_hook)
{
    VERIFY(!s_creation_hook);
    s_creation_hook = move(creation_hook);
}

ErrorOr<NonnullOwnPtr<AudioCodecPlugin>> AudioCodecPlugin::create(NonnullRefPtr<Audio::Loader> loader)
{
    VERIFY(s_creation_hook);
    return s_creation_hook(move(loader));
}

ErrorOr<FixedArray<Audio::Sample>> AudioCodecPlugin::read_samples_from_loader(Audio::Loader& loader, size_t samples_to_load, size_t device_sample_rate)
{
    auto buffer_or_error = loader.get_more_samples(samples_to_load);
    if (buffer_or_error.is_error()) {
        dbgln("Error while loading samples: {}", buffer_or_error.error().description);
        return Error::from_string_literal("Error while loading samples");
    }

    Audio::ResampleHelper<Audio::Sample> resampler(loader.sample_rate(), device_sample_rate);
    return FixedArray<Audio::Sample>::create(resampler.resample(buffer_or_error.release_value()).span());
}

Duration AudioCodecPlugin::current_loader_position(Audio::Loader const& loader, size_t device_sample_rate)
{
    auto samples_played = static_cast<double>(loader.loaded_samples());
    auto sample_rate = static_cast<double>(loader.sample_rate());

    auto source_to_device_ratio = sample_rate / static_cast<double>(device_sample_rate);
    samples_played *= source_to_device_ratio;

    return Duration::from_milliseconds(static_cast<i64>(samples_played / sample_rate * 1000.0));
}

}
