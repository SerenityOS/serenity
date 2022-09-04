/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibWeb/Bindings/HeadersIteratorPrototype.h>
#include <LibWeb/Fetch/HeadersIterator.h>
#include <LibWeb/HTML/Window.h>

namespace Web::Fetch {

JS::NonnullGCPtr<HeadersIterator> HeadersIterator::create(Headers const& headers, JS::Object::PropertyKind iteration_kind)
{
    return *headers.heap().allocate<HeadersIterator>(headers.realm(), headers, iteration_kind);
}

HeadersIterator::HeadersIterator(Headers const& headers, JS::Object::PropertyKind iteration_kind)
    : PlatformObject(headers.realm())
    , m_headers(headers)
    , m_iteration_kind(iteration_kind)
{
    set_prototype(&headers.global_object().ensure_web_prototype<Bindings::HeadersIteratorPrototype>("HeadersIterator"));
}

HeadersIterator::~HeadersIterator() = default;

void HeadersIterator::visit_edges(JS::Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_headers);
}

// https://webidl.spec.whatwg.org/#es-iterable, Step 2
JS::ThrowCompletionOr<JS::Object*> HeadersIterator::next()
{
    // The value pairs to iterate over are the return value of running sort and combine with this’s header list.
    auto value_pairs_to_iterate_over = [&]() -> JS::ThrowCompletionOr<Vector<Fetch::Infrastructure::Header>> {
        auto headers_or_error = m_headers.m_header_list.sort_and_combine();
        if (headers_or_error.is_error())
            return vm().throw_completion<JS::InternalError>(JS::ErrorType::NotEnoughMemoryToAllocate);
        return headers_or_error.release_value();
    };

    auto pairs = TRY(value_pairs_to_iterate_over());

    if (m_index >= pairs.size())
        return create_iterator_result_object(vm(), JS::js_undefined(), true);

    auto const& pair = pairs[m_index++];

    switch (m_iteration_kind) {
    case JS::Object::PropertyKind::Key:
        return create_iterator_result_object(vm(), JS::js_string(vm(), StringView { pair.name }), false);
    case JS::Object::PropertyKind::Value:
        return create_iterator_result_object(vm(), JS::js_string(vm(), StringView { pair.value }), false);
    case JS::Object::PropertyKind::KeyAndValue: {
        auto* array = JS::Array::create_from(realm(), { JS::js_string(vm(), StringView { pair.name }), JS::js_string(vm(), StringView { pair.value }) });
        return create_iterator_result_object(vm(), array, false);
    }
    default:
        VERIFY_NOT_REACHED();
    }
}

}
