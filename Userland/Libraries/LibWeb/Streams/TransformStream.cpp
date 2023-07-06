/*
 * Copyright (c) 2023, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Streams/AbstractOperations.h>
#include <LibWeb/Streams/TransformStream.h>
#include <LibWeb/Streams/Transformer.h>
#include <LibWeb/Streams/WritableStream.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Streams {

// https://streams.spec.whatwg.org/#ts-construct
WebIDL::ExceptionOr<JS::NonnullGCPtr<TransformStream>> TransformStream::construct_impl(JS::Realm& realm, Optional<JS::Handle<JS::Object>> transformer_object, QueuingStrategy const& writable_strategy, QueuingStrategy const& readable_strategy)
{
    (void)transformer_object;
    (void)writable_strategy;
    (void)readable_strategy;

    return MUST_OR_THROW_OOM(realm.heap().allocate<TransformStream>(realm, realm));
}

TransformStream::TransformStream(JS::Realm& realm)
    : Bindings::PlatformObject(realm)
{
}

TransformStream::~TransformStream() = default;

JS::ThrowCompletionOr<void> TransformStream::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::TransformStreamPrototype>(realm, "TransformStream"));

    return {};
}

void TransformStream::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_backpressure_change_promise);
    visitor.visit(m_controller);
    visitor.visit(m_readable);
    visitor.visit(m_writable);
}

}
