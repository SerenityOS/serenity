/*
 * Copyright (c) 2024, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/OscillatorNodePrototype.h>
#include <LibWeb/WebAudio/AudioScheduledSourceNode.h>

namespace Web::WebAudio {

// https://webaudio.github.io/web-audio-api/#OscillatorOptions
struct OscillatorOptions : AudioNodeOptions {
    Bindings::OscillatorType type { Bindings::OscillatorType::Sine };
    float frequency { 440 };
    float detune { 0 };
    JS::GCPtr<PeriodicWave> periodic_wave;
};

// https://webaudio.github.io/web-audio-api/#OscillatorNode
class OscillatorNode : public AudioScheduledSourceNode {
    WEB_PLATFORM_OBJECT(OscillatorNode, AudioScheduledSourceNode);
    JS_DECLARE_ALLOCATOR(OscillatorNode);

public:
    virtual ~OscillatorNode() override;

    static WebIDL::ExceptionOr<JS::NonnullGCPtr<OscillatorNode>> create(JS::Realm&, JS::NonnullGCPtr<BaseAudioContext>, OscillatorOptions const& = {});
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<OscillatorNode>> construct_impl(JS::Realm&, JS::NonnullGCPtr<BaseAudioContext>, OscillatorOptions const& = {});

protected:
    OscillatorNode(JS::Realm&, JS::NonnullGCPtr<BaseAudioContext>, OscillatorOptions const& = {});

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;
};

}
