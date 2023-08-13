/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/Realm.h>
#include <LibJS/Runtime/VM.h>
#include <LibWasm/AbstractMachine/AbstractMachine.h>
#include <LibWeb/Bindings/InstancePrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/WebAssembly/Instance.h>
#include <LibWeb/WebAssembly/Memory.h>
#include <LibWeb/WebAssembly/Module.h>
#include <LibWeb/WebAssembly/Table.h>
#include <LibWeb/WebAssembly/WebAssembly.h>

namespace Web::WebAssembly {

WebIDL::ExceptionOr<JS::NonnullGCPtr<Instance>> Instance::construct_impl(JS::Realm& realm, Module& module, Optional<JS::Handle<JS::Object>>& import_object)
{
    // FIXME: Implement the importObject parameter.
    (void)import_object;

    auto& vm = realm.vm();

    auto index = TRY(Detail::instantiate_module(vm, module.module()));
    return vm.heap().allocate<Instance>(realm, realm, index);
}

Instance::Instance(JS::Realm& realm, size_t index)
    : Bindings::PlatformObject(realm)
    , m_exports(Object::create(realm, nullptr))
    , m_index(index)
{
}

void Instance::initialize(JS::Realm& realm)
{
    auto& vm = this->vm();

    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::InstancePrototype>(realm, "WebAssembly.Instance"sv));

    auto& instance = *Detail::s_instantiated_modules[m_index];
    auto& cache = Detail::s_module_caches.at(m_index);

    for (auto& export_ : instance.exports()) {
        export_.value().visit(
            [&](Wasm::FunctionAddress const& address) {
                Optional<JS::GCPtr<JS::FunctionObject>> object = cache.function_instances.get(address);
                if (!object.has_value()) {
                    object = Detail::create_native_function(vm, address, export_.name());
                    cache.function_instances.set(address, *object);
                }

                m_exports->define_direct_property(export_.name(), *object, JS::default_attributes);
            },
            [&](Wasm::MemoryAddress const& address) {
                Optional<JS::GCPtr<Memory>> object = cache.memory_instances.get(address);
                if (!object.has_value()) {
                    object = heap().allocate<Memory>(realm, realm, address);
                    cache.memory_instances.set(address, *object);
                }

                m_exports->define_direct_property(export_.name(), *object, JS::default_attributes);
            },
            [&](Wasm::TableAddress const& address) {
                Optional<JS::GCPtr<Table>> object = cache.table_instances.get(address);
                if (!object.has_value()) {
                    object = heap().allocate<Table>(realm, realm, address);
                    cache.table_instances.set(address, *object);
                }

                m_exports->define_direct_property(export_.name(), *object, JS::default_attributes);
            },
            [&](auto const&) {
                // FIXME: Implement other exports!
            });
    }

    MUST(m_exports->set_integrity_level(IntegrityLevel::Frozen));
}

void Instance::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_exports);
}

}
