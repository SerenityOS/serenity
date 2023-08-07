/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Iterator.h>
#include <LibWeb/Bindings/HeadersIteratorPrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Fetch/HeadersIterator.h>

namespace Web::Bindings {

template<>
void Intrinsics::create_web_prototype_and_constructor<HeadersIteratorPrototype>(JS::Realm& realm)
{
    auto prototype = heap().allocate<HeadersIteratorPrototype>(realm, realm).release_allocated_value_but_fixme_should_propagate_errors();
    m_prototypes.set("HeadersIterator"sv, prototype);
}

}

namespace Web::Fetch {

WebIDL::ExceptionOr<JS::NonnullGCPtr<HeadersIterator>> HeadersIterator::create(Headers const& headers, JS::Object::PropertyKind iteration_kind)
{
    return MUST_OR_THROW_OOM(headers.heap().allocate<HeadersIterator>(headers.realm(), headers, iteration_kind));
}

HeadersIterator::HeadersIterator(Headers const& headers, JS::Object::PropertyKind iteration_kind)
    : PlatformObject(headers.realm())
    , m_headers(headers)
    , m_iteration_kind(iteration_kind)
{
}

HeadersIterator::~HeadersIterator() = default;

void HeadersIterator::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HeadersIteratorPrototype>(realm, "HeadersIterator"));
}

void HeadersIterator::visit_edges(JS::Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_headers);
}

// https://webidl.spec.whatwg.org/#es-iterable, Step 2
JS::ThrowCompletionOr<JS::Object*> HeadersIterator::next()
{
    // The value pairs to iterate over are the return value of running sort and combine with this’s header list.
    auto value_pairs_to_iterate_over = [&]() -> JS::ThrowCompletionOr<Vector<Fetch::Infrastructure::Header>> {
        auto headers_or_error = m_headers->m_header_list->sort_and_combine();
        if (headers_or_error.is_error())
            return vm().throw_completion<JS::InternalError>(JS::ErrorType::NotEnoughMemoryToAllocate);
        return headers_or_error.release_value();
    };

    auto pairs = TRY(value_pairs_to_iterate_over());

    if (m_index >= pairs.size())
        return create_iterator_result_object(vm(), JS::js_undefined(), true).ptr();

    auto const& pair = pairs[m_index++];
    StringView pair_name { pair.name };
    StringView pair_value { pair.value };

    switch (m_iteration_kind) {
    case JS::Object::PropertyKind::Key:
        return create_iterator_result_object(vm(), MUST_OR_THROW_OOM(JS::PrimitiveString::create(vm(), pair_name)), false).ptr();
    case JS::Object::PropertyKind::Value:
        return create_iterator_result_object(vm(), MUST_OR_THROW_OOM(JS::PrimitiveString::create(vm(), pair_value)), false).ptr();
    case JS::Object::PropertyKind::KeyAndValue: {
        auto array = JS::Array::create_from(realm(), { MUST_OR_THROW_OOM(JS::PrimitiveString::create(vm(), pair_name)), MUST_OR_THROW_OOM(JS::PrimitiveString::create(vm(), pair_value)) });
        return create_iterator_result_object(vm(), array, false).ptr();
    }
    default:
        VERIFY_NOT_REACHED();
    }
}

}
