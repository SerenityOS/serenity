/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/URLSearchParamsIteratorPrototype.h>
#include <LibWeb/URL/URLSearchParamsIterator.h>

namespace Web::URL {

JS::NonnullGCPtr<URLSearchParamsIterator> URLSearchParamsIterator::create(URLSearchParams const& url_search_params, JS::Object::PropertyKind iteration_kind)
{
    return *url_search_params.heap().allocate<URLSearchParamsIterator>(url_search_params.realm(), url_search_params, iteration_kind);
}

URLSearchParamsIterator::URLSearchParamsIterator(URLSearchParams const& url_search_params, JS::Object::PropertyKind iteration_kind)
    : PlatformObject(url_search_params.realm())
    , m_url_search_params(url_search_params)
    , m_iteration_kind(iteration_kind)
{
    set_prototype(&Bindings::ensure_web_prototype<Bindings::URLSearchParamsIteratorPrototype>(url_search_params.realm(), "URLSearchParamsIterator"));
}

URLSearchParamsIterator::~URLSearchParamsIterator() = default;

void URLSearchParamsIterator::visit_edges(JS::Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_url_search_params);
}

JS::Object* URLSearchParamsIterator::next()
{
    if (m_index >= m_url_search_params.m_list.size())
        return create_iterator_result_object(vm(), JS::js_undefined(), true);

    auto& entry = m_url_search_params.m_list[m_index++];
    if (m_iteration_kind == JS::Object::PropertyKind::Key)
        return create_iterator_result_object(vm(), JS::js_string(vm(), entry.name), false);
    else if (m_iteration_kind == JS::Object::PropertyKind::Value)
        return create_iterator_result_object(vm(), JS::js_string(vm(), entry.value), false);

    return create_iterator_result_object(vm(), JS::Array::create_from(realm(), { JS::js_string(vm(), entry.name), JS::js_string(vm(), entry.value) }), false);
}

}
