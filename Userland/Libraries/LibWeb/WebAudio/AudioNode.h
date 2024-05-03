/*
 * Copyright (c) 2024, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <LibJS/Forward.h>
#include <LibWeb/Bindings/AudioNodePrototype.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/WebIDL/Types.h>

namespace Web::WebAudio {

// https://webaudio.github.io/web-audio-api/#AudioNodeOptions
struct AudioNodeOptions {
    Optional<WebIDL::UnsignedLong> channel_count;
    Bindings::ChannelCountMode channel_count_mode;
    Bindings::ChannelInterpretation channel_interpretation;
};

// https://webaudio.github.io/web-audio-api/#AudioNode
class AudioNode : public DOM::EventTarget {
    WEB_PLATFORM_OBJECT(AudioNode, DOM::EventTarget);
    JS_DECLARE_ALLOCATOR(AudioNode);

public:
    virtual ~AudioNode() override;

    WebIDL::ExceptionOr<JS::NonnullGCPtr<AudioNode>> connect(JS::NonnullGCPtr<AudioNode> destination_node, WebIDL::UnsignedLong output = 0, WebIDL::UnsignedLong input = 0);
    WebIDL::ExceptionOr<JS::NonnullGCPtr<AudioNode>> connect(JS::NonnullGCPtr<AudioParam> destination_param, WebIDL::UnsignedLong output = 0);

    void disconnect();
    void disconnect(WebIDL::UnsignedLong output);
    void disconnect(JS::NonnullGCPtr<AudioNode> destination_node);
    void disconnect(JS::NonnullGCPtr<AudioNode> destination_node, WebIDL::UnsignedLong output);
    void disconnect(JS::NonnullGCPtr<AudioNode> destination_node, WebIDL::UnsignedLong output, WebIDL::UnsignedLong input);
    void disconnect(JS::NonnullGCPtr<AudioParam> destination_param);
    void disconnect(JS::NonnullGCPtr<AudioParam> destination_param, WebIDL::UnsignedLong output);

    // https://webaudio.github.io/web-audio-api/#dom-audionode-context
    JS::NonnullGCPtr<BaseAudioContext const> context() const
    {
        // The BaseAudioContext which owns this AudioNode.
        return m_context;
    }

protected:
    AudioNode(JS::Realm&, JS::NonnullGCPtr<BaseAudioContext>);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

private:
    JS::NonnullGCPtr<BaseAudioContext> m_context;
};

}
