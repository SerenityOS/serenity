/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Bodies.h>

namespace Web::Fetch::Infrastructure {

Body::Body(JS::Handle<Streams::ReadableStream> stream)
    : m_stream(move(stream))
{
}

Body::Body(JS::Handle<Streams::ReadableStream> stream, SourceType source, Optional<u64> length)
    : m_stream(move(stream))
    , m_source(move(source))
    , m_length(move(length))
{
}

// https://fetch.spec.whatwg.org/#concept-body-clone
WebIDL::ExceptionOr<Body> Body::clone() const
{
    // To clone a body body, run these steps:

    auto& vm = Bindings::main_thread_vm();
    auto& realm = *vm.current_realm();
    auto& window = verify_cast<HTML::Window>(realm.global_object());

    // FIXME: 1. Let « out1, out2 » be the result of teeing body’s stream.
    // FIXME: 2. Set body’s stream to out1.
    auto* out2 = vm.heap().allocate<Streams::ReadableStream>(realm, window);

    // 3. Return a body whose stream is out2 and other members are copied from body.
    return Body { JS::make_handle(out2), m_source, m_length };
}

}
