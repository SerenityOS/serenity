/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>
#include <LibWasm/AbstractMachine/AbstractMachine.h>
#include <LibWeb/Forward.h>

namespace Web::Bindings {

JS::ThrowCompletionOr<size_t> parse_module(JS::VM&, JS::Object* buffer);
JS::NativeFunction* create_native_function(JS::VM&, Wasm::FunctionAddress address, DeprecatedString const& name);
JS::Value to_js_value(JS::VM&, Wasm::Value& wasm_value);
JS::ThrowCompletionOr<Wasm::Value> to_webassembly_value(JS::VM&, JS::Value value, Wasm::ValueType const& type);

class WebAssemblyObject final {
public:
    static JS::ThrowCompletionOr<size_t> instantiate_module(JS::VM&, Wasm::Module const&);

    struct CompiledWebAssemblyModule {
        explicit CompiledWebAssemblyModule(Wasm::Module&& module)
            : module(move(module))
        {
        }

        Wasm::Module module;
    };

    // FIXME: These should just be members of the module (instance) object,
    //        but the module needs to stick around while its instance is alive
    //        so ideally this would be a refcounted object, shared between
    //        WebAssemblyModuleObject's and WebAssemblyInstantiatedModuleObject's.
    struct ModuleCache {
        HashMap<Wasm::FunctionAddress, JS::GCPtr<JS::FunctionObject>> function_instances;
        HashMap<Wasm::MemoryAddress, JS::GCPtr<WebAssembly::Memory>> memory_instances;
        HashMap<Wasm::TableAddress, JS::GCPtr<WebAssembly::Table>> table_instances;
    };
    struct GlobalModuleCache {
        HashMap<Wasm::FunctionAddress, JS::GCPtr<JS::NativeFunction>> function_instances;
    };

    static Vector<NonnullOwnPtr<CompiledWebAssemblyModule>> s_compiled_modules;
    static Vector<NonnullOwnPtr<Wasm::ModuleInstance>> s_instantiated_modules;
    static Vector<ModuleCache> s_module_caches;
    static GlobalModuleCache s_global_cache;

    static Wasm::AbstractMachine s_abstract_machine;
};

}
