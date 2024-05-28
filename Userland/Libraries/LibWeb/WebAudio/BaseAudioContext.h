/*
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2024, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/BaseAudioContextPrototype.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/WebIDL/Types.h>

namespace Web::WebAudio {

// https://webaudio.github.io/web-audio-api/#BaseAudioContext
class BaseAudioContext : public DOM::EventTarget {
    WEB_PLATFORM_OBJECT(BaseAudioContext, DOM::EventTarget);

public:
    virtual ~BaseAudioContext() override;

    // https://webaudio.github.io/web-audio-api/#dom-baseaudiocontext-createbuffer-numberofchannels
    // > An implementation MUST support at least 32 channels.
    // Other browsers appear to only allow 32 channels - so let's limit ourselves to that too.
    static constexpr WebIDL::UnsignedLong MAX_NUMBER_OF_CHANNELS { 32 };

    // https://webaudio.github.io/web-audio-api/#dom-baseaudiocontext-createbuffer-samplerate
    // > An implementation MUST support sample rates in at least the range 8000 to 96000.
    // This doesn't seem consistent between browsers. We use what firefox accepts from testing BaseAudioContext.createAudioBuffer.
    static constexpr float MIN_SAMPLE_RATE { 8000 };
    static constexpr float MAX_SAMPLE_RATE { 192000 };

    float sample_rate() const { return m_sample_rate; }
    double current_time() const { return m_current_time; }
    Bindings::AudioContextState state() const { return m_control_thread_state; }

    // https://webaudio.github.io/web-audio-api/#--nyquist-frequency
    float nyquist_frequency() const { return m_sample_rate / 2; }

    void set_onstatechange(WebIDL::CallbackType*);
    WebIDL::CallbackType* onstatechange();

    void set_sample_rate(float sample_rate) { m_sample_rate = sample_rate; }
    void set_control_state(Bindings::AudioContextState state) { m_control_thread_state = state; }
    void set_rendering_state(Bindings::AudioContextState state) { m_rendering_thread_state = state; }

    static WebIDL::ExceptionOr<void> verify_audio_options_inside_nominal_range(JS::Realm&, WebIDL::UnsignedLong number_of_channels, WebIDL::UnsignedLong length, float sample_rate);

    WebIDL::ExceptionOr<JS::NonnullGCPtr<AudioBuffer>> create_buffer(WebIDL::UnsignedLong number_of_channels, WebIDL::UnsignedLong length, float sample_rate);
    WebIDL::ExceptionOr<JS::NonnullGCPtr<OscillatorNode>> create_oscillator();
    WebIDL::ExceptionOr<JS::NonnullGCPtr<DynamicsCompressorNode>> create_dynamics_compressor();
    JS::NonnullGCPtr<GainNode> create_gain();

protected:
    explicit BaseAudioContext(JS::Realm&, float m_sample_rate = 0);

    virtual void initialize(JS::Realm&) override;

private:
    float m_sample_rate { 0 };
    double m_current_time { 0 };

    Bindings::AudioContextState m_control_thread_state = Bindings::AudioContextState::Suspended;
    Bindings::AudioContextState m_rendering_thread_state = Bindings::AudioContextState::Suspended;
};

}
