/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Promise.h>
#include <LibJS/Runtime/VM.h>
#include <LibWasm/AbstractMachine/AbstractMachine.h>
#include <LibWasm/AbstractMachine/Validator.h>
#include <LibWeb/WebAssembly/Instance.h>
#include <LibWeb/WebAssembly/Memory.h>
#include <LibWeb/WebAssembly/Module.h>
#include <LibWeb/WebAssembly/Table.h>
#include <LibWeb/WebAssembly/WebAssembly.h>
#include <LibWeb/WebAssembly/WebAssemblyObject.h>

namespace Web::WebAssembly {

void visit_edges(JS::Cell::Visitor& visitor)
{
    for (auto& entry : Bindings::WebAssemblyObject::s_global_cache.function_instances)
        visitor.visit(entry.value);

    for (auto& module_cache : Bindings::WebAssemblyObject::s_module_caches) {
        for (auto& entry : module_cache.function_instances)
            visitor.visit(entry.value);
        for (auto& entry : module_cache.memory_instances)
            visitor.visit(entry.value);
        for (auto& entry : module_cache.table_instances)
            visitor.visit(entry.value);
    }
}

// https://webassembly.github.io/spec/js-api/#dom-webassembly-validate
bool validate(JS::VM& vm, JS::Handle<JS::Object>& bytes)
{
    // 1. Let stableBytes be a copy of the bytes held by the buffer bytes.
    // Note: There's no need to copy the bytes here as the buffer data cannot change while we're compiling the module.

    // 2. Compile stableBytes as a WebAssembly module and store the results as module.
    auto maybe_module = Bindings::parse_module(vm, bytes.cell());

    // 3. If module is error, return false.
    if (maybe_module.is_error())
        return false;

    // Drop the module from the cache, we're never going to refer to it.
    ScopeGuard drop_from_cache { [&] { (void)Bindings::WebAssemblyObject::s_compiled_modules.take_last(); } };

    // 3 continued - our "compile" step is lazy with validation, explicitly do the validation.
    if (Bindings::WebAssemblyObject::s_abstract_machine.validate(Bindings::WebAssemblyObject::s_compiled_modules[maybe_module.value()]->module).is_error())
        return false;

    // 4. Return true.
    return true;
}

// https://webassembly.github.io/spec/js-api/#dom-webassembly-compile
WebIDL::ExceptionOr<JS::Value> compile(JS::VM& vm, JS::Handle<JS::Object>& bytes)
{
    auto& realm = *vm.current_realm();

    // FIXME: This shouldn't block!
    auto module = Bindings::parse_module(vm, bytes.cell());
    auto promise = JS::Promise::create(realm);

    if (module.is_error()) {
        promise->reject(*module.release_error().value());
    } else {
        auto module_object = MUST_OR_THROW_OOM(vm.heap().allocate<Module>(realm, realm, module.release_value()));
        promise->fulfill(module_object);
    }

    return promise;
}

// https://webassembly.github.io/spec/js-api/#dom-webassembly-instantiate
WebIDL::ExceptionOr<JS::Value> instantiate(JS::VM& vm, JS::Handle<JS::Object>& bytes, Optional<JS::Handle<JS::Object>>& import_object)
{
    // FIXME: Implement the importObject parameter.
    (void)import_object;

    auto& realm = *vm.current_realm();

    // FIXME: This shouldn't block!
    auto module = Bindings::parse_module(vm, bytes.cell());
    auto promise = JS::Promise::create(realm);

    if (module.is_error()) {
        promise->reject(*module.release_error().value());
        return promise;
    }

    auto const& compiled_module = Bindings::WebAssemblyObject::s_compiled_modules.at(module.release_value())->module;
    auto result = Bindings::WebAssemblyObject::instantiate_module(vm, compiled_module);

    if (result.is_error()) {
        promise->reject(*result.release_error().value());
    } else {
        auto module_object = MUST_OR_THROW_OOM(vm.heap().allocate<Module>(realm, realm, Bindings::WebAssemblyObject::s_compiled_modules.size() - 1));
        auto instance_object = MUST_OR_THROW_OOM(vm.heap().allocate<Instance>(realm, realm, result.release_value()));

        auto object = JS::Object::create(realm, nullptr);
        object->define_direct_property("module", module_object, JS::default_attributes);
        object->define_direct_property("instance", instance_object, JS::default_attributes);
        promise->fulfill(object);
    }

    return promise;
}

// https://webassembly.github.io/spec/js-api/#dom-webassembly-instantiate-moduleobject-importobject
WebIDL::ExceptionOr<JS::Value> instantiate(JS::VM& vm, Module const& module_object, Optional<JS::Handle<JS::Object>>& import_object)
{
    // FIXME: Implement the importObject parameter.
    (void)import_object;

    auto& realm = *vm.current_realm();
    auto promise = JS::Promise::create(realm);

    auto const& compiled_module = module_object.module();
    auto result = Bindings::WebAssemblyObject::instantiate_module(vm, compiled_module);

    if (result.is_error()) {
        promise->reject(*result.release_error().value());
    } else {
        auto instance_object = MUST_OR_THROW_OOM(vm.heap().allocate<Instance>(realm, realm, result.release_value()));
        promise->fulfill(instance_object);
    }

    return promise;
}

}
