/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2023, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Streams/ReadableByteStreamController.h>
#include <LibWeb/Streams/ReadableStreamBYOBRequest.h>
#include <LibWeb/WebIDL/Buffers.h>

namespace Web::Streams {

JS_DEFINE_ALLOCATOR(ReadableStreamBYOBRequest);

// https://streams.spec.whatwg.org/#rs-byob-request-view
JS::GCPtr<WebIDL::ArrayBufferView> ReadableStreamBYOBRequest::view()
{
    // 1. Return this.[[view]].
    return m_view;
}

ReadableStreamBYOBRequest::ReadableStreamBYOBRequest(JS::Realm& realm)
    : Bindings::PlatformObject(realm)
{
}

void ReadableStreamBYOBRequest::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_controller);
    visitor.visit(m_view);
}

}
