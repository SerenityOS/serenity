/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "URLSearchParams.h"
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/URL/URLSearchParams.h>

namespace Web::URL {

class URLSearchParamsIterator : public Bindings::Wrappable
    , public RefCounted<URLSearchParamsIterator> {
public:
    using WrapperType = Bindings::URLSearchParamsIteratorWrapper;

    static NonnullRefPtr<URLSearchParamsIterator> create(URLSearchParams const& url_search_params, JS::Object::PropertyKind iteration_kind)
    {
        return adopt_ref(*new URLSearchParamsIterator(url_search_params, iteration_kind));
    }

    JS::Object* next();

    void visit_edges(JS::Cell::Visitor&);

private:
    explicit URLSearchParamsIterator(URLSearchParams const& url_search_params, JS::Object::PropertyKind iteration_kind)
        : m_url_search_params(url_search_params)
        , m_iteration_kind(iteration_kind)
    {
    }

    URLSearchParams const& m_url_search_params;
    JS::Object::PropertyKind m_iteration_kind;
    size_t m_index { 0 };
};
}

namespace Web::Bindings {

URLSearchParamsIteratorWrapper* wrap(JS::GlobalObject&, URL::URLSearchParamsIterator&);

}
