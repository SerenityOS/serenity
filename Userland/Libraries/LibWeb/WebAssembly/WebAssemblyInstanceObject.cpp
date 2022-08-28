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
#include <LibWeb/HTML/Window.h>
#include <LibWeb/WebAssembly/WebAssemblyInstanceObject.h>
#include <LibWeb/WebAssembly/WebAssemblyMemoryPrototype.h>
#include <LibWeb/WebAssembly/WebAssemblyObject.h>

namespace Web::Bindings {

WebAssemblyInstanceObject::WebAssemblyInstanceObject(JS::Realm& realm, size_t index)
    : Object(static_cast<Web::HTML::Window&>(realm.global_object()).ensure_web_prototype<WebAssemblyInstancePrototype>("WebAssemblyInstancePrototype"))
    , m_index(index)
{
}

void WebAssemblyInstanceObject::initialize(JS::Realm& realm)
{
    Object::initialize(realm);

    auto& vm = this->vm();

    VERIFY(!m_exports_object);
    m_exports_object = create(realm, nullptr);
    auto& instance = this->instance();
    auto& cache = this->cache();
    for (auto& export_ : instance.exports()) {
        export_.value().visit(
            [&](Wasm::FunctionAddress const& address) {
                Optional<JS::FunctionObject*> object = cache.function_instances.get(address);
                if (!object.has_value()) {
                    object = create_native_function(vm, address, export_.name());
                    cache.function_instances.set(address, *object);
                }
                m_exports_object->define_direct_property(export_.name(), *object, JS::default_attributes);
            },
            [&](Wasm::MemoryAddress const& address) {
                Optional<WebAssemblyMemoryObject*> object = cache.memory_instances.get(address);
                if (!object.has_value()) {
                    object = heap().allocate<Web::Bindings::WebAssemblyMemoryObject>(realm, realm, address);
                    cache.memory_instances.set(address, *object);
                }
                m_exports_object->define_direct_property(export_.name(), *object, JS::default_attributes);
            },
            [&](auto const&) {
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
