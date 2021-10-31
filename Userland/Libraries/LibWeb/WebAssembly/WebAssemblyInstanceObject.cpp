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
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/WebAssembly/WebAssemblyInstanceObject.h>
#include <LibWeb/WebAssembly/WebAssemblyMemoryPrototype.h>
#include <LibWeb/WebAssembly/WebAssemblyObject.h>

namespace Web::Bindings {

WebAssemblyInstanceObject::WebAssemblyInstanceObject(JS::GlobalObject& global_object, size_t index)
    : Object(static_cast<Web::Bindings::WindowObject&>(global_object).ensure_web_prototype<WebAssemblyInstancePrototype>("WebAssemblyInstancePrototype"))
    , m_index(index)
{
}

void WebAssemblyInstanceObject::initialize(JS::GlobalObject& global_object)
{
    Object::initialize(global_object);

    VERIFY(!m_exports_object);
    m_exports_object = create(global_object, nullptr);
    auto& instance = this->instance();
    auto& cache = this->cache();
    for (auto& export_ : instance.exports()) {
        export_.value().visit(
            [&](const Wasm::FunctionAddress& address) {
                auto object = cache.function_instances.get(address);
                if (!object.has_value()) {
                    object = create_native_function(global_object, address, export_.name());
                    cache.function_instances.set(address, *object);
                }
                m_exports_object->define_direct_property(export_.name(), *object, JS::default_attributes);
            },
            [&](const Wasm::MemoryAddress& address) {
                auto object = cache.memory_instances.get(address);
                if (!object.has_value()) {
                    object = heap().allocate<Web::Bindings::WebAssemblyMemoryObject>(global_object, global_object, address);
                    cache.memory_instances.set(address, *object);
                }
                m_exports_object->define_direct_property(export_.name(), *object, JS::default_attributes);
            },
            [&](const auto&) {
                // FIXME: Implement other exports!
            });
    }

    MUST(m_exports_object->set_integrity_level(IntegrityLevel::Frozen));
}

void WebAssemblyInstanceObject::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_exports_object);
}

}
