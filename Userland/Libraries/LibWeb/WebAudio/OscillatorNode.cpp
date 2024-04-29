/*
 * Copyright (c) 2024, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/OscillatorNodePrototype.h>
#include <LibWeb/WebAudio/OscillatorNode.h>

namespace Web::WebAudio {

JS_DEFINE_ALLOCATOR(OscillatorNode);

OscillatorNode::~OscillatorNode() = default;

WebIDL::ExceptionOr<JS::NonnullGCPtr<OscillatorNode>> OscillatorNode::create(JS::Realm& realm, JS::NonnullGCPtr<BaseAudioContext> context, OscillatorOptions const& options)
{
    return construct_impl(realm, context, options);
}

// https://webaudio.github.io/web-audio-api/#dom-oscillatornode-oscillatornode
WebIDL::ExceptionOr<JS::NonnullGCPtr<OscillatorNode>> OscillatorNode::construct_impl(JS::Realm& realm, JS::NonnullGCPtr<BaseAudioContext> context, OscillatorOptions const& options)
{
    // FIXME: Invoke "Initialize the AudioNode" steps.
    return realm.vm().heap().allocate<OscillatorNode>(realm, realm, context, options);
}

OscillatorNode::OscillatorNode(JS::Realm& realm, JS::NonnullGCPtr<BaseAudioContext> context, OscillatorOptions const&)
    : AudioScheduledSourceNode(realm, context)
{
}

void OscillatorNode::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(OscillatorNode);
}

void OscillatorNode::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
}

}
