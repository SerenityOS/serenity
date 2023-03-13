/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWasm/AbstractMachine/Interpreter.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/WebAssembly/WebAssemblyInstanceObject.h>
#include <LibWeb/WebAssembly/WebAssemblyMemoryPrototype.h>
#include <LibWeb/WebAssembly/WebAssemblyObject.h>
#include <LibWeb/WebAssembly/WebAssemblyTableObject.h>

namespace Web::Bindings {

WebAssemblyInstanceObject::WebAssemblyInstanceObject(JS::Realm& realm, size_t index)
    : Object(ConstructWithPrototypeTag::Tag, Bindings::ensure_web_prototype<WebAssemblyInstancePrototype>(realm, "WebAssembly.Instance"))
    , m_index(index)
{
}

JS::ThrowCompletionOr<void> WebAssemblyInstanceObject::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Object::initialize(realm));

    auto& vm = this->vm();

    VERIFY(!m_exports_object);
    m_exports_object = create(realm, nullptr);
    auto& instance = this->instance();
    auto& cache = this->cache();
    for (auto& export_ : instance.exports()) {
        TRY(export_.value().visit(
            [&](Wasm::FunctionAddress const& address) -> JS::ThrowCompletionOr<void> {
                Optional<JS::GCPtr<JS::FunctionObject>> object = cache.function_instances.get(address);
                if (!object.has_value()) {
                    object = create_native_function(vm, address, export_.name());
                    cache.function_instances.set(address, *object);
                }
                m_exports_object->define_direct_property(export_.name(), *object, JS::default_attributes);
                return {};
            },
            [&](Wasm::MemoryAddress const& address) -> JS::ThrowCompletionOr<void> {
                Optional<JS::GCPtr<WebAssemblyMemoryObject>> object = cache.memory_instances.get(address);
                if (!object.has_value()) {
                    object = MUST_OR_THROW_OOM(heap().allocate<Web::Bindings::WebAssemblyMemoryObject>(realm, realm, address));
                    cache.memory_instances.set(address, *object);
                }
                m_exports_object->define_direct_property(export_.name(), *object, JS::default_attributes);
                return {};
            },
            [&](Wasm::TableAddress const& address) -> JS::ThrowCompletionOr<void> {
                Optional<JS::GCPtr<WebAssemblyTableObject>> object = cache.table_instances.get(address);
                if (!object.has_value()) {
                    object = MUST_OR_THROW_OOM(heap().allocate<Web::Bindings::WebAssemblyTableObject>(realm, realm, address));
                    cache.table_instances.set(address, *object);
                }
                m_exports_object->define_direct_property(export_.name(), *object, JS::default_attributes);
                return {};
            },
            [&](auto const&) -> JS::ThrowCompletionOr<void> {
                // FIXME: Implement other exports!
                return {};
            }));
    }

    MUST(m_exports_object->set_integrity_level(IntegrityLevel::Frozen));

    return {};
}

void WebAssemblyInstanceObject::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_exports_object);
}

}
