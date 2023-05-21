/*
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/WebAudio/BaseAudioContext.h>

namespace Web::WebAudio {

// https://webaudio.github.io/web-audio-api/#AudioContext
class AudioContext final : public BaseAudioContext {
    WEB_PLATFORM_OBJECT(AudioContext, BaseAudioContext);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<AudioContext>> construct_impl(JS::Realm&);

    virtual ~AudioContext() override;

private:
    explicit AudioContext(JS::Realm&);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
};

}
