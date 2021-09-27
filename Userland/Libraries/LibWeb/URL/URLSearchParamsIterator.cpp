/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibWeb/Bindings/URLSearchParamsIteratorWrapper.h>
#include <LibWeb/Bindings/Wrapper.h>
#include <LibWeb/URL/URLSearchParamsIterator.h>

namespace Web::URL {

JS::Object* URLSearchParamsIterator::next()
{
    auto& global_object = wrapper()->global_object();
    auto& vm = global_object.vm();

    if (m_index >= m_url_search_params.m_list.size())
        return create_iterator_result_object(global_object, JS::js_undefined(), true);

    auto& entry = m_url_search_params.m_list[m_index++];
    if (m_iteration_kind == JS::Object::PropertyKind::Key)
        return create_iterator_result_object(global_object, JS::js_string(vm, entry.name), false);
    else if (m_iteration_kind == JS::Object::PropertyKind::Value)
        return create_iterator_result_object(global_object, JS::js_string(vm, entry.value), false);

    return create_iterator_result_object(global_object, JS::Array::create_from(global_object, { JS::js_string(vm, entry.name), JS::js_string(vm, entry.value) }), false);
}

void URLSearchParamsIterator::visit_edges(JS::Cell::Visitor& visitor)
{
    visitor.visit(m_url_search_params.wrapper());
}

}
