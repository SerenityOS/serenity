/*
 * Copyright (c) 2024, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Array.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::WebIDL {

// https://webidl.spec.whatwg.org/#idl-observable-array
class ObservableArray final : public JS::Array {
public:
    static JS::NonnullGCPtr<ObservableArray> create(JS::Realm& realm);

    virtual JS::ThrowCompletionOr<bool> internal_set(JS::PropertyKey const& property_key, JS::Value value, JS::Value receiver, JS::CacheablePropertyMetadata* metadata = nullptr) override;
    virtual JS::ThrowCompletionOr<bool> internal_delete(JS::PropertyKey const& property_key) override;

    using SetAnIndexedValueCallbackFunction = Function<ExceptionOr<void>(JS::Value&)>;
    using DeleteAnIndexedValueCallbackFunction = Function<ExceptionOr<void>()>;

    void set_on_set_an_indexed_value_callback(SetAnIndexedValueCallbackFunction&& callback);
    void set_on_delete_an_indexed_value_callback(DeleteAnIndexedValueCallbackFunction&& callback);

    JS::ThrowCompletionOr<void> append(JS::Value value);
    void clear();

    explicit ObservableArray(Object& prototype);

    virtual void visit_edges(JS::Cell::Visitor&) override;

private:
    using SetAnIndexedValueCallbackHeapFunction = JS::HeapFunction<SetAnIndexedValueCallbackFunction::FunctionType>;
    using DeleteAnIndexedValueCallbackHeapFunction = JS::HeapFunction<DeleteAnIndexedValueCallbackFunction::FunctionType>;

    JS::GCPtr<SetAnIndexedValueCallbackHeapFunction> m_on_set_an_indexed_value;
    JS::GCPtr<DeleteAnIndexedValueCallbackHeapFunction> m_on_delete_an_indexed_value;
};

}
