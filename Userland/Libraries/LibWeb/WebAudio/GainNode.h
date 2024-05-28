/*
 * Copyright (c) 2024, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/GainNodePrototype.h>
#include <LibWeb/WebAudio/AudioScheduledSourceNode.h>

namespace Web::WebAudio {

// https://webaudio.github.io/web-audio-api/#GainOptions
struct GainOptions : AudioNodeOptions {
    float gain { 1.0 };
};

// https://webaudio.github.io/web-audio-api/#GainNode
class GainNode : public AudioNode {
    WEB_PLATFORM_OBJECT(GainNode, AudioNode);
    JS_DECLARE_ALLOCATOR(GainNode);

public:
    virtual ~GainNode() override;

    static JS::NonnullGCPtr<GainNode> create(JS::Realm&, JS::NonnullGCPtr<BaseAudioContext>, GainOptions const& = {});
    static JS::NonnullGCPtr<GainNode> construct_impl(JS::Realm&, JS::NonnullGCPtr<BaseAudioContext>, GainOptions const& = {});

    JS::NonnullGCPtr<AudioParam const> gain() const { return m_gain; }

protected:
    GainNode(JS::Realm&, JS::NonnullGCPtr<BaseAudioContext>, GainOptions const& = {});

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

private:
    // https://webaudio.github.io/web-audio-api/#dom-gainnode-gain
    JS::NonnullGCPtr<AudioParam> m_gain;
};

}
