/*
 * Copyright (c) 2024, Shannon Booth <shannon@serenityos.org>
 * Copyright (c) 2024, Bar Yemini <bar.ye651@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/AudioDestinationNodePrototype.h>
#include <LibWeb/WebAudio/AudioNode.h>
#include <LibWeb/WebAudio/BaseAudioContext.h>
#include <LibWeb/WebIDL/Types.h>

namespace Web::WebAudio {

// https://webaudio.github.io/web-audio-api/#AudioDestinationNode
class AudioDestinationNode : public AudioNode {
    WEB_PLATFORM_OBJECT(AudioDestinationNode, AudioNode);
    JS_DECLARE_ALLOCATOR(AudioDestinationNode);

public:
    virtual ~AudioDestinationNode() override;

    WebIDL::UnsignedLong max_channel_count();

    static JS::NonnullGCPtr<AudioDestinationNode> construct_impl(JS::Realm&, JS::NonnullGCPtr<BaseAudioContext>);

protected:
    AudioDestinationNode(JS::Realm&, JS::NonnullGCPtr<BaseAudioContext>);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;
};

}
