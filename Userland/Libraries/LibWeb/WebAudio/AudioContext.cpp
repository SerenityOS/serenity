/*
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/WebAudio/AudioContext.h>

namespace Web::WebAudio {

// https://webaudio.github.io/web-audio-api/#dom-audiocontext-audiocontext
WebIDL::ExceptionOr<JS::NonnullGCPtr<AudioContext>> AudioContext::construct_impl(JS::Realm& realm)
{
    dbgln("(STUBBED) new AudioContext()");
    return MUST_OR_THROW_OOM(realm.heap().allocate<AudioContext>(realm, realm));
}

AudioContext::AudioContext(JS::Realm& realm)
    : BaseAudioContext(realm)
{
}

AudioContext::~AudioContext() = default;

JS::ThrowCompletionOr<void> AudioContext::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::AudioContextPrototype>(realm, "AudioContext"));

    return {};
}

}
