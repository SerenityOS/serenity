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
#include <LibWeb/Bindings/InstancePrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/WebAssembly/Instance.h>
#include <LibWeb/WebAssembly/Memory.h>
#include <LibWeb/WebAssembly/Module.h>
#include <LibWeb/WebAssembly/Table.h>
#include <LibWeb/WebAssembly/WebAssembly.h>

namespace Web::WebAssembly {

JS_DEFINE_ALLOCATOR(Instance);

WebIDL::ExceptionOr<JS::NonnullGCPtr<Instance>> Instance::construct_impl(JS::Realm& realm, Module& module, Optional<JS::Handle<JS::Object>>& import_object)
{
    // FIXME: Implement the importObject parameter.
    (void)import_object;

    auto& vm = realm.vm();

    auto module_instance = TRY(Detail::instantiate_module(vm, module.compiled_module()->module));
    return vm.heap().allocate<Instance>(realm, realm, move(module_instance));
}

Instance::Instance(JS::Realm& realm, NonnullOwnPtr<Wasm::ModuleInstance> module_instance)
    : Bindings::PlatformObject(realm)
    , m_exports(Object::create(realm, nullptr))
    , m_module_instance(move(module_instance))
{
}

void Instance::initialize(JS::Realm& realm)
{
    auto& vm = this->vm();

    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE_WITH_CUSTOM_NAME(Instance, WebAssembly.Instance);

    for (auto& export_ : m_module_instance->exports()) {
        export_.value().visit(
            [&](Wasm::FunctionAddress const& address) {
                Optional<JS::GCPtr<JS::FunctionObject>> object = m_function_instances.get(address);
                if (!object.has_value()) {
                    object = Detail::create_native_function(vm, address, export_.name(), this);
                    m_function_instances.set(address, *object);
                }

                m_exports->define_direct_property(export_.name(), *object, JS::default_attributes);
            },
            [&](Wasm::MemoryAddress const& address) {
                Optional<JS::GCPtr<Memory>> object = m_memory_instances.get(address);
                if (!object.has_value()) {
                    object = heap().allocate<Memory>(realm, realm, address);
                    m_memory_instances.set(address, *object);
                }

                m_exports->define_direct_property(export_.name(), *object, JS::default_attributes);
            },
            [&](Wasm::TableAddress const& address) {
                Optional<JS::GCPtr<Table>> object = m_table_instances.get(address);
                if (!object.has_value()) {
                    object = heap().allocate<Table>(realm, realm, address);
                    m_table_instances.set(address, *object);
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
    visitor.visit(m_function_instances);
    visitor.visit(m_memory_instances);
    visitor.visit(m_table_instances);
}

}
