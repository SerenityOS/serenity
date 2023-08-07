/*
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/BaseAudioContextPrototype.h>
#include <LibWeb/DOM/EventTarget.h>

namespace Web::WebAudio {

// https://webaudio.github.io/web-audio-api/#BaseAudioContext
class BaseAudioContext : public DOM::EventTarget {
    WEB_PLATFORM_OBJECT(BaseAudioContext, DOM::EventTarget);

public:
    virtual ~BaseAudioContext() override;

    float sample_rate() const { return m_sample_rate; }
    double current_time() const { return m_current_time; }
    Bindings::AudioContextState state() const { return m_control_thread_state; }

    void set_onstatechange(WebIDL::CallbackType*);
    WebIDL::CallbackType* onstatechange();

    void set_sample_rate(float sample_rate) { m_sample_rate = sample_rate; }
    void set_control_state(Bindings::AudioContextState state) { m_control_thread_state = state; }
    void set_rendering_state(Bindings::AudioContextState state) { m_rendering_thread_state = state; }

protected:
    explicit BaseAudioContext(JS::Realm&);

    virtual void initialize(JS::Realm&) override;

private:
    float m_sample_rate { 0 };
    double m_current_time { 0 };

    Bindings::AudioContextState m_control_thread_state = Bindings::AudioContextState::Suspended;
    Bindings::AudioContextState m_rendering_thread_state = Bindings::AudioContextState::Suspended;
};

}
