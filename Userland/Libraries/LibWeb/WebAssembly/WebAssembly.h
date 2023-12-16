/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Handle.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Value.h>
#include <LibWasm/AbstractMachine/AbstractMachine.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Forward.h>

namespace Web::WebAssembly {

void visit_edges(JS::Cell::Visitor&);

bool validate(JS::VM&, JS::Handle<WebIDL::BufferSource>& bytes);
WebIDL::ExceptionOr<JS::Value> compile(JS::VM&, JS::Handle<WebIDL::BufferSource>& bytes);

WebIDL::ExceptionOr<JS::Value> instantiate(JS::VM&, JS::Handle<WebIDL::BufferSource>& bytes, Optional<JS::Handle<JS::Object>>& import_object);
WebIDL::ExceptionOr<JS::Value> instantiate(JS::VM&, Module const& module_object, Optional<JS::Handle<JS::Object>>& import_object);

namespace Detail {

JS::ThrowCompletionOr<size_t> instantiate_module(JS::VM&, Wasm::Module const&);
JS::ThrowCompletionOr<size_t> parse_module(JS::VM&, JS::Object* buffer);
JS::NativeFunction* create_native_function(JS::VM&, Wasm::FunctionAddress address, ByteString const& name);
JS::ThrowCompletionOr<Wasm::Value> to_webassembly_value(JS::VM&, JS::Value value, Wasm::ValueType const& type);
JS::Value to_js_value(JS::VM&, Wasm::Value& wasm_value);

struct CompiledWebAssemblyModule {
    explicit CompiledWebAssemblyModule(Wasm::Module&& module)
        : module(move(module))
    {
    }

    Wasm::Module module;
};

// FIXME: These should just be members of the module (instance) object, but the module needs to stick
//        around while its instance is alive so ideally this would be a refcounted object, shared between
//        WebAssemblyModuleObject's and WebAssemblyInstantiatedModuleObject's.
struct ModuleCache {
    HashMap<Wasm::FunctionAddress, JS::GCPtr<JS::FunctionObject>> function_instances;
    HashMap<Wasm::MemoryAddress, JS::GCPtr<WebAssembly::Memory>> memory_instances;
    HashMap<Wasm::TableAddress, JS::GCPtr<WebAssembly::Table>> table_instances;
};
struct GlobalModuleCache {
    HashMap<Wasm::FunctionAddress, JS::GCPtr<JS::NativeFunction>> function_instances;
};

extern Vector<NonnullOwnPtr<CompiledWebAssemblyModule>> s_compiled_modules;
extern Vector<NonnullOwnPtr<Wasm::ModuleInstance>> s_instantiated_modules;
extern Vector<ModuleCache> s_module_caches;
extern GlobalModuleCache s_global_cache;
extern Wasm::AbstractMachine s_abstract_machine;

}

}
