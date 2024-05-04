/*
 * Copyright (c) 2024, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/DynamicsCompressorNodePrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/WebAudio/DynamicsCompressorNode.h>

namespace Web::WebAudio {

JS_DEFINE_ALLOCATOR(DynamicsCompressorNode);

DynamicsCompressorNode::~DynamicsCompressorNode() = default;

WebIDL::ExceptionOr<JS::NonnullGCPtr<DynamicsCompressorNode>> DynamicsCompressorNode::create(JS::Realm& realm, JS::NonnullGCPtr<BaseAudioContext> context, DynamicsCompressorOptions const& options)
{
    return construct_impl(realm, context, options);
}

// https://webaudio.github.io/web-audio-api/#dom-dynamicscompressornode-dynamicscompressornode
WebIDL::ExceptionOr<JS::NonnullGCPtr<DynamicsCompressorNode>> DynamicsCompressorNode::construct_impl(JS::Realm& realm, JS::NonnullGCPtr<BaseAudioContext> context, DynamicsCompressorOptions const& options)
{
    // FIXME: Invoke "Initialize the AudioNode" steps.
    return realm.vm().heap().allocate<DynamicsCompressorNode>(realm, realm, context, options);
}

DynamicsCompressorNode::DynamicsCompressorNode(JS::Realm& realm, JS::NonnullGCPtr<BaseAudioContext> context, DynamicsCompressorOptions const&)
    : AudioNode(realm, context)
{
}

void DynamicsCompressorNode::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(DynamicsCompressorNode);
}

void DynamicsCompressorNode::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
}

}
