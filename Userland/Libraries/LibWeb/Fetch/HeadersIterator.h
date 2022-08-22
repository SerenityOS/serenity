/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/Fetch/Headers.h>

namespace Web::Fetch {

class HeadersIterator
    : public Bindings::Wrappable
    , public RefCounted<HeadersIterator> {
public:
    using WrapperType = Bindings::HeadersIteratorWrapper;

    static NonnullRefPtr<HeadersIterator> create(Headers const& headers, JS::Object::PropertyKind iteration_kind)
    {
        return adopt_ref(*new HeadersIterator(headers, iteration_kind));
    }

    JS::ThrowCompletionOr<JS::Object*> next();

    void visit_edges(JS::Cell::Visitor&);

private:
    HeadersIterator(Headers const& headers, JS::Object::PropertyKind iteration_kind)
        : m_headers(headers)
        , m_iteration_kind(iteration_kind)
    {
    }

    Headers const& m_headers;
    JS::Object::PropertyKind m_iteration_kind;
    size_t m_index { 0 };
};

}

namespace Web::Bindings {

HeadersIteratorWrapper* wrap(JS::Realm&, Fetch::HeadersIterator&);

}
