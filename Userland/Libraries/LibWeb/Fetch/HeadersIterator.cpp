/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibWeb/Bindings/HeadersIteratorWrapper.h>
#include <LibWeb/Bindings/Wrapper.h>
#include <LibWeb/Fetch/HeadersIterator.h>

namespace Web::Fetch {

// https://webidl.spec.whatwg.org/#es-iterable, Step 2
JS::ThrowCompletionOr<JS::Object*> HeadersIterator::next()
{
    auto& global_object = wrapper()->global_object();
    auto& vm = global_object.vm();
    auto& realm = *global_object.associated_realm();

    // The value pairs to iterate over are the return value of running sort and combine with this’s header list.
    auto value_pairs_to_iterate_over = [&]() -> JS::ThrowCompletionOr<Vector<Fetch::Infrastructure::Header>> {
        auto headers_or_error = m_headers.m_header_list.sort_and_combine();
        if (headers_or_error.is_error())
            return vm.throw_completion<JS::InternalError>(JS::ErrorType::NotEnoughMemoryToAllocate);
        return headers_or_error.release_value();
    };

    auto pairs = TRY(value_pairs_to_iterate_over());

    if (m_index >= pairs.size())
        return create_iterator_result_object(vm, JS::js_undefined(), true);

    auto const& pair = pairs[m_index++];

    switch (m_iteration_kind) {
    case JS::Object::PropertyKind::Key:
        return create_iterator_result_object(vm, JS::js_string(vm, StringView { pair.name }), false);
    case JS::Object::PropertyKind::Value:
        return create_iterator_result_object(vm, JS::js_string(vm, StringView { pair.value }), false);
    case JS::Object::PropertyKind::KeyAndValue: {
        auto* array = JS::Array::create_from(realm, { JS::js_string(vm, StringView { pair.name }), JS::js_string(vm, StringView { pair.value }) });
        return create_iterator_result_object(vm, array, false);
    }
    default:
        VERIFY_NOT_REACHED();
    }
}

void HeadersIterator::visit_edges(JS::Cell::Visitor& visitor)
{
    visitor.visit(m_headers.wrapper());
}

}
