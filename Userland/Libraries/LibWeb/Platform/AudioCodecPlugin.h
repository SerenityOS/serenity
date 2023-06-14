/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedArray.h>
#include <AK/Function.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Optional.h>
#include <LibAudio/Forward.h>

namespace Web::Platform {

class AudioCodecPlugin {
public:
    static void install_creation_hook(Function<ErrorOr<NonnullOwnPtr<AudioCodecPlugin>>()>);
    static ErrorOr<NonnullOwnPtr<AudioCodecPlugin>> create();

    virtual ~AudioCodecPlugin();

    virtual size_t device_sample_rate() = 0;

    virtual void enqueue_samples(FixedArray<Audio::Sample>) = 0;
    virtual size_t remaining_samples() const = 0;

    virtual void resume_playback() = 0;
    virtual void pause_playback() = 0;
    virtual void playback_ended() = 0;

    virtual void set_volume(double) = 0;

protected:
    AudioCodecPlugin();
};

}
