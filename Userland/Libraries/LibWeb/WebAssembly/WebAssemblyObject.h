/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>
#include <LibWasm/AbstractMachine/AbstractMachine.h>
#include <LibWeb/Forward.h>
#include <LibWeb/WebAssembly/WebAssemblyInstanceObjectPrototype.h>

namespace Web::Bindings {

class WebAssemblyMemoryObject;
class WebAssemblyTableObject;
JS::ThrowCompletionOr<size_t> parse_module(JS::VM&, JS::Object* buffer);
JS::NativeFunction* create_native_function(JS::VM&, Wasm::FunctionAddress address, DeprecatedString const& name);
JS::Value to_js_value(JS::VM&, Wasm::Value& wasm_value);
JS::ThrowCompletionOr<Wasm::Value> to_webassembly_value(JS::VM&, JS::Value value, Wasm::ValueType const& type);

class WebAssemblyObject final : public JS::Object {
    JS_OBJECT(WebAssemblyObject, JS::Object);

public:
    explicit WebAssemblyObject(JS::Realm&);
    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
    virtual ~WebAssemblyObject() override = default;

    virtual void visit_edges(Cell::Visitor&) override;

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
        HashMap<Wasm::MemoryAddress, JS::GCPtr<WebAssemblyMemoryObject>> memory_instances;
        HashMap<Wasm::TableAddress, JS::GCPtr<WebAssemblyTableObject>> table_instances;
    };
    struct GlobalModuleCache {
        HashMap<Wasm::FunctionAddress, JS::GCPtr<JS::NativeFunction>> function_instances;
    };

    static Vector<NonnullOwnPtr<CompiledWebAssemblyModule>> s_compiled_modules;
    static Vector<NonnullOwnPtr<Wasm::ModuleInstance>> s_instantiated_modules;
    static Vector<ModuleCache> s_module_caches;
    static GlobalModuleCache s_global_cache;

    static Wasm::AbstractMachine s_abstract_machine;

private:
    JS_DECLARE_NATIVE_FUNCTION(validate);
    JS_DECLARE_NATIVE_FUNCTION(compile);
    JS_DECLARE_NATIVE_FUNCTION(instantiate);
};

class WebAssemblyMemoryObject final : public JS::Object {
    JS_OBJECT(WebAssemblyMemoryObject, JS::Object);

public:
    WebAssemblyMemoryObject(JS::Realm&, Wasm::MemoryAddress);
    virtual ~WebAssemblyMemoryObject() override = default;

    auto address() const { return m_address; }

private:
    Wasm::MemoryAddress m_address;
};

}
